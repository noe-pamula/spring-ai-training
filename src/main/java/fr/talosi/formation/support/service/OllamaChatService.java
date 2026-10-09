package fr.talosi.formation.support.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class OllamaChatService implements ChatService {

    private final ChatClient chatClient;

    public OllamaChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Tu es un assistant de support informatique.
                        Réponds en français, de manière concise et pratique.
                        Si tu ne connais pas la réponse, indique-le clairement.
                        """)
                .build();
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
