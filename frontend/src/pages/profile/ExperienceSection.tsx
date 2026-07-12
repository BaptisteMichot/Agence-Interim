import { useState, type FormEvent } from 'react';
import {
  addExperience,
  deleteExperience,
  updateExperience,
} from '../../api/profile';
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
import { formatDate } from '../../profile/format';
import type { ExperienceItem, ExperiencePayload } from '../../profile/types';

interface ExperienceSectionProps {
  experiences: ExperienceItem[];
  onChanged: () => void;
}

type FormMode = { type: 'closed' } | { type: 'new' } | { type: 'edit'; item: ExperienceItem };

export default function ExperienceSection({ experiences, onChanged }: ExperienceSectionProps) {
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
      await deleteExperience(id);
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Expériences professionnelles</h2>
        {mode.type === 'closed' && (
          <button type="button" className={btnSecondary} onClick={() => setMode({ type: 'new' })}>
            + Ajouter
          </button>
        )}
      </div>

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}

      {experiences.length === 0 && mode.type === 'closed' && (
        <p className="text-sm text-slate-500">Aucune expérience renseignée.</p>
      )}

      <ul className="space-y-3">
        {experiences.map((item) => (
          <li
            key={item.id}
            className="flex items-start justify-between gap-4 rounded-lg border border-slate-200 p-4"
          >
            <div>
              <p className="font-medium text-slate-900">
                {item.position} — {item.companyName}
              </p>
              <p className="text-sm text-slate-500">
                {formatDate(item.startDate)} → {item.endDate ? formatDate(item.endDate) : 'En cours'}
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
        <ExperienceForm
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
        title="Supprimer l'expérience"
        message="Cette expérience sera définitivement supprimée."
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setConfirmId(null)}
      />
    </section>
  );
}

interface ExperienceFormProps {
  item?: ExperienceItem;
  onCancel: () => void;
  onSaved: () => void;
}

function ExperienceForm({ item, onCancel, onSaved }: ExperienceFormProps) {
  const [companyName, setCompanyName] = useState(item?.companyName ?? '');
  const [position, setPosition] = useState(item?.position ?? '');
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
    const payload: ExperiencePayload = {
      companyName,
      position,
      startDate,
      endDate: ongoing ? null : endDate,
    };
    setSaving(true);
    try {
      if (item) {
        await updateExperience(item.id, payload);
      } else {
        await addExperience(payload);
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
        <label className={labelClass} htmlFor="exp-company">Entreprise</label>
        <input id="exp-company" required value={companyName} onChange={(e) => setCompanyName(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="exp-position">Poste</label>
        <input id="exp-position" required value={position} onChange={(e) => setPosition(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="exp-start">Date de début</label>
        <input id="exp-start" type="date" required value={startDate} onChange={(e) => setStartDate(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="exp-end">Date de fin</label>
        {!ongoing && (
          <input
            id="exp-end"
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
