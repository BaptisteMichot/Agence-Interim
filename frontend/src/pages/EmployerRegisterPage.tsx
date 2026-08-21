import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { registerEmployer } from '../api/client';
import { errorMessage } from '../api/http';
import AddressFields from '../components/AddressFields';
import { EMPTY_ADDRESS, formatAddress, type AddressParts } from '../components/address';
import PasswordInput from '../components/PasswordInput';
import { btnAuthSubmit, errorBox, inputClass, labelClass } from '../components/ui';

export default function EmployerRegisterPage() {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [address, setAddress] = useState<AddressParts>({ ...EMPTY_ADDRESS });
  const [companyNumber, setCompanyNumber] = useState('');
  const [jointCommittee, setJointCommittee] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await registerEmployer({
        firstName,
        lastName,
        email,
        password,
        companyName,
        address: formatAddress(address),
        companyNumber,
        jointCommittee,
      });
      setSubmitted(true);
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    } finally {
      setSubmitting(false);
    }
  };

  const inputClassLocal = `mb-4 ${inputClass}`;

  if (submitted) {
    return (
      <div className="flex min-h-full items-center justify-center bg-slate-50 px-4 py-8">
        <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-600">
            ✓
          </div>
          <h1 className="text-xl font-semibold text-slate-900">Demande envoyée</h1>
          <p className="mt-2 text-sm text-slate-600">
            Votre demande d'accès employeur a bien été envoyée. Elle sera validée par l'agence. Vous
            pourrez vous connecter à votre espace une fois la demande acceptée.
          </p>
          <Link
            to="/login"
            className="mt-6 inline-block rounded-md bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700"
          >
            Retour à la connexion
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-50 px-4 py-8">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm"
      >
        <h1 className="mb-1 text-2xl font-semibold text-slate-900">Demande d'accès employeur</h1>
        <p className="mb-6 text-sm text-slate-500">
          Créez votre compte employeur. L'accès sera validé par l'agence.
        </p>

        {error && (
          <p className={`mb-4 ${errorBox}`}>
            {error}
          </p>
        )}

        <label className={labelClass} htmlFor="companyName">
          Entreprise
        </label>
        <input
          id="companyName"
          required
          value={companyName}
          onChange={(e) => setCompanyName(e.target.value)}
          className={inputClassLocal}
        />

        <span className={labelClass}>Adresse du siège</span>
        <div className="mb-4">
          <AddressFields
            idPrefix="company-address"
            parts={address}
            onChange={setAddress}
            required
          />
        </div>

        <label className={labelClass} htmlFor="companyNumber">
          Numéro d'entreprise (BCE)
        </label>
        <input
          id="companyNumber"
          required
          maxLength={15}
          placeholder="0000.000.000"
          value={companyNumber}
          onChange={(e) => setCompanyNumber(e.target.value)}
          className={inputClassLocal}
        />

        <label className={labelClass} htmlFor="jointCommittee">
          Commission paritaire
        </label>
        <input
          id="jointCommittee"
          required
          maxLength={10}
          placeholder="Par exemple 111 ou 200"
          value={jointCommittee}
          onChange={(e) => setJointCommittee(e.target.value)}
          className={inputClassLocal}
        />
        <p className="mb-4 text-xs text-slate-500">
          Ces mentions sont obligatoires sur les contrats de mission.
        </p>

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
          {submitting ? 'Envoi…' : 'Envoyer la demande'}
        </button>

        <p className="mt-4 text-center text-sm text-slate-600">
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Retour à la connexion
          </Link>
        </p>
      </form>
    </div>
  );
}
