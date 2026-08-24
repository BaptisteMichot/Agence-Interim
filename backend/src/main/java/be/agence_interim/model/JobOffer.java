package be.agence_interim.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Offre d'emploi publiée par un employeur.
 */
@Entity
@Table(name = "job_offer")
@Getter
@Setter
@NoArgsConstructor
public class JobOffer {

    /** Borne de la description : un texte libre sans plafond est une entrée non validée. */
    public static final int DESCRIPTION_MAX_LENGTH = 5000;

    public static final int TITLE_MAX_LENGTH = 50;
    public static final int SECTOR_MAX_LENGTH = 20;
    public static final int CITY_MAX_LENGTH = 20;
    public static final int PROVINCE_MAX_LENGTH = 20;
    public static final int EXPERIENCE_TIME_MAX_LENGTH = 5;
    public static final int STATUS_MAX_LENGTH = 8;

    /**
     * Valeur faciale maximale d'un titre-repas, en vigueur depuis le 1er janvier 2026 :
     * au-delà, l'avantage perd son exonération de cotisations sociales (AR du 28
     * novembre 1969, art. 19bis §2). Déclarée en chaîne parce que les annotations de
     * validation n'acceptent que des constantes de compilation ; à mettre à jour ici,
     * et nulle part ailleurs, si le barème change.
     */
    public static final String MEAL_VOUCHER_MAX_AMOUNT = "10.00";

    /** Un montant renseigné doit être réel : un chèque de 0 € n'est pas un avantage. */
    public static final String MEAL_VOUCHER_MIN_AMOUNT = "0.01";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employer", nullable = false)
    private User employer;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = SECTOR_MAX_LENGTH)
    private Sector sector;

    @Column(nullable = false, length = CITY_MAX_LENGTH)
    private String city;

    /** Maille de recherche géographique : la ville seule ne permet pas d'élargir. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = PROVINCE_MAX_LENGTH)
    private Province province;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private LocalDateTime publishedAt;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    @Column(length = EXPERIENCE_TIME_MAX_LENGTH)
    private String experienceTime;

    private Boolean vehicleMandatory;

    /**
     * Valeur faciale du titre-repas accordé par journée prestée, ou {@code null} si
     * l'employeur n'en accorde pas : l'avantage est facultatif, un seul champ suffit
     * donc à le décrire — nul signifie « pas de chèques-repas ».
     */
    private BigDecimal mealVoucherAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private JobOfferStatus status = JobOfferStatus.OPEN;
}
