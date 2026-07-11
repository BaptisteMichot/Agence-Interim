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
 * Langue requise par une offre d'emploi, avec son caractère obligatoire
 * et le niveau attendu.
 */
@Entity
@Table(name = "language_job_offer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LanguageJobOffer {

    public static final int REQUIRED_LEVEL_MAX_LENGTH = 2;

    @EmbeddedId
    private LanguageJobOfferId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idLanguage")
    @JoinColumn(name = "id_language", nullable = false)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idJobOffer")
    @JoinColumn(name = "id_job_offer", nullable = false)
    private JobOffer jobOffer;

    @Column(nullable = false)
    private Boolean isMandatory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = REQUIRED_LEVEL_MAX_LENGTH)
    private LanguageLevel requiredLevel;
}
