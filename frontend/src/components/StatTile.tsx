import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface StatTileProps {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  to?: string;
  /** Met le chiffre en avant lorsqu'il appelle une action. */
  highlight?: boolean;
}

/** Chiffre clé d'un tableau de bord, cliquable lorsqu'il mène à sa section. */
export default function StatTile({ label, value, hint, to, highlight = false }: StatTileProps) {
  const content = (
    <>
      <p className="text-xs font-medium uppercase tracking-wide text-muted">{label}</p>
      <p className={`mt-2 text-3xl font-semibold ${highlight ? 'text-brand-600' : 'text-ink'}`}>
        {value}
      </p>
      {hint && <p className="mt-1 text-sm text-muted">{hint}</p>}
    </>
  );

  const base = 'rounded-xl border border-line bg-surface p-5';
  return to ? (
    <Link to={to} className={`${base} block transition hover:border-brand-200 hover:shadow-sm`}>
      {content}
    </Link>
  ) : (
    <div className={base}>{content}</div>
  );
}
