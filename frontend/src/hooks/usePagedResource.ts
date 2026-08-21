import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../api/http';
import { EMPTY_PAGE, type Page } from '../api/page';

/**
 * Chargement d'une liste paginée, page par page.
 *
 * `fetcher` DOIT être stable (fonction de module, ou useCallback côté appelant) :
 * son identité sert justement à détecter un changement de filtre, qui ramène à la
 * première page — sinon on resterait sur une page 7 qui n'existe plus.
 */
export function usePagedResource<T>(
  fetcher: (page: number) => Promise<Page<T>>,
  loadErrorMessage: string,
) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<T> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Ajustement d'état pendant le rendu (motif documenté par React) : le fetcher a
  // changé, donc le filtre aussi ; React relance le rendu sans lancer la requête
  // de l'ancienne page.
  const [lastFetcher, setLastFetcher] = useState(() => fetcher);
  if (lastFetcher !== fetcher) {
    setLastFetcher(() => fetcher);
    setPage(0);
  }

  const reload = useCallback(async () => {
    setError(null);
    setLoading(true);
    try {
      setData(await fetcher(page));
    } catch (err) {
      setError(errorMessage(err, loadErrorMessage));
    } finally {
      setLoading(false);
    }
  }, [fetcher, page, loadErrorMessage]);

  useEffect(() => {
    reload();
  }, [reload]);

  // La dernière page s'est vidée (annulation, suppression…) : on recule d'une page
  // plutôt que d'afficher un écran vide sous une pagination qui annonce des éléments.
  useEffect(() => {
    if (data !== null && data.content.length === 0 && data.page > 0) {
      setPage(data.page - 1);
    }
  }, [data]);

  const goTo = useCallback((next: number) => {
    setPage(Math.max(next, 0));
  }, []);

  /** Remplace les éléments de la page courante sans recharger (mise à jour en place). */
  const setItems = useCallback((update: (items: T[]) => T[]) => {
    setData((current) => (current === null ? current : { ...current, content: update(current.content) }));
  }, []);

  const pageData: Page<T> = data ?? (EMPTY_PAGE as Page<T>);

  return { items: pageData.content, pageData, setItems, loading, error, setError, reload, goTo };
}
