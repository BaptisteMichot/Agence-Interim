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
import be.agence_interim.dto.ScheduleEntryResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.Contract;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Mission;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.Unavailability;
import be.agence_interim.model.User;
import be.agence_interim.model.WorkReason;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.ContractRepository;
import be.agence_interim.repository.DailyScheduleRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.MissionRepository;
import be.agence_interim.repository.UnavailabilityRepository;

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

    /** Statuts d'une mission encore en cours de négociation (validation ou réponse attendue). */
    private static final Set<MissionStatus> UNDER_REVIEW = EnumSet.of(
            MissionStatus.PENDING, MissionStatus.APPROVED, MissionStatus.RENEWAL);

    /** Statuts qui attendent une décision de l'intérimaire. */
    private static final Set<MissionStatus> AWAITING_WORKER = EnumSet.of(
            MissionStatus.APPROVED, MissionStatus.RENEWAL);

    private final MissionRepository missionRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final ContractRepository contractRepository;
    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final ContractService contractService;
    private final MailService mailService;
    private final String frontendUrl;

    public MissionService(
            MissionRepository missionRepository,
            DailyScheduleRepository dailyScheduleRepository,
            ContractRepository contractRepository,
            ApplicationRepository applicationRepository,
            JobOfferRepository jobOfferRepository,
            UnavailabilityRepository unavailabilityRepository,
            ContractService contractService,
            MailService mailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.missionRepository = missionRepository;
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.contractRepository = contractRepository;
        this.applicationRepository = applicationRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.unavailabilityRepository = unavailabilityRepository;
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
        requireCompanyDetails(application.getJobOffer().getEmployer());

        Mission mission = new Mission();
        mission.setApplication(application);
        mission.setStatus(MissionStatus.PENDING);
        applyFields(mission, request, application.getJobOffer());
        return saveWithSlots(mission, request.slots());
    }

    /** Corrige une mission refusée par l'agence et la soumet à nouveau. */
    @Transactional
    public MissionResponse update(int employerId, int missionId, MissionRequest request) {
        Mission mission = employerMission(employerId, missionId);
        if (mission.getStatus() != MissionStatus.REFUSED) {
            throw new IllegalArgumentException("Seule une mission refusée par l'agence peut être corrigée.");
        }
        applyFields(mission, request, mission.getApplication().getJobOffer());
        // Un renouvellement corrigé repasse par l'intérimaire : il avait accepté d'autres conditions.
        mission.setStatus(
                mission.getPreviousMission() != null ? MissionStatus.RENEWAL : MissionStatus.PENDING);
        mission.setRefusalReason(null);
        return saveWithSlots(mission, request.slots());
    }

    /**
     * Demande le renouvellement d'une mission confirmée (US19) : une nouvelle mission
     * est créée sur la même candidature et proposée directement à l'intérimaire, qui
     * l'accepte ou la refuse avant la validation de l'agence.
     */
    @Transactional
    public MissionResponse renew(int employerId, int missionId, MissionRequest request) {
        Mission source = employerMission(employerId, missionId);
        if (source.getStatus() != MissionStatus.ACTIVE) {
            throw new IllegalArgumentException("Seule une mission confirmée peut être renouvelée.");
        }
        if (missionRepository.existsByApplicationIdAndStatusIn(
                source.getApplication().getId(), UNDER_REVIEW)) {
            throw new IllegalArgumentException(
                    "Un renouvellement est déjà en cours pour cette mission.");
        }
        if (!request.startDate().isAfter(source.getEndDate())) {
            throw new IllegalArgumentException(
                    "Le renouvellement doit commencer après la fin de la mission en cours.");
        }

        Mission renewal = new Mission();
        renewal.setApplication(source.getApplication());
        renewal.setPreviousMission(source);
        renewal.setStatus(MissionStatus.RENEWAL);
        applyFields(renewal, request, source.getApplication().getJobOffer());
        return saveWithSlots(renewal, request.slots());
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
        return toResponses(missionRepository.findByJobSeekerIdFetchAll(jobSeekerId).stream()
                .filter(this::visibleToJobSeeker)
                .toList());
    }

    @Transactional(readOnly = true)
    public MissionResponse getForJobSeeker(int jobSeekerId, int missionId) {
        return toResponse(jobSeekerMission(jobSeekerId, missionId));
    }

    /**
     * Journées de travail de l'intérimaire sur une période, pour son planning.
     * Seules les missions confirmées y figurent (cf. analyse).
     */
    @Transactional(readOnly = true)
    public List<ScheduleEntryResponse> schedule(int jobSeekerId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }
        return dailyScheduleRepository
                .findForJobSeekerBetween(jobSeekerId, MissionStatus.ACTIVE, from, to)
                .stream().map(ScheduleEntryResponse::fromEntity).toList();
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
        requireWorkerDetails(mission.getApplication().getJobSeeker());
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

    /**
     * Enchaînement commun à la création, à la correction et au renouvellement :
     * sauvegarde, contrôle de chevauchement puis remplacement des journées.
     */
    private MissionResponse saveWithSlots(Mission mission, List<DailySlotRequest> slots) {
        Mission saved = missionRepository.save(mission);
        checkNoOverlap(saved);
        replaceSlots(saved, slots);
        return toResponse(saved);
    }

    /** Mission activée, avec les journées et le contrat déjà chargés par {@link #activate}. */
    private record ActivatedMission(Mission mission, List<DailySchedule> slots, Contract contract) {
    }

    /** Active la mission : contrat généré, offre clôturée et contrat envoyé (envoi simulé). */
    private ActivatedMission activate(Mission mission) {
        mission.setStatus(MissionStatus.ACTIVE);
        Mission saved = missionRepository.save(mission);
        List<DailySchedule> slots =
                dailyScheduleRepository.findByMissionIdOrderByDateAscStartTimeAsc(saved.getId());
        clearConflictingUnavailabilities(saved, slots);
        Contract contract = contractService.generate(saved, slots);

        JobOffer offer = saved.getApplication().getJobOffer();
        if (offer.getStatus() == JobOfferStatus.OPEN) {
            offer.setStatus(JobOfferStatus.CLOSED);
            jobOfferRepository.save(offer);
        }
        sendContract(saved, contract);
        return new ActivatedMission(saved, slots, contract);
    }

    /** Assemble la réponse d'une mission activée sans recharger journées ni contrat. */
    private MissionResponse toResponse(ActivatedMission activated) {
        return MissionResponse.of(activated.mission(), activated.slots(), activated.contract());
    }

    /**
     * Une mission acceptée l'emporte sur les indisponibilités déclarées sur ses
     * journées : on les retire pour garder un planning cohérent (l'intérimaire en
     * est averti avant d'accepter).
     */
    private void clearConflictingUnavailabilities(Mission mission, List<DailySchedule> slots) {
        List<Unavailability> conflicting = unavailabilityRepository
                .findByUserIdAndDateBetweenOrderByDateAscStartTimeAsc(
                        mission.getApplication().getJobSeeker().getId(),
                        mission.getStartDate(), mission.getEndDate())
                .stream()
                .filter(item -> slots.stream().anyMatch(slot -> slot.getDate().equals(item.getDate())
                        && slot.getStartTime().isBefore(item.getEndTime())
                        && item.getStartTime().isBefore(slot.getEndTime())))
                .toList();
        if (!conflicting.isEmpty()) {
            unavailabilityRepository.deleteAll(conflicting);
        }
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

    /**
     * Un contrat ne peut pas être établi sans les mentions légales de l'entreprise
     * utilisatrice : on l'exige avant même de proposer la mission à l'agence.
     */
    private void requireCompanyDetails(User employer) {
        if (isBlank(employer.getCompanyName()) || isBlank(employer.getAddress())
                || isBlank(employer.getCompanyNumber()) || isBlank(employer.getJointCommittee())) {
            throw new IllegalArgumentException(
                    "Complétez la fiche de votre entreprise (adresse, numéro d'entreprise et commission "
                            + "paritaire) avant de proposer une mission : ces mentions figurent sur le contrat.");
        }
    }

    /**
     * De même côté intérimaire, mais au moment de l'acceptation : c'est lui qui peut
     * compléter son profil, et c'est son acceptation qui déclenche le contrat.
     */
    private void requireWorkerDetails(User worker) {
        if (isBlank(worker.getAddress()) || isBlank(worker.getNationalNumber())
                || isBlank(worker.getIban())) {
            throw new IllegalArgumentException(
                    "Complétez votre adresse, votre numéro de registre national et votre numéro "
                            + "de compte dans votre profil avant d'accepter : le contrat ne peut pas "
                            + "être établi ni votre salaire versé sans eux.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
        mission.setDescription(request.description().trim());
        mission.setJointCommittee(request.jointCommittee().trim());
        mission.setHourlyWage(request.hourlyWage());
        mission.setWorkReason(request.workReason());
        mission.setReplacedWorker(replacedWorker(request));
        mission.setNotes(Strings.blankToNull(request.notes()));
    }

    /**
     * Le nom du travailleur remplacé est une mention légale obligatoire lorsque le
     * recours à l'intérim se justifie par un remplacement, et n'a pas de sens sinon.
     */
    private String replacedWorker(MissionRequest request) {
        if (request.workReason() != WorkReason.REPLACEMENT) {
            return null;
        }
        String value = Strings.blankToNull(request.replacedWorker());
        if (value == null) {
            throw new IllegalArgumentException(
                    "Le nom du travailleur remplacé est obligatoire lorsque le motif est un remplacement.");
        }
        return value;
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
            checkBreak(slot);
            DailySchedule entity = new DailySchedule();
            entity.setMission(mission);
            entity.setDate(slot.date());
            entity.setStartTime(slot.startTime());
            entity.setEndTime(slot.endTime());
            entity.setBreakStart(slot.breakStart());
            entity.setBreakEnd(slot.breakEnd());
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

    /**
     * Contrôle la pause fixée par l'employeur. Elle est facultative — l'avertissement
     * légal des 30 minutes au-delà de 6 heures est laissé à l'appréciation de
     * l'employeur — mais elle doit rester cohérente avec l'horaire de la journée.
     */
    private void checkBreak(DailySlotRequest slot) {
        if (slot.breakStart() == null && slot.breakEnd() == null) {
            return;
        }
        if (slot.breakStart() == null || slot.breakEnd() == null) {
            throw new IllegalArgumentException(
                    "La pause du " + slot.date() + " doit avoir une heure de début et une heure de fin.");
        }
        if (!slot.breakEnd().isAfter(slot.breakStart())) {
            throw new IllegalArgumentException(
                    "La fin de la pause doit être postérieure à son début (journée du " + slot.date() + ").");
        }
        if (slot.breakStart().isBefore(slot.startTime()) || slot.breakEnd().isAfter(slot.endTime())) {
            throw new IllegalArgumentException(
                    "La pause du " + slot.date() + " doit être comprise dans l'horaire de la journée.");
        }
        if (WorkTime.paidMinutes(slot.startTime(), slot.endTime(), slot.breakStart(), slot.breakEnd()) <= 0) {
            throw new IllegalArgumentException(
                    "La pause du " + slot.date() + " ne laisse aucun temps de travail rémunéré.");
        }
    }

    /** Refuse de placer deux missions retenues sur la même période pour un même intérimaire. */
    private void checkNoOverlap(Mission mission) {
        boolean overlap = missionRepository.existsOverlapping(
                mission.getApplication().getJobSeeker().getId(), BOOKED,
                mission.getStartDate(), mission.getEndDate(), mission.getId());
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
        if (mission.getApplication().getJobSeeker().getId() != jobSeekerId || !visibleToJobSeeker(mission)) {
            throw new NoSuchElementException("Mission introuvable.");
        }
        return mission;
    }

    /**
     * L'intérimaire ne voit une mission qu'à partir du moment où elle lui est soumise :
     * les échanges entre l'employeur et l'agence (mission provisoire, refus) lui restent
     * invisibles. Exception : un renouvellement qu'il a déjà accepté et qui attend l'agence.
     */
    private boolean visibleToJobSeeker(Mission mission) {
        return switch (mission.getStatus()) {
            case PENDING -> mission.getPreviousMission() != null;
            case REFUSED -> false;
            default -> true;
        };
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
