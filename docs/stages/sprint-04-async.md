# Sprint 04 — Prise en charge asynchrone et maîtrise de l'exécution

**Statut : deux incréments réalisés dans leur périmètre technique.** La prise en charge s'arrête à `PROCESSING` ; le parcours de création d'une famille de bout en bout n'est pas réalisé par ce flux. Restitution orale non évaluée et clôture pédagogique à confirmer.

**[Simulation ConfluxLab]** Le sprint classe deux résultats distincts : il n'impose ni une seule tâche ni une seule note par sprint. Les IDs restent stables ; cette réorganisation ne change ni leurs preuves ni leur chronologie.

| Incrément | Résultat livré | Cours |
|---|---|---|
| **INT-13 + INT-14 + INT-16** | Prendre en charge les demandes en arrière-plan | [Insertion, publication, proxy, claim, suivi et fenêtres de panne](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md) |
| **INT-15** | Borner et diagnostiquer l'exécution | [Pool borné, paramètres, rejet et erreurs async](../../notes/sprint-04/INT-15-pool-borne-et-erreurs-async.md) |

**Prérequis réalisés :** acceptation persistée et suivi MongoDB d'INT-10/12, transition atomique d'INT-11. Une application Spring Boot ; Experience appelle l'interface publique de Capability en Java, sans accès au store interne. Le parcours réel nécessite MongoDB ; les tests historiques utilisent Testcontainers.

## INT-13 + INT-14 + INT-16 — Prendre en charge les demandes en arrière-plan

En tant que développeur, je veux détacher la prise en charge de la requête HTTP et consulter son état, afin de suivre les demandes sans attendre leur traitement ni promettre une exécution durable.

**Périmètre réalisé :** insertion confirmée de `PENDING`, publication locale de l'UUID, listener via proxy async, claim atomique et suivi courant. Les trois fenêtres de panne et les preuves de concurrence sont expliquées dans le [cours de prise en charge](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md).

**Critères observables réalisés, selon les preuves historiques :**

- [x] Publication seulement après insertion confirmée ; échec de sauvegarde sans publication ni reçu d'acceptation.
- [x] POST `202 PENDING` possible pendant que le test bloque les claims ; le worker est distinct du thread HTTP, sans ordre imposé autour de la réception du `202`.
- [x] Deux réceptions du même UUID peuvent lire `PENDING`, mais la commande conditionnelle atomique MongoDB ne laisse qu'un gagnant.
- [x] GET limité aux métadonnées, pouvant lire `PENDING` ou `PROCESSING` ; UUID et date de création stables, sans payload exposé.
- [x] Insertion non confirmée, demande enregistrée sans prise en charge et interruption après claim expliquées ; attentes bornées et nettoyage des tests relus dans INT-16, sans nouveau scénario ou run.

**Repère de lecture :** INT-14 utilisait `applicationTaskExecutor`, auto-configuré par Boot. INT-15 a ensuite ajouté le pool dédié `familyRequestExecutor` ; INT-16 a relu ce câblage final depuis `59c881c`. Rattacher cette relecture au premier incrément ne lui attribue pas la réalisation du bornage.

## INT-15 — Borner et diagnostiquer l'exécution

En tant que développeur, je veux limiter les tâches en cours et en attente et identifier leurs erreurs sans données privées, afin de rendre les saturations visibles sans bloquer le thread HTTP par un travail de secours.

**Périmètre réalisé :** pool dédié nommé par `@Async`, trois propriétés configurables, rejet `AbortPolicy`, handler d'exceptions async `void` et attente de fermeture bornée. Les réglages et leurs conséquences sont expliqués dans le [cours de maîtrise de l'exécution](../../notes/sprint-04/INT-15-pool-borne-et-erreurs-async.md).

**Critères observables réalisés, selon les preuves historiques :**

- [x] Pool fixe et file bornée : défauts de 2 workers, 16 places et 5 s d'attente à la destruction du bean ; valeurs invalides refusées au démarrage par le code.
- [x] Avec 2 workers/1 place occupés dans le test, la soumission suivante produit `500` sans reçu ni `Location`, sans exécution sur le thread HTTP ; l'insertion reste `PENDING`.
- [x] Erreur de lecture async injectée : journal `ERROR` avec UUID/type, sans message privé ; le handler ne change pas le document et n'invente pas `FAILED`.
- [x] Attente de fermeture configurée, sans la présenter comme un timeout de tâche ou un arrêt forcé ; aucun test d'arrêt prolongé revendiqué.

## Traçabilité des IDs et des preuves

Les IDs historiques restent stables. Les comptes décrivent les passages du **2026-10-22**, pas une validation renouvelée.

| ID stable | Incrément de rattachement | Contribution et preuve historique |
|---|---|---|
| **INT-13** | Prise en charge | [Fenêtres de panne](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md#5-trois-fenêtres-de-panne-utiles) : analyse documentaire, un schéma et trois situations, aucune panne exécutée |
| **INT-14** | Prise en charge | [Preuves INT-14](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md#6-preuves-historiques), repère `bdb93f8` : **5 cas Surefire + 4 cas Failsafe** réussis, aucun échec/erreur/ignoré ; un seul scénario ajouté, suivi/insertion/redémarrage réutilisés |
| **INT-15** | Bornage et diagnostic | [Preuves INT-15](../../notes/sprint-04/INT-15-pool-borne-et-erreurs-async.md#6-preuves-historiques-int-15), repère `59c881c` : **1 cas Surefire + 3 cas Failsafe** réussis, aucun échec/erreur/ignoré ; deux cas ajoutés, concurrence réutilisée |
| **INT-16** | Prise en charge, relecture après INT-15 | [Consolidation des preuves](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md#6-preuves-historiques) : code et deux rapports ciblés INT-15 relus, aucune adaptation nécessaire pour les critères ; **aucun build ou test réexécuté** |

Les sélections se recouvrent : **ne pas additionner leurs comptes ni en déduire une suite globale actuelle**. La présente rédaction ne relit/actualise pas les rapports générés, ne lance aucun test et ne redémarre pas le JAR.

## Limites communes et bilan

L'événement Spring reste local et non durable, pas un broker. Le `202` ne signifie ni fin du traitement ni garantie de reprise ; l'UUID corrèle, sans rendre les POST idempotents. Un document peut survivre sans tâche ou worker vivant.

Le listener termine après la tentative de claim. **Même après INT-20, l'orchestration Java n'est pas raccordée au listener : aucun appel partenaire ni état final `SUCCEEDED`/`FAILED` n'est produit par ce chemin.** Aucun mécanisme de reprise ou stockage de diagnostics n'est ajouté.

La conservation de `PROCESSING` après redémarrage du vrai JAR reste une preuve INT-14, pas une reprise de traitement. Pas de suite complète, crash forcé, mesure de performance ou CI verte revendiqués. Les deux résultats techniques sont documentés séparément ; la clôture pédagogique reste à confirmer, sans maîtrise orale inventée ni exercice imposé.

Cette réorganisation n'autorise aucun développement suivant. Suivi existant : [repères de prise en charge](../../notes/sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md#7-repères-et-suivi) et [repères de bornage/diagnostic](../../notes/sprint-04/INT-15-pool-borne-et-erreurs-async.md#7-repères-et-suivi).
