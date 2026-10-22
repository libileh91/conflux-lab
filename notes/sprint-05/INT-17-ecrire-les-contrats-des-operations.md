# INT-17 — Écrire les contrats des opérations

**Statut : terminée sur feu vert « go 17 », le 2026-10-22.** Point de départ : `58b84ff`, plan du sprint 05 simplifié. Deux fichiers Java sont ajoutés : un contrat interne et une doublure de test. **La compilation de production et de test a réussi ; aucun test n'a été exécuté.** La doublure sera exercée avec le graphe dans INT-18/19, sans suite dédiée au seul montage.

**[Simulation ConfluxLab]** Les six opérations et leurs identifiants sont pédagogiques, pas les contrats réels GFP/Open. Aucun HTTP partenaire, traitement métier ou état final n'est ajouté. Le feu vert autorise INT-17, pas la suivante ; il ne vaut pas évaluation de la clôture pédagogique du sprint 04.

## Parcours de lecture

- Sections **1 à 3** : rôle des contrats, données A–F et extraits utiles.
- Sections **4 et 5** : doublure pilotable, commande et limites des vérifications.
- Les extraits sont **partiels** et proviennent des deux fichiers ajoutés dans cette tâche. Imports, classe complète et préparation répétitive ne sont pas recopiés.

### Sommaire

- [1. Le problème : relier des résultats, pas des chaînes anonymes](#1-le-problème--relier-des-résultats-pas-des-chaînes-anonymes)
- [2. Les six contrats et leurs dépendances](#2-les-six-contrats-et-leurs-dépendances)
- [3. Lire les signatures utiles](#3-lire-les-signatures-utiles)
- [4. La doublure contrôlée par les futurs tests](#4-la-doublure-contrôlée-par-les-futurs-tests)
- [5. Commande exécutée, résultat et limites](#5-commande-exécutée-résultat-et-limites)
- [6. Formulation d'entretien](#6-formulation-dentretien)
- [7. Suivi centralisé](#7-suivi-centralisé)

## 1. Le problème : relier des résultats, pas des chaînes anonymes

Le graphe est connu : A précède B/C/D ; E nécessite B/C ; F nécessite B/D. Avant de composer ce parcours, il faut savoir **quelles données passent d'une étape à la suivante**.

Avec trois paramètres `String` sans distinction, il serait facile de passer un identifiant de personne à la place d'un identifiant de contrat. On définit donc trois petits types internes, sans multiplier les DTO ou inventer de nouveaux contrats HTTP.

| Terme | Sens dans cet incrément |
|---|---|
| **Contrat d'opération** | Signature Java indiquant les entrées nécessaires et le type du résultat à venir ; pas une implémentation |
| **Identifiant typé** | Valeur enveloppée dans un type qui exprime son rôle, par exemple `ContractId` |
| **Future** | Objet représentant un résultat à venir, réussi ou en échec ; ce n'est pas un thread |
| **Doublure / fixture** | Montage réservé aux tests, remplaçant les opérations réelles et laissant le scénario décider quand elles se terminent |

### Où se trouvent les ajouts ?

| Fichier | Responsabilité |
|---|---|
| [FamilyOperations.java](../../capability-api/src/main/java/dev/confluxlab/capability/internal/FamilyOperations.java) | Interface interne des six opérations et trois types d'identifiants |
| [ControlledFamilyOperations.java](../../capability-api/src/test/java/dev/confluxlab/capability/internal/ControlledFamilyOperations.java) | Mock Mockito et six futures encore incomplètes, uniquement sous `src/test` |

L'interface reste dans `capability.internal`, sans `public` sur le type extérieur ni annotation Spring. Elle n'est pas l'API Java publique appelée par Experience. Aucun bean n'est créé et le listener n'en dépend pas encore.

## 2. Les six contrats et leurs dépendances

Chaque méthode reçoit le **`requestId` UUID** de la demande. Le tableau ne le répète pas dans chaque cellule ; il reste distinct des identifiants des ressources créées.

| Étape / méthode | Autres entrées | Résultat de la future | Dépendances exprimées par les données |
|---|---|---|---|
| **A** — `createPrincipal` | `PersonPayload principal` | `PrincipalId` | Les données initiales de la personne principale |
| **B** — `openContract` | `PrincipalId principalId` | `ContractId` | Le principal créé par A |
| **C** — `createFirstDependent` | `PrincipalId principalId`, `PersonPayload dependent` | `DependentId` | A et les données du premier ayant droit |
| **D** — `createSecondDependent` | Même nature d'entrées pour le second ayant droit | `DependentId` | A et les données du second ayant droit |
| **E** — `attachFirstDependent` | `ContractId contractId`, `DependentId dependentId` | `Void` | Le contrat de B et la personne de C |
| **F** — `attachSecondDependent` | Même nature d'entrées pour le second rattachement | `Void` | Le contrat de B et la personne de D |

`PersonPayload` est réutilisé depuis `contracts` : ses nom, prénom et date de naissance existent déjà. On ne modifie ni ce type ni `CreateFamilyRequest`, les réponses POST/GET ou le document MongoDB.

Pour E/F, une réussite n'a pas besoin de retourner un nouvel identifiant utilisé par la suite : `CompletableFuture<Void>` suffit. **Cela ne signifie ni que l'opération est déjà terminée ni qu'elle ne peut pas échouer.** On évite de créer un `AttachmentId` sans consommateur.

Le futur orchestrateur devra associer le bon ayant droit à C ou D. Les deux utilisent `DependentId` parce qu'il s'agit de la même nature de ressource ; ce type seul n'empêche pas d'inverser deux personnes ou deux demandes.

## 3. Lire les signatures utiles

### 3.1 Distinguer les identifiants

Dans `FamilyOperations.java`, **extrait partiel** :

```java
record PrincipalId(String value) {}

record ContractId(String value) {}

record DependentId(String value) {}
```

Ces records distinguent les rôles au niveau du compilateur : un `PrincipalId` ne peut pas être passé à la place d'un `ContractId`. Ils enveloppent des chaînes fictives, sans imposer un format de retour partenaire non encore défini.

Ce sont des valeurs simples : **aucune validation de contenu ou d'appartenance à une famille n'est ajoutée par ces records**. Ils ne constituent pas une nouvelle règle d'idempotence. Le `requestId`, lui, reste un identifiant de corrélation.

### 3.2 Consommer le résultat de A pour préparer B

Même fichier, **extrait partiel** :

```java
CompletableFuture<PrincipalId> createPrincipal(UUID requestId, PersonPayload principal);

CompletableFuture<ContractId> openContract(UUID requestId, PrincipalId principalId);
```

A reçoit la personne et fournit son identifiant ; B demande cet identifiant et fournit celui du contrat. L'interface exprime ce passage de données, mais **ne lance pas B après A à elle seule** : la composition des futures appartient à INT-18.

Le retour d'une `CompletableFuture` ne rend pas automatiquement un appel Feign non bloquant. Il n'y a d'ailleurs aucune implémentation HTTP dans cet incrément, aucun executor créé et aucun recours au `commonPool`.

### 3.3 Rattacher après réception des deux résultats nécessaires

Même fichier, **extrait partiel** :

```java
CompletableFuture<Void> attachFirstDependent(UUID requestId, ContractId contractId, DependentId dependentId);
```

E exige l'identifiant du contrat et celui du premier ayant droit : les résultats de B et C. Il ne reçoit pas le résultat de D. F a une méthode distincte avec la même forme d'entrées pour utiliser B et D.

L'absence d'une donnée de D dans E aide à lire le graphe ; elle ne suffit pas à prouver l'absence d'une barrière globale dans du code de composition qui n'existe pas encore.

## 4. La doublure contrôlée par les futurs tests

### 4.1 Des résultats créés, mais pas déjà terminés

Dans `ControlledFamilyOperations.java`, **extrait partiel** :

```java
final FamilyOperations operations = mock(FamilyOperations.class);
final CompletableFuture<PrincipalId> principal = new CompletableFuture<>();
final CompletableFuture<ContractId> contract = new CompletableFuture<>();
```

Le mock remplace les opérations ; les futures sont créées **sans résultat initial**. Elles ne lancent ni thread, ni HTTP, ni temporisation. Les quatre autres futures suivent la même logique.

| Future de la fixture | Opération pilotée |
|---|---|
| `principal` | A |
| `contract` | B |
| `firstDependent` / `secondDependent` | C / D |
| `firstAttachment` / `secondAttachment` | E / F |

### 4.2 Renvoyer la future que le scénario peut contrôler

Même fichier, **extrait partiel du constructeur de la fixture** :

```java
when(operations.createPrincipal(any(UUID.class), any(PersonPayload.class))).thenReturn(principal);
when(operations.openContract(any(UUID.class), any(PrincipalId.class))).thenReturn(contract);
```

Mockito prépare le retour pour les appels correspondants. **Ce montage n'appelle pas les opérations réelles** et ne vérifie pas l'ordre A–F. Dans INT-18, les vérifications Mockito pourront observer les méthodes effectivement appelées et leurs arguments.

Le scénario pourra terminer une future par `complete(...)`, ou lui donner une issue d'échec par `completeExceptionally(...)`. Pour E/F, `complete(null)` signifie une fin normale sans valeur ; c'est différent de retourner une future `null`. Ces usages sont décrits pour les prochains tests, **pas annoncés comme exécutés dans INT-17**.

La fixture n'achève pas automatiquement toutes les opérations : on pourra garder D en attente pour observer E, par exemple. Une instance porte les six résultats d'**un parcours piloté** ; utiliser cette même instance pour plusieurs demandes partagerait ces futures. Il faudra un montage adapté si l'on vérifie plusieurs demandes en INT-20.

## 5. Commande exécutée, résultat et limites

**Départ : racine `conflux-lab/`. Prérequis : JDK 21 et dépendances Maven disponibles ou téléchargeables ; Docker non nécessaire pour cette commande.**

```sh
./mvnw -B -ntp -pl capability-api -am test-compile
```

| Élément | Rôle et effet |
|---|---|
| `./mvnw` | Utiliser le Maven Wrapper du dépôt |
| `-B -ntp` | Mode batch et absence de progression des téléchargements |
| `-pl capability-api -am` | Sélectionner Capability et construire ses dépendances dans le réacteur, dont `contracts` |
| `test-compile` | Atteindre la compilation de production puis celle des sources de test, **sans atteindre la phase `test`** |

**Résultat observé le 2026-10-22 : `BUILD SUCCESS`.** Les trois modules sélectionnés — racine, `contracts`, `capability-api` — sont en succès. Le compilateur utilise `release 21` et compile notamment le contrat et la fixture Mockito.

Cette commande écrit les sorties locales `target/` ; des dépendances absentes du cache peuvent nécessiter le réseau. Elle ne package pas le JAR Boot, ne publie rien et ne démarre aucun contexte Spring ou conteneur MongoDB. Aucun POM ni réglage applicatif n'a changé.

### Ce qui est vérifié, et ce qui ne l'est pas

| Vérification | Preuve réelle |
|---|---|
| Signatures et types compatibles | Compilation Java de production réussie |
| Fixture compatible avec les six méthodes | Compilation des sources de test réussie |
| Passage des données A–F | Revue des signatures et tableau ci-dessus ; composition pas encore implémentée |
| Succès/échec pilotés par la fixture | Futures et stubs préparés ; **pas de scénario exécuté dans cet incrément** |
| Ordre, concurrence, propagation des erreurs | Non vérifiés dynamiquement : INT-18/19 restent à autoriser |

**Aucun cas de test ajouté ou exécuté**, pas même les tests existants ; ne pas annoncer des « tests verts » à partir de ce `test-compile`. Aucun build global, contrôle ArchUnit, redémarrage du JAR ou preuve de charge n'est revendiqué. Le listener s'arrête toujours à sa tentative de prise en charge `PROCESSING`, sans état final ni faux traitement métier.

## 6. Formulation d'entretien

> Dans le labo, j'ai commencé par un contrat interne pour les six opérations. Chaque méthode indique les données nécessaires et retourne une future typée. Un identifiant de contrat n'est pas interchangeable avec celui du principal, mais ces types ne prouvent pas à eux seuls que le graphe est correctement composé. Les rattachements demandent le contrat et le bon ayant droit, sans dépendance supplémentaire à l'autre branche. Une fixture Mockito expose des futures encore incomplètes pour que les prochains tests choisissent quand chaque étape réussit ou échoue. À ce stade, j'ai vérifié la compilation du contrat et de la fixture, pas encore l'exécution de l'orchestration.

**« Retourner une future suffit pour exécuter sur un autre thread ? »** Non. Le type représente un résultat à venir ; c'est l'implémentation et son mécanisme d'exécution qui déterminent où le travail se fait.

**« Pourquoi ne pas mettre ces contrats dans le module `contracts` ? »** Ils concernent les opérations internes de Capability. `contracts` reste la bibliothèque des types d'échange publics existants ; Experience n'a pas à connaître ces futures ou identifiants internes.

## 7. Suivi centralisé

- [TD-R023 — Contrats internes, identifiants typés et futures pilotables](../todos.md#td-r023).
- [TD-B015 — INT-18 : composer et vérifier le parcours réussi, feu vert requis](../todos.md#td-b015).
