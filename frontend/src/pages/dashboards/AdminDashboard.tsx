import { useMemo, useState } from 'react';
import {
  acceptEmployerRequest,
  getEmployerRequests,
  refuseEmployerRequest,
  type AdminEmployerRequest,
} from '../../api/employer';
import { errorMessage } from '../../api/http';
import ConfirmDialog from '../../components/ConfirmDialog';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import { SkeletonRows } from '../../components/Skeleton';
import StatusBadge from '../../components/StatusBadge';
import {
  btnDanger,
  btnPrimary,
  card,
  checkboxInput,
  checkboxRow,
  errorBox,
  sectionTitle,
} from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { formatDate } from '../../profile/format';

type PendingAction = { id: number; company: string; action: 'accept' | 'refuse' };

const STATUS_LABEL: Record<string, string> = {
  ACCEPTED: 'Acceptée',
  REFUSED: 'Refusée',
};

/** Référence stable pour la liste vide, comme l'état initial `[]` d'avant. */
const NO_REQUESTS: AdminEmployerRequest[] = [];

export default function AdminDashboard() {
  const [pending, setPending] = useState<PendingAction | null>(null);
  /** Refus définitif : coché, l'utilisateur ne pourra plus soumettre de demande. */
  const [blockReapply, setBlockReapply] = useState(false);

  const { data, loading, error, setError, reload } = useResource(
    getEmployerRequests,
    'Impossible de charger les demandes.',
  );
  const requests = data ?? NO_REQUESTS;

  const waiting = useMemo(() => requests.filter((r) => r.status === 'PENDING'), [requests]);
  const history = useMemo(
    () => requests.filter((r) => r.status !== 'PENDING').reverse(),
    [requests],
  );

  const confirm = async () => {
    if (!pending) {
      return;
    }
    const { id, action } = pending;
    const block = blockReapply;
    setPending(null);
    setError(null);
    try {
      await (action === 'accept' ? acceptEmployerRequest(id) : refuseEmployerRequest(id, block));
      reload();
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  return (
    <>
      <PageHeader
        title="Espace administrateur"
        subtitle="Traitement des demandes d'accès employeur."
      />

      {error && <p className={`mb-6 ${errorBox}`}>{error}</p>}

      <section className={card}>
        <h2 className={`mb-4 ${sectionTitle}`}>Demandes d'accès en attente</h2>
        {loading && <SkeletonRows rows={2} />}
        {!loading && waiting.length === 0 && (
          <EmptyState
            title="Aucune demande en attente"
            description="Les demandes d'accès employeur apparaîtront ici dès qu'un utilisateur en formulera une."
          />
        )}
        <ul className="space-y-3">
          {waiting.map((request) => (
            <RequestCard key={request.id} request={request}>
              <button
                type="button"
                className={btnPrimary}
                onClick={() =>
                  setPending({ id: request.id, company: request.companyName, action: 'accept' })
                }
              >
                Accepter
              </button>
              <button
                type="button"
                className={btnDanger}
                onClick={() => {
                  setBlockReapply(false);
                  setPending({ id: request.id, company: request.companyName, action: 'refuse' });
                }}
              >
                Refuser
              </button>
            </RequestCard>
          ))}
        </ul>
      </section>

      {history.length > 0 && (
        <section className={`mt-6 ${card}`}>
          <h2 className={`mb-4 ${sectionTitle}`}>Historique</h2>
          <ul className="space-y-3">
            {history.map((request) => (
              <RequestCard key={request.id} request={request}>
                <StatusBadge tone={request.status === 'ACCEPTED' ? 'success' : 'danger'}>
                  {STATUS_LABEL[request.status]}
                </StatusBadge>
              </RequestCard>
            ))}
          </ul>
        </section>
      )}

      <ConfirmDialog
        open={pending !== null}
        title={pending?.action === 'accept' ? 'Accepter la demande' : 'Refuser la demande'}
        message={
          pending?.action === 'accept'
            ? `Accorder le rôle employeur pour « ${pending?.company} » ?`
            : `Refuser la demande de « ${pending?.company} » ?`
        }
        confirmLabel={pending?.action === 'accept' ? 'Accepter' : 'Refuser'}
        onConfirm={confirm}
        onCancel={() => setPending(null)}
      >
        {pending?.action === 'refuse' && (
          <label className={checkboxRow}>
            <input
              type="checkbox"
              checked={blockReapply}
              onChange={(event) => setBlockReapply(event.target.checked)}
              className={checkboxInput}
            />
            Interdire toute nouvelle demande
          </label>
        )}
      </ConfirmDialog>
    </>
  );
}

function RequestCard({
  request,
  children,
}: {
  request: AdminEmployerRequest;
  children: React.ReactNode;
}) {
  const [showMessage, setShowMessage] = useState(false);

  return (
    <li className="rounded-lg border border-line p-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="font-medium text-ink">
            {request.companyName}
            {request.resubmission && (
              <span className="ml-2">
                <StatusBadge tone="warning">Nouvelle demande après refus</StatusBadge>
              </span>
            )}
          </p>
          <p className="text-sm text-muted">
            {request.firstName} {request.lastName} · {request.email}
          </p>
          <p className="text-xs text-slate-400">Demande du {formatDate(request.requestDate)}</p>
          {request.message && (
            <button
              type="button"
              className="mt-1 text-xs font-medium text-brand-600 hover:underline"
              onClick={() => setShowMessage((v) => !v)}
            >
              {showMessage ? 'Masquer le message' : 'Voir le message'}
            </button>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">{children}</div>
      </div>
      {showMessage && request.message && (
        <p className="mt-3 rounded-md bg-slate-50 p-3 text-sm text-slate-700">{request.message}</p>
      )}
    </li>
  );
}
