import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/http';
import { downloadContract, getAdminMissions, refuseMission, validateMission } from '../../api/missions';
import ConfirmDialog from '../../components/ConfirmDialog';
import PromptDialog from '../../components/PromptDialog';
import { btnDanger, btnPrimary, btnSecondary, errorBox, linkBack } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import MissionFacts from '../../missions/MissionFacts';
import MissionListItem from '../../missions/MissionListItem';
import MissionSchedule from '../../missions/MissionSchedule';
import type { Mission } from '../../missions/types';
import { formatDate } from '../../profile/format';

/** Mission en attente, dépliée avec toutes les informations utiles à la décision. */
function PendingMissionCard({
  mission,
  onValidate,
  onRefuse,
}: {
  mission: Mission;
  onValidate: () => void;
  onRefuse: () => void;
}) {
  return (
    <li className="rounded-lg border border-slate-200 p-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="flex flex-wrap items-center gap-2 font-medium text-slate-900">
            {mission.position}
            {mission.renewal && (
              <span className="rounded-full bg-violet-100 px-2.5 py-0.5 text-xs font-medium text-violet-800">
                Renouvellement
              </span>
            )}
          </p>
          <p className="mt-0.5 text-sm text-slate-600">
            {mission.employerCompanyName ?? '—'} → {mission.candidateFirstName}{' '}
            {mission.candidateLastName}
          </p>
          <p className="text-xs text-slate-400">
            Offre « {mission.offerTitle} » · candidature #{mission.applicationId}
          </p>
          {mission.renewal && mission.previousStartDate && (
            <p className="mt-1 text-xs text-violet-700">
              Renouvelle la mission du {formatDate(mission.previousStartDate)} au{' '}
              {formatDate(mission.previousEndDate)} — déjà acceptée par l'intérimaire.
            </p>
          )}
        </div>
        <div className="flex shrink-0 flex-wrap gap-2">
          <button type="button" className={btnPrimary} onClick={onValidate}>
            Valider
          </button>
          <button type="button" className={btnDanger} onClick={onRefuse}>
            Refuser
          </button>
        </div>
      </div>

      <div className="mt-4 border-t border-slate-100 pt-4">
        <MissionFacts mission={mission} />
      </div>
      <div className="mt-4 border-t border-slate-100 pt-4">
        <MissionSchedule slots={mission.slots} />
      </div>
    </li>
  );
}

/** Référence stable pour la liste vide, comme l'état initial `[]` d'avant. */
const NO_MISSIONS: Mission[] = [];

/** Validation des missions provisoires par l'agence (FR14). */
export default function AdminMissionsPage() {
  const [validating, setValidating] = useState<Mission | null>(null);
  const [refusing, setRefusing] = useState<Mission | null>(null);

  const { data, loading, error, setError, reload } = useResource(
    getAdminMissions,
    'Impossible de charger les missions.',
  );
  const missions = data ?? NO_MISSIONS;

  const pending = useMemo(() => missions.filter((m) => m.status === 'PENDING'), [missions]);
  const others = useMemo(() => missions.filter((m) => m.status !== 'PENDING'), [missions]);

  const confirmValidate = async () => {
    if (!validating) {
      return;
    }
    const id = validating.id;
    setValidating(null);
    setError(null);
    try {
      await validateMission(id);
      reload();
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  /** Ouvre le document du contrat dans un nouvel onglet (l'agence le consulte sans le signer). */
  const openContract = async (missionId: number) => {
    setError(null);
    try {
      const blob = await downloadContract(missionId);
      window.open(URL.createObjectURL(blob), '_blank', 'noopener');
    } catch (err) {
      setError(errorMessage(err, 'Le contrat n’a pas pu être ouvert.'));
    }
  };

  const confirmRefuse = async (reason: string) => {
    if (!refusing) {
      return;
    }
    const id = refusing.id;
    setRefusing(null);
    setError(null);
    try {
      await refuseMission(id, reason);
      reload();
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  return (
    <section className="space-y-6">
      <div>
        <Link to="/admin" className={linkBack}>
          ← Retour à l'espace administrateur
        </Link>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">Missions d'intérim</h1>
        <p className="mt-1 text-slate-600">
          Validez les missions proposées par les employeurs : une mission validée est ensuite
          soumise à l'intérimaire, qui déclenche la génération du contrat en l'acceptant.
        </p>
      </div>

      {error && <p className={errorBox}>{error}</p>}

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">
          À valider {pending.length > 0 && <span className="text-slate-400">({pending.length})</span>}
        </h2>
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && pending.length === 0 && (
          <p className="text-sm text-slate-500">Aucune mission en attente de validation.</p>
        )}
        <ul className="space-y-4">
          {pending.map((mission) => (
            <PendingMissionCard
              key={mission.id}
              mission={mission}
              onValidate={() => setValidating(mission)}
              onRefuse={() => setRefusing(mission)}
            />
          ))}
        </ul>
      </div>

      {others.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Suivi</h2>
          <ul className="space-y-3">
            {others.map((mission) => (
              <MissionListItem
                key={mission.id}
                mission={mission}
                subtitle={
                  <>
                    {mission.employerCompanyName ?? '—'} → {mission.candidateFirstName}{' '}
                    {mission.candidateLastName}
                  </>
                }
              >
                {mission.contract && (
                  <>
                    <span className="text-xs text-slate-500">
                      Contrat n° {mission.contract.id} ·{' '}
                      {mission.contract.statusEmployer === 'SIGNED' &&
                      mission.contract.statusWorker === 'SIGNED'
                        ? 'signé par les deux parties'
                        : 'signatures en attente'}
                    </span>
                    <button
                      type="button"
                      className={btnSecondary}
                      onClick={() => openContract(mission.id)}
                    >
                      📄 Contrat
                    </button>
                  </>
                )}
              </MissionListItem>
            ))}
          </ul>
        </div>
      )}

      <ConfirmDialog
        open={validating !== null}
        title="Valider la mission"
        message={`La mission « ${validating?.position} » sera proposée à ${validating?.candidateFirstName} ${validating?.candidateLastName}, qui devra l'accepter.`}
        confirmLabel="Valider"
        onConfirm={confirmValidate}
        onCancel={() => setValidating(null)}
      />

      <PromptDialog
        open={refusing !== null}
        title="Refuser la mission"
        message={`L'employeur ${refusing?.employerCompanyName ?? ''} pourra corriger la mission et la soumettre à nouveau.`}
        label="Motif du refus (obligatoire)"
        placeholder="Ex. : le salaire horaire est inférieur au barème du secteur."
        confirmLabel="Refuser"
        onConfirm={confirmRefuse}
        onCancel={() => setRefusing(null)}
      />
    </section>
  );
}
