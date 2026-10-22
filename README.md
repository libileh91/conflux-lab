# ConfluxLab

Laboratoire pédagogique Java/Spring : création d'une famille fictive de trois personnes, suivi asynchrone et orchestration de six opérations partenaires simulées. Ce projet ne reproduit pas un SI AXA certifié et n'est pas destiné à la production.

## État actuel

- **INT-01 à INT-12 sont terminées dans leur périmètre** : socle, contrats, assemblage, frontières, acceptation, suivi, refus, stockage MongoDB, transitions atomiques et relecture après redémarrage de l'application. La clôture pédagogique du sprint 03 reste à confirmer. Le passage au sprint 03 a été demandé après les stories techniques du sprint 02, sans évaluation de la restitution orale. Le [sprint 01 est clôturé techniquement et documenté](docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) sur demande explicite ; la restitution orale de l'apprenant n'a pas été évaluée.
- `application` produit le seul JAR Boot exécutable ; Experience appelle l'interface Java publique de Capability, sans HTTP interne.
- Le dernier `clean verify` **non filtré** observé est celui d'INT-08 : **181 tests** (59 contrats, 43 Capability, 4 BFF, 75 assemblage/HTTP/architecture), aucun échec/erreur/ignoré. Le dernier `verify` ciblé, dans INT-15, compile et package le réacteur avec **4 cas réussis**, sans rejouer toutes les suites.
- Depuis **INT-10**, le POST conserve la demande avant d'acquitter. **Si l'insertion et la soumission réussissent, un POST valide retourne `202`, UUID, `PENDING` et des liens cohérents** ; depuis INT-15, une saturation peut refuser cette soumission après l'insertion. Le GET relit cette base ; MongoDB doit donc être accessible pour réussir ces opérations. Aucun partenaire n'est nécessaire.
- Le **GET retourne `200` pour une demande connue**, avec seulement `requestId`, `status` et `createdAt` ; UUID inconnu `404`, identifiant mal formé `400`. Le GET ne modifie ni statut ni date.
- L'ancien stockage mémoire n'existe plus en production ; il reste une doublure de test. Un `_id` UUID unique interdit l'écrasement à l'insertion. Depuis INT-14, un événement local et un listener asynchrone assurent la prise en charge ; aucun métier de création de famille, état final ou workflow CI n'est livré. **INT-12 démontre la conservation après redémarrage du JAR avec la même base** ; MongoDB lui-même n'a pas été redémarré.
- **INT-09** a défini les dates et le graphe `PENDING → PROCESSING → SUCCEEDED/FAILED`, sans sauvegarde de transition à cette étape. **INT-10 : 2 nouveaux tests + 1 test POST/GET existant réussis** avec un MongoDB temporaire réel ; panne d'insertion simulée renvoyant `500`, sans faux reçu. À cette étape INT-10, suite globale et JAR non relancés.

- **INT-11** ajoute une transition MongoDB conditionnelle et atomique, sans upsert : un seul appelant prend la demande en charge, les dates ne reculent pas et un état final ne peut être écrasé. **2 nouveaux tests + 2 tests du modèle passent à cette étape** ; le listener INT-14 utilise désormais cette opération en production. Le reçu POST reste `PENDING` et le GET demeure en lecture seule, mais peut voir `PROCESSING`.
- **INT-12** branche trois classes `*IT` à Failsafe et ajoute **un seul scénario** : deux JVM successives exécutent le même JAR et retrouvent le même UUID, `PENDING` et `createdAt` sur le même MongoDB temporaire. Les quatre cas MongoDB préexistants sont réutilisés ; processus et conteneurs temporaires sont arrêtés à l'issue de la vérification.

- **INT-13** est [simplifiée](notes/sprint-04/INT-13-dessiner-les-fenetres-de-panne.md) : un schéma et trois cas essentiels (insertion non confirmée, `PENDING` sans prise en charge, traitement interrompu). Documentation uniquement ; sa simplification et la consigne d'équilibre sont commitées localement dans `6081781`.
- **INT-14 est terminée dans son périmètre** : publication après insertion, listener `@Async` et prise en charge atomique. **Un seul scénario ajouté, 9 cas ciblés réussis** ; le code s'arrête à `PROCESSING`, sans métier fictif. Le pool dédié et les erreurs sont traités ensuite dans INT-15. INT-14 et le plan allégé sont commitées localement dans `bdb93f8`.
- **INT-15 est terminée dans son périmètre** : `familyRequestExecutor` dédié, défauts modifiables de 2 workers/16 places/5 s d'attente de fermeture, rejet explicite et journal async UUID/type sans message privé. **Deux cas ajoutés, 4 cas ciblés réussis** ; aucun état final, stockage de diagnostics ou reprise. INT-15 et les notes enrichies sont commitées dans `59c881c`.
- **INT-16 est terminée dans son périmètre** : preuves consolidées, câblage et synchronisation des trois cas événementiels relus, deux rapports ciblés existants inspectés. Aucun code, test ou réglage modifié, aucun test ajouté ou relancé. La clôture pédagogique du sprint 04 reste à confirmer.
- **INT-17 est terminée dans son périmètre** : six opérations internes décrites par `FamilyOperations`, identifiants typés et fixture Mockito à futures pilotables. `test-compile` ciblé réussi, sans test exécuté. Aucun branchement au listener ni parcours métier ajouté ; INT-18 attend son feu vert.

## Prérequis

- JDK **21**, avec `java` et `javac` accessibles ; `JAVA_HOME` peut désigner son installation.
- Pas besoin de Maven préinstallé : utiliser les scripts du dépôt. Le Wrapper 3.3.4 télécharge Maven **3.9.16**, avec empreinte SHA-256 épinglée, lors du premier lancement.
- Accès réseau à Maven Central au premier lancement et pour les dépendances non mises en cache.
- **Docker disponible pour les tests Spring de l'assemblage** : Testcontainers démarre un MongoDB standalone `mongo:8.0.16`, supprimé en fin de JVM. Les tests unitaires des autres modules n'en ont pas besoin. Testcontainers est épinglé en 1.21.4 pour la compatibilité Docker récente.
- Pour l'usage local de l'application : MongoDB accessible, configuré via `MONGODB_URI`. Par défaut : `mongodb://localhost:27017/conflux_lab?serverSelectionTimeoutMS=3000&connectTimeoutMS=3000&socketTimeoutMS=5000&retryWrites=false`. Aucun secret réel dans les exemples.

## Vérifications depuis la racine

Sur Linux/macOS, commandes de repérage exécutées dans les étapes précédentes :

```sh
./mvnw -v
./mvnw -B -ntp validate
./mvnw -B -ntp -pl contracts -am test
```

Ces commandes ont réussi historiquement sous Linux avec Java 21. La sélection `-pl contracts` ne compile pas Experience.

### Vérification ciblée INT-15 — exécutée

```sh
./mvnw -B -ntp -pl application -am '-Dtest=ModuleArchitectureTest#experienceOnlyUsesThePublicCapabilityApi' -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=FamilyRequestEventsIT verify
```

**Résultat observé le 2026-10-22 : 1 cas Surefire + 3 cas Failsafe réussis**, aucun échec/erreur/ignoré. Le passage couvre la frontière publique, le cas concurrent réutilisé, la saturation et l'erreur async corrélée. La [note détaillée INT-15](notes/sprint-04/INT-15-borner-l-execution.md) donne le code, les réglages, la lecture des tests et leurs limites. Suite complète, arrêt prolongé et redémarrage du JAR non exécutés dans cet incrément.

### Vérification historique INT-14 — exécutée

Le `verify` ciblé du 2026-10-22 réussit avec **5 cas Surefire + 4 cas Failsafe**, aucun échec/erreur/ignoré. Il couvre publication, suivi HTTP, frontière Experience/Capability, double réception avec worker bloqué, insertion et conservation de `PROCESSING` après redémarrage du JAR. La [note INT-14](notes/sprint-04/INT-14-publier-et-consommer.md#8-commande-exécutée-et-résultat) donne la commande exacte, les rapports et les limites ; ce n'est pas une suite complète ni une preuve de reprise.

### Vérification historique INT-12 — exécutée

Depuis la racine, avec JDK 21 et Docker accessibles :

```sh
./mvnw -B -ntp '-Dtest=ModuleArchitectureTest#experienceOnlyUsesThePublicCapabilityApi' -Dsurefire.failIfNoSpecifiedTests=false clean verify
```

**Résultat observé le 2026-10-22 : `BUILD SUCCESS`, 1 cas Surefire + 5 cas Failsafe réussis, aucun échec/erreur/ignoré.** Tout le réacteur est compilé et packagé, mais seules ces vérifications sont exécutées :

- 1 contrôle ArchUnit de frontière Experience/Capability ;
- 2 cas d'insertion/échec de stockage dans `MongoFamilyRequestStoreIT` ;
- 2 cas de transitions atomiques dans `MongoFamilyRequestTransitionIT` ;
- 1 nouveau cas dans `FamilyRequestPersistenceIT`, avec POST/GET HTTP réels puis redémarrage du JAR sur la même base.

`-B -ntp` rend la sortie non interactive et sans progression des téléchargements. `-Dtest` sélectionne Surefire, pas Failsafe ; l'option `surefire.failIfNoSpecifiedTests=false` accepte que les autres modules n'aient pas le test sélectionné. `clean` supprime les sorties `target/`, notamment les anciennes classes après renommage `*Test` → `*IT`, pas les sources. `verify` atteint le packaging Boot puis les goals Failsafe `integration-test` et `verify`.

Rapports : `application/target/surefire-reports/` et `application/target/failsafe-reports/`. Le [cours INT-12](notes/sprint-03/INT-12-verifier-la-persistance.md) explique le cycle Maven, les deux JVM et les limites de la preuve. Les commandes INT-10/11 dans leurs notes restent historiques : leurs classes MongoDB portent désormais le suffixe `IT`.

### Vérification plus large — non exécutée sans filtre dans INT-12/14

```sh
./mvnw -B -ntp verify
```

Cette commande est configurée pour lancer les suites Surefire et les IT Failsafe ; **elle n'a pas été exécutée telle quelle dans INT-12/14**. Certains tests HTTP `*Test` utilisent eux aussi MongoDB : la phase Surefire n'est pas entièrement unitaire ou indépendante de Docker. Le dernier résultat non filtré reste celui d'INT-08, soit 181 cas à cette étape, avant MongoDB/Failsafe ; ce n'est pas le total courant validé.

Sous Windows/PowerShell, utiliser `./mvnw.cmd` à la place de `./mvnw` ; le script Windows est fourni mais son exécution n'a pas été testée dans cet environnement Linux. `.gitattributes` fixe LF pour le script Unix et CRLF pour le script Windows.

### Particularité réseau de cet environnement

[`.mvn/jvm.config`](.mvn/jvm.config) force IPv4 pour le processus Maven afin de contourner une panne IPv6 reproduite lors du dépannage. Ce réglage ne configure ni JDTLS ni la JVM d'une future application. À réévaluer avant usage sur un réseau exclusivement IPv6.

## Démarrage local

**Exemple manuel avec volume nommé, non exécuté dans INT-10/12/14.** Depuis la racine, pour un MongoDB local standalone avec volume conservé :

```sh
docker run -d --name conflux-lab-mongo -p 127.0.0.1:27017:27017 -v conflux-lab-mongo-data:/data/db mongo:8.0.16 --wiredTigerCacheSizeGB 0.25
./mvnw -B -ntp -DskipTests package
java -jar application/target/application-0.1.0-SNAPSHOT.jar --server.port=0 --server.address=127.0.0.1
```

La première commande crée un conteneur local et un volume ; le port n'est exposé que sur loopback. Elle suppose le nom et le port libres. Le cache WiredTiger est réglé à 0,25 Gio : **ce n'est pas une limite de mémoire totale**. Le volume `/data/db` conserve les données indépendamment de l'arrêt de l'application ; ne pas le supprimer pour tester un redémarrage. Pas d'authentification dans cet exemple local, jamais à exposer sur Internet. Un conteneur déjà créé peut être relancé par `docker start conflux-lab-mongo`, sans refaire `docker run`.

`-DskipTests package` produit le JAR sans relancer les tests ; ce n'est pas une preuve de validation. INT-10 n'avait pas produit ni lancé de JAR ; **INT-12 l'a depuis packagé et lancé deux fois automatiquement avec un MongoDB Testcontainers**, sans utiliser le volume nommé de cet exemple. `0` demande un port disponible, affiché par Tomcat ; pour ce démarrage manuel, arrêter l'application par `Ctrl+C`, puis éventuellement la base par `docker stop conflux-lab-mongo` (volume conservé). Aucun service sur 8081 ni propriété `CAPABILITY_BASE_URL` n'est requis.

**Si l'insertion et la soumission sont confirmées, un `POST /api/v1/families` valide retourne `202 Accepted`**, avec `requestId`, `status: PENDING`, `statusUrl` et `Location`. Les données fictives de `contracts/src/test/resources/contracts/create-family.json` servent à la démonstration. Capability valide aussi les appels Java directs et insère un snapshot du payload dans MongoDB avant d'acquitter. Un échec d'enregistrement ne produit pas d'acceptation ; les détails internes restent masqués.

Le lien `Location`/`statusUrl` est désormais **consultable par GET** : une demande connue retourne `200` et les trois métadonnées, sans payload personnel ni résultat métier. Un UUID inconnu reçoit `404 application/problem+json` avec `Request not found.` ; un identifiant manifestement mal formé reçoit `400` avec `Invalid request.`.

Le `202` signifie désormais « demande insérée avec acquittement MongoDB », pas famille créée ni exécution durable. Depuis INT-14, la publication suit l'insertion et le listener peut déjà avoir passé le document à `PROCESSING` lorsque le client reçoit le `202`. L'écriture impose `majority` avec journalisation ; sur cette instance seule, cela ne fournit ni réplication ni sauvegarde. Une déconnexion peut laisser l'issue d'une écriture inconnue, même avec un `500` : ne pas rejouer aveuglément un POST. La démonstration INT-12 retrouve les mêmes métadonnées après arrêt puis relance de l'application, avec la même base temporaire maintenue en fonctionnement ; elle ne teste ni redémarrage MongoDB ni perte du disque. Voir les cours [INT-10](notes/sprint-03/INT-10-persister-avant-d-accepter.md) et [INT-12](notes/sprint-03/INT-12-verifier-la-persistance.md) ; les cours INT-06/07 décrivent l'étape historique en mémoire.

### Refus et confidentialité

INT-08 vérifie les contraintes existantes aux niveaux HTTP et Java, l'absence d'appel à l'acceptation ou au store en cas de refus, et l'isolation de deux demandes. Une régression réelle a été corrigée : Spring recopiait un chemin rejeté dans `ProblemDetail.instance`. Une référence URI vide non nulle empêche cet écho ; avec la sérialisation actuelle, `instance` est omis. Statuts et détails génériques restent inchangés.

Les 181 cas étaient le total du projet à l'étape INT-08, pas 181 explications d'un même refus. Le [cours INT-08](notes/sprint-02/INT-08-tester-les-refus.md) distingue les scénarios essentiels de la matrice de non-régression, plus large que le minimum pédagogique.

## Architecture assemblée — un monolithe modulaire

| Module | Responsabilité | État |
|---|---|---|
| `contracts` | Types Java échangés entre modules ; ni métier, ni contrôleur, ni persistance | Cinq types et 59 cas de test dans INT-05 |
| `capability-api` | API Java publique, puis règles métier, statuts, stockage et orchestration ; le suffixe `-api` ne signifie pas un serveur autonome | Acceptation `PENDING`, MongoDB, événement local et prise en charge async `PROCESSING` ; métier non réalisé |
| `experience-api` | BFF mince : contrat HTTP, validation de forme et adaptation des réponses | POST `202`, GET `200` des métadonnées ; erreurs génériques `400`/`404`/`500` |
| `application` | Assemblage, seul démarrage Spring Boot et seul JAR Boot exécutable | Créé et démarré dans INT-03 |

Experience appelle l'API publique Java de Capability ; aucune dépendance inverse ni aucun accès à ses classes internes. INT-04 protège ces frontières avec ArchUnit 1.4.1 en scope `test` : cycles, sens des dépendances, API publique de Capability, indépendance de `contracts` et absence de retour vers l'assemblage. Des exemples fautifs et un exemple autorisé vérifient les mêmes règles ; ni ArchUnit ni ces fixtures ne sont livrés dans le JAR. Feign/Resilience4j sera réservé au partenaire externe. Voir le [DAT](docs/dat/ConfluxLab.md) pour le graphe et les limites.

## Travail et comptes rendus

- [Plan des sprints](docs/stages/README.md).
- [Sprint 01 — Socle](docs/stages/sprint-01-socle.md).
- [INT-01 — Frontières](notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md).
- [INT-02 — Build reproductible et diagnostic JDTLS](notes/sprint-01/INT-02-rendre-le-build-reproductible.md).
- [INT-03 — Assemblage unique et appel Java](notes/sprint-01/INT-03-assembler-les-modules.md).
- [INT-04 — Frontières testées et vérification des règles](notes/sprint-01/INT-04-verifier-les-frontieres.md).
- [INT-05 — Contrats et tests](notes/sprint-02/INT-05-definir-les-contrats.md).
- [INT-06 — Acceptation et stockage temporaire](notes/sprint-02/INT-06-accepter-une-demande.md).
- [INT-07 — Suivi des métadonnées et erreurs de consultation](notes/sprint-02/INT-07-consulter-la-demande.md).
- [INT-08 — Comprendre les refus et leurs preuves](notes/sprint-02/INT-08-tester-les-refus.md).
- [INT-09 — Données, dates et transitions internes](notes/sprint-03/INT-09-definir-les-donnees-et-transitions.md).
- [INT-10 — MongoDB avant acceptation](notes/sprint-03/INT-10-persister-avant-d-accepter.md).
- [INT-11 — Transitions conditionnelles atomiques](notes/sprint-03/INT-11-securiser-les-mises-a-jour.md).
- [INT-12 — Failsafe et persistance entre deux JVM](notes/sprint-03/INT-12-verifier-la-persistance.md).
- [INT-13 — Fenêtres de panne avant et après le `202`](notes/sprint-04/INT-13-dessiner-les-fenetres-de-panne.md).
- [INT-14 — Publication locale et prise en charge asynchrone](notes/sprint-04/INT-14-publier-et-consommer.md).
- [INT-15 — Executor borné, rejet et erreurs async](notes/sprint-04/INT-15-borner-l-execution.md).
- [INT-16 — Consolidation des preuves et synchronisation des tests](notes/sprint-04/INT-16-tester-avec-un-traitement-controle.md).
- [INT-17 — Contrats des six opérations et futures pilotables](notes/sprint-05/INT-17-ecrire-les-contrats-des-operations.md).
- [Spécification HTTP initiale](docs/dat/http-contracts.md).
- [Index des notes](notes/README.md).

Chaque tâche de développement exige le feu vert explicite de l'utilisateur. Une tâche autorisée ne déclenche pas automatiquement la suivante. La CD reste strictement théorique.
