package be.agence_interim.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.MissionResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.dto.RefuseMissionRequest;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.MissionService;
import jakarta.validation.Valid;

/** Validation des missions par l'agence (routes /api/admin/** = rôle ADMIN). */
@RestController
@RequestMapping("/api/admin/missions")
public class AdminMissionController {

    private final MissionService missionService;

    public AdminMissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    /** Une section de la liste : pending (à valider) ou history. */
    @GetMapping
    public PageResponse<MissionResponse> list(
            @RequestParam(defaultValue = "pending") String group,
            @RequestParam(defaultValue = "0") int page) {
        return missionService.listForAdmin(group, Pages.of(page));
    }

    /** Nombre de missions en attente de validation (chiffre du tableau de bord). */
    @GetMapping("/pending-count")
    public Map<String, Long> pendingCount() {
        return Map.of("count", missionService.pendingCountForAdmin());
    }

    @GetMapping("/{id}")
    public MissionResponse get(@PathVariable int id) {
        return missionService.getForAdmin(id);
    }

    @PostMapping("/{id}/validate")
    public MissionResponse validate(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return missionService.validate(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/refuse")
    public MissionResponse refuse(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody RefuseMissionRequest request) {
        return missionService.refuse(CurrentUser.id(jwt), id, request.reason().trim());
    }
}
