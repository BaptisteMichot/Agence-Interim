import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { changePassword, closeAccount, exportMyData } from '../../api/account';
import { saveBlob } from '../../api/files';
import { errorMessage } from '../../api/http';
import { useAuth } from '../../auth/AuthContext';
import type { Role } from '../../auth/types';
import ConfirmDialog from '../../components/ConfirmDialog';
import PageHeader from '../../components/PageHeader';
import PasswordInput from '../../components/PasswordInput';
import {
  btnDangerSolid,
  btnPrimary,
  btnSecondary,
  card,
  errorBox,
  labelClass,
  mutedText,
  sectionTitle,
} from '../../components/ui';

/**
 * Textes propres à chaque rôle.
 *
 * <p>Ce que la plateforme détient et ce qu'une clôture emporte ne sont pas les mêmes
 * pour un intérimaire, un employeur et l'agence : parler de CV et de candidatures à un
 * employeur, ou d'offres publiées à un intérimaire, décrit une application qui n'est pas
 * la sienne. Sur un écran qui annonce une suppression définitive, cette imprécision n'est
 * pas cosmétique — elle empêche de décider en connaissance de cause.
 *
 * <p>`close` à `null` signifie que le rôle ne peut pas se clôturer lui-même ; le bouton
 * cède alors la place à l'explication. Le backend refuse de toute façon, mais afficher un
 * bouton pour rendre une erreur serait une invitation à essayer.
 */
interface RoleCopy {
  /** Ce que contient l'export. */
  data: string;
  /** Ce qu'une clôture emporte, ou `null` si le rôle ne peut pas clôturer. */
  close: string | null;
  /** Rappel affiché dans la boîte de confirmation. */
  confirm: string;
}

const COPY: Record<Role, RoleCopy> = {
  JOBSEEKER: {
    data:
      'Votre profil, vos compétences, diplômes et langues, vos expériences et formations, ' +
      'vos candidatures, vos contrats et les messages que vous avez envoyés.',
    close:
      'Votre profil, votre CV, vos compétences et vos messages sont effacés. Si vous avez ' +
      'déjà postulé, les candidatures, missions et contrats correspondants sont conservés ' +
      'sans votre nom : la loi impose de garder cinq ans un contrat de travail, et ces ' +
      "documents concernent aussi l'employeur. Un compte engagé dans une mission en cours, " +
      "ou dans une mission encore soumise à l'agence, ne peut pas être clôturé.",
    confirm:
      'Vous perdrez l’accès à la plateforme. Votre profil, votre CV et vos messages seront ' +
      'effacés ; vos candidatures et vos contrats seront conservés sans votre nom.',
  },
  EMPLOYER: {
    data:
      'Votre compte, la fiche de votre entreprise, les offres que vous avez publiées, ' +
      'vos contrats et les messages que vous avez envoyés.',
    close:
      'Votre compte et vos messages sont effacés, et aucune de vos offres ne reste en ligne : ' +
      'celles qui n’ont reçu aucune candidature sont supprimées, les autres sont clôturées et ' +
      'conservées sans votre nom, car elles portent l’historique des intérimaires qui y ont ' +
      'postulé. Les candidatures encore en cours sont annulées. Les missions et les contrats ' +
      'sont conservés, également sans votre nom : un contrat de travail se garde cinq ans et ' +
      'engage aussi l’intérimaire. Les mentions légales de votre entreprise restent, ' +
      'puisqu’elles figurent sur ces contrats. Un compte engagé dans une mission en cours, ou ' +
      'dans une mission encore soumise à l’agence, ne peut pas être clôturé.',
    confirm:
      'Vous perdrez l’accès à la plateforme. Vos offres sans candidature seront supprimées, ' +
      'les autres clôturées ; les candidatures en cours seront annulées. Vos contrats seront ' +
      'conservés sans votre nom.',
  },
  EMPLOYER_PENDING: {
    data:
      "Votre compte et la fiche de l'entreprise renseignée lors de votre demande d'accès.",
    close:
      "Votre compte et votre demande d'accès sont supprimés définitivement. Vous n'avez " +
      'encore rien publié : rien n’est conservé, et vous pourrez déposer une nouvelle ' +
      'demande plus tard avec la même adresse.',
    confirm:
      "Votre compte et votre demande d'accès seront supprimés. Cette action est définitive.",
  },
  ADMIN: {
    data: "Votre compte de l'agence et les messages que vous avez envoyés.",
    close: null,
    confirm: '',
  },
};

/**
 * Page « Mon compte » : les trois opérations qu'un utilisateur doit pouvoir mener seul
 * sur son propre compte.
 *
 * Accessible aux trois rôles, et non rangée dans le profil intérimaire : un employeur et
 * l'agence ont exactement les mêmes droits sur leur mot de passe et sur leurs données.
 */
export default function AccountSecurityPage() {
  const { user, logout } = useAuth();
  const copy = COPY[user?.role ?? 'JOBSEEKER'];

  return (
    <div className="space-y-6">
      <PageHeader title="Mon compte" subtitle={user?.email} />
      <PasswordCard />
      <DataCard description={copy.data} />
      <CloseAccountCard copy={copy} onClosed={logout} />
    </div>
  );
}

/** Changement de mot de passe : l'ancien est redemandé, les autres sessions tombent. */
function PasswordCard() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    setError(null);
    if (newPassword !== confirmation) {
      setError('Les deux nouveaux mots de passe ne correspondent pas.');
      return;
    }
    setBusy(true);
    try {
      const response = await changePassword(currentPassword, newPassword);
      setNotice(response.message);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmation('');
    } catch (err) {
      setError(errorMessage(err, "Le mot de passe n'a pas pu être modifié."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className={card}>
      <h2 className={sectionTitle}>Mot de passe</h2>
      <p className={`mt-1 ${mutedText}`}>
        Au moins 14 caractères, avec une minuscule, une majuscule, un chiffre et un caractère
        spécial. Le changement déconnecte vos autres appareils.
      </p>

      {notice !== null && (
        <p className="mt-4 rounded-md bg-brand-50 px-3 py-2 text-sm text-brand-700">{notice}</p>
      )}
      {error !== null && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <form onSubmit={submit} className="mt-4 grid gap-4 sm:max-w-md">
        <div>
          <label className={labelClass} htmlFor="current-password">
            Mot de passe actuel
          </label>
          <PasswordInput
            id="current-password"
            value={currentPassword}
            onChange={setCurrentPassword}
            required
            autoComplete="current-password"
          />
        </div>
        <div>
          <label className={labelClass} htmlFor="new-password">
            Nouveau mot de passe
          </label>
          <PasswordInput
            id="new-password"
            value={newPassword}
            onChange={setNewPassword}
            required
            autoComplete="new-password"
          />
        </div>
        <div>
          <label className={labelClass} htmlFor="confirm-password">
            Confirmer le nouveau mot de passe
          </label>
          <PasswordInput
            id="confirm-password"
            value={confirmation}
            onChange={setConfirmation}
            required
            autoComplete="new-password"
          />
        </div>
        <div>
          <button type="submit" className={btnPrimary} disabled={busy}>
            {busy ? 'Enregistrement…' : 'Changer mon mot de passe'}
          </button>
        </div>
      </form>
    </section>
  );
}

/** Export des données personnelles (RGPD, droit d'accès). */
function DataCard({ description }: { description: string }) {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const download = async () => {
    setError(null);
    setBusy(true);
    try {
      saveBlob(await exportMyData(), 'mes-donnees.txt');
    } catch (err) {
      setError(errorMessage(err, "L'export n'a pas pu être généré."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className={card}>
      <h2 className={sectionTitle}>Mes données</h2>
      <p className={`mt-1 ${mutedText}`}>{description}</p>
      <p className={`mt-1 ${mutedText}`}>
        Le fichier est un document texte, lisible tel quel, sans logiciel particulier.
      </p>
      {error !== null && <p className={`mt-4 ${errorBox}`}>{error}</p>}
      <button type="button" className={`mt-4 ${btnSecondary}`} onClick={download} disabled={busy}>
        {busy ? 'Préparation…' : 'Télécharger mes données'}
      </button>
    </section>
  );
}

/** Clôture du compte : suppression si rien n'a été engagé, anonymisation sinon. */
function CloseAccountCard({
  copy,
  onClosed,
}: {
  copy: RoleCopy;
  onClosed: () => Promise<void>;
}) {
  const navigate = useNavigate();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const confirm = async () => {
    setConfirmOpen(false);
    setError(null);
    setBusy(true);
    try {
      await closeAccount();
      // Le serveur a déjà effacé le cookie et révoqué les jetons ; il reste à vider
      // l'état de la page pour qu'elle cesse de se croire connectée.
      await onClosed();
      navigate('/login', { replace: true });
    } catch (err) {
      setError(errorMessage(err, "Le compte n'a pas pu être clôturé."));
    } finally {
      setBusy(false);
    }
  };

  if (copy.close === null) {
    return (
      <section className={card}>
        <h2 className={sectionTitle}>Clôturer mon compte</h2>
        <p className={`mt-1 ${mutedText}`}>
          Un compte de l'agence ne se clôture pas depuis cet écran : il donne accès à la
          validation des missions et au traitement des demandes d'accès employeur. Sa
          suppression relève de l'administration de la plateforme.
        </p>
      </section>
    );
  }

  return (
    <section className={card}>
      <h2 className={sectionTitle}>Clôturer mon compte</h2>
      <p className={`mt-1 ${mutedText}`}>{copy.close}</p>
      {error !== null && <p className={`mt-4 ${errorBox}`}>{error}</p>}
      <button
        type="button"
        className={`mt-4 ${btnDangerSolid}`}
        onClick={() => setConfirmOpen(true)}
        disabled={busy}
      >
        Clôturer définitivement mon compte
      </button>

      <ConfirmDialog
        open={confirmOpen}
        title="Clôturer votre compte ?"
        message={copy.confirm}
        confirmLabel="Clôturer mon compte"
        onConfirm={confirm}
        onCancel={() => setConfirmOpen(false)}
      />
    </section>
  );
}
