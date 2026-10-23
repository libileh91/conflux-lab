# Sprint 07 — Démontrer et mesurer le parcours

**Statut : INT-25 à INT-28 réalisés le 2026-10-23 sur feu vert « commit & go next tasks ».**
Prérequis utilisés : parcours et pannes ciblées du sprint 06 réalisés, JDK 21 et Docker disponibles. La compréhension orale n'est pas évaluée par ces preuves.
Un seul feu vert couvre cet incrément ; les anciens IDs sont des repères de traçabilité, pas quatre tâches à autoriser.

## Objectif / story

En tant que développeur, je veux démontrer le parcours, diagnostiquer un échec et mesurer le traitement, afin d'expliquer ce que le laboratoire fait réellement.

## Périmètre et actions

- Réutiliser les cas et preuves de succès, d'échec et de concurrence des sprints précédents ; compléter seulement une preuve manquante du parcours raccordé, sans réécrire les scénarios ni lancer une seconde campagne exhaustive.
- Préparer une courte démonstration locale : requête fictive, `202`, suivi jusqu'à l'état final, puis diagnostic d'un échec ; conserver les commandes de lancement et d'arrêt utiles.
- Compléter seulement les logs utiles avec UUID de demande, opération et statut, y compris en arrière-plan, sans payload, donnée privée ni secret.
- Ne pas imposer de MDC : s'il est déjà nécessaire à la corrélation, assurer sa transmission et son nettoyage entre threads.
- Relever les trois mesures ci-dessous par des horodatages et un tableau simple ; préciser l'environnement, l'échantillon et les bornes de mesure.
- Comparer séquentiel et concurrent dans un montage expérimental local, avec les mêmes opérations, données fictives, délais partenaires contrôlés et ressources ; ne pas créer un second parcours de production.

## Mesures à distinguer

| Mesure | Définition |
|---|---|
| Latence HTTP `202` | Réception du POST → réponse `202` |
| Durée métier | Acceptation de la demande → état final, sans supposer que le traitement commence après le retour HTTP |
| Taux de réussite | Demandes réussies / demandes terminées dans un échantillon et une fenêtre définis ; dénombrer séparément celles encore en cours |

## Critères observables et preuves minimum

- [x] La démonstration montre une acceptation puis une fin de traitement, et un échec après acceptation reste visible dans le suivi.
- [x] Quelques lignes de logs permettent de retrouver UUID, opérations et statuts d'un échec ; un contrôle de deux demandes raccordées démontre l'absence de mélange d'états et de résultats. Réutiliser un scénario existant s'il porte déjà cette preuve.
- [x] Le relevé distingue les trois mesures, avec effectifs et conditions ; un `202` ne vaut ni traitement terminé ni réussite métier.
- [x] Le tableau séquentiel/concurrent indique le protocole reproductible, quelques relevés et leur variation, sans seuil chronométrique fragile comme critère de test.
- [x] Les rapports existants cités gardent leur date et leur périmètre ; les nouvelles observations ne sont pas présentées comme une nouvelle campagne de tests.

**Preuves du 2026-10-23 :** `clean verify` ciblé réussi, **7 Surefire + 6 Failsafe**, aucun échec/erreur/ignoré ; seulement deux nouveaux scénarios intégrés. Serveur de test sur vrai port HTTP : deux demandes soumises sans attendre la première, UUID/payloads/échanges distincts, `SUCCEEDED` et `FAILED`, journal de finalisation confirmé. Trois passages par mode après warmup, mêmes données, deux workers et délai WireMock 150 ms : médianes **944,205 ms séquentiel / 634,928 ms concurrent**. Aucune assertion chronométrique ni suite globale.

**Cours et protocole : [INT-25 à INT-28 — Démontrer et mesurer le parcours](../../notes/sprint-07/INT-25-demontrer-et-mesurer-le-parcours.md).** Commande de lancement/arrêt automatique, relevés, variation et bornes réelles : latence vue du client, durée issue des dates du document et taux sur deux demandes, dont un échec injecté. Pas d'instrumentation exacte des acquittements serveur ni de preuve d'un chevauchement serveur synchronisé.

## Limites nécessaires

Monolithe modulaire inchangé : BFF mince → Capability en Java dans la même JVM ; Feign/Resilience4j vers WireMock uniquement.
L'événement local n'est pas durable : un crash peut abandonner une demande ; aucune reprise automatique n'est promise.
Aucune plateforme de métriques ni extension Actuator imposée sans besoin démontré. Les chiffres sont des **mesures ConfluxLab**, pas des résultats AXA ni une garantie de performance générale.

## Traçabilité des IDs conservés

| ID | Ancien sujet intégré |
|---|---|
| INT-25 | Corréler le parcours |
| INT-26 | Consolider les scénarios |
| INT-27 | Séparer les métriques |
| INT-28 | Comparer et documenter |

**Livrable réalisé :** la note commune ci-dessus porte les quatre IDs, les relevés et les preuves réutilisées situées. [INT-29 à INT-32](sprint-08-ci.md) ont ensuite été engagés localement : `verify` complet réussi et workflow créé, statut partiel sans run GitHub observé. Les opérations distantes restent à autoriser ; les preuves ci-dessus restent celles d'INT-25 à INT-28.
