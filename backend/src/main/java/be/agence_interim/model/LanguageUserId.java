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
 * Clé composite de l'association {@link LanguageUser} : (langue, utilisateur).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LanguageUserId implements Serializable {

    @Column(name = "id_language")
    private int idLanguage;

    @Column(name = "id_user")
    private int idUser;
}
