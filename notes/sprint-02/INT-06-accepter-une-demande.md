# INT-06 — Accepter une demande

| Contexte | Description |
|---|---|
| Sprint | [02 — Contrat REST et BFF mince](../../docs/stages/sprint-02-rest.md) |
| Statut | **INT-06 NON réalisée** : ébauche historique pré-INT-03 ; seuls la délégation Java et le refus `503` sont validés dans INT-03, pas l'acceptation |
| Portée | Lecture historique pré-INT-03 du contrôleur, du client HTTP initial, de sa configuration et des erreurs ; éléments liés à INT-07/08, sans les déclarer réalisés |
| Sources des preuves | Note rétrospective antérieure, fichiers Experience de l'époque (certains supprimés depuis), inventaire historique des sources `contracts`/`capability-api`, [note INT-02](../sprint-01/INT-02-rendre-le-build-reproductible.md) et [DAT](../../docs/dat/ConfluxLab.md) |
| Rédaction initiale du cours | Documentation seulement ; les preuves ci-dessous concernaient une ébauche aux types manquants |
| Mise à jour INT-05 | DTO et 59 cas de test de données ajoutés ; build global réussi. À cette date, contrôleur/client inchangés, aucun test HTTP ni serveur exécuté |
| Mise à jour après INT-03 | Vérifications exécutées dans la session INT-03 : assemblage et délégation Java vérifiés, refus `503` sur le JAR réel ; aucune acceptation ni consultation métier réalisée |

**[Simulation ConfluxLab]** Ces routes sont pédagogiques, pas des endpoints historiques GFP/Open. Le code est expliqué comme une ébauche historique, jamais comme une démonstration d'acceptation fonctionnelle.

**Repère temporel — avant INT-03.** Tous les anciens extraits ci-dessous sont conservés pour expliquer l'ébauche : **ceux de `FamilyController` et d'`ApiExceptionHandler` ne sont plus des extraits exacts des sources actuelles**. Dans [INT-03 — Assembler les modules](../sprint-01/INT-03-assembler-les-modules.md), `CapabilityClient`, `CapabilityClientConfiguration`, `CapabilityProperties`, `CapabilityException`, `ExperienceApplication` et le YAML d'Experience ont été supprimés. Le BFF appelle désormais `FamilyRequests` en Java ; l'implémentation provisoire lève `CapabilityNotReadyException`, sans dépendance HTTP, et le BFF la traduit en `503`. Ce raccordement et ce refus ne réalisent pas INT-06/07. Aucune validation applicative n'est relancée pour cette mise à jour documentaire.

## 1. Objectif et vocabulaire

Le client veut demander une création de famille puis retrouver sa demande sans attendre la réalisation du métier. Le **BFF** adapte les échanges HTTP ; **Capability** porte l'acceptation, les règles métier et le stockage.

- Une **route** associe une méthode HTTP et un chemin à une méthode Java.
- Un **DTO** transporte des données ; il n'enregistre rien et n'exécute pas le métier à lui seul.
- Un **UUID** est ici un identifiant de corrélation permettant de retrouver une demande.
- `202 Accepted` signifie « demande acceptée », pas « famille créée ». `PENDING` désigne l'attente du traitement.
- Un **bean** est un objet créé et géré par Spring ; l'injection lui fournit ses dépendances.

## 2. Comprendre le problème

Un traitement long ne doit pas être présenté comme terminé dès la réponse initiale. Le contrat visé sépare **acceptation** et **consultation** : le POST retourne un identifiant et un lien ; le GET consulte ensuite l'état associé. Deux POST identiques peuvent encore produire deux demandes : UUID et `202` ne garantissent aucune idempotence.

L'ébauche supposait un aller-retour HTTP entre Experience et Capability. **Cette hypothèse est abandonnée et son code a été supprimé dans INT-03** : le monolithe modulaire appelle désormais l'API publique Java de Capability dans la même JVM. INT-03 a réalisé ce remplacement et l'assemblage unique ; les explications réseau ci-dessous décrivent l'ancien mécanisme, pas l'architecture à reproduire.

## 3. Travail réalisé pas à pas

### 3.1. Recevoir une requête et injecter la dépendance

Extrait partiel **historique, avant INT-03**, de [FamilyController.java](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java), commenté après le bloc ; il ne reproduit plus la source actuelle :

```java
@RestController
@RequestMapping("/api/v1")
public class FamilyController {

    private final CapabilityClient capabilityClient;

    public FamilyController(CapabilityClient capabilityClient) {
        this.capabilityClient = capabilityClient;
    }
```

`@RestController` déclare un contrôleur dont les valeurs de retour alimentent la réponse HTTP, avec conversion JSON selon les convertisseurs disponibles. `@RequestMapping` fixe le préfixe commun. Dans cette version historique, Spring devait fournir `CapabilityClient` au **constructeur** : le contrôleur ne le construisait pas lui-même. Depuis INT-03, la dépendance injectée est l'interface `FamilyRequests`. Un constructeur unique n'exige pas `@Autowired` ; `final` empêche de réaffecter la référence après construction. Cela rend la dépendance explicite et permet aussi de fournir un double dans un test.

### 3.2. Lire le POST : corps, validation et réponse d'acceptation

Extrait partiel **historique du contrôleur avant INT-03**, non exact des sources actuelles :

```java
    @PostMapping("/families")
    public ResponseEntity<RequestAccepted> createFamily(@Valid @RequestBody CreateFamilyRequest request) {
        RequestAccepted accepted = capabilityClient.createFamily(request);
        String statusUrl = "/api/v1/requests/" + accepted.requestId();
        RequestAccepted publicResponse = new RequestAccepted(accepted.requestId(), accepted.status(), statusUrl);
        return ResponseEntity.accepted()
                .location(URI.create(statusUrl))
                .body(publicResponse);
    }
```

`@PostMapping` complète le préfixe en `POST /api/v1/families`. `@RequestBody` demande de désérialiser le corps vers `CreateFamilyRequest`. `@Valid` déclenche la validation des contraintes du DTO lorsqu'elles existent ; il ne définit ni les champs ni leurs règles. **À l'époque de l'ébauche, les trois types importés étaient absents.** Ils sont désormais définis et testés dans [INT-05](INT-05-definir-les-contrats.md), avec le [schéma initial](../../docs/dat/http-contracts.md). Ces tests de données ne prouvent pas, à eux seuls, l'exécution HTTP de la validation. Capability doit en outre protéger ses invariants même lorsqu'elle est appelée sans HTTP.

Dans cet extrait historique, le contrôleur attend le retour de `createFamily`, reprend son identifiant et son statut, puis reconstruit une URL publique relative. Il ne génère pas l'UUID et n'enregistre pas la demande. `ResponseEntity` permet de choisir statut, en-têtes et corps ; `accepted()` prépare `202`, `location(...)` renseigne `Location`, `body(...)` fixe le contenu. **`Location` est un lien de suivi, pas une redirection automatique ni une preuve de succès métier.** Le code évite ainsi de recopier une éventuelle adresse interne dans `statusUrl`. Ce chemin de succès ne démontre aucune acceptation : l'implémentation provisoire d'INT-03 lève une exception, traduite en `503` avant la construction du `202`.

### 3.3. Lire le GET et la conversion UUID

Extrait partiel **historique du contrôleur avant INT-03**, non exact des sources actuelles :

```java
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<RequestView> getRequest(@PathVariable("requestId") UUID requestId) {
        return ResponseEntity.ok(capabilityClient.getRequest(requestId));
    }
```

`@PathVariable` lie le segment du chemin au paramètre. Spring doit convertir le texte en `UUID` **avant** d'appeler la méthode : un format invalide relève d'un `400`. Un UUID syntaxiquement valide mais inconnu devra relever du suivi métier et du `404` attendu quand ce suivi sera implémenté ; dans l'assemblage provisoire d'INT-03, un UUID valide reçoit `503`. `ok(...)` prépare `200`, sans démontrer que le suivi existe. Les réponses `200`/`404` n'ont pas été démontrées pour cette ébauche.

### 3.4. Comprendre l'ancien appel réseau bloquant

Extrait partiel **historique** de `experience-api/src/main/java/dev/confluxlab/experience/CapabilityClient.java` (**supprimé dans INT-03**), dans `createFamily` :

```java
        RequestAccepted accepted = execute(() -> restClient.post()
                .uri("/api/v1/families")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (outgoing, response) -> {
                    throw mapStatus(response.getStatusCode());
                })
                .toEntity(RequestAccepted.class), HttpStatus.ACCEPTED);
```

`@Component` rend ce client injectable ; son constructeur reçoit le bean `RestClient`. La chaîne prépare un POST JSON : `contentType` décrit le corps envoyé, `accept` la représentation souhaitée en retour. `retrieve()` prépare la récupération ; `toEntity(...)` exécute l'échange et décode statut, en-têtes et corps. **`RestClient` est synchrone : le thread appelant attend le réseau.** Renvoyer ensuite `202` ne rend pas cet échange non bloquant.

La méthode générique `execute` reçoit un `Supplier`, c'est-à-dire une opération différée appelée par `call.get()`. Elle exige exactement `202` pour le POST, `200` pour le GET et un corps non nul : tout `2xx` n'est donc pas accepté. Le POST vérifie aussi `requestId` et `status` ; le GET vérifie la concordance de l'UUID, un statut et `createdAt` non nuls. Ces contrôles locaux ne prouvent ni enregistrement ni cohérence métier complète. Aucun retry applicatif explicite n'est écrit.

### 3.5. Relier propriétés, timeouts et client HTTP

Extrait partiel **historique** de `experience-api/src/main/java/dev/confluxlab/experience/CapabilityProperties.java` (**supprimé dans INT-03**) :

```java
@Validated
@ConfigurationProperties("capability")
public record CapabilityProperties(
        @NotBlank String baseUrl,
        @NotNull @DurationMin(millis = 1) Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) Duration readTimeout) {
}
```

Le binding associe les propriétés `capability.*` aux composants du record. `@Validated` demande leur validation : URL non blanche, durées présentes et d'au moins 1 ms. `@NotBlank` ne prouve pas qu'une URL est joignable, ni même qu'elle représente le service attendu.

Extrait partiel **historique** de `experience-api/src/main/java/dev/confluxlab/experience/CapabilityClientConfiguration.java` (**supprimé dans INT-03**), dans le bean `capabilityRestClient` :

```java
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
```

`@Configuration` regroupe la configuration ; `@Bean` expose l'objet retourné à Spring. Le timeout de connexion borne l'établissement d'une connexion ; le timeout de lecture borne l'attente de réponse selon le client utilisé. Ce ne sont pas un budget global du parcours métier ni une garantie de durée exacte. Les redirections automatiques sont désactivées, et l'URL de base complète les chemins relatifs du client.

Contenu **historique** de `experience-api/src/main/resources/application.yml` (**fichier supprimé dans INT-03**) :

```yaml
spring:
  application:
    name: experience-api

server:
  port: 8080
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never
    include-exception: false

capability:
  base-url: ${CAPABILITY_BASE_URL:http://localhost:8081}
  connect-timeout: ${CAPABILITY_CONNECT_TIMEOUT:2s}
  read-timeout: ${CAPABILITY_READ_TIMEOUT:5s}
```

La syntaxe `${VARIABLE:valeur}` lit une variable d'environnement ou utilise le défaut. Les valeurs écrites sont donc 8080 pour Experience, `http://localhost:8081` pour l'ancienne cible, 2 s et 5 s pour les timeouts ; elles ne prouvent aucun service démarré. Les options `server.error` limitent les détails des erreurs serveur standard. L'HTTP interne et sa configuration appartenaient à l'ébauche supprimée dans INT-03, pas à un second serveur à lancer. La configuration de démarrage est désormais dans `application/src/main/resources/application.yml`, sans URL HTTP interne.

### 3.6. Traduire les erreurs sans exposer les détails internes

L'ancienne `experience-api/src/main/java/dev/confluxlab/experience/CapabilityException.java` (**supprimée dans INT-03**) était une `RuntimeException` transportant un `HttpStatus` via `status()`, sans le corps reçu du service. Extrait partiel **historique, avant INT-03**, de [ApiExceptionHandler.java](../../experience-api/src/main/java/dev/confluxlab/experience/ApiExceptionHandler.java), **différent de la source actuelle** :

```java
    @ExceptionHandler(CapabilityException.class)
    public ResponseEntity<ProblemDetail> handleCapability(CapabilityException exception) {
        return ResponseEntity.status(exception.status()).body(problem(exception.status()));
    }
```

`@RestControllerAdvice` centralise le traitement des exceptions des contrôleurs. `ProblemDetail` structure une erreur HTTP, notamment avec `status`, `title` et `detail`, plutôt que de renvoyer une trace Java. Le détail est choisi localement ; par exemple, le `400` reçoit exactement `Invalid request.`.

| Branche historique du client/advice pré-INT-03 | Statut prévu dans l'ébauche, non validé par ces preuves |
|---|---|
| Réponse amont `400` ou `404` | Même statut, avec détail générique local |
| Autre réponse HTTP non attendue, corps absent/incohérent ou autre `RestClientException` | `502 Bad Gateway` |
| `ResourceAccessException` sans timeout reconnu | `503 Service Unavailable` |
| Cause `SocketTimeoutException` ou `HttpTimeoutException` trouvée dans la chaîne | `504 Gateway Timeout` |
| Exception inattendue traitée par l'advice | `500 Internal Server Error` |

Dans cette version de l'advice, la surcharge de `handleExceptionInternal` remplaçait aussi les corps d'erreurs du framework pour écarter valeurs rejetées et diagnostics de parsing. Ce choix réduit les fuites de données, mais ne démontre pas une sécurité exhaustive. Le mapping « upstream » décrit l'ancien réseau : il ne justifie pas de simuler des pannes réseau entre modules Java.

## 4. Commandes expliquées

**Racine de référence :** dossier `conflux-lab/` contenant le POM parent. Aucune commande de ce chapitre n'a été exécutée pour cette mise à jour. Les commandes suivantes sont des **exercices de lecture non exécutés**, adaptés aux fichiers présents après INT-03 : elles montrent les sources en vigueur, pas les anciens extraits reproduits plus haut.

```sh
cat experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java
cat application/src/main/resources/application.yml
```

Avec `cat` disponible et depuis cette racine, la première affiche le contrôleur avec sa dépendance `FamilyRequests` ; la seconde affiche la configuration du seul assemblage Boot. Elles ne modifient rien, ne compilent pas et ne contactent aucun serveur. **Aucune nouvelle requête HTTP n'est exécutée pour cette mise à jour** ; les essais du JAR et leurs refus `503` ont été exécutés et consignés dans [INT-03](../sprint-01/INT-03-assembler-les-modules.md), sans acceptation réelle. Les exemples de payload restent disponibles dans le [contrat INT-05](../../docs/dat/http-contracts.md).

## 5. Vérifications et limites

La lecture documentaire initiale avait couvert le contrôleur, le client HTTP, sa configuration, ses propriétés, l'exception dédiée, le gestionnaire d'erreurs et le YAML, ainsi que les notes sources et le cadrage. L'inventaire initial ne montrait pas de DTO ; INT-05 a ajouté cinq types et deux classes de tests dans `contracts`. À cette date pré-INT-03, aucun service métier Capability ni test HTTP n'avait été ajouté. INT-03 a depuis ajouté l'implémentation indisponible et les tests de délégation/assemblage ; cela ne constitue pas un service d'acceptation ou de suivi métier.

L'ancienne note indiquait un parent Maven absent : ce constat était historique. Les POM et le Wrapper ont été réparés dans INT-02, puis INT-05 a défini les types. **Le `clean verify` d'INT-05 avait réussi** avec 59 cas de test de données et compilation d'Experience, avant l'assemblage. Dans la session INT-03, les vérifications exécutées ont ensuite montré 5 tests ciblés réussis, un `clean verify` à 64 tests (59 contrats, 2 contrôleur, 3 assemblage) et le démarrage du JAR réel ; POST valide et GET avec UUID ont répondu `503 application/problem+json`, sans `Location` ni `requestId`, puis le processus a été arrêté. **Le runtime d'assemblage est donc validé, mais INT-06 reste NON réalisée** : ni acceptation métier ni stockage ne sont implémentés.

Aucun enregistrement, UUID généré par Capability, suivi effectif en `PENDING`, MongoDB, événement ou traitement partenaire n'est démontré. Les anciens extraits et leurs branches ne prouvaient aucun `202`, `200`, `400` ou `404` sur un serveur. La validation INT-03 porte sur la délégation Java et le refus explicite `503`, **pas sur un succès `202`/`200` d'acceptation ou de suivi**. Cette mise à jour ne termine ni INT-06 ni INT-07/08 et n'autorise aucun développement.

## 6. Suivi centralisé

- [TD-B001 — Contrats et DTO INT-05](../todos.md#td-b001)
- [TD-B003 — Assemblage et appel Java INT-03](../todos.md#td-b003)
- [TD-B005 — Acceptation, suivi et refus INT-06/07/08](../todos.md#td-b005)
- [TD-R004 — 202 et UUID : ni succès métier ni idempotence](../todos.md#td-r004)
- [TD-R005 — DTO, @Valid et contrat : pas une preuve de fonctionnement](../todos.md#td-r005)
