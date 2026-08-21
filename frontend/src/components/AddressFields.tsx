import { type AddressParts } from './address';
import { inputClass, labelClass } from './ui';

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
