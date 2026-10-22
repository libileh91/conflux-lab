# INT-19 — Composer les échecs

**Statut : terminée pour la composition des échecs sur feu vert « commit 18 & passe à 19 », le 2026-10-22.** INT-18 a d'abord été commitée localement dans **`516ea0e`**, sans push. Cet incrément explicite la jonction finale et ajoute **un seul scénario d'échec** dans le test existant. La sélection repasse les deux cas nominaux : **3 cas réussis, aucun échec, erreur ou cas ignoré**.

**[Simulation ConfluxLab]** L'échec est injecté dans la future de B, sans appel partenaire réel. La finalisation vérifiée est celle de **la future globale du graphe**, pas une écriture de `FAILED` dans MongoDB. Le listener reste inchangé et non raccordé à l'orchestrateur ; aucune nouvelle réponse HTTP ou reprise automatique n'est livrée.

## Parcours de lecture

- Sections **1 et 2** : comprendre la règle d'échec et la différence entre dépendances de démarrage et attente finale.
- Section **3** : lire uniquement les parties du test qui portent la preuve.
- Sections **4 et 5** : distinguer résultat du graphe, persistance et vérifications réellement exécutées.
- Tous les extraits Java sont **partiels**, issus des fichiers inspectés pour cette tâche. Les classes, imports et préparations communes ne sont pas recopiés.

### Sommaire

- [1. Le problème : échouer sans abandonner les branches lancées](#1-le-problème--échouer-sans-abandonner-les-branches-lancées)
- [2. La règle dans le code](#2-la-règle-dans-le-code)
- [3. Un échec représentatif, piloté sans pause](#3-un-échec-représentatif-piloté-sans-pause)
- [4. Future en échec ne signifie pas statut persisté](#4-future-en-échec-ne-signifie-pas-statut-persisté)
- [5. Commande exécutée et limites](#5-commande-exécutée-et-limites)
- [6. Formulation d'entretien](#6-formulation-dentretien)
- [7. Suivi centralisé](#7-suivi-centralisé)

## 1. Le problème : échouer sans abandonner les branches lancées

Après A, B/C/D peuvent être en cours. Si B échoue, E et F ne disposent pas du contrat nécessaire et ne doivent pas être appelés. Mais C et D ont déjà démarré : leur disparition du résultat métier ne signifie pas qu'elles se sont arrêtées.

La règle retenue est simple : **ne pas lancer les descendants privés d'un prérequis réussi, laisser les opérations déjà lancées se terminer, puis terminer le graphe en échec**. On n'ajoute ni annulation forcée, ni compensation, ni faux résultat dégradé présenté comme une réussite.

| Moment du scénario | B | C | D | Résultat attendu |
|---|---|---|---|---|
| Après A | En cours | En cours | En cours | Graphe encore incomplet |
| B échoue | En échec | En cours | En cours | Pas d'E/F ; attendre les branches lancées |
| C finit | En échec | Réussie | En cours | Graphe encore incomplet |
| D finit | En échec | Réussie | Réussie | Graphe en échec, avec la cause de B |

Ce cas suffit au risque retenu : B est un prérequis commun aux deux rattachements. Il ne constitue pas une validation exhaustive des erreurs possibles sur A–F.

## 2. La règle dans le code

[FamilyOrchestrator.java](../../capability-api/src/main/java/dev/confluxlab/capability/internal/FamilyOrchestrator.java) conserve la composition d'INT-18. Seuls un commentaire d'intention et les arguments du `allOf` final changent ; aucun gestionnaire d'erreur, executor ou nouvel objet de résultat n'est ajouté.

### 2.1 Les descendants ne démarrent que sur des prérequis réussis

**Extrait partiel existant, pour E :**

```java
CompletableFuture<Void> firstAttachment = contract.thenCombine(firstDependent,
                (contractId, dependentId) -> operations.attachFirstDependent(requestId, contractId, dependentId))
        .thenCompose(future -> future);
```

La fonction de combinaison n'est appelée que si B **et** C réussissent. Si B échoue, le code `attachFirstDependent(...)` n'est pas exécuté ; le chemin composé porte l'échec. F suit le même mécanisme avec B/D.

On conserve `thenCompose` pour suivre la fin du rattachement lorsqu'il a réellement été lancé. On ne remplace pas l'échec par une valeur normale : pour `Void`, un résultat `null` peut être une réussite sans valeur, **pas un code d'erreur**.

### 2.2 Attendre explicitement les branches à la fin

**Extrait partiel du retour actuel :**

```java
return CompletableFuture.allOf(contract, firstDependent, secondDependent, firstAttachment, secondAttachment);
```

| Future attendue | Pourquoi elle est présente |
|---|---|
| `contract` | Conserver la terminaison de B, réussie ou en échec |
| `firstDependent` / `secondDependent` | Attendre explicitement C/D, même si les rattachements ne sont pas appelés |
| `firstAttachment` / `secondAttachment` | Suivre les chemins E/F : leur fin réelle s'ils démarrent, ou leur terminaison exceptionnelle si un prérequis échoue |

Ce `allOf` **ne précède pas** les appels E/F : les continuations ont déjà été enregistrées. C'est une jonction de **fin**, pas une barrière B/C/D ajoutée avant le démarrage des rattachements. Les deux cas nominaux d'INT-18 restent donc pertinents et sont rejoués.

L'attente de toutes ces futures ne transforme pas l'échec en réussite : `allOf` ne termine normalement que si toutes réussissent. En présence de B en échec, la future globale reste exceptionnelle après l'attente des autres branches.

Ce changement rend la règle finale explicite ; il ne signifie pas qu'une panne avait été reproduite dans INT-18. La composition native savait déjà propager des échecs. Il n'y a ni boucle d'attente, ni `join`, ni `get` bloquant dans l'orchestrateur.

## 3. Un échec représentatif, piloté sans pause

[FamilyOrchestratorTest.java](../../capability-api/src/test/java/dev/confluxlab/capability/internal/FamilyOrchestratorTest.java) garde les deux cas nominaux et ajoute `waitsForStartedBranchesAndPropagatesContractFailureWithoutStartingAttachments`.

Le montage réutilise `ControlledFamilyOperations` sans la modifier. Après l'appel d'`execute`, le test termine A et vérifie les quatre appels A/B/C/D avec le bon UUID et les bonnes données. Ces appels sont marqués comme vérifiés avant les contrôles suivants.

### 3.1 Faire échouer B, sans annuler C/D ni finaliser trop tôt

**Extrait partiel du nouveau scénario :**

```java
IllegalStateException failure = new IllegalStateException("Simulated contract creation failure.");
controlled.contract.completeExceptionally(failure);
assertFalse(controlled.firstDependent.isDone(), "The first started branch must not be cancelled.");
assertFalse(controlled.secondDependent.isDone(), "The second started branch must not be cancelled.");
assertFalse(result.isDone(), "The failed graph must still await its started branches.");
verifyNoMoreInteractions(controlled.operations);
```

`completeExceptionally` termine la future de B avec l'exception simulée : ce n'est ni une coupure réseau ni une exception issue d'un vrai partenaire. C/D restent incomplètes ; une annulation les aurait aussi terminées et serait détectée ici.

Le résultat global reste incomplet. Comme A/B/C/D sont les seuls appels déjà vérifiés, `verifyNoMoreInteractions` détecterait notamment un démarrage d'E/F malgré l'échec de B.

### 3.2 Terminer C ne suffit pas tant que D reste en cours

**Extrait partiel suivant :**

```java
controlled.firstDependent.complete(FIRST_ID);
assertFalse(result.isDone(), "The second dependent is still in flight.");
verifyNoMoreInteractions(controlled.operations);
```

C réussit, mais D reste en cours : il est encore trop tôt pour annoncer la fin globale. E ne démarre pas non plus lorsque C devient prête, puisque B a échoué.

### 3.3 Après D, observer l'échec et sa cause originale

**Extrait partiel final :**

```java
controlled.secondDependent.complete(SECOND_ID);
ExecutionException propagated = assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
assertSame(failure, propagated.getCause());
verifyNoMoreInteractions(controlled.operations);
```

Le test termine D, puis attend avec une borne d'une seconde. `get(...)` expose l'échec de la future à travers `ExecutionException` ; `assertSame` vérifie que la cause est bien l'objet `failure` injecté dans B. On ne se contente pas de constater une exception quelconque.

Le dernier contrôle exige toujours l'absence d'appel des rattachements. Les futures **de la fixture** pour E/F peuvent rester incomplètes : leurs méthodes n'ont pas été appelées. Il ne faut pas les confondre avec les futures **composées par l'orchestrateur**, qui terminent en échec à cause de leurs prérequis et permettent à la jonction de se terminer.

Le scénario a le même garde-fou JUnit que les cas nominaux : `@Timeout` de 5 s sur un thread de test séparé. Il n'utilise ni `Thread.sleep`, ni pool métier, ni worker bloqué derrière un signal à libérer. Ces bornes protègent le test ; elles n'ajoutent pas de timeout de traitement à l'application.

## 4. Future en échec ne signifie pas statut persisté

| Élément | Réalisation de cet incrément |
|---|---|
| Échec du graphe après la fin des branches lancées | Implémenté et vérifié sur le scénario B/C/D |
| Interdiction d'appeler E/F après l'échec de B | Vérifiée par les interactions Mockito |
| Conservation de la cause de B | Vérifiée à la lecture de la future globale |
| Écriture de `FAILED` ou `SUCCEEDED` dans MongoDB | **Non ajoutée, non testée ici** |
| Branchement au listener et changement du suivi HTTP | **Non réalisés** |

La future globale fournit le **point à observer avant une future écriture d'état final**. Le raccordement devra utiliser sa terminaison, pas l'erreur isolée de B pendant que d'autres branches avancent encore.

On ne revendique donc pas la preuve d'une protection contre des écritures MongoDB tardives dans un parcours raccordé. Dans le périmètre actuel, aucune branche du graphe n'écrit de statut. Les transitions atomiques déjà développées restent distinctes de cette preuve Java ; un raccordement devra préserver leurs garanties.

Même une opération connue en échec ne permet pas d'annuler magiquement des créations partenaires déjà réalisées. Aucun rollback distribué, retry, fallback de réussite ou reprise automatique n'est ajouté.

## 5. Commande exécutée et limites

**Départ : racine `conflux-lab/`. Prérequis : JDK 21 et dépendances Maven disponibles ou téléchargeables. Docker n'est pas nécessaire à cette sélection.**

```sh
./mvnw -B -ntp -pl capability-api -am -Dtest=FamilyOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

| Élément | Rôle et effet |
|---|---|
| `./mvnw -B -ntp` | Maven Wrapper, mode batch, sans progression des téléchargements |
| `-pl capability-api -am` | Sélectionner Capability et ses dépendances, dont `contracts`, pas l'assemblage Boot |
| `-Dtest=FamilyOrchestratorTest` | Exécuter les trois cas de cette classe sous Surefire |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Accepter les modules qui ne contiennent pas la classe sélectionnée |
| `test` | Compiler si nécessaire puis exécuter la sélection, sans packaging Boot ni Failsafe |

**Résultat du 2026-10-22 : `BUILD SUCCESS`, 3 cas réussis, 0 échec, 0 erreur, 0 ignoré.** Le rapport `capability-api/target/surefire-reports/TEST-dev.confluxlab.capability.internal.FamilyOrchestratorTest.xml` a été relu : il contient les deux invocations paramétrées du succès et le nouveau scénario d'échec.

Il s'agit de **2 cas réutilisés et 1 cas ajouté**, pas de cinq scénarios distincts en additionnant les résultats INT-18 et INT-19. Le rapport courant remplace le précédent ; la [note INT-18](INT-18-composer-le-succes.md) conserve son résultat historique au commit `516ea0e`.

La commande écrit sous `target/` et peut télécharger des dépendances absentes du cache. Elle ne publie rien et ne démarre ni Spring, MongoDB ou HTTP. L'avertissement habituel d'auto-attachement Mockito/Byte Buddy a été observé sans empêcher la réussite ; aucun réglage n'est ajouté pour le masquer. Aucun build n'est relancé pour rédiger cette note.

**Limites :** un échec de future de B est testé, sans matrice A–F ; aucune validation globale des adaptateurs futurs n'est revendiquée. Pas de suite complète, contrôle ArchUnit, test d'intégration, état final persisté, redémarrage du JAR ou mesure de performance. Le bornage des ressources INT-20 attend son propre feu vert.

## 6. Formulation d'entretien

> Dans le labo, si B échoue, les rattachements E et F ne doivent pas être appelés, mais C et D peuvent déjà être en cours. J'ai rendu la règle explicite dans la jonction finale : attendre les branches lancées et les chemins de rattachement, puis exposer l'échec sans le convertir en succès. Le test fait échouer B, garde C/D incomplètes, termine C puis D et vérifie que la future globale ne finit qu'à ce moment, avec la cause originale. Cette preuve reste une composition Java : je n'annonce pas un statut FAILED persisté ni un rollback partenaire. Le raccordement au suivi devra observer la terminaison globale pour finaliser sans précipitation.

**« Pourquoi ne pas écrire `FAILED` dès que B échoue ? »** D'autres branches déjà lancées peuvent encore avancer. La règle retenue attend leur fin ; ici on vérifie cette frontière, pas encore l'écriture MongoDB.

**« Attendre toutes les branches veut dire lancer tous les descendants malgré l'erreur ? »** Non. Les fonctions de rattachement ne sont pas appelées si leurs prérequis échouent. Leurs chemins composés terminent exceptionnellement, ce qui n'est pas une exécution métier du rattachement.

## 7. Suivi centralisé

- [TD-B016 — Composition des échecs INT-19 : réalisée, sans état final persisté](../todos.md#td-b016).
- [TD-R025 — Attendre les branches lancées et préserver l'échec](../todos.md#td-r025).
- [TD-B017 — INT-20 : borner les ressources, nouveau feu vert requis](../todos.md#td-b017).
