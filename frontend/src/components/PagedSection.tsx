import { useEffect, useRef, type ReactNode } from 'react';
import type { Page } from '../api/page';
import Pagination from './Pagination';
import { usePagedResource } from '../hooks/usePagedResource';

/** Ce qu'une section paginée fournit à son contenu. */
export type PagedSectionState<T> = {
  items: T[];
  /** Nombre total d'éléments de la section, toutes pages confondues. */
  total: number;
  loading: boolean;
  error: string | null;
  /** Les commandes de pagination, à placer en bas de la section. */
  pagination: ReactNode;
  reload: () => void;
};

/**
 * Une section de liste paginée pour elle-même.
 *
 * <p>Les pages qui empilent plusieurs blocs (missions à confirmer, en cours,
 * historique…) ne peuvent pas se contenter d'une seule requête découpée après coup :
 * la pagination ne verrait alors qu'une partie de chaque bloc. Chaque section
 * interroge donc le serveur avec son propre filtre et sa propre page.
 *
 * <p>`fetch` doit être stable (useCallback) : son identité sert à détecter un
 * changement de filtre, qui ramène la section à sa première page.
 */
export default function PagedSection<T>({
  fetch,
  loadError,
  label,
  reloadToken,
  children,
}: {
  fetch: (page: number) => Promise<Page<T>>;
  loadError: string;
  /** Nom des éléments comptés, au pluriel : « missions », « demandes »… */
  label: string;
  /**
   * Compteur que la page incrémente après une action ayant changé les données.
   * Sert aux pages dont une décision touche plusieurs sections à la fois.
   */
  reloadToken?: number;
  children: (state: PagedSectionState<T>) => ReactNode;
}) {
  const { items, pageData, loading, error, reload, goTo } = usePagedResource(fetch, loadError);

  // La page courante est conservée : c'est celle que l'utilisateur regarde. Si l'action
  // l'a vidée, `usePagedResource` recule d'une page de lui-même.
  const lastToken = useRef(reloadToken);
  useEffect(() => {
    if (lastToken.current !== reloadToken) {
      lastToken.current = reloadToken;
      reload();
    }
  }, [reloadToken, reload]);

  return children({
    items,
    total: pageData.totalElements,
    loading,
    error,
    pagination: <Pagination page={pageData} onChange={goTo} label={label} />,
    reload,
  });
}
