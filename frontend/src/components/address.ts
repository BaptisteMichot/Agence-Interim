/**
 * Manipulation des adresses saisies champ par champ.
 *
 * <p>Séparé de `AddressFields.tsx` : un fichier qui exporte un composant ne doit
 * exporter que cela, sinon le rafraîchissement à chaud de Vite recharge la page
 * entière au lieu du seul composant.
 */

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
