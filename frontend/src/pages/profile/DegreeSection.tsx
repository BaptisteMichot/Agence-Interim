import { useMemo, useState, type FormEvent } from 'react';
import { addDegree, deleteDegree, getDegreeOptions, getUserDegrees, updateDegree } from '../../api/profile';
import { errorMessage } from '../../api/http';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnSecondary, errorBox, inputClass, labelClass } from '../../components/ui';
import { DEGREE_TYPES, degreeTypeLabel } from '../../profile/format';
import type { DegreeOption, DegreeType, FormMode, UserDegree } from '../../profile/types';
import { useConfirmDelete } from '../../profile/useConfirmDelete';
import { useProfileCollection } from '../../profile/useProfileCollection';
import { FormActions, SectionForm, SectionHeader } from './SectionParts';

export default function DegreeSection() {
  const {
    items: degrees,
    options,
    loading,
    error,
    setError,
    reload,
  } = useProfileCollection<UserDegree, DegreeOption>(
    getUserDegrees,
    getDegreeOptions,
    'Impossible de charger les diplômes.',
  );
  const [mode, setMode] = useState<FormMode<UserDegree>>({ type: 'closed' });
  const { confirmId, setConfirmId, confirmDelete } = useConfirmDelete(deleteDegree, reload, setError);

  const sections = useMemo(
    () => Array.from(new Set(options.map((o) => o.section))).sort(),
    [options],
  );

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <SectionHeader
        title="Diplômes"
        showAdd={mode.type === 'closed'}
        onAdd={() => setMode({ type: 'new' })}
      />

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}
      {loading && <p className="text-sm text-slate-500">Chargement…</p>}

      {!loading && degrees.length === 0 && mode.type === 'closed' && (
        <p className="text-sm text-slate-500">Aucun diplôme renseigné.</p>
      )}

      <ul className="space-y-3">
        {degrees.map((degree) => (
          <li
            key={degree.degreeId}
            className="flex items-start justify-between gap-4 rounded-lg border border-slate-200 p-4"
          >
            <div>
              <p className="font-medium text-slate-900">
                {degreeTypeLabel(degree.type)} — {degree.section}
                {degree.custom && (
                  <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">perso</span>
                )}
              </p>
              <p className="text-sm text-slate-500">
                {degree.institution ?? 'Établissement non précisé'}
                {degree.graduationYear ? ` · ${degree.graduationYear}` : ''}
              </p>
            </div>
            <div className="flex shrink-0 gap-2">
              <button type="button" className={btnSecondary} onClick={() => setMode({ type: 'edit', item: degree })}>
                Modifier
              </button>
              <button type="button" className={btnDanger} onClick={() => setConfirmId(degree.degreeId)}>
                Supprimer
              </button>
            </div>
          </li>
        ))}
      </ul>

      {mode.type !== 'closed' && (
        <DegreeForm
          item={mode.type === 'edit' ? mode.item : undefined}
          sections={sections}
          onCancel={() => setMode({ type: 'closed' })}
          onSaved={() => {
            setMode({ type: 'closed' });
            reload();
          }}
        />
      )}

      <ConfirmDialog
        open={confirmId !== null}
        title="Supprimer le diplôme"
        message="Ce diplôme sera retiré de votre profil."
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setConfirmId(null)}
      />
    </section>
  );
}

interface DegreeFormProps {
  item?: UserDegree;
  sections: string[];
  onCancel: () => void;
  onSaved: () => void;
}

function DegreeForm({ item, sections, onCancel, onSaved }: DegreeFormProps) {
  const editing = item !== undefined;
  const [type, setType] = useState<DegreeType>(item?.type ?? 'BACHELIER');
  const [section, setSection] = useState(item?.section ?? '');
  const [institution, setInstitution] = useState(item?.institution ?? '');
  const [graduationYear, setGraduationYear] = useState(item?.graduationYear?.toString() ?? '');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!editing && !section.trim()) {
      setError('Indiquez la section du diplôme.');
      return;
    }
    const year = graduationYear ? Number(graduationYear) : null;
    setSaving(true);
    try {
      if (item) {
        await updateDegree(item.degreeId, { institution: institution || null, graduationYear: year });
      } else {
        await addDegree({ type, section: section.trim(), institution: institution || null, graduationYear: year });
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
      {editing ? (
        <p className="sm:col-span-2 text-sm font-medium text-slate-700">
          {degreeTypeLabel(type)} — {section}
        </p>
      ) : (
        <>
          <div>
            <label className={labelClass} htmlFor="deg-type">Type</label>
            <select
              id="deg-type"
              value={type}
              onChange={(e) => setType(e.target.value as DegreeType)}
              className={inputClass}
            >
              {DEGREE_TYPES.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className={labelClass} htmlFor="deg-section">Section</label>
            <input
              id="deg-section"
              list="degree-sections"
              value={section}
              onChange={(e) => setSection(e.target.value)}
              placeholder="Choisir ou saisir…"
              className={inputClass}
            />
            <datalist id="degree-sections">
              {sections.map((s) => (
                <option key={s} value={s} />
              ))}
            </datalist>
          </div>
        </>
      )}

      <div>
        <label className={labelClass} htmlFor="deg-institution">Établissement</label>
        <input
          id="deg-institution"
          value={institution}
          onChange={(e) => setInstitution(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label className={labelClass} htmlFor="deg-year">Année d'obtention</label>
        <input
          id="deg-year"
          type="number"
          min={1950}
          max={2100}
          value={graduationYear}
          onChange={(e) => setGraduationYear(e.target.value)}
          className={inputClass}
        />
      </div>

      <FormActions
        saving={saving}
        submitLabel="Enregistrer"
        savingLabel="Enregistrement…"
        onCancel={onCancel}
      />
    </SectionForm>
  );
}
