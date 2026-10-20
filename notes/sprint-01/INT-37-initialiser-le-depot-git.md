# INT-37 — Initialiser le dépôt Git

**Sprint :** [01 — Socle](../../docs/stages/sprint-01-socle.md), tâche d'initialisation ajoutée sur demande explicite.  
**Statut :** initialisation locale et premier push réalisés.  
**Autorisation :** demande utilisateur de versionner le projet dans `https://github.com/libileh91/conflux-lab`. Cette tâche ne vaut pas feu vert pour INT-01 à INT-04 ni pour un développement applicatif.

## Objectif

Versionner l'état existant du laboratoire, relier le dossier local au dépôt GitHub et publier une première branche `main`, sans écraser d'historique ni modifier la configuration Git globale.

## Réalisation

- Vérification de l'absence initiale de dépôt Git local et de l'identité Git déjà configurée, sans la modifier.
- Vérification du dépôt distant par `git ls-remote` : accès en lecture réussi, sans référence distante retournée avant initialisation.
- Création de `.gitignore` pour les sorties Maven, métadonnées IDE, environnements locaux, formats de clés usuels et logs. Le Maven Wrapper n'est pas exclu.
- Ajout de `.gitkeep` dans les modules encore vides et dans les dossiers de notes des sprints 03 à 09 : Git ne suit pas les répertoires vides. Les dossiers et liens documentaires doivent rester présents après un clone.
- Initialisation sur `main` et ajout d'`origin` vers `https://github.com/libileh91/conflux-lab.git`.
- Création du commit initial **`067e6a4`**, message `chore: initialize ConfluxLab repository`, portant 36 fichiers.
- Push HTTPS réussi et suivi de `origin/main` configuré pour la branche locale `main`.

## Fichiers concernés

- [`.gitignore`](../../.gitignore).
- [`capability-api/.gitkeep`](../../capability-api/.gitkeep) et [`contracts/.gitkeep`](../../contracts/.gitkeep).
- `notes/sprint-03/.gitkeep` à `notes/sprint-09/.gitkeep`.
- Tous les fichiers déjà présents de documentation, skill, notes et ébauche Experience sont inclus dans le commit initial, sans modification de leurs sources applicatives pour cette tâche.
- La présente note et ses liens dans les index/fiches font l'objet d'un commit documentaire après le premier push.

## Vérifications et résultats observés

```sh
git init -b main
git remote add origin https://github.com/libileh91/conflux-lab.git
git remote -v
git add -- .gitignore .agents docs experience-api notes capability-api/.gitkeep contracts/.gitkeep
git --no-pager diff --cached --stat
GIT_EDITOR=true GIT_TERMINAL_PROMPT=0 git commit -m "chore: initialize ConfluxLab repository"
GIT_TERMINAL_PROMPT=0 GCM_INTERACTIVE=never git push -u origin main
```

Le push a retourné :

```text
To https://github.com/libileh91/conflux-lab.git
 * [new branch]      main -> main
branch 'main' set up to track 'origin/main'.
```

Avant publication, un inventaire des fichiers et un contrôle ciblé de motifs de clés privées, tokens GitHub et clés AWS ont été effectués : aucun motif détecté par ce contrôle. Cela ne constitue pas un audit de sécurité exhaustif.

## Authentification : diagnostic utile

- `gh repo view` a échoué avec `HTTP 401: Bad credentials`, y compris sans les variables de tokens GitHub dans l'environnement du processus de diagnostic.
- Le test Git SSH en mode non interactif a échoué avec `Permission denied (publickey)`.
- **Le push Git HTTPS a pourtant réussi avec la configuration Git existante.** Une panne d'authentification de `gh` ou de SSH ne prouve donc pas un blocage de Git HTTPS.
- Aucune clé ni aucun token n'a été demandé, affiché ou écrit dans le projet. Aucun changement de credential helper, de configuration globale, de permissions du dépôt ou de visibilité GitHub n'a été effectué.

## Limites et suite

Aucun push forcé, rebase, déploiement ni workflow CI. Aucun build ou test applicatif n'a été lancé : le socle reste incomplet, comme décrit dans les notes rétrospectives. Le succès du push ne prouve pas que l'application compile ou fonctionne.

INT-37 est une tâche d'outillage ajoutée après le découpage initial, sans renuméroter INT-01 à INT-36. Le sprint 01 n'est pas terminé ; les autres tâches conservent leur feu vert individuel.
