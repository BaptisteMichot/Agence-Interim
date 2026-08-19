import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/http';
import {
  addUnavailability,
  getEditableFrom,
  getSchedule,
  getUnavailabilities,
  removeUnavailability,
} from '../../api/planning';
import ConfirmDialog from '../../components/ConfirmDialog';
import {
  btnDanger,
  btnPrimary,
  btnSecondary,
  checkboxInput,
  errorBox,
  inputClass,
  labelClass,
  linkBack,
} from '../../components/ui';
import {
  addDays,
  addMonths,
  formatMinutes,
  isFullDay,
  monthGrid,
  paidMinutes,
  scheduleLabel,
  shortTime,
  startOfMonth,
  todayIso,
  weekdayLabel,
  workedRanges,
} from '../../missions/format';
import MonthCalendar from '../../missions/MonthCalendar';
import type { ScheduleEntry, Unavailability } from '../../missions/types';
import { formatDate } from '../../profile/format';

const FULL_DAY_START = '00:00';
const FULL_DAY_END = '23:59';

/** Planning de l'intérimaire : missions confirmées et indisponibilités déclarées. */
export default function PlanningPage() {
  const [monthStart, setMonthStart] = useState(() => startOfMonth(todayIso()));
  const [selected, setSelected] = useState<string | null>(() => todayIso());
  const [missions, setMissions] = useState<ScheduleEntry[]>([]);
  const [unavailabilities, setUnavailabilities] = useState<Unavailability[]>([]);
  const [editableFrom, setEditableFrom] = useState(() => addDays(todayIso(), 8));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Unavailability | null>(null);

  // Formulaire d'ajout d'indisponibilité pour le jour sélectionné.
  const [fullDay, setFullDay] = useState(true);
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('17:00');
  const [reason, setReason] = useState('');
  const [saving, setSaving] = useState(false);

  const grid = useMemo(() => monthGrid(monthStart), [monthStart]);
  const from = grid[0];
  const to = grid[grid.length - 1];

  const reload = useCallback(async () => {
    setError(null);
    try {
      const [schedule, unavailable, editable] = await Promise.all([
        getSchedule(from, to),
        getUnavailabilities(from, to),
        getEditableFrom(),
      ]);
      setMissions(schedule);
      setUnavailabilities(unavailable);
      setEditableFrom(editable.editableFrom);
    } catch (err) {
      setError(errorMessage(err, 'Impossible de charger le planning.'));
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    reload();
  }, [reload]);

  const selectedMissions = useMemo(
    () => missions.filter((entry) => entry.date === selected),
    [missions, selected],
  );
  const selectedUnavailabilities = useMemo(
    () => unavailabilities.filter((item) => item.date === selected),
    [unavailabilities, selected],
  );

  const monthMissions = useMemo(
    () => missions.filter((entry) => entry.date.slice(0, 7) === monthStart.slice(0, 7)),
    [missions, monthStart],
  );
  const monthMinutes = monthMissions.reduce((total, entry) => total + paidMinutes(entry), 0);

  const upcoming = useMemo(
    () => missions.filter((entry) => entry.date >= todayIso()).slice(0, 5),
    [missions],
  );

  const canEditSelected = selected !== null && selected >= editableFrom;

  const submitUnavailability = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selected) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await addUnavailability({
        date: selected,
        startTime: fullDay ? FULL_DAY_START : startTime,
        endTime: fullDay ? FULL_DAY_END : endTime,
        reason: reason.trim() ? reason.trim() : null,
      });
      setReason('');
      await reload();
    } catch (err) {
      setError(errorMessage(err, "L'indisponibilité n'a pas pu être ajoutée."));
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleting) {
      return;
    }
    const id = deleting.id;
    setDeleting(null);
    setError(null);
    try {
      await removeUnavailability(id);
      await reload();
    } catch (err) {
      setError(errorMessage(err, 'La suppression a échoué.'));
    }
  };

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link to="/interimaire" className={linkBack}>
            ← Retour à mon espace
          </Link>
          <h1 className="mt-2 text-2xl font-semibold text-slate-900">Mon planning</h1>
          <p className="mt-1 text-slate-600">
            Tes journées de mission confirmées et les périodes où tu n'es pas disponible.
          </p>
        </div>
        <Link to="/interimaire/missions" className={btnSecondary}>
          Mes missions
        </Link>
      </div>

      {error && <p className={errorBox}>{error}</p>}
      {loading && <p className="text-sm text-slate-500">Chargement…</p>}

      <MonthCalendar
        monthStart={monthStart}
        missions={missions}
        unavailabilities={unavailabilities}
        selected={selected}
        onSelect={setSelected}
        onMonthShift={(delta) => setMonthStart((month) => addMonths(month, delta))}
        onToday={() => {
          setMonthStart(startOfMonth(todayIso()));
          setSelected(todayIso());
        }}
        lockedBefore={editableFrom}
      />

      <p className="text-sm text-slate-600">
        Ce mois-ci :{' '}
        <span className="font-medium text-slate-900">
          {monthMissions.length} journée(s) de mission
        </span>{' '}
        · {formatMinutes(monthMinutes)} de travail rémunéré.
      </p>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="rounded-xl border border-slate-200 bg-white p-6 lg:col-span-2">
          <h2 className="text-lg font-semibold capitalize text-slate-900">
            {selected ? `${weekdayLabel(selected)} ${formatDate(selected)}` : 'Aucun jour sélectionné'}
          </h2>

          {selected && (
            <>
              <div className="mt-4 space-y-2">
                {selectedMissions.length === 0 && selectedUnavailabilities.length === 0 && (
                  <p className="text-sm text-slate-500">Rien de prévu ce jour-là.</p>
                )}

                {selectedMissions.map((entry) => (
                  <Link
                    key={`${entry.missionId}-${entry.startTime}`}
                    to={`/interimaire/missions/${entry.missionId}`}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-indigo-200 bg-indigo-50 px-4 py-3 transition hover:bg-indigo-100"
                  >
                    <span>
                      <span className="block font-medium text-indigo-900">
                        {scheduleLabel(entry)}
                      </span>
                      <span className="block text-sm text-indigo-700">{entry.workplace}</span>
                    </span>
                    <span className="text-right text-sm font-medium text-indigo-900">
                      {workedRanges(entry).map((range) => (
                        <span key={range} className="block">
                          {range}
                        </span>
                      ))}
                    </span>
                  </Link>
                ))}

                {selectedUnavailabilities.map((item) => (
                  <div
                    key={item.id}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3"
                  >
                    <span>
                      <span className="block font-medium text-amber-900">
                        Indisponible{' '}
                        {isFullDay(item)
                          ? 'toute la journée'
                          : `de ${shortTime(item.startTime)} à ${shortTime(item.endTime)}`}
                      </span>
                      {item.reason && <span className="block text-sm text-amber-800">{item.reason}</span>}
                    </span>
                    {item.editable ? (
                      <button type="button" className={btnDanger} onClick={() => setDeleting(item)}>
                        Supprimer
                      </button>
                    ) : (
                      <span className="text-xs text-amber-700">🔒 Non modifiable</span>
                    )}
                  </div>
                ))}
              </div>

              <div className="mt-6 border-t border-slate-100 pt-4">
                <h3 className="text-sm font-semibold text-slate-900">Déclarer une indisponibilité</h3>
                {!canEditSelected ? (
                  <p className="mt-2 rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-600">
                    🔒 Tes disponibilités ne sont modifiables qu'à partir du{' '}
                    {formatDate(editableFrom)} (délai de 8 jours).
                  </p>
                ) : selectedMissions.length > 0 ? (
                  <p className="mt-2 rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-600">
                    Une mission confirmée est planifiée ce jour-là : contacte l'agence si tu ne peux
                    pas l'assurer.
                  </p>
                ) : (
                  <form onSubmit={submitUnavailability} className="mt-3 space-y-3">
                    <label className="flex cursor-pointer items-center gap-2 text-sm text-slate-700">
                      <input
                        type="checkbox"
                        className={checkboxInput}
                        checked={fullDay}
                        onChange={(e) => setFullDay(e.target.checked)}
                      />
                      Toute la journée
                    </label>

                    {!fullDay && (
                      <div className="flex flex-wrap items-end gap-3">
                        <div>
                          <label className={labelClass} htmlFor="unavailability-start">
                            De
                          </label>
                          <input
                            id="unavailability-start"
                            type="time"
                            className={`${inputClass} w-32`}
                            value={startTime}
                            onChange={(e) => setStartTime(e.target.value)}
                          />
                        </div>
                        <div>
                          <label className={labelClass} htmlFor="unavailability-end">
                            À
                          </label>
                          <input
                            id="unavailability-end"
                            type="time"
                            className={`${inputClass} w-32`}
                            value={endTime}
                            onChange={(e) => setEndTime(e.target.value)}
                          />
                        </div>
                      </div>
                    )}

                    <div>
                      <label className={labelClass} htmlFor="unavailability-reason">
                        Motif (facultatif)
                      </label>
                      <input
                        id="unavailability-reason"
                        className={inputClass}
                        value={reason}
                        maxLength={150}
                        placeholder="Congé, rendez-vous, formation…"
                        onChange={(e) => setReason(e.target.value)}
                      />
                    </div>

                    <button type="submit" className={btnPrimary} disabled={saving}>
                      {saving ? 'Enregistrement…' : 'Ajouter'}
                    </button>
                  </form>
                )}
              </div>
            </>
          )}
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="text-lg font-semibold text-slate-900">Prochaines journées</h2>
          {upcoming.length === 0 && (
            <p className="mt-3 text-sm text-slate-500">Aucune journée de mission planifiée.</p>
          )}
          <ul className="mt-3 space-y-2">
            {upcoming.map((entry) => (
              <li key={`${entry.missionId}-${entry.date}`}>
                <button
                  type="button"
                  className="w-full rounded-lg border border-slate-200 px-3 py-2 text-left hover:border-indigo-300"
                  onClick={() => {
                    setMonthStart(startOfMonth(entry.date));
                    setSelected(entry.date);
                  }}
                >
                  <span className="block text-sm font-medium capitalize text-slate-900">
                    {weekdayLabel(entry.date)} {formatDate(entry.date)}
                  </span>
                  <span className="block truncate text-xs text-slate-500">
                    {scheduleLabel(entry)} · {shortTime(entry.startTime)}–{shortTime(entry.endTime)}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <ConfirmDialog
        open={deleting !== null}
        title="Supprimer l'indisponibilité"
        message={`L'indisponibilité du ${deleting ? formatDate(deleting.date) : ''} sera supprimée : tu redeviens disponible ce jour-là.`}
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setDeleting(null)}
      />
    </section>
  );
}
