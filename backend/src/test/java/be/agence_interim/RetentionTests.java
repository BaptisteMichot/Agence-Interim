package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import be.agence_interim.config.RetentionJob;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.CvService;

/**
 * La politique de conservation.
 *
 * <p>Le RGPD ne demande pas seulement de protéger les données : il demande de ne pas les
 * garder plus longtemps que nécessaire. Cette tâche est la seule de l'application qui
 * efface sans que personne ne l'ait demandé, ce qui en fait aussi la plus dangereuse —
 * une borne mal posée détruit des données qu'il fallait garder, et personne ne s'en
 * aperçoit avant d'en avoir besoin.
 *
 * <p>Elle est désactivée par défaut. Ce test l'active pour lui seul, ce qui lui vaut son
 * propre contexte Spring : c'est le prix d'un dispositif qu'on ne veut surtout pas voir
 * tourner par accident.
 */
@SpringBootTest(properties = {
    "app.retention.enabled=true",
    "app.retention.dormant-account-months=24"
})
class RetentionTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private RetentionJob retentionJob;

    @Autowired
    private CvService cvService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Le CV d'un compte dormant est effacé, celui d'un compte actif est gardé")
    void thecvOfADormantAccountIsErased() {
        // Un CV porte un parcours, une adresse, parfois une photo. Le garder indéfiniment
        // pour quelqu'un qui ne revient plus n'a aucune justification ; la borne est le
        // seul moyen de s'en séparer sans attendre une demande.
        User dormant = withCv("dormant", LocalDateTime.now().minusMonths(30));
        User active = withCv("actif", LocalDateTime.now().minusDays(3));

        retentionJob.purge();

        assertThat(userRepository.requireById(dormant.getId()).getCvFilePath()).isNull();
        assertThat(userRepository.requireById(active.getId()).getCvFilePath()).isNotNull();
    }

    @Test
    @DisplayName("Un compte qui ne s'est jamais connecté ne perd pas son CV")
    void anaccountThatNeverLoggedInKeepsItsCv() {
        // Sans date de dernière connexion, rien ne dit que le compte est dormant : c'est
        // aussi la situation d'un compte tout juste créé. Effacer par défaut retirerait
        // son CV à quelqu'un qui vient de le déposer.
        User neverSeen = withCv("jamais", null);

        retentionJob.purge();

        assertThat(userRepository.requireById(neverSeen.getId()).getCvFilePath()).isNotNull();
    }

    @Test
    @DisplayName("La passe se répète sans rien casser")
    void thepassCanRunAgainWithoutBreaking() {
        // Elle tourne toutes les nuits : la seconde exécution retrouve des lignes déjà
        // traitées et des fichiers déjà absents, ce qui ne doit pas l'interrompre.
        withCv("repete", LocalDateTime.now().minusMonths(30));

        retentionJob.purge();
        retentionJob.purge();
    }

    private User withCv(String label, LocalDateTime lastLoginAt) {
        User user = new User();
        user.setEmail(label + "-ret-" + SEQUENCE.incrementAndGet() + "@example.be");
        user.setPassword("$2a$10$peu-importe-aucun-test-ne-se-connecte-ici");
        user.setFirstName("Test");
        user.setLastName("Conservation");
        user.setRole(Role.JOBSEEKER);
        user.setLastLoginAt(lastLoginAt);
        User saved = userRepository.save(user);
        cvService.store(saved.getId(), new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "%PDF-1.7\n".getBytes()));
        return saved;
    }
}
