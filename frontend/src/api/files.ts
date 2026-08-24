/**
 * Ouverture des documents téléchargés depuis l'API.
 *
 * Les PDF (CV, contrats) ne sont pas des liens : ils passent par un appel authentifié qui
 * rend un blob, que la page transforme en URL locale. Cette URL doit être révoquée —
 * sans quoi le navigateur garde le document en mémoire pour toute la durée de vie de
 * l'onglet, et l'URL reste ouvrable après la déconnexion. Sur un poste partagé, un onglet
 * laissé ouvert suffit alors à rouvrir un contrat.
 *
 * Le délai laisse au nouvel onglet le temps de charger le document : révoquer
 * immédiatement après `window.open` annule l'ouverture sur plusieurs navigateurs.
 */

/** Délai avant révocation : assez pour que l'onglet ait chargé, assez court pour ne pas traîner. */
const REVOKE_DELAY_MS = 60_000;

/**
 * Ouvre un blob dans un nouvel onglet, puis libère l'URL locale.
 *
 * `noopener` empêche la page ouverte d'accéder à `window.opener`, donc à l'application.
 */
export function openBlob(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), REVOKE_DELAY_MS);
}

/**
 * Propose un contenu au téléchargement sous le nom indiqué.
 *
 * Utilisé par l'export des données personnelles, qui n'a pas vocation à s'afficher dans
 * un onglet. Le lien est créé, cliqué puis retiré : c'est la seule façon de nommer le
 * fichier côté navigateur.
 */
export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), REVOKE_DELAY_MS);
}
