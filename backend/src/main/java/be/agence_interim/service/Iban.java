package be.agence_interim.service;

import java.util.Locale;

/**
 * Contrôle du numéro de compte bancaire de l'intérimaire. L'IBAN (norme ISO 13616)
 * porte une clé de contrôle modulo 97 : la vérifier évite qu'une faute de frappe
 * envoie le salaire d'une mission sur un compte inexistant.
 */
public final class Iban {

    /** Longueurs extrêmes d'un IBAN, séparateurs de saisie retirés. */
    private static final int MIN_LENGTH = 15;
    private static final int MAX_LENGTH = 34;

    private Iban() {
    }

    /** Ne conserve que les caractères significatifs, en majuscules (la saisie est libre). */
    private static String compact(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    /**
     * IBAN valide : deux lettres de pays, deux chiffres de clé, puis l'identifiant national.
     * Les quatre premiers caractères passent à la fin, chaque lettre devient son rang dans
     * l'alphabet augmenté de 9 (A = 10), et le nombre obtenu doit valoir 1 modulo 97.
     */
    public static boolean isValid(String value) {
        String iban = compact(value);
        if (!iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{" + (MIN_LENGTH - 4) + "," + (MAX_LENGTH - 4) + "}")) {
            return false;
        }
        return remainder(iban.substring(4) + iban.substring(0, 4)) == 1;
    }

    /**
     * Reste modulo 97 du nombre représenté par la chaîne. Le calcul se fait chiffre par
     * chiffre : le nombre complet dépasse la capacité d'un {@code long}, et une lettre
     * vaut deux chiffres.
     */
    private static int remainder(String rearranged) {
        int remainder = 0;
        for (char character : rearranged.toCharArray()) {
            int digits = Character.isDigit(character) ? character - '0' : character - 'A' + 10;
            remainder = (remainder * (digits > 9 ? 100 : 10) + digits) % 97;
        }
        return remainder;
    }

    /** « BE68 5390 0754 7034 » : groupes de quatre caractères, comme sur un relevé bancaire. */
    public static String format(String value) {
        String iban = compact(value);
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < iban.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(' ');
            }
            formatted.append(iban.charAt(i));
        }
        return formatted.toString();
    }
}
