import type { Role } from './types';

/** Chemin du tableau de bord correspondant à chaque rôle. */
export const HOME_PATH_BY_ROLE: Record<Role, string> = {
  JOBSEEKER: '/interimaire',
  EMPLOYER: '/employeur',
  ADMIN: '/admin',
  INTERIM_RECRUITER: '/agence',
};

/** Libellé lisible de chaque rôle (pour l'affichage). */
export const ROLE_LABEL: Record<Role, string> = {
  JOBSEEKER: 'Intérimaire',
  EMPLOYER: 'Employeur',
  ADMIN: 'Administrateur',
  INTERIM_RECRUITER: "Agence d'intérim",
};

export function homePathForRole(role: Role): string {
  return HOME_PATH_BY_ROLE[role];
}
