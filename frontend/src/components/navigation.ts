import type { Role } from '../auth/types';
import type { IconName } from './Icon';

/** Compteurs affichés en pastille dans la barre latérale. */
export type NavCounter =
  | 'missionsToConfirm'
  | 'unreadMessages'
  | 'missionsToValidate'
  | 'contractsToSign';

export interface NavItem {
  to: string;
  label: string;
  icon: IconName;
  /** Vrai pour l'accueil : sinon toutes ses sous-pages le marqueraient actif. */
  end?: boolean;
  counter?: NavCounter;
}

/**
 * Sections accessibles à chaque rôle, dans l'ordre de la barre latérale. C'est la seule
 * description de la navigation : les pages n'ont plus à proposer de retour au tableau
 * de bord, il reste visible en permanence.
 */
export const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  JOBSEEKER: [
    { to: '/interimaire', label: 'Accueil', icon: 'home', end: true },
    { to: '/interimaire/offres', label: "Offres d'emploi", icon: 'briefcase' },
    { to: '/interimaire/candidatures', label: 'Mes candidatures', icon: 'document' },
    {
      to: '/interimaire/missions',
      label: 'Mes missions',
      icon: 'check',
      counter: 'missionsToConfirm',
    },
    { to: '/interimaire/planning', label: 'Mon planning', icon: 'calendar' },
    { to: '/documents', label: 'Mes documents', icon: 'document', counter: 'contractsToSign' },
    { to: '/messages', label: 'Messages', icon: 'chat', counter: 'unreadMessages' },
    { to: '/interimaire/profil', label: 'Mon profil', icon: 'user' },
  ],
  EMPLOYER: [
    { to: '/employeur', label: 'Accueil', icon: 'home', end: true },
    { to: '/employeur/offres', label: "Mes offres d'emploi", icon: 'briefcase' },
    { to: '/employeur/missions', label: 'Mes missions', icon: 'check' },
    { to: '/documents', label: 'Mes documents', icon: 'document', counter: 'contractsToSign' },
    { to: '/messages', label: 'Messages', icon: 'chat', counter: 'unreadMessages' },
    { to: '/employeur/entreprise', label: 'Mon entreprise', icon: 'building' },
  ],
  ADMIN: [
    { to: '/admin', label: "Demandes d'accès", icon: 'home', end: true },
    {
      to: '/admin/missions',
      label: 'Missions à valider',
      icon: 'check',
      counter: 'missionsToValidate',
    },
  ],
  // Un employeur en attente reste sur sa page de statut, hors de la coquille.
  EMPLOYER_PENDING: [],
};
