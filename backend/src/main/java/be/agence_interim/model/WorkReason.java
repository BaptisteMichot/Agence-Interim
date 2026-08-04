package be.agence_interim.model;

/** Motif du recours au travail intérimaire, repris sur le contrat. */
public enum WorkReason {
    /** Remplacement d'un travailleur permanent. */
    REPLACEMENT,
    /** Surcroît temporaire de travail. */
    OVERLOAD,
    /** Travail exceptionnel. */
    EXCEPTION
}
