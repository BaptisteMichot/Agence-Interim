package be.agence_interim.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * Mission d'intérim issue d'une candidature retenue. L'employeur y renseigne les
 * conditions reprises dans le contrat ; ses jours travaillés sont les
 * {@link DailySchedule} qui la référencent.
 */
@Entity
@Table(name = "mission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mission {

    public static final int STATUS_MAX_LENGTH = 8;
    public static final int POSITION_MAX_LENGTH = 50;
    public static final int WORKPLACE_MAX_LENGTH = 50;
    public static final int WORK_REASON_MAX_LENGTH = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_application", nullable = false)
    private Application application;

    /** Mission renouvelée par celle-ci ; null pour une première mission. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_previous_mission")
    private Mission previousMission;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private MissionStatus status = MissionStatus.PENDING;

    /** Intitulé du poste repris sur le contrat (pré-rempli avec le titre de l'offre). */
    @Column(nullable = false, length = POSITION_MAX_LENGTH)
    private String position;

    /** Lieu d'exécution de la mission (pré-rempli avec la ville de l'offre). */
    @Column(nullable = false, length = WORKPLACE_MAX_LENGTH)
    private String workplace;

    /** Salaire horaire brut convenu, en euros. */
    @Column(nullable = false)
    private BigDecimal hourlyWage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = WORK_REASON_MAX_LENGTH)
    private WorkReason workReason;

    /** Conditions particulières facultatives, reprises sur le contrat. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Motif du refus de l'agence, obligatoire lorsque la mission est refusée. */
    @Column(columnDefinition = "TEXT")
    private String refusalReason;
}
