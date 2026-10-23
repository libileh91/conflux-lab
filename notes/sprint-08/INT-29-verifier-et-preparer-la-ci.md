# INT-29 à INT-32 — Vérifier le projet et observer la CI

**[Simulation ConfluxLab] — Réalisé le 2026-10-23**, sur feu vert local « next tasks go », puis accord explicite « feu-vert sur tout la ci » pour commit, push et PR de preuve. Vérification locale complète, **run GitHub vert, régression contrôlée rouge avec rapports disponibles, puis retour au vert réellement observés**. Aucun déploiement ; la compréhension orale n'est pas évaluée par ces résultats.

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

Surefire inclut les tests unitaires mais aussi certains tests Spring/MongoDB ; son nom ne garantit pas qu'un test soit unitaire. Les six classes Failsafe couvrent persistance/redémarrage du JAR, événements, transitions MongoDB, parcours partenaire et démonstration. WireMock et Testcontainers restent isolés. Aucun POM modifié ; une course de préparation d'un test async a ensuite été corrigée après sa détection sur GitHub, sans modifier le comportement applicatif (voir section 5).

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

Les événements couvrent les push sur `main` et les PR **ciblant** `main`. La branche par défaut **`main` a été confirmée sur GitHub** avec `gh repo view libileh91/conflux-lab`, en plus du suivi local `origin/main`.

Le job utilise `ubuntu-24.04`, Temurin 21 et un cache Maven indexé sur les POM. `docker info` vérifie le daemon avant le build ; cette étape et les tests MongoDB Testcontainers ont effectivement réussi sur le runner GitHub. Le job est borné à 15 minutes et exécute la même commande `verify` depuis la racine.

```yaml
permissions:
  contents: read
```

Le token du workflow n'a pas de permission d'écriture sur le dépôt. Le checkout ne conserve pas ses credentials. Les actions officielles sont fixées aux versions stables **`checkout@v7.0.1`, `setup-java@v6.1.0`, `upload-artifact@v7.0.2`**. Leurs releases et métadonnées ont été vérifiées sur GitHub : runtime Node 24 et entrées compatibles. Le premier run vert avec `v4` signalait une dépréciation ; le commit `5668bbb` a actualisé les actions, puis son propre run a réussi sans ces avertissements. Aucune étape de déploiement, publication d'image ou injection de secret applicatif.

## 3. Conserver un diagnostic même si les tests échouent

```yaml
- name: Upload test reports
  if: always()
  uses: actions/upload-artifact@v7.0.2
```

Cette étape collecte les répertoires Surefire et Failsafe de **tous les modules** dans l'artefact `test-reports`, conservé 14 jours. Si Maven échoue, son code retour n'est pas masqué : aucun `continue-on-error`, filtre ou contournement ne fabrique du vert. L'upload tente de conserver les rapports déjà disponibles.

Limites : un échec de compilation peut ne produire aucun rapport ; un échec Surefire peut empêcher Failsafe de démarrer. L'absence de fichiers donne un avertissement. `always()` ne garantit pas un upload après perte du runner, expiration du job ou annulation. La disponibilité après l'échec contrôlé est désormais vérifiée : étape d'upload réussie et artefact téléchargé malgré le code retour Maven non nul.

Les rapports peuvent inclure sorties de tests, fixtures et propriétés système. Le workflow n'injecte aucun secret explicite et les données de test sont fictives. Le contrôle local initial de 48 fichiers, puis celui de **143 fichiers téléchargés des trois artefacts distants vert/rouge/retour au vert**, n'ont trouvé aucun motif de clé privée, token GitHub ou URI MongoDB authentifiée. Les sorties examinées utilisent des données fictives ; ces contrôles ne sont **pas un audit exhaustif** ni une garantie pour de futures données. Ne jamais ajouter de données réelles ou secrets dans les fixtures/propriétés collectées.

## 4. Runs et rapports effectivement observés

[PR de preuve #1](https://github.com/libileh91/conflux-lab/pull/1), branche `ci/proof-int-32`, sans fusion de la régression dans `main`. Tous les runs utilisent le `verify` complet, sans filtre ni saut.

| Preuve du 2026-10-23 | Commit de tête | Résultat des XML téléchargés |
|---|---|---|
| [Vert sur main, actions Node 24 — 39933137330](https://github.com/libileh91/conflux-lab/actions/runs/39933137330) | `5668bbb` | **190 Surefire + 14 Failsafe**, aucun échec/erreur/ignoré |
| [Rouge contrôlé sur PR — 39933362753](https://github.com/libileh91/conflux-lab/actions/runs/39933362753) | `4ace6ff` | 190 Surefire réussis ; 14 Failsafe dont **1 assertion en échec**, 0 erreur/ignoré |
| [Premier retour après restauration — 39933581860](https://github.com/libileh91/conflux-lab/actions/runs/39933581860) | `7113c95` | Assertion volontaire restaurée ; 190 Surefire réussis, 14 Failsafe dont **1 erreur async indépendante**, détaillée ci-dessous |
| [Retour au vert confirmé sur PR — 39934115383](https://github.com/libileh91/conflux-lab/actions/runs/39934115383) | `6a77745` | **190 Surefire + 14 Failsafe**, aucun échec/erreur/ignoré |

Le premier bootstrap [`39932689507`](https://github.com/libileh91/conflux-lab/actions/runs/39932689507), commit `b62b250`, était aussi vert avec 204 cas, avant actualisation des actions. Ces passages ne s'additionnent pas en couverture.

Les trois artefacts principaux portent le nom `test-reports` : identifiants **12491962758** (vert Node 24), **12492318224** (rouge contrôlé), **12492678545** (retour vert). Ils ont été téléchargés et leurs XML inspectés : 16 suites Surefire et 6 Failsafe. L'upload réussit aussi dans le run rouge. Accès depuis chaque run, rubrique **Artifacts**, pendant la rétention de 14 jours ; les liens et le bilan restent dans cette note après expiration.

Pour inspecter les preuves, commandes réellement utilisées depuis la racine (le répertoire de téléchargement est local, ignoré par Git) :

```sh
gh run view 39933362753 --repo libileh91/conflux-lab
gh run download 39933362753 --repo libileh91/conflux-lab --name test-reports --dir application/target/ci-evidence/red
```

L'assertion volontaire dans `FamilyRequestPersistenceIT.retainsTheSameRequestAfterRestartingThePackagedApplication` attendait **201 au lieu du vrai 202**. Le rapport rouge contient `expected: <201> but was: <202>` : il s'agit d'une assertion réellement détectée, pas d'un échec YAML ou Docker. Le commit `7113c95` annule exactement cette modification. Aucun code applicatif changé pour cette preuve.

## 5. Course de test révélée par la CI

Le premier run restauré a exposé un défaut distinct : `FamilyRequestEventsIT.acceptsWhileTheListenerIsBlockedAndOnlyOneDuplicateEventCanClaim` pouvait rejeter le deuxième événement. Avec deux core threads déjà créés, même inactifs, les soumissions passent par la file de capacité 1 ; publier deux tâches sans attendre la prise de la première peut remplir cette file avant le réveil des workers.

Correction uniquement dans [le test d'événements](../../application/src/test/java/dev/confluxlab/capability/internal/event/FamilyRequestEventsIT.java), fragment :

```java
await("First listener to reach the claim").atMost(Duration.ofSeconds(5))
        .until(() -> ready.getCount() == 1);
events.publishEvent(new FamilyRequestSubmitted(requestId));
```

Le premier worker atteint le claim bloqué avant la seconde soumission. Les deux claims restent ensuite en concurrence derrière le même latch ; la preuve du gagnant unique est conservée. Même synchronisation entre les deux premières soumissions du scénario de saturation : deux workers occupés, troisième tâche en file, quatrième refusée. **Aucune capacité ou assertion modifiée, aucun retry de test, aucun sleep arbitraire.**

Validation locale ciblée réellement exécutée avant publication du correctif :

```sh
./mvnw -B -ntp -pl application -am -Dtest=FamilyRequestListenerTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=FamilyRequestEventsIT verify
```

**1 Surefire + 3 Failsafe réussis**, aucun échec/erreur/ignoré, puis `verify` GitHub complet vert à 204 cas. Le correctif `6a77745` de la branche de preuve est reporté seul sur `main` par `e175ca3`, sans y apporter les commits de régression/restauration. La PR de preuve est fermée sans fusion ; sa branche et ses liens conservent la trace des essais.

Le [sprint 08](../../docs/stages/sprint-08-ci.md), [l'index](../README.md) et [TD-B019](../todos.md#td-b019) conservent ces preuves. Les runs ont consommé des ressources GitHub Actions ; aucune image applicative publiée, aucun secret créé et aucun déploiement. **INT-33 à INT-36 ne sont pas engagés** ; la CD reste théorique.
