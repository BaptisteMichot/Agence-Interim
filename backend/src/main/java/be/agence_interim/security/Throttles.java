package be.agence_interim.security;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Quotas de tentatives des points d'entrée sensibles.
 *
 * <p>Tous les limiteurs sont réunis ici plutôt que dispersés dans les services : les
 * valeurs se lisent d'un coup d'œil, et la question « qu'est-ce qui est protégé, et à
 * quelle hauteur ? » a une seule réponse dans le code.
 *
 * <p>La connexion est comptée deux fois, et c'est délibéré. Le quota par email arrête
 * l'attaque qui s'acharne sur un compte connu ; le quota par adresse arrête celle qui
 * balaie beaucoup de comptes avec quelques mots de passe courants — un
 * <em>password spraying</em> reste sous le quota par email de chaque victime. Le second
 * est plus large que le premier parce qu'une adresse peut légitimement porter plusieurs
 * utilisateurs (réseau d'entreprise, université).
 */
@Component
public class Throttles {

    private static final Logger log = LoggerFactory.getLogger(Throttles.class);

    /** Tentatives infructueuses tolérées sur un même compte avant blocage temporaire. */
    private static final int LOGIN_PER_EMAIL = 5;
    /** Tentatives infructueuses tolérées depuis une même adresse, tous comptes confondus. */
    private static final int LOGIN_PER_IP = 20;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    /** Créations de compte depuis une même adresse : freine le remplissage automatisé. */
    private static final int SIGNUP_PER_IP = 5;
    private static final Duration SIGNUP_WINDOW = Duration.ofHours(1);

    /**
     * Emails déclenchés par un même utilisateur (code de signature, réinitialisation).
     * Sans ce plafond, un appel en boucle transforme l'application en outil d'inondation
     * de boîte mail — au détriment, en prime, de la réputation du domaine expéditeur.
     */
    private static final int MAIL_PER_ACTOR = 5;
    private static final Duration MAIL_WINDOW = Duration.ofMinutes(15);

    private final RateLimiter loginByEmail =
            new RateLimiter(LOGIN_PER_EMAIL, LOGIN_WINDOW, "de connexion sur ce compte");
    private final RateLimiter loginByIp =
            new RateLimiter(LOGIN_PER_IP, LOGIN_WINDOW, "de connexion depuis cette adresse");
    private final RateLimiter signupByIp =
            new RateLimiter(SIGNUP_PER_IP, SIGNUP_WINDOW, "d'inscription depuis cette adresse");
    private final RateLimiter mailByActor =
            new RateLimiter(MAIL_PER_ACTOR, MAIL_WINDOW, "d'envoi d'email");

    /**
     * Vérifie qu'une tentative de connexion est encore permise, avant toute comparaison
     * de mot de passe : un compte bloqué ne doit pas consommer un hachage BCrypt.
     */
    public void checkLogin(String email, String ip) {
        loginByIp.check(ip);
        loginByEmail.check(email);
    }

    /** Enregistre l'échec sur les deux compteurs et le journalise. */
    public void recordLoginFailure(String email, String ip) {
        loginByEmail.record(email);
        loginByIp.record(ip);
        // Trace volontairement sobre : l'email suffit à corréler une attaque, le mot de
        // passe essayé n'a rien à faire dans un journal.
        log.warn("Échec d'authentification pour {} depuis {}.", email, ip);
    }

    /** Connexion réussie : le compte n'est plus suspect, son compteur repart à zéro. */
    public void recordLoginSuccess(String email) {
        loginByEmail.clear(email);
    }

    /** Consomme une création de compte pour cette adresse. */
    public void checkAndRecordSignup(String ip) {
        signupByIp.check(ip);
        signupByIp.record(ip);
    }

    /**
     * Consomme un envoi d'email pour cet acteur.
     *
     * @param actor identité stable de l'appelant — identifiant utilisateur si la requête
     *              est authentifiée, adresse email visée sinon
     */
    public void checkAndRecordMail(String actor) {
        mailByActor.check(actor);
        mailByActor.record(actor);
    }
}
