import { inputClass, labelClass } from './ui';

/** Adresse saisie champ par champ ; la base ne stocke qu'une chaîne (voir {@link formatAddress}). */
export interface AddressParts {
  street: string;
  number: string;
  postalCode: string;
  city: string;
  country: string;
}

export const EMPTY_ADDRESS: AddressParts = {
  street: '',
  number: '',
  postalCode: '',
  city: '',
  country: 'Belgique',
};

/**
 * Forme canonique « Rue de la Loi 16, 1000 Bruxelles, Belgique ». C'est elle qui part en
 * base et qui figure sur les contrats : une seule colonne, comme le prévoit le modèle.
 */
export function formatAddress(parts: AddressParts): string {
  const street = [parts.street.trim(), parts.number.trim()].filter(Boolean).join(' ');
  const city = [parts.postalCode.trim(), parts.city.trim()].filter(Boolean).join(' ');
  return [street, city, parts.country.trim()].filter(Boolean).join(', ');
}

/**
 * Relit une adresse enregistrée pour réalimenter les champs. La découpe suit la forme
 * produite par {@link formatAddress} ; une adresse saisie autrement retombe dans « Rue »,
 * où elle reste modifiable plutôt que perdue.
 */
export function parseAddress(value: string | null | undefined): AddressParts {
  if (!value || !value.trim()) {
    return { ...EMPTY_ADDRESS };
  }
  const segments = value.split(',').map((segment) => segment.trim());
  const [streetPart = '', cityPart = '', countryPart = ''] = segments;

  // Le numéro est le dernier mot de la rue s'il contient un chiffre (« 16 », « 16A »).
  const streetWords = streetPart.split(/\s+/);
  const last = streetWords[streetWords.length - 1] ?? '';
  const hasNumber = streetWords.length > 1 && /\d/.test(last);

  // Le code postal est le premier mot de la localité s'il n'est fait que de chiffres.
  const cityWords = cityPart.split(/\s+/).filter(Boolean);
  const hasPostalCode = cityWords.length > 0 && /^\d+$/.test(cityWords[0]);

  return {
    street: hasNumber ? streetWords.slice(0, -1).join(' ') : streetPart,
    number: hasNumber ? last : '',
    postalCode: hasPostalCode ? cityWords[0] : '',
    city: (hasPostalCode ? cityWords.slice(1) : cityWords).join(' '),
    country: countryPart || (segments.length > 1 ? '' : 'Belgique'),
  };
}

/** Vrai lorsque tous les champs nécessaires au contrat sont remplis. */
export function isAddressComplete(parts: AddressParts): boolean {
  return Boolean(
    parts.street.trim() && parts.number.trim() && parts.postalCode.trim() && parts.city.trim(),
  );
}

interface AddressFieldsProps {
  /** Préfixe des `id`, pour que deux adresses puissent cohabiter sur une même page. */
  idPrefix: string;
  parts: AddressParts;
  onChange: (parts: AddressParts) => void;
  /** Appelé à la sortie d'un champ (enregistrement automatique du profil). */
  onBlur?: () => void;
  required?: boolean;
  disabled?: boolean;
}

export default function AddressFields({
  idPrefix,
  parts,
  onChange,
  onBlur,
  required = false,
  disabled = false,
}: AddressFieldsProps) {
  const field = (key: keyof AddressParts) => ({
    id: `${idPrefix}-${key}`,
    value: parts[key],
    required,
    disabled,
    onBlur,
    onChange: (event: React.ChangeEvent<HTMLInputElement>) =>
      onChange({ ...parts, [key]: event.target.value }),
    className: inputClass,
  });

  return (
    <div className="grid gap-3 sm:grid-cols-6">
      <div className="sm:col-span-4">
        <label className={labelClass} htmlFor={`${idPrefix}-street`}>
          Rue
        </label>
        <input {...field('street')} maxLength={60} />
      </div>
      <div className="sm:col-span-2">
        <label className={labelClass} htmlFor={`${idPrefix}-number`}>
          Numéro
        </label>
        <input {...field('number')} maxLength={10} />
      </div>
      <div className="sm:col-span-2">
        <label className={labelClass} htmlFor={`${idPrefix}-postalCode`}>
          Code postal
        </label>
        <input {...field('postalCode')} maxLength={10} />
      </div>
      <div className="sm:col-span-2">
        <label className={labelClass} htmlFor={`${idPrefix}-city`}>
          Ville
        </label>
        <input {...field('city')} maxLength={40} />
      </div>
      <div className="sm:col-span-2">
        <label className={labelClass} htmlFor={`${idPrefix}-country`}>
          Pays
        </label>
        <input {...field('country')} maxLength={30} />
      </div>
    </div>
  );
}
