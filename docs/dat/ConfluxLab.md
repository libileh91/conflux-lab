# DAT — ConfluxLab

**Type :** pseudo-projet technique et pédagogique Java/Spring.  
**Objectif :** comprendre, illustrer et défendre à l'oral une architecture inspirée du parcours AXA.  
**État :** progression organisée dans `docs/stages/` à la demande de l'utilisateur ; travail par petits incréments, sans implémentation globale en une seule fois. Le sprint 01 est clôturé techniquement et documenté sur demande explicite de l'utilisateur le 2026-10-21 ; sa restitution orale n'a pas été évaluée. Cette clôture n'autorise aucune story suivante.

**Dans le périmètre :** backend, tests automatisés et **CI opérationnelle lors de la phase de code**.  
**Hors périmètre pratique :** CD, déploiement, cloud, serveur distant, infrastructure de production.

## 1. Principes de conception

- **Fidélité aux principes rapportés** : préserver la séparation Experience / Capability, MongoDB tampon, UUID, Spring Event, asynchronisme, Feign et Resilience4j.
- **Simplification métier** : un seul cas d'usage, la création d'une famille fictive.
- **Modernisation pédagogique** : Java 21 / Spring Boot 3.x, sans prétendre qu'il s'agissait des versions réellement utilisées chez AXA.
- **Traçabilité** : distinguer explicitement **[AXA rapporté]**, **[Simulation ConfluxLab]**, **[Proposition]** et **[Non confirmé]**.
- **Document-first** : théorie et DAT d'abord ; code uniquement lorsque le périmètre/contrat sont validés.
- **Minimum suffisant** : ne pas introduire de nouvelles briques qui n'illustrent pas une compétence ciblée.

## 2. Cas d'usage

Une requête HTTP demande la création d'une famille fictive de **trois personnes** : un assuré principal et deux ayants droit.

**Lecture du flux :** INT-21 + INT-22 raccordent le parcours complet sur feu vert « ok go » : acceptation persistée, événement local, prise en charge atomique gagnante, graphe A–F, Feign vers WireMock et finalisation conditionnelle. La composition Java INT-17 à INT-20 est réutilisée sans changement. La politique détaillée des erreurs et répétitions reste le lot INT-23 + INT-24, non autorisé ; aucun retry ou Resilience4j n'est ajouté à ce stade.

1. Le module Experience/BFF reçoit la demande HTTP, valide son format et appelle l'interface Java publique du module Capability, dans la même application Spring Boot.
2. Depuis INT-10, Capability génère un UUID et insère la demande `PENDING` dans MongoDB avant l'acceptation. À l'étape historique INT-06, ce stockage était temporairement réalisé en mémoire.
3. Capability publie un **événement Spring local** contenant l'UUID ; un listener `@Async` est planifié.
4. Capability renvoie l'acceptation et Experience retourne **HTTP `202 Accepted`** avec un `requestId` et une `statusUrl`, sans attendre les appels partenaires.
5. Le listener lit la demande et tente la transition atomique vers `PROCESSING` ; seul le résultat gagnant déclenche l'orchestrateur. Il observe sa future sans attente bloquante dans le pool.
6. Les six appels passent par le client Spring Cloud OpenFeign vers le partenaire fictif, sans retry Feign ou HC5 ni redirection automatique.
7. Après la terminaison globale du graphe, une transition conditionnelle tente `SUCCEEDED` ou `FAILED`. Une écriture non confirmée est journalisée : l'état final n'est pas garanti si MongoDB échoue, et le document peut rester `PROCESSING`.
8. Le client consulte **GET `/api/v1/requests/{requestId}`**.

**Nuance importante :** le traitement `@Async` peut commencer **avant** la fin de la réponse HTTP `202` ; le schéma décrit les responsabilités, pas une garantie de chronologie de ces deux opérations.

## 3. Orchestration métier simplifiée

Six opérations fictives, qui ne décrivent **pas** les endpoints réels GFP/Open :

- **A** — Créer l'assuré principal.
- **B** — Ouvrir le contrat, après A.
- **C** — Créer l'ayant droit 1, après A.
- **D** — Créer l'ayant droit 2, après A.
- **E** — Rattacher l'ayant droit 1 au contrat, après B **et** C.
- **F** — Rattacher l'ayant droit 2 au contrat, après B **et** D.

```mermaid
flowchart TD
  A[Créer principal] --> B[Ouvrir contrat]
  A --> C[Créer ayant droit 1]
  A --> D[Créer ayant droit 2]
  B --> E[Rattacher ayant droit 1]
  C --> E
  B --> F[Rattacher ayant droit 2]
  D --> F
  E --> G[Finaliser]
  F --> G
```

B, C et D peuvent être lancées en parallèle ; E et F n'attendent que leurs prérequis respectifs. Il ne s'agit pas de lancer aveuglément les six appels simultanément.

### 3.1 Contrats internes des opérations — INT-17

**[Simulation ConfluxLab — réalisée sur feu vert « go 17 »]** : `FamilyOperations`, interface sans bean à l'étape INT-17, désormais dans `capability.internal.orchestration`, définit les six opérations avec `CompletableFuture`. Chaque appel reçoit le `requestId` de corrélation ; A consomme `PersonPayload` et retourne `PrincipalId`, B utilise ce dernier et retourne `ContractId`, C/D utilisent le principal et les données de l'ayant droit pour retourner chacun un `DependentId`, E/F utilisent contrat et ayant droit et retournent `Void` (réussite sans nouvelle donnée). Les trois records d'identifiants distinguent les rôles, sans ajouter de validation de contenu ou d'appartenance à une famille.

`ControlledFamilyOperations`, sous `src/test` uniquement, prépare un mock Mockito et six futures incomplètes. `test-compile` ciblé de Capability et de ses dépendances réussit ; aucun test n'est exécuté et la fixture sera exercée avec INT-18/19. Aucun contrat HTTP, POM, configuration, listener ou stockage n'est modifié. L'interface ne compose pas le graphe et n'implique aucun thread ou appel HTTP automatique. Voir le [cours INT-17](../../notes/sprint-05/INT-17-orchestrer-les-six-operations.md).

### 3.2 Composition nominale — INT-18

**[Simulation ConfluxLab — réalisée sur feu vert « commit 17 & go 18 »]** : INT-17 est commitée localement dans `d6f0e86`, sans push. `FamilyOrchestrator`, classe Java interne sans bean Spring, dépend de `FamilyOperations` et retourne une `CompletableFuture<Void>`. A fournit le principal ; trois `thenCompose` enregistrent B/C/D indépendamment. E combine B/C et F combine B/D ; leur future de rattachement est aplatie avec `thenCompose`. Le seul `allOf` attend E/F à la fin, sans barrière globale avant leur démarrage.

**Preuves :** un test paramétré réutilise la fixture INT-17 dans les deux ordres C/D ; 2 cas Surefire ciblés réussissent, aucun échec/erreur/ignoré. Il vérifie l'ordre, les UUID/données transmis, le chevauchement possible des opérations encore en cours, l'indépendance d'E/F et l'absence de fin globale prématurée. Pas de mesure de parallélisme de threads ou de performance réseau. Aucun `join`, `get` ou executor dans l'orchestrateur, aucun HTTP, accès MongoDB, état final ou branchement au listener. La gestion métier des échecs et le bornage restent dans INT-19/20. Voir le [cours INT-18](../../notes/sprint-05/INT-17-orchestrer-les-six-operations.md).

### 3.3 Composition des échecs — INT-19

**[Simulation ConfluxLab — réalisée sur feu vert « commit 18 & passe à 19 »]** : INT-18 est commitée localement dans `516ea0e`, sans push. La jonction finale de `FamilyOrchestrator` attend explicitement B/C/D et les chemins E/F, sans modifier leurs conditions de démarrage. Si B échoue, les fonctions de rattachement ne sont pas appelées, les branches C/D déjà lancées ne sont pas annulées et la future globale ne termine en échec qu'après leur fin. Aucun fallback de réussite ou nouvel état n'est ajouté.

**Preuves :** un seul cas d'échec B ajouté dans `FamilyOrchestratorTest`, avec C/D maintenues en cours puis terminées séparément, absence d'appel E/F et cause originale vérifiées. Le passage ciblé compte 3 cas Surefire réussis (2 nominaux réutilisés + 1 nouveau), aucun échec/erreur/ignoré. La future globale fournit la frontière de finalisation ; aucune écriture de `FAILED`, vérification d'écritures MongoDB tardives ou modification du suivi HTTP n'est réalisée. Le raccordement au listener reste absent. Le bornage est ensuite traité dans INT-20, comme décrit ci-dessous. Voir le [cours INT-19](../../notes/sprint-05/INT-17-orchestrer-les-six-operations.md).

### 3.4 Soumission et ressources — INT-20

**[Simulation ConfluxLab — réalisée sur feu vert « commit 19 & go 20 »]** : INT-19 est commitée localement dans `eb0dc5a`, sans push. `FamilyOrchestrator` reçoit un `Executor` explicite ; chaque invocation A–F passe par `supplyAsync(..., executor)`, puis sa future est aplatie avec `thenCompose`. Un rejet, y compris initial, devient une future exceptionnelle ; la jonction finale continue d'attendre les branches lancées. Aucun `commonPool` implicite, `join/get`, nouvel executor, réglage ou bean d'orchestrateur ajouté.

**Choix de raccordement retenu dans INT-20 :** réutiliser `familyRequestExecutor` d'INT-15 et observer la fin du graphe sans bloquer le listener. Ce test Java ne prouvait pas encore le câblage Spring, réalisé ensuite dans INT-21 + INT-22. Le pool borne ses workers et sa file, pas les futures qu'un adaptateur ferait travailler ailleurs ; une admission d'A ne réserve pas la capacité pour tout le graphe.

**Preuves :** 6 cas Surefire ciblés réussis, 0 échec/erreur/ignoré : 3 cas existants, 2 cas de rejet A/E et un contrôle de partage/isolation. Ce dernier réutilise la configuration du pool à un worker, démarre les compositions depuis ce même pool et distingue deux UUID avec un succès et un échec. Saturation INT-15 réutilisée historiquement, sans nouvel IT ou test HTTP. Aucun état final persisté ni appel partenaire. Voir le [cours INT-20](../../notes/sprint-05/INT-17-orchestrer-les-six-operations.md).

### 3.5 Parcours partenaire et finalisation — INT-21 + INT-22

**[Simulation ConfluxLab — réalisée sur feu vert « ok go », le 2026-10-22]** : `FamilyPartnerClient` expose quatre routes de ressources pour les six opérations : création du principal, du contrat, des ayants droit, puis rattachements. Chaque appel transmet `X-Request-Id`. Les DTO du partenaire restent internes et `FeignFamilyOperations` adapte les réponses aux identifiants typés existants. Les identifiants absents/vides sont refusés ; aucune nouvelle donnée partenaire n'est exposée dans le suivi public.

Le bean `FamilyOrchestrator` reçoit le pool existant explicitement qualifié. Après un claim gagnant, le listener lui fournit le payload stocké et observe sa fin par `whenComplete`, sans `join/get`. Le callback finalise avec le store atomique existant ; il capture et journalise lui-même les erreurs d'écriture, sans message, payload ou trace brute, car ces erreurs de callback ne passent pas automatiquement dans le handler `@Async void`. Aucun rollback, état de secours artificiel ou reprise n'est ajouté.

Le BOM Spring Cloud 2025.0.0 accompagne Boot 3.5.7, OpenFeign 4.3.0 et Feign 13.6. Le client est configuré avec connexion 1000 ms, lecture 3000 ms et redirections désactivées ; `Retryer.NEVER_RETRY` et HC5 `disableAutomaticRetries()` neutralisent les deux niveaux de répétition. L'URL utilise `FAMILY_PARTNER_URL`, sinon `http://localhost:9090`. Ces délais ne sont pas une deadline globale. WireMock standalone 3.13.1 est une dépendance de test ; le JAR ne lance pas lui-même de simulateur.

**Preuves :** `verify` ciblé réussi avec **14 cas Surefire + 8 Failsafe**, aucun échec/erreur/ignoré. Trois nouveaux scénarios et 19 cas réutilisés : parcours nominal à six échanges, contrat `503` unique sans rattachement/fuite puis `FAILED`, journal d'écriture finale non confirmée. Le test existant du JAR conserve désormais `FAILED` au redémarrage sans rappeler le partenaire. Pas de suite globale ni CI verte revendiquée. [Cours et commande exacte](../../notes/sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md).

### 3.6 Pannes partenaire et répétitions — INT-23 + INT-24

**[Simulation ConfluxLab — réalisée le 2026-10-23 sur feu vert « commit & go next INT »]** : l'adaptateur journalise les échecs par UUID et opération A–F, avec catégorie, statut HTTP disponible et type d'exception, sans message, URL, corps ni trace brute. Les refus 4xx hors 408/429 sont distingués des réponses potentiellement transitoires 408/429/5xx et des timeouts de socket. L'exception initiale est relancée ; le graphe et sa finalisation après les branches lancées restent inchangés.

Les six POST n'ont aucune garantie de répétition sûre : **aucun retry, même après une erreur transitoire**. Feign et HC5 restent sans répétition automatique ; Resilience4j n'est pas ajouté sans opération éligible. Un timeout signifie une issue distante inconnue ; `FAILED` n'affirme ni absence de création externe ni rollback.

**Preuves :** `clean verify` ciblé réussi avec **7 cas Surefire + 4 Failsafe**, aucun échec/erreur/ignoré. Parcours nominal, refus `400`, contrat `503` et timeout de lecture sur A vérifiés ; une seule tentative fautive, diagnostic corrélé, aucun descendant interdit et suivi final. Le timeout de connexion n'est pas simulé et les délais socket ne constituent pas une deadline globale. [Politique, code et commande](../../notes/sprint-06/INT-23-terminer-malgre-une-panne-partenaire.md).

## 4. Stack technique proposée

| Domaine | Technologies / rôle |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot 3.x, Spring MVC, Bean Validation |
| Structure | Monolithe modulaire : modules logiques Experience/BFF et Capability, une seule application Spring Boot et un seul artefact exécutable |
| Communication interne | Appels Java via les interfaces publiques des modules ; pas de HTTP interne ni d'accès aux classes internes d'un autre module |
| Build | Maven multi-modules + Maven Wrapper ; bibliothèques `contracts`, `capability-api`, `experience-api` et assemblage unique `application` réalisé dans INT-03 |
| Persistance | MongoDB, Spring Data MongoDB |
| Événements | Spring Events in-process, `@Async` |
| Orchestration concurrente | `CompletableFuture`, executor borné |
| HTTP sortant | Spring Cloud OpenFeign vers le partenaire externe uniquement, pas entre Experience et Capability |
| Résilience | Resilience4j ; timeouts, retry sélectif, fallback explicite |
| Partenaire fictif | WireMock |
| Tests | JUnit 5, Mockito, Spring Boot Test, Testcontainers MongoDB |
| Observabilité | Logs corrélés, Actuator ; métriques à définir |
| **CI (pratique, après le DAT)** | **GitHub Actions + Maven `verify` + rapports de tests** |
| **CD (théorie uniquement)** | Promotion, environnement, stratégie de rollout et rollback |

Aucun frontend, Kafka, Kubernetes, authentification avancée ou plateforme cloud dans le MVP.

### 4.1 Responsabilités des modules

**[Simulation ConfluxLab — choix retenu dans le cadre de la délégation utilisateur pour le BFF]** : Experience est un BFF mince. Il expose le contrat REST, valide la forme des entrées et adapte les réponses/erreurs. Capability porte les règles métier, les invariants, les statuts, la persistance et l'orchestration. Elle ne se repose pas sur la seule validation du BFF pour protéger ses invariants. Les deux modules restent séparés dans la même application ; aucun appel HTTP interne n'est nécessaire.

### 4.2 Frontières fixées dans INT-01

**[Simulation ConfluxLab — cadrage réalisé sur feu vert pour INT-01]** : conserver les noms des modules existants. `capability-api` porte le métier complet, pas un second serveur HTTP ; son API publique Java sera distinguée de ses classes internes. Le module `contracts` est conservé comme bibliothèque de types Java d'échange, sans contrôleur, repository, document MongoDB ou dépendance vers Experience/Capability. Le schéma initial et ses contraintes sont définis et testés dans INT-05, sans service métier ajouté ; voir le [contrat HTTP initial](http-contracts.md).

Graphe de dépendances **assemblé dans INT-03 et protégé par les contrôles automatiques d'INT-04** :

```mermaid
flowchart TD
  App[application — seul démarrage Boot] --> Exp[experience-api — BFF mince]
  App --> Cap[capability-api — métier]
  Exp -->|API publique Java uniquement| Cap
  Exp --> Contracts[contracts — types Java partagés]
  Cap --> Contracts
```

Aucun cycle, aucune dépendance Capability → Experience, aucun HTTP interne ni accès du BFF aux repositories. Dans INT-03, les dépendances Maven suivent ce graphe ; seul `application` déclare le plugin Boot. Son démarrage `dev.confluxlab.ConfluxLabApplication` est à la racine des packages pour découvrir Experience et Capability dans le même contexte Spring.

Le BFF injecte `dev.confluxlab.capability.api.FamilyRequests` ; son implémentation reste dans `capability.internal.service`, sans import de ce package par Experience. Dans INT-03/04, l'exception publique provisoire `CapabilityNotReadyException`, sans statut HTTP, signalait l'indisponibilité des deux opérations, adaptée en `503` par le BFF. L'acceptation mémoire l'a remplacée pour le POST dans INT-06, puis la consultation réelle dans INT-07 a permis de supprimer cette exception et son mapping provisoire.

Le `clean verify` global d'INT-03 réussit avec 64 tests et le JAR réel démarre sans base ni partenaire. Les deux routes valides répondent `503` ; les réponses de succès des tests de délégation proviennent uniquement d'un double. Voir les [preuves INT-03](../../notes/sprint-01/INT-01-socle-modulaire.md). Les modèles d'échange sont définis dans INT-05 ; cette preuve historique ne réalisait pas encore l'acceptation, développée ensuite dans INT-06. Le suivi effectif a ensuite été réalisé dans INT-07, sans modifier ces preuves historiques. Les contrôles automatiques de frontières ont ensuite été réalisés dans INT-04, sans modification du métier.

**INT-04 — garde-fous retenus :** ArchUnit 1.4.1, en dépendance `test` du module d'assemblage, analyse les classes de production sous `dev.confluxlab`, en excluant les sorties de test. Les règles interdisent les cycles entre sous-packages racine, Capability → Experience, Experience → Capability hors `capability.api..`, toute dépendance de `contracts` vers les autres modules, et le retour des bibliothèques vers l'assemblage. Le package racine `dev.confluxlab`, qui contient le démarrage Boot, est réservé à l'assemblage et protégé séparément, car il n'appartient pas aux slices des sous-packages. Les dépendances vers un éventuel `dev.confluxlab.application..` sont protégées de la même manière.

Les mêmes règles sont exercées avec des fixtures de test délibérément fautives et un graphe autorisé ; les diagnostics vérifient les classes source et cible des violations. Le `clean verify` global d'INT-04 réussit avec **76 tests** (dont 5 de frontières, 7 de détection et les 3 tests d'assemblage existants). ArchUnit et les fixtures sont absents de l'artefact livré. Ces contrôles de dépendances de bytecode ne remplacent ni les tests métier, ni une revue des appels indirects par réflexion/configuration ; ils supposent que les packages respectent les responsabilités convenues. Voir le [tutoriel INT-04](../../notes/sprint-01/INT-01-socle-modulaire.md).

### 4.3 Acceptation temporaire réalisée dans INT-06

**[Simulation ConfluxLab — incrément autorisé]** : `InMemoryFamilyRequests` remplace le bean indisponible, valide une copie locale du payload avec les contraintes existantes, génère un UUID et conserve un `StoredFamilyRequest` en `PENDING` avec `createdAt`, avant de retourner l'acceptation. Les noms de types et le stockage restent internes. Le BFF conserve le contrat HTTP et la construction des liens publics ; aucun appel réseau interne ou accès à la map ne lui est ajouté.

Le store utilise `ConcurrentHashMap` et `putIfAbsent` : une collision ne remplace pas une demande existante et échoue sans acquittement. La liste conservée est copiée et rendue non modifiable ; `PersonPayload` ne référence que des valeurs immuables. Un `Clock` UTC injectable fournit l'horodatage. Les contraintes sont vérifiées aussi sur les appels Java directs, sans inventer de règle d'identité ou de métier supplémentaire ; leur refus public est adapté en `400` générique, les erreurs inattendues en `500` générique.

Cette mémoire est **volatile, non bornée et sans nettoyage** : le `202` ne garantit ni durabilité, ni idempotence, ni début de traitement. Deux POST identiques créent deux demandes indépendantes. À l'étape INT-06, le GET restait `503`, même pour un UUID accepté ; son `Location` préparait INT-07, désormais réalisée. Aucun événement, `@Async`, MongoDB ou partenaire n'est ajouté. Un futur provider MongoDB devra remplacer ce stockage, pas le rendre artificiellement durable par documentation.

Le `clean verify` global INT-06 passe avec **96 tests**. Le JAR réel a répondu `202` à deux POST avec UUID distincts, `503` au GET et `400` au corps invalide, puis a été arrêté ; les tests internes prouvent séparément la conservation du payload avant retour. Voir le [tutoriel INT-06](../../notes/sprint-02/INT-05-accepter-et-consulter-une-demande.md).

### 4.4 Consultation réalisée dans INT-07

**[Simulation ConfluxLab — incrément autorisé]** : `getRequest(UUID)` recherche la demande dans le store interne et projette uniquement `requestId`, `status` et le `createdAt` d'origine vers `RequestView`. Cette lecture ne crée aucune entrée, ne change aucun statut, n'actualise aucune date et ne déclenche aucun traitement. Le contrôleur continue d'appeler l'interface publique, sans accéder au stockage.

L'absence est signalée par `RequestNotFoundException`, publique mais sans dépendance HTTP, puis adaptée en `404` générique dans Experience. Un identifiant HTTP manifestement mal formé est refusé en `400` par la conversion MVC, avant l'appel à Capability. Un argument Java `null` est rejeté explicitement comme erreur d'argument avant le store ; ce cas ne se confond pas avec une demande inconnue. Aucun durcissement supplémentaire de canonicalisation ou de version UUID n'est introduit.

Le `clean verify` global INT-07 réussit avec **108 tests**, dont les tests de frontières toujours actifs. Le JAR réel démontre POST `202` puis GET `200 PENDING`, des métadonnées stables et limitées à trois champs, ainsi que `404`/`400`. Après un redémarrage local, l'UUID précédemment accepté répond `404`, confirmant la volatilité ; les deux processus sont arrêtés. Les fixtures de lecture couvrent les quatre valeurs de statut, mais l'application actuelle ne produit que `PENDING` : aucune transition métier n'est ajoutée. Voir le [tutoriel INT-07](../../notes/sprint-02/INT-05-accepter-et-consulter-une-demande.md).

### 4.5 Refus et confidentialité vérifiés dans INT-08

**[Simulation ConfluxLab — incrément autorisé]** : la matrice des contraintes existantes est exercée en HTTP et en Java direct. Les tests prouvent l'absence d'appel à l'acceptation pour les refus MVC et l'absence d'interaction avec le store pour les refus de Capability ; une borne positive à 100 caractères évite de valider un système qui rejetterait tout. Aucune règle d'identité ou d'assurance supplémentaire n'est inventée. Deux demandes restent isolées malgré un refus intercalé et depuis deux appelants Java concurrents, sans fonctionnalité asynchrone ajoutée en production.

Un défaut a été reproduit : un identifiant mal formé contenant un marqueur fictif était recopié par MVC dans `ProblemDetail.instance`, malgré un détail générique. Le helper d'erreur fournit désormais `URI.create("")`, non nulle, pour empêcher cet écho automatique. La sérialisation actuelle omet cette valeur vide ; les tests protègent à la fois l'objet Java et l'absence stricte du champ et du marqueur dans le JSON. Ce couplage MVC/Jackson est volontairement testé ; aucun identifiant d'erreur supplémentaire n'est introduit. Le changement concerne les erreurs gérées par cet advice, pas une garantie sur tous les logs, proxys ou filtres.

Le `clean verify` global réussit avec **181 tests**. Les erreurs `500` et le refus de Capability sont simulés explicitement par doublures ; le JAR réel valide des refus `400`/`415`/`404`, la confidentialité et deux acceptations/consultations indépendantes. Le processus est arrêté. Cette couverture de non-régression est plus large que le minimum nécessaire pour comprendre le principe, pas une preuve de sécurité exhaustive. Voir le [cours INT-08](../../notes/sprint-02/INT-05-accepter-et-consulter-une-demande.md).

### 4.6 Organisation interne de Capability

**[Simulation ConfluxLab — refactor explicite]** Sous `dev.confluxlab.capability.internal`, ce rangement vise la compacité de lecture, pas une nouvelle architecture ni un changement métier :

| Sous-package | Rôle |
|---|---|
| `config` | Câblage Spring, MongoDB et executor |
| `service` | Acceptation et consultation des demandes |
| `persistence` | Document, contrat de stockage et adaptateur MongoDB |
| `event` | Événement local et listener asynchrone |
| `orchestration` | Contrat des opérations et composition du graphe A–F |
| `client` | Client partenaire Feign, configuration et adaptation HTTP |

Les tests suivent ces sous-packages ; la fixture `application/src/test/java/dev/confluxlab/capability/internal/fixture` ne bouge pas. Certaines classes et méthodes deviennent `public` pour les échanges Java entre sous-packages, mais restent internes : le BFF est limité à `capability.api`, les règles ArchUnit sont préservées. **Validation après déplacement :** `clean verify` ciblé réussi le 2026-10-22, **25 cas Surefire + 8 Failsafe**, aucun échec/erreur/ignoré. Les contrôles de frontières, le parcours partenaire et le redémarrage du JAR passent ; le test MongoDB confirme aussi la lecture avec l'ancien nom `_class` du document. Aucune migration des documents n'est nécessaire pour cette lecture typée. [Commande et limites](../../notes/sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md#organisation-interne-après-déplacement).

## 5. Contrats HTTP

Le [contrat initial INT-05](http-contracts.md) précise les champs, contraintes, exemples JSON et refus. INT-06 démontre maintenant le POST `202` après acceptation réelle en mémoire. INT-07 démontre maintenant le GET `200` pour une demande présente en mémoire, `404` si elle est absente et `400` pour les formats d'identifiant invalides testés. L'ancien `503` du placeholder a été supprimé ; aucun comportement d'erreur MongoDB ou partenaire n'est déduit de cette évolution. `RequestView` est actuellement limité aux métadonnées ; résultat et erreur métier détaillés appartiennent à une évolution ultérieure du suivi.

**POST `/api/v1/families`**

- Reçoit une demande de création fictive.
- Retourne `202 Accepted` avec `requestId`, `statusUrl` et `status`.
- Si l'acceptation échoue avant la prise en charge, répondre avec une erreur HTTP explicite, sans prétendre que le traitement est lancé.

**GET `/api/v1/requests/{requestId}`**

- Dans INT-07, retourne uniquement les métadonnées de `RequestView` ; le résultat ou motif d'échec métier appartient à une évolution ultérieure, non implémentée.
- Statuts minimaux : `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`.
- Une panne partenaire **après le `202`** modifie le statut consultable, pas la réponse HTTP déjà envoyée.

## 6. Données MongoDB

Collection cible : **`family_requests`**, non créée à l'étape INT-09.

**[Simulation ConfluxLab — INT-09 réalisée sur demande de passage à cette tâche]** : à cette étape, le modèle interne `StoredFamilyRequest` était un record Java sans annotation MongoDB, non exposé au BFF. INT-10 lui ajoute ensuite le mapping décrit en section 6.1. Son contrat de données est le suivant :

| Champ | Rôle et règle de mise à jour |
|---|---|
| `requestId` | UUID de corrélation fixé à la création ; depuis INT-10, clé MongoDB `_id` unique |
| `status` | `PENDING` à la création, puis transitions autorisées ci-dessous |
| `payload` | Copie des données reçues, liste des ayants droit non modifiable ; inchangée pendant le cycle |
| `createdAt` | `Instant` de création fourni par l'horloge de Capability ; ne change jamais |
| `updatedAt` | Initialement égal à `createdAt`, puis date de la dernière transition ; ne recule pas |
| `completedAt` | Absent en `PENDING`/`PROCESSING` ; égal à `updatedAt` à l'entrée dans un état final |
| `result?` | Champ cible optionnel pour un résultat métier en succès ; structure et implémentation différées au traitement partenaire, aucun champ vide ajouté au record |
| `error?` | Champ cible optionnel pour un motif métier d'échec, pas une exception/trace brute ; structure et implémentation différées, aucune exposition HTTP ajoutée |

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> PROCESSING
    PROCESSING --> SUCCEEDED
    PROCESSING --> FAILED
```

`PENDING` signifie « acceptée, en attente », `PROCESSING` « prise en charge », `SUCCEEDED` « terminée en succès » et `FAILED` « terminée en échec ». Le modèle refuse tout autre passage, y compris une répétition du même état ou la sortie d'un état final. Un échec technique avant l'acceptation n'est pas une transition de ce graphe ; il ne produit pas de faux `202`.

`pending(...)` initialise les dates. `transitionTo(...)` retourne un **nouveau record**, conserve l'UUID, le payload et `createdAt`, et met à jour les dates. Elle ne sauvegarde rien. Deux appelants partant du même objet pourraient chacun produire un objet `PROCESSING` : la prise en charge atomique en base, réalisée ensuite dans INT-11 (section 6.2), reste distincte de cette validation locale. Le constructeur permet de représenter un état stocké avec des dates cohérentes ; il ne prouve pas l'historique des transitions.

**Preuves INT-09 :** deux nouveaux tests de cycle/refus et un test existant d'acceptation exécutés avec succès ; pas de nouveau build global ni de test MongoDB. Le POST utilise désormais `pending(...)` mais reste en mémoire ; aucun traitement de production n'appelle les transitions. Le suivi HTTP reste limité à `requestId`, `status`, `createdAt`. Voir le [cours INT-09](../../notes/sprint-03/INT-09-modele-et-transitions-atomiques.md).

L'UUID **corrèle** la demande HTTP, le futur document persisté, l'événement et le traitement ; il n'assure ni idempotence, ni verrouillage, ni unicité de la création côté partenaire.

### 6.1. Persister avant d'accepter — INT-10

**[Simulation ConfluxLab — réalisée sur feu vert « commit la 9 & go INT-10 »]** : `FamilyRequestService` remplace le nom devenu trompeur `InMemoryFamilyRequests`. Validation, génération de l'UUID et projection du suivi restent dans ce service. Il dépend de l'interface interne `FamilyRequestStore`, dont l'unique implémentation de production est `MongoFamilyRequestStore`, avec `MongoTemplate`. L'ancien store mémoire est déplacé sous `src/test` pour préserver les tests unitaires ; aucun fallback mémoire ni profil de repli n'est livré.

`StoredFamilyRequest` est mappé par `@Document("family_requests")` et `@Id` sur l'UUID : son `requestId` Java devient `_id` en BSON UUID standard, protégé par l'index unique natif de MongoDB. L'adaptateur utilise **`insert`**, pas un upsert : une collision échoue sans remplacer le document existant. Les dates sont stockées à la précision milliseconde de BSON Date ; la création est normalisée à cette précision avant insertion. Le document et le repository restent internes, le contrat HTTP est inchangé.

Le `MongoTemplate` de Capability impose **`w=majority`, journalisation `j=true`, `wtimeout` configuré à 5 s**. Ce dernier concerne la satisfaction du write concern lorsqu'il s'applique ; il ne borne pas universellement un flush du journal ni toute la requête HTTP, en particulier sur le standalone. Les délais réseau restent distincts. Le service attend le retour synchrone de l'insertion avant de construire son reçu ; le BFF retourne alors `202`. Sur l'instance standalone du labo, « majority » ne crée aucune réplication : la majorité est cette instance unique, avec acquittement du journal. Ce n'est ni une sauvegarde ni une garantie contre la perte du disque.

Une erreur d'écriture remonte vers l'advice existant en **`500` générique**, sans `Location` ou reçu d'acceptation. Un timeout ou une déconnexion peut néanmoins survenir après une écriture réussie : erreur HTTP ne signifie pas certitude d'absence en base. Aucun retry applicatif ni idempotence n'est ajouté ; les écritures rejouables du driver sont désactivées dans l'URI par défaut.

La configuration de l'assemblage utilise `MONGODB_URI`, avec une valeur locale par défaut vers `conflux_lab`, des délais de connexion/sélection/lecture et la représentation UUID standard. Un MongoDB accessible est désormais nécessaire pour réussir POST/GET ; démarrer le contexte seul ne constitue pas un contrôle de disponibilité. Le choix utilisateur retient l'image officielle `mongo:8.0.16`, une instance standalone, pas une image Alpine personnalisée ni un cluster.

**Preuves :** deux nouveaux tests avec MongoDB Testcontainers et un test POST/GET existant réutilisé réussissent : relecture de l'UUID/payload/dates, collision refusée sans écrasement, panne d'insertion simulée renvoyant `500` au lieu de `202`, suivi limité aux métadonnées. Les tests Spring d'assemblage partagent désormais ce MongoDB temporaire ; le cache WiredTiger de test est limité à 0,25 Gio, pas la mémoire totale du conteneur. Testcontainers 1.21.4 corrige l'incompatibilité Docker reproduite avec la 1.21.3 gérée par Boot. Les conteneurs de test ont été supprimés après la JVM ; aucune base utilisateur n'a été utilisée. Pas de nouveau build global, JAR démarré ou preuve de redémarrage : ces travaux et les mises à jour atomiques restent distincts (INT-11/12). Voir le [cours INT-10](../../notes/sprint-03/INT-10-insertion-et-persistance-mongodb.md).

### 6.2. Sécuriser les mises à jour — INT-11

**[Simulation ConfluxLab — réalisée sur feu vert « commit ça & push puis go INT-11 »]** : `MongoFamilyRequestStore.transition(requestId, expectedStatus, nextStatus, changedAt)` ajoute une opération interne, sans nouvelle route HTTP ni extension de l'API publique. `StoredFamilyRequest.requireTransition(...)` porte désormais la règle du graphe, réutilisée par la copie locale et par l'adaptateur MongoDB ; une transition interdite est rejetée avant l'appel en base.

L'adaptateur exécute **un seul `findAndModify`**, avec filtre sur `_id`, statut attendu et `updatedAt <= changedAt` (date normalisée à la milliseconde). MongoDB évalue la condition et la modification atomiquement sur le document. Une double tentative `PENDING → PROCESSING` ne laisse qu'un résultat gagnant. Une transition finale ne peut être écrasée par un appel resté sur `PROCESSING`, car son filtre ne correspond plus. Aucun verrou Java, transaction multi-document, champ de version ou lecture préalable n'est ajouté ; le graphe actuel ne rouvre jamais un état antérieur.

L'`Update` ne modifie que `status`, `updatedAt` et `completedAt` : date de fin supprimée pour `PROCESSING`, fixée à la date de transition pour `SUCCEEDED`/`FAILED`. UUID, payload, `createdAt` et autres champs sont préservés. `returnNew(true)` retourne la valeur après modification ; `upsert(false)` interdit de créer un document lors d'une transition d'UUID inconnu. Un résultat présent signifie « modification confirmée » ; `Optional.empty()` signifie « aucune correspondance » (absence, statut différent ou date trop ancienne), pas une panne. Les exceptions MongoDB remontent normalement et un timeout peut toujours laisser une issue d'écriture inconnue.

**Preuves :** 2 nouveaux tests MongoDB et les 2 tests du modèle existants réussissent. Deux appelants synchronisés laissent un seul gagnant ; dates, transitions invalides, états finaux et absence d'upsert sont vérifiés. Un diagnostic fictif non mappé ajouté uniquement dans la fixture reste en base après modification mais n'apparaît pas dans le suivi HTTP. Cela ne définit pas le futur schéma `error`/`result`, toujours différé. Aucun composant de production ne déclenche encore ces transitions : les POST restent normalement `PENDING`, le GET demeure une projection en lecture seule. Pas de reprise, propriétaire de traitement, bail, fencing token ou garantie exactly-once. Aucun replica set, nouvelle dépendance, suite globale, démarrage du JAR ou redémarrage n'est ajouté. Voir le [cours INT-11](../../notes/sprint-03/INT-09-modele-et-transitions-atomiques.md).

### 6.3. Vérifier la persistance et le cycle Maven — INT-12

**[Simulation ConfluxLab — réalisée sur feu vert « commit 11 & go 12 »]** : INT-11 a été commitée localement (`af3f215`) avant cet incrément. Aucun code applicatif n'est modifié. Les deux classes MongoDB sont renommées `MongoFamilyRequestStoreIT` et `MongoFamilyRequestTransitionIT`, sans ajout de cas ; Failsafe 3.5.4 est déclaré dans le module `application`, avec les goals `integration-test` et `verify`. Le POM racine en fixe la version dans `pluginManagement` ; cette gestion seule ne suffirait pas à l'activer. `failIfNoTests=true` évite une réussite silencieuse si aucun IT n'est découvert.

Un seul scénario est ajouté : `FamilyRequestPersistenceIT`. Failsafe s'exécute après `package` et le repackage Boot ; le chemin du JAR est fourni par la propriété système de test `application.jar`. Le test lance une première JVM sur loopback et port libre, effectue POST `202` puis GET `200`, arrête cette JVM, puis lance le même JAR dans une seconde JVM avec **la même URI MongoDB Testcontainers**. L'UUID, `PENDING` et le `createdAt` restent identiques à la relecture. Le MongoDB temporaire reste vivant entre les deux processus ; il n'est ni redémarré ni recréé. Ce test démontre la persistance à travers un redémarrage de l'application, pas la récupération après crash de MongoDB ou perte du stockage.

**Preuves du 2026-10-22 :** `clean verify` du réacteur complet avec sélection d'un seul test Surefire de frontière Experience/Capability ; **1 cas Surefire + 5 cas Failsafe réussis**, aucun échec/erreur/ignoré. Les quatre cas MongoDB préexistants sont réutilisés et le nouveau scénario ajoute un seul test. Les goals `failsafe:integration-test` puis `failsafe:verify` et les rapports correspondants ont été observés. Les deux JVM réelles (PID `591557`, puis `591651`) relisent la demande `3bfc5530-6199-402c-ace1-c54f18d6c9ae`, via les ports HTTP `43047` puis `32929`, sur le même MongoDB `localhost:32773`. Les processus et conteneurs temporaires ont été arrêtés/supprimés ; leurs logs locaux sous `target/failsafe-reports/` sont ignorés par Git.

Ce passage **n'exécute pas la suite Surefire complète**. La commande non filtrée `./mvnw -B -ntp verify` est désormais configurée pour inclure les IT, mais n'a pas été lancée telle quelle : ne pas lui attribuer ce résultat de six cas sélectionnés. Certains tests HTTP existants nommés `*Test` restent sous Surefire et utilisent aussi MongoDB ; la convention de nommage ne rend pas tous les `*Test` unitaires. Aucun déploiement, CI verte, replica set, événement ou traitement asynchrone n'est ajouté. Les stories techniques du sprint 03 sont réalisées ; la restitution pédagogique n'est pas évaluée et INT-13 attend son propre feu vert. Voir le [cours INT-12](../../notes/sprint-03/INT-10-insertion-et-persistance-mongodb.md).

## 7. Sujets techniques prioritaires

- `@Async` et Spring Events locaux : exécution, exceptions, race conditions et perte d'événements à l'arrêt.
- `CompletableFuture` : graphe de dépendances, propagation des échecs, bornage des pools.
- Feign/Resilience4j : timeout, retry uniquement quand répétition sûre, fallback sans succès fictif.
- MongoDB/partenaire : états, opérations partielles, reprise, absence de transaction distribuée.
- CI : comment un commit déclenche compilation/tests, avec MongoDB et WireMock **isolés et reproductibles**.
- CD : expliquer séparément livraison, déploiement, approbations, rollback et stratégie de migration.

### 7.1 Fenêtres de panne documentées — INT-13

**[Simulation ConfluxLab — analyse documentaire simplifiée sur demande utilisateur]** : comprendre trois situations suffit pour cet incrément ; aucun mécanisme de fiabilité supplémentaire n'est demandé.

1. **Insertion non confirmée** : pas de publication ni de reçu `202` ; une erreur/timeout ne prouve pas que le document est absent, car l'acquittement peut avoir été perdu.
2. **Demande enregistrée mais pas prise en charge** : arrêt avant publication, rejet de soumission ou perte de la tâche en mémoire peuvent laisser `PENDING` sans traitement. Le client peut avoir reçu une erreur, une coupure, ou déjà `202` si la tâche avait été acceptée avant sa perte.
3. **Traitement pris en charge puis interrompu** : `PROCESSING` peut rester bloqué ; `FAILED` suppose une transition effectivement écrite. Une erreur du worker ne réécrit pas un `202` déjà reçu.

**[Proposition — flux cible]** : insertion acquittée → publication/soumission → branches HTTP et listener indépendantes. Le listener peut commencer avant ou après la réception du `202`. L'événement local n'est pas durable ; le graphe reste `PENDING → PROCESSING → SUCCEEDED/FAILED`, sans reprise automatique ajoutée.

Le [cours INT-13](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md) porte le schéma et les explications. Analyse statique uniquement, aucune panne exécutée dans INT-13. Après la simplification, INT-14 a été reprise et réalisée séparément, comme décrit ci-dessous.

### 7.2 Publication et prise en charge asynchrone — INT-14

**[Simulation ConfluxLab — réalisée sur reprise explicite « commit la 13 et reprends la 14 »]** : INT-13 simplifiée et la règle d'équilibre sont commitées localement dans `6081781`. `FamilyRequestService` reçoit `ApplicationEventPublisher` et publie `FamilyRequestSubmitted(requestId)` uniquement après le retour de l'insertion. L'événement est un record interne sans payload personnel ; le BFF et les contrats publics ne changent pas.

À l'étape INT-14, `@EnableAsync` active les proxies ; `FamilyRequestListener` associe `@EventListener` et `@Async("applicationTaskExecutor")`. Il utilise alors provisoirement l'executor auto-configuré par Boot ; INT-15 le remplace ensuite par le pool dédié décrit en section 7.3. Il relit le document et, seulement s'il est `PENDING`, tente le `findAndModify` atomique d'INT-11. Deux lectures concurrentes ne remplacent pas cette protection en base.

À l'étape INT-14, le listener s'arrêtait après cette tentative : aucun travail métier ou état final n'était produit. Ce point d'arrêt historique est dépassé par le raccordement INT-21 + INT-22 décrit en section 3.5. Le POST conserve son reçu initial `PENDING` ; le GET peut déjà lire `PROCESSING`. Ni l'ordre de démarrage par rapport au `202`, ni une reprise après crash ne sont garantis. La future orchestration devra continuer uniquement sur une prise en charge gagnante.

**Preuves du 2026-10-22 :** `verify` ciblé réussi, **5 cas Surefire + 4 cas Failsafe**, aucun échec/erreur/ignoré. Deux tests unitaires existants vérifient publication après sauvegarde et absence de publication si elle échoue. Un seul scénario est ajouté : POST accepté pendant que les tentatives de prise en charge sont bloquées, puis deux réceptions donnent un seul gagnant sur MongoDB réel. Les tests de suivi, insertion et redémarrage existants sont adaptés au passage à `PROCESSING` ; le JAR réel conserve cet état entre deux JVM, ce qui ne démontre pas une reprise. Suite complète, saturation, traitement métier, CI et INT-15 non exécutés. Voir le [cours INT-14](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md).

### 7.3 Bornage et journalisation — INT-15

**[Simulation ConfluxLab — réalisée sur feu vert « ok commit ça & go 15 »]** : INT-14 et le plan allégé sont commitées localement dans `bdb93f8`, sans push. Le listener utilise désormais `@Async("familyRequestExecutor")`. Ce bean `ThreadPoolTaskExecutor` a un nombre fixe de workers et une file bornée ; défauts pédagogiques modifiables : `conflux.processing.workers=2`, `conflux.processing.queue-capacity=16`, `conflux.processing.shutdown-await-seconds=5`. Workers et attente sont positifs, file non négative ; une configuration invalide est refusée au démarrage.

`AbortPolicy` rend le rejet explicite, sans suppression silencieuse ni exécution sur le thread HTTP. Le mapping générique existant retourne `500` sans reçu ni `Location`, pas un nouveau `503`. L'insertion préalable n'est pas annulée : un document `PENDING` peut rester sans tâche. À la destruction du bean, les tâches soumises peuvent terminer et l'attente est bornée ; les 5 s ne sont ni un timeout de tâche ni une garantie d'arrêt forcé ou de durée totale de fermeture de la JVM.

`AsyncConfigurer` fournit le handler des exceptions non capturées du listener `void`. Le journal `ERROR` contient l'UUID et le type d'exception, pas ses arguments, message ou trace brute susceptibles de contenir des données personnelles. Aucun diagnostic MongoDB/HTTP, nouvel état ou passage artificiel à `FAILED` n'est ajouté ; le handler ne modifie pas le document.

**Preuves du 2026-10-22 :** `verify` ciblé réussi, **1 contrôle Surefire de frontière + 3 cas Failsafe**, aucun échec/erreur/ignoré. Le cas concurrent INT-14 est réutilisé ; deux cas sont ajoutés : saturation avec 2 workers/1 place puis refus du quatrième POST, et erreur de lecture simulée journalisée avec UUID sans message privé, document toujours `PENDING`. Pas de nouveau redémarrage du JAR, test d'arrêt prolongé, crash ou suite complète. Voir le [cours INT-15](../../notes/sprint-04/INT-15-pool-borne-et-erreurs-async.md).

### 7.4 Consolidation des preuves — INT-16

**[Simulation ConfluxLab — réalisée sur feu vert « go 16 »]** : revue du code commité dans `59c881c`, sans modification applicative ou de test. Les trois cas existants de `FamilyRequestEventsIT` couvrent les quatre comportements : réponse indépendante, prise en charge unique, rejet à saturation et erreur async corrélée. Le câblage sur le pool dédié, les attentes bornées et le nettoyage ont été relus ; aucun manque nécessitant une adaptation n'a été identifié pour ces critères.

Les deux rapports ciblés d'INT-15 ont été relus (1 cas Surefire + 3 cas Failsafe, aucun échec/erreur/ignoré) : ce sont des preuves historiques, pas une nouvelle exécution INT-16. Aucun build, test, redémarrage du JAR ou crash n'est relancé. Le risque d'abandon `PENDING`/`PROCESSING` reste une limite, sans reprise automatique. Voir le [bilan INT-16](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md). Les stories techniques du sprint 04 sont réalisées ; leur consolidation ne valait pas feu vert pour INT-17. Ce feu vert est intervenu ensuite, séparément ; la clôture pédagogique du sprint 04 reste à confirmer.

## 8. Performance et observabilité

Distinguer :

1. **Latence HTTP** = réception du POST jusqu'au `202`.
2. **Durée métier** = acceptation jusqu'à état terminal.
3. **Taux de réussite** = demandes réussies / demandes terminées dans un échantillon défini.

L'asynchronisme peut réduire l'attente HTTP **sans** réduire la durée métier. Le parallélisme peut accélérer les appels indépendants si les ressources et les contraintes partenaires le permettent.

Les résultats AXA rapportés (environ **30 s → 2,5 s** et **50 % → >90 %** de réussite sous charge) sont un **récit historique à contextualiser**, pas une promesse ni un benchmark ConfluxLab.

### 8.1 Démonstration et mesures locales — INT-25 à INT-28

**[Simulation ConfluxLab — réalisée le 2026-10-23 sur feu vert « commit & go next tasks »]** : le montage de test traverse un vrai port HTTP, MongoDB et Feign/WireMock avec deux demandes distinctes soumises sans attendre la première. Payloads, UUID, états finaux et échanges restent associés : un succès, un refus de contrat. Le listener journalise désormais UUID/statut après une transition finale appliquée, sans donnée personnelle ni MDC ajouté.

Les relevés distinguent la latence **vue du client** (487,422 / 478,392 ms, en incluant la réception du corps `202`), la durée issue des dates stockées (1 071 / 914 ms, bornes avant insertion/avant écriture finale), et le taux sur cet échantillon (1 succès / 2 terminées, 0 en cours). Le refus est volontaire : ces **50 % ne mesurent pas la fiabilité**. Les bornes pratiques ne sont pas une instrumentation exacte des acquittements durables.

Le comparateur uniquement sous `src/test` utilise les mêmes six appels, le même pool de deux workers et 150 ms de délai WireMock par appel. Après un warmup de chaque mode, trois passages alternés donnent les médianes **944,205 ms séquentiel / 634,928 ms concurrent**. Cette durée du graphe exclut HTTP d'entrée/listener/MongoDB ; pas de gain général ou sous charge revendiqué. Aucun second parcours de production, outil de métriques ni nouvelle dépendance.

**Preuves :** `clean verify` ciblé réussi, **7 Surefire + 6 Failsafe**, aucun échec/erreur/ignoré. Deux nouveaux scénarios intégrés ; pas de seuil chronométrique, suite globale ni CI. [Commande, données et limites](../../notes/sprint-07/INT-25-demontrer-et-mesurer-le-parcours.md).

## 9. Validation fonctionnelle et technique

| Cas | Ce qui doit être observable |
|---|---|
| Succès nominal | `202` puis `SUCCEEDED` |
| Partenaire lent | Réponse HTTP rapide ; statut encore en cours |
| Deux créations concurrentes | UUID et statuts distincts, absence de confusion entre dossiers |
| `5xx` transitoire | Comportement de retry documenté, opération répétable uniquement |
| Erreur définitive | `FAILED` et diagnostic lisible |
| Crash après insertion en base | Risque de `PENDING` abandonné ; limite d'un événement non durable |
| Séquentiel vs parallèle | Mesures séparées latence HTTP / durée métier |
| CI : changement cassant | Test en échec ⇒ workflow rouge, sans déploiement |

**Mécanisme à apprendre :** `@Async` ne transforme pas un appel Feign bloquant en I/O non bloquante. Les pools doivent être dimensionnés pour des appels qui immobilisent des threads.

## 10. Progression pédagogique par sprints

Le [plan de travail dans `docs/stages/`](../stages/README.md) détaille les objectifs, incréments, tests, exclusions et critères de clôture. Il remplace la progression globale précédente : théorie, implémentation ciblée, tests et explication sont associés dans chaque sprint.

| Sprint | Objet | Livrable cible |
|---|---|---|
| 01 | Socle et frontières modulaires | Build reproductible, une application, EXP/Capability séparées |
| 02 | Contrat REST et BFF mince | POST/GET testés, stockage temporaire explicitement non durable |
| 03 | MongoDB et statuts | Persistance vérifiée, transitions contrôlées |
| 04 | Spring Events et `@Async` | Découplage HTTP/travail, analyse des fenêtres de panne |
| 05 | `CompletableFuture` et concurrence | Graphe A–F vérifié avec doublures contrôlées |
| 06 | Parcours partenaire complet, puis pannes | Deux incréments : INT-21 + INT-22 raccordent POST/Feign/suivi final ; INT-23 + INT-24 rendent les pannes explicites et les répétitions sûres |
| 07 | Démonstration et mesures | Un incrément INT-25 à INT-28 : cas existants réutilisés, diagnostic corrélé et mesures ConfluxLab, sans plateforme supplémentaire par défaut |
| 08 | CI exploitable sur accord | Un incrément INT-29 à INT-32 : `verify`, GitHub Actions réellement observé, rapports et détection de régression |
| 09 | Atelier projet et livraison | Un atelier INT-33 à INT-36 : restitution du projet et CD conceptuelle, aucun déploiement |

**[Simulation ConfluxLab — méthode révisée sur demande du 2026-10-22] :** livrer un résultat utilisable par incrément plutôt qu'une classe ou une notion par micro-tâche. Les 16 IDs restants sont conservés et regroupés en 5 ensembles, sans modifier l'historique. Présenter le résultat, les IDs couverts, les fichiers et les preuves utiles, puis obtenir un feu vert pour cet ensemble ; ses étapes internes ne demandent pas chacune un nouvel accord. Une seule note centrée sur le flux et les principaux extraits de code, selon [le guide](../../notes/README.md), remplace les notes par sous-étape. Réutiliser les tests et compléter les risques concrets sans nouvelle matrice par défaut. Puis attendre le feu vert du prochain incrément. La réorganisation ne démarre aucun développement et ne valide ni résultat technique ni compréhension orale ; les publications distantes restent soumises à accord, la CD reste théorique.

## 11. CI — Incluse dans le périmètre pratique

### 11.1 Pourquoi l'ajouter ?

La CI est directement utile à la compréhension de la stack : elle démontre que l'application compile, que les tests du parcours et des cas de panne sont reproductibles et qu'une régression est détectée **avant** fusion. Elle introduit GitHub Actions **sans imposer de déploiement**.

### 11.2 Workflow exécuté et preuves GitHub observées

**[Simulation ConfluxLab — feux verts « next tasks go », puis « feu-vert sur tout la ci », le 2026-10-23]** : [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) déclenche le job sur `push` vers `main` et `pull_request` ciblant `main`. Le job utilise `ubuntu-24.04`, Temurin 21, cache Maven et contrôle Docker, puis `./mvnw -B -ntp verify` sans filtre. Permissions `contents: read`, credentials de checkout non conservés, limite 15 minutes. Les rapports Surefire/Failsafe de tous les modules sont collectés avec `if: always()`, rétention 14 jours ; aucun déploiement ni secret applicatif ajouté.

```mermaid
flowchart LR
  PR[PR / Push main] --> GA[GitHub Actions]
  GA --> JDK[JDK 21 + cache Maven]
  JDK --> Verify[./mvnw -B -ntp verify]
  Verify --> UT[Tests unitaires]
  Verify --> IT[Tests intégration\nTestcontainers + WireMock]
  UT --> GATE{Tous les tests OK ?}
  IT --> GATE
  GATE -->|Oui| Green[CI verte]
  GATE -->|Non| Red[CI rouge + rapports]
```

**Pipeline P0 — minimal mais utile :**

1. Checkout + configuration JDK 21 ; cache Maven.
2. `./mvnw -B -ntp verify` depuis la racine du dépôt.
3. Tests : validation, orchestration des six tâches, transitions d'état et absence de répétitions dangereuses ; aucun retry/fallback de réussite artificielle ajouté.
4. Tests d'intégration isolés : MongoDB Testcontainers et API WireMock (timeouts, `5xx`, états finaux).
5. Rapports de tests exploitables en cas d'échec ; permissions GitHub minimales ; aucune variable de production.

**Point de rigueur :** Failsafe est déjà raccordé à `integration-test`/`verify` dans `application`, avec `failIfNoTests=true`. Le passage local non filtré du 2026-10-23 réussit : **190 Surefire + 14 Failsafe**, aucun échec/erreur/ignoré, XML et résumé Failsafe inspectés. Les preuves GitHub ont ensuite été obtenues séparément : [vert sur main](https://github.com/libileh91/conflux-lab/actions/runs/39933137330), [rouge contrôlé](https://github.com/libileh91/conflux-lab/actions/runs/39933362753), puis [retour au vert](https://github.com/libileh91/conflux-lab/actions/runs/39934115383). Artefacts `test-reports` téléchargés et XML vérifiés : 190 Surefire + 14 Failsafe sans échec sur les verts, une seule assertion en échec sur le rouge ; upload réussi même après cet échec.

**Pipeline P1 — options après premier succès :** format/lint, JaCoCo, analyse des dépendances, archivage des JARs ; **pas** de seuil de couverture arbitraire.

**Definition of Done CI (quand implémentée) :** un nouveau clone peut lancer les tests sans service externe réel ; une PR cassant la logique métier fait échouer le workflow ; succès observé sur un run GitHub Actions ; commandes et preuves enregistrées.

**Statut INT-29 à INT-32 : réalisé.** Actions officielles actualisées aux releases Node 24 (`checkout@v7.0.1`, `setup-java@v6.1.0`, `upload-artifact@v7.0.2`) après avertissement de dépréciation sur le premier run. La PR de preuve #1 est fermée sans fusion ; régression volontaire restaurée. Le premier run restauré a révélé une course de test async : attente du premier worker ajoutée avant une seconde soumission, sans changer capacités, assertions ou code applicatif. Seule cette stabilisation est reportée sur `main`. Aucun déploiement. [Runs, correction, artefacts et limites](../../notes/sprint-08/INT-29-verifier-et-preparer-la-ci.md).

## 12. CD — Uniquement en théorie

**Définition :** comprendre les étapes de livraison et de déploiement automatisé, sans les exécuter sur ConfluxLab.

```mermaid
flowchart LR
  CI[CI verte] --> Artifact[Artefact versionné, immuable]
  Artifact --> Stage[Staging conceptuel]
  Stage --> Smoke[Smoke tests conceptuels]
  Smoke --> Approval[Approbation conceptuelle]
  Approval --> Prod[Rollout conceptuel]
  Prod --> Observe[Observabilité]
  Observe --> Rollback[Plan de rollback si problème]
```

Sujets d'entretien à maîtriser :

- Différence entre **continuous delivery** (livrable prêt et contrôlé) et **continuous deployment** (déploiement automatique après validations).
- Promotion de **la même version** d'artefact entre environnements, plutôt qu'un rebuild non traçable.
- Secrets, variables de configuration et autorisations minimales, **sans créer ni stocker de secrets dans le dépôt**.
- Stratégies rolling / blue-green / canary : choix selon risque, coût, capacité de rollback.
- Smoke tests, healthchecks/readiness, métriques, rollback et limites des migrations de données.

**Décision ferme de périmètre :** pas de fichier `.github/workflows/deploy.yml`, pas de publication de conteneur, pas de SSH, pas de VPS, pas de cloud et pas de déploiement effectif.

## 13. Décisions de cadrage

| ID | Décision proposée | Statut |
|---|---|---|
| D-01 | Un seul cas d'usage de création, trois personnes fictives | Proposée |
| D-02 | Monolithe modulaire : une application Spring Boot, modules Experience/BFF et Capability, appels Java internes ; partenaire externe simulé | Validée par l'utilisateur |
| D-03 | MongoDB tampon + UUID + Spring Event local + `@Async` | Proposée |
| D-04 | `CompletableFuture` et Feign/Resilience4j | Proposée |
| D-05 | Java 21 + Spring Boot 3.x + Maven | Proposée |
| **D-06** | **CI GitHub Actions pratique quand le projet sera codé** | **Recommandée** |
| **D-07** | **CD théorique seulement, aucun déploiement effectif** | **Recommandée** |
| D-08 | Doctrine d'apprentissage : faits AXA ≠ simulation ≠ proposition | Proposée |
| D-09 | BFF mince pour le contrat HTTP ; métier, invariants et orchestration dans Capability | Retenue dans le cadre de la délégation utilisateur pour le BFF |
| D-10 | Incréments complets plutôt que micro-tâches ; IDs regroupés explicitement, un feu vert et une note par résultat ; plan dans `docs/stages/` | Révisée sur demande utilisateur le 2026-10-22 ; aucun développement autorisé par cette révision |

**[Simulation ConfluxLab — décision utilisateur] :** le monolithe modulaire est le modèle de déploiement du laboratoire, sans prétendre décrire le déploiement historique AXA. Experience ne doit pas accéder directement au stockage de Capability ; les dépendances entre modules restent unidirectionnelles et passent par leurs interfaces publiques. Feign/Resilience4j reste réservé au partenaire externe.

**Questions ouvertes (non bloquantes pour le cadrage) :** évolution du suivi au-delà des métadonnées initiales ; implémentation de l'acceptation et du suivi derrière l'interface assemblée dans INT-03 ; définition historique précise des 2,5 s ; politiques exactes de pools/retries non confirmées. Ces inconnues ne doivent pas être comblées par une histoire inventée.

---

*DAT ConfluxLab V0.2 — évolution du DAT édité par l'utilisateur : ajout d'une CI pratique différée, et d'une CD strictement conceptuelle.*
