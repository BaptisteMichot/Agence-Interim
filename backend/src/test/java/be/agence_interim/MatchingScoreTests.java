package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.Experience;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.Skill;
import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.User;
import be.agence_interim.service.MatchingService;
import be.agence_interim.service.MatchingService.CandidateProfile;
import be.agence_interim.service.MatchingService.MatchScore;
import be.agence_interim.service.MatchingService.OfferRequirements;

/**
 * Le score de correspondance entre le profil d'un intérimaire et une offre d'emploi.
 *
 * <p>C'est la fonction que l'analyse appelle « correspondance intelligente » : elle
 * décide quelles offres apparaissent dans « Pour moi » et, à la publication d'une offre,
 * qui reçoit un email. Une erreur ici ne provoque aucune panne — elle propose simplement
 * les mauvaises offres aux mauvaises personnes, ce qui ne se voit pas.
 *
 * <p>Les valeurs attendues sont recalculées à la main à partir de la règle énoncée
 * (score = 100 × Σ(poids × taux) / Σ(poids), poids 2 pour un critère exigé, 1 pour un
 * critère souhaité) et non reprises d'une exécution : un test qui recopierait la sortie
 * du code ne vérifierait plus rien.
 *
 * <p>Le service est instancié <em>sans aucun dépôt</em>. Ce n'est pas un raccourci de
 * test, c'est la première propriété que ces tests établissent : le calcul du score ne lit
 * rien en base, il ne dépend que du profil et des exigences qu'on lui passe. C'est ce qui
 * rend le résultat reproductible et l'algorithme discutable indépendamment du stockage.
 */
class MatchingScoreTests {

    private static final int CARISTE = 1;
    private static final int SOUDURE = 2;
    private static final int ANGLAIS = 10;
    private static final int BACHELIER_INFORMATIQUE = 20;
    private static final int GRAPHISTE = 21;

    private final MatchingService matching = new MatchingService(null, null, null, null, null, null, null);

    @Test
    @DisplayName("Cariste expert et anglais B1 sur une offre « cariste avancé exigé, anglais B2 souhaité » : 92 %")
    void theReferenceExampleScoresNinetyTwoPercent() {
        // L'exemple qui sert de référence à toute la formule.
        // Cariste : expert au-dessus d'avancé, taux 1, poids 2, contribue 2.
        // Anglais : B1 sous B2, crédit partiel (2+1)/(3+1) = 0,75, poids 1, contribue 0,75.
        // score = 100 × 2,75 / 3 = 91,67, arrondi à 92.
        MatchScore match = matching.score(
                profile().skill(CARISTE, SkillLevel.EXPERT).language(ANGLAIS, LanguageLevel.B1).build(),
                offer().skill(CARISTE, SkillLevel.AVANCE, true).language(ANGLAIS, LanguageLevel.B2, false).build());

        assertThat(match.score()).isEqualTo(92);
        assertThat(match.mandatoryOk()).isTrue();
    }

    @Test
    @DisplayName("Un critère exigé pèse deux fois plus lourd qu'un critère souhaité")
    void aMandatoryCriterionWeighsTwiceAsMuch() {
        // Deux offres portant les deux mêmes compétences, avec les rôles inversés : le
        // candidat en satisfait exactement une des deux dans les deux cas. Si les poids
        // étaient égaux, les deux scores vaudraient 50. C'est l'écart 67/33 qui montre que
        // ce que l'employeur a marqué comme exigé compte double.
        CandidateProfile candidate = profile().skill(CARISTE, SkillLevel.EXPERT).build();

        // 100 × (2×1 + 1×0) / 3 = 67
        assertThat(matching.score(candidate,
                offer().skill(CARISTE, SkillLevel.AVANCE, true).skill(SOUDURE, SkillLevel.AVANCE, false).build())
                .score()).isEqualTo(67);

        // 100 × (1×1 + 2×0) / 3 = 33
        assertThat(matching.score(candidate,
                offer().skill(CARISTE, SkillLevel.AVANCE, false).skill(SOUDURE, SkillLevel.AVANCE, true).build())
                .score()).isEqualTo(33);
    }

    @Test
    @DisplayName("Un score élevé ne rend pas éligible : il manque un niveau exigé, le candidat est écarté")
    void aHighScoreDoesNotMakeACandidateEligible() {
        // Cariste avancé alors que l'offre exige expert : (2+1)/(3+1) = 0,75, donc un score
        // de 75 — mais l'exigence n'est pas remplie. Les deux informations sont séparées
        // exprès : le score classe, la règle d'exclusion filtre. Les confondre enverrait à
        // l'employeur des candidats qui ne peuvent pas tenir le poste.
        MatchScore match = matching.score(
                profile().skill(CARISTE, SkillLevel.AVANCE).build(),
                offer().skill(CARISTE, SkillLevel.EXPERT, true).build());

        assertThat(match.score()).isEqualTo(75);
        assertThat(match.mandatoryOk()).isFalse();
        assertThat(match.shouldContact()).isFalse();
    }

    @Test
    @DisplayName("Posséder la compétence, même au niveau débutant, vaut mieux que ne pas la posséder")
    void holdingASkillAtTheLowestLevelBeatsNotHoldingIt() {
        // Le « +1 » de la formule de crédit partiel existe pour cela : sans lui, un débutant
        // (position 0) obtiendrait 0, soit exactement le score de quelqu'un qui n'a jamais
        // touché à la compétence. Or l'agence a tout intérêt à les distinguer.
        OfferRequirements expertWanted = offer().skill(CARISTE, SkillLevel.EXPERT, false).build();

        assertThat(matching.score(profile().skill(CARISTE, SkillLevel.DEBUTANT).build(), expertWanted).score())
                .isEqualTo(25);
        assertThat(matching.score(profile().build(), expertWanted).score())
                .isEqualTo(0);
    }

    @Test
    @DisplayName("Dépasser le niveau demandé ne rapporte rien de plus que l'atteindre")
    void exceedingTheRequiredLevelEarnsNothingExtra() {
        // Le score mesure l'adéquation au poste, pas la valeur du candidat : un expert et un
        // avancé conviennent également à une offre qui demande « avancé ». Récompenser la
        // surqualification ferait remonter en tête des offres situées en dessous du profil.
        OfferRequirements advancedWanted = offer().skill(CARISTE, SkillLevel.AVANCE, false).build();

        assertThat(matching.score(profile().skill(CARISTE, SkillLevel.AVANCE).build(), advancedWanted).score())
                .isEqualTo(100);
        assertThat(matching.score(profile().skill(CARISTE, SkillLevel.EXPERT).build(), advancedWanted).score())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Une compétence exigée sans niveau précis est satisfaite par n'importe quel niveau")
    void aSkillRequiredWithoutALevelIsSatisfiedByAnyLevel() {
        // Le niveau requis est facultatif dans le modèle de données : l'employeur peut
        // demander « soudure » sans dire à quel point. Il déclare alors se contenter de la
        // compétence, et un débutant remplit l'exigence.
        OfferRequirements anyLevel = offer().skill(SOUDURE, null, true).build();

        MatchScore beginner = matching.score(profile().skill(SOUDURE, SkillLevel.DEBUTANT).build(), anyLevel);
        assertThat(beginner.score()).isEqualTo(100);
        assertThat(beginner.mandatoryOk()).isTrue();

        // Ne pas posséder la compétence du tout reste éliminatoire.
        assertThat(matching.score(profile().build(), anyLevel).mandatoryOk()).isFalse();
    }

    @Test
    @DisplayName("Les langues sont comparées sur l'échelle du CECR, A1 étant le plus bas")
    void languagesAreComparedOnTheCommonEuropeanScale() {
        // A2 est en position 1, C1 en position 4 : (1+1)/(4+1) = 0,4. Le crédit partiel est
        // nettement plus sévère que pour les compétences parce que l'échelle compte six
        // niveaux au lieu de quatre — ce qui se défend, un C1 ne s'improvise pas.
        OfferRequirements c1Wanted = offer().language(ANGLAIS, LanguageLevel.C1, false).build();

        assertThat(matching.score(profile().language(ANGLAIS, LanguageLevel.A2).build(), c1Wanted).score())
                .isEqualTo(40);
        assertThat(matching.score(profile().language(ANGLAIS, LanguageLevel.C1).build(), c1Wanted).score())
                .isEqualTo(100);
        assertThat(matching.score(profile().language(ANGLAIS, LanguageLevel.C2).build(), c1Wanted).score())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Un diplôme est acquis ou ne l'est pas : il n'existe pas de demi-diplôme")
    void aDegreeIsAllOrNothing() {
        // Contrairement aux compétences et aux langues, un diplôme ne se possède pas à
        // moitié : il a été délivré ou il ne l'a pas été. Aucun crédit partiel, donc, et un
        // diplôme voisin ne remplace pas celui qui est demandé.
        OfferRequirements diplomaRequired = offer().degree(BACHELIER_INFORMATIQUE, true).build();

        assertThat(matching.score(profile().degree(BACHELIER_INFORMATIQUE).build(), diplomaRequired))
                .isEqualTo(new MatchScore(true, 100));
        assertThat(matching.score(profile().degree(GRAPHISTE).build(), diplomaRequired))
                .isEqualTo(new MatchScore(false, 0));
    }

    @Test
    @DisplayName("Le véhicule n'est un critère que si l'offre l'exige, et il est alors éliminatoire")
    void theVehicleOnlyCountsWhenTheOfferRequiresIt() {
        // Une offre desservie par les transports en commun ne doit pas pénaliser celui qui
        // n'a pas de voiture : tant que la case n'est pas cochée, le véhicule n'entre pas
        // dans le calcul. Cochée, il devient éliminatoire par nature — l'intérimaire ne peut
        // pas se rendre sur le lieu de travail, et aucun score ne rattrape cela.
        CandidateProfile withoutCar = profile().skill(CARISTE, SkillLevel.EXPERT).build();
        CandidateProfile withCar = profile().skill(CARISTE, SkillLevel.EXPERT).vehicle().build();

        assertThat(matching.score(withoutCar, offer().skill(CARISTE, SkillLevel.AVANCE, false).build()))
                .isEqualTo(new MatchScore(true, 100));

        OfferRequirements carRequired = offer()
                .skill(CARISTE, SkillLevel.AVANCE, false).vehicleRequired().build();
        // 100 × (1×1 + 2×0) / 3 = 33, et l'exigence n'est pas remplie.
        assertThat(matching.score(withoutCar, carRequired)).isEqualTo(new MatchScore(false, 33));
        assertThat(matching.score(withCar, carRequired)).isEqualTo(new MatchScore(true, 100));
    }

    @Test
    @DisplayName("Un profil qui ne dit rien du véhicule est traité comme n'en ayant pas")
    void anUndeclaredVehicleCountsAsNoVehicle() {
        // Le champ est facultatif au profil. En l'absence de réponse, l'agence ne peut pas
        // promettre à l'employeur que l'intérimaire sera sur place : le doute profite à
        // l'exigence, pas au candidat.
        assertThat(matching.score(profile().build(), offer().vehicleRequired().build()).mandatoryOk()).isFalse();
    }

    @Test
    @DisplayName("L'expérience n'écarte personne : elle ne fait que peser sur le score")
    void experienceNeverExcludesACandidate() {
        // L'offre porte un nombre d'années, jamais un caractère obligatoire — le modèle de
        // données ne prévoit pas de case à cocher pour l'expérience. Le choix se défend : la
        // durée ne dit rien de ce que la personne sait faire, et l'employeur garde la main
        // au moment de choisir parmi les candidatures reçues.
        OfferRequirements fourYearsWanted = offer()
                .skill(CARISTE, SkillLevel.AVANCE, true).experienceYears("4").build();

        // 2 ans sur 4 demandés, taux 0,5. score = 100 × (2×1 + 1×0,5) / 3 = 83.
        MatchScore half = matching.score(
                profile().skill(CARISTE, SkillLevel.EXPERT).experienceOverYears(2).build(), fourYearsWanted);
        assertThat(half.score()).isEqualTo(83);
        assertThat(half.mandatoryOk()).isTrue();

        // Aucune expérience, taux 0. score = 100 × (2×1 + 1×0) / 3 = 67, et le candidat
        // reste proposé : c'est exactement le cas du débutant que l'agence veut placer.
        MatchScore none = matching.score(profile().skill(CARISTE, SkillLevel.EXPERT).build(), fourYearsWanted);
        assertThat(none.score()).isEqualTo(67);
        assertThat(none.mandatoryOk()).isTrue();
    }

    @Test
    @DisplayName("Une expérience toujours en cours est comptée jusqu'à aujourd'hui")
    void anOngoingExperienceCountsUntilToday() {
        // Sans cela, l'intérimaire en poste depuis trois ans compterait zéro année
        // d'expérience tant qu'il n'a pas quitté son emploi — soit l'inverse de ce que le
        // score cherche à mesurer.
        MatchScore match = matching.score(
                profile().ongoingExperienceSince(3).build(),
                offer().experienceYears("3").build());

        assertThat(match).isEqualTo(new MatchScore(true, 100));
    }

    @Test
    @DisplayName("L'expérience au-delà de ce qui est demandé ne rapporte rien de plus")
    void experienceBeyondWhatIsAskedEarnsNothingExtra() {
        // Le taux est plafonné à 1 : dix ans d'expérience sur une offre qui en demande deux
        // ne doivent pas pouvoir compenser une compétence manquante ailleurs.
        assertThat(matching.score(
                profile().experienceOverYears(10).build(),
                offer().experienceYears("2").build()).score())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Une offre qui ne demande aucune année d'expérience ne crée pas de critère")
    void anOfferAskingForZeroYearsCreatesNoCriterion() {
        // « 0 an » n'est pas un critère satisfait à 0 % : c'est l'absence de critère. Le
        // traiter autrement diviserait par deux le score de tout candidat sans expérience
        // sur les offres ouvertes aux débutants, qui sont justement celles qui les visent.
        assertThat(matching.score(
                profile().skill(CARISTE, SkillLevel.EXPERT).build(),
                offer().skill(CARISTE, SkillLevel.AVANCE, false).experienceYears("0").build()).score())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Une offre qui n'exige rien correspond à tout le monde")
    void anOfferWithoutAnyRequirementMatchesEveryone() {
        // Il n'y a rien à mesurer : plutôt qu'une division par zéro, la règle retenue est que
        // l'offre convient à tous. Conséquence à connaître — à sa publication, une telle
        // offre écrit à l'ensemble des intérimaires inscrits.
        assertThat(matching.score(profile().build(), offer().build()))
                .isEqualTo(new MatchScore(true, 100));
    }

    @Test
    @DisplayName("Un profil vide n'est pas écarté d'une offre sans exigence, mais son score le laisse hors du contact")
    void anEmptyProfileIsRankedLastRatherThanExcluded() {
        // Rien d'obligatoire n'est en défaut, donc rien à exclure : l'offre reste visible
        // dans « Pour moi », en bas de liste. Elle ne déclenche simplement pas d'email,
        // parce qu'écrire à quelqu'un dont on ignore tout n'est pas une recommandation.
        MatchScore match = matching.score(
                profile().build(), offer().skill(CARISTE, SkillLevel.AVANCE, false).build());

        assertThat(match).isEqualTo(new MatchScore(true, 0));
        assertThat(match.shouldContact()).isFalse();
    }

    @Test
    @DisplayName("Le contact automatique exige à la fois les critères obligatoires et un score d'au moins 50")
    void automaticContactNeedsBothConditions() {
        // Les deux conditions sont indépendantes, et le seuil est inclusif : c'est la règle
        // qui décide qui reçoit un email à chaque publication d'offre, donc celle dont
        // l'assouplissement involontaire se paierait en courriers non sollicités.
        assertThat(new MatchScore(true, 50).shouldContact()).isTrue();
        assertThat(new MatchScore(true, 49).shouldContact()).isFalse();
        assertThat(new MatchScore(false, 100).shouldContact()).isFalse();
    }

    // ------------------------------------------------------------------------------
    // Construction des données de test. Les deux constructeurs disent l'offre et le
    // profil dans les mots du métier, pour que chaque test tienne en trois lignes.
    // ------------------------------------------------------------------------------

    private static OfferBuilder offer() {
        return new OfferBuilder();
    }

    private static ProfileBuilder profile() {
        return new ProfileBuilder();
    }

    private static final class OfferBuilder {

        private final JobOffer jobOffer = new JobOffer();
        private final List<SkillJobOffer> skills = new ArrayList<>();
        private final List<DegreeJobOffer> degrees = new ArrayList<>();
        private final List<LanguageJobOffer> languages = new ArrayList<>();

        private OfferBuilder() {
            jobOffer.setVehicleMandatory(false);
        }

        OfferBuilder skill(int skillId, SkillLevel requiredLevel, boolean mandatory) {
            Skill skill = new Skill();
            skill.setId(skillId);
            SkillJobOffer required = new SkillJobOffer();
            required.setSkill(skill);
            required.setRequiredLevel(requiredLevel);
            required.setIsMandatory(mandatory);
            skills.add(required);
            return this;
        }

        OfferBuilder language(int languageId, LanguageLevel requiredLevel, boolean mandatory) {
            Language language = new Language();
            language.setId(languageId);
            LanguageJobOffer required = new LanguageJobOffer();
            required.setLanguage(language);
            required.setRequiredLevel(requiredLevel);
            required.setIsMandatory(mandatory);
            languages.add(required);
            return this;
        }

        OfferBuilder degree(int degreeId, boolean mandatory) {
            Degree degree = new Degree();
            degree.setId(degreeId);
            DegreeJobOffer required = new DegreeJobOffer();
            required.setDegree(degree);
            required.setIsMandatory(mandatory);
            degrees.add(required);
            return this;
        }

        OfferBuilder vehicleRequired() {
            jobOffer.setVehicleMandatory(true);
            return this;
        }

        OfferBuilder experienceYears(String years) {
            jobOffer.setExperienceTime(years);
            return this;
        }

        OfferRequirements build() {
            return new OfferRequirements(jobOffer, skills, degrees, languages);
        }
    }

    private static final class ProfileBuilder {

        private final User jobSeeker = new User();
        private final Map<Integer, SkillLevel> skills = new HashMap<>();
        private final Map<Integer, LanguageLevel> languages = new HashMap<>();
        private final List<Integer> degrees = new ArrayList<>();
        private final List<Experience> experiences = new ArrayList<>();

        ProfileBuilder skill(int skillId, SkillLevel level) {
            skills.put(skillId, level);
            return this;
        }

        ProfileBuilder language(int languageId, LanguageLevel level) {
            languages.put(languageId, level);
            return this;
        }

        ProfileBuilder degree(int degreeId) {
            degrees.add(degreeId);
            return this;
        }

        ProfileBuilder vehicle() {
            jobSeeker.setHasVehicle(true);
            return this;
        }

        /** Une expérience terminée aujourd'hui, entamée il y a le nombre d'années donné. */
        ProfileBuilder experienceOverYears(int years) {
            return experience(LocalDate.now().minusYears(years), LocalDate.now());
        }

        /** Une expérience commencée il y a le nombre d'années donné et toujours en cours. */
        ProfileBuilder ongoingExperienceSince(int years) {
            return experience(LocalDate.now().minusYears(years), null);
        }

        private ProfileBuilder experience(LocalDate start, LocalDate end) {
            Experience experience = new Experience();
            experience.setStartDate(start);
            experience.setEndDate(end);
            experiences.add(experience);
            return this;
        }

        CandidateProfile build() {
            return new CandidateProfile(jobSeeker, skills, languages, degrees, experiences);
        }
    }
}
