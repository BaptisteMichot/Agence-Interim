import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { acceptMission, declineMission, getMyMission } from '../../api/missions';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnPrimary, errorBox } from '../../components/ui';
import ContractPanel from '../../missions/ContractPanel';
import MissionFacts from '../../missions/MissionFacts';
import MissionSchedule from '../../missions/MissionSchedule';
import MissionStatusBadge from '../../missions/MissionStatusBadge';
import type { Mission } from '../../missions/types';
import { formatDate } from '../../profile/format';

/**
 * Mission proposée à l'intérimaire : toutes les conditions du contrat, puis
 * acceptation (la mission passe au planning et le contrat est généré) ou refus.
 */
export default function MissionDetailPage() {
  const { id } = useParams();
  const missionId = Number(id);
  const navigate = useNavigate();

  const [mission, setMission] = useState<Mission | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [declining, setDeclining] = useState(false);

  const reload = useCallback(async () => {
    setError(null);
    try {
      setMission(await getMyMission(missionId));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger la mission.');
    } finally {
      setLoading(false);
    }
  }, [missionId]);

  useEffect(() => {
    reload();
  }, [reload]);

  const respond = async (accept: boolean) => {
    setBusy(true);
    setError(null);
    try {
      setMission(accept ? await acceptMission(missionId) : await declineMission(missionId));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <p className="text-slate-500">Chargement…</p>;
  }

  if (!mission) {
    return <p className={errorBox}>{error ?? 'Mission introuvable.'}</p>;
  }

  const awaitingAnswer = mission.status === 'APPROVED' || mission.status === 'RENEWAL';
  const company =
    mission.employerCompanyName ?? `${mission.employerFirstName} ${mission.employerLastName}`;

  return (
    <div className="space-y-6">
      <div>
        <button
          type="button"
          onClick={() => navigate('/interimaire/missions')}
          className="text-sm text-indigo-600 hover:underline"
        >
          ← Retour à mes missions
        </button>
        <div className="mt-2 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">{mission.position}</h1>
            <p className="mt-1 text-slate-600">
              {company} · {mission.workplace}
            </p>
            <p className="text-sm text-slate-500">
              Suite à ta candidature à l'offre « {mission.offerTitle} »
            </p>
          </div>
          <MissionStatusBadge status={mission.status} renewal={mission.renewal} />
        </div>
      </div>

      {error && <p className={errorBox}>{error}</p>}

      {awaitingAnswer && (
        <div className="rounded-xl border border-sky-200 bg-sky-50 p-6">
          <h2 className="text-lg font-semibold text-sky-900">
            {mission.status === 'RENEWAL'
              ? 'Renouvellement de mission proposé'
              : 'Cette mission t’est proposée'}
          </h2>
          <p className="mt-1 text-sm text-sky-800">
            L'agence a validé les conditions ci-dessous. En acceptant, la mission est ajoutée à ton
            planning et ton contrat est généré.
            {mission.renewal && mission.previousStartDate && (
              <>
                {' '}
                Il s'agit du renouvellement de ta mission du {formatDate(mission.previousStartDate)}{' '}
                au {formatDate(mission.previousEndDate)}.
              </>
            )}
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              className={btnPrimary}
              disabled={busy}
              onClick={() => respond(true)}
            >
              {busy ? 'Envoi…' : 'Accepter la mission'}
            </button>
            <button
              type="button"
              className={btnDanger}
              disabled={busy}
              onClick={() => setDeclining(true)}
            >
              Refuser
            </button>
          </div>
        </div>
      )}

      {mission.status === 'PENDING' && (
        <p className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          Tu as accepté ce renouvellement : l'agence doit encore le valider avant de générer le
          contrat.
        </p>
      )}

      {mission.status === 'DECLINED' && (
        <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-900">
          Tu as refusé cette mission.
        </p>
      )}

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Conditions proposées</h2>
        <MissionFacts mission={mission} />
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Horaire</h2>
        <MissionSchedule slots={mission.slots} />
      </section>

      <ContractPanel
        mission={mission}
        party="worker"
        onSigned={(contract) => setMission({ ...mission, contract })}
      />

      <ConfirmDialog
        open={declining}
        title="Refuser la mission"
        message="Cette mission ne te sera plus proposée et l'employeur en sera informé."
        confirmLabel="Refuser"
        onConfirm={() => {
          setDeclining(false);
          respond(false);
        }}
        onCancel={() => setDeclining(false)}
      />
    </div>
  );
}
