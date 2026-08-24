import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

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
    proxy: {
      // Les appels /api sont transmis au backend Spring Boot (évite les soucis de CORS en dev).
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // WebSocket du chat : le proxy doit relayer la mise à niveau du protocole.
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
}))
