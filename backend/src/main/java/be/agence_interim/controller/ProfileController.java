package be.agence_interim.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.ProfileResponse;
import be.agence_interim.dto.UpdateProfileRequest;
import be.agence_interim.model.User;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.ExperienceService;
import be.agence_interim.service.FormationService;
import be.agence_interim.service.ProfileService;
import jakarta.validation.Valid;

/** Profil de l'utilisateur authentifié : champs de base + expériences + formations. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ExperienceService experienceService;
    private final FormationService formationService;

    public ProfileController(
            ProfileService profileService,
            ExperienceService experienceService,
            FormationService formationService) {
        this.profileService = profileService;
        this.experienceService = experienceService;
        this.formationService = formationService;
    }

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        int userId = CurrentUser.id(jwt);
        return ProfileResponse.of(
                profileService.getUser(userId),
                experienceService.list(userId),
                formationService.list(userId));
    }

    @PutMapping
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        int userId = CurrentUser.id(jwt);
        User updated = profileService.updateBase(userId, request);
        return ProfileResponse.of(
                updated,
                experienceService.list(userId),
                formationService.list(userId));
    }
}
