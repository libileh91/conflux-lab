# ConfluxLab — Tâches et sprints

**[Simulation ConfluxLab]** Ce plan pédagogique ne décrit pas le déroulement historique d'un projet AXA. Le [DAT](../dat/ConfluxLab.md) porte l'architecture ; les [cours](../../notes/README.md) expliquent le code et les preuves ; le [registre](../../notes/todos.md) garde le suivi utile.

## Notre façon de travailler

**Un incrément correspond à un résultat cohérent, pas à une classe, une méthode ou une notion isolée.** Il n'y a pas davantage de règle imposant une seule tâche ou une seule note par sprint : séparer les sujets autonomes, réunir les étapes qui ne prennent sens qu'ensemble.

1. Présenter le résultat attendu, les IDs couverts, le périmètre, les fichiers et les vérifications utiles.
2. Obtenir un feu vert pour cet ensemble. Il couvre ses étapes internes, pas le prochain lot ni une publication distante non autorisée.
3. Réaliser le code et le raccordement prévus. Contrats, câblage, succès et échec nécessaire ne deviennent pas automatiquement quatre tâches.
4. Vérifier les comportements utiles en réutilisant l'existant, sans suite par couche ni matrice exhaustive par défaut. Les IT restent nécessaires lorsqu'ils portent la preuve du nouveau raccordement ; la CI prévoit une vérification globale.
5. Expliquer le flux avec les principaux fragments de code, les schémas et tableaux utiles, puis fournir un bilan bref. Le détail des tests dans les notes est facultatif ; leurs résultats ne sont jamais inventés. S'arrêter avant le prochain incrément.

Les IDs `INT-XX` restent uniques et stables. Leur regroupement documentaire ne change ni les autorisations passées ni les preuves. Les fiches indiquent **statut, story, périmètre et critères observables** ; une réécriture ne termine aucune fonctionnalité et ne valide pas la compréhension orale.

## Roadmap

| Sprint | Résultats cohérents et IDs conservés | État réel |
|---|---|---|
| [01 — Socle](sprint-01-socle.md) | Architecture/assemblage/frontières : INT-01 + INT-03 + INT-04 ; build/versionnement : INT-02 + INT-37 | Deux ensembles réalisés ; clôture technique/documentaire du 2026-10-21, oral non évalué |
| [02 — REST](sprint-02-rest.md) | Accepter et consulter une demande : INT-05 à INT-08 | Réalisé au stade mémoire ; passage à MongoDB demandé ensuite, oral non évalué |
| [03 — MongoDB](sprint-03-mongodb.md) | Modèle/transitions : INT-09 + INT-11 ; insertion/persistance : INT-10 + INT-12 | Deux ensembles réalisés ; complément replica set théorique, clôture pédagogique non confirmée |
| [04 — Asynchronisme](sprint-04-async.md) | Prise en charge async : INT-13 + INT-14 + INT-16 ; ressources/erreurs : INT-15 | Deux ensembles réalisés jusqu'à `PROCESSING` ; clôture pédagogique non confirmée |
| [05 — Orchestration](sprint-05-orchestration.md) | Graphe, échecs et soumission : INT-17 à INT-20 | Réalisé en Java, non raccordé au listener ; clôture pédagogique non confirmée |
| [06 — Parcours partenaire](sprint-06-partenaire.md) | Parcours complet : INT-21 + INT-22 ; pannes : INT-23 + INT-24 | Deux lots réalisés : parcours puis pannes ciblées ; absence de retry justifiée, 11 cas ciblés réussis pour INT-23/24 |
| [07 — Démonstration et mesures](sprint-07-validation.md) | Montrer, diagnostiquer et mesurer le parcours : INT-25 à INT-28 | Réalisé : deux demandes isolées, relevés locaux et comparaison test-only ; 13 cas ciblés réussis, oral non évalué |
| [08 — CI exploitable](sprint-08-ci.md) | Vérification et pipeline réellement observé : INT-29 à INT-32 | Un lot à autoriser ; opérations distantes sur accord distinct |
| [09 — Atelier projet et livraison](sprint-09-cd-theorique.md) | Restitution et livraison conceptuelle : INT-33 à INT-36 | Un atelier à autoriser, sans déploiement |

Les numéros de sprint sont des repères de classement, pas une durée ou un quota. Deux lots restent prévus après le sprint 07 : CI et atelier théorique ; ce découpage découle de leur contenu, pas d'un objectif de nombre.

**Prochain résultat : [INT-29 à INT-32 — CI exploitable](sprint-08-ci.md), à autoriser.** Les [mesures INT-25 à INT-28](../../notes/sprint-07/INT-25-demontrer-et-mesurer-le-parcours.md) sont locales et ne prouvent aucune CI verte. Push, PR et exécution distante exigent un accord distinct ; la CD reste théorique.

## Lire l'historique

Les tableaux des fiches et des cours regroupés conservent les IDs, statuts et preuves des étapes passées. Ne pas prendre les regroupements actuels pour l'ordre exact de réalisation : INT-05 a notamment été préparée pour débloquer le build d'INT-02 avant l'assemblage INT-03.

Le socle a d'abord exposé des opérations indisponibles, puis le REST a utilisé la mémoire, MongoDB l'a remplacée et l'async a ajouté la prise en charge `PROCESSING`. Le graphe A–F restait indépendant du listener à l'étape INT-20 ; INT-21 + INT-22 le raccordent ensuite jusqu'au statut final. Un ancien `503`, une perte mémoire ou un résultat de test daté ne décrit donc pas nécessairement le code actuel.

Le [journal de clôture du sprint 01](sprint-01-socle.md#journal-de-clôture--2026-10-21) est conservé. Aucun autre bilan oral n'est créé par la présente condensation.

## Architecture à conserver

Une application Spring Boot, une JVM et un JAR exécutable. Experience/BFF reçoit l'HTTP, valide sa forme et adapte les réponses ; Capability possède métier, invariants, stockage et traitement. Les modules se parlent en Java par leurs API publiques, sans cycle ni accès du BFF aux détails de stockage. Feign/Resilience4j est réservé au partenaire fictif externe.

`202` signifie acceptation, pas réussite métier ; un événement Spring est local et non durable ; l'UUID corrèle sans garantir l'idempotence. La CD reste strictement théorique.

## Notes de fin d'incrément

- Classer sous `notes/sprint-XX/`, nommer les IDs couverts et lier la note depuis la fiche et [l'index](../../notes/README.md). Plusieurs notes restent possibles pour des sujets réellement distincts, sans répéter le même flux.
- Expliquer le code **par fragments**, pas en bloc ; conserver/améliorer les schémas et tableaux utiles. Omettre les harnais de test, rappels Maven et détails d'exécution qui n'aident pas à comprendre.
- Conserver un bilan compact des preuves historiques ou nouvelles, sans les additionner ou les présenter comme des exécutions actuelles. Mettre à jour le suivi existant plutôt que créer une liste concurrente.
- Les notes remplacées sont fusionnées sur demande, avec migration des liens ; une note rétrospective ou partielle ne prouve pas une nouvelle réussite.

## Définition de fin commune

- [ ] Le résultat prévu est réellement atteint et ses preuves sont disponibles ; les blocages et parties non vérifiées sont signalés.
- [ ] Le flux et les choix importants sont expliqués, sans prétendre une maîtrise orale non évaluée.
- [ ] La documentation est liée, proportionnée et sans données réelles ni secrets.
- [ ] Le prochain lot reste soumis à son propre accord ; aucun déploiement n'est ajouté.

## Journal à compléter en clôture de chaque sprint

Renvoyer aux notes et preuves existantes : résultats, dates, limites et accord pour la suite. Ce journal ne nécessite pas un second compte rendu détaillé ni une cérémonie après chaque modification.
