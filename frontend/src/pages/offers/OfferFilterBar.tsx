import Icon from '../../components/Icon';
import { btnSecondary, card, checkboxInput, checkboxRow, inputClass, labelClass } from '../../components/ui';
import { hasActiveFilters, NO_FILTERS, PROVINCES, SECTORS } from '../../offers/format';
import type { OfferFilters } from '../../offers/types';

/**
 * Critères de recherche de la page Offres. Un champ laissé vide ne filtre rien :
 * l'utilisateur n'a jamais à choisir « tous » pour retrouver la liste complète.
 */
export default function OfferFilterBar({
  filters,
  onChange,
}: {
  filters: OfferFilters;
  onChange: (filters: OfferFilters) => void;
}) {
  const set = <K extends keyof OfferFilters>(key: K, value: OfferFilters[K]) =>
    onChange({ ...filters, [key]: value });

  return (
    <div className={`${card} space-y-4`}>
      <div>
        <label className={labelClass} htmlFor="offer-keyword">
          Mot-clé
        </label>
        <div className="relative">
          <Icon
            name="search"
            className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
          />
          <input
            id="offer-keyword"
            type="search"
            value={filters.keyword}
            onChange={(event) => set('keyword', event.target.value)}
            placeholder="Intitulé ou description du poste"
            className={`${inputClass} pl-9`}
          />
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <label className={labelClass} htmlFor="offer-filter-sector">
            Secteur
          </label>
          <select
            id="offer-filter-sector"
            value={filters.sector}
            onChange={(event) => set('sector', event.target.value as OfferFilters['sector'])}
            className={inputClass}
          >
            <option value="">Tous les secteurs</option>
            {SECTORS.map(({ value, label }) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className={labelClass} htmlFor="offer-filter-province">
            Province
          </label>
          <select
            id="offer-filter-province"
            value={filters.province}
            onChange={(event) => set('province', event.target.value as OfferFilters['province'])}
            className={inputClass}
          >
            <option value="">Toutes les provinces</option>
            {PROVINCES.map(({ value, label }) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className={labelClass} htmlFor="offer-filter-wage">
            Salaire minimum (€/h)
          </label>
          <input
            id="offer-filter-wage"
            type="number"
            min="0"
            step="0.5"
            value={filters.minHourlyWage}
            onChange={(event) => set('minHourlyWage', event.target.value)}
            className={inputClass}
          />
          <p className="mt-1 text-xs text-muted">Les offres sans fourchette annoncée sont écartées.</p>
        </div>

        <div>
          <label className={labelClass} htmlFor="offer-filter-experience">
            Expérience demandée (années max.)
          </label>
          <input
            id="offer-filter-experience"
            type="number"
            min="0"
            max="99"
            step="1"
            value={filters.maxExperienceYears}
            onChange={(event) => set('maxExperienceYears', event.target.value)}
            className={inputClass}
          />
          <p className="mt-1 text-xs text-muted">Les offres sans exigence conviennent toujours.</p>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <label className={`${checkboxRow} sm:w-auto`}>
          <input
            type="checkbox"
            className={checkboxInput}
            checked={filters.noVehicleRequired}
            onChange={(event) => set('noVehicleRequired', event.target.checked)}
          />
          Sans véhicule obligatoire
        </label>
        <button
          type="button"
          onClick={() => onChange(NO_FILTERS)}
          disabled={!hasActiveFilters(filters)}
          className={`${btnSecondary} disabled:opacity-50`}
        >
          Réinitialiser
        </button>
      </div>
    </div>
  );
}
