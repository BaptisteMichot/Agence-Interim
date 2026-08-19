package be.agence_interim.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clé composite de l'association {@link LanguageUser} : (langue, utilisateur).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LanguageUserId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_language")
    private int idLanguage;

    @Column(name = "id_user")
    private int idUser;
}
