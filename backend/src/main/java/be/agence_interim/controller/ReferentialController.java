package be.agence_interim.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.DegreeOptionResponse;
import be.agence_interim.dto.LanguageOptionResponse;
import be.agence_interim.dto.SkillOptionResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.DegreeService;
import be.agence_interim.service.LanguageService;
import be.agence_interim.service.SkillService;

/** Listes de référence servant à alimenter les sélecteurs du profil. */
@RestController
@RequestMapping("/api")
public class ReferentialController {

    private final SkillService skillService;
    private final DegreeService degreeService;
    private final LanguageService languageService;

    public ReferentialController(
            SkillService skillService, DegreeService degreeService, LanguageService languageService) {
        this.skillService = skillService;
        this.degreeService = degreeService;
        this.languageService = languageService;
    }

    @GetMapping("/skills")
    public List<SkillOptionResponse> skills(@AuthenticationPrincipal Jwt jwt) {
        return skillService.available(CurrentUser.id(jwt))
                .stream().map(SkillOptionResponse::fromEntity).toList();
    }

    @GetMapping("/degrees")
    public List<DegreeOptionResponse> degrees(@AuthenticationPrincipal Jwt jwt) {
        return degreeService.available(CurrentUser.id(jwt))
                .stream().map(DegreeOptionResponse::fromEntity).toList();
    }

    @GetMapping("/languages")
    public List<LanguageOptionResponse> languages() {
        return languageService.available()
                .stream().map(LanguageOptionResponse::fromEntity).toList();
    }
}
