package be.agence_interim.dto;

import be.agence_interim.model.EmployerAccessStatus;

/** Statut de la demande d'accès employeur de l'utilisateur courant (null s'il n'en a pas). */
public record MyEmployerRequestResponse(EmployerAccessStatus status) {
}
