# ConfluxLab

Laboratoire pédagogique Java/Spring : création d'une famille fictive de trois personnes, suivi asynchrone et orchestration de six opérations partenaires simulées. Ce projet ne reproduit pas un SI AXA certifié et n'est pas destiné à la production.

## État actuel

- **INT-01/02/03/05 sont terminées dans leur périmètre** : cadrage, build, contrats et assemblage unique. Les contrôles automatiques de frontières restent dans INT-04, sur feu vert distinct.
- `application` produit le seul JAR Boot exécutable ; Experience appelle l'interface Java publique de Capability, sans HTTP interne.
- Le `clean verify` global réussit : **64 tests** (59 de contrats, 2 de délégation Java, 3 d'assemblage/disponibilité), aucun échec/erreur/ignoré.
- Le JAR a réellement démarré sans MongoDB ni partenaire. **L'acceptation et le suivi métier ne sont pas implémentés** : un POST valide et un GET avec UUID valide retournent provisoirement `503`, pas un faux `202`/`200`. Aucun stockage, traitement métier ni workflow CI n'est livré.

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

**Résultat observé dans INT-03 : succès du réacteur complet, 64 tests, aucun échec/erreur/ignoré.** Le build produit les trois bibliothèques et le JAR Boot d'assemblage. `clean` supprime les sorties `target/`, pas les sources. Une résolution de dépendances ou un simple `validate` ne remplace pas ce contrôle. Les avertissements Mockito d'auto-attachement d'agent sous Java 21 n'ont pas fait échouer les tests ; la compatibilité avec un futur JDK n'est pas validée ici.

Sous Windows/PowerShell, utiliser `./mvnw.cmd` à la place de `./mvnw` ; le script Windows est fourni mais son exécution n'a pas été testée dans cet environnement Linux. `.gitattributes` fixe LF pour le script Unix et CRLF pour le script Windows.

### Particularité réseau de cet environnement

[`.mvn/jvm.config`](.mvn/jvm.config) force IPv4 pour le processus Maven afin de contourner une panne IPv6 reproduite lors du dépannage. Ce réglage ne configure ni JDTLS ni la JVM d'une future application. À réévaluer avant usage sur un réseau exclusivement IPv6.

## Démarrage local

Après le build global, depuis la racine :

```sh
java -jar application/target/application-0.1.0-SNAPSHOT.jar --server.port=0 --server.address=127.0.0.1
```

C'est la commande réellement lancée par le contrôle borné d'INT-03 : `0` demande un port disponible, affiché par Tomcat. Le superviseur de vérification a attendu le démarrage, vérifié POST/GET en HTTP local et arrêté le processus ; lancement manuel à arrêter avec `Ctrl+C`. Sans ces options, la configuration par défaut écoute sur le port 8080. Aucun service sur 8081 ni propriété `CAPABILITY_BASE_URL` n'est requis.

**À ce stade, une demande valide retourne `503 Service Unavailable`**, au format `application/problem+json`, avec le détail `Family request operations are not available yet.` Le suivi est lui aussi indisponible. Ce raccordement ne génère aucun UUID et ne conserve aucune demande ; le `503` n'est pas la preuve d'une panne de MongoDB ou du partenaire. Voir le [tutoriel INT-03](notes/sprint-01/INT-03-assembler-les-modules.md) pour les tests et la distinction entre doubles et comportement réel.

## Architecture assemblée — un monolithe modulaire

| Module | Responsabilité | État |
|---|---|---|
| `contracts` | Types Java échangés entre modules ; ni métier, ni contrôleur, ni persistance | Cinq types et 59 cas de test dans INT-05 |
| `capability-api` | API Java publique, puis règles métier, statuts, stockage et orchestration ; le suffixe `-api` ne signifie pas un serveur autonome | Interface `FamilyRequests` et implémentation interne provisoirement indisponible ; métier non réalisé |
| `experience-api` | BFF mince : contrat HTTP, validation de forme et adaptation des réponses | Appels Java vers Capability ; traduction de l'indisponibilité en `503` vérifiée |
| `application` | Assemblage, seul démarrage Spring Boot et seul JAR Boot exécutable | Créé et démarré dans INT-03 |

Experience appelle l'API publique Java de Capability ; aucune dépendance inverse ni aucun accès à ses classes internes. Les garde-fous automatisés restent à réaliser dans INT-04. Feign/Resilience4j sera réservé au partenaire externe. Voir le [DAT](docs/dat/ConfluxLab.md) pour le graphe et les limites.

## Travail et comptes rendus

- [Plan des sprints](docs/stages/README.md).
- [Sprint 01 — Socle](docs/stages/sprint-01-socle.md).
- [INT-01 — Frontières](notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md).
- [INT-02 — Build reproductible et diagnostic JDTLS](notes/sprint-01/INT-02-rendre-le-build-reproductible.md).
- [INT-03 — Assemblage unique et appel Java](notes/sprint-01/INT-03-assembler-les-modules.md).
- [INT-05 — Contrats et tests](notes/sprint-02/INT-05-definir-les-contrats.md).
- [Spécification HTTP initiale](docs/dat/http-contracts.md).
- [Index des notes](notes/README.md).

Chaque tâche de développement exige le feu vert explicite de l'utilisateur. Une tâche autorisée ne déclenche pas automatiquement la suivante. La CD reste strictement théorique.
