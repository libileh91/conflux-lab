# Notes de travail — ConfluxLab

Ces cours expliquent le code du laboratoire, **par sujets cohérents et par fragments**, pas selon un quota de notes par sprint. Le [plan des tâches](../docs/stages/README.md) indique les résultats et leur statut ; le [DAT](../docs/dat/ConfluxLab.md) porte les choix d'architecture ; le [registre](todos.md) garde les actions ouvertes et les repères utiles.

## Parcours de lecture

| Sprint | Cours regroupé | IDs couverts et portée |
|---|---|---|
| 01 — Socle | [Architecture, assemblage et frontières](sprint-01/INT-01-socle-modulaire.md) | INT-01 + INT-03 + INT-04 : une application, des modules séparés et des appels Java |
| 01 — Outillage | [Build reproductible et versionnement](sprint-01/INT-02-build-et-versionnement.md) | INT-02 + INT-37 : Maven, Wrapper, configuration utile et Git |
| 02 — REST | [Accepter et consulter une demande](sprint-02/INT-05-accepter-et-consulter-une-demande.md) | INT-05 à INT-08 : contrat, validation, POST/GET et confidentialité au stade mémoire |
| 03 — Cycle de vie | [Modèle et transitions atomiques](sprint-03/INT-09-modele-et-transitions-atomiques.md) | INT-09 + INT-11 : états, dates, copie locale et arbitrage MongoDB |
| 03 — Stockage | [Insertion et persistance MongoDB](sprint-03/INT-10-insertion-et-persistance-mongodb.md) | INT-10 + INT-12 : insertion avant acceptation, acquittement, suivi et redémarrage |
| 04 — Prise en charge | [Traiter les demandes en arrière-plan](sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md) | INT-13 + INT-14 + INT-16 : événement, proxy async, claim et fenêtres de panne |
| 04 — Ressources | [Pool borné et erreurs async](sprint-04/INT-15-pool-borne-et-erreurs-async.md) | INT-15 : paramètres, rejet explicite et journalisation confidentielle |
| 05 — Orchestration | [Orchestrer les six opérations](sprint-05/INT-17-orchestrer-les-six-operations.md) | INT-17 à INT-20 : composition Java avant raccordement au listener |
| 06 — Parcours partenaire | [Créer une famille de bout en bout](sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md) | INT-21 + INT-22 : Feign/WireMock, claim gagnant, états finaux et absence de retry |

**Complément au sprint 03 :** [MongoDB standalone ou replica set](sprint-03/INT-10-complement-mongodb-replica-set.md), théorie condensée avec schéma et tableau de quorum. Ce n'est ni une nouvelle tâche ni un cluster implémenté.

Les résultats techniques sont documentés ; une compréhension orale non évaluée ne devient pas acquise par cette réécriture. Le [journal historique de clôture du sprint 01](../docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) reste accessible.

## Lire les versions sans mélanger les étapes

- Le cours REST montre l'étape historique **en mémoire**, avec les extraits situés dans leur version Git. Ce stockage n'est plus utilisé en production.
- MongoDB a ensuite remplacé la mémoire. Depuis le raccordement async, le reçu POST reste `PENDING`, mais le GET peut déjà lire `PROCESSING`.
- Les cours 01 à 05 sont situés dans leurs étapes, jusqu'à INT-20 : le graphe y existe sans listener raccordé. Le cours INT-21 + INT-22 décrit le raccordement ultérieur via Feign et la finalisation ; il ne transforme pas rétroactivement les anciennes preuves.
- Les tableaux de preuves reprennent les résultats réellement observés à l'époque. **Aucun test, build ou démarrage n'a été relancé pour cette consolidation.** Les sélections successives ne s'additionnent pas et ne constituent pas une suite globale actuelle.

## Comment les notes sont organisées

1. **Résultat et flux** : où l'on entre, quelles classes collaborent, où le travail s'arrête.
2. **Code expliqué par parties** : courts extraits réels, immédiatement suivis de leur rôle et du choix qu'ils illustrent ; pas une classe complète livrée sans explication.
3. **Schémas et tableaux utiles** : responsabilités, données, étapes, annotations ou propriétés, quand ils rendent le parcours plus lisible.
4. **Preuves et limites** : bilan compact, commandes essentielles contextualisées et liens vers le suivi. Le code des tests et leur montage peuvent être omis ; les résultats conservés restent honnêtes et datés.

Le nombre de tâches et de notes dépend du sujet : ne pas tout fusionner pour raccourcir, ni créer un cours par méthode. Des sujets autonomes peuvent rester séparés dans un même sprint. Une note peut renvoyer à une autre pour éviter de répéter Maven, la configuration du pool ou l'historique entier.

## Convention et suivi

- Les IDs `INT-XX` restent stables. Une note regroupée nomme les IDs couverts ; le fichier utilise le premier ID du groupe, sous `notes/sprint-XX/`.
- Les anciennes notes remplacées sont condensées dans les cours ci-dessus, avec leurs preuves et limites ; les liens du dépôt sont migrés. L'historique Git conserve les anciennes versions suivies.
- Les ancres `TD-B001`, etc., de [todos.md](todos.md) restent stables : `TD-Bxxx` suit une action ; `TD-Rxxx` désigne une notion, pas une connaissance évaluée. Les petits tableaux regroupent les champs source, périmètre, preuve et statut sans recopier le cours.
- La fiche de sprint porte les critères ; ne pas créer un TODO ou un repère par sous-étape. Une consolidation documentaire ne vaut pas feu vert de développement.

## Suite

Le premier lot du sprint 06 est réalisé. La [suite](../docs/stages/README.md#roadmap) reste à autoriser, sans note de réussite anticipée : **INT-23 + INT-24 — Terminer proprement malgré une panne partenaire**, puis démonstration/mesures, CI et atelier théorique. Un accord porte sur le lot présenté, pas automatiquement sur tous les travaux du sprint.
