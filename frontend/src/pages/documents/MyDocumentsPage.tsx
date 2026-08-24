import { useEffect, useState } from 'react';
import { openBlob } from '../../api/files';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/http';
import { downloadContract, getContractsToSignCount, getMyContracts } from '../../api/missions';
import { useAuth } from '../../auth/AuthContext';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import StatTile from '../../components/StatTile';
import StatusBadge, { type BadgeTone } from '../../components/StatusBadge';
import { btnPrimary, btnSecondary, errorBox } from '../../components/ui';
import { usePagedResource } from '../../hooks/usePagedResource';
import SignatureLine from '../../missions/SignatureLine';
import type { ContractSummary } from '../../missions/types';
import { formatDate, formatDateTime } from '../../profile/format';

/** État d'ensemble d'un contrat, du point de vue du lecteur. */
function overallState(contract: ContractSummary): { tone: BadgeTone; label: string } {
  if (contract.statusEmployer === 'SIGNED' && contract.statusWorker === 'SIGNED') {
    return { tone: 'success', label: 'Signé par les deux parties' };
  }
  if (contract.awaitingMySignature) {
    return { tone: 'warning', label: 'En attente de votre signature' };
  }
  return { tone: 'info', label: "En attente de l'autre partie" };
}

/**
 * Registre des contrats reçus par l'utilisateur, employeur ou intérimaire.
 *
 * <p>La signature elle-même reste sur la mission : c'est là que se trouve le contexte
 * (période, horaires, rémunération) sur lequel on s'engage. Cette page rassemble les
 * documents, leur date et l'état des deux signatures, et renvoie vers la mission.
 */
export default function MyDocumentsPage() {
  const { user } = useAuth();
  const isEmployer = user?.role === 'EMPLOYER';
  const missionsPath = isEmployer ? '/employeur/missions' : '/interimaire/missions';

  const { items, pageData, loading, error, setError, goTo } = usePagedResource(
    getMyContracts,
    'Impossible de charger vos documents.',
  );
  const [toSign, setToSign] = useState<number | null>(null);

  // Une page de dix ne dit rien du total : le nombre de signatures attendues vient
  // de son propre comptage. La page est remontée à chaque venue, le chiffre est donc
  // relu après une signature faite depuis la mission.
  useEffect(() => {
    getContractsToSignCount()
      .then(setToSign)
      .catch(() => setToSign(null));
  }, []);

  const open = async (contract: ContractSummary) => {
    setError(null);
    try {
      const blob = await downloadContract(contract.missionId);
      openBlob(blob);
    } catch (err) {
      setError(errorMessage(err, "Le document n'a pas pu être ouvert."));
    }
  };

  return (
    <section>
      <PageHeader
        title="Mes documents"
        subtitle="Les contrats que l'agence vous a adressés, avec l'état des signatures."
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <StatTile label="Documents reçus" value={pageData.totalElements} />
        <StatTile
          label="En attente de votre signature"
          value={toSign ?? '—'}
          hint={toSign ? 'Ouvrez la mission concernée pour signer.' : undefined}
          highlight={(toSign ?? 0) > 0}
        />
      </div>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <div className="mt-4 rounded-xl border border-line bg-surface p-6">
        {loading && <p className="text-sm text-muted">Chargement…</p>}

        {!loading && items.length === 0 && (
          <EmptyState
            title="Aucun document pour le moment"
            description={
              isEmployer
                ? "Un contrat est généré dès qu'un intérimaire accepte l'une de vos missions."
                : "Un contrat est généré dès que vous acceptez une mission proposée par l'agence."
            }
            action={
              <Link to={missionsPath} className={btnSecondary}>
                Voir mes missions
              </Link>
            }
          />
        )}

        <ul className="space-y-3">
          {items.map((contract) => {
            const state = overallState(contract);
            return (
              <li key={contract.id} className="rounded-lg border border-line p-4">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="min-w-0">
                    <p className="flex flex-wrap items-center gap-2 font-medium text-ink">
                      Contrat de mission — {contract.position}
                      <StatusBadge tone={state.tone}>{state.label}</StatusBadge>
                    </p>
                    <p className="mt-0.5 text-sm text-slate-500">
                      {isEmployer ? contract.workerName : contract.companyName} · du{' '}
                      {formatDate(contract.startDate)} au {formatDate(contract.endDate)}
                    </p>
                    <p className="mt-0.5 text-xs text-slate-400">
                      Établi le {formatDateTime(contract.generationTime)} · document n°{' '}
                      {contract.id}
                    </p>
                  </div>
                  <div className="flex shrink-0 flex-wrap items-center gap-2">
                    <button type="button" className={btnSecondary} onClick={() => open(contract)}>
                      📄 Consulter
                    </button>
                    <Link
                      to={`${missionsPath}/${contract.missionId}`}
                      className={contract.awaitingMySignature ? btnPrimary : btnSecondary}
                    >
                      {contract.awaitingMySignature ? 'Signer' : 'Voir la mission'}
                    </Link>
                  </div>
                </div>

                <ul className="mt-3 divide-y divide-slate-100 border-t border-slate-100 pt-1">
                  <SignatureLine
                    label="Entreprise utilisatrice"
                    signed={contract.statusEmployer === 'SIGNED'}
                    signedAt={contract.employerSignedAt}
                  />
                  <SignatureLine
                    label="Travailleur intérimaire"
                    signed={contract.statusWorker === 'SIGNED'}
                    signedAt={contract.workerSignedAt}
                  />
                </ul>
              </li>
            );
          })}
        </ul>

        <Pagination page={pageData} onChange={goTo} label="documents" />
      </div>
    </section>
  );
}
