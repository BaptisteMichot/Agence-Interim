package be.agence_interim.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clé composite de l'association {@link SkillUser} : (compétence, utilisateur).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SkillUserId implements Serializable {

    @Column(name = "id_skill")
    private int idSkill;

    @Column(name = "id_user")
    private int idUser;
}
