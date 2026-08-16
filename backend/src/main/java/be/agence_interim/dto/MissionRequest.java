package be.agence_interim.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import be.agence_interim.model.Mission;
import be.agence_interim.model.WorkReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Conditions de la mission saisies par l'employeur ; elles sont reprises dans le contrat. */
public record MissionRequest(
        @NotNull(message = "La date de début est obligatoire.") LocalDate startDate,
        @NotNull(message = "La date de fin est obligatoire.") LocalDate endDate,
        @NotBlank(message = "L'intitulé du poste est obligatoire.")
        @Size(max = Mission.POSITION_MAX_LENGTH, message = "L'intitulé du poste est trop long.")
        String position,
        @NotBlank(message = "L'adresse du lieu de travail est obligatoire.")
        @Size(max = Mission.WORKPLACE_MAX_LENGTH, message = "L'adresse du lieu de travail est trop longue.")
        String workplace,
        @NotBlank(message = "La description du poste est obligatoire.")
        @Size(max = Mission.DESCRIPTION_MAX_LENGTH, message = "La description du poste est trop longue.")
        String description,
        @NotNull(message = "Le salaire horaire est obligatoire.")
        @DecimalMin(value = "0.01", message = "Le salaire horaire doit être supérieur à 0.")
        BigDecimal hourlyWage,
        @NotNull(message = "Le motif de recours est obligatoire.") WorkReason workReason,
        @Size(max = Mission.REPLACED_WORKER_MAX_LENGTH, message = "Le nom du travailleur remplacé est trop long.")
        String replacedWorker,
        String notes,
        @NotEmpty(message = "La mission doit comporter au moins une journée de travail.")
        @Valid List<DailySlotRequest> slots) {
}
