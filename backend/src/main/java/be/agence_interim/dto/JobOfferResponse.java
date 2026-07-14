package be.agence_interim.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillLevel;

/** Offre d'emploi complète (champs + exigences), renvoyée à l'employeur ou à l'intérimaire. */
public record JobOfferResponse(
        int id,
        String title,
        String sector,
        String city,
        String description,
        LocalDateTime publishedAt,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String experienceTime,
        Boolean vehicleMandatory,
        JobOfferStatus status,
        String companyName,
        List<SkillRequirement> skills,
        List<DegreeRequirement> degrees,
        List<LanguageRequirement> languages) {

    public record SkillRequirement(int skillId, String name, boolean isMandatory, SkillLevel requiredLevel) {

        public static SkillRequirement fromEntity(SkillJobOffer entity) {
            return new SkillRequirement(
                    entity.getSkill().getId(),
                    entity.getSkill().getName(),
                    Boolean.TRUE.equals(entity.getIsMandatory()),
                    entity.getRequiredLevel());
        }
    }

    public record DegreeRequirement(int degreeId, DegreeType type, String section, boolean isMandatory) {

        public static DegreeRequirement fromEntity(DegreeJobOffer entity) {
            return new DegreeRequirement(
                    entity.getDegree().getId(),
                    entity.getDegree().getType(),
                    entity.getDegree().getSection(),
                    Boolean.TRUE.equals(entity.getIsMandatory()));
        }
    }

    public record LanguageRequirement(int languageId, String name, boolean isMandatory, LanguageLevel requiredLevel) {

        public static LanguageRequirement fromEntity(LanguageJobOffer entity) {
            return new LanguageRequirement(
                    entity.getLanguage().getId(),
                    entity.getLanguage().getName(),
                    Boolean.TRUE.equals(entity.getIsMandatory()),
                    entity.getRequiredLevel());
        }
    }

    public static JobOfferResponse of(
            JobOffer offer,
            List<SkillJobOffer> skills,
            List<DegreeJobOffer> degrees,
            List<LanguageJobOffer> languages) {
        return new JobOfferResponse(
                offer.getId(),
                offer.getTitle(),
                offer.getSector(),
                offer.getCity(),
                offer.getDescription(),
                offer.getPublishedAt(),
                offer.getSalaryMin(),
                offer.getSalaryMax(),
                offer.getExperienceTime(),
                offer.getVehicleMandatory(),
                offer.getStatus(),
                offer.getEmployer().getCompanyName(),
                skills.stream().map(SkillRequirement::fromEntity).toList(),
                degrees.stream().map(DegreeRequirement::fromEntity).toList(),
                languages.stream().map(LanguageRequirement::fromEntity).toList());
    }
}
