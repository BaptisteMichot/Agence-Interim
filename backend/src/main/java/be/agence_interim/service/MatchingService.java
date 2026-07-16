package be.agence_interim.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.Experience;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.User;
import be.agence_interim.repository.DegreeJobOfferRepository;
import be.agence_interim.repository.DegreeUserRepository;
import be.agence_interim.repository.ExperienceRepository;
import be.agence_interim.repository.LanguageJobOfferRepository;
import be.agence_interim.repository.LanguageUserRepository;
import be.agence_interim.repository.SkillJobOfferRepository;
import be.agence_interim.repository.SkillUserRepository;

/**
 * Correspondance intelligente profil ↔ offre (score déterministe basé sur des règles).
 *
 * <p>Chaque critère de l'offre compte à poids égal dans le score (pourcentage de critères
 * satisfaits). Les critères marqués obligatoires doivent TOUS être satisfaits pour que le
 * candidat soit retenu. Le véhicule obligatoire est un critère obligatoire par nature ;
 * l'expérience minimum est un critère optionnel (comptabilisé dans le score).</p>
 */
@Service
public class MatchingService {

    /** Score minimal (en %) pour contacter automatiquement un candidat. */
    public static final int CONTACT_MIN_SCORE = 50;

    private final SkillJobOfferRepository skillJobOfferRepository;
    private final DegreeJobOfferRepository degreeJobOfferRepository;
    private final LanguageJobOfferRepository languageJobOfferRepository;
    private final SkillUserRepository skillUserRepository;
    private final DegreeUserRepository degreeUserRepository;
    private final LanguageUserRepository languageUserRepository;
    private final ExperienceRepository experienceRepository;

    public MatchingService(
            SkillJobOfferRepository skillJobOfferRepository,
            DegreeJobOfferRepository degreeJobOfferRepository,
            LanguageJobOfferRepository languageJobOfferRepository,
            SkillUserRepository skillUserRepository,
            DegreeUserRepository degreeUserRepository,
            LanguageUserRepository languageUserRepository,
            ExperienceRepository experienceRepository) {
        this.skillJobOfferRepository = skillJobOfferRepository;
        this.degreeJobOfferRepository = degreeJobOfferRepository;
        this.languageJobOfferRepository = languageJobOfferRepository;
        this.skillUserRepository = skillUserRepository;
        this.degreeUserRepository = degreeUserRepository;
        this.languageUserRepository = languageUserRepository;
        this.experienceRepository = experienceRepository;
    }

    /** Résultat du matching : critères obligatoires satisfaits ? + score global (0-100). */
    public record MatchScore(boolean mandatoryOk, int score) {

        /** Candidat à contacter automatiquement à la publication ? */
        public boolean shouldContact() {
            return mandatoryOk && score >= CONTACT_MIN_SCORE;
        }
    }

    /** Exigences d'une offre, chargées une fois pour évaluer plusieurs candidats. */
    public record OfferRequirements(
            JobOffer offer,
            List<SkillJobOffer> skills,
            List<DegreeJobOffer> degrees,
            List<LanguageJobOffer> languages) {
    }

    @Transactional(readOnly = true)
    public OfferRequirements loadRequirements(JobOffer offer) {
        return new OfferRequirements(
                offer,
                skillJobOfferRepository.findByJobOfferIdFetchSkill(offer.getId()),
                degreeJobOfferRepository.findByJobOfferIdFetchDegree(offer.getId()),
                languageJobOfferRepository.findByJobOfferIdFetchLanguage(offer.getId()));
    }

    /** Évalue la correspondance entre un intérimaire et une offre. */
    @Transactional(readOnly = true)
    public MatchScore score(User jobSeeker, OfferRequirements requirements) {
        int userId = jobSeeker.getId();
        Map<Integer, SkillLevel> userSkills = skillUserRepository.findByUserIdFetchSkill(userId)
                .stream().collect(Collectors.toMap(su -> su.getSkill().getId(), su -> su.getLevel()));
        Map<Integer, LanguageLevel> userLanguages = languageUserRepository.findByUserIdFetchLanguage(userId)
                .stream().collect(Collectors.toMap(lu -> lu.getLanguage().getId(), lu -> lu.getLevel()));
        List<Integer> userDegreeIds = degreeUserRepository.findByUserIdFetchDegree(userId)
                .stream().map(du -> du.getDegree().getId()).toList();

        int total = 0;
        int satisfied = 0;
        boolean mandatoryOk = true;

        for (SkillJobOffer required : requirements.skills()) {
            SkillLevel userLevel = userSkills.get(required.getSkill().getId());
            boolean ok = userLevel != null && required.getRequiredLevel() != null
                    ? userLevel.ordinal() >= required.getRequiredLevel().ordinal()
                    : userLevel != null;
            total++;
            satisfied += ok ? 1 : 0;
            mandatoryOk &= !Boolean.TRUE.equals(required.getIsMandatory()) || ok;
        }

        for (DegreeJobOffer required : requirements.degrees()) {
            boolean ok = userDegreeIds.contains(required.getDegree().getId());
            total++;
            satisfied += ok ? 1 : 0;
            mandatoryOk &= !Boolean.TRUE.equals(required.getIsMandatory()) || ok;
        }

        for (LanguageJobOffer required : requirements.languages()) {
            LanguageLevel userLevel = userLanguages.get(required.getLanguage().getId());
            boolean ok = userLevel != null && required.getRequiredLevel() != null
                    ? userLevel.ordinal() >= required.getRequiredLevel().ordinal()
                    : userLevel != null;
            total++;
            satisfied += ok ? 1 : 0;
            mandatoryOk &= !Boolean.TRUE.equals(required.getIsMandatory()) || ok;
        }

        if (Boolean.TRUE.equals(requirements.offer().getVehicleMandatory())) {
            boolean ok = Boolean.TRUE.equals(jobSeeker.getHasVehicle());
            total++;
            satisfied += ok ? 1 : 0;
            mandatoryOk &= ok;
        }

        Integer requiredYears = parseYears(requirements.offer().getExperienceTime());
        if (requiredYears != null) {
            boolean ok = totalExperienceYears(userId) >= requiredYears;
            total++;
            satisfied += ok ? 1 : 0;
        }

        // Aucune exigence : l'offre correspond à tout le monde.
        int score = total == 0 ? 100 : Math.round(100f * satisfied / total);
        return new MatchScore(mandatoryOk, score);
    }

    /** Années d'expérience cumulées du profil (une expérience en cours compte jusqu'à aujourd'hui). */
    private double totalExperienceYears(int userId) {
        long totalDays = 0;
        for (Experience experience : experienceRepository.findByUserIdOrderByStartDateDesc(userId)) {
            LocalDate end = experience.getEndDate() != null ? experience.getEndDate() : LocalDate.now();
            if (end.isAfter(experience.getStartDate())) {
                totalDays += ChronoUnit.DAYS.between(experience.getStartDate(), end);
            }
        }
        return totalDays / 365.0;
    }

    private Integer parseYears(String experienceTime) {
        if (experienceTime == null || experienceTime.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(experienceTime.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
