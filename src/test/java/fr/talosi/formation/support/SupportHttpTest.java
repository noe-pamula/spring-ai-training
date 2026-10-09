package fr.talosi.formation.support;

import fr.talosi.formation.support.model.Priority;
import fr.talosi.formation.support.model.TicketAnalysis;
import fr.talosi.formation.support.service.ChatService;
import fr.talosi.formation.support.service.TicketAnalysisException;
import fr.talosi.formation.support.service.TicketAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SupportHttpTest.ChatTestConfiguration.class)
class SupportHttpTest {

    @TestConfiguration
    static class ChatTestConfiguration {

        @Bean
        @Primary
        ChatService testChatService() {
            return new ChatService() {
                @Override
                public String reply(String message) {
                    return "Réponse Ollama simulée pour le test : " + message;
                }

                @Override
                public Flux<String> stream(String message) {
                    if (message.equals("erreur")) {
                        return Flux.error(new IllegalStateException("Échec simulé"));
                    }
                    return Flux.just("Réponse", " ", "progressive", " : ", message);
                }
            };
        }

        @Bean
        @Primary
        TicketAnalysisService testTicketAnalysisService() {
            return ticketText -> switch (ticketText) {
                case "Le VPN affiche l'erreur 619 depuis 9h pour toute l'équipe commerciale." ->
                        new TicketAnalysis(
                                "Erreur VPN 619 depuis 9h pour toute l'équipe commerciale.",
                                Priority.HIGH,
                                List.of()
                        );
                case "Ça ne marche pas." -> new TicketAnalysis(
                        "Un dysfonctionnement non précisé est signalé.",
                        Priority.MEDIUM,
                        List.of(
                                "Quel équipement ou service est concerné ?",
                                "Quel message d'erreur est affiché ?",
                                "Depuis quand le problème se produit-il ?",
                                "Quel est l'impact pour l'utilisateur ?"
                        )
                );
                case "conversion invalide" -> throw new TicketAnalysisException(
                        "Conversion simulée"
                );
                default -> new TicketAnalysis(ticketText, Priority.MEDIUM, List.of());
            };
        }
    }

    @LocalServerPort
    private int port;

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + path)
        );
        HttpRequest.BodyPublisher bodyPublisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

        builder
                .header("Content-Type", "application/json")
                .method(method, bodyPublisher);

        return HttpClient.newHttpClient().send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    @Test
    void fullJourney() throws Exception {
        HttpResponse<String> page = call("GET", "/", null);
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Talosi · Support technique"));
        assertTrue(page.body().contains("Réponse complète"));
        assertTrue(page.body().contains("Réponse en streaming"));

        assertTrue(call("GET", "/api/tickets?status=OPEN&q=VPN", null)
                .body()
                .contains("Connexion VPN impossible"));
        assertEquals("[]", call("GET", "/api/tickets?q=introuvablexyz", null).body());
        assertTrue(call("GET", "/api/articles", null).body().contains("VPN"));
        assertEquals(200, call("GET", "/api/articles/1", null).statusCode());

        HttpResponse<String> created = call(
                "POST",
                "/api/tickets",
                "{\"title\":\"Test\",\"description\":\"Incident\",\"requester\":\"Alice\",\"priority\":\"HIGH\"}"
        );
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        assertEquals(200, call("GET", location, null).statusCode());

        HttpResponse<String> changed = call(
                "PATCH",
                location + "/status",
                "{\"status\":\"RESOLVED\"}"
        );
        assertEquals(200, changed.statusCode());
        assertTrue(changed.body().contains("RESOLVED"));
        assertTrue(call("GET", location, null).body().contains("RESOLVED"));

        assertEquals(404, call("GET", "/api/tickets/99999", null).statusCode());
        assertEquals(
                404,
                call("PATCH", "/api/tickets/99999/status", "{\"status\":\"OPEN\"}").statusCode()
        );
        assertEquals(400, call("POST", "/api/tickets", "{}").statusCode());
        assertEquals(
                400,
                call("PATCH", location + "/status", "{\"status\":\"UNKNOWN\"}").statusCode()
        );

        HttpResponse<String> completeAnalysis = call(
                "POST",
                "/tickets/analyze",
                "{\"ticketText\":\"Le VPN affiche l'erreur 619 depuis 9h pour toute l'équipe commerciale.\"}"
        );
        assertEquals(200, completeAnalysis.statusCode());
        assertTrue(completeAnalysis.body().contains("\"priority\":\"HIGH\""));
        assertTrue(completeAnalysis.body().contains("\"missingInformation\":[]"));

        HttpResponse<String> vagueAnalysis = call(
                "POST",
                "/tickets/analyze",
                "{\"ticketText\":\"Ça ne marche pas.\"}"
        );
        assertEquals(200, vagueAnalysis.statusCode());
        assertTrue(vagueAnalysis.body().contains("\"priority\":\"MEDIUM\""));
        assertTrue(vagueAnalysis.body().contains("Quel équipement ou service est concerné ?"));
        assertTrue(vagueAnalysis.body().contains("Quel est l'impact pour l'utilisateur ?"));
        assertEquals(
                400,
                call("POST", "/tickets/analyze", "{\"ticketText\":\"  \"}").statusCode()
        );

        HttpResponse<String> invalidAnalysis = call(
                "POST",
                "/tickets/analyze",
                "{\"ticketText\":\"conversion invalide\"}"
        );
        assertEquals(502, invalidAnalysis.statusCode());
        assertTrue(invalidAnalysis.body().contains("Réponse structurée invalide"));
        assertTrue(invalidAnalysis.body().contains("analyse exploitable"));

        assertEquals(400, call("POST", "/api/chat", "{\"message\":\"  \"}").statusCode());

        HttpResponse<String> chat = call("POST", "/api/chat", "{\"message\":\"Bonjour\"}");
        assertEquals(200, chat.statusCode());
        assertTrue(chat.body().contains("ollama:qwen3:0.6b"));
        assertTrue(chat.body().contains("Bonjour"));

        HttpResponse<String> stream = call(
                "GET",
                "/api/chat/stream?message=Bonjour",
                null
        );
        assertEquals(200, stream.statusCode());
        assertTrue(stream.headers().firstValue("Content-Type").orElse("")
                .startsWith("text/event-stream"));
        assertTrue(stream.body().indexOf("Réponse") < stream.body().indexOf("progressive"));
        assertTrue(stream.body().indexOf("progressive") < stream.body().indexOf("Bonjour"));
        assertTrue(stream.body().contains("{\"content\":\" \"}"));
        assertTrue(stream.body().contains("{\"content\":\" : \"}"));
        assertTrue(stream.body().contains("event:done"));

        HttpResponse<String> streamError = call(
                "GET",
                "/api/chat/stream?message=erreur",
                null
        );
        assertEquals(200, streamError.statusCode());
        assertTrue(streamError.body().contains("event:generation-error"));
        assertTrue(streamError.body().contains("La génération de la réponse a échoué."));
        assertEquals(
                400,
                call("GET", "/api/chat/stream?message=%20", null).statusCode()
        );

        assertEquals(200, call("GET", "/js/app.js", null).statusCode());
    }
}
