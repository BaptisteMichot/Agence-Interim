package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import be.agence_interim.service.Iban;

/**
 * La clé de contrôle du compte bancaire sur lequel part le salaire de la mission.
 *
 * <p>C'est le seul champ du profil dont une erreur coûte de l'argent : un IBAN mal
 * recopié mais syntaxiquement plausible envoie le virement ailleurs, et le rappeler
 * prend des semaines. La clé ISO 13616 attrape la quasi-totalité des fautes de frappe
 * réalistes — un chiffre changé, deux chiffres intervertis — d'où ces tests.
 */
class IbanTests {

    /** IBAN belge de test, celui que l'on trouve dans la documentation de la norme. */
    private static final String BELGE = "BE68 5390 0754 7034";

    @Test
    @DisplayName("Un IBAN belge correct est accepté, espacé ou non, en majuscules ou non")
    void aCorrectBelgianIbanIsAccepted() {
        // Un IBAN se recopie depuis une carte bancaire ou un relevé, donc avec les
        // espaces ; il se colle aussi depuis une application bancaire, donc sans. Les
        // deux formes désignent le même compte : refuser l'une serait arbitraire.
        assertThat(Iban.isValid(BELGE)).isTrue();
        assertThat(Iban.isValid("BE68539007547034")).isTrue();
        assertThat(Iban.isValid("be68 5390 0754 7034")).isTrue();
        assertThat(Iban.isValid("BE68-5390-0754-7034")).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Un IBAN étranger est accepté : un intérimaire peut être payé hors de Belgique")
    @ValueSource(strings = {
            "FR14 2004 1010 0505 0001 3M02 606",     // France, avec une lettre dans le numéro de compte
            "DE89 3704 0044 0532 0130 00",           // Allemagne
            "NL91 ABNA 0417 1643 00",                // Pays-Bas
            "GB82 WEST 1234 5698 7654 32",           // Royaume-Uni
            "MT84 MALT 0110 0001 2345 MTLC AST0 01S" // Malte : 31 caractères, la limite haute
    })
    void aForeignIbanIsAccepted(String value) {
        // Un travailleur frontalier ou détaché garde son compte d'origine. Le contrôle
        // porte sur la clé, jamais sur le pays : imposer « BE » exclurait une partie du
        // public de l'agence sans rien vérifier de plus.
        assertThat(Iban.isValid(value)).isTrue();
    }

    @Test
    @DisplayName("Une lettre du numéro de compte compte pour deux chiffres dans le calcul")
    void aLetterInsideTheAccountNumberCountsAsTwoDigits() {
        // Le « M » de l'IBAN français vaut 22, et le reste doit être mis à jour de deux
        // rangs pour ce seul caractère. Une implémentation qui décalerait d'un seul rang
        // validerait des IBAN faux : ces deux assertions séparent les deux cas.
        assertThat(Iban.isValid("FR14 2004 1010 0505 0001 3M02 606")).isTrue();
        assertThat(Iban.isValid("FR14 2004 1010 0505 0001 3M02 607")).isFalse();
    }

    @ParameterizedTest(name = "« {0} » est refusé")
    @DisplayName("Un IBAN dont la clé ou la forme ne va pas est refusé")
    @ValueSource(strings = {
            "BE68 5390 0754 7035", // dernier chiffre modifié : la faute de frappe type
            "BE68 5390 0754 7043", // deux chiffres intervertis
            "BE86 5390 0754 7034", // les deux chiffres de clé intervertis
            "BE68 5390 0754 703",  // un chiffre manquant
            "BE68 5390",           // trop court pour être un IBAN
            "BEXX 5390 0754 7034", // clé non numérique
            "6853 9007 5470 34",   // pas de code pays
            ""
    })
    void anIncorrectIbanIsRejected(String value) {
        assertThat(Iban.isValid(value)).isFalse();
    }

    @Test
    @DisplayName("Un IBAN absent est refusé plutôt que de faire échouer le contrôle")
    void aMissingIbanIsRejected() {
        // Le compte bancaire n'est exigé qu'au moment d'accepter une mission : le profil
        // s'enregistre sans, et le contrôle doit répondre « non » sans lever.
        assertThat(Iban.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("L'IBAN est stocké en groupes de quatre, comme sur un relevé bancaire")
    void theIbanIsStoredInGroupsOfFour() {
        // On normalise à l'enregistrement plutôt qu'à l'affichage : le même compte saisi
        // deux fois de deux façons doit s'écrire pareil dans la base et sur le contrat.
        assertThat(Iban.format("BE68539007547034")).isEqualTo(BELGE);
        assertThat(Iban.format("be68-5390-0754-7034")).isEqualTo(BELGE);
        assertThat(Iban.format(BELGE)).isEqualTo(BELGE);
    }

    @Test
    @DisplayName("Un dernier groupe incomplet n'est pas complété artificiellement")
    void aTrailingPartialGroupIsLeftAsIs() {
        // Les IBAN de longueur non multiple de 4 sont fréquents (la France en fait 27) :
        // le dernier groupe est simplement plus court.
        assertThat(Iban.format("FR1420041010050500013M02606"))
                .isEqualTo("FR14 2004 1010 0505 0001 3M02 606");
    }
}
