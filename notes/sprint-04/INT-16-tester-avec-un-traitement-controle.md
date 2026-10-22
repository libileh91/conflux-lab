# INT-16 — Tester avec un traitement contrôlé

**Statut : terminée sur feu vert « go 16 », le 2026-10-22.** Cette tâche consolide les preuves existantes : **aucun code de production, test, POM ou réglage n'est modifié ; aucun test n'est ajouté ou réexécuté**. Le point de départ est le commit local **`59c881c`**, qui contient INT-15 et les notes enrichies INT-14/15.

**[Simulation ConfluxLab]** Les scénarios concernent le laboratoire, pas une implémentation historique AXA. Le « traitement contrôlé » est ici une tentative de prise en charge bloquée par le test, **pas un faux parcours métier ajouté à l'application**.

**Bilan court :** les trois cas de `FamilyRequestEventsIT` couvrent les quatre comportements de la story. Leur câblage, leurs signaux et leur nettoyage ne nécessitent pas d'adaptation pour ces critères. Les deux rapports ciblés d'INT-15 ont été relus : 1 contrôle Surefire et 3 cas Failsafe, sans échec, erreur ou cas ignoré. Ce sont des **résultats historiques réutilisés**, pas quatre nouveaux tests exécutés dans INT-16.

## Parcours de lecture

- Sections **1 à 3** : ce que l'on vérifie, où et avec quels signaux.
- Sections **4 et 5** : résultats réellement disponibles et limites.
- Le [cours INT-15](INT-15-borner-l-execution.md#6-les-tests--extraits-et-lecture-guidée) explique les réglages et les assertions clés des trois cas ; cette note approfondit leur synchronisation et leur nettoyage. Tous les extraits Java ci-dessous sont **partiels**, pris dans le code de **`59c881c`** : chaque bloc conserve des lignes contiguës, sans réécriture ni points de suspension ajoutés. Aucune des deux notes ne recopie toute la classe.

### Sommaire

- [1. Pourquoi consolider plutôt que rajouter des tests ?](#1-pourquoi-consolider-plutôt-que-rajouter-des-tests-)
- [2. Quatre comportements, trois scénarios](#2-quatre-comportements-trois-scénarios)
- [3. Comment le test maîtrise la concurrence](#3-comment-le-test-maîtrise-la-concurrence)
- [4. Commandes et origine des preuves](#4-commandes-et-origine-des-preuves)
- [5. Ce que cette consolidation ne prouve pas](#5-ce-que-cette-consolidation-ne-prouve-pas)
- [6. Formulation d'entretien](#6-formulation-dentretien)
- [7. Suivi centralisé](#7-suivi-centralisé)

## 1. Pourquoi consolider plutôt que rajouter des tests ?

Un test asynchrone peut réussir par hasard s'il suppose qu'un worker « aura eu le temps » de démarrer. Il faut contrôler une condition observable, pas deviner la vitesse de la machine.

| Terme | Sens dans ces tests |
|---|---|
| **Instrumentation** | Code réservé au test qui observe ou bloque une opération réelle |
| **Signal / latch** | Compteur de synchronisation : `await` attend zéro et `countDown` décrémente le compteur |
| **Spy Mockito** | Enveloppe d'un vrai objet ; une méthode interceptée peut encore appeler son implémentation réelle |
| **Assertion** | Vérification qui fait échouer le test si le résultat attendu n'est pas observé |
| **Attente bornée** | Attente d'une condition, avec un délai maximal d'échec ; pas une mesure de performance |

INT-14 avait ajouté un cas couvrant la réponse indépendante du worker et la double réception. INT-15 l'a raccordé au pool dédié et a ajouté saturation et erreur async. INT-16 ne demande donc pas de recréer ces preuves dans une nouvelle classe.

La revue porte sur [FamilyRequestEventsIT.java](../../application/src/test/java/dev/confluxlab/capability/internal/FamilyRequestEventsIT.java), le [listener](../../capability-api/src/main/java/dev/confluxlab/capability/internal/FamilyRequestListener.java), la [configuration](../../capability-api/src/main/java/dev/confluxlab/capability/internal/FamilyRequestConfiguration.java) et [MongoTestSupport.java](../../application/src/test/java/dev/confluxlab/MongoTestSupport.java). Le contrôle reste dans les tests : **le code applicatif s'arrête toujours après la tentative `PENDING → PROCESSING`**.

## 2. Quatre comportements, trois scénarios

Tous les noms de méthodes de ce tableau appartiennent à `FamilyRequestEventsIT` dans `59c881c`.

| Comportement attendu | Méthode existante | Preuve vérifiée dans le code |
|---|---|---|
| **Le POST n'attend pas la prise en charge** | `acceptsWhileTheListenerIsBlockedAndOnlyOneDuplicateEventCanClaim` | Le POST obtient `202 PENDING` avant l'ouverture du signal ; les workers ne sont pas le thread de requête |
| **Une double réception ne donne qu'un gagnant** | Même méthode | Deux candidats atteignent la transition après lecture de `PENDING` ; deux retours normaux, un seul `Optional` présent, puis relecture `PROCESSING` |
| **La saturation refuse explicitement** | `rejectsTheNextRequestWhenWorkersAndQueueAreFull` | Deux workers bloqués, une place en file, quatrième POST en `500` sans reçu ni `Location` ; trois transitions seulement et quatrième document toujours `PENDING` |
| **L'erreur async est corrélée sans message privé** | `logsAsyncFailureWithItsRequestIdWithoutLeakingTheExceptionMessage` | UUID et type d'exception dans la sortie, marqueur privé absent, document réel encore `PENDING` |

Le premier test regroupe deux comportements car **le même blocage contrôlé sert les deux assertions**. Il n'est pas nécessaire de séparer artificiellement ce scénario pour obtenir quatre méthodes.

### Le câblage effectivement exercé

Le câblage de `FamilyRequestEventsIT.java` se lit sans recopier ses annotations ni ses champs :

| Annotation / support | Rôle vérifié dans le flux |
|---|---|
| `@SpringBootTest` et ses propriétés | Charger l'assemblage réel avec **2 workers fixes et 1 place en file**, au lieu du défaut **16** ; le quatrième POST atteint un refus sans campagne de charge |
| `@Autowired` et `@Qualifier("familyRequestExecutor")` | Injecter le même bean que celui demandé par `@Async("familyRequestExecutor")` sur le listener, pas un pool factice sans lien avec le flux |
| `@AutoConfigureMockMvc` / `MockMvc` | Traverser le contrôleur et le service **sans socket réseau** ; le thread du test joue celui de la requête, contrairement à la démonstration du JAR avec un vrai client HTTP |
| `MongoTestSupport` et `@MockitoSpyBean` | Utiliser MongoDB réel fourni par Testcontainers ; seules les interceptions explicites du spy modifient le comportement du vrai store |
| `@ExtendWith(OutputCaptureExtension.class)` / `CapturedOutput` | Capturer les sorties pour contrôler le journal corrélé et l'absence du marqueur privé |

## 3. Comment le test maîtrise la concurrence

### 3.1 Arrêter juste avant la commande atomique

Extrait partiel de la première méthode de `FamilyRequestEventsIT.java` :

```java
doAnswer(invocation -> {
    workers.add(Thread.currentThread());
    ready.countDown();
    try {
        assertTrue(release.await(10, TimeUnit.SECONDS), "Test must release both claim attempts.");
        Optional<?> result = (Optional<?>) invocation.callRealMethod();
        results.add(result);
        return result;
    } finally {
        finished.countDown();
    }
}).when(store).transition(any(UUID.class), eq(RequestStatus.PENDING), eq(RequestStatus.PROCESSING), any(Instant.class));
```

`ready` commence à **2**. Lorsqu'il atteint zéro, les deux workers ont lu un document `PENDING` et sont arrivés à l'interception de `transition`. Ils n'ont **pas encore exécuté la commande MongoDB** : `release`, initialisé à **1**, leur ferme la porte.

Le POST a déjà rendu son reçu sans que le test ouvre cette porte. Le second événement est publié avec le **même UUID**, sans second POST : on vérifie une double réception, pas l'idempotence de deux créations HTTP identiques.

Après ouverture, `invocation.callRealMethod()` exécute le véritable `findAndModify`. Le spy ne décide pas artificiellement quel candidat gagne. Les résultats et threads sont collectés dans des `ConcurrentLinkedQueue`, adaptées aux écritures de plusieurs workers.

### 3.2 Vérifier les retours, pas seulement l'état final

Extrait partiel de la même méthode :

```java
release.countDown();
assertTrue(finished.await(10, TimeUnit.SECONDS), "Both claim attempts must finish.");
assertEquals(2, results.size(), "Both MongoDB attempts must return normally; inspect async logs otherwise.");
assertEquals(1L, results.stream().filter(Optional::isPresent).count());
assertEquals(RequestStatus.PROCESSING, store.findById(requestId).orElseThrow().status());
```

`finished` atteint zéro même si une tentative échoue, grâce au `finally` du callback. Il ne suffit donc pas à prouver deux retours normaux : **`results.size() == 2` est indispensable** avant de compter le gagnant.

Un `Optional.empty()` est une absence de correspondance de la commande conditionnelle, pas une panne MongoDB. Le test vérifie ensuite le suivi public en `PROCESSING`. Il ne prouve pas un « exactly once » des futurs effets partenaires, qui n'existent pas encore.

### 3.3 Saturer seulement après confirmation des deux workers

Le cas de saturation attend `running.await(5, TimeUnit.SECONDS)` avant le troisième POST. À ce moment, les deux workers sont réellement occupés et bloqués. Le troisième POST remplit la seule place disponible ; le test vérifie la taille de la file avant le quatrième POST.

Le refus doit alors produire `500`, sans `Location` ni `requestId` de reçu. Les quatre appels `save` sont capturés pour retrouver l'UUID refusé **en interne**, sans enrichir le contrat HTTP. Après libération et vidage du pool, le test attend seulement trois transitions et relit le quatrième document : l'insertion n'a pas été annulée par le rejet.

La comparaison d'identité des threads vérifie aussi l'absence d'exécution de secours sur le thread de requête. Le refus n'est pas déduit d'une lenteur mesurée.

### 3.4 Observer une erreur async sans réutiliser la méthode sabotée

Dans le troisième cas, `doThrow(...)` fait échouer `store.findById`. C'est **une erreur injectée dans le test**, pas une panne réseau effectivement provoquée.

Extrait partiel de cette méthode :

```java
String expectedLog = "Async family request failed: requestId=" + requestId
        + ", exceptionType=DataAccessResourceFailureException";
await("Correlated async error log").atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertTrue(output.getAll().contains(expectedLog)));
awaitWorkersIdle();
assertFalse(output.getAll().contains(privateMarker));
StoredFamilyRequest stored = mongoTemplate.findById(requestId, StoredFamilyRequest.class);
assertEquals(RequestStatus.PENDING, stored.status(), "A failure before claiming must not invent FAILED.");
```

La lecture finale utilise `mongoTemplate` car `store.findById` est précisément interceptée pour échouer. Elle relit donc la base réelle sans repasser par cette interception. Le test vérifie le texte corrélé et l'absence du marqueur privé ; le niveau `ERROR` est fixé par `LOGGER.error(...)` dans la configuration inspectée.

### 3.5 Attendre et nettoyer avec des bornes explicites

| Point vérifié | Borne et condition dans le code |
|---|---|
| Arrivée des deux candidats / workers | `ready.await` ou `running.await` : 5 s maximum |
| Blocage volontaire des tentatives | `release.await` : 10 s maximum, avec assertion sur le résultat |
| Fin des deux tentatives concurrentes | `finished.await` : 10 s maximum |
| Apparition du journal corrélé | Awaitility : 5 s maximum |
| Nettoyage après chaque scénario | Awaitility : 10 s maximum, zéro worker actif **et** file vide |

Le helper réel, **extrait partiel du fichier**, est :

```java
private void awaitWorkersIdle() {
    await("Async test workers to finish").atMost(Duration.ofSeconds(10))
            .until(() -> taskExecutor.getActiveCount() == 0 && taskExecutor.getThreadPoolExecutor().getQueue().isEmpty());
}
```

Dans les deux cas qui bloquent des workers, le `finally` extérieur ouvre toujours `release`, puis appelle ce helper. Dans le cas d'erreur, le `finally` attend également le pool au repos. Le but est de laisser finir les tâches **avant que les interceptions du spy soient réinitialisées et qu'un autre scénario s'exécute**.

Aucun `Thread.sleep` n'est utilisé comme preuve. Ces bornes font échouer une attente non satisfaite ; elles ne garantissent pas la durée totale d'une suite, ne mesurent pas la rapidité du POST et ne sont pas le réglage d'arrêt du bean.

## 4. Commandes et origine des preuves

### 4.1 Vérifications réalisées pendant INT-16

Pendant la consolidation INT-16, le code et les **deux rapports XML précis** conservés sous les chemins suivants ont été relus :

- `application/target/surefire-reports/TEST-dev.confluxlab.ModuleArchitectureTest.xml` : **1** cas, `experienceOnlyUsesThePublicCapabilityApi` ;
- `application/target/failsafe-reports/TEST-dev.confluxlab.capability.internal.FamilyRequestEventsIT.xml` : **3** cas, ceux du tableau de la section 2.

Chaque rapport indique **0 échec, 0 erreur, 0 ignoré**. Leur lecture via le parseur XML de Python est une opération locale sans exécution Java ni démarrage de conteneur. Ces sorties générées sont ignorées par Git et peuvent être supprimées par `clean` ; les [preuves INT-15](INT-15-borner-l-execution.md#7-commande-exécutée-résultats-et-limites) conservent le contexte de leur production.

La revue ne révèle pas de manque au regard des critères d'INT-16 : **aucune adaptation du test n'est nécessaire**. Les changements de cette tâche sont documentaires. La vérification finale du diff sert à confirmer que les sources, tests, POM et configurations restent inchangés, et les liens locaux de la documentation sont contrôlés séparément.

### 4.2 Commande historique conservée, non relancée

**Départ : racine `conflux-lab/`. Exécutée dans INT-15, pas dans INT-16.** Elle nécessite JDK 21, Docker accessible, dépendances Maven et image MongoDB disponibles ou téléchargeables.

```sh
./mvnw -B -ntp -pl application -am '-Dtest=ModuleArchitectureTest#experienceOnlyUsesThePublicCapabilityApi' -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=FamilyRequestEventsIT verify
```

| Élément | Rôle |
|---|---|
| `./mvnw -B -ntp` | Maven Wrapper, mode non interactif, sans progression des téléchargements |
| `-pl application -am` | Construire l'assemblage et ses modules nécessaires |
| `-Dtest=...#...` | Ne sélectionner que ce contrôle de frontière sous Surefire |
| `-Dsurefire.failIfNoSpecifiedTests=false` | Accepter que les autres modules ne contiennent pas cette méthode |
| `-Dit.test=FamilyRequestEventsIT` | Sélectionner séparément les trois cas d'intégration sous Failsafe |
| `verify` | Atteindre compilation, packaging, exécution des IT et vérification de leur résultat |

Lors de son exécution historique, cette commande a écrit les sorties `target/` et utilisé un MongoDB Testcontainers temporaire ; les téléchargements manquants pouvaient nécessiter le réseau. Aucun déploiement ni publication distante. **Résultat historique d'INT-15 : `BUILD SUCCESS`, 4 cas réussis.**

Le [passage INT-14](INT-14-publier-et-consommer.md#8-commande-exécutée-et-résultat) comptait 9 cas dans une sélection différente. **Ne pas additionner 9 et 4 pour annoncer 13 scénarios distincts**, ni les présenter comme une nouvelle validation globale d'INT-16 : le cas événementiel et le contrôle de frontière sont réutilisés.

## 5. Ce que cette consolidation ne prouve pas

- **Aucun nouveau build ou test n'est exécuté dans INT-16**, pas même les trois cas relus. On réutilise leurs résultats documentés et leurs rapports existants parce que le code n'a pas changé.
- Le test de concurrence n'impose pas que le listener démarre avant ou après le `202`. Il vérifie que le POST peut revenir sans libérer la prise en charge.
- Le redémarrage réel du JAR reste une preuve d'INT-14, pas une nouvelle démonstration. La fermeture avec un worker durablement bloqué et un crash forcé ne sont pas testés ici.
- Le délai de fermeture de 5 s configuré dans INT-15 borne une attente à la destruction du bean : ni timeout de tâche, ni interruption forcée, ni borne de fermeture totale de la JVM.

**Limite d'abandon, expliquée dans [INT-13](INT-13-dessiner-les-fenetres-de-panne.md) :** le document persiste, pas l'événement local ni la tâche en file mémoire. Un arrêt avant prise en charge peut laisser `PENDING`, et un arrêt après peut laisser `PROCESSING`. Aucun mécanisme ne relance automatiquement ces demandes. Un journal d'erreur n'écrit pas magiquement `FAILED` ; il ne modifie pas non plus un `202` déjà reçu.

Les critères techniques INT-13 à INT-16 sont documentés comme réalisés ; **la clôture pédagogique du sprint reste à confirmer**, sans évaluation orale supposée. Cette consolidation n'autorise ni INT-17 ni une nouvelle fonctionnalité.

## 6. Formulation d'entretien

> Dans ConfluxLab, je contrôle les points de synchronisation plutôt que d'attendre une durée arbitraire. Le test bloque les tentatives juste avant la mise à jour MongoDB : le POST répond quand même, puis deux réceptions du même UUID donnent un seul gagnant après libération. Un autre scénario remplit deux workers et une place en file pour vérifier le refus suivant ; le dernier injecte une erreur et vérifie le journal corrélé sans message privé. Les attentes sont bornées et les signaux sont libérés dans les finally. J'ai consolidé ces preuves sans ajouter de doublons ni les annoncer comme de nouvelles exécutions. Elles ne garantissent pas une reprise après crash : la notification reste locale et volatile.

**« Pourquoi ne pas remplacer les signaux par une pause de deux secondes ? »** La pause ne prouve ni l'arrivée des workers ni l'occupation du pool. Elle dépend de la machine. Les signaux attendent les conditions utiles, avec un délai maximal en cas de problème.

**« Retrouver `PROCESSING` après redémarrage prouve que le travail reprend ? »** Non. Cela prouve la conservation du statut. Le listener termine déjà après la prise en charge dans cet incrément, et aucune reprise n'est implémentée.

## 7. Suivi centralisé

- [TD-B014 — INT-16 : consolidation terminée, sans nouveau scénario ni réexécution](../todos.md#td-b014).
- [TD-R021 — Publication, proxy et prise en charge atomique](../todos.md#td-r021).
- [TD-R022 — Capacités, rejet et erreurs async](../todos.md#td-r022).
- [TD-R020 — Persistance et signal volatile : limite d'abandon](../todos.md#td-r020).
