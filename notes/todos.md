# Actions et repères — ConfluxLab

Ce fichier centralise **les actions restantes et les points à retenir issus des notes**. Les [stories](../docs/stages/README.md) décrivent le travail prévu ; les [notes](README.md) expliquent ce qui a été fait et vérifié. Ce registre les relie sans les remplacer.

## Mode d'emploi

- **`TD-B001`, etc.** : actions de suivi. Chaque action indique sa source, ses dépendances et la preuve attendue.
- **`TD-R001`, etc.** : repères de cours. Leur statut « Référence » ne signifie pas que l'apprenant les maîtrise déjà, ni qu'un développement est terminé.
- Les IDs sont uniques et stables dans leur catégorie. Mettre à jour une entrée existante plutôt que recopier la même action dans plusieurs notes ; ne pas réutiliser un identifiant.
- **Aucune entrée n'autorise une exécution.** Le feu vert reste requis pour chaque story `INT-XX` et pour toute action externe non déjà autorisée.
- Les statuts reflètent les preuves disponibles : à autoriser, bloqué, manuel, optionnel, terminé avec preuve. Un fichier ou un exemple de commande ne suffit pas à déclarer un succès.
- Les chemins marqués « à créer » sont des cibles futures, pas des fichiers présentés comme existants. Aucun endpoint d'authentification ou autre fonctionnalité de l'exemple de format n'est ajouté au projet.

## Vue d'ensemble des actions

| Action | Sujet | Story liée | Situation |
|---|---|---|---|
| [TD-B001](#td-b001) | Définir les types d'échange | INT-05 | Terminée dans INT-05 : types et tests disponibles |
| [TD-B002](#td-b002) | Réévaluer `verify` global | INT-02 | Terminée : `clean verify` global réussi après INT-05 |
| [TD-B003](#td-b003) | Assembler l'application et retirer l'HTTP interne | INT-03 | Terminée : seul JAR Boot démarré et appel Java vérifié |
| [TD-B004](#td-b004) | Contrôler les frontières | INT-04 | Terminée : règles de production et détection de violations vérifiées |
| [TD-B005](#td-b005) | Valider le parcours REST | INT-06/07/08 | Terminée : acceptation, suivi et refus mémoire vérifiés dans INT-06/07/08 |
| [TD-B006](#td-b006) | Vérifier les raccourcis dans l'interface Zed | INT-02 | Vérification manuelle non constatée |
| [TD-B007](#td-b007) | Vérifier le Wrapper sous Windows | INT-02 | Optionnel ; environnement Windows requis |
| [TD-B008](#td-b008) | Revoir `gh` ou SSH si nécessaire | INT-37 | Non prioritaire : Git HTTPS a fonctionné |
| [TD-B009](#td-b009) | Remplacer la mémoire par MongoDB | INT-10 | Terminée : insertion/relecture et unicité réelles, erreur sans faux reçu |
| [TD-B010](#td-b010) | Sécuriser les mises à jour | INT-11 | À autoriser ; MongoDB INT-10 disponible |

Les anciens repères conservent leurs noms historiques : depuis INT-10, `InMemoryFamilyRequests` et son test sont renommés `FamilyRequestService` / `FamilyRequestServiceTest`. `InMemoryFamilyRequestStore` ne subsiste que sous `capability-api/src/test/`.

## Actions restantes

## TD-B001

**Définir les contrats et les trois DTO manquants**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-02](sprint-01/INT-02-rendre-le-build-reproductible.md), blocage de compilation ; également expliqué dans INT-01/INT-06 |
| **Story liée** | [INT-05 — Définir les contrats](../docs/stages/sprint-02-rest.md#int-05--définir-les-contrats) |
| **Dépend de** | Cadrage INT-01 terminé ; accord explicite pour INT-05 |
| **Fichiers** | Cinq types dans `contracts/src/main/java/dev/confluxlab/contracts/`, tests/fixtures sous `contracts/src/test/`, [contrat HTTP](../docs/dat/http-contracts.md) |
| **Action** | Définir les données utiles, leurs règles et les types `CreateFamilyRequest`, `RequestAccepted`, `RequestView`, sans classes vides de dépannage ni service métier ajouté |
| **Validation** | Exemples et types cohérents, tests de données/contrat, puis nouvelle compilation ; ne pas déduire une API fonctionnelle de la seule présence des DTO |
| **Statut** | ✅ Terminée sur feu vert INT-05 ; [tutoriel et preuves](sprint-02/INT-05-definir-les-contrats.md), 59 cas de test réussis |

## TD-B002

**Réévaluer le build complet après définition des types**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-02](sprint-01/INT-02-rendre-le-build-reproductible.md) |
| **Story liée** | INT-02 — Rendre le build reproductible |
| **Dépend de** | [TD-B001](#td-b001) ; ne pas masquer d'autres erreurs qui seraient révélées ensuite |
| **Fichiers** | `pom.xml`, POM des modules, `contracts/src/`, `experience-api/src/` |
| **Action** | Relancer `./mvnw -B -ntp verify` depuis la racine et analyser le résultat réel, sans exclure Experience ou sa compilation |
| **Validation** | Réacteur global réussi et résultats enregistrés ; distinguer compilation, tests réellement exécutés et bibliothèques vides |
| **Statut** | ✅ Terminée : `./mvnw -B -ntp clean verify` global réussi dans INT-05, 59 cas, aucun échec/erreur/ignoré ; INT-02 clôturée pour le build |

## TD-B003

**Assembler le monolithe et remplacer l'appel HTTP interne**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-01](sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md), écart aussi décrit dans INT-06 |
| **Story liée** | [INT-03 — Assembler les modules](../docs/stages/sprint-01-socle.md#int-03--assembler-les-modules) |
| **Dépend de** | [TD-B002](#td-b002), terminée ; feu vert INT-03 reçu et exécuté |
| **Fichiers** | [POM d'application](../application/pom.xml), [ConfluxLabApplication.java](../application/src/main/java/dev/confluxlab/ConfluxLabApplication.java), [FamilyRequests.java](../capability-api/src/main/java/dev/confluxlab/capability/api/FamilyRequests.java), [FamilyController.java](../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java), [configuration unique](../application/src/main/resources/application.yml) et tests d'assemblage/délégation |
| **Action** | Créer le seul assemblage Boot et une API Java publique de Capability ; remplacer le chemin BFF → Capability par cet appel Java |
| **Validation** | Un seul JAR Boot démarre sans base/partenaire obligatoire à ce stade ; un test confirme l'appel Java, sans faux parcours métier réussi |
| **Statut** | ✅ Terminée sur feu vert INT-03 ; [tutoriel et preuves](sprint-01/INT-03-assembler-les-modules.md), 64 tests globaux, JAR réel démarré, POST/GET valides `503` sans faux succès |

## TD-B004

**Tester les frontières entre modules**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-01](sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md) |
| **Story liée** | [INT-04 — Vérifier les frontières](../docs/stages/sprint-01-socle.md#int-04--vérifier-les-frontières) |
| **Dépend de** | [TD-B003](#td-b003), terminée ; feu vert INT-04 reçu et exécuté |
| **Fichiers** | `application/pom.xml`, `ModuleArchitectureRules.java`, `ModuleArchitectureTest.java`, `ArchitectureRuleDetectionTest.java` et fixtures sous `application/src/test/java/dev/confluxlab/` ; `ApplicationAssemblyTest.java` existant conservé |
| **Action** | S'appuyer sur le test de contexte ajouté dans INT-03 et ajouter des contrôles simples contre les cycles, la dépendance Capability → BFF et les accès aux internals/repositories |
| **Validation** | Les contrôles passent dans `verify` et détectent une violation introduite dans un test contrôlé ; aucune infrastructure imposée sans nécessité |
| **Statut** | ✅ Terminée sur feu vert INT-04 ; [tutoriel et preuves](sprint-01/INT-04-verifier-les-frontieres.md), 5 tests de production + 7 tests de détection ; `clean verify` global réussi, 76 tests dont le contexte existant |

## TD-B005

**Démontrer l'acceptation, le suivi et les refus REST**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-06](sprint-02/INT-06-accepter-une-demande.md), suivi initié dans l'ébauche puis actualisé après réalisation de l'acceptation |
| **Stories liées** | INT-06, INT-07 et INT-08 ; chaque story conserve son feu vert individuel |
| **Dépend de** | INT-05 et socle assemblé/vérifié, notamment [TD-B004](#td-b004) |
| **Fichiers** | `experience-api/src/`, `capability-api/src/`, `contracts/src/` |
| **Action** | Brancher une acceptation réelle en mémoire dans Capability, puis le GET et les refus, sans HTTP interne ni prétendre que la famille est créée |
| **Validation** | Tests POST `202`, GET `200`/`404`, entrée invalide `400`, refus sans enregistrement et demandes distinctes ; état `PENDING` et limite de mémoire explicités |
| **Statut** | ✅ Terminée dans le périmètre REST mémoire INT-06/07/08 ; [preuves INT-08](sprint-02/INT-08-tester-les-refus.md), 181 tests et JAR réel vérifiés, refus sans enregistrement, erreurs génériques et isolation. Ni persistance durable, ni traitement, ni clôture pédagogique automatique ne sont déduits de ce résultat |

## TD-B006

**Confirmer la navigation dans l'interface Zed**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-02](sprint-01/INT-02-rendre-le-build-reproductible.md), limite des preuves JDTLS |
| **Story liée** | Diagnostic éditeur associé à INT-02, distinct du build global |
| **Dépend de** | JDTLS opérationnel ; disponibilité de l'utilisateur pour la vérification visuelle |
| **Fichiers** | `FamilyController.java` ; réglages et raccourcis utilisateur Zed hors dépôt |
| **Action** | Placer le curseur sur `FamilyRequests` (remplace l'ancien `CapabilityClient` depuis INT-03), `UUID`, puis `ResponseEntity` et essayer F12 ou Ctrl + Shift + B ; comparer avec la commande de navigation de la palette si nécessaire |
| **Validation** | Constater l'ouverture visuelle de la bonne définition ; noter séparément reconnaissance du raccourci et navigation. Ne pas redéduire ce résultat des réponses LSP historiques |
| **Statut** | 🧭 Manuel — non constaté ; serveur LSP vérifié historiquement, interaction GUI non vérifiée |

## TD-B007

**Vérifier le Wrapper dans un environnement Windows**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-02](sprint-01/INT-02-rendre-le-build-reproductible.md) |
| **Story liée** | Portabilité d'INT-02 |
| **Dépend de** | Machine Windows, JDK 21 et accès réseau ; pas de validation Windows simulée sous Linux |
| **Fichiers** | `mvnw.cmd`, `.gitattributes`, `.mvn/wrapper/maven-wrapper.properties` |
| **Action** | Exécuter le Wrapper Windows pour sa version et `validate`, puis consigner l'environnement et les résultats |
| **Validation** | Maven épinglé démarre et les POM sont validés ; ne pas présenter ce contrôle comme un `verify` global réussi |
| **Statut** | ⏳ Optionnel — non testé, environnement Windows non disponible dans les preuves fournies |

## TD-B008

**Revoir l'authentification `gh` ou SSH seulement si un usage le nécessite**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-37](sprint-01/INT-37-initialiser-le-depot-git.md) |
| **Story liée** | Diagnostic complémentaire à INT-37, qui est déjà terminée |
| **Dépend de** | Besoin explicite de la CLI `gh` ou de Git SSH ; accord avant changement de configuration/authentification |
| **Fichiers** | Configuration utilisateur d'authentification hors dépôt ; aucun secret à écrire dans le projet ou le chat |
| **Action** | Si nécessaire, authentifier le canal choisi via son mécanisme normal ; ne pas modifier le HTTPS Git déjà fonctionnel pour cette seule raison |
| **Validation** | Commande de lecture réussie avec le canal voulu, sans exposition de token ni clé privée |
| **Statut** | ⏳ Non prioritaire — erreurs historiques `gh`/SSH, mais le push Git HTTPS a réussi ; pas de blocage Git déduit aujourd'hui |

## TD-B009

**Persister la demande avant de l'accepter**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-09](sprint-03/INT-09-definir-les-donnees-et-transitions.md) |
| **Story liée** | [INT-10 — Persister avant d'accepter](../docs/stages/sprint-03-mongodb.md#int-10--persister-avant-daccepter) |
| **Dépend de** | INT-09 terminée et commitée `185bdd7` ; feu vert INT-10 reçu, Docker disponible |
| **Fichiers** | `FamilyRequestService.java`, `FamilyRequestStore.java`, `MongoFamilyRequestStore.java`, `StoredFamilyRequest.java`, `FamilyRequestConfiguration.java`, POM et `application.yml` ; `MongoTestSupport.java` et `MongoFamilyRequestStoreTest.java` sous les tests de l'assemblage |
| **Action** | Remplacer le stockage mémoire par MongoDB en conservant les frontières ; confirmer l'écriture avant le retour d'acceptation |
| **Validation** | Enregistrement/relecture du même UUID et payload, unicité en base, échec d'écriture sans faux `202` selon les critères d'INT-10 ; ne pas confondre modèle Java et persistance réelle |
| **Statut** | ✅ Terminée sur feu vert INT-10 ; [cours et preuves](sprint-03/INT-10-persister-avant-d-accepter.md), 2 nouveaux tests + 1 test existant réussis avec MongoDB réel temporaire. Panne d'écriture simulée ; ni redémarrage ni suite globale revendiqués |

## TD-B010

**Sécuriser les mises à jour de statut en base**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-10](sprint-03/INT-10-persister-avant-d-accepter.md) |
| **Story liée** | [INT-11 — Sécuriser les mises à jour](../docs/stages/sprint-03-mongodb.md#int-11--sécuriser-les-mises-à-jour) |
| **Dépend de** | INT-10 terminée ; nouveau feu vert individuel |
| **Fichiers** | `MongoFamilyRequestStore.java`, `StoredFamilyRequest.java` et leurs tests à étendre ; pas de méthode de prise en charge ajoutée dans INT-10 |
| **Action** | Faire dépendre la mise à jour atomique du statut actuel autorisé, conserver les dates cohérentes et interdire l'écrasement d'un état final |
| **Validation** | Une seule prise en charge gagne lors d'un double passage concurrent `PENDING → PROCESSING` ; transitions invalides et écrasement final refusés selon les critères de la story |
| **Statut** | ⏸ À autoriser — INT-11 non commencée ; cette entrée suit la story existante, sans autoriser son développement |

## Repères de cours

## TD-R001

**Séparer les modules sans créer plusieurs applications**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-01](sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md), illustré par l'[assemblage INT-03](sprint-01/INT-03-assembler-les-modules.md) |
| **Stories liées** | INT-01, INT-03 et INT-04 |
| **Dépend de** | Aucune exécution requise pour lire le repère ; assemblage réalisé dans INT-03 et contrôles de frontières réalisés séparément dans INT-04 |
| **Fichiers** | `docs/dat/ConfluxLab.md`, POM des modules |
| **À retenir** | Un module organise le code ; un processus exécute l'application. BFF = HTTP ; Capability = métier/stockage ; `contracts` = types. INT-03 assemble trois bibliothèques dans le seul JAR Boot d'application, avec appels Java internes ; cela ne réalise pas encore le métier |
| **Vérification pédagogique** | Expliquer pourquoi une flèche de dépendance Maven n'est pas un appel HTTP et pourquoi le BFF n'accède pas au repository |
| **Statut** | 📘 Référence documentée — compréhension de l'apprenant non évaluée par cette réécriture |

## TD-R002

**Distinguer POM, dépendances et étapes Maven**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-02](sprint-01/INT-02-rendre-le-build-reproductible.md), illustré aussi dans INT-01 |
| **Story liée** | INT-02 |
| **Dépend de** | Aucune ; lire le tutoriel Maven pour les exemples |
| **Fichiers** | `pom.xml`, POM des modules |
| **À retenir** | Le parent partage la configuration ; l'agrégateur regroupe des projets ; une dépendance fournit des types. `validate` ne compile pas ; `verify` parcourt le cycle et les tests raccordés ; `install` ajoute l'artefact au dépôt local. Sélectionner un sous-ensemble ne prouve pas le succès du reste |
| **Vérification pédagogique** | Expliquer comment les POM peuvent être valides alors qu'Experience ne compile pas, et pourquoi une bibliothèque vide peut se construire sans exécuter de test métier |
| **Statut** | 📘 Référence documentée — pas une preuve de build global vert |

## TD-R003

**Distinguer les JVM et la configuration réseau**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-02](sprint-01/INT-02-rendre-le-build-reproductible.md) |
| **Story liée** | Diagnostic JDTLS/Maven associé à INT-02 |
| **Dépend de** | Aucune ; versions tirées du diagnostic historique, pas de la stack AXA |
| **Fichiers** | `.mvn/jvm.config`, propriétés Java des POM ; réglages JDTLS utilisateur hors dépôt |
| **À retenir** | JDTLS, Maven et la future application sont des processus distincts. `maven.compiler.release=21` fixe la cible compilée ; `.mvn/jvm.config` configure Maven. Le forçage IPv4 ne règle ni JDTLS ni la future application et doit être réévalué sur un réseau uniquement IPv6 |
| **Vérification pédagogique** | Dire pourquoi corriger un raccourci ne répare pas un serveur Java incompatible, et pourquoi un import JDTLS réussi ne prouve pas la compilation de l'application |
| **Statut** | 📘 Référence documentée — diagnostic propre à l'environnement décrit |

## TD-R004

**Ne pas confondre acceptation, suivi et réussite métier**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-06](sprint-02/INT-06-accepter-une-demande.md) |
| **Stories liées** | INT-06 et INT-07 |
| **Dépend de** | Aucune exécution de l'ébauche requise pour expliquer le contrat |
| **Fichiers** | `FamilyController.java`, [RequestAccepted.java](../contracts/src/main/java/dev/confluxlab/contracts/RequestAccepted.java), [RequestView.java](../contracts/src/main/java/dev/confluxlab/contracts/RequestView.java) et [contrat initial](../docs/dat/http-contracts.md) |
| **À retenir** | `202` signifie demande acceptée, pas famille créée ni exécution durable garantie. `Location`/`statusUrl` indiquent où consulter. Le UUID corrèle les étapes ; il ne garantit pas l'idempotence, c'est-à-dire l'absence d'effet supplémentaire si l'on répète la demande |
| **Vérification pédagogique** | Expliquer ce que le client doit consulter après le POST et pourquoi deux POST identiques peuvent encore produire deux demandes |
| **Statut** | 📘 Référence illustrée dans INT-06/07 : acceptation `202`, consultation `200 PENDING`, absence `404`, sans traitement ni durabilité ; compréhension de l'apprenant non évaluée |

## TD-R005

**Comprendre ce qu'une annotation REST ou `@Valid` ne prouve pas**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-06](sprint-02/INT-06-accepter-une-demande.md) |
| **Stories liées** | INT-05 à INT-08 |
| **Dépend de** | Aucune ; DTO réels et tests requis pour prouver le comportement |
| **Fichiers** | `FamilyController.java`, types `contracts` et tests réalisés dans [INT-05](sprint-02/INT-05-definir-les-contrats.md), `ApiExceptionHandler.java` |
| **À retenir** | Le contrôleur reçoit des données et délègue ; `@Valid` déclenche les contraintes réellement déclarées, il ne les invente pas. Un DTO transporte des données ; ce n'est pas un document MongoDB. Une annotation et une branche `ResponseEntity` ne prouvent ni compilation, ni démarrage, ni réponse observée |
| **Vérification pédagogique** | Distinguer lecture du code, test du contrat et requête réellement exécutée ; expliquer pourquoi Capability protège aussi ses invariants |
| **Statut** | 📘 Référence documentée — DTO testés dans INT-05, acceptation et validation directe réalisées dans INT-06 ; suivi réalisé dans INT-07, matrice de refus INT-08 réalisée, sans prétendre couvrir tous les comportements non spécifiés |

## TD-R006

**Distinguer fichier modifié, commit, push et authentification**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-37](sprint-01/INT-37-initialiser-le-depot-git.md) |
| **Story liée** | INT-37 |
| **Dépend de** | Aucune nouvelle opération distante pour lire le repère |
| **Fichiers** | `.gitignore`, `.git/` local et notes de preuve ; jamais de secret dans ces notes |
| **À retenir** | `add` prépare le contenu, `commit` crée une version locale, `push` la publie. `.gitignore` ne retire pas un fichier déjà suivi. Les authentifications `gh`, SSH et Git HTTPS sont distinctes ; leur état peut différer |
| **Vérification pédagogique** | Expliquer pourquoi `gh` a pu échouer alors que le push HTTPS réussissait, et pourquoi le dépôt propre constaté après ce push n'est pas une garantie de l'état actuel |
| **Statut** | 📘 Référence fondée sur le premier push observé — pas une nouvelle vérification distante |

## TD-R007

**Comprendre la version, l'empreinte et les caches du Wrapper**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-02](sprint-01/INT-02-rendre-le-build-reproductible.md) |
| **Story liée** | INT-02 |
| **Dépend de** | Aucune ; réseau nécessaire pour reproduire un premier téléchargement |
| **Fichiers** | `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `.gitattributes` |
| **À retenir** | Le Wrapper épingle Maven 3.9.16 et vérifie l'empreinte configurée lors du téléchargement. Un cache de distribution neuf n'est pas un cache de dépendances neuf. LF/CRLF protège les scripts au checkout ; cela ne remplace pas une exécution Windows |
| **Vérification pédagogique** | Décrire ce que le test avec `MAVEN_USER_HOME` isolé a démontré et ce qu'il n'a pas validé |
| **Statut** | 📘 Référence du bootstrap Linux ; build global ensuite vérifié dans INT-05, exécution Windows toujours non testée |

## TD-R008

**Tester les dates sans dépendre du jour d'exécution**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-05](sprint-02/INT-05-definir-les-contrats.md) |
| **Story liée** | INT-05 — Définir les contrats |
| **Dépend de** | Aucune nouvelle exécution ; lire les tests à horloge fixe |
| **Fichiers** | `contracts/src/test/java/dev/confluxlab/contracts/ContractValidationTest.java`, `PersonPayload.java` |
| **À retenir** | `@PastOrPresent` consulte l'horloge du validateur. Un `Clock` fixe injecté par `clockProvider` rend les cas passé/présent/futur déterministes, sans changer l'horloge de l'ordinateur ou de production |
| **Vérification pédagogique** | Expliquer pourquoi le 22 octobre 2026 reste une date future pour un test fixé au 21 octobre 2026, même relancé plus tard |
| **Statut** | 📘 Référence fondée sur les tests réussis d'INT-05 — compréhension de l'apprenant non évaluée |

## TD-R009

**Choisir un record pour transporter des données**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-05 — choix des records](sprint-02/INT-05-definir-les-contrats.md#pourquoi-des-records-pour-ces-dto) |
| **Story liée** | INT-05 — Définir les contrats |
| **Dépend de** | Aucune nouvelle exécution ; choix des DTO déjà présents |
| **Fichiers** | `contracts/src/main/java/dev/confluxlab/contracts/PersonPayload.java`, `CreateFamilyRequest.java`, `RequestAccepted.java`, `RequestView.java` |
| **À retenir** | Un record rend le rôle de transport explicite et génère constructeur, accesseurs, comparaison et représentation textuelle, sans setters. Ses champs sont finaux, mais les objets référencés peuvent rester mutables ; sa construction ne lance pas Bean Validation |
| **Vérification pédagogique** | Expliquer `firstName()` plutôt que `getFirstName()`, pourquoi la liste peut rester modifiable et quand préférer une classe classique |
| **Statut** | 📘 Explication ajoutée sur demande — conversion JSON déjà testée dans INT-05 ; aucun nouveau test exécuté pour cet ajout documentaire |

## TD-R010

**Assembler Spring sans confondre double de test et comportement livré**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-03](sprint-01/INT-03-assembler-les-modules.md) |
| **Story liée** | INT-03 — Assembler les modules |
| **Dépend de** | Lire l'assemblage et ses deux niveaux de tests ; aucune nouvelle exécution requise |
| **Fichiers** | `application/pom.xml`, `ConfluxLabApplication.java`, `FamilyRequests.java`, ancien `UnavailableFamilyRequests.java` (historique INT-03, remplacé dans INT-06), `FamilyControllerTest.java`, `ApplicationAssemblyTest.java` |
| **À retenir** | Une dépendance Maven fournit des types, pas automatiquement un bean. Le démarrage placé au-dessus des packages découvre l'implémentation, injectée via l'interface publique. Le test unitaire peut retourner un succès préparé ; le contexte réel et le JAR doivent refléter l'indisponibilité des opérations absentes (`503`), sans faux identifiant ni prise en charge |
| **Vérification pédagogique** | Expliquer pourquoi, dans INT-03, le contrôleur obtenait `202` avec un double mais le JAR répondait `503`, et pourquoi un test MockMvc ne prouve pas à lui seul le démarrage du JAR ; distinguer cette histoire de l'acceptation réelle d'INT-06 |
| **Statut** | 📘 Référence fondée sur les vérifications INT-03 (64 tests, inspection et démarrage du JAR) — compréhension de l'apprenant non évaluée |

## TD-R011

**Tester une frontière et vérifier que son détecteur fonctionne**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-04](sprint-01/INT-04-verifier-les-frontieres.md) |
| **Story liée** | INT-04 — Vérifier les frontières |
| **Dépend de** | Packages et API publique définis dans le DAT ; aucune nouvelle exécution requise pour lire le repère |
| **Fichiers** | `ModuleArchitectureRules.java`, `ModuleArchitectureTest.java`, `ArchitectureRuleDetectionTest.java`, fixtures sous `application/src/test/java/` et `ApplicationAssemblyTest.java` |
| **À retenir** | ArchUnit lit les dépendances des classes compilées ; les règles ne vérifient pas les règles métier. Des fixtures fautives prouvent que les mêmes règles rejettent effectivement un cycle ou un accès interdit, avec source et cible attendues. Un graphe autorisé vérifie aussi l'absence de rejet abusif. Exclure les tests de l'analyse de production évite de confondre violations simulées et application réelle |
| **Vérification pédagogique** | Expliquer pourquoi un test négatif qui réussit prouve un refus, pourquoi il faut aussi un cas autorisé et pourquoi le test de contexte ne remplace pas les tests de frontières |
| **Statut** | 📘 Référence fondée sur 12 nouveaux tests réussis et 76 tests globaux dans INT-04 — [clôture technique et documentaire du sprint 01](../docs/stages/sprint-01-socle.md#journal-de-clôture--2026-10-21) demandée par l'utilisateur ; restitution orale de l'apprenant toujours non évaluée |

## TD-R012

**Conserver avant d'acquitter, sans promettre la durabilité**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-06](sprint-02/INT-06-accepter-une-demande.md) |
| **Story liée** | INT-06 — Accepter une demande |
| **Dépend de** | Contrats et frontières déjà réalisés ; aucune nouvelle exécution requise pour lire le repère |
| **Fichiers** | `InMemoryFamilyRequests.java`, `InMemoryFamilyRequestStore.java`, `StoredFamilyRequest.java`, `FamilyRequestConfiguration.java`, tests Capability et tests HTTP d'acceptation |
| **À retenir** | Capability valide un snapshot, génère l'UUID et conserve `PENDING` avant de retourner le reçu. `List.copyOf` protège la liste conservée ; `putIfAbsent` empêche l'écrasement d'une clé mais ne dédoublonne pas les demandes. `202` ne signifie ni famille créée ni durabilité ; la mémoire est perdue au redémarrage et reste non bornée. À l'étape INT-06, le lien GET était préparé mais pas encore fonctionnel ; la consultation est réalisée depuis INT-07 |
| **Vérification pédagogique** | Expliquer ce qui se passe si `save` échoue, si l'appelant modifie sa liste, si l'application redémarre ou si le client répète le POST ; distinguer le `Clock` d'horodatage de l'horloge de validation |
| **Statut** | 📘 Référence fondée sur les 96 tests réussis et la démonstration du JAR INT-06 ; compréhension de l'apprenant non évaluée ; INT-07 a depuis été réalisée sur feu vert distinct, INT-08 a depuis été réalisée sur son propre feu vert |

## TD-R013

**Consulter sans modifier, distinguer absence et format invalide**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-07](sprint-02/INT-07-consulter-la-demande.md) |
| **Story liée** | INT-07 — Consulter la demande |
| **Dépend de** | Acceptation INT-06 et DTO `RequestView` existants ; aucune nouvelle exécution requise pour lire le repère |
| **Fichiers** | `InMemoryFamilyRequests.java`, `RequestNotFoundException.java`, `ApiExceptionHandler.java`, `InMemoryFamilyRequestsTest.java`, `FamilyRequestTrackingHttpTest.java` |
| **À retenir** | Le GET projette l'identifiant, le statut stocké et la date d'origine, sans `save`, nouvel horodatage ou traitement. Capability signale l'absence sans statut HTTP ; le BFF l'adapte en `404`. Le format HTTP invalide est refusé en `400` avant l'appel Java. La vue ne divulgue pas le payload. Après redémarrage, une ancienne demande peut devenir absente car la mémoire n'est pas durable |
| **Vérification pédagogique** | Expliquer pourquoi `400`, `404` et un statut métier `FAILED` ne veulent pas dire la même chose ; pourquoi `createdAt` ne change pas au polling et pourquoi un `404` après redémarrage n'invalide pas le `202` précédemment observé |
| **Statut** | 📘 Référence fondée sur 108 tests réussis et la démonstration du JAR INT-07, redémarrage compris ; compréhension de l'apprenant non évaluée ; INT-08 a depuis complété les refus et la confidentialité |

## TD-R014

**Comprendre un refus, puis prouver ses effets et sa confidentialité**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-08](sprint-02/INT-08-tester-les-refus.md) |
| **Story liée** | INT-08 — Tester les refus |
| **Dépend de** | Contrats et parcours mémoire existants ; aucune nouvelle exécution pour lire ce repère |
| **Fichiers** | `FamilyRequestRefusalHttpTest.java`, `FamilyRequestAcceptanceFailureHttpTest.java`, `InMemoryFamilyRequestsTest.java`, `ApiExceptionHandler.java`, `ApiExceptionHandlerTest.java` |
| **À retenir** | Quelques scénarios suffisent pour comprendre le principe ; une matrice paramétrée teste davantage de variantes et de frontières. Un `400` ne prouve pas seul l'absence d'écriture : spy et mock permettent de vérifier les appels. Un détail générique ne suffit pas si `instance` recopie une saisie rejetée ; le correctif et la représentation JSON doivent aussi être testés. Le nombre de cas n'est ni une mesure de compréhension ni une preuve de sécurité exhaustive |
| **Vérification pédagogique** | Expliquer pourquoi on teste aussi un cas valide, ce que prouvent respectivement MVC, appel Java direct et doublure de panne, et comment la fuite via `instance` a été reproduite puis corrigée |
| **Statut** | 📘 Référence fondée sur 93 cas ciblés finaux et 181 tests globaux réussis ; 73 cas nets ajoutés à la suite existante. Le passage à INT-09 a depuis été demandé ; compréhension de l'apprenant toujours non évaluée |

## TD-R015

**Un cycle d'état local n'est ni une sauvegarde ni un verrou**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-09](sprint-03/INT-09-definir-les-donnees-et-transitions.md) |
| **Story liée** | INT-09 — Définir les données et transitions |
| **Dépend de** | États de `RequestStatus` et modèle interne de Capability ; aucune nouvelle exécution pour lire ce repère |
| **Fichiers** | `StoredFamilyRequest.java`, `StoredFamilyRequestTest.java`, `InMemoryFamilyRequests.java`, [DAT — données](../docs/dat/ConfluxLab.md#6-données-mongodb) |
| **À retenir** | `pending(...)` initialise `createdAt == updatedAt`, sans `completedAt`. `transitionTo(...)` retourne une nouvelle valeur selon le graphe autorisé ; `createdAt` reste fixe, `updatedAt` ne recule pas, `completedAt` apparaît à la fin. Un état final ne se rouvre pas. Cela ne modifie pas le store et ne protège pas deux appelants concurrents ; l'écriture atomique sera traitée dans INT-11. `result?`/`error?` sont documentés pour la cible mais non implémentés |
| **Vérification pédagogique** | Expliquer pourquoi deux threads pourraient chacun produire `PROCESSING` à partir du même objet `PENDING`, et distinguer cette validation locale de l'insertion durable réalisée ensuite dans INT-10 |
| **Statut** | 📘 Référence fondée sur les 3 tests ciblés INT-09, sans MongoDB à cette étape ; INT-10 réalise ensuite la persistance. Pas de nouveau build global ni d'évaluation orale |

## TD-R016

**Insérer puis acquitter, sans promettre une issue toujours connue**

| Champ | Valeur |
|---|---|
| **Créé dans** | [Note INT-10](sprint-03/INT-10-persister-avant-d-accepter.md) |
| **Story liée** | INT-10 — Persister avant d'accepter |
| **Dépend de** | Modèle INT-09, repository MongoDB et contrat HTTP existant |
| **Fichiers** | `FamilyRequestService.java`, `MongoFamilyRequestStore.java`, `FamilyRequestConfiguration.java`, `StoredFamilyRequest.java`, `MongoFamilyRequestStoreTest.java`, `MongoTestSupport.java` |
| **À retenir** | `insert` attend l'acquittement du journal avant le reçu, et l'index `_id` interdit une collision sans upsert. Sur un standalone, `majority` ne signifie pas réplication. Une panne réseau peut masquer une écriture réussie : `500` ne garantit pas l'absence en base. UUID unique ne signifie pas idempotence du POST. Le store mémoire ne sert plus qu'aux tests ; les deux nouveaux tests vérifient MongoDB réel et un échec simulé, sans matrice exhaustive |
| **Vérification pédagogique** | Expliquer pourquoi remplacer `insert` par `save` pourrait écraser un document, pourquoi `202` n'est pas une famille créée et pourquoi rejouer un POST après timeout peut créer un doublon métier |
| **Statut** | 📘 Référence fondée sur 3 tests ciblés réussis ; incompatibilité Docker initiale corrigée par Testcontainers 1.21.4. Suite globale et redémarrage non exécutés, compréhension de l'apprenant non évaluée |
