# Sprint 04 — Spring Events locaux et traitement asynchrone

**Statut : planifié.** Prérequis : sprint 03 clos et accord pour ouvrir ce sprint.

## Objectif

Séparer l'acceptation HTTP du travail métier, sans confondre événement local et file de messages durable.

## Incréments, un par échange

- [ ] **INT-13 — Dessiner les fenêtres de panne.** Documenter insertion MongoDB, publication, soumission à l'executor, prise en charge et réponse HTTP. Définir la réponse si l'executor rejette la tâche avant le `202`, et le devenir du document déjà enregistré.
- [ ] **INT-14 — Publier et consommer.** Publier un événement contenant le `requestId` après écriture confirmée. Un listener `@Async` recharge la demande et prend atomiquement en charge `PENDING → PROCESSING`.
- [ ] **INT-15 — Borner l'exécution.** Définir executor, capacité de file, politique de rejet et arrêt propre. Attraper et enregistrer les échecs du traitement ; ne pas abandonner silencieusement les exceptions d'une méthode async `void`.
- [ ] **INT-16 — Tester avec un traitement contrôlé.** Utiliser une doublure dans les tests pour bloquer/libérer le travail et provoquer un échec. Elle prépare la frontière d'orchestration, sans déclarer un parcours métier complet en production.

## Critères de fin et tests

- [ ] Le test prouve que l'acceptation ne dépend pas de la fin du travail, sans hypothèse « le listener démarre après le 202 ».
- [ ] L'événement est publié uniquement après succès du stockage.
- [ ] Un événement reçu deux fois ne déclenche pas deux prises en charge concurrentes de la même demande.
- [ ] Les tests couvrent rejet de soumission et exception de traitement avec états cohérents et diagnostic exploitable.
- [ ] Les attentes des tests sont bornées ; pas de `Thread.sleep` utilisé comme preuve de synchronisation.
- [ ] La fenêtre crash après insertion / avant consommation est expliquée et reproductible par une simulation contrôlée.

## Limites et hors périmètre

Un `PENDING` ou `PROCESSING` peut rester abandonné après crash. Le `202` ne garantit pas l'achèvement. Pas de reprise automatique ni de broker ajouté pour cacher cette limite. Le traitement contrôlé des tests ne remplace pas les six opérations métier, livrées ensuite.

## Explication à maîtriser

Que découple réellement `@Async` ? Pourquoi un événement local peut-il être perdu alors que la demande existe dans MongoDB ?

## Preuves de clôture

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), avec scénario de saturation, états observés et limites de durabilité.
