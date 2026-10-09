package fr.talosi.formation.support.web;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import fr.talosi.formation.support.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping ("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    public record ChatRequest(@NotBlank @Size(max=4000) String message) {
    }

    public record ChatResponse(String reply, String mode) {}

    public record StreamPayload(String content) {
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest chatRequest) {
        String reply = chatService.reply(chatRequest.message());
        return new ChatResponse(reply, "Ollama");
    }

    @GetMapping(value= "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamPayload>> stream(@RequestParam @NotBlank @Size(max=4000) String message) {
        
        Flux<ServerSentEvent<StreamPayload>> chunks = chatService.stream(message)
        .map(chunk -> ServerSentEvent.builder(new StreamPayload(chunk))
        .event("chunk")
        .build()); 
        
        Flux<ServerSentEvent<StreamPayload>> done = Flux.just(
                ServerSentEvent.<StreamPayload>builder()
                .event("done")
                .data(new StreamPayload("J4AI FINI"))
                .build()
        );
        
        
        return chunks
                .concatWith(done)
                             ;
    }
    

}
