import type { AuthResponse, EmployerRegisterPayload, RegisterPayload } from '../auth/types';
import { API_BASE, readError } from './http';

/**
 * POST public (endpoints d'auth, sans token). Ne passe pas par request() de http.ts :
 * un 401 pour mauvais identifiants ne doit pas déclencher expireSession().
 */
async function postPublic<T>(path: string, payload: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
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

/** Inscription employeur : crée le compte + une demande d'accès. Ne connecte pas (pas de token). */
export function registerEmployer(payload: EmployerRegisterPayload): Promise<{ message: string }> {
  return postPublic('/auth/register-employer', payload);
}
