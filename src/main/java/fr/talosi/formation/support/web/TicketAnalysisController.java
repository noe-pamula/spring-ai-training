package fr.talosi.formation.support.web;

import fr.talosi.formation.support.model.TicketAnalysis;
import fr.talosi.formation.support.service.TicketAnalysisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketAnalysisController {

    private final TicketAnalysisService ticketAnalysis;

    public TicketAnalysisController(TicketAnalysisService ticketAnalysis) {
        this.ticketAnalysis = ticketAnalysis;
    }

    public record AnalyzeTicket(
            @NotBlank @Size(max = 3000) String ticketText
    ) {
    }

    @PostMapping({"/tickets/analyze", "/api/tickets/analyze"})
    public TicketAnalysis analyze(@Valid @RequestBody AnalyzeTicket request) {
        return ticketAnalysis.analyze(request.ticketText());
    }
}
