package be.agence_interim.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
 * Diplôme obtenu par un utilisateur, avec l'établissement et l'année d'obtention.
 */
@Entity
@Table(name = "degree_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DegreeUser {

    public static final int INSTITUTION_MAX_LENGTH = 50;

    @EmbeddedId
    private DegreeUserId id = new DegreeUserId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idDegree")
    @JoinColumn(name = "id_degree", nullable = false)
    private Degree degree;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idUser")
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(nullable = true, length = INSTITUTION_MAX_LENGTH)
    private String institution;

    @Column(nullable = true)
    private Integer graduationYear;
}
