# Sprint 03 — Persistance MongoDB et cycle des états

**Statut : planifié.** Prérequis : sprint 02 clos ; Docker disponible pour Testcontainers ; accord pour ouvrir ce sprint.

## Objectif

Remplacer la mémoire par MongoDB et rendre le suivi indépendant du redémarrage de l'application. Capability reste seule propriétaire de la persistance.

## Incréments, un par échange

- [ ] **INT-09 — Définir les données et transitions.** Modéliser `family_requests` : UUID, payload, statut, dates, résultat/erreur éventuels. Fixer les transitions autorisées `PENDING → PROCESSING → SUCCEEDED/FAILED` et interdire le retour d'un état terminal vers un état en cours.
- [ ] **INT-10 — Persister avant d'accepter.** Remplacer le stockage temporaire derrière la frontière de Capability. Confirmer l'écriture avant le `202` ; une erreur MongoDB donne une erreur HTTP explicite, pas une acceptation fictive.
- [ ] **INT-11 — Sécuriser les mises à jour.** Utiliser des changements d'état conditionnels adaptés pour empêcher deux prises en charge simultanées et les écrasements d'états terminaux. Définir les dates et le format d'erreur sans exposer les détails techniques au client.
- [ ] **INT-12 — Vérifier la persistance.** Tests MongoDB Testcontainers, isolation des tests et démonstration de relecture après redémarrage avec la même base.

## Critères de fin et tests

- [ ] Une demande sauvegardée est relue avec le même UUID et le même payload.
- [ ] Le contrôle d'unicité de l'identifiant est effectif côté stockage.
- [ ] Une panne de stockage avant acceptation ne retourne pas `202`.
- [ ] Les transitions autorisées passent ; les transitions invalides et doubles prises en charge sont refusées.
- [ ] Les tests d'intégration sont réellement raccordés à `verify` ; si les classes sont nommées `*IT`, configurer Failsafe.
- [ ] Experience n'importe ni document MongoDB ni repository de Capability.

## Limites et hors périmètre

Pas encore de listener : les demandes restent normalement `PENDING`. Les autres transitions sont vérifiées par tests. Aucun événement durable, reprise automatique, transaction distribuée ni idempotence partenaire n'est promis. La persistance du UUID ne suffit pas à empêcher deux créations métier identiques.

## Explication à maîtriser

Pourquoi stocker avant d'accepter ? Pourquoi MongoDB et un appel HTTP externe ne forment-ils pas une transaction atomique ?

## Preuves de clôture

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), dont les prérequis Docker, résultats des tests et preuve de relecture après redémarrage.
