# Sprint 08 — CI réellement exécutée

**Statut : planifié.** Prérequis : sprint 07 clos, accord explicite pour la CI, dépôt Git/GitHub et runner avec Docker disponibles. Ce plan ne crée aucun dépôt distant.

## Objectif

Faire exécuter les vérifications par GitHub Actions pour détecter une régression avant fusion, sans livrer ni déployer l'application.
Ce sprint prévoit une CI pratique : chaque tâche attend son feu vert individuel et toute opération distante un accord préalable ; le plan ne lance rien.

## Repères

- La **CI** (intégration continue) compile le projet et lance ses tests après un changement de code.
- Un **runner** est la machine qui exécute les étapes GitHub Actions ; Docker y sert aux tests, pas au déploiement.
- **Failsafe** est le plugin Maven qui doit raccorder les tests d'intégration `*IT` au cycle `verify`.

## Tâches — un feu vert par tâche

### INT-29 — Vérifier la reproductibilité locale
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux lancer les mêmes vérifications sur une installation neuve, afin de ne pas dépendre de réglages cachés sur ma machine.

**À faire :**
- Vérifier depuis la racine `./mvnw -B -ntp verify`, les versions requises et le raccordement des `*IT` à Failsafe.
- Vérifier Git et le nom réel de la branche principale avant de définir les déclencheurs.

**Terminé quand :**
- [ ] La commande exécute les tests unitaires et d'intégration, avec des rapports qui le prouvent, sans service partenaire réel.
- [ ] Une procédure pour un clone neuf indique les versions, Docker et la commande ; le nom réel de la branche principale est confirmé.

### INT-30 — Créer la CI minimale
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux automatiser les vérifications à chaque changement proposé, afin de détecter un problème avant son intégration à la branche principale.

**À faire :**
- Prévoir `pull_request` et `push` sur la branche principale réelle, puis récupération du code, JDK 21, cache Maven et `./mvnw -B -ntp verify`.
- Donner seulement les permissions nécessaires et utiliser un runner capable d'exécuter Testcontainers.

**Terminé quand :**
- [ ] Le workflow décrit les déclencheurs attendus et les étapes minimales avec des permissions limitées.
- [ ] Le runner dispose de Docker pour les tests ; le workflow ne contient ni livraison, ni déploiement, ni publication d'image.

### INT-31 — Rendre les échecs lisibles
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux consulter les rapports quand la CI échoue, afin de trouver le test responsable sans devoir deviner la cause.

**À faire :**
- Rendre disponibles les rapports des tests unitaires (Surefire) et d'intégration (Failsafe), même en échec.
- Vérifier que les logs et fichiers joints ne contiennent ni secret réel ni donnée personnelle.

**Terminé quand :**
- [ ] Sur un échec de test, les rapports utiles restent accessibles et permettent d'identifier la cause.
- [ ] La revue des logs et fichiers joints ne révèle aucun secret réel ni contenu personnel.

### INT-32 — Observer les preuves
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux observer une CI verte et une régression détectée en rouge, afin de vérifier que l'automatisation teste réellement le projet.

**À faire :**
- Avec accord avant les opérations distantes, observer un run réussi puis une régression contrôlée sur une demande de fusion de test.
- Enregistrer les preuves et ne pas laisser volontairement la branche principale cassée.

**Terminé quand :**
- [ ] Un lien ou identifiant de run réellement réussi est consigné ; ses rapports montrent les tests unitaires et d'intégration.
- [ ] Un run réellement rouge révèle le test volontairement cassé, avec ses rapports ; la branche principale n'est pas laissée en échec volontaire.

## Limites et hors périmètre

Ne pas lancer Testcontainers avant disponibilité de Docker. Si GitHub ou le runner manque, consigner le blocage sans déclarer la CI validée.
Un fichier YAML ne prouve ni une CI verte ni l'exécution des `*IT` : `verify` doit être relié à Failsafe, puis les runs réellement observés.
JaCoCo, lint et analyse des dépendances restent des améliorations éventuelles, pas des ajouts par défaut. Aucun secret de déploiement ni CD opérationnelle.

## Démo et clôture

- [ ] Présenter les runs vert et rouge et leurs rapports ; expliquer pourquoi Docker pour les tests ne signifie pas déploiement.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les commandes locales, liens des runs, rapports et limites de l'environnement réellement observés.
