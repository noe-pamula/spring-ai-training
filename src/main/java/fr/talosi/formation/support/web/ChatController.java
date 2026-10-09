package fr.talosi.formation.support.web;

import fr.talosi.formation.support.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String message) {
    }

    public record ChatResponse(String reply, String mode) {
    }

    public record StreamPayload(String content) {
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(chat.reply(request.message()), "ollama:qwen3:0.6b");
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamPayload>> stream(
            @RequestParam @NotBlank @Size(max = 4000) String message
    ) {
        Flux<ServerSentEvent<StreamPayload>> chunks = chat.stream(message)
                .map(content -> ServerSentEvent.builder(new StreamPayload(content))
                        .event("chunk")
                        .build());

        Flux<ServerSentEvent<StreamPayload>> done = Flux.just(
                ServerSentEvent.<StreamPayload>builder()
                        .event("done")
                        .data(new StreamPayload(""))
                        .build()
        );

        return chunks
                .concatWith(done)
                .onErrorResume(error -> Flux.just(
                        ServerSentEvent.<StreamPayload>builder()
                                .event("generation-error")
                                .data(new StreamPayload("La génération de la réponse a échoué."))
                                .build()
                ));
    }
}
