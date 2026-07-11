// Types partagés avec le backend (voir be.agence_interim.model.Role et dto.AuthResponse).

export type Role = 'ADMIN' | 'INTERIM_RECRUITER' | 'EMPLOYER' | 'JOBSEEKER';

/** Réponse renvoyée par /api/auth/register et /api/auth/login. */
export interface AuthResponse {
  userId: number;
  lastName: string;
  firstName: string;
  email: string;
  role: Role;
  token: string;
  message: string;
}

/** Utilisateur authentifié conservé côté frontend (sans le token). */
export interface AuthUser {
  userId: number;
  lastName: string;
  firstName: string;
  email: string;
  role: Role;
}

/** Corps attendu par /api/auth/register. */
export interface RegisterPayload {
  lastName: string;
  firstName: string;
  email: string;
  password: string;
}
