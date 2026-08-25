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

> **Aucun TLS.** L'application est servie en clair, `COOKIE_SECURE` reste donc à `false` —
> un cookie marqué `Secure` serait ignoré par le navigateur. C'est aussi ce qui laisse
> `ProductionGuard` en simple avertissement au démarrage.

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
