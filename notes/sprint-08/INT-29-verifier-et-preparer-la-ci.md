# INT-29 à INT-32 — Vérifier le projet et préparer la CI

**[Simulation ConfluxLab] — Partiel au 2026-10-23, sur feu vert local « next tasks go », puis accord explicite « feu-vert sur tout la ci ».** La vérification locale complète est réussie et le workflow est créé. La publication et la PR de régression contrôlée sont désormais autorisées ; **aucun run GitHub vert ou rouge n'est encore observé à ce stade**. Les preuves restent à compléter avant clôture.

## 1. Ce qui fonctionne en local

Depuis la racine, commande réellement exécutée **sans filtre, exclusion ni option de saut** :

```sh
./mvnw -B -ntp verify
```

Environnement observé : Linux amd64, Temurin **21.0.12.1**, Maven Wrapper **3.9.16**, Docker Engine **29.8.0** accessible. Le Wrapper est suivi comme exécutable (`100755`). Pour un clone neuf, il faut JDK 21, Docker démarré et l'accès réseau aux dépendances Maven et à l'image `mongo:8.0.16` ; aucun MongoDB manuel ou partenaire réel à configurer pour les tests.

**Résultat observé : `BUILD SUCCESS`, 204 cas au total, aucun échec, erreur ou cas ignoré.** Les rapports XML ont été inspectés et le résumé Failsafe confirme l'exécution des 14 IT. Durée Maven observée : environ 1 min 11 s ; ce n'est pas une mesure de performance métier.

| Module | Cas Surefire | Cas Failsafe |
|---|---:|---:|
| `contracts` | 59 | — |
| `capability-api` | 52 | — |
| `experience-api` | 4 | — |
| `application` | 75 | 14 |
| **Total** | **190** | **14** |

Surefire inclut les tests unitaires mais aussi certains tests Spring/MongoDB ; son nom ne garantit pas qu'un test soit unitaire. Les six classes Failsafe couvrent persistance/redémarrage du JAR, événements, transitions MongoDB, parcours partenaire et démonstration. WireMock et Testcontainers restent isolés ; aucune réécriture des tests ou modification des POM n'a été nécessaire.

Les sorties sont sous `contracts/target/surefire-reports/`, `capability-api/target/surefire-reports/`, `experience-api/target/surefire-reports/`, puis `application/target/surefire-reports/` et `application/target/failsafe-reports/`. Elles sont générées et ignorées par Git. Les chiffres des anciennes notes restent historiques ; cette exécution globale ne les remplace pas rétroactivement.

## 2. Le workflow ajouté

Source : [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml), extraits partiels.

```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
```

Les événements couvrent les push sur `main` et les PR **ciblant** `main`. Ce nom est confirmé par la branche locale et son suivi `origin/main`, utilisé lors du push précédent ; la configuration par défaut du dépôt GitHub n'a pas été interrogée à distance dans ce lot.

Le job utilise `ubuntu-24.04`, Temurin 21 et un cache Maven indexé sur les POM. `docker info` vérifie le daemon avant le build : le runner GitHub hébergé fournit normalement Docker, mais aucun résultat de runner n'est revendiqué ici. Le job est borné à 15 minutes et exécute la même commande `verify` depuis la racine.

```yaml
permissions:
  contents: read
```

Le token du workflow n'a pas de permission d'écriture sur le dépôt. Le checkout ne conserve pas ses credentials. Les actions officielles `checkout@v4`, `setup-java@v4` et `upload-artifact@v4` sont référencées ; aucune étape de déploiement, publication d'image ou injection de secret applicatif n'est ajoutée.

## 3. Conserver un diagnostic même si les tests échouent

```yaml
- name: Upload test reports
  if: always()
  uses: actions/upload-artifact@v4
```

Cette étape collecte les répertoires Surefire et Failsafe de **tous les modules** dans l'artefact `test-reports`, conservé 14 jours. Si Maven échoue, son code retour n'est pas masqué : aucun `continue-on-error`, filtre ou contournement ne fabrique du vert. L'upload tente de conserver les rapports déjà disponibles.

Limites : un échec de compilation peut ne produire aucun rapport ; un échec Surefire peut empêcher Failsafe de démarrer. L'absence de fichiers donne un avertissement. `always()` ne garantit pas un upload après perte du runner, expiration du job ou annulation. La présence effective des artefacts après un test rouge reste à observer sur GitHub.

Les rapports peuvent inclure sorties de tests, fixtures et propriétés système. Le workflow n'injecte aucun secret explicite et les données de test sont fictives. Un contrôle local borné de **48 fichiers de rapports** n'a trouvé aucun motif de clé privée, token GitHub ou URI MongoDB authentifiée ; ce n'est **pas un audit exhaustif** ni une garantie pour de futures données. Ne jamais ajouter de données réelles ou secrets dans les fixtures/propriétés collectées.

## 4. Validation réalisée et preuves manquantes

| Élément | État prouvé |
|---|---|
| `verify` local non filtré | **Réussi : 190 Surefire + 14 Failsafe**, sans échec/erreur/ignoré |
| Workflow et chemins de collecte | Fichier créé ; syntaxe YAML et structure ciblée vérifiées avec PyYAML ; les globs correspondent à quatre répertoires Surefire et un Failsafe locaux |
| Sémantique du workflow | Revue statique réalisée ; pas d'exécution du moteur GitHub ni d'`actionlint` disponible localement |
| Run GitHub vert et rapports | **Non exécuté / non observé** |
| Run GitHub rouge sur régression contrôlée | **Non exécuté / non observé** |
| Retour au vert après retrait de cette régression | **Non exécuté / non observé** ; aucune régression volontaire n'a été introduite |

L'accord distant explicite étant reçu, la preuve restante consiste à :

1. Publier la version saine et conserver l'identifiant/lien du run vert ainsi que ses rapports Surefire/Failsafe.
2. Créer une branche/PR de test vers `main`, y casser volontairement une assertion existante et observer le test rouge avec ses rapports disponibles.
3. Retirer la régression sur cette branche, observer le retour au vert et conserver les liens, sans fusionner une version volontairement cassée dans `main`.

Ces étapes sont un **protocole autorisé restant à exécuter**, pas des preuves déjà obtenues. Les runs consommeront des ressources GitHub Actions selon l'offre du dépôt et téléchargeront actions, dépendances et images de test. Docker sert aux tests, pas à un déploiement.

Le [sprint 08](../../docs/stages/sprint-08-ci.md), [l'index](../README.md) et le [registre](../todos.md) signalent ce statut partiel. **INT-33 à INT-36 ne sont pas engagés** et la CD reste théorique. Publication et observation des runs autorisées ; les identifiants et résultats seront conservés après exécution effective.
