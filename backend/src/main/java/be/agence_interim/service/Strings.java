package be.agence_interim.service;

/** Normalisations de chaînes partagées entre services. */
public final class Strings {

    private Strings() {
    }

    /** Chaîne épurée de ses espaces, ou {@code null} si elle est vide ou blanche. */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Email normalisé pour comparaison et stockage : épuré et en minuscules. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
