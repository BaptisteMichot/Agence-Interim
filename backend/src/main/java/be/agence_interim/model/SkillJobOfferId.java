package be.agence_interim.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clé composite de l'association {@link SkillJobOffer} : (compétence, offre).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class SkillJobOfferId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_skill")
    private int idSkill;

    @Column(name = "id_job_offer")
    private int idJobOffer;
}
