package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import be.agence_interim.model.Application;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.Province;
import be.agence_interim.model.Role;
import be.agence_interim.model.Sector;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.AccountService;
import be.agence_interim.service.MailService;
import be.agence_interim.service.MatchNotificationService;

/**
 * Qui reçoit un email lorsqu'une offre est publiée.
 *
 * <p>Le contact automatique parcourt tous les intérimaires inscrits. Un compte clôturé
 * qui avait déjà postulé n'est pas supprimé mais anonymisé : sa ligne reste en base pour
 * que la chaîne contractuelle demeure vérifiable, avec son rôle intact et une adresse
 * email de remplacement. Rien ne le distinguait donc d'un utilisateur actif — et comme
 * la clôture vide son profil, il correspondait à 100 % à toute offre sans exigence,
 * c'est-à-dire qu'il était le premier contacté.
 *
 * <p>L'offre de ce test n'exige rien : c'est le seul cas où un profil vide obtient un
 * score suffisant, donc exactement celui qui met la règle à l'épreuve.
 */
@SpringBootTest
class MatchNotificationTests {

    @MockitoSpyBean
    private MailService mailService;

    @Autowired
    private MatchNotificationService matchNotificationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    @DisplayName("Un compte clôturé ne reçoit plus les offres publiées, celui qui reste les reçoit")
    void aClosedAccountIsNoLongerContacted() {
        User employer = create("contact-employeur@example.be", Role.EMPLOYER);
        JobOffer offer = publishOfferWithoutAnyRequirement(employer);

        // Ce candidat s'en va. Il a postulé, donc la clôture l'anonymise au lieu de le
        // supprimer : c'est le seul cas où la ligne survit, et donc le seul qui pose
        // problème. Il est créé avant l'autre pour être rencontré en premier par la
        // boucle de contact — s'il devait recevoir un email, ce serait avant celui dont
        // le test attend l'envoi.
        User leaving = create("contact-partant@example.be", Role.JOBSEEKER);
        apply(leaving, offer);
        accountService.close(leaving.getId());
        String replacementAddress = userRepository.requireById(leaving.getId()).getEmail();

        User staying = create("contact-restant@example.be", Role.JOBSEEKER);

        matchNotificationService.notifyMatchingJobSeekers(offer.getId());

        // Le contact part bien : la règle écarte les comptes clôturés, pas la
        // notification elle-même.
        verify(mailService, timeout(5_000))
                .send(eq(staying.getEmail()), anyString(), anyString());
        // Laisser la boucle s'achever avant de conclure qu'un envoi n'a pas eu lieu.
        verify(mailService, after(500).never())
                .send(eq(replacementAddress), anyString(), anyString());
    }

    @Test
    @DisplayName("La clôture laisse la ligne en base mais la sort du vivier des intérimaires")
    void aClosedAccountSurvivesInTheDatabaseButLeavesThePool() {
        // Les deux moitiés de la règle tiennent ensemble : supprimer la ligne casserait
        // les candidatures et les contrats qui la référencent, la laisser dans le vivier
        // revient à traiter les données de quelqu'un qui est parti.
        User employer = create("vivier-employeur@example.be", Role.EMPLOYER);
        User leaving = create("vivier-partant@example.be", Role.JOBSEEKER);
        apply(leaving, publishOfferWithoutAnyRequirement(employer));

        accountService.close(leaving.getId());

        assertThat(userRepository.findById(leaving.getId())).isPresent();
        assertThat(userRepository.requireById(leaving.getId()).getClosedAt()).isNotNull();
        assertThat(userRepository.findByRoleAndClosedAtIsNull(Role.JOBSEEKER))
                .extracting(user -> user.getId())
                .doesNotContain(leaving.getId());
    }

    // ------------------------------------------------------------------------------ outils

    /**
     * Offre sans compétence, langue, diplôme, véhicule ni expérience exigés : elle
     * correspond à tout le monde à 100 %, y compris à un profil vide.
     */
    private JobOffer publishOfferWithoutAnyRequirement(User employer) {
        JobOffer offer = new JobOffer();
        offer.setEmployer(employer);
        offer.setTitle("Manutentionnaire, formation assurée");
        offer.setSector(Sector.LOGISTIQUE);
        offer.setCity("Mons");
        offer.setProvince(Province.HAINAUT);
        offer.setDescription("Offre sans aucune exigence, pour éprouver le contact automatique.");
        offer.setPublishedAt(LocalDateTime.now());
        offer.setVehicleMandatory(false);
        return jobOfferRepository.save(offer);
    }

    private void apply(User jobSeeker, JobOffer offer) {
        Application application = new Application();
        application.setJobSeeker(jobSeeker);
        application.setJobOffer(offer);
        application.setApplicationTime(LocalDateTime.now());
        applicationRepository.save(application);
    }

    private User create(String email, Role role) {
        userRepository.findByEmail(email).ifPresent(existing -> userRepository.delete(existing));
        User user = new User();
        user.setEmail(email);
        user.setPassword("$2a$10$peu-importe-aucun-test-ne-se-connecte-ici");
        user.setFirstName("Test");
        user.setLastName("Contact");
        user.setRole(role);
        if (role == Role.EMPLOYER) {
            user.setCompanyName("Entreprise de test");
            user.setCompanyNumber("0454.460.440");
            user.setJointCommittee("111");
            user.setAddress("Rue de la Gare 1, 7000 Mons");
        }
        return userRepository.save(user);
    }
}
