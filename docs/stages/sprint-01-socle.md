# Sprint 01 — Socle du monolithe modulaire

**Statut : à ouvrir.** Prérequis : accord sur cette fiche avant de reprendre le code.

## Objectif

Obtenir un build Java 21 reproductible et une seule application Spring Boot assemblant deux modules distincts : Experience/BFF et Capability. Ce sprint ne livre pas encore le parcours de création d'une famille.

## Incréments, un par échange

- [ ] **INT-01 — Inspecter et fixer les frontières.** Relire le squelette existant et vérifier JDK, accès réseau et disponibilité de Maven/Docker. Écrire le graphe des dépendances. Conserver les noms `experience-api` et `capability-api` si cela évite des renommages inutiles ; préciser le rôle de `contracts` avant de le remplir.
- [ ] **INT-02 — Rendre le build reproductible.** Créer le POM racine et le Maven Wrapper avec des versions stables compatibles. Les bibliothèques ne produisent pas de JAR Boot exécutable ; seul le module d'assemblage `application` le fait.
- [ ] **INT-03 — Assembler les modules.** Placer le démarrage Boot dans `application`. Exposer une interface publique minimale de Capability, appelée en Java depuis Experience. Remplacer le client HTTP interne et sa configuration après revue, sans perdre les éléments réutilisables.
- [ ] **INT-04 — Vérifier les frontières.** Ajouter un test de contexte et un contrôle automatisé des dépendances : pas de cycle, pas d'import d'internals Capability depuis Experience. Choisir le mécanisme le plus simple adapté au découpage ; ne pas imposer Spring Modulith.

## Critères de fin et tests

- [ ] `./mvnw -B -ntp verify` réussit depuis la racine avec Java 21.
- [ ] Un seul JAR Boot est produit et son démarrage est vérifié par un test borné ou une démonstration locale documentée.
- [ ] Experience dépend de l'API publique de Capability ; Capability ne dépend pas d'Experience.
- [ ] Aucun client HTTP interne, deuxième serveur ou deuxième point de démarrage autonome ne reste nécessaire.
- [ ] Le démarrage ne demande ni MongoDB ni partenaire à cette étape.

## Hors périmètre

Endpoints métier complets, persistance, événements, Feign, résilience, workflow CI et déploiement. Ne pas réécrire tout le code existant sous prétexte de créer le socle.

## Explication à maîtriser

Pourquoi plusieurs modules Maven ne signifient-ils pas plusieurs applications ? Où est la frontière qui empêche le BFF d'accéder au stockage de Capability ?

## Notes existantes

- [INT-01 — Inspecter et fixer les frontières](../../notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md) : constat rétrospectif du socle amorcé ; incomplet, non validé. Cette note ne clôt ni la tâche ni le sprint et ne donne pas de feu vert pour continuer.

## Preuves de clôture

À compléter avec le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint), les commandes et les résultats observés. Aucun test n'est déclaré exécuté par cette fiche.
