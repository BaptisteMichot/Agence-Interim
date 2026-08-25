package be.agence_interim.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.config.AgencyProperties;
import be.agence_interim.dto.ExperienceResponse;
import be.agence_interim.dto.FormationResponse;
import be.agence_interim.dto.PersonalDataExport;
import be.agence_interim.dto.UserDegreeResponse;
import be.agence_interim.dto.UserLanguageResponse;
import be.agence_interim.dto.UserSkillResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.AuditAction;
import be.agence_interim.model.Conversation;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.ContractRepository;
import be.agence_interim.repository.DegreeJobOfferRepository;
import be.agence_interim.repository.DegreeUserRepository;
import be.agence_interim.repository.EmployerAccessRequestRepository;
import be.agence_interim.repository.ExperienceRepository;
import be.agence_interim.repository.FavoriteJobOfferRepository;
import be.agence_interim.repository.FormationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.LanguageJobOfferRepository;
import be.agence_interim.repository.LanguageUserRepository;
import be.agence_interim.repository.MessageRepository;
import be.agence_interim.repository.MissionRepository;
import be.agence_interim.repository.SkillJobOfferRepository;
import be.agence_interim.repository.SkillUserRepository;
import be.agence_interim.repository.UnavailabilityRepository;
import be.agence_interim.repository.UserRepository;

/**
 * Cycle de vie du compte de l'utilisateur courant : mot de passe, export de ses données,
 * clôture.
 *
 * <p>Ces trois opérations répondent à des droits que l'application affichait sans les
 * offrir. Sa page « Vie privée » annonce un droit d'accès et un droit à l'effacement ;
 * seul un employeur encore en attente de validation pouvait effectivement partir.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    /**
     * Missions qui retiennent encore le compte : sa clôture est refusée tant que l'une
     * d'elles subsiste.
     *
     * <p>{@code PENDING} en fait partie : une mission soumise à l'agence attend une
     * décision, et laisser partir l'employeur reviendrait à demander à l'agence de
     * trancher un dossier dont une partie n'existe plus.
     */
    private static final Set<MissionStatus> BINDING = EnumSet.of(
            MissionStatus.PENDING,
            MissionStatus.APPROVED,
            MissionStatus.ACTIVE,
            MissionStatus.RENEWAL);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final AgencyProperties agency;
    private final PasswordEncoder passwordEncoder;
    private final CvService cvService;
    private final SkillService skillService;
    private final DegreeService degreeService;
    private final LanguageService languageService;

    private final ExperienceRepository experienceRepository;
    private final FormationRepository formationRepository;
    private final SkillUserRepository skillUserRepository;
    private final DegreeUserRepository degreeUserRepository;
    private final LanguageUserRepository languageUserRepository;
    private final FavoriteJobOfferRepository favoriteJobOfferRepository;
    private final SkillJobOfferRepository skillJobOfferRepository;
    private final DegreeJobOfferRepository degreeJobOfferRepository;
    private final LanguageJobOfferRepository languageJobOfferRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final MessageRepository messageRepository;
    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final MissionRepository missionRepository;
    private final ContractRepository contractRepository;
    private final EmployerAccessRequestRepository employerAccessRequestRepository;
    private final MailService mailService;

    @SuppressWarnings("java:S107") // Une clôture de compte touche, par nature, tout le modèle.
    public AccountService(
            UserRepository userRepository,
            AuthService authService,
            AuditService auditService,
            MailService mailService,
            AgencyProperties agency,
            PasswordEncoder passwordEncoder,
            CvService cvService,
            SkillService skillService,
            DegreeService degreeService,
            LanguageService languageService,
            ExperienceRepository experienceRepository,
            FormationRepository formationRepository,
            SkillUserRepository skillUserRepository,
            DegreeUserRepository degreeUserRepository,
            LanguageUserRepository languageUserRepository,
            FavoriteJobOfferRepository favoriteJobOfferRepository,
            SkillJobOfferRepository skillJobOfferRepository,
            DegreeJobOfferRepository degreeJobOfferRepository,
            LanguageJobOfferRepository languageJobOfferRepository,
            UnavailabilityRepository unavailabilityRepository,
            MessageRepository messageRepository,
            ApplicationRepository applicationRepository,
            JobOfferRepository jobOfferRepository,
            MissionRepository missionRepository,
            ContractRepository contractRepository,
            EmployerAccessRequestRepository employerAccessRequestRepository) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.mailService = mailService;
        this.agency = agency;
        this.passwordEncoder = passwordEncoder;
        this.cvService = cvService;
        this.skillService = skillService;
        this.degreeService = degreeService;
        this.languageService = languageService;
        this.experienceRepository = experienceRepository;
        this.formationRepository = formationRepository;
        this.skillUserRepository = skillUserRepository;
        this.degreeUserRepository = degreeUserRepository;
        this.languageUserRepository = languageUserRepository;
        this.favoriteJobOfferRepository = favoriteJobOfferRepository;
        this.skillJobOfferRepository = skillJobOfferRepository;
        this.degreeJobOfferRepository = degreeJobOfferRepository;
        this.languageJobOfferRepository = languageJobOfferRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.messageRepository = messageRepository;
        this.applicationRepository = applicationRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.missionRepository = missionRepository;
        this.contractRepository = contractRepository;
        this.employerAccessRequestRepository = employerAccessRequestRepository;
    }

    /** Change le mot de passe et consigne l'opération. Retourne le compte à reconnecter. */
    @Transactional
    public User changePassword(int userId, String currentPassword, String newPassword) {
        User user = authService.changePassword(userId, currentPassword, newPassword);
        auditService.record(AuditAction.PASSWORD_CHANGED, userId, "USER", userId, null);
        return user;
    }

    /**
     * Toutes les données personnelles du compte, mises en forme en un document texte.
     *
     * <p>Le recueil est le même pour tous les rôles ; ce sont les rubriques vides qui
     * font la différence à l'impression. Un employeur n'a ni compétences ni candidatures,
     * un intérimaire n'a pas d'offres publiées, et le document ne mentionne que ce qui
     * existe.
     */
    @Transactional(readOnly = true)
    public String export(int userId) {
        User user = userRepository.requireById(userId);
        auditService.record(AuditAction.DATA_EXPORTED, userId, "USER", userId, null);

        PersonalDataExport collected = new PersonalDataExport(
                LocalDateTime.now(),
                identity(user),
                skillService.userSkills(userId).stream().map(UserSkillResponse::fromEntity).toList(),
                degreeService.userDegrees(userId).stream().map(UserDegreeResponse::fromEntity).toList(),
                languageService.userLanguages(userId).stream().map(UserLanguageResponse::fromEntity).toList(),
                experienceRepository.findByUserIdOrderByStartDateDesc(userId)
                        .stream().map(ExperienceResponse::fromEntity).toList(),
                formationRepository.findByUserIdOrderByStartDateDesc(userId)
                        .stream().map(FormationResponse::fromEntity).toList(),
                applications(userId),
                offers(userId),
                contracts(userId),
                messages(userId));
        return PersonalDataDocument.render(collected, agency);
    }

    /**
     * Clôture le compte de l'utilisateur courant.
     *
     * <p><strong>Supprimer ou anonymiser ?</strong> Les deux, selon ce que le compte a
     * produit. Un compte qui n'a jamais rien engagé disparaît vraiment. Dès qu'il a
     * postulé ou publié une offre, la suppression pure emporterait avec elle des
     * candidatures, des missions et des contrats de travail — que la loi belge impose de
     * conserver cinq ans, et qui concernent aussi l'autre partie. Le compte est alors
     * anonymisé : tout ce qui identifie la personne est effacé, la chaîne contractuelle
     * reste vérifiable.
     *
     * <p>Une clôture est refusée tant qu'une mission engage encore les parties : on ne
     * quitte pas la plateforme au milieu d'un contrat en cours.
     */
    @Transactional
    public void close(int userId) {
        User user = userRepository.requireById(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Un compte de l'agence ne peut pas être clôturé depuis cet écran.");
        }
        requireNoBindingMission(user);

        // L'adresse et le prénom sont relevés maintenant, tant qu'ils existent encore :
        // la clôture se termine soit par une anonymisation, qui remplace l'email par une
        // adresse de rebut, soit par la suppression pure et simple de la ligne. Dans les
        // deux cas, il ne resterait plus rien à qui écrire.
        String recipient = user.getEmail();
        String firstName = user.getFirstName();

        // Données de profil : rien ne les retient, elles partent dans les deux cas.
        cvService.delete(userId);
        experienceRepository.deleteByUserId(userId);
        formationRepository.deleteByUserId(userId);
        skillUserRepository.deleteByUserId(userId);
        degreeUserRepository.deleteByUserId(userId);
        languageUserRepository.deleteByUserId(userId);
        favoriteJobOfferRepository.deleteByJobSeekerId(userId);
        unavailabilityRepository.deleteByUserId(userId);
        messageRepository.deleteByUserId(userId);
        withdrawOffers(userId);

        boolean engaged = applicationRepository.existsByJobSeekerId(userId)
                || jobOfferRepository.existsByEmployerId(userId);
        auditService.record(
                AuditAction.ACCOUNT_CLOSED,
                userId,
                "USER",
                userId,
                engaged
                        ? "Anonymisation ; offres sans candidature supprimées, les autres clôturées"
                        : "Suppression complète");

        if (engaged) {
            anonymise(user);
            log.info("Compte {} anonymisé.", userId);
        } else {
            employerAccessRequestRepository.deleteByUserId(userId);
            userRepository.delete(user);
            log.info("Compte {} supprimé.", userId);
        }

        // En dernier : l'accusé ne part qu'une fois la clôture réellement faite. Il vaut
        // aussi confirmation écrite de l'exercice du droit à l'effacement, que le RGPD
        // demande de traiter mais dont rien d'autre ici ne laisserait de trace à
        // l'intéressé — son compte, précisément, n'existe plus.
        notifyClosure(recipient, firstName, engaged);
    }

    /**
     * Accuse réception de la clôture.
     *
     * <p>Le texte diffère selon l'issue, parce que l'engagement n'est pas le même. Un
     * compte sans rien derrière lui disparaît ; un compte qui a candidaté ou publié laisse
     * des pièces que la loi impose de conserver, et l'annoncer évite de promettre un
     * effacement total qui n'a pas eu lieu.
     */
    private void notifyClosure(String recipient, String firstName, boolean engaged) {
        mailService.send(recipient,
                "Votre compte a été clôturé",
                "Bonjour " + firstName + ",\n\n"
                        + "Votre compte a été clôturé et vos données de profil ont été effacées.\n\n"
                        + (engaged
                                ? "Les candidatures, offres et contrats auxquels vous avez pris part "
                                        + "sont conservés sans votre identité : ils engagent d'autres "
                                        + "parties et relèvent d'obligations légales de conservation.\n\n"
                                : "Aucune donnée vous concernant ne subsiste sur la plateforme.\n\n")
                        + "Vous ne recevrez plus aucun message de notre part.\n\n"
                        + "L'agence d'intérim");
    }

    /**
     * Retire de la circulation les offres de l'employeur qui s'en va.
     *
     * <p>Aucune offre ne peut lui survivre en l'état : plus personne ne relève les
     * candidatures, et une offre ouverte dont l'auteur a disparu fait perdre leur temps à
     * ceux qui y postulent. Deux sorts, selon ce que l'offre entraîne avec elle.
     *
     * <p><strong>Sans aucune candidature</strong>, l'offre est supprimée : rien ni
     * personne n'en dépend. Ses exigences et les favoris qui la visent partent avec elle.
     *
     * <p><strong>Dès qu'une candidature s'y rattache</strong>, l'offre est clôturée et
     * conservée. La supprimer effacerait la candidature d'un intérimaire, c'est-à-dire une
     * donnée qui n'appartient pas à l'employeur : son historique de recherche d'emploi lui
     * reste, débarrassé du nom de son interlocuteur par l'anonymisation du compte.
     *
     * <p>Les candidatures encore en cours sur ces offres sont annulées. Sans cela, elles
     * resteraient éternellement « en cours » dans l'espace de l'intérimaire, en attente
     * d'une réponse que personne ne donnera jamais.
     */
    private void withdrawOffers(int employerId) {
        for (Application application
                : applicationRepository.findByJobOfferEmployerIdAndStatus(
                        employerId, ApplicationStatus.PENDING)) {
            application.setStatus(ApplicationStatus.CANCELED);
            applicationRepository.save(application);
        }

        for (JobOffer offer : jobOfferRepository.findByEmployerId(employerId)) {
            if (applicationRepository.existsByJobOfferId(offer.getId())) {
                offer.setStatus(JobOfferStatus.CLOSED);
                jobOfferRepository.save(offer);
            } else {
                deleteOffer(offer);
            }
        }
    }

    /** Supprime une offre et tout ce qui n'existe que par elle. */
    private void deleteOffer(JobOffer offer) {
        skillJobOfferRepository.deleteByJobOfferId(offer.getId());
        degreeJobOfferRepository.deleteByJobOfferId(offer.getId());
        languageJobOfferRepository.deleteByJobOfferId(offer.getId());
        favoriteJobOfferRepository.deleteByJobOfferId(offer.getId());
        jobOfferRepository.delete(offer);
    }

    /** Refuse la clôture tant qu'une mission engage encore le compte. */
    private void requireNoBindingMission(User user) {
        long binding = user.getRole() == Role.EMPLOYER
                ? missionRepository.countForEmployerByStatuses(user.getId(), BINDING)
                : missionRepository.countByApplicationJobSeekerIdAndStatusIn(user.getId(), BINDING);
        if (binding > 0) {
            throw new IllegalArgumentException(
                    "Une mission est encore en cours : la clôture du compte n'est pas possible "
                            + "tant qu'elle n'est pas terminée.");
        }
    }

    /**
     * Efface tout ce qui identifie la personne, en gardant la ligne pour que les
     * documents qui la référencent restent cohérents.
     *
     * <p>Les mentions de l'entreprise — dénomination, numéro BCE, commission paritaire —
     * sont conservées : elles désignent une personne morale, elles figurent sur des
     * contrats en vigueur, et elles ne sont pas des données personnelles.
     */
    private void anonymise(User user) {
        user.setFirstName("Compte");
        user.setLastName("clôturé");
        user.setEmail("supprime+" + user.getId() + "@invalide.local");
        // Mot de passe irrécupérable : le compte ne doit plus jamais s'ouvrir. Le champ
        // ne peut pas être vidé, il est déclaré non nul, et un haché de valeur aléatoire
        // vaut mieux qu'une valeur fixe partagée par tous les comptes clôturés.
        user.setPassword(passwordEncoder.encode(randomSecret()));
        user.setBirthdate(null);
        user.setHasVehicle(null);
        user.setAddress(null);
        user.setNationalNumber(null);
        user.setIban(null);
        user.setCvFilePath(null);
        // La ligne survit, mais le compte ne doit plus compter parmi les utilisateurs :
        // cette date est ce qui le retire des traitements qui parcourent une population,
        // à commencer par le contact automatique à la publication d'une offre.
        user.setClosedAt(LocalDateTime.now());
        // Les jetons encore en circulation cessent d'être acceptés à l'instant.
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private PersonalDataExport.Identity identity(User user) {
        return new PersonalDataExport.Identity(
                user.getRole().name(),
                user.getLastName(),
                user.getFirstName(),
                user.getEmail(),
                user.getBirthdate(),
                user.getHasVehicle(),
                user.getAddress(),
                user.getNationalNumber(),
                user.getIban(),
                user.getCompanyName(),
                user.getCompanyNumber(),
                user.getJointCommittee(),
                user.getCvFilePath(),
                user.getLastLoginAt());
    }

    private List<PersonalDataExport.ApplicationLine> applications(int userId) {
        return applicationRepository.findByJobSeekerIdFetchOffer(userId, Pageable.unpaged())
                .map(application -> new PersonalDataExport.ApplicationLine(
                        application.getJobOffer().getTitle(),
                        application.getJobOffer().getEmployer().getCompanyName(),
                        application.getApplicationTime(),
                        application.getStatus().name(),
                        application.getRating()))
                .toList();
    }

    /**
     * Offres publiées par le compte.
     *
     * <p>Absente de la première version de l'export, qui ne rendait donc presque rien à
     * un employeur : ses offres sont bien des données le concernant, puisque c'est lui
     * qui les a écrites et qu'elles portent son entreprise.
     */
    private List<PersonalDataExport.OfferLine> offers(int userId) {
        return jobOfferRepository.findByEmployerIdFetchEmployer(userId, Pageable.unpaged())
                .map(offer -> new PersonalDataExport.OfferLine(
                        offer.getTitle(),
                        offer.getCity(),
                        offer.getPublishedAt(),
                        offer.getStatus().name()))
                .toList();
    }

    private List<PersonalDataExport.ContractLine> contracts(int userId) {
        return contractRepository.findForUser(userId, Pageable.unpaged())
                .map(contract -> new PersonalDataExport.ContractLine(
                        contract.getMission().getPosition(),
                        contract.getGenerationTime(),
                        contract.getStatusEmployer().name(),
                        contract.getEmployerSignedAt(),
                        contract.getStatusWorker().name(),
                        contract.getWorkerSignedAt()))
                .toList();
    }

    private List<PersonalDataExport.MessageLine> messages(int userId) {
        return messageRepository.findByUserIdOrderBySentTimeAsc(userId).stream()
                .map(message -> new PersonalDataExport.MessageLine(
                        conversationLabel(message.getConversation(), userId),
                        message.getSentTime(),
                        message.getContent()))
                .toList();
    }

    /**
     * Nomme un fil de discussion par son interlocuteur et l'offre qui l'a fait naître.
     *
     * <p>« Conversation n° 12 » ne dit rien à qui lit son export : le numéro est une clé
     * primaire, pas un repère. Le nom de l'autre partie et l'intitulé de l'offre, si.
     */
    private String conversationLabel(Conversation conversation, int userId) {
        User other = conversation.getSender().getId() == userId
                ? conversation.getReceiver()
                : conversation.getSender();
        return other.getFirstName() + " " + other.getLastName()
                + " — offre « " + conversation.getApplication().getJobOffer().getTitle() + " »";
    }
}
