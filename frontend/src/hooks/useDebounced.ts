import { useEffect, useState } from 'react';

/**
 * Valeur retardée : elle ne rejoint `value` qu'après `delay` millisecondes sans
 * nouvelle modification.
 *
 * Sert aux critères de recherche : sans elle, chaque frappe dans le champ mot-clé
 * changerait l'identité du fetcher et lancerait une requête.
 */
export function useDebounced<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
