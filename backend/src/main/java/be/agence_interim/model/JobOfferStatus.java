package be.agence_interim.model;

/**
 * Statut d'une offre d'emploi. Codes courts (colonne VARCHAR(8)).
 * Libellés d'affichage côté frontend : OPEN → « Ouverte », CLOSED → « Clôturée ».
 */
public enum JobOfferStatus {
    OPEN,
    CLOSED
}
