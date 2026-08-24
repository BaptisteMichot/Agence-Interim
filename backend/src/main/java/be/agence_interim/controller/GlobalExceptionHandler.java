package be.agence_interim.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import be.agence_interim.security.TooManyAttemptsException;

/**
 * Gestion des erreurs commune aux controllers REST (hors authentification, qui a
 * ses propres handlers locaux). Les handlers locaux d'un controller ont priorité.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Erreurs de validation des DTO : renvoie la liste des messages. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<String>> handleValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .toList();
        return ResponseEntity.badRequest().body(errors);
    }

    /** Données invalides côté métier. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    /** Ressource introuvable ou n'appartenant pas à l'utilisateur courant. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    /**
     * Quota de tentatives épuisé.
     *
     * <p>{@code 429} plutôt que {@code 403} : le refus est temporaire, et l'en-tête
     * {@code Retry-After} le dit — un client honnête sait quand revenir, l'interface peut
     * l'annoncer, et le journal du serveur distingue le blocage du rejet définitif.
     */
    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<String> handleTooManyAttempts(TooManyAttemptsException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfter().toSeconds()))
                .body(exception.getMessage());
    }

    /** Fichier trop volumineux (limite multipart dépassée). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.badRequest().body("Le CV ne doit pas depasser 5 Mo.");
    }
}
