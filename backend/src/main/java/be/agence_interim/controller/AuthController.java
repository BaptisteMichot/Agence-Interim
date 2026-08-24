package be.agence_interim.controller;

import be.agence_interim.dto.AuthResponse;
import be.agence_interim.dto.EmployerRegisterRequest;
import be.agence_interim.dto.LoginRequest;
import be.agence_interim.dto.MessageResponse;
import be.agence_interim.dto.RegisterRequest;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.security.AuthCookie;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.AuthService;
import be.agence_interim.service.EmployerAccessService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String INVALID_CREDENTIALS = "Identifiants incorrects, veuillez réessayer.";

    private final AuthService authService;
    private final EmployerAccessService employerAccessService;
    private final UserRepository userRepository;
    private final AuthCookie authCookie;

    public AuthController(
            AuthService authService,
            EmployerAccessService employerAccessService,
            UserRepository userRepository,
            AuthCookie authCookie) {
        this.authService = authService;
        this.employerAccessService = employerAccessService;
        this.userRepository = userRepository;
        this.authCookie = authCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User savedUser = authService.register(toUser(request));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, connect(savedUser).toString())
                .body(AuthResponse.of(savedUser, null, "Inscription reussie."));
    }

    /** Inscription employeur : crée le compte + une demande d'accès en attente (pas de connexion). */
    @PostMapping("/register-employer")
    public ResponseEntity<MessageResponse> registerEmployer(
            @Valid @RequestBody EmployerRegisterRequest request) {
        employerAccessService.registerEmployer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse(
                        "Votre demande d'acces employeur a ete envoyee. Elle sera validee par l'agence."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, connect(user).toString())
                .body(AuthResponse.of(
                        user, employerAccessService.latestStatus(user.getId()), "Connexion reussie."));
    }

    /**
     * Identité de la session en cours.
     *
     * <p>Le cookie étant HttpOnly, la page ne peut pas y lire qui elle représente : elle
     * le demande ici au démarrage. Cet appel dépose au passage le cookie XSRF-TOKEN dont
     * la première écriture aura besoin.
     */
    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal Jwt jwt) {
        User user = userRepository.requireById(CurrentUser.id(jwt));
        return AuthResponse.of(
                user, employerAccessService.latestStatus(user.getId()), "Session active.");
    }

    /** Efface le cookie de session : la page ne peut pas le faire elle-même. */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.clear().toString())
                .body(new MessageResponse("Deconnexion reussie."));
    }

    /** Cookie de session à poser sur la réponse pour l'utilisateur qui vient de s'identifier. */
    private ResponseCookie connect(User user) {
        return authCookie.issue(authService.createToken(user));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(INVALID_CREDENTIALS);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception) {
        if ("loginRequest".equals(exception.getBindingResult().getObjectName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(INVALID_CREDENTIALS);
        }

        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .toList();

        return ResponseEntity.badRequest().body(errors);
    }

    private User toUser(RegisterRequest request) {
        User user = new User();
        user.setLastName(request.lastName());
        user.setFirstName(request.firstName());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setHasVehicle(request.hasVehicle());
        user.setBirthdate(request.birthdate());
        user.setCvFilePath(request.cvFilePath());
        user.setCompanyName(request.companyName());
        return user;
    }
}
