package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.CvService;

/**
 * Le dépôt du CV.
 *
 * <p>C'est le seul endroit où l'application accepte un fichier venu de l'extérieur et
 * l'écrit sur son disque. Trois choses s'y jouent, dont deux sont des demandes non
 * fonctionnelles de l'analyse : le CV doit être un PDF (6) et ne pas dépasser 5 Mo (5).
 * La troisième n'est écrite nulle part mais découle des deux autres — le nom du fichier
 * est fourni par celui qui l'envoie, et un nom peut contenir un chemin.
 */
@SpringBootTest
class CvTests {

    /** Les quatre octets qui ouvrent tout fichier PDF. */
    private static final byte[] PDF_HEADER = "%PDF-1.7\n".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private CvService cvService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private User worker;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        worker = fixtures.user("cv", Role.JOBSEEKER);
    }

    @Test
    @DisplayName("Un CV déposé se relit et son nom est retenu au profil")
    void adepositedCvCanBeReadBack() {
        String stored = cvService.store(worker.getId(), pdf("mon-cv.pdf"));

        assertThat(stored).isEqualTo("mon-cv.pdf");
        assertThat(userRepository.requireById(worker.getId()).getCvFilePath()).isEqualTo("mon-cv.pdf");
        assertThat(bytesOf(cvService.load(worker.getId()))).startsWith(PDF_HEADER);
    }

    @Test
    @DisplayName("Le format est vérifié sur le contenu du fichier, pas sur son extension")
    void theformatIsCheckedOnTheContentAndNotTheExtension() {
        // Demande non fonctionnelle 6. Se fier au nom laisserait déposer n'importe quoi
        // sous une extension .pdf — un exécutable, une archive — que l'employeur
        // ouvrirait en croyant lire un CV.
        MultipartFile disguised = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "Ceci n'est pas un PDF".getBytes(StandardCharsets.UTF_8));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cvService.store(worker.getId(), disguised))
                .withMessageContaining("PDF");
    }

    @Test
    @DisplayName("Un CV de plus de 5 Mo est refusé")
    void acvLargerThanFiveMegabytesIsRejected() {
        // Demande non fonctionnelle 5. La limite protège le disque, mais surtout
        // l'employeur : un CV se consulte à l'écran, pas en téléchargeant vingt mégaoctets.
        MultipartFile huge = new MockMultipartFile(
                "file", "gros-cv.pdf", "application/pdf", pdfOf((int) CvService.MAX_SIZE_BYTES + 1));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cvService.store(worker.getId(), huge))
                .withMessageContaining("5 Mo");
    }

    @Test
    @DisplayName("Un fichier exactement à la limite passe")
    void afileExactlyAtTheLimitGoesThrough() {
        // « Ne doit pas dépasser 5 Mo » se lit borne comprise : un CV pesant exactement la
        // limite annoncée doit être accepté, sans quoi le message serait faux.
        MultipartFile atTheLimit = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", pdfOf((int) CvService.MAX_SIZE_BYTES));

        assertThatNoException().isThrownBy(() -> cvService.store(worker.getId(), atTheLimit));
    }

    @Test
    @DisplayName("Un fichier vide ou absent est refusé")
    void anemptyOrMissingFileIsRejected() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cvService.store(worker.getId(), null))
                .withMessageContaining("Aucun fichier");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cvService.store(worker.getId(),
                        new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0])))
                .withMessageContaining("Aucun fichier");
    }

    @Test
    @DisplayName("Un nom de fichier contenant un chemin est ramené à son seul nom")
    void afileNameCarryingAPathIsReducedToItsName() {
        // Le nom vient du poste de celui qui dépose et n'est jamais contrôlé par le
        // navigateur. Écrit tel quel, « ../../application.properties » sortirait du dossier
        // de dépôt et écraserait un fichier de l'application.
        String stored = cvService.store(worker.getId(), pdf("../../../etc/passwd.pdf"));

        assertThat(stored).doesNotContain("..").doesNotContain("/").doesNotContain("\\");
        assertThat(bytesOf(cvService.load(worker.getId()))).startsWith(PDF_HEADER);
    }

    @Test
    @DisplayName("Un nom sans extension, ou vide, devient un nom de PDF utilisable")
    void anameWithoutAnExtensionBecomesAUsablePdfName() {
        // Le fichier est ensuite servi en téléchargement : un nom vide ou sans extension
        // donnerait un document que le poste du destinataire ne saurait pas ouvrir.
        assertThat(cvService.store(worker.getId(), pdf("curriculum"))).isEqualTo("curriculum.pdf");
        assertThat(cvService.store(worker.getId(), pdf("@@@"))).endsWith(".pdf");
    }

    @Test
    @DisplayName("Déposer un nouveau CV remplace le précédent, sans laisser l'ancien fichier")
    void anewCvReplacesThePreviousOne() {
        // Sans cela, chaque dépôt laisserait un fichier de plus sur le disque, et le profil
        // ne pointerait plus que vers le dernier : des CV oubliés, que plus rien ne relie
        // à personne mais qui restent lisibles.
        cvService.store(worker.getId(), pdf("ancien.pdf"));

        cvService.store(worker.getId(), pdf("nouveau.pdf"));

        assertThat(userRepository.requireById(worker.getId()).getCvFilePath()).isEqualTo("nouveau.pdf");
        assertThat(cvService.load(worker.getId()).getFilename()).isEqualTo("nouveau.pdf");
    }

    @Test
    @DisplayName("Sans CV déposé, la consultation répond qu'il n'y en a pas")
    void withoutAcvTheReadSaysSo() {
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> cvService.load(worker.getId()))
                .withMessageContaining("Aucun CV");
    }

    @Test
    @DisplayName("Supprimer son CV le retire du profil, et se supprimer deux fois ne lève pas")
    void deletingTheCvRemovesItFromTheProfile() {
        // La suppression est appelée à la clôture du compte, où l'on ignore si un CV avait
        // été déposé : elle doit rester sans effet plutôt que d'interrompre la clôture.
        cvService.store(worker.getId(), pdf("cv.pdf"));

        cvService.delete(worker.getId());

        assertThat(userRepository.requireById(worker.getId()).getCvFilePath()).isNull();
        assertThatNoException().isThrownBy(() -> cvService.delete(worker.getId()));
    }

    @Test
    @DisplayName("Le CV de chacun est rangé chez lui : deux CV de même nom ne se confondent pas")
    void eachCvIsFiledUnderItsOwner() {
        // Les fichiers portent le nom d'origine, et « cv.pdf » est le nom le plus courant
        // qui soit. Sans un dossier par personne, le second dépôt écraserait le premier.
        User other = fixtures.user("cv-voisin", Role.JOBSEEKER);
        cvService.store(worker.getId(), new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", pdfContaining("CV de la premiere personne")));
        cvService.store(other.getId(), new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", pdfContaining("CV de la seconde personne")));

        assertThat(new String(bytesOf(cvService.load(worker.getId())), StandardCharsets.UTF_8))
                .contains("premiere personne");
        assertThat(new String(bytesOf(cvService.load(other.getId())), StandardCharsets.UTF_8))
                .contains("seconde personne");
    }

    // ------------------------------------------------------------------------------ outils

    private static MultipartFile pdf(String fileName) {
        return new MockMultipartFile("file", fileName, "application/pdf", PDF_HEADER);
    }

    /** Un PDF de la taille demandée : les quatre octets de signature, puis du remplissage. */
    private static byte[] pdfOf(int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(PDF_HEADER, 0, bytes, 0, PDF_HEADER.length);
        return bytes;
    }

    private static byte[] pdfContaining(String marker) {
        byte[] suffix = marker.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[PDF_HEADER.length + suffix.length];
        System.arraycopy(PDF_HEADER, 0, bytes, 0, PDF_HEADER.length);
        System.arraycopy(suffix, 0, bytes, PDF_HEADER.length, suffix.length);
        return bytes;
    }

    private static byte[] bytesOf(Resource resource) {
        try (InputStream stream = resource.getInputStream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Le CV déposé est illisible.", e);
        }
    }
}
