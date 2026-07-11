import type { AuthResponse, RegisterPayload } from '../auth/types';

const API_BASE = '/api';

/**
 * Extrait un message d'erreur lisible du corps d'une réponse en échec.
 * Le backend renvoie soit du texte brut (400/401), soit un tableau JSON de
 * messages de validation.
 */
async function readError(response: Response): Promise<string> {
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    const body = await response.json();
    if (Array.isArray(body)) {
      return body.join('\n');
    }
    if (typeof body === 'string') {
      return body;
    }
    return JSON.stringify(body);
  }
  const text = await response.text();
  return text || `Erreur ${response.status}`;
}

async function postAuth(path: string, payload: unknown): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<AuthResponse>;
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return postAuth('/auth/login', { email, password });
}

export function register(payload: RegisterPayload): Promise<AuthResponse> {
  return postAuth('/auth/register', payload);
}
