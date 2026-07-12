import type { FormationStatus } from './types';

/** Convertit une date ISO (yyyy-MM-dd) en format lisible jj/mm/aaaa. */
export function formatDate(iso: string | null): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.split('-');
  return `${day}/${month}/${year}`;
}

/** Libellé d'affichage d'un statut de formation. */
export function formationStatusLabel(status: FormationStatus): string {
  return status === 'EN_COURS' ? 'En cours' : 'Terminé';
}
