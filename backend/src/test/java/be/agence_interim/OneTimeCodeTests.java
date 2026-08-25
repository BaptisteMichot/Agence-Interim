package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.agence_interim.service.OneTimeCodes;
import be.agence_interim.service.OneTimeCodes.InvalidCodeException;

/**
 * Le code à usage unique envoyé par email.
 *
 * <p>C'est la seule preuve de consentement de toute l'application : saisir ce code est
 * ce qui distingue une signature de contrat d'un simple clic, et ce qui autorise à
 * choisir un nouveau mot de passe. La demande non fonctionnelle 7 de l'analyse le
 * chiffre — « six chiffres, valable quinze minutes, invalidé après cinq mauvaises
 * tentatives » — et ces trois nombres sont exactement le genre de valeur qu'un
 * remaniement change sans que rien ne proteste.
 *
 * <p>La classe garde ses codes en mémoire et ne dépend d'aucun contexte : ces tests
 * s'exécutent en quelques millisecondes, et l'expiration se simule par une durée de vie
 * déjà écoulée plutôt qu'en faisant attendre la suite.
 */
class OneTimeCodeTests {

    private static final Duration VALIDITY = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;
    private static final String KEY = "42:7";

    private final OneTimeCodes codes = new OneTimeCodes(VALIDITY, MAX_ATTEMPTS, "de signature");

    @Test
    @DisplayName("Un code est fait de six chiffres, zéros de tête compris")
    void acodeIsMadeOfSixDigits() {
        // Le zéro de tête est la raison du formatage : un tirage qui rendrait « 4213 »
        // au lieu de « 004213 » donnerait un code que l'utilisateur saisit tel qu'il le
        // lit, et qui serait refusé.
        for (int attempt = 0; attempt < 50; attempt++) {
            assertThat(codes.issue(KEY)).matches("\\d{6}");
        }
    }

    @Test
    @DisplayName("Le bon code est accepté une fois, et une seule")
    void thecorrectCodeIsAcceptedExactlyOnce() {
        // « À usage unique » n'est pas une formule : le code circule par email, une boîte
        // mail se consulte à plusieurs, et un code rejouable signerait deux fois.
        String code = codes.issue(KEY);

        assertThatNoException().isThrownBy(() -> codes.verify(KEY, code));

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> codes.verify(KEY, code))
                .withMessageContaining("Aucun code valide");
    }

    @Test
    @DisplayName("Cinq tentatives erronées épuisent le code, qui ne fonctionne plus même juste")
    void fiveWrongAttemptsBurnTheCode() {
        // Six chiffres, c'est un million de combinaisons : sans plafond, une machine les
        // parcourt en quelques minutes et signe à la place de la personne. Le plafond est
        // ce qui rend le code court acceptable.
        String code = codes.issue(KEY);

        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            assertThatExceptionOfType(InvalidCodeException.class)
                    .isThrownBy(() -> codes.verify(KEY, "000000"))
                    .withMessage("Code incorrect.");
        }

        // La cinquième erreur ne dit plus « code incorrect » : elle retire le code.
        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> codes.verify(KEY, "000000"))
                .withMessageContaining("Trop de tentatives");

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> codes.verify(KEY, code))
                .withMessageContaining("Aucun code valide");
    }

    @Test
    @DisplayName("Une tentative juste remet le compteur à zéro en consommant le code")
    void acorrectAttemptEndsTheCountingByConsumingTheCode() {
        // Les essais ratés ne s'accumulent pas d'un code à l'autre : quelqu'un qui se
        // trompe quatre fois, redemande un code et le saisit juste ne doit pas être
        // arrêté par les erreurs de la fois précédente.
        codes.issue(KEY);
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            assertThatExceptionOfType(InvalidCodeException.class)
                    .isThrownBy(() -> codes.verify(KEY, "000000"));
        }

        String fresh = codes.issue(KEY);

        assertThatNoException().isThrownBy(() -> codes.verify(KEY, fresh));
    }

    @Test
    @DisplayName("Un code expiré est refusé")
    void anexpiredCodeIsRejected() {
        // La durée de vie est simulée par une échéance déjà passée : faire patienter la
        // suite quinze minutes pour éprouver une soustraction de dates serait absurde.
        OneTimeCodes expired = new OneTimeCodes(Duration.ofSeconds(-1), MAX_ATTEMPTS, "de signature");
        String code = expired.issue(KEY);

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> expired.verify(KEY, code))
                .withMessageContaining("Aucun code valide");
    }

    @Test
    @DisplayName("Demander un nouveau code périme le précédent")
    void askingForANewCodeVoidsThePreviousOne() {
        // Sinon chaque demande ajouterait une clé valable de plus : le lien « je n'ai rien
        // reçu, renvoyez-moi le code » multiplierait les codes en circulation.
        String first = codes.issue(KEY);
        String second = codes.issue(KEY);

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> codes.verify(KEY, first));
        assertThatNoException().isThrownBy(() -> codes.verify(KEY, second));
    }

    @Test
    @DisplayName("Le code d'une clé n'ouvre pas celle d'une autre")
    void acodeIssuedForOneKeyDoesNotOpenAnother() {
        // La clé porte le contrat et le signataire. Sans cette séparation, le code reçu
        // pour un contrat signerait tous les autres, et celui d'une partie signerait pour
        // l'autre.
        String mine = codes.issue("42:7");
        codes.issue("42:9");

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> codes.verify("42:9", mine))
                .withMessage("Code incorrect.");
    }

    @Test
    @DisplayName("Un code saisi avec des espaces est accepté, un code absent est refusé sans casse")
    void asubmittedCodeIsTrimmedAndNeverNull() {
        // Le code arrive par copier-coller depuis un email : l'espace de fin est la faute
        // la plus courante, et refuser un code juste pour cette raison serait incompris.
        String code = codes.issue(KEY);
        assertThatNoException().isThrownBy(() -> codes.verify(KEY, "  " + code + " "));

        codes.issue(KEY);
        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> codes.verify(KEY, null))
                .withMessage("Code incorrect.");
    }

    @Test
    @DisplayName("Un code retiré ne vaut plus rien")
    void aninvalidatedCodeIsWorthNothing() {
        // La réinitialisation de mot de passe s'en sert : une fois le mot de passe changé,
        // le code qui l'a autorisé ne doit plus pouvoir l'être une seconde fois.
        String code = codes.issue(KEY);

        codes.invalidate(KEY);

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> codes.verify(KEY, code))
                .withMessageContaining("Aucun code valide");
        // Retirer un code qui n'existe pas ne doit pas lever : l'appel se fait sur des
        // chemins où l'on ignore si un code était en cours.
        assertThatNoException().isThrownBy(() -> codes.invalidate("clef-inconnue"));
    }

    @Test
    @DisplayName("La durée de validité annoncée est celle qui est appliquée")
    void theannouncedValidityIsTheOneApplied() {
        // Elle est reprise telle quelle dans l'email envoyé au signataire : l'annoncer
        // autrement que ce que le contrôle applique serait mentir à l'utilisateur.
        assertThat(codes.getValidity()).isEqualTo(VALIDITY);
    }
}
