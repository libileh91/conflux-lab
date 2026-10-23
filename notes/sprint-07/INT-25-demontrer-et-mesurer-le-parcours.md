# INT-25 à INT-28 — Démontrer et mesurer le parcours

**[Simulation ConfluxLab] — Réalisé le 2026-10-23 sur feu vert « commit & go next tasks ».** Deux demandes fictives traversent un vrai port HTTP local, MongoDB, le listener et Feign ; leurs résultats restent isolés. Un montage de test compare ensuite les six mêmes appels en séquentiel et avec le graphe concurrent existant. Ces chiffres sont des **mesures ConfluxLab**, jamais des résultats AXA.

## 1. Lancer et arrêter la démonstration

Commande réellement exécutée depuis la racine, avec JDK 21, Docker disponible et dépendances Maven accessibles :

```sh
./mvnw -B -ntp -pl application -am '-Dtest=FamilyOrchestratorTest,FamilyRequestListenerTest' -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=FamilyRequestDemonstrationIT,FamilyPartnerFlowIT clean verify
```

Les options de sélection sont expliquées dans le [README](../../README.md#vérifier-depuis-la-racine). La commande compile, construit le JAR et exécute les seuls cas sélectionnés. **La démonstration utilise le serveur Spring Boot du test sur loopback et port libre, pas une JVM démarrée depuis le JAR** ; la preuve de redémarrage du JAR reste celle d'[INT-21/22](../sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md).

Le montage démarre MongoDB Testcontainers et WireMock sur ports libres. À la fin, le contexte ferme Tomcat et le pool, le test arrête WireMock et Testcontainers nettoie sa base temporaire à la sortie de la JVM. Aucun serveur permanent à laisser tourner, volume durable, partenaire réel ou publication distante. `Ctrl+C` interrompt une exécution manuelle ; l'arrêt normal est automatique.

Sorties utiles sous `application/target/failsafe-reports/` : rapport `FamilyRequestDemonstrationIT`, lignes `DEMO_ACCEPT`, `DEMO_HTTP`, `DEMO_RATE`, `DEMO_GRAPH`, et `family-graph-measurements.csv`. Ces fichiers générés sont ignorés par Git ; les relevés de cette session sont conservés ci-dessous. `clean` supprime les anciens rapports avant le passage suivant.

## 2. Deux demandes, des résultats séparés

Source : [`FamilyRequestDemonstrationIT.java`](../../application/src/test/java/dev/confluxlab/capability/internal/client/FamilyRequestDemonstrationIT.java), extraits partiels. Les personnes sont fictives, les payloads et les identifiants partenaires diffèrent entre le parcours de succès et celui de refus.

```java
CompletableFuture<TimedResponse> first = postFamily(client, successfulPayload);
CompletableFuture<TimedResponse> second = postFamily(client, rejectedPayload);
```

Les deux POST sont soumis par le client HTTP avant d'attendre leurs réponses. Chacun reçoit `202`, son UUID et son lien ; le GET atteint respectivement `SUCCEEDED` et `FAILED`. Le montage vérifie les payloads conservés et les échanges partenaires **par UUID, route et corps** : six appels pour le succès, quatre pour le contrat refusé, aucun rattachement pour ce dernier.

C'est une preuve d'isolation de deux demandes soumises sans attendre la première, **pas une synchronisation prouvant un chevauchement exact de leurs traitements serveur**. Les tests du graphe déjà existants restent la preuve déterministe des dépendances et de leur composition.

Le diagnostic de l'échec reprend les logs d'INT-23/24. Seul ajout en production, dans [`FamilyRequestListener.java`](../../capability-api/src/main/java/dev/confluxlab/capability/internal/event/FamilyRequestListener.java), après une transition terminale appliquée :

```java
LOGGER.info("Family request finalized: requestId={}, status={}", request.requestId(), target);
```

Cela lie le statut confirmé à l'UUID, sans MDC ni propagation de contexte supplémentaire. Si la transition ne correspond à rien ou lève une erreur, les journaux existants « not applied » / « unconfirmed » restent distincts : aucune réussite de stockage n'est inventée.

**Deux lignes réellement observées, extraites du parcours refusé :**

```text
Family partner call failed: requestId=b93e6e2d-36ea-482a-8cdb-d7ad043c693a, operation=B, failureKind=REJECTED, httpStatus=400, exceptionType=BadRequest
Family request finalized: requestId=b93e6e2d-36ea-482a-8cdb-d7ad043c693a, status=FAILED
```

L'UUID permet de retrouver l'opération fautive et l'état final. Aucun corps d'erreur n'est exposé dans ces logs ou dans le suivi. Les créations déjà réussies ne sont pas annulées : `FAILED` n'est pas un rollback.

## 3. Trois mesures, trois bornes explicites

| Mesure du montage | Borne réelle et limite |
|---|---|
| `client202Ms` | `nanoTime` juste avant `sendAsync` → réception du corps complet du `202`. Inclut connexion, transport, serveur et ordonnancement client ; exclut sérialisation préalable et parsing JSON ultérieur. C'est la latence HTTP **vue du client local**, pas une instrumentation exacte réception/émission du serveur. |
| `businessMs` | `completedAt − createdAt` du document relu. `createdAt` est pris avant insertion et `completedAt` avant l'écriture finale : approximation du parcours métier, pas chronométrage exact entre deux acquittements durables. Horloge murale UTC, précision milliseconde et sensibilité aux ajustements d'horloge. |
| Taux de réussite | Demandes `SUCCEEDED` / demandes terminées de l'échantillon. Dénombrer aussi `FAILED` et celles encore `PENDING`/`PROCESSING`, sans compter un `202` comme succès métier. |

Fragment du relevé client :

```java
return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> new TimedResponse(response, (System.nanoTime() - started) / 1_000_000.0));
```

On chronomètre la réponse, pas l'achèvement du traitement. Ne pas soustraire ce temps à `businessMs` : les origines diffèrent et le listener peut travailler avant le retour du `202`.

**Relevé du 2026-10-23, deux demandes soumises dans la même fenêtre :**

| Demande | Latence client `202` | Durée issue du document | État final |
|---|---:|---:|---|
| Succès | 487,422 ms | 1 071 ms | `SUCCEEDED` |
| Refus du contrat | 478,392 ms | 914 ms | `FAILED` |

Fenêtre observée, du lancement des POST à l'observation des deux états finaux par GET : **1 516,6 ms**. Effectifs : **2 soumises, 2 terminées, 1 réussie, 1 échouée, 0 encore en cours**, donc **50 %**. Le refus étant injecté volontairement, ce pourcentage n'est **pas une estimation de fiabilité**. Première entrée HTTP, initialisation du servlet et connexions incluses ; aucun warmup HTTP ni relevé sous charge.

## 4. Comparer le graphe sans second parcours de production

Source : [`FamilyGraphMeasurements.java`](../../application/src/test/java/dev/confluxlab/capability/internal/client/FamilyGraphMeasurements.java), helper uniquement sous `src/test`. Le mode concurrent appelle le `FamilyOrchestrator` existant ; le séquentiel compose A → B → C → D → E → F, avec les mêmes méthodes `FamilyOperations`, données et identifiants typés.

Fragment de soumission commun au comparateur séquentiel :

```java
return CompletableFuture.supplyAsync(operation, executor).thenCompose(Function.identity());
```

Les deux modes utilisent **le même pool de deux workers**, le même client Feign/HC5 et les mêmes stubs. Chaque réponse WireMock attend **150 ms** ; les six appels sont réussis. Le mode séquentiel n'occupe qu'un worker à la fois par construction, pas un pool configuré différemment.

Le driver chronomètre avec `System.nanoTime()` du lancement du graphe au retour de son attente bornée. L'attente `get` est dans le driver de test, jamais dans les workers. La durée inclut soumission, file, composition, HTTP partenaire et réveil du driver ; elle **exclut l'HTTP d'entrée, le listener et MongoDB**. Elle n'est donc pas la durée métier du tableau précédent.

Protocole : un warmup de chaque mode exclu des résultats ; trois passages mesurés par mode, ordre séquentiel/concurrent, puis concurrent/séquentiel, puis séquentiel/concurrent. Une seule famille par passage, UUID propre à chaque parcours, aucun autre parcours de la démonstration lancé en parallèle. Huit graphes au total, **48 appels HTTP vérifiés**. Aucune assertion sur les durées ou le gain.

Environnement observé : **Linux amd64, Java 21.0.12.1, 4 processeurs visibles, Boot 3.5.7, MongoDB 8.0.16 standalone**, WireMock local ; deux workers et aucun retry. Même JVM pour les deux modes ; cette petite expérience n'isole pas les variations de la machine hôte.

| Passage | Séquentiel | Concurrent |
|---|---:|---:|
| 1 | 947,994 ms | 630,754 ms |
| 2 | 941,233 ms | 634,928 ms |
| 3 | 944,205 ms | 637,191 ms |
| Médiane | **944,205 ms** | **634,928 ms** |
| Min–max | 941,233–947,994 ms | 630,754–637,191 ms |

Ce montage illustre le chevauchement d'appels indépendants avec deux workers : environ **×1,49** sur les médianes de cet échantillon. Il ne démontre ni gain général, ni capacité sous charge, ni amélioration du taux de réussite. Trois passages ne donnent pas une estimation statistique robuste. Les résultats AXA rapportés restent distincts, sans assimilation à une latence `202` de 2,5 secondes.

## 5. Preuves et suite

**Résultat observé : `BUILD SUCCESS`, 7 cas Surefire + 6 Failsafe, aucun échec/erreur/ignoré**, rapports XML inspectés. Deux nouvelles preuves intégrées uniquement : la démonstration de deux demandes et les relevés comparatifs. Les six cas du graphe, le cas d'écriture finale et les quatre cas du parcours partenaire sont réutilisés. Ce passage ciblé n'est pas une nouvelle campagne globale ni une CI verte.

Les preuves historiques de redémarrage, timeout, refus et frontières conservent leurs dates et leur périmètre dans les cours précédents. Aucun nouveau crash, benchmark de charge, scénario de connexion lente, outil de métriques ou nouvelle dépendance. Le monolithe et l'API publique restent inchangés ; l'événement non durable, les effets partenaires partiels et les écritures finales incertaines restent des limites.

La [fiche du sprint 07](../../docs/stages/sprint-07-validation.md) et le [suivi](../todos.md) sont actualisés. **INT-29 à INT-32 — CI exploitable** restent à autoriser ; aucun workflow, push, PR ou run distant n'a été lancé.
