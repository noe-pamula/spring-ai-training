package fr.talosi.formation.support.model;

import java.time.Instant;

public record Ticket(
        long id,
        String title,
        String description,
        String requester,
        Priority priority,
        Status status,
        Instant createdAt
) {

    public enum Priority {
        LOW,
        NORMAL,
        HIGH
    }

    public enum Status {
        OPEN,
        IN_PROGRESS,
        RESOLVED
    }

    public Ticket withStatus(Status next) {
        return new Ticket(id, title, description, requester, priority, next, createdAt);
    }
}
