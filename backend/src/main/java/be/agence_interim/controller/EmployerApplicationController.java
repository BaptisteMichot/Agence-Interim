package be.agence_interim.controller;

import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.CandidateProfileResponse;
import be.agence_interim.dto.OfferApplicationResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.dto.RateApplicationRequest;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.EmployerApplicationService;
import jakarta.validation.Valid;

/** Candidatures reçues sur les offres de l'employeur authentifié (routes /api/employer/** = rôle EMPLOYER). */
@RestController
@RequestMapping("/api/employer")
public class EmployerApplicationController {

    private final EmployerApplicationService employerApplicationService;

    public EmployerApplicationController(EmployerApplicationService employerApplicationService) {
        this.employerApplicationService = employerApplicationService;
    }

    /** Une page des candidatures reçues sur une offre. `sort` : date-desc, date-asc ou rating-desc. */
    @GetMapping("/offers/{offerId}/applications")
    public PageResponse<OfferApplicationResponse> listForOffer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int offerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "date-desc") String sort) {
        return employerApplicationService.listForOffer(
                CurrentUser.id(jwt), offerId, sort, Math.max(page, 0), Pages.PAGE_SIZE);
    }

    /** Nombre total de candidatures en cours reçues (chiffre du tableau de bord). */
    @GetMapping("/applications/pending-count")
    public Map<String, Long> pendingCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", employerApplicationService.pendingCount(CurrentUser.id(jwt)));
    }

    @PutMapping("/applications/{id}/rating")
    public OfferApplicationResponse rate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody RateApplicationRequest request) {
        return employerApplicationService.rate(CurrentUser.id(jwt), id, request.rating());
    }

    @GetMapping("/applications/{id}/profile")
    public CandidateProfileResponse candidateProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return employerApplicationService.candidateProfile(CurrentUser.id(jwt), id);
    }

    @GetMapping("/applications/{id}/cv")
    public ResponseEntity<Resource> candidateCv(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        Resource resource = employerApplicationService.candidateCv(CurrentUser.id(jwt), id);
        return PdfResponses.inline(resource, "cv.pdf");
    }
}
