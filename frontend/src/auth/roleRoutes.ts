import type { AuthUser, Role } from './types';

/** Page de statut affichée à un employeur dont la demande est en attente ou refusée. */
export const EMPLOYER_STATUS_PATH = '/statut-employeur';

/** Chemin du tableau de bord correspondant à chaque rôle. */
export const HOME_PATH_BY_ROLE: Record<Role, string> = {
  JOBSEEKER: '/interimaire',
  EMPLOYER: '/employeur',
  EMPLOYER_PENDING: EMPLOYER_STATUS_PATH,
  ADMIN: '/admin',
  INTERIM_RECRUITER: '/agence',
};

/** Libellé lisible de chaque rôle (pour l'affichage). */
export const ROLE_LABEL: Record<Role, string> = {
  JOBSEEKER: 'Intérimaire',
  EMPLOYER: 'Employeur',
  EMPLOYER_PENDING: 'Employeur (en attente)',
  ADMIN: 'Administrateur',
  INTERIM_RECRUITER: "Agence d'intérim",
};

export function homePathForRole(role: Role): string {
  return HOME_PATH_BY_ROLE[role];
}

/**
 * Destination après connexion selon le rôle. Un employeur en attente (EMPLOYER_PENDING)
 * est dirigé vers sa page de statut.
 */
export function homePathForUser(user: AuthUser): string {
  return HOME_PATH_BY_ROLE[user.role];
}
