# Sprint 05 — Graphe métier et concurrence bornée

**Statut : planifié.** Prérequis : sprint 04 clos et accord pour ouvrir ce sprint.

## Objectif

Composer les six opérations fictives avec `CompletableFuture`, d'abord contre des doublures de test. Le graphe métier, et non la commodité du code, détermine les dépendances.

## Graphe à respecter

- A crée le principal.
- B ouvre le contrat après A ; C et D créent les deux ayants droit après A.
- E rattache le premier ayant droit après B **et** C.
- F rattache le second après B **et** D.
- La réussite globale exige la réussite de toutes les opérations nécessaires.

## Incréments, un par échange

- [ ] **INT-17 — Écrire les contrats des opérations.** Fixer entrées, identifiants retournés et dépendances de données. Prévoir des doublures pilotables, sans HTTP à cette étape.
- [ ] **INT-18 — Composer le succès.** Après A, permettre le chevauchement de B/C/D. E n'attend pas D et F n'attend pas C. Éviter une barrière globale inutile avant les rattachements.
- [ ] **INT-19 — Composer les échecs.** Une dépendance en échec empêche ses opérations descendantes. Définir le devenir des branches déjà lancées et le moment de finalisation ; éviter une écriture tardive écrasant un état terminal.
- [ ] **INT-20 — Borner les ressources.** Choisir explicitement les executors, éviter le pool commun implicite et la famine provoquée par un listener attendant des tâches dans son propre pool saturé. Vérifier le cas de plusieurs demandes concurrentes.

## Critères de fin et tests

- [ ] Les tests pilotent les complétions par futures/latches, avec échéances bornées, plutôt que par des durées de sommeil.
- [ ] A précède B/C/D, et B/C/D peuvent être en cours simultanément.
- [ ] E peut démarrer alors que D est encore bloquée ; F peut démarrer alors que C est bloquée.
- [ ] Les dépendances échouées ne déclenchent pas d'opération descendante.
- [ ] La finalisation attend ou traite explicitement les branches déjà en vol ; aucun faux succès global n'est enregistré.
- [ ] Les états et UUID de demandes concurrentes ne se mélangent pas.

## Limites et hors périmètre

Pas encore d'appels partenaires réels ni de résilience HTTP. Annuler une future ne garantit pas l'arrêt d'une I/O bloquante ou l'annulation d'une action déjà réalisée. Aucun rollback distribué n'est promis.

## Explication à maîtriser

Quelle différence entre asynchronisme et parallélisme ? Pourquoi mettre Feign dans une future ne rend-il pas son I/O non bloquante ?

## Preuves de clôture

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), avec les tests qui démontrent les dépendances et la gestion des branches en échec.
