# Sprint 06 — Partenaire fictif et résilience proportionnée

**Statut : planifié.** Prérequis : sprint 05 clos et accord pour ouvrir ce sprint.

## Objectif

Brancher l'orchestration sur six opérations HTTP fictives via Feign, puis ajouter uniquement les mécanismes Resilience4j justifiés par des scénarios testés.

## Incréments, un par échange

- [ ] **INT-21 — Définir le partenaire.** Écrire les contrats WireMock A–F, identifiants retournés et erreurs. Ils ne représentent pas les endpoints GFP/Open historiques. Maintenir l'adaptateur HTTP hors du BFF et derrière les contrats utilisés par l'orchestrateur.
- [ ] **INT-22 — Brancher Feign sans retry.** Vérifier un appel, puis le parcours nominal des six opérations. Fixer explicitement les timeouts de connexion/lecture et neutraliser les retries implicites avant d'ajouter une politique volontaire.
- [ ] **INT-23 — Écrire la matrice d'erreurs.** Distinguer refus métier, `4xx`, `5xx`, connexion refusée et timeout avec résultat partenaire inconnu. Pour chaque opération, documenter si et pourquoi sa répétition est sûre.
- [ ] **INT-24 — Ajouter la résilience ciblée.** Configurer un retry borné seulement si erreur transitoire et opération répétable. Pour une création, exiger un mécanisme partenaire d'idempotence explicitement simulé et testé, ou ne pas réessayer. Étudier le circuit breaker sur un cas concret avant de l'ajouter. Ne créer aucun fallback de réussite artificielle.

## Critères de fin et tests

- [ ] WireMock démontre le parcours complet jusqu'à `SUCCEEDED`, avec les bons identifiants propagés entre opérations.
- [ ] Les tests vérifient le nombre exact de tentatives, y compris l'absence de retry sur une création non sûre.
- [ ] `4xx`, timeout et erreurs persistantes donnent un suivi d'échec explicite après le `202`.
- [ ] Un timeout de création est présenté comme résultat partenaire potentiellement inconnu, pas comme preuve d'absence de création.
- [ ] Un test d'échec partiel constate les appels déjà réussis et l'absence de rollback global.
- [ ] Les appels vont uniquement de l'intégration au partenaire ; aucun HTTP Experience → Capability n'est réintroduit.

## Limites et hors périmètre

Aucune garantie exactly-once, aucun retry aveugle de POST, aucune compensation automatique ni partenaire réel. Un état local `FAILED` ne signifie pas « aucune ressource externe créée ». Broker, service mesh et circuit breakers sans scénario justifié restent exclus.

## Explication à maîtriser

Après un timeout, pourquoi réessayer une création peut-il produire un doublon ? Quelles garanties supplémentaires un UUID local ne donne-t-il pas ?

## Preuves de clôture

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), avec matrice d'erreurs, journaux WireMock des tentatives et résultats des tests.
