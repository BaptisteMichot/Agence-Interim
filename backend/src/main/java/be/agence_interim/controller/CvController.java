package be.agence_interim.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import be.agence_interim.dto.CvResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.CvService;

/** CV (PDF) de l'utilisateur authentifié. */
@RestController
@RequestMapping("/api/profile/cv")
public class CvController {

    private final CvService cvService;

    public CvController(CvService cvService) {
        this.cvService = cvService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CvResponse upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        return new CvResponse(cvService.store(CurrentUser.id(jwt), file));
    }

    @GetMapping
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Jwt jwt) {
        Resource resource = cvService.load(CurrentUser.id(jwt));
        String fileName = resource.getFilename() != null ? resource.getFilename() : "cv.pdf";
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        cvService.delete(CurrentUser.id(jwt));
        return ResponseEntity.noContent().build();
    }
}
