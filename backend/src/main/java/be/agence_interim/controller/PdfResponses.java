package be.agence_interim.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Construction des réponses HTTP servant un document PDF. */
final class PdfResponses {

    private PdfResponses() {
    }

    /** Réponse affichant le PDF dans le navigateur, sous son nom de fichier ou le nom par défaut. */
    static ResponseEntity<Resource> inline(Resource resource, String defaultFileName) {
        String fileName = resource.getFilename() != null ? resource.getFilename() : defaultFileName;
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
