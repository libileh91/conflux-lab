# INT-02 — Rendre le build reproductible

| Contexte | Description |
|---|---|
| Sprint | [01 — Socle](../../docs/stages/sprint-01-socle.md) |
| Date des preuves conservées | 2026-10-21 |
| Statut | **Terminée pour le build** : blocage levé dans INT-05 ; preuve historique pré-INT-03 : `clean verify` global réussi, 59 cas de test de contrats |
| Portée | Travaux INT-02 : parent Maven, trois bibliothèques, Wrapper et diagnostic d'import/navigation Java ; pas d'assemblage ni de contrat ajouté dans cette tâche |
| Sources des preuves | Ancienne version de cette note : fil de dépannage JDTLS/Maven, puis reprise Maven ; fichiers cités ci-dessous relus lors de la réécriture antérieure, avant INT-03 |
| Réécriture documentaire antérieure | Réorganisation pédagogique ; **aucune commande Maven, aucun test applicatif ni nouvelle requête LSP exécutés**. Les lectures Git locales et contrôles documentaires sont distincts des preuves historiques du build |

Les versions et choix décrits concernent **[Simulation ConfluxLab]**, pas la stack historique AXA. **Étape INT-05, avant INT-03 :** les types manquants ont été définis et testés ; le build global a réussi à cette étape et a permis de clôturer INT-02 pour son périmètre de build. Les descriptions A/B/C ci-dessous conservent le diagnostic historique, pas un blocage encore actif. La preuve historique figure en section 5.3 et dans le [tutoriel INT-05](../sprint-02/INT-05-definir-les-contrats.md). Cette validation ne réalisait pas à elle seule l'assemblage ou les stories suivantes.

**Repère temporel — avant INT-03.** Les extraits et preuves de ce cours décrivent le socle avant son assemblage. Depuis [INT-03 — Assembler les modules](INT-03-assembler-les-modules.md), `application` a été ajouté au réacteur et porte le seul démarrage/JAR Boot ; le BFF appelle `FamilyRequests` en Java. Le client HTTP interne, sa configuration et ses propriétés, `CapabilityException`, `ExperienceApplication` et le YAML d'Experience ont été supprimés. La validation ultérieure a été exécutée dans la session INT-03 et consignée dans sa note, sans être rejouée pour cette mise à jour documentaire ; elle ne réalise ni INT-04 ni INT-06/07.

**Sommaire**
- [1. Objectif et vocabulaire](#1-objectif-et-vocabulaire)
- [2. Comprendre le problème](#2-comprendre-le-problème)
- [3. Travail réalisé pas à pas](#3-travail-réalisé-pas-à-pas)
- [4. Commandes expliquées](#4-commandes-expliquées)
- [5. Vérifications et limites](#5-vérifications-et-limites)
- [6. Suivi centralisé](#6-suivi-centralisé)

## 1. Objectif et vocabulaire

L'objectif est de construire et d'importer le même projet avec des versions d'outils explicites, sans dépendre d'un Maven installé au hasard sur une machine. Cela aide aussi l'éditeur à retrouver les types Java. Ce n'est ni la création des contrats métier, ni le démarrage d'une application complète.

| Terme | Explication pour lire la suite |
|---|---|
| POM | Fichier `pom.xml` décrivant un projet Maven : identité, dépendances, modules et configuration du build |
| Coordonnées | Triplet `groupId:artifactId:version`, par exemple `dev.confluxlab:contracts:0.1.0-SNAPSHOT`, identifiant un artefact |
| Parent / agrégateur | Le parent transmet une configuration ; l'agrégateur énumère les projets construits ensemble. Le POM racine remplit les deux rôles |
| Réacteur | Ensemble des projets sélectionnés par une invocation Maven, ordonnés notamment selon leurs dépendances |
| Dépendance / plugin | Une dépendance fournit des classes au projet ; un plugin réalise une opération de build, comme compiler ou lancer des tests |
| Classpath / DTO | Le classpath contient les classes accessibles ; un DTO est un objet transportant des données. Un JAR accessible peut ne contenir aucun des DTO attendus |
| Wrapper / JAR | Le Wrapper lance le Maven prévu par le dépôt ; un JAR est une archive Java, pas nécessairement une application exécutable |

« Reproductible » signifie ici **socle et commandes explicités**. Aucun résultat ne démontre encore un build global réussi depuis un clone vierge, ni des archives identiques octet pour octet entre machines.

## 2. Comprendre le problème

### 2.1. Trois causes différentes, donc trois diagnostics

| Couche | Constat du dépannage initial | Pourquoi cela bloquait |
|---|---|---|
| Serveur Java de l'éditeur | JDTLS 1.63.0 contenait des composants exigeant Java 25, mais démarrait sous Java 21 | Le serveur de langage n'avait pas une JVM compatible ; une copie séparée de JDTLS 1.61.0 a été configurée dans les réglages utilisateur de Zed |
| Modèle Maven | Experience référençait le parent absent `dev.confluxlab:conflux-lab:0.1.0-SNAPSHOT` et `contracts`, sans POM/classes correspondants | `Non-resolvable parent POM` empêchait l'import correct ; les trois requêtes de définition testées renvoyaient alors une liste vide |
| Réseau Java | Première génération du Wrapper : `Network is unreachable` vers Central ; DNS renvoyant IPv6 avant IPv4 | La requête HTTPS Java réussissait en forçant IPv4, alors que le téléchargement Python réussissait déjà |

Après relance complète de Zed, le processus JDTLS du dépannage utilisait bien la copie 1.61.0 et annonçait `Ready`. L'avertissement `Using incubator modules: jdk.incubator.vector` apparaissait aussi lors de réussites : il ne suffisait pas à identifier la panne.

### 2.2. Ne pas confondre les périodes ni les niveaux de réussite

| Période | Ce qui lui appartient |
|---|---|
| **A — Fil de dépannage historique** | Correction JDTLS, création du socle Maven/Wrapper, installation locale des bibliothèques, résolution des dépendances et preuves de navigation par protocole |
| **B — Reprise Maven historique** | Feu vert explicite INT-01/INT-02 ; ajout de `.gitattributes`, README explicatif, nouvelles vérifications Maven et bootstrap du Wrapper depuis un cache de distribution neuf ; pas de nouvel essai GUI/LSP |
| **C — Réécriture documentaire** | Lecture des fichiers et explication des preuves A/B ; aucun nouveau résultat d'exécution ni réglage utilisateur de Zed pendant cette phase |
| **D — INT-05, avant INT-03** | Cinq types et tests ajoutés dans `contracts` ; 59 cas réussis et compilation globale réussie après nettoyage, sans assemblage de l'application |

Le feu vert historique ne couvrait ni INT-03 ni les contrats d'INT-05. **Serveur prêt → projet importé → dépendances résolues → navigation disponible** ne signifie pas **application compilable**. Maven peut trouver le JAR `contracts` alors que le compilateur ne trouve pas les classes attendues dans ce JAR.

## 3. Travail réalisé pas à pas

Les extraits suivants expliquent le socle créé en A/B et relu en C, **avant INT-03**. Ils ne sont pas des fichiers à recopier intégralement : INT-05 a rempli `contracts`, puis INT-03 a ajouté `application` et les dépendances de l'assemblage. En particulier, la liste historique à trois modules ci-dessous n'est plus la liste complète du POM racine. Le socle parent et les réglages du Wrapper restent conservés.

### 3.1. Donner au dépôt un parent et un agrégateur

Le [`pom.xml` racine](../../pom.xml) porte les coordonnées `dev.confluxlab:conflux-lab:0.1.0-SNAPSHOT`. Il hérite du parent Spring Boot :

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.7</version>
    <relativePath/>
</parent>
```

`<relativePath/>` vide évite de chercher ce parent Spring Boot dans un POM voisin ; Maven le résout via ses dépôts. Le parent Boot apporte notamment la gestion cohérente des versions de dépendances. **3.5.7 est conservé depuis l'ébauche**, pas présenté comme une mise à niveau de sécurité.

```xml
<packaging>pom</packaging>
<modules>
    <module>contracts</module>
    <module>capability-api</module>
    <module>experience-api</module>
</modules>
```

Le packaging `pom` fait de la racine un projet de configuration, pas une bibliothèque compilée. Dans cet extrait historique, `<modules>` regroupait les trois enfants depuis la racine ; INT-03 en a ajouté un quatrième, `application`. Cette liste ne crée aucune dépendance Java entre eux.

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

`release=21` fixe le niveau de langage, le bytecode et les API Java standard ciblés par le compilateur ; cela ne choisit pas automatiquement le JDK installé. UTF-8 rend explicite l'encodage des sources.

Les versions **Compiler 3.14.1** et **Surefire 3.5.4**, conservées depuis l'ébauche, sont centralisées dans `<build><pluginManagement>`. Cette section gère les paramètres des plugins utilisés : elle ne déclenche pas n'importe quel plugin à elle seule. Compiler et Surefire participent déjà au cycle standard des projets JAR.

### 3.2. Rattacher les trois enfants sans inventer leur contenu

Les POM [`contracts`](../../contracts/pom.xml), [`capability-api`](../../capability-api/pom.xml) et [`experience-api`](../../experience-api/pom.xml) déclarent le même parent local :

```xml
<parent>
    <groupId>dev.confluxlab</groupId>
    <artifactId>conflux-lab</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

Ici, `../pom.xml` vise réellement le parent dans le dépôt. Chaque enfant fournit son `artifactId` et hérite notamment du groupe, de la version et des réglages communs. Sans packaging explicite, le packaging Maven par défaut est `jar`.

En A/B/C, `contracts` et `capability-api` n'avaient ni dépendance déclarée ni source Java. À la fin d'INT-05, `contracts` contenait les DTO, l'API Jakarta Validation et des dépendances de test ; Capability était encore vide, avant son câblage dans INT-03. Experience déclarait les starters Boot web, validation et test, ce dernier avec `scope=test`, ainsi que :

```xml
<dependency>
    <groupId>dev.confluxlab</groupId>
    <artifactId>contracts</artifactId>
    <version>${project.version}</version>
</dependency>
```

`${project.version}` reprend ici `0.1.0-SNAPSHOT`. Cette dépendance rend l'artefact accessible ; elle ne fabrique pas ses classes. Les imports suivants étaient présents dans la version pré-INT-03 de [`FamilyController.java`](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java) et dans l'ancien `experience-api/src/main/java/dev/confluxlab/experience/CapabilityClient.java` (**supprimé dans INT-03**) :

```java
import dev.confluxlab.contracts.CreateFamilyRequest;
import dev.confluxlab.contracts.RequestAccepted;
import dev.confluxlab.contracts.RequestView;
```

Ces trois types absents expliquaient l'échec global en A/B/C ; INT-05 les définit et lève ce blocage. Aucun DTO provisoire, profil d'exclusion, `skipTests` ou service métier n'a été ajouté pour masquer le problème.

L'exécution Boot `repackage` a été retirée de la bibliothèque Experience pendant le dépannage. Les JAR reconstruits de `contracts` et `capability-api` ne contenaient pas de `BOOT-INF/`. Le POM effectif d'Experience ne déclarait pas de plugin Boot actif. Ce constat historique ne prouvait pas d'assemblage exécutable. Après INT-05, Experience compilait sous forme de bibliothèque, sans application assemblée à cette date ; INT-03 a depuis ajouté l'exécutable séparé `application`.

Le client HTTP interne, `experience-api/src/main/java/dev/confluxlab/experience/ExperienceApplication.java` et `experience-api/src/main/resources/application.yml` existaient encore à la fin d'INT-05. L'URL `localhost:8081` de cet ancien YAML appartenait à l'ébauche, pas à la cible monolithe. **Ces fichiers ont été supprimés dans INT-03**, qui a réalisé l'assemblage unique et l'appel Java interne ; le démarrage et la configuration résident désormais dans `application`.

### 3.3. Épingler Maven avec le Wrapper et son empreinte

Les scripts officiels [`mvnw`](../../mvnw) et [`mvnw.cmd`](../../mvnw.cmd) proviennent du Wrapper **3.3.4**, type `only-script` : aucun JAR de Wrapper n'est ajouté au dépôt. Le fichier [`.mvn/wrapper/maven-wrapper.properties`](../../.mvn/wrapper/maven-wrapper.properties) contient :

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
distributionSha256Sum=5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce
```

**3.3.4 est la version du mécanisme Wrapper ; 3.9.16 est celle de Maven exécuté.** L'URL fixe la distribution ; l'empreinte SHA-256 permet au script de refuser un téléchargement ne correspondant pas au fichier attendu. Elle ne contrôle pas toutes les dépendances applicatives et suppose que les propriétés du dépôt sont dignes de confiance.

Le dépannage rapporte un choix des versions après consultation de Central et un contrôle SHA-512 officiel de la distribution de bootstrap avant son exécution ; cette consultation et ce contrôle ne sont pas refaits aujourd'hui. Le SHA-256 ci-dessus est bien lisible dans la configuration actuelle.

Au premier lancement, le Wrapper télécharge et extrait Maven ; les suivants réutilisent normalement `~/.m2/wrapper/dists/`. Le dépôt local des dépendances est, par défaut, `~/.m2/repository/` : **ce sont deux caches différents**.

### 3.4. Séparer réglage réseau, JVM et portabilité des scripts

Le fichier [`.mvn/jvm.config`](../../.mvn/jvm.config) contient une option transmise à la JVM de Maven :

```text
-Djava.net.preferIPv4Stack=true
```

Elle contourne la panne IPv6 reproduite sur cet environnement ; ce n'est ni une propriété métier ni une modification de `release=21`.

| JVM | Rôle et portée du réglage |
|---|---|
| JDTLS | Analyse le projet pour l'éditeur ; son JDK et ses arguments relèvent du lancement du serveur, pas de `.mvn/jvm.config` |
| Maven | Exécute les plugins du build ; lit ici l'option IPv4 du projet |
| Application | Exécute Spring Boot ; son lancement n'hérite pas automatiquement de ce fichier Maven |

Ce contournement ne convient pas nécessairement à un réseau exclusivement IPv6. Le téléchargement initial du Wrapper peut utiliser `wget`, `curl` ou un mécanisme Java distinct : le fichier JVM de Maven ne doit pas être présenté comme un réglage universel de tous ces outils.

La reprise B a ajouté [`.gitattributes`](../../.gitattributes) :

```gitattributes
mvnw text eol=lf
mvnw.cmd text eol=crlf
```

LF préserve le script Unix lors des checkouts ; CRLF convient au script Windows. Ces attributs ne remplacent pas le droit d'exécution Unix. Aucune renormalisation ni mise en index des fichiers utilisateur n'a été effectuée dans cette reprise.

Le [`.gitignore`](../../.gitignore) ignore les sorties `target/` et les métadonnées Eclipse/JDTLS `.project`, `.classpath`, `.settings/`, `.factorypath`, sans supprimer les fichiers locaux. Le [README](../../README.md), dans sa version complétée en B, expliquait les prérequis, commandes, frontières et blocage ; il ne présentait pas encore le projet comme une application exécutable.

## 4. Commandes expliquées

### 4.1. Cadre d'exécution et options communes

**Les commandes des sections 4.1 à 4.4 décrivent les vérifications historiques A/B, depuis la racine `conflux-lab/`.** L'exécution ultérieure d'INT-05 est distinguée en section 5.3. Prérequis communs : JDK 21 (`java` et `javac`, éventuellement `JAVA_HOME`), script exécutable sous Linux, outils de téléchargement/extraction/contrôle d'empreinte et réseau pour les éléments non cachés. Aucun Maven préinstallé n'est nécessaire, ni Docker pour ces contrôles historiques des bibliothèques alors vides.

| Option | Effet précis |
|---|---|
| `-B` | Mode batch, sans interaction ; ne signifie ni « build complet » ni « ignorer les erreurs » |
| `-ntp` | Masque la progression des transferts pour des logs plus lisibles ; les téléchargements restent possibles |
| `-pl contracts,capability-api` | Sélectionne ces projets ; ne vérifie pas Experience |
| `-am` | Ajoute les projets requis par la sélection dans le réacteur ; n'ajoute pas tous les projets qui en dépendent |
| `clean` | Cycle séparé supprimant les sorties du build, notamment `target/` des projets concernés ; ne vide pas le dépôt local Maven |

`./mvnw -v` affiche les versions de Maven et de sa JVM sans compiler. **A et B : succès, Maven 3.9.16 et Java Temurin 21.0.12.1**. Cela confirme le lancement de l'outil, pas celui de l'application.

### 4.2. Phases : valider le modèle, construire, puis installer localement

Vue simplifiée du cycle principal : `validate → compile → test → package → verify → install`. Demander une phase exécute les étapes précédentes de ce cycle, pour les projets sélectionnés. `clean` n'appartient pas à cette chaîne.

Voici les trois contrôles de la reprise, depuis la racine. **Ce bloc explique les exécutions historiques ; il n'est pas relancé par la réécriture.**

```sh
# Structure des POM : succès observé, sans compilation Java.
./mvnw -B -ntp validate

# Nettoyage puis build des deux bibliothèques vides seulement : succès observé.
./mvnw -B -ntp -pl contracts,capability-api -am clean verify

# Projet entier : échec observé sur les trois DTO manquants.
./mvnw -B -ntp verify
```

| Commande historique | But, effets et prérequis particuliers | Résultat observé |
|---|---|---|
| `./mvnw -B -ntp validate` | Vérifier la validité du modèle/projet ; le parent doit être résolvable. Ne compile pas les sources | **A/B : succès des quatre POM**, parent compris |
| `./mvnw -B -ntp verify` | Compiler, exécuter les tests configurés et produire/vérifier les artefacts ; exige notamment que les classes importées existent | **A/B : échec dans Experience** sur les trois DTO ; les phases suivantes de ce module ne sont pas atteintes |
| `./mvnw -B -ntp -pl contracts,capability-api -am clean verify` | Nettoyer puis reconstruire le sous-réacteur ; distingue ses sorties neuves d'anciens fichiers générés | **B : succès**, bibliothèques vides, aucun test métier |
| `./mvnw -B -ntp -pl contracts,capability-api -am install` | Aller jusqu'à `verify`, puis copier POM/JAR dans le dépôt local pour une autre invocation Maven ; ce n'est pas un déploiement distant | **A : succès**, parent et deux bibliothèques installés localement, sans sources métier ni tests à exécuter |

Un succès limité à deux bibliothèques vides n'est pas un succès global. La présence de Surefire ou de `spring-boot-starter-test` ne crée aucun test. `verify` ne garantit pas non plus des tests d'intégration qui ne seraient pas raccordés au cycle Maven.

### 4.3. Goals de diagnostic : résoudre et comprendre l'héritage

Contrairement à une phase, `dependency:resolve` et `help:effective-pom` sont des **goals de plugins** ciblés : ils ne lancent pas automatiquement toute la chaîne de compilation/tests.

```sh
./mvnw -B -ntp -pl experience-api dependency:resolve
./mvnw -B -ntp -pl experience-api help:effective-pom -Doutput=target/effective-pom.xml
```

Dans le graphe de dépendances de l'époque, la première commande résolvait les dépendances d'Experience. Sans `-am`, elle ne construisait pas `contracts` : l'installation locale précédente rendait cet artefact disponible. Depuis INT-03, Experience dépend aussi de `capability-api` ; ce résultat historique n'atteste pas la résolution du nouveau graphe. **A : succès**, dont `contracts`, les starters Boot 3.5.7 et Spring Web 6.2.12. Aucun contrôle du contenu métier du JAR n'est démontré par ce succès.

La seconde écrit le modèle effectif, c'est-à-dire le résultat de l'héritage et de la configuration Maven, dans `target/effective-pom.xml` du module ciblé. Elle nécessite la résolution des parents et du plugin d'aide, pas une compilation réussie. **A/B : succès** ; lecture XML par Python : cible 21, Compiler 3.14.1, Surefire 3.5.4 et absence de plugin Boot dans les plugins actifs d'Experience.

### 4.4. Génération historique et bootstrap depuis un cache neuf

La génération A utilisait le goal `org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.9.16 -Dtype=only-script`, avec empreinte de distribution. Ce goal **écrit les fichiers du Wrapper** : ce n'est pas une simple vérification. Première tentative bloquée par le réseau ; seconde réussie avec `MAVEN_OPTS=-Djava.net.preferIPv4Stack=true`. Le lanceur initial complet n'est pas conservé dans la preuve, donc il n'est pas reconstitué ici.

La reprise B a ensuite exécuté cette commande sur la machine historique :

```sh
MAVEN_USER_HOME=/home/lob/workspace/java/conflux-lab/target/int02-wrapper-home MVNW_VERBOSE=true ./mvnw -v
```

`MAVEN_USER_HOME` donne au Wrapper un emplacement de cache distinct ; le chemin absolu ci-dessus n'est pas portable tel quel. `MVNW_VERBOSE=true` détaille son téléchargement. Prérequis propre à cette preuve : cache de distribution **initialement vide**, accès à Central, outils et droits d'écriture. Résultat B : téléchargement officiel Maven 3.9.16, contrôle SHA-256 configuré et démarrage réussis ; fichiers sous `target/`, ignorés par Git.

## 5. Vérifications et limites

### 5.1. Preuves JDTLS du fil A : protocole, pas interface graphique

Après correction Maven, l'import a été actualisé sur le **JDTLS déjà utilisé par Zed**, via son proxy HTTP local, sans nouveau redémarrage de l'éditeur. La requête conservée est :

```json
{
  "method": "workspace/executeCommand",
  "params": {
    "command": "java.project.changeImportedProjects",
    "arguments": [
      [],
      ["file:///home/lob/workspace/java/conflux-lab/experience-api"],
      []
    ]
  }
}
```

`result: null` ne prouve pas seul la fin d'un travail en arrière-plan. Le log confirmait ensuite `Projects updated in 1058 ms`, à `2026-10-21 00:01:27.633`. Le diagnostic du POM Experience ne rapportait alors plus d'erreur ni d'avertissement ; cela ne supprimait pas les types Java manquants.

Les requêtes `textDocument/definition` suivantes ont interrogé ce serveur réel, pas seulement un projet temporaire. Les lignes sont celles de la preuve historique :

| Type utilisé dans `FamilyController.java` | Définition obtenue après correction |
|---|---|
| `CapabilityClient`, paramètre du constructeur, ligne 24 | `CapabilityClient.java`, ligne 21 |
| `UUID`, ligne 39 | `java.util.UUID`, ligne 75 du contenu fourni par JDTLS |
| `ResponseEntity`, ligne 29 | `org.springframework.http.ResponseEntity`, Spring Web 6.2.12, ligne 80 du contenu fourni par JDTLS |

Pour les deux URI externes `jdt://…`, `java/classFileContents` a renvoyé du Java non vide : **523 lignes pour `UUID`, 623 pour `ResponseEntity`**. Ce contenu peut être décompilé ; ce n'est pas une preuve de téléchargement des archives de sources originales.

Le premier appel à `ResponseEntity` a dépassé les cinq secondes autorisées par le proxy ; une seule reprise a obtenu la définition en **0,060 seconde**. La cause du premier délai n'a pas été établie.

F12 ou Ctrl + Shift + B déclenchent la navigation depuis un nom de type sous le curseur ; ils ne construisent pas le classpath. **La pression des touches et l'ouverture réelle de l'onglet cible n'ont pas été testées.** Le routage serveur et la disponibilité du contenu ont été vérifiés en A, pas la GUI. Aucun nouvel essai LSP/GUI n'appartient à B ou C.

### 5.2. Contrôles complémentaires et portée réelle des preuves

| Contrôle historique | Résultat et limite |
|---|---|
| Inspection ZIP des JAR, A/B | Pas de `BOOT-INF/` dans `contracts` et `capability-api` ; ne démontre ni le JAR d'Experience ni un démarrage Boot |
| Syntaxe et permissions Unix, B | `sh -n mvnw` réussissait ; droit exécutable présent sous Linux. Pas une preuve d'exécution Windows |
| Attributs de fin de ligne, B | `git check-attr text eol -- mvnw mvnw.cmd` confirmait LF/CRLF ; commande historique, non relancée aujourd'hui |
| Contrôles documentaires, B | Vérification Python des liens locaux du README, des fiches et des notes réussie ; INT-01 cochée, INT-02/03/04 décochées et absence de nouveaux DTO vérifiées à cette date |

**Cache Wrapper neuf ≠ cache de dépendances neuf.** Le test avec `-v` porte sur le téléchargement/démarrage de Maven ; il ne compile rien et ne démontre ni la résolution de toutes les dépendances sans cache, ni un build depuis un clone neuf. Les sorties d'exécution historiques ne sont pas reproduites par cette réécriture. Les liens/ancres et la correspondance des extraits avec les fichiers de l'époque avaient fait l'objet d'un contrôle documentaire distinct ; il ne prouvait ni nouveau build ni nouvelle navigation LSP. Les extraits pré-INT-03 sont conservés comme preuves historiques, pas comme copies exactes de toutes les sources actuelles.

`mvnw.cmd` est fourni, mais **non exécuté sous Windows**. Aucun test métier, démarrage de l'application, parcours HTTP, test d'intégration, CI ou CD n'est démontré par ces preuves. Les bibliothèques vides expliquent les succès partiels ; elles ne constituent pas une couverture de tests.

Le blocage historique venait du code/contrat, pas des raccourcis : il fallait définir les types dans INT-05, sans fabriquer de classes vides. Le critère du JAR Boot unique appartient à INT-03, pas à la clôture technique de ce build.

### 5.3. Validation après les contrats INT-05

Dans la session INT-05, les commandes suivantes ont réellement réussi depuis la racine :

```sh
./mvnw -B -ntp -pl contracts -am test
./mvnw -B -ntp clean verify
```

À la date d'INT-05, le second contrôle a reconstruit le parent et les trois bibliothèques, compilé les sept sources d'Experience et exécuté **59 cas de test de contrats, sans échec, erreur ou ignoré**. Les rapports Surefire ont été relus. Les trois JAR inspectés étaient ordinaires, sans `BOOT-INF/`.

INT-02 est donc terminée pour le socle et le build global. Ce succès ne valide ni la GUI Zed, ni Windows, ni un test HTTP, ni un service Capability, ni une CI. Les preuves et explications de ces 59 tests sont dans la [note INT-05](../sprint-02/INT-05-definir-les-contrats.md).

**Validation exécutée ultérieurement dans la session INT-03 :** 5 tests ciblés réussis, puis `clean verify` réussi avec **64 tests** (59 contrats, 2 contrôleur, 3 assemblage). Le JAR réel a démarré sur le port loopback `37961` ; POST valide et GET avec UUID ont répondu `503 application/problem+json`, sans `Location` ni `requestId`, puis le processus a été arrêté. Voir [INT-03](INT-03-assembler-les-modules.md). Ces preuves complètent l'historique sans remplacer les résultats INT-02/05 ; aucun build n'a été relancé ici et INT-06/07 restent non réalisées.

## 6. Suivi centralisé

- [TD-B001 — Types et contrats INT-05](../todos.md#td-b001)
- [TD-B002 — Build global INT-02, dépendant de TD-B001](../todos.md#td-b002)
- [TD-B006 — Navigation GUI F12 / Ctrl + Shift + B](../todos.md#td-b006)
- [TD-B007 — Wrapper Windows, contrôle optionnel](../todos.md#td-b007)
- [TD-R002 — POM et étapes Maven](../todos.md#td-r002)
- [TD-R003 — JVM JDTLS, Maven, application et réglage IPv4](../todos.md#td-r003)
- [TD-R007 — Wrapper épinglé, empreinte et caches](../todos.md#td-r007)
