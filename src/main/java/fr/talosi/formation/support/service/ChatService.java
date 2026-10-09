package fr.talosi.formation.support.service;
import reactor.core.publisher.Flux;
public interface ChatService {
    String reply(String message);

    default Flux<String> stream(String message) {
        return Flux.just(reply(message));
    }
}
