package be.agence_interim.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.MyApplicationResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;

/** Candidatures de l'intérimaire : postuler, suivre, annuler. */
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            JobOfferRepository jobOfferRepository,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.userRepository = userRepository;
    }

    /**
     * Postule à une offre ouverte. Une candidature annulée sur la même offre est
     * réactivée plutôt que dupliquée.
     */
    @Transactional
    public MyApplicationResponse apply(int jobSeekerId, int offerId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
        if (offer.getStatus() != JobOfferStatus.OPEN) {
            throw new IllegalArgumentException("Cette offre est clôturée.");
        }

        Application application = applicationRepository
                .findByJobSeekerIdAndJobOfferId(jobSeekerId, offerId)
                .orElse(null);
        if (application != null && application.getStatus() == ApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Vous avez déjà postulé à cette offre.");
        }
        if (application == null) {
            application = new Application();
            application.setJobOffer(offer);
            application.setJobSeeker(userRepository.getReferenceById(jobSeekerId));
        }
        application.setStatus(ApplicationStatus.PENDING);
        application.setApplicationTime(LocalDateTime.now());
        application.setRating(null);
        return MyApplicationResponse.fromEntity(applicationRepository.save(application));
    }

    /** Toutes les candidatures de l'intérimaire (y compris annulées), les plus récentes d'abord. */
    @Transactional(readOnly = true)
    public List<MyApplicationResponse> mine(int jobSeekerId) {
        return applicationRepository.findByJobSeekerIdFetchOffer(jobSeekerId)
                .stream().map(MyApplicationResponse::fromEntity).toList();
    }

    /** Identifiants des offres avec candidature en cours (pour marquer « déjà postulé »). */
    public List<Integer> appliedOfferIds(int jobSeekerId) {
        return applicationRepository.findOfferIdsByJobSeekerIdAndStatus(jobSeekerId, ApplicationStatus.PENDING);
    }

    /** Annule une candidature en cours de l'intérimaire. */
    @Transactional
    public MyApplicationResponse cancel(int jobSeekerId, int applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .filter(a -> a.getJobSeeker().getId() == jobSeekerId)
                .orElseThrow(() -> new NoSuchElementException("Candidature introuvable."));
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Cette candidature est déjà annulée.");
        }
        application.setStatus(ApplicationStatus.CANCELED);
        return MyApplicationResponse.fromEntity(applicationRepository.save(application));
    }
}
