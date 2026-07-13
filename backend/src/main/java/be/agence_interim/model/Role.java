package be.agence_interim.model;

/**
 * Rôles des utilisateurs. {@code EMPLOYER_PENDING} est un état transitoire : l'employeur
 * l'a le temps que sa demande d'accès soit validée, puis passe à {@code EMPLOYER}.
 */
public enum Role {
    ADMIN,
    INTERIM_RECRUITER,
    EMPLOYER,
    EMPLOYER_PENDING,
    JOBSEEKER
}
