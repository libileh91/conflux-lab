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

- **Feu vert par incrément complet — préférence utilisateur du 2026-10-22** : lire le plan et présenter le résultat, les IDs couverts, les fichiers et les vérifications utiles. Un lot peut regrouper plusieurs IDs existants ; un seul accord couvre cet ensemble et ses étapes internes, pas le lot suivant ni les opérations distantes non autorisées. Ne pas transformer une classe, une méthode ou une notion en tâche indépendante par défaut ; ne pas imposer non plus une seule tâche ou note par sprint. Le découpage suit les sujets et résultats cohérents.
- **Identifiants des tâches** : utiliser `INT-XX` dans toutes les fiches et notes, avec une séquence unique et stable entre sprints. Ne pas recommencer la numérotation dans chaque sprint ni réutiliser un identifiant.
- **Une note par incrément, pas par sous-étape** : sous `notes/sprint-XX/`, conserver le premier ID dans le fichier et citer tous les IDs regroupés dans le titre/contexte ; exemple futur `INT-21-creer-une-famille-de-bout-en-bout.md`, titre `# INT-21 + INT-22 — Créer une famille de bout en bout`. Suivre `notes/README.md` : flux du code, extraits importants, décisions et preuves réelles, pas un cours exhaustif sur les tests. Lier depuis la fiche et l'index ; conserver les preuves historiques et signaler honnêtement les parties non réalisées. Sur demande de consolidation, les anciens fichiers peuvent être remplacés, avec migration des liens, plutôt que gardés en doublon.
- **Suivi centralisé mais sobre** : lire et maintenir les entrées utiles `TD-Bxxx` / `TD-Rxxx` de `notes/todos.md` avec leurs IDs et preuves. Pas de nouvelle entrée automatique par sous-étape ; le plan porte déjà les critères. Un repère n'est pas une connaissance évaluée et le registre ne vaut pas feu vert.
- **Stories pour débutant** : formuler « En tant que…, je veux…, afin de… », avec actions courtes, vocabulaire expliqué et critères testables ; ne pas changer les IDs ou les statuts lors d'une reformulation.
- **Arrêt après l'incrément autorisé** : fournir sa note et un bilan bref, puis attendre le feu vert du prochain ensemble. Pas de questionnaire ou de nouveau document de clôture imposé après chaque modification ; ne pas déclarer une restitution orale non évaluée.
- **BFF mince** : contrat REST, validation de forme et adaptation des réponses ; règles métier, invariants, statuts, persistance et orchestration dans Capability. La validation du BFF ne remplace pas les contrôles de Capability.
- Expliquer d'abord le résultat et le flux à travers le code, puis les choix, preuves et limites utiles. Les tests servent de preuves, sans devenir le principal livrable pédagogique.
- Corriger les approximations techniques avec explications, sans feindre de connaître les versions, paramètres de pools, métriques ou décisions réelles d'AXA.
- **Juste niveau de complexité — préférence utilisateur du 2026-10-22, prioritaire** : ni overkill ni simplification trompeuse, avec une vigilance particulière contre l'overkill. Pour le code, les tests et les explications, chaque ajout doit servir un critère de la story, un risque concret ou une notion indispensable. Préserver les invariants et nuances utiles, sans anticiper des besoins de production ni rechercher l'exhaustivité. Cette règle prévaut sur les demandes antérieures de profondeur documentaire.
- **Documentation pédagogique proportionnée** : expliquer le pourquoi, le flux et les limites nécessaires à un junior, avec un exemple, schéma ou extrait réel lorsqu'il aide. Ne pas transformer chaque sujet en catalogue de scénarios, politiques ou alternatives, ni déplacer automatiquement le surplus en annexe. Adapter la longueur au besoin, sans objectif de nombre de lignes ; rester compréhensible et techniquement exact. Distinguer réalisation, illustration et évolution future. La documentation n'autorise aucun développement, test supplémentaire ou réexécution.
- **Notes par fragments — préférence durable de l'utilisateur** : suivre `notes/README.md`. Montrer un court extrait utile puis l'expliquer, sans classe complète ni imports, constructeurs ou câblage répétitifs. Garder ou améliorer les schémas et tableaux qui aident réellement. Le code et le montage des tests peuvent être omis ; préserver un bilan honnête des résultats et limites sans toucher aux tests applicatifs. La clarté vient du flux et de l'explication, pas du volume recopié.
- **Tests au strict minimum — préférence utilisateur du 2026-10-21** : ajouter uniquement les scénarios nécessaires aux critères de la story et aux risques concrets du changement (cas nominal et refus/échec critique utile). Réutiliser les tests existants ; éviter les matrices exhaustives, les variantes redondantes et la multiplication des niveaux de test pour le même comportement. Aucun objectif de nombre de tests ni de couverture chiffrée. Exécuter par défaut les seuls tests ciblés concernés ; réserver la suite globale, les tests d'intégration et les démonstrations supplémentaires aux besoins réels de validation, à la CI prévue ou à une demande explicite. Ne pas supprimer les tests déjà présents sans demande ; signaler honnêtement les vérifications non exécutées.
- Avant toute modification : expliquer ce qui va changer ; après : nommer les fichiers concernés, les vérifications faites, ce qui n'a pas été testé et les décisions restant ouvertes.
- Ne jamais prétendre qu'une CI est verte ou qu'un test a été exécuté sans preuve.
- Ne pas utiliser de données assurés réelles ni de secrets dans Git, fixtures, logs ou exemples.
- Ne pas exécuter de commandes destructives ou modifier des ressources distantes sans autorisation explicite.

## Réponse attendue

Répondre en **français technique clair**, en donnant la priorité au résultat et au code utile, sans longue cérémonie. Les réponses orales de 30–60 secondes et les relances corrigées sont réservées au mode entretien ou à une demande explicite.
