import {
  isFullDay,
  isWeekend,
  monthGrid,
  monthLabel,
  scheduleLabel,
  shortTime,
  todayIso,
  WEEKDAYS,
} from './format';
import type { ScheduleEntry, Unavailability } from './types';

interface MonthCalendarProps {
  /** Premier jour du mois affiché (yyyy-MM-01). */
  monthStart: string;
  missions: ScheduleEntry[];
  unavailabilities: Unavailability[];
  selected: string | null;
  onSelect: (date: string) => void;
  /** Décalage du mois affiché (-1 = précédent, +1 = suivant). */
  onMonthShift: (delta: number) => void;
  onToday: () => void;
  /** Avant cette date, les disponibilités ne sont plus modifiables (J+8). */
  lockedBefore: string;
}

/** Regroupe une liste par date ISO. */
function byDate<T extends { date: string }>(items: T[]): Map<string, T[]> {
  const map = new Map<string, T[]>();
  for (const item of items) {
    const list = map.get(item.date);
    if (list) {
      list.push(item);
    } else {
      map.set(item.date, [item]);
    }
  }
  return map;
}

/**
 * Calendrier mensuel du planning : une pastille par journée de mission confirmée
 * et par indisponibilité déclarée. Le jour sélectionné est détaillé sous la grille.
 */
export default function MonthCalendar({
  monthStart,
  missions,
  unavailabilities,
  selected,
  onSelect,
  onMonthShift,
  onToday,
  lockedBefore,
}: MonthCalendarProps) {
  const today = todayIso();
  const days = monthGrid(monthStart);
  const missionsByDate = byDate(missions);
  const unavailabilitiesByDate = byDate(unavailabilities);
  const currentMonth = monthStart.slice(0, 7);

  return (
    <div className="rounded-xl border border-slate-200 bg-white">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
        <div className="flex items-center gap-2">
          <button
            type="button"
            aria-label="Mois précédent"
            className="rounded-md border border-slate-300 px-2.5 py-1 text-slate-600 hover:bg-slate-100"
            onClick={() => onMonthShift(-1)}
          >
            ‹
          </button>
          <p className="min-w-44 text-center text-lg font-semibold capitalize text-slate-900">
            {monthLabel(monthStart)}
          </p>
          <button
            type="button"
            aria-label="Mois suivant"
            className="rounded-md border border-slate-300 px-2.5 py-1 text-slate-600 hover:bg-slate-100"
            onClick={() => onMonthShift(1)}
          >
            ›
          </button>
          <button
            type="button"
            className="ml-1 rounded-md border border-slate-300 px-3 py-1 text-sm font-medium text-slate-700 hover:bg-slate-100"
            onClick={onToday}
          >
            Aujourd'hui
          </button>
        </div>
        <ul className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
          <li className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-indigo-500" /> Mission confirmée
          </li>
          <li className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-amber-400" /> Indisponibilité
          </li>
          <li className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm bg-slate-200" /> Verrouillé (J+8)
          </li>
        </ul>
      </div>

      <div className="overflow-x-auto p-4">
        <div className="min-w-[40rem]">
          <div className="grid grid-cols-7 gap-1 pb-1">
            {WEEKDAYS.map((weekday) => (
              <div
                key={weekday}
                className="text-center text-xs font-semibold uppercase tracking-wide text-slate-400"
              >
                {weekday}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-1">
            {days.map((date) => {
              const inMonth = date.slice(0, 7) === currentMonth;
              const dayMissions = missionsByDate.get(date) ?? [];
              const dayUnavailabilities = unavailabilitiesByDate.get(date) ?? [];
              const locked = date < lockedBefore;
              const isToday = date === today;
              return (
                <button
                  type="button"
                  key={date}
                  onClick={() => onSelect(date)}
                  aria-pressed={selected === date}
                  className={`flex min-h-24 flex-col gap-1 rounded-lg border p-1.5 text-left transition ${
                    selected === date
                      ? 'border-indigo-500 ring-2 ring-indigo-200'
                      : 'border-slate-200 hover:border-indigo-300'
                  } ${!inMonth ? 'bg-slate-50/60 opacity-60' : locked ? 'bg-slate-50' : 'bg-white'}`}
                >
                  <span className="flex items-center justify-between">
                    <span
                      className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold ${
                        isToday
                          ? 'bg-indigo-600 text-white'
                          : isWeekend(date)
                            ? 'text-amber-700'
                            : 'text-slate-700'
                      }`}
                    >
                      {Number(date.slice(8, 10))}
                    </span>
                    {locked && inMonth && (
                      <span className="text-[10px] text-slate-400" title="Modifiable à partir de J+8">
                        🔒
                      </span>
                    )}
                  </span>

                  {dayMissions.map((entry) => (
                    <span
                      key={`${entry.missionId}-${entry.startTime}`}
                      className="rounded bg-indigo-500 px-1.5 py-0.5 text-[11px] font-medium text-white"
                      title={`${scheduleLabel(entry)} (${shortTime(entry.startTime)}–${shortTime(entry.endTime)})`}
                    >
                      <span className="block">{shortTime(entry.startTime)}</span>
                      <span className="block truncate">{scheduleLabel(entry)}</span>
                    </span>
                  ))}

                  {dayUnavailabilities.map((item) => (
                    <span
                      key={item.id}
                      className="truncate rounded bg-amber-100 px-1.5 py-0.5 text-[11px] font-medium text-amber-800"
                      title={item.reason ?? 'Indisponible'}
                    >
                      {isFullDay(item) ? 'Indisponible' : `${shortTime(item.startTime)} indispo.`}
                    </span>
                  ))}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
