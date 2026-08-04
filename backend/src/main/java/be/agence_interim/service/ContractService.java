package be.agence_interim.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.ContractResponse;
import be.agence_interim.model.Contract;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.Mission;
import be.agence_interim.model.SignatureStatus;
import be.agence_interim.model.User;
import be.agence_interim.model.WorkReason;
import be.agence_interim.repository.ContractRepository;
import be.agence_interim.repository.DailyScheduleRepository;
import be.agence_interim.repository.MissionRepository;
import jakarta.annotation.PostConstruct;

/**
 * Contrat d'une mission acceptée : génération du document (envoi simulé, cf. analyse),
 * téléchargement par les parties et signature simulée.
 */
@Service
public class ContractService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    private final Path storageDir;
    private final ContractRepository contractRepository;
    private final MissionRepository missionRepository;
    private final DailyScheduleRepository dailyScheduleRepository;

    public ContractService(
            @Value("${app.contract.storage-dir:uploads/contracts}") String storageDir,
            ContractRepository contractRepository,
            MissionRepository missionRepository,
            DailyScheduleRepository dailyScheduleRepository) {
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        this.contractRepository = contractRepository;
        this.missionRepository = missionRepository;
        this.dailyScheduleRepository = dailyScheduleRepository;
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(storageDir);
    }

    /**
     * Génère le contrat d'une mission acceptée : ligne en base, document sur disque et
     * statuts de signature en attente pour les deux parties.
     */
    @Transactional
    public Contract generate(Mission mission, List<DailySchedule> slots) {
        Contract contract = contractRepository.findByMissionId(mission.getId()).orElseGet(Contract::new);
        contract.setMission(mission);
        contract.setGenerationTime(LocalDateTime.now());
        contract.setStatusEmployer(SignatureStatus.PENDING);
        contract.setStatusWorker(SignatureStatus.PENDING);
        contract.setContractFilePath(fileName(mission.getId()));
        Contract saved = contractRepository.save(contract);
        write(saved, mission, slots);
        return saved;
    }

    /** Contrat d'une mission, s'il a déjà été généré. */
    @Transactional(readOnly = true)
    public ContractResponse get(int missionId, int userId, boolean admin) {
        Mission mission = accessibleMission(missionId, userId, admin);
        return ContractResponse.fromEntity(loadContract(mission.getId()));
    }

    /** Document du contrat, accessible aux deux parties et à l'agence. */
    @Transactional(readOnly = true)
    public Resource load(int missionId, int userId, boolean admin) {
        Mission mission = accessibleMission(missionId, userId, admin);
        Contract contract = loadContract(mission.getId());
        Path file = storageDir.resolve(contract.getContractFilePath()).normalize();
        if (!file.startsWith(storageDir) || !Files.exists(file)) {
            throw new NoSuchElementException("Document du contrat introuvable.");
        }
        return new FileSystemResource(file);
    }

    /** Signature simulée du contrat par la partie authentifiée (employeur ou intérimaire). */
    @Transactional
    public ContractResponse sign(int missionId, int userId) {
        Mission mission = accessibleMission(missionId, userId, false);
        Contract contract = loadContract(mission.getId());
        boolean isEmployer = mission.getApplication().getJobOffer().getEmployer().getId() == userId;
        if (isEmployer) {
            requireNotSigned(contract.getStatusEmployer());
            contract.setStatusEmployer(SignatureStatus.SIGNED);
        } else {
            requireNotSigned(contract.getStatusWorker());
            contract.setStatusWorker(SignatureStatus.SIGNED);
        }
        Contract saved = contractRepository.save(contract);
        write(saved, mission, dailyScheduleRepository.findByMissionIdOrderByDateAscStartTimeAsc(mission.getId()));
        return ContractResponse.fromEntity(saved);
    }

    private void requireNotSigned(SignatureStatus status) {
        if (status == SignatureStatus.SIGNED) {
            throw new IllegalArgumentException("Vous avez déjà signé ce contrat.");
        }
    }

    private Contract loadContract(int missionId) {
        return contractRepository.findByMissionId(missionId)
                .orElseThrow(() -> new NoSuchElementException("Aucun contrat n'a encore été généré pour cette mission."));
    }

    /** Charge la mission en vérifiant que l'utilisateur en est une partie (ou l'agence). */
    private Mission accessibleMission(int missionId, int userId, boolean admin) {
        Mission mission = missionRepository.findByIdFetchAll(missionId)
                .orElseThrow(() -> new NoSuchElementException("Mission introuvable."));
        boolean party = mission.getApplication().getJobSeeker().getId() == userId
                || mission.getApplication().getJobOffer().getEmployer().getId() == userId;
        if (!admin && !party) {
            throw new NoSuchElementException("Mission introuvable.");
        }
        return mission;
    }

    private String fileName(int missionId) {
        return "contrat-mission-" + missionId + ".txt";
    }

    /** Écrit (ou réécrit après signature) le document du contrat sur le disque. */
    private void write(Contract contract, Mission mission, List<DailySchedule> slots) {
        User employer = mission.getApplication().getJobOffer().getEmployer();
        User worker = mission.getApplication().getJobSeeker();
        long totalMinutes = slots.stream()
                .mapToLong(slot -> Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes())
                .sum();

        StringBuilder text = new StringBuilder();
        text.append("CONTRAT DE TRAVAIL INTÉRIMAIRE\n");
        text.append("==============================\n\n");
        text.append("Contrat n° ").append(contract.getId())
                .append(" — généré le ").append(TIMESTAMP.format(contract.getGenerationTime())).append("\n\n");

        text.append("ENTREPRISE UTILISATRICE\n");
        text.append("  Société : ").append(employer.getCompanyName() == null ? "—" : employer.getCompanyName())
                .append("\n");
        text.append("  Contact : ").append(employer.getFirstName()).append(' ').append(employer.getLastName())
                .append(" (").append(employer.getEmail()).append(")\n\n");

        text.append("TRAVAILLEUR INTÉRIMAIRE\n");
        text.append("  Nom     : ").append(worker.getFirstName()).append(' ').append(worker.getLastName())
                .append("\n");
        text.append("  Email   : ").append(worker.getEmail()).append("\n\n");

        text.append("CONDITIONS DE LA MISSION\n");
        text.append("  Poste            : ").append(mission.getPosition()).append("\n");
        text.append("  Lieu de travail  : ").append(mission.getWorkplace()).append("\n");
        text.append("  Motif de recours : ").append(reasonLabel(mission.getWorkReason())).append("\n");
        text.append("  Période          : du ").append(DATE.format(mission.getStartDate()))
                .append(" au ").append(DATE.format(mission.getEndDate())).append("\n");
        text.append("  Salaire horaire  : ").append(mission.getHourlyWage()).append(" € brut\n");
        text.append("  Volume total     : ").append(formatDuration(totalMinutes))
                .append(" réparties sur ").append(slots.size()).append(" journée(s)\n\n");

        text.append("HORAIRE DE TRAVAIL\n");
        for (DailySchedule slot : slots) {
            long minutes = Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
            text.append("  ").append(DAY.format(slot.getDate())).append(" : ")
                    .append(TIME.format(slot.getStartTime())).append(" - ").append(TIME.format(slot.getEndTime()))
                    .append(" (").append(formatDuration(minutes)).append(")\n");
        }
        text.append("\n");

        text.append("CONDITIONS PARTICULIÈRES\n");
        text.append("  ").append(mission.getNotes() == null || mission.getNotes().isBlank()
                ? "Aucune." : mission.getNotes()).append("\n\n");

        text.append("SIGNATURES\n");
        text.append("  Entreprise utilisatrice : ").append(signatureLabel(contract.getStatusEmployer())).append("\n");
        text.append("  Travailleur intérimaire : ").append(signatureLabel(contract.getStatusWorker())).append("\n\n");

        text.append("Document généré automatiquement par la plateforme de l'agence d'intérim.\n");
        text.append("Dans le cadre de ce projet, son envoi et sa signature sont simulés.\n");

        Path file = storageDir.resolve(contract.getContractFilePath()).normalize();
        try {
            Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire le contrat de la mission.", e);
        }
    }

    private String reasonLabel(WorkReason reason) {
        return switch (reason) {
            case REPLACEMENT -> "Remplacement d'un travailleur permanent";
            case OVERLOAD -> "Surcroît temporaire de travail";
            case EXCEPTION -> "Travail exceptionnel";
        };
    }

    private String signatureLabel(SignatureStatus status) {
        return status == SignatureStatus.SIGNED ? "signé" : "en attente de signature";
    }

    /** Formate une durée en minutes sous la forme « 7 h 30 ». */
    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? hours + " h" : hours + " h " + String.format("%02d", rest);
    }
}
