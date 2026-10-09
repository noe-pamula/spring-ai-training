package fr.talosi.formation.support.model;

import java.util.List;
import java.util.Objects;

public record TicketAnalysis(
        String summary,
        Priority priority,
        List<String> missingInformation
) {

    public TicketAnalysis {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Le résumé de l'analyse est obligatoire");
        }
        summary = summary.trim();
        priority = Objects.requireNonNull(priority, "La priorité de l'analyse est obligatoire");
        missingInformation = List.copyOf(Objects.requireNonNull(
                missingInformation,
                "La liste des informations manquantes est obligatoire"
        ));
    }
}
