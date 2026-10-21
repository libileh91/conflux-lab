# ConfluxLab — Stories et sprints

**[Simulation ConfluxLab]** Ce plan sert à apprendre progressivement. Il ne décrit pas le déroulement historique d'un projet AXA.

Le [DAT](../dat/ConfluxLab.md) explique l'architecture ; les fiches ci-dessous décrivent le travail à faire ; les [notes](../../notes/README.md) conservent ce qui a réellement été réalisé et vérifié.

## Comment lire une story ?

Une story répond à trois questions : **pour qui ? quel besoin ? pourquoi ?**

> En tant que client de l'API, je veux consulter l'état de ma demande avec son identifiant, afin de savoir où elle en est.

Chaque tâche `INT-XX` garde le même format :

1. **Statut et prérequis** : ce qui est fait, ce qui manque et ce qui doit venir avant.
2. **Story** : « En tant que…, je veux…, afin de… ».
3. **À faire** : les petites actions nécessaires, sans cours technique complet.
4. **Terminé quand** : les résultats observables qui permettent de vérifier le travail.

Certaines stories sont techniques : leur bénéficiaire est alors le développeur. Par exemple, un build reproductible sert à construire le projet avec les outils prévus. On ne transforme pas artificiellement chaque tâche d'outillage en besoin du client final.

**Une case de critère cochée ne signifie pas que toute la story est terminée.** Son statut et l'ensemble de ses critères font foi. Une réécriture de fiche n'est jamais une preuve de réalisation.

## Quelques mots utiles

| Mot | Sens dans ce projet |
|---|---|
| API | Interface par laquelle un client ou un module demande un service |
| BFF / Experience | Partie qui reçoit les requêtes HTTP et adapte les réponses ; elle ne porte pas le métier |
| Capability | Partie qui porte les règles métier, les états, le stockage et le traitement |
| Monolithe modulaire | Une application, avec des parties de code séparées et des appels Java entre elles |
| Contrat / DTO | Description des données échangées / objet Java qui transporte ces données |
| Build | Étapes qui compilent, testent et préparent les fichiers produits par le projet |
| Test unitaire / d'intégration | Vérification d'un comportement isolé / de plusieurs composants qui travaillent ensemble |
| UUID / corrélation | Identifiant utilisé pour retrouver une demande dans les données et les logs, pas pour garantir l'absence de doublon métier |

Les autres termes indispensables sont expliqués dans chaque sprint au moment où ils apparaissent.

## Notre façon de travailler

1. Choisir **une seule story** et présenter son périmètre et ses vérifications.
2. **Attendre ton feu vert pour cette story.** Un accord sur un sprint ou la story précédente n'autorise pas les suivantes ; une demande explicite de développer cette story constitue le feu vert.
3. Réaliser uniquement le périmètre autorisé, avec ses tests, sans te redemander une confirmation pour chaque modification déjà prévue.
4. Écrire la note de fin, montrer les résultats et signaler tout test non exécuté ou blocage.
5. **S'arrêter avant la story suivante**, même dans le même sprint. La clôture d'un sprint exige ses critères remplis et un point ensemble.

Les tests accompagnent chaque story : ils ne sont pas reportés au sprint 07. **Préférence utilisateur : strict minimum de scénarios utiles, réutilisation de l'existant et exécution ciblée par défaut ; pas de course à la couverture ni de matrices exhaustives.** Les sprints n'ont pas de durée imposée ; chacun vise un résultat pédagogique vérifiable. La CD reste uniquement théorique.

## Roadmap

| Sprint | Ce que tu sauras faire à la fin | Stories | État actuel |
|---|---|---|---|
| [01 — Socle modulaire](sprint-01-socle.md) | Construire et démarrer une application aux modules séparés | INT-01 à INT-04 ; INT-37 | Clôturé techniquement et documenté sur demande utilisateur (2026-10-21) ; restitution orale non évaluée |
| [02 — Contrat REST](sprint-02-rest.md) | Accepter une demande et consulter son état | INT-05 à INT-08 | INT-05 à INT-08 terminées techniquement ; passage à INT-09 demandé, restitution orale non évaluée |
| [03 — MongoDB](sprint-03-mongodb.md) | Conserver une demande après redémarrage | INT-09 à INT-12 | INT-09 à INT-12 terminées techniquement ; clôture et restitution pédagogique à confirmer |
| [04 — Asynchronisme](sprint-04-async.md) | Séparer la réponse HTTP du travail en arrière-plan | INT-13 à INT-16 | Planifié |
| [05 — Orchestration](sprint-05-orchestration.md) | Faire avancer les six opérations selon leurs dépendances | INT-17 à INT-20 | Planifié |
| [06 — Partenaire et résilience](sprint-06-partenaire.md) | Appeler un partenaire simulé et traiter ses pannes sans retry aveugle | INT-21 à INT-24 | Planifié |
| [07 — Validation et mesures](sprint-07-validation.md) | Démontrer le parcours, expliquer un échec et mesurer le labo | INT-25 à INT-28 | Planifié |
| [08 — CI](sprint-08-ci.md) | Faire compiler et tester automatiquement le projet par GitHub Actions | INT-29 à INT-32 | Planifié |
| [09 — CD et entretien](sprint-09-cd-theorique.md) | Expliquer la livraison et défendre les choix à l'oral, sans déployer | INT-33 à INT-36 | Planifié — théorie |

### Ordre de travail après les contrats initiaux

Les identifiants ne changent pas ; ils ne sont pas un ordre de développement obligatoire.

- **Réalisé sur feu vert :** INT-05 a défini les types manquants et leurs tests ; le `clean verify` global a réussi et clôt le blocage d'INT-02.
- **Réalisé ensuite sur feu vert INT-03 :** assemblage unique, appel Java et indisponibilité explicite (`503`) vérifiés, sans acceptation réelle.
- **Réalisé ensuite sur feu vert INT-04 :** frontières et détection de violations vérifiées dans le build global (76 tests), sans modification du métier.
- **Clôture demandée le 2026-10-21 :** [bilan technique et synthèse documentaire du sprint 01](sprint-01-socle.md#journal-de-clôture--2026-10-21), sans évaluation orale de l'apprenant.
- **Réalisé ensuite sur feu vert INT-06 :** validation directe, UUID et conservation mémoire avant `202`, sans suivi ni traitement.
- **Réalisé ensuite sur feu vert INT-07 :** suivi des métadonnées en lecture seule, UUID inconnu `404`, identifiant mal formé `400` ; perte au redémarrage démontrée.
- **Réalisé ensuite sur feu vert INT-08 :** refus sans enregistrement, confidentialité des réponses et isolation vérifiés ; 181 tests globaux, correctif d'écho du chemin dans `instance`.
- **Passage demandé ensuite :** commit local INT-08 `fedb8b7`, puis INT-09 réalisée sur demande « commit la 08 & passer 09 » ; modèle/dates et transitions locales, 2 tests ajoutés et 3 tests ciblés réussis. La restitution pédagogique du sprint 02 reste non évaluée.
- **Réalisé ensuite sur feu vert INT-10 :** commit INT-09 `185bdd7`, remplacement de la mémoire de production par MongoDB, insertion acquittée avant `202`, UUID unique et refus sans faux reçu ; 2 nouveaux tests et 1 test POST/GET réutilisé réussis.
- **Réalisé ensuite sur feu vert INT-11 :** complément théorique INT-10 commité et poussé (`66e0ade`), puis transitions MongoDB atomiques avec filtre de statut/date, protection des états finaux et suivi confidentiel ; 2 nouveaux tests et 2 tests du modèle réutilisés réussis.
- **Réalisé ensuite sur feu vert INT-12 :** commit local INT-11 `af3f215`, raccordement Failsafe, réutilisation de 4 cas MongoDB et ajout d'un seul test redémarrant le JAR sur la même base ; 1 contrôle Surefire ciblé et 5 cas Failsafe réussis.
- **Suite :** clôture pédagogique du sprint 03 à confirmer ; INT-13 nécessite un nouveau feu vert individuel. Aucun asynchronisme n'est lancé.
- La préparation avancée d'INT-05 ne lance ni l'assemblage ni les autres stories du sprint 02.

## Architecture à conserver

Une application Spring Boot et un seul JAR exécutable. BFF et Capability restent séparés : le BFF gère l'HTTP ; Capability protège le métier et possède son stockage. La communication interne est un appel Java via l'interface publique de Capability. Feign/Resilience4j servira uniquement au partenaire externe fictif.

Le dernier passage non filtré observé, lors d'INT-08, réussit avec 181 tests : 59 contrats, 43 Capability, 4 BFF et 75 assemblage/HTTP/architecture. INT-06 conserve le payload avant `202` ; INT-07 consulte les seules métadonnées en `200`, avec `404` pour l'absence et `400` pour les formats invalides testés. À cette étape historique, deux POST/GET indépendants et la perte après redémarrage ont été démontrés sur le JAR réel, sans stockage durable ni traitement. INT-08 a complété les refus et corrigé l'écho de données rejetées dans les erreurs ; le passage à INT-09 a ensuite été demandé sans évaluer la restitution orale. INT-09 ajoute le modèle de dates et les transitions internes, vérifiés par 3 tests ciblés. INT-10 branche ensuite MongoDB et vérifie l'insertion avant acceptation, l'unicité et le suivi avec 3 tests ciblés. INT-11 sécurise ensuite les transitions par une mise à jour conditionnelle atomique, vérifiée par 4 tests ciblés. INT-12 démontre la relecture après redémarrage du JAR avec la même base, dans un `clean verify` du réacteur complet : 1 contrôle Surefire sélectionné + 5 cas Failsafe réussis, sans exécuter la suite Surefire complète. Voir le [README du projet](../../README.md) pour les commandes et les limites.

## Notes de fin de tâche

- **Dossier** : `notes/sprint-XX/` à la racine, pas sous `docs/stages/`.
- **Fichier** : `INT-YY-<nom-de-la-tache-en-kebab-case>.md` ; **titre** : `# INT-YY — <Nom de la tâche>`.
- **Exemple** : `notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md`.
- **Contenu** : cours/tutoriel junior selon le [guide des notes](../../notes/README.md), avec problème, étapes réalisées, code/config important et commandes expliqués, preuves et limites. Distinguer résultats historiques, vérifications actuelles et exemples non exécutés.
- **Suivi** : centraliser les actions restantes et points à retenir dans [notes/todos.md](../../notes/todos.md), puis les lier depuis le tutoriel ; ne pas maintenir une seconde liste de TODO dans chaque note.
- **Liens** : depuis la fiche du sprint et `notes/README.md`. Une note partielle ou rétrospective ne prouve pas une story terminée.

Les IDs INT-01 à INT-37 restent uniques et stables, même après cette réécriture. Toute nouvelle tâche recevra un identifiant disponible, sans renuméroter les existantes.

## Définition de fin commune

- [ ] Les critères de la story sont remplis et ses vérifications sont réellement observées.
- [ ] La démonstration est reproductible, avec ses prérequis et ses limites.
- [ ] La note est écrite et liée ; aucune donnée réelle ni aucun secret n'est ajouté.
- [ ] Tu peux expliquer le résultat et un cas d'échec avant d'autoriser la suite.

## Journal à compléter en clôture de chaque sprint

| Élément | À renseigner après réalisation |
|---|---|
| Date et stories terminées | Identifiants et résultats réels |
| Fichiers concernés | Liens vers les fichiers et notes |
| Vérifications | Commandes, résultats et tests non exécutés |
| Démonstration | Ce qui a été observé, pas seulement prévu |
| Limites / blocages | Ce qui reste à faire et pourquoi |
| Accord pour le sprint suivant | À demander explicitement |
