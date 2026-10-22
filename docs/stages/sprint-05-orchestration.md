# Sprint 05 — Graphe métier et concurrence bornée

**Statut : planifié — périmètre et tests simplifiés sur demande utilisateur.** Prérequis : sprint 04 clos et accord pour ouvrir ce sprint. Cette révision ne démarre aucune tâche INT-17 à INT-20.

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
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux connaître les données attendues et renvoyées par chaque opération, afin de relier les six étapes sans confondre leurs identifiants.

**À faire :**
- Définir les entrées, les identifiants retournés et les données nécessaires à chaque opération.
- Préparer une doublure réutilisable dont les tests pilotent les résultats avec des futures.

**Terminé quand :**
- [ ] Les contrats A–F décrivent les données nécessaires à chaque dépendance du graphe.
- [ ] La doublure permet de terminer ou faire échouer une opération à la demande ; elle sera exercée dans les scénarios INT-18/19, sans suite de test dédiée au seul montage.

### INT-18 — Composer le succès
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux lancer chaque opération dès que ses prérequis sont prêts, afin de ne pas attendre une branche indépendante.

**À faire :**
- Composer le parcours réussi avec `CompletableFuture`, sans barrière globale avant E/F.
- Réutiliser un scénario nominal piloté pour vérifier les dépendances et l'indépendance des branches, sans test distinct pour chaque flèche du graphe.

**Terminé quand :**
- [ ] A précède B/C/D, qui peuvent être en cours ensemble ; E attend B/C et F attend B/D, sans barrière globale.
- [ ] Le scénario contrôlé vérifie l'ordre et l'avancement d'une branche pendant que l'autre reste bloquée. Varier l'ordre de fin de C/D seulement si nécessaire pour vérifier les deux chemins, sans dupliquer toute la préparation.

### INT-19 — Composer les échecs
**Statut : à faire — feu vert requis.**

En tant que consommateur de l'API, je veux un suivi d'échec fiable, afin de ne pas voir une demande annoncée réussie alors qu'une opération nécessaire a échoué.

**À faire :**
- Empêcher le démarrage des opérations dont une dépendance a échoué.
- Garder une règle simple : attendre la fin des branches déjà lancées avant d'écrire l'état final, sans annulation forcée.

**Terminé quand :**
- [ ] Un échec représentatif, par exemple B, empêche ses descendants E/F de démarrer et interdit un résultat global réussi.
- [ ] Le même scénario garde une branche en cours, puis la termine pour vérifier la finalisation en échec ; pas de matrice d'erreur A–F ni de réécriture tardive d'un état terminal.

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
