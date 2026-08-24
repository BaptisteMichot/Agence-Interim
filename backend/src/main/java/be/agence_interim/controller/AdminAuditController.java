package be.agence_interim.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.jspecify.annotations.Nullable;

import be.agence_interim.dto.AuditEventResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.model.AuditAction;
import be.agence_interim.repository.AuditEventRepository;

/**
 * Consultation du journal d'audit par l'agence (routes /api/admin/** = rôle ADMIN).
 *
 * <p>Lecture seule, et il n'existe aucune route d'écriture ni de suppression : un
 * journal que son lecteur peut retoucher ne prouve rien.
 */
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AuditEventRepository auditEventRepository;

    public AdminAuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /** Une page du journal, du plus récent au plus ancien, filtrable par type d'acte. */
    @GetMapping
    public PageResponse<AuditEventResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) @Nullable String action) {
        AuditAction filter = parse(action);
        return PageResponse.of(
                filter == null
                        ? auditEventRepository.findByOrderByOccurredAtDescIdDesc(Pages.of(page))
                        : auditEventRepository.findByActionOrderByOccurredAtDescIdDesc(
                                filter, Pages.of(page)),
                AuditEventResponse::fromEntity);
    }

    /** Un filtre inconnu vaut absence de filtre, plutôt qu'une erreur 500. */
    private static AuditAction parse(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        try {
            return AuditAction.valueOf(action);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
