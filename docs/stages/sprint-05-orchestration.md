# Sprint 05 — Graphe métier et concurrence bornée

**Statut : INT-17 à INT-19 réalisées dans leur périmètre ; INT-20 non commencée.** INT-17 et INT-18 sont commitées localement dans `d6f0e86` et `516ea0e`, sans push. INT-19 vérifie la composition des échecs en Java, pas l'écriture d'un état final MongoDB ou le raccordement au listener. La clôture pédagogique du sprint 04 reste à confirmer ; le feu vert par tâche est conservé.

## Objectif

Exécuter les six opérations dans le bon ordre, en permettant aux opérations indépendantes d'avancer ensemble. Commencer avec des doublures de test, sans HTTP.
Ce sprint est un objectif pédagogique : chaque tâche attend son feu vert individuel ; le plan ne lance rien et les tests accompagnent chaque tâche.

## Repères

- Une **future** (`CompletableFuture`) représente le résultat à venir d'une opération, réussi ou en échec.
- Un **latch** permet à un test de bloquer puis de libérer une étape au moment choisi.
- Un **executor** répartit les tâches sur des threads, dont le nombre et la file d'attente doivent être limités.

## Graphe à respecter

- A crée le principal ; après A, B ouvre le contrat et C/D créent les deux ayants droit.
- E rattache le premier ayant droit après B **et** C, sans attendre D.
- F rattache le second après B **et** D, sans attendre C.
- La réussite globale exige la réussite de toutes les opérations nécessaires.

## Tâches — un feu vert par tâche

### INT-17 — Écrire les contrats des opérations
**Statut : terminée sur feu vert « go 17 ».** [Cours et limites INT-17](../../notes/sprint-05/INT-17-ecrire-les-contrats-des-operations.md).

En tant que développeur, je veux connaître les données attendues et renvoyées par chaque opération, afin de relier les six étapes sans confondre leurs identifiants.

**À faire :**
- Définir les entrées, les identifiants retournés et les données nécessaires à chaque opération.
- Préparer une doublure réutilisable dont les tests pilotent les résultats avec des futures.

**Terminé quand :**
- [x] `FamilyOperations` décrit les six opérations, leurs données et les identifiants typés nécessaires aux dépendances, sans modifier les contrats HTTP publics.
- [x] `ControlledFamilyOperations`, uniquement sous `src/test`, prépare six futures pilotables en succès ou en échec. Elle est compilée et sera exercée dans INT-18/19, sans suite dédiée au seul montage.

**Preuves :** `./mvnw -B -ntp -pl capability-api -am test-compile` réussi : production et sources de test compilées, aucun test exécuté. Deux fichiers Java ajoutés, aucune dépendance ou configuration modifiée. Aucun orchestrateur, branchement au listener, HTTP, executor ou état final ajouté.

### INT-18 — Composer le succès
**Statut : terminée sur feu vert « commit 17 & go 18 ».** [Cours, extraits et preuves INT-18](../../notes/sprint-05/INT-18-composer-le-succes.md).

En tant que développeur, je veux lancer chaque opération dès que ses prérequis sont prêts, afin de ne pas attendre une branche indépendante.

**À faire :**
- Composer le parcours réussi avec `CompletableFuture`, sans barrière globale avant E/F.
- Réutiliser un scénario nominal piloté pour vérifier les dépendances et l'indépendance des branches, sans test distinct pour chaque flèche du graphe.

**Terminé quand :**
- [x] `FamilyOrchestrator` compose A puis B/C/D ; E attend B/C et F attend B/D. `thenCompose` aplatit les futures et le seul `allOf` attend les rattachements à la fin.
- [x] Un scénario paramétré couvre les deux ordres C/D : données et appels vérifiés, B/C/D en cours ensemble, E ou F avançant avec l'autre ayant droit encore en attente, résultat global incomplet jusqu'à la fin réelle des deux rattachements.

**Preuves :** `test` ciblé de `FamilyOrchestratorTest`, 2 cas Surefire réussis, aucun échec/erreur/ignoré. Fixture INT-17 réutilisée sans modification, attentes bornées, ni `sleep`, Spring, MongoDB ou HTTP. Aucun executor, branchement au listener ou état final ajouté ; on vérifie des futures en cours, pas un parallélisme de threads mesuré.

### INT-19 — Composer les échecs
**Statut : terminée pour la composition Java sur feu vert « commit 18 & passe à 19 ».** [Cours, preuve et limites INT-19](../../notes/sprint-05/INT-19-composer-les-echecs.md).

En tant que consommateur de l'API, je veux un suivi d'échec fiable, afin de ne pas voir une demande annoncée réussie alors qu'une opération nécessaire a échoué.

**À faire :**
- Empêcher le démarrage des opérations dont une dépendance a échoué.
- Garder une règle simple : attendre la fin des branches déjà lancées avant d'écrire l'état final, sans annulation forcée.

**Terminé quand :**
- [x] Un échec simulé de B empêche les appels E/F et se retrouve comme cause de l'échec global, sans faux résultat réussi.
- [x] Le même scénario laisse C/D en cours, termine C puis D et vérifie que la future globale ne finit qu'après les branches lancées. C'est le point à utiliser avant une finalisation ; aucune écriture d'état terminal n'est ajoutée ou revendiquée ici.

**Preuves :** 3 cas Surefire ciblés réussis, dont les 2 cas nominaux réutilisés et un seul cas d'échec ajouté ; aucun échec/erreur/ignoré. La jonction finale attend explicitement B/C/D et les chemins E/F, sans déplacer les conditions de démarrage. Pas d'annulation forcée, fallback, HTTP, Spring ou MongoDB.

**Limite de portée :** le raccordement au suivi, l'écriture de `FAILED` et la vérification de l'absence d'écritures MongoDB tardives dans un parcours raccordé ne sont pas réalisés. Le graphe n'écrit actuellement aucun statut ; ne pas présenter cette preuve Java comme une preuve de suivi HTTP de bout en bout.

### INT-20 — Borner les ressources
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux limiter les ressources utilisées par les demandes concurrentes, afin d'éviter qu'elles bloquent tous les threads disponibles.

**À faire :**
- Choisir explicitement l'executor du graphe et ses limites, en réutilisant l'existant si adapté ; pas de `commonPool` implicite ni d'attente de sous-tâches dans leur propre pool.
- Réutiliser les preuves de saturation d'INT-15 ; compléter seulement si le nouvel executor ou le nouveau chemin de soumission introduit un risque non couvert.

**Terminé quand :**
- [ ] Les ressources sont bornées, le rejet est explicite et le listener ne bloque pas son propre pool en attendant le graphe ; un contrôle ciblé complète seulement les preuves manquantes.
- [ ] UUID, états et résultats restent propres à chaque demande. Vérifier l'isolation sur deux demandes si un état est partagé, sans campagne de charge.

## Tests au minimum utile

- Partir d'un parcours de succès piloté et d'un échec représentatif ; réutiliser et enrichir ces scénarios entre les tâches, sans nouvelle suite à chaque incrément.
- Tester le graphe en Java avec des futures contrôlées : pas de contexte Spring, MongoDB ou HTTP ajouté pour vérifier la composition seule.
- Garder les attentes bornées et libérer les blocages dans les `finally` ; pas de `Thread.sleep` comme preuve.
- Ajouter un contrôle de ressources ou d'isolation uniquement pour un risque concret du code retenu. Pas de test par DTO, par flèche ou par panne possible, ni d'objectif de nombre de tests.

## Limites et hors périmètre

Le monolithe modulaire reste dans une seule JVM : Experience/BFF mince appelle Capability métier par son interface Java publique ; aucune orchestration dans le BFF.
Pas encore d'HTTP partenaire ni de résilience HTTP. Plus tard, Feign restera une entrée/sortie (I/O) bloquante, même dans une future.
Annuler une future ne garantit ni l'arrêt d'une I/O ni l'annulation d'une action déjà réalisée ; aucun rollback distribué n'est promis.

## Démo et clôture

- [ ] Montrer un succès et un échec pilotés par les tests, puis expliquer la différence entre traitement asynchrone et opérations concurrentes.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les tests réellement observés sur les dépendances, les échecs et les ressources.
