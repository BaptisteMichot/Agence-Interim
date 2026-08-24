import type { AuthResponse, EmployerRegisterPayload, RegisterPayload } from '../auth/types';
import { API_BASE, apiPost, readError } from './http';

/**
 * POST public (endpoints d'auth, sans token). Ne passe pas par request() de http.ts :
 * un 401 pour mauvais identifiants ne doit pas déclencher expireSession().
 */
async function postPublic<T>(path: string, payload: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    // La réponse dépose le cookie de session : il faut que le navigateur l'accepte.
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<T>;
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return postPublic('/auth/login', { email, password });
}

export function register(payload: RegisterPayload): Promise<AuthResponse> {
  return postPublic('/auth/register', payload);
}

/** Inscription employeur : crée le compte + une demande d'accès. Ne connecte pas. */
export function registerEmployer(payload: EmployerRegisterPayload): Promise<{ message: string }> {
  return postPublic('/auth/register-employer', payload);
}

/**
 * Identité de la session en cours, ou `null` s'il n'y en a pas.
 *
 * Le cookie étant HttpOnly, la page ne peut pas savoir seule qui elle représente : elle
 * le demande au serveur à chaque chargement. Un 401 n'est pas une erreur ici, c'est la
 * réponse normale d'un visiteur non connecté — d'où l'appel direct plutôt que par
 * `apiGet`, qui déclencherait une expiration de session.
 */
export async function me(): Promise<AuthResponse | null> {
  const response = await fetch(`${API_BASE}/auth/me`, { credentials: 'same-origin' });
  if (response.status === 401) {
    return null;
  }
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<AuthResponse>;
}

/** Efface le cookie de session côté serveur : la page ne peut pas le faire elle-même. */
export function logout(): Promise<{ message: string }> {
  return apiPost('/auth/logout', {});
}
