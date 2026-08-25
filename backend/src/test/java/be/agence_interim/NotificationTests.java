package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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

import be.agence_interim.dto.EmployerRegisterRequest;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.AccountService;
import be.agence_interim.service.EmployerAccessService;
import be.agence_interim.service.MailService;
import be.agence_interim.service.MissionService;
import be.agence_interim.service.PasswordResetService;

/**
 * Les emails envoyés aux moments où la plateforme décide quelque chose sans son
 * destinataire.
 *
 * <p>Quatre des envois de l'application répondent à un geste que l'utilisateur vient de
 * faire : un code qu'il réclame, un contrat qu'il vient d'accepter. Ceux réunis ici sont
 * d'une autre nature. L'agence tranche une demande d'accès, elle refuse une mission, un
 * mot de passe change : le destinataire n'est pas devant son écran, et rien d'autre ne
 * l'avertira. C'est ce qui rend le contenu du message important, et non seulement son
 * envoi — il doit dire ce qui vient de se passer <em>et</em> ce qu'il reste à faire.
 *
 * <p>La vérification la plus utile de cette classe est celle de la clôture de compte. Le
 * message part vers une adresse qui, à l'instant de l'envoi, n'existe déjà plus : clôturer
 * anonymise le compte — l'email devient une adresse de rebut — ou supprime la ligne.
 * Écrire au bon destinataire suppose de l'avoir relevé avant, rien dans le code ne le
 * rappelle, et {@link MailService} ne lève jamais : l'erreur serait donc parfaitement
 * silencieuse. Un test la rend visible.
 *
 * <p>L'espion sur {@link MailService} est déclaré exactement comme dans
 * {@code ContractSignatureTests} et {@code PasswordResetTests}. À déclaration identique,
 * Spring réutilise le contexte déjà en cache au lieu d'en construire un de plus, ce qui
 * coûterait quelques secondes à chaque exécution de la suite.
 */
@SpringBootTest
class NotificationTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");
    private static final String OLD_PASSWORD = "AncienMotDePasse1!";
    private static final String NEW_PASSWORD = "NouveauMotDePasse1!";

    /** Numéro d'entreprise réel, donc à clé de contrôle valide. */
    private static final String COMPANY_NUMBER = "0403.199.702";

    @MockitoSpyBean
    private MailService mailService;

    @Autowired
    private EmployerAccessService employerAccessService;

    @Autowired
    private MissionService missionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
    }

    // ------------------------------------------------------ demande d'accès employeur

    @Test
    @DisplayName("L'employeur dont l'accès est accordé apprend qu'il doit se reconnecter")
    void anAcceptedEmployerIsToldToSignInAgain() {
        // L'acceptation incrémente la version de session pour que le nouveau rôle prenne
        // effet tout de suite. L'intéressé se retrouve donc déconnecté au moment même où on
        // lui ouvre la porte : sans cette phrase, l'effet ressemble à une panne.
        User employer = registeredEmployer();

        employerAccessService.accept(fixtures.admin().getId(), latestRequestId(employer));

        verify(mailService).send(
                eq(employer.getEmail()),
                eq("Votre accès employeur est accordé"),
                contains("Reconnectez-vous"));
    }

    @Test
    @DisplayName("Un refus ordinaire indique par où soumettre une nouvelle demande")
    void anOrdinaryRefusalPointsToTheResubmissionScreen() {
        User employer = registeredEmployer();

        employerAccessService.refuse(fixtures.admin().getId(), latestRequestId(employer), false);

        verify(mailService).send(
                eq(employer.getEmail()),
                eq("Votre demande d'accès employeur"),
                contains("/statut-employeur"));
    }

    @Test
    @DisplayName("Un refus définitif n'invite pas à recommencer")
    void aFinalRefusalDoesNotInviteToTryAgain() {
        // Envoyer resoumettre quelqu'un dont la porte est fermée le ferait se heurter à un
        // écran de refus : les deux refus n'ouvrent pas les mêmes suites, le message ne
        // peut donc pas être le même.
        User employer = registeredEmployer();

        employerAccessService.refuse(fixtures.admin().getId(), latestRequestId(employer), true);

        String body = capturedBody(employer.getEmail(), "Votre demande d'accès employeur");
        assertThat(body).contains("définitive").doesNotContain("/statut-employeur");
    }

    // -------------------------------------------------------------- refus de mission

    @Test
    @DisplayName("L'employeur reçoit le motif du refus de l'agence et le lien pour corriger")
    void theEmployerReceivesTheAgencyRefusalReason() {
        // Une mission refusée ne se voit que de l'employeur et de l'agence : l'intérimaire
        // ignore encore qu'on lui destinait cette mission. Sans email, l'employeur devrait
        // revenir de lui-même constater un refus dont la correction n'attend que lui.
        int missionId = missionService.create(
                fixtures.employer.getId(), fixtures.application().getId(), fixtures.request()).id();

        missionService.refuse(
                fixtures.admin().getId(), missionId, "Le salaire est sous le barème de la CP 140.");

        verify(mailService).send(
                eq(fixtures.employer.getEmail()),
                contains("correction demandée"),
                contains("Le salaire est sous le barème de la CP 140."));
    }

    // ------------------------------------------------------------- mot de passe changé

    @Test
    @DisplayName("Un mot de passe changé depuis le compte est signalé à son titulaire")
    void aDeliberatePasswordChangeIsReportedToItsOwner() {
        User user = userWithKnownPassword();

        accountService.changePassword(user.getId(), OLD_PASSWORD, NEW_PASSWORD);

        verify(mailService).send(
                eq(user.getEmail()), eq("Votre mot de passe a été modifié"), anyString());
    }

    @Test
    @DisplayName("Un mot de passe réinitialisé par code est signalé de la même façon")
    void aPasswordResetByCodeIsReportedTheSameWay() {
        // Les deux chemins qui mènent à un mot de passe changé passent par la même méthode.
        // Ce test vérifie que la réinitialisation emprunte bien ce point d'envoi unique, et
        // qu'il n'existe pas un second texte à tenir à jour ailleurs.
        User user = userWithKnownPassword();
        String email = user.getEmail();
        passwordResetService.requestCode(email);
        String code = sixDigitsOf(capturedBody(email, "Réinitialisation de votre mot de passe"));

        passwordResetService.reset(email, code, NEW_PASSWORD);

        verify(mailService).send(
                eq(email), eq("Votre mot de passe a été modifié"), anyString());
    }

    // -------------------------------------------------------------- clôture de compte

    @Test
    @DisplayName("Le compte supprimé est accusé à son adresse, relevée avant qu'elle disparaisse")
    void aDeletedAccountIsAcknowledgedAtItsFormerAddress() {
        // Rien ne rattache ce compte à quoi que ce soit : sa ligne est supprimée, et
        // l'adresse à laquelle écrire n'existe donc plus au moment de l'envoi.
        User user = fixtures.user("sansengage", Role.JOBSEEKER);
        String formerEmail = user.getEmail();

        accountService.close(user.getId());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        verify(mailService).send(
                eq(formerEmail), eq("Votre compte a été clôturé"), contains("Aucune donnée"));
    }

    @Test
    @DisplayName("Le compte anonymisé est averti à sa vraie adresse, pas à l'adresse de rebut")
    void anAnonymisedAccountIsWarnedAtItsRealAddress() {
        // Un compte qui a déjà candidaté n'est pas supprimé mais anonymisé : son email
        // devient « supprime+<id>@invalide.local ». Relever l'adresse après coup enverrait
        // l'accusé dans le vide, sans la moindre erreur.
        fixtures.application();
        String formerEmail = fixtures.worker.getEmail();

        accountService.close(fixtures.worker.getId());

        User closed = userRepository.findById(fixtures.worker.getId()).orElseThrow();
        assertThat(closed.getEmail()).isNotEqualTo(formerEmail);
        assertThat(closed.getClosedAt()).isNotNull();
        verify(mailService).send(
                eq(formerEmail), eq("Votre compte a été clôturé"), contains("conservés"));
    }

    // ----------------------------------------------------------------------- décor

    private User registeredEmployer() {
        // Le préfixe est propre à cette classe. La base H2 est partagée par toute la suite,
        // et chaque classe compte de son côté à partir de 1 : deux classes qui composent
        // leurs adresses de la même façon produisent la même suite, et la seconde à passer
        // échoue sur « Cet email est déjà utilisé ». EmployerAccessTests emploie « acces- ».
        String email = "notif-" + SEQUENCE.incrementAndGet() + "@example.be";
        employerAccessService.registerEmployer(new EmployerRegisterRequest(
                "Dupont", "Jean", email, "MotDePasseSolide1!",
                "Entrepots du Borinage", "Rue de la Gare 1, 7000 Mons", COMPANY_NUMBER, "140"));
        return userRepository.findByEmail(email).orElseThrow();
    }

    private int latestRequestId(User user) {
        return employerAccessService.latestRequest(user.getId()).getId();
    }

    /** Un compte dont on connaît le mot de passe en clair, seul moyen de le faire changer. */
    private User userWithKnownPassword() {
        User created = fixtures.user("motdepasse", Role.JOBSEEKER);
        created.setPassword(passwordEncoder.encode(OLD_PASSWORD));
        return fixtures.save(created);
    }

    /** Le corps du message envoyé à cette adresse sous ce sujet. */
    private String capturedBody(String recipient, String subject) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq(recipient), eq(subject), body.capture());
        return body.getValue();
    }

    private String sixDigitsOf(String body) {
        Matcher matcher = SIX_DIGITS.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
