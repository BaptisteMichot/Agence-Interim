package be.agence_interim.dto;

import static be.agence_interim.model.Experience.COMPANY_NAME_MAX_LENGTH;
import static be.agence_interim.model.Experience.POSITION_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Création ou mise à jour d'une expérience professionnelle. */
public record ExperienceRequest(
        @NotBlank(message = "Le nom de l'entreprise est obligatoire.") @Size(max = COMPANY_NAME_MAX_LENGTH, message = "Le nom de l'entreprise ne peut pas depasser {max} caracteres.") String companyName,
        @NotBlank(message = "Le poste est obligatoire.") @Size(max = POSITION_MAX_LENGTH, message = "Le poste ne peut pas depasser {max} caracteres.") String position,
        @NotNull(message = "La date de debut est obligatoire.") LocalDate startDate,
        LocalDate endDate) {
}
