package be.agence_interim.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.AdminEmployerRequestResponse;
import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.service.EmployerAccessService;

/** Traitement des demandes d'accès employeur par l'administrateur (routes /api/admin/** = rôle ADMIN). */
@RestController
@RequestMapping("/api/admin/employer-requests")
public class AdminEmployerRequestController {

    private final EmployerAccessService employerAccessService;

    public AdminEmployerRequestController(EmployerAccessService employerAccessService) {
        this.employerAccessService = employerAccessService;
    }

    /** Toutes les demandes (en attente + historique). {@code resubmission} = l'utilisateur avait déjà une demande antérieure. */
    @GetMapping
    public List<AdminEmployerRequestResponse> list() {
        List<AdminEmployerRequestResponse> result = new ArrayList<>();
        Set<Integer> seenUsers = new HashSet<>();
        for (EmployerAccessRequest request : employerAccessService.listAll()) {
            int userId = request.getUser().getId();
            boolean resubmission = seenUsers.contains(userId);
            result.add(AdminEmployerRequestResponse.fromEntity(request, resubmission));
            seenUsers.add(userId);
        }
        return result;
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable int id) {
        employerAccessService.accept(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refuse")
    public ResponseEntity<Void> refuse(@PathVariable int id) {
        employerAccessService.refuse(id);
        return ResponseEntity.noContent().build();
    }
}
