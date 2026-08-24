package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;

/**
 * Le chiffrement au repos fait bien ce qu'il annonce.
 *
 * <p>Un convertisseur JPA est facile à croire sur parole : l'application relit ce
 * qu'elle a écrit, donc tout semble fonctionner même s'il ne chiffre rien. Ces tests
 * vont donc lire la colonne <em>en SQL</em>, en contournant JPA, et vérifient que la
 * valeur en clair ne s'y trouve pas.
 */
@SpringBootTest
class EncryptionTests {

    private static final String NATIONAL_NUMBER = "85.07.30-033.61";
    private static final String IBAN = "BE68 5390 0754 7034";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sensitiveFieldsAreUnreadableInTheDatabase() {
        User saved = userRepository.save(profileWith("chiffrement@example.be"));

        String storedNationalNumber = rawColumn("national_number", saved.getId());
        String storedIban = rawColumn("iban", saved.getId());

        // Ce que voit quelqu'un qui obtient une copie de la base.
        assertThat(storedNationalNumber).doesNotContain(NATIONAL_NUMBER).startsWith("enc:v1:");
        assertThat(storedIban).doesNotContain("5390").doesNotContain("7034").startsWith("enc:v1:");
    }

    @Test
    void applicationStillReadsTheClearValues() {
        User saved = userRepository.save(profileWith("chiffrement-lecture@example.be"));
        userRepository.flush();

        User reloaded = userRepository.requireById(saved.getId());

        assertThat(reloaded.getNationalNumber()).isEqualTo(NATIONAL_NUMBER);
        assertThat(reloaded.getIban()).isEqualTo(IBAN);
    }

    @Test
    void twoIdenticalValuesProduceDifferentCiphertexts() {
        User first = userRepository.save(profileWith("chiffrement-a@example.be"));
        User second = userRepository.save(profileWith("chiffrement-b@example.be"));

        // Le vecteur d'initialisation est tiré à chaque écriture : sans cela, deux
        // personnes portant le même IBAN se reconnaîtraient dans la base sans qu'on ait
        // besoin de déchiffrer quoi que ce soit.
        assertThat(rawColumn("iban", first.getId()))
                .isNotEqualTo(rawColumn("iban", second.getId()));
    }

    /** Lit la colonne sans passer par JPA, donc sans passer par le convertisseur. */
    private String rawColumn(String column, int userId) {
        return jdbcTemplate.queryForObject(
                "select " + column + " from users where id = ?", String.class, userId);
    }

    private User profileWith(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("$2a$10$peu-importe-ce-n-est-pas-l-objet-du-test");
        user.setFirstName("Test");
        user.setLastName("Chiffrement");
        user.setRole(Role.JOBSEEKER);
        user.setNationalNumber(NATIONAL_NUMBER);
        user.setIban(IBAN);
        return user;
    }
}
