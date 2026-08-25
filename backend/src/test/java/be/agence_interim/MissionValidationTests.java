package be.agence_interim;

import static be.agence_interim.MissionFixtures.day;
import static be.agence_interim.MissionFixtures.workingDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import be.agence_interim.dto.DailySlotRequest;
import be.agence_interim.dto.MissionRequest;
import be.agence_interim.dto.MissionResponse;
import be.agence_interim.dto.RefuseMissionRequest;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.MissionService;
import jakarta.validation.Validator;

/**
 * Ce qu'une mission refuse d'enregistrer.
 *
 * <p>Le contrat de travail intérimaire est un document réglementé, et l'analyse ajoute
 * ses propres exigences : salaire dans la fourchette annoncée (demande 14), fiche
 * entreprise complète (15), coordonnées de l'intérimaire complètes (16), refus motivé
 * (18). Une mission qui passerait au travers de ces contrôles produirait un contrat
 * irrégulier, et le défaut ne se verrait qu'au moment où quelqu'un lirait le PDF.
 *
 * <p>Les contrôles se répartissent en deux endroits, et c'est délibéré : la forme —
 * champ vide, longueur, format — est refusée par la validation des DTO, à l'entrée ; la
 * cohérence métier, qui suppose de connaître l'offre ou le profil, est refusée par le
 * service. Les deux sont éprouvés ici, chacun à son niveau.
 */
@SpringBootTest
class MissionValidationTests {

    @Autowired
    private MissionService missionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private Validator validator;

    private MissionFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = newFixtures();
    }

    // -------------------------------------------------------------------- rémunération

    @Test
    @DisplayName("Le salaire de la mission reste dans la fourchette annoncée dans l'offre")
    void thewageStaysInsideTheRangeAnnouncedByTheOffer() {
        // Demande 14 de l'analyse. L'offre a engagé l'employeur vis-à-vis de tous ceux qui
        // y ont postulé : proposer moins au moment de contractualiser reviendrait à avoir
        // attiré des candidats sous un salaire qu'on n'entendait pas payer.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestPaying(new BigDecimal("12.99"))))
                .withMessageContaining("inférieur au minimum");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestPaying(new BigDecimal("18.01"))))
                .withMessageContaining("supérieur au maximum");
    }

    @Test
    @DisplayName("Les deux bornes de la fourchette sont des salaires acceptables")
    void bothEndsOfTheRangeAreAcceptable() {
        // « Entre 13 et 18 € » se lit bornes comprises : refuser 13 € rendrait le minimum
        // annoncé impossible à payer, ce que personne ne comprendrait. Deux jeux de
        // données, parce qu'une candidature ne porte qu'une mission à la fois.
        assertThat(create(fixtures.requestPaying(MissionFixtures.SALARY_MIN)).hourlyWage())
                .isEqualByComparingTo(MissionFixtures.SALARY_MIN);

        MissionFixtures other = newFixtures();
        assertThat(create(other, other.requestPaying(MissionFixtures.SALARY_MAX)).hourlyWage())
                .isEqualByComparingTo(MissionFixtures.SALARY_MAX);
    }

    // ---------------------------------------------------------------- mentions des parties

    @Test
    @DisplayName("Un employeur dont la fiche entreprise est incomplète ne peut pas proposer de mission")
    void anincompleteCompanyFileBlocksTheMission() {
        // Demande 15. Le numéro d'entreprise et la commission paritaire sont des mentions
        // obligatoires du contrat : les réclamer au moment de la signature serait trop
        // tard, l'intérimaire attendrait déjà.
        User employer = fixtures.employer;
        employer.setCompanyNumber(null);
        fixtures.save(employer);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.request()))
                .withMessageContaining("numéro d'entreprise");

        // Une fois le numéro renseigné, c'est la commission paritaire qui manque : le
        // contrôle porte sur les quatre mentions, pas sur la première venue.
        employer.setCompanyNumber("0403.199.702");
        employer.setJointCommittee(null);
        fixtures.save(employer);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.request()))
                .withMessageContaining("commission");
    }

    @Test
    @DisplayName("Un intérimaire sans adresse, registre national ou numéro de compte ne peut pas accepter")
    void anincompleteWorkerProfileBlocksTheAcceptance() {
        // Demande 16, et la symétrie qui compte : la fiche de l'employeur est exigée à la
        // création, celle de l'intérimaire à l'acceptation. Chacun est arrêté au moment où
        // il peut lui-même corriger ce qui manque.
        User worker = fixtures.worker;
        worker.setIban(null);
        fixtures.save(worker);

        int missionId = create(fixtures.request()).id();
        missionService.validate(fixtures.admin().getId(), missionId);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> missionService.accept(worker.getId(), missionId))
                .withMessageContaining("compte");

        // Le profil complété, l'acceptation passe : le blocage n'était pas définitif.
        worker.setIban("BE68 5390 0754 7034");
        fixtures.save(worker);
        assertThatNoException().isThrownBy(() -> missionService.accept(worker.getId(), missionId));
    }

    @Test
    @DisplayName("Un remplacement nomme la personne remplacée, les autres motifs ne le demandent pas")
    void areplacementNamesThePersonBeingReplaced() {
        // Mention légale : le contrat conclu pour remplacer un travailleur doit nommer
        // celui-ci, car c'est son absence qui justifie le recours à l'intérim.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestReplacing("   ")))
                .withMessageContaining("travailleur remplacé");

        assertThat(create(fixtures.requestReplacing("Jean Dupont")).replacedWorker())
                .isEqualTo("Jean Dupont");

        // Le nom n'est pas repris lorsque le motif est autre : il n'aurait aucune valeur
        // sur le contrat, et laisserait croire à un remplacement qui n'a pas lieu.
        MissionFixtures other = newFixtures();
        assertThat(create(other, other.request()).replacedWorker()).isNull();
    }

    // -------------------------------------------------------------- journées de travail

    @Test
    @DisplayName("Le premier et le dernier jour de la mission sont forcément travaillés")
    void thefirstAndLastDayAreAlwaysWorked() {
        // Sinon la période annoncée sur le contrat déborderait du travail réellement
        // presté : une mission « du 1er au 5 » dont on ne travaille pas le 1er est une
        // mission du 2, et c'est cette date-là qui doit figurer au contrat.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestWorking(
                        workingDays(fixtures.start.plusDays(1), fixtures.end))))
                .withMessageContaining("premier et le dernier jour");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestWorking(
                        workingDays(fixtures.start, fixtures.end.minusDays(1)))))
                .withMessageContaining("premier et le dernier jour");
    }

    @Test
    @DisplayName("Une journée hors de la période ou renseignée deux fois est refusée")
    void adayOutsideThePeriodOrEnteredTwiceIsRejected() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestWorking(
                        andAlso(workingDays(fixtures.start, fixtures.end), day(fixtures.end.plusDays(1))))))
                .withMessageContaining("en dehors de la période");

        // Deux horaires le même jour donneraient deux fois la même journée sur le contrat,
        // et le volume rémunéré compterait double.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestWorking(
                        andAlso(workingDays(fixtures.start, fixtures.end), day(fixtures.start)))))
                .withMessageContaining("deux fois");
    }

    @Test
    @DisplayName("Une journée doit se terminer après avoir commencé")
    void adayMustEndAfterItBegins() {
        // Le travail de nuit à cheval sur deux dates n'est pas représentable : une journée
        // porte une seule date, son heure de fin est donc forcément le même jour.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.requestWorking(List.of(
                        day(fixtures.start, LocalTime.of(22, 0), LocalTime.of(6, 0), null, null),
                        day(fixtures.end)))))
                .withMessageContaining("heure de fin");
    }

    @Test
    @DisplayName("La date de fin de la mission ne précède pas sa date de début")
    void theendDateNeverPrecedesTheStartDate() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.request(fixtures.end, fixtures.start)))
                .withMessageContaining("date de fin");
    }

    @Test
    @DisplayName("Une pause n'a de sens qu'entière, dans l'horaire, et laissant du temps payé")
    void abreakOnlyMakesSenseWhenItFitsTheDay() {
        // La pause n'est pas rémunérée : elle est soustraite du temps payé, et chacune de
        // ces quatre incohérences fausserait ce calcul sans que rien ne l'affiche.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(oneDayWithBreak(LocalTime.of(12, 0), null)))
                .withMessageContaining("heure de début et une heure de fin");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(oneDayWithBreak(LocalTime.of(12, 30), LocalTime.of(12, 0))))
                .withMessageContaining("postérieure à son début");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(oneDayWithBreak(LocalTime.of(7, 0), LocalTime.of(9, 0))))
                .withMessageContaining("comprise dans l'horaire");

        // Une pause qui couvre toute la journée laisserait un contrat à zéro heure payée.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(oneDayWithBreak(LocalTime.of(8, 0), LocalTime.of(16, 30))))
                .withMessageContaining("aucun temps de travail rémunéré");
    }

    // ----------------------------------------------------------------- unicité et agenda

    @Test
    @DisplayName("Une candidature ne porte qu'une seule mission à la fois")
    void anapplicationCarriesOneMissionAtATime() {
        // Deux missions en cours sur la même candidature laisseraient le candidat devant
        // deux propositions concurrentes pour le même poste chez le même employeur.
        create(fixtures.request());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> create(fixtures.request()))
                .withMessageContaining("déjà en cours");
    }

    @Test
    @DisplayName("Une candidature annulée ne donne plus lieu à une mission")
    void acanceledApplicationCannotBecomeAMission() {
        // L'intérimaire s'est retiré : lui proposer la mission reviendrait à ignorer son
        // désistement. La candidature est déclarée introuvable plutôt qu'invalide, comme
        // si elle n'avait jamais été déposée.
        Application canceled = fixtures.applicationOf(fixtures.worker("desiste"));
        canceled.setStatus(ApplicationStatus.CANCELED);
        fixtures.save(canceled);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> missionService.create(
                        fixtures.employer.getId(), canceled.getId(), fixtures.request()));
    }

    @Test
    @DisplayName("Un intérimaire déjà retenu sur la période ne peut pas l'être par un autre employeur")
    void aworkerAlreadyBookedCannotBeBookedAgain() {
        // Deux missions retenues sur les mêmes dates, c'est une personne à deux endroits.
        // Le contrôle porte sur les missions validées ou actives : une mission provisoire
        // ne réserve encore rien, puisque l'agence peut la refuser.
        int first = create(fixtures.request()).id();
        missionService.validate(fixtures.admin().getId(), first);

        // Le même intérimaire postule chez un second employeur, sur les mêmes dates.
        MissionFixtures other = newFixtures();
        Application elsewhere = other.applicationOf(fixtures.worker);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> missionService.create(
                        other.employer.getId(), elsewhere.getId(), other.request()))
                .withMessageContaining("déjà retenu");
    }

    // ------------------------------------------------------------------ refus de l'agence

    @Test
    @DisplayName("L'agence qui refuse une mission doit se justifier")
    void theagencyMustJustifyARefusal() {
        // Demande 18 de l'analyse. La règle est portée par la validation du corps de la
        // requête, en amont du service : l'employeur doit savoir quoi corriger, un refus
        // muet le laisserait resoumettre la même mission indéfiniment.
        assertThat(validator.validate(new RefuseMissionRequest("   ")))
                .as("un motif vide est refusé dès l'entrée")
                .isNotEmpty();
        assertThat(validator.validate(new RefuseMissionRequest("Salaire sous le barème.")))
                .isEmpty();
    }

    // ------------------------------------------------------------------------------ outils

    private MissionFixtures newFixtures() {
        return new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
    }

    private MissionResponse create(MissionRequest request) {
        return create(fixtures, request);
    }

    private MissionResponse create(MissionFixtures on, MissionRequest request) {
        return missionService.create(on.employer.getId(), on.application().getId(), request);
    }

    /** Mission d'une seule journée, dont la pause est celle qu'on veut éprouver. */
    private MissionRequest oneDayWithBreak(LocalTime breakStart, LocalTime breakEnd) {
        return fixtures.requestWorking(
                fixtures.start, fixtures.start,
                List.of(day(fixtures.start, LocalTime.of(8, 0), LocalTime.of(16, 30), breakStart, breakEnd)));
    }

    private static List<DailySlotRequest> andAlso(List<DailySlotRequest> days, DailySlotRequest extra) {
        List<DailySlotRequest> all = new ArrayList<>(days);
        all.add(extra);
        return all;
    }
}
