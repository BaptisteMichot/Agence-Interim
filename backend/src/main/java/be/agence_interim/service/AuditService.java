package be.agence_interim.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import be.agence_interim.model.AuditAction;
import be.agence_interim.model.AuditEvent;
import be.agence_interim.repository.AuditEventRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.security.ClientIp;

/**
 * Écriture du journal d'audit.
 *
 * <p><strong>{@code REQUIRES_NEW}.</strong> Chaque trace est écrite dans sa propre
 * transaction. Sans cela, une trace jointe à la transaction métier disparaîtrait avec
 * elle en cas d'annulation — or c'est précisément la tentative avortée qu'on veut
 * parfois retrouver. La contrepartie est acceptée : une trace peut subsister pour une
 * opération finalement annulée, ce qui est le bon sens d'un journal.
 *
 * <p>L'adresse de l'appelant est lue dans la requête en cours, quand il y en a une : les
 * traitements de fond ({@code @Async}, tâches planifiées) n'en ont pas, et la colonne
 * reste alors vide plutôt que de porter une valeur inventée.
 */
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public AuditService(AuditEventRepository auditEventRepository, UserRepository userRepository) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Consigne un acte.
     *
     * @param action     nature de l'acte
     * @param actorId    auteur, ou {@code null} si c'est le système
     * @param targetType nature de l'objet visé (CONTRACT, MISSION, USER…)
     * @param targetId   identifiant de l'objet visé
     * @param detail     précision courte et lisible, ou {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            AuditAction action, Integer actorId, String targetType, Integer targetId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setOccurredAt(LocalDateTime.now());
        event.setAction(action);
        event.setActorId(actorId);
        event.setActorEmail(actorEmail(actorId));
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setIp(currentIp());
        event.setDetail(truncate(detail));
        auditEventRepository.save(event);
    }

    /** Email de l'auteur, recopié dans la trace pour qu'elle survive à son compte. */
    private String actorEmail(Integer actorId) {
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).map(user -> user.getEmail()).orElse(null);
    }

    private String currentIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet
                ? ClientIp.of(servlet.getRequest())
                : null;
    }

    private static String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= AuditEvent.DETAIL_MAX_LENGTH
                ? detail
                : detail.substring(0, AuditEvent.DETAIL_MAX_LENGTH);
    }
}
