package be.agence_interim.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.JobOfferResponse;
import be.agence_interim.dto.JobOfferSummaryResponse;
import be.agence_interim.model.FavoriteJobOffer;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.repository.FavoriteJobOfferRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;

/** Consultation des offres par l'intérimaire et gestion de ses favoris. */
@Service
public class OfferBrowseService {

    private final JobOfferRepository jobOfferRepository;
    private final FavoriteJobOfferRepository favoriteRepository;
    private final UserRepository userRepository;
    private final JobOfferService jobOfferService;

    public OfferBrowseService(
            JobOfferRepository jobOfferRepository,
            FavoriteJobOfferRepository favoriteRepository,
            UserRepository userRepository,
            JobOfferService jobOfferService) {
        this.jobOfferRepository = jobOfferRepository;
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.jobOfferService = jobOfferService;
    }

    /** Toutes les offres ouvertes, les plus récentes d'abord. */
    @Transactional(readOnly = true)
    public List<JobOfferSummaryResponse> browseOpen() {
        return jobOfferRepository.findByStatusOrderByPublishedAtDesc(JobOfferStatus.OPEN)
                .stream().map(JobOfferSummaryResponse::fromEntity).toList();
    }

    /** Détail d'une offre (même clôturée, pour consulter un favori devenu obsolète). */
    @Transactional(readOnly = true)
    public JobOfferResponse detail(int offerId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
        return jobOfferService.toResponse(offer);
    }

    /** Identifiants des offres en favori (pour marquer les étoiles côté frontend). */
    public List<Integer> favoriteIds(int jobSeekerId) {
        return favoriteRepository.findOfferIdsByJobSeekerId(jobSeekerId);
    }

    /** Offres mises en favori, avec leur statut (une offre clôturée reste listée). */
    @Transactional(readOnly = true)
    public List<JobOfferSummaryResponse> favorites(int jobSeekerId) {
        return favoriteRepository.findByJobSeekerIdFetchOffer(jobSeekerId)
                .stream().map(favorite -> JobOfferSummaryResponse.fromEntity(favorite.getJobOffer())).toList();
    }

    @Transactional
    public void addFavorite(int jobSeekerId, int offerId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
        if (offer.getStatus() != JobOfferStatus.OPEN) {
            throw new IllegalArgumentException("Cette offre est clôturée.");
        }
        if (favoriteRepository.existsByJobSeekerIdAndJobOfferId(jobSeekerId, offerId)) {
            throw new IllegalArgumentException("Cette offre est déjà dans vos favoris.");
        }
        FavoriteJobOffer favorite = new FavoriteJobOffer();
        favorite.setJobSeeker(userRepository.getReferenceById(jobSeekerId));
        favorite.setJobOffer(offer);
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(int jobSeekerId, int offerId) {
        FavoriteJobOffer favorite = favoriteRepository.findByJobSeekerIdAndJobOfferId(jobSeekerId, offerId)
                .orElseThrow(() -> new NoSuchElementException("Cette offre n'est pas dans vos favoris."));
        favoriteRepository.delete(favorite);
    }
}
