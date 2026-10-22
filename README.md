# ConfluxLab

Laboratoire Java/Spring : accepter une création de famille fictive de trois personnes, suivre son état et apprendre une orchestration de six opérations. **[Simulation ConfluxLab]** Ce n'est ni une reproduction certifiée d'un SI AXA ni une application de production.

- [Plan des tâches et résultats](docs/stages/README.md)
- [Cours regroupés — code expliqué par fragments](notes/README.md)
- [DAT de référence](docs/dat/ConfluxLab.md) · [Contrat HTTP](docs/dat/http-contracts.md) · [Suivi utile](notes/todos.md)

## État actuel

Le POST valide la demande, génère un UUID et attend l'insertion `PENDING` dans MongoDB avant de publier un événement local et de retourner son reçu. Le listener `@Async` tente atomiquement `PENDING → PROCESSING`, sur un pool borné. Le GET ne fait que lire `requestId`, `status` et `createdAt`.

Le graphe A–F existe en Java, avec dépendances, propagation des échecs et executor explicite, **mais il n'est pas raccordé au listener**. Aucun appel partenaire, état final métier de production, mécanisme de reprise ou workflow CI n'est livré. Le prochain lot à autoriser est [INT-21 + INT-22 — Créer une famille de bout en bout](docs/stages/sprint-06-partenaire.md).

| HTTP | Comportement actuel |
|---|---|
| POST `/api/v1/families` | `202` avec UUID, `PENDING`, `statusUrl` et `Location` si insertion et soumission sont confirmées |
| GET `/api/v1/requests/{requestId}` | `200` pour une demande connue ; métadonnées seulement, sans payload ni mutation |
| Refus | Format invalide `400`, UUID inconnu `404`, panne inattendue `500` générique ; aucune exception brute exposée par l'advice |

`202` n'est pas une réussite métier. Le listener peut déjà avoir modifié l'état au retour HTTP. Une saturation de soumission peut laisser un document `PENDING` malgré un `500` ; un timeout d'écriture peut masquer une insertion réussie. L'UUID corrèle sans rendre un nouveau POST idempotent. L'événement reste local et non durable.

## Une application, plusieurs modules

| Module | Responsabilité |
|---|---|
| `contracts` | DTO et contraintes partagés ; ni contrôleur, ni stockage, ni orchestration |
| `experience-api` | BFF mince : entrée HTTP, validation de forme, liens et adaptation des réponses |
| `capability-api` | API Java publique, invariants, statuts, MongoDB, événements et orchestration internes |
| `application` | Assemblage et seul démarrage/JAR Spring Boot exécutable |

Experience appelle Capability **en Java dans la même JVM**, sans HTTP interne ni accès à ses classes internes. Les frontières sont contrôlées par ArchUnit sous `application/src/test`, absent du JAR livré. Feign/Resilience4j reste réservé au futur partenaire fictif.

## Prérequis

- **JDK 21** ; Maven Wrapper fourni (`./mvnw`, ou `./mvnw.cmd` sous Windows), Maven **3.9.16** épinglé. Le script Windows n'a pas été exécuté dans les preuves Linux.
- Accès à Maven Central pour les dépendances absentes du cache.
- **Docker pour les tests MongoDB et Spring d'assemblage** : Testcontainers 1.21.4 utilise `mongo:8.0.16`, supprimé après les tests. Certains `*Test` Surefire utilisent aussi cette base ; ils ne sont pas tous unitaires.
- Pour réussir POST/GET localement : MongoDB accessible via `MONGODB_URI`. Le défaut est `mongodb://localhost:27017/conflux_lab?serverSelectionTimeoutMS=3000&connectTimeoutMS=3000&socketTimeoutMS=5000&retryWrites=false`, UUID standard.

[`.mvn/jvm.config`](.mvn/jvm.config) force IPv4 pour le processus Maven après une panne IPv6 historique ; cela ne configure ni JDTLS ni la JVM applicative. À réévaluer sur un réseau exclusivement IPv6.

## Vérifier depuis la racine

Sélection du graphe, sans Docker :

```sh
./mvnw -B -ntp -pl capability-api -am -Dtest=FamilyOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

`-pl ... -am` sélectionne Capability et ses dépendances ; `-Dtest` filtre Surefire ; la propriété `failIfNoSpecifiedTests=false` accepte les modules sans cette classe. `-B -ntp` donne une sortie batch sans progression des téléchargements. La commande compile si nécessaire et écrit les sorties sous `target/`, sans publication.

**Dernière exécution historique INT-20, le 2026-10-22 : 6 cas réussis, 0 échec/erreur/ignoré.** Elle réutilise trois cas et en ajoute trois pour rejet et partage/isolation ; ni IT, Spring, MongoDB ni HTTP démarrés. Les avertissements connus d'auto-attachement Mockito/Byte Buddy n'empêchent pas ce succès. Aucun test n'a été relancé pour la consolidation documentaire.

Vérification complète prévue, avec Docker :

```sh
./mvnw -B -ntp verify
```

Le cycle est configuré pour inclure Surefire puis Failsafe (`*IT`) après le packaging Boot. **Ne pas confondre cette commande non filtrée avec les passages ciblés ci-dessous** ; aucun nouveau résultat global n'est revendiqué.

| Preuve historique | Résultat et limite |
|---|---|
| Socle INT-03/04 | 64 puis 76 tests globaux à ces étapes ; seul JAR démarré dans INT-03, POST/GET encore `503` |
| REST INT-08 | Dernier `clean verify` non filtré observé : **181 cas**, avant MongoDB/Failsafe ; pas le total actuel validé |
| MongoDB INT-12 | **1 Surefire sélectionné + 5 Failsafe**, même JAR relancé sur la même base ; pas toute la suite ni redémarrage MongoDB |
| Async INT-14/15 | Respectivement **5 + 4**, puis **1 + 3** cas Surefire/Failsafe ; preuves de claim, rejet et journal corrélé, sans reprise métier |
| Graphe INT-18/19/20 | **2 → 3 → 6** cas ciblés successifs, non cumulatifs ; pas un parcours partenaire raccordé |

Les [cours](notes/README.md) conservent le contexte, les commandes historiques et les limites utiles. Rapports générés : `application/target/surefire-reports/`, `application/target/failsafe-reports/` et `capability-api/target/surefire-reports/`.

## Démarrage local — exemple manuel non exécuté pour cette documentation

Depuis la racine, avec Docker et le port/nom libres :

```sh
docker run -d --name conflux-lab-mongo -p 127.0.0.1:27017:27017 -v conflux-lab-mongo-data:/data/db mongo:8.0.16 --wiredTigerCacheSizeGB 0.25
./mvnw -B -ntp -DskipTests package
java -jar application/target/application-0.1.0-SNAPSHOT.jar --server.port=0 --server.address=127.0.0.1
```

Le conteneur et le volume nommé sont créés localement ; la base n'est exposée que sur loopback. Sans authentification dans cet exemple, ne jamais l'exposer sur Internet. Le volume conserve les données à l'arrêt de l'application. `0.25` règle le cache WiredTiger, pas la mémoire totale du conteneur. Un conteneur déjà créé se relance avec `docker start conflux-lab-mongo`.

`-DskipTests package` construit sans exécuter les tests et ne prouve pas une validation. Tomcat affiche le port libre choisi par `server.port=0`. Utiliser le payload fictif de [l'exemple JSON](contracts/src/test/resources/contracts/create-family.json), puis le lien du reçu pour le GET. Arrêter l'application par `Ctrl+C`, et éventuellement la base par `docker stop conflux-lab-mongo`, sans supprimer le volume pour un essai de redémarrage.

Les notes REST décrivent la mémoire historique, remplacée par MongoDB. L'acquittement `majority` avec journal sur le standalone ne fournit ni réplication ni sauvegarde. La réorganisation documentaire et l'exemple de lancement ne valent pas nouveau feu vert de développement, commit, push ou déploiement. **La CD reste théorique.**
