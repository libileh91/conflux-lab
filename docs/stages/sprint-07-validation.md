# Sprint 07 — Parcours complet, observabilité et mesures

**Statut : planifié.** Prérequis : sprint 06 clos et accord pour ouvrir ce sprint.

## Objectif

Rendre le parcours complet démontrable et ses échecs explicables. Ce sprint consolide les tests existants ; il ne marque pas le début de la stratégie de test.

## Incréments, un par échange

- [ ] **INT-25 — Corréler le parcours.** Associer `requestId`, opération et changement d'état aux logs, y compris sur les threads asynchrones. Si un MDC est utilisé, propager puis nettoyer son contexte pour éviter les mélanges entre demandes.
- [ ] **INT-26 — Consolider les scénarios.** Parcours HTTP public → Capability → MongoDB → événement → orchestration → WireMock → suivi terminal. Couvrir succès, partenaire lent, échec définitif, échec partiel et demandes concurrentes.
- [ ] **INT-27 — Séparer les métriques.** Définir latence HTTP, durée métier et taux de réussite avec début, fin et population explicites. Ajouter seulement les métriques/points Actuator nécessaires, sans données personnelles ni UUID comme étiquette à forte cardinalité.
- [ ] **INT-28 — Comparer et documenter.** Mesurer séquentiel/parallèle avec les mêmes délais partenaires contrôlés et les mêmes ressources. Documenter environnement, échantillon, variance et limites, sans transformer un test de concurrence en assertion chronométrique fragile.

## Critères de fin et tests

- [ ] Les tests d'intégration tournent avec MongoDB Testcontainers et WireMock isolés, sans partenaire réel.
- [ ] Le polling attend un état terminal avec une échéance, pas indéfiniment.
- [ ] Deux demandes concurrentes ne partagent ni état, ni résultat, ni contexte de log.
- [ ] Une erreur survenue après le `202` est visible dans le GET de suivi, avec diagnostic adapté.
- [ ] La saturation et le risque de demandes abandonnées après crash sont décrits avec les limites constatées.
- [ ] Une procédure locale explique lancement, payload fictif, suivi, diagnostic et arrêt.
- [ ] Les chiffres sont étiquetés « mesures ConfluxLab » ; aucune métrique du labo n'est attribuée à AXA.

## Hors périmètre

Plateforme d'observabilité de production, benchmark commercial, garantie de performance absolue et infrastructure cloud. Le plan ne promet pas de reproduire les chiffres historiques rapportés.

## Explication à maîtriser

Pourquoi une réponse HTTP rapide ne prouve-t-elle pas un traitement métier plus rapide ? Quelle preuve distingue un graphe correct d'un simple gain de temps apparent ?

## Preuves de clôture

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), avec rapports de tests, protocole de mesure et résultats réellement observés.
