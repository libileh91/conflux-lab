# Sprint 05 — Graphe métier et concurrence bornée

**Statut : planifié.** Prérequis : sprint 04 clos et accord pour ouvrir ce sprint.

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
- Préparer des doublures dont les tests pilotent la réussite ou l'échec.

**Terminé quand :**
- [ ] Les contrats A–F décrivent les données nécessaires à chaque dépendance du graphe.
- [ ] Les tests peuvent compléter les doublures à la demande, sans appel HTTP.

### INT-18 — Composer le succès
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux lancer chaque opération dès que ses prérequis sont prêts, afin de ne pas attendre une branche indépendante.

**À faire :**
- Composer le parcours réussi avec `CompletableFuture`, sans barrière globale avant E/F.
- Tester l'ordre et le chevauchement avec des futures/latches et des attentes bornées, pas avec des `sleep` comme preuve.

**Terminé quand :**
- [ ] Un test prouve que A précède B/C/D et que B/C/D peuvent être en cours simultanément.
- [ ] Les tests prouvent que E démarre avec D encore bloquée et que F démarre avec C encore bloquée.

### INT-19 — Composer les échecs
**Statut : à faire — feu vert requis.**

En tant que consommateur de l'API, je veux un suivi d'échec fiable, afin de ne pas voir une demande annoncée réussie alors qu'une opération nécessaire a échoué.

**À faire :**
- Empêcher le démarrage des opérations dont une dépendance a échoué.
- Définir comment traiter les branches déjà lancées avant d'enregistrer l'état final.

**Terminé quand :**
- [ ] Les tests vérifient qu'une dépendance en échec bloque ses descendants.
- [ ] Un test avec une branche encore en vol vérifie son traitement avant finalisation, sans faux succès ni écriture tardive écrasant un état terminal.

### INT-20 — Borner les ressources
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux limiter les ressources utilisées par les demandes concurrentes, afin d'éviter qu'elles bloquent tous les threads disponibles.

**À faire :**
- Choisir explicitement les executors, leurs limites et le comportement à saturation, sans `commonPool` implicite.
- Tester plusieurs demandes sans laisser le listener attendre des tâches dans son propre pool saturé.

**Terminé quand :**
- [ ] Des tests à attentes bornées vérifient le comportement à saturation et l'absence de blocage du listener par son propre pool.
- [ ] Les tests de demandes concurrentes ne mélangent ni UUID, ni états, ni résultats.

## Limites et hors périmètre

Le monolithe modulaire reste dans une seule JVM : Experience/BFF mince appelle Capability métier par son interface Java publique ; aucune orchestration dans le BFF.
Pas encore d'HTTP partenaire ni de résilience HTTP. Plus tard, Feign restera une entrée/sortie (I/O) bloquante, même dans une future.
Annuler une future ne garantit ni l'arrêt d'une I/O ni l'annulation d'une action déjà réalisée ; aucun rollback distribué n'est promis.

## Démo et clôture

- [ ] Montrer un succès et un échec pilotés par les tests, puis expliquer la différence entre traitement asynchrone et opérations concurrentes.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les tests réellement observés sur les dépendances, les échecs et les ressources.
