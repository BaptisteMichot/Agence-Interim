package be.agence_interim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import be.agence_interim.dto.DailySlotRequest;
import be.agence_interim.dto.MissionRequest;
import be.agence_interim.model.Application;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Province;
import be.agence_interim.model.Role;
import be.agence_interim.model.Sector;
import be.agence_interim.model.User;
import be.agence_interim.model.WorkReason;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;

/**
 * Le décor d'une mission : un employeur en règle, un intérimaire au profil complet et
 * une offre. La candidature qui les relie ne se monte qu'à la demande.
 *
 * <p>Une mission ne s'atteint jamais seule — il faut au minimum quatre lignes en base
 * avant de pouvoir en créer une. Rassembler ce décor ici laisse les classes de test ne
 * dire que la règle qu'elles éprouvent, et garantit que les deux le font sur les mêmes
 * données de départ : une différence de décor entre deux classes fait diverger leurs
 * conclusions sans qu'on voie pourquoi.
 *
 * <p>Chaque instance tire ses propres adresses email. La base de test est partagée par
 * toute la suite et l'adresse est unique : deux jeux de données identiques se
 * marcheraient dessus.
 */
class MissionFixtures {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /** Fourchette annoncée dans l'offre ; le salaire de la mission doit y tenir. */
    static final BigDecimal SALARY_MIN = new BigDecimal("13.00");
    static final BigDecimal SALARY_MAX = new BigDecimal("18.00");

    /** Salaire convenu par défaut, au milieu de la fourchette. */
    static final BigDecimal WAGE = new BigDecimal("15.00");

    private final UserRepository userRepository;
    private final JobOfferRepository jobOfferRepository;
    private final ApplicationRepository applicationRepository;

    final User employer;
    final User worker;
    final JobOffer offer;

    /**
     * Période de la mission, placée dans le futur : une mission passée tomberait dans
     * l'historique et sortirait des sections « en cours », selon le jour où la suite est
     * exécutée.
     */
    final LocalDate start = LocalDate.now().plusDays(30);
    final LocalDate end = start.plusDays(4);

    MissionFixtures(
            UserRepository userRepository,
            JobOfferRepository jobOfferRepository,
            ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.applicationRepository = applicationRepository;
        this.employer = employer("employeur");
        this.worker = worker("interimaire");
        this.offer = offer();
    }

    // ------------------------------------------------------------------------ parties

    private User admin;

    /**
     * L'agence : elle n'apparaît dans la mission que par l'identifiant repris au journal
     * d'audit, aucune règle métier ne dépend de quel employé de l'agence tranche. Un seul
     * compte suffit donc pour tout un test, et il n'est créé que s'il sert.
     */
    User admin() {
        if (admin == null) {
            admin = user("agence", Role.ADMIN);
        }
        return admin;
    }

    /** Un employeur dont la fiche entreprise porte toutes les mentions du contrat. */
    User employer(String label) {
        User created = user(label, Role.EMPLOYER);
        created.setCompanyName("Entrepots du Borinage");
        created.setCompanyNumber("0403.199.702");
        created.setJointCommittee("140");
        created.setAddress("Rue de la Gare 1, 7000 Mons");
        return userRepository.save(created);
    }

    /** Un intérimaire dont le profil est complet : il peut donc accepter une mission. */
    User worker(String label) {
        User created = user(label, Role.JOBSEEKER);
        created.setAddress("Rue Neuve 12, 7000 Mons");
        created.setNationalNumber("85.07.30-033.28");
        created.setIban("BE68 5390 0754 7034");
        return userRepository.save(created);
    }

    /**
     * Un compte du rôle demandé, dont l'adresse email est unique.
     *
     * <p>L'étiquette est raccourcie parce que la colonne fait 35 caractères : un libellé
     * de test un peu bavard fait échouer l'insertion sur une contrainte de longueur, et
     * l'erreur n'a alors plus rien à voir avec ce que le test cherchait à établir.
     */
    User user(String label, Role role) {
        String shortLabel = label.length() > 12 ? label.substring(0, 12) : label;
        User created = new User();
        created.setEmail(shortLabel + "-" + SEQUENCE.incrementAndGet() + "@example.be");
        created.setPassword("$2a$10$peu-importe-aucun-test-ne-se-connecte-ici");
        created.setFirstName("Test");
        created.setLastName("Mission");
        created.setRole(role);
        return userRepository.save(created);
    }

    User save(User user) {
        return userRepository.save(user);
    }

    // ------------------------------------------------------- offre et candidature

    private JobOffer offer() {
        JobOffer created = new JobOffer();
        created.setEmployer(employer);
        created.setTitle("Cariste");
        created.setSector(Sector.LOGISTIQUE);
        created.setCity("Mons");
        created.setProvince(Province.HAINAUT);
        created.setDescription("Poste en entrepôt, horaire de jour.");
        created.setPublishedAt(LocalDateTime.now());
        created.setVehicleMandatory(false);
        created.setSalaryMin(SALARY_MIN);
        created.setSalaryMax(SALARY_MAX);
        return jobOfferRepository.save(created);
    }

    private Application application;

    /**
     * La candidature de l'intérimaire sur l'offre, créée au premier appel seulement.
     *
     * <p>Elle n'est pas montée d'office : les tests de l'offre et de la candidature
     * partent d'une offre vierge, et une candidature déjà en place y ferait echouer le
     * premier « postuler » comme un doublon.
     */
    Application application() {
        if (application == null) {
            application = applicationOf(worker);
        }
        return application;
    }

    Application applicationOf(User jobSeeker) {
        Application created = new Application();
        created.setJobSeeker(jobSeeker);
        created.setJobOffer(offer);
        created.setApplicationTime(LocalDateTime.now());
        return applicationRepository.save(created);
    }

    Application save(Application application) {
        return applicationRepository.save(application);
    }

    /** Statut de l'offre relu en base : la mission le fait changer sans passer par elle. */
    JobOfferStatus offerStatus() {
        return jobOfferRepository.findById(offer.getId()).orElseThrow().getStatus();
    }

    void closeOffer() {
        offer.setStatus(JobOfferStatus.CLOSED);
        jobOfferRepository.save(offer);
    }

    // ------------------------------------------------------------- conditions saisies

    /** Mission valide : toutes les journées de la période sont travaillées, 8 h payées. */
    MissionRequest request() {
        return request(start, end);
    }

    MissionRequest request(LocalDate from, LocalDate to) {
        return request(from, to, WAGE, WorkReason.OVERLOAD, null, workingDays(from, to));
    }

    MissionRequest requestPaying(BigDecimal wage) {
        return request(start, end, wage, WorkReason.OVERLOAD, null, workingDays(start, end));
    }

    MissionRequest requestWorking(List<DailySlotRequest> slots) {
        return requestWorking(start, end, slots);
    }

    MissionRequest requestWorking(LocalDate from, LocalDate to, List<DailySlotRequest> slots) {
        return request(from, to, WAGE, WorkReason.OVERLOAD, null, slots);
    }

    MissionRequest requestReplacing(String replacedWorker) {
        return request(start, end, WAGE, WorkReason.REPLACEMENT, replacedWorker, workingDays(start, end));
    }

    private MissionRequest request(
            LocalDate from,
            LocalDate to,
            BigDecimal wage,
            WorkReason reason,
            String replacedWorker,
            List<DailySlotRequest> slots) {
        return new MissionRequest(
                from, to, "Cariste", "Rue de l'Entrepot 4, 7000 Mons",
                "Chargement et déchargement de palettes au transpalette électrique.",
                "140", wage, null, reason, replacedWorker, null, slots);
    }

    /** Une journée par jour de la période, week-ends compris. */
    static List<DailySlotRequest> workingDays(LocalDate from, LocalDate to) {
        List<DailySlotRequest> slots = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            slots.add(day(date));
        }
        return slots;
    }

    /** Journée type : 08:00-16:30 avec une pause de midi, soit 8 h rémunérées. */
    static DailySlotRequest day(LocalDate date) {
        return day(date, LocalTime.of(8, 0), LocalTime.of(16, 30), LocalTime.of(12, 0), LocalTime.of(12, 30));
    }

    static DailySlotRequest day(
            LocalDate date, LocalTime from, LocalTime to, LocalTime breakStart, LocalTime breakEnd) {
        return new DailySlotRequest(date, from, to, breakStart, breakEnd);
    }
}
