import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import { homePathForRole } from '../auth/roleRoutes';
import PasswordInput from '../components/PasswordInput';
import { btnAuthSubmit, errorBox } from '../components/ui';

export default function LoginPage() {
  const { login, sessionExpired } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await login(email, password);
      navigate(homePathForRole(user.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-50 px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm"
      >
        <h1 className="mb-6 text-2xl font-semibold text-slate-900">Connexion</h1>

        {sessionExpired && error === null && (
          <p className="mb-4 rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
            Votre session a expiré. Veuillez vous reconnecter.
          </p>
        )}

        {error && (
          <p className={`mb-4 ${errorBox}`}>
            {error}
          </p>
        )}

        <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="email">
          Email
        </label>
        <input
          id="email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="mb-4 w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500"
        />

        <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="password">
          Mot de passe
        </label>
        <div className="mb-6">
          <PasswordInput
            id="password"
            value={password}
            onChange={setPassword}
            required
            autoComplete="current-password"
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className={btnAuthSubmit}
        >
          {submitting ? 'Connexion…' : 'Se connecter'}
        </button>

        <p className="mt-4 text-center text-sm text-slate-600">
          Pas encore de compte ?{' '}
          <Link to="/register" className="font-medium text-brand-600 hover:underline">
            Créer un compte
          </Link>
        </p>
        <p className="mt-2 text-center text-sm text-slate-600">
          Vous êtes un employeur ?{' '}
          <Link to="/inscription-employeur" className="font-medium text-brand-600 hover:underline">
            Demander un accès
          </Link>
        </p>
      </form>
    </div>
  );
}
