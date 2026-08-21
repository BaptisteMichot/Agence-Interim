package be.agence_interim.service;

import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.CandidateProfileResponse;
import be.agence_interim.dto.ExperienceResponse;
import be.agence_interim.dto.FormationResponse;
import be.agence_interim.dto.OfferApplicationResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.dto.UserDegreeResponse;
import be.agence_interim.dto.UserLanguageResponse;
import be.agence_interim.dto.UserSkillResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.ExperienceRepository;
import be.agence_interim.repository.FormationRepository;
import be.agence_interim.repository.JobOfferRepository;

/** Candidatures reçues sur les offres de l'employeur : consultation, note, profil du candidat. */
@Service
public class EmployerApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final ExperienceRepository experienceRepository;
    private final FormationRepository formationRepository;
    private final SkillService skillService;
    private final DegreeService degreeService;
    private final LanguageService languageService;
    private final CvService cvService;

    public EmployerApplicationService(
            ApplicationRepository applicationRepository,
            JobOfferRepository jobOfferRepository,
            ExperienceRepository experienceRepository,
            FormationRepository formationRepository,
            SkillService skillService,
            DegreeService degreeService,
            LanguageService languageService,
            CvService cvService) {
        this.applicationRepository = applicationRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.experienceRepository = experienceRepository;
        this.formationRepository = formationRepository;
        this.skillService = skillService;
        this.degreeService = degreeService;
        this.languageService = languageService;
        this.cvService = cvService;
    }

    /**
     * Une page des candidatures en cours d'une offre de l'employeur (les annulées sont
     * exclues). Le tri est appliqué en base : trier après coup ne classerait que les
     * quelques candidatures de la page affichée, pas l'ensemble des candidats.
     */
    @Transactional(readOnly = true)
    public PageResponse<OfferApplicationResponse> listForOffer(
            int employerId, int offerId, String sort, int page, int size) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .filter(o -> o.getEmployer().getId() == employerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
        Pageable pageable = PageRequest.of(page, size, sortOf(sort));
        return PageResponse.of(
                applicationRepository.findByJobOfferIdAndStatusFetchJobSeeker(
                        offer.getId(), ApplicationStatus.PENDING, pageable),
                OfferApplicationResponse::fromEntity);
    }

    /**
     * Tris proposés à l'employeur (FR12). Les candidatures non notées passent en
     * dernier sur le tri par note, et la date départage les ex æquo pour que la
     * pagination ne fasse pas réapparaître deux fois la même ligne.
     */
    private static Sort sortOf(String sort) {
        Sort byDateDesc = Sort.by(Sort.Order.desc("applicationTime"));
        return switch (sort == null ? "" : sort) {
            case "date-asc" -> Sort.by(Sort.Order.asc("applicationTime"));
            case "rating-desc" -> Sort.by(Sort.Order.desc("rating").nullsLast()).and(byDateDesc);
            default -> byDateDesc;
        };
    }

    /** Nombre total de candidatures en cours, toutes offres confondues (tableau de bord). */
    public long pendingCount(int employerId) {
        return applicationRepository.countByJobOfferEmployerIdAndStatus(employerId, ApplicationStatus.PENDING);
    }

    /** Note une candidature (1 à 5) reçue sur une des offres de l'employeur. */
    @Transactional
    public OfferApplicationResponse rate(int employerId, int applicationId, int rating) {
        Application application = ownedApplication(employerId, applicationId);
        application.setRating(rating);
        return OfferApplicationResponse.fromEntity(applicationRepository.save(application));
    }

    /** Profil complet du candidat d'une candidature reçue. */
    @Transactional(readOnly = true)
    public CandidateProfileResponse candidateProfile(int employerId, int applicationId) {
        Application application = ownedApplication(employerId, applicationId);
        User candidate = application.getJobSeeker();
        int candidateId = candidate.getId();
        return new CandidateProfileResponse(
                candidateId,
                candidate.getLastName(),
                candidate.getFirstName(),
                candidate.getEmail(),
                candidate.getBirthdate(),
                candidate.getHasVehicle(),
                candidate.getCvFilePath() != null,
                application.getJobOffer().getId(),
                application.getJobOffer().getTitle(),
                skillService.userSkills(candidateId).stream().map(UserSkillResponse::fromEntity).toList(),
                degreeService.userDegrees(candidateId).stream().map(UserDegreeResponse::fromEntity).toList(),
                languageService.userLanguages(candidateId).stream().map(UserLanguageResponse::fromEntity).toList(),
                experienceRepository.findByUserIdOrderByStartDateDesc(candidateId)
                        .stream().map(ExperienceResponse::fromEntity).toList(),
                formationRepository.findByUserIdOrderByStartDateDesc(candidateId)
                        .stream().map(FormationResponse::fromEntity).toList());
    }

    /** CV du candidat d'une candidature reçue. */
    @Transactional(readOnly = true)
    public Resource candidateCv(int employerId, int applicationId) {
        return cvService.load(ownedApplication(employerId, applicationId).getJobSeeker().getId());
    }

    /** Charge une candidature en vérifiant qu'elle concerne une offre de l'employeur et n'est pas annulée. */
    private Application ownedApplication(int employerId, int applicationId) {
        return applicationRepository.findById(applicationId)
                .filter(a -> a.getJobOffer().getEmployer().getId() == employerId)
                .filter(a -> a.getStatus() != ApplicationStatus.CANCELED)
                .orElseThrow(() -> new NoSuchElementException("Candidature introuvable."));
    }
}
