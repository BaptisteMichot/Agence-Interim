package be.agence_interim.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.MissionRequest;
import be.agence_interim.dto.MissionResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.MissionService;
import jakarta.validation.Valid;

/** Missions proposées par l'employeur authentifié (routes /api/employer/** = rôle EMPLOYER). */
@RestController
@RequestMapping("/api/employer")
public class EmployerMissionController {

    private final MissionService missionService;

    public EmployerMissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    /** Sélectionne le candidat d'une candidature et crée la mission provisoire. */
    @PostMapping("/applications/{applicationId}/mission")
    public ResponseEntity<MissionResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int applicationId,
            @Valid @RequestBody MissionRequest request) {
        MissionResponse mission = missionService.create(CurrentUser.id(jwt), applicationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mission);
    }

    @GetMapping("/missions")
    public List<MissionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return missionService.listForEmployer(CurrentUser.id(jwt));
    }

    @GetMapping("/missions/{id}")
    public MissionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return missionService.getForEmployer(CurrentUser.id(jwt), id);
    }

    /** Corrige une mission refusée par l'agence et la soumet à nouveau. */
    @PutMapping("/missions/{id}")
    public MissionResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody MissionRequest request) {
        return missionService.update(CurrentUser.id(jwt), id, request);
    }

    /** Demande le renouvellement d'une mission confirmée : l'intérimaire répond en premier. */
    @PostMapping("/missions/{id}/renew")
    public ResponseEntity<MissionResponse> renew(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody MissionRequest request) {
        MissionResponse renewal = missionService.renew(CurrentUser.id(jwt), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(renewal);
    }
}
