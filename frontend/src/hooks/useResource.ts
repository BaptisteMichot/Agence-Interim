import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../api/http';

/**
 * Chargement d'une ressource au montage, avec rechargement à la demande.
 * `fetcher` DOIT être stable (fonction de module, ou useCallback côté appelant) :
 * l'effet se ré-exécute quand son identité change.
 */
export function useResource<T>(fetcher: () => Promise<T>, loadErrorMessage: string) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      setData(await fetcher());
    } catch (err) {
      setError(errorMessage(err, loadErrorMessage));
    } finally {
      setLoading(false);
    }
  }, [fetcher, loadErrorMessage]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, setData, loading, error, setError, reload };
}
