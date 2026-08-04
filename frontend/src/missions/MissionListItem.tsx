import { formatDate } from '../profile/format';
import { formatMinutes, totalMinutes } from './format';
import MissionStatusBadge from './MissionStatusBadge';
import type { Mission } from './types';

/** Ligne de liste commune aux trois portails : identité de la mission + actions. */
export default function MissionListItem({
  mission,
  subtitle,
  className = '',
  children,
}: {
  mission: Mission;
  subtitle: React.ReactNode;
  className?: string;
  children?: React.ReactNode;
}) {
  return (
    <li className={`rounded-lg border border-slate-200 p-4 ${className}`}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="flex flex-wrap items-center gap-2 font-medium text-slate-900">
            {mission.position}
            <MissionStatusBadge status={mission.status} renewal={mission.renewal} />
          </p>
          <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>
          <p className="mt-0.5 text-sm text-slate-600">
            Du {formatDate(mission.startDate)} au {formatDate(mission.endDate)} ·{' '}
            {mission.slots.length} journée(s) · {formatMinutes(totalMinutes(mission.slots))} ·{' '}
            {mission.hourlyWage} €/h
          </p>
          {mission.status === 'REFUSED' && mission.refusalReason && (
            <p className="mt-2 rounded-md bg-red-50 px-3 py-2 text-sm text-red-800">
              Motif de l'agence : {mission.refusalReason}
            </p>
          )}
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-2">{children}</div>
      </div>
    </li>
  );
}
