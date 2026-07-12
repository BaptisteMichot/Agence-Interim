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

import be.agence_interim.dto.ExperienceRequest;
import be.agence_interim.dto.ExperienceResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.ExperienceService;
import jakarta.validation.Valid;

/** Expériences professionnelles de l'utilisateur authentifié. */
@RestController
@RequestMapping("/api/profile/experiences")
public class ExperienceController {

    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping
    public List<ExperienceResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return experienceService.list(CurrentUser.id(jwt))
                .stream()
                .map(ExperienceResponse::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ExperienceResponse> add(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExperienceRequest request) {
        ExperienceResponse body = ExperienceResponse.fromEntity(
                experienceService.add(CurrentUser.id(jwt), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    public ExperienceResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody ExperienceRequest request) {
        return ExperienceResponse.fromEntity(
                experienceService.update(CurrentUser.id(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        experienceService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
