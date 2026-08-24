package be.agence_interim.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.ChangePasswordRequest;
import be.agence_interim.dto.MessageResponse;
import be.agence_interim.model.User;
import be.agence_interim.security.AuthCookie;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.AccountService;
import be.agence_interim.service.AuthService;
import jakarta.validation.Valid;

/** Gestion du compte de l'utilisateur courant : mot de passe, export, clôture. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;
    private final AuthCookie authCookie;

    public AccountController(
            AccountService accountService, AuthService authService, AuthCookie authCookie) {
        this.accountService = accountService;
        this.authService = authService;
        this.authCookie = authCookie;
    }

    /**
     * Changement de mot de passe.
     *
     * <p>Un cookie neuf accompagne la réponse : le changement révoque toutes les sessions,
     * y compris celle qui vient de le demander. Sans ce jeton de remplacement,
     * l'utilisateur serait déconnecté par son propre geste — et les autres appareils,
     * eux, le restent, ce qui est exactement l'effet recherché.
     */
    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        User user = accountService.changePassword(
                CurrentUser.id(jwt), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        authCookie.issue(authService.createToken(user)).toString())
                .body(new MessageResponse(
                        "Mot de passe modifie. Vos autres sessions ont ete deconnectees."));
    }

    /**
     * Export des données personnelles du compte (RGPD, droit d'accès).
     *
     * <p>Un document texte plutôt qu'un JSON : le destinataire est la personne concernée,
     * pas un programme, et le fichier doit s'ouvrir sans outil. Le jeu de caractères est
     * déclaré explicitement — sans lui, un lecteur qui suppose du Latin-1 abîme tous les
     * accents du document.
     */
    @GetMapping("/export")
    public ResponseEntity<String> export(@AuthenticationPrincipal Jwt jwt) {
        int userId = CurrentUser.id(jwt);
        // Pas d'identifiant dans le nom du fichier : c'est une clé primaire, et elle
        // n'apprend rien à celui qui reçoit le document.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("mes-donnees.txt", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(accountService.export(userId));
    }

    /**
     * Clôture du compte : suppression si rien n'a été engagé, anonymisation sinon.
     *
     * <p>Le cookie est effacé dans la foulée — la clôture révoque déjà les jetons, mais
     * laisser le navigateur en présenter un périmé n'apporterait qu'un écran d'erreur.
     */
    @DeleteMapping
    public ResponseEntity<Void> close(@AuthenticationPrincipal Jwt jwt) {
        accountService.close(CurrentUser.id(jwt));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookie.clear().toString())
                .build();
    }
}
