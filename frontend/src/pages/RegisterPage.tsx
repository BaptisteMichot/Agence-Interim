import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import { homePathForRole } from '../auth/roleRoutes';
import PasswordInput from '../components/PasswordInput';
import { btnAuthSubmit, errorBox, inputClass, labelClass } from '../components/ui';

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
      setError(errorMessage(err, 'Une erreur est survenue.'));
    } finally {
      setSubmitting(false);
    }
  };

  const inputClassLocal = `mb-4 ${inputClass}`;

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-50 px-4 py-8">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm"
      >
        <h1 className="mb-6 text-2xl font-semibold text-slate-900">Créer un compte</h1>

        {error && (
          <p className={`mb-4 ${errorBox}`}>
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
          className={inputClassLocal}
        />

        <label className={labelClass} htmlFor="lastName">
          Nom
        </label>
        <input
          id="lastName"
          required
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          className={inputClassLocal}
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
          className={inputClassLocal}
        />

        <label className={labelClass} htmlFor="password">
          Mot de passe
        </label>
        <div className="mb-2">
          <PasswordInput
            id="password"
            value={password}
            onChange={setPassword}
            required
            autoComplete="new-password"
          />
        </div>
        <p className="mb-6 text-xs text-slate-500">
          Au moins 14 caractères, avec une majuscule, une minuscule, un chiffre et un caractère spécial.
        </p>

        <button
          type="submit"
          disabled={submitting}
          className={btnAuthSubmit}
        >
          {submitting ? 'Création…' : 'Créer mon compte'}
        </button>

        <p className="mt-4 text-center text-sm text-slate-600">
          Déjà inscrit ?{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Se connecter
          </Link>
        </p>
        <p className="mt-2 text-center text-sm text-slate-600">
          Vous êtes un employeur ?{' '}
          <Link to="/inscription-employeur" className="font-medium text-brand-600 hover:underline">
            Demander un accès
          </Link>
        </p>
        <p className="mt-6 text-center text-xs text-slate-500">
          <Link to="/vie-privee" className="hover:underline">
            Vie privée et cookies
          </Link>
        </p>
      </form>
    </div>
  );
}
