# Sprint 06 — Partenaire fictif et résilience proportionnée

**Statut : planifié.** Prérequis : sprint 05 clos et accord pour ouvrir ce sprint.

## Objectif

Appeler un partenaire fictif pour les six opérations, puis gérer ses pannes sans répéter les créations à l’aveugle ni masquer les échecs.
Ce sprint est un objectif pédagogique : chaque tâche attend son feu vert individuel ; le plan ne lance rien et les tests accompagnent chaque tâche.

## Repères

- **WireMock** simule une API HTTP avec des réponses et des erreurs choisies pour les tests.
- **Feign** est le client HTTP qui appelle ce partenaire ; ses appels restent bloquants, même dans une future.
- Un **retry** est une nouvelle tentative, permise seulement si l'erreur est transitoire et si répéter l'opération est sûr.

## Tâches — un feu vert par tâche

### INT-21 — Définir le partenaire
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux un partenaire fictif au comportement connu, afin de tester les six opérations sans dépendre d'un service réel.

**À faire :**
- Décrire les requêtes, réponses, identifiants et erreurs A–F dans WireMock, sans prétendre reproduire les endpoints GFP/Open historiques.
- Garder l'adaptateur HTTP derrière les contrats de l'orchestrateur, hors du BFF.

**Terminé quand :**
- [ ] Les tests WireMock vérifient les échanges A–F et leurs identifiants attendus.
- [ ] Feign/Resilience4j restent réservés au partenaire WireMock ; Experience appelle Capability en Java, jamais en HTTP.

### INT-22 — Brancher Feign sans retry
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux vérifier les appels HTTP sans répétition automatique, afin de comprendre le comportement de départ avant d'ajouter la résilience.

**À faire :**
- Brancher Feign sur WireMock : vérifier un appel, puis le parcours complet.
- Fixer les délais maximaux de connexion et de lecture, puis neutraliser les retries implicites.

**Terminé quand :**
- [ ] Un test atteint `SUCCEEDED` avec les bons identifiants transmis entre les six opérations.
- [ ] Les tests vérifient les délais configurés et l'absence de tentatives automatiques supplémentaires.

### INT-23 — Écrire la matrice d'erreurs
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux distinguer les pannes et les risques de répétition, afin de choisir une réponse adaptée à chaque opération.

**À faire :**
- Lister refus métier, `4xx`, `5xx`, connexion refusée et dépassement de délai (timeout).
- Préciser pour chaque opération si sa répétition est sûre et pourquoi ; après un timeout de création, le résultat partenaire peut rester inconnu.

**Terminé quand :**
- [ ] Le tableau distingue ces erreurs et justifie la possibilité ou l'interdiction de répéter chaque opération.
- [ ] Les tests de `4xx`, timeout et erreur persistante exposent un échec explicite dans le suivi après le `202`, sans présenter un timeout comme une preuve d'absence de création.

### INT-24 — Ajouter la résilience ciblée
**Statut : à faire — feu vert requis.**

En tant que consommateur de l'API, je veux que seules les tentatives sûres soient répétées, afin de mieux supporter une panne temporaire sans provoquer de doublon.

**À faire :**
- Ajouter avec Resilience4j un retry borné seulement pour une erreur transitoire et une répétition sûre ; pour une création, simuler et tester une garantie partenaire contre les doublons, ou ne pas réessayer.
- Étudier un coupe-circuit (circuit breaker) sur un scénario concret avant tout ajout ; ne jamais remplacer un échec par une réussite fictive.

**Terminé quand :**
- [ ] Les tests comptent exactement les tentatives, y compris l'absence de retry d'une création non sûre et la garantie partenaire si elle est retenue.
- [ ] Un test d'échec partiel montre les appels déjà réussis et l'absence de rollback global, sans réussite artificielle.

## Limites et hors périmètre

Le monolithe modulaire conserve Experience/BFF mince et Capability métier dans la même JVM ; le BFF ne porte ni métier ni persistance.
L'UUID local sert à corréler une demande, pas à garantir l'idempotence, c'est-à-dire empêcher un effet supplémentaire lors d'une répétition.
Aucun exactly-once, retry aveugle de `POST`, rollback distribué ou compensation automatique : `FAILED` ne signifie pas « aucune ressource externe créée ».
Pas de partenaire réel, de broker, de service mesh ni de coupe-circuit sans scénario justifié.

## Démo et clôture

- [ ] Montrer le succès puis une panne WireMock ; expliquer les tentatives observées et pourquoi un timeout de création peut cacher une création réussie chez le partenaire.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec la matrice d'erreurs, les journaux WireMock des tentatives et les résultats réellement observés.
