# INT-06 — Accepter une demande

**Sprint :** [02 — Contrat REST](../../docs/stages/sprint-02-rest.md).  
**Type :** note rétrospective de l'ébauche de code antérieure à la planification finale.  
**Statut :** partiel, non testé, non exécutable avec les seuls fichiers présents. INT-06 n'est pas terminée ; cette note n'ouvre pas le sprint 02.

## Objectif et portée du rattachement

Tracer le code déjà amorcé pour l'API Experience, sans prétendre disposer d'une acceptation métier effective. La note couvre aussi les éléments de suivi et d'erreurs liés à INT-07/INT-08 ; elle ne signifie pas que ces tâches ont été réalisées ou autorisées dans le plan actuel.

**[Simulation ConfluxLab]** : ces endpoints et conventions sont pédagogiques ; ils ne décrivent pas les endpoints GFP/Open historiques.

## Code présent et mécanique prévue

| Fichier | Ce que le code exprime — non vérifié à l'exécution |
|---|---|
| [FamilyController.java](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java) | Routes sous `/api/v1`, POST `/families` avec `@Valid`, appel au client Capability, réponse `202` avec `Location` et `statusUrl` reconstruits ; GET `/requests/{requestId}` avec conversion UUID et réponse `200` |
| [CapabilityClient.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityClient.java) | Appels POST/GET via `RestClient`, statuts attendus `202`/`200`, contrôle du corps de réponse et de certains champs ; contrôles de concordance UUID et date sur le suivi |
| [CapabilityClientConfiguration.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityClientConfiguration.java) | `HttpClient` JDK, timeouts de connexion/lecture, redirections désactivées, URL de base injectée dans `RestClient` |
| [CapabilityProperties.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityProperties.java) | Propriétés `capability` : URL non vide et durées non nulles d'au moins 1 ms |
| [CapabilityException.java](../../experience-api/src/main/java/dev/confluxlab/experience/CapabilityException.java) | Exception portant un statut HTTP pour les erreurs de communication ou de réponse |
| [ApiExceptionHandler.java](../../experience-api/src/main/java/dev/confluxlab/experience/ApiExceptionHandler.java) | Réponses `ProblemDetail`, détails génériques pour éviter de renvoyer valeurs rejetées, diagnostics de parsing ou traces internes |
| [application.yml](../../experience-api/src/main/resources/application.yml) | Port 8080 ; URL Capability par défaut `http://localhost:8081` ; timeouts par défaut 2 s/5 s, surchargeables par variables d'environnement ; options d'erreurs serveur restrictives |

Le contrôleur reconstruit l'URL de suivi publique plutôt que de recopier une éventuelle adresse interne. Il ne génère pas le UUID et ne crée pas lui-même une famille. Le statut et l'identifiant doivent venir du composant qui accepte la demande.

### Correspondance d'erreurs écrite dans le client initial

- Réponse amont `400` ou `404` : même statut côté erreur BFF.
- Autre réponse non attendue ou corps incohérent : `502`.
- Erreur d'accès réseau : `503`, ou `504` si la chaîne de causes contient une exception de timeout reconnue par le code.
- Erreur inattendue traitée par l'advice : `500` avec détail générique.

Il s'agit d'une **lecture des branches du code**, pas d'une preuve que tous les cas sont correctement traduits à l'exécution. Aucun retry applicatif explicite n'est écrit dans ce client.

## Écart avec l'architecture retenue

Le `RestClient` Experience → Capability, son URL sur le port 8081 et les erreurs « upstream Capability » correspondent à l'hypothèse initiale de deux applications. Ils ne correspondent plus au monolithe modulaire retenu.

La cible est un **appel Java vers l'API publique de Capability**. Le BFF conserve son rôle HTTP, mais pas un réseau interne artificiel. Feign/Resilience4j reste réservé aux partenaires externes, dans une étape ultérieure. Ce remplacement n'est pas fait par la présente documentation.

## Ce qui manque

- Les DTO `CreateFamilyRequest`, `RequestAccepted` et `RequestView` importés depuis `contracts` ne sont pas présents. `@Valid` seul ne démontre donc aucune règle complète de validation du payload.
- Capability ne contient pas de service d'acceptation, génération UUID ou stockage en mémoire.
- Aucun suivi `PENDING` effectif, persistance MongoDB, événement, orchestration ou appel partenaire n'est implémenté.
- Aucun fichier de test n'est présent dans l'arborescence de tests Experience inspectée.
- Le parent Maven et le module `contracts` compilable sont absents ; voir la [note sur le socle](../sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md).

## Vérifications et limites

**Effectué :** lecture des sept classes Java Experience, du POM et de la configuration, puis inventaire des répertoires `contracts`, `capability-api` et des tests Experience. Les vérifications d'environnement sont consignées dans la note du socle.

**Non exécutés :** compilation, tests MockMvc/client HTTP, lancement du serveur, requêtes curl, échanges avec Capability ou parcours de bout en bout. Aucun `202`, `200`, `400` ou `404` n'a été observé sur une application en cours d'exécution. Ne pas confondre les annotations du contrôleur avec des endpoints démontrés fonctionnels.

## Suite — uniquement après feu vert

Achever d'abord le socle modulaire du sprint 01. Au sprint 02, définir les vrais contrats, brancher Capability en Java, implémenter l'acceptation et le suivi temporaires puis tester le contrat public et ses refus. Chaque tâche garde son feu vert individuel ; aucune n'est cochée par cette note.

## Point à retenir

Le `202` doit représenter une acceptation réelle de la demande, pas simplement une intention dans un contrôleur. Il ne signifie jamais que la famille est déjà créée ni que le traitement métier a réussi.
