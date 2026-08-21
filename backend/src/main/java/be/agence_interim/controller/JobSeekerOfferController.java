package be.agence_interim.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.JobOfferResponse;
import be.agence_interim.dto.JobOfferSummaryResponse;
import be.agence_interim.dto.MatchingOfferResponse;
import be.agence_interim.dto.MyApplicationResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.ApplicationService;
import be.agence_interim.service.OfferBrowseService;

/** Consultation des offres, favoris et candidature de l'intérimaire (routes /api/offers/** = rôle JOBSEEKER). */
@RestController
@RequestMapping("/api/offers")
public class JobSeekerOfferController {

    private final OfferBrowseService offerBrowseService;
    private final ApplicationService applicationService;

    public JobSeekerOfferController(
            OfferBrowseService offerBrowseService, ApplicationService applicationService) {
        this.offerBrowseService = offerBrowseService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public PageResponse<JobOfferSummaryResponse> browse(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return offerBrowseService.browseOpen(CurrentUser.id(jwt), Pages.of(page));
    }

    /** Offres correspondant au profil (obligatoires satisfaits), triées par score décroissant. */
    @GetMapping("/matching")
    public PageResponse<MatchingOfferResponse> matching(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return offerBrowseService.matching(CurrentUser.id(jwt), Math.max(page, 0), Pages.PAGE_SIZE);
    }

    @GetMapping("/favorites")
    public PageResponse<JobOfferSummaryResponse> favorites(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return offerBrowseService.favorites(CurrentUser.id(jwt), Pages.of(page));
    }

    @GetMapping("/{id}")
    public JobOfferResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return offerBrowseService.detail(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        offerBrowseService.addFavorite(CurrentUser.id(jwt), id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        offerBrowseService.removeFavorite(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<MyApplicationResponse> apply(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        MyApplicationResponse body = applicationService.apply(CurrentUser.id(jwt), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
