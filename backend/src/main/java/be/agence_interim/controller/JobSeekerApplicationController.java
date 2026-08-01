package be.agence_interim.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.MyApplicationResponse;
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
    public List<MyApplicationResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return applicationService.mine(CurrentUser.id(jwt));
    }

    /** Identifiants des offres avec candidature en cours (pour marquer « déjà postulé »). */
    @GetMapping("/offer-ids")
    public List<Integer> appliedOfferIds(@AuthenticationPrincipal Jwt jwt) {
        return applicationService.appliedOfferIds(CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/cancel")
    public MyApplicationResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return applicationService.cancel(CurrentUser.id(jwt), id);
    }
}
