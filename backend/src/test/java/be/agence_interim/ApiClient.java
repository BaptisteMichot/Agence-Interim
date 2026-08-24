package be.agence_interim;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Client HTTP d'un utilisateur de test : garde ses cookies, renvoie le jeton CSRF.
 *
 * <p>Un client par personne, comme un navigateur par personne. C'est ce qui permet
 * d'écrire les tests d'autorisation dans les termes du problème — « l'employeur B
 * demande l'offre de l'employeur A » — plutôt qu'en manipulant des jetons à la main.
 *
 * <p>Le jeton CSRF est relu dans le pot de cookies avant chaque écriture : c'est
 * exactement ce que fait le frontend, et cela vérifie au passage que le serveur le
 * dépose bien.
 */
final class ApiClient {

    private final HttpClient http;
    private final CookieManager cookies;
    private final String baseUrl;

    ApiClient(int port) {
        this.cookies = new CookieManager();
        this.cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.http = HttpClient.newBuilder().cookieHandler(cookies).build();
        this.baseUrl = "http://localhost:" + port;
    }

    HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    HttpResponse<String> post(String path, String json) throws Exception {
        return send(withCsrf(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(body(json))));
    }

    HttpResponse<String> put(String path, String json) throws Exception {
        return send(withCsrf(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(body(json))));
    }

    HttpResponse<String> delete(String path) throws Exception {
        return send(withCsrf(HttpRequest.newBuilder(uri(path)).DELETE()));
    }

    /** POST volontairement dépourvu du jeton CSRF, pour vérifier qu'il est bien exigé. */
    HttpResponse<String> postWithoutCsrf(String path, String json) throws Exception {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(body(json)));
    }

    /** Connexion : la réponse dépose le cookie de session dans ce client. */
    HttpResponse<String> login(String email, String password) throws Exception {
        return post("/api/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    /** Valeur courante du cookie de session, pour vérifier qu'un jeton révoqué est refusé. */
    String sessionCookie() {
        return cookie("auth-token");
    }

    /** Repose un cookie de session donné : sert à rejouer un jeton révoqué. */
    void restoreSessionCookie(String value) {
        HttpCookie cookie = new HttpCookie("auth-token", value);
        cookie.setPath("/");
        cookies.getCookieStore().add(URI.create(baseUrl), cookie);
    }

    /**
     * Ajoute le jeton CSRF, en allant le chercher s'il manque encore.
     *
     * <p>Les routes exemptées de CSRF — connexion, inscription — traversent le filtre
     * sans le déclencher : elles ne déposent donc pas le cookie XSRF-TOKEN. Un client qui
     * vient de se connecter n'en a pas encore, exactement comme le navigateur. Le
     * frontend le récupère au passage de son appel {@code GET /api/auth/me} au
     * chargement ; ce client fait la même chose, au même endroit.
     */
    private HttpRequest.Builder withCsrf(HttpRequest.Builder builder) throws Exception {
        String token = cookie("XSRF-TOKEN");
        if (token == null) {
            get("/api/auth/me");
            token = cookie("XSRF-TOKEN");
        }
        return token == null ? builder : builder.header("X-XSRF-TOKEN", token);
    }

    private String cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(candidate -> name.equals(candidate.getName()))
                // Lambda plutôt que référence de méthode : convention du projet, une
                // référence non liée fait râler l'analyse de nullité d'Eclipse.
                .map(candidate -> candidate.getValue())
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpRequest.BodyPublisher body(String json) {
        return HttpRequest.BodyPublishers.ofString(json);
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    /** Extraction naïve d'un entier d'une réponse JSON, suffisante pour un test. */
    static int intField(String json, String field) {
        List<String> parts = List.of(json.split("\"" + field + "\"\\s*:\\s*"));
        if (parts.size() < 2) {
            throw new IllegalArgumentException("Champ " + field + " absent de : " + json);
        }
        StringBuilder digits = new StringBuilder();
        for (char character : parts.get(1).toCharArray()) {
            if (Character.isDigit(character)) {
                digits.append(character);
            } else {
                break;
            }
        }
        return Integer.parseInt(digits.toString());
    }
}
