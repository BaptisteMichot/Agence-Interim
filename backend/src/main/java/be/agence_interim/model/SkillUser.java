package be.agence_interim.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Compétence possédée par un utilisateur, avec son niveau.
 */
@Entity
@Table(name = "skill_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillUser {

    @EmbeddedId
    private SkillUserId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idSkill")
    @JoinColumn(name = "id_skill", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idUser")
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private SkillLevel level;
}
