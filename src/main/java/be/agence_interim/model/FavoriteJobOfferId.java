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
 * Clé composite de l'association {@link FavoriteJobOffer} : (intérimaire, offre).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriteJobOfferId implements Serializable {

    @Column(name = "id_job_seeker")
    private int idJobSeeker;

    @Column(name = "id_job_offer")
    private int idJobOffer;
}
