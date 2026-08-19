package be.agence_interim.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.ContractResponse;
import be.agence_interim.dto.SignContractRequest;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.ContractService;
import jakarta.validation.Valid;

/**
 * Contrat d'une mission : consultable et signable par les deux parties, consultable
 * par l'agence. L'accès est vérifié dans le service, la route étant simplement
 * réservée aux utilisateurs authentifiés.
 */
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/{missionId}")
    public ContractResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable int missionId) {
        return contractService.get(missionId, CurrentUser.id(jwt), CurrentUser.isAdmin(jwt));
    }

    /** Document du contrat (envoi simulé : il est mis à disposition sur le portail). */
    @GetMapping("/{missionId}/file")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Jwt jwt, @PathVariable int missionId) {
        Resource resource = contractService.load(missionId, CurrentUser.id(jwt), CurrentUser.isAdmin(jwt));
        return PdfResponses.inline(resource, "contrat.pdf");
    }

    /** Envoie par email le code à usage unique qui confirmera la signature. */
    @PostMapping("/{missionId}/signing-code")
    public ResponseEntity<Void> requestSigningCode(
            @AuthenticationPrincipal Jwt jwt, @PathVariable int missionId) {
        contractService.requestSigningCode(missionId, CurrentUser.id(jwt));
        return ResponseEntity.noContent().build();
    }

    /** Signature par la partie authentifiée, après vérification du code reçu par email. */
    @PostMapping("/{missionId}/sign")
    public ContractResponse sign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int missionId,
            @Valid @RequestBody SignContractRequest request) {
        return contractService.sign(missionId, CurrentUser.id(jwt), request.code());
    }
}
