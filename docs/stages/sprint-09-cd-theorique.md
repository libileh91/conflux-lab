# Sprint 09 — Expliquer le projet et la livraison

**Statut : non commencé — théorie / atelier uniquement, feu vert requis pour l'incrément complet INT-33 à INT-36.**
Prérequis : parcours et résultats de CI disponibles pour discussion ; une CI bloquée n'empêche pas l'atelier, mais doit être présentée comme telle.
Une seule séance, un seul feu vert et une seule note pour l'ensemble ; aucun des quatre IDs ne constitue une séance à autoriser séparément.

## Objectif / story

En tant que développeur préparant un entretien, je veux présenter le projet puis raisonner sur sa livraison, afin de défendre ses choix et ses limites sans revendiquer une expérience non confirmée.

## Déroulé de la séance

- Présenter d'abord le parcours et dessiner le graphe A–F : A précède B/C/D, E attend B et C, F attend B et D ; distinguer acceptation HTTP, traitement asynchrone et concurrence.
- Situer BFF mince et Capability dans la même JVM, avec appels Java internes, MongoDB et événement local ; Feign/Resilience4j reste réservé au partenaire fictif WireMock.
- Expliquer les limites utiles : événement non durable après crash, UUID de corrélation sans idempotence, échecs partiels sans transaction globale ni garantie exactly-once ; citer une preuve existante sans relancer de test.
- Dessiner ensuite une livraison conceptuelle : construire une fois, promouvoir le **même artefact immuable** entre environnements, séparer configuration/secrets et prévoir contrôles et approbation avant mise en service.
- Distinguer CI, livraison continue (version prête à livrer) et déploiement continu (mise en service automatique après validations), sans installer ni exécuter de pipeline.
- Sur un incident fictif après mise en service, expliquer les signaux de détection, la décision d'arrêter ou poursuivre et le rollback ; revenir à une version précédente n'annule ni les données modifiées ni une action partenaire.
- Avancer **une question à la fois** : attendre la réponse, corriger puis proposer une formulation orale de 30–60 secondes avant la suivante, dans cette même séance.
- Séparer **[AXA rapporté]**, **[Simulation ConfluxLab]**, **[Proposition]** et **[Non confirmé]** ; ne pas attribuer à AXA les mesures du labo, la stack pédagogique ou la conception intégrale à l'utilisateur.

## Critères observables et preuves minimum

- [ ] La présentation restitue les responsabilités et dépendances A–F ; elle explique pourquoi `202` ne signifie pas travail terminé et pourquoi asynchrone ne signifie pas automatiquement plus rapide.
- [ ] Un dessin du parcours et un schéma de livraison conceptuelle permettent de suivre le même artefact, la configuration externe, les contrôles et l'approbation.
- [ ] Une réponse discutée sur l'incident explique quand envisager un rollback et ce qu'il ne répare pas, sans promettre de rollback distribué.
- [ ] Les réponses distinguent faits AXA confirmés, simulation, propositions et inconnues ; tests prévus, preuves réellement disponibles et éventuelle CI bloquée ne sont pas confondus.
- [ ] La séance traite notamment : « Pourquoi une CI verte ne prouve-t-elle ni un déploiement ni le bon fonctionnement en production ? » ; les réponses réellement données et les points à retravailler sont consignés brièvement.

## Limites nécessaires

Aucun test, build, workflow CD, secret, publication d'image, SSH, infrastructure ou déploiement opérationnel : les contrôles de livraison restent discutés, jamais exécutés.
La présence de cette fiche ou d'un schéma ne prouve pas une connaissance acquise ; les critères ne se cochent qu'après les échanges réellement observés.
Ne pas inventer de chiffres, versions, réglages ou responsabilités historiques AXA ; une mesure ConfluxLab reste une mesure de simulation.
Pas de catalogue de stratégies de déploiement ni de nouvelle plateforme : un scénario de livraison et un incident suffisent à l'atelier.

## Traçabilité des IDs conservés

| ID | Ancien sujet intégré |
|---|---|
| INT-33 | Dessiner la livraison conceptuelle |
| INT-34 | Analyser un incident |
| INT-35 | Restituer le parcours |
| INT-36 | Simuler l'entretien |

**Clôture future :** une seule note synthétique pour INT-33 à INT-36, avec les dessins, les réponses réellement discutées et les limites de compréhension observées ; pas quatre comptes rendus ni feux verts.
Cette réécriture ne lance pas la séance et ne crée aucune note de réussite ni validation de connaissances.
