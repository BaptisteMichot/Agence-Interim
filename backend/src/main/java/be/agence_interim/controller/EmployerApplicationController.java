package be.agence_interim.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.CandidateProfileResponse;
import be.agence_interim.dto.OfferApplicationResponse;
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

    @GetMapping("/offers/{offerId}/applications")
    public List<OfferApplicationResponse> listForOffer(
            @AuthenticationPrincipal Jwt jwt, @PathVariable int offerId) {
        return employerApplicationService.listForOffer(CurrentUser.id(jwt), offerId);
    }

    /** Nombre de candidatures en cours par offre (offres sans candidature absentes). */
    @GetMapping("/offers/application-counts")
    public Map<Integer, Long> applicationCounts(@AuthenticationPrincipal Jwt jwt) {
        return employerApplicationService.countsByOffer(CurrentUser.id(jwt));
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
