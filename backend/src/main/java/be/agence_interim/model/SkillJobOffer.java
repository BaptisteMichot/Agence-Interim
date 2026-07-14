package be.agence_interim.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compétence requise par une offre d'emploi, avec son caractère obligatoire
 * et le niveau attendu.
 */
@Entity
@Table(name = "skill_job_offer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillJobOffer {

    @EmbeddedId
    private SkillJobOfferId id = new SkillJobOfferId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idSkill")
    @JoinColumn(name = "id_skill", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idJobOffer")
    @JoinColumn(name = "id_job_offer", nullable = false)
    private JobOffer jobOffer;

    @Column(nullable = false)
    private Boolean isMandatory;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    private SkillLevel requiredLevel;
}
