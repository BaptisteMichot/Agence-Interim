# Image du frontend, construite en deux étapes.
#
# Le résultat d'un build Vite est un dossier de fichiers statiques : ni Node ni npm ne
# servent à l'exécution. La seconde étape repart donc d'un nginx nu, dans lequel on ne
# dépose que le contenu de dist/. L'image finale pèse quelques dizaines de mégaoctets là
# où l'étape de construction en pèse plusieurs centaines.

# ------------------------------------------------------------------- étape 1 : build
FROM node:22-alpine AS build
WORKDIR /build

# Le verrou avant le reste, pour que l'installation forme une couche distincte : tant
# que les dépendances ne changent pas, une modification de code source ne relance pas
# `npm ci`.
COPY package.json package-lock.json ./

# npm ci, et non npm install : les versions installées sont exactement celles du verrou,
# donc celles éprouvées en développement et par l'intégration continue.
RUN npm ci

COPY . .

# `npm run build` enchaîne `tsc -b` et `vite build` : une erreur de type arrête donc la
# construction de l'image. C'est voulu — une image ne doit pas pouvoir être produite à
# partir d'un code qui ne compile pas.
#
# À noter : ce build produit la variante *production* de la politique de sécurité du
# contenu, plus stricte que celle du serveur de développement (ni script en ligne, ni
# ws: dans connect-src). C'est ici, et nulle part ailleurs, qu'elle s'exerce pour la
# première fois.
RUN npm run build

# --------------------------------------------------------------- étape 2 : exécution
FROM nginx:alpine AS runtime

# La configuration par défaut sert un « Welcome to nginx » et ne relaie rien : elle est
# remplacée, pas complétée. Le corps commun est déposé à part, parce que la variante TLS
# l'inclut elle aussi — elle est montée par compose.https.yml par-dessus default.conf,
# sans reconstruction de l'image.
COPY nginx-common.conf /etc/nginx/app-common.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80
