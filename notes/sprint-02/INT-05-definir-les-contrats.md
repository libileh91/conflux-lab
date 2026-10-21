# INT-05 — Définir les contrats

| Contexte | Description |
|---|---|
| **Sprint** | [02 — Contrat REST](../../docs/stages/sprint-02-rest.md#int-05--définir-les-contrats), préparation avancée pour débloquer le build |
| **Statut** | Terminée : types, exemples et tests de données réalisés ; aucun service métier ni échange HTTP exécuté |
| **Autorisation** | « next step - go! », appliqué à INT-05, prochaine tâche identifiée dans le plan |
| **Sources des preuves** | Sources et fixtures créées dans `contracts`, tests JUnit et rapports Surefire de cette session |
| **Résultat** | 59 cas de test réussis ; `clean verify` global réussi ; blocage de compilation d'INT-02 levé |
| **Contrat de référence** | [Champs, exemples JSON et erreurs](../../docs/dat/http-contracts.md) |

**[Simulation ConfluxLab]** Les champs et limites ci-dessous sont des choix pédagogiques, pas des règles AXA ou des critères d'éligibilité d'assurance. L'application n'est pas encore assemblée.

## 1. Objectif et vocabulaire

Avant d'écrire le traitement d'une famille, les composants doivent comprendre les mêmes données. Un contrat décrit **ce que l'on envoie, ce que l'on reçoit et les valeurs acceptables**.

- Un **DTO** transporte ces données : il ne sauvegarde rien et n'appelle pas de partenaire.
- Un **record Java** déclare un petit type de données avec constructeur, accesseurs, `equals` et `hashCode` générés par Java.
- **Désérialiser** transforme du JSON en objet Java ; **sérialiser** fait le chemin inverse.
- **Bean Validation** examine les contraintes déclarées sur les objets. Ce n'est pas la même opération que lire du JSON.
- Une **fixture** est un exemple stable de données utilisé par les tests.

Ici, le rôle de `contracts` est de fournir des types Java partagés, sans Spring MVC, repository ou modèle MongoDB. Les autres modules peuvent utiliser ces types sans devenir propriétaires du stockage ou du métier.

### Pourquoi des records pour ces DTO

Nos DTO servent surtout à **transporter des données**, sans exécuter le métier. Le `record` convient à ce rôle pour quatre raisons :

- **Moins de code répétitif** : Java génère le constructeur, les accesseurs, `equals()`, `hashCode()` et `toString()`.
- **Intention claire** : `PersonPayload` décrit des données, pas un service ou un objet de persistance.
- **Pas de setters générés** : les champs sont finaux et ne peuvent pas être réaffectés après construction, ce qui limite les modifications accidentelles.
- **Compatibilité avec notre stack** : Jackson sait convertir ces records en JSON et inversement ; les tests d'INT-05 vérifient cette conversion.

On accède ainsi au prénom avec `person.firstName()`, pas `person.getFirstName()`.

Deux limites restent importantes : **l'immuabilité est de surface** — la liste `dependents` peut encore être modifiée — et **la construction ne déclenche pas automatiquement Bean Validation**. Les annotations comme `@NotNull` nécessitent un validateur, appelé explicitement ou via une intégration comme celle de Spring.

Une classe classique serait préférable si nous avions besoin d'un état modifiable par setters ou d'héritage de classe. Ce n'est pas nécessaire pour ces DTO. Le repère de synthèse est centralisé dans [TD-R009](../todos.md#td-r009).

#### Exemple réel avec Jackson

Voici une méthode existante de [ContractJsonTest.java](../../contracts/src/test/java/dev/confluxlab/contracts/ContractJsonTest.java), déjà exécutée avec succès dans INT-05. C'est un **extrait de la classe de test**, pas une classe autonome : `MAPPER` est son `ObjectMapper`, `readFixture` charge le JSON de test et les constantes reprennent l'UUID et l'URL de cet exemple.

```java
    @Test
    void requestAcceptedFixtureMatchesDtoInBothDirections() throws IOException {
        var expected = new RequestAccepted(REQUEST_ID, RequestStatus.PENDING, STATUS_URL);
        JsonNode fixture = readFixture("request-accepted.json");

        assertEquals(expected, MAPPER.treeToValue(fixture, RequestAccepted.class));
        assertEquals(fixture, MAPPER.readTree(MAPPER.writeValueAsString(expected)));
        assertAll(
                () -> assertEquals(REQUEST_ID.toString(), fixture.path("requestId").textValue()),
                () -> assertEquals("PENDING", fixture.path("status").textValue()),
                () -> assertEquals(STATUS_URL, fixture.path("statusUrl").textValue()));
    }
```

- **JSON → record** : `treeToValue` construit un `RequestAccepted` depuis le JSON. Le premier `assertEquals` vérifie que ses composants correspondent à ceux du record attendu, sans setter ni constructeur sans argument.
- **Record → JSON** : `writeValueAsString` sérialise le record, puis `readTree` relit le résultat. Le deuxième `assertEquals` compare les arbres JSON : même contenu attendu, sans dépendre de l'ordre des propriétés.
- **Valeurs de référence** : `assertAll` vérifie aussi l'UUID textuel, le nom du statut et l'URL de la fixture.

La [fixture utilisée](../../contracts/src/test/resources/contracts/request-accepted.json) et la configuration de `MAPPER` sont présentées plus bas dans le cours. Ce test prouve la conversion Jackson pour ce DTO et cette configuration ; il ne démarre pas de serveur HTTP et ne lance pas Bean Validation. Aucun nouveau test n'est créé ou relancé pour l'ajout de cet extrait documentaire.

## 2. Comprendre le problème

L'ébauche Experience importait `CreateFamilyRequest`, `RequestAccepted` et `RequestView`, mais aucune de ces classes n'existait. Maven savait résoudre les POM ; le compilateur ne pouvait pas résoudre les imports Java.

Le correctif ne consiste pas à créer trois classes vides. Il faut choisir des champs cohérents avec le cas d'usage, définir leur représentation JSON et vérifier les données. Deux types supplémentaires rendent ce modèle explicite : `PersonPayload` pour une personne et `RequestStatus` pour les quatre états.

| Type | Rôle |
|---|---|
| `PersonPayload` | Décrire une personne fictive |
| `CreateFamilyRequest` | Transporter un principal et deux ayants droit |
| `RequestStatus` | Nommer `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED` |
| `RequestAccepted` | Décrire l'accusé d'acceptation et le lien de suivi |
| `RequestView` | Décrire les métadonnées du suivi : identifiant, état et date de création |

Les éventuels résultats détaillés ou erreurs métier ne sont pas inventés dans ce premier DTO de suivi. Le [contrat initial](../../docs/dat/http-contracts.md) distingue cette étape de la cible complète du DAT.

## 3. Travail réalisé pas à pas

### 3.1. Décrire une personne avec des contraintes simples

Extrait de [PersonPayload.java](../../contracts/src/main/java/dev/confluxlab/contracts/PersonPayload.java), imports omis :

```java
public record PersonPayload(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @PastOrPresent LocalDate birthDate) {
}
```

`firstName()` est l'accesseur du prénom : un record ne génère pas ici une méthode `getFirstName()`. Les annotations expriment des conditions, pas des traitements exécutés dans le constructeur :

| Annotation | Effet lors d'une validation |
|---|---|
| `@NotBlank` | Refuse `null`, une chaîne vide ou uniquement blanche |
| `@Size(max = 100)` | Limite la longueur du nom ; ne remplace pas la contrainte de présence |
| `@NotNull` | Exige une date fournie |
| `@PastOrPresent` | Autorise une date passée ou le jour courant, pas une date future |

La limite 100 utilise la longueur Java UTF-16, pas un comptage des caractères affichés. Aucun nom n'est limité à l'ASCII ; aucun espace n'est automatiquement supprimé. L'horloge du validateur définit « aujourd'hui ». On ne décide pas ici de l'âge légal du principal, de l'identité des personnes ou d'une éligibilité métier.

### 3.2. Valider aussi les objets contenus dans la famille

Extrait de [CreateFamilyRequest.java](../../contracts/src/main/java/dev/confluxlab/contracts/CreateFamilyRequest.java), imports omis :

```java
public record CreateFamilyRequest(
        @NotNull @Valid PersonPayload principal,
        @NotNull @Size(min = 2, max = 2) List<@NotNull @Valid PersonPayload> dependents) {
}
```

Trois niveaux différents sont protégés :

1. `@NotNull` sur `principal` exige une personne, puis `@Valid` examine ses champs.
2. `@NotNull` sur la liste exige sa présence ; `@Size` exige exactement deux éléments.
3. Les annotations dans `List<...>` refusent un élément `null` et déclenchent la validation de chaque personne.

`@Valid` seul ne refuse pas une valeur `null`. Sans validation en cascade, une famille pourrait avoir la bonne taille mais contenir un prénom blanc ou une date future non contrôlés.

**Un record n'est pas profondément immuable.** Sa référence `dependents` est finale, mais la liste reçue peut rester mutable : aucun constructeur de copie n'a été ajouté. Ce DTO n'est donc pas un mécanisme de synchronisation ou de protection contre les doublons.

### 3.3. Donner un format clair aux réponses

Les deux extraits suivants viennent de fichiers distincts ; ce n'est pas une unité Java à copier telle quelle :

[RequestAccepted.java](../../contracts/src/main/java/dev/confluxlab/contracts/RequestAccepted.java), imports omis :

```java
public record RequestAccepted(
        @NotNull UUID requestId,
        @NotNull RequestStatus status,
        @NotBlank String statusUrl) {
}
```

[RequestView.java](../../contracts/src/main/java/dev/confluxlab/contracts/RequestView.java), imports omis :

```java
public record RequestView(
        @NotNull UUID requestId,
        @NotNull RequestStatus status,
        @NotNull Instant createdAt) {
}
```

`UUID` évite de traiter l'identifiant comme un texte quelconque dans le code ; `Instant` représente un instant, illustré en UTC dans le JSON. L'[enum](../../contracts/src/main/java/dev/confluxlab/contracts/RequestStatus.java) limite le statut aux quatre noms prévus.

Les contraintes des réponses sont vérifiables par `Validator`, mais ne valident pas automatiquement un retour HTTP du contrôleur actuel. `@NotBlank` ne prouve pas non plus que `statusUrl` correspond au bon UUID. Le contrat impose ce lien ; le service et les tests HTTP futurs devront le démontrer.

### 3.4. Relier les types aux exemples JSON

La [fixture d'acceptation](../../contracts/src/test/resources/contracts/request-accepted.json) contient :

```json
{
  "requestId": "e91aa057-68f9-4d38-9d24-e8eb7f747630",
  "status": "PENDING",
  "statusUrl": "/api/v1/requests/e91aa057-68f9-4d38-9d24-e8eb7f747630"
}
```

Ce UUID est une valeur fictive stable, pas un identifiant créé par un service pendant le test. L'exemple représente l'état à l'acceptation, pas une garantie que le traitement serait encore en attente lorsque le client reçoit le `202`.

Les autres fixtures décrivent [la famille d'entrée](../../contracts/src/test/resources/contracts/create-family.json) et [le suivi](../../contracts/src/test/resources/contracts/request-view.json). Leurs JSON sont repris dans la [spécification HTTP](../../docs/dat/http-contracts.md), avec des exemples de refus `400` et `404`. Aucun de ces exemples n'est présenté comme une requête réellement servie.

### 3.5. Tester sans démarrer Spring Boot

Le [POM contracts](../../contracts/pom.xml) ajoute uniquement l'API Jakarta Validation en dépendance de production. JUnit, Hibernate Validator et Jackson sont en portée `test`. Il n'y a ni contrôleur ni dépendance vers Spring MVC dans ce module.

- Jakarta fournit les annotations et l'interface `Validator`.
- Hibernate Validator évalue les contraintes dans les tests.
- `ParameterMessageInterpolator` permet ces tests sans moteur d'expressions EL ; les messages utilisés n'en ont pas besoin. Le futur contexte Spring n'est pas lancé ici.
- Jackson et `JavaTimeModule` permettent les tests JSON des records et des dates, sans ajouter de dépendance JSON de production à cette bibliothèque.

Extrait du [test de validation](../../contracts/src/test/java/dev/confluxlab/contracts/ContractValidationTest.java) :

```java
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);
```

Le validateur du test reçoit cette horloge via `clockProvider`. Une naissance au 22 octobre est ainsi future par rapport au 21 octobre **du test**, même si l'on relance la suite plusieurs mois plus tard. On ne change pas l'horloge de production ni celle de l'ordinateur.

Extrait du [test JSON](../../contracts/src/test/java/dev/confluxlab/contracts/ContractJsonTest.java) :

```java
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

Le module gère `LocalDate` et `Instant` ; l'option produit des dates textuelles ISO plutôt que des timestamps. Les tests comparent des arbres JSON, pas un ordre arbitraire de propriétés.

Un test montre aussi que Jackson peut construire un objet aux données invalides sans lancer Bean Validation. Lire le JSON et vérifier les annotations sont donc deux étapes différentes. Le BFF aura son intégration `@Valid` et Capability devra protéger ses invariants ; leur exécution réelle n'est pas prouvée par ces tests isolés.

## 4. Commandes expliquées

**Commandes exécutées dans cette session, depuis la racine `conflux-lab/`, avec le JDK 21.** Elles n'exigent ni MongoDB, ni Docker, ni partenaire réseau. Un accès à Maven Central peut être nécessaire pour des dépendances absentes du cache.

```sh
# Vérifier d'abord le module modifié et ses prérequis.
./mvnw -B -ntp -pl contracts -am test

# Reconstruire ensuite tout le projet, Experience compris.
./mvnw -B -ntp clean verify
```

| Élément | Explication |
|---|---|
| `./mvnw` | Utilise le Maven épinglé dans le dépôt |
| `-B -ntp` | Mode sans interaction et sans affichage de progression des transferts |
| `-pl contracts -am` | Sélectionne `contracts` et ses prérequis du réacteur ; ce premier contrôle ne compile pas Experience |
| `test` | Compile les sources et lance les tests unitaires de la sélection |
| `clean` | Supprime les sorties de build `target/` concernées, pas les sources ni tout le dépôt Maven local |
| `verify` sans `-pl` | Compile et vérifie tous les modules du réacteur ; ne signifie pas « démarrer l'application » |

Le premier contrôle ciblé et le second global ont réussi. `clean` a aussi supprimé le cache de démonstration du Wrapper placé précédemment dans le `target/` racine : c'était une sortie temporaire, pas une source utilisateur.

## 5. Vérifications et limites

### 5.1. Résultats effectivement observés

| Contrôle | Résultat |
|---|---|
| `ContractValidationTest` | 43 cas : valeurs obligatoires, cardinalités, cascade, noms, dates à horloge fixe et réponses |
| `ContractJsonTest` | 16 cas : trois fixtures aller-retour, formats ISO/UUID/enums, refus de formats invalides, désérialisation distincte de la validation |
| Total Surefire | **59 cas, 0 échec, 0 erreur, 0 ignoré** ; les paramètres d'un test comptent comme plusieurs cas, pas nécessairement plusieurs méthodes |
| Réacteur global | Parent, `contracts`, `capability-api` et `experience-api` construits avec succès après nettoyage |
| Inspection des trois JAR | Bibliothèques ordinaires sans `BOOT-INF/` ; pas de JAR Boot assemblé |

Les rapports sont générés dans `contracts/target/surefire-reports/` et ne sont pas versionnés. Les fichiers XML ont été relus pour confirmer les nombres. Experience compile désormais ses sept sources, sans modification de son contrôleur ou de son client HTTP.

### 5.2. Ce que ce succès ne prouve pas

Aucun test HTTP, enregistrement, génération d'UUID, appel Java vers un service Capability, événement ou orchestration n'a été exécuté. L'ancien client HTTP interne existe toujours ; INT-03 reste à autoriser. Aucun test d'intégration, démarrage serveur, CI verte, déploiement ou validation Windows n'est revendiqué.

Les vérifications `400`/`404` de la spécification restent contractuelles ; le test Jackson d'un format invalide ne constitue pas un test du statut HTTP renvoyé par Spring. Les règles d'identité, de transitions et d'éligibilité métier ne sont pas inventées dans ces DTO.

INT-05 est terminée et le contrôle global attendu par INT-02 est désormais réussi. Cela ne termine ni INT-03, ni INT-06, ni un sprint entier. Aucun commit ou push n'est effectué par cette tâche.

## 6. Suivi centralisé

- [TD-B001 — Types et contrat définis](../todos.md#td-b001).
- [TD-B002 — Build global réévalué](../todos.md#td-b002).
- [TD-B003 — Assemblage et appel Java : feu vert encore requis](../todos.md#td-b003).
- [TD-B005 — Parcours REST : toujours non démontré](../todos.md#td-b005).
- [TD-R004 — Acceptation et corrélation ne sont pas une réussite métier](../todos.md#td-r004).
- [TD-R005 — DTO, désérialisation et validation explicite](../todos.md#td-r005).
- [TD-R008 — Dates et tests déterministes](../todos.md#td-r008).
- [TD-R009 — Pourquoi choisir un record pour un DTO](../todos.md#td-r009).
