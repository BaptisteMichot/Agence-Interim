package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import be.agence_interim.dto.ContractResponse;
import be.agence_interim.model.SignatureStatus;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ContractService;
import be.agence_interim.service.MailService;
import be.agence_interim.service.MissionService;
import be.agence_interim.service.OneTimeCodes.InvalidCodeException;
import be.agence_interim.service.SigningCodeService;

/**
 * Le contrat de travail et sa signature par les deux parties.
 *
 * <p>C'est l'acte le plus engageant de la plateforme, et le seul dont l'analyse fixe la
 * forme : le contrat est généré en PDF avec les mentions légales (demande 21), et chaque
 * partie le signe en confirmant un code à usage unique reçu par email (demande 22). La
 * mécanique du code est éprouvée à part dans {@link OneTimeCodeTests} ; ce qui se joue
 * ici, c'est son branchement sur un vrai contrat — qui peut le demander, pour quelle
 * signature il vaut, et ce qu'il change au document.
 */
@SpringBootTest
class ContractSignatureTests {

    @MockitoSpyBean
    private MailService mailService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private SigningCodeService signingCodeService;

    @Autowired
    private MissionService missionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private int missionId;
    private int contractId;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        missionId = missionService.create(
                fixtures.employer.getId(), fixtures.application().getId(), fixtures.request()).id();
        missionService.validate(fixtures.admin().getId(), missionId);
        contractId = missionService.accept(fixtures.worker.getId(), missionId).contract().id();
    }

    @Test
    @DisplayName("Le contrat généré attend la signature des deux parties et existe sur le disque")
    void thegeneratedContractAwaitsBothSignatures() {
        ContractResponse contract = contractService.get(missionId, fixtures.worker.getId(), false);

        assertThat(contract.statusEmployer()).isEqualTo(SignatureStatus.PENDING);
        assertThat(contract.statusWorker()).isEqualTo(SignatureStatus.PENDING);
        assertThat(contract.generationTime()).isNotNull();
        assertThat(contract.fileName()).endsWith(".pdf");
        // Le document existe vraiment : une ligne en base sans fichier laisserait un
        // contrat consultable dans la liste et introuvable au téléchargement.
        assertThat(documentBytes()).isNotEmpty();
    }

    @Test
    @DisplayName("Aucun contrat n'existe tant que l'intérimaire n'a pas accepté")
    void nocontractExistsBeforeTheWorkerAccepts() {
        // Le contrat matérialise l'accord des deux parties : en produire un avant que
        // l'intérimaire ait répondu reviendrait à le lui présenter comme un fait acquis.
        MissionFixtures other = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        int pending = missionService.create(
                other.employer.getId(), other.application().getId(), other.request()).id();

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> contractService.get(pending, other.worker.getId(), false))
                .withMessageContaining("Aucun contrat");
    }

    @Test
    @DisplayName("Le code de signature part sur l'adresse du signataire, à six chiffres et pour quinze minutes")
    void thesigningCodeReachesTheSignerByEmail() {
        // Demande 22 : c'est l'accès à la boîte mail rattachée au compte qui vaut
        // consentement. Le code doit donc partir à l'adresse du signataire lui-même, et
        // l'email doit annoncer la durée réellement appliquée.
        contractService.requestSigningCode(missionId, fixtures.worker.getId());

        // Le sujet départage : l'acceptation de la mission a déjà envoyé l'email
        // annonçant le contrat à cette même adresse.
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(
                eq(fixtures.worker.getEmail()), contains("Code de signature"), body.capture());

        assertThat(body.getValue()).containsPattern("\\b\\d{6}\\b");
        assertThat(body.getValue())
                .contains("valable " + signingCodeService.getValidityMinutes() + " minutes");
    }

    @Test
    @DisplayName("Chaque partie ne signe que pour elle-même")
    void eachPartyOnlySignsForItself() {
        contractService.sign(missionId, fixtures.employer.getId(), codeFor(fixtures.employer));

        ContractResponse afterEmployer = contractService.get(missionId, fixtures.worker.getId(), false);
        assertThat(afterEmployer.statusEmployer()).isEqualTo(SignatureStatus.SIGNED);
        assertThat(afterEmployer.employerSignedAt()).isNotNull();
        assertThat(afterEmployer.statusWorker()).as("l'intérimaire n'a rien signé")
                .isEqualTo(SignatureStatus.PENDING);
        assertThat(afterEmployer.workerSignedAt()).isNull();

        ContractResponse afterBoth = contractService.sign(
                missionId, fixtures.worker.getId(), codeFor(fixtures.worker));
        assertThat(afterBoth.statusWorker()).isEqualTo(SignatureStatus.SIGNED);
        assertThat(afterBoth.workerSignedAt()).isNotNull();
    }

    @Test
    @DisplayName("Le code délivré à une partie ne signe pas pour l'autre")
    void thecodeIssuedToOnePartyDoesNotSignForTheOther() {
        // Les deux parties partagent le contrat mais pas le code : sinon l'employeur, qui
        // a reçu le sien, signerait aussi à la place de l'intérimaire.
        String employerCode = codeFor(fixtures.employer);

        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> contractService.sign(missionId, fixtures.worker.getId(), employerCode));
    }

    @Test
    @DisplayName("Un code erroné ne signe pas, et cinq essais l'épuisent")
    void awrongCodeNeverSigns() {
        String code = codeFor(fixtures.worker);

        for (int attempt = 1; attempt < 5; attempt++) {
            assertThatExceptionOfType(InvalidCodeException.class)
                    .isThrownBy(() -> contractService.sign(missionId, fixtures.worker.getId(), "000000"))
                    .withMessage("Code incorrect.");
        }
        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> contractService.sign(missionId, fixtures.worker.getId(), "000000"))
                .withMessageContaining("Trop de tentatives");

        // Le code exact ne vaut plus rien : il faut en redemander un.
        assertThatExceptionOfType(InvalidCodeException.class)
                .isThrownBy(() -> contractService.sign(missionId, fixtures.worker.getId(), code));
        assertThat(contractService.get(missionId, fixtures.worker.getId(), false).statusWorker())
                .isEqualTo(SignatureStatus.PENDING);
    }

    @Test
    @DisplayName("On ne signe pas deux fois le même contrat")
    void nobodySignsTheSameContractTwice() {
        contractService.sign(missionId, fixtures.worker.getId(), codeFor(fixtures.worker));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> contractService.requestSigningCode(missionId, fixtures.worker.getId()))
                .withMessageContaining("déjà signé");
    }

    @Test
    @DisplayName("Le document est réécrit à chaque signature pour en porter la trace")
    void thedocumentIsRewrittenOnEverySignature() {
        // Un statut en base dit qu'un contrat est signé ; c'est le PDF que les parties
        // impriment et gardent. S'il ne changeait pas, la signature n'existerait que dans
        // l'application.
        byte[] before = documentBytes();

        contractService.sign(missionId, fixtures.employer.getId(), codeFor(fixtures.employer));

        assertThat(documentBytes()).isNotEqualTo(before);
    }

    @Test
    @DisplayName("Le contrat ne s'ouvre qu'aux deux parties et à l'agence")
    void thecontractOnlyOpensToBothPartiesAndTheAgency() {
        // Un contrat de travail nomme deux personnes, leur adresse et leur rémunération.
        // L'agence y a accès parce qu'elle en est l'employeur juridique ; personne d'autre.
        User outsider = fixtures.employer("tiers");

        assertThatNoException()
                .isThrownBy(() -> contractService.get(missionId, fixtures.employer.getId(), false));
        assertThatNoException()
                .isThrownBy(() -> contractService.get(missionId, outsider.getId(), true));

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> contractService.get(missionId, outsider.getId(), false));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> contractService.load(missionId, outsider.getId(), false));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> contractService.requestSigningCode(missionId, outsider.getId()));
    }

    @Test
    @DisplayName("Un contrat en attente de signature est compté pour chacune des deux parties")
    void apendingContractIsCountedForBothParties() {
        // C'est le chiffre du badge « à signer » : le laisser à zéro alors qu'un contrat
        // attend ferait manquer la signature à quelqu'un qui n'ouvre que son tableau de bord.
        assertThat(contractService.awaitingSignatureCount(fixtures.worker.getId())).isEqualTo(1);
        assertThat(contractService.awaitingSignatureCount(fixtures.employer.getId())).isEqualTo(1);

        contractService.sign(missionId, fixtures.worker.getId(), codeFor(fixtures.worker));

        assertThat(contractService.awaitingSignatureCount(fixtures.worker.getId())).isZero();
        assertThat(contractService.awaitingSignatureCount(fixtures.employer.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------------------------ outils

    /**
     * Le code réellement délivré à ce signataire. On le tire par le service plutôt que de
     * le lire dans l'email : ce qui est éprouvé ici est la signature, pas l'acheminement.
     */
    private String codeFor(User signer) {
        return signingCodeService.generate(contractId, signer.getId());
    }

    private byte[] documentBytes() {
        Resource document = contractService.load(missionId, fixtures.worker.getId(), false);
        try (InputStream stream = document.getInputStream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Le document du contrat est illisible.", e);
        }
    }
}
