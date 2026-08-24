import { Link } from 'react-router-dom';
import { getAgency } from '../api/client';
import { card, sectionTitle } from '../components/ui';
import { useResource } from '../hooks/useResource';

/** Bloc de la politique : un titre et son contenu. */
function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className={`${card} space-y-3`}>
      <h2 className={sectionTitle}>{title}</h2>
      <div className="space-y-3 text-sm text-slate-700">{children}</div>
    </section>
  );
}

/** Ligne du tableau des données traitées. */
function DataRow({ what, why, basis }: { what: string; why: string; basis: string }) {
  return (
    <tr className="border-t border-line align-top">
      <td className="py-2 pr-4 font-medium text-ink">{what}</td>
      <td className="py-2 pr-4">{why}</td>
      <td className="py-2 text-muted">{basis}</td>
    </tr>
  );
}

/**
 * Politique de confidentialité, accessible sans compte : elle doit pouvoir être lue
 * avant de s'inscrire.
 *
 * <p>L'application ne dépose aucun bandeau de consentement, et ce n'est pas un oubli :
 * ses deux seuls cookies sont strictement nécessaires au service demandé, cas que
 * l'article 129 de la loi du 13 juin 2005 dispense de consentement. Le raisonnement est
 * écrit noir sur blanc ci-dessous plutôt que sous-entendu.
 */
export default function PrivacyPage() {
  const { data: agency } = useResource(getAgency, "Impossible de charger l'identité de l'agence.");

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <Link to="/login" className="text-sm text-brand-600 transition hover:underline">
        ← Retour à la connexion
      </Link>

      <h1 className="mt-3 text-2xl font-semibold text-ink">Vie privée et cookies</h1>
      <p className="mt-1 text-muted">
        Quelles données sont collectées à votre sujet, pourquoi, combien de temps elles
        sont conservées, et quels cookies sont déposés sur votre navigateur.
      </p>

      <div className="mt-6 space-y-4">
        <Section title="Responsable du traitement">
          {agency === null ? (
            <p>L'agence d'intérim exploitant la plateforme.</p>
          ) : (
            <p>
              <strong className="text-ink">{agency.name}</strong>
              <br />
              {agency.address}
              <br />
              Numéro d'entreprise {agency.companyNumber} · Agrément {agency.licenceNumber}
              <br />
              Commission paritaire {agency.jointCommittee} (travail intérimaire)
            </p>
          )}
        </Section>

        <Section title="Données traitées et pourquoi">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="text-xs uppercase tracking-wide text-muted">
                  <th className="pb-2 pr-4 font-medium">Données</th>
                  <th className="pb-2 pr-4 font-medium">Finalité</th>
                  <th className="pb-2 font-medium">Base légale</th>
                </tr>
              </thead>
              <tbody>
                <DataRow
                  what="Nom, prénom, email, mot de passe"
                  why="Créer et sécuriser votre compte"
                  basis="Exécution du contrat"
                />
                <DataRow
                  what="Compétences, diplômes, langues, expériences, formations, CV, permis de conduire"
                  why="Vous proposer les offres qui correspondent à votre profil et transmettre votre candidature à l'employeur"
                  basis="Exécution du contrat"
                />
                <DataRow
                  what="Disponibilités et indisponibilités"
                  why="Établir votre planning et éviter les missions incompatibles"
                  basis="Exécution du contrat"
                />
                <DataRow
                  what="Adresse, date de naissance, numéro de registre national, numéro de compte bancaire"
                  why="Rédiger le contrat de travail intérimaire et permettre votre rémunération"
                  basis="Obligation légale"
                />
                <DataRow
                  what="Adresse du siège, numéro d'entreprise et commission paritaire de l'employeur"
                  why="Identifier l'entreprise utilisatrice sur le contrat"
                  basis="Obligation légale"
                />
                <DataRow
                  what="Candidatures, notes attribuées, missions, horaires, contrats signés"
                  why="Conduire le recrutement et exécuter la mission d'intérim"
                  basis="Exécution du contrat"
                />
                <DataRow
                  what="Messages échangés avec un employeur"
                  why="Permettre l'échange lié à une candidature"
                  basis="Exécution du contrat"
                />
              </tbody>
            </table>
          </div>
          <p className="text-muted">
            Le numéro de registre national et le numéro de compte bancaire ne sont
            demandés qu'au moment d'accepter une mission : ce sont des mentions
            obligatoires du contrat de travail intérimaire (loi du 24 juillet 1987).
          </p>
        </Section>

        <Section title="Qui voit quoi">
          <ul className="list-disc space-y-1 pl-5">
            <li>
              L'employeur ne voit votre profil qu'à partir du moment où vous avez postulé
              à l'une de ses offres.
            </li>
            <li>
              L'agence voit les missions qui lui sont soumises pour validation, et les
              contrats qu'elle établit.
            </li>
            <li>
              Aucune donnée n'est transmise à un tiers, ni utilisée à des fins publicitaires
              ou statistiques.
            </li>
          </ul>
        </Section>

        <Section title="Cookies">
          <p>
            <strong className="text-ink">Deux cookies</strong> sont déposés sur votre
            navigateur. Aucun ne sert à la mesure d'audience, à la publicité ou au suivi
            de votre navigation.
          </p>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="text-xs uppercase tracking-wide text-muted">
                  <th className="pb-2 pr-4 font-medium">Cookie</th>
                  <th className="pb-2 pr-4 font-medium">Rôle</th>
                  <th className="pb-2 font-medium">Durée</th>
                </tr>
              </thead>
              <tbody>
                <DataRow
                  what="auth-token"
                  why="Vous maintenir connecté d'une page à l'autre."
                  basis="1 heure"
                />
                <DataRow
                  what="XSRF-TOKEN"
                  why="Empêcher qu'un autre site déclenche une action en votre nom pendant que vous êtes connecté."
                  basis="Durée de la session"
                />
              </tbody>
            </table>
          </div>
          <p>
            Ces deux cookies sont indispensables au fonctionnement du site : sans le
            premier, vous ne pouvez pas rester connecté d'une page à l'autre ; sans le
            second, un autre site pourrait agir en votre nom pendant votre session. Ils
            relèvent donc des cookies strictement nécessaires, pour lesquels votre
            consentement préalable n'est pas requis (article 129 de la loi du 13 juin 2005
            relative aux communications électroniques). Vous n'avez rien à accepter ni à
            refuser ; si vous les bloquez dans votre navigateur, la connexion au site ne
            fonctionnera plus.
          </p>
        </Section>

        <Section title="Durée de conservation">
          <p>
            Vos données de profil sont conservées tant que votre compte existe. Les
            contrats et les documents liés à une mission relèvent, eux, des obligations de
            conservation des documents sociaux, qui survivent à la suppression du compte.
          </p>
        </Section>

        <Section title="Vos droits">
          <p>
            Vous pouvez accéder à vos données, les rectifier, en demander l'effacement ou
            la portabilité, et vous opposer à leur traitement. La plupart de ces
            informations se modifient directement depuis votre profil.
          </p>
          <p>
            Pour toute demande, adressez-vous à l'agence. Vous pouvez également introduire
            une réclamation auprès de l'Autorité de protection des données, rue de la
            Presse 35, 1000 Bruxelles.
          </p>
        </Section>
      </div>
    </div>
  );
}
