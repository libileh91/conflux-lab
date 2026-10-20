# Sprint 08 — CI réellement exécutée

**Statut : planifié.** Prérequis : sprint 07 clos ; accord explicite pour la CI ; dépôt Git/GitHub et runner Docker disponibles. Aucun dépôt distant n'est créé par ce plan.

## Objectif

Exécuter les mêmes vérifications reproductibles sur GitHub Actions et détecter une régression avant fusion, sans livraison ni déploiement.

## Incréments, un par échange

- [ ] **INT-29 — Vérifier la reproductibilité locale.** Confirmer `./mvnw -B -ntp verify`, versions, absence de service réel obligatoire et raccordement des tests `*IT` à Failsafe. Vérifier Git et la branche principale réelle avant d'écrire les déclencheurs.
- [ ] **INT-30 — Créer la CI minimale.** Déclencheurs `pull_request` et `push` sur la branche principale ; checkout, JDK 21, cache Maven et permissions minimales. Le runner doit pouvoir exécuter Testcontainers, sans impliquer un déploiement Docker.
- [ ] **INT-31 — Rendre les échecs lisibles.** Publier les rapports Surefire/Failsafe utiles même en échec ; ne jamais transmettre de secret réel ni de payload personnel dans les logs/artefacts.
- [ ] **INT-32 — Observer les preuves.** Avec accord avant toute action distante, observer un run réussi et un run échouant pour une régression contrôlée sur une PR de test. Ne pas laisser volontairement la branche principale cassée.

## Critères de fin et tests

- [ ] Les tests unitaires et d'intégration sont visibles dans les rapports du run réel.
- [ ] Un lien ou identifiant de run réussi est enregistré ; un fichier YAML présent ne suffit pas.
- [ ] Un test volontairement cassé rend le workflow rouge et ses rapports permettent d'identifier la cause.
- [ ] Un clone neuf dispose de la procédure et des prérequis nécessaires pour exécuter les vérifications.
- [ ] Aucun secret de déploiement, publication d'image ou étape de livraison n'est ajouté.

## Limites et hors périmètre

Si GitHub ou le runner requis n'est pas disponible, consigner le blocage : la CI n'est pas déclarée validée. JaCoCo, lint et analyse de dépendances sont des améliorations éventuelles après cette CI minimale, pas un catalogue à installer par défaut. Aucune CD opérationnelle.

## Explication à maîtriser

Pourquoi Maven `verify` ne garantit-il pas à lui seul l'exécution des tests nommés `*IT` ? Quelle différence entre Docker pour les tests et Docker pour déployer ?

## Preuves de clôture

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), avec commandes locales, liens des runs, rapports et limites de l'environnement.
