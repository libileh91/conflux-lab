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

**Profondeur demandée par l'utilisateur :** pour chaque nouvelle technologie, expliquer son rôle, son fonctionnement concret dans le projet, les alternatives écartées et les pièges. Inclure les schémas d'architecture avant/après et les séquences utiles de succès/erreur, la carte des fichiers, le câblage Spring et les extraits de code/config importants expliqués. Les paramètres de commandes doivent être compréhensibles sans connaissance implicite de l'outil. Les illustrations non exécutées restent étiquetées comme telles.

Un cours long comporte un sommaire et un parcours de lecture. Il ne doit pas devenir une copie intégrale des sources : expliquer les extraits utiles est plus important que multiplier les blocs de code. **Documentation approfondie ne signifie pas davantage de tests** : conserver la sélection minimale adaptée au risque et ne pas relancer des commandes pour le seul besoin d'illustrer le texte.

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
| INT-06 | [Accepter une demande](sprint-02/INT-06-accepter-une-demande.md) | Terminée : conservation mémoire UUID/`PENDING` avant `202`, copie du payload, refus sans acquittement ; 96 tests globaux à cette étape, GET encore `503` avant INT-07 |
| INT-07 | [Consulter la demande](sprint-02/INT-07-consulter-la-demande.md) | Terminée : POST/GET réel, lecture seule des métadonnées, erreurs `404`/`400`, perte après redémarrage ; 108 tests globaux |
| INT-08 | [Tester les refus](sprint-02/INT-08-tester-les-refus.md) | Terminée : refus HTTP/Java sans enregistrement, erreurs génériques, correction d'`instance`, isolation ; 181 tests globaux |
| INT-09 | [Définir les données et transitions](sprint-03/INT-09-definir-les-donnees-et-transitions.md) | Terminée : modèle interne, dates et transitions locales ; 2 tests ajoutés, 3 tests ciblés réussis à cette étape ; MongoDB branché ensuite dans INT-10 |
| INT-10 | [Persister avant d'accepter](sprint-03/INT-10-persister-avant-d-accepter.md) | Terminée : insertion MongoDB acquittée avant `202`, UUID unique, relecture et erreur sans faux reçu ; 2 nouveaux tests + 1 test existant réussis |
| INT-11 | [Sécuriser les mises à jour](sprint-03/INT-11-securiser-les-mises-a-jour.md) | Terminée : transitions conditionnelles atomiques, un seul gagnant, dates et états finaux protégés ; 2 nouveaux tests + 2 tests du modèle réussis |
| INT-12 | [Vérifier la persistance](sprint-03/INT-12-verifier-la-persistance.md) | Terminée : Failsafe branché à `verify`, JAR redémarré sur la même base, métadonnées conservées ; 1 test ajouté, 6 cas sélectionnés réussis |
| INT-13 | [Dessiner les fenêtres de panne](sprint-04/INT-13-dessiner-les-fenetres-de-panne.md) | Terminée, simplifiée : un schéma, trois situations de panne et les nuances indispensables ; documentation uniquement, aucune panne exécutée |
| INT-14 | [Publier et consommer](sprint-04/INT-14-publier-et-consommer.md) | Terminée : événement après sauvegarde, listener async, prise en charge atomique ; 1 scénario ajouté, 9 cas ciblés réussis ; pas de métier ni d'état final |
| INT-37 | [Initialiser le dépôt Git](sprint-01/INT-37-initialiser-le-depot-git.md) | Initialisation locale et premier push HTTPS réalisés |

Les preuves historiques restent datées : INT-01 conserve le cadrage initial ; le cours INT-06 explique désormais l'acceptation réalisée et renvoie à la version publiée de sa rétrospective pour l'ébauche antérieure. Les autres dossiers n'ont pas de compte rendu de réussite anticipé.

**INT-13** ouvre le sprint 04 par une explication courte des limites du flux Spring Event/`@Async`, sans clôture pédagogique automatique du sprint 03. La [note simplifiée](sprint-04/INT-13-dessiner-les-fenetres-de-panne.md) n'impose pas une matrice exhaustive de tests. Après sa pause, INT-14 est reprise sur demande explicite et validée avec 9 cas ciblés ; le cours distingue la prise en charge livrée du métier et du bornage encore à venir. [Repère TD-R020](todos.md#td-r020).

## Compléments théoriques

Ces notes approfondissent une story existante sans ajouter de tâche, modifier l'architecture implémentée ou autoriser une expérimentation.

- **INT-10 — [MongoDB standalone ou replica set](sprint-03/INT-10-complement-mongodb-replica-set.md)** : deux ou trois membres, quorum, acquittement, URI, délais et limites des reprises ; théorie uniquement. [Repère TD-R017](todos.md#td-r017).

## Clôtures de sprint

- [Sprint 01 — Bilan technique et synthèse documentaire](../docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) : clôturé sur demande explicite le 2026-10-21 ; 76 tests globaux au moment de la clôture, restitution orale non évaluée ; le feu vert INT-06 a été donné ensuite, séparément.

## Documentation déjà produite

- Le DAT décrit le cas d'usage fictif, le graphe A–F, les statuts et les limites de l'événement local.
- Neuf fiches de sprint définissent incréments, exclusions, tests et critères de fin.
- Les règles du skill imposent le BFF mince, le métier dans Capability, les appels Java internes et le feu vert avant chaque tâche.
- Le plan a été déplacé dans `docs/stages/` ; ses liens locaux ont été vérifiés lors du déplacement.
- La demande initiale d'organisation des notes et des identifiants n'a modifié ni les sources Java, ni le POM existant, ni la configuration applicative. La réparation Maven autorisée ultérieurement est décrite dans la note INT-02.

Lors d'INT-08, le dernier `clean verify` non filtré observé réussit : 181 tests (59 contrats, 43 Capability, 4 BFF, 75 assemblage/HTTP/architecture). Les refus sont vérifiés aux deux frontières et un écho de chemin dans `instance` a été reproduit puis corrigé ; le [cours INT-08](sprint-02/INT-08-tester-les-refus.md) distingue les cas essentiels de la matrice plus large de non-régression. Les preuves de redémarrage suivantes restent celles d'INT-07 : Le JAR réel a répondu `202` puis `200 PENDING` pour deux demandes distinctes, avec des métadonnées stables ; UUID inconnu `404`, format invalide `400`, puis ancien UUID `404` après redémarrage. Les deux processus ont été arrêtés. L'acceptation et le suivi sont réalisés en mémoire, sans traitement de création de famille. La démonstration INT-08 confirme aussi les refus sans marqueur et les vues stables de deux demandes ; les contrôles de frontières restent verts. La résolution des dépendances et la navigation Java/Spring côté JDTLS restent des preuves historiques d'INT-02, pas une nouvelle validation GUI. INT-09 a ensuite défini les dates et transitions du modèle interne avec 3 tests ciblés réussis, sans relancer ce build global. À cette étape INT-09, aucun stockage durable n'était revendiqué. **Depuis INT-10**, le service utilise MongoDB et 3 tests ciblés réussissent avec une base temporaire réelle ; la panne d'insertion est simulée et les conteneurs sont supprimés. À l'étape INT-10, la suite complète et le JAR n'avaient pas été relancés ; aucun redémarrage n'avait été démontré. INT-11 a ensuite ajouté les transitions conditionnelles atomiques et leur validation par 4 tests ciblés, sans déclencher de traitement réel. **INT-12** a depuis exécuté `clean verify` avec 1 cas Surefire sélectionné et 5 cas Failsafe réussis : le JAR réel est redémarré sur la même base et retrouve les mêmes métadonnées. Ce n'est pas un nouveau passage de toutes les suites. Aucun traitement de création de famille, CI verte ou mesure de performance n'est revendiqué.
