import { useState, type FormEvent } from 'react';
import { addFormation, deleteFormation, updateFormation } from '../../api/profile';
import { errorMessage } from '../../api/http';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnSecondary, errorBox, inputClass, labelClass } from '../../components/ui';
import { dateRangeError, formatDate, formationStatusLabel } from '../../profile/format';
import type { FormationItem, FormationPayload, FormMode } from '../../profile/types';
import { useConfirmDelete } from '../../profile/useConfirmDelete';
import { DateRangeFields, FormActions, SectionForm, SectionHeader } from './SectionParts';

interface FormationSectionProps {
  formations: FormationItem[];
  onChanged: () => void;
}

export default function FormationSection({ formations, onChanged }: FormationSectionProps) {
  const [mode, setMode] = useState<FormMode<FormationItem>>({ type: 'closed' });
  const [error, setError] = useState<string | null>(null);
  const { confirmId, setConfirmId, confirmDelete } = useConfirmDelete(
    deleteFormation,
    onChanged,
    setError,
  );

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <SectionHeader
        title="Formations"
        showAdd={mode.type === 'closed'}
        onAdd={() => setMode({ type: 'new' })}
      />

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
    const dateError = dateRangeError(startDate, endDate, ongoing);
    if (dateError) {
      setError(dateError);
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
      setError(errorMessage(err, 'Une erreur est survenue.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <SectionForm onSubmit={handleSubmit} error={error}>
      <div>
        <label className={labelClass} htmlFor="form-title">Intitulé</label>
        <input id="form-title" required value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label className={labelClass} htmlFor="form-institution">Établissement</label>
        <input id="form-institution" required value={institution} onChange={(e) => setInstitution(e.target.value)} className={inputClass} />
      </div>
      <DateRangeFields
        idPrefix="form"
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
