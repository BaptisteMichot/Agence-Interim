package be.agence_interim.model;

/**
 * Statut d'une demande d'accès employeur. Codes courts (colonne VARCHAR(8)).
 * Libellés d'affichage côté frontend : PENDING → « En attente », ACCEPTED → « Acceptée »,
 * REFUSED → « Refusée ».
 */
public enum EmployerAccessStatus {
    PENDING,
    ACCEPTED,
    REFUSED
}
