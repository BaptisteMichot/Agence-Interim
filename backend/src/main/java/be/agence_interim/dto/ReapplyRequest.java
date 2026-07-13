package be.agence_interim.dto;

import static be.agence_interim.model.EmployerAccessRequest.MESSAGE_MAX_LENGTH;

import jakarta.validation.constraints.Size;

/** Nouvelle demande d'accès employeur après refus, avec un message justificatif facultatif. */
public record ReapplyRequest(
        @Size(max = MESSAGE_MAX_LENGTH, message = "Le message ne peut pas depasser {max} caracteres.") String message) {
}
