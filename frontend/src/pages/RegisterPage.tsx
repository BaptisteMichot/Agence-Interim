import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { homePathForRole } from '../auth/roleRoutes';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await register({ firstName, lastName, email, password });
      navigate(homePathForRole(user.role), { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    } finally {
      setSubmitting(false);
    }
  };

  const inputClass =
    'mb-4 w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500';
  const labelClass = 'mb-1 block text-sm font-medium text-slate-700';

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-50 px-4 py-8">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm"
      >
        <h1 className="mb-6 text-2xl font-semibold text-slate-900">Créer un compte</h1>

        {error && (
          <p className="mb-4 whitespace-pre-line rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        <label className={labelClass} htmlFor="firstName">
          Prénom
        </label>
        <input
          id="firstName"
          required
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          className={inputClass}
        />

        <label className={labelClass} htmlFor="lastName">
          Nom
        </label>
        <input
          id="lastName"
          required
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          className={inputClass}
        />

        <label className={labelClass} htmlFor="email">
          Email
        </label>
        <input
          id="email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={inputClass}
        />

        <label className={labelClass} htmlFor="password">
          Mot de passe
        </label>
        <input
          id="password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mb-2 w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
        />
        <p className="mb-6 text-xs text-slate-500">
          Au moins 14 caractères, avec une majuscule, une minuscule, un chiffre et un caractère spécial.
        </p>

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
        >
          {submitting ? 'Création…' : 'Créer mon compte'}
        </button>

        <p className="mt-4 text-center text-sm text-slate-600">
          Déjà inscrit ?{' '}
          <Link to="/login" className="font-medium text-indigo-600 hover:underline">
            Se connecter
          </Link>
        </p>
      </form>
    </div>
  );
}
