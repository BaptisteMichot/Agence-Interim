import { useState, type FormEvent } from 'react';
import { updateProfile } from '../../api/profile';
import { btnPrimary, checkboxInput, checkboxRow, errorBox, inputClass, labelClass } from '../../components/ui';
import type { Profile } from '../../profile/types';

interface PersonalInfoFormProps {
  profile: Profile;
  onSaved: (profile: Profile) => void;
}

/** Édition des informations personnelles de base (nom, prénom, naissance, véhicule). */
export default function PersonalInfoForm({ profile, onSaved }: PersonalInfoFormProps) {
  const [firstName, setFirstName] = useState(profile.firstName);
  const [lastName, setLastName] = useState(profile.lastName);
  const [birthdate, setBirthdate] = useState(profile.birthdate ?? '');
  const [hasVehicle, setHasVehicle] = useState(profile.hasVehicle ?? false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSaved(false);
    setSaving(true);
    try {
      const updated = await updateProfile({
        firstName,
        lastName,
        birthdate: birthdate || null,
        hasVehicle,
      });
      onSaved(updated);
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Informations personnelles</h2>

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}

      <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
        <div>
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
        </div>
        <div>
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
        </div>
        <div>
          <label className={labelClass} htmlFor="birthdate">
            Date de naissance
          </label>
          <input
            id="birthdate"
            type="date"
            value={birthdate}
            onChange={(e) => setBirthdate(e.target.value)}
            className={inputClass}
          />
        </div>
        <div className="flex flex-col justify-end">
          <span className={labelClass}>Véhicule</span>
          <label className={checkboxRow}>
            <input
              type="checkbox"
              checked={hasVehicle}
              onChange={(e) => setHasVehicle(e.target.checked)}
              className={checkboxInput}
            />
            Je possède un véhicule
          </label>
        </div>

        <div className="sm:col-span-2 flex items-center gap-3">
          <button type="submit" disabled={saving} className={btnPrimary}>
            {saving ? 'Enregistrement…' : 'Enregistrer'}
          </button>
          {saved && <span className="text-sm text-green-600">Modifications enregistrées.</span>}
        </div>
      </form>
    </section>
  );
}
