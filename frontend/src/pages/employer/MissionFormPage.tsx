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
  breakMinutes,
  breakTooShort,
  datesBetween,
  estimatedPay,
  formatMinutes,
  isWeekend,
  monthLabel,
  paidMinutes,
  presenceMinutes,
  shortTime,
  todayIso,
  totalPaidMinutes,
  weekdayLabel,
  WORK_REASONS,
} from '../../missions/format';
import type { MissionPayload, WorkReason } from '../../missions/types';
import { formatDate } from '../../profile/format';

/** Une journée de la période, prestée ou non. Une pause vide signifie « pas de pause ». */
interface EditableDay {
  date: string;
  worked: boolean;
  startTime: string;
  endTime: string;
  breakStart: string;
  breakEnd: string;
}

/**
 * create : nouvelle mission pour un candidat retenu ; edit : correction après un refus
 * de l'agence ; renew : renouvellement d'une mission confirmée (US19).
 */
export type MissionFormMode = 'create' | 'edit' | 'renew';

/** Au-delà, l'édition jour par jour n'a plus de sens dans un écran. */
const MAX_DAYS = 92;

/** Colonnes alignées du tableau des journées : jour, horaire, pause, temps rémunéré. */
const dayGrid = 'grid grid-cols-[minmax(11rem,1fr)_13rem_13rem_7rem] items-center gap-x-4 px-3';

/** Champ horaire compact, adapté à la densité du tableau des journées. */
const timeInput =
  'w-24 rounded-md border border-slate-300 px-2 py-1 text-sm text-slate-700 outline-none '
  + 'focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500';

/** Heure de pause renvoyée par le backend, ramenée à « HH:mm » ou à une saisie vide. */
function breakOf(time: string | null): string {
  return time ? shortTime(time) : '';
}

/**
 * Contrôle de cohérence de la pause d'une journée (mêmes règles que le backend).
 * La pause reste facultative : seule une saisie incohérente est refusée.
 */
function breakError(day: EditableDay): string | null {
  const jour = formatDate(day.date);
  if (!day.breakStart && !day.breakEnd) {
    return null;
  }
  if (!day.breakStart || !day.breakEnd) {
    return `La pause du ${jour} doit avoir une heure de début et une heure de fin.`;
  }
  if (day.breakEnd <= day.breakStart) {
    return `La fin de la pause du ${jour} doit être postérieure à son début.`;
  }
  if (day.breakStart < day.startTime || day.breakEnd > day.endTime) {
    return `La pause du ${jour} doit être comprise dans l'horaire de la journée.`;
  }
  if (paidMinutes(day) <= 0) {
    return `La pause du ${jour} ne laisse aucun temps de travail rémunéré.`;
  }
  return null;
}

// Journée type : 8 h 30 de présence dont une pause de midi de 30 min non payée = 8 h payées.
const DEFAULT_START = '08:00';
const DEFAULT_END = '16:30';
const DEFAULT_BREAK_START = '12:00';
const DEFAULT_BREAK_END = '12:30';

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
  const [defaultBreakStart, setDefaultBreakStart] = useState(DEFAULT_BREAK_START);
  const [defaultBreakEnd, setDefaultBreakEnd] = useState(DEFAULT_BREAK_END);

  const [position, setPosition] = useState('');
  const [workplace, setWorkplace] = useState('');
  const [description, setDescription] = useState('');
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
            breakStart: DEFAULT_BREAK_START,
            breakEnd: DEFAULT_BREAK_END,
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
          setDescription(source.description);
          setHourlyWage(String(source.hourlyWage));
          setWorkReason(source.workReason);
          setNotes(source.notes ?? '');
          setPreviousPeriod({ start: source.startDate, end: source.endDate });

          // Même durée, à la suite de la mission renouvelée, avec les horaires d'origine.
          const firstSlot = source.slots[0];
          const start = firstSlot ? shortTime(firstSlot.startTime) : DEFAULT_START;
          const end = firstSlot ? shortTime(firstSlot.endTime) : DEFAULT_END;
          const pauseStart = firstSlot?.breakStart ? shortTime(firstSlot.breakStart) : '';
          const pauseEnd = firstSlot?.breakEnd ? shortTime(firstSlot.breakEnd) : '';
          setDefaultStart(start);
          setDefaultEnd(end);
          setDefaultBreakStart(pauseStart);
          setDefaultBreakEnd(pauseEnd);
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
              breakStart: pauseStart,
              breakEnd: pauseEnd,
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
          setDescription(mission.description);
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
                // Une journée non prestée reçoit la pause type, au cas où elle serait cochée.
                breakStart: slot ? breakOf(slot.breakStart) : DEFAULT_BREAK_START,
                breakEnd: slot ? breakOf(slot.breakEnd) : DEFAULT_BREAK_END,
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
        day.worked
          ? {
              ...day,
              startTime: defaultStart,
              endTime: defaultEnd,
              breakStart: defaultBreakStart,
              breakEnd: defaultBreakEnd,
            }
          : day,
      ),
    );
  };

  const setAllWorked = (worked: boolean) => {
    setDays((list) => list.map((day) => ({ ...day, worked })));
  };

  // Un renouvellement ne peut démarrer qu'après la fin de la mission renouvelée.
  const minDate = previousPeriod ? addDays(previousPeriod.end, 1) : todayIso();
  const workedDays = useMemo(() => days.filter((day) => day.worked), [days]);
  const minutes = totalPaidMinutes(workedDays);
  // Avertissement légal non bloquant : plus de 6 h de présence sans pause d'au moins 30 min.
  const daysWithoutBreak = useMemo(() => workedDays.filter(breakTooShort), [workedDays]);
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
    const invalid = workedDays.find((day) => presenceMinutes(day) <= 0);
    if (invalid) {
      setError(`L'horaire du ${formatDate(invalid.date)} est incohérent : la fin doit suivre le début.`);
      return;
    }
    const badBreak = workedDays.map(breakError).find((message) => message !== null);
    if (badBreak) {
      setError(badBreak);
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
      description: description.trim(),
      hourlyWage: wageNumber,
      workReason,
      notes: notes.trim() ? notes.trim() : null,
      slots: workedDays.map((day) => ({
        date: day.date,
        startTime: day.startTime,
        endTime: day.endTime,
        breakStart: day.breakStart || null,
        breakEnd: day.breakEnd || null,
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
              Adresse du lieu de travail
            </label>
            <input
              id="mission-workplace"
              className={inputClass}
              value={workplace}
              maxLength={100}
              required
              placeholder="Rue, numéro, code postal et localité"
              onChange={(e) => setWorkplace(e.target.value)}
            />
            <p className="mt-1 text-xs text-slate-500">
              Adresse exacte où l'intérimaire doit se présenter.
            </p>
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
            <label className={labelClass} htmlFor="mission-description">
              Description du poste
            </label>
            <textarea
              id="mission-description"
              className={`${inputClass} min-h-28`}
              value={description}
              maxLength={2000}
              required
              placeholder="Tâches confiées, matériel utilisé, environnement de travail, horaires particuliers…"
              onChange={(e) => setDescription(e.target.value)}
            />
            <p className="mt-1 text-xs text-slate-500">
              Décrivez le travail réellement confié : c'est sur cette description que l'agence
              s'appuie pour valider ou refuser la mission. {description.length}/2000
            </p>
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
          Choisissez la période, puis décochez les jours non prestés et ajustez les horaires. La
          pause que vous fixez n'est pas rémunérée : elle est déduite du temps de travail payé.
        </p>

        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
          <div>
            <label className={labelClass} htmlFor="mission-default-break-start">
              Pause type (non payée)
            </label>
            <div className="flex items-center gap-2">
              <input
                id="mission-default-break-start"
                type="time"
                className={inputClass}
                value={defaultBreakStart}
                onChange={(e) => setDefaultBreakStart(e.target.value)}
              />
              <span className="text-slate-400">–</span>
              <input
                type="time"
                className={inputClass}
                value={defaultBreakEnd}
                aria-label="Fin de la pause type"
                onChange={(e) => setDefaultBreakEnd(e.target.value)}
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

            <div className="mt-2 overflow-x-auto rounded-lg border border-slate-200">
              <div className="min-w-[46rem]">
                <div
                  className={`${dayGrid} border-b border-slate-200 bg-slate-50 py-2 text-xs font-semibold uppercase tracking-wide text-slate-500`}
                >
                  <span>Journée</span>
                  <span>Horaire</span>
                  <span>Pause (non payée)</span>
                  <span className="text-right">Rémunéré</span>
                </div>

                <ul className="divide-y divide-slate-100">
                  {days.map((day, index) => {
                    const newMonth =
                      index === 0 || monthLabel(day.date) !== monthLabel(days[index - 1].date);
                    const presence = presenceMinutes(day);
                    const pause = breakMinutes(day);
                    const paid = presence - pause;
                    const warned = day.worked && breakTooShort(day);
                    return (
                      <li key={day.date}>
                        {newMonth && (
                          <p className="bg-slate-50 px-3 py-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                            {monthLabel(day.date)}
                          </p>
                        )}
                        <div
                          className={`${dayGrid} py-1.5 ${
                            !day.worked ? 'bg-slate-50/60' : warned ? 'bg-amber-50/70' : ''
                          }`}
                        >
                          <label className="flex cursor-pointer items-center gap-2">
                            <input
                              type="checkbox"
                              className={checkboxInput}
                              checked={day.worked}
                              onChange={(e) => setDay(day.date, { worked: e.target.checked })}
                            />
                            <span
                              className={`text-sm ${
                                !day.worked
                                  ? 'text-slate-400'
                                  : isWeekend(day.date)
                                    ? 'text-amber-700'
                                    : 'text-slate-700'
                              }`}
                            >
                              <span className="capitalize">{weekdayLabel(day.date)}</span>{' '}
                              {formatDate(day.date)}
                            </span>
                          </label>

                          {day.worked ? (
                            <>
                              <div className="flex items-center gap-1.5">
                                <input
                                  type="time"
                                  className={timeInput}
                                  value={day.startTime}
                                  aria-label={`Début du ${formatDate(day.date)}`}
                                  onChange={(e) => setDay(day.date, { startTime: e.target.value })}
                                />
                                <span className="text-slate-300">–</span>
                                <input
                                  type="time"
                                  className={timeInput}
                                  value={day.endTime}
                                  aria-label={`Fin du ${formatDate(day.date)}`}
                                  onChange={(e) => setDay(day.date, { endTime: e.target.value })}
                                />
                              </div>

                              <div className="flex items-center gap-1.5">
                                <input
                                  type="time"
                                  className={timeInput}
                                  value={day.breakStart}
                                  aria-label={`Début de la pause du ${formatDate(day.date)}`}
                                  onChange={(e) => setDay(day.date, { breakStart: e.target.value })}
                                />
                                <span className="text-slate-300">–</span>
                                <input
                                  type="time"
                                  className={timeInput}
                                  value={day.breakEnd}
                                  aria-label={`Fin de la pause du ${formatDate(day.date)}`}
                                  onChange={(e) => setDay(day.date, { breakEnd: e.target.value })}
                                />
                              </div>

                              <div className="text-right">
                                <span
                                  className={`text-sm ${
                                    paid <= 0 ? 'font-medium text-red-600' : 'text-slate-700'
                                  }`}
                                >
                                  {warned && (
                                    <span
                                      className="mr-1 text-amber-600"
                                      title="Plus de 6 h de présence sans pause d'au moins 30 minutes."
                                    >
                                      ⚠
                                    </span>
                                  )}
                                  {formatMinutes(Math.max(0, paid))}
                                </span>
                                {pause > 0 && (
                                  <span className="block text-[11px] text-slate-400">
                                    sur {formatMinutes(Math.max(0, presence))}
                                  </span>
                                )}
                              </div>
                            </>
                          ) : (
                            <span className="col-span-3 text-sm text-slate-400">
                              Journée non prestée
                            </span>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              </div>
            </div>

            {daysWithoutBreak.length > 0 && (
              <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                <p className="font-medium">
                  {daysWithoutBreak.length} journée(s) dépassent 6 h de présence sans pause d'au
                  moins 30 minutes.
                </p>
                <p className="mt-1 text-amber-800">
                  Une pause est légalement requise au-delà de 6 h de travail. Vous pouvez tout de
                  même envoyer la mission : l'agence en jugera.
                </p>
                <p className="mt-1 text-xs text-amber-700">
                  Concernées : {daysWithoutBreak.map((day) => formatDate(day.date)).join(', ')}
                </p>
              </div>
            )}

            <div className="mt-4 rounded-lg bg-indigo-50 px-4 py-3 text-sm text-indigo-900">
              <span className="font-semibold">{workedDays.length} journée(s)</span> ·{' '}
              {formatMinutes(minutes)} rémunérées (pauses déduites)
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
