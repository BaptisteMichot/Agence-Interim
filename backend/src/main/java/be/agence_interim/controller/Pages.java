package be.agence_interim.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Construction des {@link Pageable} à partir du paramètre {@code ?page=} des listes.
 *
 * <p>La taille de page est fixée ici, pas côté client : une requête ne peut pas
 * réclamer 50 000 éléments d'un coup en gonflant un paramètre.
 */
final class Pages {

    /** Nombre d'éléments par page pour toutes les listes de l'application. */
    static final int PAGE_SIZE = 10;

    /** Les fils de discussion se lisent par paquets plus larges (« messages plus anciens »). */
    static final int MESSAGE_PAGE_SIZE = 30;

    private Pages() {
    }

    /** Page demandée, taille standard. Un numéro négatif est ramené à la première page. */
    static Pageable of(int page) {
        return of(page, PAGE_SIZE);
    }

    static Pageable of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), size);
    }
}
