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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.UpdateLanguageLevelRequest;
import be.agence_interim.dto.UserLanguageRequest;
import be.agence_interim.dto.UserLanguageResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.LanguageService;
import jakarta.validation.Valid;

/** Langues du profil de l'utilisateur authentifié (choisies dans la liste fixe). */
@RestController
@RequestMapping("/api/profile/languages")
public class ProfileLanguageController {

    private final LanguageService languageService;

    public ProfileLanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping
    public List<UserLanguageResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return languageService.userLanguages(CurrentUser.id(jwt))
                .stream().map(UserLanguageResponse::fromEntity).toList();
    }

    @PostMapping
    public ResponseEntity<UserLanguageResponse> add(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserLanguageRequest request) {
        UserLanguageResponse body = UserLanguageResponse.fromEntity(
                languageService.add(CurrentUser.id(jwt), request.languageId(), request.level()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{languageId}")
    public UserLanguageResponse updateLevel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int languageId,
            @Valid @RequestBody UpdateLanguageLevelRequest request) {
        return UserLanguageResponse.fromEntity(
                languageService.updateLevel(CurrentUser.id(jwt), languageId, request.level()));
    }

    @DeleteMapping("/{languageId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable int languageId) {
        languageService.remove(CurrentUser.id(jwt), languageId);
        return ResponseEntity.noContent().build();
    }
}
