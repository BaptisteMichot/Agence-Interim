/** Suffixe « · min – max €/h » d'une carte d'offre, vide si aucune fourchette annoncée. */
export function salarySuffix(salaryMin: number | null, salaryMax: number | null): string {
  return salaryMin !== null || salaryMax !== null
    ? ` · ${salaryMin ?? '?'} – ${salaryMax ?? '?'} €/h`
    : '';
}
