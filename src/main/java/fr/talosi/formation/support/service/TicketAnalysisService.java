package fr.talosi.formation.support.service;

import fr.talosi.formation.support.model.TicketAnalysis;

public interface TicketAnalysisService {

    TicketAnalysis analyze(String ticketText);
}
