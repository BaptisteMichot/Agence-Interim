import type { OfferFilters, Province, Sector } from './types';

/** Suffixe « · min – max €/h » d'une carte d'offre, vide si aucune fourchette annoncée. */
export function salarySuffix(salaryMin: number | null, salaryMax: number | null): string {
  return salaryMin !== null || salaryMax !== null
    ? ` · ${salaryMin ?? '?'} – ${salaryMax ?? '?'} €/h`
    : '';
}

/** Secteurs d'activité (valeur backend → libellé), par ordre alphabétique, « Autre » en dernier. */
export const SECTORS: { value: Sector; label: string }[] = [
  { value: 'ADMINISTRATION', label: 'Administration' },
  { value: 'AGRICULTURE', label: 'Agriculture' },
  { value: 'COMMERCE', label: 'Commerce' },
  { value: 'CONSTRUCTION', label: 'Construction' },
  { value: 'ENSEIGNEMENT', label: 'Enseignement' },
  { value: 'HORECA', label: 'Horeca' },
  { value: 'INDUSTRIE', label: 'Industrie' },
  { value: 'INFORMATIQUE', label: 'Informatique' },
  { value: 'LOGISTIQUE', label: 'Logistique' },
  { value: 'NETTOYAGE', label: 'Nettoyage' },
  { value: 'SANTE', label: 'Santé' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'AUTRE', label: 'Autre' },
];

/** Provinces (valeur backend → libellé), par ordre alphabétique. */
export const PROVINCES: { value: Province; label: string }[] = [
  { value: 'ANVERS', label: 'Anvers' },
  { value: 'BRABANT_FLAMAND', label: 'Brabant flamand' },
  { value: 'BRABANT_WALLON', label: 'Brabant wallon' },
  { value: 'BRUXELLES', label: 'Bruxelles-Capitale' },
  { value: 'FLANDRE_OCCIDENTALE', label: 'Flandre-Occidentale' },
  { value: 'FLANDRE_ORIENTALE', label: 'Flandre-Orientale' },
  { value: 'HAINAUT', label: 'Hainaut' },
  { value: 'LIEGE', label: 'Liège' },
  { value: 'LIMBOURG', label: 'Limbourg' },
  { value: 'LUXEMBOURG', label: 'Luxembourg' },
  { value: 'NAMUR', label: 'Namur' },
];

export function sectorLabel(sector: Sector): string {
  return SECTORS.find((s) => s.value === sector)?.label ?? sector;
}

export function provinceLabel(province: Province): string {
  return PROVINCES.find((p) => p.value === province)?.label ?? province;
}

/** Aucun critère : l'état initial de la barre de recherche. */
export const NO_FILTERS: OfferFilters = {
  keyword: '',
  sector: '',
  province: '',
  minHourlyWage: '',
  maxExperienceYears: '',
  noVehicleRequired: false,
};

/** Vrai dès qu'un critère est renseigné : sert à distinguer « aucune offre » de « aucun résultat ». */
export function hasActiveFilters(filters: OfferFilters): boolean {
  return (
    filters.keyword !== '' ||
    filters.sector !== '' ||
    filters.province !== '' ||
    filters.minHourlyWage !== '' ||
    filters.maxExperienceYears !== '' ||
    filters.noVehicleRequired
  );
}
