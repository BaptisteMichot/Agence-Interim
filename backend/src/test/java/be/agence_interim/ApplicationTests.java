package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import be.agence_interim.dto.MyApplicationResponse;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ApplicationService;
import be.agence_interim.service.EmployerApplicationService;
import be.agence_interim.service.JobOfferService;

/**
 * La candidature, des deux côtés : celui qui postule et celui qui reçoit.
 *
 * <p>C'est le seul objet de l'application que les deux parties manipulent en même temps
 * sans se voir. L'intérimaire postule, se ravise, repostule ; l'employeur consulte, note,
 * classe. Les deux vues portent sur la même ligne en base, et c'est là que les règles se
 * contredisent le plus facilement — une candidature annulée qui resterait visible côté
 * employeur ferait perdre son temps à quelqu'un, dans les deux sens.
 */
@SpringBootTest
class ApplicationTests {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private EmployerApplicationService employerApplicationService;

    @Autowired
    private JobOfferService jobOfferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private User employer;
    private User worker;
    private int offerId;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        employer = fixtures.employer;
        worker = fixtures.worker;
        offerId = fixtures.offer.getId();
    }

    // ------------------------------------------------------------------- côté candidat

    @Test
    @DisplayName("Postuler à une offre ouverte crée une candidature en cours")
    void applyingToAnOpenOfferCreatesAPendingApplication() {
        MyApplicationResponse application = applicationService.apply(worker.getId(), offerId);

        assertThat(application.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.applicationTime()).isNotNull();
        assertThat(application.offer().id()).isEqualTo(offerId);
        assertThat(applicationService.pendingCount(worker.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("On ne postule pas deux fois à la même offre")
    void nobodyAppliesTwiceToTheSameOffer() {
        // Deux candidatures identiques feraient deux lignes dans l'écran de l'employeur
        // pour une seule personne, et fausseraient le compteur de son tableau de bord.
        applicationService.apply(worker.getId(), offerId);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> applicationService.apply(worker.getId(), offerId))
                .withMessageContaining("déjà postulé");
    }

    @Test
    @DisplayName("Une offre clôturée ne reçoit plus de candidature")
    void aclosedOfferReceivesNoMoreApplications() {
        // Le poste est pourvu ou retiré : accepter encore des candidatures reviendrait à
        // laisser des gens postuler dans le vide, sans que personne ne relève.
        jobOfferService.close(employer.getId(), offerId);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> applicationService.apply(worker.getId(), offerId))
                .withMessageContaining("clôturée");
    }

    @Test
    @DisplayName("Repostuler après s'être rétracté réactive la candidature au lieu d'en créer une seconde")
    void reapplyingAfterWithdrawingRevivesTheSameApplication() {
        // Changer d'avis est légitime, mais l'historique doit rester lisible : une seule
        // ligne par couple candidat-offre, avec sa date remise à l'heure du nouvel envoi.
        int applicationId = applicationService.apply(worker.getId(), offerId).id();
        applicationService.cancel(worker.getId(), applicationId);

        MyApplicationResponse again = applicationService.apply(worker.getId(), offerId);

        assertThat(again.id()).isEqualTo(applicationId);
        assertThat(again.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(applicationRepository.findByJobSeekerIdAndJobOfferId(worker.getId(), offerId))
                .isPresent();
    }

    @Test
    @DisplayName("La note donnée avant un retrait ne survit pas à la nouvelle candidature")
    void theratingGivenBeforeAWithdrawalDoesNotSurvive() {
        // La note est le jugement de l'employeur sur une candidature qu'il a examinée. La
        // personne se retire puis revient : c'est une nouvelle démarche, que l'employeur
        // doit réexaminer plutôt que de retrouver classée d'avance.
        int applicationId = applicationService.apply(worker.getId(), offerId).id();
        employerApplicationService.rate(employer.getId(), applicationId, 5);
        applicationService.cancel(worker.getId(), applicationId);

        applicationService.apply(worker.getId(), offerId);

        assertThat(applicationRepository.findById(applicationId).orElseThrow().getRating()).isNull();
    }

    @Test
    @DisplayName("Une candidature ne s'annule qu'une fois, et seulement par celui qui l'a déposée")
    void anapplicationIsWithdrawnOnceAndOnlyByItsAuthor() {
        int applicationId = applicationService.apply(worker.getId(), offerId).id();
        User other = fixtures.worker("autre");

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> applicationService.cancel(other.getId(), applicationId));

        assertThat(applicationService.cancel(worker.getId(), applicationId).status())
                .isEqualTo(ApplicationStatus.CANCELED);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> applicationService.cancel(worker.getId(), applicationId))
                .withMessageContaining("déjà annulée");
    }

    @Test
    @DisplayName("Le candidat garde ses candidatures annulées dans son suivi")
    void thecandidateKeepsWithdrawnApplicationsInHisOwnList() {
        // Côté employeur elles disparaissent ; côté candidat elles restent, parce que c'est
        // son historique de recherche d'emploi et qu'il est seul à le lire.
        int applicationId = applicationService.apply(worker.getId(), offerId).id();
        applicationService.cancel(worker.getId(), applicationId);

        assertThat(applicationService.mine(worker.getId(), PageRequest.of(0, 20)).content())
                .extracting(application -> application.id())
                .contains(applicationId);
        assertThat(applicationService.pendingCount(worker.getId())).isZero();
    }

    // ------------------------------------------------------------------ côté employeur

    @Test
    @DisplayName("Les candidatures annulées disparaissent de la liste de l'employeur")
    void withdrawnApplicationsLeaveTheEmployerList() {
        int kept = applicationService.apply(worker.getId(), offerId).id();
        User leaving = fixtures.worker("retire");
        int withdrawn = applicationService.apply(leaving.getId(), offerId).id();
        applicationService.cancel(leaving.getId(), withdrawn);

        assertThat(offerApplicationIds()).contains(kept).doesNotContain(withdrawn);
        assertThat(employerApplicationService.pendingCount(employer.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Le tri par note place les candidatures non notées en dernier")
    void sortingByRatingPutsUnratedApplicationsLast() {
        // Demande 12 de l'analyse. Une candidature sans note n'est pas une mauvaise
        // candidature : c'est une candidature que l'employeur n'a pas encore ouverte. La
        // classer avec les zéros la ferait passer à la trappe.
        int rated = applicationService.apply(worker.getId(), offerId).id();
        int unrated = applicationService.apply(fixtures.worker("sans-note").getId(), offerId).id();
        employerApplicationService.rate(employer.getId(), rated, 3);

        assertThat(offerApplicationIds("rating-desc")).containsExactly(rated, unrated);
    }

    @Test
    @DisplayName("Le tri par date propose les deux sens")
    void sortingByDateWorksBothWays() {
        int first = applicationService.apply(worker.getId(), offerId).id();
        int second = applicationService.apply(fixtures.worker("suivant").getId(), offerId).id();

        assertThat(offerApplicationIds("date-asc")).containsExactly(first, second);
        assertThat(offerApplicationIds("date-desc")).containsExactly(second, first);
    }

    @Test
    @DisplayName("Un employeur ne note et ne consulte que les candidatures reçues sur ses propres offres")
    void anemployerOnlyReachesApplicationsSentToHisOwnOffers() {
        int applicationId = applicationService.apply(worker.getId(), offerId).id();
        User outsider = fixtures.employer("concurrent");

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> employerApplicationService.rate(outsider.getId(), applicationId, 5));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> employerApplicationService.candidateProfile(outsider.getId(), applicationId));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> employerApplicationService.listForOffer(outsider.getId(), offerId, null, 0, 20));
    }

    @Test
    @DisplayName("Le profil du candidat s'ouvre depuis sa candidature, et se referme avec son retrait")
    void thecandidateProfileOpensFromTheApplicationAndClosesWithIt() {
        // L'employeur accède aux compétences, diplômes et expériences d'un inconnu parce
        // que celui-ci a postulé chez lui. Le retrait de la candidature retire aussi ce
        // droit de regard : c'est la candidature qui le fondait, pas l'identité.
        int applicationId = applicationService.apply(worker.getId(), offerId).id();

        assertThat(employerApplicationService.candidateProfile(employer.getId(), applicationId))
                .satisfies(profile -> {
                    assertThat(profile.userId()).isEqualTo(worker.getId());
                    assertThat(profile.offerId()).isEqualTo(offerId);
                    assertThat(profile.hasCv()).isFalse();
                });

        applicationService.cancel(worker.getId(), applicationId);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> employerApplicationService.candidateProfile(employer.getId(), applicationId));
    }

    // ------------------------------------------------------------------------------ outils

    private List<Integer> offerApplicationIds() {
        return offerApplicationIds(null);
    }

    private List<Integer> offerApplicationIds(String sort) {
        return employerApplicationService.listForOffer(employer.getId(), offerId, sort, 0, 20)
                .content().stream().map(application -> application.id()).toList();
    }
}
