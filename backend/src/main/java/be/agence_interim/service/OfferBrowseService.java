package be.agence_interim.service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.JobOfferResponse;
import be.agence_interim.dto.JobOfferSummaryResponse;
import be.agence_interim.dto.MatchingOfferResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.FavoriteJobOffer;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.FavoriteJobOfferRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.MatchingService.CandidateProfile;
import be.agence_interim.service.MatchingService.MatchScore;

/** Consultation des offres par l'intérimaire et gestion de ses favoris. */
@Service
public class OfferBrowseService {

    private final JobOfferRepository jobOfferRepository;
    private final FavoriteJobOfferRepository favoriteRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobOfferService jobOfferService;
    private final MatchingService matchingService;

    public OfferBrowseService(
            JobOfferRepository jobOfferRepository,
            FavoriteJobOfferRepository favoriteRepository,
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            JobOfferService jobOfferService,
            MatchingService matchingService) {
        this.jobOfferRepository = jobOfferRepository;
        this.favoriteRepository = favoriteRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.jobOfferService = jobOfferService;
        this.matchingService = matchingService;
    }

    /** Une page des offres ouvertes, les plus récentes d'abord. */
    @Transactional(readOnly = true)
    public PageResponse<JobOfferSummaryResponse> browseOpen(int jobSeekerId, Pageable pageable) {
        Page<JobOffer> page = jobOfferRepository.findByStatusFetchEmployer(JobOfferStatus.OPEN, pageable);
        Set<Integer> favorites = favoritesAmong(jobSeekerId, page.getContent());
        return PageResponse.of(
                page, offer -> JobOfferSummaryResponse.forJobSeeker(offer, favorites.contains(offer.getId())));
    }

    /**
     * Parmi les offres de la page, celles qui sont en favori : une seule requête, bornée
     * à la page. Le frontend n'a plus besoin de la liste entière des favoris pour savoir
     * quelles étoiles allumer.
     */
    private Set<Integer> favoritesAmong(int jobSeekerId, List<JobOffer> offers) {
        if (offers.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(favoriteRepository.findFavoriteOfferIdsIn(
                jobSeekerId, offers.stream().map(offer -> offer.getId()).toList()));
    }

    /** Détail d'une offre (même clôturée, pour consulter un favori devenu obsolète). */
    @Transactional(readOnly = true)
    public JobOfferResponse detail(int jobSeekerId, int offerId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
        return jobOfferService.toJobSeekerResponse(
                offer,
                favoriteRepository.existsByJobSeekerIdAndJobOfferId(jobSeekerId, offerId),
                applicationRepository.existsByJobSeekerIdAndJobOfferIdAndStatus(
                        jobSeekerId, offerId, ApplicationStatus.PENDING));
    }

    /** Une offre retenue et le score qu'elle obtient face au profil du candidat. */
    private record ScoredOffer(JobOffer offer, int score) {
    }

    /**
     * Une page des offres ouvertes correspondant au profil de l'intérimaire (exigences
     * obligatoires satisfaites), triées par score de correspondance décroissant.
     *
     * <p>Limite assumée : un score de correspondance ne s'exprime pas en SQL. Toutes les
     * offres ouvertes doivent donc être évaluées avant qu'un classement existe ; seul le
     * découpage en pages est fait en mémoire.
     */
    @Transactional(readOnly = true)
    public PageResponse<MatchingOfferResponse> matching(int jobSeekerId, int page, int size) {
        User jobSeeker = userRepository.findById(jobSeekerId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable."));
        // Le profil du candidat est chargé une seule fois pour toutes les offres.
        CandidateProfile profile = matchingService.loadProfile(jobSeeker);

        // Le tri est stable et la source est déjà ordonnée par date de publication : à
        // score égal, les offres gardent le même rang d'une page à l'autre.
        List<ScoredOffer> ranked = jobOfferRepository.findAllByStatusFetchEmployer(JobOfferStatus.OPEN)
                .stream()
                .map(offer -> {
                    MatchScore match = matchingService.score(profile, matchingService.loadRequirements(offer));
                    return match.mandatoryOk() ? new ScoredOffer(offer, match.score()) : null;
                })
                .filter(scored -> scored != null)
                .sorted(Comparator.comparingInt((ScoredOffer scored) -> scored.score()).reversed())
                .toList();

        int from = Math.min(page * size, ranked.size());
        List<ScoredOffer> pageContent = ranked.subList(from, Math.min(from + size, ranked.size()));
        Set<Integer> favorites = favoritesAmong(
                jobSeekerId, pageContent.stream().map(scored -> scored.offer()).toList());

        List<MatchingOfferResponse> content = pageContent.stream()
                .map(scored -> new MatchingOfferResponse(
                        JobOfferSummaryResponse.forJobSeeker(
                                scored.offer(), favorites.contains(scored.offer().getId())),
                        scored.score()))
                .toList();
        return PageResponse.of(content, page, size, ranked.size());
    }

    /** Une page des offres mises en favori, avec leur statut (une offre clôturée reste listée). */
    @Transactional(readOnly = true)
    public PageResponse<JobOfferSummaryResponse> favorites(int jobSeekerId, Pageable pageable) {
        return PageResponse.of(
                favoriteRepository.findByJobSeekerIdFetchOffer(jobSeekerId, pageable),
                favorite -> JobOfferSummaryResponse.forJobSeeker(favorite.getJobOffer(), true));
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
