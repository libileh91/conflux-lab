# Sprint 02 — Contrat REST et BFF mince

**Statut : en cours — INT-05, INT-06 et INT-07 terminées sur feu vert individuel.** Acceptation et suivi sont réalisés en mémoire ; INT-08 reste à autoriser. Le sprint n'est pas clôturé.

## Objectif

Permettre au client de demander une création de famille puis de consulter son état, sans annoncer que la famille est déjà créée.

## Repères

- Un **contrat d'API** décrit les données envoyées, les réponses et les erreurs attendues.
- Un **DTO** est un objet Java qui transporte des données entre composants ; ce n'est ni un service métier ni un document MongoDB.
- `202 Accepted` signifie « demande acceptée », pas « traitement terminé » ; `PENDING` signifie « en attente ».

**Prérequis :** INT-05 peut être préparée après INT-01, sur feu vert, pour débloquer INT-02. Les autres stories attendent le socle assemblé et vérifié d'INT-02/03/04. Cette proposition d'ordre ne démarre aucune tâche.

## Stories — un feu vert par tâche

### INT-05 — Définir les contrats
**Statut : terminée.** Préparation avancée sur feu vert pour lever le blocage d'INT-02. [Tutoriel INT-05](../../notes/sprint-02/INT-05-definir-les-contrats.md) et [contrat HTTP](../dat/http-contracts.md).

En tant que développeur, je veux connaître les données et les règles du contrat, afin d'écrire le BFF et Capability sans deviner ce qu'ils doivent échanger.

**À faire :**
- Décrire des exemples JSON : un principal, deux ayants droit, acceptation, suivi et erreurs.
- Définir les types Java correspondants, dont les trois DTO absents, et distinguer validation de forme et invariants métier.

**Terminé quand :**
- [x] Les champs, règles minimales et exemples de réponses/refus sont documentés, sans données réelles.
- [x] Les cinq types correspondent aux exemples ; 59 cas de test passent et le `clean verify` global réussit, sans service métier ajouté.

### INT-06 — Accepter une demande
**Statut : terminée sur demande explicite « ok go 06 ».** Socle INT-02/03/04 et contrats INT-05 disponibles. [Cours et preuves INT-06](../../notes/sprint-02/INT-06-accepter-une-demande.md).

En tant que client de l'API, je veux recevoir un identifiant après l'acceptation de ma demande, afin de pouvoir la suivre sans attendre sa réalisation.

**À faire :**
- Dans Capability, générer un UUID et conserver la demande dans un stockage temporaire en mémoire.
- Exposer `POST /api/v1/families` dans le BFF, qui appelle Capability en Java.

**Terminé quand :**
- [x] Le test du POST valide retourne `202`, `requestId`, `PENDING`, `statusUrl` et `Location` cohérents avec la vraie implémentation ; les tests Capability prouvent la conservation préalable du payload et l'absence d'acceptation si le store échoue.
- [x] Deux demandes identiques reçoivent des UUID distincts et sont conservées séparément ; aucune famille n'est annoncée créée ni aucun traitement simulé.

**Preuves :** `clean verify` global réussi (96 tests). Démarrage borné du JAR réel : deux POST valides `202` avec UUID distincts, GET de suivi encore `503` et corps invalide `400`, puis processus arrêté. Validation Java directe, copie immuable, collisions et erreurs d'enregistrement sont également testées, sans déclarer INT-08 complète.

### INT-07 — Consulter la demande
**Statut : terminée sur demande explicite « commit & go 07 ».** Prérequis INT-06 rempli et commit local `6936408` créé avant le développement. [Cours et preuves INT-07](../../notes/sprint-02/INT-07-consulter-la-demande.md).

En tant que client de l'API, je veux consulter l'état de ma demande avec son identifiant, afin de savoir où elle en est.

**À faire :**
- Exposer `GET /api/v1/requests/{requestId}` en passant par l'API Java de Capability.
- Traiter les identifiants inconnus ou mal formés sans accès direct du BFF au stockage.

**Terminé quand :**
- [x] Un POST réel puis GET via `Location` retrouve le même UUID en `PENDING`, avec `200` et les seules métadonnées `requestId`, `status`, `createdAt`. Le GET répété conserve l'horodatage.
- [x] Les tests vérifient `404` pour un UUID inconnu et `400` pour trois identifiants manifestement mal formés, avec erreurs génériques sans champs de succès.

**Preuves :** `clean verify` global réussi (108 tests), dont tests de lecture sans écriture, date d'origine et projection du statut stocké. Le JAR réel vérifie deux POST/GET indépendants, `404`/`400`, puis `404` pour un UUID précédemment accepté après redémarrage ; les deux processus sont arrêtés. Les autres statuts n'apparaissent que dans des fixtures de projection, sans transition métier ajoutée.

### INT-08 — Tester les refus
**Statut : à faire — feu vert requis.** Prérequis : contrats et routes INT-05/06/07 disponibles.

En tant que client de l'API, je veux comprendre pourquoi ma demande est refusée, afin de pouvoir la corriger sans voir de détails techniques internes.

**À faire :**
- Tester les données invalides : champs obligatoires absents et nombre d'ayants droit incorrect.
- Vérifier les contrôles métier de Capability directement, sans passer uniquement par le BFF.

**Terminé quand :**
- [ ] Les demandes invalides sont refusées sans enregistrement ; Capability protège aussi ses invariants lorsqu'elle est appelée directement.
- [ ] Les erreurs sont lisibles sans recopier les données personnelles, les traces ou les détails internes ; les suivis de deux demandes ne se mélangent pas.

## Limites et hors périmètre

La mémoire est un stockage pédagogique **perdu au redémarrage**. Les demandes restent `PENDING` : pas encore d'événement, d'orchestration ou de partenaire. Le `202` ne prouve donc ni une conservation durable ni un traitement complet. Un UUID sert à retrouver la demande ; deux POST identiques peuvent encore créer deux demandes.

INT-06 vérifie l'acceptation réelle en mémoire et INT-07 sa consultation via l'API publique, sans HTTP interne. Le GET est une lecture seule, pas un déclencheur métier ; après redémarrage, la map vide ne retrouve pas les anciens UUID. Les refus déjà testés ne constituent pas toute la couverture INT-08. La map n'est ni bornée ni nettoyée à ce stade : usage de labo uniquement, aucune promesse de durabilité. Pas de MongoDB, résilience ou authentification entreprise dans ce sprint.

## Démo et clôture

- [x] Montrer un POST valide, son GET de suivi et un refus de données invalides : démonstrations INT-06/07 et tests associés ; cela ne clôt pas INT-08 ni le sprint.
- [ ] Expliquer la différence entre « demande acceptée » et « famille créée » ainsi que le partage des validations BFF/Capability.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les exemples fictifs et résultats réellement observés.
