import { useState } from 'react';
import { errorMessage } from '../api/http';
import { downloadContract, requestSigningCode, signContract } from '../api/missions';
import { btnPrimary, btnSecondary, errorBox, inputClass, labelClass } from '../components/ui';
import { formatDateTime } from '../profile/format';
import SignatureLine from './SignatureLine';
import type { Contract, Mission } from './types';

type Party = 'employer' | 'worker';

/**
 * Contrat d'une mission confirmée : consultation du document et signature de la
 * partie connectée (employeur ou intérimaire), confirmée par un code reçu par email.
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
  const [codeSent, setCodeSent] = useState(false);
  const [code, setCode] = useState('');

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
      setError(errorMessage(err, 'Le contrat n’a pas pu être ouvert.'));
    }
  };

  const sendCode = async () => {
    setBusy(true);
    setError(null);
    try {
      await requestSigningCode(mission.id);
      setCodeSent(true);
      setCode('');
    } catch (err) {
      setError(errorMessage(err, "Le code n'a pas pu être envoyé."));
    } finally {
      setBusy(false);
    }
  };

  const sign = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const signed = await signContract(mission.id, code);
      setContract(signed);
      setCodeSent(false);
      onSigned?.(signed);
    } catch (err) {
      setError(errorMessage(err, 'La signature a échoué.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-xl border border-line bg-surface p-6">
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
          {!alreadySigned && !codeSent && (
            <button type="button" className={btnPrimary} onClick={sendCode} disabled={busy}>
              {busy ? 'Envoi…' : 'Signer le contrat'}
            </button>
          )}
        </div>
      </div>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      {!alreadySigned && codeSent && (
        <form onSubmit={sign} className="mt-4 rounded-lg border border-brand-200 bg-brand-50 p-4">
          <p className="text-sm text-brand-900">
            Un code à 6 chiffres vient d'être envoyé à votre adresse email. Saisissez-le pour signer
            le contrat.
          </p>
          <div className="mt-3 flex flex-wrap items-end gap-3">
            <div>
              <label className={labelClass} htmlFor="signing-code">
                Code de signature
              </label>
              <input
                id="signing-code"
                className={`${inputClass} w-40 text-center font-mono text-lg tracking-widest`}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                inputMode="numeric"
                maxLength={6}
                placeholder="000000"
                required
                autoFocus
              />
            </div>
            <button type="submit" className={btnPrimary} disabled={busy}>
              {busy ? 'Signature…' : 'Valider et signer'}
            </button>
            <button type="button" className={btnSecondary} onClick={sendCode} disabled={busy}>
              Renvoyer un code
            </button>
          </div>
        </form>
      )}

      <ul className="mt-4 divide-y divide-slate-100 border-t border-slate-100 pt-2">
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
    </section>
  );
}
