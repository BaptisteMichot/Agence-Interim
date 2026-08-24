package be.agence_interim.dto;

import java.math.BigDecimal;

import be.agence_interim.model.Province;
import be.agence_interim.model.Sector;

/**
 * Critères de recherche d'offres saisis par l'intérimaire.
 *
 * <p>Un champ nul signifie « pas de filtre sur ce critère » : la requête compare
 * chaque paramètre à {@code null} avant de l'appliquer, ce qui permet à une seule
 * requête de couvrir toutes les combinaisons de critères.
 */
public record OfferFilter(
        String keyword,
        Sector sector,
        Province province,
        BigDecimal minHourlyWage,
        Integer maxExperienceYears,
        boolean noVehicleRequired) {

    /** Longueur au-delà de laquelle un mot-clé n'est plus une recherche. */
    public static final int KEYWORD_MAX_LENGTH = 100;

    /**
     * Caractère d'échappement du motif {@code like}, déclaré côté requête par
     * {@code escape '!'}. Un point d'exclamation plutôt que la barre oblique inverse :
     * celle-ci est déjà le caractère d'échappement des chaînes dans certaines
     * configurations de base, ce qui obligerait à la doubler une fois de plus.
     */
    public static final char LIKE_ESCAPE = '!';

    /**
     * Motif {@code like} du mot-clé, en minuscules et entouré de {@code %}, ou
     * {@code null} si aucun mot-clé n'a été saisi.
     *
     * <p>Les jokers que le mot-clé contient déjà sont neutralisés. Sans cela, chercher
     * {@code 100%} retournait toutes les offres, et une saisie de la forme
     * {@code %_%_%_%_%} forçait un balayage complet de la table à chaque frappe — la
     * recherche étant déclenchée à la volée. Il n'y a jamais eu d'injection possible,
     * le motif restant un paramètre lié ; le joker se glisse dans la valeur, pas dans
     * la requête.
     */
    public String keywordPattern() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String cleaned = keyword.trim().toLowerCase();
        if (cleaned.length() > KEYWORD_MAX_LENGTH) {
            cleaned = cleaned.substring(0, KEYWORD_MAX_LENGTH);
        }
        return "%" + escapeWildcards(cleaned) + "%";
    }

    /** Préfixe les jokers {@code %} et {@code _} — et le caractère d'échappement lui-même. */
    private static String escapeWildcards(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (char character : value.toCharArray()) {
            if (character == '%' || character == '_' || character == LIKE_ESCAPE) {
                escaped.append(LIKE_ESCAPE);
            }
            escaped.append(character);
        }
        return escaped.toString();
    }
}
