package be.agence_interim.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.DegreeUser;
import be.agence_interim.model.Experience;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.LanguageUser;
import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.SkillUser;
import be.agence_interim.model.User;
import be.agence_interim.repository.DegreeJobOfferRepository;
import be.agence_interim.repository.DegreeUserRepository;
import be.agence_interim.repository.ExperienceRepository;
import be.agence_interim.repository.LanguageJobOfferRepository;
import be.agence_interim.repository.LanguageUserRepository;
import be.agence_interim.repository.SkillJobOfferRepository;
import be.agence_interim.repository.SkillUserRepository;

/**
 * Correspondance intelligente entre le profil d'un intérimaire et une offre d'emploi.
 * L'algorithme est déterministe (basé sur des règles) : mêmes données → même score.
 *
 * PRINCIPE GÉNÉRAL
 * Une offre définit une liste de critères : compétences requises, diplômes requis,
 * langues requises, véhicule obligatoire, expérience minimum. Pour un candidat donné,
 * chaque critère reçoit :
 * - un taux de satisfaction entre 0 et 1 (0 = pas du tout satisfait, 1 = totalement
 *   satisfait, entre les deux = crédit partiel) ;
 * - un poids : 2 si le critère est marqué obligatoire par l'employeur, 1 s'il est
 *   optionnel. Un critère obligatoire compte donc double.
 *
 * Le score final est une moyenne pondérée, ramenée sur 100 :
 *     score = 100 × Σ(poids × taux) / Σ(poids)
 *
 * RÈGLE D'EXCLUSION
 * Indépendamment du score, chaque critère obligatoire doit être satisfait à 100 %.
 * Si un seul critère obligatoire est partiellement ou pas satisfait, le candidat est
 * écarté (mandatoryOk = false) et l'offre n'apparaît pas dans son onglet « Pour moi ».
 * Le véhicule obligatoire est par nature un critère obligatoire.
 *
 * QUI DÉCLENCHE LE CALCUL
 * Le score n'est calculé qu'à la demande du candidat, quand il ouvre « Pour moi ».
 * L'application a longtemps contacté d'office par email tout candidat dépassant un
 * seuil à la publication d'une offre ; cet envoi a été retiré, faute de pouvoir offrir
 * un moyen de s'y opposer. Le rapprochement reste, la sollicitation non : c'est le
 * candidat qui vient voir ce qui lui correspond.
 *
 * EXEMPLE CHIFFRÉ
 * Offre : Cariste niveau Avancé (obligatoire), Anglais B2 (optionnel).
 * Candidat : Cariste Expert, Anglais B1.
 * - Cariste : Expert ≥ Avancé → taux 1, poids 2 (obligatoire) → contribue 2 × 1 = 2
 * - Anglais : B1 < B2 → crédit partiel (2+1)/(3+1) = 0,75, poids 1 → contribue 0,75
 * score = 100 × (2 + 0,75) / (2 + 1) = 100 × 2,75 / 3 ≈ 92 %, et le candidat est
 * retenu car son seul critère obligatoire (Cariste) est satisfait à 100 %.
 */
@Service
public class MatchingService {

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

    /**
     * Résultat du matching pour un couple (candidat, offre).
     *
     * @param mandatoryOk true si TOUS les critères obligatoires sont satisfaits à
     *                    100 %
     * @param score       moyenne pondérée des taux de satisfaction, de 0 à 100
     */
    public record MatchScore(boolean mandatoryOk, int score) {
    }

    /**
     * Exigences d'une offre, chargées une seule fois puis réutilisées pour évaluer
     * tous les candidats (évite de relire l'offre en base à chaque candidat).
     */
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

    /**
     * Profil d'un candidat, chargé une seule fois puis réutilisé pour évaluer
     * plusieurs offres (évite de relire le profil en base à chaque offre).
     * Les compétences et langues sont indexées par identifiant pour être
     * retrouvées en O(1) quand on parcourt les exigences d'une offre. Les
     * expériences sont gardées telles quelles : la durée d'une expérience « en
     * cours » dépend du jour courant et n'est calculée qu'au moment du score.
     */
    public record CandidateProfile(
            User jobSeeker,
            Map<Integer, SkillLevel> skills,
            Map<Integer, LanguageLevel> languages,
            List<Integer> degreeIds,
            List<Experience> experiences) {
    }

    /** Charge le profil d'un candidat (compétences, langues, diplômes, expériences). */
    @Transactional(readOnly = true)
    public CandidateProfile loadProfile(User jobSeeker) {
        int userId = jobSeeker.getId();
        return buildProfile(
                jobSeeker,
                skillUserRepository.findByUserIdFetchSkill(userId),
                languageUserRepository.findByUserIdFetchLanguage(userId),
                degreeUserRepository.findByUserIdFetchDegree(userId),
                experienceRepository.findByUserIdOrderByStartDateDesc(userId));
    }

    private CandidateProfile buildProfile(
            User jobSeeker,
            List<SkillUser> skills,
            List<LanguageUser> languages,
            List<DegreeUser> degrees,
            List<Experience> experiences) {
        return new CandidateProfile(
                jobSeeker,
                skills.stream().collect(Collectors.toMap(su -> su.getSkill().getId(), su -> su.getLevel())),
                languages.stream().collect(Collectors.toMap(lu -> lu.getLanguage().getId(), lu -> lu.getLevel())),
                degrees.stream().map(du -> du.getDegree().getId()).toList(),
                experiences);
    }

    /**
     * Évalue la correspondance entre un intérimaire et une offre.
     *
     * Déroulement : (1) évaluer chaque critère de l'offre (taux de satisfaction
     * + poids) dans l'accumulateur, (2) en déduire le score final et
     * l'éligibilité. Le profil du candidat a été chargé au préalable par
     * {@link #loadProfile}, une seule fois quel que soit le nombre d'offres
     * évaluées.
     */
    public MatchScore score(CandidateProfile profile, OfferRequirements requirements) {

        // ---------- ÉTAPE 1 : évaluer chaque critère de l'offre ----------
        ScoreAccumulator accumulator = new ScoreAccumulator();

        // 1a. Compétences requises : crédit partiel si le niveau du candidat est
        // inférieur au niveau demandé (voir levelRatio).
        for (SkillJobOffer required : requirements.skills()) {
            SkillLevel userLevel = profile.skills().get(required.getSkill().getId());
            double ratio = levelRatio(
                    userLevel == null ? null : userLevel.ordinal(),
                    required.getRequiredLevel() == null ? null : required.getRequiredLevel().ordinal());
            accumulator.add(ratio, Boolean.TRUE.equals(required.getIsMandatory()));
        }

        // 1b. Diplômes requis : pas de notion de niveau → tout ou rien
        // (1 si le candidat possède le diplôme, 0 sinon).
        for (DegreeJobOffer required : requirements.degrees()) {
            double ratio = profile.degreeIds().contains(required.getDegree().getId()) ? 1.0 : 0.0;
            accumulator.add(ratio, Boolean.TRUE.equals(required.getIsMandatory()));
        }

        // 1c. Langues requises : même logique de crédit partiel que les compétences,
        // avec les niveaux CECR (A1 < A2 < B1 < B2 < C1 < C2).
        for (LanguageJobOffer required : requirements.languages()) {
            LanguageLevel userLevel = profile.languages().get(required.getLanguage().getId());
            double ratio = levelRatio(
                    userLevel == null ? null : userLevel.ordinal(),
                    required.getRequiredLevel() == null ? null : required.getRequiredLevel().ordinal());
            accumulator.add(ratio, Boolean.TRUE.equals(required.getIsMandatory()));
        }

        // 1d. Véhicule : n'est un critère QUE si l'offre l'exige. Dans ce cas il est
        // obligatoire par nature (tout ou rien).
        if (Boolean.TRUE.equals(requirements.offer().getVehicleMandatory())) {
            accumulator.add(Boolean.TRUE.equals(profile.jobSeeker().getHasVehicle()) ? 1.0 : 0.0, true);
        }

        // 1e. Expérience minimum : critère optionnel, proportionnel aux années
        // accumulées. Ex. 2 ans d'expérience quand l'offre en demande 4 → 0,5.
        Integer requiredYears = parseYears(requirements.offer().getExperienceTime());
        if (requiredYears != null && requiredYears > 0) {
            accumulator.add(Math.min(1.0, totalExperienceYears(profile.experiences()) / requiredYears), false);
        }

        // ---------- ÉTAPE 2 : score final ----------
        return accumulator.toScore();
    }

    /**
     * Taux de satisfaction d'un critère à niveaux (compétence ou langue).
     *
     * Les niveaux sont comparés par leur position dans l'énumération (ordinal) :
     * pour les compétences Débutant=0, Intermédiaire=1, Avancé=2, Expert=3 ;
     * pour les langues A1=0 … C2=5.
     *
     * - Candidat sans cette compétence/langue → 0 ;
     * - niveau du candidat ≥ niveau requis → 1 (pleinement satisfait) ;
     * - niveau inférieur → crédit partiel : (position candidat + 1) / (position requise + 1).
     *   Le « +1 » évite qu'un débutant (position 0) obtienne 0 alors qu'il possède
     *   la compétence. Ex. Intermédiaire (1) demandé Avancé (2) → 2/3 ≈ 0,67.
     */
    private double levelRatio(Integer userOrdinal, Integer requiredOrdinal) {
        if (userOrdinal == null) {
            return 0.0; // compétence/langue absente du profil
        }
        if (requiredOrdinal == null || userOrdinal >= requiredOrdinal) {
            return 1.0; // niveau suffisant (ou aucun niveau exigé)
        }
        return (userOrdinal + 1.0) / (requiredOrdinal + 1.0); // crédit partiel
    }

    /**
     * Petit collecteur qui applique la formule du score au fil des critères.
     *
     * Pour chaque critère on lui donne le taux de satisfaction (0 à 1) et si le
     * critère est obligatoire. Il maintient trois informations :
     * - weightedSum : Σ(poids × taux), le numérateur de la formule ;
     * - totalWeight : Σ(poids), le dénominateur ;
     * - mandatoryOk : passe (définitivement) à false dès qu'un critère obligatoire
     *   n'est pas satisfait à 100 %.
     */
    private static final class ScoreAccumulator {

        /** Poids d'un critère obligatoire : compte double dans le score. */
        private static final double MANDATORY_WEIGHT = 2.0;
        /** Poids d'un critère optionnel. */
        private static final double OPTIONAL_WEIGHT = 1.0;
        /**
         * Tolérance de comparaison : les taux sont des nombres à virgule flottante,
         * on considère « satisfait à 100 % » tout taux ≥ 0,999 plutôt que == 1.0.
         */
        private static final double FULLY_SATISFIED = 0.999;

        private double weightedSum;
        private double totalWeight;
        private boolean mandatoryOk = true;

        /** Enregistre un critère évalué. */
        void add(double ratio, boolean mandatory) {
            double weight = mandatory ? MANDATORY_WEIGHT : OPTIONAL_WEIGHT;
            weightedSum += weight * ratio;
            totalWeight += weight;
            if (mandatory && ratio < FULLY_SATISFIED) {
                mandatoryOk = false; // un critère obligatoire non satisfait → candidat écarté
            }
        }

        /** Applique la formule finale : score = 100 × Σ(poids × taux) / Σ(poids). */
        MatchScore toScore() {
            // Offre sans aucune exigence : elle correspond à tout le monde (100 %).
            int score = totalWeight == 0 ? 100 : (int) Math.round(100.0 * weightedSum / totalWeight);
            return new MatchScore(mandatoryOk, score);
        }
    }

    /**
     * Années d'expérience cumulées du candidat : somme des durées de toutes ses
     * expériences professionnelles. Une expérience sans date de fin (« en cours »)
     * compte jusqu'à aujourd'hui. Résultat en années décimales (730 jours ≈ 2,0
     * ans).
     */
    private double totalExperienceYears(List<Experience> experiences) {
        long totalDays = 0;
        for (Experience experience : experiences) {
            LocalDate end = experience.getEndDate() != null ? experience.getEndDate() : LocalDate.now();
            if (end.isAfter(experience.getStartDate())) {
                totalDays += ChronoUnit.DAYS.between(experience.getStartDate(), end);
            }
        }
        return totalDays / 365.0;
    }

    /**
     * Convertit le champ texte {@code experienceTime} de l'offre en années (null si
     * vide/invalide).
     */
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
