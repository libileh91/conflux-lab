# INT-01 — Inspecter et fixer les frontières

**Sprint :** [01 — Socle](../../docs/stages/sprint-01-socle.md).  
**Type :** note rétrospective des travaux antérieurs, rédigée à la demande de l'utilisateur.  
**Statut :** partiel — socle amorcé, architecture à réaligner, build non exécuté. Cette note ne clôt pas INT-01 et ne constitue pas un feu vert pour continuer.

## Objectif et contexte

L'amorce initiale visait deux applications Experience et Capability avec des contrats communs. L'utilisateur a ensuite précisé le choix d'un **monolithe modulaire** : Experience et Capability restent séparées mais sont assemblées dans une seule application Spring Boot et communiquent en Java. Le code présent n'a pas encore été adapté à cette décision.

Le rattachement à INT-01 sert à tracer l'état de départ du socle. Les travaux manquants de build, d'assemblage et de vérification relèvent aussi d'INT-02, INT-03 et INT-04 ; leur présence dans cette note ne les valide pas.

## Réalisation présente dans les fichiers

| Élément                                                                                                               | Constat par inspection                                                                                                                            |
| --------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| [experience-api/pom.xml](../../experience-api/pom.xml)                                                                | POM enfant `dev.confluxlab:conflux-lab:0.1.0-SNAPSHOT`, dépendance vers `contracts`, starters web/validation/test, cible Java 21 et plugins Maven |
| [ExperienceApplication.java](../../experience-api/src/main/java/dev/confluxlab/experience/ExperienceApplication.java) | Point de démarrage Boot dans le package Experience et activation de `CapabilityProperties`                                                        |
| [application.yml](../../experience-api/src/main/resources/application.yml)                                            | Port Experience 8080 et URL Capability proposée sur 8081 ; cette séparation réseau n'est plus la cible retenue                                    |
| `capability-api/`                                                                                                     | Arborescence de sources/tests créée, sans code ni POM dans l'inventaire effectué                                                                  |
| `contracts/`                                                                                                          | Arborescence créée, sans DTO ni POM dans l'inventaire effectué                                                                                    |
| Racine du projet                                                                                                      | Aucun POM parent ni Maven Wrapper présent dans l'inventaire effectué                                                                              |

Le POM Experience contient notamment le plugin Boot `3.5.7`, le compilateur `3.14.1` et Surefire `3.5.4`. Ce sont des versions écrites dans l'ébauche, **pas la preuve d'un ensemble de dépendances résolu ou validé**. Le parent attendu est absent et les classes importées depuis `contracts` ne sont pas présentes.

## Explication et décision d'architecture

Un POM enfant ne suffit pas à constituer un build multi-module exécutable : le parent, les dépendances internes et l'assemblage doivent exister. Un dossier de module vide n'est pas un composant métier livré.

**Cible retenue, non encore implémentée :**

- Un module d'assemblage pour le seul démarrage et le seul JAR Boot.
- Experience/BFF mince pour l'HTTP, Capability pour le métier et sa persistance.
- Dépendance Experience vers l'API publique Java de Capability, jamais l'inverse ; pas d'accès direct à ses classes internes.
- Suppression ou adaptation de l'ancien chemin HTTP interne uniquement dans une future tâche autorisée.

La mise en cohérence ne consiste pas à supprimer arbitrairement l'ébauche : les contrats REST et la gestion des erreurs peuvent fournir une base à revoir.

## Vérifications réellement effectuées

Pendant la préparation de cette note : lecture des sources/POM et inventaire des dossiers avec les outils de fichiers. La commande d'environnement suivante a aussi été exécutée :

```sh
java -version; command -v mvn; command -v docker; git --no-optional-locks status --short
```

Résultats observés :

- Java : OpenJDK Temurin `21.0.12.1` disponible.
- `mvn` : aucun exécutable trouvé dans le `PATH` par `command -v mvn`.
- Docker : exécutable `/usr/bin/docker` trouvé ; disponibilité du daemon et Testcontainers **non vérifiée**.
- Git : « not a git repository » ; la commande composée termine avec le code 128 de la commande Git.

**Non exécutés :** build Maven, compilation Java, tests unitaires/intégration, démarrage de l'application et CI. Aucun résultat de ces validations n'est revendiqué. Aucun besoin de télécharger Maven ou de modifier le code pour rédiger ce constat.

## Reste à faire — sur feu vert, pas dans cette note

Terminer le cadrage des frontières et prérequis d'INT-01, puis traiter séparément le parent/Wrapper, l'assemblage unique, le remplacement de l'HTTP interne et les tests prévus dans la fiche. La note décrit l'existant ; elle ne valide pas les critères du sprint.

## Point à retenir

**Plusieurs modules ne signifient pas plusieurs services.** Le monolithe modulaire exige à la fois une exécution commune et des frontières de code explicites ; aucune des deux ne se déduit d'un simple nom de dossier.
