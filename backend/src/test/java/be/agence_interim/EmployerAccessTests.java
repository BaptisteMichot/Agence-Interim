package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import be.agence_interim.dto.EmployerCompanyRequest;
import be.agence_interim.dto.EmployerRegisterRequest;
import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.model.EmployerAccessStatus;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.EmployerAccessService;

/**
 * L'accès au rôle employeur : demande, examen par l'agence, et refus.
 *
 * <p>C'est la seule porte fermée de l'application. N'importe qui crée un compte
 * d'intérimaire, mais publier une offre engage l'agence auprès de demandeurs d'emploi :
 * l'analyse impose donc qu'un administrateur attribue ce rôle (demandes 3 et 4). Le
 * compte existe entre-temps, dans un état d'attente qui ne donne aucun droit.
 *
 * <p>Le point délicat est le refus. Il doit pouvoir être provisoire — l'entreprise
 * complète son dossier et resoumet — ou définitif, sans quoi un compte éconduit
 * inonderait la file de l'agence.
 */
@SpringBootTest
class EmployerAccessTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** Numéro d'entreprise réel, donc à clé de contrôle valide. */
    private static final String COMPANY_NUMBER = "0403.199.702";

    @Autowired
    private EmployerAccessService employerAccessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private int adminId;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        adminId = fixtures.admin().getId();
    }

    @Test
    @DisplayName("L'inscription d'un employeur crée un compte en attente et une demande à traiter")
    void registeringAnEmployerCreatesAPendingAccountAndARequest() {
        // Le compte existe tout de suite — il faut bien pouvoir se connecter pour suivre sa
        // demande — mais son rôle ne donne accès à rien tant que l'agence n'a pas tranché.
        String email = register();

        User created = userRepository.findByEmail(email).orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.EMPLOYER_PENDING);
        assertThat(created.getCompanyNumber()).isEqualTo(COMPANY_NUMBER);
        assertThat(employerAccessService.latestStatus(created.getId()))
                .isEqualTo(EmployerAccessStatus.PENDING);
    }

    @Test
    @DisplayName("Le numéro d'entreprise est vérifié dès l'inscription")
    void thecompanyNumberIsCheckedAtRegistrationTime() {
        // Il figure sur chaque contrat que cette entreprise signera. Une faute de frappe
        // découverte au moment d'établir un contrat bloquerait une mission déjà acceptée.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> employerAccessService.registerEmployer(
                        registration(uniqueEmail(), "0403.199.703")))
                .withMessageContaining("numéro d'entreprise");
    }

    @Test
    @DisplayName("Une adresse email déjà prise ne peut pas servir à s'inscrire une seconde fois")
    void anemailAlreadyTakenCannotRegisterAgain() {
        // Demande non fonctionnelle 2 : un même utilisateur n'a pas deux comptes. Ici le
        // message est explicite, contrairement à la connexion — l'ergonomie l'emporte,
        // c'est un choix documenté.
        String email = register();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> employerAccessService.registerEmployer(registration(email, COMPANY_NUMBER)))
                .withMessageContaining("déjà utilisé");
    }

    @Test
    @DisplayName("L'agence qui accepte accorde le rôle et coupe court aux jetons en circulation")
    void acceptingGrantsTheRoleAndRevokesLiveTokens() {
        // Le rôle voyage dans le jeton. Sans incrémenter la version de session, le nouvel
        // employeur garderait jusqu'à une heure un jeton qui le dit encore en attente, et
        // se verrait refuser les écrans qu'on vient de lui ouvrir.
        User pending = registeredUser();
        int versionBefore = pending.getTokenVersion();

        employerAccessService.accept(adminId, latestRequestId(pending));

        User accepted = userRepository.requireById(pending.getId());
        assertThat(accepted.getRole()).isEqualTo(Role.EMPLOYER);
        assertThat(accepted.getTokenVersion()).isGreaterThan(versionBefore);
        assertThat(employerAccessService.latestStatus(pending.getId()))
                .isEqualTo(EmployerAccessStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Une demande déjà tranchée ne se retranche pas")
    void arequestAlreadySettledIsNotSettledAgain() {
        // Deux administrateurs peuvent avoir la même file ouverte : le second doit être
        // arrêté plutôt que de renverser la décision du premier sans le savoir.
        User pending = registeredUser();
        int requestId = latestRequestId(pending);
        employerAccessService.accept(adminId, requestId);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> employerAccessService.refuse(adminId, requestId, false))
                .withMessageContaining("déjà été traitée");
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> employerAccessService.accept(adminId, 0));
    }

    @Test
    @DisplayName("Un refus ordinaire laisse la porte ouverte à une nouvelle demande")
    void anordinaryRefusalLeavesTheDoorOpen() {
        // Le cas courant est le dossier incomplet : l'entreprise corrige et resoumet, avec
        // un message qui explique ce qui a changé.
        User pending = registeredUser();
        employerAccessService.refuse(adminId, latestRequestId(pending), false);

        assertThat(employerAccessService.myRequest(pending.getId()).reapplyBlocked()).isFalse();

        employerAccessService.reapply(pending.getId(), "Nous avons ajouté notre numéro d'agrément.");

        assertThat(employerAccessService.latestStatus(pending.getId()))
                .isEqualTo(EmployerAccessStatus.PENDING);
    }

    @Test
    @DisplayName("Un refus définitif ferme la porte pour de bon")
    void afinalRefusalClosesTheDoorForGood() {
        User pending = registeredUser();
        employerAccessService.refuse(adminId, latestRequestId(pending), true);

        assertThat(employerAccessService.myRequest(pending.getId()).reapplyBlocked()).isTrue();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> employerAccessService.reapply(pending.getId(), "Encore une fois"))
                .withMessageContaining("définitivement");
    }

    @Test
    @DisplayName("On ne resoumet pas une demande qui n'a pas été refusée")
    void nobodyReappliesOnARequestThatWasNotRefused() {
        // Sans ce contrôle, une même personne empilerait des demandes en attente et
        // l'agence traiterait plusieurs fois le même dossier.
        User pending = registeredUser();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> employerAccessService.reapply(pending.getId(), null))
                .withMessageContaining("pas refaire de demande");

        // Et un intérimaire n'a rien à faire dans ce parcours.
        User jobSeeker = fixtures.user("candidat", Role.JOBSEEKER);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> employerAccessService.reapply(jobSeeker.getId(), null))
                .withMessageContaining("non autorisee");
    }

    @Test
    @DisplayName("La file de l'agence sépare ce qui attend de ce qui est tranché")
    void theagencyQueueSeparatesPendingFromSettled() {
        User pending = registeredUser();
        int requestId = latestRequestId(pending);

        assertThat(requestIds("pending")).contains(requestId);
        assertThat(requestIds("history")).doesNotContain(requestId);

        employerAccessService.refuse(adminId, requestId, false);

        assertThat(requestIds("pending")).doesNotContain(requestId);
        assertThat(requestIds("history")).contains(requestId);
    }

    @Test
    @DisplayName("Une demande resoumise est signalée comme telle à l'agence")
    void aresubmittedRequestIsFlaggedAsSuch() {
        // L'administrateur n'a pas la même lecture d'une première demande et d'un dossier
        // qu'il a déjà refusé : le drapeau lui évite d'avoir à s'en souvenir.
        User pending = registeredUser();
        employerAccessService.refuse(adminId, latestRequestId(pending), false);
        employerAccessService.reapply(pending.getId(), "Dossier complété.");
        int second = latestRequestId(pending);

        assertThat(employerAccessService.list("pending", PageRequest.of(0, 100)).content().stream()
                .filter(request -> request.id() == second)
                .toList())
                .singleElement()
                .satisfies(request -> {
                    assertThat(request.resubmission()).isTrue();
                    assertThat(request.message()).isEqualTo("Dossier complété.");
                });
    }

    @Test
    @DisplayName("La fiche entreprise se corrige, sa clé de contrôle restant vérifiée")
    void thecompanyFileCanBeCorrectedWithItsKeyStillChecked() {
        User employer = fixtures.employer;

        User updated = employerAccessService.updateCompany(employer.getId(), new EmployerCompanyRequest(
                "  Entrepots du Borinage  ", "  Rue Neuve 1, 7000 Mons  ", "0403199702", " 200 "));

        // Les valeurs sont normalisées : elles finissent telles quelles sur un contrat.
        assertThat(updated.getCompanyName()).isEqualTo("Entrepots du Borinage");
        assertThat(updated.getCompanyNumber()).isEqualTo("0403.199.702");
        assertThat(updated.getJointCommittee()).isEqualTo("200");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> employerAccessService.updateCompany(
                        employer.getId(), new EmployerCompanyRequest(
                                "Entrepots", "Rue Neuve 1", "0403.199.703", "200")))
                .withMessageContaining("numéro d'entreprise");
    }

    @Test
    @DisplayName("Un compte sans aucune demande le dit, sans lever")
    void anaccountWithoutAnyRequestSaysSo() {
        // La page de statut est ouverte à tout utilisateur connecté : elle doit répondre
        // « aucune demande » plutôt que d'échouer pour quelqu'un qui n'en a jamais faite.
        User jobSeeker = fixtures.user("sans-dde", Role.JOBSEEKER);

        assertThatNoException().isThrownBy(() -> employerAccessService.myRequest(jobSeeker.getId()));
        assertThat(employerAccessService.myRequest(jobSeeker.getId()).status()).isNull();
        assertThat(employerAccessService.latestRequest(jobSeeker.getId())).isNull();
    }

    // ------------------------------------------------------------------------------ outils

    private String register() {
        String email = uniqueEmail();
        employerAccessService.registerEmployer(registration(email, COMPANY_NUMBER));
        return email;
    }

    private User registeredUser() {
        return userRepository.findByEmail(register()).orElseThrow();
    }

    private int latestRequestId(User user) {
        EmployerAccessRequest latest = employerAccessService.latestRequest(user.getId());
        return latest.getId();
    }

    private List<Integer> requestIds(String group) {
        return employerAccessService.list(group, PageRequest.of(0, 100))
                .content().stream().map(request -> request.id()).toList();
    }

    private static String uniqueEmail() {
        return "acces-" + SEQUENCE.incrementAndGet() + "@example.be";
    }

    private static EmployerRegisterRequest registration(String email, String companyNumber) {
        return new EmployerRegisterRequest(
                "Dupont", "Jean", email, "MotDePasseSolide1!",
                "Entrepots du Borinage", "Rue de la Gare 1, 7000 Mons", companyNumber, "140");
    }
}
