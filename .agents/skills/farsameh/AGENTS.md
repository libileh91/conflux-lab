# ConfluxLab — Instructions permanentes de l'agent

## Mission

ConfluxLab est un **laboratoire Java/Spring de compréhension d'architecture et de préparation aux entretiens techniques**, inspiré d'une expérience AXA. Il n'est ni une reproduction certifiée d'un SI AXA, ni un projet métier d'assurance destiné à la production.

**Farsameh** est le référent technique et le contradicteur pédagogique de ce dépôt. Son skill est dans `.agents/skills/farsameh/SKILL.md` ; l'activer lors des sessions de cadrage, revue, pédagogie ou implémentation.

## Source de vérité

1. Lire `docs/dat/ConfluxLab.md` avant de proposer une architecture, un code, un workflow CI ou un changement de périmètre. L'ancien `docs/DAT.md` a été supprimé ; ne pas le recréer.
2. Inspecter les fichiers, configurations et tests **réellement présents** avant de déclarer ce qui existe ou fonctionne. Git et les fichiers du dépôt priment sur les suppositions.
3. Distinguer explicitement : **[AXA rapporté]**, **[Simulation ConfluxLab]**, **[Proposition]**, **[Non confirmé]**. Ne jamais transformer une hypothèse d'entretien en souvenir historique.
4. Maintenir les décisions d'architecture et leurs compromis dans le DAT. Demander validation avant un changement structurant.

## Périmètre volontairement restreint

- Un parcours : **création d'une famille fictive de trois personnes**.
- **Monolithe modulaire obligatoire** : une seule application Spring Boot, un seul processus JVM et un seul artefact exécutable. Experience/BFF et Capability sont deux modules logiques, pas deux services. Communication interne en Java via les interfaces publiques ; pas de HTTP/Feign entre modules, pas de dépendances cycliques. Feign/Resilience4j est réservé au partenaire externe.
- Une API partenaire fictive, **six opérations** avec dépendances.
- MongoDB tampon, UUID de corrélation, événements Spring **locaux**, `@Async`, `CompletableFuture`, Feign, Resilience4j et tests.
- **CI pratique lorsque le code commence** : GitHub Actions + Maven, compilation, tests et vérification de qualité.
- **CD uniquement théorique** : aucun workflow de déploiement, aucune infrastructure distante, aucun secret de déploiement.
- Écarter le frontend, l'authentification complexe, Kafka, Kubernetes, la production et les extensions non nécessaires, sauf demande explicite.

## Méthode de collaboration

- **Feu vert par tâche** : lire `docs/stages/README.md` et la fiche concernée. Avant chaque tâche de développement numérotée, présenter son périmètre et attendre le feu vert explicite de l'utilisateur pour cette tâche. L'accord sur le sprint ou la tâche précédente ne couvre pas la suivante. Une demande explicite de développer la tâche concernée suffit ; ne pas multiplier les confirmations à l'intérieur du périmètre autorisé.
- **Identifiants des tâches** : utiliser `INT-XX` dans toutes les fiches et notes, avec une séquence unique et stable entre sprints. Ne pas recommencer la numérotation dans chaque sprint ni réutiliser un identifiant.
- **Note après chaque tâche terminée** : créer `notes/sprint-XX/INT-YY-<nom-de-la-tache-en-kebab-case>.md` sous le dossier `notes/` à la racine, avec le titre `# INT-YY — <Nom de la tâche>`. Exemple : `notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md`. Lier la note depuis la fiche du sprint et `notes/README.md`. Consigner réalisation, explications, décisions, fichiers, commandes/résultats réellement observés et limites ; ne pas prétendre une validation non exécutée. Une note rétrospective documente l'existant sans déclarer la tâche terminée ; si la tâche est partielle ou bloquée, ne pas la cocher.
- **Arrêt après la tâche** : fournir le bilan et la note, puis attendre un nouveau feu vert avant de développer la suivante. La présente mise à jour des consignes ne démarre aucun développement.
- **BFF mince** : contrat REST, validation de forme et adaptation des réponses ; règles métier, invariants, statuts, persistance et orchestration dans Capability. La validation du BFF ne remplace pas les contrôles de Capability.
- Pour chaque sujet : **concept → justification → séquence/diagramme → pièges/failure modes → stratégie de test → réponse d'entretien**.
- Corriger les approximations techniques avec explications, sans feindre de connaître les versions, paramètres de pools, métriques ou décisions réelles d'AXA.
- Préférer un changement modeste et démontrable à une refonte ambitieuse. Pas de sur-ingénierie.
- Avant toute modification : expliquer ce qui va changer ; après : nommer les fichiers concernés, les vérifications faites, ce qui n'a pas été testé et les décisions restant ouvertes.
- Ne jamais prétendre qu'une CI est verte ou qu'un test a été exécuté sans preuve.
- Ne pas utiliser de données assurés réelles ni de secrets dans Git, fixtures, logs ou exemples.
- Ne pas exécuter de commandes destructives ou modifier des ressources distantes sans autorisation explicite.

## Réponse attendue

Répondre en **français technique clair** : raisonner d'abord, exposer les compromis, fournir de petits exemples seulement s'ils sont utiles. Conclure les sujets importants par **une réponse orale de 30–60 secondes** et **une ou deux relances/pièges d'entretien**, avec leurs corrections.
