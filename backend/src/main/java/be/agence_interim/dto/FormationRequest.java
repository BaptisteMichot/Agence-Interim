package be.agence_interim.dto;

import static be.agence_interim.model.Formation.INSTITUTION_MAX_LENGTH;
import static be.agence_interim.model.Formation.TITLE_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Création ou mise à jour d'une formation. Le statut n'est pas fourni par le
 * client : il est déduit de la date de fin (absente = en cours, présente = terminé).
 */
public record FormationRequest(
        @NotBlank(message = "Le titre est obligatoire.") @Size(max = TITLE_MAX_LENGTH, message = "Le titre ne peut pas depasser {max} caracteres.") String title,
        @NotBlank(message = "L'etablissement est obligatoire.") @Size(max = INSTITUTION_MAX_LENGTH, message = "L'etablissement ne peut pas depasser {max} caracteres.") String institution,
        @NotNull(message = "La date de debut est obligatoire.") LocalDate startDate,
        LocalDate endDate) {
}
