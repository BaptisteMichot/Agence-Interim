import type { AuthResponse, RegisterPayload } from '../auth/types';
import { readError } from './http';

const API_BASE = '/api';

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
