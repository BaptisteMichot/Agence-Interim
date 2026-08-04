package be.agence_interim.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.UnavailabilityRequest;
import be.agence_interim.dto.UnavailabilityResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.AvailabilityService;
import jakarta.validation.Valid;

/**
 * Disponibilités de l'intérimaire authentifié
 * (routes /api/profile/** = rôle JOBSEEKER).
 */
@RestController
@RequestMapping("/api/profile/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public List<UnavailabilityResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilityService.list(CurrentUser.id(jwt), from, to);
    }

    /** Première date modifiable, pour que le frontend grise les jours verrouillés. */
    @GetMapping("/editable-from")
    public Map<String, LocalDate> editableFrom() {
        return Map.of("editableFrom", AvailabilityService.editableFrom());
    }

    @PostMapping
    public ResponseEntity<UnavailabilityResponse> add(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UnavailabilityRequest request) {
        UnavailabilityResponse created = availabilityService.add(CurrentUser.id(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        availabilityService.remove(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
