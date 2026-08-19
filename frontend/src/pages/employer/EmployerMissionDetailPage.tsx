import { useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getEmployerMission } from '../../api/missions';
import { btnPrimary, btnSecondary, errorBox, linkBack } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import ContractPanel from '../../missions/ContractPanel';
import MissionFacts from '../../missions/MissionFacts';
import MissionSchedule from '../../missions/MissionSchedule';
import MissionStatusBadge from '../../missions/MissionStatusBadge';
import type { Mission } from '../../missions/types';
import { formatDate } from '../../profile/format';

/** Message d'étape affiché à l'employeur selon l'avancement de la mission. */
function StatusBanner({ mission }: { mission: Mission }) {
  const banners: Partial<Record<Mission['status'], { tone: string; text: string }>> = {
    PENDING: {
      tone: 'border-amber-200 bg-amber-50 text-amber-900',
      text: "La mission a été transmise à l'agence, qui doit la valider avant de la proposer à l'intérimaire.",
    },
    REFUSED: {
      tone: 'border-red-200 bg-red-50 text-red-900',
      text: "L'agence a refusé cette mission. Corrigez-la et renvoyez-la pour validation.",
    },
    APPROVED: {
      tone: 'border-sky-200 bg-sky-50 text-sky-900',
      text: "L'agence a validé la mission : l'intérimaire doit maintenant l'accepter.",
    },
    RENEWAL: {
      tone: 'border-violet-200 bg-violet-50 text-violet-900',
      text: "La demande de renouvellement attend la réponse de l'intérimaire.",
    },
    ACTIVE: {
      tone: 'border-green-200 bg-green-50 text-green-900',
      text: "L'intérimaire a accepté : la mission est confirmée et le contrat est disponible.",
    },
    DECLINED: {
      tone: 'border-red-200 bg-red-50 text-red-900',
      text: "L'intérimaire a refusé cette mission. Vous pouvez proposer une nouvelle mission à un autre candidat.",
    },
  };
  const banner = banners[mission.status];
  if (!banner) {
    return null;
  }
  return <p className={`rounded-xl border px-4 py-3 text-sm ${banner.tone}`}>{banner.text}</p>;
}

/** Détail d'une mission côté employeur : conditions, horaire, contrat. */
export default function EmployerMissionDetailPage() {
  const { id } = useParams();
  const missionId = Number(id);
  const navigate = useNavigate();

  const fetcher = useCallback(() => getEmployerMission(missionId), [missionId]);
  const {
    data: mission,
    setData: setMission,
    loading,
    error,
  } = useResource(fetcher, 'Impossible de charger la mission.');

  if (loading) {
    return <p className="text-slate-500">Chargement…</p>;
  }

  if (!mission) {
    return <p className={errorBox}>{error ?? 'Mission introuvable.'}</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <button
          type="button"
          onClick={() => navigate('/employeur/missions')}
          className={linkBack}
        >
          ← Retour à mes missions
        </button>
        <div className="mt-2 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">{mission.position}</h1>
            <p className="mt-1 text-slate-600">
              {mission.candidateFirstName} {mission.candidateLastName} · {mission.candidateEmail}
            </p>
            <p className="text-sm text-slate-500">Offre « {mission.offerTitle} »</p>
            {mission.renewal && mission.previousStartDate && (
              <p className="mt-1 text-sm text-violet-700">
                Renouvellement de la mission du {formatDate(mission.previousStartDate)} au{' '}
                {formatDate(mission.previousEndDate)}.
              </p>
            )}
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-2">
            <MissionStatusBadge status={mission.status} renewal={mission.renewal} />
            {mission.status === 'REFUSED' && (
              <Link to={`/employeur/missions/${mission.id}/corriger`} className={btnSecondary}>
                Corriger la mission
              </Link>
            )}
            {mission.status === 'ACTIVE' && (
              <Link to={`/employeur/missions/${mission.id}/renouveler`} className={btnPrimary}>
                Renouveler la mission
              </Link>
            )}
          </div>
        </div>
      </div>

      {error && <p className={errorBox}>{error}</p>}

      <StatusBanner mission={mission} />

      {mission.status === 'REFUSED' && mission.refusalReason && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4">
          <p className="text-sm font-medium text-red-900">Motif du refus de l'agence</p>
          <p className="mt-1 whitespace-pre-line text-sm text-red-800">{mission.refusalReason}</p>
        </div>
      )}

      <section className="rounded-xl border border-line bg-surface p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Conditions</h2>
        <MissionFacts mission={mission} />
      </section>

      <section className="rounded-xl border border-line bg-surface p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Horaire</h2>
        <MissionSchedule slots={mission.slots} />
      </section>

      <ContractPanel
        mission={mission}
        party="employer"
        onSigned={(contract) => setMission({ ...mission, contract })}
      />
    </div>
  );
}
