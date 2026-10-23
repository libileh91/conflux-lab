# Sprint 03 — Persistance MongoDB et cycle des états

> **Portée historique :** ce document décrit les étapes indiquées dans son titre, pas une nouvelle validation du parcours actuel. Le raccordement ultérieur via Feign et la finalisation sont réalisés dans [INT-21 + INT-22](../../notes/sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md).

**[Simulation ConfluxLab] — Deux incréments réalisés techniquement ; clôture et restitution pédagogiques non confirmées.** Les regroupements répondent à deux résultats distincts, sans imposer la fusion de tout le sprint. Les IDs, dates et preuves restent ceux des réalisations INT-09 → INT-10 → INT-11 → INT-12.

## Prérequis communs

- Contrat POST/GET et frontières du monolithe établis : une application Spring Boot, appel Java Experience → API publique de Capability, aucun HTTP interne.
- Capability possède modèle, stockage et transitions ; le BFF adapte les réponses sans accès direct à MongoDB.
- JDK 21 et Maven Wrapper. Docker accessible pour les tests MongoDB/Testcontainers et les tests Spring concernés, pas pour le modèle unitaire seul.
- Pour l'usage local de l'application : MongoDB accessible via `MONGODB_URI`. Démarrer le contexte Spring ne prouve pas que la base accepte les lectures/écritures.

## INT-09 + INT-11 — Modèle et transitions atomiques

**Statut : réalisé techniquement.** [Cours — Modèle et transitions atomiques](../../notes/sprint-03/INT-09-modele-et-transitions-atomiques.md).

En tant que développeur, je veux représenter la vie d'une demande et conditionner ses transitions à l'état réellement stocké, afin d'éviter les cycles incohérents, les doubles prises en charge et l'écrasement d'un état final.

**Résultat livré :** un modèle de dates et de statuts, puis une modification partielle conditionnelle dans un seul `findAndModify`. La copie Java locale ne remplace pas la protection en base. La partie MongoDB s'appuie sur le document créé par l'incrément insertion/persistance.

**Critères observables, vérifiés historiquement :**

- [x] Le cycle autorise seulement `PENDING → PROCESSING → SUCCEEDED/FAILED`, sans saut, répétition ni réouverture d'un état final.
- [x] `createdAt` reste fixe, `updatedAt` ne recule pas, et `completedAt` apparaît uniquement à la fin, égal à `updatedAt`. `transitionTo(...)` retourne une copie sans sauvegarde.
- [x] Deux tentatives concurrentes sur le même UUID `PENDING` donnent un seul gagnant ; statut attendu périmé ou date trop ancienne ne modifie pas le document. Aucun upsert ne crée une demande inconnue.
- [x] Une tentative périmée n'écrase ni `SUCCEEDED` ni `FAILED`, même avec une date future ; UUID, payload, dates d'origine et champs non concernés sont conservés.
- [x] La projection HTTP ne divulgue pas le diagnostic fictif utilisé pour vérifier la conservation des champs internes. Ce témoin de test n'est pas un nouveau schéma métier d'erreur.

## INT-10 + INT-12 — Insertion et persistance MongoDB

**Statut : réalisé techniquement.** [Cours — Insertion et persistance MongoDB](../../notes/sprint-03/INT-10-insertion-et-persistance-mongodb.md).

En tant que développeur, je veux enregistrer la demande avant son acceptation et la retrouver après redémarrage de l'application, afin que son suivi ne dépende plus de la mémoire de la JVM.

**Résultat livré :** MongoDB remplace la mémoire de production ; insertion acquittée avant `202`, UUID unique sans remplacement, consultation des métadonnées et preuve HTTP sur deux JVM successives du même JAR. Failsafe exécute les IT après le packaging.

**Critères observables, vérifiés historiquement :**

- [x] Une demande valide est insérée avant le retour d'acceptation ; UUID, payload et dates sont ensuite relus dans MongoDB. Une collision de `_id` échoue sans écrasement ni fallback mémoire.
- [x] Une exception d'insertion simulée avec le vrai service et le BFF donne `500` générique sans `Location` ni reçu. Ce n'est pas une coupure réseau après écriture.
- [x] Le GET lit sans modifier et n'expose que `requestId`, `status`, `createdAt`. Une absence devient `404` ; un état métier `FAILED` connu reste consultable en `200`.
- [x] Après arrêt de la première JVM et relance du même JAR dans une nouvelle JVM, avec la même base maintenue en fonctionnement, le GET retrouve les mêmes métadonnées sans recréer la demande.
- [x] Les goals Failsafe `integration-test` et `verify` sont réellement observés après packaging. Le passage INT-12 sélectionne **1 cas Surefire + 5 cas Failsafe**, pas toute la suite.

## IDs, statuts et preuves historiques

| ID | Incrément et statut technique | Preuve datée et portée |
|---|---|---|
| **INT-09** | Modèle/transitions — terminée | **2026-10-21 : 3 cas ciblés réussis**, 2 tests du modèle ajoutés + 1 acceptation existante. Stockage encore en mémoire, pas de MongoDB à cette étape |
| **INT-10** | Insertion/persistance — terminée | **2026-10-21 : 3 cas ciblés réussis**, 2 nouveaux + 1 suivi réutilisé. Insertion/relecture/collision réelles sur standalone ; panne d'insertion simulée sans faux reçu |
| **INT-11** | Modèle/transitions — terminée | **2026-10-22 : 4 cas ciblés réussis**, 2 nouveaux MongoDB + 2 modèle réutilisés. Un seul gagnant, dates/statuts périmés refusés, états finaux protégés |
| **INT-12** | Insertion/persistance — terminée | **2026-10-22 : 1 cas Surefire + 5 cas Failsafe réussis**, `BUILD SUCCESS`, aucun échec/erreur/ignoré. Quatre cas MongoDB réutilisés + un nouveau redémarrage : UUID, `PENDING` et `createdAt` conservés |

Les vérifications MongoDB utilisent `mongo:8.0.16` standalone via Testcontainers `1.21.4`. Les processus et conteneurs temporaires ont été nettoyés à l'issue des preuves historiques. Les critères cochés ci-dessus ne signalent aucune nouvelle exécution lors de cette révision documentaire.

## Complément de lecture — replica set, théorie uniquement

[INT-10 — Standalone ou replica set](../../notes/sprint-03/INT-10-complement-mongodb-replica-set.md) distingue deux ou trois membres, quorum électoral, acquittement, découverte, délais et reprises du driver. **Ce complément n'est ni une nouvelle tâche ni un troisième incrément. Aucun cluster n'est créé ou testé.**

## Lecture historique et limites

À la fin d'INT-12, les demandes restaient normalement **`PENDING`**. Depuis INT-14, la publication après insertion et le listener peuvent les passer à **`PROCESSING`** ; le scénario actuel de redémarrage attend cet état puis vérifie sa conservation. Le reçu POST garde son statut initial, même si le GET voit déjà `PROCESSING` ; le worker peut démarrer avant réception du `202`.

Les cours répartissent les limites selon leur sujet :

- **Modèle et CAS :** atomicité d'un document, pas d'exactly-once, de propriétaire/bail ou de transaction MongoDB + HTTP partenaire. Les transitions finales sont protégées, mais aucun parcours métier finalisant la demande n'est actuellement raccordé.
- **Insertion et persistance :** UUID unique n'est pas idempotence ; dates BSON à la milliseconde ; timeout/déconnexion peuvent masquer une écriture réussie. `wtimeout` ne borne ni tout le journal ni toute la requête HTTP. Le redémarrage conserve MongoDB vivant, sans preuve de récupération après perte du disque.
- **Vérifications :** `-Dtest` sélectionne Surefire, pas Failsafe. Le `verify` INT-12 compile et package le réacteur mais n'exécute pas toute la suite Surefire : les six cas ne prouvent pas une suite complète verte ou une CI exécutée.

## Clôture pédagogique

- [x] Deux résultats techniques documentés avec leurs cours, fragments expliqués, critères et preuves datées ; complément théorique distinct.
- [ ] Compréhension de l'apprenant, restitution du refus de faux `202` et clôture pédagogique : **non confirmées**. La réalisation technique ne vaut pas évaluation orale.

**Aucun code, test, build ou commit exécuté pour cette révision.**
