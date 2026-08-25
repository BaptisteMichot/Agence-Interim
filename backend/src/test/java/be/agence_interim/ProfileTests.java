package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import be.agence_interim.dto.ExperienceRequest;
import be.agence_interim.dto.FormationRequest;
import be.agence_interim.dto.UpdateProfileRequest;
import be.agence_interim.model.Experience;
import be.agence_interim.model.Formation;
import be.agence_interim.model.FormationStatus;
import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.Role;
import be.agence_interim.model.Skill;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ExperienceService;
import be.agence_interim.service.FormationService;
import be.agence_interim.service.LanguageService;
import be.agence_interim.service.ProfileService;
import be.agence_interim.service.SkillService;

/**
 * Le profil de l'intérimaire : ce qu'il déclare, et ce qui n'appartient qu'à lui.
 *
 * <p>Le profil est la matière première de toute l'application. Il alimente le score de
 * correspondance, il fournit les mentions que le contrat de travail doit porter, et il
 * est ce que l'employeur consulte avant de choisir. Deux familles de règles s'y jouent :
 * la validité de ce qui finira sur un document contractuel — registre national et IBAN,
 * dont la clé est vérifiée dès la saisie plutôt qu'au moment de signer — et
 * l'appartenance, chaque ligne du profil ne devant être modifiable que par la personne
 * qu'elle décrit.
 */
@SpringBootTest
class ProfileTests {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private ExperienceService experienceService;

    @Autowired
    private FormationService formationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private User worker;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        // Un profil vierge : la fixture livre un intérimaire déjà complet, ce qui
        // masquerait les règles de première saisie.
        worker = fixtures.user("profil", Role.JOBSEEKER);
    }

    // ------------------------------------------------------------------- identité

    @Test
    @DisplayName("Le registre national et l'IBAN sont enregistrés au format officiel")
    void theidentifiersAreStoredInTheirOfficialFormat() {
        // La normalisation se fait à l'écriture, pas à l'affichage : le même numéro saisi
        // avec ou sans séparateurs doit s'écrire de la même façon en base et sur le
        // contrat, sans quoi deux profils identiques paraîtraient différents.
        User updated = profileService.updateBase(worker.getId(), profile("85073003328", "BE68539007547034"));

        assertThat(updated.getNationalNumber()).isEqualTo("85.07.30-033.28");
        assertThat(updated.getIban()).isEqualTo("BE68 5390 0754 7034");
    }

    @Test
    @DisplayName("Un registre national ou un IBAN dont la clé ne tombe pas juste est refusé dès la saisie")
    void awrongCheckKeyIsRejectedAtEntryTime() {
        // Le contrôle est fait ici et non au moment d'accepter une mission : découvrir une
        // faute de frappe à la signature bloquerait l'intérimaire sur une mission qui
        // l'attend, alors que la corriger dans son profil ne coûte rien.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> profileService.updateBase(
                        worker.getId(), profile("85073003329", null)))
                .withMessageContaining("registre national");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> profileService.updateBase(
                        worker.getId(), profile(null, "BE68 5390 0754 7035")))
                .withMessageContaining("numéro de compte");
    }

    @Test
    @DisplayName("Un registre national ou un IBAN laissé vide reste vide, sans être refusé")
    void ablankIdentifierStaysEmptyInsteadOfBeingRejected() {
        // Ces deux champs ne sont exigés qu'au moment d'accepter une mission : les rendre
        // obligatoires dès l'inscription arrêterait quelqu'un qui veut seulement regarder
        // les offres.
        User updated = profileService.updateBase(worker.getId(), profile("   ", ""));

        assertThat(updated.getNationalNumber()).isNull();
        assertThat(updated.getIban()).isNull();
    }

    // --------------------------------------------------------------- compétences

    @Test
    @DisplayName("Une compétence saisie librement rejoint celle qui porte déjà le même nom")
    void afreelyTypedSkillJoinsTheExistingOneWithTheSameName() {
        // C'est la même règle que du côté de l'offre, et elle est ici pour la même raison :
        // le score compare des identifiants. « Soudure TIG » écrit par l'employeur et par
        // le candidat doit désigner une seule et même ligne.
        Skill fromProfile = skillService.resolveSkill(worker.getId(), null, "Soudure TIG");
        Skill fromElsewhere = skillService.resolveSkill(
                fixtures.employer.getId(), null, "  soudure tig  ");

        assertThat(fromElsewhere.getId()).isEqualTo(fromProfile.getId());
    }

    @Test
    @DisplayName("Une compétence perso reste invisible aux autres profils")
    void acustomSkillStaysInvisibleToOtherProfiles() {
        // Elle est retrouvable par son nom — c'est ce qui les réunifie — mais elle n'entre
        // pas dans la liste proposée aux autres, qui serait sinon polluée par les
        // libellés approximatifs de tout le monde.
        Skill mine = skillService.resolveSkill(worker.getId(), null, "Pilotage de nacelle 3B");
        User other = fixtures.user("autre-profil", Role.JOBSEEKER);

        assertThat(skillService.available(worker.getId())).extracting(skill -> skill.getId())
                .contains(mine.getId());
        assertThat(skillService.available(other.getId())).extracting(skill -> skill.getId())
                .doesNotContain(mine.getId());
        // Et elle ne s'ajoute pas non plus par son identifiant.
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> skillService.resolveSkill(other.getId(), mine.getId(), null));
    }

    @Test
    @DisplayName("Une compétence ne figure qu'une fois au profil, et son niveau se corrige")
    void askillAppearsOnceAndItsLevelCanBeCorrected() {
        Skill skill = skillService.resolveSkill(worker.getId(), null, "Cariste");
        skillService.add(worker.getId(), skill.getId(), null, SkillLevel.DEBUTANT);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> skillService.add(worker.getId(), skill.getId(), null, SkillLevel.EXPERT))
                .withMessageContaining("déjà dans votre profil");

        assertThat(skillService.updateLevel(worker.getId(), skill.getId(), SkillLevel.EXPERT).getLevel())
                .isEqualTo(SkillLevel.EXPERT);

        skillService.remove(worker.getId(), skill.getId());
        assertThat(skillService.userSkills(worker.getId())).isEmpty();
    }

    @Test
    @DisplayName("Une compétence ne s'ajoute pas sans être nommée")
    void askillCannotBeAddedWithoutBeingNamed() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> skillService.resolveSkill(worker.getId(), null, "   "))
                .withMessageContaining("Indiquez une compétence");
    }

    // -------------------------------------------------------------------- langues

    @Test
    @DisplayName("Une langue ne figure qu'une fois au profil, et son niveau se corrige")
    void alanguageAppearsOnceAndItsLevelCanBeCorrected() {
        // Les langues sont un référentiel fixe : rien à créer, seulement à choisir.
        Language language = languageService.available().get(0);
        languageService.add(worker.getId(), language.getId(), LanguageLevel.A2);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> languageService.add(worker.getId(), language.getId(), LanguageLevel.C1))
                .withMessageContaining("déjà dans votre profil");

        assertThat(languageService.updateLevel(worker.getId(), language.getId(), LanguageLevel.C1).getLevel())
                .isEqualTo(LanguageLevel.C1);

        languageService.remove(worker.getId(), language.getId());
        assertThat(languageService.userLanguages(worker.getId())).isEmpty();
    }

    // ------------------------------------------------------ expériences et formations

    @Test
    @DisplayName("Une expérience sans date de fin est une expérience en cours")
    void anexperienceWithoutAnEndDateIsStillRunning() {
        // Le score de correspondance la compte jusqu'à aujourd'hui : c'est la façon de
        // déclarer l'emploi qu'on occupe encore.
        Experience ongoing = experienceService.add(worker.getId(), new ExperienceRequest(
                "Entrepots du Borinage", "Cariste", LocalDate.now().minusYears(2), null));

        assertThat(ongoing.getEndDate()).isNull();
        assertThat(experienceService.list(worker.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Une expérience ou une formation ne peut pas se terminer avant d'avoir commencé")
    void nothingEndsBeforeItStarts() {
        LocalDate start = LocalDate.now().minusYears(1);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> experienceService.add(worker.getId(), new ExperienceRequest(
                        "Entrepots du Borinage", "Cariste", start, start.minusDays(1))))
                .withMessageContaining("date de fin");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> formationService.add(worker.getId(), new FormationRequest(
                        "Bachelier en logistique", "HEH", start, start.minusDays(1))))
                .withMessageContaining("date de fin");
    }

    @Test
    @DisplayName("Le statut d'une formation se déduit de sa date de fin, il n'est jamais saisi")
    void aformationStatusIsDeducedFromItsEndDate() {
        // Laisser le client l'envoyer permettrait de déclarer « terminé » une formation
        // sans date de fin, ou l'inverse : deux informations qui se contrediraient sur le
        // profil que l'employeur consulte.
        LocalDate start = LocalDate.now().minusYears(3);

        Formation running = formationService.add(worker.getId(), new FormationRequest(
                "Bachelier en logistique", "HEH", start, null));
        assertThat(running.getStatus()).isEqualTo(FormationStatus.EN_COURS);

        Formation finished = formationService.update(worker.getId(), running.getId(), new FormationRequest(
                "Bachelier en logistique", "HEH", start, start.plusYears(3)));
        assertThat(finished.getStatus()).isEqualTo(FormationStatus.TERMINE);
    }

    @Test
    @DisplayName("Personne ne modifie ni ne supprime une ligne du profil d'un autre")
    void nobodyTouchesSomeoneElsesProfileLines() {
        // Les identifiants sont des entiers séquentiels : sans ce contrôle, il suffirait de
        // décrémenter un numéro dans l'URL pour effacer l'expérience professionnelle de
        // quelqu'un d'autre.
        LocalDate start = LocalDate.now().minusYears(2);
        int experienceId = experienceService.add(worker.getId(), new ExperienceRequest(
                "Entrepots du Borinage", "Cariste", start, null)).getId();
        int formationId = formationService.add(worker.getId(), new FormationRequest(
                "Bachelier en logistique", "HEH", start, null)).getId();
        User intruder = fixtures.user("intrus", Role.JOBSEEKER);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> experienceService.delete(intruder.getId(), experienceId));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> experienceService.update(intruder.getId(), experienceId,
                        new ExperienceRequest("Ailleurs", "Autre", start, null)));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> formationService.delete(intruder.getId(), formationId));

        assertThat(experienceService.list(worker.getId())).hasSize(1);
        assertThat(formationService.list(worker.getId())).hasSize(1);
    }

    // ------------------------------------------------------------------------------ outils

    private UpdateProfileRequest profile(String nationalNumber, String iban) {
        return new UpdateProfileRequest(
                "Dupont", "Jean", LocalDate.of(1985, 7, 30), true,
                "Rue Neuve 12, 7000 Mons", nationalNumber, iban);
    }
}
