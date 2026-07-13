package be.agence_interim.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.MessageResponse;
import be.agence_interim.dto.MyEmployerRequestResponse;
import be.agence_interim.dto.ReapplyRequest;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.EmployerAccessService;
import jakarta.validation.Valid;

/** Suivi et re-soumission de la demande d'accès employeur par l'utilisateur courant. */
@RestController
@RequestMapping("/api/employer-requests")
public class EmployerRequestController {

    private final EmployerAccessService employerAccessService;

    public EmployerRequestController(EmployerAccessService employerAccessService) {
        this.employerAccessService = employerAccessService;
    }

    @GetMapping("/me")
    public MyEmployerRequestResponse myRequest(@AuthenticationPrincipal Jwt jwt) {
        return new MyEmployerRequestResponse(employerAccessService.latestStatus(CurrentUser.id(jwt)));
    }

    /** Nouvelle demande après un refus (message justificatif facultatif). */
    @PostMapping
    public ResponseEntity<MessageResponse> reapply(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReapplyRequest request) {
        employerAccessService.reapply(CurrentUser.id(jwt), request.message());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("Votre nouvelle demande a ete envoyee."));
    }
}
