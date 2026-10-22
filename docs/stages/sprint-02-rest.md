# Sprint 02 — Accepter et consulter une demande

**[Simulation ConfluxLab] — INT-05 à INT-08 regroupées en un résultat technique réalisé.** Les développements et preuves historiques ont été obtenus sur leurs feux verts individuels. Ce regroupement documentaire ne crée ni nouvelle exécution, ni validation pédagogique : la restitution orale de l'apprenant reste non évaluée.

> **Version à retenir :** ce sprint décrit l'acceptation et le suivi **en mémoire au stade 02**, perdus au redémarrage. Depuis INT-10, la production du laboratoire utilise MongoDB ; depuis INT-14/15, elle publie un événement local et tente une prise en charge asynchrone. Ne pas lire les anciennes limites comme l'état actuel du code.

## Story et résultat attendu

En tant que client de l'API, je veux faire accepter une demande de création de famille, obtenir son identifiant puis consulter son état, afin de la suivre sans attendre ni supposer que la famille est déjà créée.

**Résultat livré au terme du stade 02 :** POST valide → validation → appel Java à Capability → conservation d'un snapshot `PENDING` avec UUID et date → `202` avec liens cohérents ; GET → lecture des seules métadonnées ; refus explicites sans faux reçu ni données rejetées dans les réponses testées.

**Prérequis historiques :** contrats INT-05 et socle INT-02/03/04 assemblé et vérifié pour le parcours HTTP. INT-05 avait été préparée après INT-01 pour débloquer la compilation d'INT-02, avant l'assemblage INT-03. Le regroupement ne réécrit pas cet ordre de réalisation.

## Périmètre réalisé ensemble

- Définir cinq types partagés et leurs contraintes : une famille fictive composée d'un principal et de deux ayants droit, reçu et vue de suivi.
- Garder le **BFF mince** : contrat HTTP, désérialisation/validation MVC, liens publics et adaptation des erreurs. Capability protège aussi les appels Java directs et possède états et stockage.
- Conserver avant le reçu : copie du payload, UUID de corrélation, `PENDING`, `createdAt`, insertion sans écrasement ; pas d'acquittement artificiel si l'enregistrement échoue.
- Exposer `POST /api/v1/families` et `GET /api/v1/requests/{requestId}` dans la même application, sans HTTP interne ni accès du BFF au store.
- Lire sans modifier : `requestId`, `status`, `createdAt` seulement ; distinguer entrée invalide, absence et panne. Protéger les réponses contre l'écho de données sensibles, notamment via `ProblemDetail.instance`.

**Cours commun : [INT-05 à INT-08 — Accepter et consulter une demande](../../notes/sprint-02/INT-05-accepter-et-consulter-une-demande.md)** — flux, fragments réels, tableaux DTO/HTTP/annotations, preuves et différence avec MongoDB/async actuels. Références : [DAT](../dat/ConfluxLab.md) et [contrat HTTP initial](../dat/http-contracts.md).

## Traçabilité des IDs et des preuves historiques

Les IDs restent stables ; les lignes ci-dessous sont les contributions au même résultat, pas quatre nouvelles tâches à autoriser.

| ID conservé | Contribution au résultat | Preuve rapportée, non réexécutée ici |
|---|---|---|
| **INT-05 — Définir les contrats** | DTO, contraintes et exemples ; compilation débloquée | 59 cas de données/JSON et `clean verify` réussis, sans serveur ni test HTTP à cette étape |
| **INT-06 — Accepter une demande** | UUID, snapshot mémoire et `PENDING` conservés avant `202` | 96 tests globaux ; JAR : deux POST `202` distincts, entrée invalide `400` ; **GET encore `503`**, pas encore une recherche |
| **INT-07 — Consulter la demande** | Projection de trois métadonnées en lecture seule ; absence et format distingués | 108 tests globaux ; JAR : deux POST/GET `200 PENDING`, dates stables, `404`/`400`, puis ancien UUID `404` après redémarrage |
| **INT-08 — Tester les refus** | Refus HTTP/Java sans enregistrement, erreurs confidentielles et isolation ; correction de l'écho via `instance` | 93 cas ciblés finaux, puis 181 tests globaux ; JAR : refus `400`/`415`/`404` sans marqueur ni `instance`, vues de deux demandes stables ; `500` simulés par doublures |

Ces totaux sont les états successifs consignés dans les notes, **pas des nombres à additionner ni des exécutions de cette réorganisation**. Le cours commun conserve le contexte des passages ciblés et des échecs ayant révélé le défaut de confidentialité, sans recopier les matrices de tests.

## Critères observables — réalisation historique

- [x] Les cinq types et le contrat décrivent les champs, contraintes minimales et réponses, sans données réelles ni règle d'assurance inventée.
- [x] Le vrai parcours POST retourne `202`, un UUID et `PENDING`, avec `Location` et `statusUrl` cohérents ; les tests internes prouvent la conservation préalable du payload et l'absence de reçu si le store échoue.
- [x] Le GET réalisé depuis INT-07 retrouve l'UUID accepté en `200`, sans exposer le payload ; les GET répétés conservent `createdAt` et ne déclenchent aucune écriture ou traitement.
- [x] UUID absent : `404` ; identifiants manifestement mal formés testés : `400` ; panne de lecture simulée : `500`, pas faux `404`.
- [x] Les refus MVC n'appellent pas l'acceptation ; les refus Java ne touchent pas le store. Un cas valide à la borne de 100 caractères reste accepté.
- [x] Les erreurs testées sont génériques, sans valeurs rejetées, traces, causes ou champs de succès ; le défaut d'écho du chemin dans `instance` est reproduit puis corrigé, sur l'objet Java et sa représentation HTTP.
- [x] Deux demandes restent distinctes, malgré un refus intercalé et dans les essais Java concurrents bornés ; deux POST identiques ne sont pas présentés comme idempotents.
- [x] La perte mémoire après redémarrage du JAR est démontrée ; les processus de démonstration sont arrêtés et les frontières modulaires restent vérifiées.

**Apprentissage : non évalué à l'oral.** Les preuves techniques et le passage demandé à la suite ne valident pas automatiquement la compréhension de l'apprenant. Aucun questionnaire ni nouveau travail pédagogique n'est imposé par cette réécriture.

## Limites de ce stade et évolution actuelle

Au stade 02, la map est volatile, non bornée et sans nettoyage ; les demandes produites restent `PENDING`. Aucun événement, `@Async`, partenaire ou traitement de création de famille n'y est livré. `202` signifie acceptation, pas famille créée ni durabilité. L'UUID sert à la corrélation, pas à l'idempotence ou au contrôle d'accès.

Les refus et la confidentialité sont prouvés pour les chemins exercés, pas pour toutes les combinaisons, coercitions JSON, champs inconnus ou erreurs hors de l'advice. La référence URI vide empêche MVC de recopier le chemin ; son omission du JSON dépend de la sérialisation observée. Ce n'est pas un audit complet de sécurité ou des logs.

**Aujourd'hui, le code a évolué :** insertion MongoDB avant le reçu, événement local puis tentative atomique vers `PROCESSING` sur executor borné. Le listener peut démarrer avant le retour du `202` ; il n'est pas encore raccordé au graphe d'orchestration ni aux appels partenaires. Les preuves de persistance après redémarrage relèvent d'INT-12, pas de ce sprint. Une erreur d'écriture ou de soumission actuelle ne prouve pas nécessairement l'absence de document. Voir les [évolutions situées dans le cours commun](../../notes/sprint-02/INT-05-accepter-et-consulter-une-demande.md#8-ne-pas-confondre-ce-stade-avec-le-flux-actuel).

## Bilan technique historique et passage à la suite — 2026-10-21

| Élément | Constat conservé |
|---|---|
| Résultat | INT-05 à INT-08 réalisées techniquement : contrat, acceptation, consultation et refus au stade mémoire |
| Dernier passage global de ce stade | `clean verify` INT-08 : 181 tests réussis ; démonstration du JAR réel décrite dans le cours commun |
| Contrôle avant commit | 1 test ciblé de confidentialité réussi ; suite globale et démonstration non relancées |
| Référence Git | Commit INT-08 local `fedb8b7` ; aucun push lors de ce passage historique |
| Accord historique suivant | « commit la 08 & passer 09 » : passage à INT-09 demandé, sans autorisation implicite pour INT-10 ni validation orale |
| Présente réorganisation | Anciennes notes regroupées dans le cours commun, liens et index migrés ; aucun code, test, build, démarrage ou commit exécuté |

Suivi existant : [TD-B005 — Parcours REST mémoire vérifié](../../notes/todos.md#td-b005), [TD-R004 — Acceptation et réussite distinctes](../../notes/todos.md#td-r004), [TD-R014 — Refus et confidentialité](../../notes/todos.md#td-r014).
