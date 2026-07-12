const API_BASE = '/api';
const TOKEN_KEY = 'auth.token';

/**
 * Extrait un message d'erreur lisible du corps d'une réponse en échec.
 * Le backend renvoie soit du texte brut (400/401/404), soit un tableau JSON de
 * messages de validation.
 */
export async function readError(response: Response): Promise<string> {
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

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem(TOKEN_KEY);
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      ...authHeaders(),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  // 204 No Content (ex. suppression) : rien à parser.
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>('GET', path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>('POST', path, body);
}

export function apiPut<T>(path: string, body: unknown): Promise<T> {
  return request<T>('PUT', path, body);
}

export function apiDelete(path: string): Promise<void> {
  return request<void>('DELETE', path);
}

/** Envoi d'un fichier (multipart). Ne fixe pas Content-Type : le navigateur gère la frontière. */
export async function apiUpload<T>(path: string, formData: FormData): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: authHeaders(),
    body: formData,
  });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<T>;
}

/** Téléchargement d'un binaire authentifié (retourne un Blob). */
export async function apiDownload(path: string): Promise<Blob> {
  const response = await fetch(`${API_BASE}${path}`, { headers: authHeaders() });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.blob();
}
