package fr.talosi.formation.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SupportHttpTest {

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
        assertEquals(400, call("POST", "/api/chat", "{\"message\":\"  \"}").statusCode());

        HttpResponse<String> chat = call("POST", "/api/chat", "{\"message\":\"Bonjour\"}");
        assertEquals(200, chat.statusCode());
        assertTrue(chat.body().contains("simulation"));
        assertTrue(chat.body().contains("Bonjour"));

        assertEquals(200, call("GET", "/js/app.js", null).statusCode());
    }
}
