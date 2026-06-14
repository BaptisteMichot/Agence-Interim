package be.agence_interim;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import be.agence_interim.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthControllerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerAndLoginThroughWebEndpoints() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> registerResponse = client.send(
                post("/api/auth/register", """
                        {
                          "lastName": "Dupont",
                          "firstName": "Jean",
                          "email": "web@example.be",
                          "password": "Password123456!",
                          "hasVehicle": true,
                          "birthdate": "2000-01-15",
                          "cvFilePath": null,
                          "companyName": null
                        }
                        """),
                HttpResponse.BodyHandlers.ofString());

        assertThat(registerResponse.statusCode()).isEqualTo(201);
        assertThat(registerResponse.body()).contains("\"email\":\"web@example.be\"");
        assertThat(registerResponse.body()).contains("\"message\":\"Inscription reussie.\"");
        assertThat(registerResponse.body()).contains("\"role\":\"JOBSEEKER\"");
        assertThat(registerResponse.body()).contains("\"token\":");
        assertThat(registerResponse.body()).doesNotContain("Password123456!");

        HttpResponse<String> loginResponse = client.send(
                post("/api/auth/login", """
                        {
                          "email": "web@example.be",
                          "password": "Password123456!"
                        }
                        """),
                HttpResponse.BodyHandlers.ofString());

        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.body()).contains("\"email\":\"web@example.be\"");
        assertThat(loginResponse.body()).contains("\"message\":\"Connexion reussie.\"");
        assertThat(loginResponse.body()).contains("\"token\":");
        assertThat(loginResponse.body()).doesNotContain("Password123456!");
    }

    @Test
    void registrationReturnsAnnotationValidationMessages() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                post("/api/auth/register", """
                        {
                          "lastName": "",
                          "firstName": "",
                          "email": "invalid-email",
                          "password": "short",
                          "hasVehicle": false,
                          "companyName": "1234567890123456789012345678901"
                        }
                        """),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("Le nom est obligatoire.");
        assertThat(response.body()).contains("Le prenom est obligatoire.");
        assertThat(response.body()).contains("L'email doit etre une adresse valide");
        assertThat(response.body()).contains("Le mot de passe doit contenir au moins 14 caracteres.");
        assertThat(response.body()).contains("Le nom de l'entreprise ne peut pas depasser 30 caracteres.");
    }

    @Test
    void loginDoesNotExposeCredentialValidationDetails() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                post("/api/auth/login", """
                        {
                          "email": "",
                          "password": ""
                        }
                        """),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).isEqualTo("Identifiants incorrects, veuillez réessayer.");
    }

    @Test
    void registrationKeepsHasVehicleNullWhenMissing() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                post("/api/auth/register", """
                        {
                          "lastName": "Dupont",
                          "firstName": "Jean",
                          "email": "minimal@example.be",
                          "password": "Password123456!"
                        }
                        """),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"role\":\"JOBSEEKER\"");
        assertThat(userRepository.findByEmail("minimal@example.be"))
                .get()
                .extracting(user -> user.getHasVehicle())
                .isNull();
    }

    private HttpRequest post(String path, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
