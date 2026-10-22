# Sprint 08 — Obtenir une CI exploitable

**Statut : non commencé — feu vert requis pour l'incrément complet INT-29 à INT-32.**
Prérequis : sprint 07 clos, dépôt Git/GitHub accessible et Docker disponible localement et sur le runner.
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

- [ ] Le `verify` local non filtré réussit ; sa commande, l'environnement et les rapports prouvent l'exécution des tests unitaires et d'intégration, sans tests sautés pour obtenir le vert.
- [ ] Le workflow utilise la branche principale confirmée, les deux déclencheurs, JDK 21, cache Maven, permissions minimales et un runner avec Docker.
- [ ] Un lien ou identifiant de run GitHub Actions réellement vert est conservé ; ses rapports Surefire/Failsafe confirment que les deux niveaux de tests ont tourné.
- [ ] Un lien ou identifiant de run réellement rouge identifie le test volontairement cassé ; les rapports disponibles restent consultables malgré l'échec.
- [ ] Les preuves indiquent le retour à une version sans régression volontaire ; logs et pièces jointes ne contiennent ni secret réel ni donnée privée.

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

**Clôture future :** une seule note synthétique pour INT-29 à INT-32, avec résultat local, liens des runs, rapports et blocages réels ; pas quatre notes ni validations indépendantes.
Cette réécriture ne crée aucun workflow et n'autorise aucun test, build, push, PR ou lancement distant.
