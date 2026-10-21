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
| [TD-B005](#td-b005) | Valider le parcours REST | INT-06/07/08 | Appel Java branché ; métier absent (`503`), feu vert requis |
| [TD-B006](#td-b006) | Vérifier les raccourcis dans l'interface Zed | INT-02 | Vérification manuelle non constatée |
| [TD-B007](#td-b007) | Vérifier le Wrapper sous Windows | INT-02 | Optionnel ; environnement Windows requis |
| [TD-B008](#td-b008) | Revoir `gh` ou SSH si nécessaire | INT-37 | Non prioritaire : Git HTTPS a fonctionné |

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
| **Créé dans** | [Note INT-06](sprint-02/INT-06-accepter-une-demande.md), ébauche non validée |
| **Stories liées** | INT-06, INT-07 et INT-08 ; chaque story conserve son feu vert individuel |
| **Dépend de** | INT-05 et socle assemblé/vérifié, notamment [TD-B004](#td-b004) |
| **Fichiers** | `experience-api/src/`, `capability-api/src/`, `contracts/src/` |
| **Action** | Brancher une acceptation réelle en mémoire dans Capability, puis le GET et les refus, sans HTTP interne ni prétendre que la famille est créée |
| **Validation** | Tests POST `202`, GET `200`/`404`, entrée invalide `400`, refus sans enregistrement et demandes distinctes ; état `PENDING` et limite de mémoire explicités |
| **Statut** | ⏳ À autoriser story par story ; INT-03 a seulement branché l'API Java et vérifié POST/GET valides `503`, sans enregistrement ni suivi métier |

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
| **Statut** | 📘 Référence de contrat — aucun parcours HTTP fonctionnel déduit de la seule lecture du contrôleur |

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
| **Statut** | 📘 Référence documentée — DTO testés dans INT-05 ; délégation Java et indisponibilité `503` vérifiées dans INT-03, mais acceptation et suivi métier toujours non réalisés |

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
| **Fichiers** | `application/pom.xml`, `ConfluxLabApplication.java`, `FamilyRequests.java`, `UnavailableFamilyRequests.java`, `FamilyControllerTest.java`, `ApplicationAssemblyTest.java` |
| **À retenir** | Une dépendance Maven fournit des types, pas automatiquement un bean. Le démarrage placé au-dessus des packages découvre l'implémentation, injectée via l'interface publique. Le test unitaire peut retourner un succès préparé ; le contexte réel et le JAR doivent refléter l'indisponibilité des opérations absentes (`503`), sans faux identifiant ni prise en charge |
| **Vérification pédagogique** | Expliquer pourquoi le contrôleur obtient `202` avec un double mais le JAR répond `503`, et pourquoi un test MockMvc ne prouve pas à lui seul le démarrage du JAR |
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
