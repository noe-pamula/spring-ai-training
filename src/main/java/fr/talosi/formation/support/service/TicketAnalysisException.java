package fr.talosi.formation.support.service;

public class TicketAnalysisException extends RuntimeException {

    public TicketAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public TicketAnalysisException(String message) {
        super(message);
    }
}
