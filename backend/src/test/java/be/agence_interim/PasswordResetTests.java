package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.MailService;
import be.agence_interim.service.OneTimeCodes.InvalidCodeException;
import be.agence_interim.service.PasswordResetService;

/**
 * La réinitialisation du mot de passe par code envoyé sur l'adresse du compte.
 *
 * <p>Ce parcours est le seul par lequel on reprend la main sur un compte, et donc aussi
 * celui par lequel on tenterait d'en prendre un qui n'est pas le sien. Deux propriétés
 * comptent autant l'une que l'autre : le code doit vraiment fonctionner, et la demande
 * ne doit jamais laisser deviner si une adresse correspond à un compte — sur un site
 * d'emploi, savoir que telle personne y est inscrite est déjà une information sur elle.
 */
@SpringBootTest
class PasswordResetTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");
    private static final String OLD_PASSWORD = "AncienMotDePasse1!";
    private static final String NEW_PASSWORD = "NouveauMotDePasse1!";

    @MockitoSpyBean
    private MailService mailService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private String email;

    @BeforeEach
    void setUp() {
        // Une adresse par exécution : le quota d'envoi d'emails est compté par adresse, et
        // une adresse réutilisée ferait échouer un test à cause du précédent.
        email = "oubli-" + SEQUENCE.incrementAndGet() + "@example.be";
        user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(OLD_PASSWORD));
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setRole(Role.JOBSEEKER);
        user = userRepository.save(user);
    }

    @Test
    @DisplayName("Le code reçu par email permet de choisir un nouveau mot de passe")
    void thecodeReceivedByEmailAllowsANewPassword() {
        String code = requestCode();

        passwordResetService.reset(email, code, NEW_PASSWORD);

        User updated = userRepository.requireById(user.getId());
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, updated.getPassword())).isFalse();
    }

    @Test
    @DisplayName("Réinitialiser son mot de passe met fin aux sessions ouvertes")
    void resettingThePasswordEndsOpenSessions() {
        // Quelqu'un qui réinitialise son mot de passe a souvent une raison de croire son
        // compte compromis. Laisser vivre les jetons déjà émis laisserait l'intrus
        // connecté jusqu'à une heure après le changement.
        int versionBefore = user.getTokenVersion();

        passwordResetService.reset(email, requestCode(), NEW_PASSWORD);

        assertThat(userRepository.requireById(user.getId()).getTokenVersion())
                .isGreaterThan(versionBefore);
    }

    @Test
    @DisplayName("Une adresse inconnue reçoit la même réponse qu'une adresse connue")
    void anunknownAddressGetsTheSameAnswerAsAKnownOne() {
        // C'est la propriété qui empêche d'énumérer les comptes une requête à la fois. Le
        // service ne lève pas et ne dit rien ; simplement, aucun email ne part.
        String unknown = "personne-" + SEQUENCE.incrementAndGet() + "@example.be";

        assertThatNoException().isThrownBy(() -> passwordResetService.requestCode(unknown));

        verify(mailService, never()).send(eq(unknown), anyString(), anyString());
    }

    @Test
    @DisplayName("L'adresse est reconnue quelle que soit sa casse")
    void theaddressIsRecognisedWhateverItsCase() {
        // Personne ne retape son adresse exactement comme le jour de l'inscription. La
        // refuser sur une majuscule laisserait quelqu'un dehors sans lui dire pourquoi.
        passwordResetService.requestCode(email.toUpperCase());

        String code = capturedCode();
        assertThatNoException().isThrownBy(
                () -> passwordResetService.reset(email.toUpperCase(), code, NEW_PASSWORD));
    }

    @Test
    @DisplayName("Un code erroné ne change pas le mot de passe")
    void awrongCodeChangesNothing() {
        requestCode();

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> passwordResetService.reset(email, "000000", NEW_PASSWORD));

        assertThat(passwordEncoder.matches(OLD_PASSWORD, userRepository.requireById(user.getId()).getPassword()))
                .isTrue();
    }

    @Test
    @DisplayName("Le code ne sert qu'une fois")
    void thecodeOnlyServesOnce() {
        // Il transite par une boîte mail, qui peut se consulter à plusieurs : rejouable, il
        // laisserait reprendre la main sur le compte une seconde fois.
        String code = requestCode();
        passwordResetService.reset(email, code, NEW_PASSWORD);

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> passwordResetService.reset(email, code, "EncoreUnAutre1!"))
                .withMessageContaining("Aucun code valide");
    }

    @Test
    @DisplayName("Le code d'un compte ne réinitialise pas celui d'un autre")
    void thecodeOfOneAccountDoesNotResetAnother() {
        // Le code est rangé sous l'adresse qui l'a demandé : sans cela, recevoir un code
        // pour sa propre adresse suffirait à changer le mot de passe de n'importe qui.
        String mine = requestCode();
        String otherEmail = "voisin-" + SEQUENCE.incrementAndGet() + "@example.be";
        User other = new User();
        other.setEmail(otherEmail);
        other.setPassword(passwordEncoder.encode(OLD_PASSWORD));
        other.setFirstName("Marie");
        other.setLastName("Martin");
        other.setRole(Role.JOBSEEKER);
        userRepository.save(other);

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> passwordResetService.reset(otherEmail, mine, NEW_PASSWORD));
    }

    @Test
    @DisplayName("L'email annonce un code à six chiffres et sa durée de validité")
    void theemailAnnouncesASixDigitCodeAndItsValidity() {
        passwordResetService.requestCode(email);

        assertThat(capturedBody())
                .containsPattern("\\b\\d{6}\\b")
                .contains("valable 15 minutes")
                // Le message doit rassurer celui qui n'a rien demandé : sans cette phrase,
                // un email de réinitialisation non sollicité ressemble à une intrusion.
                .contains("ignorez ce message");
    }

    // ------------------------------------------------------------------------------ outils

    /** Demande un code et le relit dans l'email simulé. */
    private String requestCode() {
        passwordResetService.requestCode(email);
        return capturedCode();
    }

    private String capturedCode() {
        Matcher matcher = SIX_DIGITS.matcher(capturedBody());
        assertThat(matcher.find()).as("l'email porte un code à six chiffres").isTrue();
        return matcher.group(1);
    }

    private String capturedBody() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq(email), anyString(), body.capture());
        return body.getValue();
    }
}
