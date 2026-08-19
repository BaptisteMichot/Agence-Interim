import { useState, type FormEvent } from 'react';
import {
  addExperience,
  deleteExperience,
  updateExperience,
} from '../../api/profile';
import { errorMessage } from '../../api/http';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnSecondary, errorBox, inputClass, labelClass } from '../../components/ui';
import { dateRangeError, formatDate } from '../../profile/format';
import type { ExperienceItem, ExperiencePayload, FormMode } from '../../profile/types';
import { useConfirmDelete } from '../../profile/useConfirmDelete';
import { DateRangeFields, FormActions, SectionForm, SectionHeader } from './SectionParts';

interface ExperienceSectionProps {
  experiences: ExperienceItem[];
  onChanged: () => void;
}

export default function ExperienceSection({ experiences, onChanged }: ExperienceSectionProps) {
  const [mode, setMode] = useState<FormMode<ExperienceItem>>({ type: 'closed' });
  const [error, setError] = useState<string | null>(null);
  const { confirmId, setConfirmId, confirmDelete } = useConfirmDelete(
    deleteExperience,
    onChanged,
    setError,
  );

  return (
    <section className="rounded-xl border border-line bg-surface p-6">
      <SectionHeader
        title="Expériences professionnelles"
        showAdd={mode.type === 'closed'}
        onAdd={() => setMode({ type: 'new' })}
      />

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
    const dateError = dateRangeError(startDate, endDate, ongoing);
    if (dateError) {
      setError(dateError);
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
      setError(errorMessage(err, 'Une erreur est survenue.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <SectionForm onSubmit={handleSubmit} error={error}>
      <div>
        <label className={labelClass} htmlFor="exp-company">Entreprise</label>
        <input id="exp-company" required value={companyName} onChange={(e) => setCompanyName(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="exp-position">Poste</label>
        <input id="exp-position" required value={position} onChange={(e) => setPosition(e.target.value)} className={inputClass} />
      </div>
      <DateRangeFields
        idPrefix="exp"
        startDate={startDate}
        endDate={endDate}
        ongoing={ongoing}
        onStartDate={setStartDate}
        onEndDate={setEndDate}
        onOngoing={setOngoing}
      />
      <FormActions
        saving={saving}
        submitLabel="Enregistrer"
        savingLabel="Enregistrement…"
        onCancel={onCancel}
      />
    </SectionForm>
  );
}
