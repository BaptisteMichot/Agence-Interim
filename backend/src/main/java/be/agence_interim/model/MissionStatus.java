package be.agence_interim.model;

/**
 * Cycle de vie d'une mission d'intérim :
 * l'employeur crée une mission provisoire ({@code PENDING}), l'agence la valide
 * ({@code APPROVED}) ou la refuse avec un motif ({@code REFUSED}), puis l'intérimaire
 * l'accepte ({@code ACTIVE} : contrat généré et mission au planning) ou la refuse
 * ({@code DECLINED}). {@code RENEWAL} est une demande de renouvellement en attente
 * de la réponse de l'intérimaire.
 */
public enum MissionStatus {
    PENDING,
    REFUSED,
    APPROVED,
    RENEWAL,
    ACTIVE,
    DECLINED
}
