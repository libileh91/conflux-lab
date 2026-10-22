# INT-18 — Composer le succès

**Statut : terminée sur feu vert « commit 17 & go 18 », le 2026-10-22.** INT-17 a d'abord été commitée localement dans **`d6f0e86`**, sans push. Cet incrément ajoute `FamilyOrchestrator` et un test paramétré utilisant la fixture existante : **2 cas réussis, aucun échec, erreur ou cas ignoré**.

**[Simulation ConfluxLab]** On compose le parcours nominal en Java, sans HTTP ni MongoDB. L'orchestrateur n'est pas un bean et **n'est pas branché au listener** : le POST/GET conserve son comportement actuel, sans production d'état final. La gestion métier des échecs reste dans INT-19 ; les ressources et le futur câblage d'exécution restent à traiter dans leur périmètre.

## Parcours de lecture

- Sections **1 et 2** : comprendre la composition et les données qui relient les opérations.
- Section **3** : comprendre les preuves du test, sans recopier sa classe ni ses fixtures.
- Section **4** : commande réellement exécutée, résultat et limites.
- Tous les extraits Java sont **partiels**, pris dans les fichiers ajoutés pour INT-18. La [note INT-17](INT-17-ecrire-les-contrats-des-operations.md) conserve la preuve historique de compilation du contrat ; cette note décrit sa première utilisation dans un scénario exécuté.

### Sommaire

- [1. Composer les dépendances, pas attendre les threads](#1-composer-les-dépendances-pas-attendre-les-threads)
- [2. Lire le graphe dans le code](#2-lire-le-graphe-dans-le-code)
- [3. Un scénario, deux ordres de fin](#3-un-scénario-deux-ordres-de-fin)
- [4. Commande exécutée et limites des preuves](#4-commande-exécutée-et-limites-des-preuves)
- [5. Formulation d'entretien](#5-formulation-dentretien)
- [6. Suivi centralisé](#6-suivi-centralisé)

## 1. Composer les dépendances, pas attendre les threads

INT-17 a défini les entrées et résultats des six opérations. Il faut maintenant décider **quand leurs méthodes peuvent être appelées**, à partir des résultats disponibles.

| Étape | Résultats nécessaires | Ce qu'elle ne doit pas attendre en plus |
|---|---|---|
| A | Données initiales du principal | Aucune autre opération |
| B, C et D | Identifiant du principal retourné par A | La fin des deux autres opérations du groupe |
| E | Contrat de B et premier ayant droit de C | D ou F |
| F | Contrat de B et second ayant droit de D | C ou E |
| Fin globale | Fin effective des rattachements E et F | Aucune nouvelle opération fictive |

Une **continuation** est une fonction appelée lorsqu'une future satisfait ses conditions de terminaison. On décrit ces continuations, plutôt que d'attendre les résultats avec des appels bloquants.

| Mécanisme utilisé | Rôle dans le graphe |
|---|---|
| `thenCompose` | Enchaîner une opération qui retourne elle-même une future, en conservant une future aplatie |
| `thenCombine` | Attendre deux résultats réussis, puis appeler la fonction qui les utilise |
| `CompletableFuture.allOf` | Attendre les deux branches terminales, seulement à la fin du graphe |
| Versions sans suffixe `Async` | Ne pas soumettre automatiquement les continuations à un executor ; elles peuvent s'exécuter sur le thread de complétion ou, si le résultat est déjà prêt, lors de leur enregistrement |

**Une future n'est pas un thread.** Cet incrément rend possible le chevauchement d'opérations encore en cours ; il ne transforme pas un appel bloquant en I/O non bloquante et ne mesure pas un gain de temps.

## 2. Lire le graphe dans le code

[FamilyOrchestrator.java](../../capability-api/src/main/java/dev/confluxlab/capability/internal/FamilyOrchestrator.java) est une classe interne sans annotation Spring. Son constructeur reçoit `FamilyOperations`. `execute(requestId, request)` retourne `CompletableFuture<Void>` : une fin de parcours, pas une réponse HTTP ni un statut MongoDB.

L'entrée est supposée déjà validée, avec ses deux ayants droit, comme dans le flux de création existant. La méthode sélectionne les deux `PersonPayload` avant de construire les continuations ; elle ne redéfinit pas la validation du contrat public.

### 2.1 Après A, enregistrer les trois continuations indépendantes

**Extrait partiel de `execute(...)` :**

```java
CompletableFuture<PrincipalId> principal = operations.createPrincipal(requestId, request.principal());
CompletableFuture<ContractId> contract = principal.thenCompose(
        principalId -> operations.openContract(requestId, principalId));
CompletableFuture<DependentId> firstDependent = principal.thenCompose(
        principalId -> operations.createFirstDependent(requestId, principalId, firstPerson));
CompletableFuture<DependentId> secondDependent = principal.thenCompose(
        principalId -> operations.createSecondDependent(requestId, principalId, secondPerson));
```

A est appelé immédiatement et fournit sa future. B, C et D dépendent chacun de **la même future de A**, pas les uns des autres. Leur ordre de démarrage n'est pas imposé, et aucun ne doit attendre que B, C ou D soit terminé pour pouvoir être appelé.

Chaque continuation retourne une future d'opération ; `thenCompose` donne directement accès à son résultat futur, sans niveau imbriqué. Les identifiants typés et le même `requestId` sont transmis aux appels correspondants.

### 2.2 Pour E, joindre seulement B et C

Même fichier, **extrait partiel** :

```java
CompletableFuture<Void> firstAttachment = contract.thenCombine(firstDependent,
                (contractId, dependentId) -> operations.attachFirstDependent(requestId, contractId, dependentId))
        .thenCompose(future -> future);
```

`thenCombine` attend les résultats du contrat et du premier ayant droit, puis appelle E avec ces deux identifiants. **D n'apparaît pas dans cette condition.** F suit la même forme avec `secondDependent`, donc B et D, sans attente supplémentaire de C.

Pourquoi le `thenCompose` final ? La fonction de combinaison retourne déjà `CompletableFuture<Void>`. Sans aplatissement, on obtiendrait une future contenant une autre future : la terminaison extérieure annoncerait que le rattachement a été **lancé**, pas qu'il a **fini**. L'aplatissement suit sa fin effective.

### 2.3 Ne joindre toutes les branches qu'à la fin

**Extrait partiel du retour :**

```java
return CompletableFuture.allOf(firstAttachment, secondAttachment);
```

On n'utilise pas une barrière globale B/C/D avant de démarrer E et F. Le `allOf` final porte uniquement sur les deux rattachements ; leurs dépendances incluent déjà toutes les opérations nécessaires.

Le résultat global ne doit donc pas être terminé après un seul rattachement, ni juste après l'appel du second. Ce résultat ne déclenche encore **aucune écriture de `SUCCEEDED`**.

## 3. Un scénario, deux ordres de fin

[FamilyOrchestratorTest.java](../../capability-api/src/test/java/dev/confluxlab/capability/internal/FamilyOrchestratorTest.java) utilise [ControlledFamilyOperations](../../capability-api/src/test/java/dev/confluxlab/capability/internal/ControlledFamilyOperations.java) sans modifier cette fixture. Une nouvelle instance est créée pour chaque cas, avec trois personnes fictives distinctes et des identifiants distincts pour les deux ayants droit.

| Instrumentation du test | Rôle |
|---|---|
| `@ParameterizedTest` et `@ValueSource(booleans = {true, false})` | Exécuter la même préparation et les mêmes phases dans les deux ordres C/D |
| `@Timeout(value = 5, threadMode = SEPARATE_THREAD)` | Garde-fou JUnit sur un thread de test séparé, notamment contre une régression introduisant une attente bloquante ; pas un timeout d'opération métier |
| Futures de la fixture | Décider quand chaque opération réussit ; aucune durée arbitraire pour deviner son avancement |
| `verify` / `verifyNoMoreInteractions` | Vérifier les appels et leurs arguments, puis détecter un appel prématuré, supplémentaire ou dupliqué |
| `result.get(1, TimeUnit.SECONDS)` | Attendre la fin globale avec une borne explicite à la dernière étape |

| Valeur du paramètre | Branche terminée en premier | Preuve d'indépendance |
|---|---|---|
| `true` | C, puis E après B | E démarre alors que D est encore incomplète |
| `false` | D, puis F après B | F démarre alors que C est encore incomplète |

Deux ordres sont utiles ici : un seul montrerait l'indépendance d'un seul des rattachements. Ce n'est ni une matrice de pannes ni un test distinct par flèche.

### 3.1 Avant de terminer A, aucun descendant ne doit être appelé

**Extrait partiel du test :**

```java
CompletableFuture<Void> result = orchestrator.execute(requestId, request);
verify(controlled.operations).createPrincipal(requestId, PRINCIPAL);
verifyNoMoreInteractions(controlled.operations);
assertFalse(result.isDone());
```

Le graphe doit rendre une future alors qu'A est encore en attente. Seul A est appelé : B/C/D et les rattachements n'ont pas démarré. Un blocage sur A empêcherait le scénario de progresser.

### 3.2 Après A, B/C/D peuvent être en cours ensemble

**Extrait partiel suivant :**

```java
controlled.principal.complete(PRINCIPAL_ID);
verify(controlled.operations).openContract(requestId, PRINCIPAL_ID);
verify(controlled.operations).createFirstDependent(requestId, PRINCIPAL_ID, FIRST_PERSON);
verify(controlled.operations).createSecondDependent(requestId, PRINCIPAL_ID, SECOND_PERSON);
verifyNoMoreInteractions(controlled.operations);
```

Le test n'a encore terminé **aucune** des futures B/C/D. Pourtant, les trois méthodes ont été appelées, avec le bon UUID, l'identifiant du principal et les bonnes personnes. Cela vérifie le chevauchement possible des opérations en cours, **pas trois calculs exécutés simultanément sur trois threads**.

Les appels sont vérifiés individuellement : aucun ordre arbitraire B → C → D n'est exigé.

### 3.3 Un ayant droit prêt ne suffit pas sans B ; avec B, l'autre branche ne bloque pas

Les variables `earlyDependent` et `earlyAttachment` désignent C/E ou D/F selon le paramètre ; `lateDependent` désigne l'autre ayant droit. **Extrait partiel :**

```java
earlyDependent.complete(firstDependentFinishesFirst ? FIRST_ID : SECOND_ID);
verifyNoMoreInteractions(controlled.operations);
assertFalse(result.isDone());

controlled.contract.complete(CONTRACT_ID);
verifyAttachment(controlled, requestId, firstDependentFinishesFirst);
verifyNoMoreInteractions(controlled.operations);
assertFalse(lateDependent.isDone(), "The independent branch must still be pending.");
earlyAttachment.complete(null);
assertFalse(result.isDone(), "One completed attachment must not finish the whole graph.");
```

Le premier contrôle interdit de rattacher avant la fin de B. Après B, le helper `verifyAttachment(...)` vérifie la bonne méthode, le contrat et l'identifiant du bon ayant droit ; l'autre branche reste incomplète. Le premier rattachement peut même finir sans terminer le graphe global.

### 3.4 Attendre la fin du second rattachement, pas seulement son démarrage

**Extrait partiel final :**

```java
lateDependent.complete(firstDependentFinishesFirst ? SECOND_ID : FIRST_ID);
verifyAttachment(controlled, requestId, !firstDependentFinishesFirst);
verifyNoMoreInteractions(controlled.operations);
assertFalse(result.isDone(), "The second attachment must actually finish, not merely start.");
lateAttachment.complete(null);
assertNull(result.get(1, TimeUnit.SECONDS));
```

Le second rattachement est appelé avec ses propres identifiants. Le résultat global doit rester incomplet jusqu'à sa terminaison, puis finir normalement sans valeur (`Void`). Comme chaque rattachement occupe le rôle du second dans l'un des deux cas, ce contrôle protège l'aplatissement des **deux** branches.

Il n'y a ni `Thread.sleep` ni worker métier bloqué derrière un latch. Les futures et le mock appartiennent à chaque cas ; aucun pool ou signal partagé ne demande ici de nettoyage dans un `finally`. Le thread de garde-fou JUnit ne constitue pas un executor du traitement métier.

## 4. Commande exécutée et limites des preuves

**Départ : racine `conflux-lab/`. Prérequis : JDK 21 et dépendances Maven disponibles ou téléchargeables. Docker n'est pas nécessaire à cette sélection.**

```sh
./mvnw -B -ntp -pl capability-api -am -Dtest=FamilyOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

| Élément | Rôle et effet |
|---|---|
| `./mvnw -B -ntp` | Maven Wrapper, mode batch et absence de progression des téléchargements |
| `-pl capability-api -am` | Construire Capability et ses dépendances, dont `contracts`, pas l'assemblage Boot |
| `-Dtest=FamilyOrchestratorTest` | Sélectionner cette classe sous Surefire, soit ses deux invocations paramétrées |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Accepter que les autres modules ne contiennent pas cette classe ; cela ne prouve pas à lui seul que la sélection a été découverte |
| `test` | Compiler production et tests, puis exécuter la sélection ; ne pas atteindre le packaging ou Failsafe |

**Résultat observé le 2026-10-22 : `BUILD SUCCESS`, 2 cas réussis, 0 échec, 0 erreur, 0 ignoré.** Le rapport `capability-api/target/surefire-reports/TEST-dev.confluxlab.capability.internal.FamilyOrchestratorTest.xml` a été relu : il contient les deux invocations `[1]` et `[2]` de la méthode paramétrée. On n'ajoute pas les anciens rapports des autres classes à ce total.

La commande écrit les sorties locales `target/` ; des téléchargements manquants peuvent nécessiter le réseau. Elle ne démarre ni Spring, ni MongoDB, ni HTTP et ne publie aucun artefact. L'avertissement d'auto-attachement Mockito/Byte Buddy sous le JDK a été observé ; il n'a pas empêché les deux cas de réussir et aucun réglage n'a été ajouté pour le masquer. Aucun build n'a été relancé pour rédiger cette note.

### Limites importantes

- Le test prouve les dépendances et l'avancement avec des futures contrôlées, **pas une latence réseau ni un parallélisme de threads mesuré**.
- L'orchestrateur ne fait ni `join`, ni `get`, ni soumission à un executor. Une future ne garantit toutefois pas que son implémentation d'opération ne bloque jamais avant de la retourner ; le futur adaptateur HTTP devra traiter ce point.
- Aucune politique métier de gestion d'échec, annulation, reprise ou écriture d'état final n'est ajoutée. La propagation native des futures existe, mais n'est pas revendiquée comme validation d'INT-19.
- Aucun branchement au listener ni nouvelle configuration Spring : le flux HTTP en production ne déclenche pas encore ce graphe. Le bornage des ressources reste dans INT-20.
- Pas de suite globale, contrôle ArchUnit, test d'intégration ou redémarrage du JAR exécuté. INT-19 reste non commencée.

## 5. Formulation d'entretien

> J'ai composé le graphe à partir des contrats internes, sans attendre les résultats de façon bloquante. A fournit le principal ; B, C et D dépendent chacun de cette future et peuvent rester en cours ensemble. E combine B/C et F combine B/D, sans barrière globale avant les rattachements. Comme ces appels retournent eux-mêmes des futures, j'aplatis les résultats pour suivre leur fin réelle, puis j'attends E et F à la fin. Un seul scénario paramétré vérifie les deux ordres C/D, les données transmises et l'absence de fin prématurée. Cela prouve la composition nominale, pas un gain réseau ni un traitement déjà branché à l'API.

**« Pourquoi ne pas attendre B/C/D ensemble avant les rattachements ? »** E attendrait D inutilement et F attendrait C. Les conditions de démarrage doivent rester locales à chaque branche ; la jonction globale vient à la fin.

**« Pourquoi `thenCombine` seul ne suffit pas ici ? »** Sa fonction retourne une future de rattachement. Sans aplatissement, sa terminaison extérieure indique la création de cette future, pas la fin du rattachement.

## 6. Suivi centralisé

- [TD-B015 — Composition nominale INT-18 : terminée avec deux cas ciblés réussis](../todos.md#td-b015).
- [TD-R024 — Dépendances locales, aplatissement et fin globale](../todos.md#td-r024).
- [TD-B016 — INT-19 : composer les échecs, nouveau feu vert requis](../todos.md#td-b016).
