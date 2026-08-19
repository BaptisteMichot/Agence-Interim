package be.agence_interim.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clé composite de l'association {@link DegreeUser} : (diplôme, utilisateur).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class DegreeUserId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_degree")
    private int idDegree;

    @Column(name = "id_user")
    private int idUser;
}
