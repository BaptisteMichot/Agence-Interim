package be.agence_interim.service;

import java.time.LocalDateTime;

import be.agence_interim.model.User;
import be.agence_interim.model.Role;
import be.agence_interim.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /**
     * Mot de passe dont le haché sert de leurre.
     *
     * <p>Il est comparé au mot de passe saisi lorsque l'email est inconnu, pour que la
     * requête coûte le même temps que si le compte existait. Sans ce détour, la réponse
     * revient en une milliseconde pour un email inconnu et en une centaine pour un email
     * connu — BCrypt est lent, c'est tout son intérêt. Le message d'erreur a beau être
     * identique dans les deux cas, le chronomètre, lui, dit lequel des deux s'est
     * produit : c'est une énumération de comptes à part entière.
     */
    private static final String DECOY_PASSWORD = "mot-de-passe-leurre-jamais-utilise";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String decoyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Haché une seule fois au démarrage : le recalculer à chaque échec ajouterait le
        // coût d'un chiffrement là où seule une comparaison est nécessaire.
        this.decoyHash = passwordEncoder.encode(DECOY_PASSWORD);
    }

    /**
     * Valide, normalise et enregistre un nouvel utilisateur.
     *
     * @param newUser utilisateur contenant encore le mot de passe en clair
     * @return utilisateur persiste avec un mot de passe BCrypt
     */
    public User register(User newUser) {
        String email = Strings.normalizeEmail(newUser.getEmail());
        newUser.setEmail(email);
        newUser.setRole(Role.JOBSEEKER);
        // Lance une exception car le mail est déjà utilisé
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        return userRepository.save(newUser);
    }

    /**
     * Authentifie un utilisateur deja present dans la base.
     *
     * @param rawEmail    email tel qu'il a ete saisi
     * @param rawPassword mot de passe en clair a comparer au hash BCrypt
     * @return utilisateur authentifie
     */
    public User login(String rawEmail, String rawPassword) {
        String email = Strings.normalizeEmail(rawEmail);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Compte inconnu : la comparaison a quand même lieu, contre le leurre, pour que
            // les deux chemins coûtent le même temps. Le résultat est ignoré, il est faux.
            passwordEncoder.matches(rawPassword, decoyHash);
            throw new BadCredentialsException("");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /** Genere un token apres une authentification reussie. */
    public String createToken(User user) {
        return jwtService.generateToken(user);
    }

    /**
     * Change le mot de passe de l'utilisateur connecté, après vérification de l'ancien.
     *
     * <p>Toutes les sessions sont révoquées au passage : c'est exactement le geste que
     * l'on fait quand on soupçonne que quelqu'un d'autre est entré, et laisser vivre les
     * jetons déjà émis viderait l'opération de son sens. L'appelant reçoit un jeton neuf,
     * pour ne pas être déconnecté de la session depuis laquelle il agit.
     *
     * @return l'utilisateur, porteur de sa nouvelle version de session
     */
    @Transactional
    public User changePassword(int userId, String currentPassword, String newPassword) {
        User user = userRepository.requireById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "Le nouveau mot de passe doit être différent de l'actuel.");
        }
        return applyNewPassword(user, newPassword);
    }

    /** Pose un nouveau mot de passe et révoque les jetons déjà émis. */
    @Transactional
    public User applyNewPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        return userRepository.save(user);
    }

    /**
     * Invalide tous les jetons déjà émis pour cet utilisateur.
     *
     * <p>Une déconnexion efface le cookie du navigateur, ce qui suffit à l'utilisateur
     * honnête mais ne retire rien à une copie du jeton faite entre-temps. Incrémenter la
     * version de session, elle, la retire vraiment.
     */
    @Transactional
    public void revokeSessions(int userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
        });
    }
}
