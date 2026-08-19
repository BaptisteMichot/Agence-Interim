import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../api/http';

/**
 * Chargement d'une section du profil : les éléments de l'utilisateur et les options
 * du référentiel, chargés ensemble au montage puis rechargés après chaque mutation.
 * `loadItems`/`loadOptions` doivent être des fonctions stables (fonctions de module).
 */
export function useProfileCollection<T, O>(
  loadItems: () => Promise<T[]>,
  loadOptions: () => Promise<O[]>,
  loadErrorMessage: string,
) {
  const [items, setItems] = useState<T[]>([]);
  const [options, setOptions] = useState<O[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      const [loadedItems, loadedOptions] = await Promise.all([loadItems(), loadOptions()]);
      setItems(loadedItems);
      setOptions(loadedOptions);
    } catch (err) {
      setError(errorMessage(err, loadErrorMessage));
    } finally {
      setLoading(false);
    }
  }, [loadItems, loadOptions, loadErrorMessage]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { items, setItems, options, loading, error, setError, reload };
}
