# Sprint 02 — Contrat REST et BFF mince

**Statut : préparation INT-05 terminée sur feu vert, avant l'assemblage du sprint 01.** INT-06 reste une ébauche partielle, non validée. Les autres stories attendent leur feu vert individuel.

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
**Statut : à faire — ébauche existante non validée, feu vert requis.** [Note rétrospective](../../notes/sprint-02/INT-06-accepter-une-demande.md).

En tant que client de l'API, je veux recevoir un identifiant après l'acceptation de ma demande, afin de pouvoir la suivre sans attendre sa réalisation.

**À faire :**
- Dans Capability, générer un UUID et conserver la demande dans un stockage temporaire en mémoire.
- Exposer `POST /api/v1/families` dans le BFF, qui appelle Capability en Java.

**Terminé quand :**
- [ ] Le test du POST valide retourne `202`, `requestId`, `status`, `statusUrl` et un en-tête `Location` cohérent, après enregistrement.
- [ ] Deux demandes reçoivent des identifiants distincts ; aucune famille n'est annoncée créée ni aucun succès métier simulé.

### INT-07 — Consulter la demande
**Statut : à faire — feu vert requis.** Prérequis : INT-06 terminée.

En tant que client de l'API, je veux consulter l'état de ma demande avec son identifiant, afin de savoir où elle en est.

**À faire :**
- Exposer `GET /api/v1/requests/{requestId}` en passant par l'API Java de Capability.
- Traiter les identifiants inconnus ou mal formés sans accès direct du BFF au stockage.

**Terminé quand :**
- [ ] Un test POST puis GET retrouve la même demande en `PENDING` avec une réponse `200`.
- [ ] Les tests vérifient `404` pour un UUID inconnu et `400` pour un identifiant invalide.

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

Les annotations présentes dans l'ébauche REST ne prouvent pas qu'elle fonctionne. Pas de MongoDB, résilience ou authentification entreprise dans ce sprint.

## Démo et clôture

- [ ] Montrer un POST valide, son GET de suivi et un refus de données invalides, avec leurs tests.
- [ ] Expliquer la différence entre « demande acceptée » et « famille créée » ainsi que le partage des validations BFF/Capability.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les exemples fictifs et résultats réellement observés.
