package be.agence_interim.dto;

import java.time.LocalDateTime;

import be.agence_interim.model.AuditEvent;

/** Une ligne du journal d'audit, telle que l'agence la consulte. */
public record AuditEventResponse(
        int id,
        LocalDateTime occurredAt,
        String action,
        int actorId,
        String actorEmail,
        String targetType,
        int targetId,
        String detail) {

    public static AuditEventResponse fromEntity(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getAction().name(),
                event.getActorId(),
                event.getActorEmail(),
                event.getTargetType(),
                event.getTargetId(),
                event.getDetail());
    }
}
