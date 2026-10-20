# ConfluxLab — Progression par sprints

**[Simulation ConfluxLab / proposition de découpage]** — Ce plan organise le laboratoire, pas le déroulement historique du projet AXA.

Référence d'architecture : [DAT](../dat/ConfluxLab.md). Le DAT porte les décisions ; ce dossier porte le travail et les preuves de validation.

## Architecture retenue

- **Un monolithe modulaire** : une application Spring Boot, une JVM et un seul JAR exécutable.
- **Experience/BFF et Capability restent des modules distincts**. Leur communication est un appel Java via l'API publique de Capability, pas un appel HTTP.
- **BFF mince** : contrat REST, validation de forme, adaptation des réponses et erreurs. Aucune règle métier, persistance ou orchestration.
- **Capability** : règles métier, protection des invariants, cycle des statuts, stockage et orchestration. La validation BFF ne remplace pas les contrôles métier de Capability.
- **Intégration partenaire** : Feign/Resilience4j vers WireMock, jamais entre BFF et Capability.

## Notre façon de travailler

1. Choisir **un seul sprint**, puis une seule tâche (identifiant `INT-XX`, par exemple `INT-01`) de sa fiche. La numérotation est unique à l'échelle du projet, continue entre les sprints et stable après attribution ; ne pas réutiliser un identifiant.
2. Présenter numéro, nom, objectif, périmètre, fichiers concernés et vérifications prévues, puis **attendre le feu vert explicite de l'utilisateur avant le développement de cette tâche**. Un accord sur le sprint ou sur la tâche précédente ne suffit pas. Une demande explicite de développer cette tâche constitue le feu vert.
3. Réaliser uniquement la tâche autorisée, lancer les vérifications pertinentes et expliquer le résultat. Son feu vert couvre les modifications, tests et note compris dans son périmètre : pas de confirmations redondantes.
4. Après la tâche terminée, créer sa note numérotée selon la convention ci-dessous et la lier depuis la fiche du sprint. Ne cocher la tâche que si ses critères sont effectivement remplis.
5. Fournir le bilan, puis **s'arrêter et attendre un nouveau feu vert pour développer la tâche suivante**, même dans le même sprint.
6. Clore le sprint uniquement avec ses critères remplis et ses preuves enregistrées. Attendre un accord explicite avant d'ouvrir le sprint suivant. Un plan n'est pas une autorisation d'implémenter tout le projet.

Les tâches restent décochées tant qu'elles ne sont pas vérifiées. Une compilation réussie ne prouve ni le fonctionnement métier ni une CI verte. Un blocage d'environnement doit être noté, pas contourné en supprimant un test utile.

**Pas de durée imposée** : un sprint est ici un objectif pédagogique avec un livrable vérifiable, pas une promesse calendaire. Les tests sont écrits à chaque sprint, pas reportés au sprint de consolidation.

## Notes de fin de tâche

Les comptes rendus vivent dans **`notes/` à la racine**, avec un sous-dossier par sprint. Voir l'[index des notes](../../notes/README.md). Les fiches de travail restent ici, dans `docs/stages/`.

- **Chemin** : `notes/sprint-XX/INT-YY-<nom-de-la-tache-en-kebab-case>.md`.
- **Titre** : `# INT-YY — <Nom de la tâche>`.
- **Exemple** : `notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md`, titre `# INT-01 — Inspecter et fixer les frontières`.
- **Identifiants** : `INT-01` à `INT-36` remplacent les anciennes références `01.1` à `09.4`, dans l'ordre des fiches. Le numéro du sprint reste distinct. INT-37 a été ajoutée au sprint 01 pour l'initialisation Git demandée ensuite. Les identifiants attribués ne changent pas ; attribuer un nouvel identifiant disponible pour une nouvelle tâche.
- **Contenu** : objectif, réalisation et explication technique, décisions motivées, fichiers concernés, vérifications avec commandes/résultats réellement observés, limites et points à retenir. Signaler les tests non exécutés et les blocages.
- **Traçabilité** : ajouter un lien vers la note depuis la fiche du sprint et l'index des notes ; conserver l'identifiant exact de la tâche. Une note partielle ou rétrospective ne justifie pas de cocher une tâche non terminée et ne donne pas de feu vert pour poursuivre.

Les tirets du nom de fichier sont le séparateur portable pour « numéro : nom ». La note complète le journal de sprint, elle ne le remplace pas. Aucun compte rendu de réussite n'est rédigé à l'avance.

## Roadmap

| Sprint | Objectif | Démonstration attendue | Statut |
|---|---|---|---|
| [01 — Socle modulaire](sprint-01-socle.md) | Build et frontières EXP/Capability | Une application assemblée, un appel Java entre modules | À ouvrir |
| [02 — Contrat REST](sprint-02-rest.md) | POST, GET, validation et erreurs | `202` puis suivi `PENDING`, stockage pédagogique en mémoire | Planifié |
| [03 — Persistance](sprint-03-mongodb.md) | MongoDB, UUID, états | Demande relue après redémarrage ; échec de stockage sans faux `202` | Planifié |
| [04 — Asynchronisme](sprint-04-async.md) | Spring Events et `@Async` | Acceptation indépendante du traitement ; limites de durabilité expliquées | Planifié |
| [05 — Orchestration](sprint-05-orchestration.md) | Graphe A–F et concurrence bornée | Dépendances démontrées par tests contrôlés | Planifié |
| [06 — Partenaire et résilience](sprint-06-partenaire.md) | Feign, WireMock, timeouts et retries sûrs | Parcours réel vers le mock, succès et échecs explicites | Planifié |
| [07 — Validation et observabilité](sprint-07-validation.md) | Parcours complet, logs et mesures du labo | Scénarios reproductibles et mesures distinctes HTTP/métier | Planifié |
| [08 — CI](sprint-08-ci.md) | GitHub Actions + Maven `verify` | Run réel et test cassant détecté, sans déploiement | Planifié |
| [09 — CD et entretien](sprint-09-cd-theorique.md) | **Théorie uniquement** | Schéma de livraison et restitution orale | Planifié |

## Point de départ observé

Lors de la préparation du plan :

- Un squelette partiel existe dans `experience-api/`, avec un POM et un client `RestClient` vers Capability.
- Des répertoires `capability-api/` et `contracts/` existent ; leur présence ne prouve pas des modules fonctionnels.
- Aucun POM racine ni Maven Wrapper n'est présent dans l'inventaire effectué. Aucun sprint n'est considéré terminé.
- Le client HTTP interne existant ne correspond pas à l'architecture retenue. Son remplacement par une frontière Java est une tâche du sprint 01, pas un travail effectué par ce plan.
- `AGENTS.md` n'est pas présent à la racine ; les consignes disponibles sont dans `.agents/skills/farsameh/AGENTS.md`.

Aucun code existant n'est supprimé ou réécrit lors de cette planification. L'outillage et l'état des fichiers seront revérifiés au début du sprint 01.

## Définition de fin commune

- [ ] Le périmètre de la fiche est respecté, sans fonctionnalité du sprint suivant ajoutée en avance.
- [ ] Les tests du changement sont présents et leurs résultats réellement observés.
- [ ] La démonstration est reproductible et documentée avec ses prérequis.
- [ ] Les limites connues sont expliquées ; aucune donnée réelle ni secret n'est utilisé.
- [ ] L'utilisateur peut expliquer la responsabilité de chaque module et un cas de panne.
- [ ] La revue de fin est faite et le passage à la suite est validé.

## Journal à compléter en clôture de chaque sprint

| Élément | À renseigner |
|---|---|
| Date et incréments réalisés | Non renseignés |
| Fichiers concernés | Non renseignés |
| Commandes exécutées et résultats | Non exécutées dans le cadre du plan |
| Démonstration et preuves | Aucune pour le moment |
| Limites / blocages | À constater pendant le sprint |
| Accord pour le sprint suivant | Non obtenu |
