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
 * Diplôme requis par une offre d'emploi, avec son caractère obligatoire,
 * le type et la section attendus.
 */
@Entity
@Table(name = "degree_job_offer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DegreeJobOffer {

    public static final int REQUIRED_TYPE_MAX_LENGTH = 10;
    public static final int REQUIRED_SECTION_MAX_LENGTH = 30;

    @EmbeddedId
    private DegreeJobOfferId id = new DegreeJobOfferId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idDegree")
    @JoinColumn(name = "id_degree", nullable = false)
    private Degree degree;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idJobOffer")
    @JoinColumn(name = "id_job_offer", nullable = false)
    private JobOffer jobOffer;

    @Column(nullable = false)
    private Boolean isMandatory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = REQUIRED_TYPE_MAX_LENGTH)
    private DegreeType requiredType;

    @Column(nullable = true, length = REQUIRED_SECTION_MAX_LENGTH)
    private String requiredSection;
}
