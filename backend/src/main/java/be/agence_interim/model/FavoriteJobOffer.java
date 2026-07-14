package be.agence_interim.model;

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
 * Offre ajoutée en favori par un intérimaire.
 */
@Entity
@Table(name = "favorite_job_offer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteJobOffer {

    @EmbeddedId
    private FavoriteJobOfferId id = new FavoriteJobOfferId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idJobSeeker")
    @JoinColumn(name = "id_job_seeker", nullable = false)
    private User jobSeeker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idJobOffer")
    @JoinColumn(name = "id_job_offer", nullable = false)
    private JobOffer jobOffer;
}
