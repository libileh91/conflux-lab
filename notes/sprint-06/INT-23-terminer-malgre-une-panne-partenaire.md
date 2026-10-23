# INT-23 + INT-24 — Terminer proprement malgré une panne partenaire

**[Simulation ConfluxLab] — Réalisé le 2026-10-23, sur feu vert « commit & go next INT ».** Le parcours reste celui d'[INT-21 + INT-22](INT-21-creer-une-famille-de-bout-en-bout.md). Ce lot ajoute le diagnostic des échecs par opération et vérifie le timeout réel, sans inventer une garantie de répétition sûre.

## 1. Résultat et politique retenue

Les six opérations A–F sont des POST. Le contrat partenaire ne garantit ni clé d'idempotence, ni déduplication, ni répétition sûre des rattachements. **Aucune n'est donc rejouée automatiquement**, même après une erreur potentiellement transitoire. Feign et HC5 gardent leurs retries désactivés ; aucun Resilience4j, fallback, circuit breaker ou mécanisme de reprise n'est ajouté pour placer un outil.

| Signal observé | Catégorie du diagnostic | Décision de ce lot |
|---|---|---|
| HTTP 4xx, sauf 408/429 | `REJECTED` | Refus de la requête telle qu'envoyée ; ne pas la répéter inchangée |
| HTTP 408/429 ou 5xx | `TRANSIENT_HTTP_ERROR` | Potentiellement transitoire, mais répétition non sûre : aucun retry |
| Cause `SocketTimeoutException` | `TIMEOUT` | Issue distante inconnue ; aucun retry |
| Autre erreur Feign sans statut HTTP | `TRANSPORT_ERROR` | Échec de communication ; aucun retry ni conclusion sur l'état distant |
| Autre erreur de l'adaptateur | `UNEXPECTED_ERROR` | Conserver l'exception, ne pas fabriquer de succès |

Une catégorie est un **diagnostic**, pas un moteur de retry. HTTP `408` est ici une réponse HTTP, distincte d'un timeout local de socket. `httpStatus=-1` signifie qu'aucun statut HTTP n'est disponible.

Le GET expose toujours uniquement `requestId`, `status`, `createdAt`. `FAILED` dit que le traitement local n'a pas abouti avec succès, **pas que le partenaire n'a rien créé**. Aucune compensation ou annulation distante n'est promise.

## 2. Identifier l'opération sans changer le graphe

Source : [`FeignFamilyOperations.java`](../../capability-api/src/main/java/dev/confluxlab/capability/internal/client/FeignFamilyOperations.java), extraits partiels du code livré dans ce lot.

```java
return invoke(requestId, "A", () -> new PrincipalId(requireId(client.createPrincipal(requestId, principal))));
```

Chaque méthode fournit au helper son UUID et son opération constante : A pour le principal, B pour le contrat, C/D pour les ayants droit, E/F pour les rattachements. Ces lettres viennent du code, pas du payload ; le journal n'a pas besoin de noms, d'identifiants partenaire ou d'URL.

```java
return CompletableFuture.completedFuture(operation.get());
```

Le chemin de succès reste identique : l'appel HTTP est bloquant sur le worker de l'orchestrateur, puis sa valeur est emballée dans une future terminée. Le helper n'ajoute pas de thread ni un second mécanisme async.

## 3. Diagnostiquer, puis laisser remonter la même erreur

Fragment du `catch` dans ce helper :

```java
LOGGER.error("Family partner call failed: requestId={}, operation={}, failureKind={}, httpStatus={}, exceptionType={}",
        requestId, operationName, failureKind(failure, httpStatus), httpStatus, failure.getClass().getSimpleName());
throw failure;
```

On journalise seulement la corrélation, l'opération, la catégorie, le statut disponible et le nom du type. **Ni message d'exception, ni objet `Throwable`, corps, URL ou trace brute** : une exception Feign peut contenir la requête et la réponse privées. Le même objet d'exception est relancé ; aucun fallback ne transforme l'échec en réussite.

Fragment de reconnaissance d'un timeout :

```java
for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
    if (cause instanceof SocketTimeoutException) {
        return "TIMEOUT";
    }
}
```

Feign enveloppe notamment le timeout de lecture dans une `RetryableException`. Son **nom ne signifie pas qu'une nouvelle tentative a eu lieu** : notre `Retryer.NEVER_RETRY` l'interdit. Le parcours des causes retrouve le signal de socket sans analyser ou journaliser le message.

L'orchestrateur existant capture l'exception dans la future et attend les branches déjà lancées avant la finalisation. Le listener conserve son résumé d'échec global et tente `PROCESSING → FAILED`. Un échec de B n'annule pas les créations C/D déjà lancées ; E/F ne démarrent pas faute de contrat. Rien ne change dans le store ou le contrat public.

## 4. Preuves et limites

Depuis la racine, avec JDK 21, Docker disponible et les dépendances Maven accessibles :

```sh
./mvnw -B -ntp -pl application -am '-Dtest=FamilyOrchestratorTest,FamilyRequestListenerTest' -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=FamilyPartnerFlowIT clean verify
```

Les sélections Surefire/Failsafe et les effets du build sont expliqués dans la [note précédente](INT-21-creer-une-famille-de-bout-en-bout.md#7-vérifications-de-cette-session). `clean` retire les anciens rapports et classes ; cette commande construit le JAR et lance MongoDB/WireMock de test, sans publication.

**Résultat observé : `BUILD SUCCESS`, 7 cas Surefire + 4 cas Failsafe, aucun échec/erreur/ignoré ; rapports XML inspectés.** Deux cas supplémentaires par rapport au parcours précédent : le refus `400` et le timeout. Aucun objectif de couverture ni matrice exhaustive n'est ajouté.

| Preuve utile | Observation |
|---|---|
| Parcours nominal réutilisé | Six échanges corrélés, suivi `SUCCEEDED`, options 1000 ms connexion / 3000 ms lecture inchangées |
| Contrat `400` / `503`, scénario existant paramétré | B appelé une fois ; A et les deux créations d'ayants droit réussies restent visibles dans WireMock ; aucun rattachement ; `FAILED` et catégorie attendue, sans corps privé dans le suivi ou les logs |
| Création A retardée de 6000 ms par WireMock | Timeout de lecture à 3000 ms configurées, diagnostic `TIMEOUT`, un POST A observé, aucun descendant, suivi `FAILED` |
| Composition et finalisation réutilisées | Six cas du graphe, dont attente des branches lancées, et cas du journal d'écriture finale non confirmée |

Le délai WireMock retarde une réponse `201` ; il prouve la réception d'une requête et un timeout côté client, **pas une création durable dans un vrai système distant**. Le test n'impose pas une durée exacte du parcours HTTP complet.

Le timeout de connexion n'est pas simulé : sa valeur est vérifiée dans les options, pas son déclenchement réseau. Les catégories HTTP 408/429 et les autres erreurs de transport ne sont pas exercées séparément. Pas de deadline globale : attente en file, acquisition de connexion, durée du graphe et délais socket restent distincts. Une erreur MongoDB finale peut toujours laisser un état `PROCESSING` ou une écriture incertaine.

Aucune suite globale, CI, campagne de charge ou nouvelle mesure de performance n'a été exécutée. Les avertissements Mockito/JVM restent sans échec de validation.

## Suivi

Le [sprint 06](../../docs/stages/sprint-06-partenaire.md) et le [registre](../todos.md#td-b018) conservent ce résultat et ses limites. La suite **[INT-25 à INT-28 — Démonstration et mesures](../sprint-07/INT-25-demontrer-et-mesurer-le-parcours.md)** a été réalisée le 2026-10-23, avec ses propres relevés et preuves. Les résultats ci-dessus restent ceux du lot pannes ; la CI demeure à autoriser.
