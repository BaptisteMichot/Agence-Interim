package be.agence_interim.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.DailySlotRequest;
import be.agence_interim.dto.MissionRequest;
import be.agence_interim.dto.MissionResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.Contract;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Mission;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.ContractRepository;
import be.agence_interim.repository.DailyScheduleRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.MissionRepository;

/**
 * Cycle de vie des missions d'intérim : création de la mission provisoire par
 * l'employeur, validation ou refus par l'agence, acceptation ou refus par
 * l'intérimaire (qui déclenche la génération du contrat et la mise au planning).
 */
@Service
public class MissionService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    /** Statuts pour lesquels la candidature est déjà engagée dans une mission. */
    private static final Set<MissionStatus> IN_PROGRESS = EnumSet.of(
            MissionStatus.PENDING, MissionStatus.APPROVED, MissionStatus.RENEWAL, MissionStatus.ACTIVE);

    /** Statuts qui réservent l'agenda de l'intérimaire. */
    private static final Set<MissionStatus> BOOKED = EnumSet.of(MissionStatus.APPROVED, MissionStatus.ACTIVE);

    /** Statuts qui attendent une décision de l'intérimaire. */
    private static final Set<MissionStatus> AWAITING_WORKER = EnumSet.of(
            MissionStatus.APPROVED, MissionStatus.RENEWAL);

    private final MissionRepository missionRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final ContractRepository contractRepository;
    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final ContractService contractService;
    private final MailService mailService;
    private final String frontendUrl;

    public MissionService(
            MissionRepository missionRepository,
            DailyScheduleRepository dailyScheduleRepository,
            ContractRepository contractRepository,
            ApplicationRepository applicationRepository,
            JobOfferRepository jobOfferRepository,
            ContractService contractService,
            MailService mailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.missionRepository = missionRepository;
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.contractRepository = contractRepository;
        this.applicationRepository = applicationRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.contractService = contractService;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
    }

    // ---------------------------------------------------------------- employeur

    /** Crée la mission provisoire proposée au candidat retenu (statut en attente de l'agence). */
    @Transactional
    public MissionResponse create(int employerId, int applicationId, MissionRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .filter(a -> a.getJobOffer().getEmployer().getId() == employerId)
                .filter(a -> a.getStatus() != ApplicationStatus.CANCELED)
                .orElseThrow(() -> new NoSuchElementException("Candidature introuvable."));
        if (missionRepository.existsByApplicationIdAndStatusIn(applicationId, IN_PROGRESS)) {
            throw new IllegalArgumentException("Une mission est déjà en cours pour cette candidature.");
        }

        Mission mission = new Mission();
        mission.setApplication(application);
        mission.setStatus(MissionStatus.PENDING);
        applyFields(mission, request, application.getJobOffer());
        Mission saved = missionRepository.save(mission);
        checkNoOverlap(saved);
        replaceSlots(saved, request.slots());
        return toResponse(saved);
    }

    /** Corrige une mission refusée par l'agence et la soumet à nouveau. */
    @Transactional
    public MissionResponse update(int employerId, int missionId, MissionRequest request) {
        Mission mission = employerMission(employerId, missionId);
        if (mission.getStatus() != MissionStatus.REFUSED) {
            throw new IllegalArgumentException("Seule une mission refusée par l'agence peut être corrigée.");
        }
        applyFields(mission, request, mission.getApplication().getJobOffer());
        mission.setStatus(MissionStatus.PENDING);
        mission.setRefusalReason(null);
        Mission saved = missionRepository.save(mission);
        checkNoOverlap(saved);
        replaceSlots(saved, request.slots());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listForEmployer(int employerId) {
        return toResponses(missionRepository.findByEmployerIdFetchAll(employerId));
    }

    @Transactional(readOnly = true)
    public MissionResponse getForEmployer(int employerId, int missionId) {
        return toResponse(employerMission(employerId, missionId));
    }

    // -------------------------------------------------------------------- agence

    @Transactional(readOnly = true)
    public List<MissionResponse> listForAdmin() {
        return toResponses(missionRepository.findAllFetchAll());
    }

    @Transactional(readOnly = true)
    public MissionResponse getForAdmin(int missionId) {
        return toResponse(mission(missionId));
    }

    /**
     * Valide une mission provisoire. Un renouvellement a déjà été accepté par
     * l'intérimaire : il devient actif immédiatement. Sinon la mission part en
     * attente de la réponse de l'intérimaire.
     */
    @Transactional
    public MissionResponse validate(int missionId) {
        Mission mission = mission(missionId);
        if (mission.getStatus() != MissionStatus.PENDING) {
            throw new IllegalArgumentException("Seule une mission en attente de validation peut être validée.");
        }
        checkNoOverlap(mission);
        if (mission.getPreviousMission() != null) {
            return toResponse(activate(mission));
        }
        mission.setStatus(MissionStatus.APPROVED);
        return toResponse(missionRepository.save(mission));
    }

    /** Refuse une mission provisoire ; le motif est transmis à l'employeur, qui peut corriger. */
    @Transactional
    public MissionResponse refuse(int missionId, String reason) {
        Mission mission = mission(missionId);
        if (mission.getStatus() != MissionStatus.PENDING) {
            throw new IllegalArgumentException("Seule une mission en attente de validation peut être refusée.");
        }
        mission.setStatus(MissionStatus.REFUSED);
        mission.setRefusalReason(reason);
        return toResponse(missionRepository.save(mission));
    }

    // --------------------------------------------------------------- intérimaire

    @Transactional(readOnly = true)
    public List<MissionResponse> listForJobSeeker(int jobSeekerId) {
        return toResponses(missionRepository.findByJobSeekerIdFetchAll(jobSeekerId));
    }

    @Transactional(readOnly = true)
    public MissionResponse getForJobSeeker(int jobSeekerId, int missionId) {
        return toResponse(jobSeekerMission(jobSeekerId, missionId));
    }

    /** Nombre de missions en attente d'une décision de l'intérimaire (badge du portail). */
    @Transactional(readOnly = true)
    public long decisionCount(int jobSeekerId) {
        return missionRepository.countByApplicationJobSeekerIdAndStatusIn(jobSeekerId, AWAITING_WORKER);
    }

    /**
     * L'intérimaire accepte : une mission validée par l'agence devient active
     * (contrat généré, mission au planning) ; une demande de renouvellement repart
     * en validation à l'agence.
     */
    @Transactional
    public MissionResponse accept(int jobSeekerId, int missionId) {
        Mission mission = jobSeekerMission(jobSeekerId, missionId);
        requireAwaitingWorker(mission);
        if (mission.getStatus() == MissionStatus.RENEWAL) {
            mission.setStatus(MissionStatus.PENDING);
            return toResponse(missionRepository.save(mission));
        }
        checkNoOverlap(mission);
        return toResponse(activate(mission));
    }

    /** L'intérimaire refuse la mission proposée ou la demande de renouvellement. */
    @Transactional
    public MissionResponse decline(int jobSeekerId, int missionId) {
        Mission mission = jobSeekerMission(jobSeekerId, missionId);
        requireAwaitingWorker(mission);
        mission.setStatus(MissionStatus.DECLINED);
        return toResponse(missionRepository.save(mission));
    }

    // ------------------------------------------------------------------ interne

    /** Active la mission : contrat généré, offre clôturée et contrat envoyé (envoi simulé). */
    private Mission activate(Mission mission) {
        mission.setStatus(MissionStatus.ACTIVE);
        Mission saved = missionRepository.save(mission);
        List<DailySchedule> slots =
                dailyScheduleRepository.findByMissionIdOrderByDateAscStartTimeAsc(saved.getId());
        Contract contract = contractService.generate(saved, slots);

        JobOffer offer = saved.getApplication().getJobOffer();
        if (offer.getStatus() == JobOfferStatus.OPEN) {
            offer.setStatus(JobOfferStatus.CLOSED);
            jobOfferRepository.save(offer);
        }
        sendContract(saved, contract);
        return saved;
    }

    private void sendContract(Mission mission, Contract contract) {
        User worker = mission.getApplication().getJobSeeker();
        User employer = mission.getApplication().getJobOffer().getEmployer();
        String period = DATE.format(mission.getStartDate()) + " au " + DATE.format(mission.getEndDate());
        String subject = "Contrat de mission n° " + contract.getId() + " — " + mission.getPosition();
        mailService.send(worker.getEmail(), subject,
                "Bonjour " + worker.getFirstName() + ",\n\n"
                        + "Votre mission « " + mission.getPosition() + " » du " + period
                        + " est confirmée. Le contrat est disponible sur votre portail :\n"
                        + frontendUrl + "/interimaire/missions/" + mission.getId() + "\n\n"
                        + "L'agence d'intérim");
        mailService.send(employer.getEmail(), subject,
                "Bonjour " + employer.getFirstName() + ",\n\n"
                        + worker.getFirstName() + " " + worker.getLastName()
                        + " a accepté la mission « " + mission.getPosition()
                        + " » du " + period + ". Le contrat est disponible sur votre portail :\n"
                        + frontendUrl + "/employeur/missions/" + mission.getId() + "\n\n"
                        + "L'agence d'intérim");
    }

    private void requireAwaitingWorker(Mission mission) {
        if (!AWAITING_WORKER.contains(mission.getStatus())) {
            throw new IllegalArgumentException("Cette mission n'attend pas de réponse de votre part.");
        }
    }

    /** Applique les conditions saisies par l'employeur après validation métier. */
    private void applyFields(Mission mission, MissionRequest request, JobOffer offer) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        checkWage(request.hourlyWage(), offer);
        mission.setStartDate(request.startDate());
        mission.setEndDate(request.endDate());
        mission.setPosition(request.position().trim());
        mission.setWorkplace(request.workplace().trim());
        mission.setHourlyWage(request.hourlyWage());
        mission.setWorkReason(request.workReason());
        mission.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
    }

    /** Le salaire convenu doit rester dans la fourchette annoncée dans l'offre. */
    private void checkWage(BigDecimal wage, JobOffer offer) {
        if (offer.getSalaryMin() != null && wage.compareTo(offer.getSalaryMin()) < 0) {
            throw new IllegalArgumentException(
                    "Le salaire horaire ne peut pas être inférieur au minimum annoncé dans l'offre ("
                            + offer.getSalaryMin() + " €/h).");
        }
        if (offer.getSalaryMax() != null && wage.compareTo(offer.getSalaryMax()) > 0) {
            throw new IllegalArgumentException(
                    "Le salaire horaire ne peut pas être supérieur au maximum annoncé dans l'offre ("
                            + offer.getSalaryMax() + " €/h).");
        }
    }

    /** Remplace les journées travaillées de la mission après validation des créneaux. */
    private void replaceSlots(Mission mission, List<DailySlotRequest> slots) {
        Set<LocalDate> dates = new HashSet<>();
        List<DailySchedule> entities = new ArrayList<>();
        for (DailySlotRequest slot : slots) {
            if (slot.date().isBefore(mission.getStartDate()) || slot.date().isAfter(mission.getEndDate())) {
                throw new IllegalArgumentException(
                        "La journée du " + slot.date() + " est en dehors de la période de la mission.");
            }
            if (!slot.endTime().isAfter(slot.startTime())) {
                throw new IllegalArgumentException(
                        "L'heure de fin doit être postérieure à l'heure de début (journée du " + slot.date() + ").");
            }
            if (!dates.add(slot.date())) {
                throw new IllegalArgumentException("La journée du " + slot.date() + " est renseignée deux fois.");
            }
            DailySchedule entity = new DailySchedule();
            entity.setMission(mission);
            entity.setDate(slot.date());
            entity.setStartTime(slot.startTime());
            entity.setEndTime(slot.endTime());
            entities.add(entity);
        }
        if (!dates.contains(mission.getStartDate()) || !dates.contains(mission.getEndDate())) {
            throw new IllegalArgumentException(
                    "Le premier et le dernier jour de la mission doivent être des journées travaillées.");
        }
        dailyScheduleRepository.deleteByMissionId(mission.getId());
        dailyScheduleRepository.flush();
        dailyScheduleRepository.saveAll(entities);
    }

    /** Refuse de placer deux missions retenues sur la même période pour un même intérimaire. */
    private void checkNoOverlap(Mission mission) {
        boolean overlap = missionRepository
                .findOverlapping(mission.getApplication().getJobSeeker().getId(), BOOKED,
                        mission.getStartDate(), mission.getEndDate())
                .stream()
                .anyMatch(other -> other.getId() != mission.getId());
        if (overlap) {
            throw new IllegalArgumentException(
                    "L'intérimaire est déjà retenu pour une autre mission sur cette période.");
        }
    }

    private Mission mission(int missionId) {
        return missionRepository.findByIdFetchAll(missionId)
                .orElseThrow(() -> new NoSuchElementException("Mission introuvable."));
    }

    private Mission employerMission(int employerId, int missionId) {
        Mission mission = mission(missionId);
        if (mission.getApplication().getJobOffer().getEmployer().getId() != employerId) {
            throw new NoSuchElementException("Mission introuvable.");
        }
        return mission;
    }

    private Mission jobSeekerMission(int jobSeekerId, int missionId) {
        Mission mission = mission(missionId);
        if (mission.getApplication().getJobSeeker().getId() != jobSeekerId) {
            throw new NoSuchElementException("Mission introuvable.");
        }
        return mission;
    }

    private MissionResponse toResponse(Mission mission) {
        return MissionResponse.of(
                mission,
                dailyScheduleRepository.findByMissionIdOrderByDateAscStartTimeAsc(mission.getId()),
                contractRepository.findByMissionId(mission.getId()).orElse(null));
    }

    /** Assemble une liste de missions en chargeant journées et contrats en deux requêtes. */
    private List<MissionResponse> toResponses(List<Mission> missions) {
        if (missions.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = missions.stream().map(mission -> mission.getId()).toList();
        Map<Integer, List<DailySchedule>> slotsByMission =
                dailyScheduleRepository.findByMissionIdInOrderByDateAscStartTimeAsc(ids).stream()
                        .collect(Collectors.groupingBy(slot -> slot.getMission().getId()));
        Map<Integer, Contract> contractsByMission = contractRepository.findByMissionIdIn(ids).stream()
                .collect(Collectors.toMap(contract -> contract.getMission().getId(), Function.identity()));
        return missions.stream()
                .map(mission -> MissionResponse.of(
                        mission,
                        slotsByMission.getOrDefault(mission.getId(), List.of()),
                        contractsByMission.get(mission.getId())))
                .toList();
    }
}
