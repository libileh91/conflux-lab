# Sprint 01 — Socle du monolithe modulaire

**Statut : en cours.** INT-01, INT-02, INT-03 et INT-37 sont terminées dans leur périmètre ; les contrats d'INT-05 ont levé le blocage du build. INT-04 attend son feu vert. Le sprint n'est pas clôturé.

## Objectif

Pouvoir construire une seule application Java, avec deux parties bien séparées : le BFF reçoit les requêtes et Capability porte le métier. Ce sprint ne crée pas encore de famille.

## Repères

- Un **module** est une partie du code avec une responsabilité précise ; plusieurs modules peuvent fonctionner dans une seule application.
- Le **Maven Wrapper** (`mvnw`) utilise la version de Maven prévue par le projet, même sans Maven préinstallé.
- Un **JAR** est une archive Java ; seul le futur module `application` produira le JAR Spring Boot exécutable.

**Dépendance résolue :** INT-05 a défini les types Java manquants et le `clean verify` global réussit avec 59 cas de test. INT-02 est donc clôturée pour le build. INT-03 a ensuite réalisé l'assemblage sur feu vert distinct ; les tests de frontières INT-04 restent à autoriser.

## Stories — un feu vert par tâche

### INT-01 — Inspecter et fixer les frontières
**Statut : terminée pour le cadrage.** [Note INT-01](../../notes/sprint-01/INT-01-inspecter-et-fixer-les-frontieres.md).

En tant que développeur débutant, je veux savoir où placer chaque responsabilité, afin de ne pas mélanger le traitement HTTP, le métier et le stockage.

**À faire :**
- Examiner l'existant et vérifier Java, Maven, le réseau et Docker.
- Décrire les rôles des modules et leurs dépendances autorisées dans le DAT.

**Terminé quand :**
- [x] Les rôles du BFF, de Capability et de `contracts` ainsi que le graphe cible sont documentés.
- [x] Les prérequis disponibles et les limites constatées figurent dans la note ; aucun fonctionnement métier n'est prétendu.

### INT-02 — Rendre le build reproductible
**Statut : terminée pour le socle et la compilation.** Déblocage constaté pendant les vérifications d'INT-05, pas un assemblage applicatif. [Note INT-02](../../notes/sprint-01/INT-02-rendre-le-build-reproductible.md).

En tant que développeur, je veux construire le projet avec une commande documentée, afin d'utiliser les mêmes versions d'outils et de détecter les erreurs de compilation.

**À faire :**
- Configurer le parent Maven, les modules, Java 21 et le Wrapper avec une version épinglée.
- Documenter la commande globale et son résultat, sans exclure les sources qui échouent.

**Terminé quand :**
- [x] Le Wrapper démarre depuis un cache de distribution neuf et les versions sont documentées.
- [x] `./mvnw -B -ntp clean verify` réussit pour tous les modules après INT-05 : 59 cas de test, aucun échec/erreur/ignoré.

### INT-03 — Assembler les modules
**Statut : terminée sur feu vert explicite.** Prérequis INT-02 et INT-05 remplis. [Tutoriel et preuves INT-03](../../notes/sprint-01/INT-03-assembler-les-modules.md).

En tant que développeur, je veux démarrer les modules ensemble, afin d'obtenir un monolithe plutôt que deux applications séparées.

**À faire :**
- Créer `application`, y placer le seul démarrage Boot et y assembler les bibliothèques.
- Remplacer l'appel HTTP BFF → Capability par un appel à son interface publique Java.

**Terminé quand :**
- [x] Un seul JAR Boot démarre, sans MongoDB ni partenaire requis à ce stade ; les trois autres JAR sont des bibliothèques ordinaires.
- [x] Deux tests vérifient la délégation Java à l'interface publique ; trois tests d'assemblage/disponibilité passent, sans client HTTP interne ni second démarrage autonome.

**Preuves :** `./mvnw -B -ntp clean verify` réussi (64 tests au total), inspection des archives et démarrage borné du JAR. Un POST valide et un GET avec UUID valide répondent réellement `503` sans `Location` ni `requestId` : aucune acceptation ou consultation métier n'est prétendue.

### INT-04 — Vérifier les frontières
**Statut : à faire — feu vert requis.** Prérequis : INT-03 terminée.

En tant que développeur, je veux détecter automatiquement les mauvaises dépendances, afin de conserver la séparation des modules lorsque le code évolue.

**À faire :**
- Vérifier que le contexte Spring assemble correctement les modules.
- Ajouter le contrôle de dépendances le plus simple adapté au projet, sans imposer Spring Modulith.

**Terminé quand :**
- [ ] Les tests refusent les cycles, une dépendance Capability → BFF et les accès du BFF aux classes internes ou repositories de Capability.
- [ ] Ces tests et le test de contexte passent dans `./mvnw -B -ntp verify`.

### INT-37 — Initialiser le dépôt Git
**Statut : terminée — ajoutée sur demande explicite.** [Note INT-37](../../notes/sprint-01/INT-37-initialiser-le-depot-git.md).

En tant que développeur, je veux versionner le projet sur GitHub, afin de conserver son historique et de partager une version identifiable.

**À faire :**
- Initialiser Git, ignorer les fichiers locaux/générés et conserver les dossiers encore vides.
- Configurer le dépôt distant, créer le premier commit et le publier sans écraser d'historique.

**Terminé quand :**
- [x] Le dépôt Git local contient les fichiers prévus et un `.gitignore` adapté.
- [x] Le premier push de `main` vers `libileh91/conflux-lab` est confirmé dans la note.

## Limites et hors périmètre

INT-03 a réalisé le graphe Maven et l'assemblage Spring ; le démarrage et l'indisponibilité explicite sont vérifiés, pas le parcours métier. Les contrôles automatiques des frontières restent dans INT-04. Les preuves JDTLS du dépannage précédent sont conservées dans la note INT-02.

Pas de parcours métier complet, stockage, événement, appel partenaire, CI ou déploiement. Les types ajoutés dans INT-05 sont de vrais contrats testés, pas des classes vides de dépannage.

## Démo et clôture

- [x] Depuis la racine, montrer un build global réussi, puis le démarrage du seul JAR Boot et un appel Java entre modules : preuves INT-03, avec double explicite pour le succès de délégation.
- [ ] Expliquer pourquoi plusieurs modules ne signifient pas plusieurs applications ; le BFF ne porte ni métier ni persistance.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) lors de la clôture avec les résultats réellement observés. La validation d'INT-03 ne clôt pas le sprint : INT-04 et l'explication par l'apprenant restent distinctes.
