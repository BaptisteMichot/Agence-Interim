package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.agence_interim.service.Strings;

/**
 * Les deux normalisations partagées par les services.
 *
 * <p>Dix-huit lignes, mais elles décident de deux choses qui ne se rattrapent pas
 * ailleurs. La première : ce qui distingue « champ laissé vide » de « champ contenant
 * des espaces », faute de quoi une chaîne blanche s'enregistre comme une valeur et les
 * contrôles de complétude la prennent pour un champ rempli. La seconde : la forme sous
 * laquelle une adresse email est comparée, c'est-à-dire ce qui fait qu'un même
 * utilisateur n'a pas deux comptes.
 */
class StringsTests {

    @Test
    @DisplayName("Une chaîne blanche vaut « rien », une chaîne pleine est épurée")
    void ablankStringIsNothingAndAFullOneIsTrimmed() {
        // Le champ « adresse » de l'intérimaire est exigé avant d'accepter une mission,
        // et ce contrôle demande simplement si la valeur est nulle. Un espace enregistré
        // tel quel lui ferait répondre oui.
        assertThat(Strings.blankToNull(null)).isNull();
        assertThat(Strings.blankToNull("")).isNull();
        assertThat(Strings.blankToNull("   ")).isNull();
        assertThat(Strings.blankToNull("\t\n")).isNull();

        assertThat(Strings.blankToNull("  Rue Neuve 12  ")).isEqualTo("Rue Neuve 12");
        assertThat(Strings.blankToNull("Mons")).isEqualTo("Mons");
    }

    @Test
    @DisplayName("Une adresse email est comparée en minuscules et sans espaces")
    void anemailIsComparedInLowercaseAndWithoutSpaces() {
        // Demande non fonctionnelle 2 : le même utilisateur n'a pas deux comptes. Sans
        // cette normalisation, « Jean@Example.be » et « jean@example.be » en feraient deux,
        // et le second empêcherait le premier de se connecter sans qu'on comprenne pourquoi.
        assertThat(Strings.normalizeEmail("  Jean.Dupont@Example.BE ")).isEqualTo("jean.dupont@example.be");
        assertThat(Strings.normalizeEmail("jean@example.be")).isEqualTo("jean@example.be");
    }

    @Test
    @DisplayName("Une adresse absente le reste, sans lever")
    void amissingEmailStaysMissing() {
        // La normalisation est appelée avant toute validation, y compris sur les chemins
        // publics où le corps de la requête peut être incomplet.
        assertThat(Strings.normalizeEmail(null)).isNull();
    }
}
