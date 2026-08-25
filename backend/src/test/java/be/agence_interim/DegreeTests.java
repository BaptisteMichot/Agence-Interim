package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.DegreeService;

/**
 * Les diplômes du profil, et la règle qui les rend comparables.
 *
 * <p>Le score de correspondance compare les diplômes par identifiant, sans regarder leur
 * libellé. Toute la difficulté tient donc à un point : « Bachelier en logistique » saisi
 * par l'employeur dans son offre et par l'intérimaire dans son profil doit désigner la
 * même ligne en base. Deux lignes distinctes ne provoqueraient aucune erreur — elles
 * feraient simplement dire au score que le candidat n'a pas le diplôme exigé, et le
 * candidat serait écarté sans que personne ne comprenne pourquoi.
 */
@SpringBootTest
class DegreeTests {

    @Autowired
    private DegreeService degreeService;

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
        worker = fixtures.user("diplome", Role.JOBSEEKER);
    }

    @Test
    @DisplayName("Un même diplôme saisi par deux personnes désigne une seule ligne")
    void thesameDegreeTypedByTwoPeopleIsOneSingleRow() {
        // La casse et les espaces ne font pas deux diplômes. C'est la condition pour que
        // le score reconnaisse chez le candidat ce que l'offre exige.
        Degree fromProfile = degreeService.resolveDegree(
                worker.getId(), null, DegreeType.BACHELIER, "Logistique appliquée");
        Degree fromOffer = degreeService.resolveDegree(
                fixtures.employer.getId(), null, DegreeType.BACHELIER, "  logistique appliquée  ");

        assertThat(fromOffer.getId()).isEqualTo(fromProfile.getId());
    }

    @Test
    @DisplayName("Le type distingue deux diplômes de même section")
    void thetypeTellsApartTwoDegreesOfTheSameField() {
        // Un bachelier et un master en informatique ne sont pas le même titre : les
        // confondre ferait passer l'un pour l'autre dans une exigence d'offre.
        Degree bachelier = degreeService.resolveDegree(
                worker.getId(), null, DegreeType.BACHELIER, "Informatique de gestion");
        Degree master = degreeService.resolveDegree(
                worker.getId(), null, DegreeType.MASTER, "Informatique de gestion");

        assertThat(master.getId()).isNotEqualTo(bachelier.getId());
    }

    @Test
    @DisplayName("Un diplôme perso reste hors de la liste proposée aux autres")
    void acustomDegreeStaysOutOfOtherPeoplesList() {
        // Retrouvable par son libellé — c'est ce qui le réunifie — mais absent de la liste
        // des autres, qui serait sinon encombrée des intitulés approximatifs de chacun.
        Degree mine = degreeService.resolveDegree(
                worker.getId(), null, DegreeType.BACHELIER, "Cuniculiculture appliquée");
        User other = fixtures.user("autre-dipl", Role.JOBSEEKER);

        assertThat(degreeService.available(worker.getId()))
                .extracting(degree -> degree.getId()).contains(mine.getId());
        assertThat(degreeService.available(other.getId()))
                .extracting(degree -> degree.getId()).doesNotContain(mine.getId());
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> degreeService.resolveDegree(other.getId(), mine.getId(), null, null));
    }

    @Test
    @DisplayName("Un diplôme ne figure qu'une fois au profil, avec son établissement et son année")
    void adegreeAppearsOnceWithItsSchoolAndYear() {
        // Le diplôme est désigné par son type et sa section, comme le fait l'écran : c'est
        // le service qui le retrouve ou le crée, l'utilisateur n'en connaît pas l'identifiant.
        int degreeId = degreeService.add(
                worker.getId(), null, DegreeType.BACHELIER, "Gestion des transports", "HEH", 2010)
                .getDegree().getId();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> degreeService.add(
                        worker.getId(), null, DegreeType.BACHELIER, "Gestion des transports",
                        "Autre école", 2012))
                .withMessageContaining("déjà dans votre profil");

        assertThat(degreeService.update(worker.getId(), degreeId, "HELHa", 2011))
                .satisfies(updated -> {
                    assertThat(updated.getInstitution()).isEqualTo("HELHa");
                    assertThat(updated.getGraduationYear()).isEqualTo(2011);
                });

        degreeService.remove(worker.getId(), degreeId);
        assertThat(degreeService.userDegrees(worker.getId())).isEmpty();
    }

    @Test
    @DisplayName("Un diplôme ne s'ajoute pas sans type ni section")
    void adegreeCannotBeAddedWithoutATypeAndAField() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> degreeService.resolveDegree(worker.getId(), null, null, "Logistique"))
                .withMessageContaining("type et la section");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> degreeService.resolveDegree(
                        worker.getId(), null, DegreeType.BACHELIER, "   "))
                .withMessageContaining("type et la section");
    }

    @Test
    @DisplayName("On ne retire pas du profil un diplôme qui n'y est pas")
    void nobodyRemovesADegreeThatIsNotInTheProfile() {
        Degree degree = degreeService.resolveDegree(
                worker.getId(), null, DegreeType.BACHELIER, "Sciences administratives");

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> degreeService.remove(worker.getId(), degree.getId()))
                .withMessageContaining("introuvable dans votre profil");
    }
}
