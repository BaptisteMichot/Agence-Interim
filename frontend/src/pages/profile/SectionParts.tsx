import type { FormEvent, ReactNode } from 'react';
import {
  btnPrimary,
  btnSecondary,
  checkboxInput,
  checkboxRow,
  errorBox,
  inputClass,
  labelClass,
} from '../../components/ui';

// Briques partagées par les sections du profil (expériences, formations,
// compétences, diplômes, langues) : même structure, même rendu.

/** En-tête d'une section : titre et bouton « + Ajouter » quand le formulaire est fermé. */
export function SectionHeader({
  title,
  showAdd,
  onAdd,
}: {
  title: string;
  showAdd: boolean;
  onAdd: () => void;
}) {
  return (
    <div className="mb-4 flex items-center justify-between">
      <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
      {showAdd && (
        <button type="button" className={btnSecondary} onClick={onAdd}>
          + Ajouter
        </button>
      )}
    </div>
  );
}

/** Enveloppe des formulaires d'ajout/édition : encart indigo en grille deux colonnes. */
export function SectionForm({
  onSubmit,
  error,
  children,
}: {
  onSubmit: (event: FormEvent) => void;
  error: string | null;
  children: ReactNode;
}) {
  return (
    <form onSubmit={onSubmit} className="mt-4 grid gap-4 rounded-lg border border-indigo-200 bg-indigo-50/40 p-4 sm:grid-cols-2">
      {error && <p className={`sm:col-span-2 ${errorBox}`}>{error}</p>}
      {children}
    </form>
  );
}

/** Pied de formulaire : bouton de validation (libellé selon l'état) et « Annuler ». */
export function FormActions({
  saving,
  submitLabel,
  savingLabel,
  onCancel,
}: {
  saving: boolean;
  submitLabel: string;
  savingLabel: string;
  onCancel: () => void;
}) {
  return (
    <div className="sm:col-span-2 flex gap-3">
      <button type="submit" disabled={saving} className={btnPrimary}>
        {saving ? savingLabel : submitLabel}
      </button>
      <button type="button" className={btnSecondary} onClick={onCancel}>
        Annuler
      </button>
    </div>
  );
}

/** Champs « Date de début / Date de fin / En cours » (expériences et formations). */
export function DateRangeFields({
  idPrefix,
  startDate,
  endDate,
  ongoing,
  onStartDate,
  onEndDate,
  onOngoing,
}: {
  idPrefix: string;
  startDate: string;
  endDate: string;
  ongoing: boolean;
  onStartDate: (value: string) => void;
  onEndDate: (value: string) => void;
  onOngoing: (value: boolean) => void;
}) {
  return (
    <>
      <div>
        <label className={labelClass} htmlFor={`${idPrefix}-start`}>Date de début</label>
        <input
          id={`${idPrefix}-start`}
          type="date"
          required
          value={startDate}
          onChange={(e) => onStartDate(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label className={labelClass} htmlFor={`${idPrefix}-end`}>Date de fin</label>
        {!ongoing && (
          <input
            id={`${idPrefix}-end`}
            type="date"
            value={endDate}
            min={startDate || undefined}
            required
            onChange={(e) => onEndDate(e.target.value)}
            className={inputClass}
          />
        )}
        <label className={`mt-2 ${checkboxRow}`}>
          <input
            type="checkbox"
            checked={ongoing}
            onChange={(e) => onOngoing(e.target.checked)}
            className={checkboxInput}
          />
          En cours
        </label>
      </div>
    </>
  );
}
