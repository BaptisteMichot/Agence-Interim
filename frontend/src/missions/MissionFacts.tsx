import { formatDate } from '../profile/format';
import { estimatedPay, formatMinutes, totalPaidMinutes, workReasonLabel } from './format';
import type { Mission } from './types';

function Fact({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-slate-400">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium text-slate-900">{value}</dd>
    </div>
  );
}

/** Conditions de la mission, telles qu'elles figureront sur le contrat. */
export default function MissionFacts({ mission }: { mission: Mission }) {
  const minutes = totalPaidMinutes(mission.slots);
  return (
    <div className="space-y-4">
      <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Fact label="Poste" value={mission.position} />
        <Fact label="Lieu de travail" value={mission.workplace} />
        <Fact label="Motif de recours" value={workReasonLabel(mission.workReason)} />
        <Fact
          label="Période"
          value={`du ${formatDate(mission.startDate)} au ${formatDate(mission.endDate)}`}
        />
        <Fact label="Salaire horaire" value={`${mission.hourlyWage} €/h brut`} />
        <Fact
          label="Volume rémunéré"
          value={`${formatMinutes(minutes)} · ${estimatedPay(minutes, mission.hourlyWage)} brut`}
        />
      </dl>
      {mission.notes && (
        <div>
          <p className="text-xs uppercase tracking-wide text-slate-400">Conditions particulières</p>
          <p className="mt-1 whitespace-pre-line text-sm text-slate-700">{mission.notes}</p>
        </div>
      )}
    </div>
  );
}
