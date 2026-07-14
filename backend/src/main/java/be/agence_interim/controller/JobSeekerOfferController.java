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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.JobOfferResponse;
import be.agence_interim.dto.JobOfferSummaryResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.OfferBrowseService;

/** Consultation des offres et favoris de l'intérimaire (routes /api/offers/** = rôle JOBSEEKER). */
@RestController
@RequestMapping("/api/offers")
public class JobSeekerOfferController {

    private final OfferBrowseService offerBrowseService;

    public JobSeekerOfferController(OfferBrowseService offerBrowseService) {
        this.offerBrowseService = offerBrowseService;
    }

    @GetMapping
    public List<JobOfferSummaryResponse> browse() {
        return offerBrowseService.browseOpen();
    }

    @GetMapping("/favorites")
    public List<JobOfferSummaryResponse> favorites(@AuthenticationPrincipal Jwt jwt) {
        return offerBrowseService.favorites(CurrentUser.id(jwt));
    }

    @GetMapping("/favorites/ids")
    public List<Integer> favoriteIds(@AuthenticationPrincipal Jwt jwt) {
        return offerBrowseService.favoriteIds(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public JobOfferResponse detail(@PathVariable int id) {
        return offerBrowseService.detail(id);
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
}
