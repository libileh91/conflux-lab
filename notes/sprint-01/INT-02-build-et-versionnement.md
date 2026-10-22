# INT-02 et INT-37 — Build reproductible et versionnement

**[Simulation ConfluxLab] — Résultat réalisé du [sprint 01](../../docs/stages/sprint-01-socle.md).** Le dépôt fournit les outils et réglages pour construire le projet et conserver des versions identifiables. INT-02 est débloquée par les contrats d'INT-05 ; INT-37 couvre l'initialisation Git et les premiers push, pas un succès applicatif.

Ce sujet complète [INT-01/03/04 — Architecture, assemblage et frontières](INT-01-socle-modulaire.md), sans en répéter le flux Spring ni les règles. Révision documentaire du 2026-10-22 : **aucun test, build, démarrage, commit, push ou accès distant exécuté**. Les résultats chiffrés restent des preuves historiques, pas une validation des sources actuelles.

## 1. Le vocabulaire Maven utile

| Terme | Sens concret |
|---|---|
| POM | `pom.xml` décrit l'identité du projet, ses dépendances et son build |
| Coordonnées | `groupId:artifactId:version` identifie un artefact, ici par exemple `dev.confluxlab:contracts:0.1.0-SNAPSHOT` |
| Parent / agrégateur | Le parent partage la configuration ; l'agrégateur liste les projets construits ensemble ; la racine remplit ces deux rôles |
| Réacteur | Ensemble des projets d'une invocation Maven, ordonnés notamment selon leurs dépendances |
| Dépendance / plugin | La dépendance fournit du code ; le plugin réalise une opération de build |
| Classpath / JAR | Ensemble des classes accessibles / archive Java, qui n'est pas forcément exécutable |
| Wrapper | Lance la version de Maven prévue par le dépôt, sans Maven préinstallé ; un JDK reste nécessaire |

Un JAR `contracts` résolu mais vide ne contient pas les DTO attendus. C'était le blocage initial : les POM pouvaient être valides et certaines bibliothèques construites, tandis qu'Experience ne compilait pas. INT-05 a défini les vrais types, sans classes vides ou exclusions pour cacher l'erreur.

## 2. Lire les réglages communs et les dépendances

Dans le [POM racine](../../pom.xml), extrait partiel du parent externe :

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.7</version>
    <relativePath/>
</parent>
```

Le parent Boot apporte une gestion cohérente de versions et de plugins. `<relativePath/>` vide évite de chercher ce parent dans un POM voisin. Les enfants utilisent au contraire `../pom.xml` pour retrouver le parent **du dépôt**. Boot 3.5.7 est conservé depuis l'ébauche, pas présenté comme une mise à jour de sécurité.

Autre fragment actuel du même fichier :

```xml
<packaging>pom</packaging>
<modules>
    <module>contracts</module>
    <module>capability-api</module>
    <module>experience-api</module>
    <module>application</module>
</modules>
```

La racine configure le build sans produire de bibliothèque Java. La liste regroupe les quatre modules, mais ne crée aucune dépendance entre eux ; leurs responsabilités et leur graphe sont dans le [cours architecture](INT-01-socle-modulaire.md#1-plusieurs-modules-une-seule-application).

Dans le [POM d'Experience](../../experience-api/pom.xml), extrait :

```xml
<dependency>
    <groupId>dev.confluxlab</groupId>
    <artifactId>capability-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

`${project.version}` reprend ici `0.1.0-SNAPSHOT`. Maven fournit l'artefact pour compiler, sans fabriquer ses classes ni ses beans Spring. Une dépendance peut apporter ses propres dépendances par **transitivité** ; ce n'est pas une communication réseau.

La racine fixe Java **21**, `maven.compiler.release=21` et UTF-8. `release` borne langage, bytecode et API Java standard ciblés ; il ne sélectionne pas le JDK installé. Dans `pluginManagement`, Compiler **3.14.1**, Surefire et désormais Failsafe **3.5.4** sont centralisés : gérer un plugin ne l'active pas à soi seul. L'activation du plugin Boot dans l'assemblage est expliquée dans l'[autre cours](INT-01-socle-modulaire.md#2-assembler-le-code-et-les-objets-spring).

## 3. Wrapper, versions, réseau et portabilité

Configuration de [`.mvn/wrapper/maven-wrapper.properties`](../../.mvn/wrapper/maven-wrapper.properties) :

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
distributionSha256Sum=5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce
```

Le **Wrapper 3.3.4** lance **Maven 3.9.16** : deux versions différentes. `only-script` fournit les lanceurs sans JAR de Wrapper. Le SHA-256 vérifie la distribution téléchargée par rapport à l'empreinte configurée ; il ne contrôle pas toutes les dépendances ni un dépôt compromis.

Deux caches distincts : `~/.m2/wrapper/dists/` pour les distributions Maven, `~/.m2/repository/` pour les dépendances et artefacts installés localement. Le bootstrap historique depuis un cache de distribution neuf exécutait seulement `-v` : **pas un build depuis un clone vierge ou un cache de dépendances neuf**.

Dans [`.mvn/jvm.config`](../../.mvn/jvm.config) :

```text
-Djava.net.preferIPv4Stack=true
```

Cette option contourne la panne IPv6 reproduite historiquement pour la JVM de Maven. Elle ne configure ni JDTLS ni la JVM applicative, et ne règle pas universellement le téléchargement initial du Wrapper. À réévaluer sur un réseau exclusivement IPv6 ; ce n'est pas une propriété métier.

Dans [`.gitattributes`](../../.gitattributes) :

```gitattributes
mvnw text eol=lf
mvnw.cmd text eol=crlf
```

Git conserve des fins de ligne adaptées aux scripts Unix/Windows. Cela ne remplace ni le droit exécutable Unix, ni un essai Windows. Le dépôt explicite ses outils ; **aucune preuve d'archives identiques octet pour octet entre machines** n'est fournie.

## 4. Commandes et prérequis, sans réexécution

Depuis la racine `conflux-lab/`, commandes de référence **non exécutées pour ce cours** :

```sh
# Afficher Maven et sa JVM ; ne compile pas le projet.
./mvnw -v

# Vérifier le modèle des projets, sans compilation des sources Java.
./mvnw -B -ntp validate

# Nettoyer les sorties générées puis construire et vérifier tout le réacteur.
./mvnw -B -ntp clean verify
```

`-B` évite les interactions ; `-ntp` masque la progression des transferts. `clean` supprime les `target/`, pas les sources, l'historique Git ou le cache Maven. La chaîne principale est `validate → compile → test → package → verify → install` ; `clean` est un cycle séparé. `install` copie les artefacts dans le dépôt **local**, sans publier à distance.

`-pl` sélectionne des projets ; `-am` ajoute leurs prérequis du réacteur, pas tous leurs consommateurs. Un succès partiel ne valide pas les modules exclus. Les goals `dependency:resolve` ou `help:effective-pom` aident à résoudre les dépendances/lire l'héritage ; ils ne prouvent pas une compilation ou une application fonctionnelle.

**Aujourd'hui :** JDK 21, téléchargement/extraction du Wrapper et réseau pour les éléments non cachés ; Docker/Testcontainers est nécessaire aux tests MongoDB du build non filtré. Failsafe est activé depuis INT-12 dans [`application/pom.xml`](../../application/pom.xml), avec `integration-test` et `verify` après le packaging. Certains `*Test` Surefire utilisent aussi MongoDB : le nom ne suffit pas à qualifier un test d'unitaire. `verify` ne prouve que les tests réellement raccordés et exécutés.

Pour le JAR, une base accessible est nécessaire à POST/GET. Dans [`application.yml`](../../application/src/main/resources/application.yml), extrait actuel sous `spring.data` :

```yaml
mongodb:
  uri: ${MONGODB_URI:mongodb://localhost:27017/conflux_lab?serverSelectionTimeoutMS=3000&connectTimeoutMS=3000&socketTimeoutMS=5000&retryWrites=false}
  uuid-representation: standard
```

`MONGODB_URI` remplace la valeur locale par défaut ; `standard` fixe la représentation BSON des UUID. Ce raccordement est postérieur au socle : les anciens prérequis « sans MongoDB » ne décrivent plus le parcours actuel. La [commande de lancement et ses preuves](INT-01-socle-modulaire.md#4-preuves-historiques-et-limites) restent dans le cours assemblage.

## 5. Git : choisir, enregistrer, puis publier

**Fichiers de travail → index (`git add`) → commit local → push distant.** L'index contient l'instantané préparé : une édition ultérieure n'y entre pas automatiquement. Un commit enregistre localement cet instantané, son parent éventuel et ses métadonnées ; il ne publie rien. Le push ne transmet pas les modifications non commitées.

Une **branche** est une référence mobile vers un commit ; `HEAD` désigne habituellement la branche courante. Git gère les versions ; GitHub héberge ici les commits publiés. Ni un commit ni un push ne prouvent que Maven compile ou que l'application fonctionne.

Dans [`.gitignore`](../../.gitignore), fragment :

```gitignore
.env
.env.*
!.env.example
!.env.sample
*.pem
*.key
```

Les `!` réautorisent des exemples qui doivent rester sans secret. Le fichier ignore aussi `target/`, classes, logs et métadonnées IDE. **Ignorer ne détecte pas tous les secrets, ne retire pas un fichier déjà suivi et n'efface pas l'historique.** Un secret exposé devrait être révoqué ; aucun incident n'est affirmé ici. Les `.gitkeep` préservent des dossiers vides : convention de fichier, sans rôle Git spécial ni effet Maven/Spring.

INT-37 a créé `main`, associé l'alias `origin` à l'URL HTTPS du projet et publié sans force, après contrôle du distant. L'option historique `-u` a établi le suivi de `origin/main` ; cette référence locale représente le dernier état distant connu, pas une interrogation permanente de GitHub. Aucun réglage global n'a été modifié et aucun helper précis n'est déduit du succès.

Les échecs historiques de `gh` (`401`) et de SSH (`publickey`) n'empêchaient pas Git HTTPS de fonctionner : **canaux d'authentification distincts**. L'identité auteur est aussi distincte de l'authentification distante. Aucun token, clé ou nouveau remote n'est ajouté ici.

Repères de lecture locale, non exécutés, depuis `conflux-lab/` :

```sh
# Voir les modifications sans écriture facultative de métadonnées.
git --no-optional-locks status --short
# Examiner l'instantané déjà préparé pour le prochain commit, sans pager.
git --no-pager diff --cached
```

Le diff de l'index ne remplace pas celui des modifications encore non préparées. Aucune recette d'initialisation ou de publication n'est relancée : le dépôt existe déjà et une opération distante demanderait son autorisation propre.

## 6. Preuves et limites historiques

| Étape | Résultat consigné | Portée exacte |
|---|---|---|
| Diagnostic/reprise INT-02 | Maven **3.9.16**, Temurin **21.0.12.1** ; bootstrap Linux du Wrapper depuis un cache de distribution neuf réussi | Outillage ; succès des bibliothèques alors vides ≠ succès global |
| INT-02 débloquée par INT-05 | Échec initial sur trois DTO absents, puis `clean verify` global : **59 cas**, 0 échec/erreur/ignoré | Contrats réels définis ; pas encore d'assemblage applicatif |
| INT-37 | `067e6a4`, **36 fichiers**, premier push HTTPS de `main` réussi ; `7cfee84` pour la note et ses liens, publié ensuite | Versionnement/publication de l'époque, sans preuve applicative ou nouvelle interrogation distante |

Les **64/76 tests** d'assemblage/frontières appartiennent au [cours INT-01/03/04](INT-01-socle-modulaire.md#4-preuves-historiques-et-limites) ; le nouveau passage de 76 tests avant publication reste au [journal de clôture du 2026-10-21](../../docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21). Aucun de ces chiffres ne décrit une exécution actuelle.

Le dépannage JDTLS séparait une JVM incompatible (1.63.0 exigeant Java 25, remplacé par 1.61.0 sous Java 21), le modèle Maven absent et les DTO manquants. Import et navigation Java/Spring ont été constatés **par protocole**, pas par pression de F12 ou ouverture visuelle d'un onglet. Un serveur de langage prêt ne garantit pas la compilation.

**Non vérifiés :** build depuis un clone vierge, exécution Windows de `mvnw.cmd`, interaction GUI Zed. Un état Git historiquement propre ne décrit pas les modifications actuelles ni les fichiers ignorés. Les avertissements Mockito/JVM n'empêchaient pas les builds historiques sous Java 21, sans promettre un futur JDK compatible. Restitution orale non évaluée ; ni CI verte ni déploiement déduits de ce travail.

## 7. Repères et suivi

- Maven : [TD-R002 — POM/cycle](../todos.md#td-r002), [TD-R003 — JVM/réseau](../todos.md#td-r003), [TD-R007 — Wrapper/caches](../todos.md#td-r007) ; blocages résolus [TD-B001 — contrats](../todos.md#td-b001), [TD-B002 — build global](../todos.md#td-b002).
- Git : [TD-R006 — commit/push/authentification](../todos.md#td-r006), [TD-B008 — gh/SSH si nécessaire](../todos.md#td-b008). Contrôles non constatés : [TD-B006 — GUI](../todos.md#td-b006), [TD-B007 — Windows optionnel](../todos.md#td-b007).
- [Cours architecture, assemblage et frontières](INT-01-socle-modulaire.md). Cette réorganisation ne donne aucun feu vert de développement, validation ou publication.
