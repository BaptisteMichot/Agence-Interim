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

import be.agence_interim.dto.JobOfferRequest;
import be.agence_interim.dto.JobOfferResponse;
import be.agence_interim.dto.JobOfferSummaryResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.JobOfferService;
import be.agence_interim.service.MatchNotificationService;
import jakarta.validation.Valid;

/** Offres d'emploi de l'employeur authentifié (routes /api/employer/** = rôle EMPLOYER). */
@RestController
@RequestMapping("/api/employer/offers")
public class EmployerOfferController {

    private final JobOfferService jobOfferService;
    private final MatchNotificationService matchNotificationService;

    public EmployerOfferController(
            JobOfferService jobOfferService, MatchNotificationService matchNotificationService) {
        this.jobOfferService = jobOfferService;
        this.matchNotificationService = matchNotificationService;
    }

    @GetMapping
    public List<JobOfferSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return jobOfferService.listMine(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public JobOfferResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return jobOfferService.getMine(CurrentUser.id(jwt), id);
    }

    @PostMapping
    public ResponseEntity<JobOfferResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JobOfferRequest request) {
        JobOfferResponse body = jobOfferService.create(CurrentUser.id(jwt), request);
        // Après le commit : contact automatique (asynchrone) des candidats correspondants.
        matchNotificationService.notifyMatchingJobSeekers(body.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    public JobOfferResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody JobOfferRequest request) {
        return jobOfferService.update(CurrentUser.id(jwt), id, request);
    }

    @PostMapping("/{id}/close")
    public JobOfferResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return jobOfferService.close(CurrentUser.id(jwt), id);
    }
}
