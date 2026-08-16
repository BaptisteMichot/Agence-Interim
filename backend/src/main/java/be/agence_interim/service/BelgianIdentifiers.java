package be.agence_interim.service;

/**
 * Contrôle des identifiants belges figurant sur le contrat. Les deux numéros portent
 * une clé de contrôle modulo 97 : la vérifier évite qu'une faute de frappe se retrouve
 * sur un document contractuel.
 */
public final class BelgianIdentifiers {

    private BelgianIdentifiers() {
    }

    /** Ne conserve que les chiffres (les séparateurs de saisie sont libres). */
    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    /**
     * Numéro de registre national : 11 chiffres, les 9 premiers formant un nombre dont
     * le reste modulo 97 doit compléter les 2 derniers à 97. Les personnes nées à partir
     * de 2000 sont reconnues par un « 2 » placé devant le nombre de base.
     */
    public static boolean isValidNationalNumber(String value) {
        String digits = digits(value);
        if (digits.length() != 11) {
            return false;
        }
        long base = Long.parseLong(digits.substring(0, 9));
        int key = Integer.parseInt(digits.substring(9));
        return 97 - (base % 97) == key || 97 - ((2_000_000_000L + base) % 97) == key;
    }

    /**
     * Numéro d'entreprise (BCE) : 10 chiffres commençant par 0 ou 1, dont les 2 derniers
     * complètent à 97 le reste modulo 97 des 8 premiers.
     */
    public static boolean isValidCompanyNumber(String value) {
        String digits = digits(value);
        if (digits.length() != 10 || (digits.charAt(0) != '0' && digits.charAt(0) != '1')) {
            return false;
        }
        long base = Long.parseLong(digits.substring(0, 8));
        int key = Integer.parseInt(digits.substring(8));
        return 97 - (base % 97) == key;
    }

    /** « 98.03.12-123.45 » à partir des chiffres saisis. */
    public static String formatNationalNumber(String value) {
        String d = digits(value);
        return d.length() != 11 ? value
                : d.substring(0, 2) + "." + d.substring(2, 4) + "." + d.substring(4, 6)
                        + "-" + d.substring(6, 9) + "." + d.substring(9);
    }

    /** « 0123.456.789 » à partir des chiffres saisis. */
    public static String formatCompanyNumber(String value) {
        String d = digits(value);
        return d.length() != 10 ? value
                : d.substring(0, 4) + "." + d.substring(4, 7) + "." + d.substring(7);
    }
}
