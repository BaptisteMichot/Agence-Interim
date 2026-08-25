package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import be.agence_interim.service.BelgianIdentifiers;

/**
 * Les clés de contrôle des identifiants belges repris sur le contrat de travail.
 *
 * <p>Ces deux numéros sont saisis à la main et personne ne les relit : le registre
 * national par l'intérimaire, le numéro d'entreprise par l'employeur. Une faute de
 * frappe non détectée ne se voit qu'au moment où l'ONSS refuse la déclaration Dimona,
 * c'est-à-dire longtemps après que le contrat a été signé. La clé modulo 97 est le seul
 * garde-fou du système, d'où ces tests sur des numéros dont la clé a été calculée à la
 * main plutôt que reprise du code.
 */
class BelgianIdentifiersTests {

    /** Registre national d'une personne née le 30 juillet 1985 : 850730033 % 97 = 69, clé 28. */
    private static final String NISS_1985 = "85.07.30-033.28";

    /** Née le 14 mai 2001 : la clé ne tombe juste qu'en préfixant le nombre de base par un 2. */
    private static final String NISS_2001 = "01.05.14-123.27";

    /** Numéro d'entreprise réel (BNP Paribas Fortis), donc au format effectivement rencontré. */
    private static final String BCE_REELLE = "0403.199.702";

    @Test
    @DisplayName("Un registre national correct est accepté, quels que soient les séparateurs")
    void nationalNumberIsAcceptedWhateverTheSeparators() {
        // La saisie est libre côté écran : ce sont les chiffres qui portent la clé, pas
        // la ponctuation. Accepter les trois formes évite de rejeter un numéro juste.
        assertThat(BelgianIdentifiers.isValidNationalNumber(NISS_1985)).isTrue();
        assertThat(BelgianIdentifiers.isValidNationalNumber("85073003328")).isTrue();
        assertThat(BelgianIdentifiers.isValidNationalNumber("85 07 30 033 28")).isTrue();
    }

    @Test
    @DisplayName("Une naissance à partir de l'an 2000 se reconnaît au 2 ajouté devant le nombre de base")
    void nationalNumberOfSomeoneBornAfter2000IsAccepted() {
        // Le numéro ne dit pas le siècle : 01.05.14 se lit 1901 comme 2001. La règle
        // officielle recalcule donc la clé sur 2 000 000 000 + le nombre de base pour les
        // personnes nées à partir de 2000. Sans cette seconde tentative, tout intérimaire
        // de moins de 26 ans serait refusé à l'inscription.
        assertThat(BelgianIdentifiers.isValidNationalNumber(NISS_2001)).isTrue();
        assertThat(BelgianIdentifiers.isValidNationalNumber("01.05.14-123.24")).isFalse();
    }

    @Test
    @DisplayName("Une clé de 97 est valide : c'est le cas d'un nombre de base multiple de 97")
    void aCheckKeyOfNinetySevenIsValid() {
        // 850730061 est divisible par 97, donc 97 - 0 = 97. Ce cas limite est le premier
        // que casse une implémentation qui ramènerait le reste dans l'intervalle 0-96.
        assertThat(BelgianIdentifiers.isValidNationalNumber("85073006197")).isTrue();
    }

    @ParameterizedTest(name = "« {0} » est refusé")
    @DisplayName("Un registre national dont la clé ou la longueur ne tombe pas juste est refusé")
    @ValueSource(strings = {
            "85.07.30-033.29", // clé fausse d'une unité : la faute de frappe type
            "85.07.30-033.82", // les deux chiffres de la clé intervertis
            "85.07.03-033.28", // jour et mois de naissance intervertis
            "8507300332",      // 10 chiffres : un chiffre oublié
            "850730033281",    // 12 chiffres : un chiffre en trop
            "",
            "pas un numéro"
    })
    void anIncorrectNationalNumberIsRejected(String value) {
        assertThat(BelgianIdentifiers.isValidNationalNumber(value)).isFalse();
    }

    @Test
    @DisplayName("Un registre national absent est refusé plutôt que de faire échouer le contrôle")
    void aMissingNationalNumberIsRejected() {
        // Le champ est facultatif tant que l'intérimaire n'accepte pas de mission : le
        // contrôle doit répondre « non » sans lever, sinon un profil incomplet devient
        // impossible à enregistrer.
        assertThat(BelgianIdentifiers.isValidNationalNumber(null)).isFalse();
    }

    @Test
    @DisplayName("Un numéro d'entreprise correct est accepté, quels que soient les séparateurs")
    void companyNumberIsAcceptedWhateverTheSeparators() {
        assertThat(BelgianIdentifiers.isValidCompanyNumber(BCE_REELLE)).isTrue();
        assertThat(BelgianIdentifiers.isValidCompanyNumber("0403199702")).isTrue();
        assertThat(BelgianIdentifiers.isValidCompanyNumber("BE 0403.199.702")).isTrue();
    }

    @Test
    @DisplayName("Un numéro d'entreprise commençant par 1 est accepté")
    void companyNumberStartingWithOneIsAccepted() {
        // La BCE a épuisé la plage en 0 et attribue depuis 2008 des numéros en 1 : les
        // refuser exclurait les entreprises les plus récentes, celles qui recrutent.
        assertThat(BelgianIdentifiers.isValidCompanyNumber("1020.304.002")).isTrue();
    }

    @ParameterizedTest(name = "« {0} » est refusé")
    @DisplayName("Un numéro d'entreprise dont la clé, la longueur ou le premier chiffre ne va pas est refusé")
    @ValueSource(strings = {
            "0403.199.703", // clé fausse d'une unité
            "2020.304.023", // clé juste, mais aucun numéro d'entreprise ne commence par 2
            "9876.543.265", // idem : la clé ne suffit pas à faire un numéro d'entreprise
            "040319970",    // 9 chiffres
            "04031997022",  // 11 chiffres
            ""
    })
    void anIncorrectCompanyNumberIsRejected(String value) {
        assertThat(BelgianIdentifiers.isValidCompanyNumber(value)).isFalse();
    }

    @Test
    @DisplayName("Un numéro d'entreprise absent est refusé plutôt que de faire échouer le contrôle")
    void aMissingCompanyNumberIsRejected() {
        assertThat(BelgianIdentifiers.isValidCompanyNumber(null)).isFalse();
    }

    @Test
    @DisplayName("Les deux numéros sont remis au format officiel avant d'aller sur le contrat")
    void bothIdentifiersAreFormattedTheOfficialWay() {
        // Le contrat de travail est un document lu par un tiers : on y écrit le numéro
        // sous la forme dans laquelle il est publié, pas sous celle où il a été tapé.
        assertThat(BelgianIdentifiers.formatNationalNumber("85073003328")).isEqualTo("85.07.30-033.28");
        assertThat(BelgianIdentifiers.formatNationalNumber("85 07 30 033 28")).isEqualTo("85.07.30-033.28");
        assertThat(BelgianIdentifiers.formatCompanyNumber("0403199702")).isEqualTo("0403.199.702");
        assertThat(BelgianIdentifiers.formatCompanyNumber("BE 0403.199.702")).isEqualTo("0403.199.702");
    }

    @Test
    @DisplayName("Un numéro impossible à mettre en forme est rendu tel quel, sans être tronqué")
    void anUnformattableIdentifierIsReturnedUnchanged() {
        // La mise en forme n'est pas un contrôle : elle est appelée sur des valeurs déjà
        // validées. Si une valeur inattendue passait quand même, mieux vaut la rendre
        // intacte que d'en fabriquer une fausse par découpage.
        assertThat(BelgianIdentifiers.formatNationalNumber("850730")).isEqualTo("850730");
        assertThat(BelgianIdentifiers.formatCompanyNumber("040319")).isEqualTo("040319");
    }
}
