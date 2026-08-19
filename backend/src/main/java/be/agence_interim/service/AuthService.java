package be.agence_interim.service;

import be.agence_interim.model.User;
import be.agence_interim.model.Role;
import be.agence_interim.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(""));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("");
        }

        return user;
    }

    /** Genere un token apres une authentification reussie. */
    public String createToken(User user) {
        return jwtService.generateToken(user);
    }
}
