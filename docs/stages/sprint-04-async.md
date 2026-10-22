# Sprint 04 — Spring Events locaux et traitement asynchrone

**Statut : INT-13 simplifiée et INT-14 terminées ; INT-15/16 non commencées.** INT-14 a été reprise explicitement après le commit de la simplification (`6081781`). Chaque story suivante conserve son feu vert individuel. La clôture pédagogique du sprint 03 reste à confirmer.

## Objectif

Répondre au client sans attendre la fin du travail, tout en comprenant les erreurs possibles entre acceptation et traitement.

## Repères

- Un **événement Spring local** transmet une information dans l'application ; il n'est pas conservé pour survivre à un arrêt.
- Un **listener** reçoit cet événement ; `@Async` lui permet de travailler sur un autre thread.
- Un **executor** fournit les threads et une file d'attente dont les capacités doivent être limitées.

## Stories — un feu vert par tâche

### INT-13 — Dessiner les fenêtres de panne
**Statut : terminée sur feu vert « go 13 » ; documentation uniquement.** Point de départ : INT-12 commitée et poussée (`470494b`). [Cours, schémas et limites INT-13](../../notes/sprint-04/INT-13-dessiner-les-fenetres-de-panne.md).

En tant que développeur débutant, je veux repérer où une demande peut rester bloquée, afin de comprendre ce que le client peut attendre après son acceptation.

**À faire :**
- Dessiner l'écriture en base, la publication, la mise en file, la prise en charge et la réponse HTTP.
- Décrire les conséquences d'un arrêt ou du refus d'une tâche par l'executor.

**Terminé quand :**
- [x] Le schéma cible sépare les branches HTTP et worker après soumission ; la prise en charge peut précéder ou suivre le `202`, sans ordre artificiel.
- [x] Trois situations expliquent les réponses HTTP et états possibles : insertion non confirmée ; demande enregistrée mais pas prise en charge (arrêt ou rejet inclus) ; traitement pris en charge puis interrompu.

**Preuves :** analyse statique initiale du code à l'issue d'INT-12, puis réécriture documentaire simplifiée. Le [DAT §7.1](../dat/ConfluxLab.md#71-fenêtres-de-panne-documentées--int-13) en conserve la synthèse. Un schéma et trois cas suffisent ; aucune panne exécutée, aucune reprise automatique ajoutée. La révision ne touche pas à l'ébauche INT-14.

### INT-14 — Publier et consommer
**Statut : terminée sur reprise « commit la 13 et reprends la 14 ».** Après la pause et le commit local `6081781`, le périmètre retenu reste publication, listener et prise en charge ; bornage dédié et erreurs restent dans INT-15. [Cours et preuves INT-14](../../notes/sprint-04/INT-14-publier-et-consommer.md).

En tant que client de l'API, je veux que le travail soit pris en charge séparément de ma requête HTTP, afin de ne pas attendre sa réalisation complète.

**À faire :**
- Publier un événement avec le `requestId` uniquement après écriture confirmée en base.
- Faire relire la demande par le listener `@Async`, puis la passer de `PENDING` à `PROCESSING` une seule fois.

**Terminé quand :**
- [x] Deux tests unitaires existants vérifient la publication après sauvegarde et son absence en cas d'échec ; le test événementiel soumet deux réceptions et observe un seul gagnant sur MongoDB réel.
- [x] Le POST répond `202` alors que le listener reste bloqué par un signal de test ; deux workers atteignent la prise en charge avant libération, sur des threads distincts du thread HTTP. Aucun ordre après le `202` n'est imposé.

**Preuves :** `verify` ciblé réussi, 5 cas Surefire + 4 cas Failsafe, aucun échec/erreur/ignoré. Un seul scénario ajouté ; tests de suivi, insertion et redémarrage réutilisés et adaptés. Le JAR réel conserve `PROCESSING` après redémarrage. Le listener s'arrête à cette transition : aucun métier, état final, reprise ou pool dédié borné ajouté. La suite complète et les scénarios d'INT-15/16 n'ont pas été exécutés.

### INT-15 — Borner l'exécution
**Statut : à faire — feu vert requis.** Prérequis : listener INT-14 disponible. Périmètre resserré sur demande utilisateur ; aucun développement lancé par cette révision.

En tant que développeur, je veux limiter les tâches en cours et en attente, afin d'éviter une accumulation sans limite et de rendre les refus visibles.

**À faire :**
- Brancher le listener sur un seul executor dédié : threads et file bornés, rejet explicite, attente d'arrêt bornée.
- Journaliser les exceptions du listener `@Async void` avec le `requestId`, sans payload personnel ni nouveau champ public de diagnostic.

**Terminé quand :**
- [ ] Les limites et l'attente d'arrêt sont configurées et expliquées. Un test contrôlé remplit le pool et sa file, puis vérifie le refus suivant : pas de tâche exécutée sur le thread HTTP ni de `202` masquant ce rejet.
- [ ] Une exception du listener produit un journal corrélé à l'UUID. Ne pas forcer un état `FAILED` pour un échec technique avant prise en charge, ni prétendre pouvoir écrire un statut quand MongoDB est indisponible.

**Limite volontaire :** pas de stockage de diagnostics, de nouvel état, de reprise ou de simulation de crash. Les états finaux seront gérés avec le travail métier réel.

### INT-16 — Tester avec un traitement contrôlé
**Statut : à faire — feu vert requis.** Prérequis : INT-14/15 disponibles. Cette tâche consolide les preuves, elle ne recrée pas leurs scénarios.

En tant que développeur, je veux disposer de tests fiables et de preuves lisibles du flux asynchrone, afin de le vérifier sans dépendre du hasard ni multiplier les tests.

**À faire :**
- Réutiliser le test événementiel d'INT-14 et les vérifications d'INT-15 ; adapter leur instrumentation au câblage final si nécessaire, sans créer de faux métier de production.
- Vérifier les attentes bornées, la libération des signaux de test et les preuves obtenues ; compléter seulement un manque concret.

**Terminé quand :**
- [ ] Les preuves couvrent `202` indépendant du worker, un seul gagnant, rejet à saturation et erreur async journalisée. Chaque comportement est vérifié au bon endroit, sans doublon obligatoire entre INT-14/15/16 ni `Thread.sleep` comme preuve.
- [ ] Un bilan court indique les tests réellement exécutés et rappelle la limite d'abandon expliquée dans INT-13. Aucun nouveau test, redémarrage du JAR ou crash forcé n'est exigé si les preuves existantes suffisent.

## Limites et hors périmètre

Le `202` n'assure ni réussite finale ni exécution durable. Un arrêt peut abandonner une demande `PENDING` ou `PROCESSING` ; l'événement Spring local n'est pas une file de messages durable.

Les blocages et erreurs contrôlés restent dans les tests : pas de nouveau faux traitement métier dans l'application. Pas de broker, de reprise automatique, de stockage de diagnostics ou d'orchestration A–F dans ce sprint.

## Démo et clôture

- [ ] Présenter les preuves réutilisées d'INT-14/15 : réponse HTTP indépendante, prise en charge unique, rejet et erreur journalisée avec l'UUID.
- [ ] Expliquer, sans imposer une nouvelle simulation, pourquoi un événement local peut être perdu et laisser une demande abandonnée.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les résultats et limites réellement observés.
