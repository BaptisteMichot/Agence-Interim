package be.agence_interim.controller;

import be.agence_interim.dto.AuthResponse;
import be.agence_interim.dto.EmployerRegisterRequest;
import be.agence_interim.dto.ForgotPasswordRequest;
import be.agence_interim.dto.LoginRequest;
import be.agence_interim.dto.MessageResponse;
import be.agence_interim.dto.RegisterRequest;
import be.agence_interim.dto.ResetPasswordRequest;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.security.AuthCookie;
import be.agence_interim.security.ClientIp;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.security.Throttles;
import be.agence_interim.service.AuthService;
import be.agence_interim.service.EmployerAccessService;
import be.agence_interim.service.PasswordResetService;
import be.agence_interim.service.Strings;
import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Réponse unique de la demande de réinitialisation, succès ou non. Voir
     * {@link PasswordResetService} : dire si l'adresse est connue reviendrait à publier
     * la liste des comptes.
     */
    private static final String RESET_REQUESTED =
            "Si un compte existe pour cette adresse, un code vient d'y être envoyé.";

    private final AuthService authService;
    private final EmployerAccessService employerAccessService;
    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;
    private final AuthCookie authCookie;
    private final Throttles throttles;

    public AuthController(
            AuthService authService,
            EmployerAccessService employerAccessService,
            PasswordResetService passwordResetService,
            UserRepository userRepository,
            AuthCookie authCookie,
            Throttles throttles) {
        this.authService = authService;
        this.employerAccessService = employerAccessService;
        this.passwordResetService = passwordResetService;
        this.userRepository = userRepository;
        this.authCookie = authCookie;
        this.throttles = throttles;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        throttles.checkAndRecordSignup(ClientIp.of(httpRequest));
        User savedUser = authService.register(toUser(request));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, connect(savedUser).toString())
                .body(AuthResponse.of(savedUser, null, "Inscription reussie."));
    }

    /** Inscription employeur : crée le compte + une demande d'accès en attente (pas de connexion). */
    @PostMapping("/register-employer")
    public ResponseEntity<MessageResponse> registerEmployer(
            @Valid @RequestBody EmployerRegisterRequest request, HttpServletRequest httpRequest) {
        throttles.checkAndRecordSignup(ClientIp.of(httpRequest));
        employerAccessService.registerEmployer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse(
                        "Votre demande d'acces employeur a ete envoyee. Elle sera validee par l'agence."));
    }

    /**
     * Connexion.
     *
     * <p>Le quota est vérifié <em>avant</em> toute comparaison de mot de passe : un
     * compte déjà bloqué ne doit pas faire tourner BCrypt, sans quoi la protection
     * contre l'attaque par dictionnaire deviendrait elle-même un levier de saturation.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String email = Strings.normalizeEmail(request.email());
        String ip = ClientIp.of(httpRequest);
        throttles.checkLogin(email, ip);

        User user;
        try {
            user = authService.login(request.email(), request.password());
        } catch (BadCredentialsException e) {
            throttles.recordLoginFailure(email, ip);
            throw e;
        }

        throttles.recordLoginSuccess(email);
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

    /**
     * Déconnexion : efface le cookie et invalide les jetons déjà émis.
     *
     * <p>Effacer le cookie suffit à l'utilisateur devant son écran, mais ne retire rien
     * à une copie du jeton faite entre-temps : le jeton resterait valable jusqu'à son
     * expiration. Incrémenter la version de session le révoque pour de bon.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.revokeSessions(CurrentUser.id(jwt));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.clear().toString())
                .body(new MessageResponse("Deconnexion reussie."));
    }

    /** Envoie un code de réinitialisation si l'adresse correspond à un compte. */
    @PostMapping("/password/forgot")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestCode(request.email());
        return new MessageResponse(RESET_REQUESTED);
    }

    /**
     * Pose le nouveau mot de passe après vérification du code reçu par email.
     *
     * <p>Aucun cookie n'est déposé en retour : la réinitialisation sert souvent à
     * reprendre un compte dont on craint qu'il soit compromis, et connecter
     * automatiquement celui qui vient de saisir le code irait à l'encontre de ce réflexe.
     * L'utilisateur se reconnecte, ce qui vérifie au passage qu'il connaît bien le mot de
     * passe qu'il vient de choisir.
     */
    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.email(), request.code(), request.newPassword());
        return new MessageResponse("Mot de passe modifie. Vous pouvez vous reconnecter.");
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
        user.setCompanyName(request.companyName());
        return user;
    }
}
