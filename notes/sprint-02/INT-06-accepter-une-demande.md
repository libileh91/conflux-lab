# INT-06 — Accepter une demande

| Contexte | Description |
|---|---|
| Sprint | [02 — Contrat REST et BFF mince](../../docs/stages/sprint-02-rest.md) |
| Statut | **Ébauche partielle : compilable après INT-05, mais runtime non validé. INT-06 non terminée** |
| Portée | Lecture pédagogique du contrôleur, du client HTTP initial, de sa configuration et des erreurs ; éléments liés à INT-07/08, sans les déclarer réalisés |
| Sources des preuves | Note rétrospective antérieure, fichiers Experience liés ci-dessous, inventaire des sources `contracts`/`capability-api`, [note INT-02](../sprint-01/INT-02-rendre-le-build-reproductible.md) et [DAT](../../docs/dat/ConfluxLab.md) |
| Rédaction initiale du cours | Documentation seulement ; les preuves ci-dessous concernaient une ébauche aux types manquants |
| Mise à jour INT-05 | DTO et 59 cas de test de données ajoutés ; build global réussi. Contrôleur/client inchangés, aucun test HTTP ni serveur exécuté |

**[Simulation ConfluxLab]** Ces routes sont pédagogiques, pas des endpoints historiques GFP/Open. Le code est expliqué comme une ébauche, jamais comme une démonstration fonctionnelle.

## 1. Objectif et vocabulaire

Le client veut demander une création de famille puis retrouver sa demande sans attendre la réalisation du métier. Le **BFF** adapte les échanges HTTP ; **Capability** porte l'acceptation, les règles métier et le stockage.

- Une **route** associe une méthode HTTP et un chemin à une méthode Java.
- Un **DTO** transporte des données ; il n'enregistre rien et n'exécute pas le métier à lui seul.
- Un **UUID** est ici un identifiant de corrélation permettant de retrouver une demande.
- `202 Accepted` signifie « demande acceptée », pas « famille créée ». `PENDING` désigne l'attente du traitement.
- Un **bean** est un objet créé et géré par Spring ; l'injection lui fournit ses dépendances.

## 2. Comprendre le problème

Un traitement long ne doit pas être présenté comme terminé dès la réponse initiale. Le contrat visé sépare **acceptation** et **consultation** : le POST retourne un identifiant et un lien ; le GET consulte ensuite l'état associé. Deux POST identiques peuvent encore produire deux demandes : UUID et `202` ne garantissent aucune idempotence.

L'ébauche suppose un aller-retour HTTP entre Experience et Capability. **Cette hypothèse est désormais abandonnée** : le monolithe modulaire retenu appelle l'API publique Java de Capability dans la même JVM. INT-03 porte ce remplacement et l'assemblage unique ; les explications réseau ci-dessous décrivent l'ancien mécanisme, pas l'architecture à reproduire.

## 3. Travail réalisé pas à pas

### 3.1. Recevoir une requête et injecter la dépendance

Extrait partiel exact de [FamilyController.java](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java), commenté après le bloc :

```java
@RestController
@RequestMapping("/api/v1")
public class FamilyController {

    private final CapabilityClient capabilityClient;

    public FamilyController(CapabilityClient capabilityClient) {
        this.capabilityClient = capabilityClient;
    }
```

`@RestController` déclare un contrôleur dont les valeurs de retour alimentent la réponse HTTP, avec conversion JSON selon les convertisseurs disponibles. `@RequestMapping` fixe le préfixe commun. Spring fournit le `CapabilityClient` au **constructeur** : le contrôleur ne le construit pas lui-même. Un constructeur unique n'exige pas `@Autowired` ; `final` empêche de réaffecter la référence après construction. Cela rend la dépendance explicite et permet aussi de fournir un double dans un test.

### 3.2. Lire le POST : corps, validation et réponse d'acceptation

Extrait partiel exact du même contrôleur :

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

`@PostMapping` complète le préfixe en `POST /api/v1/families`. `@RequestBody` demande de désérialiser le corps vers `CreateFamilyRequest`. `@Valid` déclenche la validation des contraintes du DTO lorsqu'elles existent ; il ne définit ni les champs ni leurs règles. **À l'époque de l'ébauche, les trois types importés étaient absents.** Ils sont désormais définis et testés dans [INT-05](INT-05-definir-les-contrats.md), avec le [schéma initial](../../docs/dat/http-contracts.md). Leur présence ne prouve pas l'exécution HTTP de la validation. Capability doit en outre protéger ses invariants même lorsqu'elle est appelée sans HTTP.

Le contrôleur attend le retour de `createFamily`, reprend son identifiant et son statut, puis reconstruit une URL publique relative. Il ne génère pas l'UUID et n'enregistre pas la demande. `ResponseEntity` permet de choisir statut, en-têtes et corps ; `accepted()` prépare `202`, `location(...)` renseigne `Location`, `body(...)` fixe le contenu. **`Location` est un lien de suivi, pas une redirection automatique ni une preuve de succès métier.** Le code évite ainsi de recopier une éventuelle adresse interne dans `statusUrl`.

### 3.3. Lire le GET et la conversion UUID

Extrait partiel exact du même contrôleur :

```java
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<RequestView> getRequest(@PathVariable("requestId") UUID requestId) {
        return ResponseEntity.ok(capabilityClient.getRequest(requestId));
    }
```

`@PathVariable` lie le segment du chemin au paramètre. Spring doit convertir le texte en `UUID` **avant** d'appeler la méthode : un format invalide relève d'un `400`, alors qu'un UUID syntaxiquement valide mais inconnu relève du suivi métier et du `404` attendu. `ok(...)` prépare `200`, sans démontrer que le suivi existe. Aucune de ces réponses n'a été observée à l'exécution pour cette ébauche.

### 3.4. Comprendre l'ancien appel réseau bloquant

Extrait partiel exact de [CapabilityClient.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityClient.java), dans `createFamily` :

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

Extrait partiel exact de [CapabilityProperties.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityProperties.java) :

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

Extrait partiel exact de [CapabilityClientConfiguration.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityClientConfiguration.java), dans le bean `capabilityRestClient` :

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

Contenu exact de [application.yml](../../experience-api/src/main/resources/application.yml) :

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

La syntaxe `${VARIABLE:valeur}` lit une variable d'environnement ou utilise le défaut. Les valeurs écrites sont donc 8080 pour Experience, `http://localhost:8081` pour l'ancienne cible, 2 s et 5 s pour les timeouts ; elles ne prouvent aucun service démarré. Les options `server.error` limitent les détails des erreurs serveur standard. L'HTTP interne et sa configuration appartiennent à l'ébauche à remplacer, pas à un second serveur à lancer.

### 3.6. Traduire les erreurs sans exposer les détails internes

[CapabilityException.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityException.java) est une `RuntimeException` qui transporte un `HttpStatus` via `status()` ; elle ne porte pas le corps reçu du service. Extrait partiel exact de [ApiExceptionHandler.java](../../experience-api/src/main/java/dev/confluxlab/experience/ApiExceptionHandler.java) :

```java
    @ExceptionHandler(CapabilityException.class)
    public ResponseEntity<ProblemDetail> handleCapability(CapabilityException exception) {
        return ResponseEntity.status(exception.status()).body(problem(exception.status()));
    }
```

`@RestControllerAdvice` centralise le traitement des exceptions des contrôleurs. `ProblemDetail` structure une erreur HTTP, notamment avec `status`, `title` et `detail`, plutôt que de renvoyer une trace Java. Le détail est choisi localement ; par exemple, le `400` reçoit exactement `Invalid request.`.

| Branche écrite dans le client/advice | Statut prévu, non observé ici |
|---|---|
| Réponse amont `400` ou `404` | Même statut, avec détail générique local |
| Autre réponse HTTP non attendue, corps absent/incohérent ou autre `RestClientException` | `502 Bad Gateway` |
| `ResourceAccessException` sans timeout reconnu | `503 Service Unavailable` |
| Cause `SocketTimeoutException` ou `HttpTimeoutException` trouvée dans la chaîne | `504 Gateway Timeout` |
| Exception inattendue traitée par l'advice | `500 Internal Server Error` |

La surcharge de `handleExceptionInternal` remplace aussi les corps d'erreurs du framework pour écarter valeurs rejetées et diagnostics de parsing. Ce choix réduit les fuites de données, mais ne démontre pas une sécurité exhaustive. Le mapping « upstream » décrit l'ancien réseau : il ne justifie pas de simuler des pannes réseau entre modules Java.

## 4. Commandes expliquées

**Racine de référence :** dossier `conflux-lab/` contenant le POM parent. Aucune commande de ce chapitre n'a été exécutée pour cette réécriture. Les sources ont été lues avec les outils de fichiers ; les commandes suivantes sont des **exercices de lecture non exécutés**, pas des résultats de validation. Les contrôles documentaires séparés vérifient les liens et les extraits, pas le fonctionnement du serveur.

```sh
sed -n '18,41p' experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java
cat experience-api/src/main/resources/application.yml
```

Avec `sed`/`cat` disponibles et depuis cette racine, la première affiche les lignes 18 à 41 du contrôleur ; la seconde affiche la configuration entière. Elles ne modifient rien, ne compilent pas et ne contactent aucun serveur. **Aucune requête HTTP n'a été exécutée** ; aucun appel `curl` n'est fourni comme démonstration fonctionnelle puisque l'application n'est pas assemblée. Les exemples de payload sont désormais disponibles dans le [contrat INT-05](../../docs/dat/http-contracts.md).

## 5. Vérifications et limites

La lecture documentaire a couvert le contrôleur, le client HTTP, sa configuration, ses propriétés, l'exception dédiée, le gestionnaire d'erreurs et le YAML, ainsi que les notes sources et le cadrage. L'inventaire initial ne montrait pas de DTO ; INT-05 a depuis ajouté cinq types et deux classes de tests dans `contracts`. Aucun service métier dans Capability ni test HTTP Experience n'a été ajouté ; lire leurs sources ne constitue pas une exécution du parcours.

L'ancienne note indiquait un parent Maven absent : ce constat était historique. Les POM et le Wrapper ont été réparés dans INT-02, puis INT-05 a défini les types. **Le `clean verify` global a maintenant réussi**, avec 59 cas de test de données et compilation d'Experience. La présente story d'acceptation n'a toujours ni service ni stockage implémenté et aucun runtime n'est validé.

Aucun enregistrement, UUID généré par Capability, suivi effectif en `PENDING`, MongoDB, événement ou traitement partenaire n'est démontré. Aucun `202`, `200`, `400` ou `404` n'a été observé sur un serveur pour cette ébauche. Les annotations et branches de code expliquent une intention, pas une acceptation réelle ; cette réécriture ne termine ni INT-06 ni INT-07/08 et n'autorise aucun développement.

## 6. Suivi centralisé

- [TD-B001 — Contrats et DTO INT-05](../todos.md#td-b001)
- [TD-B003 — Assemblage et appel Java INT-03](../todos.md#td-b003)
- [TD-B005 — Acceptation, suivi et refus INT-06/07/08](../todos.md#td-b005)
- [TD-R004 — 202 et UUID : ni succès métier ni idempotence](../todos.md#td-r004)
- [TD-R005 — DTO, @Valid et contrat : pas une preuve de fonctionnement](../todos.md#td-r005)
