import { useCallback, useState } from 'react';
import { updateProfile } from '../../api/profile';
import { checkboxInput, checkboxRow, errorBox, inputClass, labelClass } from '../../components/ui';
import type { Profile, ProfileBasePayload } from '../../profile/types';
import { useAutoSave } from '../../profile/useAutoSave';

interface PersonalInfoFormProps {
  profile: Profile;
  onSaved: (profile: Profile) => void;
}

/**
 * Édition des informations personnelles de base (nom, prénom, naissance, véhicule).
 * Comme le reste du profil, la section s'enregistre d'elle-même.
 */
export default function PersonalInfoForm({ profile, onSaved }: PersonalInfoFormProps) {
  const [firstName, setFirstName] = useState(profile.firstName);
  const [lastName, setLastName] = useState(profile.lastName);
  const [birthdate, setBirthdate] = useState(profile.birthdate ?? '');
  const [hasVehicle, setHasVehicle] = useState(profile.hasVehicle ?? false);

  const values: ProfileBasePayload = {
    firstName,
    lastName,
    birthdate: birthdate || null,
    hasVehicle,
  };
  // Le backend refuse un nom ou un prénom vide : inutile de l'appeler dans ce cas.
  const namesFilled = firstName.trim() !== '' && lastName.trim() !== '';

  const persist = useCallback(
    async (next: ProfileBasePayload) => {
      onSaved(await updateProfile(next));
    },
    [onSaved],
  );

  const { error, flush, saveNow } = useAutoSave(values, persist, { valid: namesFilled });

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Informations personnelles</h2>

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}

      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className={labelClass} htmlFor="firstName">
            Prénom
          </label>
          <input
            id="firstName"
            required
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            onBlur={flush}
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
            onBlur={flush}
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
            onBlur={flush}
            className={inputClass}
          />
        </div>
        <div className="flex flex-col justify-end">
          <span className={labelClass}>Véhicule</span>
          <label className={checkboxRow}>
            <input
              type="checkbox"
              checked={hasVehicle}
              onChange={(e) => {
                // Une case à cocher n'a pas de frappe : on enregistre sans attendre.
                setHasVehicle(e.target.checked);
                saveNow({ ...values, hasVehicle: e.target.checked });
              }}
              className={checkboxInput}
            />
            Je possède un véhicule
          </label>
        </div>
      </div>
    </section>
  );
}
