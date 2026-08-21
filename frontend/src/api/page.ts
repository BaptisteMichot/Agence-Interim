import { apiGet } from './http';

/** Une page de résultats telle que le backend la renvoie (cf. `dto/PageResponse`). */
export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/** Page vide, pour l'état initial et les cas d'erreur. */
export const EMPTY_PAGE: Page<never> = {
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
};

/**
 * GET d'une liste paginée. Le numéro de page est le seul paramètre envoyé : la
 * taille est fixée côté serveur, un client ne peut pas réclamer toute la table.
 * `extra` porte les filtres propres à la liste (statut, tri…).
 */
export function apiGetPage<T>(
  path: string,
  page: number,
  extra?: Record<string, string | number | undefined>,
): Promise<Page<T>> {
  const params = new URLSearchParams({ page: String(page) });
  for (const [key, value] of Object.entries(extra ?? {})) {
    if (value !== undefined) {
      params.set(key, String(value));
    }
  }
  return apiGet<Page<T>>(`${path}?${params}`);
}

/** Lit un compteur renvoyé sous la forme `{ "count": n }`. */
export function apiGetCount(path: string): Promise<number> {
  return apiGet<{ count: number }>(path).then((body) => body.count);
}
