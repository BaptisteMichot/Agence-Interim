package be.agence_interim.model;

/**
 * Province où se situe le poste, Bruxelles-Capitale comprise.
 *
 * <p>La ville seule ne permet pas d'élargir une recherche : deux offres voisines
 * n'ont aucune valeur en commun tant qu'elles ne sont pas dans la même commune.
 * La province donne la maille intermédiaire attendue d'une recherche d'emploi.
 */
public enum Province {
    BRUXELLES,
    BRABANT_WALLON,
    HAINAUT,
    LIEGE,
    LUXEMBOURG,
    NAMUR,
    ANVERS,
    BRABANT_FLAMAND,
    FLANDRE_OCCIDENTALE,
    FLANDRE_ORIENTALE,
    LIMBOURG
}
