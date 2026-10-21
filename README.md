# ConfluxLab

Laboratoire pédagogique Java/Spring : création d'une famille fictive de trois personnes, suivi asynchrone et orchestration de six opérations partenaires simulées. Ce projet ne reproduit pas un SI AXA certifié et n'est pas destiné à la production.

## État actuel

- **INT-01/02/03/04/05/06/07 sont terminées dans leur périmètre** : socle, contrats, assemblage, frontières, acceptation et suivi en mémoire. Le [sprint 01 est clôturé techniquement et documenté](docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) sur demande explicite ; la restitution orale de l'apprenant n'a pas été évaluée.
- `application` produit le seul JAR Boot exécutable ; Experience appelle l'interface Java publique de Capability, sans HTTP interne.
- Le `clean verify` global réussit : **108 tests** (59 contrats, 22 Capability, 4 BFF, 23 assemblage/HTTP/architecture), aucun échec/erreur/ignoré.
- Le JAR démarre sans MongoDB ni partenaire. Un **POST valide retourne `202`, UUID, `PENDING` et des liens cohérents après conservation en mémoire**. Deux POST identiques ont produit deux UUID distincts lors de la démonstration.
- Le **GET retourne `200` pour une demande connue**, avec seulement `requestId`, `status` et `createdAt` ; UUID inconnu `404`, identifiant mal formé `400`. Le GET ne modifie ni statut ni date.
- La mémoire est perdue au redémarrage et non bornée : une ancienne demande reçoit alors `404`. Aucun traitement, événement, asynchronisme ou workflow CI n'est livré ; INT-08 reste à autoriser.

## Prérequis

- JDK **21**, avec `java` et `javac` accessibles ; `JAVA_HOME` peut désigner son installation.
- Pas besoin de Maven préinstallé : utiliser les scripts du dépôt. Le Wrapper 3.3.4 télécharge Maven **3.9.16**, avec empreinte SHA-256 épinglée, lors du premier lancement.
- Accès réseau à Maven Central au premier lancement et pour les dépendances non mises en cache.
- Docker sera nécessaire pour les tests Testcontainers des sprints suivants ; il n'est nécessaire ni aux tests actuels ni au démarrage de l'assemblage INT-03.

## Vérifications depuis la racine

Sur Linux/macOS :

```sh
./mvnw -v
./mvnw -B -ntp validate
./mvnw -B -ntp -pl contracts -am test
```

Ces commandes ont réussi sous Linux avec Java 21. La sélection `-pl contracts` vérifie les 59 cas de données/JSON, mais ne compile pas Experience ; le contrôle global reste donc nécessaire :

```sh
./mvnw -B -ntp clean verify
```

**Résultat observé dans INT-07 : succès du réacteur complet, 108 tests, aucun échec/erreur/ignoré.** Le build produit les trois bibliothèques et le JAR Boot d'assemblage. `clean` supprime les sorties `target/`, pas les sources. Une résolution de dépendances ou un simple `validate` ne remplace pas ce contrôle. Les avertissements Mockito d'auto-attachement d'agent sous Java 21 n'ont pas fait échouer les tests ; la compatibilité avec un futur JDK n'est pas validée ici.

Sous Windows/PowerShell, utiliser `./mvnw.cmd` à la place de `./mvnw` ; le script Windows est fourni mais son exécution n'a pas été testée dans cet environnement Linux. `.gitattributes` fixe LF pour le script Unix et CRLF pour le script Windows.

### Particularité réseau de cet environnement

[`.mvn/jvm.config`](.mvn/jvm.config) force IPv4 pour le processus Maven afin de contourner une panne IPv6 reproduite lors du dépannage. Ce réglage ne configure ni JDTLS ni la JVM d'une future application. À réévaluer avant usage sur un réseau exclusivement IPv6.

## Démarrage local

Après le build global, depuis la racine :

```sh
java -jar application/target/application-0.1.0-SNAPSHOT.jar --server.port=0 --server.address=127.0.0.1
```

C'est la commande réellement lancée par les contrôles bornés d'INT-03, d'INT-06 et d'INT-07 : `0` demande un port disponible, affiché par Tomcat. Le superviseur de vérification a attendu le démarrage, vérifié POST/GET en HTTP local et arrêté le processus ; lancement manuel à arrêter avec `Ctrl+C`. Sans ces options, la configuration par défaut écoute sur le port 8080. Aucun service sur 8081 ni propriété `CAPABILITY_BASE_URL` n'est requis.

**À ce stade, `POST /api/v1/families` valide retourne `202 Accepted`**, avec `requestId`, `status: PENDING`, `statusUrl` et `Location`. Les données fictives de `contracts/src/test/resources/contracts/create-family.json` servent à la démonstration. Capability valide aussi les appels Java directs et conserve un snapshot du payload avant d'acquitter. Un échec d'enregistrement ne produit pas d'acceptation ; les détails internes restent masqués.

Le lien `Location`/`statusUrl` est désormais **consultable par GET** : une demande connue retourne `200` et les trois métadonnées, sans payload personnel ni résultat métier. Un UUID inconnu reçoit `404 application/problem+json` avec `Request not found.` ; un identifiant manifestement mal formé reçoit `400` avec `Invalid request.`.

Le `202` signifie toujours seulement « demande conservée en mémoire », pas famille créée, traitement lancé ou conservation durable. Le redémarrage remet la map à vide, sans nettoyage ni borne de capacité ajoutés. Voir les tutoriels [INT-06 — acceptation](notes/sprint-02/INT-06-accepter-une-demande.md) et [INT-07 — consultation](notes/sprint-02/INT-07-consulter-la-demande.md).

## Architecture assemblée — un monolithe modulaire

| Module | Responsabilité | État |
|---|---|---|
| `contracts` | Types Java échangés entre modules ; ni métier, ni contrôleur, ni persistance | Cinq types et 59 cas de test dans INT-05 |
| `capability-api` | API Java publique, puis règles métier, statuts, stockage et orchestration ; le suffixe `-api` ne signifie pas un serveur autonome | Acceptation `PENDING`, validation et consultation du stockage mémoire internes ; traitement non réalisé |
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
- [Spécification HTTP initiale](docs/dat/http-contracts.md).
- [Index des notes](notes/README.md).

Chaque tâche de développement exige le feu vert explicite de l'utilisateur. Une tâche autorisée ne déclenche pas automatiquement la suivante. La CD reste strictement théorique.
