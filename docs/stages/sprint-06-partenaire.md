# Sprint 06 — Faire fonctionner le parcours partenaire

**Statut : planifié, aucun développement autorisé par cette réorganisation.** Deux incréments complets remplacent les quatre micro-tâches. Un feu vert et une note par incrément ; les IDs existants restent des repères de traçabilité.

## INT-21 + INT-22 — Créer une famille de bout en bout

**Prérequis :** orchestration INT-17 à INT-20 disponible, accord pour ouvrir ce lot et Docker disponible pour les tests MongoDB. La clôture pédagogique du sprint 05 n'est pas revendiquée par ce plan.

**Story :** en tant que client de l'API, je veux que ma demande acceptée aboutisse à une famille créée chez le partenaire fictif, afin de suivre un vrai traitement jusqu'à son résultat.

**À réaliser ensemble :**
- Définir les échanges A–F dans WireMock et implémenter le client Feign derrière `FamilyOperations`, sans HTTP interne ni métier dans le BFF.
- Brancher le listener à l'orchestrateur uniquement après une prise en charge atomique gagnante, avec le bean borné `familyRequestExecutor`, sans `join/get` dans le pool.
- Finaliser dans MongoDB après la terminaison du graphe : `SUCCEEDED` en succès, `FAILED` en échec, sans oublier les erreurs de finalisation ni exposer d'exception brute.
- Configurer les délais de connexion/lecture et désactiver les retries implicites. Les six appels restent des I/O bloquantes exécutées dans les tâches soumises.

**Fichiers concernés :** configuration, listener et contrats internes existants de Capability ; client Feign, DTO et stubs partenaires à créer ; POM et configuration d'assemblage à adapter selon les dépendances retenues.

**Terminé quand :**
- [ ] Le même parcours HTTP public effectue POST `202`, appelle A–F avec les bons identifiants et expose ensuite `SUCCEEDED` au GET.
- [ ] Un échec partenaire représentatif ne produit pas de faux succès : les branches déjà lancées sont attendues puis le GET expose `FAILED`, sans trace privée. Une panne de l'écriture finale est signalée, pas déclarée persistée à tort.
- [ ] Les appels passent par le pool explicite, les délais sont configurés et aucune répétition automatique n'est active.

**Preuves au minimum utile :** réutiliser les tests de composition ; ajouter le parcours intégré nominal et un échec nécessaire au nouveau raccordement, avec MongoDB Testcontainers et WireMock. Vérifier les échanges et les statuts dans ces scénarios, pas dans une nouvelle suite par couche ou par endpoint.

**Livrable pédagogique :** une note qui suit le flux réel POST → listener → orchestrateur → Feign → état final. Expliquer les principaux extraits du code, pas le montage complet des tests.

## INT-23 + INT-24 — Terminer proprement malgré une panne partenaire

**Prérequis :** premier incrément réalisé et feu vert pour ce lot.

**Story :** en tant que client de l'API, je veux connaître l'échec d'un traitement et éviter les répétitions dangereuses, afin qu'une panne ne soit ni masquée ni aggravée par des doublons.

**À réaliser ensemble :**
- Distinguer refus définitif, erreur transitoire et timeout dans une politique courte, liée aux opérations fictives réellement implémentées.
- Vérifier les délais en panne, rendre le diagnostic corrélable par UUID et opération sans données personnelles, et conserver la finalisation après les branches lancées.
- Ajouter un retry Resilience4j borné seulement pour une erreur transitoire **et** une répétition sûre. Si aucune garantie partenaire ne permet cette répétition, conserver explicitement l'absence de retry ; ne pas inventer une garantie pour placer un outil.

**Terminé quand :**
- [ ] Un timeout de création termine en échec observable, sans nouvelle tentative aveugle ni affirmation que le partenaire n'a rien créé.
- [ ] La politique choisie est vérifiée par le nombre de tentatives : refus définitif non rejoué et, si une opération est réellement répétable, répétition transitoire bornée.
- [ ] Un échec partiel laisse visibles les appels déjà réussis et ne déclenche ni rollback global promis ni fallback de réussite artificielle.

**Preuves au minimum utile :** enrichir les scénarios du premier lot pour couvrir le timeout et la décision de répétition ; réutiliser son échec si celui-ci prouve déjà le refus définitif ou l'échec partiel. Pas de matrice exhaustive A–F × pannes.

**Fichiers concernés :** adaptateur partenaire, configuration de délais/résilience et tests du parcours créés dans le premier lot. Une seule note pour la politique et les modifications réellement utiles.

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
