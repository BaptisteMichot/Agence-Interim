package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import be.agence_interim.config.AgencyProperties;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ContractService;
import be.agence_interim.service.MissionService;
import be.agence_interim.service.SigningCodeService;

/**
 * Le contenu du contrat de travail, lu dans le PDF lui-même.
 *
 * <p>La demande 21 de l'analyse exige que le contrat « reprenne les mentions légales
 * obligatoires d'un contrat de travail intérimaire ». C'est le seul document que la
 * plateforme produit et qui engage juridiquement ses utilisateurs : la loi du 24 juillet
 * 1987 énumère ce qui doit y figurer, et une mention manquante rend le contrat
 * irrégulier.
 *
 * <p>Ces tests extraient le texte du PDF plutôt que d'inspecter le code qui l'écrit. La
 * différence est capitale : un paragraphe peut être construit sans jamais être ajouté au
 * document, et rien à la compilation ni à l'exécution ne le signalerait.
 */
@SpringBootTest
class ContractDocumentTests {

    @Autowired
    private ContractService contractService;

    @Autowired
    private SigningCodeService signingCodeService;

    @Autowired
    private MissionService missionService;

    @Autowired
    private AgencyProperties agency;

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
    @DisplayName("Le contrat nomme les trois parties d'une relation d'intérim")
    void thecontractNamesTheThreePartiesOfATemporaryWorkRelationship() {
        // L'intérim est une relation triangulaire, et c'est ce qui la distingue d'un
        // contrat de travail ordinaire : l'agence est l'employeur juridique, l'entreprise
        // utilisatrice donne les instructions, l'intérimaire preste. Le document doit les
        // faire apparaître toutes les trois, sans quoi on ne sait pas qui doit le salaire.
        String text = contractText();

        // Les intitulés sont mis en capitales par la feuille de style du document : la
        // comparaison ignore la casse pour porter sur le contenu et non sur sa présentation.
        assertThat(text)
                .containsIgnoringCase("Entreprise de travail intérimaire")
                .contains(agency.getName())
                .contains("Agrément : " + agency.getLicenceNumber())
                .containsIgnoringCase("Entreprise utilisatrice")
                .contains(fixtures.employer.getCompanyName())
                .containsIgnoringCase("Travailleur intérimaire")
                .contains(fixtures.worker.getFirstName() + " " + fixtures.worker.getLastName());
    }

    @Test
    @DisplayName("Le contrat porte les identifiants légaux des trois parties")
    void thecontractCarriesTheLegalIdentifiersOfAllParties() {
        // Ce sont ces numéros que l'ONSS rapproche de la déclaration Dimona. Leur validité
        // est contrôlée à la saisie ; ce test vérifie qu'ils arrivent bien jusqu'au
        // document, ce qui est l'autre moitié du travail.
        String text = contractText();

        assertThat(text)
                .contains(agency.getCompanyNumber())
                .contains(fixtures.employer.getCompanyNumber())
                .contains(fixtures.worker.getNationalNumber())
                .containsIgnoringCase("Commission paritaire");
    }

    @Test
    @DisplayName("Le contrat énonce le motif de recours, sans lequel l'intérim est illégal")
    void thecontractStatesTheReasonForUsingTemporaryWork() {
        // On ne recourt pas à l'intérim librement : la loi n'autorise que des motifs
        // limités, et le contrat doit dire lequel s'applique. C'est la mention dont
        // l'absence fait requalifier la mission en contrat à durée indéterminée.
        assertThat(contractText())
                .containsIgnoringCase("Motif de recours")
                .contains("Surcroît temporaire de travail");
    }

    @Test
    @DisplayName("Le contrat décrit la mission, sa période, son horaire et son volume rémunéré")
    void thecontractDescribesTheAssignmentItsPeriodAndItsPaidVolume() {
        String text = contractText();

        assertThat(text)
                .containsIgnoringCase("Fonction")
                .contains("Cariste")
                .containsIgnoringCase("Lieu d'exécution")
                .contains("Rue de l'Entrepot 4")
                // Cinq journées de 8 h payées : c'est ce volume qui, multiplié par le
                // salaire horaire, donne la rémunération annoncée.
                .containsIgnoringCase("Volume rémunéré")
                .contains("40 h")
                .containsIgnoringCase("Salaire horaire brut")
                .contains("15,00 €");
    }

    @Test
    @DisplayName("Le contrat reprend la période d'essai et le délai de signature")
    void thecontractRestatesTheTrialPeriodAndTheSigningDeadline() {
        // Deux règles propres à l'intérim que les parties ignorent souvent : les trois
        // premiers jours ouvrables valent période d'essai, et le contrat doit être signé
        // dans les deux jours ouvrables du début de la mission.
        assertThat(contractText())
                .contains("trois premiers jours ouvrables")
                .contains("période d'essai")
                .contains("deux jours ouvrables");
    }

    @Test
    @DisplayName("Le contrat annonce les titres-repas, y compris pour dire qu'il n'y en a pas")
    void thecontractAnnouncesMealVouchersEvenWhenThereAreNone() {
        // La loi impose de reprendre au contrat la rémunération *et les avantages*. Une
        // ligne muette laisserait croire à un oubli plutôt qu'à une absence.
        assertThat(contractText())
                .containsIgnoringCase("Titres-repas")
                .contains("Non octroyés");
    }

    @Test
    @DisplayName("Le contrat non signé le dit, et porte la date dès qu'une partie a signé")
    void thecontractSaysWhenItIsUnsignedAndCarriesTheDateOnceSigned() {
        // Le document est le seul exemplaire que les parties impriment : il doit refléter
        // l'état des signatures, sinon un contrat signé sur la plateforme ressemble encore
        // à un projet sur papier.
        assertThat(contractText()).contains("En attente de signature");

        contractService.sign(missionId, fixtures.employer.getId(),
                signingCodeService.generate(contractId, fixtures.employer.getId()));

        assertThat(contractText())
                .containsIgnoringCase("Signé électroniquement le")
                .as("l'autre partie n'a pas encore signé")
                .contains("En attente de signature");
    }

    // ------------------------------------------------------------------------------ outils

    /** Texte du PDF réellement écrit sur le disque, page par page. */
    private String contractText() {
        Resource document = contractService.load(missionId, fixtures.worker.getId(), false);
        try (InputStream stream = document.getInputStream()) {
            PdfReader reader = new PdfReader(stream.readAllBytes());
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            reader.close();
            return text.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Le contrat produit n'est pas un PDF lisible.", e);
        }
    }
}
