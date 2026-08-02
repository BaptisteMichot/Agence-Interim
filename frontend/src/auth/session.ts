import type { AuthUser } from './types';

/**
 * Session conservée par le navigateur.
 *
 * Le stockage est {@code sessionStorage} et non {@code localStorage} : il est propre à
 * chaque onglet. Avec localStorage, une connexion dans un onglet écrasait le token de
 * tous les autres, qui continuaient d'afficher leur rôle tout en envoyant le token du
 * dernier connecté (403 sur les routes du rôle initial).
 */

const TOKEN_KEY = 'auth.token';
const USER_KEY = 'auth.user';

/** Émis quand le serveur refuse le token courant (401/403) : la session vient d'être purgée. */
export const SESSION_EXPIRED_EVENT = 'auth:session-expired';

// Sessions écrites par les versions précédentes (localStorage, partagé entre onglets).
localStorage.removeItem(TOKEN_KEY);
localStorage.removeItem(USER_KEY);

export function readToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function readUser(): AuthUser | null {
  const raw = sessionStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function saveSession(token: string, user: AuthUser): void {
  sessionStorage.setItem(TOKEN_KEY, token);
  sessionStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

/**
 * Purge la session et prévient l'application, qui repasse à l'écran de connexion.
 * Sans effet s'il n'y avait pas de session (évite un événement inutile).
 */
export function expireSession(): void {
  if (readToken() === null) {
    return;
  }
  clearSession();
  window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}
