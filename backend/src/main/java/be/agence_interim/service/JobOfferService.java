package be.agence_interim.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.JobOfferRequest;
import be.agence_interim.dto.JobOfferResponse;
import be.agence_interim.dto.JobOfferSummaryResponse;
import be.agence_interim.dto.OfferDegreeRequirement;
import be.agence_interim.dto.OfferLanguageRequirement;
import be.agence_interim.dto.OfferSkillRequirement;
import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.Skill;
import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.repository.DegreeJobOfferRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.LanguageJobOfferRepository;
import be.agence_interim.repository.LanguageRepository;
import be.agence_interim.repository.SkillJobOfferRepository;
import be.agence_interim.repository.UserRepository;

/** Gestion des offres d'emploi de l'employeur courant (création, suivi, clôture). */
@Service
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final SkillJobOfferRepository skillJobOfferRepository;
    private final DegreeJobOfferRepository degreeJobOfferRepository;
    private final LanguageJobOfferRepository languageJobOfferRepository;
    private final LanguageRepository languageRepository;
    private final SkillService skillService;
    private final DegreeService degreeService;
    private final UserRepository userRepository;

    public JobOfferService(
            JobOfferRepository jobOfferRepository,
            SkillJobOfferRepository skillJobOfferRepository,
            DegreeJobOfferRepository degreeJobOfferRepository,
            LanguageJobOfferRepository languageJobOfferRepository,
            LanguageRepository languageRepository,
            SkillService skillService,
            DegreeService degreeService,
            UserRepository userRepository) {
        this.jobOfferRepository = jobOfferRepository;
        this.skillJobOfferRepository = skillJobOfferRepository;
        this.degreeJobOfferRepository = degreeJobOfferRepository;
        this.languageJobOfferRepository = languageJobOfferRepository;
        this.languageRepository = languageRepository;
        this.skillService = skillService;
        this.degreeService = degreeService;
        this.userRepository = userRepository;
    }

    @Transactional
    public JobOfferResponse create(int employerId, JobOfferRequest request) {
        validateSalaries(request);
        JobOffer offer = new JobOffer();
        offer.setEmployer(userRepository.getReferenceById(employerId));
        offer.setPublishedAt(LocalDateTime.now());
        offer.setStatus(JobOfferStatus.OPEN);
        applyFields(offer, request);
        JobOffer saved = jobOfferRepository.save(offer);
        saveRequirements(employerId, saved, request);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<JobOfferSummaryResponse> listMine(int employerId) {
        return jobOfferRepository.findByEmployerIdOrderByPublishedAtDesc(employerId)
                .stream().map(JobOfferSummaryResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public JobOfferResponse getMine(int employerId, int offerId) {
        return toResponse(ownedOffer(employerId, offerId));
    }

    /** Met à jour l'offre et remplace ses exigences. Une offre clôturée n'est plus modifiable. */
    @Transactional
    public JobOfferResponse update(int employerId, int offerId, JobOfferRequest request) {
        validateSalaries(request);
        JobOffer offer = ownedOffer(employerId, offerId);
        if (offer.getStatus() == JobOfferStatus.CLOSED) {
            throw new IllegalArgumentException("Une offre clôturée ne peut plus être modifiée.");
        }
        applyFields(offer, request);
        JobOffer saved = jobOfferRepository.save(offer);
        skillJobOfferRepository.deleteByJobOfferId(offerId);
        degreeJobOfferRepository.deleteByJobOfferId(offerId);
        languageJobOfferRepository.deleteByJobOfferId(offerId);
        saveRequirements(employerId, saved, request);
        return toResponse(saved);
    }

    @Transactional
    public JobOfferResponse close(int employerId, int offerId) {
        JobOffer offer = ownedOffer(employerId, offerId);
        if (offer.getStatus() == JobOfferStatus.CLOSED) {
            throw new IllegalArgumentException("Cette offre est déjà clôturée.");
        }
        offer.setStatus(JobOfferStatus.CLOSED);
        return toResponse(jobOfferRepository.save(offer));
    }

    private JobOffer ownedOffer(int employerId, int offerId) {
        return jobOfferRepository.findByIdAndEmployerId(offerId, employerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
    }

    private void applyFields(JobOffer offer, JobOfferRequest request) {
        offer.setTitle(request.title());
        offer.setSector(request.sector());
        offer.setCity(request.city());
        offer.setDescription(request.description());
        offer.setSalaryMin(request.salaryMin());
        offer.setSalaryMax(request.salaryMax());
        offer.setExperienceTime(request.experienceTime());
        offer.setVehicleMandatory(request.vehicleMandatory());
    }

    private void validateSalaries(JobOfferRequest request) {
        if (request.salaryMin() != null && request.salaryMax() != null
                && request.salaryMax().compareTo(request.salaryMin()) < 0) {
            throw new IllegalArgumentException("Le salaire maximum doit etre superieur ou egal au salaire minimum.");
        }
    }

    private void saveRequirements(int employerId, JobOffer offer, JobOfferRequest request) {
        saveSkills(employerId, offer, request.skills() == null ? List.of() : request.skills());
        saveDegrees(employerId, offer, request.degrees() == null ? List.of() : request.degrees());
        saveLanguages(offer, request.languages() == null ? List.of() : request.languages());
    }

    private void saveSkills(int employerId, JobOffer offer, List<OfferSkillRequirement> requirements) {
        Set<Integer> seen = new HashSet<>();
        for (OfferSkillRequirement requirement : requirements) {
            Skill skill = skillService.resolveSkill(employerId, null, requirement.name());
            if (!seen.add(skill.getId())) {
                throw new IllegalArgumentException(
                        "La compétence « " + skill.getName() + " » est renseignée plusieurs fois.");
            }
            SkillJobOffer entity = new SkillJobOffer();
            entity.setSkill(skill);
            entity.setJobOffer(offer);
            entity.setIsMandatory(requirement.isMandatory());
            entity.setRequiredLevel(requirement.requiredLevel());
            skillJobOfferRepository.save(entity);
        }
    }

    private void saveDegrees(int employerId, JobOffer offer, List<OfferDegreeRequirement> requirements) {
        Set<Integer> seen = new HashSet<>();
        for (OfferDegreeRequirement requirement : requirements) {
            Degree degree = degreeService.resolveDegree(employerId, null, requirement.type(), requirement.section());
            if (!seen.add(degree.getId())) {
                throw new IllegalArgumentException(
                        "Le diplôme « " + degree.getSection() + " » est renseigné plusieurs fois.");
            }
            DegreeJobOffer entity = new DegreeJobOffer();
            entity.setDegree(degree);
            entity.setJobOffer(offer);
            entity.setIsMandatory(requirement.isMandatory());
            degreeJobOfferRepository.save(entity);
        }
    }

    private void saveLanguages(JobOffer offer, List<OfferLanguageRequirement> requirements) {
        Set<Integer> seen = new HashSet<>();
        for (OfferLanguageRequirement requirement : requirements) {
            Language language = languageRepository.findById(requirement.languageId())
                    .orElseThrow(() -> new NoSuchElementException("Langue introuvable."));
            if (!seen.add(language.getId())) {
                throw new IllegalArgumentException(
                        "La langue « " + language.getName() + " » est renseignée plusieurs fois.");
            }
            LanguageJobOffer entity = new LanguageJobOffer();
            entity.setLanguage(language);
            entity.setJobOffer(offer);
            entity.setIsMandatory(requirement.isMandatory());
            entity.setRequiredLevel(requirement.requiredLevel());
            languageJobOfferRepository.save(entity);
        }
    }

    /** Charge les exigences et assemble la réponse complète (dans la transaction courante). */
    JobOfferResponse toResponse(JobOffer offer) {
        return JobOfferResponse.of(
                offer,
                skillJobOfferRepository.findByJobOfferIdFetchSkill(offer.getId()),
                degreeJobOfferRepository.findByJobOfferIdFetchDegree(offer.getId()),
                languageJobOfferRepository.findByJobOfferIdFetchLanguage(offer.getId()));
    }
}
