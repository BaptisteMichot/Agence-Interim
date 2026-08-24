package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;

/**
 * Ce que l'application ne doit <em>pas</em> laisser faire.
 *
 * <p>La suite existante couvrait l'inscription et la connexion, c'est-à-dire l'entrée.
 * Or la propriété qui compte vraiment ici est ailleurs : un employeur ne voit pas les
 * candidatures d'un autre, un intérimaire n'atteint pas l'espace employeur, un jeton
 * révoqué ne rouvre rien. Le code faisait déjà tout cela correctement ; rien ne
 * garantissait qu'un remaniement le préserve.
 *
 * <p>Chaque test suit la même forme : A crée quelque chose, B le demande, et la réponse
 * attendue est <strong>404 et non 403</strong> — répondre « interdit » confirmerait
 * l'existence de la ressource, et permettrait de cartographier la base en balayant des
 * identifiants.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthorizationTests {

    private static final String PASSWORD = "MotDePasseSolide1!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ApiClient anonymous;

    @BeforeEach
    void setUp() {
        anonymous = new ApiClient(port);
    }

    // ------------------------------------------------------------------ cloisonnement des rôles

    @Test
    @DisplayName("Un intérimaire n'atteint aucune route de l'espace employeur")
    void jobSeekerCannotReachEmployerRoutes() throws Exception {
        ApiClient jobSeeker = signedIn("cloison-jobseeker@example.be", Role.JOBSEEKER);

        assertThat(jobSeeker.get("/api/employer/offers").statusCode()).isEqualTo(403);
        assertThat(jobSeeker.get("/api/employer/applications/pending-count").statusCode())
                .isEqualTo(403);
        assertThat(jobSeeker.get("/api/admin/employer-requests").statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Un employeur n'atteint ni le profil intérimaire ni l'espace agence")
    void employerCannotReachJobSeekerOrAdminRoutes() throws Exception {
        ApiClient employer = signedIn("cloison-employer@example.be", Role.EMPLOYER);

        assertThat(employer.get("/api/profile").statusCode()).isEqualTo(403);
        assertThat(employer.get("/api/offers").statusCode()).isEqualTo(403);
        assertThat(employer.get("/api/admin/missions").statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Un employeur en attente de validation n'a aucun droit d'employeur")
    void pendingEmployerHasNoEmployerRights() throws Exception {
        ApiClient pending = signedIn("cloison-pending@example.be", Role.EMPLOYER_PENDING);

        assertThat(pending.get("/api/employer/offers").statusCode()).isEqualTo(403);
        // Sa page de statut, en revanche, doit rester accessible.
        assertThat(pending.get("/api/employer-requests/me").statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Sans session, toute route métier répond 401")
    void anonymousIsRejected() throws Exception {
        assertThat(anonymous.get("/api/profile").statusCode()).isEqualTo(401);
        assertThat(anonymous.get("/api/contracts").statusCode()).isEqualTo(401);
        assertThat(anonymous.get("/api/chat/conversations").statusCode()).isEqualTo(401);
        // Les deux routes publiques le restent.
        assertThat(anonymous.get("/api/agency").statusCode()).isEqualTo(200);
        assertThat(anonymous.get("/api/auth/me").statusCode()).isEqualTo(401);
    }

    // ---------------------------------------------------------- cloisonnement entre comptes

    @Test
    @DisplayName("Un employeur ne voit ni l'offre ni les candidatures d'un autre employeur")
    void employerCannotReadAnotherEmployersOffer() throws Exception {
        ApiClient employerA = signedIn("offre-a@example.be", Role.EMPLOYER);
        ApiClient employerB = signedIn("offre-b@example.be", Role.EMPLOYER);

        HttpResponse<String> created = employerA.post("/api/employer/offers", """
                {
                  "title": "Cariste de nuit",
                  "sector": "LOGISTIQUE",
                  "city": "Liège",
                  "province": "LIEGE",
                  "description": "Préparation de commandes en entrepôt frigorifique.",
                  "experienceTime": "2"
                }
                """);
        assertThat(created.statusCode()).isEqualTo(201);
        int offerId = ApiClient.intField(created.body(), "id");

        // Le propriétaire lit son offre.
        assertThat(employerA.get("/api/employer/offers/" + offerId).statusCode()).isEqualTo(200);

        // L'autre employeur reçoit 404, et non 403 : rien ne lui confirme que l'offre existe.
        assertThat(employerB.get("/api/employer/offers/" + offerId).statusCode()).isEqualTo(404);
        assertThat(employerB.get("/api/employer/offers/" + offerId + "/applications").statusCode())
                .isEqualTo(404);
        assertThat(employerB.put("/api/employer/offers/" + offerId, """
                {
                  "title": "Titre détourné",
                  "sector": "COMMERCE",
                  "city": "Namur",
                  "province": "NAMUR",
                  "description": "Tentative de modification par un tiers."
                }
                """).statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Un intérimaire ne lit ni la mission ni le contrat d'un inconnu")
    void jobSeekerCannotReadForeignMissionOrContract() throws Exception {
        ApiClient jobSeeker = signedIn("mission-tiers@example.be", Role.JOBSEEKER);

        // Aucune mission ni contrat ne le concerne : la réponse ne distingue pas
        // « n'existe pas » de « n'est pas à vous ».
        assertThat(jobSeeker.get("/api/missions/999999").statusCode()).isEqualTo(404);
        assertThat(jobSeeker.get("/api/contracts/999999").statusCode()).isEqualTo(404);
        assertThat(jobSeeker.get("/api/contracts/999999/file").statusCode()).isEqualTo(404);
        assertThat(jobSeeker.get("/api/chat/conversations/999999").statusCode()).isEqualTo(404);
    }

    // -------------------------------------------------------------- révocation des jetons

    @Test
    @DisplayName("Après déconnexion, l'ancien cookie ne rouvre plus la session")
    void logoutRevokesTheToken() throws Exception {
        ApiClient user = signedIn("revocation-logout@example.be", Role.JOBSEEKER);
        String stolenCookie = user.sessionCookie();
        assertThat(stolenCookie).isNotNull();
        assertThat(user.get("/api/profile").statusCode()).isEqualTo(200);

        assertThat(user.post("/api/auth/logout", "{}").statusCode()).isEqualTo(200);

        // Le jeton est rejoué tel qu'un attaquant l'aurait recopié avant la déconnexion.
        ApiClient replay = new ApiClient(port);
        replay.restoreSessionCookie(stolenCookie);
        assertThat(replay.get("/api/profile").statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Un changement de mot de passe invalide les jetons déjà émis")
    void passwordChangeRevokesOtherSessions() throws Exception {
        String email = "revocation-motdepasse@example.be";
        ApiClient phone = signedIn(email, Role.JOBSEEKER);
        String cookieOnOtherDevice = phone.sessionCookie();

        ApiClient laptop = new ApiClient(port);
        assertThat(laptop.login(email, PASSWORD).statusCode()).isEqualTo(200);
        assertThat(laptop.put("/api/account/password", """
                {"currentPassword": "%s", "newPassword": "NouveauMotDePasse1!"}
                """.formatted(PASSWORD)).statusCode()).isEqualTo(200);

        // L'appareil qui a demandé le changement reste connecté...
        assertThat(laptop.get("/api/profile").statusCode()).isEqualTo(200);

        // ...les autres, non.
        ApiClient replay = new ApiClient(port);
        replay.restoreSessionCookie(cookieOnOtherDevice);
        assertThat(replay.get("/api/profile").statusCode()).isEqualTo(401);
    }

    // --------------------------------------------------------------------------- CSRF

    @Test
    @DisplayName("Une écriture sans jeton CSRF est refusée")
    void writeWithoutCsrfTokenIsRejected() throws Exception {
        ApiClient user = signedIn("csrf@example.be", Role.JOBSEEKER);

        // Le cookie de session part quand même : c'est tout le principe du CSRF. Seule
        // l'absence du second jeton, que l'origine tierce ne peut pas lire, arrête la requête.
        assertThat(user.postWithoutCsrf("/api/auth/logout", "{}").statusCode()).isEqualTo(403);
        assertThat(user.post("/api/auth/logout", "{}").statusCode()).isEqualTo(200);
    }

    // ------------------------------------------------------------------- limitation du débit

    @Test
    @DisplayName("Les tentatives de connexion sont plafonnées")
    void repeatedLoginFailuresAreThrottled() throws Exception {
        String email = "brute-force@example.be";
        create(email, Role.JOBSEEKER);
        ApiClient attacker = new ApiClient(port);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(attacker.login(email, "MauvaisMotDePasse1!").statusCode())
                    .as("tentative %d", attempt + 1)
                    .isEqualTo(401);
        }

        HttpResponse<String> blocked = attacker.login(email, "MauvaisMotDePasse1!");
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.headers().firstValue("Retry-After")).isPresent();

        // Le blocage porte bien sur le compte, pas seulement sur le mot de passe saisi :
        // même le bon mot de passe est refusé tant que la fenêtre court.
        assertThat(attacker.login(email, PASSWORD).statusCode()).isEqualTo(429);
    }

    // ------------------------------------------------- validation appliquée sur tous les canaux

    @Test
    @DisplayName("Un message trop long est refusé")
    void oversizedMessageIsRejected() throws Exception {
        ApiClient user = signedIn("message-long@example.be", Role.JOBSEEKER);
        String tooLong = "a".repeat(2_001);

        HttpResponse<String> response = user.post("/api/chat/conversations/999999/messages", """
                {"content": "%s"}
                """.formatted(tooLong));

        // 400 pour la longueur, jamais 404 : la validation du corps précède la recherche
        // de la conversation.
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("2000");
    }

    // ------------------------------------------------------------------------------ outils

    /** Crée un compte du rôle voulu et retourne un client déjà connecté avec. */
    private ApiClient signedIn(String email, Role role) throws Exception {
        create(email, role);
        ApiClient client = new ApiClient(port);
        assertThat(client.login(email, PASSWORD).statusCode())
                .as("connexion de %s", email)
                .isEqualTo(200);
        return client;
    }

    /**
     * Crée le compte directement en base.
     *
     * <p>Passer par l'inscription consommerait le quota de créations par adresse, et
     * l'API n'expose de toute façon pas la création d'un employeur validé ni d'un
     * administrateur.
     */
    private void create(String email, Role role) {
        userRepository.findByEmail(email).ifPresent(existing -> userRepository.delete(existing));
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFirstName("Test");
        user.setLastName("Utilisateur");
        user.setRole(role);
        userRepository.save(user);
    }
}
