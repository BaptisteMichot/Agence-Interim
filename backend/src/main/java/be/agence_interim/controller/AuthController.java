package be.agence_interim.controller;

import be.agence_interim.dto.AuthResponse;
import be.agence_interim.dto.EmployerRegisterRequest;
import be.agence_interim.dto.LoginRequest;
import be.agence_interim.dto.MessageResponse;
import be.agence_interim.dto.RegisterRequest;
import be.agence_interim.model.User;
import be.agence_interim.service.AuthService;
import be.agence_interim.service.EmployerAccessService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    public AuthController(AuthService authService, EmployerAccessService employerAccessService) {
        this.authService = authService;
        this.employerAccessService = employerAccessService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User savedUser = authService.register(toUser(request));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AuthResponse.of(
                        savedUser,
                        null,
                        authService.createToken(savedUser),
                        "Inscription reussie."));
    }

    /** Inscription employeur : crée le compte + une demande d'accès en attente (pas de connexion). */
    @PostMapping("/register-employer")
    public ResponseEntity<MessageResponse> registerEmployer(
            @Valid @RequestBody EmployerRegisterRequest request) {
        employerAccessService.registerEmployer(
                request.lastName(),
                request.firstName(),
                request.email(),
                request.password(),
                request.companyName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse(
                        "Votre demande d'acces employeur a ete envoyee. Elle sera validee par l'agence."));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        return AuthResponse.of(
                user,
                employerAccessService.latestStatus(user.getId()),
                authService.createToken(user),
                "Connexion reussie.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
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
