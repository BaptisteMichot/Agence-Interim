package be.agence_interim.model;

import java.time.LocalDate;

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
 * Demande d'attribution du rôle employeur, soumise par un utilisateur et
 * traitée par un administrateur.
 */
@Entity
@Table(name = "employer_access_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployerAccessRequest {

    public static final int STATUS_MAX_LENGTH = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false, length = STATUS_MAX_LENGTH)
    private String status;
}
