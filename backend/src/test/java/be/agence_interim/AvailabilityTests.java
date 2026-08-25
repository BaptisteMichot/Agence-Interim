package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import be.agence_interim.dto.UnavailabilityRequest;
import be.agence_interim.dto.UnavailabilityResponse;
import be.agence_interim.model.Role;
import be.agence_interim.model.Unavailability;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UnavailabilityRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.AvailabilityService;
import be.agence_interim.service.MissionService;

/**
 * Les indisponibilités déclarées par l'intérimaire.
 *
 * <p>La demande 20 de l'analyse tient en une phrase : « un intérimaire doit pouvoir
 * modifier ses disponibilités à partir de J+8 ». Le délai de prévenance est ce qui rend
 * le planning utilisable par l'agence — sans lui, quelqu'un pourrait se déclarer
 * indisponible la veille d'une mission qu'on lui a trouvée. Il vaut dans les deux sens :
 * on n'ajoute pas plus qu'on ne retire une indisponibilité sur les huit prochains jours.
 *
 * <p>Le second garde-fou est la cohérence : une indisponibilité ne peut recouvrir ni une
 * autre indisponibilité, ni une journée de mission confirmée. Le planning est un seul
 * calendrier ; deux vérités sur la même heure n'en font pas un.
 */
@SpringBootTest
class AvailabilityTests {

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private MissionService missionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UnavailabilityRepository unavailabilityRepository;

    private MissionFixtures fixtures;
    private User worker;

    /** Premier jour que l'intérimaire peut encore déclarer. */
    private LocalDate editable;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        worker = fixtures.user("dispo", Role.JOBSEEKER);
        editable = AvailabilityService.editableFrom();
    }

    @Test
    @DisplayName("La première date modifiable est bien aujourd'hui plus huit jours")
    void thefirstEditableDateIsEightDaysFromToday() {
        // Le nombre est affiché à l'écran et repris dans le message d'erreur : le calculer
        // autrement ici que dans le service ne prouverait rien, alors on le confronte à la
        // règle telle qu'elle est écrite dans l'analyse.
        assertThat(AvailabilityService.editableFrom()).isEqualTo(LocalDate.now().plusDays(8));
        assertThat(AvailabilityService.NOTICE_DAYS).isEqualTo(8);
    }

    @Test
    @DisplayName("Une indisponibilité se déclare à partir de J+8, jamais avant")
    void anunavailabilityIsDeclaredFromTheEighthDayOn() {
        // La borne est inclusive : J+8 lui-même est déclarable, sinon le message qui
        // annonce cette date désignerait un jour encore interdit.
        assertThatNoException().isThrownBy(() -> declare(editable));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> declare(editable.minusDays(1)))
                .withMessageContaining("délai de 8 jours");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> declare(LocalDate.now()))
                .withMessageContaining("délai de 8 jours");
    }

    @Test
    @DisplayName("Le délai vaut aussi pour retirer une indisponibilité")
    void thenoticeAlsoAppliesToRemoval() {
        // C'est le sens le moins évident et le plus important : se rendre à nouveau
        // disponible au dernier moment déferait le planning que l'agence vient d'établir
        // en se fiant à la déclaration.
        int declared = declare(editable).id();
        int tooSoon = force(editable.minusDays(3));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> availabilityService.remove(worker.getId(), tooSoon))
                .withMessageContaining("délai de 8 jours");

        assertThatNoException().isThrownBy(() -> availabilityService.remove(worker.getId(), declared));
    }

    @Test
    @DisplayName("Une indisponibilité ne peut pas en recouvrir une autre")
    void anunavailabilityCannotOverlapAnother() {
        availabilityService.add(worker.getId(), new UnavailabilityRequest(
                editable, LocalTime.of(9, 0), LocalTime.of(13, 0), "Rendez-vous médical"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> availabilityService.add(worker.getId(), new UnavailabilityRequest(
                        editable, LocalTime.of(12, 0), LocalTime.of(17, 0), null)))
                .withMessageContaining("couvre déjà");

        // Deux plages qui se touchent sans se chevaucher restent acceptables : 13 h est la
        // fin de l'une et le début de l'autre, pas un moment couvert deux fois.
        assertThatNoException().isThrownBy(() -> availabilityService.add(
                worker.getId(), new UnavailabilityRequest(
                        editable, LocalTime.of(13, 0), LocalTime.of(17, 0), null)));
    }

    @Test
    @DisplayName("Une plage horaire doit finir après avoir commencé")
    void atimeRangeMustEndAfterItBegins() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> availabilityService.add(worker.getId(), new UnavailabilityRequest(
                        editable, LocalTime.of(17, 0), LocalTime.of(9, 0), null)))
                .withMessageContaining("heure de fin");
    }

    @Test
    @DisplayName("On ne se déclare pas indisponible sur une journée de mission confirmée")
    void nobodyDeclaresUnavailabilityOnAConfirmedMissionDay() {
        // La mission acceptée est un engagement contractuel : l'indisponibilité déclarée
        // après coup ne l'annulerait pas, elle rendrait seulement le planning menteur.
        MissionFixtures booked = confirmedMission();
        User bookedWorker = booked.worker;
        LocalDate missionDay = booked.start;

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> availabilityService.add(bookedWorker.getId(), new UnavailabilityRequest(
                        missionDay, LocalTime.of(9, 0), LocalTime.of(13, 0), null)))
                .withMessageContaining("mission confirmée");

        // En dehors des heures prestées, la journée reste déclarable : la mission va de
        // 8 h à 16 h 30, la soirée n'appartient pas à l'employeur.
        assertThatNoException().isThrownBy(() -> availabilityService.add(
                bookedWorker.getId(), new UnavailabilityRequest(
                        missionDay, LocalTime.of(18, 0), LocalTime.of(22, 0), null)));
    }

    @Test
    @DisplayName("Accepter une mission efface les indisponibilités qu'elle recouvre")
    void acceptingAMissionClearsTheUnavailabilitiesItCovers() {
        // L'intérimaire est averti du chevauchement avant d'accepter ; une fois qu'il a
        // accepté, garder les deux laisserait dans son calendrier un congé sur un jour
        // qu'il s'est engagé à travailler.
        MissionFixtures booked = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        LocalDate onMission = booked.start;
        LocalDate elsewhere = booked.end.plusDays(10);
        availabilityService.add(booked.worker.getId(), new UnavailabilityRequest(
                onMission, LocalTime.of(9, 0), LocalTime.of(13, 0), "Congé"));
        availabilityService.add(booked.worker.getId(), new UnavailabilityRequest(
                elsewhere, LocalTime.of(9, 0), LocalTime.of(13, 0), "Congé"));

        int missionId = missionService.create(
                booked.employer.getId(), booked.application().getId(), booked.request()).id();
        missionService.validate(booked.admin().getId(), missionId);
        missionService.accept(booked.worker.getId(), missionId);

        assertThat(availabilityService.list(booked.worker.getId(), onMission, onMission)).isEmpty();
        assertThat(availabilityService.list(booked.worker.getId(), elsewhere, elsewhere))
                .as("les congés hors mission ne sont pas touchés")
                .hasSize(1);
    }

    @Test
    @DisplayName("Le calendrier de chacun n'appartient qu'à lui")
    void everyonesCalendarIsTheirOwn() {
        int declared = declare(editable).id();
        User intruder = fixtures.user("intrus", Role.JOBSEEKER);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> availabilityService.remove(intruder.getId(), declared));
        assertThat(availabilityService.list(intruder.getId(), editable, editable)).isEmpty();
        assertThat(availabilityService.list(worker.getId(), editable, editable)).hasSize(1);
    }

    @Test
    @DisplayName("La consultation dit de chaque indisponibilité si elle est encore modifiable")
    void thelistSaysWhichEntriesAreStillEditable() {
        // C'est ce qui pose le cadenas dans le calendrier : l'intérimaire voit ce qu'il
        // peut encore changer avant d'essayer, plutôt que de se heurter à un refus.
        force(editable.minusDays(3));
        declare(editable);

        assertThat(availabilityService.list(worker.getId(), LocalDate.now(), editable))
                .hasSize(2)
                .extracting(entry -> entry.editable())
                .containsExactly(false, true);
    }

    // ------------------------------------------------------------------------------ outils

    private UnavailabilityResponse declare(LocalDate date) {
        return availabilityService.add(worker.getId(), new UnavailabilityRequest(
                date, LocalTime.of(9, 0), LocalTime.of(13, 0), "Congé"));
    }

    /**
     * Pose une indisponibilité dans le passé proche en contournant le service.
     *
     * <p>Le délai de prévenance interdit de la créer par la voie normale, mais une
     * déclaration faite il y a trois semaines pour la semaine prochaine est parfaitement
     * ordinaire — et c'est elle qu'on ne peut plus retirer.
     */
    private int force(LocalDate date) {
        Unavailability unavailability = new Unavailability();
        unavailability.setUser(worker);
        unavailability.setDate(date);
        unavailability.setStartTime(LocalTime.of(9, 0));
        unavailability.setEndTime(LocalTime.of(13, 0));
        return unavailabilityRepository.save(unavailability).getId();
    }

    /** Un jeu de données dont la mission est confirmée, journées prestées à l'appui. */
    private MissionFixtures confirmedMission() {
        MissionFixtures booked = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        int missionId = missionService.create(
                booked.employer.getId(), booked.application().getId(), booked.request()).id();
        missionService.validate(booked.admin().getId(), missionId);
        missionService.accept(booked.worker.getId(), missionId);
        return booked;
    }
}
