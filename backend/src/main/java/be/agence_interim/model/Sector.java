package be.agence_interim.model;

/**
 * Secteur d'activité d'une offre d'emploi.
 *
 * <p>Liste fermée : le secteur sert de critère de recherche, et une saisie libre en
 * produirait autant de valeurs distinctes que d'orthographes (« Horeca », « horeca »,
 * « HoReCa »), rendant tout filtre inutilisable. La colonne reste un
 * {@code VARCHAR(20)} : seules les valeurs qu'elle accepte changent.
 */
public enum Sector {
    CONSTRUCTION,
    LOGISTIQUE,
    TRANSPORT,
    INDUSTRIE,
    HORECA,
    COMMERCE,
    NETTOYAGE,
    SANTE,
    ADMINISTRATION,
    INFORMATIQUE,
    ENSEIGNEMENT,
    AGRICULTURE,
    AUTRE
}
