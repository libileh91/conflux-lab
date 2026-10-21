# Sprint 03 — Persistance MongoDB et cycle des états

**Statut : stories techniques INT-09 à INT-12 terminées ; clôture et restitution pédagogique à confirmer.** Le passage à INT-09 a été demandé après les stories techniques INT-05 à INT-08 ; la restitution pédagogique du sprint 02 n'a pas été évaluée. Docker n'est pas nécessaire au modèle et aux tests unitaires INT-09 ; depuis INT-10, il est requis pour les tests MongoDB et les tests Spring d'assemblage/HTTP qui partagent cette base temporaire. Chaque story conserve son feu vert individuel.

## Objectif

Remplacer le stockage en mémoire par MongoDB pour retrouver une demande après un redémarrage et contrôler ses changements d'état.

## Repères

- La **persistance** conserve les données au-delà de l'arrêt de l'application.
- Un **repository** est le composant qui lit et écrit les données ; seul Capability y accède.
- **Testcontainers** lance un MongoDB temporaire dans Docker pour isoler les tests.

## Stories — un feu vert par tâche

### INT-09 — Définir les données et transitions
**Statut : terminée sur demande « commit la 08 & passer 09 ».** Commit local préalable INT-08 : `fedb8b7`, sans push. [Cours et preuves INT-09](../../notes/sprint-03/INT-09-definir-les-donnees-et-transitions.md).

En tant que développeur, je veux définir les informations conservées et les changements d'état permis, afin de représenter correctement la vie d'une demande.

**À faire :**
- Décrire `family_requests` : identifiant, données reçues, état, dates et éventuels résultat/erreur.
- Définir `PENDING → PROCESSING → SUCCEEDED/FAILED`, en expliquant chaque état.

**Terminé quand :**
- [x] Le [modèle documenté](../dat/ConfluxLab.md#6-données-mongodb) décrit les champs et dates ; `StoredFamilyRequest` porte les six champs utiles au cycle. `result?`/`error?` restent des champs cibles optionnels explicitement différés, sans schéma métier inventé ni exposition au BFF.
- [x] Deux tests unitaires vérifient les deux issues du cycle et les refus, notamment la sortie de `SUCCEEDED`/`FAILED` ; un test existant confirme toujours l'acceptation en mémoire avant retour.

**Preuves :** 3 tests ciblés exécutés, aucun échec/erreur/ignoré, `BUILD SUCCESS`. Seuls deux tests ont été ajoutés. Aucun MongoDB, nouveau test HTTP, build global ou traitement asynchrone. Les transitions retournent une copie et ne réalisent ni sauvegarde ni prise en charge atomique.

### INT-10 — Persister avant d'accepter
**Statut : terminée sur feu vert « commit la 9 & go INT-10 », confirmé ensuite par « ok continue ».** INT-09 commitée au préalable : `185bdd7`, sans push. [Cours et preuves INT-10](../../notes/sprint-03/INT-10-persister-avant-d-accepter.md).

En tant que client de l'API, je veux que ma demande soit enregistrée avant son acceptation, afin de pouvoir la retrouver si l'application redémarre.

**À faire :**
- Remplacer la mémoire par un repository MongoDB dans Capability.
- Attendre la confirmation d'écriture avant de renvoyer `202`.

**Terminé quand :**
- [x] Un POST réel via MVC insère dans MongoDB ; le test relit le même UUID, payload et dates. Une seconde insertion du même `_id` est refusée sans écrasement par l'index unique natif.
- [x] Une erreur d'insertion simulée sur le store, avec le vrai service et le BFF, renvoie `500` générique sans `Location` ni reçu `202`.

**Preuves :** 2 nouveaux tests et 1 test de suivi POST/GET réutilisé, tous réussis avec un MongoDB standalone Testcontainers `mongo:8.0.16`. Le service attend l'insertion acquittée (`majority` + journal) avant le reçu. Aucun fallback mémoire de production. Testcontainers est épinglé en 1.21.4 après reproduction d'une incompatibilité API Docker en 1.21.3 ; conteneurs temporaires supprimés. À l'issue d'INT-10, suite globale, JAR et redémarrage non exécutés ; INT-11/12 n'étaient pas encore commencées. Leurs résultats ultérieurs figurent ci-dessous.

**Complément documentaire, sans changement de périmètre :** [MongoDB standalone ou replica set](../../notes/sprint-03/INT-10-complement-mongodb-replica-set.md), lié à INT-10 ; aucune topologie répliquée créée ou testée.

### INT-11 — Sécuriser les mises à jour
**Statut : terminée sur feu vert « commit ça & push puis go INT-11 ».** Le complément théorique INT-10 a été commité et poussé auparavant : `66e0ade`. [Cours et preuves INT-11](../../notes/sprint-03/INT-11-securiser-les-mises-a-jour.md).

En tant que développeur, je veux empêcher deux traitements de prendre la même demande en charge simultanément, afin d'éviter des exécutions concurrentes incohérentes.

**À faire :**
- Modifier un état seulement si sa valeur actuelle permet la transition demandée.
- Conserver des dates et erreurs cohérentes, sans écraser un résultat final.

**Terminé quand :**
- [x] Deux appelants libérés ensemble tentent la prise en charge sur MongoDB réel ; un seul `findAndModify` correspond au statut `PENDING` et retourne le document passé à `PROCESSING`.
- [x] Graphe interdit, date antérieure, statut attendu périmé et écrasement des deux états finaux sont refusés ; dates et données restent cohérentes. Le suivi HTTP n'expose pas le diagnostic fictif conservé dans la fixture.

**Preuves :** 2 nouveaux tests MongoDB + 2 tests du modèle existants, tous réussis (`BUILD SUCCESS`), sans suite globale. Mise à jour conditionnelle en une commande, sans upsert, préservant payload et dates d'origine. Le diagnostic du test ne constitue pas un nouveau schéma d'erreur métier ; `error`/`result` restent différés. Aucun appelant de production n'exécute encore de traitement et aucune route de transition n'est créée. À l'issue d'INT-11, INT-12 n'était pas commencée ; sa preuve ultérieure figure ci-dessous.

### INT-12 — Vérifier la persistance
**Statut : terminée sur feu vert « commit 11 & go 12 ».** INT-11 commitée localement au préalable : `af3f215`, sans push. [Cours et preuves INT-12](../../notes/sprint-03/INT-12-verifier-la-persistance.md).

En tant que développeur, je veux tester la conservation des demandes avec un vrai MongoDB isolé, afin de vérifier autre chose qu'une simulation en mémoire.

**À faire :**
- Exécuter les tests MongoDB via Testcontainers avec des données isolées.
- Relancer l'application avec la même base pour démontrer la relecture ; raccorder les tests `*IT` à Maven Failsafe.

**Terminé quand :**
- [x] Un seul nouveau test démarre le JAR réel, crée et consulte une demande, arrête la première JVM puis relance le même JAR avec la même base temporaire : UUID, statut `PENDING` et `createdAt` identiques après redémarrage.
- [x] Failsafe est raccordé aux phases `integration-test`/`verify` et exécute 5 cas dans 3 classes `*IT`. Le passage `clean verify` observé sélectionne un seul contrôle Surefire de frontière Experience/Capability ; les rapports attestent 6 réussites au total. La commande sans filtre est configurée mais n'a pas été exécutée telle quelle.

**Preuves :** JAR packagé et exécuté dans les PID `591557`, puis `591651`, arrêtés à l'issue du test. Même demande `3bfc5530-6199-402c-ace1-c54f18d6c9ae` relue depuis le même MongoDB `localhost:32773`. Les 4 cas MongoDB INT-10/11 sont réutilisés après renommage `*IT` ; seul le test de redémarrage est nouveau. `BUILD SUCCESS`, aucun échec/erreur/ignoré, conteneurs temporaires supprimés. Pas de suite Surefire complète, de redémarrage MongoDB, de CI ou d'asynchronisme.

## Limites et hors périmètre

Il n'y a pas encore de traitement en arrière-plan : les demandes restent normalement `PENDING`. Les autres transitions sont appelées dans les tests, pas simulées comme un parcours métier réussi.

Le UUID ne suffit pas à empêcher deux créations métier identiques. MongoDB et un partenaire HTTP ne partagent pas de transaction qui annulerait tout ensemble. Pas de reprise automatique, événement durable ou transaction distribuée.

## Démo et clôture

- [x] Enregistrer une demande, redémarrer l'application avec la même base et retrouver son état : démonstration automatisée du JAR dans INT-12.
- [ ] Montrer le test d'échec de stockage et expliquer pourquoi il interdit un faux `202` : le test a été réexécuté avec succès sous Failsafe ; la restitution de l'apprenant reste non évaluée.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les prérequis Docker, tests et preuve de relecture réellement observés.
