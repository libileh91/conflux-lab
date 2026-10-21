# INT-05 — Contrats HTTP initiaux

**[Simulation ConfluxLab] — étape initiale INT-05.** Ce document fixe le contrat pédagogique de création et de suivi d'une demande fictive, dans le contexte Java 21 / Spring Boot 3.5.7.
Les termes « doit » et « ne doit pas » décrivent les obligations du contrat, pas des comportements déjà démontrés.
**Tous les échanges HTTP ci-dessous sont des exemples contractuels, jamais des réponses observées.**

## 1. Périmètre et responsabilités

Le [DAT de référence](ConfluxLab.md) conserve la cible globale ; cette page précise uniquement le contrat initial de [INT-05 — Définir les contrats](../stages/sprint-02-rest.md#int-05--définir-les-contrats).
Experience est un **BFF mince** : réception HTTP, validation de forme et adaptation des réponses. Capability porte le métier, les invariants, les états et le stockage.
La cible est un **monolithe modulaire** : une application, avec de futurs appels Java via l'API publique de Capability, sans HTTP interne.
Le client HTTP interne de l'ébauche Experience n'est pas le modèle à reproduire ; son remplacement et l'assemblage restent hors de cette documentation.
INT-05 ne réalise ni service d'acceptation, ni génération d'UUID, ni stockage, ni asynchronisme, ni test HTTP, ni assemblage applicatif.
`contracts` est une bibliothèque de DTO (objets de transport) : Java et API Jakarta Validation seulement en production, sans contrôleur Spring ni dépendance OpenAPI.
Jackson et son support des dates Java sont des dépendances de test de ce module ; leur usage vérifie la représentation JSON, pas les règles métier.

## 2. Champs et contraintes des DTO

### Entrée : une personne et une famille

| Type / champ | Type Java | Contraintes déclarées et sens |
|---|---|---|
| [PersonPayload](../../contracts/src/main/java/dev/confluxlab/contracts/PersonPayload.java).`firstName` | `String` | `@NotBlank @Size(max = 100)` : obligatoire, non vide, pas uniquement blanc, longueur maximale 100 |
| `PersonPayload.lastName` | `String` | Mêmes contraintes que `firstName` |
| `PersonPayload.birthDate` | `LocalDate` | `@NotNull @PastOrPresent` : date obligatoire, passée ou égale à aujourd'hui |
| [CreateFamilyRequest](../../contracts/src/main/java/dev/confluxlab/contracts/CreateFamilyRequest.java).`principal` | `PersonPayload` | `@NotNull @Valid` : objet obligatoire et validation de ses champs |
| `CreateFamilyRequest.dependents` | `List<PersonPayload>` | `@NotNull @Size(min = 2, max = 2)` : liste obligatoire contenant exactement deux éléments |
| Chaque élément de `dependents` | `PersonPayload` | `@NotNull @Valid` sur le type d'élément : aucune personne `null`, validation de chaque personne |

La longueur des noms est celle de `String.length()` : **unités de code UTF-16 Java**, pas nécessairement caractères visibles. Aucune regex ASCII, normalisation ou suppression d'espaces n'est imposée.
`birthDate` est illustrée en JSON par une date ISO `YYYY-MM-DD`, sans heure. « Aujourd'hui » dépend de l'horloge et du fuseau du `ClockProvider` de validation ; la date du jour est autorisée.
`@NotNull` interdit l'absence de valeur ; `@Valid` déclenche la validation en cascade d'un objet présent et **n'interdit pas `null` à lui seul**. `@Size` ne remplace pas `@NotNull`.

### Réponses : acceptation et métadonnées de suivi

| Type / champ | Type Java | Contraintes déclarées |
|---|---|---|
| [RequestAccepted](../../contracts/src/main/java/dev/confluxlab/contracts/RequestAccepted.java).`requestId` | `UUID` | `@NotNull` |
| `RequestAccepted.status` | `RequestStatus` | `@NotNull` |
| `RequestAccepted.statusUrl` | `String` | `@NotBlank` ; le contrat HTTP ci-dessous exige en plus un lien de suivi cohérent |
| [RequestView](../../contracts/src/main/java/dev/confluxlab/contracts/RequestView.java).`requestId` | `UUID` | `@NotNull` |
| `RequestView.status` | `RequestStatus` | `@NotNull` |
| `RequestView.createdAt` | `Instant` | `@NotNull` ; instant de création de la demande, représenté en ISO-8601 UTC dans l'exemple |

Un constructeur de record et Jackson **ne valident pas automatiquement** ces annotations : un appel explicite à `Validator` ou une intégration de validation est nécessaire.
Le `@Valid @RequestBody` du contrôleur exprime cette validation à l'entrée HTTP ; cela ne prouve pas son exécution dans une application assemblée.
Les annotations des réponses ne déclenchent pas automatiquement la validation des retours des endpoints actuels. Elles ne garantissent pas à elles seules la cohérence des liens ou l'état initial.
Un record rend ses références finales, **pas une liste profondément immuable** : ces records ne font pas de copie défensive de `dependents`.

## 3. POST : demander une création

`POST /api/v1/families`, avec `Content-Type: application/json`. Corps exact de la [fixture create-family.json](../../contracts/src/test/resources/contracts/create-family.json) :

```json
{
  "principal": {
    "firstName": "Alex",
    "lastName": "Exemple",
    "birthDate": "1990-04-12"
  },
  "dependents": [
    {
      "firstName": "Camille",
      "lastName": "Exemple",
      "birthDate": "2015-08-21"
    },
    {
      "firstName": "Noa",
      "lastName": "Exemple",
      "birthDate": "2018-02-03"
    }
  ]
}
```

Après une acceptation effectivement réalisée, la réponse initiale attendue est :

```http
HTTP/1.1 202 Accepted
Content-Type: application/json
Location: /api/v1/requests/e91aa057-68f9-4d38-9d24-e8eb7f747630
```

Corps exact de la [fixture request-accepted.json](../../contracts/src/test/resources/contracts/request-accepted.json) :

```json
{
  "requestId": "e91aa057-68f9-4d38-9d24-e8eb7f747630",
  "status": "PENDING",
  "statusUrl": "/api/v1/requests/e91aa057-68f9-4d38-9d24-e8eb7f747630"
}
```

`statusUrl` et `Location` doivent désigner le même suivi public relatif et le même UUID ; le contrôleur existant les construit à partir de `requestId`. `Location` n'est pas une redirection automatique.
`202` signifie **demande acceptée, pas famille créée**. Il ne prouve ni un traitement terminé ni une conservation durable.
L'UUID fourni ici est une donnée de fixture, pas un identifiant généré pendant cette rédaction. Il sert à la corrélation, **pas à l'idempotence** : deux POST identiques peuvent produire deux demandes.

## 4. GET : consulter les métadonnées

`GET /api/v1/requests/e91aa057-68f9-4d38-9d24-e8eb7f747630` doit retrouver le même identifiant après acceptation ; réponse contractuelle `200 OK`, `Content-Type: application/json`.
Corps exact de la [fixture request-view.json](../../contracts/src/test/resources/contracts/request-view.json) :

```json
{
  "requestId": "e91aa057-68f9-4d38-9d24-e8eb7f747630",
  "status": "PENDING",
  "createdAt": "2026-10-21T12:00:00Z"
}
```

`RequestView` initial contient **uniquement des métadonnées** : ni résultat, ni erreur métier, ni identifiant de famille créée. Aucun DTO supplémentaire de succès ou d'échec n'est défini ici.
La cible globale du DAT pourra enrichir le suivi plus tard ; elle ne doit pas être présentée comme déjà couverte par ce DTO.

| Valeur exacte de [RequestStatus](../../contracts/src/main/java/dev/confluxlab/contracts/RequestStatus.java) | Sens contractuel |
|---|---|
| `PENDING` | Demande acceptée, en attente de traitement |
| `PROCESSING` | Traitement en cours |
| `SUCCEEDED` | Traitement métier terminé avec succès |
| `FAILED` | Traitement terminé en échec |

L'énumération contient exactement ces quatre valeurs. Les annotations des deux réponses les autorisent toutes ; elles n'imposent ni `PENDING` initial ni transitions d'état.
Ces transitions et leur mise en œuvre appartiennent à Capability, ultérieurement. Un suivi futur `FAILED` reste distinct d'une erreur HTTP empêchant de consulter la demande.

## 5. Refus et erreurs HTTP

| Situation | Réponse contractuelle attendue |
|---|---|
| Forme invalide : champ obligatoire, nom blanc, cardinalité incorrecte, date future | `400 Bad Request` |
| JSON illisible, date mal formée ou impossible, paramètre UUID non convertible | `400 Bad Request` |
| UUID syntaxiquement valide mais demande inconnue lors du GET | `404 Not Found` |
| Indisponibilité ou échec de la future persistance empêchant l'acceptation | Erreur `5xx` adaptée, **jamais un faux `202`** |

Exemples `Content-Type: application/problem+json`, alignés sur les détails génériques de l'advice existant :

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request."
}
```

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Request not found."
}
```

Ces objets illustrent les champs utiles de `ProblemDetail`, **pas une liste exclusive de champs** : le framework peut notamment ajouter `instance`. Ne pas exposer valeurs rejetées, données personnelles, traces ou diagnostics de parseur.
Avant l'acceptation et l'envoi du `202`, un refus doit être communiqué en HTTP, sans prétendre qu'une prise en charge a réussi.
Après le `202`, un échec du traitement futur ne peut pas réécrire la réponse déjà envoyée : il doit être consultable dans le suivi ultérieur, dont le détail métier reste à définir.
Le code `5xx` précis d'une panne de persistance n'est pas fixé ici ; les messages de l'ancien client réseau ne constituent pas une politique MongoDB.

## 6. Exemples invalides et frontière métier

À partir du corps POST valide, appliquer **une seule modification par ligne**, en laissant le reste inchangé. Référence temporelle de l'exemple : horloge de validation fixée au `2026-10-21T12:00:00Z`, fuseau UTC.

| Modification du JSON | Cause du refus attendu |
|---|---|
| `principal` devient `null` | `@NotNull` sur le principal |
| `dependents` devient `[{"firstName":"Camille","lastName":"Exemple","birthDate":"2015-08-21"}]` | Un seul ayant droit au lieu de deux : `@Size` |
| `principal.firstName` devient `" "` | Nom uniquement blanc : `@NotBlank` |
| `dependents[1].birthDate` devient `"2026-10-22"` | Date postérieure au jour de l'horloge fixée : `@PastOrPresent` |

Cette dernière date n'est « future » que relativement à la référence indiquée ; un test relatif doit calculer la date depuis son horloge de validation, pas depuis un « demain » codé en dur.
La validation des DTO contrôle la **forme contractuelle**. Elle ne remplace pas les invariants de Capability : identités, réglages métier et autres décisions du cas d'usage restent à définir, sans règle inventée ici.
Aucune politique HTTP stricte de coercition des types ou de rejet des champs inconnus n'est annoncée : elle n'a pas été vérifiée par un test HTTP.

## 7. Sources utiles et points non vérifiés

- [FamilyController.java](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java) et [ApiExceptionHandler.java](../../experience-api/src/main/java/dev/confluxlab/experience/ApiExceptionHandler.java) : sources lues pour aligner routes, liens et erreurs, sans ajout de contrôleur.
- [ContractJsonTest.java](../../contracts/src/test/java/dev/confluxlab/contracts/ContractJsonTest.java) et [ContractValidationTest.java](../../contracts/src/test/java/dev/confluxlab/contracts/ContractValidationTest.java) : 59 cas de test réussis lors d'INT-05 ; les [commandes et preuves](../../notes/sprint-02/INT-05-definir-les-contrats.md#5-vérifications-et-limites) portent sur les données et la compilation, pas sur un parcours HTTP.
- [INT-05](../stages/sprint-02-rest.md#int-05--définir-les-contrats) et [registre central](../../notes/todos.md) : critères et suivi existants, sans nouvelle liste de TODO ni changement de statut.

**Validation INT-05 :** tests ciblés puis `./mvnw -B -ntp clean verify` global réussis. **Non vérifiés :** démarrage, échanges HTTP, acceptation réelle, stockage, traitement et transitions d'état. Le succès des tests DTO ne constitue aucune preuve de ces comportements.
