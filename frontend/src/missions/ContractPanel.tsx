import { useState } from 'react';
import { downloadContract, signContract } from '../api/missions';
import { btnPrimary, btnSecondary, errorBox } from '../components/ui';
import { formatDateTime } from '../profile/format';
import type { Contract, Mission } from './types';

type Party = 'employer' | 'worker' | 'agency';

function SignatureLine({ label, signed }: { label: string; signed: boolean }) {
  return (
    <li className="flex items-center justify-between gap-4 py-1.5">
      <span className="text-sm text-slate-700">{label}</span>
      <span
        className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
          signed ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-600'
        }`}
      >
        {signed ? 'Signé' : 'En attente de signature'}
      </span>
    </li>
  );
}

/**
 * Contrat d'une mission confirmée : consultation du document et signature simulée
 * de la partie connectée. L'agence peut consulter sans signer.
 */
export default function ContractPanel({
  mission,
  party,
  onSigned,
}: {
  mission: Mission;
  party: Party;
  onSigned?: (contract: Contract) => void;
}) {
  const [contract, setContract] = useState<Contract | null>(mission.contract);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!contract) {
    return null;
  }

  const alreadySigned =
    party === 'employer' ? contract.statusEmployer === 'SIGNED' : contract.statusWorker === 'SIGNED';

  const open = async () => {
    setError(null);
    try {
      const blob = await downloadContract(mission.id);
      window.open(URL.createObjectURL(blob), '_blank', 'noopener');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Le contrat n’a pas pu être ouvert.');
    }
  };

  const sign = async () => {
    setBusy(true);
    setError(null);
    try {
      const signed = await signContract(mission.id);
      setContract(signed);
      onSigned?.(signed);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'La signature a échoué.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Contrat</h2>
          <p className="mt-1 text-sm text-slate-500">
            Généré le {formatDateTime(contract.generationTime)} · envoi simulé par l'agence.
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2">
          <button type="button" className={btnSecondary} onClick={open}>
            📄 Consulter le contrat
          </button>
          {party !== 'agency' && !alreadySigned && (
            <button type="button" className={btnPrimary} onClick={sign} disabled={busy}>
              {busy ? 'Signature…' : 'Signer le contrat'}
            </button>
          )}
        </div>
      </div>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <ul className="mt-4 divide-y divide-slate-100 border-t border-slate-100 pt-2">
        <SignatureLine label="Entreprise utilisatrice" signed={contract.statusEmployer === 'SIGNED'} />
        <SignatureLine label="Travailleur intérimaire" signed={contract.statusWorker === 'SIGNED'} />
      </ul>
    </section>
  );
}
