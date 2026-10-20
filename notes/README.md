# Notes de travail — ConfluxLab

Les fiches de sprint et les tâches à réaliser sont dans [docs/stages](../docs/stages/README.md). Le [DAT](../docs/dat/ConfluxLab.md) porte les décisions d'architecture. Ce dossier rassemble les comptes rendus, sous un répertoire par sprint.

## Convention

- Identifiant de tâche : **`INT-XX`**, unique à l'échelle du projet et conservé après attribution. La séquence continue entre les sprints ; ne pas réutiliser un identifiant.
- Chemin : `notes/sprint-XX/INT-YY-<nom-de-la-tache-en-kebab-case>.md`.
- Titre : `# INT-YY — <Nom de la tâche>`.
- Une note terminée précise objectif, réalisation, explications, décisions, fichiers, commandes/résultats réellement observés et limites.
- Une note partielle ou rétrospective ne prouve pas que la tâche est terminée. Elle ne déclenche pas de développement : **le feu vert reste requis pour chaque tâche**.
- Les notes sont liées depuis cet index et la fiche du sprint concerné. Les tâches restent décochées tant que leurs critères ne sont pas vérifiés.

## Répertoires par sprint

| Dossier | Tâches planifiées | Sujet |
|---|---|---|
| [sprint-01](sprint-01/) | INT-01 à INT-04 | Socle modulaire |
| [sprint-02](sprint-02/) | INT-05 à INT-08 | Contrat REST |
| [sprint-03](sprint-03/) | INT-09 à INT-12 | MongoDB et statuts |
| [sprint-04](sprint-04/) | INT-13 à INT-16 | Asynchronisme |
| [sprint-05](sprint-05/) | INT-17 à INT-20 | Orchestration |
| [sprint-06](sprint-06/) | INT-21 à INT-24 | Partenaire et résilience |
| [sprint-07](sprint-07/) | INT-25 à INT-28 | Validation et observabilité |
| [sprint-08](sprint-08/) | INT-29 à INT-32 | CI |
| [sprint-09](sprint-09/) | INT-33 à INT-36 | CD théorique et entretien |

Les anciens numéros `01.1` à `09.4` ont été remplacés dans les fiches par ces identifiants, dans l'ordre des tâches. Une nouvelle tâche recevra un identifiant disponible sans renuméroter les existantes.

## Comptes rendus existants

| Tâche | Note | État constaté |
|---|---|---|
| INT-01 | [Inspecter et fixer les frontières](sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md) | Rétrospective du socle initial ; incomplet, build non exécuté |
| INT-06 | [Accepter une demande](sprint-02/INT-06-accepter-une-demande.md) | Rétrospective de l'ébauche Experience ; partielle, non testée, HTTP interne à remplacer |

Ces deux notes documentent le travail amorcé **avant** l'organisation actuelle par sprints et le cadrage final du monolithe modulaire. Leur rattachement est rétrospectif : ni INT-01 ni INT-06 n'est déclarée terminée. Les autres dossiers sont préparés sans compte rendu anticipé.

## Documentation déjà produite

- Le DAT décrit le cas d'usage fictif, le graphe A–F, les statuts et les limites de l'événement local.
- Neuf fiches de sprint définissent incréments, exclusions, tests et critères de fin.
- Les règles du skill imposent le BFF mince, le métier dans Capability, les appels Java internes et le feu vert avant chaque tâche.
- Le plan a été déplacé dans `docs/stages/` ; ses liens locaux ont été vérifiés lors du déplacement.
- La présente demande organise les notes et les identifiants ; elle ne modifie pas les sources Java, le POM existant ou la configuration applicative.

Aucune compilation, exécution du parcours métier, CI verte ou mesure de performance n'a été démontrée à ce stade.
