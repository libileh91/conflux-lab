# Sprint 01 — Socle du monolithe modulaire

**Statut : clôturé sur décision explicite de l'utilisateur le 2026-10-21 — volets technique et documentaire.** INT-01, INT-02, INT-03, INT-04 et INT-37 sont terminées dans leur périmètre ; les contrats d'INT-05 ont levé le blocage du build. Le bilan et la synthèse de clôture figurent ci-dessous. La restitution orale de l'apprenant n'a pas été évaluée : cette clôture n'est pas une attestation de maîtrise.

## Objectif

Pouvoir construire une seule application Java, avec deux parties bien séparées : le BFF reçoit les requêtes et Capability porte le métier. Ce sprint ne crée pas encore de famille.

## Repères

- Un **module** est une partie du code avec une responsabilité précise ; plusieurs modules peuvent fonctionner dans une seule application.
- Le **Maven Wrapper** (`mvnw`) utilise la version de Maven prévue par le projet, même sans Maven préinstallé.
- Un **JAR** est une archive Java ; seul le module `application` produit le JAR Spring Boot exécutable.

**Dépendance résolue :** INT-05 a défini les types Java manquants et le `clean verify` global réussit avec 59 cas de test. INT-02 est donc clôturée pour le build. INT-03 a ensuite réalisé l'assemblage et INT-04 a vérifié les frontières, chacune sur feu vert distinct.

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
**Statut : terminée sur feu vert explicite.** Prérequis INT-03 rempli. [Tutoriel et preuves INT-04](../../notes/sprint-01/INT-04-verifier-les-frontieres.md).

En tant que développeur, je veux détecter automatiquement les mauvaises dépendances, afin de conserver la séparation des modules lorsque le code évolue.

**À faire :**
- Vérifier que le contexte Spring assemble correctement les modules.
- Ajouter le contrôle de dépendances le plus simple adapté au projet, sans imposer Spring Modulith.

**Terminé quand :**
- [x] Les tests refusent les cycles, une dépendance Capability → BFF et les accès du BFF à Capability hors de son API publique, y compris les types internes de stockage ; `contracts` et le sens vers l'assemblage sont également protégés.
- [x] Ces tests et le test de contexte existant passent dans `./mvnw -B -ntp clean verify` : 76 tests, aucun échec/erreur/ignoré.

**Preuves :** 5 tests sur le bytecode de production et 7 tests des mêmes règles (6 violations contrôlées et 1 graphe autorisé). ArchUnit et les fixtures sont limités au test et absents du JAR construit. Le comportement métier n'est pas modifié.

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

INT-03 a réalisé le graphe Maven et l'assemblage Spring ; le démarrage et l'indisponibilité explicite sont vérifiés, pas le parcours métier. INT-04 vérifie désormais les frontières sur le bytecode de production et la capacité des règles à détecter les violations. Les preuves JDTLS du dépannage précédent sont conservées dans la note INT-02.

Pas de parcours métier complet, stockage, événement, appel partenaire, CI ou déploiement. Les types ajoutés dans INT-05 sont de vrais contrats testés, pas des classes vides de dépannage.

## Démo et clôture

- [x] Depuis la racine, montrer un build global réussi, puis le démarrage du seul JAR Boot et un appel Java entre modules : preuves INT-03, avec double explicite pour le succès de délégation.
- [x] Fournir le bilan technique et la synthèse documentaire de clôture, sur demande explicite de l'utilisateur.
- [ ] Évaluer la restitution par l'apprenant : pourquoi plusieurs modules ne signifient pas plusieurs applications et pourquoi le BFF ne porte ni métier ni persistance. Cette évaluation n'a pas été réalisée et n'est pas déclarée réussie ; la clôture demandée porte sur le livrable technique et documentaire.

## Journal de clôture — 2026-10-21

**Repère historique :** le bilan ci-dessous décrit la clôture avant le feu vert ultérieur « ok go 06 ». L'[acceptation INT-06](../../notes/sprint-02/INT-06-accepter-une-demande.md) a depuis été réalisée en mémoire ; ses 96 tests ne remplacent pas les 76 tests constatés à cette clôture. Le suivi INT-07 reste distinct et à autoriser.

Ce journal applique le [format commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint). La demande « oui fait la cloture - then commit l'ensemble & push » autorise la clôture et la publication du travail existant, pas le développement de la story suivante.

| Élément | Résultat constaté |
|---|---|
| **Stories terminées** | INT-01 : cadrage ; INT-02 : socle Maven ; INT-03 : assemblage unique et appel Java ; INT-04 : frontières vérifiées ; INT-37 : initialisation Git. INT-05 a été réalisée en préparation du sprint 02 pour débloquer le build, sans être renumérotée |
| **Fichiers et notes** | [POM racine](../../pom.xml), [assemblage](../../application/pom.xml), [API publique de Capability](../../capability-api/src/main/java/dev/confluxlab/capability/api/FamilyRequests.java), [contrôleur BFF](../../experience-api/src/main/java/dev/confluxlab/experience/FamilyController.java), [règles d'architecture](../../application/src/test/java/dev/confluxlab/ModuleArchitectureRules.java) ; tutoriels INT-01/02/03/04/37 liés dans cette fiche et dans l'[index des notes](../../notes/README.md) |
| **Vérification globale** | `./mvnw -B -ntp clean verify` exécuté pendant INT-04 : **76 tests**, aucun échec, erreur ou ignoré. Détail : 59 contrats, 2 délégation, 3 assemblage/disponibilité, 5 frontières et 7 détection des règles |
| **Contrôle avant publication** | `./mvnw -B -ntp verify` réexécuté le 2026-10-21 après la demande de clôture : `BUILD SUCCESS`, 76 tests, aucun échec/erreur/ignoré ; les avertissements Mockito restent non bloquants |
| **Démonstration réelle** | INT-03 : un seul JAR Boot a démarré sans base ni partenaire ; POST valide et GET avec UUID valide ont répondu `503 application/problem+json`, sans `Location` ni `requestId` ; le processus a été arrêté. INT-04 : contexte revalidé, règles éprouvées et archive inspectée sans nouveau lancement HTTP du JAR |
| **Frontières et livrable** | Appels Java via l'API publique ; aucun client HTTP interne. Les contrôles refusent cycles et dépendances interdites, y compris l'accès aux types internes de stockage. ArchUnit et les fixtures fautives ne sont pas embarqués |
| **Point pédagogique fourni** | Synthèse ci-dessous et cours INT-03/04. Restitution de l'apprenant non évaluée, sans case de réussite cochée par anticipation |
| **Limites** | Aucune acceptation ni consultation métier réelle, aucun stockage, événement, traitement asynchrone, partenaire, CI ou déploiement. Windows et les raccourcis GUI Zed ne sont pas nouvellement validés. Les avertissements Mockito sous Java 21 n'ont pas fait échouer le build |
| **Décision de clôture** | Clôture technique et documentaire demandée explicitement par l'utilisateur le 2026-10-21 ; commit de l'ensemble et push autorisés séparément du développement |
| **Accord pour la suite** | **Aucun feu vert pour INT-06.** Le prochain incrément doit conserver son cadrage et son autorisation individuels |

### Synthèse de clôture à retenir

Le dépôt contient **plusieurs modules Maven, mais une seule application**. `application` assemble les bibliothèques et produit le seul JAR Boot exécutable ; il n'est pas un nouveau module métier. Experience reçoit l'HTTP, valide la forme et adapte les réponses. Capability porte la porte d'entrée Java du métier et possédera ses invariants, son stockage et son orchestration. `contracts` transporte les données sans dépendre de ses consommateurs.

Le test de contexte répond à « Spring sait-il relier les objets ? ». Les tests d'architecture répondent à « les dépendances sont-elles autorisées ? ». Les exemples fautifs vérifient que les règles détectent réellement un écart, et le graphe autorisé évite un détecteur qui interdirait tout.

**Piège central :** un `202` obtenu avec un double de test ne prouve pas une acceptation réelle. L'application livrée refuse encore les opérations par `503`, plutôt que d'inventer un identifiant ou de perdre silencieusement une demande. Le socle est prêt pour INT-06, mais cette story n'est pas réalisée.

**Formulation orale proposée, non évaluée :** « Dans ConfluxLab, j'ai construit un monolithe modulaire avec un seul démarrage Spring Boot. Le BFF appelle l'interface publique Java de Capability dans la même JVM, sans HTTP interne. Maven vérifie les contrats, l'assemblage et les frontières ; des exemples interdits et autorisés éprouvent les règles. Le build global passe avec 76 tests. Ce socle ne réalise pas encore l'acceptation métier : les routes répondent explicitement `503`. Ces choix décrivent le laboratoire pédagogique, pas une architecture historique AXA que je prétendrais avoir conçue seul. »

### Suivi centralisé

- [TD-B004 — Frontières réalisées et vérifiées](../../notes/todos.md#td-b004).
- [TD-B005 — Acceptation, suivi et refus réels, sur feu vert individuel](../../notes/todos.md#td-b005).
- [TD-R001 — Modules et application unique](../../notes/todos.md#td-r001).
- [TD-R010 — Assemblage et doubles de test](../../notes/todos.md#td-r010).
- [TD-R011 — Frontières et détecteurs](../../notes/todos.md#td-r011).
