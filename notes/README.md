# Notes de travail — ConfluxLab

Les fiches de sprint et les tâches à réaliser sont dans [docs/stages](../docs/stages/README.md). Le [DAT](../docs/dat/ConfluxLab.md) porte les décisions d'architecture. Ce dossier rassemble les cours/tutoriels issus du travail réalisé, sous un répertoire par sprint. Le registre [todos.md](todos.md) centralise leurs points à retenir et leurs actions restantes.

## Convention

- Identifiant de tâche : **`INT-XX`**, unique à l'échelle du projet et conservé après attribution. La séquence continue entre les sprints ; ne pas réutiliser un identifiant.
- Chemin : `notes/sprint-XX/INT-YY-<nom-de-la-tache-en-kebab-case>.md`.
- Titre : `# INT-YY — <Nom de la tâche>`.
- Une note est un **tutoriel pour junior**, pas seulement un journal de commandes : elle explique le vocabulaire, le problème, les décisions, les étapes et le code réellement présent.
- Les extraits importants indiquent le fichier source et leur caractère partiel ; chaque commande précise sa racine de lancement, ses options, ses effets et son résultat observé. Une commande documentée n'est pas exécutée automatiquement.
- Les preuves historiques, les vérifications de la session et les exercices non exécutés sont présentés séparément. L'état propre d'un ancien commit ne décrit pas le travail courant.
- Les points à retenir et prochains travaux sont enregistrés dans [todos.md](todos.md) ; la dernière section de chaque cours pointe vers les entrées concernées, sans deuxième liste de TODO.
- Une note partielle ou rétrospective ne prouve pas que la tâche est terminée. Elle ne déclenche pas de développement : **le feu vert reste requis pour chaque tâche**.
- Les notes sont liées depuis cet index et la fiche du sprint concerné. Les tâches restent décochées tant que leurs critères ne sont pas vérifiés.

## Plan d'un tutoriel

| Section | Ce que le lecteur doit comprendre |
|---|---|
| **Contexte** | Story, statut réel, périmètre et origine des preuves |
| **1. Objectif et vocabulaire** | Ce que le cours explique et le sens des mots nouveaux |
| **2. Comprendre le problème** | Ce qui ne fonctionnait pas et pourquoi |
| **3. Travail réalisé pas à pas** | Décisions, fichiers et extraits de code/config commentés ; cible future clairement distinguée |
| **4. Commandes expliquées** | Où les lancer, ce qu'elles font, leurs options, effets et résultats réels |
| **5. Vérifications et limites** | Ce qui est prouvé, historique ou non testé ; pas de succès inventé |
| **6. Suivi centralisé** | Liens vers les actions et repères de `todos.md` |

Un cours peut avoir des sous-sections et un sommaire. Il ne doit pas devenir une copie intégrale des sources : expliquer les extraits utiles est plus important que multiplier les blocs de code.

## Lire le registre central

- **`TD-Bxxx`** : action restante ou vérification à effectuer, avec dépendances et statut ; elle ne crée pas un nouveau feu vert.
- **`TD-Rxxx`** : notion à retenir, avec sa source et une question de vérification pédagogique ; sa présence ne signifie pas que l'apprenant la maîtrise déjà.
- Chaque entrée a un tableau descriptif : source, story liée, dépendances, fichiers, action/notion, validation et statut. Une action déjà liée à une story n'est pas recréée en doublon.
- Les IDs sont stables ; les ancres `#td-b001`, etc., restent utilisables lorsque le libellé descriptif évolue.

## Répertoires par sprint

| Dossier | Tâches planifiées | Sujet |
|---|---|---|
| [sprint-01](sprint-01/) | INT-01 à INT-04 ; INT-37 ajoutée sur demande | Socle modulaire et initialisation Git |
| [sprint-02](sprint-02/) | INT-05 à INT-08 | Contrat REST |
| [sprint-03](sprint-03/) | INT-09 à INT-12 | MongoDB et statuts |
| [sprint-04](sprint-04/) | INT-13 à INT-16 | Asynchronisme |
| [sprint-05](sprint-05/) | INT-17 à INT-20 | Orchestration |
| [sprint-06](sprint-06/) | INT-21 à INT-24 | Partenaire et résilience |
| [sprint-07](sprint-07/) | INT-25 à INT-28 | Validation et observabilité |
| [sprint-08](sprint-08/) | INT-29 à INT-32 | CI |
| [sprint-09](sprint-09/) | INT-33 à INT-36 | CD théorique et entretien |

Les anciens numéros `01.1` à `09.4` ont été remplacés dans les fiches par ces identifiants, dans l'ordre des tâches. Une nouvelle tâche recevra un identifiant disponible sans renuméroter les existantes.

## Comptes rendus existants

| Tâche | Note | État constaté |
|---|---|---|
| INT-01 | [Inspecter et fixer les frontières](sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md) | Cadrage terminé : frontières, rôle de contracts, graphe cible et prérequis vérifiés ; historique initial conservé |
| INT-02 | [Rendre le build reproductible](sprint-01/INT-02-rendre-le-build-reproductible.md) | Terminée pour le build : blocage levé dans INT-05, `clean verify` global réussi ; historique Maven/JDTLS conservé |
| INT-03 | [Assembler les modules](sprint-01/INT-03-assembler-les-modules.md) | Terminée : seul JAR Boot démarré, appel Java et indisponibilité explicite ; 64 tests globaux, sans acceptation métier |
| INT-04 | [Vérifier les frontières](sprint-01/INT-04-verifier-les-frontieres.md) | Terminée : 5 règles testées sur la production, 7 tests de détection, contexte réutilisé ; 76 tests globaux, métier inchangé |
| INT-05 | [Définir les contrats](sprint-02/INT-05-definir-les-contrats.md) | Terminée : cinq types, exemples JSON et 59 cas de test ; pas de service ni de test HTTP |
| INT-06 | [Accepter une demande](sprint-02/INT-06-accepter-une-demande.md) | Terminée : conservation mémoire UUID/`PENDING` avant `202`, copie du payload, refus sans acquittement ; 96 tests globaux, GET toujours `503` |
| INT-37 | [Initialiser le dépôt Git](sprint-01/INT-37-initialiser-le-depot-git.md) | Initialisation locale et premier push HTTPS réalisés |

Les preuves historiques restent datées : INT-01 conserve le cadrage initial ; le cours INT-06 explique désormais l'acceptation réalisée et renvoie à la version publiée de sa rétrospective pour l'ébauche antérieure. Les autres dossiers n'ont pas de compte rendu de réussite anticipé.

## Clôtures de sprint

- [Sprint 01 — Bilan technique et synthèse documentaire](../docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) : clôturé sur demande explicite le 2026-10-21 ; 76 tests globaux au moment de la clôture, restitution orale non évaluée ; le feu vert INT-06 a été donné ensuite, séparément.

## Documentation déjà produite

- Le DAT décrit le cas d'usage fictif, le graphe A–F, les statuts et les limites de l'événement local.
- Neuf fiches de sprint définissent incréments, exclusions, tests et critères de fin.
- Les règles du skill imposent le BFF mince, le métier dans Capability, les appels Java internes et le feu vert avant chaque tâche.
- Le plan a été déplacé dans `docs/stages/` ; ses liens locaux ont été vérifiés lors du déplacement.
- La demande initiale d'organisation des notes et des identifiants n'a modifié ni les sources Java, ni le POM existant, ni la configuration applicative. La réparation Maven autorisée ultérieurement est décrite dans la note INT-02.

Après INT-06, le `clean verify` global réussit : 96 tests (59 contrats, 16 Capability, 3 BFF, 18 assemblage/HTTP/architecture). Le JAR réel a répondu `202 PENDING` à deux POST avec UUID distincts, puis `503` au GET d'un UUID accepté et `400` au corps invalide ; le processus a été arrêté. La conservation du payload et l'absence d'acquittement en cas d'échec sont prouvées par les tests, sans réaliser le suivi ni le traitement. Les contrôles de frontières restent verts. La résolution des dépendances et la navigation Java/Spring côté JDTLS restent des preuves historiques d'INT-02, pas une nouvelle validation GUI. Aucun traitement de création de famille, suivi fonctionnel, CI verte ou mesure de performance n'est revendiqué.
