# Sprint 02 — Contrat REST et BFF mince

**Statut : planifié.** Prérequis : sprint 01 clos et accord pour ouvrir ce sprint.

## Objectif

Accepter une demande fictive de trois personnes et permettre sa consultation, sans laisser croire que la famille est déjà créée.

## Incréments, un par échange

- [ ] **INT-05 — Définir les contrats.** Choisir les champs et règles minimales du principal et des deux ayants droit. Documenter les JSON du POST, du suivi et des erreurs. Distinguer validation de forme côté Experience et invariants métier côté Capability.
- [ ] **INT-06 — Accepter une demande.** Capability génère un UUID et conserve la demande dans un stockage temporaire en mémoire. Experience expose `POST /api/v1/families` et retourne `202`, `requestId`, `statusUrl`, `status` et un en-tête `Location` cohérent.
- [ ] **INT-07 — Consulter la demande.** Exposer `GET /api/v1/requests/{requestId}` : `200` avec état courant, `404` si inconnu, `400` si UUID invalide. Le BFF appelle Capability en Java et ne lit pas son stockage.
- [ ] **INT-08 — Tester les refus.** Payload invalide, champs absents, mauvaise cardinalité, demande inconnue et deux demandes distinctes. Donner des erreurs lisibles sans recopier les données personnelles ni les traces internes.

## Critères de fin et tests

- [ ] Les tests HTTP vérifient POST `202`, corps de réponse, `Location`, GET `200`, `400` et `404`.
- [ ] Un payload invalide n'est pas accepté ni enregistré.
- [ ] Les tests de Capability protègent ses invariants même sans passer par le contrôleur BFF.
- [ ] Deux demandes donnent deux UUID et deux suivis distincts.
- [ ] Une démonstration documentée montre le POST puis le GET sur la même application.

## Limites temporaires obligatoires

Le stockage en mémoire est **une simulation pédagogique**, perdu au redémarrage. Les demandes restent `PENDING` : aucun événement ni appel partenaire n'est encore exécuté. Pas de faux `SUCCEEDED`. Le `202` de cette étape ne démontre pas une prise en charge durable ni un traitement complet. Ce stockage sera remplacé au sprint 03.

L'UUID est un identifiant de corrélation, pas une clé d'idempotence. Deux POST identiques peuvent donner deux demandes.

## Hors périmètre

MongoDB, traitement asynchrone, orchestration, résilience et authentification entreprise.

## Explication à maîtriser

Pourquoi `202 Accepted` plutôt que « famille créée » ? Qu'est-ce qui appartient au BFF et qu'est-ce qui doit être vérifié par Capability ?

## Notes existantes

- [INT-06 — Accepter une demande](../../notes/sprint-02/INT-06-accepter-une-demande.md) : ébauche REST antérieure, incluant des éléments de suivi et de gestion des erreurs liés à INT-07/INT-08. Capability et les DTO sont absents ; aucun parcours ni test n'est validé. Ce rattachement rétrospectif n'ouvre pas le sprint.

## Preuves de clôture

À compléter selon le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), avec exemples fictifs, commandes et résultats. Rien n'est validé par anticipation.
