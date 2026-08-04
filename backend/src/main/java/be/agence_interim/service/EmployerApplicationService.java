package be.agence_interim.service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.CandidateProfileResponse;
import be.agence_interim.dto.ExperienceResponse;
import be.agence_interim.dto.FormationResponse;
import be.agence_interim.dto.OfferApplicationResponse;
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

    /** Candidatures en cours d'une offre de l'employeur (les annulées sont exclues). */
    @Transactional(readOnly = true)
    public List<OfferApplicationResponse> listForOffer(int employerId, int offerId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .filter(o -> o.getEmployer().getId() == employerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
        return applicationRepository
                .findByJobOfferIdAndStatusFetchJobSeeker(offer.getId(), ApplicationStatus.PENDING)
                .stream().map(OfferApplicationResponse::fromEntity).toList();
    }

    /** Nombre de candidatures en cours par offre de l'employeur. */
    public Map<Integer, Long> countsByOffer(int employerId) {
        return applicationRepository.countByOfferForEmployer(employerId, ApplicationStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(count -> count.getOfferId(), count -> count.getTotal()));
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
