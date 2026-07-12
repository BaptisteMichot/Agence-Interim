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

import be.agence_interim.dto.UpdateDegreeRequest;
import be.agence_interim.dto.UserDegreeRequest;
import be.agence_interim.dto.UserDegreeResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.DegreeService;
import jakarta.validation.Valid;

/** Diplômes du profil de l'utilisateur authentifié. */
@RestController
@RequestMapping("/api/profile/degrees")
public class ProfileDegreeController {

    private final DegreeService degreeService;

    public ProfileDegreeController(DegreeService degreeService) {
        this.degreeService = degreeService;
    }

    @GetMapping
    public List<UserDegreeResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return degreeService.userDegrees(CurrentUser.id(jwt))
                .stream().map(UserDegreeResponse::fromEntity).toList();
    }

    @PostMapping
    public ResponseEntity<UserDegreeResponse> add(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserDegreeRequest request) {
        UserDegreeResponse body = UserDegreeResponse.fromEntity(degreeService.add(
                CurrentUser.id(jwt),
                request.degreeId(),
                request.type(),
                request.section(),
                request.institution(),
                request.graduationYear()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{degreeId}")
    public UserDegreeResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int degreeId,
            @Valid @RequestBody UpdateDegreeRequest request) {
        return UserDegreeResponse.fromEntity(degreeService.update(
                CurrentUser.id(jwt), degreeId, request.institution(), request.graduationYear()));
    }

    @DeleteMapping("/{degreeId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable int degreeId) {
        degreeService.remove(CurrentUser.id(jwt), degreeId);
        return ResponseEntity.noContent().build();
    }
}
