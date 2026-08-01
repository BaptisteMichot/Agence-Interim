package be.agence_interim.model;

/** Statut d'une candidature (VARCHAR(8) : chaque nom fait au plus 8 caractères). */
public enum ApplicationStatus {
    /** En attente de traitement par l'employeur. */
    PENDING,
    /** Annulée par l'intérimaire. */
    CANCELED
}
