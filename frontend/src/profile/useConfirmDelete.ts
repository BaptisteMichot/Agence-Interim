import { useState } from 'react';
import { errorMessage } from '../api/http';

/**
 * Suppression d'un élément d'une section du profil, confirmée par un ConfirmDialog :
 * porte l'id en attente de confirmation et exécute la suppression confirmée.
 */
export function useConfirmDelete(
  remove: (id: number) => Promise<void>,
  onDeleted: () => void,
  setError: (message: string | null) => void,
) {
  const [confirmId, setConfirmId] = useState<number | null>(null);

  const confirmDelete = async () => {
    if (confirmId === null) {
      return;
    }
    const id = confirmId;
    setConfirmId(null);
    setError(null);
    try {
      await remove(id);
      onDeleted();
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  return { confirmId, setConfirmId, confirmDelete };
}
