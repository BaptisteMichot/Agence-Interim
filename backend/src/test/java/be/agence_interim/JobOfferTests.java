package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import be.agence_interim.dto.JobOfferRequest;
import be.agence_interim.dto.JobOfferResponse;
import be.agence_interim.dto.OfferDegreeRequirement;
import be.agence_interim.dto.OfferLanguageRequirement;
import be.agence_interim.dto.OfferSkillRequirement;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.Province;
import be.agence_interim.model.Sector;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.LanguageRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ApplicationService;
import be.agence_interim.service.JobOfferService;

/**
 * L'offre d'emploi : ce que l'employeur publie, et ce qu'il ne peut plus en changer.
 *
 * <p>Une offre est un engagement pris devant des inconnus. Tant que personne n'a
 * postulé, elle se corrige librement ; dès la première candidature, la retoucher
 * reviendrait à changer les conditions sous les yeux de celui qui s'est déjà décidé.
 * C'est l'essentiel de ce que ces tests tiennent.
 *
 * <p>Ils portent aussi sur les exigences de l'offre, qui alimentent le score de
 * correspondance : une exigence mal enregistrée fausse silencieusement le matching, et
 * {@link MatchingScoreTests} ne le verrait pas puisqu'il part d'exigences déjà en mémoire.
 */
@SpringBootTest
class JobOfferTests {

    @Autowired
    private JobOfferService jobOfferService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private LanguageRepository languageRepository;

    private MissionFixtures fixtures;
    private User employer;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        employer = fixtures.employer;
    }

    @Test
    @DisplayName("Une offre publiée est ouverte et porte les exigences saisies")
    void apublishedOfferIsOpenAndCarriesItsRequirements() {
        JobOfferResponse offer = jobOfferService.create(employer.getId(), request(
                List.of(new OfferSkillRequirement("Chariot élévateur", true, SkillLevel.AVANCE)),
                List.of(new OfferDegreeRequirement(DegreeType.BACHELIER, "Logistique", false)),
                List.of(new OfferLanguageRequirement(anyLanguage().getId(), false, LanguageLevel.B2))));

        assertThat(offer.status()).isEqualTo(JobOfferStatus.OPEN);
        assertThat(offer.publishedAt()).isNotNull();
        assertThat(offer.skills()).singleElement()
                .satisfies(skill -> {
                    assertThat(skill.name()).isEqualTo("Chariot élévateur");
                    assertThat(skill.isMandatory()).isTrue();
                    assertThat(skill.requiredLevel()).isEqualTo(SkillLevel.AVANCE);
                });
        assertThat(offer.degrees()).hasSize(1);
        assertThat(offer.languages()).hasSize(1);
        // Tant que personne n'a postulé, l'employeur peut encore corriger son annonce.
        assertThat(offer.editable()).isTrue();
    }

    @Test
    @DisplayName("Une compétence saisie librement rejoint celle qui porte déjà le même nom")
    void afreelyTypedSkillJoinsTheExistingOneWithTheSameName() {
        // Le score de correspondance compare les compétences par identifiant. Si deux
        // employeurs qui écrivent « Cariste » créaient deux lignes distinctes, un candidat
        // rattaché à l'une ne correspondrait jamais aux offres de l'autre — et rien ne le
        // signalerait, le score dirait simplement zéro.
        int first = jobOfferService.create(employer.getId(),
                request(List.of(new OfferSkillRequirement("Cariste", true, SkillLevel.AVANCE)), List.of(), List.of()))
                .skills().get(0).skillId();

        User other = fixtures.employer("concurrent");
        int second = jobOfferService.create(other.getId(),
                request(List.of(new OfferSkillRequirement("cariste", false, SkillLevel.DEBUTANT)), List.of(), List.of()))
                .skills().get(0).skillId();

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("Une même exigence ne peut pas être renseignée deux fois")
    void thesameRequirementCannotBeEnteredTwice() {
        // La casse ne fait pas deux compétences : les deux lignes viseraient le même
        // identifiant, et la seconde écraserait le niveau exigé par la première.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> jobOfferService.create(employer.getId(), request(
                        List.of(
                                new OfferSkillRequirement("Soudure", true, SkillLevel.AVANCE),
                                new OfferSkillRequirement("soudure", false, SkillLevel.DEBUTANT)),
                        List.of(), List.of())))
                .withMessageContaining("plusieurs fois");
    }

    @Test
    @DisplayName("Le salaire maximum ne peut pas être inférieur au minimum annoncé")
    void themaximumSalaryCannotBeBelowTheMinimum() {
        // Une fourchette inversée ne veut rien dire, et c'est elle qui borne ensuite le
        // salaire de la mission : la laisser passer rendrait toute mission impossible.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> jobOfferService.create(employer.getId(), request(
                        new BigDecimal("18.00"), new BigDecimal("13.00"))))
                .withMessageContaining("salaire maximum");
    }

    @Test
    @DisplayName("Une offre qui a reçu une candidature ne se modifie plus")
    void anofferThatReceivedAnApplicationIsFrozen() {
        // C'est la règle qui protège le candidat : il a postulé au vu d'un salaire, d'un
        // lieu et d'exigences précises. Les changer après coup reviendrait à obtenir son
        // acte de candidature sous une annonce qui n'existe plus.
        int offerId = jobOfferService.create(employer.getId(), request()).id();
        applicationService.apply(fixtures.worker.getId(), offerId);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> jobOfferService.update(employer.getId(), offerId, request()))
                .withMessageContaining("déjà reçu une candidature");

        assertThat(jobOfferService.getMine(employer.getId(), offerId).editable()).isFalse();
    }

    @Test
    @DisplayName("Une offre clôturée ne se modifie plus et ne se clôture pas deux fois")
    void aclosedOfferIsNeitherEditedNorClosedAgain() {
        int offerId = jobOfferService.create(employer.getId(), request()).id();

        assertThat(jobOfferService.close(employer.getId(), offerId).status())
                .isEqualTo(JobOfferStatus.CLOSED);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> jobOfferService.close(employer.getId(), offerId))
                .withMessageContaining("déjà clôturée");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> jobOfferService.update(employer.getId(), offerId, request()))
                .withMessageContaining("clôturée");
    }

    @Test
    @DisplayName("La mise à jour remplace les exigences au lieu de les cumuler")
    void updatingReplacesTheRequirementsInsteadOfAddingToThem() {
        // Retirer une compétence de l'annonce doit la retirer du score : si les anciennes
        // exigences survivaient, l'employeur continuerait d'écarter des candidats sur un
        // critère qu'il croit avoir supprimé.
        int offerId = jobOfferService.create(employer.getId(), request(
                List.of(
                        new OfferSkillRequirement("Cariste", true, SkillLevel.AVANCE),
                        new OfferSkillRequirement("Manutention", false, SkillLevel.DEBUTANT)),
                List.of(), List.of()))
                .id();

        JobOfferResponse updated = jobOfferService.update(employer.getId(), offerId, request(
                List.of(new OfferSkillRequirement("Manutention", true, SkillLevel.EXPERT)),
                List.of(), List.of()));

        assertThat(updated.skills()).singleElement()
                .satisfies(skill -> {
                    assertThat(skill.name()).isEqualTo("Manutention");
                    assertThat(skill.requiredLevel()).isEqualTo(SkillLevel.EXPERT);
                });
    }

    @Test
    @DisplayName("Une offre n'appartient qu'à son auteur")
    void anofferBelongsToItsAuthorAlone() {
        // Le contrôle rend l'offre « introuvable » et non « interdite » : un employeur n'a
        // pas à découvrir, par la nature du refus, combien d'offres ses concurrents ont
        // publiées ni sous quels identifiants.
        int offerId = jobOfferService.create(employer.getId(), request()).id();
        User outsider = fixtures.employer("tiers");

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> jobOfferService.getMine(outsider.getId(), offerId));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> jobOfferService.update(outsider.getId(), offerId, request()));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> jobOfferService.close(outsider.getId(), offerId));
    }

    @Test
    @DisplayName("Une expérience laissée vide n'est pas enregistrée comme une chaîne vide")
    void anemptyExperienceIsStoredAsNothingAtAll() {
        // La recherche compare ce champ à un nombre en SQL : une chaîne vide y ferait
        // échouer la conversion et l'offre disparaîtrait des résultats filtrés.
        JobOfferResponse offer = jobOfferService.create(employer.getId(), new JobOfferRequest(
                "Manutentionnaire", Sector.LOGISTIQUE, "Mons", Province.HAINAUT,
                "Poste en entrepôt, formation assurée.",
                new BigDecimal("13.00"), new BigDecimal("18.00"),
                "   ", false, null, List.of(), List.of(), List.of()));

        assertThat(offer.experienceTime()).isNull();
    }

    // ------------------------------------------------------------------------------ outils

    private JobOfferRequest request() {
        return request(List.of(), List.of(), List.of());
    }

    private JobOfferRequest request(BigDecimal salaryMin, BigDecimal salaryMax) {
        return new JobOfferRequest(
                "Cariste", Sector.LOGISTIQUE, "Mons", Province.HAINAUT,
                "Poste en entrepôt, horaire de jour.", salaryMin, salaryMax,
                "2", false, null, List.of(), List.of(), List.of());
    }

    private JobOfferRequest request(
            List<OfferSkillRequirement> skills,
            List<OfferDegreeRequirement> degrees,
            List<OfferLanguageRequirement> languages) {
        return new JobOfferRequest(
                "Cariste", Sector.LOGISTIQUE, "Mons", Province.HAINAUT,
                "Poste en entrepôt, horaire de jour.",
                new BigDecimal("13.00"), new BigDecimal("18.00"),
                "2", false, null, skills, degrees, languages);
    }

    /** Les langues sont un référentiel fixe, alimenté au démarrage. */
    private Language anyLanguage() {
        return languageRepository.findAll().get(0);
    }
}
