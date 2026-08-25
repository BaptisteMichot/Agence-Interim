# Agence d'intérim — plateforme de recrutement

Application web pour une agence de travail intérimaire, réalisée dans le cadre d'un
travail de fin d'études. Elle couvre le parcours complet d'une mission d'intérim : un
demandeur d'emploi crée son profil, un employeur publie une offre, le système rapproche
les deux, et la mission suit sa vie propre jusqu'au contrat de travail signé
électroniquement par les deux parties.

L'intérim est une relation **triangulaire**, et c'est ce qui structure toute
l'application : l'agence est l'employeur juridique, l'entreprise utilisatrice donne les
instructions, l'intérimaire preste. Chaque décision passe par celle des trois parties
qu'elle concerne, jamais par une autre.

---

## Ce que fait l'application

**Pour l'intérimaire**
- Profil détaillé : compétences et niveaux, langues (CECR), diplômes, expériences,
  formations, CV en PDF
- Recherche d'offres avec filtres, favoris, et une liste **« Pour moi »** classée par
  score de correspondance
- Candidature, suivi, retrait ; messagerie avec l'employeur
- Acceptation ou refus des missions proposées, planning des journées prestées,
  déclaration d'indisponibilités
- Signature du contrat par code à usage unique reçu par email
- Export de ses données personnelles et clôture de son compte (RGPD)

**Pour l'employeur**
- Demande d'accès soumise à l'agence, fiche entreprise avec ses mentions légales
- Publication d'offres avec leurs exigences (compétences, langues, diplômes, véhicule,
  expérience), suivi et clôture
- Consultation, notation et tri des candidatures ; consultation du profil des candidats
- Création de la mission après sélection, correction après refus, renouvellement
- Signature du contrat

**Pour l'agence (administrateur)**
- Traitement des demandes d'accès employeur
- Validation ou refus motivé des missions proposées
- Journal d'audit des actes engageants

### Le score de correspondance

Le rapprochement profil/offre est **déterministe**, sans apprentissage automatique :
chaque critère d'une offre reçoit un taux de satisfaction entre 0 et 1, pondéré selon
qu'il est exigé (poids 2) ou souhaité (poids 1).

```
score = 100 × Σ(poids × taux) / Σ(poids)
```

Un critère **exigé** doit être satisfait entièrement, sans quoi le candidat est écarté
quel que soit son score — le score classe, la règle d'exclusion filtre. Le calcul se fait
à la demande du candidat, quand il ouvre son onglet « Pour moi » : aucun email n'est
envoyé à la publication d'une offre.

---

## Pile technique

| Couche | Technologies |
|---|---|
| Backend | Java 25, Spring Boot 4.0.6, Spring Security, Spring Data JPA, Maven |
| Base de données | PostgreSQL 18 |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4, React Router v7 |
| Documents | OpenPDF (contrats de travail) |
| Temps réel | WebSocket native (messagerie) |

L'architecture est en **trois tiers** : le frontend ne parle qu'à l'API, l'API est la
seule à connaître la base. L'authentification repose sur un JWT transporté par un
**cookie HttpOnly + SameSite=Strict**, hors de portée d'une injection XSS, ce qui rend
pertinente la protection CSRF par jeton double-envoi.

---

## Démarrer avec Docker

C'est le chemin le plus court, et le seul qui ne demande d'installer ni Java, ni Node, ni
PostgreSQL : les trois vivent dans les images.

```bash
cp .env.example .env     # puis remplir les douze valeurs demandées
docker compose up --build
```

L'application est servie sur <http://localhost:8081>.

Le fichier `.env` de la racine ne contient que les secrets et les adresses personnelles.
Tout le reste — ports, chemins, identité de l'agence, réglages SMTP — est écrit en clair
dans `compose.yml`, qui vaut ainsi description lisible de ce que l'application attend. Ce
fichier n'a rien à voir avec `backend/.env`, qui configure l'exécution directe sur le
poste : ce sont deux installations distinctes, avec deux bases distinctes.

Trois services sont démarrés. Seul le frontend publie un port ; PostgreSQL et le backend
ne sont joignables que par le réseau privé des conteneurs. nginx sert les fichiers
statiques et relaie `/api` et `/ws` vers le backend — ce que faisait le serveur de
développement de Vite, qui n'existe plus une fois l'application construite.

Les CV déposés, les contrats générés et la base de données vivent dans des volumes
nommés. `docker compose down` détruit les conteneurs sans y toucher ; il faut
`docker compose down -v` pour tout effacer et repartir d'une base vierge.

> **Le schéma est créé par Hibernate** (`DDL_AUTO=update`), faute de migrations
> versionnées. C'est sans danger ici : la base naît avec le conteneur.

Ce premier montage est servi **en clair**, `COOKIE_SECURE` reste donc à `false` — un
cookie marqué `Secure` serait ignoré par le navigateur. C'est aussi ce qui laisse
`ProductionGuard` en simple avertissement au démarrage. Pour le chiffrer, voir ci-dessous.

### En TLS

Un certificat est à produire une seule fois, par l'un des deux moyens. Avec
[mkcert](https://github.com/FiloSottile/mkcert), qui signe avec une autorité ajoutée au
magasin de confiance du poste — le navigateur n'avertit plus, et le HSTS devient
réellement actif :

```bash
mkcert -install                                   # une fois par machine
mkdir -p certs
mkcert -key-file certs/agence.key -cert-file certs/agence.crt agence-interim.localhost
```

Sans mkcert, un certificat auto-signé fait le même travail de chiffrement, au prix d'un
avertissement à chaque première visite :

```bash
mkdir -p certs
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 365 \
  -keyout certs/agence.key -out certs/agence.crt \
  -subj "/C=BE/O=Agence Interim SA/CN=agence-interim.localhost" \
  -addext "subjectAltName=DNS:agence-interim.localhost"
```

Dans les deux cas le certificat ne porte **que** `agence-interim.localhost`, jamais
`localhost` : voir l'encart sur le HSTS plus bas.

```bash
docker compose -f compose.yml -f compose.https.yml up -d
```

L'application est alors servie sur <https://agence-interim.localhost:8443>, et le port
8081 ne fait plus que rediriger vers elle. Le navigateur avertira à la première visite :
sans domaine public, aucune autorité ne peut certifier ce nom.

**L'ordre compte à la première bascule.** `COOKIE_SECURE=true` réveille `ProductionGuard`,
qui refuse alors de démarrer sur un jeu de démonstration actif ou sur un `ddl-auto` qui
modifie le schéma. La variante TLS éteint donc ces deux réglages, ce qui suppose une base
déjà établie :

```bash
docker compose up -d                                      # en clair : schéma + données
docker compose down
docker compose -f compose.yml -f compose.https.yml up -d  # bascule
```

Les volumes survivent à l'opération, la démonstration reste entièrement jouable.

> **Pourquoi `agence-interim.localhost` et non `localhost`.** Le HSTS ignore le numéro de
> port : reçu depuis `https://localhost:8443`, il forcerait en HTTPS tout ce qui répond
> sur `localhost`, serveur de développement compris. Le certificat ne porte que le nom
> dédié, si bien qu'un accès par `localhost` échoue plutôt que de risquer l'effet de bord.
> Les navigateurs résolvent d'eux-mêmes `*.localhost` vers 127.0.0.1 (RFC 6761).

> **Jusqu'où va la confiance.** Un certificat certifie un nom de domaine, et aucune
> autorité publique ne signe un nom en `.localhost`. mkcert contourne la difficulté en
> installant sa propre autorité sur le poste : la confiance y est complète, mais elle
> s'arrête à ce poste. Ailleurs, l'avertissement reparaît. Un certificat reconnu partout
> supposerait un vrai domaine — et le livrer avec le dépôt exigerait d'y publier la clé
> privée, c'est-à-dire la faute même que le TLS prévient.

> **Un détail qui a son importance.** Un navigateur n'enregistre le HSTS que sur une
> connexion dont le certificat est valide. Avec un auto-signé, l'en-tête est émis mais
> ignoré ; avec mkcert, il prend effet pour de bon. C'est à ce moment que le choix du nom
> dédié cesse d'être une précaution théorique.

---

## Démarrer sans Docker

À préférer pour développer : le rechargement à chaud du backend et de Vite est bien plus
rapide qu'une reconstruction d'image.

### Prérequis

- **JDK 25**
- **Node.js 22** ou plus récent
- **PostgreSQL 18**, avec une base vide créée pour l'application

Maven n'a pas besoin d'être installé : le dépôt embarque le wrapper (`mvnw`).

### 1. Configurer le backend

Toute la configuration passe par un fichier `.env` dans `backend/`. Un modèle est fourni :

```bash
cd backend
cp .env.example .env
```

Le modèle ne porte que des noms en guise de valeurs : chaque ligne est à remplir. Les
plus importantes :

| Variable | Rôle |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL |
| `DDL_AUTO` | `update` en développement |
| `JWT_SECRET` | Secret de signature des jetons, au moins 32 caractères |
| `ENCRYPTION_KEY` | Chiffrement au repos du registre national et de l'IBAN, au moins 32 caractères |
| `COOKIE_SECURE` | `false` tant que l'application est servie en HTTP |
| `AGENCY_*` | Identité de l'agence, reprise sur chaque contrat |
| `MAIL_ENABLED` | `false` en développement : les emails sont journalisés au lieu d'être envoyés |
| `DEMO_DATA_ENABLED` | `true` pour amorcer le jeu de démonstration |

`application.properties` ne contient **aucune valeur par défaut** : une variable
manquante fait échouer le démarrage avec un message explicite, plutôt que de laisser
l'application tourner sur une valeur implicite que personne n'a choisie.

> **`MAIL_ENABLED=false` n'est pas qu'un confort.** Les codes de signature et de
> réinitialisation de mot de passe sont alors écrits dans la console : c'est le seul
> moyen de parcourir la signature d'un contrat sans serveur SMTP.

### 2. Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
```

L'API écoute sur le port défini par `SERVER_PORT`. **Gardez 8080** : le serveur de
développement du frontend y relaie ses appels, et cette cible n'est pas configurable.
Au premier démarrage, Hibernate crée le schéma et un compte administrateur est amorcé
depuis `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

### 3. Lancer le frontend

```bash
cd frontend
npm install
npm run dev
```

L'interface est servie sur <http://localhost:5173>. Le serveur de développement relaie
`/api` et `/ws` vers le backend, ce qui évite toute question de CORS en développement.

### 4. Développer en HTTPS (facultatif)

Aligne l'environnement de travail sur celui des conteneurs : le chat s'ouvre en `wss`, et
les différences de comportement liées au protocole se découvrent en développant plutôt
qu'au déploiement. Il suffit d'un certificat, que Vite prend en compte s'il le trouve :

```bash
mkcert -key-file certs/localhost.key -cert-file certs/localhost.crt localhost 127.0.0.1
```

Puis, dans `backend/.env` :

```properties
FRONTEND_URL=https://localhost:5173
```

`WebSocketConfig` n'accepte la poignée de main du chat que si l'en-tête `Origin` de la
requête correspond exactement à cette valeur : l'oublier ne coupe que la messagerie, et
sans message clair.

Le certificat absent, `npm run dev` repart en clair sans rien signaler — rien ne casse.

> **`COOKIE_SECURE` reste à `false`, même en HTTPS.** Le passer à `true` réveillerait
> `ProductionGuard`, qui exigerait alors de couper le jeu de démonstration et d'abandonner
> `ddl-auto=update` : il rendrait l'environnement de développement inutilisable. Un cookie
> sans cet attribut fonctionne parfaitement sur une connexion chiffrée.

> **Le backend, lui, reste en clair.** Le chiffrement s'arrête au serveur de Vite, comme
> il s'arrête à nginx dans les conteneurs. C'est aussi ce qui évite que Spring émette un
> HSTS : cet en-tête ignore le numéro de port, et forcerait en HTTPS **tout** ce qui
> tourne sur `localhost`, projets étrangers compris, pendant un an.

---

## Comptes de démonstration

Avec `DEMO_DATA_ENABLED=true`, le démarrage crée un jeu de données suffisant pour voir
l'application peuplée — offres, candidatures, missions, volumes permettant d'observer la
pagination :

| Rôle | Adresse |
|---|---|
| Intérimaire | `test@jobseeker.com` |
| Employeur | `test@employer.com` |
| Agence | `test@admin.com` |

Le mot de passe commun est celui de `DEMO_DATA_PASSWORD`. L'amorçage est sans effet si
les comptes existent déjà.

> À laisser à `false` hors développement. Un garde-fou (`ProductionGuard`) refuse
> d'ailleurs le démarrage si le jeu de démonstration, les emails simulés ou un
> `ddl-auto` destructeur cohabitent avec une configuration HTTPS.

---

## Tests

```bash
cd backend && ./mvnw test          # 276 tests
```

```bash
cd frontend
npm run build                      # contrôle des types + construction
npx oxlint src                     # analyse statique
```

Les tests du backend ne demandent **ni base de données ni fichier `.env`** : ils tournent
sur une H2 en mémoire, avec leur propre configuration. Ils couvrent le score de
correspondance, le cycle de vie des missions, les offres et candidatures, le contrat et
sa signature, le profil et le CV, la recherche, les disponibilités, la messagerie,
l'accès employeur, l'export RGPD et le journal d'audit.

Deux d'entre eux méritent d'être signalés parce qu'ils vérifient ce qu'un test ordinaire
laisse passer : `ContractDocumentTests` **extrait le texte du PDF produit** pour y
chercher les mentions imposées par la loi du 24 juillet 1987 — un paragraphe peut être
construit sans jamais être ajouté au document, et rien ne le signalerait ; et
`EncryptionTests` relit les colonnes chiffrées **en SQL brut**, en contournant JPA, parce
qu'un convertisseur qui ne chiffre rien relit parfaitement ce qu'il a écrit.

---

## Structure du dépôt

```
backend/                      Spring Boot
  src/main/java/be/agence_interim/
    model/                    23 entités JPA et 21 énumérations
    repository/               Accès aux données (Spring Data)
    service/                  32 services : toute la logique métier
    controller/               26 contrôleurs REST
    dto/                      Objets d'entrée et de sortie de l'API
    security/                 Authentification, CSRF, quotas, chiffrement
    config/                   Configuration, amorçage, garde-fous
  src/test/java/              276 tests
  .env.example                Modèle de configuration

frontend/                     React + Vite
  src/
    pages/                    40 écrans, regroupés par rôle
    components/               Briques d'interface partagées
    api/                      Appels HTTP typés
    auth/  chat/  missions/   Contextes et logique par domaine
```

---

## Choix et limites assumées

Quelques décisions valent d'être connues avant de lire le code.

**Le contact automatique des candidats a été retiré.** L'analyse prévoyait qu'une offre
publiée écrive d'office à tout candidat dépassant un seuil de correspondance. C'était le
seul email que la plateforme envoyait sans que son destinataire ait rien demandé, et le
seul à partir à plusieurs personnes d'un coup — proportionnellement au vivier, non au
nombre d'offres. Rien ne permettait de s'y soustraire : ni case à cocher à l'inscription,
ni lien de désinscription, ni réglage dans le profil, alors que le RGPD ouvre un droit
d'opposition. Plutôt que d'ajouter un consentement à un travail qui touchait à sa fin,
l'envoi a été supprimé. Le rapprochement, lui, demeure : le candidat le déclenche
lui-même en ouvrant « Pour moi ». Le seuil de contact a disparu avec l'email qu'il
servait à décider.

**Le score ne tient pas compte des disponibilités.** L'analyse les cite pourtant parmi
les données qui affinent les propositions. La raison tient au modèle de données : une
offre d'emploi ne porte aucune date, seule la mission en porte. Le score n'a donc rien à
confronter aux indisponibilités déclarées.

**Le classement de « Pour moi » se fait en mémoire.** Un score de correspondance ne
s'exprime pas en SQL : toutes les offres retenues par les filtres doivent être évaluées
avant qu'un classement existe. Les filtres, eux, sont appliqués en base — on ne score que
ce qui a survécu aux critères.

**Le schéma est encore géré par Hibernate** (`ddl-auto=update`), sans migrations
versionnées. Cette limite s'est manifestée trois fois en développement : Hibernate
n'élargit pas une colonne existante, n'ajoute pas une colonne non nulle à une table déjà
peuplée, et ne rappelle rien au bon moment. Deux garde-fous (`SchemaGuard`,
`ProductionGuard`) transforment ces pannes différées en refus de démarrer explicites, en
attendant le passage à Flyway.

**Conteneurisation et diffusion multi-instances non réalisées.** La messagerie tient un
registre de sessions en mémoire, ce qui suppose une seule instance du backend. Le TLS est
prévu au niveau d'un reverse proxy, et le code frontend le prend déjà en compte.

---

## Documentation

L'analyse complète — cahier des charges, cas d'utilisation, diagrammes de séquence, MCD,
MLD, MPD et diagramme de classes — accompagne ce dépôt dans le dossier d'analyse du
travail de fin d'études.
