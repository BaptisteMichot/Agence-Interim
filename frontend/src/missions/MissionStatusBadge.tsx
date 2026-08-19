import { MISSION_STATUS } from './format';
import type { MissionStatus } from './types';

/** Pastille de statut d'une mission, avec l'étiquette « renouvellement » si besoin. */
export default function MissionStatusBadge({
  status,
  renewal = false,
  finished = false,
}: {
  status: MissionStatus;
  renewal?: boolean;
  /** Mission confirmée dont la période est écoulée : elle n'est plus « en cours ». */
  finished?: boolean;
}) {
  const { label, className } = finished
    ? { label: 'Terminée', className: 'bg-slate-100 text-slate-700' }
    : MISSION_STATUS[status];
  return (
    <span className="inline-flex flex-wrap items-center gap-2">
      <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${className}`}>{label}</span>
      {renewal && (
        <span className="rounded-full bg-violet-100 px-2.5 py-0.5 text-xs font-medium text-violet-800">
          Renouvellement
        </span>
      )}
    </span>
  );
}
