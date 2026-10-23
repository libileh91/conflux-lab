# Sprint 08 — Obtenir une CI exploitable

**Statut : INT-29 à INT-32 réalisés le 2026-10-23**, feu vert local « next tasks go », puis accord distant explicite « feu-vert sur tout la ci ». `verify` local, runs GitHub vert/rouge/retour au vert et artefacts réellement vérifiés.
Prérequis vérifiés : JDK 21, Maven Wrapper, Docker local et sur runner ; `main` confirmée comme branche par défaut sur GitHub. PR de preuve #1 fermée sans fusion ; aucune régression volontaire sur `main`.
L'accord couvre cet incrément et ses opérations CI, pas INT-33 à INT-36 ni un déploiement.

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
- [x] Le workflow est écrit pour `main`, avec `push`/PR ciblant `main`, Temurin 21, cache Maven, `contents: read`, runner `ubuntu-24.04` et contrôle Docker. Syntaxe/structure vérifiées localement et exécution effective sur le runner GitHub confirmée.
- [x] [Run vert sur main 39933137330](https://github.com/libileh91/conflux-lab/actions/runs/39933137330) : artefact téléchargé, **190 Surefire + 14 Failsafe**, aucun échec/erreur/ignoré.
- [x] [Run rouge contrôlé 39933362753](https://github.com/libileh91/conflux-lab/actions/runs/39933362753) : seule assertion `FamilyRequestPersistenceIT` attend 201 au lieu de 202 ; upload réussi, XML téléchargés malgré l'échec.
- [x] [Retour vert 39934115383](https://github.com/libileh91/conflux-lab/actions/runs/39934115383), **204 cas réussis** : assertion restaurée et course d'un test async corrigée sans contournement. Rapports fictifs contrôlés selon le périmètre borné décrit dans la note ; PR fermée sans fusion.

**Preuve locale du 2026-10-23 :** `./mvnw -B -ntp verify` depuis la racine, sans filtre, exclusion ni saut : `BUILD SUCCESS`, **204 cas**, environ 1 min 11 s. Linux amd64, Temurin 21.0.12.1, Maven 3.9.16, Docker 29.8.0 ; Failsafe déjà raccordé, aucun POM modifié. Une course de préparation dans `FamilyRequestEventsIT`, révélée par le premier run restauré, a ensuite été corrigée par synchronisation des workers, sans changer assertions ou capacité du pool. Workflow [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), collecte multi-module `if: always()`, rétention 14 jours. L'upload en échec est désormais vérifié à distance ; le contrôle borné des rapports téléchargés ne constitue pas un audit exhaustif des futurs artefacts.

**Note commune : [INT-29 à INT-32 — Vérifier le projet et observer la CI](../../notes/sprint-08/INT-29-verifier-et-preparer-la-ci.md).** Résultats, versions d'actions Node 24, runs, artefacts, échec indépendant rencontré et correction. Commits CI publiés ; [PR #1](https://github.com/libileh91/conflux-lab/pull/1) fermée sans fusion. Seul le correctif de synchronisation est reporté sur `main`.

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

**Résultat technique prouvé :** les runs et XML liés ci-dessus valident le vert, la détection d'une assertion cassée, les rapports malgré l'échec et le retour au vert. Pas de déploiement ni de compréhension orale déclarée acquise. INT-33 à INT-36 ne sont pas engagés.
