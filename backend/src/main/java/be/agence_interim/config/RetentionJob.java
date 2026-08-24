package be.agence_interim.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.Contract;
import be.agence_interim.model.User;
import be.agence_interim.repository.ContractRepository;
import be.agence_interim.repository.MessageRepository;
import be.agence_interim.repository.UserRepository;

/**
 * Politique de conservation des données.
 *
 * <p>Le RGPD ne demande pas seulement de protéger les données : il demande de ne pas les
 * garder plus longtemps que nécessaire. Sans échéance, un CV déposé une fois reste sur le
 * disque indéfiniment, et les conversations d'une candidature close s'accumulent sans
 * qu'aucune règle ne dise pourquoi.
 *
 * <p>Trois durées, une par nature de donnée, parce qu'elles n'obéissent pas aux mêmes
 * raisons :
 * <ul>
 *   <li><strong>Contrats</strong> — cinq ans par défaut. Ce n'est pas un choix de
 *       conception mais une obligation : un contrat de travail intérimaire se conserve,
 *       et l'effacer plus tôt serait une faute d'un autre genre.</li>
 *   <li><strong>Messages</strong> — deux ans. Une conversation de recrutement n'a plus
 *       d'utilité passé ce délai, et son contenu est bien plus bavard qu'un statut de
 *       candidature.</li>
 *   <li><strong>CV des comptes dormants</strong> — deux ans sans connexion. Le document
 *       le plus riche en données personnelles de toute la plateforme n'a pas à survivre à
 *       l'usage qu'on en faisait.</li>
 * </ul>
 *
 * <p><strong>Désactivé par défaut.</strong> Une tâche qui efface ne s'active pas toute
 * seule : l'exploitant la met en marche quand il a arrêté ses durées, et il les inscrit
 * dans la politique de confidentialité — c'est ce document, pas ce fichier, qui engage
 * l'agence vis-à-vis des personnes concernées.
 */
@Component
@ConditionalOnProperty(name = "app.retention.enabled", havingValue = "true")
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final MessageRepository messageRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final Path contractStorageDir;
    private final Path cvStorageDir;
    private final int messageMonths;
    private final int contractYears;
    private final int dormantMonths;

    public RetentionJob(
            MessageRepository messageRepository,
            ContractRepository contractRepository,
            UserRepository userRepository,
            @Value("${app.contract.storage-dir:uploads/contracts}") String contractStorageDir,
            @Value("${app.cv.storage-dir:uploads/cv}") String cvStorageDir,
            @Value("${app.retention.message-months:24}") int messageMonths,
            @Value("${app.retention.contract-years:5}") int contractYears,
            @Value("${app.retention.dormant-account-months:24}") int dormantMonths) {
        this.messageRepository = messageRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.contractStorageDir = Paths.get(contractStorageDir).toAbsolutePath().normalize();
        this.cvStorageDir = Paths.get(cvStorageDir).toAbsolutePath().normalize();
        this.messageMonths = messageMonths;
        this.contractYears = contractYears;
        this.dormantMonths = dormantMonths;
    }

    /** Passage quotidien, à une heure creuse. */
    @Scheduled(cron = "${app.retention.cron:0 30 3 * * *}")
    @Transactional
    public void purge() {
        log.info("Application de la politique de conservation.");
        purgeMessages();
        purgeContracts();
        purgeDormantCvs();
    }

    private void purgeMessages() {
        long removed = messageRepository.deleteBySentTimeBefore(
                LocalDateTime.now().minusMonths(messageMonths));
        if (removed > 0) {
            log.info("Conservation : {} message(s) de plus de {} mois effacé(s).", removed, messageMonths);
        }
    }

    private void purgeContracts() {
        List<Contract> expired = contractRepository.findByGenerationTimeBefore(
                LocalDateTime.now().minusYears(contractYears));
        for (Contract contract : expired) {
            deleteQuietly(contractStorageDir, contract.getContractFilePath());
        }
        if (!expired.isEmpty()) {
            contractRepository.deleteAll(expired);
            log.info("Conservation : {} contrat(s) de plus de {} ans effacé(s).",
                    expired.size(), contractYears);
        }
    }

    private void purgeDormantCvs() {
        List<User> dormant = userRepository.findByLastLoginAtBeforeAndCvFilePathIsNotNull(
                LocalDateTime.now().minusMonths(dormantMonths));
        for (User user : dormant) {
            deleteQuietly(cvStorageDir.resolve(String.valueOf(user.getId())), user.getCvFilePath());
            user.setCvFilePath(null);
        }
        if (!dormant.isEmpty()) {
            userRepository.saveAll(dormant);
            log.info("Conservation : CV effacé pour {} compte(s) sans connexion depuis {} mois.",
                    dormant.size(), dormantMonths);
        }
    }

    /**
     * Efface un fichier sans faire échouer la passe.
     *
     * <p>Le contrôle {@code startsWith} est répété ici bien que les noms viennent de la
     * base : une tâche qui supprime des fichiers doit vérifier elle-même qu'elle reste
     * dans son dossier, sans dépendre de ce qui a écrit la ligne.
     */
    private void deleteQuietly(Path directory, String fileName) {
        if (fileName == null) {
            return;
        }
        try {
            Path file = directory.resolve(fileName).normalize();
            if (file.startsWith(directory)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Conservation : suppression de {} impossible ({}).", fileName, e.getMessage());
        }
    }
}
