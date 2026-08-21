package be.agence_interim.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.AdminEmployerRequestResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.service.EmployerAccessService;

/** Traitement des demandes d'accès employeur par l'administrateur (routes /api/admin/** = rôle ADMIN). */
@RestController
@RequestMapping("/api/admin/employer-requests")
public class AdminEmployerRequestController {

    private final EmployerAccessService employerAccessService;

    public AdminEmployerRequestController(EmployerAccessService employerAccessService) {
        this.employerAccessService = employerAccessService;
    }

    /**
     * Une section de la liste : pending (en attente) ou history.
     * {@code resubmission} = l'utilisateur avait déjà déposé une demande auparavant.
     */
    @GetMapping
    public PageResponse<AdminEmployerRequestResponse> list(
            @RequestParam(defaultValue = "pending") String group,
            @RequestParam(defaultValue = "0") int page) {
        return employerAccessService.list(group, Pages.of(page));
    }

    /** Nombre de demandes en attente (chiffre du tableau de bord). */
    @GetMapping("/pending-count")
    public Map<String, Long> pendingCount() {
        return Map.of("count", employerAccessService.pendingCount());
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable int id) {
        employerAccessService.accept(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refuse")
    public ResponseEntity<Void> refuse(@PathVariable int id, @RequestParam(defaultValue = "false") boolean block) {
        employerAccessService.refuse(id, block);
        return ResponseEntity.noContent().build();
    }
}
