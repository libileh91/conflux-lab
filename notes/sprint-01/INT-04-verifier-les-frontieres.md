# INT-04 — Vérifier les frontières

> **Repère temporel — depuis INT-07.** Le GET d'une demande conservée retourne `200` avec les seules métadonnées `requestId`, `status` et `createdAt` ; UUID inconnu : `404`, UUID malformé : `400`. Les constats, extraits et preuves chiffrées ci-dessous, notamment les anciens GET `503`, restent historiques et propres aux étapes décrites. Voir le [cours INT-07](../sprint-02/INT-07-consulter-la-demande.md).

> **Actualisation INT-06 — 2026-10-21, après la clôture du sprint 01.** L'[acceptation INT-06](../sprint-02/INT-06-accepter-une-demande.md) est désormais réalisée : enregistrement en mémoire avant retour du `202`, avec UUID et statut `PENDING`. Le GET reste en `503` jusqu'à INT-07 ; pas de MongoDB, d'événement Spring ni d'asynchronisme. Les vérifications historiques ci-dessous ont été exécutées par l'agent ; aucun build n'est relancé pour cette actualisation.

| Contexte | Description |
|---|---|
| **Sprint** | [01 — Socle du monolithe modulaire](../../docs/stages/sprint-01-socle.md) |
| **Statut** | **Terminée sur demande explicite « commit INT-03 & go 04 »** ; aucun feu vert pour INT-06 à l'issue d'INT-04, le 2026-10-21, avant INT-06 |
| **Date des vérifications** | 2026-10-21 |
| **Objectif du tutoriel** | Rendre les frontières détectables dans le build et vérifier que les détecteurs refusent vraiment les écarts |
| **Origine des preuves** | Nouveaux tests ciblés, `clean verify` global, rapports Surefire, diagnostics et inspection de l'archive dans cette session |
| **Travail Git pendant INT-04** | INT-03 commitée localement : `cea7362`, `feat(INT-03): assemble single Boot application with Java module API` ; aucun push à cette étape |
| **Historique distinct** | Les 64 tests et le démarrage HTTP réel d'INT-03 sont antérieurs ; INT-04 réexécute les tests existants et ajoute 12 tests, sans relancer le serveur du JAR |
| **Suivi** | [Registre central](../todos.md) ; le sprint a ensuite été clôturé techniquement et documenté sur demande explicite, sans évaluation de la restitution de l'apprenant |

**[Simulation ConfluxLab]** Ces tests protègent le découpage du laboratoire. ArchUnit est un choix pédagogique actuel, pas un outil attribué au contexte historique AXA.

**Clôture ultérieure, le 2026-10-21 :** l'utilisateur a demandé la clôture, puis le commit de l'ensemble et son push. Le [journal de clôture du sprint 01](../../docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) conserve le bilan, la synthèse et les limites. La restitution orale n'a pas été évaluée et INT-06 n'était pas autorisée lors de cette clôture du 2026-10-21, avant sa réalisation ultérieure. Les constats Git de la section 4.1 décrivent l'étape antérieure à cette demande de publication.

## Sommaire

- [1. Objectif et vocabulaire](#1-objectif-et-vocabulaire)
- [2. Comprendre le problème](#2-comprendre-le-problème)
- [3. Travail réalisé pas à pas](#3-travail-réalisé-pas-à-pas)
- [4. Commandes et résultats expliqués](#4-commandes-et-résultats-expliqués)
- [5. Vérifications et limites](#5-vérifications-et-limites)
- [6. Formulation d'entretien](#6-formulation-dentretien)
- [7. Suivi centralisé](#7-suivi-centralisé)

## 1. Objectif et vocabulaire

INT-03 a rendu le monolithe démarrable. Cette tâche ajoute une alarme contre les mauvaises dépendances : si le BFF commence à utiliser une classe interne de Capability, le build doit échouer même si Java peut compiler cette référence.

| Terme | Sens concret |
|---|---|
| **Frontière** | Limite de responsabilité et de dépendance entre deux modules |
| **Dépendance Java** | Référence d'une classe à une autre : type d'un champ, d'un paramètre, d'un retour, appel de méthode, etc. |
| **Bytecode** | Instructions et métadonnées des fichiers `.class`, produits par le compilateur |
| **ArchUnit** | Bibliothèque de test qui lit ces classes et vérifie des règles de dépendances |
| **Prédicat** | Condition utilisée pour sélectionner des classes, par exemple « appartient au package Capability mais pas à son API publique » |
| **Slice** | Groupe de classes utilisé ici pour rechercher les cycles entre modules |
| **Cycle** | Chaîne qui revient au point de départ, par exemple Experience → Capability → Experience |
| **Fixture** | Exemple préparé pour un test ; ici, quelques types artificiels avec des dépendances intentionnellement interdites |
| **Contrôle négatif / positif** | Vérifier qu'un exemple interdit est rejeté / qu'un exemple autorisé est accepté |
| **Scope `test`** | Dépendance disponible pour compiler et exécuter les tests, pas pour faire fonctionner l'application livrée |

Ces tests ne créent pas de famille et ne déterminent pas les règles métier. Ils protègent **qui peut connaître qui**.

## 2. Comprendre le problème

### 2.1. Compiler ne suffit pas à protéger un package interne

Le POM d'Experience dépend de `capability-api`. Cette dépendance rend disponibles les classes publiques de l'artefact, pas uniquement celles de `capability.api`.

L'implémentation provisoire d'INT-03 était package-private (service supprimé dans INT-06), mais un futur document ou repository pourrait être public pour répondre à un besoin technique. Java autoriserait alors une référence depuis le BFF si les types sont accessibles. Le nom `internal` exprime une intention ; **ce nom n'interdit pas les accès à lui seul**.

### 2.2. Démarrer Spring ne suffit pas non plus

Le test de contexte prouve que Spring sait créer et relier les composants présents. Un contexte pourrait tout de même démarrer avec un contrôleur qui dépend d'un repository interne. Les deux questions sont différentes :

- **Assemblage :** « les objets nécessaires existent-ils et sont-ils injectables ? »
- **Architecture :** « les dépendances respectent-elles le découpage décidé ? »

Nous conservons donc [`ApplicationAssemblyTest`](../../application/src/test/java/dev/confluxlab/ApplicationAssemblyTest.java), sans le recopier, et ajoutons des tests de frontières distincts.

### 2.3. Une alarme silencieuse peut être une alarme mal branchée

Une règle qui passe sur le code actuel peut être correcte, mais aussi sélectionner le mauvais package. Nous vérifions son comportement avec des exemples fautifs. Nous ajoutons aussi un exemple autorisé : tout interdire ne serait pas une bonne protection.

## 3. Travail réalisé pas à pas

### 3.1. Ajouter une dépendance de test, pas une nouvelle infrastructure

Extrait partiel de [`application/pom.xml`](../../application/pom.xml) :

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit</artifactId>
    <version>1.4.1</version>
    <scope>test</scope>
</dependency>
```

Nous utilisons ArchUnit directement avec le JUnit déjà fourni par le starter de test. Aucun module Maven supplémentaire, moteur JUnit additionnel, plugin de build ou Spring Modulith n'est nécessaire.

Les tests sont placés dans `application` parce que son classpath réunit déjà Experience, Capability et les contrats. Un test local à Capability ne disposerait pas normalement du BFF ; ajouter cette dépendance en production pour tester l'architecture serait justement une mauvaise solution.

L'inspection du JAR après le build a confirmé l'absence d'ArchUnit et des fixtures : l'outil ne change ni l'application livrée ni sa façon de démarrer.

### 3.2. Définir les cinq règles à un seul endroit

Les règles se trouvent dans [`ModuleArchitectureRules.java`](../../application/src/test/java/dev/confluxlab/ModuleArchitectureRules.java), un utilitaire **de test**. Les tests de production et les tests des détecteurs réutilisent les mêmes objets `ArchRule`, au lieu de maintenir deux copies de la politique.

| Règle | Ce qu'elle interdit | Pourquoi |
|---|---|---|
| `MODULES_HAVE_NO_CYCLES` | Cycles entre les sous-packages racine de `dev.confluxlab` | Conserver des dépendances orientées, sans modules qui se connaissent dans les deux sens |
| `CAPABILITY_DOES_NOT_DEPEND_ON_EXPERIENCE` | Tout Capability → Experience, API comprise | Le métier ne doit pas dépendre du BFF |
| `EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API` | Experience → Capability hors `capability.api..` | Le BFF ne doit pas connaître les détails internes, dont le stockage |
| `CONTRACTS_DO_NOT_DEPEND_ON_OTHER_MODULES` | Contracts → tout autre module du projet | Garder les types partagés indépendants des consommateurs |
| `LIBRARIES_DO_NOT_DEPEND_ON_APPLICATION` | Bibliothèques → assemblage, y compris la classe Boot dans le package racine | L'assemblage réunit les bibliothèques ; elles ne doivent pas dépendre de lui |

#### Lire la règle de frontière du BFF

Extrait partiel exact de l'utilitaire :

```java
static final ArchRule EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API = noClasses()
        .that().resideInAPackage(EXPERIENCE)
        .should().dependOnClassesThat(
                resideInAPackage(CAPABILITY).and(resideOutsideOfPackage(CAPABILITY_API)));
```

Les constantes valent respectivement `dev.confluxlab.experience..`, `dev.confluxlab.capability..` et `dev.confluxlab.capability.api..`.

La lecture est : **aucune classe du BFF ne doit dépendre d'une classe qui appartient à Capability et se trouve hors de son API publique**. Le `and` porte sur la cible interdite, pas sur deux règles alternatives.

Dans les motifs ArchUnit, `..` inclut les sous-packages. Ainsi, la règle couvre aussi un futur `capability.repository` ou `capability.persistence`, pas seulement un package précisément nommé `internal`. Les rôles de stockage doivent rester hors de l'API publique, conformément au DAT.

La règle utilise des **dépendances de classes**, pas uniquement des appels de méthodes : un champ typé avec un document interne suffit à être refusé. En revanche, un `import` Java inutilisé et supprimé par la compilation ne crée pas, à lui seul, de dépendance de bytecode.

#### Lire la règle de cycles et sa limite de package

Extrait partiel exact :

```java
static final ArchRule MODULES_HAVE_NO_CYCLES = slices()
        .matching(ROOT + ".(*)..")
        .should().beFreeOfCycles();
```

Avec `ROOT = "dev.confluxlab"`, `(*)` capture le premier sous-package : `experience`, `capability`, `contracts`, etc. Tous les sous-packages de Capability appartiennent donc à la même slice. On contrôle ici les **cycles entre modules**, pas tous les cycles possibles entre classes d'un même module.

`ConfluxLabApplication` est directement dans `dev.confluxlab`, sans premier sous-package à capturer. Il faut donc éviter un angle mort : la cinquième règle interdit aux bibliothèques de dépendre du package racine, réservé à l'assemblage. Elle couvre également un éventuel `dev.confluxlab.application..`. Sans arête de retour autorisée vers l'assemblage, un cycle passant par ce démarrage ne peut pas être accepté par l'ensemble des règles.

### 3.3. Analyser la production sans ses exemples fautifs

Extrait partiel de [`ModuleArchitectureTest.java`](../../application/src/test/java/dev/confluxlab/ModuleArchitectureTest.java) :

```java
private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("dev.confluxlab");
```

`ClassFileImporter` lit les classes compilées accessibles sur le classpath. `DO_NOT_INCLUDE_TESTS` exclut les emplacements de sortie des tests, ici les répertoires Maven conventionnels `target/test-classes`.

Cette exclusion est essentielle : les fixtures portent volontairement des packages de modules et contiennent de mauvaises dépendances. Elles ne font pas partie de la production et ne doivent pas faire échouer son analyse.

Chaque méthode JUnit applique ensuite une règle :

```java
@Test
void experienceOnlyUsesThePublicCapabilityApi() {
    ModuleArchitectureRules.EXPERIENCE_ONLY_USES_PUBLIC_CAPABILITY_API.check(PRODUCTION_CLASSES);
}
```

`check(...)` fait échouer le test si la règle trouve une violation. Les cinq tests sont nommés `*Test`, ce qui les raccorde à Surefire déjà configuré ; aucun branchement Failsafe n'est nécessaire ici.

### 3.4. Vérifier les règles avec six violations contrôlées

Les fixtures sont limitées aux sources de test de `application` :

- [`CapabilityFixtures.java`](../../application/src/test/java/dev/confluxlab/capability/internal/fixture/CapabilityFixtures.java) : types internes artificiels, dépendance inverse et moitié d'un cycle ;
- [`ExperienceFixtures.java`](../../application/src/test/java/dev/confluxlab/experience/fixture/ExperienceFixtures.java) : accès interdits, autre moitié du cycle et exemple autorisé ;
- [`ContractsFixture.java`](../../application/src/test/java/dev/confluxlab/contracts/fixture/ContractsFixture.java) : dépendance interdite vers Capability.

Ces types ne sont ni des repositories MongoDB ni des documents réels. Leurs champs servent uniquement à inscrire une dépendance dans le bytecode. Ils n'ont pas de stéréotype Spring, de stockage ou de parcours métier.

[`ArchitectureRuleDetectionTest.java`](../../application/src/test/java/dev/confluxlab/ArchitectureRuleDetectionTest.java) les importe **explicitement**, sans les mêler au jeu de production.

| Exemple de test | Résultat attendu |
|---|---|
| Capability ↔ Experience, via deux types `InCycle` | Violation de la règle de cycles |
| Capability → `FamilyController` | Violation de la dépendance inverse |
| BFF → type interne `Repository` | Violation de la frontière d'API |
| BFF → type interne `Document` | Même règle, autre type de stockage |
| Contracts → `FamilyRequests` | Violation de l'indépendance des contrats |
| BFF → `ConfluxLabApplication` à la racine | Violation du sens vers l'assemblage |

Extrait partiel exact du contrôle utilisé :

```java
private static void assertRejected(ArchRule rule, Class<?> origin, Class<?> target) {
    JavaClasses classes = new ClassFileImporter().importClasses(origin, target);
    EvaluationResult result = rule.evaluate(classes);
    String report = result.getFailureReport().toString();

    assertTrue(result.hasViolation(), "The rule must reject the deliberately invalid fixture.");
    assertTrue(report.contains(origin.getName()), report);
    assertTrue(report.contains(target.getName()), report);
}
```

`evaluate(...)` retourne un résultat analysable au lieu de lancer immédiatement l'erreur de `check(...)`. Le test exige une violation **et les deux classes attendues dans le diagnostic** : cela réduit le risque de réussir à cause d'un refus sans rapport avec l'exemple.

**Un test négatif vert signifie ici que le rejet attendu a été observé**, pas que la dépendance interdite est autorisée. Nous n'avons pas ajouté de mauvais import dans `src/main`, ni laissé le projet volontairement cassé après un essai.

### 3.5. Vérifier aussi un graphe autorisé

Le septième test de détection applique les cinq mêmes règles à un graphe autorisé : BFF → API publique de Capability → DTO partagés, avec accès direct du BFF aux DTO.

L'ensemble de classes contient des sujets pour les règles concernées. Il ne suffit pas d'évaluer un jeu vide ou de désactiver les règles sans correspondance pour annoncer un résultat positif.

Ce test évite notamment qu'une règle trop large interdise tout Capability au BFF, alors que son interface publique est le point d'entrée prévu.

## 4. Commandes et résultats expliqués

### 4.1. Commit local d'INT-03, avant les modifications d'INT-04

Depuis la racine `conflux-lab`, le statut et le diff ont d'abord été contrôlés. Les chemins d'INT-03 ont été ajoutés à l'index explicitement, avec `git add -- ...` utilisé comme sélection de chemins ; la liste complète n'est pas reproduite ici comme commande à recopier.

Commandes de contrôle et de commit réellement exécutées, regroupées ici sans reproduire tous les affichages intermédiaires :

```sh
git --no-optional-locks status --short --branch
git --no-pager diff --check
git --no-pager diff --cached --check
git --no-pager diff --cached --stat
GIT_EDITOR=true git commit -m "feat(INT-03): assemble single Boot application with Java module API"
```

- `--short --branch` montre les modifications et la relation avec la branche de suivi ; `--no-optional-locks` évite les écritures de métadonnées facultatives pendant la lecture.
- `--cached` lit le contenu préparé dans l'index, qui sera commité ; `--check` détecte notamment des erreurs d'espacement et des marqueurs de conflit.
- `--no-pager` empêche un affichage interactif bloquant.
- `GIT_EDITOR=true` évite l'ouverture d'un éditeur ; `-m` fournit le message directement.
- Le commit écrit l'historique **local**, pas le dépôt GitHub.

**Résultat historique, avant la demande de clôture et de publication :** commit `cea7362`, 31 fichiers concernés, état de travail propre juste après et branche `main` en avance d'un commit sur la référence locale `origin/main`. Aucun push, nouvelle branche ou amendement n'avait été effectué à cette étape. Les modifications d'INT-04 étaient hors de ce commit ; leur commit et leur push ont ensuite été demandés explicitement.

### 4.2. Tests ciblés d'architecture

Commande réellement exécutée depuis la racine, avec JDK 21, le Wrapper et l'accès aux dépendances Maven nécessaires :

```sh
./mvnw -B -ntp -pl application -am -Dtest=ModuleArchitectureTest,ArchitectureRuleDetectionTest -Dsurefire.failIfNoSpecifiedTests=false test
```

| Option | Rôle |
|---|---|
| `-B -ntp` | Mode batch et réduction de la progression des téléchargements |
| `-pl application -am` | Sélectionne l'assemblage et construit aussi ses dépendances du réacteur |
| `-Dtest=...` | Exécute uniquement les deux nouvelles classes de tests |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Autorise les autres modules à ne pas contenir les classes sélectionnées ; ne masque pas les échecs des tests exécutés |
| `test` | Compile et exécute les tests sélectionnés, sans produire encore l'archive finale |

**Résultat constaté :** 5 tests de production et 7 tests de détection réussis, aucun échec/erreur/ignoré. Après la revue, les diagnostics négatifs ont été renforcés pour exiger source et cible ; la version finale a ensuite été validée dans le build global ci-dessous.

Ce contrôle ciblé n'exécute ni les 59 tests de contrats ni le test de contexte existant. Il ne suffit donc pas à lui seul pour clôturer la story.

### 4.3. Build global final

Commande réellement exécutée :

```sh
./mvnw -B -ntp clean verify
```

`clean` supprime les sorties `target/`, pas les sources ni l'historique Git. Le cycle recompile les projets, exécute les tests raccordés et produit les bibliothèques puis le JAR Boot. Il peut télécharger les dépendances manquantes et écrit des sorties locales, sans publication ni déploiement. Ni Docker ni MongoDB ne sont nécessaires à ces tests.

Les rapports Surefire ont été relus par un contrôle XML :

| Classe | Tests | Échecs / erreurs / ignorés |
|---|---:|---|
| `ContractValidationTest` | 43 | 0 / 0 / 0 |
| `ContractJsonTest` | 16 | 0 / 0 / 0 |
| `FamilyControllerTest` | 2 | 0 / 0 / 0 |
| `ApplicationAssemblyTest` | 3 | 0 / 0 / 0 |
| `ModuleArchitectureTest` | 5 | 0 / 0 / 0 |
| `ArchitectureRuleDetectionTest` | 7 | 0 / 0 / 0 |
| **Total** | **76** | **0 / 0 / 0** |

**Résultat : `BUILD SUCCESS`** pour le parent et les quatre modules. Les 12 nouveaux tests s'ajoutent aux 64 tests existants. Lors d'INT-04 (2026-10-21, avant INT-06), le test de contexte a été réexécuté et les deux routes valides ont été vérifiées en `503` via MockMvc.

### 4.4. Contrôler l'archive et les diagnostics

Un contrôle Python ponctuel a lu le JAR Boot avec `zipfile`, puis les trois bibliothèques du projet embarquées sous `BOOT-INF/lib/`. Il a vérifié l'absence d'ArchUnit, des classes de règles/tests et des chemins de fixtures dans les archives concernées.

Sortie utile réellement observée :

```text
Surefire total [tests, failures, errors, skipped]: [76, 0, 0, 0]
Packaged application and project libraries: no ArchUnit or architecture test fixtures
```

L'outil de diagnostics de l'éditeur a également été exécuté : **aucune erreur ni aucun avertissement signalé**. Cela reste distinct des avertissements de la JVM/Mockito affichés pendant les tests : ils n'ont pas fait échouer le build sous Java 21, sans promettre une compatibilité avec un futur JDK.

## 5. Vérifications et limites

### Ce qui est démontré dans cette session

- Les frontières définies sont vérifiées sur les classes de production, indépendamment du démarrage Spring.
- Les cinq mêmes règles détectent les six violations contrôlées, avec origine et cible attendues, et acceptent le graphe valide.
- Lors d'INT-04 (2026-10-21, avant INT-06), les 76 tests globaux ont réussi, dont le contexte et les refus `503` existants.
- Le nouvel outil et les exemples de test ne sont pas embarqués dans l'application.
- Une revue indépendante n'a pas identifié de défaut bloquant ou important ; sa suggestion sur la précision des diagnostics a été intégrée avant le build final.

### Limites explicites

- **Pas de modification de `src/main` ni de comportement métier dans INT-04 (2026-10-21, avant INT-06).** L'acceptation, le stockage, le suivi et les refus réels des stories INT-06/07/08 n'étaient pas démarrés à cette étape.
- Les règles utilisent la convention des packages du DAT. Elles ne devinent pas le rôle métier d'une classe volontairement mal placée dans `api`. Toute nouvelle organisation de packages doit être répercutée dans ces contrôles.
- L'analyse porte sur le bytecode importé sous `dev.confluxlab`, pas sur tous les usages indirects par réflexion, chaînes de caractères, configuration ou code externe. Elle ne prouve pas non plus à elle seule l'absence d'un appel HTTP déterminé à l'exécution.
- Les cycles visés sont intermodules, pas tous les cycles entre classes internes d'un même module. Les cycles de POM sont également une contrainte de construction Maven ; ces tests ne constituent pas un parseur du graphe des POM.
- Les types `Repository` et `Document` des fixtures sont artificiels : aucun repository MongoDB ni document persistant n'est créé pour tester une frontière de package.
- Aucun nouveau démarrage HTTP du JAR n'a été exécuté dans INT-04. La preuve de démarrage réel reste celle d'INT-03 ; ici, le contexte est revalidé et l'archive reconstruite est inspectée.
- Aucune CI, CD, mesure de performance, vérification Windows ou nouvelle interaction GUI Zed n'est revendiquée.
- **Fin des critères techniques ne signifie pas clôture pédagogique automatique.** L'explication par l'apprenant reste non évaluée ; aucun passage au sprint suivant ou à INT-06 n'est déduit de ce build.

## 6. Formulation d'entretien

> « Dans ConfluxLab, le monolithe modulaire a été complété avec des tests d'architecture ArchUnit exécutés par Maven. Ils interdisent les cycles, le retour de Capability vers le BFF et l'accès du BFF aux types de Capability hors de son API publique. Les contrats restent indépendants et les bibliothèques ne dépendent pas du module de démarrage. Pour vérifier que les règles fonctionnent réellement, l'agent les a exercées aussi sur des classes de test volontairement fautives et sur un graphe autorisé. Le projet conserve séparément le test de contexte Spring : démarrer ne prouve pas le respect des frontières. Ces protections sont limitées au bytecode et aux conventions de packages ; elles ne remplacent ni les tests métier ni les revues. »

**Relance :** « Pourquoi les tests négatifs passent-ils alors qu'ils contiennent des cycles ? » **Parce qu'ils attendent une violation.** Les fixtures sont importées dans un contrôle séparé ; elles sont exclues de l'analyse de la production et du JAR livré.

**Relance :** « Si une classe de stockage devient publique, le BFF peut-il désormais l'utiliser ? » **Java pourrait l'autoriser, mais la règle d'architecture le refusera si elle reste hors de l'API publique de Capability.** C'est précisément le garde-fou ajouté ; la bonne classification des packages reste nécessaire.

## 7. Suivi centralisé

- [TD-B003 — Assemblage unique, terminé dans INT-03](../todos.md#td-b003).
- [TD-B004 — Frontières et détection de violations, terminé dans INT-04](../todos.md#td-b004).
- [TD-B005 — Acceptation, suivi et refus métier, non démarrés à la clôture du sprint 01 (2026-10-21, avant INT-06)](../todos.md#td-b005).
- [TD-R001 — Modules distincts, une seule application](../todos.md#td-r001).
- [TD-R010 — Assemblage et différence entre doubles et comportement livré](../todos.md#td-r010).
- [TD-R011 — Tester les frontières et leurs détecteurs](../todos.md#td-r011).
