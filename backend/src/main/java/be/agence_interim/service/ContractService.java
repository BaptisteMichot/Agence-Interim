package be.agence_interim.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.config.AgencyProperties;
import be.agence_interim.dto.ContractResponse;
import be.agence_interim.model.Contract;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.Mission;
import be.agence_interim.model.SignatureStatus;
import be.agence_interim.model.User;
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

    private final Path storageDir;
    private final ContractRepository contractRepository;
    private final MissionRepository missionRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final AgencyProperties agency;
    private final SigningCodeService signingCodeService;
    private final MailService mailService;

    public ContractService(
            @Value("${app.contract.storage-dir:uploads/contracts}") String storageDir,
            ContractRepository contractRepository,
            MissionRepository missionRepository,
            DailyScheduleRepository dailyScheduleRepository,
            AgencyProperties agency,
            SigningCodeService signingCodeService,
            MailService mailService) {
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        this.contractRepository = contractRepository;
        this.missionRepository = missionRepository;
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.agency = agency;
        this.signingCodeService = signingCodeService;
        this.mailService = mailService;
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
        contract.setContractFilePath("contrat-mission-" + mission.getId() + ".pdf");
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

    /**
     * Envoie au signataire le code à usage unique qui confirmera sa signature. Le code
     * part sur l'adresse email de son compte : le saisir prouve qu'il en a le contrôle.
     */
    @Transactional(readOnly = true)
    public void requestSigningCode(int missionId, int userId) {
        SigningContext context = signingContext(missionId, userId);
        Mission mission = context.mission();
        Contract contract = context.contract();

        User signer = context.isEmployer()
                ? mission.getApplication().getJobOffer().getEmployer()
                : mission.getApplication().getJobSeeker();
        String code = signingCodeService.generate(contract.getId(), userId);
        mailService.send(signer.getEmail(),
                "Code de signature du contrat n° " + contract.getId(),
                "Bonjour " + signer.getFirstName() + ",\n\n"
                        + "Votre code de signature est : " + code + "\n"
                        + "Il est valable " + signingCodeService.getValidityMinutes() + " minutes.\n\n"
                        + "Saisissez-le sur la plateforme pour signer le contrat de la mission « "
                        + mission.getPosition() + " ».\n\n"
                        + "L'agence d'intérim");
    }

    /**
     * Signature du contrat par la partie authentifiée, après vérification du code reçu
     * par email. Le document est régénéré pour y faire figurer la signature.
     */
    @Transactional
    public ContractResponse sign(int missionId, int userId, String code) {
        SigningContext context = signingContext(missionId, userId);
        Mission mission = context.mission();
        Contract contract = context.contract();
        signingCodeService.verify(contract.getId(), userId, code);

        LocalDateTime now = LocalDateTime.now();
        if (context.isEmployer()) {
            contract.setStatusEmployer(SignatureStatus.SIGNED);
            contract.setEmployerSignedAt(now);
        } else {
            contract.setStatusWorker(SignatureStatus.SIGNED);
            contract.setWorkerSignedAt(now);
        }
        Contract saved = contractRepository.save(contract);
        write(saved, mission, dailyScheduleRepository.findByMissionIdOrderByDateAscStartTimeAsc(mission.getId()));
        return ContractResponse.fromEntity(saved);
    }

    /** Données communes aux deux étapes de la signature (demande du code, puis signature). */
    private record SigningContext(Mission mission, Contract contract, boolean isEmployer) {
    }

    /** Charge la mission et son contrat pour l'une des parties, et vérifie qu'elle n'a pas déjà signé. */
    private SigningContext signingContext(int missionId, int userId) {
        Mission mission = accessibleMission(missionId, userId, false);
        Contract contract = loadContract(mission.getId());
        boolean isEmployer = isEmployer(mission, userId);
        requireNotSigned(isEmployer ? contract.getStatusEmployer() : contract.getStatusWorker());
        return new SigningContext(mission, contract, isEmployer);
    }

    private boolean isEmployer(Mission mission, int userId) {
        return mission.getApplication().getJobOffer().getEmployer().getId() == userId;
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

    /** Écrit (ou réécrit après signature) le document du contrat sur le disque. */
    private void write(Contract contract, Mission mission, List<DailySchedule> slots) {
        Path file = storageDir.resolve(contract.getContractFilePath()).normalize();
        try {
            Files.write(file, new ContractDocument(agency).render(contract, mission, slots));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire le contrat de la mission.", e);
        }
    }
}
