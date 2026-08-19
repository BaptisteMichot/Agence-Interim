import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { linkBack } from './ui';

interface PageHeaderProps {
  title: string;
  subtitle?: ReactNode;
  /**
   * Retour vers la page parente. Inutile pour une section de premier niveau :
   * la barre latérale y donne déjà accès en permanence.
   */
  backTo?: string;
  backLabel?: string;
  actions?: ReactNode;
}

/** En-tête commun à toutes les pages : lien de retour, titre, sous-titre et actions. */
export default function PageHeader({
  title,
  subtitle,
  backTo,
  backLabel = 'Retour',
  actions,
}: PageHeaderProps) {
  return (
    <div className="mb-6">
      {backTo && (
        <Link to={backTo} className={linkBack}>
          ← {backLabel}
        </Link>
      )}
      <div className={`flex flex-wrap items-start justify-between gap-4 ${backTo ? 'mt-2' : ''}`}>
        <div>
          <h1 className="text-2xl font-semibold text-ink">{title}</h1>
          {subtitle && <p className="mt-1 text-muted">{subtitle}</p>}
        </div>
        {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
      </div>
    </div>
  );
}
