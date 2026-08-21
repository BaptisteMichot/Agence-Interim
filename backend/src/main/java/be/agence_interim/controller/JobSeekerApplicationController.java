package be.agence_interim.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.MyApplicationResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.ApplicationService;

/** Suivi des candidatures de l'intérimaire (routes /api/applications/** = rôle JOBSEEKER). */
@RestController
@RequestMapping("/api/applications")
public class JobSeekerApplicationController {

    private final ApplicationService applicationService;

    public JobSeekerApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public PageResponse<MyApplicationResponse> mine(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return applicationService.mine(CurrentUser.id(jwt), Pages.of(page));
    }

    /** Nombre de candidatures en cours (chiffre du tableau de bord). */
    @GetMapping("/pending-count")
    public Map<String, Long> pendingCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", applicationService.pendingCount(CurrentUser.id(jwt)));
    }

    @PostMapping("/{id}/cancel")
    public MyApplicationResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return applicationService.cancel(CurrentUser.id(jwt), id);
    }
}
