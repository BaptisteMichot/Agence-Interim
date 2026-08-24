/**
 * Session du navigateur.
 *
 * Rien n'est conservé par la page : la session **est** le cookie HttpOnly posé par le
 * serveur, que le JavaScript ne peut ni lire ni écrire. C'est tout l'intérêt du
 * procédé — une injection XSS ne peut pas voler un jeton auquel elle n'a pas accès.
 * L'identité de l'utilisateur est donc redemandée au serveur (`GET /api/auth/me`) à
 * chaque chargement de l'application.
 *
 * Conséquence assumée : la session n'est plus propre à un onglet, comme elle l'était
 * du temps de `sessionStorage`. Un cookie appartient à l'origine, pas à l'onglet : se
 * connecter dans un onglet remplace la session de tous les autres, qui basculeront
 * vers l'écran de connexion à leur prochain appel.
 */

// Sessions écrites par les versions précédentes, quand le jeton vivait dans la page.
for (const store of [localStorage, sessionStorage]) {
  store.removeItem('auth.token');
  store.removeItem('auth.user');
}

/** Émis quand le serveur refuse la session en cours (401/403) : la page doit se rabattre sur /login. */
export const SESSION_EXPIRED_EVENT = 'auth:session-expired';

/** Prévient l'application que sa session ne vaut plus rien. */
export function expireSession(): void {
  window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}
