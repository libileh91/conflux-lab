# Actions et repères — ConfluxLab

Registre de suivi, pas second cours ni journal des stories : le [plan](../docs/stages/README.md) porte les critères, les [cours regroupés](README.md) expliquent le flux et les preuves. **[Simulation ConfluxLab]** : les réalisations et paramètres du laboratoire ne sont pas des faits historiques AXA.

- **TD-B** : action ouverte ou archive résolue dans son périmètre ; **TD-R** : référence documentaire, jamais maîtrise de l'apprenant évaluée ni tâche terminée.
- IDs et ancres stables : actualiser l'existant, sans nouvelle entrée automatique par story, notion ou test. Les noms de fichiers servent à retrouver les sources, pas à inventorier tout le code.
- **Aucune entrée ne vaut feu vert.** Un accord couvre l'incrément complet annoncé, pas le suivant ni une opération distante non autorisée ; une réorganisation documentaire n'autorise aucun développement.
- **Preuves historiques uniquement, reprises des cours ci-dessous ; aucune nouvelle exécution pour cette réécriture.** Les sélections se recouvrent : ne pas additionner les comptes, ni les présenter comme une suite actuelle ou une CI verte. Les rapports sous `target/`, ignorés par Git, peuvent avoir disparu.
- Les cibles « à créer » restent futures. Aucun endpoint d'authentification ni autre fonctionnalité d'un exemple de format n'entre dans le périmètre ; aucun secret dans le dépôt ou le chat.

**Sources :** [Architecture — INT-01/03/04][C1] · [Build et versionnement — INT-02/37][C1-build] · [REST — INT-05 à INT-08][C2] · [Modèle et transitions — INT-09/11][C3] · [Insertion et persistance — INT-10/12][C3-mongo] · [Replica set — complément théorique INT-10][C3-replica] · [Prise en charge async — INT-13/14/16][C4] · [Pool et erreurs async — INT-15][C4-pool] · [Orchestration — INT-17 à INT-20][C5].

## À regarder maintenant

| Action | Situation et condition |
|---|---|
| [TD-B006](#td-b006) — Navigation Zed | **Manuel, non constaté** : interaction GUI à vérifier par l'utilisateur, distincte du diagnostic LSP/build. |
| [TD-B007](#td-b007) — Wrapper Windows | **Optionnel, non testé** : nécessite Windows, JDK 21 et réseau. |
| [TD-B008](#td-b008) — `gh` ou SSH | **Seulement si nécessaire, non bloquant** : le push Git HTTPS historique a réussi. |
| Suite du parcours | [INT-25 à INT-28 — Démonstration et mesures](../docs/stages/sprint-07-validation.md) reste à autoriser ; [TD-B018](#td-b018) conserve le raccordement et le complément réalisé sur les pannes. |

## Actions ouvertes — manuelles, optionnelles ou futures

### TD-B006

| Suivi | **Confirmer la navigation dans l'interface Zed** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build], diagnostic éditeur INT-02 ; `FamilyController.java`, raccourcis/réglages utilisateur Zed hors dépôt. |
| Action · preuve attendue | Sur `FamilyRequests` (remplace l'ancien `CapabilityClient` depuis INT-03), `UUID` puis `ResponseEntity`, essayer F12 ou Ctrl + Shift + B, et la palette si nécessaire. Constater visuellement la bonne définition ; distinguer reconnaissance du raccourci et navigation effective. |
| Statut · dépendance | 🧭 **Manuel — non constaté** ; JDTLS opérationnel et disponibilité de l'utilisateur nécessaires. Les réponses LSP historiques ne prouvent ni pression de touche ni ouverture d'onglet ; ce contrôle est distinct du build global. |

### TD-B007

| Suivi | **Vérifier le Wrapper dans un environnement Windows** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build], portabilité INT-02 ; `mvnw.cmd`, `.gitattributes`, `.mvn/wrapper/maven-wrapper.properties`. |
| Action · preuve attendue | Contrôler la version puis `validate` avec le Wrapper Windows ; consigner environnement et résultats. Maven épinglé et POM validés ne signifieraient pas `verify` global réussi. |
| Statut · dépendance | ⏳ **Optionnel — non testé** ; machine Windows, JDK 21 et réseau requis, environnement absent des preuves fournies. Ni Linux ni les fins de ligne configurées ne simulent cette validation. |

### TD-B008

| Suivi | **Revoir `gh` ou SSH seulement si un usage le nécessite** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build], complément à INT-37 déjà terminée ; configuration d'authentification utilisateur hors dépôt. |
| Action · preuve attendue | Si besoin explicite, authentifier le canal choisi par son mécanisme normal, puis réussir une lecture sans exposer token ou clé privée. Ne pas modifier Git HTTPS pour cette seule raison ; accord requis avant changement de configuration/authentification. |
| Statut · dépendance | ⏳ **Non prioritaire, non bloquant** : erreurs historiques `gh`/SSH, mais push Git HTTPS réussi. Les canaux sont distincts ; leur état actuel n'a pas été revérifié et aucun blocage Git actuel n'est déduit. |


## Archives résolues — périmètre et preuve historiques

### TD-B018

| Archive | **Raccorder le parcours partenaire et la finalisation sans attendre dans le pool** |
|---|---|
| Source · IDs · fichiers | Créée dans INT-20 ; réalisée sur feu vert « ok go » dans [INT-21 + INT-22](sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md). `FamilyPartnerClient`, `FeignFamilyOperations`, configuration Feign, listener et tests du parcours. |
| Résultat · preuve | Pool existant explicitement injecté ; graphe lancé uniquement après claim gagnant, finalisation conditionnelle après terminaison. Feign/HC5 sans retry, délais configurés. **14 Surefire + 8 Failsafe réussis**, aucun échec/erreur/ignoré : 3 nouveaux scénarios, 19 cas réutilisés ; pas de suite globale. Le JAR conserve `FAILED` après redémarrage sans nouvel appel partenaire. |
| Statut · limites | ✅ **Réalisée le 2026-10-22.** Une erreur d'écriture finale est journalisée sans données privées, pas transformée en faux acquittement ; `PROCESSING` ou une issue d'écriture inconnue reste possible. Ni reprise, rollback ni idempotence. |
| Complément · preuve · limites | **[INT-23 + INT-24](sprint-06/INT-23-terminer-malgre-une-panne-partenaire.md) réalisées le 2026-10-23**, sur feu vert « commit & go next INT ». Diagnostic par UUID/opération, `400`, `503` et timeout de lecture sans répétition ; `clean verify` ciblé : **7 Surefire + 4 Failsafe**, aucun échec/erreur/ignoré, pas de suite globale. Aucune opération n'a de garantie de répétition sûre : pas de retry Resilience4j. Après timeout, l'issue distante reste inconnue ; aucun rollback. INT-25 à INT-28 restent à autoriser. |

Les accords mentionnés ci-dessous ont été reçus pour les réalisations passées, sans autoriser la suite. Les anciennes appellations mémoire désignent leur version historique : depuis INT-10, `InMemoryFamilyRequests` / son test sont devenus `FamilyRequestService` / `FamilyRequestServiceTest` ; `InMemoryFamilyRequestStore` ne subsiste que sous `capability-api/src/test/`, sans repli mémoire de production.

### TD-B001

| Archive | **Définir les contrats et les trois DTO manquants** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build] → [REST][C2], blocage INT-02 résolu par INT-05 après cadrage INT-01 ; `contracts/src/` et [contrat HTTP](../docs/dat/http-contracts.md). |
| Action · preuve historique | Cinq types réels, dont `CreateFamilyRequest`, `RequestAccepted`, `RequestView`, contraintes et JSON cohérents : 59 cas réussis. Pas de classes vides de dépannage ni de service métier ajouté ; les DTO seuls ne prouvent pas une API fonctionnelle. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-05** ; contrats disponibles et compilation débloquée. La preuve globale associée est conservée dans [TD-B002](#td-b002). |

### TD-B002

| Archive | **Réévaluer le build complet après définition des types** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build] et [REST][C2], clôture build INT-02 après INT-05 ; POM racine/modules, `contracts/src/`, `experience-api/src/`. |
| Action · preuve historique | Réacteur entier, Experience comprise : `clean verify` global réussi dans INT-05, 59 cas, aucun échec/erreur/ignoré. Les bibliothèques auparavant vides et les sélections partielles n'étaient pas une preuve globale ; aucune autre erreur n'est masquée par exclusion. |
| Statut · dépendance | ✅ **Archive résolue**, INT-02 clôturée pour le build après [TD-B001](#td-b001). Compilation, tests réellement exécutés et assemblage applicatif restent des preuves distinctes ; ce résultat n'actualise pas le build courant. |

### TD-B003

| Archive | **Assembler le monolithe et remplacer l'appel HTTP interne** |
|---|---|
| Source · IDs · fichiers | [Socle][C1], écart INT-01 traité dans INT-03 ; `application/pom.xml`, `ConfluxLabApplication.java`, `FamilyRequests.java`, `FamilyController.java`, configuration unique `application.yml`. |
| Action · preuve historique | Un seul assemblage Boot et une API publique Java : 64 tests globaux réussis, JAR inspecté et démarré sans base/partenaire à ce stade. POST/GET valides `503`, sans faux identifiant ni succès métier ; la délégation Java est vérifiée séparément. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-03**, après [TD-B002](#td-b002). Le démarrage historique sans base ne prouve pas un parcours actuel sans MongoDB ; [TD-R010](#td-r010) distingue doublure et bean réel. |

### TD-B004

| Archive | **Tester les frontières entre modules** |
|---|---|
| Source · IDs · fichiers | [Socle][C1], suivi INT-01 → INT-04 ; `ModuleArchitectureRules.java`, `ModuleArchitectureTest.java`, `ArchitectureRuleDetectionTest.java`, fixtures dans `application/src/test/`. |
| Action · preuve historique | Cycles, Capability → BFF, accès aux internals/repositories et autres dépendances interdites contrôlés sans infrastructure supplémentaire : 5 contrôles production + 7 de détection réussis, 76 tests globaux avec le contexte INT-03 conservé. Violations contrôlées refusées, graphe autorisé accepté. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-04**, après [TD-B003](#td-b003). Contrôles raccordés à `verify` ; clôture technique/documentaire du sprint 01 demandée le 2026-10-21, restitution orale non évaluée. |

### TD-B005

| Archive | **Démontrer l'acceptation, le suivi et les refus REST** |
|---|---|
| Source · IDs · fichiers | [REST][C2], suivi initié dans INT-06 puis réalisé dans INT-06/07/08 ; sources `experience-api`, `capability-api`, `contracts`. |
| Action · preuve historique | Acceptation mémoire `202`, GET `200`/`404`, entrées invalides `400`, refus sans enregistrement, isolation et erreurs génériques : 181 tests globaux et JAR réel vérifiés à INT-08. Le reçu reste `PENDING`, sans HTTP interne ni famille effectivement créée. |
| Statut · dépendance | ✅ **Archive résolue dans le seul périmètre REST mémoire**, après INT-05 et [TD-B004](#td-b004), avec feux verts individuels INT-06/07/08. Ni persistance durable, ni traitement, ni clôture pédagogique automatique ; [TD-R012](#td-r012) à [TD-R014](#td-r014) gardent les distinctions. |

### TD-B009

| Archive | **Persister la demande avant de l'accepter** |
|---|---|
| Source · IDs · fichiers | [Modèle et transitions][C3] → [Insertion et persistance][C3-mongo], INT-09 → INT-10 ; service, `FamilyRequestStore.java`, `MongoFamilyRequestStore.java`, `StoredFamilyRequest.java`, configuration, POM et `application.yml`. |
| Action · preuve historique | MongoDB remplace la mémoire en conservant les frontières ; insertion acquittée avant reçu. Deux cas nouveaux et un réutilisé réussis : UUID/payload relus et unicité réelle sans écrasement ; panne d'insertion simulée sans faux `202`. Modèle Java seul ≠ persistance. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-10**, INT-09 terminée/commitée et Docker disponible. Base temporaire réelle, pas base utilisateur ; ni redémarrage ni suite globale revendiqués à cette étape. Une erreur peut néanmoins laisser l'issue d'écriture inconnue, voir [TD-R016](#td-r016). |

### TD-B010

| Archive | **Sécuriser les mises à jour de statut en base** |
|---|---|
| Source · IDs · fichiers | [Insertion et persistance][C3-mongo] → [Modèle et transitions][C3], INT-10 → INT-11 ; `MongoFamilyRequestStore.java`, `StoredFamilyRequest.java`, `MongoFamilyRequestTransitionIT.java`. |
| Action · preuve historique | Conditionner atomiquement la mise à jour au statut attendu et aux dates cohérentes ; un seul gagnant concurrent `PENDING → PROCESSING`, transitions invalides et écrasement final refusés. Deux nouveaux cas MongoDB et deux cas du modèle réutilisés réussis. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-11**, après INT-10 et publication autorisée du complément théorique. Ni traitement réel, ni redémarrage, ni suite globale à cette étape ; la portée du CAS est conservée dans [TD-R018](#td-r018). |

### TD-B011

| Archive | **Démontrer la persistance et raccorder les IT à Maven** |
|---|---|
| Source · IDs · fichiers | [Modèle et transitions][C3] → [Insertion et persistance][C3-mongo], INT-11 → INT-12 ; POM racine/application, `MongoTestSupport.java`, `FamilyRequestPersistenceIT.java` et IT MongoDB existants. |
| Action · preuve historique | Même JAR relancé dans deux JVM, même base conservée : UUID, `PENDING` et date retrouvés. Failsafe observé dans `verify` : 1 cas Surefire sélectionné + 5 Failsafe réussis, dont un seul scénario ajouté ; aucune ouverture du stockage à Experience. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-12**, INT-09/10/11 réalisées, INT-11 commitée localement, Docker disponible. JVM arrêtées et conteneurs supprimés ; suite Surefire complète et CI non exécutées. Ni redémarrage MongoDB ni récupération disque prouvés ; voir [TD-R019](#td-r019). |

### TD-B012

| Archive | **Publier et consommer après insertion confirmée** |
|---|---|
| Source · IDs · fichiers | [Async][C4], INT-13 → INT-14 ; service/configuration, `FamilyRequestSubmitted.java`, `FamilyRequestListener.java`, `FamilyRequestEventsIT.java` ; store réutilisé, suivi/persistance adaptés. |
| Action · preuve historique | Publication de l'UUID après insertion, aucune publication sur échec ; listener proxifié, relecture et claim atomique unique malgré double réception. Réponse possible pendant le contrôle du worker : 5 cas Surefire + 4 Failsafe réussis, un seul scénario ajouté ; pas d'ordre imposé « après le 202 ». |
| Statut · dépendance | ✅ **Archive résolue sur reprise explicite INT-14**, après simplification/commit local INT-13. Arrêt à `PROCESSING`, sans métier, état final ou reprise ; suite complète non exécutée. Le BFF reste sans accès au store. |

### TD-B013

| Archive | **Borner l'exécution et journaliser ses erreurs** |
|---|---|
| Source · IDs · fichiers | [Async][C4] → [Pool et erreurs async][C4-pool], INT-13 → INT-15 ; `FamilyRequestConfiguration.java` (executor/`AsyncConfigurer`), `FamilyRequestListener.java`, `FamilyRequestEventsIT.java`. |
| Action · preuve historique | Executor unique dédié, workers/file bornés, rejet explicite. À 2 workers/1 place, quatrième POST `500` sans reçu ni travail sur HTTP, document `PENDING` ; erreur de lecture async simulée : journal UUID/type, sans message privé ni `FAILED` forcé. 1 cas Surefire + 3 Failsafe réussis, dont deux ajoutés. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-15**, INT-14 et plan allégé commités. Aucun nouveau champ MongoDB/HTTP, diagnostic stocké ou dépendance. Attente de fermeture par défaut 5 s, arrêt prolongé non testé ; ni crash/reprise, suite complète ou JAR rejoués. |

### TD-B014

| Archive | **Consolider les preuves du traitement contrôlé** |
|---|---|
| Source · IDs · fichiers | [Async][C4] et [Pool et erreurs async][C4-pool], suivi INT-13 → INT-16 ; IT événementiel, listener/configuration, `MongoTestSupport.java` et deux rapports ciblés INT-15 relus depuis le code commité `59c881c`. |
| Action · preuve historique | Quatre comportements couverts par trois cas existants : réponse indépendante, gagnant unique, rejet, erreur corrélée. Attentes bornées et nettoyage relus, aucun manque concret identifié pour les critères ; rapports historiques : 1 cas Surefire + 3 Failsafe sans échec/erreur/ignoré. |
| Statut · dépendance | ✅ **Archive documentaire résolue sur feu vert INT-16**, après INT-14/15 commitées. Aucun code modifié, scénario dupliqué ou test/build/JAR/crash rejoué dans INT-16 ; pas de reprise ajoutée, risque d'abandon maintenu. |

### TD-B015

| Archive | **Composer le parcours réussi à partir des contrats INT-17** |
|---|---|
| Source · IDs · fichiers | [Orchestration][C5], INT-17 → INT-18 ; `FamilyOrchestrator.java`, `FamilyOrchestratorTest.java`, contrats et fixture `ControlledFamilyOperations.java` réutilisés sans modification. |
| Action · preuve historique | A puis B/C/D ; E après B/C, F après B/D, sans barrière globale préalable. Deux cas paramétrés réussis : ordres C/D, bons UUID/identifiants, indépendance E/F et fin globale non prématurée, avec attentes bornées en Java seul. |
| Statut · dépendance | ✅ **Archive résolue sur feu vert INT-18**, contrats/fixture INT-17 commités `d6f0e86`. Aucun raccordement listener, état final, HTTP/Spring/MongoDB ou mesure de parallélisme de threads ; ces 2 cas sont réutilisés ensuite, pas additionnés. |

### TD-B016

| Archive | **Composer les échecs sans succès ni finalisation prématurés** |
|---|---|
| Source · IDs · fichiers | [Orchestration][C5], INT-18 → INT-19 ; orchestrateur/test modifiés, fixture inchangée. |
| Action · preuve historique | Un échec représentatif B empêche E/F, conserve la cause et attend C/D déjà lancées sans annulation forcée. **3 cas ciblés réussis = les 2 nominaux réutilisés + 1 ajouté**, pas une matrice d'échecs A–F. |
| Statut · dépendance | ✅ **Archive résolue pour la composition Java sur feu vert INT-19**, après INT-18 commitée `516ea0e`. Raccordement au suivi absent : aucun `FAILED` persisté ni preuve d'écritures MongoDB tardives dans un parcours raccordé. |

### TD-B017

| Archive | **Borner les ressources sans dupliquer les preuves de saturation** |
|---|---|
| Source · IDs · fichiers | [Orchestration][C5], INT-19 → INT-20 ; orchestrateur/test modifiés ; configuration, listener et IT événementiel relus sans modification. |
| Action · preuve historique | Executor explicite des six invocations, sans `commonPool` implicite ou `join/get`. **6 cas ciblés réussis = 3 réutilisés + rejets A/E + partage/isolation** : à un worker, retour sans attendre les sous-tâches et deux UUID distingués avec succès/échec. Saturation INT-15 réutilisée historiquement, sans nouvel IT. |
| Statut · dépendance | ✅ **Archive résolue pour le mécanisme Java sur feu vert INT-20**, après INT-19 commitée `eb0dc5a`. **2 → 3 → 6 sont des sélections successives non cumulatives.** Aucun nouveau pool, câblage listener ou état final ; fournir le bean existant reste [TD-B018](#td-b018), à autoriser. |

## Repères — socle, outillage et frontières

Tous les repères sont **documentés, compréhension/restitution orale non évaluées**. La clôture technique/documentaire du sprint 01 ne change pas ce statut ; les clôtures pédagogiques suivantes ne sont pas acquises par cette condensation. Aucune exécution n'est requise pour les lire ; les pistes de restitution indiquent ce qu'il faudrait savoir distinguer, pas un questionnaire à lancer.

### TD-R001

| Repère | **Séparer les modules sans créer plusieurs applications** |
|---|---|
| Source · IDs · fichiers | [Socle][C1], INT-01/03/04 ; [DAT](../docs/dat/ConfluxLab.md), POM des modules. |
| Notion · preuve | Module = organisation du code, processus = exécution. BFF : HTTP ; Capability : métier/stockage ; `contracts` : types. INT-03 assemble trois bibliothèques dans un seul JAR Boot, appels Java internes ; cela ne réalise pas encore le métier. Distinguer flèche Maven et appel HTTP, expliquer l'interdiction d'accès BFF au repository. |
| Statut · dépendance | 📘 **Référence** ; assemblage [TD-B003](#td-b003) et contrôles séparés [TD-B004](#td-b004) réalisés historiquement, pas preuve de compréhension de l'apprenant. |

### TD-R002

| Repère | **Distinguer POM, dépendances et étapes Maven** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build], INT-02, illustré dès INT-01 ; `pom.xml` et POM des modules. |
| Notion · preuve | Parent : configuration partagée ; agrégateur : projets regroupés ; dépendance : types disponibles. `validate` ne compile pas ; `verify` atteint les tests raccordés ; `install` ajoute au dépôt local. POM valides ou bibliothèque vide ne prouvent ni compilation d'Experience ni test métier ; un sous-ensemble vert ne valide pas le reste. |
| Statut · dépendance | 📘 **Référence**, sans prérequis d'exécution ; lire les exemples du cours. La preuve globale historique est [TD-B002](#td-b002), pas ce repère lui-même. |

### TD-R003

| Repère | **Distinguer les JVM et la configuration réseau** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build], diagnostic JDTLS/Maven INT-02 ; `.mvn/jvm.config`, propriétés Java des POM, réglages JDTLS hors dépôt. |
| Notion · preuve | JDTLS, Maven et application sont des processus distincts. `maven.compiler.release=21` fixe la cible compilée, pas le JDK lancé ; `.mvn/jvm.config` configure Maven. Le forçage IPv4 ne règle ni JDTLS ni l'application et doit être réévalué en réseau IPv6 seul. Raccourci corrigé ≠ serveur compatible ; import JDTLS réussi ≠ application compilée. |
| Statut · dépendance | 📘 **Référence environnementale**, sans exécution requise ; versions issues du diagnostic historique, pas de la stack AXA. La GUI reste [TD-B006](#td-b006), non vérifiée. |

### TD-R006

| Repère | **Distinguer fichier modifié, commit, push et authentification** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build], INT-37 ; `.gitignore`, `.git/` local, preuves historiques du cours, jamais de secret. |
| Notion · preuve | `add` prépare, `commit` versionne localement, `push` publie ; `.gitignore` ne retire pas un fichier déjà suivi. `gh`, SSH et Git HTTPS ont des authentifications distinctes : échecs des deux premiers compatibles avec un push HTTPS réussi. L'état propre observé après ce push n'est pas une garantie actuelle. |
| Statut · dépendance | 📘 **Référence fondée sur le premier push observé**, sans nouvelle opération distante ; [TD-B008](#td-b008) n'est utile qu'en cas de besoin explicite. |

### TD-R007

| Repère | **Comprendre version, empreinte et caches du Wrapper** |
|---|---|
| Source · IDs · fichiers | [Build et versionnement][C1-build], INT-02 ; `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `.gitattributes`. |
| Notion · preuve | Maven 3.9.16 épinglé, empreinte configurée vérifiée au téléchargement. Cache de distribution neuf ≠ cache de dépendances neuf ou clone vierge validé. LF/CRLF protège le checkout des scripts, pas leur exécution Windows ; le bootstrap Linux isolé démontrait le lancement du Wrapper, pas un build complet. |
| Statut · dépendance | 📘 **Référence du bootstrap Linux** ; réseau requis pour reproduire un premier téléchargement, aucune relance demandée. Build global ensuite vérifié dans INT-05 ; Windows reste [TD-B007](#td-b007), non testé. |

### TD-R010

| Repère | **Assembler Spring sans confondre double de test et comportement livré** |
|---|---|
| Source · IDs · fichiers | [Socle][C1], INT-03 ; POM application, `ConfluxLabApplication.java`, `FamilyRequests.java`, `ApplicationAssemblyTest.java` ; ancien `UnavailableFamilyRequests.java`, remplacé dans INT-06. |
| Notion · preuve | Dépendance Maven ≠ bean : le démarrage au-dessus des packages permet de découvrir l'implémentation injectée par interface. Dans INT-03, le double du contrôleur prépare `202`, mais contexte/JAR reflètent l'indisponibilité réelle `503`, sans prise en charge ni faux UUID. MockMvc seul ne prouve pas le démarrage du JAR ; l'acceptation réelle arrive avec INT-06. |
| Statut · dépendance | 📘 **Référence des deux niveaux de vérification INT-03**, inspection/démarrage réel inclus, voir [TD-B003](#td-b003). Aucun besoin de rejouer pour lire ce repère. |

### TD-R011

| Repère | **Tester une frontière et vérifier son détecteur** |
|---|---|
| Source · IDs · fichiers | [Socle][C1], INT-04 ; règles/tests ArchUnit et fixtures dans `application/src/test/`, test de contexte conservé. |
| Notion · preuve | ArchUnit lit les dépendances des classes compilées, pas les règles métier. Les mêmes règles refusent les fixtures fautives avec source/cible attendues et acceptent un graphe autorisé ; exclure les tests de l'analyse production sépare simulation et application. Un test négatif vert prouve un refus, pas une dépendance permise ; contexte ≠ contrôle des frontières. |
| Statut · dépendance | 📘 **Référence**, fondée sur [TD-B004](#td-b004), packages/API publique du DAT. [Clôture technique et documentaire sprint 01](../docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) demandée ; restitution orale toujours non évaluée. |

## Repères — contrat, acceptation et consultation

### TD-R004

| Repère | **Ne pas confondre acceptation, suivi et réussite métier** |
|---|---|
| Source · IDs · fichiers | [REST][C2], INT-06/07 ; `FamilyController.java`, `RequestAccepted.java`, `RequestView.java`, [contrat initial](../docs/dat/http-contracts.md). |
| Notion · preuve | `202` = demande acceptée, ni famille créée ni exécution durable garantie. `Location`/`statusUrl` désignent le GET à consulter ; UUID = corrélation, pas idempotence : deux POST identiques peuvent créer deux demandes. Illustration historique : `202`, `200 PENDING`, absence `404`, sans traitement ou durabilité à ce stade. |
| Statut · dépendance | 📘 **Référence de contrat**, illustrée par [TD-B005](#td-b005) ; aucune exécution de l'ébauche nécessaire pour l'expliquer. |

### TD-R005

| Repère | **Comprendre ce qu'une annotation REST ou `@Valid` ne prouve pas** |
|---|---|
| Source · IDs · fichiers | [REST][C2], initié dans INT-06, INT-05 à INT-08 liés ; contrôleur, types `contracts`, `ApiExceptionHandler.java`. |
| Notion · preuve | Le contrôleur reçoit et délègue ; `@Valid` déclenche les contraintes déclarées, ne les invente pas. DTO ≠ document MongoDB. Annotation ou branche `ResponseEntity` ne prouvent ni compilation, démarrage ou réponse observée ; distinguer lecture, test du contrat et requête exécutée. Capability protège aussi ses invariants en appel Java direct. |
| Statut · dépendance | 📘 **Référence illustrée** : DTO testés INT-05, acceptation/validation directe INT-06, suivi INT-07, refus INT-08. DTO réels et preuves nécessaires, sans prétention de couvrir les comportements non spécifiés. |

### TD-R008

| Repère | **Tester les dates sans dépendre du jour d'exécution** |
|---|---|
| Source · IDs · fichiers | [REST][C2], INT-05 ; `ContractValidationTest.java`, `PersonPayload.java` dans `contracts`. |
| Notion · preuve | `@PastOrPresent` consulte l'horloge du validateur ; `Clock` fixe via `clockProvider` rend passé/présent/futur déterministes sans changer l'ordinateur ou la production. Le 22 octobre 2026 reste futur dans un test fixé au 21 octobre 2026, même exécuté plus tard. |
| Statut · dépendance | 📘 **Référence fondée sur les cas réussis INT-05**, pas évaluation de maîtrise ; lire l'horloge fixe existante, sans nouvelle exécution. |

### TD-R009

| Repère | **Choisir un record pour transporter des données** |
|---|---|
| Source · IDs · fichiers | [REST][C2], INT-05 ; `PersonPayload.java`, `CreateFamilyRequest.java`, `RequestAccepted.java`, `RequestView.java`. |
| Notion · preuve | Le record explicite le transport, génère constructeur/accesseurs/comparaison/représentation, sans setters. Références finales ≠ objets référencés immuables ; une liste peut rester modifiable et la construction ne lance pas Bean Validation. Savoir expliquer `firstName()` plutôt que `getFirstName()` et quand préférer une classe. |
| Statut · dépendance | 📘 **Explication documentaire ajoutée sur demande**, choix des DTO existants ; conversion JSON déjà testée INT-05, aucun nouveau test pour cet ajout. |

### TD-R012

| Repère | **Conserver avant d'acquitter, sans promettre la durabilité** |
|---|---|
| Source · IDs · fichiers | [REST][C2], INT-06 ; anciens service/store mémoire, `StoredFamilyRequest.java`, configuration (noms historiques précisés aux archives). |
| Notion · preuve | Snapshot validé, UUID et conservation `PENDING` avant reçu ; `List.copyOf` protège la liste, `putIfAbsent` la clé, pas le dédoublonnage métier. Échec de `save` : pas de reçu ; redémarrage : mémoire perdue, non bornée. `Clock` d'horodatage ≠ horloge de validation. À INT-06, GET seulement préparé ; INT-07 l'a réalisé. |
| Statut · dépendance | 📘 **Référence historique**, contrats/frontières préexistants ; 96 tests et JAR INT-06 étayent cette étape. INT-07 et INT-08 réalisées ensuite sur accords distincts ; ni `202` ni ces preuves mémoire ne promettent durabilité ou famille créée. |

### TD-R013

| Repère | **Consulter sans modifier, distinguer absence et format invalide** |
|---|---|
| Source · IDs · fichiers | [REST][C2], INT-07 ; ancien service mémoire, `RequestNotFoundException.java`, `ApiExceptionHandler.java`, preuve de suivi HTTP. |
| Notion · preuve | GET = UUID/statut stocké/date d'origine, sans `save`, nouvel horodatage ou traitement, sans payload. Capability signale l'absence, BFF adapte en `404` ; format invalide : MVC `400` avant appel Java. Ni l'un ni l'autre n'est un état métier `FAILED`. Mémoire historique : ancien UUID `404` après redémarrage, sans annuler le `202` antérieur. |
| Statut · dépendance | 📘 **Référence après acceptation INT-06 et DTO `RequestView`** ; 108 tests et JAR INT-07, redémarrage compris, étayent la lecture sans mutation. INT-08 a ensuite complété refus/confidentialité ; pas de relance demandée. |

### TD-R014

| Repère | **Comprendre un refus, ses effets et sa confidentialité** |
|---|---|
| Source · IDs · fichiers | [REST][C2], INT-08 ; `ApiExceptionHandler.java`, tests de refus HTTP/Java et de panne d'acceptation sur le parcours mémoire existant. |
| Notion · preuve | `400` seul ne prouve pas l'absence d'écriture : vérifier les appels par spy/mock. Détail générique insuffisant si `instance` répète la saisie ; fuite reproduite/corrigée, objet et JSON contrôlés. Cas valide nécessaire pour éviter « tout refuser » ; MVC, Java direct et doublure de panne prouvent des choses distinctes. |
| Statut · limite | 📘 **Référence fondée sur 93 cas ciblés finaux / 181 globaux réussis** (73 ajouts nets), pas objectif de volume ni sécurité exhaustive. Quelques scénarios expliquent le principe ; nombre de cas ≠ compréhension. Passage à INT-09 demandé ensuite, pas autorisé par ce repère. |

## Repères — données, MongoDB et concurrence

### TD-R015

| Repère | **Un cycle d'état local n'est ni sauvegarde ni verrou** |
|---|---|
| Source · IDs · fichiers | [Modèle et transitions][C3], INT-09 ; `StoredFamilyRequest.java`, service interne, [DAT — données](../docs/dat/ConfluxLab.md#6-données-mongodb). |
| Notion · preuve | `pending` : `createdAt == updatedAt`, pas de fin. `transitionTo` retourne une copie selon le graphe, conserve création, refuse recul d'`updatedAt`, pose `completedAt` à la fin ; état final fermé. Deux threads peuvent pourtant produire chacun `PROCESSING` depuis le même ancien objet : ni sauvegarde ni exclusion concurrente. `result?`/`error?` restent cibles non implémentées. |
| Statut · dépendance | 📘 **Référence du modèle interne et de `RequestStatus`**, 3 cas ciblés INT-09 réussis sans MongoDB à cette étape. Persistance INT-10 et atomicité INT-11 distinctes ; pas de nouveau build global ou d'évaluation orale. |

### TD-R016

| Repère | **Insérer puis acquitter, sans promettre une issue toujours connue** |
|---|---|
| Source · IDs · fichiers | [Insertion et persistance][C3-mongo], INT-10 ; service/store MongoDB, modèle/configuration, `MongoTestSupport.java`. |
| Notion · preuve | `insert` attend l'acquittement configuré avec journal ; `_id` unique refuse la collision sans upsert, contrairement au risque d'écrasement d'un `save`. Standalone : `majority` ≠ réplication. Timeout/déconnexion/`500` peuvent masquer une écriture réussie ; rejouer le POST peut doubler le métier, UUID unique ≠ idempotence et `202` ≠ famille créée. |
| Statut · dépendance | 📘 **Référence après modèle INT-09, repository et contrat HTTP existant** ; [TD-B009](#td-b009) : MongoDB réel et panne simulée, pas matrice exhaustive. Testcontainers 1.21.4 a corrigé l'incompatibilité Docker initiale ; store mémoire réservé aux tests, suite globale/redémarrage non exécutés dans INT-10. |

### TD-R017

| Repère | **Distinguer quorum, acquittement et découverte d'un replica set** |
|---|---|
| Source · IDs · fichiers | [Replica set (théorie)][C3-replica], complément théorique INT-10 demandé par l'utilisateur ; configuration et `application.yml` relus sans modification, pas nouvelle story. |
| Notion · preuve | Hypothèse : membres votants contenant les données, sans arbitre. Majorité : 2/2, donc perte d'un membre bloquante ; 2/3, continuité possible sous conditions d'élection/disponibilité. `MAJORITY` reste pertinent ; URI de découverte adaptée aux serveurs réellement configurés, délais à réévaluer. URI ≠ création du replica set ; lectures primary évitent un secondary en retard. |
| Statut · limite | 📘 **Théorie uniquement**, fondée sur la configuration d'acquittement actuelle ; aucun replica set créé, test exécuté ou réglage modifié. Quorum électoral, acquittement, réplication, sauvegarde et idempotence diffèrent ; retry du driver ≠ nouveau POST idempotent. |

### TD-R018

| Repère | **Comparer et modifier atomiquement plutôt que lire puis écraser** |
|---|---|
| Source · IDs · fichiers | [Modèle et transitions][C3], INT-11 ; `MongoFamilyRequestStore.java`, `StoredFamilyRequest.java`, preuves de transition. Prérequis : graphe INT-09, persistance INT-10. |
| Notion | `requireTransition` vérifie les flèches, pas les dates ; `transitionTo` vérifie la chronologie locale et copie sans sauvegarder. Le store filtre `_id + statut attendu + updatedAt <= date` et applique l'Update en un `findAndModify` : le second candidat perd sur le statut, même avec date plus récente ; une tentative périmée ne remplace pas `FAILED`. |
| Preuve · portée | Présent = modification gagnante, vide = aucune correspondance, panne = exception, jamais `Optional.empty()` artificiel. `returnNew` renvoie le document modifié, sans upsert, champs non concernés préservés. Deux nouveaux cas MongoDB + deux du modèle réussis ; atomicité d'un document ≠ exactly-once, bail/propriétaire ou transaction avec le partenaire. |
| Statut · version | 📘 **Référence** : dans INT-11, `transition` n'avait que des appelants de test ; depuis INT-14, le listener appelle le claim, pas la finalisation métier. Aucun traitement partenaire ou évaluation orale n'est prouvé par INT-11. |

### TD-R019

| Repère | **Distinguer les phases Maven et prouver la persistance entre deux JVM** |
|---|---|
| Source · IDs · fichiers | [Insertion et persistance][C3-mongo], INT-12 ; POM racine/application, `FamilyRequestPersistenceIT.java`, support/IT MongoDB, contrôle de frontière. |
| Notion · preuve | Surefire : `*Test` à `test` ; trois classes `*IT` alors découvertes par Failsafe après packaging, contrôlées à `verify`. `pluginManagement` seul n'active rien. Même JAR, deux JVM successives, MongoDB conservé : UUID/statut/date retrouvés ; pas simple nouveau repository/deux contextes dans une JVM, ni redémarrage MongoDB/récupération disque. |
| Statut · dépendance | 📘 **Référence fondée sur [TD-B011](#td-b011)** : Boot unique, Testcontainers, Failsafe activé ; 1 Surefire + 5 Failsafe (1 scénario ajouté, 4 MongoDB réutilisés). `-Dtest` ≠ `-Dit.test` ; sélection ≠ suite complète. Certains `*Test` HTTP sont intégrés : suffixe ≠ nature du test. |

## Repères — événement local, asynchronisme et limites

### TD-R020

| Repère | **Séparer persistance, soumission volatile, claim et réponse HTTP** |
|---|---|
| Source · IDs · fichiers | [Async][C4], INT-13 ; service/modèle/store/BFF relus, [DAT — fenêtres de panne](../docs/dat/ConfluxLab.md#71-fenêtres-de-panne-documentées--int-13), sans modification applicative. |
| Notion · preuve | Trois situations : insertion non confirmée ; `PENDING` sans prise en charge ; interruption en `PROCESSING`. Événement local non durable : document survivant ≠ tâche survivante. Erreur/timeout ≠ absence d'écriture. Listener possible avant `202`, qui ne fixe ni début ni réussite ; aucune reprise automatique. |
| Statut · dépendance | 📘 **Analyse documentaire**, appuyée sur persistance INT-10/12 et graphe/atomicité INT-09/11 : un schéma, trois cas, aucune panne exécutée ni validation de l'ébauche INT-14 par cette seule analyse. |

### TD-R021

| Repère | **Publier après sauvegarde et traverser le proxy async avant le claim** |
|---|---|
| Source · IDs · fichiers | [Async][C4], INT-14 ; service/configuration, `FamilyRequestSubmitted.java`, `FamilyRequestListener.java`, IT événementiel et preuves existantes adaptées. |
| Notion · preuve | Insertion confirmée avant publication. `@EnableAsync` active l'interception, `@EventListener` reçoit le type, `@Async` soumet au worker ; appel direct hors proxy ≠ preuve async. Executor Boot dans INT-14, `familyRequestExecutor` dédié depuis INT-15. Reçu `PENDING`, GET déjà `PROCESSING` possible ; deux lectures `PENDING` exigent le CAS MongoDB, pas une réservation Java. |
| Statut · dépendance | 📘 **Référence fondée sur [TD-B012](#td-b012)** : 9 cas ciblés, un seul scénario nouveau ; publication locale, proxy Spring et CAS INT-11. POST obtenu pendant listener contrôlé ; arrêt au claim, sans métier fictif, état final ou reprise. |

### TD-R022

| Repère | **Borner les tâches sans masquer les rejets ou promettre une reprise** |
|---|---|
| Source · IDs · fichiers | [Pool et erreurs async][C4-pool], INT-15 ; configuration/listener/IT événementiel ; listener proxifié INT-14 et executor dédié INT-15 requis. |
| Notion · preuve | Pool fixe/file bornée limitent les tâches admises ; `AbortPolicy` refuse sans exécuter sur HTTP. Rejet synchrone : `500`, document parfois déjà `PENDING` ; exception du worker `@Async void` : journal UUID/type sans message/trace bruts, statut inchangé. Aucun des deux n'implique automatiquement `FAILED` ou reprise. |
| Statut · limite | 📘 **Référence fondée sur [TD-B013](#td-b013)** : 4 cas ciblés dont deux ajoutés. Attente de fermeture ≠ timeout de tâche ni arrêt forcé ; arrêt prolongé non testé et maîtrise non évaluée. |

## Repères — graphe A–F et executor explicite

### TD-R023

| Repère | **Distinguer contrats internes, identifiants typés et futures pilotables** |
|---|---|
| Source · IDs · fichiers | [Orchestration][C5], INT-17 ; `FamilyOperations.java` en production, `ControlledFamilyOperations.java` seulement sous `src/test` ; graphe A–F et `PersonPayload` préexistants. |
| Notion · preuve | Signatures = données nécessaires, pas ordre ; `PrincipalId`/`ContractId`/`DependentId` distinguent les rôles sans valider contenu/appartenance ni empêcher l'inversion de deux ayants droit. UUID corrèle, sans idempotence. Future ≠ thread ; fixture pilotable par `complete`/`completeExceptionally`, sans HTTP. E exige B/C, pas D. |
| Statut · dépendance | 📘 **Référence compilée dans INT-17**, sans infrastructure nouvelle : `test-compile` n'exécutait aucun test ni le graphe. Fixture exercée ensuite dans les 2 cas réussis INT-18 ; pas de compréhension évaluée. |

### TD-R024

| Repère | **Composer des dépendances locales et suivre la fin réelle des futures** |
|---|---|
| Source · IDs · fichiers | [Orchestration][C5], INT-18 ; orchestrateur/test et fixture INT-17. |
| Notion · preuve | B/C/D dépendent chacune d'A ; E combine B/C, F combine B/D. `thenCompose` aplatit pour suivre la fin réelle, pas le seul démarrage ; `thenCombine` combine les résultats, `allOf` reste final. Une barrière B/C/D préalable retarderait E/F ; fin globale interdite tant qu'un rattachement continue. |
| Statut · limite | 📘 **Référence illustrée par [TD-B015](#td-b015)**, 2 cas ciblés, contrats/fixture INT-17 requis. Continuations sans `Async` ≠ nouveaux threads ; futures en cours ≠ mesure de parallélisme ou d'I/O. Ni HTTP, Spring, MongoDB ou performance prouvés. |

### TD-R025

| Repère | **Attendre les branches lancées sans masquer l'échec** |
|---|---|
| Source · IDs · fichiers | [Orchestration][C5], INT-19 ; orchestrateur/test, fixture INT-17 inchangée ; composition native et jonction finale explicite B/C/D/E/F. |
| Notion · preuve | B échouée interdit les appels E/F sans annuler C/D ; leurs futures composées peuvent échouer sans invocation partenaire E/F. La jonction attend les branches avant d'exposer l'échec, sans retarder les démarrages nominaux. `get` enveloppe dans `ExecutionException`, cause originale vérifiée ; future exceptionnelle ≠ `FAILED` écrit en MongoDB. |
| Statut · limite | 📘 **Référence illustrée par [TD-B016](#td-b016)**, 3 cas dont les 2 nominaux réutilisés et un échec B ajouté. Erreur de branche ≠ fin globale ; aucun état final persisté ou maîtrise évaluée. |

### TD-R026

| Repère | **Soumettre explicitement sans bloquer et connaître la borne réelle du pool** |
|---|---|
| Source · IDs · fichiers | [Orchestration][C5], INT-20 ; orchestrateur/test, configuration/listener existants ; composition INT-18/19 et saturation historique INT-15. |
| Notion | `supplyAsync` avec executor soumet l'invocation, `thenCompose` suit sa future. Rejet initial possible avant le retour d'une future, converti en `failedFuture`. Pas de `join/get` dans le pool ; continuations limitées à composer/soumettre. À un worker, rendre la future du graphe sans l'y attendre évite de bloquer ses propres sous-tâches. |
| Portée · preuve | Type `Executor` ≠ bornage : l'instance borne workers/file, pas un travail externalisé ailleurs. Admission A ≠ réservation pour la suite : E peut être refusée plus tard. Partage/isolation Java vérifiés dans les 6 cas INT-20, dont 3 réutilisés ; ni test de charge complet ni preuve de câblage Spring. |
| Statut · dépendance | 📘 **Référence**, sans statut final persisté ni compréhension évaluée. Le raccordement effectif au bean borné, l'adaptateur et la finalisation restent [TD-B018](#td-b018), **lot INT-21 + INT-22 toujours à autoriser**. |

[C1]: sprint-01/INT-01-socle-modulaire.md
[C1-build]: sprint-01/INT-02-build-et-versionnement.md
[C2]: sprint-02/INT-05-accepter-et-consulter-une-demande.md
[C3]: sprint-03/INT-09-modele-et-transitions-atomiques.md
[C3-mongo]: sprint-03/INT-10-insertion-et-persistance-mongodb.md
[C3-replica]: sprint-03/INT-10-complement-mongodb-replica-set.md
[C4]: sprint-04/INT-13-traiter-les-demandes-en-arriere-plan.md
[C4-pool]: sprint-04/INT-15-pool-borne-et-erreurs-async.md
[C5]: sprint-05/INT-17-orchestrer-les-six-operations.md
