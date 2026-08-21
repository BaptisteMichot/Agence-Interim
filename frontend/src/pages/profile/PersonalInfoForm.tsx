import { useCallback, useState } from 'react';
import { updateProfile } from '../../api/profile';
import AddressFields from '../../components/AddressFields';
import { formatAddress, parseAddress, type AddressParts } from '../../components/address';
import { checkboxInput, checkboxRow, errorBox, inputClass, labelClass } from '../../components/ui';
import type { Profile, ProfileBasePayload } from '../../profile/types';
import { useAutoSave } from '../../profile/useAutoSave';

/**
 * Regroupe l'IBAN par quatre caractères pendant la frappe, comme sur un relevé bancaire.
 * Le backend applique la même mise en forme : autant l'afficher tout de suite.
 */
function groupIban(value: string): string {
  const compact = value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
  return compact.replace(/(.{4})(?=.)/g, '$1 ');
}

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
  const [address, setAddress] = useState<AddressParts>(() => parseAddress(profile.address));
  const [nationalNumber, setNationalNumber] = useState(profile.nationalNumber ?? '');
  const [iban, setIban] = useState(groupIban(profile.iban ?? ''));

  const values: ProfileBasePayload = {
    firstName,
    lastName,
    birthdate: birthdate || null,
    hasVehicle,
    address: formatAddress(address) || null,
    nationalNumber: nationalNumber.trim() || null,
    iban: iban.trim() || null,
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
    <section className="rounded-xl border border-line bg-surface p-6">
      <h2 className="mb-4 text-lg font-semibold text-ink">Informations personnelles</h2>

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

        <div className="sm:col-span-2">
          <span className={`${labelClass} mb-2`}>Adresse</span>
          <AddressFields idPrefix="profile-address" parts={address} onChange={setAddress} onBlur={flush} />
        </div>

        <div>
          <label className={labelClass} htmlFor="nationalNumber">
            Numéro de registre national
          </label>
          <input
            id="nationalNumber"
            value={nationalNumber}
            maxLength={15}
            placeholder="00.00.00-000.00"
            onChange={(e) => setNationalNumber(e.target.value)}
            onBlur={flush}
            className={inputClass}
          />
        </div>
        <div>
          <label className={labelClass} htmlFor="iban">
            Numéro de compte bancaire
          </label>
          <input
            id="iban"
            value={iban}
            maxLength={42}
            placeholder="BE68 5390 0754 7034"
            onChange={(e) => setIban(groupIban(e.target.value))}
            onBlur={flush}
            className={inputClass}
          />
        </div>
        <p className="text-xs text-muted sm:col-span-2">
          Votre adresse et votre numéro de registre national figurent sur le contrat, votre numéro
          de compte sert au paiement de votre salaire : sans ces informations, vous ne pourrez pas
          accepter de mission.
        </p>
      </div>
    </section>
  );
}
