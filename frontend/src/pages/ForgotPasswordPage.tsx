import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { requestPasswordReset, resetPassword } from '../api/account';
import { errorMessage } from '../api/http';
import PasswordInput from '../components/PasswordInput';
import { btnAuthSubmit, errorBox, inputClass, labelClass } from '../components/ui';

/**
 * Réinitialisation du mot de passe, en deux temps sur une seule page.
 *
 * L'écran est le même que l'adresse existe ou non : le serveur ne le dit pas, et
 * afficher « aucun compte à cette adresse » reviendrait à publier la liste des comptes
 * de la plateforme, une requête à la fois. La deuxième étape s'ouvre donc dans tous les
 * cas — celui qui n'a pas de compte ne recevra simplement jamais de code.
 */
export default function ForgotPasswordPage() {
  const navigate = useNavigate();

  const [step, setStep] = useState<'request' | 'reset'>('request');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const askForCode = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const response = await requestPasswordReset(email);
      setNotice(response.message);
      setStep('reset');
    } catch (err) {
      setError(errorMessage(err, "Le code n'a pas pu être demandé."));
    } finally {
      setSubmitting(false);
    }
  };

  const submitNewPassword = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await resetPassword(email, code, newPassword);
      // Pas de connexion automatique : on réinitialise souvent son mot de passe parce
      // qu'on craint que le compte soit compromis. Se reconnecter vérifie au passage que
      // le nouveau mot de passe est bien celui qu'on croit avoir choisi.
      navigate('/login', { replace: true });
    } catch (err) {
      setError(errorMessage(err, "Le mot de passe n'a pas pu être modifié."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="mb-1 text-2xl font-semibold text-slate-900">Mot de passe oublié</h1>
        <p className="mb-6 text-sm text-slate-600">
          {step === 'request'
            ? 'Indiquez votre adresse : un code à six chiffres y sera envoyé.'
            : 'Saisissez le code reçu par email, puis votre nouveau mot de passe.'}
        </p>

        {notice !== null && (
          <p className="mb-4 rounded-md bg-brand-50 px-3 py-2 text-sm text-brand-700">{notice}</p>
        )}
        {error !== null && <p className={`mb-4 ${errorBox}`}>{error}</p>}

        {step === 'request' ? (
          <form onSubmit={askForCode}>
            <label className={labelClass} htmlFor="forgot-email">
              Adresse email
            </label>
            <input
              id="forgot-email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className={`mb-6 ${inputClass}`}
            />
            <button type="submit" className={btnAuthSubmit} disabled={submitting}>
              {submitting ? 'Envoi…' : 'Recevoir un code'}
            </button>
          </form>
        ) : (
          <form onSubmit={submitNewPassword}>
            <label className={labelClass} htmlFor="forgot-code">
              Code reçu par email
            </label>
            <input
              id="forgot-code"
              inputMode="numeric"
              required
              autoComplete="one-time-code"
              placeholder="123456"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              className={`mb-4 ${inputClass}`}
            />

            <label className={labelClass} htmlFor="forgot-new-password">
              Nouveau mot de passe
            </label>
            <div className="mb-6">
              <PasswordInput
                id="forgot-new-password"
                value={newPassword}
                onChange={setNewPassword}
                required
                autoComplete="new-password"
              />
            </div>

            <button type="submit" className={btnAuthSubmit} disabled={submitting}>
              {submitting ? 'Enregistrement…' : 'Changer mon mot de passe'}
            </button>
            <button
              type="button"
              className="mt-3 w-full text-center text-sm text-brand-600 hover:underline"
              onClick={() => {
                setStep('request');
                setNotice(null);
                setError(null);
              }}
            >
              Demander un nouveau code
            </button>
          </form>
        )}

        <p className="mt-6 text-center text-sm text-slate-600">
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Retour à la connexion
          </Link>
        </p>
      </div>
    </div>
  );
}
