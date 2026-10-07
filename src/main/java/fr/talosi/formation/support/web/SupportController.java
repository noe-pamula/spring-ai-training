package fr.talosi.formation.support.web;

import fr.talosi.formation.support.model.Article;
import fr.talosi.formation.support.model.Ticket;
import fr.talosi.formation.support.service.SupportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SupportController {

    private final SupportService support;

    public SupportController(SupportService support) {
        this.support = support;
    }

    public record CreateTicket(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 3000) String description,
            @NotBlank @Size(max = 100) String requester,
            @NotNull Ticket.Priority priority
    ) {
    }

    public record ChangeStatus(@NotNull Ticket.Status status) {
    }

    @GetMapping("/tickets")
    public List<Ticket> tickets(
            @RequestParam(required = false) Ticket.Status status,
            @RequestParam(required = false) String q
    ) {
        return support.tickets(status, q);
    }

    @GetMapping("/tickets/{id}")
    public Ticket ticket(@PathVariable long id) {
        return support.ticket(id);
    }

    @PostMapping("/tickets")
    public ResponseEntity<Ticket> create(@Valid @RequestBody CreateTicket request) {
        Ticket ticket = support.create(
                request.title(),
                request.description(),
                request.requester(),
                request.priority()
        );
        return ResponseEntity
                .created(URI.create("/api/tickets/" + ticket.id()))
                .body(ticket);
    }

    @PatchMapping("/tickets/{id}/status")
    public Ticket status(
            @PathVariable long id,
            @Valid @RequestBody ChangeStatus request
    ) {
        return support.updateStatus(id, request.status());
    }

    @GetMapping("/articles")
    public List<Article> articles() {
        return support.articles();
    }

    @GetMapping("/articles/{id}")
    public Article article(@PathVariable long id) {
        return support.article(id);
    }
}
