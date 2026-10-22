# INT-10 + INT-12 — Insertion et persistance MongoDB

**[Simulation ConfluxLab] — Réalisé techniquement ; clôture pédagogique non confirmée.** Le résultat : enregistrer la demande avant son acceptation et la retrouver après remplacement de la JVM applicative, avec la même base conservée.

Les preuves INT-10 et INT-12 datent des **2026-10-21 et 2026-10-22**. Les extraits partiels viennent des sources actuelles inspectées, parfois réalignées. **Aucun code, test ou build exécuté pour cette rédaction.**

La [fiche du sprint](../../docs/stages/sprint-03-mongodb.md) donne les critères. Le cours [INT-09 + INT-11 — Modèle et transitions atomiques](INT-09-modele-et-transitions-atomiques.md) porte les états/dates et le CAS ; le [complément replica set](INT-10-complement-mongodb-replica-set.md) traite séparément la topologie, en théorie uniquement.

## 1. De l'appel HTTP à l'insertion

Le trajet reste **HTTP → Experience/BFF → appel Java à Capability → MongoDB**. Une seule application Spring Boot assemble les modules ; MongoDB est un processus de stockage extérieur à sa JVM, pas une seconde API métier.

[`FamilyRequestService`](../../capability-api/src/main/java/dev/confluxlab/capability/internal/FamilyRequestService.java) copie et valide l'entrée, y compris pour un appel Java direct. La portion actuelle qui suit cette validation est :

```java
UUID requestId = UUID.randomUUID();
StoredFamilyRequest stored = StoredFamilyRequest.pending(
        requestId, snapshot, Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
store.save(stored);
events.publishEvent(new FamilyRequestSubmitted(requestId));
return new RequestAccepted(requestId, stored.status(), "/api/v1/requests/" + requestId);
```

L'UUID corrèle cette nouvelle demande. Le `Clock` injecté fournit la date ; `truncatedTo(MILLIS)` aligne la création sur BSON Date, qui ne conserve pas les nanosecondes Java. L'insertion **synchrone et bloquante** doit revenir normalement avant la construction du reçu.

**`publishEvent` est une évolution INT-14, pas une réalisation INT-10.** Le reçu conserve le `PENDING` de l'objet initial ; le listener peut déjà avoir changé la base en `PROCESSING` avant réception du `202`. Ni le début du worker ni sa réussite ne sont garantis par cette réponse.

Le [`FamilyController`](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java) ne construit `202`, `Location` et le corps qu'après le retour du service. Une exception d'insertion rejoint l'[`ApiExceptionHandler`](../../experience-api/src/main/java/dev/confluxlab/experience/ApiExceptionHandler.java) : `500` générique, sans reçu ni `Location`. Aucun repli mémoire de production ne fabrique un succès.

## 2. Mapper l'UUID et créer sans remplacer

Le service passe par l'interface interne `FamilyRequestStore`. Dans [`MongoFamilyRequestStore.save(...)`](../../capability-api/src/main/java/dev/confluxlab/capability/internal/MongoFamilyRequestStore.java), l'opération est :

```java
mongoTemplate.insert(request);
```

Malgré le nom `save` de l'interface, on utilise **`insert`**, pas `MongoTemplate.save` : une clé existante provoque une erreur au lieu d'un remplacement. L'ancien store mémoire ne subsiste que sous `src/test` pour les tests unitaires.

| Élément inspecté | Rôle dans MongoDB |
|---|---|
| `@Document("family_requests")` sur [`StoredFamilyRequest`](../../capability-api/src/main/java/dev/confluxlab/capability/internal/StoredFamilyRequest.java) | Choisir la collection, sans exposer le document au BFF |
| `@Id UUID requestId` | Mapper l'identifiant Java vers `_id`, protégé par l'index unique natif |
| `uuid-representation: standard` | Écrire et relire l'UUID BSON selon la même convention ; le JSON HTTP utilise une chaîne |
| `MongoTemplate` | API synchrone Spring Data ; son convertisseur assure le mapping, le driver contacte le serveur |

**UUID unique ne signifie pas POST idempotent.** Deux POST de même contenu génèrent deux clés et peuvent créer deux demandes. Aucun second champ `requestId` à indexer ni dédoublonnage métier n'est ajouté.

## 3. Acquittement et configuration

Dans [`FamilyRequestConfiguration`](../../capability-api/src/main/java/dev/confluxlab/capability/internal/FamilyRequestConfiguration.java), le bean `MongoTemplate` reçoit :

```java
template.setWriteConcern(WriteConcern.MAJORITY.withJournal(true).withWTimeout(5, TimeUnit.SECONDS));
```

Le **write concern** fixe la confirmation exigée avant le retour normal. `MAJORITY` demande une majorité ; `withJournal(true)` demande la journalisation correspondante. Sur le **standalone** du labo, la majorité est l'instance unique : aucune autre copie n'existe. Le journal aide à la récupération locale, pas à restaurer un disque perdu.

`withWTimeout(5, ...)` concerne l'attente serveur de satisfaction du write concern lorsqu'elle s'applique. **Il ne borne pas universellement le flush du journal, notamment sur ce standalone, ni toute la requête HTTP.** Les délais réseau restent distincts.

Extrait d'[`application.yml`](../../application/src/main/resources/application.yml) :

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/conflux_lab?serverSelectionTimeoutMS=3000&connectTimeoutMS=3000&socketTimeoutMS=5000&retryWrites=false}
      uuid-representation: standard
```

Spring choisit `MONGODB_URI` si elle est fournie, sinon l'URI après `:`. Une URI externe peut remplacer les options ci-dessous : ce sont des **défauts pédagogiques**, pas un dimensionnement de production ni une politique inviolable.

| Réglage | Fonction et limite |
|---|---|
| `localhost:27017/conflux_lab` | Base locale ; ne démarre pas MongoDB. Dans un conteneur, `localhost` désignerait ce conteneur |
| `serverSelectionTimeoutMS=3000` | Attendre la sélection d'un serveur approprié |
| `connectTimeoutMS=3000` | Borner l'établissement d'une connexion, pas l'exécution complète de l'insertion |
| `socketTimeoutMS=5000` | Délai de lecture réseau ; le client peut abandonner avant de recevoir la réponse |
| `retryWrites=false` | Désactiver les retryable writes du driver avec cette URI ; aucun retry applicatif ajouté |
| `uuid-representation: standard` | Garder une convention UUID compatible entre écriture et lecture |

**L'issue d'une écriture peut rester inconnue.** Après insertion ou modification réussie, une déconnexion peut perdre l'acquittement : timeout ou `500` ne prouve pas l'absence du document. Socket timeout et `wtimeout` valent ici tous deux 5 s ; cela ne laisse pas nécessairement de marge pour recevoir le diagnostic serveur. Ne pas additionner les délais pour annoncer une borne HTTP ni rejouer aveuglément le POST.

Le [complément théorique](INT-10-complement-mongodb-replica-set.md) explique ce qui changerait avec réplication et élection. Il ne décrit aucun cluster créé ou testé.

## 4. Relire sans exposer le document

Extrait de `MongoFamilyRequestStore.findById(...)` :

```java
return Optional.ofNullable(mongoTemplate.findById(requestId, StoredFamilyRequest.class));
```

Spring Data cherche la clé `_id` et reconstruit le record via son mapping. Un résultat `null` devient une absence dans l'`Optional` ; une panne réseau reste une exception, pas une absence artificielle.

Dans `FamilyRequestService.getRequest(...)`, le résultat est adapté ainsi :

```java
StoredFamilyRequest stored = store.findById(requestId)
        .orElseThrow(RequestNotFoundException::new);
return new RequestView(stored.requestId(), stored.status(), stored.createdAt());
```

L'absence devient `404` via le BFF. La **projection** ne contient que trois métadonnées : ni payload, ni dates internes, ni diagnostic technique. Le GET ne sauvegarde rien ; un état métier `FAILED` reste consultable en `200`, ce n'est pas une erreur HTTP de consultation.

## 5. Redémarrage du JAR et Failsafe

Relire deux fois depuis le même repository ne distingue pas MongoDB d'une map encore vivante. [`FamilyRequestPersistenceIT`](../../application/src/test/java/dev/confluxlab/FamilyRequestPersistenceIT.java) utilise donc le **JAR Boot réellement packagé**, de vraies requêtes HTTP et deux JVM applicatives successives :

1. Lancer le JAR avec l'URI MongoDB temporaire, créer la demande et consulter ses métadonnées.
2. Arrêter la première JVM et confirmer sa fin, en **laissant MongoDB vivant**.
3. Relancer le même JAR avec la même base, consulter sans second POST et comparer UUID, statut et `createdAt`.
4. Arrêter les processus applicatifs ; Testcontainers nettoie ensuite ses ressources temporaires.

**INT-12 prouvait la conservation de `PENDING`.** Depuis INT-14, le scénario existant attend `PROCESSING` avant l'arrêt et vérifie cet état après relance. Le listener actuel s'arrête à la tentative de prise en charge, décrite dans le [cours modèle et CAS](INT-09-modele-et-transitions-atomiques.md#5-pourquoi-un-état-final-ne-peut-pas-être-écrasé) : conserver `PROCESSING` n'est pas reprendre un travail métier.

[`MongoTestSupport`](../../application/src/test/java/dev/confluxlab/MongoTestSupport.java) fournit l'image officielle `mongo:8.0.16` standalone, une URI dynamique et la base isolée `conflux_lab_test`. Les contextes Spring et les JVM enfants utilisent cette base, pas une base utilisateur. Son cache WiredTiger de 0,25 Gio ne plafonne pas la mémoire totale du conteneur.

Le test exige le JAR du cycle en cours : **`test` → `package` et repackage Boot → `integration-test` → `verify`**. Surefire exécute les `*Test` à `test` ; Failsafe exécute les `*IT` après le packaging, puis contrôle leurs résultats à `verify`.

Extrait des goals activés dans [`application/pom.xml`](../../application/pom.xml), sous `build/plugins` :

```xml
<goals>
    <goal>integration-test</goal>
    <goal>verify</goal>
</goals>
```

Ces goals raccordent réellement les IT au cycle. Le [`POM racine`](../../pom.xml) fixe Failsafe à `3.5.4` dans `pluginManagement` ; **la version gérée seule ne l'activerait pas**. Renommer en `*IT` sans ce raccordement pourrait faire disparaître les tests exécutés.

| Configuration du plugin | Rôle |
|---|---|
| `failIfNoTests=true` | Refuser une réussite silencieuse si aucun IT n'est découvert |
| `application.jar` via `systemPropertyVariables` | Fournir à la JVM de test le chemin du JAR à lancer, pas une propriété métier |

Les quatre cas MongoDB existants ont été renommés, pas recopiés : [`MongoFamilyRequestStoreIT`](../../application/src/test/java/dev/confluxlab/capability/internal/MongoFamilyRequestStoreIT.java) et [`MongoFamilyRequestTransitionIT`](../../application/src/test/java/dev/confluxlab/capability/internal/MongoFamilyRequestTransitionIT.java). INT-12 ajoute seulement le scénario de redémarrage.

## 6. Commandes essentielles et preuves historiques

**Répertoire : racine `conflux-lab/`. Rien n'est lancé pour cette note.** Prérequis : JDK 21, Maven Wrapper, moteur Docker accessible, dépendances/images en cache ou téléchargeables. Docker est requis pour les tests MongoDB/Spring concernés, pas pour le modèle unitaire seul. Certains `*Test` HTTP sous Surefire utilisent aussi MongoDB : suffixe ne signifie pas nature du test.

```sh
# Contrôle préalable possible, sans démarrer de base : accès au serveur Docker.
docker version --format '{{.Server.Version}}'
```

Ce contrôle ne prouve pas la disponibilité d'une base applicative. Testcontainers démarre son propre serveur isolé ; la version `1.21.4` épinglée corrige l'incompatibilité d'API Docker rencontrée avec `1.21.3` dans INT-10.

```sh
# Commande historique INT-12, non réexécutée : Surefire sélectionné, Failsafe non filtré.
./mvnw -B -ntp '-Dtest=ModuleArchitectureTest#experienceOnlyUsesThePublicCapabilityApi' -Dsurefire.failIfNoSpecifiedTests=false clean verify
```

`-B -ntp` donne une sortie batch sans progression des téléchargements. `-Dtest=Classe#méthode` sélectionne Surefire ; `surefire.failIfNoSpecifiedTests=false` autorise les modules sans ce test, sans masquer un échec. `clean` supprime les sorties `target/`, pas les sources, notamment les classes compilées avant renommage. `verify` compile et package le réacteur puis atteint Failsafe.

**`-Dtest` ne filtre pas Failsafe, dont la sélection utilise `-Dit.test`.** Sans cette dernière, les IT présents sont découverts normalement. L'inventaire a évolué depuis INT-12 : la même commande actuelle peut donc exécuter davantage d'IT que les cinq cas historiques.

| Étape et date | Résultat réellement observé |
|---|---|
| **INT-10 — 2026-10-21** | **3 cas ciblés réussis** : 2 nouveaux + 1 suivi réutilisé. Insertion/relecture/collision sur MongoDB réel ; exception d'insertion simulée avec vrai service et BFF, sans faux reçu |
| **INT-12 — 2026-10-22** | **1 cas Surefire + 5 cas Failsafe réussis**, `BUILD SUCCESS`, aucun échec/erreur/ignoré : 1 frontière, 2 cas insertion/erreur, 2 cas transitions et 1 nouveau redémarrage |

La panne d'insertion simulée prouve le `500` générique sans `Location`, pas une coupure réseau après écriture. Les rapports et traces ont été observés historiquement sous `application/target/`, ignoré par Git ; ils peuvent disparaître au prochain `clean`. Les deux JVM ont été arrêtées et les conteneurs supprimés à l'issue d'INT-12. Ils ne sont ni réinspectés ni régénérés pour cette note.

`./mvnw -B -ntp verify` **sans filtre** est configuré pour Surefire et Failsafe, mais n'a pas été exécuté tel quel dans INT-12. Les six réussites sélectionnées ne constituent **ni une suite complète verte ni une CI exécutée**.

## 7. Portée de la persistance démontrée

La demande survit au remplacement de la JVM applicative si la même base et son stockage sont conservés. La preuve ne redémarre pas MongoDB et ne teste ni crash brutal, ni perte du disque, ni restauration de sauvegarde ou haute disponibilité.

Le `202` confirme l'acceptation persistée, pas une famille créée ni une tâche durable. La protection des transitions et ses limites — pas d'exactly-once ou de transaction commune avec un partenaire HTTP — relèvent du [cours modèle et transitions](INT-09-modele-et-transitions-atomiques.md#6-preuves-historiques-et-limites-du-résultat).

### Repères et suivi

- [TD-B009 — Persistance avant acceptation](../todos.md#td-b009), [TD-R016 — Insertion et acquittement](../todos.md#td-r016).
- [TD-B011 — Persistance et raccordement Maven](../todos.md#td-b011), [TD-R019 — Cycle Maven et deux JVM](../todos.md#td-r019).
