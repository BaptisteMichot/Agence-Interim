import { API_BASE, apiDelete, apiPostPublic, apiPut, readError } from './http';

/** Réponse des routes qui ne renvoient qu'un message. */
export interface MessageResponse {
  message: string;
}

/**
 * Change le mot de passe du compte connecté.
 *
 * Le serveur révoque toutes les sessions et renvoie un cookie neuf : l'onglet courant
 * reste connecté, les autres appareils sont déconnectés. C'est l'effet recherché — on
 * change son mot de passe précisément quand on soupçonne qu'un autre appareil est resté
 * ouvert quelque part.
 */
export function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<MessageResponse> {
  return apiPut<MessageResponse>('/account/password', { currentPassword, newPassword });
}

/** Clôture le compte : suppression si rien n'a été engagé, anonymisation sinon. */
export function closeAccount(): Promise<void> {
  return apiDelete('/account');
}

/**
 * Export des données personnelles (RGPD, droit d'accès).
 *
 * Passe par `fetch` plutôt que par `apiGet` : le serveur rend un document texte, pas du
 * JSON, et la réponse est enregistrée telle quelle dans un fichier.
 */
export async function exportMyData(): Promise<Blob> {
  const response = await fetch(`${API_BASE}/account/export`, { credentials: 'same-origin' });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.blob();
}

/**
 * Demande un code de réinitialisation.
 *
 * La réponse est la même que l'adresse existe ou non : le serveur ne dit pas si un
 * compte y correspond, et l'interface ne doit pas laisser croire le contraire.
 */
export function requestPasswordReset(email: string): Promise<MessageResponse> {
  return apiPostPublic<MessageResponse>('/auth/password/forgot', { email });
}

/** Pose le nouveau mot de passe à partir du code reçu par email. */
export function resetPassword(
  email: string,
  code: string,
  newPassword: string,
): Promise<MessageResponse> {
  return apiPostPublic<MessageResponse>('/auth/password/reset', { email, code, newPassword });
}
