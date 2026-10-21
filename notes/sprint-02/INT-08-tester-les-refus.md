# INT-08 — Tester les refus

| Contexte | Description |
|---|---|
| **Sprint** | [02 — Contrat REST et BFF mince](../../docs/stages/sprint-02-rest.md) |
| **Statut** | **Terminée sur feu vert « commit 07 & go 08 »** ; clôture du sprint et INT-09 non autorisées |
| **Date des preuves** | 2026-10-21 |
| **Objectif** | Comprendre quelques scénarios essentiels, puis expliquer comment les tests prouvent le refus, l'absence d'enregistrement, la confidentialité et l'isolation |
| **Preuves** | Passages ciblés avec échecs expliqués, passage ciblé final vert, `clean verify`, rapports Surefire, diagnostics et JAR réel |
| **Git lors des preuves initiales** | INT-07 commitée localement au préalable : `5859b57`, `feat(INT-07): expose request tracking metadata and lookup errors` ; INT-08 n'était pas encore commitée. Commit local demandé ensuite par l'utilisateur ; aucun push |
| **Suivi** | [Registre central](../todos.md) ; aucune connaissance de l'apprenant n'est déclarée évaluée |

**[Simulation ConfluxLab]** Les marqueurs et personnes sont fictifs. Les preuves concernent ce laboratoire, pas des tests ou mesures historiques AXA.

## Sommaire

- [1. Faut-il autant de tests pour comprendre un refus ?](#1-faut-il-autant-de-tests-pour-comprendre-un-refus-)
- [2. Les scénarios essentiels, avant la matrice](#2-les-scénarios-essentiels-avant-la-matrice)
- [3. Ce que chaque niveau de test prouve](#3-ce-que-chaque-niveau-de-test-prouve)
- [4. Le défaut réellement découvert et sa correction](#4-le-défaut-réellement-découvert-et-sa-correction)
- [5. Commandes, résultats et limites](#5-commandes-résultats-et-limites)
- [6. Formulation d'entretien](#6-formulation-dentretien)
- [7. Suivi centralisé](#7-suivi-centralisé)

## 1. Faut-il autant de tests pour comprendre un refus ?

**Non. Quelques scénarios bien expliqués suffisent pour comprendre le principe.** La suite ajoutée ici est plus large que le strict minimum pédagogique : elle sert à détecter des régressions sur plusieurs variantes et plusieurs frontières. Le nombre de cas ne mesure ni la compréhension du développeur, ni la qualité absolue du logiciel.

Il faut aussi distinguer **méthode de test** et **cas exécuté**. Un test paramétré est une méthode exécutée avec plusieurs jeux de données : principal absent, liste nulle, un seul ayant droit, etc. Il ne s'agit pas de réécrire tout le test pour chaque valeur.

### D'où viennent les 181 cas ?

| Partie du projet | Cas du build global |
|---|---:|
| Contrats : contraintes et JSON, déjà présents depuis INT-05 | 59 |
| Capability : acceptation, stockage, lecture, refus et isolation | 43 |
| BFF : délégation et adaptation des erreurs | 4 |
| Application : contexte, HTTP et architecture | 75 |
| **Total de tout le projet** | **181** |

Il y avait **108 cas avant INT-08**. Cette tâche en ajoute **73 nets**, surtout sous forme de variantes paramétrées. **Ce ne sont donc pas 181 tests nécessaires pour comprendre une demande refusée.**

Cette couverture a un coût : plus de données de test et de règles à maintenir. Son intérêt doit rester rattaché à un risque concret, pas à un objectif de compteur. Nous n'ajoutons pas de variantes sans fin, ni de règle métier non définie, ni de nouvelle infrastructure.

**Lecture conseillée :** comprendre d'abord les six scénarios ci-dessous. La matrice et les détails techniques servent ensuite de référence et de preuve ; il n'est pas nécessaire de mémoriser chaque ligne pour expliquer le parcours.

## 2. Les scénarios essentiels, avant la matrice

| Scénario simple | Résultat attendu | Pourquoi le statut seul ne suffit pas |
|---|---|---|
| Une demande valide | `202`, UUID et `PENDING`, après conservation | Un serveur qui refuserait tout ne serait pas correct non plus |
| Principal absent ou un seul ayant droit | `400`, aucun acquittement | On doit aussi vérifier que l'acceptation n'est pas appelée |
| Même entrée invalide appelée directement en Java | Refus de Capability, store non touché | Cet appel contourne le `@Valid` HTTP du BFF |
| UUID valide mais absent | `404` générique | Une absence n'est ni un format invalide ni un échec de traitement |
| Une dépendance lève une exception interne | `500` générique, sans faux succès ni détails privés | Le client ne doit voir ni cause interne ni données personnelles |
| Deux demandes, avec un refus entre elles | UUID, payloads et vues distincts et stables | Une réponse correcte isolée ne prouve pas l'absence de mélange entre dossiers |

Le fil à retenir est :

```text
Entrée invalide
    → refus explicite
    → aucun reçu d'acceptation
    → aucune écriture
    → réponse compréhensible, sans recopier la saisie ou les détails internes
```

Il s'agit des **contraintes déjà définies** : présence des personnes, exactement deux ayants droit, noms non blancs de 100 caractères maximum, dates présentes et non futures. Aucune règle d'identité, d'unicité des personnes, d'âge ou d'assurance n'a été inventée. Quand la story parle d'invariants de Capability, nous vérifions ces contraintes actuelles et les garanties déjà décidées, pas un métier d'assurance supposé.

## 3. Ce que chaque niveau de test prouve

### 3.1. HTTP : est-ce refusé avant l'acceptation ?

[`FamilyRequestRefusalHttpTest`](../../application/src/test/java/dev/confluxlab/FamilyRequestRefusalHttpTest.java) démarre les vrais modules avec `MockMvc`. `@MockitoSpyBean FamilyRequests` enveloppe le service réel pour observer ses appels, sans remplacer son comportement par un résultat préparé.

Extrait partiel du contrôle :

```java
verify(familyRequests, never()).createFamily(any());
```

Il prouve que l'entrée refusée par MVC n'atteint pas l'acceptation. Il complète les assertions `400` et l'absence de `Location`, `requestId` et `statusUrl`. Dans le parcours existant, aucun autre composant HTTP n'écrit dans le store.

La classe exécute **50 cas** :

- 42 mutations du JSON : champs absents ou nuls, bornes, cardinalité, validation imbriquée, dates et formes d'objets/listes invalides ;
- 5 corps illisibles ou incompatibles : JSON tronqué, racine `null`, corps vide, tableau ou chaîne à la racine ;
- 1 type de contenu non JSON, refusé en `415` ;
- 1 UUID mal formé contenant un marqueur personnel fictif ;
- 1 cas **valide** avec les six noms à exactement 100 caractères, accepté par le vrai service.

Une mutation comme celle-ci modifie un champ d'un nouvel exemple valide, puis réutilise le même contrôle :

```java
invalidRequest("principal null", request -> request.putNull("principal"))
```

Les dates futures HTTP sont calculées à partir du jour UTC plus un an : elles restent largement futures sans dépendre d'un « demain » codé en dur. La syntaxe d'une date impossible relève de Jackson ; une date lisible mais future relève de la validation. Ce sont deux chemins distincts, même si tous deux donnent `400`.

### 3.2. Java direct : le BFF n'est pas le seul gardien

Dans `InMemoryFamilyRequestsTest` (désormais [`FamilyRequestServiceTest`](../../capability-api/src/test/java/dev/confluxlab/capability/internal/FamilyRequestServiceTest.java)), **25 cas invalides** sont transmis directement au service : 7 cas structurels et 9 variantes de personne testées comme principal puis comme premier ayant droit. Cela ne prétend pas parcourir toute permutation possible des personnes.

Extrait partiel du contrôle :

```java
InvalidFamilyRequestException exception = assertThrows(InvalidFamilyRequestException.class,
        () -> service.createFamily(request));
assertEquals("Invalid family request.", exception.getMessage());
verifyNoInteractions(store);
```

Un **mock** est ici une doublure du store utilisée pour observer les interactions. `verifyNoInteractions` prouve plus qu'une absence de `save` : le refus ne touche pas du tout le store. Le validateur est réel, avec un `clockProvider` fixe ; les tests ne font pas semblant de valider.

Les tests positifs de conservation utilisent, eux, le vrai store mémoire. Le cas limite à 100 caractères évite de confondre « le test sait vérifier un refus » avec « l'application accepte et refuse correctement ».

### 3.3. Pannes simulées : ne pas transformer une erreur en succès ou en absence

[`FamilyRequestAcceptanceFailureHttpTest`](../../application/src/test/java/dev/confluxlab/FamilyRequestAcceptanceFailureHttpTest.java) utilise `@MockitoBean`, cette fois pour **remplacer** Capability et provoquer trois situations contrôlées :

- échec d'acceptation → `500`, pas `202` ;
- échec de lecture → `500`, pas un faux `404` ;
- refus explicite de Capability sur une forme HTTP valide → `400` générique.

Les messages, causes et noms d'exceptions préparés dans ces tests ne doivent pas apparaître dans la réponse. Ces simulations vérifient l'adaptation du BFF ; **elles ne reproduisent pas une panne réelle de MongoDB**, qui n'existe pas encore dans le projet.

### 3.4. Isolation : un dossier ne doit pas remplacer l'autre

Deux ajouts complètent les tests existants :

- deux payloads réellement différents, deux dates d'acceptation distinctes, avec un refus intercalé ; chaque service relit bien les données de la demande demandée ;
- deux appelants Java démarrés depuis un executor de test avec un signal commun, puis vérification des UUID, payloads et vues respectifs.

Les attentes, `Future.get` et l'arrêt des workers sont bornés ; aucun `sleep` n'est utilisé. Ce test vérifie l'isolation d'appelants concurrents, **pas un gain de parallélisme ni un benchmark**. Il n'ajoute aucun executor, événement ou asynchronisme dans l'application. Les délais sont individuels ; le signal commun ne prouve pas un chevauchement précis au sein de chaque instruction du service.

## 4. Le défaut réellement découvert et sa correction

### 4.1. Un détail générique ne suffisait pas

Le premier passage ciblé a révélé une fuite sur un UUID volontairement mal formé. Le corps observé contenait :

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request.",
  "instance": "/api/v1/requests/not-a-uuid-INT08FictivePerson"
}
```

Le détail était générique, **mais le marqueur fictif était recopié dans `instance`**. Spring complète normalement cette propriété avec le chemin courant lorsqu'elle n'est pas renseignée. Vérifier seulement `detail` aurait laissé passer ce défaut.

### 4.2. Corriger la cause dans le BFF

Extrait partiel exact de [`ApiExceptionHandler`](../../experience-api/src/main/java/dev/confluxlab/experience/ApiExceptionHandler.java) :

```java
ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
// A non-null empty URI-reference prevents MVC from echoing the rejected request path.
problem.setInstance(URI.create(""));
return problem;
```

Une référence URI vide est valide. Non nulle, elle empêche MVC de compléter automatiquement le chemin rejeté. Avec la sérialisation actuelle, la valeur vide est **omise du JSON**. Le corps observé devient :

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Invalid request."}
```

Nous n'ajoutons ni identifiant d'erreur, ni nouvelle infrastructure, et les statuts/détails existants ne changent pas. **Impact de représentation : `instance` n'est plus exposé par cet advice.**

Les tests unitaires vérifient la référence vide sur l'objet Java ; les tests HTTP exigent l'absence stricte du champ et du marqueur dans tout le corps. Cette vérification à deux niveaux encadre le comportement combiné MVC/Jackson. Si une future version sérialisait `instance: ""`, le contrat testé changerait, mais ce seul champ vide ne serait pas une fuite.

### 4.3. Chronologie honnête des vérifications

| Passage ciblé | Résultat observé | Explication |
|---|---|---|
| Avant correction | 1 échec parmi les 93 cas ciblés | Marqueur recopié dans `instance` |
| Après correction, avec assertion `instance == ""` | 52 échecs de représentation | Le marqueur avait disparu, mais Jackson omettait le champ ; mon assertion attendait une propriété inexistante |
| Assertion alignée sur l'omission réelle, contrôle anti-marqueur conservé | **93 cas réussis** | Absence stricte d'`instance`, pas d'assouplissement de la confidentialité |
| Build global final | **181 cas réussis** | Contrats, modules, HTTP et architecture revalidés |

Le deuxième échec est donc une erreur d'attente dans le test, pas une réapparition de la fuite. La note conserve cette distinction au lieu de présenter uniquement un résultat vert final.

## 5. Commandes, résultats et limites

### 5.1. Commandes réellement exécutées

Depuis la racine `conflux-lab/`, avec JDK 21 et le Wrapper :

```sh
GIT_EDITOR=true git commit -m "feat(INT-07): expose request tracking metadata and lookup errors"
./mvnw -B -ntp -pl application -am -Dtest=InMemoryFamilyRequestsTest,FamilyRequestRefusalHttpTest,FamilyRequestAcceptanceFailureHttpTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -B -ntp clean verify
```

Le commit, créé avant INT-08 après préparation explicite des chemins, est **`5859b57`**, avec 22 fichiers concernés ; il écrit l'historique local, pas GitHub. À l'issue de ces vérifications initiales, INT-08 n'était pas encore commitée et aucun push n'avait été exécuté.

`-B -ntp` utilise le mode batch et réduit la progression des téléchargements ; `-pl application -am` construit l'assemblage et ses dépendances. `-Dtest` choisit les trois classes ; `failIfNoSpecifiedTests=false` autorise les modules sans ces noms, sans masquer un échec. Cette même commande ciblée a été exécutée pour les trois passages décrits plus haut.

`clean` supprime les sorties `target/`, pas les sources. `verify` recompile, exécute les tests raccordés et produit les archives. Ces commandes peuvent télécharger des dépendances et écrivent localement ; pas besoin de Docker, base ou partenaire, et aucun déploiement n'est réalisé.

Rapports Surefire relus après le build :

| Module | Cas | Échecs / erreurs / ignorés |
|---|---:|---|
| `contracts` | 59 | 0 / 0 / 0 |
| `capability-api` | 43 | 0 / 0 / 0 |
| `experience-api` | 4 | 0 / 0 / 0 |
| `application` | 75 | 0 / 0 / 0 |
| **Total** | **181** | **0 / 0 / 0** |

**`BUILD SUCCESS`**. Les diagnostics de l'éditeur sont sans erreur ni avertissement. Les avertissements d'auto-attachement Mockito/JVM restent non bloquants sous Java 21 ; ce n'est pas une validation d'un futur JDK.

**Contrôle ciblé avant le commit demandé ensuite (2026-10-21).** Les rapports existants ont d'abord été relus : 181 cas, sans échec, erreur ou ignoré. La suite globale n'a pas été relancée. Un seul test de non-régression a été exécuté, depuis la même racine et avec les mêmes prérequis :

```sh
./mvnw -B -ntp -pl application -am '-Dtest=FamilyRequestRefusalHttpTest#rejectsAMalformedUuidWithoutEchoingPersonalDataEvenInTheProblemInstance' -Dsurefire.failIfNoSpecifiedTests=false test
```

La syntaxe `Classe#méthode` sélectionne uniquement le défaut corrigé : un UUID invalide ne doit pas être recopié dans la réponse d'erreur. **Résultat de ce nouveau passage : 1 test réussi, `BUILD SUCCESS`**, aucun échec/erreur/ignoré. Cette exécution écrit les sorties locales et actualise le rapport de cette classe ; elle ne constitue pas un nouveau passage des 181 tests. Aucun JAR réel n'a été redémarré pour ce contrôle.

À la demande de l'utilisateur, les [consignes permanentes](../../.agents/skills/farsameh/AGENTS.md) imposent désormais le strict minimum de tests pertinents, sans matrice exhaustive ni objectif chiffré de couverture. Les tests déjà présents sont conservés.

### 5.2. Démonstration du JAR réel

Un superviseur Python a exécuté depuis la racine :

```sh
java -jar application/target/application-0.1.0-SNAPSHOT.jar --server.port=0 --server.address=127.0.0.1
```

Le port libre observé était **35135**. Attente maximale de démarrage : 30 secondes ; timeout de requête : 5 secondes. Les requêtes ont utilisé `urllib.request` et la fixture existante avec des marqueurs fictifs, pas des données assurés réelles.

Résultats effectivement observés :

- première demande valide : POST `202`, GET `200 PENDING`, métadonnées seulement ;
- principal absent, zéro/trois ayants droit, nom de 101 caractères, date imbriquée impossible et JSON tronqué : `400` ;
- contenu `text/plain` : `415` ;
- UUID mal formé contenant le marqueur personnel fictif : `400`, sans écho ;
- UUID inconnu : `404` ;
- toutes ces erreurs : détail générique, **aucun `instance`, marqueur ou champ de succès** ;
- seconde demande valide : UUID distinct ; les deux vues restent inchangées autour des refus.

Sortie finale : `INT-08 packaged refusal and privacy smoke check: PASS`. Le processus a été arrêté dans un `finally`, avec attente bornée et arrêt forcé prévu si nécessaire. Le log `application/target/int-08-smoke.log` est local, ignoré par Git et supprimable par `clean`.

Cette démonstration vérifie les réponses et la stabilité des vues. **La preuve précise « pas d'appel à l'acceptation / pas d'interaction avec le store » vient des tests instrumentés**, pas d'une déduction fondée uniquement sur le statut HTTP.

### 5.3. Ce que cette couverture ne promet pas

- Pas d'exhaustivité de toutes les combinaisons, coercitions ou politiques de champs inconnus non spécifiées.
- Pas d'audit complet de sécurité, ni de preuve sur tous les logs, filtres, proxys ou chemins d'erreur hors de l'advice.
- Pas d'idempotence, d'authentification ou de règle métier d'assurance ajoutée.
- Mémoire toujours volatile, non bornée et sans nettoyage ; demandes toujours `PENDING` sans traitement.
- Pas de MongoDB, événement Spring, `@Async`, partenaire, CI, CD, benchmark ou nouvelle validation GUI/Windows.
- Les attentes bornées des tests ne constituent pas un dimensionnement de pool de production.
- Les stories techniques du sprint sont réalisées, mais sa clôture et la restitution de l'apprenant ne sont pas cochées automatiquement.

## 6. Formulation d'entretien

> « Pour vérifier un refus, je ne me limite pas au code HTTP : je contrôle aussi l'absence d'effet de bord et la confidentialité. Dans ConfluxLab, les tests HTTP prouvent que les données invalides n'atteignent pas l'acceptation ; les tests Java directs vérifient que Capability protège ses contraintes sans le BFF et sans toucher au store. Des doublures contrôlent les erreurs internes et deux demandes restent isolées. Cette couverture a révélé une fuite concrète : un chemin rejeté était recopié dans `ProblemDetail.instance`, même avec un détail générique. Le correctif est testé sur l'objet Java et le JSON réel. La matrice est une protection de non-régression, pas un nombre de tests nécessaire pour comprendre un refus. »

**Piège :** « `400` prouve-t-il que rien n'a été enregistré ? » **Non.** Il faut vérifier l'absence d'appel ou d'écriture ; un programme incorrect pourrait enregistrer puis retourner `400`.

**Piège :** « Pourquoi tester un cas valide dans une suite de refus ? » **Pour éviter le faux succès d'un système qui rejetterait tout**, notamment à la borne exacte de 100 caractères.

## 7. Suivi centralisé

- [TD-B005 — Parcours REST mémoire et refus vérifiés](../todos.md#td-b005).
- [TD-R005 — Annotations et effets réellement prouvés](../todos.md#td-r005).
- [TD-R012 — Conservation avant acquittement](../todos.md#td-r012).
- [TD-R014 — Scénarios essentiels, matrice et confidentialité](../todos.md#td-r014).
