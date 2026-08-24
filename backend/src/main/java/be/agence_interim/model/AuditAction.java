package be.agence_interim.model;

/**
 * Actes que la plateforme consigne.
 *
 * <p>La liste est délibérément courte. Un journal qui enregistre tout n'est pas relu :
 * n'y figurent que les actions engageantes — celles qu'une partie pourrait contester —
 * et celles qui touchent au cycle de vie d'un compte.
 */
public enum AuditAction {

    /** Une partie a signé le contrat d'une mission. */
    CONTRACT_SIGNED,
    /** L'agence a validé une mission. */
    MISSION_VALIDATED,
    /** L'agence a refusé une mission. */
    MISSION_REFUSED,
    /** L'agence a accordé l'accès employeur. */
    EMPLOYER_ACCESS_GRANTED,
    /** L'agence a refusé une demande d'accès employeur. */
    EMPLOYER_ACCESS_REFUSED,
    /** Un utilisateur a changé son mot de passe. */
    PASSWORD_CHANGED,
    /** Un mot de passe a été réinitialisé par code email. */
    PASSWORD_RESET,
    /** Un utilisateur a exporté ses données personnelles. */
    DATA_EXPORTED,
    /** Un compte a été supprimé ou anonymisé. */
    ACCOUNT_CLOSED
}
