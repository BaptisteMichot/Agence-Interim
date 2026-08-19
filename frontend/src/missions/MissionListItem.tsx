import { formatDate } from '../profile/format';
import { formatMinutes, missionPeriod, totalPaidMinutes } from './format';
import MissionStatusBadge from './MissionStatusBadge';
import type { Mission } from './types';

/** Ligne de liste commune aux trois portails : identité de la mission + actions. */
export default function MissionListItem({
  mission,
  subtitle,
  className = '',
  showStatus = true,
  children,
}: {
  mission: Mission;
  subtitle: React.ReactNode;
  className?: string;
  /** Masqué là où la liste porte déjà l'état des missions qu'elle regroupe. */
  showStatus?: boolean;
  children?: React.ReactNode;
}) {
  return (
    <li className={`rounded-lg border border-line p-4 ${className}`}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="flex flex-wrap items-center gap-2 font-medium text-slate-900">
            {mission.position}
            {showStatus && (
              <MissionStatusBadge
                status={mission.status}
                renewal={mission.renewal}
                finished={mission.status === 'ACTIVE' && missionPeriod(mission) === 'past'}
              />
            )}
          </p>
          <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>
          <p className="mt-0.5 text-sm text-slate-600">
            Du {formatDate(mission.startDate)} au {formatDate(mission.endDate)} ·{' '}
            {mission.slots.length} journée(s) · {formatMinutes(totalPaidMinutes(mission.slots))}{' '}
            payées ·{' '}
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
