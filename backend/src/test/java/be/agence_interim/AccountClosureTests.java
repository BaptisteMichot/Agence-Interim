package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;

/**
 * Ce qu'une clôture de compte emporte, et ce qu'elle laisse.
 *
 * <p>La règle n'est pas symétrique, et c'est ce qui la rend facile à casser sans s'en
 * apercevoir : une offre sans candidature n'appartient qu'à son auteur et disparaît avec
 * lui, une offre qui en a reçu porte l'historique d'autres personnes et doit survivre —
 * clôturée, débarrassée du nom de l'employeur. Un test vaut mieux qu'un commentaire pour
 * tenir cette distinction dans le temps.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AccountClosureTests {

    private static final String PASSWORD = "MotDePasseSolide1!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Aucune offre de l'employeur ne survit en ligne à la clôture de son compte")
    void closingAnEmployerAccountWithdrawsEveryOffer() throws Exception {
        User employer = create("cloture-employeur@example.be", Role.EMPLOYER);
        ApiClient employerClient = signedIn("cloture-employeur@example.be");

        int untouched = createOffer(employerClient, "Cariste sans candidat");
        int applied = createOffer(employerClient, "Magasinier très demandé");

        ApiClient jobSeeker = signedIn(create("cloture-candidat@example.be", Role.JOBSEEKER).getEmail());
        assertThat(jobSeeker.post("/api/offers/" + applied + "/apply", "{}").statusCode())
                .isEqualTo(201);

        assertThat(employerClient.delete("/api/account").statusCode()).isEqualTo(204);

        // L'offre que personne n'a regardée disparaît : rien ne dépendait d'elle.
        assertThat(jobOfferRepository.findById(untouched))
                .as("offre sans candidature")
                .isEmpty();

        // Celle qui a reçu une candidature reste, mais fermée : elle porte l'historique
        // d'un intérimaire, qui n'est pas une donnée de l'employeur.
        assertThat(jobOfferRepository.findById(applied))
                .as("offre avec candidature")
                .get()
                .extracting(offer -> offer.getStatus())
                .isEqualTo(JobOfferStatus.CLOSED);

        // La candidature n'attend plus une réponse que personne ne donnera.
        Application application = applicationRepository.findByJobSeekerIdAndJobOfferId(
                        userRepository.findByEmail("cloture-candidat@example.be").orElseThrow().getId(),
                        applied)
                .orElseThrow();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELED);

        // Le compte survit, anonymisé, parce que l'offre conservée le référence encore.
        User closed = userRepository.findById(employer.getId()).orElseThrow();
        assertThat(closed.getLastName()).isNotEqualTo("Utilisateur");
        assertThat(closed.getEmail()).doesNotContain("cloture-employeur");
        assertThat(closed.getNationalNumber()).isNull();
        assertThat(closed.getIban()).isNull();
        // Les mentions de l'entreprise restent : elles figurent sur les contrats et
        // désignent une personne morale.
        assertThat(closed.getCompanyName()).isEqualTo("Entreprise de test");
    }

    @Test
    @DisplayName("Un employeur qui n'a jamais rien publié est réellement supprimé")
    void closingAnEmptyEmployerAccountDeletesIt() throws Exception {
        User employer = create("cloture-vide@example.be", Role.EMPLOYER);

        assertThat(signedIn("cloture-vide@example.be").delete("/api/account").statusCode())
                .isEqualTo(204);

        assertThat(userRepository.findById(employer.getId())).isEmpty();
    }

    @Test
    @DisplayName("Un compte de l'agence ne se clôture pas depuis cet écran")
    void agencyAccountCannotCloseItself() throws Exception {
        create("cloture-admin@example.be", Role.ADMIN);

        HttpResponse<String> response = signedIn("cloture-admin@example.be").delete("/api/account");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("agence");
    }

    // ------------------------------------------------------------------------------ outils

    private int createOffer(ApiClient client, String title) throws Exception {
        HttpResponse<String> created = client.post("/api/employer/offers", """
                {
                  "title": "%s",
                  "sector": "LOGISTIQUE",
                  "city": "Mons",
                  "province": "HAINAUT",
                  "description": "Offre créée pour éprouver la clôture de compte.",
                  "experienceTime": "1"
                }
                """.formatted(title));
        assertThat(created.statusCode()).as("création de l'offre « %s »", title).isEqualTo(201);
        return ApiClient.intField(created.body(), "id");
    }

    private ApiClient signedIn(String email) throws Exception {
        ApiClient client = new ApiClient(port);
        assertThat(client.login(email, PASSWORD).statusCode())
                .as("connexion de %s", email)
                .isEqualTo(200);
        return client;
    }

    private User create(String email, Role role) {
        userRepository.findByEmail(email).ifPresent(existing -> userRepository.delete(existing));
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFirstName("Test");
        user.setLastName("Utilisateur");
        user.setRole(role);
        if (role == Role.EMPLOYER) {
            user.setCompanyName("Entreprise de test");
            user.setCompanyNumber("0454.460.440");
            user.setJointCommittee("111");
            user.setAddress("Rue de la Gare 1, 7000 Mons");
        }
        return userRepository.save(user);
    }
}
