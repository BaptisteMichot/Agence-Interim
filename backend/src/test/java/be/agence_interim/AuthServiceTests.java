package be.agence_interim;

import be.agence_interim.model.User;
import be.agence_interim.model.Role;
import be.agence_interim.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceTests {

    @Autowired
    private AuthService authService;

    @Test
    void registerAndLoginInConsole() {
        User user = new User();
        user.setLastName("Dupont");
        user.setFirstName("Jean");
        user.setEmail("test@example.be");
        user.setPassword("Password123456!");

        User registerResponse = authService.register(user);
        System.out.println("REGISTER -> " + registerResponse.getEmail());

        User loginResponse = authService.login("test@example.be", "Password123456!");
        System.out.println("LOGIN OK -> " + loginResponse.getEmail());

        assertThat(loginResponse.getEmail()).isEqualTo("test@example.be");

        assertThatThrownBy(() -> authService.login("test@example.be", "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);
        System.out.println("LOGIN KO -> Email ou mot de passe incorrect.");
    }

    @Test
    void registrationAlwaysAssignsJobseekerRole() {
        User user = new User();
        user.setLastName("Dupont");
        user.setFirstName("Marie");
        user.setEmail("jobseeker@example.be");
        user.setPassword("Password123456!");
        user.setRole(Role.INTERIM_RECRUITER);

        User registeredUser = authService.register(user);

        assertThat(registeredUser.getRole()).isEqualTo(Role.JOBSEEKER);
    }
}
