package be.agence_interim.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.Conversation;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.DegreeUser;
import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.model.EmployerAccessStatus;
import be.agence_interim.model.Experience;
import be.agence_interim.model.FavoriteJobOffer;
import be.agence_interim.model.Formation;
import be.agence_interim.model.FormationStatus;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.LanguageUser;
import be.agence_interim.model.Message;
import be.agence_interim.model.Mission;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.Province;
import be.agence_interim.model.Role;
import be.agence_interim.model.Sector;
import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.SkillUser;
import be.agence_interim.model.User;
import be.agence_interim.model.WorkReason;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.ConversationRepository;
import be.agence_interim.repository.DailyScheduleRepository;
import be.agence_interim.repository.DegreeRepository;
import be.agence_interim.repository.DegreeUserRepository;
import be.agence_interim.repository.EmployerAccessRequestRepository;
import be.agence_interim.repository.ExperienceRepository;
import be.agence_interim.repository.FavoriteJobOfferRepository;
import be.agence_interim.repository.FormationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.LanguageJobOfferRepository;
import be.agence_interim.repository.LanguageRepository;
import be.agence_interim.repository.LanguageUserRepository;
import be.agence_interim.repository.MessageRepository;
import be.agence_interim.repository.MissionRepository;
import be.agence_interim.repository.SkillJobOfferRepository;
import be.agence_interim.repository.SkillRepository;
import be.agence_interim.repository.SkillUserRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ContractService;

/**
 * Jeu de données de démonstration, activé par {@code app.demo-data.enabled}.
 *
 * <p>Il existe pour une raison précise : une liste ne montre sa pagination qu'au-delà
 * de dix éléments. Les volumes ci-dessous sont donc choisis pour que chaque liste de
 * l'application dépasse ce seuil, sections comprises.
 *
 * <p>Les comptes {@code test@employer.com} et {@code test@jobseeker.com} peuvent déjà
 * exister : ils sont alors réutilisés tels quels, et seules les mentions manquantes
 * indispensables à une mission (numéro d'entreprise, registre national, IBAN…) sont
 * complétées. Aucune valeur déjà saisie n'est écrasée.
 *
 * <p>Le garde-fou porte sur {@code demo1@jobseeker.com}, un compte que seul ce seeder
 * crée : une fois le jeu en place, un redémarrage ne le rejoue pas.
 *
 * <p>Les contrats des missions acceptées sont produits par {@code ContractService}, donc
 * avec leur PDF sur disque et les deux signatures en attente : le parcours de signature
 * reste entièrement jouable sur les données de démonstration.
 *
 * <p>Les entités sont créées directement par les repositories, sans passer par les
 * services : les règles métier (chevauchement de missions, clôture automatique de
 * l'offre, une seule mission en cours par candidature) sont respectées par construction
 * — chaque mission a sa propre candidature et sa propre période.
 */
@Component
@Order(2) // Après DataSeeder : les référentiels et le compte admin doivent exister.
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    /** Même mot de passe que l'administrateur, pour n'en retenir qu'un en démonstration. */
    private static final String PASSWORD = "AVAfur!ousPUBG03";

    private static final String EMPLOYER_EMAIL = "test@employer.com";
    private static final String JOBSEEKER_EMAIL = "test@jobseeker.com";

    /** Sept secteurs pour huit lieux : les deux listes ne se répètent pas au même rythme. */
    private static final Sector[] SECTORS = {
        Sector.LOGISTIQUE, Sector.CONSTRUCTION, Sector.HORECA, Sector.NETTOYAGE,
        Sector.COMMERCE, Sector.INDUSTRIE, Sector.SANTE
    };

    /** Ville et province vont de pair : une offre ne peut pas être à Namur en Hainaut. */
    private record Place(String city, Province province) {
    }

    private static final Place[] PLACES = {
        new Place("Liège", Province.LIEGE),
        new Place("Bruxelles", Province.BRUXELLES),
        new Place("Namur", Province.NAMUR),
        new Place("Charleroi", Province.HAINAUT),
        new Place("Verviers", Province.LIEGE),
        new Place("Mons", Province.HAINAUT),
        new Place("Wavre", Province.BRABANT_WALLON),
        new Place("Arlon", Province.LUXEMBOURG)
    };
    private static final String[] POSITIONS = {
        "Cariste", "Manutentionnaire", "Préparateur de commandes", "Magasinier",
        "Agent d'accueil", "Employé polyvalent", "Aide-monteur", "Agent de nettoyage"
    };

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployerAccessRequestRepository accessRequestRepository;
    private final JobOfferRepository jobOfferRepository;
    private final ApplicationRepository applicationRepository;
    private final FavoriteJobOfferRepository favoriteRepository;
    private final MissionRepository missionRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SkillRepository skillRepository;
    private final SkillUserRepository skillUserRepository;
    private final SkillJobOfferRepository skillJobOfferRepository;
    private final DegreeRepository degreeRepository;
    private final DegreeUserRepository degreeUserRepository;
    private final LanguageRepository languageRepository;
    private final LanguageUserRepository languageUserRepository;
    private final LanguageJobOfferRepository languageJobOfferRepository;
    private final ExperienceRepository experienceRepository;
    private final FormationRepository formationRepository;
    private final ContractService contractService;

    public DemoDataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmployerAccessRequestRepository accessRequestRepository,
            JobOfferRepository jobOfferRepository,
            ApplicationRepository applicationRepository,
            FavoriteJobOfferRepository favoriteRepository,
            MissionRepository missionRepository,
            DailyScheduleRepository dailyScheduleRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            SkillRepository skillRepository,
            SkillUserRepository skillUserRepository,
            SkillJobOfferRepository skillJobOfferRepository,
            DegreeRepository degreeRepository,
            DegreeUserRepository degreeUserRepository,
            LanguageRepository languageRepository,
            LanguageUserRepository languageUserRepository,
            LanguageJobOfferRepository languageJobOfferRepository,
            ExperienceRepository experienceRepository,
            FormationRepository formationRepository,
            ContractService contractService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessRequestRepository = accessRequestRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.applicationRepository = applicationRepository;
        this.favoriteRepository = favoriteRepository;
        this.missionRepository = missionRepository;
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.skillRepository = skillRepository;
        this.skillUserRepository = skillUserRepository;
        this.skillJobOfferRepository = skillJobOfferRepository;
        this.degreeRepository = degreeRepository;
        this.degreeUserRepository = degreeUserRepository;
        this.languageRepository = languageRepository;
        this.languageUserRepository = languageUserRepository;
        this.languageJobOfferRepository = languageJobOfferRepository;
        this.experienceRepository = experienceRepository;
        this.formationRepository = formationRepository;
        this.contractService = contractService;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        // Toute la pose se fait dans une transaction : interrompue, elle ne laisse pas
        // un jeu de données à moitié construit que le garde-fou empêcherait de refaire.
        if (userRepository.findByEmail("demo1@jobseeker.com").isPresent()) {
            return;
        }

        User employer = employerAccount();
        User jobSeeker = jobSeekerAccount();
        List<JobOffer> openOffers = createBrowsableOffers(employer);
        createFavoritesAndApplications(jobSeeker, openOffers);
        createMissions(employer, jobSeeker);
        createOtherCandidates(employer, openOffers.get(0));
        createPendingEmployerRequests();
    }

    // ----------------------------------------------------------------- comptes

    /**
     * Le compte employeur de démonstration, créé s'il n'existe pas. S'il existe, seules
     * les mentions légales absentes sont complétées : sans elles, l'application refuse
     * la création d'une mission.
     */
    private User employerAccount() {
        User employer = userRepository.findByEmail(EMPLOYER_EMAIL).orElseGet(() -> {
            User created = newUser(EMPLOYER_EMAIL, "Emma", "Ployeur", Role.EMPLOYER);
            created.setCompanyName("Entreprise Démo SPRL");
            User saved = userRepository.save(created);

            EmployerAccessRequest accepted = new EmployerAccessRequest();
            accepted.setUser(saved);
            accepted.setRequestDate(LocalDate.now().minusDays(60));
            accepted.setStatus(EmployerAccessStatus.ACCEPTED);
            accessRequestRepository.save(accepted);
            return saved;
        });

        employer.setRole(Role.EMPLOYER);
        fillIfBlank(employer.getCompanyName(), employer::setCompanyName, "Entreprise Démo SPRL");
        fillIfBlank(employer.getAddress(), employer::setAddress, "Rue de l'Entreprise 12, 4000 Liège, Belgique");
        // Numéro BCE valide (clé modulo 97), sinon la fiche entreprise reste incomplète.
        fillIfBlank(employer.getCompanyNumber(), employer::setCompanyNumber, "0987.654.394");
        fillIfBlank(employer.getJointCommittee(), employer::setJointCommittee, "200");
        return userRepository.save(employer);
    }

    /** Le compte intérimaire de démonstration, avec un profil assez fourni pour le matching. */
    private User jobSeekerAccount() {
        User jobSeeker = userRepository.findByEmail(JOBSEEKER_EMAIL).orElseGet(() -> {
            User created = newUser(JOBSEEKER_EMAIL, "Jean", "Térim", Role.JOBSEEKER);
            created.setBirthdate(LocalDate.of(1995, 4, 12));
            created.setHasVehicle(true);
            return userRepository.save(created);
        });

        if (jobSeeker.getHasVehicle() == null) {
            jobSeeker.setHasVehicle(true);
        }
        fillIfBlank(jobSeeker.getAddress(), jobSeeker::setAddress, "Rue des Candidats 9, 4000 Liège, Belgique");
        // Registre national et IBAN valides : sans eux, aucune mission ne peut être acceptée.
        fillIfBlank(jobSeeker.getNationalNumber(), jobSeeker::setNationalNumber, "95.04.12-123.85");
        fillIfBlank(jobSeeker.getIban(), jobSeeker::setIban, "BE68 5390 0754 7034");
        User saved = userRepository.save(jobSeeker);

        addSkills(saved);
        addDegree(saved);
        addLanguages(saved);
        addExperiences(saved);
        addFormations(saved);
        return saved;
    }

    /** N'écrit la valeur de démonstration que si l'utilisateur n'a rien saisi. */
    private void fillIfBlank(String current, Consumer<String> setter, String fallback) {
        if (current == null || current.isBlank()) {
            setter.accept(fallback);
        }
    }

    private User newUser(String email, String firstName, String lastName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        return user;
    }

    // ------------------------------------------------------------- profil type

    private void addSkills(User jobSeeker) {
        record Owned(String name, SkillLevel level) {
        }
        List<Owned> owned = List.of(
                new Owned("Cariste", SkillLevel.AVANCE),
                new Owned("Logistique", SkillLevel.AVANCE),
                new Owned("Gestion de stock", SkillLevel.INTERMEDIAIRE),
                new Owned("Sécurité", SkillLevel.INTERMEDIAIRE),
                new Owned("Nettoyage", SkillLevel.DEBUTANT),
                new Owned("Informatique", SkillLevel.DEBUTANT));
        for (Owned entry : owned) {
            skillRepository.findFirstByNameIgnoreCaseAndIsGlobalTrue(entry.name()).ifPresent(skill -> {
                if (skillUserRepository.existsByUserIdAndSkillId(jobSeeker.getId(), skill.getId())) {
                    return;
                }
                SkillUser link = new SkillUser();
                link.setUser(jobSeeker);
                link.setSkill(skill);
                link.setLevel(entry.level());
                skillUserRepository.save(link);
            });
        }
    }

    private void addDegree(User jobSeeker) {
        degreeRepository.findFirstByTypeAndSectionIgnoreCaseAndIsGlobalTrue(DegreeType.BACHELIER, "Construction")
                .ifPresent(degree -> {
                    if (degreeUserRepository.existsByUserIdAndDegreeId(jobSeeker.getId(), degree.getId())) {
                        return;
                    }
                    DegreeUser link = new DegreeUser();
                    link.setUser(jobSeeker);
                    link.setDegree(degree);
                    link.setInstitution("HEPL Liège");
                    link.setGraduationYear(2016);
                    degreeUserRepository.save(link);
                });
    }

    private void addLanguages(User jobSeeker) {
        record Spoken(String name, LanguageLevel level) {
        }
        List<Spoken> spoken = List.of(
                new Spoken("Français", LanguageLevel.C2),
                new Spoken("Anglais", LanguageLevel.B2),
                new Spoken("Néerlandais", LanguageLevel.A2));
        for (Spoken entry : spoken) {
            language(entry.name()).ifPresent(language -> {
                if (languageUserRepository.existsByUserIdAndLanguageId(jobSeeker.getId(), language.getId())) {
                    return;
                }
                LanguageUser link = new LanguageUser();
                link.setUser(jobSeeker);
                link.setLanguage(language);
                link.setLevel(entry.level());
                languageUserRepository.save(link);
            });
        }
    }

    /** La liste des langues est fixe et courte : une recherche par nom en mémoire suffit. */
    private Optional<Language> language(String name) {
        return languageRepository.findAllByOrderByNameAsc().stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private void addExperiences(User jobSeeker) {
        if (!experienceRepository.findByUserIdOrderByStartDateDesc(jobSeeker.getId()).isEmpty()) {
            return;
        }
        experienceRepository.save(experience(
                jobSeeker, "Transports Delvaux", "Cariste",
                LocalDate.now().minusYears(6), LocalDate.now().minusYears(3)));
        experienceRepository.save(experience(
                jobSeeker, "Entrepôts du Sart", "Magasinier",
                LocalDate.now().minusYears(3), null));
    }

    private Experience experience(User user, String company, String position, LocalDate from, LocalDate to) {
        Experience experience = new Experience();
        experience.setUser(user);
        experience.setCompanyName(company);
        experience.setPosition(position);
        experience.setStartDate(from);
        experience.setEndDate(to);
        return experience;
    }

    private void addFormations(User jobSeeker) {
        if (!formationRepository.findByUserIdOrderByStartDateDesc(jobSeeker.getId()).isEmpty()) {
            return;
        }
        Formation certificate = new Formation();
        certificate.setUser(jobSeeker);
        certificate.setTitle("Brevet cariste (chariot élévateur)");
        certificate.setInstitution("Forem");
        certificate.setStartDate(LocalDate.now().minusYears(7));
        certificate.setEndDate(LocalDate.now().minusYears(7).plusMonths(2));
        certificate.setStatus(FormationStatus.TERMINE);
        formationRepository.save(certificate);

        Formation ongoing = new Formation();
        ongoing.setUser(jobSeeker);
        ongoing.setTitle("Néerlandais professionnel");
        ongoing.setInstitution("Promotion sociale Liège");
        ongoing.setStartDate(LocalDate.now().minusMonths(4));
        ongoing.setStatus(FormationStatus.EN_COURS);
        formationRepository.save(ongoing);
    }

    // ------------------------------------------------------------------ offres

    /** 24 offres ouvertes : de quoi remplir trois pages côté intérimaire. */
    private List<JobOffer> createBrowsableOffers(User employer) {
        List<JobOffer> offers = new ArrayList<>();
        for (int index = 0; index < 24; index += 1) {
            JobOffer offer = offer(employer, POSITIONS[index % POSITIONS.length] + " (réf. " + (index + 1) + ")",
                    index, JobOfferStatus.OPEN);
            JobOffer saved = jobOfferRepository.save(offer);
            // Une offre sur trois exige une compétence : les scores de correspondance varient.
            if (index % 3 == 0) {
                requireSkill(saved, index % 6 == 0 ? "Cariste" : "Maçonnerie", index % 2 == 0);
            }
            if (index % 4 == 0) {
                requireLanguage(saved, "Néerlandais", LanguageLevel.B1);
            }
            offers.add(saved);
        }
        return offers;
    }

    private JobOffer offer(User employer, String title, int index, JobOfferStatus status) {
        JobOffer offer = new JobOffer();
        offer.setEmployer(employer);
        offer.setTitle(title);
        Place place = PLACES[index % PLACES.length];
        offer.setSector(SECTORS[index % SECTORS.length]);
        offer.setCity(place.city());
        offer.setProvince(place.province());
        offer.setDescription("Poste à pourvoir rapidement. Environnement dynamique, équipe soudée, "
                + "horaire de jour du lundi au vendredi.");
        // Publications échelonnées : la liste est triée de la plus récente à la plus ancienne.
        offer.setPublishedAt(LocalDateTime.now().minusDays(index).minusHours(index));
        offer.setSalaryMin(new BigDecimal("14.00").add(new BigDecimal(index % 5)));
        offer.setSalaryMax(new BigDecimal("19.00").add(new BigDecimal(index % 5)));
        // Trois niveaux d'exigence : sans expérience, deux ans, cinq ans.
        offer.setExperienceTime(index % 3 == 0 ? "5" : index % 2 == 0 ? "2" : null);
        offer.setVehicleMandatory(index % 5 == 0);
        // Deux offres sur trois accordent des chèques-repas, à des montants différents.
        offer.setMealVoucherAmount(switch (index % 3) {
            case 0 -> new BigDecimal("10.00");
            case 1 -> new BigDecimal("8.00");
            default -> null;
        });
        offer.setStatus(status);
        return offer;
    }

    private void requireSkill(JobOffer offer, String skillName, boolean mandatory) {
        skillRepository.findFirstByNameIgnoreCaseAndIsGlobalTrue(skillName).ifPresent(skill -> {
            SkillJobOffer requirement = new SkillJobOffer();
            requirement.setJobOffer(offer);
            requirement.setSkill(skill);
            requirement.setIsMandatory(mandatory);
            requirement.setRequiredLevel(SkillLevel.INTERMEDIAIRE);
            skillJobOfferRepository.save(requirement);
        });
    }

    private void requireLanguage(JobOffer offer, String languageName, LanguageLevel level) {
        language(languageName).ifPresent(language -> {
            LanguageJobOffer requirement = new LanguageJobOffer();
            requirement.setJobOffer(offer);
            requirement.setLanguage(language);
            requirement.setIsMandatory(false);
            requirement.setRequiredLevel(level);
            languageJobOfferRepository.save(requirement);
        });
    }

    // ------------------------------------------- favoris et candidatures du profil type

    private void createFavoritesAndApplications(User jobSeeker, List<JobOffer> offers) {
        // 14 favoris = deux pages.
        for (JobOffer offer : offers.subList(0, 14)) {
            FavoriteJobOffer favorite = new FavoriteJobOffer();
            favorite.setJobSeeker(jobSeeker);
            favorite.setJobOffer(offer);
            favoriteRepository.save(favorite);
        }
        // 16 candidatures, dont deux annulées pour montrer les trois états du suivi.
        for (int index = 0; index < 16; index += 1) {
            JobOffer offer = offers.get(index);
            applicationRepository.save(application(
                    jobSeeker, offer,
                    LocalDateTime.now().minusDays(index).minusHours(2),
                    index % 8 == 7 ? ApplicationStatus.CANCELED : ApplicationStatus.PENDING));
        }
    }

    private Application application(User jobSeeker, JobOffer offer, LocalDateTime when, ApplicationStatus status) {
        Application application = new Application();
        application.setJobSeeker(jobSeeker);
        application.setJobOffer(offer);
        application.setApplicationTime(when);
        application.setStatus(status);
        return application;
    }

    // ---------------------------------------------------------------- missions

    /**
     * Une mission par candidature et par offre, sur des périodes qui ne se chevauchent
     * pas : ce sont les deux règles que le service applique à la création.
     */
    private void createMissions(User employer, User jobSeeker) {
        int offerIndex = 100;
        // Missions terminées : 12 lignes dans « Historique » (intérimaire et employeur).
        for (int index = 0; index < 12; index += 1) {
            LocalDate end = LocalDate.now().minusDays(15L + index * 20L);
            createMission(employer, jobSeeker, offerIndex++, MissionStatus.ACTIVE, end.minusDays(9), end, true);
        }
        // Missions confirmées à venir : 11 lignes dans « Missions confirmées » / « en cours ».
        for (int index = 0; index < 11; index += 1) {
            LocalDate start = LocalDate.now().plusDays(3L + index * 15L);
            createMission(employer, jobSeeker, offerIndex++, MissionStatus.ACTIVE, start, start.plusDays(9), true);
        }
        // En attente de validation de l'agence : 12 lignes côté agence et employeur.
        for (int index = 0; index < 12; index += 1) {
            LocalDate start = LocalDate.now().plusDays(200L + index * 12L);
            createMission(employer, jobSeeker, offerIndex++, MissionStatus.PENDING, start, start.plusDays(6), false);
        }
        // Refusées par l'agence puis par le candidat : 12 lignes dans « Missions refusées ».
        for (int index = 0; index < 6; index += 1) {
            LocalDate start = LocalDate.now().plusDays(400L + index * 12L);
            createMission(employer, jobSeeker, offerIndex++, MissionStatus.REFUSED, start, start.plusDays(6), false);
        }
        for (int index = 0; index < 6; index += 1) {
            LocalDate start = LocalDate.now().plusDays(500L + index * 12L);
            createMission(employer, jobSeeker, offerIndex++, MissionStatus.DECLINED, start, start.plusDays(6), false);
        }
        // Propositions qui attendent la réponse du candidat, dont un renouvellement.
        for (int index = 0; index < 3; index += 1) {
            LocalDate start = LocalDate.now().plusDays(600L + index * 12L);
            createMission(employer, jobSeeker, offerIndex++, MissionStatus.APPROVED, start, start.plusDays(6), false);
        }
        LocalDate renewalStart = LocalDate.now().plusDays(700);
        createMission(employer, jobSeeker, offerIndex++, MissionStatus.RENEWAL,
                renewalStart, renewalStart.plusDays(6), false);
    }

    private void createMission(
            User employer,
            User jobSeeker,
            int offerIndex,
            MissionStatus status,
            LocalDate start,
            LocalDate end,
            boolean withContract) {
        String position = POSITIONS[offerIndex % POSITIONS.length];
        // Une mission acceptée clôture son offre : le statut suit la règle du service.
        JobOffer offer = jobOfferRepository.save(offer(
                employer, position, offerIndex,
                status == MissionStatus.ACTIVE ? JobOfferStatus.CLOSED : JobOfferStatus.OPEN));
        Application application = applicationRepository.save(application(
                jobSeeker, offer, LocalDateTime.now().minusDays(offerIndex % 40), ApplicationStatus.PENDING));

        Mission mission = new Mission();
        mission.setApplication(application);
        mission.setStatus(status);
        mission.setStartDate(start);
        mission.setEndDate(end);
        mission.setPosition(position);
        mission.setWorkplace(employer.getAddress());
        mission.setMealVoucherAmount(offer.getMealVoucherAmount());
        mission.setDescription("Manutention et préparation de commandes en entrepôt. "
                + "Port de charges, utilisation d'un transpalette électrique, respect des consignes de sécurité.");
        mission.setJointCommittee(employer.getJointCommittee());
        mission.setHourlyWage(new BigDecimal("16.50"));
        mission.setWorkReason(WorkReason.OVERLOAD);
        if (status == MissionStatus.REFUSED) {
            mission.setRefusalReason("Le salaire horaire proposé est inférieur au barème du secteur.");
        }
        Mission saved = missionRepository.save(mission);

        // Les journées sont enregistrées d'abord : le contrat les reprend.
        List<DailySchedule> slots = dailyScheduleRepository.saveAll(workingDays(saved, start, end));
        if (withContract) {
            // Le contrat passe par le service de production : la ligne en base ET le PDF
            // sur disque sont créés ensemble, sinon le bouton « Contrat » renverrait 404.
            contractService.generate(saved, slots);
        }
    }

    /** Journées ouvrées de la période, 08:00–16:30 avec une pause de midi non payée. */
    private List<DailySchedule> workingDays(Mission mission, LocalDate start, LocalDate end) {
        List<DailySchedule> slots = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (day.getDayOfWeek().getValue() >= 6) {
                continue;
            }
            DailySchedule slot = new DailySchedule();
            slot.setMission(mission);
            slot.setDate(day);
            slot.setStartTime(LocalTime.of(8, 0));
            slot.setEndTime(LocalTime.of(16, 30));
            slot.setBreakStart(LocalTime.of(12, 0));
            slot.setBreakEnd(LocalTime.of(12, 30));
            slots.add(slot);
        }
        return slots;
    }

    // ------------------------------------------- autres candidats et conversations

    /**
     * 14 autres candidats sur la première offre : la page « Candidatures » de l'employeur
     * dépasse ainsi dix lignes, et douze d'entre eux ont une conversation ouverte.
     */
    private void createOtherCandidates(User employer, JobOffer offer) {
        String[] firstNames = {
            "Alice", "Bruno", "Chloé", "David", "Elena", "Farid", "Gaëlle",
            "Hugo", "Inès", "Julien", "Karim", "Léa", "Marc", "Nadia"
        };
        for (int index = 0; index < firstNames.length; index += 1) {
            User candidate = newUser(
                    "demo" + (index + 1) + "@jobseeker.com", firstNames[index], "Candidat" + (index + 1),
                    Role.JOBSEEKER);
            candidate.setAddress("Rue de la Demande " + (index + 1) + ", 4000 Liège, Belgique");
            User saved = userRepository.save(candidate);

            Application application = applicationRepository.save(application(
                    saved, offer, LocalDateTime.now().minusDays(index).minusMinutes(30),
                    ApplicationStatus.PENDING));
            // Une candidature sur trois est notée : le tri par note a de quoi classer.
            if (index % 3 == 0) {
                application.setRating(5 - (index % 5));
                applicationRepository.save(application);
            }
            if (index < 12) {
                createConversation(employer, saved, application, index);
            }
        }
    }

    /** Le premier fil compte 35 messages : de quoi voir « Voir les messages plus anciens ». */
    private void createConversation(User employer, User candidate, Application application, int index) {
        Conversation conversation = new Conversation();
        conversation.setApplication(application);
        conversation.setSender(employer);
        conversation.setReceiver(candidate);
        Conversation saved = conversationRepository.save(conversation);

        int count = index == 0 ? 35 : 2 + index % 3;
        for (int number = 1; number <= count; number += 1) {
            Message message = new Message();
            message.setConversation(saved);
            message.setUser(number % 2 == 1 ? employer : candidate);
            message.setContent(number % 2 == 1
                    ? "Bonjour, êtes-vous disponible pour un entretien ? (message " + number + ")"
                    : "Bonjour, oui, je reste disponible cette semaine. (message " + number + ")");
            message.setSentTime(LocalDateTime.now().minusDays(index).minusMinutes(count - number + 1L));
            // Les messages du candidat restent non lus : le badge de l'employeur est visible.
            message.setRead(number % 2 == 1);
            messageRepository.save(message);
        }
    }

    // -------------------------------------------- demandes d'accès employeur

    /** 12 demandes en attente + 8 traitées : les deux sections de l'espace agence sont paginées. */
    private void createPendingEmployerRequests() {
        for (int index = 1; index <= 20; index += 1) {
            User applicant = newUser(
                    "demo" + index + "@employer.com", "Société", "Demandeur" + index, Role.EMPLOYER_PENDING);
            applicant.setCompanyName("Candidate Entreprise " + index + " SPRL");
            applicant.setAddress("Avenue des Demandes " + index + ", 5000 Namur, Belgique");
            User saved = userRepository.save(applicant);

            EmployerAccessRequest request = new EmployerAccessRequest();
            request.setUser(saved);
            request.setRequestDate(LocalDate.now().minusDays(index));
            if (index <= 12) {
                request.setStatus(EmployerAccessStatus.PENDING);
                if (index % 4 == 0) {
                    request.setMessage("Nous employons une dizaine d'intérimaires par an en Wallonie.");
                }
            } else {
                request.setStatus(index % 2 == 0
                        ? EmployerAccessStatus.ACCEPTED
                        : EmployerAccessStatus.REFUSED);
                saved.setRole(index % 2 == 0 ? Role.EMPLOYER : Role.EMPLOYER_PENDING);
                userRepository.save(saved);
            }
            accessRequestRepository.save(request);
        }
    }
}
