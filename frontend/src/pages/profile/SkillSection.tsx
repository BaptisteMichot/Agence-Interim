import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { addSkill, deleteSkill, getSkillOptions, getUserSkills, updateSkillLevel } from '../../api/profile';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnPrimary, btnSecondary, errorBox, inputClass, labelClass } from '../../components/ui';
import { SKILL_LEVELS } from '../../profile/format';
import type { SkillLevel, SkillOption, UserSkill } from '../../profile/types';

export default function SkillSection() {
  const [skills, setSkills] = useState<UserSkill[]>([]);
  const [options, setOptions] = useState<SkillOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [confirmId, setConfirmId] = useState<number | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      const [userSkills, skillOptions] = await Promise.all([getUserSkills(), getSkillOptions()]);
      setSkills(userSkills);
      setOptions(skillOptions);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger les compétences.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const changeLevel = async (skillId: number, level: SkillLevel) => {
    setSkills((prev) => prev.map((s) => (s.skillId === skillId ? { ...s, level } : s)));
    setError(null);
    try {
      await updateSkillLevel(skillId, level);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
      reload();
    }
  };

  const confirmDelete = async () => {
    if (confirmId === null) {
      return;
    }
    const id = confirmId;
    setConfirmId(null);
    setError(null);
    try {
      await deleteSkill(id);
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Compétences</h2>
        {!adding && (
          <button type="button" className={btnSecondary} onClick={() => setAdding(true)}>
            + Ajouter
          </button>
        )}
      </div>

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}
      {loading && <p className="text-sm text-slate-500">Chargement…</p>}

      {!loading && skills.length === 0 && !adding && (
        <p className="text-sm text-slate-500">Aucune compétence renseignée.</p>
      )}

      <ul className="space-y-3">
        {skills.map((skill) => (
          <li
            key={skill.skillId}
            className="flex items-center justify-between gap-4 rounded-lg border border-slate-200 p-3"
          >
            <span className="font-medium text-slate-900">
              {skill.name}
              {skill.custom && (
                <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">perso</span>
              )}
            </span>
            <div className="flex shrink-0 items-center gap-2">
              <select
                value={skill.level}
                onChange={(e) => changeLevel(skill.skillId, e.target.value as SkillLevel)}
                className={`${inputClass} w-auto py-1`}
                aria-label={`Niveau pour ${skill.name}`}
              >
                {SKILL_LEVELS.map((l) => (
                  <option key={l.value} value={l.value}>
                    {l.label}
                  </option>
                ))}
              </select>
              <button type="button" className={btnDanger} onClick={() => setConfirmId(skill.skillId)}>
                Supprimer
              </button>
            </div>
          </li>
        ))}
      </ul>

      {adding && (
        <SkillForm
          options={options}
          onCancel={() => setAdding(false)}
          onSaved={() => {
            setAdding(false);
            reload();
          }}
        />
      )}

      <ConfirmDialog
        open={confirmId !== null}
        title="Supprimer la compétence"
        message="Cette compétence sera retirée de votre profil."
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setConfirmId(null)}
      />
    </section>
  );
}

interface SkillFormProps {
  options: SkillOption[];
  onCancel: () => void;
  onSaved: () => void;
}

function SkillForm({ options, onCancel, onSaved }: SkillFormProps) {
  const [name, setName] = useState('');
  const [level, setLevel] = useState<SkillLevel>('DEBUTANT');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!name.trim()) {
      setError('Indiquez une compétence.');
      return;
    }
    setSaving(true);
    try {
      await addSkill({ name: name.trim(), level });
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
        <label className={labelClass} htmlFor="skill-name">Compétence</label>
        <input
          id="skill-name"
          list="skill-options"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Choisir ou saisir…"
          className={inputClass}
        />
        <datalist id="skill-options">
          {options.map((o) => (
            <option key={o.id} value={o.name} />
          ))}
        </datalist>
        <p className="mt-1 text-xs text-slate-500">
          Sélectionnez dans la liste ou saisissez une compétence qui vous est propre.
        </p>
      </div>
      <div>
        <label className={labelClass} htmlFor="skill-level">Niveau</label>
        <select
          id="skill-level"
          value={level}
          onChange={(e) => setLevel(e.target.value as SkillLevel)}
          className={inputClass}
        >
          {SKILL_LEVELS.map((l) => (
            <option key={l.value} value={l.value}>
              {l.label}
            </option>
          ))}
        </select>
      </div>

      <div className="sm:col-span-2 flex gap-3">
        <button type="submit" disabled={saving} className={btnPrimary}>
          {saving ? 'Ajout…' : 'Ajouter'}
        </button>
        <button type="button" className={btnSecondary} onClick={onCancel}>
          Annuler
        </button>
      </div>
    </form>
  );
}
