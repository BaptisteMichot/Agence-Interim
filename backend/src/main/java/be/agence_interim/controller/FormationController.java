package be.agence_interim.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.FormationRequest;
import be.agence_interim.dto.FormationResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.FormationService;
import jakarta.validation.Valid;

/** Formations de l'utilisateur authentifié. */
@RestController
@RequestMapping("/api/profile/formations")
public class FormationController {

    private final FormationService formationService;

    public FormationController(FormationService formationService) {
        this.formationService = formationService;
    }

    @GetMapping
    public List<FormationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return formationService.list(CurrentUser.id(jwt))
                .stream()
                .map(FormationResponse::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<FormationResponse> add(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FormationRequest request) {
        FormationResponse body = FormationResponse.fromEntity(
                formationService.add(CurrentUser.id(jwt), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    public FormationResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody FormationRequest request) {
        return FormationResponse.fromEntity(
                formationService.update(CurrentUser.id(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        formationService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
