package be.agence_interim.dto;

import static be.agence_interim.model.JobOffer.CITY_MAX_LENGTH;
import static be.agence_interim.model.JobOffer.TITLE_MAX_LENGTH;

import java.math.BigDecimal;
import java.util.List;

import be.agence_interim.model.JobOffer;
import be.agence_interim.model.Province;
import be.agence_interim.model.Sector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Création ou mise à jour d'une offre d'emploi, avec ses exigences. */
public record JobOfferRequest(
        @NotBlank(message = "Le titre est obligatoire.") @Size(max = TITLE_MAX_LENGTH, message = "Le titre ne peut pas dépasser {max} caractères.") String title,
        @NotNull(message = "Le secteur est obligatoire.") Sector sector,
        @NotBlank(message = "La ville est obligatoire.") @Size(max = CITY_MAX_LENGTH, message = "La ville ne peut pas dépasser {max} caractères.") String city,
        @NotNull(message = "La province est obligatoire.") Province province,
        @NotBlank(message = "La description est obligatoire.") @Size(max = JobOffer.DESCRIPTION_MAX_LENGTH, message = "La description ne peut pas dépasser {max} caractères.") String description,
        @PositiveOrZero(message = "Le salaire minimum doit être positif.") BigDecimal salaryMin,
        @PositiveOrZero(message = "Le salaire maximum doit être positif.") BigDecimal salaryMax,
        @Pattern(regexp = "\\d{1,2}", message = "L'experience requise doit être un nombre d'annees (ex. 2).") String experienceTime,
        Boolean vehicleMandatory,
        /** Valeur faciale du titre-repas par jour presté ; null = pas de chèques-repas. */
        @DecimalMin(value = JobOffer.MEAL_VOUCHER_MIN_AMOUNT, message = "Le montant du chèque-repas doit être supérieur à 0.") @DecimalMax(value = JobOffer.MEAL_VOUCHER_MAX_AMOUNT, message = "Le chèque-repas ne peut pas dépasser {value} € par jour (maximum légal).") BigDecimal mealVoucherAmount,
        @Valid List<OfferSkillRequirement> skills,
        @Valid List<OfferDegreeRequirement> degrees,
        @Valid List<OfferLanguageRequirement> languages) {
}
