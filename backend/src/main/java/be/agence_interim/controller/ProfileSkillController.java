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

import be.agence_interim.dto.UpdateSkillLevelRequest;
import be.agence_interim.dto.UserSkillRequest;
import be.agence_interim.dto.UserSkillResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.SkillService;
import jakarta.validation.Valid;

/** Compétences du profil de l'utilisateur authentifié. */
@RestController
@RequestMapping("/api/profile/skills")
public class ProfileSkillController {

    private final SkillService skillService;

    public ProfileSkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public List<UserSkillResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return skillService.userSkills(CurrentUser.id(jwt))
                .stream().map(UserSkillResponse::fromEntity).toList();
    }

    @PostMapping
    public ResponseEntity<UserSkillResponse> add(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserSkillRequest request) {
        UserSkillResponse body = UserSkillResponse.fromEntity(
                skillService.add(CurrentUser.id(jwt), request.skillId(), request.name(), request.level()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{skillId}")
    public UserSkillResponse updateLevel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int skillId,
            @Valid @RequestBody UpdateSkillLevelRequest request) {
        return UserSkillResponse.fromEntity(
                skillService.updateLevel(CurrentUser.id(jwt), skillId, request.level()));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable int skillId) {
        skillService.remove(CurrentUser.id(jwt), skillId);
        return ResponseEntity.noContent().build();
    }
}
