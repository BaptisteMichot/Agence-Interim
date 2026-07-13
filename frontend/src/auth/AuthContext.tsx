import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import * as authApi from '../api/client';
import type { AuthResponse, AuthUser, RegisterPayload } from './types';

const TOKEN_KEY = 'auth.token';
const USER_KEY = 'auth.user';

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<AuthUser>;
  register: (payload: RegisterPayload) => Promise<AuthUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

function toUser(response: AuthResponse): AuthUser {
  return {
    userId: response.userId,
    lastName: response.lastName,
    firstName: response.firstName,
    email: response.email,
    role: response.role,
    employerRequestStatus: response.employerRequestStatus,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<AuthUser | null>(loadUser);

  const persist = useCallback((response: AuthResponse): AuthUser => {
    const nextUser = toUser(response);
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setToken(response.token);
    setUser(nextUser);
    return nextUser;
  }, []);

  const login = useCallback(
    async (email: string, password: string) => persist(await authApi.login(email, password)),
    [persist],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => persist(await authApi.register(payload)),
    [persist],
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, token, isAuthenticated: token !== null, login, register, logout }),
    [user, token, login, register, logout],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth doit être utilisé à l'intérieur d'un AuthProvider.");
  }
  return context;
}
