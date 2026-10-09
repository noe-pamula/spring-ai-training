package fr.talosi.formation.support.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service 
public class OllamaChatService implements ChatService {
    
    private final ChatClient chatClient;

    public OllamaChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String reply(String message) {
        return chatClient.prompt()
        .user(message.trim())
        .call()
        .content();
    }

    @Override 
    public Flux<String> stream(String message) {
        return chatClient.prompt()
        .user(message.trim())
        .stream()
        .content();
    }
}
