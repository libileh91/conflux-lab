# Sprint 06 — Faire fonctionner le parcours partenaire

**Statut : INT-21 + INT-22 réalisées le 2026-10-22 ; INT-23 + INT-24 réalisées le 2026-10-23 sur feu vert « commit & go next INT ».** Le parcours partenaire et les pannes ciblées sont vérifiés, sans retry faute de garantie de répétition sûre. Le lot suivant reste à autoriser.

## INT-21 + INT-22 — Créer une famille de bout en bout

**Prérequis utilisés :** orchestration INT-17 à INT-20, pool existant et Docker disponible pour MongoDB Testcontainers. Le feu vert porte sur ce lot complet ; il ne vaut pas clôture orale du sprint 05.

**Story :** en tant que client de l'API, je veux que ma demande acceptée aboutisse à une famille créée chez le partenaire fictif, afin de suivre un vrai traitement jusqu'à son résultat.

**À réaliser ensemble :**
- Définir les échanges A–F dans WireMock et implémenter le client Feign derrière `FamilyOperations`, sans HTTP interne ni métier dans le BFF.
- Brancher le listener à l'orchestrateur uniquement après une prise en charge atomique gagnante, avec le bean borné `familyRequestExecutor`, sans `join/get` dans le pool.
- Finaliser dans MongoDB après la terminaison du graphe : `SUCCEEDED` en succès, `FAILED` en échec, sans oublier les erreurs de finalisation ni exposer d'exception brute.
- Configurer les délais de connexion/lecture et désactiver les retries implicites. Les six appels restent des I/O bloquantes exécutées dans les tâches soumises.

**Fichiers concernés :** configuration, listener et contrats internes existants de Capability ; client Feign, DTO et stubs partenaires à créer ; POM et configuration d'assemblage à adapter selon les dépendances retenues.

**Terminé quand :**
- [x] Le même parcours HTTP public effectue POST `202`, appelle A–F via Feign avec les bons identifiants et expose ensuite `SUCCEEDED` au GET.
- [x] Un contrat `503` termine en `FAILED`, sans rattachement ni fuite du corps d'erreur. Les tests du graphe réutilisés vérifient l'attente des branches ; un cas unitaire vérifie le journal d'une écriture finale non confirmée, sans faux rattrapage.
- [x] Les appels passent par le pool explicite ; connexion 1000 ms, lecture 3000 ms et redirections désactivées. Retry Feign et retry du transport HC5 désactivés ; une seule tentative de contrat observée en `503`.

**Preuves au minimum utile :** réutiliser les tests de composition ; ajouter le parcours intégré nominal et un échec nécessaire au nouveau raccordement, avec MongoDB Testcontainers et WireMock. Vérifier les échanges et les statuts dans ces scénarios, pas dans une nouvelle suite par couche ou par endpoint.

**Preuves du 2026-10-22 :** `verify` ciblé réussi, **14 cas Surefire + 8 cas Failsafe**, aucun échec/erreur/ignoré. Trois nouveaux scénarios (2 HTTP, 1 écriture finale) et 19 cas réutilisés ; pas de suite globale. Le test existant du JAR conserve désormais `FAILED` après redémarrage sans nouvel appel partenaire. Les anciens tests de claim/insertion isolent leur preuve avec une future de graphe incomplète, uniquement dans les tests.

**Cours : [INT-21 + INT-22 — Créer une famille de bout en bout](../../notes/sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md).** Code expliqué par fragments, contrat des quatre routes, finalisation et limites. Une panne MongoDB finale peut laisser `PROCESSING` ou une issue inconnue ; elle est journalisée sans reprise automatique.

## INT-23 + INT-24 — Terminer proprement malgré une panne partenaire

**Prérequis utilisés :** premier incrément réalisé ; feu vert « commit & go next INT » reçu le 2026-10-23.

**Story :** en tant que client de l'API, je veux connaître l'échec d'un traitement et éviter les répétitions dangereuses, afin qu'une panne ne soit ni masquée ni aggravée par des doublons.

**À réaliser ensemble :**
- Distinguer refus définitif, erreur transitoire et timeout dans une politique courte, liée aux opérations fictives réellement implémentées.
- Vérifier les délais en panne, rendre le diagnostic corrélable par UUID et opération sans données personnelles, et conserver la finalisation après les branches lancées.
- Ajouter un retry Resilience4j borné seulement pour une erreur transitoire **et** une répétition sûre. Si aucune garantie partenaire ne permet cette répétition, conserver explicitement l'absence de retry ; ne pas inventer une garantie pour placer un outil.

**Terminé quand :**
- [x] Un timeout de création termine en échec observable, sans nouvelle tentative aveugle ni affirmation que le partenaire n'a rien créé.
- [x] La politique choisie est vérifiée par le nombre de tentatives : `400`, `503` et timeout non rejoués. Aucune des six opérations ne dispose d'une garantie de répétition sûre : aucun retry ni Resilience4j ajouté.
- [x] Un échec partiel laisse visibles les appels déjà réussis et ne déclenche ni rollback global promis ni fallback de réussite artificielle.

**Preuves au minimum utile :** enrichir les scénarios du premier lot pour couvrir le timeout et la décision de répétition ; réutiliser son échec si celui-ci prouve déjà le refus définitif ou l'échec partiel. Pas de matrice exhaustive A–F × pannes.

**Fichiers concernés :** adaptateur partenaire, configuration de délais/résilience et tests du parcours créés dans le premier lot. Une seule note pour la politique et les modifications réellement utiles.

**Preuves du 2026-10-23 :** `clean verify` ciblé réussi, **7 cas Surefire + 4 Failsafe**, aucun échec/erreur/ignoré. Scénario contrat `503` réutilisé et étendu à `400`, plus timeout réel de lecture sur A ; catégories corrélées par UUID/opération, absence de corps privé, une seule tentative et suivi `FAILED`. Composition/finalisation réutilisées, sans suite globale. Le timeout de connexion n'est pas simulé.

**Cours : [INT-23 + INT-24 — Terminer proprement malgré une panne partenaire](../../notes/sprint-06/INT-23-terminer-malgre-une-panne-partenaire.md).** Le timeout ne prouve pas l'absence de création distante ; la politique ne promet ni reprise ni état final en cas de panne MongoDB.

## Traçabilité du regroupement

| ID conservé | Ancien sujet | Incrément actuel |
|---|---|---|
| INT-21 | Définir le partenaire | Parcours complet, avec INT-22 |
| INT-22 | Brancher Feign sans retry | Parcours complet, avec INT-21 |
| INT-23 | Écrire la matrice d'erreurs | Gestion des pannes, avec INT-24 ; politique courte plutôt que catalogue |
| INT-24 | Ajouter la résilience ciblée | Gestion des pannes, avec INT-23 |

Aucun de ces IDs n'est déclaré terminé par la fusion. La règle de travail et de note commune est dans le [plan général](README.md#notre-façon-de-travailler).

## Bornes

**[Simulation ConfluxLab]** Les échanges ne reproduisent pas les endpoints historiques GFP/Open. Une seule application : Experience appelle Capability en Java ; Feign/Resilience4j ne visent que WireMock. L'UUID corrèle sans rendre une création idempotente. Pas de broker, de reprise automatique, de compensation distribuée ou de circuit breaker sans besoin démontré.
