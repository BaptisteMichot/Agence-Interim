import { useState, type FormEvent } from 'react';
import { addFormation, deleteFormation, updateFormation } from '../../api/profile';
import ConfirmDialog from '../../components/ConfirmDialog';
import {
  btnDanger,
  btnPrimary,
  btnSecondary,
  checkboxInput,
  checkboxRow,
  errorBox,
  inputClass,
  labelClass,
} from '../../components/ui';
import { formatDate, formationStatusLabel } from '../../profile/format';
import type { FormationItem, FormationPayload } from '../../profile/types';

interface FormationSectionProps {
  formations: FormationItem[];
  onChanged: () => void;
}

type FormMode = { type: 'closed' } | { type: 'new' } | { type: 'edit'; item: FormationItem };

export default function FormationSection({ formations, onChanged }: FormationSectionProps) {
  const [mode, setMode] = useState<FormMode>({ type: 'closed' });
  const [error, setError] = useState<string | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);

  const confirmDelete = async () => {
    if (confirmId === null) {
      return;
    }
    const id = confirmId;
    setConfirmId(null);
    setError(null);
    try {
      await deleteFormation(id);
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Formations</h2>
        {mode.type === 'closed' && (
          <button type="button" className={btnSecondary} onClick={() => setMode({ type: 'new' })}>
            + Ajouter
          </button>
        )}
      </div>

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}

      {formations.length === 0 && mode.type === 'closed' && (
        <p className="text-sm text-slate-500">Aucune formation renseignée.</p>
      )}

      <ul className="space-y-3">
        {formations.map((item) => (
          <li
            key={item.id}
            className="flex items-start justify-between gap-4 rounded-lg border border-slate-200 p-4"
          >
            <div>
              <p className="font-medium text-slate-900">
                {item.title} — {item.institution}
              </p>
              <p className="text-sm text-slate-500">
                {formatDate(item.startDate)} → {item.endDate ? formatDate(item.endDate) : 'En cours'}
                <span
                  className={`ml-2 rounded-full px-2 py-0.5 text-xs font-medium ${
                    item.status === 'EN_COURS'
                      ? 'bg-amber-100 text-amber-700'
                      : 'bg-green-100 text-green-700'
                  }`}
                >
                  {formationStatusLabel(item.status)}
                </span>
              </p>
            </div>
            <div className="flex shrink-0 gap-2">
              <button type="button" className={btnSecondary} onClick={() => setMode({ type: 'edit', item })}>
                Modifier
              </button>
              <button type="button" className={btnDanger} onClick={() => setConfirmId(item.id)}>
                Supprimer
              </button>
            </div>
          </li>
        ))}
      </ul>

      {mode.type !== 'closed' && (
        <FormationForm
          item={mode.type === 'edit' ? mode.item : undefined}
          onCancel={() => setMode({ type: 'closed' })}
          onSaved={() => {
            setMode({ type: 'closed' });
            onChanged();
          }}
        />
      )}

      <ConfirmDialog
        open={confirmId !== null}
        title="Supprimer la formation"
        message="Cette formation sera définitivement supprimée."
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setConfirmId(null)}
      />
    </section>
  );
}

interface FormationFormProps {
  item?: FormationItem;
  onCancel: () => void;
  onSaved: () => void;
}

function FormationForm({ item, onCancel, onSaved }: FormationFormProps) {
  const [title, setTitle] = useState(item?.title ?? '');
  const [institution, setInstitution] = useState(item?.institution ?? '');
  const [startDate, setStartDate] = useState(item?.startDate ?? '');
  const [ongoing, setOngoing] = useState(item ? item.endDate === null : false);
  const [endDate, setEndDate] = useState(item?.endDate ?? '');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!ongoing && !endDate) {
      setError('La date de fin est obligatoire, ou cochez « En cours ».');
      return;
    }
    if (!ongoing && endDate < startDate) {
      setError('La date de fin ne peut pas être antérieure à la date de début.');
      return;
    }
    const payload: FormationPayload = {
      title,
      institution,
      startDate,
      endDate: ongoing ? null : endDate,
    };
    setSaving(true);
    try {
      if (item) {
        await updateFormation(item.id, payload);
      } else {
        await addFormation(payload);
      }
      onSaved();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-4 grid gap-4 rounded-lg border border-indigo-200 bg-indigo-50/40 p-4 sm:grid-cols-2">
      {error && <p className={`sm:col-span-2 ${errorBox}`}>{error}</p>}

      <div>
        <label className={labelClass} htmlFor="form-title">Intitulé</label>
        <input id="form-title" required value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="form-institution">Établissement</label>
        <input id="form-institution" required value={institution} onChange={(e) => setInstitution(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="form-start">Date de début</label>
        <input id="form-start" type="date" required value={startDate} onChange={(e) => setStartDate(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="form-end">Date de fin</label>
        {!ongoing && (
          <input
            id="form-end"
            type="date"
            value={endDate}
            min={startDate || undefined}
            required
            onChange={(e) => setEndDate(e.target.value)}
            className={inputClass}
          />
        )}
        <label className={`mt-2 ${checkboxRow}`}>
          <input
            type="checkbox"
            checked={ongoing}
            onChange={(e) => setOngoing(e.target.checked)}
            className={checkboxInput}
          />
          En cours
        </label>
      </div>

      <div className="sm:col-span-2 flex gap-3">
        <button type="submit" disabled={saving} className={btnPrimary}>
          {saving ? 'Enregistrement…' : 'Enregistrer'}
        </button>
        <button type="button" className={btnSecondary} onClick={onCancel}>
          Annuler
        </button>
      </div>
    </form>
  );
}
