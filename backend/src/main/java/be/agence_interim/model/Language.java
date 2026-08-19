package be.agence_interim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Langue du référentiel.
 */
@Entity
@Table(name = "language")
@Getter
@Setter
@NoArgsConstructor
public class Language {

    public static final int NAME_MAX_LENGTH = 25;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;
}
