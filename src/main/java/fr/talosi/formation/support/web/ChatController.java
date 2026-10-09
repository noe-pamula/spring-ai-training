package fr.talosi.formation.support.web;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import fr.talosi.formation.support.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


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

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest chatRequest) {
        String reply = chatService.reply(chatRequest.message());
        return new ChatResponse(reply, "Ollama");
    }

}
