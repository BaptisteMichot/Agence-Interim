# Image du backend Spring Boot, construite en deux étapes.
#
# POURQUOI DEUX ÉTAPES
# Compiler exige un JDK complet, Maven, et l'ensemble des dépendances téléchargées :
# près d'un demi-gigaoctet dont l'application n'a plus aucun besoin une fois le jar
# produit. La seconde étape repart d'une image ne contenant qu'un environnement
# d'exécution, et n'y recopie que le jar. Rien de ce qui a servi à construire ne se
# retrouve dans ce qui est livré — ni le code source, ni le compilateur.

# ------------------------------------------------------------------- étape 1 : build
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Le wrapper d'abord, et seul. Le dépôt est configuré en distributionType=only-script :
# `mvnw` télécharge Maven lui-même, il n'y a donc pas de maven-wrapper.jar à fournir.
COPY .mvn/ .mvn/
COPY mvnw ./

# Le bit exécutable n'est pas conservé quand le contexte de construction est lu depuis
# un système de fichiers Windows : sans ce chmod, `./mvnw` échoue en « Permission
# denied ». Le dépôt porte pourtant bien le mode 100755 dans git — c'est la copie vers
# le contexte qui le perd, pas le versionnement.
RUN chmod +x ./mvnw

# Le pom seul, avant le code source, pour que le téléchargement des dépendances forme
# une couche distincte. Docker ne réexécute une instruction que si ce dont elle dépend a
# changé : tant que le pom est inchangé, cette couche est reprise du cache et une
# modification de code ne relance pas le téléchargement de tout le dépôt Maven. Sans
# cette séparation, chaque reconstruction coûterait plusieurs minutes au lieu d'une.
COPY pom.xml ./
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

# Le code source ensuite : c'est lui qui change à chaque reconstruction.
COPY src/ src/

# Les tests ne sont pas rejoués ici. Ils le sont par l'intégration continue, sur chaque
# poussée, et les rejouer ajouterait près de deux minutes à chaque construction d'image
# sans rien vérifier de plus. Construire une image n'est pas le moment de découvrir
# qu'un test échoue — c'est le rôle de la CI, et elle est en place.
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

# --------------------------------------------------------------- étape 2 : exécution
FROM eclipse-temurin:25-jre AS runtime

# Un compte sans privilège : si l'application était compromise, l'attaquant ne serait
# pas root à l'intérieur du conteneur. C'est peu de chose, mais l'inverse — tourner en
# root sans raison — ne se défend pas.
RUN useradd --system --create-home --shell /usr/sbin/nologin agence
WORKDIR /app

# Les répertoires de dépôt sont créés et donnés à ce compte avant le changement
# d'utilisateur. Ce sont les points de montage des volumes déclarés par compose : sans
# cette appropriation, le conteneur ne pourrait écrire ni un CV ni un contrat, et
# l'échec n'apparaîtrait qu'au premier dépôt de fichier.
RUN mkdir -p /app/uploads/cv /app/uploads/contracts && chown -R agence:agence /app

COPY --from=build --chown=agence:agence /build/target/*.jar app.jar

USER agence

# Déclaratif : le port n'est pas publié pour autant, c'est compose qui en décide.
EXPOSE 8080

# La forme exec (et non `sh -c`) fait de la JVM le processus 1 du conteneur, ce qui lui
# transmet directement le SIGTERM d'un `docker compose down`. Passer par un shell
# intercalerait un processus qui ne relaie pas le signal, et l'arrêt se terminerait par
# un SIGKILL au bout du délai de grâce, sans fermeture propre des connexions.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
