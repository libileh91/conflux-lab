# Sprint 04 — Spring Events locaux et traitement asynchrone

**Statut : INT-13 terminée (analyse documentaire) ; INT-14 à INT-16 non commencées.** Passage demandé par « go 13 » après les stories techniques INT-09 à INT-12 et leur publication Git ; la clôture pédagogique du sprint 03 reste à confirmer. Chaque story conserve son feu vert individuel : cette demande n'autorise pas INT-14.

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
- [x] La matrice F1–F8 explique écriture ambiguë, arrêt avant publication, rejet, perte en file, panne avant/après prise en charge et écriture finale incertaine, avec réponses HTTP possibles et états persistés. Elle inclut le document `PENDING` sans tâche et distingue rejet de soumission d'exception du worker.

**Preuves :** inspection statique du service, du store/modèle MongoDB, de sa configuration et du BFF ; schémas et matrice relus. Le [DAT §7.1](../dat/ConfluxLab.md#71-fenêtres-de-panne-documentées--int-13) reprend les limites et hypothèses de lecture. Aucun Java/POM modifié, aucun build ou test de panne exécuté. Le distributeur synchrone avec un listener `@Async` et rejet propagé est une cible de raisonnement, pas un câblage livré. Aucun mapping de saturation `503`, `PENDING → FAILED` ou mécanisme de reprise ajouté.

### INT-14 — Publier et consommer
**Statut : à faire — feu vert requis.** Prérequis : INT-13 terminée.

En tant que client de l'API, je veux que le travail soit pris en charge séparément de ma requête HTTP, afin de ne pas attendre sa réalisation complète.

**À faire :**
- Publier un événement avec le `requestId` uniquement après écriture confirmée en base.
- Faire relire la demande par le listener `@Async`, puis la passer de `PENDING` à `PROCESSING` une seule fois.

**Terminé quand :**
- [ ] Les tests confirment qu'une écriture échouée ne publie pas d'événement et que deux réceptions du même événement ne déclenchent pas deux prises en charge concurrentes.
- [ ] Un test peut garder le travail bloqué tout en obtenant l'acceptation HTTP, sans supposer que le listener commence après la réponse.

### INT-15 — Borner l'exécution
**Statut : à faire — feu vert requis.** Prérequis : listener INT-14 disponible.

En tant que développeur, je veux limiter le travail accepté en parallèle, afin que l'application réagisse de façon connue quand ses ressources sont occupées.

**À faire :**
- Configurer le nombre de threads, la file d'attente, le refus à saturation et l'arrêt propre.
- Enregistrer les échecs du traitement, y compris les exceptions d'une méthode async sans résultat (`void`).

**Terminé quand :**
- [ ] Un test remplit les capacités prévues et vérifie la réaction documentée au refus de la tâche suivante, sans faux `202`.
- [ ] Les erreurs et l'arrêt du traitement donnent des états cohérents et un diagnostic consultable ; aucune exception n'est perdue silencieusement.

### INT-16 — Tester avec un traitement contrôlé
**Statut : à faire — feu vert requis.** Prérequis : INT-14/15 disponibles.

En tant que développeur, je veux choisir quand le traitement avance ou échoue dans un test, afin de vérifier l'asynchronisme sans dépendre du hasard.

**À faire :**
- Utiliser une doublure de test que l'on peut bloquer, libérer ou mettre en échec.
- Simuler un arrêt entre enregistrement et consommation pour observer le risque d'abandon.

**Terminé quand :**
- [ ] Les tests couvrent acceptation indépendante du travail, rejet et échec, avec des attentes limitées dans le temps ; aucun `Thread.sleep` ne sert de preuve.
- [ ] Une simulation montre qu'une demande peut rester abandonnée après un crash ; le compte rendu distingue cette limite d'une reprise réellement implémentée.

## Limites et hors périmètre

Le `202` n'assure ni réussite finale ni exécution durable. Un arrêt peut abandonner une demande `PENDING` ou `PROCESSING` ; l'événement Spring local n'est pas une file de messages durable.

La doublure est réservée aux tests : elle ne remplace pas les six opérations métier dans l'application. Pas de broker, de reprise automatique ou d'orchestration A–F ajoutés dans ce sprint.

## Démo et clôture

- [ ] Montrer une réponse HTTP pendant que la doublure de traitement reste bloquée, puis un échec visible dans le suivi.
- [ ] Expliquer un cas d'abandon après crash et la limite d'un événement non durable.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les résultats et limites réellement observés.
