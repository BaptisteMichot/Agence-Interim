import { expireSession } from '../auth/session';

export const API_BASE = '/api';

/** En-tête par lequel la page renvoie le jeton CSRF que le serveur lui a déposé. */
const CSRF_HEADER = 'X-XSRF-TOKEN';

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

/** Message d'une erreur attrapée dans un `catch`, ou le repli fourni. */
export function errorMessage(err: unknown, fallback: string): string {
  return err instanceof Error ? err.message : fallback;
}

/**
 * Jeton CSRF déposé par le serveur dans le cookie `XSRF-TOKEN`, volontairement lisible
 * par la page.
 *
 * Le cookie de session part tout seul, y compris sur une requête déclenchée par un
 * autre site : c'est le principe même du CSRF. Mais la politique de même origine
 * empêche ce site tiers de **lire** un cookie de notre domaine, donc de recopier ce
 * jeton en en-tête. Le serveur compare les deux et rejette ce qui ne concorde pas.
 */
function csrfToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/** Les méthodes de lecture ne modifient rien : le serveur ne leur réclame pas de jeton. */
function csrfHeaders(method: string): Record<string, string> {
  if (method === 'GET' || method === 'HEAD') {
    return {};
  }
  const token = csrfToken();
  return token ? { [CSRF_HEADER]: token } : {};
}

/**
 * Construit l'erreur d'une réponse en échec. Un 401 (cookie absent, invalide ou expiré)
 * ou un 403 (session d'un autre rôle) signifie que la session ne vaut plus rien : on
 * prévient l'application pour qu'elle renvoie vers la connexion, au lieu d'afficher une
 * erreur brute.
 */
async function toError(response: Response): Promise<Error> {
  if (response.status === 401 || response.status === 403) {
    expireSession();
    return new Error('Votre session n’est plus valide. Veuillez vous reconnecter.');
  }
  return new Error(await readError(response));
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    // Le cookie de session accompagne la requête ; il n'y a plus d'en-tête à poser.
    credentials: 'same-origin',
    headers: {
      ...csrfHeaders(method),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw await toError(response);
  }

  // Corps vide (204 No Content, 201 sans corps…) : rien à parser.
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>('GET', path);
}

/**
 * POST sur une route publique, jeton CSRF compris.
 *
 * Deux différences avec `apiPost`. D'abord, un 401 ou un 403 n'y signifie pas que la
 * session a expiré — il n'y a pas de session — donc `expireSession()` n'est pas
 * déclenché : sans cela, saisir un mauvais code de réinitialisation renverrait le
 * visiteur vers un écran de connexion en lui annonçant que sa session a expiré.
 *
 * Ensuite, le jeton CSRF est bien envoyé, contrairement à `postPublic` de `client.ts`
 * qui sert la connexion et l'inscription : ces trois routes-là sont explicitement
 * exemptées côté serveur, la réinitialisation de mot de passe ne l'est pas. Le cookie
 * XSRF-TOKEN est déposé dès le premier appel de la page, fût-il un 401 sur
 * `/auth/me` — il est donc toujours disponible ici.
 */
export async function apiPostPublic<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { ...csrfHeaders('POST'), 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
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
    credentials: 'same-origin',
    headers: csrfHeaders('POST'),
    body: formData,
  });
  if (!response.ok) {
    throw await toError(response);
  }
  return response.json() as Promise<T>;
}

/** Téléchargement d'un binaire authentifié (retourne un Blob). */
export async function apiDownload(path: string): Promise<Blob> {
  const response = await fetch(`${API_BASE}${path}`, { credentials: 'same-origin' });
  if (!response.ok) {
    throw await toError(response);
  }
  return response.blob();
}
