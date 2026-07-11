package be.agence_interim.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Contrat généré pour une mission validée. Son envoi est simulé (cf. analyse).
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    public static final int STATUS_MAX_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mission", nullable = false)
    private Mission mission;

    @Column(nullable = true)
    private LocalDateTime generationTime;

    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private String statusEmployer;

    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private String statusWorker;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String contractFilePath;
}
