package be.agence_interim.service;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.JobOffer;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.MatchingService.MatchScore;
import be.agence_interim.service.MatchingService.OfferRequirements;

/**
 * Contact automatique des candidats correspondants lors de la publication d'une
 * offre
 * (exécuté en tâche de fond pour ne pas ralentir la réponse à l'employeur).
 */
@Service
public class MatchNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MatchNotificationService.class);

    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final MailService mailService;
    private final String frontendUrl;

    public MatchNotificationService(
            JobOfferRepository jobOfferRepository,
            UserRepository userRepository,
            MatchingService matchingService,
            MailService mailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.jobOfferRepository = jobOfferRepository;
        this.userRepository = userRepository;
        this.matchingService = matchingService;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Évalue tous les intérimaires contre l'offre et contacte ceux qui
     * correspondent
     * (exigences obligatoires satisfaites + score suffisant).
     */
    @Async
    @Transactional(readOnly = true)
    public void notifyMatchingJobSeekers(int offerId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new NoSuchElementException("Offre introuvable."));
        OfferRequirements requirements = matchingService.loadRequirements(offer);

        int contacted = 0;
        for (User jobSeeker : userRepository.findByRole(Role.JOBSEEKER)) {
            MatchScore match = matchingService.score(jobSeeker, requirements);
            if (match.shouldContact()) {
                mailService.send(
                        jobSeeker.getEmail(),
                        "Une offre correspond à votre profil : " + offer.getTitle(),
                        buildBody(jobSeeker, offer, match));
                contacted++;
            }
        }
        log.info("Offre #{} « {} » : {} candidat(s) correspondant(s) contacté(s).",
                offer.getId(), offer.getTitle(), contacted);
    }

    private String buildBody(User jobSeeker, JobOffer offer, MatchScore match) {
        return "Bonjour " + jobSeeker.getFirstName() + ",\n\n"
                + "Une nouvelle offre d'emploi correspond à votre profil à " + match.score() + " % :\n\n"
                + "  " + offer.getTitle() + "\n"
                + "  " + offer.getEmployer().getCompanyName() + " — " + offer.getCity()
                + " (" + offer.getSector() + ")\n\n"
                + "Si elle vous intéresse, consultez-la et postulez ici :\n"
                + frontendUrl + "/interimaire/offres/" + offer.getId() + "\n\n"
                + "L'agence d'intérim";
    }
}
