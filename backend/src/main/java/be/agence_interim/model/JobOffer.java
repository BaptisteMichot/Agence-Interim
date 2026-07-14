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
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class JobOffer {

    public static final int TITLE_MAX_LENGTH = 50;
    public static final int SECTOR_MAX_LENGTH = 20;
    public static final int CITY_MAX_LENGTH = 20;
    public static final int EXPERIENCE_TIME_MAX_LENGTH = 5;
    public static final int STATUS_MAX_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employer", nullable = false)
    private User employer;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, length = SECTOR_MAX_LENGTH)
    private String sector;

    @Column(nullable = false, length = CITY_MAX_LENGTH)
    private String city;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = true)
    private LocalDateTime publishedAt;

    @Column(nullable = true)
    private BigDecimal salaryMin;

    @Column(nullable = true)
    private BigDecimal salaryMax;

    @Column(nullable = true, length = EXPERIENCE_TIME_MAX_LENGTH)
    private String experienceTime;

    @Column(nullable = true)
    private Boolean vehicleMandatory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private JobOfferStatus status = JobOfferStatus.OPEN;
}
