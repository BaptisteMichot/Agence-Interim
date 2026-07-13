package be.agence_interim.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.EmployerAccessService;

/** Gestion du compte de l'utilisateur courant. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final EmployerAccessService employerAccessService;

    public AccountController(EmployerAccessService employerAccessService) {
        this.employerAccessService = employerAccessService;
    }

    /** Suppression du compte (employeur en attente). */
    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        employerAccessService.deleteAccount(CurrentUser.id(jwt));
        return ResponseEntity.noContent().build();
    }
}
