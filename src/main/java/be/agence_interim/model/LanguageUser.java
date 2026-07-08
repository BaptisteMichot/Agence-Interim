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
 * Langue parlée par un utilisateur, avec son niveau (ex. A1, C2).
 */
@Entity
@Table(name = "language_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LanguageUser {

    public static final int LEVEL_MAX_LENGTH = 2;

    @EmbeddedId
    private LanguageUserId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idLanguage")
    @JoinColumn(name = "id_language", nullable = false)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idUser")
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = LEVEL_MAX_LENGTH)
    private LanguageLevel level;
}
