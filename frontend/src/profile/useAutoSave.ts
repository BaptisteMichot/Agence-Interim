import { useCallback, useEffect, useRef, useState } from 'react';

interface AutoSaveOptions {
  /** Délai d'inactivité avant enregistrement, en millisecondes. */
  delay?: number;
  /** Tant que la saisie est invalide, rien n'est envoyé au serveur. */
  valid?: boolean;
}

/**
 * Enregistre automatiquement une valeur de formulaire après un temps d'inactivité.
 * Rien n'est envoyé si la valeur n'a pas changé depuis le dernier enregistrement
 * réussi ou si la saisie est invalide. `flush` force l'enregistrement immédiat
 * (sortie de champ) et `saveNow` permet d'enregistrer une valeur précise sans
 * attendre le prochain rendu (cases à cocher, listes déroulantes).
 */
export function useAutoSave<T>(
  value: T,
  save: (value: T) => Promise<unknown>,
  { delay = 800, valid = true }: AutoSaveOptions = {},
) {
  const [error, setError] = useState<string | null>(null);

  const serialized = JSON.stringify(value);
  const latest = useRef(value);
  const savedSerialized = useRef(serialized);
  const saveRef = useRef(save);
  const validRef = useRef(valid);
  latest.current = value;
  saveRef.current = save;
  validRef.current = valid;

  const commit = useCallback(async (next: T) => {
    const nextSerialized = JSON.stringify(next);
    if (!validRef.current || nextSerialized === savedSerialized.current) {
      return;
    }
    setError(null);
    try {
      await saveRef.current(next);
      savedSerialized.current = nextSerialized;
    } catch (err) {
      setError(err instanceof Error ? err.message : "L'enregistrement a échoué.");
    }
  }, []);

  useEffect(() => {
    if (serialized === savedSerialized.current) {
      return;
    }
    const timer = window.setTimeout(() => commit(latest.current), delay);
    return () => window.clearTimeout(timer);
  }, [serialized, delay, commit]);

  const flush = useCallback(() => commit(latest.current), [commit]);

  return { error, flush, saveNow: commit };
}
