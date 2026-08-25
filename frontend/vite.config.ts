import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

/**
 * Certificat du serveur de développement, s'il a été produit.
 *
 * Le dépôt ne peut pas le contenir : une clé privée ne se versionne pas, et un
 * certificat n'est de toute façon reconnu que là où l'autorité qui l'a signé est
 * installée. Son absence n'est donc pas une erreur — elle fait simplement retomber le
 * serveur en HTTP, et l'application fonctionne à l'identique. La commande qui le produit
 * est dans le README.
 *
 * Servir le développement en HTTPS aligne l'environnement de travail sur celui des
 * conteneurs : le chat s'ouvre en `wss` comme il le fera en production, et les
 * différences de comportement liées au protocole se découvrent en développant plutôt
 * qu'au déploiement.
 */
function devCertificate() {
  const dir = fileURLToPath(new URL('../certs/', import.meta.url))
  const key = `${dir}localhost.key`
  const cert = `${dir}localhost.crt`
  return existsSync(key) && existsSync(cert)
    ? { key: readFileSync(key), cert: readFileSync(cert) }
    : undefined
}

/**
 * Politique de sécurité du contenu, posée sur le document HTML.
 *
 * C'est ici qu'elle compte : une réponse d'API ne charge aucune ressource, seule la
 * page peut exécuter du script. La règle de fond est `script-src 'self'` — même si une
 * donnée hostile parvenait à être injectée dans le DOM, le navigateur refuserait de
 * l'exécuter. React échappe déjà tout ce qu'il affiche et l'application n'utilise nulle
 * part `dangerouslySetInnerHTML` : la CSP est la seconde barrière, pas la première.
 *
 * En développement, Vite injecte le préambule de rafraîchissement à chaud sous forme de
 * script en ligne et ouvre une WebSocket vers son propre serveur : la politique y est
 * donc assouplie sur ces deux points, et sur eux seuls.
 *
 * `frame-ancestors` n'est pas exprimable par une balise meta ; la protection contre
 * l'inclusion dans une iframe vient de l'en-tête envoyé par le backend, et viendra du
 * reverse proxy pour les fichiers statiques.
 */
function contentSecurityPolicy(dev: boolean): string {
  return [
    "default-src 'self'",
    `script-src 'self'${dev ? " 'unsafe-inline'" : ''}`,
    // Tailwind injecte ses styles dans une balise <style> en développement, et React
    // pose des styles d'élément : les styles en ligne restent autorisés.
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data:",
    "font-src 'self'",
    // Les PDF (CV, contrats) sont ouverts depuis un blob construit par la page.
    "object-src 'self' blob:",
    "frame-src 'self' blob:",
    `connect-src 'self'${dev ? ' ws: wss:' : ''}`,
    // Rien à hériter d'une base réécrite, et aucun formulaire ne poste ailleurs.
    "base-uri 'none'",
    "form-action 'self'",
  ].join('; ')
}

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [
    react(),
    tailwindcss(),
    {
      name: 'content-security-policy',
      transformIndexHtml(html: string) {
        return {
          html,
          tags: [
            {
              tag: 'meta',
              attrs: {
                'http-equiv': 'Content-Security-Policy',
                content: contentSecurityPolicy(command === 'serve'),
              },
              injectTo: 'head-prepend' as const,
            },
          ],
        }
      },
    },
  ],
  server: {
    // Absent si le certificat n'a pas été produit : le serveur repart alors en clair.
    https: devCertificate(),
    proxy: {
      // Les appels /api sont transmis au backend Spring Boot (évite les soucis de CORS en dev).
      //
      // La cible reste en clair même quand le serveur est servi en HTTPS : le chiffrement
      // s'arrête ici, comme il s'arrête à nginx dans les conteneurs. Les deux extrémités
      // sont sur la même machine, et le backend n'écoute qu'en HTTP — lui poser un
      // certificat n'ajouterait rien qu'une seconde chose à tenir à jour.
      //
      // Conséquence utile : Spring ne voit jamais une requête chiffrée et n'émet donc
      // pas de HSTS. C'est heureux, car cet en-tête ignore le numéro de port : reçu pour
      // localhost, il forcerait en HTTPS tout ce qui tourne sur cette machine, y compris
      // les projets qui n'ont rien à voir avec celui-ci.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // WebSocket du chat : le proxy doit relayer la mise à niveau du protocole. Le
      // navigateur l'ouvre en wss quand la page est servie en HTTPS ; la liaison vers le
      // backend, elle, reste en ws.
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
}))
