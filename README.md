# ConfluxLab

Laboratoire pédagogique Java/Spring : création d'une famille fictive de trois personnes, suivi asynchrone et orchestration de six opérations partenaires simulées. Ce projet ne reproduit pas un SI AXA certifié et n'est pas destiné à la production.

## État actuel

- **INT-01 à INT-11 sont terminées dans leur périmètre** : socle, contrats, assemblage, frontières, acceptation, suivi, refus, stockage MongoDB et transitions atomiques. Le passage au sprint 03 a été demandé après les stories techniques du sprint 02, sans évaluation de la restitution orale. Le [sprint 01 est clôturé techniquement et documenté](docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) sur demande explicite ; la restitution orale de l'apprenant n'a pas été évaluée.
- `application` produit le seul JAR Boot exécutable ; Experience appelle l'interface Java publique de Capability, sans HTTP interne.
- Le dernier `clean verify` global observé est celui d'INT-08 : **181 tests** (59 contrats, 43 Capability, 4 BFF, 75 assemblage/HTTP/architecture), aucun échec/erreur/ignoré.
- Depuis **INT-10**, un **POST valide retourne `202`, UUID, `PENDING` et des liens cohérents après insertion acquittée dans MongoDB**. Le GET relit cette base ; MongoDB doit donc être accessible pour réussir ces opérations. Aucun partenaire n'est nécessaire.
- Le **GET retourne `200` pour une demande connue**, avec seulement `requestId`, `status` et `createdAt` ; UUID inconnu `404`, identifiant mal formé `400`. Le GET ne modifie ni statut ni date.
- L'ancien stockage mémoire n'existe plus en production ; il reste une doublure de test. Un `_id` UUID unique interdit l'écrasement à l'insertion. Aucun traitement, événement, asynchronisme ou workflow CI n'est livré. La conservation après redémarrage de l'application avec la même base reste à démontrer dans INT-12.
- **INT-09** a défini les dates et le graphe `PENDING → PROCESSING → SUCCEEDED/FAILED`, sans sauvegarde de transition à cette étape. **INT-10 : 2 nouveaux tests + 1 test POST/GET existant réussis** avec un MongoDB temporaire réel ; panne d'insertion simulée renvoyant `500`, sans faux reçu. Suite globale et JAR non relancés.

- **INT-11** ajoute une transition MongoDB conditionnelle et atomique, sans upsert : un seul appelant prend la demande en charge, les dates ne reculent pas et un état final ne peut être écrasé. **2 nouveaux tests + 2 tests du modèle passent** ; aucun appelant de production n'exécute encore ces transitions. Les POST restent normalement `PENDING` et le GET demeure en lecture seule.

## Prérequis

- JDK **21**, avec `java` et `javac` accessibles ; `JAVA_HOME` peut désigner son installation.
- Pas besoin de Maven préinstallé : utiliser les scripts du dépôt. Le Wrapper 3.3.4 télécharge Maven **3.9.16**, avec empreinte SHA-256 épinglée, lors du premier lancement.
- Accès réseau à Maven Central au premier lancement et pour les dépendances non mises en cache.
- **Docker disponible pour les tests Spring de l'assemblage** : Testcontainers démarre un MongoDB standalone `mongo:8.0.16`, supprimé en fin de JVM. Les tests unitaires des autres modules n'en ont pas besoin. Testcontainers est épinglé en 1.21.4 pour la compatibilité Docker récente.
- Pour l'usage local de l'application : MongoDB accessible, configuré via `MONGODB_URI`. Par défaut : `mongodb://localhost:27017/conflux_lab?serverSelectionTimeoutMS=3000&connectTimeoutMS=3000&socketTimeoutMS=5000&retryWrites=false`. Aucun secret réel dans les exemples.

## Vérifications depuis la racine

Sur Linux/macOS :

```sh
./mvnw -v
./mvnw -B -ntp validate
./mvnw -B -ntp -pl contracts -am test
```

Ces commandes ont réussi historiquement sous Linux avec Java 21. La sélection `-pl contracts` ne compile pas Experience. Pour INT-10, seule cette sélection ciblée a été exécutée avec succès après correction de la compatibilité Testcontainers :

```sh
./mvnw -B -ntp -pl application -am '-Dtest=MongoFamilyRequestStoreTest,FamilyRequestTrackingHttpTest#followsThePostLocationToPendingMetadataAndKeepsCreatedAtAcrossGets' -Dsurefire.failIfNoSpecifiedTests=false test
```

Résultat : **3 tests réussis**, dont 2 nouveaux. Le premier passage avec `clean test` avait échoué avant les tests métier, sur l'API Docker trop ancienne de Testcontainers 1.21.3 ; la 1.21.4 corrige ce problème. Le `clean` a supprimé les anciennes classes renommées et les sorties générées, pas les sources.

Le contrôle global reste disponible pour une validation plus large, mais **n'a pas été relancé pour INT-10** :

```sh
./mvnw -B -ntp clean verify
```

**Résultat observé dans INT-08 : succès du réacteur complet, 181 tests, aucun échec/erreur/ignoré.** Le build produit les trois bibliothèques et le JAR Boot d'assemblage. `clean` supprime les sorties `target/`, pas les sources. Une résolution de dépendances ou un simple `validate` ne remplace pas ce contrôle. Les avertissements Mockito d'auto-attachement d'agent sous Java 21 n'ont pas fait échouer les tests ; la compatibilité avec un futur JDK n'est pas validée ici.

Sous Windows/PowerShell, utiliser `./mvnw.cmd` à la place de `./mvnw` ; le script Windows est fourni mais son exécution n'a pas été testée dans cet environnement Linux. `.gitattributes` fixe LF pour le script Unix et CRLF pour le script Windows.

### Vérification ciblée INT-11

```sh
./mvnw -B -ntp -pl application -am -Dtest=StoredFamilyRequestTest,MongoFamilyRequestTransitionTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Résultat observé : 4 tests réussis**, dont 2 nouveaux tests MongoDB et 2 tests unitaires existants. Double prise en charge, transitions refusées, dates, états finaux et confidentialité du suivi sont vérifiés ; la suite globale et le JAR n'ont pas été relancés. Voir le [cours INT-11](notes/sprint-03/INT-11-securiser-les-mises-a-jour.md).

### Particularité réseau de cet environnement

[`.mvn/jvm.config`](.mvn/jvm.config) force IPv4 pour le processus Maven afin de contourner une panne IPv6 reproduite lors du dépannage. Ce réglage ne configure ni JDTLS ni la JVM d'une future application. À réévaluer avant usage sur un réseau exclusivement IPv6.

## Démarrage local

**Exemple manuel, non exécuté pendant INT-10.** Depuis la racine, pour un MongoDB local standalone avec volume conservé :

```sh
docker run -d --name conflux-lab-mongo -p 127.0.0.1:27017:27017 -v conflux-lab-mongo-data:/data/db mongo:8.0.16 --wiredTigerCacheSizeGB 0.25
./mvnw -B -ntp -DskipTests package
java -jar application/target/application-0.1.0-SNAPSHOT.jar --server.port=0 --server.address=127.0.0.1
```

La première commande crée un conteneur local et un volume ; le port n'est exposé que sur loopback. Elle suppose le nom et le port libres. Le cache WiredTiger est réglé à 0,25 Gio : **ce n'est pas une limite de mémoire totale**. Le volume `/data/db` conserve les données indépendamment de l'arrêt de l'application ; ne pas le supprimer pour tester un redémarrage. Pas d'authentification dans cet exemple local, jamais à exposer sur Internet. Un conteneur déjà créé peut être relancé par `docker start conflux-lab-mongo`, sans refaire `docker run`.

`-DskipTests package` produit le JAR sans relancer les tests ; ce n'est pas une preuve de validation. Le `clean test` ciblé de la session n'a pas produit de JAR. La commande `java -jar` correspond aux lancements historiques INT-03/06/07/08, mais n'a pas été rejouée avec MongoDB dans INT-10. `0` demande un port disponible, affiché par Tomcat ; arrêter l'application par `Ctrl+C`, puis éventuellement la base par `docker stop conflux-lab-mongo` (volume conservé). Aucun service sur 8081 ni propriété `CAPABILITY_BASE_URL` n'est requis.

**À ce stade, `POST /api/v1/families` valide retourne `202 Accepted`**, avec `requestId`, `status: PENDING`, `statusUrl` et `Location`. Les données fictives de `contracts/src/test/resources/contracts/create-family.json` servent à la démonstration. Capability valide aussi les appels Java directs et insère un snapshot du payload dans MongoDB avant d'acquitter. Un échec d'enregistrement ne produit pas d'acceptation ; les détails internes restent masqués.

Le lien `Location`/`statusUrl` est désormais **consultable par GET** : une demande connue retourne `200` et les trois métadonnées, sans payload personnel ni résultat métier. Un UUID inconnu reçoit `404 application/problem+json` avec `Request not found.` ; un identifiant manifestement mal formé reçoit `400` avec `Invalid request.`.

Le `202` signifie désormais « demande insérée avec acquittement MongoDB », pas famille créée ni traitement lancé. L'écriture impose `majority` avec journalisation ; sur cette instance seule, cela ne fournit ni réplication ni sauvegarde. Une déconnexion peut laisser l'issue d'une écriture inconnue, même avec un `500` : ne pas rejouer aveuglément un POST. La démonstration de redémarrage avec la même base reste à faire dans INT-12. Voir le [cours INT-10](notes/sprint-03/INT-10-persister-avant-d-accepter.md) ; les cours INT-06/07 décrivent l'étape historique en mémoire.

### Refus et confidentialité

INT-08 vérifie les contraintes existantes aux niveaux HTTP et Java, l'absence d'appel à l'acceptation ou au store en cas de refus, et l'isolation de deux demandes. Une régression réelle a été corrigée : Spring recopiait un chemin rejeté dans `ProblemDetail.instance`. Une référence URI vide non nulle empêche cet écho ; avec la sérialisation actuelle, `instance` est omis. Statuts et détails génériques restent inchangés.

Les 181 cas sont le total du projet, pas 181 explications d'un même refus. Le [cours INT-08](notes/sprint-02/INT-08-tester-les-refus.md) distingue les scénarios essentiels de la matrice de non-régression, plus large que le minimum pédagogique.

## Architecture assemblée — un monolithe modulaire

| Module | Responsabilité | État |
|---|---|---|
| `contracts` | Types Java échangés entre modules ; ni métier, ni contrôleur, ni persistance | Cinq types et 59 cas de test dans INT-05 |
| `capability-api` | API Java publique, puis règles métier, statuts, stockage et orchestration ; le suffixe `-api` ne signifie pas un serveur autonome | Acceptation `PENDING`, validation, insertion et consultation MongoDB internes ; traitement non réalisé |
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
- [Spécification HTTP initiale](docs/dat/http-contracts.md).
- [Index des notes](notes/README.md).

Chaque tâche de développement exige le feu vert explicite de l'utilisateur. Une tâche autorisée ne déclenche pas automatiquement la suivante. La CD reste strictement théorique.
