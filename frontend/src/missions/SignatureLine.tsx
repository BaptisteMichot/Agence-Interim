import { formatDateTime } from '../profile/format';

/**
 * État de signature d'une partie. Même présentation sur le contrat d'une mission et
 * dans « Mes documents » : une signature ne doit pas se lire différemment d'un écran
 * à l'autre.
 */
export default function SignatureLine({
  label,
  signed,
  signedAt,
}: {
  label: string;
  signed: boolean;
  signedAt: string | null;
}) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 py-2">
      <span>
        <span className="block text-sm text-slate-700">{label}</span>
        {signed && signedAt && (
          <span className="block text-xs text-slate-500">Signé le {formatDateTime(signedAt)}</span>
        )}
      </span>
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
