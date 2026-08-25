package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import be.agence_interim.dto.MissionRequest;
import be.agence_interim.dto.MissionResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.dto.ScheduleEntryResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.MissionService;

/**
 * Le cycle de vie d'une mission d'intérim, de la sélection du candidat au contrat.
 *
 * <p>L'analyse le décrit en une phrase : « l'employeur entre les informations exactes
 * concernant l'offre, ce qui crée une mission provisoire et informe l'agence ; si
 * l'agence valide, la proposition est soumise à l'intérimaire ; s'il accepte, le contrat
 * est généré [...] Seulement à ce moment-là, la mission est ajoutée dans l'horaire de
 * l'intérimaire. » Trois parties, quatre décisions, six statuts : c'est la mécanique la
 * plus longue de l'application, et celle où un état oublié se paie le plus cher — une
 * mission au planning sans contrat, ou l'inverse.
 *
 * <p>Ces tests parlent au service et non à l'API : ils décrivent la règle métier
 * indépendamment de la route qui l'expose. Ce que la mission refuse d'enregistrer est
 * éprouvé dans {@link MissionValidationTests}.
 */
@SpringBootTest
class MissionLifecycleTests {

    @Autowired
    private MissionService missionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
    }

    @Test
    @DisplayName("Le parcours nominal : mission provisoire, validation de l'agence, acceptation, contrat")
    void theNominalPathEndsWithAContract() {
        MissionResponse created = create();
        assertThat(created.status()).isEqualTo(MissionStatus.PENDING);
        assertThat(created.contract()).as("aucun contrat avant l'accord des deux parties").isNull();

        MissionResponse approved = validate(created.id());
        assertThat(approved.status()).isEqualTo(MissionStatus.APPROVED);
        assertThat(approved.contract()).isNull();

        MissionResponse active = accept(created.id());
        assertThat(active.status()).isEqualTo(MissionStatus.ACTIVE);
        assertThat(active.contract()).as("le contrat naît de l'acceptation").isNotNull();

        // Le poste est pourvu : l'offre quitte la liste des offres ouvertes.
        assertThat(fixtures.offerStatus()).isEqualTo(JobOfferStatus.CLOSED);
    }

    @Test
    @DisplayName("La mission n'entre au planning de l'intérimaire qu'une fois acceptée")
    void theMissionOnlyReachesTheScheduleOnceAccepted() {
        // « Seulement à ce moment-là, la mission est ajoutée dans l'horaire de
        // l'intérimaire » : la phrase est de l'analyse, et elle a une conséquence
        // pratique. Tant que la mission n'est pas acceptée, elle ne réserve pas les
        // journées, donc l'intérimaire reste libre de se déclarer indisponible ou
        // d'accepter une mission concurrente.
        int missionId = create().id();
        assertThat(schedule()).isEmpty();

        validate(missionId);
        assertThat(schedule()).isEmpty();

        accept(missionId);
        assertThat(schedule()).hasSize(5);
    }

    @Test
    @DisplayName("L'intérimaire ne voit pas la mission provisoire tant que l'agence ne l'a pas validée")
    void theWorkerSeesNothingOfTheProvisionalMission() {
        // La mission provisoire est une conversation entre l'employeur et l'agence. La
        // montrer à l'intérimaire lui ferait espérer une mission qui peut encore être
        // refusée, et lui laisserait lire le motif de ce refus.
        int missionId = create().id();

        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> get(missionId));
        assertThat(missionService.decisionCount(fixtures.worker.getId())).isZero();

        validate(missionId);

        assertThatNoException().isThrownBy(() -> get(missionId));
        assertThat(missionService.decisionCount(fixtures.worker.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Une mission refusée porte son motif, reste cachée au candidat et peut être corrigée")
    void arefusedMissionCarriesItsReasonAndCanBeCorrected() {
        int missionId = create().id();

        MissionResponse refused = refuse(missionId, "Salaire sous le barème.");
        assertThat(refused.status()).isEqualTo(MissionStatus.REFUSED);
        assertThat(refused.refusalReason()).isEqualTo("Salaire sous le barème.");
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> get(missionId));

        // L'employeur corrige et resoumet : le refus n'est pas une fin de parcours, sinon
        // il faudrait reprendre la sélection du candidat depuis le début.
        MissionResponse corrected = missionService.update(
                fixtures.employer.getId(), missionId, fixtures.request());
        assertThat(corrected.status()).isEqualTo(MissionStatus.PENDING);
        assertThat(corrected.refusalReason()).as("le motif ne survit pas à la correction").isNull();
    }

    @Test
    @DisplayName("Le refus du candidat remet l'offre en ligne, ses candidatures intactes")
    void theWorkerDecliningPutsTheOfferBackOnline() {
        // L'employeur a clôturé son offre en retenant ce candidat ; si celui-ci se
        // désiste, republier l'offre et faire repostuler tout le monde n'aurait pas de
        // sens. L'offre repart en ligne avec les candidatures déjà reçues.
        int missionId = create().id();
        validate(missionId);
        fixtures.closeOffer();

        MissionResponse declined = decline(fixtures.worker, missionId);

        assertThat(declined.status()).isEqualTo(MissionStatus.DECLINED);
        assertThat(fixtures.offerStatus()).isEqualTo(JobOfferStatus.OPEN);
        assertThat(applicationRepository.findById(fixtures.application().getId())).isPresent();
    }

    @Test
    @DisplayName("Une offre dont un autre candidat tient déjà le poste ne repart pas en ligne")
    void theOfferStaysClosedWhenAnotherCandidateHoldsThePost() {
        // Deux candidats retenus sur la même offre : le premier accepte, le second se
        // désiste. Rouvrir l'offre annoncerait un poste qui n'est plus à pourvoir.
        User other = fixtures.worker("second");
        Application otherApplication = fixtures.applicationOf(other);

        accept(validate(create().id()).id());

        int refusedByWorker = missionService.create(
                fixtures.employer.getId(), otherApplication.getId(), fixtures.request()).id();
        validate(refusedByWorker);
        decline(other, refusedByWorker);

        assertThat(fixtures.offerStatus()).isEqualTo(JobOfferStatus.CLOSED);
    }

    @Test
    @DisplayName("Le renouvellement passe par le candidat avant l'agence, et devient actif dès la validation")
    void arenewalAsksTheWorkerFirstAndTheAgencyLast() {
        // L'ordre est inversé par rapport à une première mission, et c'est délibéré : les
        // deux parties se connaissent déjà et n'ont plus à être présentées, mais
        // l'intérimaire ne doit pas se voir imposer une prolongation (US 22).
        int first = activeMission();

        MissionResponse renewal = renew(first);
        assertThat(renewal.status()).isEqualTo(MissionStatus.RENEWAL);
        assertThat(renewal.renewal()).isTrue();
        assertThat(renewal.previousStartDate()).isEqualTo(fixtures.start);

        MissionResponse acceptedByWorker = accept(renewal.id());
        assertThat(acceptedByWorker.status()).as("l'agence a maintenant la main")
                .isEqualTo(MissionStatus.PENDING);
        assertThat(acceptedByWorker.contract()).isNull();

        // Le candidat a déjà donné son accord : la validation vaut activation, il n'a pas
        // à se prononcer une seconde fois sur les mêmes conditions.
        MissionResponse validated = validate(renewal.id());
        assertThat(validated.status()).isEqualTo(MissionStatus.ACTIVE);
        assertThat(validated.contract()).isNotNull();
    }

    @Test
    @DisplayName("Un renouvellement refusé par le candidat laisse la mission d'origine en cours")
    void adeclinedRenewalLeavesTheOriginalMissionRunning() {
        // Refuser une prolongation n'est pas se désister : le poste reste tenu jusqu'à la
        // date de fin convenue, et l'offre n'a aucune raison de repartir en ligne.
        int first = activeMission();
        int renewal = renew(first).id();

        decline(fixtures.worker, renewal);

        assertThat(missionService.getForEmployer(fixtures.employer.getId(), first).status())
                .isEqualTo(MissionStatus.ACTIVE);
        assertThat(fixtures.offerStatus()).isEqualTo(JobOfferStatus.CLOSED);
    }

    @Test
    @DisplayName("Un renouvellement corrigé après un refus de l'agence repasse par le candidat")
    void acorrectedRenewalGoesBackToTheWorker() {
        // La correction porte sur des conditions que l'intérimaire avait acceptées : les
        // lui imposer modifiées reviendrait à lui faire signer autre chose que ce à quoi
        // il a consenti.
        int renewal = renew(activeMission()).id();
        accept(renewal);
        refuse(renewal, "Commission paritaire erronée.");

        MissionResponse corrected = missionService.update(
                fixtures.employer.getId(), renewal, renewalRequest());

        assertThat(corrected.status()).isEqualTo(MissionStatus.RENEWAL);
    }

    @Test
    @DisplayName("Seule une mission en cours se renouvelle, et pas deux fois à la fois")
    void onlyARunningMissionCanBeRenewedAndOnlyOnce() {
        int missionId = create().id();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> renew(missionId))
                .withMessageContaining("confirmée");

        accept(validate(missionId).id());
        renew(missionId);

        // Deux prolongations en attente sur la même candidature se recouvriraient : le
        // candidat ne saurait plus à quoi il répond.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> missionService.renew(fixtures.employer.getId(), missionId,
                        fixtures.request(fixtures.end.plusDays(6), fixtures.end.plusDays(10))))
                .withMessageContaining("déjà en cours");
    }

    @Test
    @DisplayName("Un renouvellement doit commencer après la fin de la mission qu'il prolonge")
    void arenewalStartsAfterTheMissionItExtends() {
        // Se chevaucher lui-même reviendrait à faire travailler l'intérimaire deux fois le
        // même jour, sous deux contrats différents.
        int first = activeMission();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> missionService.renew(fixtures.employer.getId(), first,
                        fixtures.request(fixtures.end, fixtures.end.plusDays(3))))
                .withMessageContaining("après la fin");
    }

    @Test
    @DisplayName("Chaque décision n'appartient qu'à celui qui doit la prendre, et ne se reprend pas")
    void eachDecisionBelongsToASinglePartyAndIsTakenOnce() {
        int missionId = create().id();

        // Le candidat ne peut pas répondre avant que l'agence n'ait tranché : la mission
        // n'existe pas encore pour lui.
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> accept(missionId));

        validate(missionId);

        // Et l'agence ne se prononce pas deux fois sur la même mission.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> validate(missionId))
                .withMessageContaining("en attente de validation");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> refuse(missionId, "Trop tard."))
                .withMessageContaining("en attente de validation");

        accept(missionId);

        // Une fois la mission active, plus personne ne revient dessus.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> decline(fixtures.worker, missionId))
                .withMessageContaining("n'attend pas de réponse");
    }

    @Test
    @DisplayName("Une mission ne concerne que ses deux parties : un tiers ne la trouve pas")
    void athirdPartyCannotReachTheMission() {
        // Le contrôle d'accès répond « introuvable » plutôt qu'« interdit » : répondre
        // « interdit » confirmerait à un inconnu qu'une mission existe sous cet
        // identifiant, et avec elle un employeur et un intérimaire.
        int missionId = create().id();
        User outsider = fixtures.employer("tiers");
        User otherWorker = fixtures.worker("etranger");

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> missionService.getForEmployer(outsider.getId(), missionId));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> missionService.getForJobSeeker(otherWorker.getId(), missionId));
    }

    @Test
    @DisplayName("Les missions se rangent dans la section qui correspond à leur statut")
    void missionsAreFiledUnderTheSectionThatMatchesTheirStatus() {
        // Chaque portail découpe la liste en sections, et le découpage est porté par la
        // requête : une mission rangée au mauvais endroit disparaît de l'écran de la
        // personne qui l'attend.
        int missionId = create().id();

        assertThat(employerSection("awaiting")).contains(missionId);
        assertThat(adminSection("pending")).contains(missionId);
        assertThat(workerSection("to-confirm")).doesNotContain(missionId);

        validate(missionId);
        assertThat(workerSection("to-confirm")).contains(missionId);
        assertThat(adminSection("pending")).doesNotContain(missionId);

        accept(missionId);
        assertThat(employerSection("current")).contains(missionId);
        assertThat(workerSection("confirmed")).contains(missionId);
        assertThat(workerSection("to-confirm")).doesNotContain(missionId);
    }

    // ------------------------------------------------------------------------------ outils

    /** Mène une mission jusqu'au statut actif et rend son identifiant. */
    private int activeMission() {
        int missionId = create().id();
        validate(missionId);
        accept(missionId);
        return missionId;
    }

    private MissionResponse create() {
        return missionService.create(
                fixtures.employer.getId(), fixtures.application().getId(), fixtures.request());
    }

    private MissionResponse validate(int missionId) {
        return missionService.validate(fixtures.admin().getId(), missionId);
    }

    private MissionResponse refuse(int missionId, String reason) {
        return missionService.refuse(fixtures.admin().getId(), missionId, reason);
    }

    private MissionResponse accept(int missionId) {
        return missionService.accept(fixtures.worker.getId(), missionId);
    }

    private MissionResponse decline(User jobSeeker, int missionId) {
        return missionService.decline(jobSeeker.getId(), missionId);
    }

    private MissionResponse get(int missionId) {
        return missionService.getForJobSeeker(fixtures.worker.getId(), missionId);
    }

    /** Prolongation enchaînée juste après la mission d'origine, de même durée. */
    private MissionResponse renew(int missionId) {
        return missionService.renew(fixtures.employer.getId(), missionId, renewalRequest());
    }

    private MissionRequest renewalRequest() {
        return fixtures.request(fixtures.end.plusDays(1), fixtures.end.plusDays(5));
    }

    private List<ScheduleEntryResponse> schedule() {
        return missionService.schedule(fixtures.worker.getId(), fixtures.start, fixtures.end);
    }

    private List<Integer> employerSection(String group) {
        return ids(missionService.listForEmployer(fixtures.employer.getId(), group, page()));
    }

    private List<Integer> workerSection(String group) {
        return ids(missionService.listForJobSeeker(fixtures.worker.getId(), group, page()));
    }

    private List<Integer> adminSection(String group) {
        return ids(missionService.listForAdmin(group, page()));
    }

    private static PageRequest page() {
        return PageRequest.of(0, 50);
    }

    private static List<Integer> ids(PageResponse<MissionResponse> page) {
        return page.content().stream().map(mission -> mission.id()).toList();
    }
}
