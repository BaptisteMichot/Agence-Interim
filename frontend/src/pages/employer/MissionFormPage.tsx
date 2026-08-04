import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getCandidateProfile } from '../../api/applications';
import { createMission, getEmployerMission, renewMission, updateMission } from '../../api/missions';
import { getMyOffer } from '../../api/offers';
import {
  btnPrimary,
  btnSecondary,
  checkboxInput,
  errorBox,
  inputClass,
  labelClass,
} from '../../components/ui';
import {
  addDays,
  datesBetween,
  estimatedPay,
  formatMinutes,
  isWeekend,
  monthLabel,
  shortTime,
  slotMinutes,
  todayIso,
  totalMinutes,
  weekdayLabel,
  WORK_REASONS,
} from '../../missions/format';
import type { MissionPayload, WorkReason } from '../../missions/types';
import { formatDate } from '../../profile/format';

/** Une journée de la période, prestée ou non. */
interface EditableDay {
  date: string;
  worked: boolean;
  startTime: string;
  endTime: string;
}

/**
 * create : nouvelle mission pour un candidat retenu ; edit : correction après un refus
 * de l'agence ; renew : renouvellement d'une mission confirmée (US19).
 */
export type MissionFormMode = 'create' | 'edit' | 'renew';

/** Au-delà, l'édition jour par jour n'a plus de sens dans un écran. */
const MAX_DAYS = 92;

const DEFAULT_START = '08:00';
const DEFAULT_END = '16:00';

/**
 * Création de la mission provisoire proposée à un candidat retenu, et correction
 * d'une mission refusée par l'agence. Les conditions saisies ici sont celles qui
 * figureront sur le contrat.
 */
export default function MissionFormPage({ mode = 'create' }: { mode?: MissionFormMode }) {
  const { applicationId: applicationParam, id: missionParam } = useParams();
  const navigate = useNavigate();
  const applicationId = Number(applicationParam);
  const missionId = Number(missionParam);
  const isEdit = mode === 'edit';
  const isRenewal = mode === 'renew';

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [candidate, setCandidate] = useState('');
  const [offerTitle, setOfferTitle] = useState('');
  const [salaryMin, setSalaryMin] = useState<number | null>(null);
  const [salaryMax, setSalaryMax] = useState<number | null>(null);

  const [rangeStart, setRangeStart] = useState(addDays(todayIso(), 7));
  const [rangeEnd, setRangeEnd] = useState(addDays(todayIso(), 11));
  const [days, setDays] = useState<EditableDay[]>([]);
  const [defaultStart, setDefaultStart] = useState(DEFAULT_START);
  const [defaultEnd, setDefaultEnd] = useState(DEFAULT_END);

  const [position, setPosition] = useState('');
  const [workplace, setWorkplace] = useState('');
  const [hourlyWage, setHourlyWage] = useState('');
  const [workReason, setWorkReason] = useState<WorkReason>('OVERLOAD');
  const [notes, setNotes] = useState('');
  const [refusalReason, setRefusalReason] = useState<string | null>(null);
  const [previousPeriod, setPreviousPeriod] = useState<{ start: string; end: string } | null>(null);

  /** Régénère la liste des journées en conservant celles déjà réglées. */
  const rebuildDays = useCallback((start: string, end: string, previous: EditableDay[]) => {
    if (!start || !end || end < start) {
      setDays([]);
      return;
    }
    const dates = datesBetween(start, end);
    if (dates.length > MAX_DAYS) {
      setDays([]);
      return;
    }
    const known = new Map(previous.map((day) => [day.date, day]));
    setDays(
      dates.map(
        (date) =>
          known.get(date) ?? {
            date,
            worked: !isWeekend(date),
            startTime: DEFAULT_START,
            endTime: DEFAULT_END,
          },
      ),
    );
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        if (isRenewal) {
          const source = await getEmployerMission(missionId);
          const offer = await getMyOffer(source.offerId);
          if (cancelled) {
            return;
          }
          setCandidate(`${source.candidateFirstName} ${source.candidateLastName}`);
          setOfferTitle(source.offerTitle);
          setSalaryMin(offer.salaryMin);
          setSalaryMax(offer.salaryMax);
          setPosition(source.position);
          setWorkplace(source.workplace);
          setHourlyWage(String(source.hourlyWage));
          setWorkReason(source.workReason);
          setNotes(source.notes ?? '');
          setPreviousPeriod({ start: source.startDate, end: source.endDate });

          // Même durée, à la suite de la mission renouvelée, avec les horaires d'origine.
          const firstSlot = source.slots[0];
          const start = firstSlot ? shortTime(firstSlot.startTime) : DEFAULT_START;
          const end = firstSlot ? shortTime(firstSlot.endTime) : DEFAULT_END;
          setDefaultStart(start);
          setDefaultEnd(end);
          const length = datesBetween(source.startDate, source.endDate).length;
          const nextStart = addDays(source.endDate, 1);
          const nextEnd = addDays(nextStart, length - 1);
          setRangeStart(nextStart);
          setRangeEnd(nextEnd);
          setDays(
            datesBetween(nextStart, nextEnd).map((date) => ({
              date,
              worked: !isWeekend(date),
              startTime: start,
              endTime: end,
            })),
          );
        } else if (isEdit) {
          const mission = await getEmployerMission(missionId);
          const offer = await getMyOffer(mission.offerId);
          if (cancelled) {
            return;
          }
          setCandidate(`${mission.candidateFirstName} ${mission.candidateLastName}`);
          setOfferTitle(mission.offerTitle);
          setSalaryMin(offer.salaryMin);
          setSalaryMax(offer.salaryMax);
          setPosition(mission.position);
          setWorkplace(mission.workplace);
          setHourlyWage(String(mission.hourlyWage));
          setWorkReason(mission.workReason);
          setNotes(mission.notes ?? '');
          setRefusalReason(mission.refusalReason);
          setRangeStart(mission.startDate);
          setRangeEnd(mission.endDate);
          const worked = new Map(mission.slots.map((slot) => [slot.date, slot]));
          setDays(
            datesBetween(mission.startDate, mission.endDate).map((date) => {
              const slot = worked.get(date);
              return {
                date,
                worked: Boolean(slot),
                startTime: slot ? shortTime(slot.startTime) : DEFAULT_START,
                endTime: slot ? shortTime(slot.endTime) : DEFAULT_END,
              };
            }),
          );
        } else {
          const profile = await getCandidateProfile(applicationId);
          const offer = await getMyOffer(profile.offerId);
          if (cancelled) {
            return;
          }
          setCandidate(`${profile.firstName} ${profile.lastName}`);
          setOfferTitle(profile.offerTitle);
          setSalaryMin(offer.salaryMin);
          setSalaryMax(offer.salaryMax);
          setPosition(offer.title);
          setWorkplace(offer.city);
          setHourlyWage(offer.salaryMin !== null ? String(offer.salaryMin) : '');
          rebuildDays(addDays(todayIso(), 7), addDays(todayIso(), 11), []);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Impossible de charger les informations.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [applicationId, isEdit, isRenewal, missionId, rebuildDays]);

  const changeRange = (start: string, end: string) => {
    setRangeStart(start);
    setRangeEnd(end);
    rebuildDays(start, end, days);
  };

  const setDay = (date: string, patch: Partial<EditableDay>) => {
    setDays((list) => list.map((day) => (day.date === date ? { ...day, ...patch } : day)));
  };

  const applyDefaultTimes = () => {
    setDays((list) =>
      list.map((day) =>
        day.worked ? { ...day, startTime: defaultStart, endTime: defaultEnd } : day,
      ),
    );
  };

  const setAllWorked = (worked: boolean) => {
    setDays((list) => list.map((day) => ({ ...day, worked })));
  };

  // Un renouvellement ne peut démarrer qu'après la fin de la mission renouvelée.
  const minDate = previousPeriod ? addDays(previousPeriod.end, 1) : todayIso();
  const workedDays = useMemo(() => days.filter((day) => day.worked), [days]);
  const minutes = totalMinutes(workedDays);
  const wageNumber = Number(hourlyWage.replace(',', '.'));
  const tooManyDays = Boolean(
    rangeStart && rangeEnd && rangeEnd >= rangeStart && datesBetween(rangeStart, rangeEnd).length > MAX_DAYS,
  );

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (workedDays.length === 0) {
      setError('Sélectionnez au moins une journée de travail.');
      return;
    }
    if (previousPeriod && workedDays.some((day) => day.date <= previousPeriod.end)) {
      setError(
        `Le renouvellement doit commencer après le ${formatDate(previousPeriod.end)}, fin de la mission en cours.`,
      );
      return;
    }
    const invalid = workedDays.find((day) => slotMinutes(day) <= 0);
    if (invalid) {
      setError(`L'horaire du ${formatDate(invalid.date)} est incohérent : la fin doit suivre le début.`);
      return;
    }
    if (!Number.isFinite(wageNumber) || wageNumber <= 0) {
      setError('Indiquez un salaire horaire valide.');
      return;
    }
    if (salaryMin !== null && wageNumber < salaryMin) {
      setError(`Le salaire horaire ne peut pas être inférieur au minimum de l'offre (${salaryMin} €/h).`);
      return;
    }
    if (salaryMax !== null && wageNumber > salaryMax) {
      setError(`Le salaire horaire ne peut pas être supérieur au maximum de l'offre (${salaryMax} €/h).`);
      return;
    }

    // La période de la mission est celle des journées réellement prestées.
    const dates = workedDays.map((day) => day.date).sort();
    const payload: MissionPayload = {
      startDate: dates[0],
      endDate: dates[dates.length - 1],
      position: position.trim(),
      workplace: workplace.trim(),
      hourlyWage: wageNumber,
      workReason,
      notes: notes.trim() ? notes.trim() : null,
      slots: workedDays.map((day) => ({
        date: day.date,
        startTime: day.startTime,
        endTime: day.endTime,
      })),
    };

    setSaving(true);
    try {
      let mission;
      if (isEdit) {
        mission = await updateMission(missionId, payload);
      } else if (isRenewal) {
        mission = await renewMission(missionId, payload);
      } else {
        mission = await createMission(applicationId, payload);
      }
      navigate(`/employeur/missions/${mission.id}`, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "La mission n'a pas pu être enregistrée.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <p className="text-slate-500">Chargement…</p>;
  }

  return (
    <form onSubmit={submit} className="space-y-6">
      <div>
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="text-sm text-indigo-600 hover:underline"
        >
          ← Retour
        </button>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">
          {isEdit ? 'Corriger la mission' : isRenewal ? 'Renouveler la mission' : 'Proposer une mission'}
        </h1>
        <p className="mt-1 text-slate-600">
          {candidate} · offre « {offerTitle} »
        </p>
        <p className="mt-1 text-sm text-slate-500">
          {isRenewal
            ? "Les conditions de la mission en cours sont reprises : ajustez-les si besoin. L'intérimaire accepte ou refuse le renouvellement, puis l'agence le valide."
            : "Ces informations seront reprises telles quelles dans le contrat. La mission est ensuite soumise à l'agence, puis à l'intérimaire."}
        </p>
      </div>

      {previousPeriod && (
        <div className="rounded-xl border border-violet-200 bg-violet-50 p-4">
          <p className="text-sm text-violet-900">
            Renouvellement de la mission du {formatDate(previousPeriod.start)} au{' '}
            {formatDate(previousPeriod.end)}. Le renouvellement doit démarrer après cette date.
          </p>
        </div>
      )}

      {refusalReason && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
          <p className="text-sm font-medium text-amber-900">Motif du refus de l'agence</p>
          <p className="mt-1 whitespace-pre-line text-sm text-amber-800">{refusalReason}</p>
        </div>
      )}

      {error && <p className={errorBox}>{error}</p>}

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Conditions du contrat</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className={labelClass} htmlFor="mission-position">
              Intitulé du poste
            </label>
            <input
              id="mission-position"
              className={inputClass}
              value={position}
              maxLength={50}
              required
              onChange={(e) => setPosition(e.target.value)}
            />
          </div>
          <div>
            <label className={labelClass} htmlFor="mission-workplace">
              Lieu de travail
            </label>
            <input
              id="mission-workplace"
              className={inputClass}
              value={workplace}
              maxLength={50}
              required
              onChange={(e) => setWorkplace(e.target.value)}
            />
          </div>
          <div>
            <label className={labelClass} htmlFor="mission-wage">
              Salaire horaire brut (€/h)
            </label>
            <input
              id="mission-wage"
              type="number"
              step="0.01"
              min="0.01"
              className={inputClass}
              value={hourlyWage}
              required
              onChange={(e) => setHourlyWage(e.target.value)}
            />
            {(salaryMin !== null || salaryMax !== null) && (
              <p className="mt-1 text-xs text-slate-500">
                Fourchette annoncée dans l'offre : {salaryMin ?? '?'} – {salaryMax ?? '?'} €/h.
              </p>
            )}
          </div>
          <div>
            <label className={labelClass} htmlFor="mission-reason">
              Motif de recours à l'intérim
            </label>
            <select
              id="mission-reason"
              className={inputClass}
              value={workReason}
              onChange={(e) => setWorkReason(e.target.value as WorkReason)}
            >
              {WORK_REASONS.map((reason) => (
                <option key={reason.value} value={reason.value}>
                  {reason.label}
                </option>
              ))}
            </select>
          </div>
          <div className="sm:col-span-2">
            <label className={labelClass} htmlFor="mission-notes">
              Conditions particulières (facultatif)
            </label>
            <textarea
              id="mission-notes"
              className={`${inputClass} min-h-20`}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Équipement fourni, consignes de sécurité, personne de contact…"
            />
          </div>
        </div>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-slate-900">Horaire de la mission</h2>
        <p className="mt-1 text-sm text-slate-500">
          Choisissez la période, puis décochez les jours non prestés et ajustez les horaires.
        </p>

        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className={labelClass} htmlFor="mission-start">
              Du
            </label>
            <input
              id="mission-start"
              type="date"
              className={inputClass}
              value={rangeStart}
              min={minDate}
              onChange={(e) => changeRange(e.target.value, rangeEnd)}
            />
          </div>
          <div>
            <label className={labelClass} htmlFor="mission-end">
              Au
            </label>
            <input
              id="mission-end"
              type="date"
              className={inputClass}
              value={rangeEnd}
              min={rangeStart || minDate}
              onChange={(e) => changeRange(rangeStart, e.target.value)}
            />
          </div>
          <div>
            <label className={labelClass} htmlFor="mission-default-start">
              Horaire type
            </label>
            <div className="flex items-center gap-2">
              <input
                id="mission-default-start"
                type="time"
                className={inputClass}
                value={defaultStart}
                onChange={(e) => setDefaultStart(e.target.value)}
              />
              <span className="text-slate-400">–</span>
              <input
                type="time"
                className={inputClass}
                value={defaultEnd}
                aria-label="Fin de l'horaire type"
                onChange={(e) => setDefaultEnd(e.target.value)}
              />
            </div>
          </div>
          <div className="flex items-end">
            <button type="button" className={`${btnSecondary} w-full`} onClick={applyDefaultTimes}>
              Appliquer à tous les jours
            </button>
          </div>
        </div>

        {tooManyDays && (
          <p className={`mt-4 ${errorBox}`}>
            La période dépasse {MAX_DAYS} jours : réduisez-la pour pouvoir régler les journées.
          </p>
        )}

        {days.length > 0 && (
          <>
            <div className="mt-6 flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-medium text-slate-700">Journées prestées</p>
              <div className="flex gap-2">
                <button
                  type="button"
                  className="text-xs font-medium text-indigo-600 hover:underline"
                  onClick={() => setAllWorked(true)}
                >
                  Tout cocher
                </button>
                <span className="text-xs text-slate-300">|</span>
                <button
                  type="button"
                  className="text-xs font-medium text-indigo-600 hover:underline"
                  onClick={() => setAllWorked(false)}
                >
                  Tout décocher
                </button>
              </div>
            </div>

            <ul className="mt-2 divide-y divide-slate-100 rounded-lg border border-slate-200">
              {days.map((day, index) => {
                const newMonth = index === 0 || monthLabel(day.date) !== monthLabel(days[index - 1].date);
                const duration = slotMinutes(day);
                return (
                  <li key={day.date}>
                    {newMonth && (
                      <p className="bg-slate-50 px-3 py-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                        {monthLabel(day.date)}
                      </p>
                    )}
                    <div
                      className={`flex flex-wrap items-center gap-3 px-3 py-2 ${
                        day.worked ? '' : 'bg-slate-50/60 text-slate-400'
                      }`}
                    >
                      <label className="flex min-w-44 flex-1 cursor-pointer items-center gap-2">
                        <input
                          type="checkbox"
                          className={checkboxInput}
                          checked={day.worked}
                          onChange={(e) => setDay(day.date, { worked: e.target.checked })}
                        />
                        <span
                          className={`text-sm ${
                            isWeekend(day.date) ? 'text-amber-700' : 'text-slate-700'
                          }`}
                        >
                          <span className="capitalize">{weekdayLabel(day.date)}</span>{' '}
                          {formatDate(day.date)}
                        </span>
                      </label>
                      <div className="flex items-center gap-2">
                        <input
                          type="time"
                          className={`${inputClass} w-28`}
                          value={day.startTime}
                          disabled={!day.worked}
                          aria-label={`Début du ${formatDate(day.date)}`}
                          onChange={(e) => setDay(day.date, { startTime: e.target.value })}
                        />
                        <span className="text-slate-400">–</span>
                        <input
                          type="time"
                          className={`${inputClass} w-28`}
                          value={day.endTime}
                          disabled={!day.worked}
                          aria-label={`Fin du ${formatDate(day.date)}`}
                          onChange={(e) => setDay(day.date, { endTime: e.target.value })}
                        />
                        <span
                          className={`w-16 text-right text-xs ${
                            day.worked && duration <= 0 ? 'font-medium text-red-600' : 'text-slate-500'
                          }`}
                        >
                          {day.worked ? formatMinutes(Math.max(0, duration)) : '—'}
                        </span>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>

            <div className="mt-4 rounded-lg bg-indigo-50 px-4 py-3 text-sm text-indigo-900">
              <span className="font-semibold">{workedDays.length} journée(s)</span> ·{' '}
              {formatMinutes(minutes)} au total
              {Number.isFinite(wageNumber) && wageNumber > 0 && (
                <> · rémunération brute estimée {estimatedPay(minutes, wageNumber)}</>
              )}
            </div>
          </>
        )}
      </section>

      <div className="flex justify-end gap-3">
        <button type="button" className={btnSecondary} onClick={() => navigate(-1)}>
          Annuler
        </button>
        <button type="submit" className={btnPrimary} disabled={saving}>
          {saving
            ? 'Envoi…'
            : isRenewal
              ? 'Proposer le renouvellement'
              : isEdit
                ? 'Renvoyer à l’agence'
                : 'Envoyer à l’agence'}
        </button>
      </div>
    </form>
  );
}
