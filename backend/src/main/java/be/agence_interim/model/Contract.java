package be.agence_interim.model;

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
 * Contrat généré pour une mission validée. Chaque partie le signe en confirmant un
 * code reçu par email ; la date de signature est conservée et reprise sur le document.
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
public class Contract {

    public static final int STATUS_MAX_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mission", nullable = false)
    private Mission mission;

    private LocalDateTime generationTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private SignatureStatus statusEmployer = SignatureStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private SignatureStatus statusWorker = SignatureStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String contractFilePath;

    /** Date de la signature de l'entreprise utilisatrice. */
    private LocalDateTime employerSignedAt;

    /** Date de la signature du travailleur intérimaire. */
    private LocalDateTime workerSignedAt;

}
