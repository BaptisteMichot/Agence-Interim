package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.agence_interim.config.AgencyProperties;
import be.agence_interim.dto.PersonalDataExport;
import be.agence_interim.dto.PersonalDataExport.ApplicationLine;
import be.agence_interim.dto.PersonalDataExport.ContractLine;
import be.agence_interim.dto.PersonalDataExport.Identity;
import be.agence_interim.dto.PersonalDataExport.MessageLine;
import be.agence_interim.dto.PersonalDataExport.OfferLine;
import be.agence_interim.service.PersonalDataDocument;

/**
 * Le document remis au titre du droit d'accès (RGPD, article 15).
 *
 * <p>Il est lu par la personne qu'il décrit, souvent parce qu'elle se demande ce que la
 * plateforme sait d'elle. Sa qualité se juge donc à des critères inhabituels pour du
 * code : il ne doit rien laisser paraître de la mécanique interne — pas d'identifiant
 * technique, pas de « null » — il doit s'adapter au rôle sans afficher huit rubriques
 * « néant », et il doit dire ce qu'il ne contient pas.
 *
 * <p>Le rendu est une fonction pure : une structure de données et l'identité de l'agence
 * entrent, un texte sort. Ces tests s'exécutent sans contexte Spring.
 */
class PersonalDataDocumentTests {

    private static final LocalDateTime MOMENT = LocalDateTime.of(2026, 8, 25, 14, 30);

    @Test
    @DisplayName("Le document s'ouvre sur l'agence, la date d'export et le fondement du droit d'accès")
    void thedocumentOpensOnTheAgencyTheDateAndTheLegalBasis() {
        // Celui qui reçoit ce fichier doit savoir de qui il vient et à quel titre : sans
        // cela, c'est un fichier texte de plus dans son dossier de téléchargements.
        String document = render(jobSeeker());

        assertThat(document)
                .contains("MES DONNÉES PERSONNELLES")
                .contains("Agence d'intérim de test")
                .contains("Export du 25/08/2026")
                .contains("article 15");
    }

    @Test
    @DisplayName("Aucun identifiant technique n'apparaît dans le document")
    void notechnicalIdentifierAppearsInTheDocument() {
        // Un « contrat n° 34 » ne dit rien à qui lit son export, et publier la numérotation
        // interne renseigne au passage sur le volume d'activité de la plateforme. Chaque
        // élément est désigné par ce qui le distingue à ses yeux : un poste, une date, un
        // interlocuteur.
        String document = render(jobSeeker());

        assertThat(document)
                .doesNotContain("n° 34")
                .doesNotContainIgnoringCase("identifiant")
                .contains("Cariste");
    }

    @Test
    @DisplayName("Une valeur absente s'écrit en toutes lettres, jamais « null »")
    void amissingValueIsWrittenOutAndNeverAsNull() {
        // Le document est lu par quelqu'un qui n'a aucune raison de savoir ce que « null »
        // signifie, et qui y verrait au mieux un défaut, au pire une donnée cachée.
        String document = render(new PersonalDataExport(
                MOMENT,
                new Identity("JOBSEEKER", "Dupont", "Jean", "jean@example.be",
                        null, null, null, null, null, null, null, null, null, null),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()));

        assertThat(document).doesNotContain("null");
    }

    @Test
    @DisplayName("Une rubrique vide n'est pas rendue, ce qui adapte le document au rôle")
    void anemptySectionIsNotRenderedWhichFitsTheDocumentToTheRole() {
        // Aucune condition sur le rôle dans le code : c'est le contenu qui décide. Un
        // employeur n'a ni compétences ni candidatures, un intérimaire n'a pas d'offres,
        // et chacun reçoit un document qui ne parle que de lui.
        String forJobSeeker = render(jobSeeker());
        String forEmployer = render(employer());

        // Les titres de rubrique sont mis en capitales : la comparaison ignore la casse
        // pour porter sur la présence de la rubrique, non sur sa typographie.
        assertThat(forJobSeeker)
                .containsIgnoringCase("Candidatures déposées")
                .doesNotContainIgnoringCase("Offres publiées");
        assertThat(forEmployer)
                .containsIgnoringCase("Offres publiées")
                .doesNotContainIgnoringCase("Candidatures déposées");
    }

    @Test
    @DisplayName("Les mentions de l'entreprise ne sont rendues que pour un employeur")
    void thecompanyDetailsAreOnlyRenderedForAnEmployer() {
        // Elles désignent une personne morale et non l'utilisateur : le document le dit,
        // pour que celui qui demande l'effacement de ses données comprenne pourquoi elles
        // survivent à la clôture de son compte.
        assertThat(render(employer()))
                .containsIgnoringCase("Entreprise utilisatrice")
                .contains("0403.199.702")
                .contains("personne morale");
        assertThat(render(jobSeeker())).doesNotContainIgnoringCase("Entreprise utilisatrice");
    }

    @Test
    @DisplayName("Les valeurs codées sont traduites, pas recopiées telles quelles")
    void codedValuesAreTranslatedRatherThanCopied() {
        // « JOBSEEKER » et « PENDING » sont des mots du programme. Les laisser tels quels
        // ferait lire à l'utilisateur le vocabulaire de la base plutôt que le sien.
        String document = render(jobSeeker());

        assertThat(document)
                .doesNotContain("JOBSEEKER")
                .doesNotContain("PENDING")
                .contains("Intérimaire");
    }

    @Test
    @DisplayName("Le document énumère ce qu'il ne contient pas, et pourquoi")
    void thedocumentListsWhatItDoesNotContain() {
        // Un export muet sur ses lacunes laisse croire qu'il est complet. Le mot de passe
        // en est absent parce qu'il ne servirait qu'à celui qui mettrait la main sur le
        // fichier ; les PDF, parce qu'ils se téléchargent par leurs propres routes.
        String document = render(jobSeeker());

        assertThat(document)
                .contains("Ne figurent pas dans ce document")
                .contains("mot de passe")
                .contains("contrats")
                .contains("téléchargent");
    }

    @Test
    @DisplayName("Un compte sans CV ni contrat ne mentionne que le mot de passe comme absent")
    void anaccountWithoutCvOrContractOnlyMentionsThePassword() {
        // La note de bas de page se construit à partir de ce que le compte détient
        // vraiment : annoncer l'absence d'un CV qui n'a jamais existé décrirait une autre
        // application que la sienne.
        String document = render(new PersonalDataExport(
                MOMENT,
                new Identity("JOBSEEKER", "Dupont", "Jean", "jean@example.be",
                        LocalDate.of(1985, 7, 30), true, "Rue Neuve 12, 7000 Mons",
                        "85.07.30-033.28", "BE68 5390 0754 7034", null, null, null, null, MOMENT),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()));

        assertThat(document)
                .contains("Ne figure pas dans ce document")
                .doesNotContain("CV")
                .doesNotContain("contrats");
    }

    @Test
    @DisplayName("L'agence ne s'invite pas à s'écrire à elle-même")
    void theagencyIsNotInvitedToWriteToItself() {
        // La note de fin renvoie au responsable du traitement. Servie à un compte de
        // l'agence, elle lui demanderait de s'adresser à lui-même.
        assertThat(render(jobSeeker())).contains("adressez-vous à");
        assertThat(render(admin())).doesNotContain("adressez-vous à");
    }

    @Test
    @DisplayName("Les messages sont regroupés par fil, l'interlocuteur nommé une seule fois")
    void messagesAreGroupedByThread() {
        // Répéter l'interlocuteur à chaque ligne serait aussi bavard que le numéro de
        // conversation qu'il remplace.
        String document = render(withMessages());

        assertThat(document).contains("Entrepots du Borinage");
        assertThat(document.split("Entrepots du Borinage", -1)).hasSize(2);
        assertThat(document).contains("Bonjour").contains("Je suis intéressé");
    }

    // ------------------------------------------------------------------------------ outils

    private static String render(PersonalDataExport data) {
        AgencyProperties agency = new AgencyProperties();
        agency.setName("Agence d'intérim de test");
        agency.setAddress("Rue de la Gare 1, 7000 Mons");
        agency.setCompanyNumber("0454.460.440");
        agency.setLicenceNumber("W.INT.999");
        agency.setJointCommittee("322");
        return PersonalDataDocument.render(data, agency);
    }

    private static PersonalDataExport jobSeeker() {
        return new PersonalDataExport(
                MOMENT,
                new Identity("JOBSEEKER", "Dupont", "Jean", "jean@example.be",
                        LocalDate.of(1985, 7, 30), true, "Rue Neuve 12, 7000 Mons",
                        "85.07.30-033.28", "BE68 5390 0754 7034",
                        null, null, null, "cv.pdf", MOMENT),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new ApplicationLine(
                        "Cariste", "Entrepots du Borinage", MOMENT, "PENDING", 4)),
                List.of(),
                List.of(new ContractLine("Cariste", MOMENT, "SIGNED", MOMENT, "PENDING", null)),
                List.of());
    }

    private static PersonalDataExport employer() {
        return new PersonalDataExport(
                MOMENT,
                new Identity("EMPLOYER", "Martin", "Marie", "marie@example.be",
                        null, null, "Rue de la Gare 1, 7000 Mons", null, null,
                        "Entrepots du Borinage", "0403.199.702", "140", null, MOMENT),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(),
                List.of(new OfferLine("Cariste", "Mons", MOMENT, "OPEN")),
                List.of(), List.of());
    }

    private static PersonalDataExport admin() {
        return new PersonalDataExport(
                MOMENT,
                new Identity("ADMIN", "Agence", "Bureau", "agence@example.be",
                        null, null, null, null, null, null, null, null, null, MOMENT),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }

    private static PersonalDataExport withMessages() {
        return new PersonalDataExport(
                MOMENT,
                new Identity("JOBSEEKER", "Dupont", "Jean", "jean@example.be",
                        null, null, null, null, null, null, null, null, null, MOMENT),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(
                        new MessageLine("Entrepots du Borinage", MOMENT, "Bonjour"),
                        new MessageLine("Entrepots du Borinage", MOMENT, "Je suis intéressé")));
    }
}
