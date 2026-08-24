import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/http';
import { createOffer, getMyOffer, updateOffer } from '../../api/offers';
import { getDegreeOptions, getLanguageOptions, getSkillOptions } from '../../api/profile';
import {
  btnDanger,
  btnPrimary,
  btnSecondary,
  checkboxInput,
  checkboxRow,
  errorBox,
  inputClass,
  labelClass,
  linkBack,
} from '../../components/ui';
import { PROVINCES, SECTORS } from '../../offers/format';
import type {
  JobOfferPayload,
  OfferDegreeRequirement,
  OfferLanguageRequirement,
  OfferSkillRequirement,
} from '../../offers/types';
import { DEGREE_TYPES, LANGUAGE_LEVELS, SKILL_LEVELS } from '../../profile/format';
import type { DegreeOption, LanguageOption, SkillOption } from '../../profile/types';

/** Copie de la liste avec l'élément `index` partiellement remplacé. */
function updateAt<T>(list: T[], index: number, patch: Partial<T>): T[] {
  return list.map((item, i) => (i === index ? { ...item, ...patch } : item));
}

/** Copie de la liste sans l'élément `index`. */
function removeAt<T>(list: T[], index: number): T[] {
  return list.filter((_, i) => i !== index);
}

/** Création ou édition d'une offre d'emploi (lecture seule si clôturée). */
export default function OfferFormPage() {
  const { id } = useParams();
  const offerId = id ? Number(id) : null;
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [sector, setSector] = useState<JobOfferPayload['sector']>('');
  const [city, setCity] = useState('');
  const [province, setProvince] = useState<JobOfferPayload['province']>('');
  const [description, setDescription] = useState('');
  const [salaryMin, setSalaryMin] = useState('');
  const [salaryMax, setSalaryMax] = useState('');
  const [experienceTime, setExperienceTime] = useState('');
  const [vehicleMandatory, setVehicleMandatory] = useState(false);
  const [skills, setSkills] = useState<OfferSkillRequirement[]>([]);
  const [degrees, setDegrees] = useState<OfferDegreeRequirement[]>([]);
  const [languages, setLanguages] = useState<OfferLanguageRequirement[]>([]);
  const [readOnly, setReadOnly] = useState(false);
  /** Pourquoi l'offre n'est plus modifiable, pour l'expliquer à l'employeur. */
  const [lockReason, setLockReason] = useState<'closed' | 'applications' | null>(null);

  const [skillOptions, setSkillOptions] = useState<SkillOption[]>([]);
  const [degreeOptions, setDegreeOptions] = useState<DegreeOption[]>([]);
  const [languageOptions, setLanguageOptions] = useState<LanguageOption[]>([]);

  const [loading, setLoading] = useState(offerId !== null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([getSkillOptions(), getDegreeOptions(), getLanguageOptions()])
      .then(([s, d, l]) => {
        setSkillOptions(s);
        setDegreeOptions(d);
        setLanguageOptions(l);
      })
      .catch(() => setError('Impossible de charger les listes de référence.'));
  }, []);

  useEffect(() => {
    if (offerId === null) {
      return;
    }
    getMyOffer(offerId)
      .then((offer) => {
        setTitle(offer.title);
        setSector(offer.sector);
        setCity(offer.city);
        setProvince(offer.province);
        setDescription(offer.description);
        setSalaryMin(offer.salaryMin?.toString() ?? '');
        setSalaryMax(offer.salaryMax?.toString() ?? '');
        setExperienceTime(offer.experienceTime ?? '');
        setVehicleMandatory(offer.vehicleMandatory ?? false);
        setSkills(offer.skills.map((s) => ({ name: s.name, isMandatory: s.isMandatory, requiredLevel: s.requiredLevel })));
        setDegrees(offer.degrees.map((d) => ({ type: d.type, section: d.section, isMandatory: d.isMandatory })));
        setLanguages(offer.languages.map((l) => ({ languageId: l.languageId, isMandatory: l.isMandatory, requiredLevel: l.requiredLevel })));
        setReadOnly(!offer.editable);
        setLockReason(offer.status === 'CLOSED' ? 'closed' : offer.editable ? null : 'applications');
      })
      .catch((err) => setError(errorMessage(err, "Impossible de charger l'offre.")))
      .finally(() => setLoading(false));
  }, [offerId]);

  const degreeSections = useMemo(
    () => Array.from(new Set(degreeOptions.map((o) => o.section))).sort(),
    [degreeOptions],
  );

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    if (skills.some((s) => !s.name.trim())) {
      setError('Chaque compétence requise doit avoir un nom.');
      return;
    }
    if (degrees.some((d) => !d.section.trim())) {
      setError('Chaque diplôme requis doit avoir une section.');
      return;
    }
    const payload: JobOfferPayload = {
      title,
      sector,
      city,
      province,
      description,
      salaryMin: salaryMin === '' ? null : Number(salaryMin),
      salaryMax: salaryMax === '' ? null : Number(salaryMax),
      experienceTime: experienceTime === '' ? null : experienceTime,
      vehicleMandatory,
      skills: skills.map((s) => ({ ...s, name: s.name.trim() })),
      degrees: degrees.map((d) => ({ ...d, section: d.section.trim() })),
      languages,
    };
    setSaving(true);
    try {
      if (offerId === null) {
        await createOffer(payload);
      } else {
        await updateOffer(offerId, payload);
      }
      navigate('/employeur/offres');
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <p className="text-slate-500">Chargement…</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link to="/employeur/offres" className={linkBack}>
          ← Retour à mes offres
        </Link>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">
          {offerId === null ? 'Nouvelle offre' : readOnly ? "Détail de l'offre" : "Modifier l'offre"}
        </h1>
      </div>

      {lockReason && (
        <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
          {lockReason === 'closed'
            ? "Cette offre est clôturée : elle ne peut plus être modifiée."
            : "Cette offre a déjà reçu une candidature : son contenu est figé, car les candidats ont postulé sur la base du texte publié. Clôturez-la et publiez-en une nouvelle si les conditions ont changé."}
        </div>
      )}

      {error && <p className={errorBox}>{error}</p>}

      <form onSubmit={handleSubmit} className="space-y-6">
        <fieldset disabled={readOnly} className="space-y-6">
          <section className="rounded-xl border border-line bg-surface p-6">
            <h2 className="mb-4 text-lg font-semibold text-slate-900">Le poste</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className={labelClass} htmlFor="offer-title">Titre</label>
                <input id="offer-title" required maxLength={50} value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
              </div>
              <div>
                <label className={labelClass} htmlFor="offer-sector">Secteur</label>
                <select id="offer-sector" required value={sector} onChange={(e) => setSector(e.target.value as JobOfferPayload['sector'])} className={inputClass}>
                  <option value="" disabled>Choisissez un secteur</option>
                  {SECTORS.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
                </select>
              </div>
              <div>
                <label className={labelClass} htmlFor="offer-city">Ville</label>
                <input id="offer-city" required maxLength={20} value={city} onChange={(e) => setCity(e.target.value)} className={inputClass} />
              </div>
              <div>
                <label className={labelClass} htmlFor="offer-province">Province</label>
                <select id="offer-province" required value={province} onChange={(e) => setProvince(e.target.value as JobOfferPayload['province'])} className={inputClass}>
                  <option value="" disabled>Choisissez une province</option>
                  {PROVINCES.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
                </select>
                <p className="mt-1 text-xs text-muted">Elle permet aux candidats d'élargir leur recherche au-delà de la ville.</p>
              </div>
              <div>
                <label className={labelClass} htmlFor="offer-exp">Expérience minimum (années)</label>
                <input id="offer-exp" type="number" min={0} max={99} value={experienceTime} onChange={(e) => setExperienceTime(e.target.value)} className={inputClass} />
              </div>
              <div>
                <label className={labelClass} htmlFor="offer-salmin">Salaire min (€/h)</label>
                <input id="offer-salmin" type="number" min={0} step="0.01" value={salaryMin} onChange={(e) => setSalaryMin(e.target.value)} className={inputClass} />
              </div>
              <div>
                <label className={labelClass} htmlFor="offer-salmax">Salaire max (€/h)</label>
                <input id="offer-salmax" type="number" min={0} step="0.01" value={salaryMax} onChange={(e) => setSalaryMax(e.target.value)} className={inputClass} />
              </div>
              <div className="sm:col-span-2">
                <label className={labelClass} htmlFor="offer-desc">Description</label>
                <textarea id="offer-desc" required rows={5} value={description} onChange={(e) => setDescription(e.target.value)} className={inputClass} />
              </div>
              <label className={checkboxRow}>
                <input type="checkbox" checked={vehicleMandatory} onChange={(e) => setVehicleMandatory(e.target.checked)} className={checkboxInput} />
                Véhicule obligatoire
              </label>
            </div>
          </section>

          <section className="rounded-xl border border-line bg-surface p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">Compétences requises</h2>
              {!readOnly && (
                <button type="button" className={btnSecondary} onClick={() => setSkills((p) => [...p, { name: '', isMandatory: true, requiredLevel: 'DEBUTANT' }])}>
                  + Ajouter
                </button>
              )}
            </div>
            {skills.length === 0 && <p className="text-sm text-slate-500">Aucune compétence requise.</p>}
            <ul className="space-y-3">
              {skills.map((skill, index) => (
                <li key={index} className="grid gap-3 rounded-lg border border-slate-200 p-3 sm:grid-cols-[1fr_auto_auto_auto]">
                  <input
                    aria-label="Compétence"
                    list="offer-skill-options"
                    placeholder="Choisir ou saisir…"
                    value={skill.name}
                    onChange={(e) => setSkills((p) => updateAt(p, index, { name: e.target.value }))}
                    className={inputClass}
                  />
                  <select
                    aria-label="Niveau requis"
                    value={skill.requiredLevel}
                    onChange={(e) => setSkills((p) => updateAt(p, index, { requiredLevel: e.target.value as OfferSkillRequirement['requiredLevel'] }))}
                    className={inputClass}
                  >
                    {SKILL_LEVELS.map((l) => (
                      <option key={l.value} value={l.value}>{l.label}</option>
                    ))}
                  </select>
                  <label className={checkboxRow}>
                    <input
                      type="checkbox"
                      checked={skill.isMandatory}
                      onChange={(e) => setSkills((p) => updateAt(p, index, { isMandatory: e.target.checked }))}
                      className={checkboxInput}
                    />
                    Obligatoire
                  </label>
                  {!readOnly && (
                    <button type="button" className={btnDanger} onClick={() => setSkills((p) => removeAt(p, index))}>
                      Retirer
                    </button>
                  )}
                </li>
              ))}
            </ul>
            <datalist id="offer-skill-options">
              {skillOptions.map((o) => (
                <option key={o.id} value={o.name} />
              ))}
            </datalist>
          </section>

          <section className="rounded-xl border border-line bg-surface p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">Diplômes requis</h2>
              {!readOnly && (
                <button type="button" className={btnSecondary} onClick={() => setDegrees((p) => [...p, { type: 'BACHELIER', section: '', isMandatory: true }])}>
                  + Ajouter
                </button>
              )}
            </div>
            {degrees.length === 0 && <p className="text-sm text-slate-500">Aucun diplôme requis.</p>}
            <ul className="space-y-3">
              {degrees.map((degree, index) => (
                <li key={index} className="grid gap-3 rounded-lg border border-slate-200 p-3 sm:grid-cols-[auto_1fr_auto_auto]">
                  <select
                    aria-label="Type de diplôme"
                    value={degree.type}
                    onChange={(e) => setDegrees((p) => updateAt(p, index, { type: e.target.value as OfferDegreeRequirement['type'] }))}
                    className={inputClass}
                  >
                    {DEGREE_TYPES.map((t) => (
                      <option key={t.value} value={t.value}>{t.label}</option>
                    ))}
                  </select>
                  <input
                    aria-label="Section"
                    list="offer-degree-sections"
                    placeholder="Section…"
                    value={degree.section}
                    onChange={(e) => setDegrees((p) => updateAt(p, index, { section: e.target.value }))}
                    className={inputClass}
                  />
                  <label className={checkboxRow}>
                    <input
                      type="checkbox"
                      checked={degree.isMandatory}
                      onChange={(e) => setDegrees((p) => updateAt(p, index, { isMandatory: e.target.checked }))}
                      className={checkboxInput}
                    />
                    Obligatoire
                  </label>
                  {!readOnly && (
                    <button type="button" className={btnDanger} onClick={() => setDegrees((p) => removeAt(p, index))}>
                      Retirer
                    </button>
                  )}
                </li>
              ))}
            </ul>
            <datalist id="offer-degree-sections">
              {degreeSections.map((s) => (
                <option key={s} value={s} />
              ))}
            </datalist>
          </section>

          <section className="rounded-xl border border-line bg-surface p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">Langues requises</h2>
              {!readOnly && languageOptions.length > 0 && (
                <button type="button" className={btnSecondary} onClick={() => setLanguages((p) => [...p, { languageId: languageOptions[0].id, isMandatory: true, requiredLevel: 'A1' }])}>
                  + Ajouter
                </button>
              )}
            </div>
            {languages.length === 0 && <p className="text-sm text-slate-500">Aucune langue requise.</p>}
            <ul className="space-y-3">
              {languages.map((language, index) => (
                <li key={index} className="grid gap-3 rounded-lg border border-slate-200 p-3 sm:grid-cols-[1fr_auto_auto_auto]">
                  <select
                    aria-label="Langue"
                    value={language.languageId}
                    onChange={(e) => setLanguages((p) => updateAt(p, index, { languageId: Number(e.target.value) }))}
                    className={inputClass}
                  >
                    {languageOptions.map((o) => (
                      <option key={o.id} value={o.id}>{o.name}</option>
                    ))}
                  </select>
                  <select
                    aria-label="Niveau requis"
                    value={language.requiredLevel}
                    onChange={(e) => setLanguages((p) => updateAt(p, index, { requiredLevel: e.target.value as OfferLanguageRequirement['requiredLevel'] }))}
                    className={inputClass}
                  >
                    {LANGUAGE_LEVELS.map((l) => (
                      <option key={l} value={l}>{l}</option>
                    ))}
                  </select>
                  <label className={checkboxRow}>
                    <input
                      type="checkbox"
                      checked={language.isMandatory}
                      onChange={(e) => setLanguages((p) => updateAt(p, index, { isMandatory: e.target.checked }))}
                      className={checkboxInput}
                    />
                    Obligatoire
                  </label>
                  {!readOnly && (
                    <button type="button" className={btnDanger} onClick={() => setLanguages((p) => removeAt(p, index))}>
                      Retirer
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </section>
        </fieldset>

        {!readOnly && (
          <div className="flex gap-3">
            <button type="submit" disabled={saving} className={btnPrimary}>
              {saving ? 'Enregistrement…' : offerId === null ? "Publier l'offre" : 'Enregistrer les modifications'}
            </button>
            <Link to="/employeur/offres" className={btnSecondary}>
              Annuler
            </Link>
          </div>
        )}
      </form>
    </div>
  );
}
