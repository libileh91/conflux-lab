# Sprint 01 — Socle du monolithe modulaire

> **Portée historique :** ce document décrit les étapes indiquées dans son titre, pas une nouvelle validation du parcours actuel. Le raccordement ultérieur via Feign et la finalisation sont réalisés dans [INT-21 + INT-22](../../notes/sprint-06/INT-21-creer-une-famille-de-bout-en-bout.md).

**Deux résultats réalisés, regroupés par sujet :** architecture/assemblage/frontières (INT-01/03/04) et build/versionnement (INT-02/37). Le sprint est un repère de classement, **pas l'obligation d'une tâche ou d'une note unique**. Les IDs et leurs preuves restent inchangés.

**Clôture technique et documentaire demandée le 2026-10-21 ; restitution orale non évaluée.** La réorganisation documentaire du 2026-10-22 ne lance aucun code, test, build, démarrage, commit ou push. Les critères cochés décrivent les vérifications historiques, pas une nouvelle validation des sources actuelles.

## INT-01 + INT-03 + INT-04 — Architecture, assemblage et frontières

**Statut : réalisé pour le cadrage, l'assemblage et ses garde-fous.** [Cours — Architecture, assemblage et frontières](../../notes/sprint-01/INT-01-socle-modulaire.md).

**Story :** en tant que développeur, je veux démarrer un monolithe aux responsabilités séparées, afin de faire évoluer l'HTTP et le métier sans créer plusieurs applications ni coupler le BFF au stockage.

**Périmètre livré :** trois bibliothèques et un seul assemblage Boot ; BFF mince, API Java publique de Capability, contrats indépendants et absence d'HTTP interne. Les contrôles de frontières complètent le test de contexte, sans modifier le métier.

**Critères techniques vérifiés historiquement :**
- [x] Rôles et dépendances autorisées documentés ; un seul JAR Boot construit/démarré et trois bibliothèques ordinaires inspectées.
- [x] Délégation Java et assemblage testés, sans faire passer les réponses d'un double pour une acceptation réelle.
- [x] Cinq règles protègent cycles intermodules, sens Capability/BFF, API publique, indépendance des contrats et absence de retour vers l'assemblage.
- [x] Les mêmes règles rejettent les exemples fautifs et acceptent le graphe autorisé ; contexte réutilisé, outil/fixtures absents du JAR livré.

**Dépendance historique :** le socle Maven et les contrats d'INT-05 devaient être disponibles avant l'assemblage. INT-05 reste rattachée au sprint 02. Son déblocage ne prouvait pas encore le démarrage Boot.

## INT-02 + INT-37 — Build reproductible et versionnement

**Statut : réalisé pour le socle de build et l'initialisation/publication Git.** [Cours — Build reproductible et versionnement](../../notes/sprint-01/INT-02-build-et-versionnement.md).

**Story :** en tant que développeur, je veux construire le projet avec des outils explicites et conserver des versions identifiables, afin de partager un état du code sans dépendre d'un Maven choisi au hasard.

**Périmètre livré :** parent/agrégateur Maven, Java 21, Wrapper épinglé avec empreinte, réglages réseau/fins de ligne et commandes documentées ; exclusions Git, initialisation de `main` et premiers push HTTPS sans force ni changement de configuration globale.

**Critères techniques vérifiés historiquement :**
- [x] Wrapper démarré sous Linux depuis un cache de distribution neuf ; versions et limites documentées.
- [x] Après INT-05, `clean verify` global réussi sans exclure les sources en échec : 59 cas de contrats ; blocage INT-02 levé.
- [x] Dépôt initialisé, fichiers prévus versionnés et premier push HTTPS de `main` confirmé dans les preuves INT-37.

« Reproductible » signifie ici outils et commandes explicités : aucun build depuis un clone vierge ni aucune identité des archives octet pour octet entre machines n'est démontré. Le versionnement ne prouve pas que l'application fonctionne. Les autorisations historiques étaient distinctes ; ce regroupement par sujet ne les modifie pas.

## Traçabilité des IDs

| ID conservé | Résultat / cours | Statut et preuve historique |
|---|---|---|
| **INT-01** | [Architecture](../../notes/sprint-01/INT-01-socle-modulaire.md) | Terminé — responsabilités, graphe cible et prérequis documentés ; aucun métier déduit du schéma |
| **INT-02** | [Build/versionnement](../../notes/sprint-01/INT-02-build-et-versionnement.md) | Terminé — Wrapper Maven 3.9.16, Java 21 ; blocage levé par INT-05, `clean verify` global : **59 cas**, 0 échec/erreur/ignoré ; pas encore d'assemblage |
| **INT-03** | [Architecture/assemblage](../../notes/sprint-01/INT-01-socle-modulaire.md) | Terminé — `clean verify` : **64 tests** = 59 contrats + 2 délégation + 3 assemblage/disponibilité ; 0 échec/erreur/ignoré. JAR inspecté/démarré sans base/partenaire à cette étape ; POST/GET valides `503` sans reçu, processus arrêté |
| **INT-04** | [Frontières](../../notes/sprint-01/INT-01-socle-modulaire.md) | Terminé — `clean verify` : **76 tests** = 64 + 5 frontières + 7 détection ; 0 échec/erreur/ignoré. Contexte revalidé, archive sans ArchUnit/fixtures ; aucun nouveau lancement HTTP du JAR |
| **INT-37** | [Build/versionnement](../../notes/sprint-01/INT-02-build-et-versionnement.md) | Terminé — `067e6a4`, 36 fichiers, premier push HTTPS de `main` ; `7cfee84`, note/liens publiés ensuite. Aucun succès applicatif ni état distant actuel garanti |

## Limites et lecture de l'état actuel

- **À la clôture, avant INT-06 :** aucune acceptation/consultation métier, persistance, publication d'événement, exécution asynchrone, partenaire ou CI n'était livrée. L'ancien `503` signifiait « opération pas encore disponible », pas une panne MongoDB ni un fallback métier.
- **Depuis les incréments suivants :** acceptation/suivi, MongoDB et prise en charge async ont été ajoutés. Le parcours actuel exige une base pour POST/GET ; les tests d'assemblage utilisent Testcontainers. Le graphe d'orchestration Java n'est pas encore raccordé au listener. Les deux cours distinguent ces évolutions des preuves du socle.
- Clone vierge, Wrapper Windows et interaction GUI Zed non vérifiés ; navigation JDTLS historique par protocole seulement. Restitution orale non évaluée : aucune maîtrise pédagogique ni clôture orale déclarée.
- Ni parcours partenaire complet, CI verte, performance ou déploiement ne sont déduits de ces résultats ; la réorganisation n'autorise aucune suite ni opération distante.

## Journal de clôture — 2026-10-21

**Bilan historique :** la clôture précède le feu vert ultérieur pour INT-06 ; les évolutions REST, MongoDB et async ne remplacent pas les preuves ci-dessous.

| Élément | Bilan consigné |
|---|---|
| Décision | Clôture technique/documentaire demandée explicitement ; commit et push du travail existant autorisés séparément du développement suivant, sans évaluation orale |
| Vérification avant publication | `./mvnw -B -ntp verify` réexécuté le 2026-10-21 : **BUILD SUCCESS, 76 tests**, 0 échec/erreur/ignoré ; avertissements Mockito/JVM non bloquants sous Java 21 |
| Démonstration | Démarrage réel et POST/GET `503` : preuve INT-03 ; contexte, frontières et archive : preuves INT-04. Aucun nouveau lancement HTTP du JAR à la clôture n'est revendiqué |
| Livrables et cours | [Architecture/assemblage/frontières — INT-01/03/04](../../notes/sprint-01/INT-01-socle-modulaire.md) ; [build/versionnement — INT-02/37](../../notes/sprint-01/INT-02-build-et-versionnement.md). Sources : [POM racine](../../pom.xml), [assemblage](../../application/pom.xml), [API Java](../../capability-api/src/main/java/dev/confluxlab/capability/api/FamilyRequests.java), [BFF](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java), [règles](../../application/src/test/java/dev/confluxlab/ModuleArchitectureRules.java) |
| Limites et suite à cet instant | Pas encore de métier accepté/consulté ni stockage/async ; Windows et GUI non validés, restitution orale non évaluée. La clôture ne donnait pas le feu vert INT-06, reçu ensuite distinctement |

## Suivi existant

- Architecture : [TD-B003 — assemblage](../../notes/todos.md#td-b003), [TD-B004 — frontières](../../notes/todos.md#td-b004), [TD-R001 — modules/application](../../notes/todos.md#td-r001), [TD-R010 — assemblage/doubles](../../notes/todos.md#td-r010), [TD-R011 — frontières/détecteurs](../../notes/todos.md#td-r011).
- Build/versionnement : [TD-B002 — build global](../../notes/todos.md#td-b002), [TD-R002 — Maven](../../notes/todos.md#td-r002), [TD-R007 — Wrapper](../../notes/todos.md#td-r007), [TD-R006 — Git](../../notes/todos.md#td-r006).
- Limites : [TD-B006 — GUI](../../notes/todos.md#td-b006), [TD-B007 — Windows](../../notes/todos.md#td-b007), [TD-B008 — gh/SSH optionnels](../../notes/todos.md#td-b008).
