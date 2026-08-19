package be.agence_interim.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Journée travaillée d'une mission : une ligne par jour presté, avec ses horaires
 * et la pause fixée par l'employeur, qui n'est pas rémunérée.
 */
@Entity
@Table(name = "daily_schedule")
@Getter
@Setter
@NoArgsConstructor
public class DailySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mission", nullable = false)
    private Mission mission;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** Début de la pause non rémunérée ; null lorsque la journée n'en comporte pas. */
    private LocalTime breakStart;

    /** Fin de la pause non rémunérée ; null lorsque la journée n'en comporte pas. */
    private LocalTime breakEnd;
}
