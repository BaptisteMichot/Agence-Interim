import { useCallback, useEffect, useState } from 'react';
import { getMyCompany, updateMyCompany, type EmployerCompany } from '../../api/employer';
import { errorMessage } from '../../api/http';
import AddressFields from '../../components/AddressFields';
import {
  EMPTY_ADDRESS,
  formatAddress,
  parseAddress,
  type AddressParts,
} from '../../components/address';
import { btnPrimary, btnSecondary, errorBox, inputClass, labelClass } from '../../components/ui';

/**
 * Fiche entreprise de l'employeur : ces mentions figurent sur chaque contrat de
 * mission, d'où l'avertissement tant qu'elles sont incomplètes.
 */
export default function CompanySection() {
  const [company, setCompany] = useState<EmployerCompany | null>(null);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [companyName, setCompanyName] = useState('');
  const [address, setAddress] = useState<AddressParts>({ ...EMPTY_ADDRESS });
  const [companyNumber, setCompanyNumber] = useState('');
  const [jointCommittee, setJointCommittee] = useState('');

  const fill = useCallback((data: EmployerCompany) => {
    setCompany(data);
    setCompanyName(data.companyName ?? '');
    setAddress(parseAddress(data.address));
    setCompanyNumber(data.companyNumber ?? '');
    setJointCommittee(data.jointCommittee ?? '');
  }, []);

  useEffect(() => {
    getMyCompany()
      .then(fill)
      .catch((err) =>
        setError(errorMessage(err, "Impossible de charger la fiche entreprise.")),
      );
  }, [fill]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      fill(
        await updateMyCompany({
          companyName,
          address: formatAddress(address),
          companyNumber,
          jointCommittee,
        }),
      );
      setEditing(false);
    } catch (err) {
      setError(errorMessage(err, "L'enregistrement a échoué."));
    } finally {
      setSaving(false);
    }
  };

  if (!company) {
    return null;
  }

  return (
    <section className="rounded-xl border border-line bg-surface p-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-semibold text-slate-900">Mon entreprise</h2>
        {!editing && (
          <button type="button" className={btnSecondary} onClick={() => setEditing(true)}>
            Modifier
          </button>
        )}
      </div>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      {company.incomplete && !editing && (
        <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          Votre fiche est incomplète. Ces mentions sont obligatoires sur les contrats : tant
          qu'elles manquent, vous ne pouvez pas proposer de mission à un candidat.
        </p>
      )}

      {editing ? (
        <form onSubmit={submit} className="mt-4 grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className={labelClass} htmlFor="company-name">
              Dénomination
            </label>
            <input
              id="company-name"
              required
              maxLength={30}
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              className={inputClass}
            />
          </div>
          <div className="sm:col-span-2">
            <span className={`${labelClass} mb-2`}>Adresse du siège</span>
            <AddressFields
              idPrefix="company-address"
              parts={address}
              onChange={setAddress}
              required
            />
          </div>
          <div>
            <label className={labelClass} htmlFor="company-number">
              Numéro d'entreprise (BCE)
            </label>
            <input
              id="company-number"
              required
              maxLength={15}
              placeholder="0000.000.000"
              value={companyNumber}
              onChange={(e) => setCompanyNumber(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label className={labelClass} htmlFor="company-committee">
              Commission paritaire
            </label>
            <input
              id="company-committee"
              required
              maxLength={10}
              placeholder="Par exemple 111 ou 200"
              value={jointCommittee}
              onChange={(e) => setJointCommittee(e.target.value)}
              className={inputClass}
            />
          </div>
          <div className="flex gap-3 sm:col-span-2">
            <button type="submit" className={btnPrimary} disabled={saving}>
              {saving ? 'Enregistrement…' : 'Enregistrer'}
            </button>
            <button
              type="button"
              className={btnSecondary}
              onClick={() => {
                fill(company);
                setEditing(false);
                setError(null);
              }}
            >
              Annuler
            </button>
          </div>
        </form>
      ) : (
        <dl className="mt-4 grid gap-4 sm:grid-cols-2">
          <Fact label="Dénomination" value={company.companyName} />
          <Fact label="Adresse du siège" value={company.address} />
          <Fact label="Numéro d'entreprise" value={company.companyNumber} />
          <Fact label="Commission paritaire" value={company.jointCommittee} />
        </dl>
      )}
    </section>
  );
}

function Fact({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-slate-400">{label}</dt>
      <dd className={`mt-0.5 text-sm ${value ? 'font-medium text-slate-900' : 'text-amber-700'}`}>
        {value || 'À compléter'}
      </dd>
    </div>
  );
}
