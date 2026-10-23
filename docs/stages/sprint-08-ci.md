# Sprint 08 — Obtenir une CI exploitable

**Statut : partiel — feu vert local « next tasks go » reçu le 2026-10-23 pour INT-29 à INT-32.** `verify` complet réussi et workflow créé ; preuves GitHub verte/rouge et retour au vert non exécutées, en attente d'accord distant distinct.
Prérequis locaux vérifiés : sprint 07 réalisé, JDK 21, Maven Wrapper et Docker disponibles ; branche `main` suivie sur `origin/main`. Disponibilité effective du runner et droits GitHub non vérifiés dans ce lot.
Un seul feu vert couvre l'incrément ; un **accord distinct reste obligatoire avant tout push, PR ou autre opération distante**.

## Objectif / story

En tant que développeur, je veux une CI qui vérifie réellement le projet et rend ses échecs lisibles, afin de détecter une régression avant fusion, sans déployer.

## Périmètre et actions

- Depuis la racine, exécuter réellement `./mvnw -B -ntp verify` en local, sans filtre de tests, exclusion ni option de saut ; vérifier les versions et Docker.
- Vérifier dans les rapports que Surefire exécute les tests unitaires et Failsafe les tests d'intégration `*IT`, sans partenaire réel ; corriger uniquement le raccordement manquant si nécessaire.
- Confirmer le nom réel de la branche principale avant de configurer les déclencheurs `pull_request` et `push` vers cette branche.
- Après autorisation de réaliser l'incrément, créer le workflow minimal : récupération du code, JDK 21, cache Maven, même `verify` non filtré et permissions minimales (`contents: read` pour le code).
- Utiliser un runner disposant de Docker pour MongoDB Testcontainers et les tests isolés avec WireMock ; Docker ne sert pas à déployer.
- Publier les rapports Surefire et Failsafe disponibles, y compris si la vérification échoue, avec une étape de collecte exécutée même en échec ; vérifier l'absence de secrets et données privées.
- Après l'accord distant distinct, observer un run vert et un run rouge sur une régression contrôlée en PR de test ; retirer cette régression sans laisser la branche principale volontairement cassée.

## Critères observables et preuves minimum

- [x] Le `verify` local non filtré réussit ; commande, environnement et XML inspectés : **190 Surefire + 14 Failsafe**, aucun échec/erreur/ignoré.
- [x] Le workflow est écrit pour `main`, avec `push`/PR ciblant `main`, Temurin 21, cache Maven, `contents: read`, runner `ubuntu-24.04` et contrôle Docker. Syntaxe/structure vérifiées localement ; exécution runner non observée.
- [ ] Un lien ou identifiant de run GitHub Actions réellement vert est conservé ; ses rapports Surefire/Failsafe confirment que les deux niveaux de tests ont tourné.
- [ ] Un lien ou identifiant de run réellement rouge identifie le test volontairement cassé ; les rapports disponibles restent consultables malgré l'échec.
- [ ] Les preuves indiquent le retour à une version sans régression volontaire ; logs et pièces jointes ne contiennent ni secret réel ni donnée privée.

**Preuve locale du 2026-10-23 :** `./mvnw -B -ntp verify` depuis la racine, sans filtre, exclusion ni saut : `BUILD SUCCESS`, **204 cas**, environ 1 min 11 s. Linux amd64, Temurin 21.0.12.1, Maven 3.9.16, Docker 29.8.0 ; Failsafe déjà raccordé, aucun POM ou test modifié. Workflow [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), collecte multi-module `if: always()`, rétention 14 jours. L'upload en échec et la confidentialité des futurs artefacts ne sont pas validés par un run local.

**Note commune : [INT-29 à INT-32 — Vérifier le projet et préparer la CI](../../notes/sprint-08/INT-29-verifier-et-preparer-la-ci.md).** Résultats, fragments du workflow, rapports et protocole distant restant. Aucun commit, push, PR ou run distant réalisé dans ce lot.

## Limites nécessaires

Un YAML présent ou un succès local ne prouve pas une CI verte ; un échec de configuration ne remplace pas la preuve rouge d'une régression détectée.
Si Docker, GitHub, les droits ou le runner manquent, marquer l'incrément **partiel ou bloqué**, préciser les preuves manquantes et ne pas déclarer la CI verte ou validée.
Pas de déploiement, publication d'image, secret de production ni infrastructure distante à créer. Aucun outil de couverture, lint ou analyse supplémentaire imposé.
La procédure minimale pour un clone neuf tient aux prérequis, à la commande et aux accès aux rapports : pas de cours CI séparé.

## Traçabilité des IDs conservés

| ID | Ancien sujet intégré |
|---|---|
| INT-29 | Vérifier la reproductibilité locale |
| INT-30 | Créer la CI minimale |
| INT-31 | Rendre les échecs lisibles |
| INT-32 | Observer les preuves |

**Clôture en attente :** compléter la note commune avec les identifiants/liens de runs réellement vert et rouge, leurs rapports et le retrait de la régression contrôlée. Le YAML et le succès local ne terminent pas INT-29 à INT-32 ; INT-33 à INT-36 ne sont pas engagés.
