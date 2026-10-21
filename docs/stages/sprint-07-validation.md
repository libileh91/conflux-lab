# Sprint 07 — Parcours complet, observabilité et mesures

**Statut : planifié.** Prérequis : sprint 06 clos et accord pour ouvrir ce sprint ; Docker disponible avant tout test Testcontainers, sinon consigner le blocage.

## Objectif

Montrer le parcours complet, retrouver la cause d'un échec et mesurer ce qui change réellement. Ce sprint consolide les tests écrits dans les sprints précédents ; il ne démarre pas les tests.
Ce sprint est un objectif pédagogique : chaque tâche attend son feu vert individuel ; le plan ne lance rien.

## Repères

- La **latence HTTP** va de la réception du POST à la réponse `202`.
- La **durée métier** va de l'acceptation de la demande à son état final.
- Le **taux de réussite** est la part des demandes réussies parmi les demandes terminées dans un échantillon défini.

## Tâches — un feu vert par tâche

### INT-25 — Corréler le parcours
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux retrouver les étapes d'une demande dans les logs, afin de comprendre son parcours même quand plusieurs demandes avancent ensemble.

**À faire :**
- Associer `requestId`, opération et changement d'état aux logs, y compris sur les threads asynchrones.
- Si un contexte de logs lié au thread (MDC) est utilisé, le transmettre puis le nettoyer.

**Terminé quand :**
- [ ] Un test permet de retrouver les opérations et changements d'état d'une demande avec son `requestId`, y compris en arrière-plan.
- [ ] Un test de demandes concurrentes vérifie que leurs contextes de logs ne se mélangent pas.

### INT-26 — Consolider les scénarios
**Statut : à faire — feu vert requis.**

En tant que consommateur de l'API, je veux pouvoir suivre une demande jusqu'à son résultat, afin de connaître aussi les erreurs survenues après son acceptation.

**À faire :**
- Consolider le parcours HTTP public → Capability → MongoDB → événement local → orchestration → WireMock → suivi final.
- Décrire le lancement local, une requête fictive, le suivi, le diagnostic et l'arrêt.

**Terminé quand :**
- [ ] Les tests isolés avec MongoDB Testcontainers et WireMock couvrent succès, partenaire lent, échec définitif, échec partiel et demandes concurrentes sans mélange d'états ni de résultats.
- [ ] Les tests interrogent le suivi avec une échéance bornée et vérifient qu'une erreur après le `202` devient visible avec un diagnostic adapté.
- [ ] La procédure locale permet de reproduire le parcours sans partenaire réel ni donnée personnelle.

### INT-27 — Séparer les métriques
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux mesurer séparément l'attente HTTP, le traitement métier et la réussite, afin de ne pas confondre réponse rapide et travail terminé.

**À faire :**
- Préciser le début, la fin et la population de chaque mesure.
- Ajouter seulement les mesures et points de consultation Actuator nécessaires.

**Terminé quand :**
- [ ] Les vérifications distinguent les trois mesures, notamment une réponse `202` obtenue alors que le traitement continue.
- [ ] Les mesures exposées ne contiennent ni données personnelles ni UUID comme étiquette de métrique.

### INT-28 — Comparer et documenter
**Statut : à faire — feu vert requis.**

En tant que développeur débutant, je veux comparer les parcours séquentiel et parallèle dans les mêmes conditions, afin d'expliquer les gains observés sans promettre une performance générale.

**À faire :**
- Mesurer les deux parcours avec les mêmes délais partenaires contrôlés et les mêmes ressources.
- Décrire l'environnement, l'échantillon, la variation des résultats et les limites constatées, dont la saturation et le risque d'abandon après crash.

**Terminé quand :**
- [ ] Le protocole permet de reproduire la comparaison ; les tests de concurrence gardent des futures/latches et des attentes bornées, pas des `sleep` ou un seuil chronométrique fragile comme preuve.
- [ ] Le compte rendu étiquette les résultats « mesures ConfluxLab », explique leurs limites et ne les attribue pas à AXA.

## Limites et hors périmètre

Le parcours reste un monolithe modulaire : Experience/BFF mince appelle Capability métier en Java dans la même JVM ; Feign/Resilience4j ne visent que WireMock.
Un événement local n'est pas durable : après un crash, une demande peut rester abandonnée ; ce sprint ne promet pas une reprise automatique.
Pas de plateforme d'observabilité de production, de benchmark commercial, de garantie absolue ni de cloud ; les chiffres historiques rapportés ne sont pas une cible à reproduire.

## Démo et clôture

- [ ] Montrer une demande acceptée puis terminée, retrouver un échec par son UUID et expliquer pourquoi la latence HTTP ne prouve pas la durée métier.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les rapports de tests, le protocole de mesure et les résultats réellement observés.
