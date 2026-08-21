package be.agence_interim.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Une page de résultats renvoyée au frontend.
 *
 * <p>Spring Data expose déjà {@link Page}, mais sa sérialisation JSON n'est pas
 * stable d'une version à l'autre : ce record fige le contrat de l'API avec les
 * seuls champs dont le frontend a besoin.
 *
 * @param content       les éléments de la page (au plus une taille de page)
 * @param page          numéro de la page, à partir de 0
 * @param size          taille de page demandée
 * @param totalElements nombre total d'éléments, toutes pages confondues
 * @param totalPages    nombre total de pages (0 si aucun élément)
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Convertit une page d'entités en page de DTO. */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** Page déjà constituée en mémoire (cas où le tri ne peut pas être fait en base). */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponse<>(
                content, page, size, totalElements, (int) Math.ceil((double) totalElements / size));
    }

    /** Page vide, sans aller interroger la base. */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0, 0);
    }
}
