# ConfluxLab

Laboratoire pédagogique Java/Spring : création d'une famille fictive de trois personnes, suivi asynchrone et orchestration de six opérations partenaires simulées. Ce projet ne reproduit pas un SI AXA certifié et n'est pas destiné à la production.

## État actuel

- Le cadrage des frontières d'**INT-01** est documenté ; leur implémentation et leurs tests restent à réaliser dans INT-03/INT-04.
- **INT-02 et INT-05 sont terminées dans leur périmètre** : le Wrapper, les POM et les contrats permettent un `clean verify` global réussi.
- Cinq types partagés et **59 cas de test de contrats** sont présents : validation des données et sérialisation JSON. Aucun DTO factice, aucune exclusion de compilation ni aucun saut de test.
- Aucun parcours HTTP/métier, assemblage exécutable ou workflow CI n'est livré à ce stade. Le client HTTP interne de l'ébauche Experience doit encore être remplacé dans INT-03.

## Prérequis

- JDK **21**, avec `java` et `javac` accessibles ; `JAVA_HOME` peut désigner son installation.
- Pas besoin de Maven préinstallé : utiliser les scripts du dépôt. Le Wrapper 3.3.4 télécharge Maven **3.9.16**, avec empreinte SHA-256 épinglée, lors du premier lancement.
- Accès réseau à Maven Central au premier lancement et pour les dépendances non mises en cache.
- Docker sera nécessaire pour les tests Testcontainers des sprints suivants ; il n'est pas nécessaire pour les tests de contrats actuels.

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

**Résultat observé après INT-05 : succès du réacteur complet, 59 cas de test, aucun échec/erreur/ignoré.** Experience compile, mais aucun serveur ni parcours HTTP n'a été lancé. `clean` supprime les sorties `target/`, pas les sources. Une résolution de dépendances ou un simple `validate` ne remplace pas ce contrôle.

Sous Windows/PowerShell, utiliser `./mvnw.cmd` à la place de `./mvnw` ; le script Windows est fourni mais son exécution n'a pas été testée dans cet environnement Linux. `.gitattributes` fixe LF pour le script Unix et CRLF pour le script Windows.

### Particularité réseau de cet environnement

[`.mvn/jvm.config`](.mvn/jvm.config) force IPv4 pour le processus Maven afin de contourner une panne IPv6 reproduite lors du dépannage. Ce réglage ne configure ni JDTLS ni la JVM d'une future application. À réévaluer avant usage sur un réseau exclusivement IPv6.

## Architecture cible — un monolithe modulaire

| Module | Responsabilité | État |
|---|---|---|
| `contracts` | Types Java échangés entre modules ; ni métier, ni contrôleur, ni persistance | Cinq types et 59 cas de test dans INT-05 |
| `capability-api` | API Java publique, règles métier, statuts, stockage et orchestration ; le suffixe `-api` ne signifie pas un serveur autonome | Bibliothèque vide |
| `experience-api` | BFF mince : contrat HTTP, validation de forme et adaptation des réponses | Ébauche compilable ; HTTP interne à remplacer et routes non testées |
| `application` | Assemblage, seul démarrage Spring Boot et seul JAR Boot exécutable | À créer dans INT-03 |

Experience appellera l'API publique Java de Capability ; aucune dépendance inverse ni aucun accès à ses classes internes. Feign/Resilience4j sera réservé au partenaire externe. Voir le [DAT](docs/dat/ConfluxLab.md) pour le graphe cible et les limites.

## Travail et comptes rendus

- [Plan des sprints](docs/stages/README.md).
- [Sprint 01 — Socle](docs/stages/sprint-01-socle.md).
- [INT-01 — Frontières](notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md).
- [INT-02 — Build reproductible et diagnostic JDTLS](notes/sprint-01/INT-02-rendre-le-build-reproductible.md).
- [INT-05 — Contrats et tests](notes/sprint-02/INT-05-definir-les-contrats.md).
- [Spécification HTTP initiale](docs/dat/http-contracts.md).
- [Index des notes](notes/README.md).

Chaque tâche de développement exige le feu vert explicite de l'utilisateur. Une tâche autorisée ne déclenche pas automatiquement la suivante. La CD reste strictement théorique.
