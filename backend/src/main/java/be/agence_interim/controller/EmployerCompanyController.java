package be.agence_interim.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.EmployerCompanyRequest;
import be.agence_interim.dto.EmployerCompanyResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.EmployerAccessService;
import be.agence_interim.service.ProfileService;
import jakarta.validation.Valid;

/**
 * Fiche entreprise de l'employeur connecté. Ces mentions sont obligatoires sur le
 * contrat de mission, d'où la possibilité de les corriger après l'inscription.
 */
@RestController
@RequestMapping("/api/employer/company")
public class EmployerCompanyController {

    private final EmployerAccessService employerAccessService;
    private final ProfileService profileService;

    public EmployerCompanyController(
            EmployerAccessService employerAccessService, ProfileService profileService) {
        this.employerAccessService = employerAccessService;
        this.profileService = profileService;
    }

    @GetMapping
    public EmployerCompanyResponse myCompany(@AuthenticationPrincipal Jwt jwt) {
        return EmployerCompanyResponse.fromEntity(profileService.getUser(CurrentUser.id(jwt)));
    }

    @PutMapping
    public EmployerCompanyResponse update(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EmployerCompanyRequest request) {
        return EmployerCompanyResponse.fromEntity(
                employerAccessService.updateCompany(CurrentUser.id(jwt), request));
    }
}
