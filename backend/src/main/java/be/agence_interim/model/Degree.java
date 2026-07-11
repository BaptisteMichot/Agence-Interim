package be.agence_interim.model;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Diplôme du référentiel. Soit global, soit créé par un utilisateur (createdBy renseigné).
 */
@Entity
@Table(name = "degree")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Degree {

    public static final int TYPE_MAX_LENGTH = 10;
    public static final int SECTION_MAX_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = TYPE_MAX_LENGTH)
    private DegreeType type;

    @Column(nullable = false, length = SECTION_MAX_LENGTH)
    private String section;

    @Column(nullable = false)
    private Boolean isGlobal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    private User createdBy;
}
