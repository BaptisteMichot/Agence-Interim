package be.agence_interim.dto;

import static be.agence_interim.model.JobOffer.CITY_MAX_LENGTH;
import static be.agence_interim.model.JobOffer.SECTOR_MAX_LENGTH;
import static be.agence_interim.model.JobOffer.TITLE_MAX_LENGTH;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Création ou mise à jour d'une offre d'emploi, avec ses exigences. */
public record JobOfferRequest(
        @NotBlank(message = "Le titre est obligatoire.") @Size(max = TITLE_MAX_LENGTH, message = "Le titre ne peut pas dépasser {max} caractères.") String title,
        @NotBlank(message = "Le secteur est obligatoire.") @Size(max = SECTOR_MAX_LENGTH, message = "Le secteur ne peut pas dépasser {max} caractères.") String sector,
        @NotBlank(message = "La ville est obligatoire.") @Size(max = CITY_MAX_LENGTH, message = "La ville ne peut pas dépasser {max} caractères.") String city,
        @NotBlank(message = "La description est obligatoire.") String description,
        @PositiveOrZero(message = "Le salaire minimum doit être positif.") BigDecimal salaryMin,
        @PositiveOrZero(message = "Le salaire maximum doit être positif.") BigDecimal salaryMax,
        @Pattern(regexp = "\\d{1,2}", message = "L'experience requise doit être un nombre d'annees (ex. 2).") String experienceTime,
        Boolean vehicleMandatory,
        @Valid List<OfferSkillRequirement> skills,
        @Valid List<OfferDegreeRequirement> degrees,
        @Valid List<OfferLanguageRequirement> languages) {
}
