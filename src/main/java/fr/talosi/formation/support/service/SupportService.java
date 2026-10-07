package fr.talosi.formation.support.service;

import fr.talosi.formation.support.model.Article;
import fr.talosi.formation.support.model.Ticket;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SupportService {

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(3);
    private final List<Article> articles = List.of(
            new Article(
                    1,
                    "Réinitialiser son mot de passe",
                    "Compte",
                    "Ouvrez le portail interne, cliquez sur Mot de passe oublié puis suivez le lien envoyé par e-mail. Ne communiquez jamais votre mot de passe au support."
            ),
            new Article(
                    2,
                    "Diagnostiquer une connexion VPN",
                    "Réseau",
                    "Vérifiez votre accès Internet, redémarrez le client VPN et contrôlez la validation MFA. Si le problème persiste, indiquez le code erreur dans un ticket."
            ),
            new Article(
                    3,
                    "Libérer de l’espace disque",
                    "Poste de travail",
                    "Videz la corbeille et supprimez les fichiers temporaires via les paramètres système. Ne supprimez pas de fichiers système."
            )
    );

    public SupportService() {
        tickets.put(
                1L,
                new Ticket(
                        1,
                        "Connexion VPN impossible",
                        "Erreur de connexion après validation MFA.",
                        "Alice Martin",
                        Ticket.Priority.HIGH,
                        Ticket.Status.OPEN,
                        Instant.parse("2026-09-28T08:00:00Z")
                )
        );
        tickets.put(
                2L,
                new Ticket(
                        2,
                        "Mot de passe expiré",
                        "Impossible de me connecter au portail.",
                        "Mehdi Dupont",
                        Ticket.Priority.NORMAL,
                        Ticket.Status.IN_PROGRESS,
                        Instant.parse("2026-09-29T09:30:00Z")
                )
        );
        tickets.put(
                3L,
                new Ticket(
                        3,
                        "Espace disque insuffisant",
                        "Le nettoyage a libéré 15 Go.",
                        "Emma Leroy",
                        Ticket.Priority.LOW,
                        Ticket.Status.RESOLVED,
                        Instant.parse("2026-09-30T13:00:00Z")
                )
        );
    }

    public List<Ticket> tickets(Ticket.Status status, String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);

        return tickets.values().stream()
                .filter(ticket -> status == null || ticket.status() == status)
                .filter(ticket -> (ticket.title() + " " + ticket.description() + " " + ticket.requester())
                        .toLowerCase(Locale.ROOT)
                        .contains(q))
                .sorted(Comparator.comparingLong(Ticket::id).reversed())
                .toList();
    }

    public Ticket ticket(long id) {
        Ticket ticket = tickets.get(id);
        if (ticket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable");
        }
        return ticket;
    }

    public Ticket create(
            String title,
            String description,
            String requester,
            Ticket.Priority priority
    ) {
        long id = sequence.incrementAndGet();
        Ticket ticket = new Ticket(
                id,
                title.trim(),
                description.trim(),
                requester.trim(),
                priority,
                Ticket.Status.OPEN,
                Instant.now()
        );
        tickets.put(id, ticket);
        return ticket;
    }

    public Ticket updateStatus(long id, Ticket.Status status) {
        Ticket result = tickets.computeIfPresent(id, (key, ticket) -> ticket.withStatus(status));
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable");
        }
        return result;
    }

    public List<Article> articles() {
        return articles;
    }

    public Article article(long id) {
        return articles.stream()
                .filter(article -> article.id() == id)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Article introuvable"
                ));
    }
}
