import { useMemo, useState, type FormEvent } from 'react';
import {
  addLanguage,
  deleteLanguage,
  getLanguageOptions,
  getUserLanguages,
  updateLanguageLevel,
} from '../../api/profile';
import { errorMessage } from '../../api/http';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, errorBox, inputClass, labelClass } from '../../components/ui';
import { LANGUAGE_LEVELS } from '../../profile/format';
import type { LanguageLevel, LanguageOption } from '../../profile/types';
import { useConfirmDelete } from '../../profile/useConfirmDelete';
import { useProfileCollection } from '../../profile/useProfileCollection';
import { FormActions, SectionForm, SectionHeader } from './SectionParts';

export default function LanguageSection() {
  const {
    items: languages,
    setItems: setLanguages,
    options,
    loading,
    error,
    setError,
    reload,
  } = useProfileCollection(getUserLanguages, getLanguageOptions, 'Impossible de charger les langues.');
  const [adding, setAdding] = useState(false);
  const { confirmId, setConfirmId, confirmDelete } = useConfirmDelete(deleteLanguage, reload, setError);

  /** Langues encore disponibles (pas déjà dans le profil). */
  const availableOptions = useMemo(() => {
    const chosen = new Set(languages.map((l) => l.languageId));
    return options.filter((o) => !chosen.has(o.id));
  }, [languages, options]);

  const changeLevel = async (languageId: number, level: LanguageLevel) => {
    setLanguages((prev) => prev.map((l) => (l.languageId === languageId ? { ...l, level } : l)));
    setError(null);
    try {
      await updateLanguageLevel(languageId, level);
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
      reload();
    }
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <SectionHeader
        title="Langues"
        showAdd={!adding && availableOptions.length > 0}
        onAdd={() => setAdding(true)}
      />

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}
      {loading && <p className="text-sm text-slate-500">Chargement…</p>}

      {!loading && languages.length === 0 && !adding && (
        <p className="text-sm text-slate-500">Aucune langue renseignée.</p>
      )}

      <ul className="space-y-3">
        {languages.map((language) => (
          <li
            key={language.languageId}
            className="flex items-center justify-between gap-4 rounded-lg border border-slate-200 p-3"
          >
            <span className="font-medium text-slate-900">{language.name}</span>
            <div className="flex shrink-0 items-center gap-2">
              <select
                value={language.level}
                onChange={(e) => changeLevel(language.languageId, e.target.value as LanguageLevel)}
                className={`${inputClass} w-auto py-1`}
                aria-label={`Niveau pour ${language.name}`}
              >
                {LANGUAGE_LEVELS.map((l) => (
                  <option key={l} value={l}>
                    {l}
                  </option>
                ))}
              </select>
              <button type="button" className={btnDanger} onClick={() => setConfirmId(language.languageId)}>
                Supprimer
              </button>
            </div>
          </li>
        ))}
      </ul>

      {adding && (
        <LanguageForm
          options={availableOptions}
          onCancel={() => setAdding(false)}
          onSaved={() => {
            setAdding(false);
            reload();
          }}
        />
      )}

      <ConfirmDialog
        open={confirmId !== null}
        title="Supprimer la langue"
        message="Cette langue sera retirée de votre profil."
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setConfirmId(null)}
      />
    </section>
  );
}

interface LanguageFormProps {
  options: LanguageOption[];
  onCancel: () => void;
  onSaved: () => void;
}

function LanguageForm({ options, onCancel, onSaved }: LanguageFormProps) {
  const [languageId, setLanguageId] = useState<number>(options[0]?.id ?? 0);
  const [level, setLevel] = useState<LanguageLevel>('A1');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await addLanguage({ languageId, level });
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
        <label className={labelClass} htmlFor="lang-id">Langue</label>
        <select
          id="lang-id"
          value={languageId}
          onChange={(e) => setLanguageId(Number(e.target.value))}
          className={inputClass}
        >
          {options.map((o) => (
            <option key={o.id} value={o.id}>
              {o.name}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label className={labelClass} htmlFor="lang-level">Niveau</label>
        <select
          id="lang-level"
          value={level}
          onChange={(e) => setLevel(e.target.value as LanguageLevel)}
          className={inputClass}
        >
          {LANGUAGE_LEVELS.map((l) => (
            <option key={l} value={l}>
              {l}
            </option>
          ))}
        </select>
      </div>

      <FormActions saving={saving} submitLabel="Ajouter" savingLabel="Ajout…" onCancel={onCancel} />
    </SectionForm>
  );
}
