import {
  formatMinutes,
  isWeekend,
  monthLabel,
  totalPaidMinutes,
  weekdayLabel,
  workedRanges,
} from './format';
import type { DailySlot } from './types';

/** Regroupe les journées par mois, dans l'ordre chronologique. */
function groupByMonth(slots: DailySlot[]): { month: string; days: DailySlot[] }[] {
  const groups: { month: string; days: DailySlot[] }[] = [];
  for (const slot of [...slots].sort((a, b) => a.date.localeCompare(b.date))) {
    const month = monthLabel(slot.date);
    const last = groups[groups.length - 1];
    if (last && last.month === month) {
      last.days.push(slot);
    } else {
      groups.push({ month, days: [slot] });
    }
  }
  return groups;
}

/** Journées travaillées d'une mission, en grille compacte groupée par mois. */
export default function MissionSchedule({ slots }: { slots: DailySlot[] }) {
  if (slots.length === 0) {
    return <p className="text-sm text-slate-500">Aucune journée planifiée.</p>;
  }

  return (
    <div className="space-y-5">
      {groupByMonth(slots).map((group) => (
        <div key={group.month}>
          <p className="mb-2 text-sm font-semibold capitalize text-slate-700">{group.month}</p>
          <ul className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
            {group.days.map((slot) => (
              <li
                key={slot.id}
                className={`rounded-lg border px-2 py-2 text-center ${
                  isWeekend(slot.date)
                    ? 'border-amber-200 bg-amber-50'
                    : 'border-slate-200 bg-slate-50'
                }`}
              >
                <p className="text-xs uppercase tracking-wide text-slate-400">
                  {weekdayLabel(slot.date)}
                </p>
                <p className="text-lg font-semibold leading-tight text-slate-900">
                  {slot.date.slice(8, 10)}
                </p>
                {workedRanges(slot).map((range) => (
                  <p key={range} className="text-xs text-slate-600">
                    {range}
                  </p>
                ))}
              </li>
            ))}
          </ul>
        </div>
      ))}
      <p className="text-sm text-slate-600">
        <span className="font-medium text-slate-900">{slots.length} journée(s)</span> ·{' '}
        {formatMinutes(totalPaidMinutes(slots))} rémunérées au total (pauses déduites)
      </p>
    </div>
  );
}
