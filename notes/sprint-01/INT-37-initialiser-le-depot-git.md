# INT-37 — Initialiser le dépôt Git

| Contexte | Description |
|---|---|
| Sprint | [01 — Socle du monolithe modulaire](../../docs/stages/sprint-01-socle.md), tâche ajoutée sur demande explicite |
| Statut | **Terminée pour l'initialisation Git locale et le premier push** ; aucun succès applicatif impliqué |
| Portée | Versionner l'existant et publier `main` vers `https://github.com/libileh91/conflux-lab`, sans écraser d'historique ni modifier la configuration Git globale |
| Sources des preuves | Preuves de l'initialisation et des deux premiers push, note historique INT-37, `.gitignore` et neuf `.gitkeep` relus ; pas de nouvelle interrogation distante |
| Réécriture actuelle | Documentation : inventaire Git local en lecture et contrôles documentaires ; aucune authentification réseau, compilation ou test applicatif, aucun nouveau commit ni push |

L'autorisation historique portait sur le versionnement du projet, pas sur INT-01 à INT-04 ou un développement applicatif. INT-37 n'achève pas à elle seule le sprint 01.

## 1. Objectif et vocabulaire

Le but était de transformer un dossier de travail en un historique partageable, puis d'en publier une première version sur GitHub. **Git** gère les versions ; **GitHub** héberge ici le dépôt distant. Utiliser Git localement ne nécessite pas d'être connecté à GitHub.

Le modèle mental comporte trois zones, auxquelles s'ajoute le dépôt distant :

1. Le **répertoire de travail** contient les fichiers que l'on édite ; ils peuvent être nouveaux, modifiés ou supprimés.
2. L'**index**, aussi appelé staging area, contient la version préparée pour le prochain commit. `git add` y copie un état des fichiers ; une modification ultérieure n'y est pas ajoutée automatiquement.
3. Un **commit** enregistre localement un instantané de l'index, son parent éventuel, un message et des métadonnées. Il ne publie rien à lui seul.
4. Un **push** transmet des objets manquants et demande la mise à jour d'une branche distante. Il ne transmet pas les modifications non commitées du dossier.

Une **branche** est une référence mobile vers un commit ; `main` est le nom choisi ici. `HEAD` désigne habituellement la branche courante. `067e6a4` et `7cfee84` sont des identifiants abrégés de commits historiques, pas des noms de branche.

## 2. Comprendre le problème

Avant Git, un fichier modifié remplace simplement son état précédent dans le dossier. L'historique permet de retrouver des versions identifiables et de comprendre leur intention. Le distant permet ensuite de partager **les commits publiés**, pas de sauvegarder magiquement tous les fichiers locaux.

Il faut choisir ce que l'on conserve : sources, documentation et outils reproductibles ont leur place ; sorties de compilation, réglages privés et secrets n'y ont pas leur place. Le premier push ne doit pas non plus écraser un historique distant inconnu. La note historique rapporte donc un contrôle du distant avant publication, puis un push ordinaire, jamais forcé.

## 3. Travail réalisé pas à pas

### 3.1. Vérifier le point de départ sans modifier l'identité

La note initiale rapporte l'absence de dépôt Git local, la vérification de l'identité Git déjà configurée et une lecture du distant par `git ls-remote`. Cette lecture avait réussi sans retourner de référence distante avant initialisation. Elle ne constituait pas encore une preuve du droit d'écriture ; c'est le push réussi qui a démontré la publication à cet instant.

L'identité auteur d'un commit et l'identité utilisée pour s'authentifier auprès de GitHub sont deux choses différentes. Aucun nom, courriel, token ou chemin de clé n'est ajouté à ce cours ; aucune configuration globale n'est modifiée par cette réécriture.

### 3.2. Écarter les fichiers générés et locaux avec `.gitignore`

Extrait partiel exact de [`.gitignore`](../../.gitignore), commenté après le bloc :

```gitignore
# Maven build outputs
target/
*.class

# Local IDE metadata
.idea/
*.iml
.vscode/
.project
.classpath
.settings/
.factorypath
```

`target/` vise les sorties Maven, y compris celles des modules ; `*.class` vise le bytecode compilé. Les autres règles évitent de partager des métadonnées locales d'IDE. Ignorer ces fichiers réduit le bruit : on versionne ce qui permet de reconstruire, pas toutes les productions de la machine. Le Maven Wrapper n'est pas exclu par ces règles.

Deuxième extrait partiel exact du même fichier :

```gitignore
# Local environment and credentials
.env
.env.*
!.env.example
!.env.sample
*.pem
*.key
*.p12
*.pfx
*.jks
```

Les règles commençant par `!` réautorisent ici les exemples d'environnement après la règle générale `.env.*`. Ces exemples doivent eux-mêmes rester sans secret. Les suffixes de clés usuels sont écartés ; la suite du fichier ignore aussi logs et fichiers du système d'exploitation. **Une règle d'exclusion n'est pas un détecteur universel de secret.**

Surtout, `.gitignore` concerne les fichiers non suivis : ajouter une règle ne retire pas un fichier déjà suivi de l'index ou des commits passés. S'il était déjà suivi, une décision explicite de retrait du suivi serait distincte de sa suppression locale. Et si un secret avait été publié, l'ignorer ensuite ne le révoquerait pas et n'effacerait pas l'historique. Aucun incident de ce type n'est affirmé ici.

### 3.3. Conserver les répertoires encore vides avec `.gitkeep`

Git suit des fichiers, pas des répertoires vides. Les modules et dossiers de notes préparés devaient donc contenir un fichier pour exister après un clone. `.gitkeep` est une **convention de nommage**, sans traitement spécial par Git, Maven ou Spring ; il ne crée aucune classe ni configuration.

Contenu exact commun à [`capability-api/.gitkeep`](../../capability-api/.gitkeep) et [`contracts/.gitkeep`](../../contracts/.gitkeep) :

```text
# Preserve the planned module directory in Git.
```

Contenu exact commun aux fichiers `notes/sprint-03/.gitkeep` à `notes/sprint-09/.gitkeep` :

```text
# Preserve this sprint notes directory in Git.
```

Il y a donc neuf marqueurs relus. Le texte commençant par `#` n'est ici qu'un contenu explicatif. Ces fichiers préservent l'arborescence documentaire et les modules prévus ; leur présence ne prouve aucun fonctionnement applicatif.

### 3.4. Initialiser `main`, nommer le distant et préparer l'index

Les commandes historiques ont initialisé le dépôt local avec `main`, puis associé le nom **`origin`** à l'URL HTTPS du projet. `origin` est un alias local conventionnel, pas un serveur spécial imposé par Git. `git remote add` enregistre l'adresse ; cette opération ne télécharge ni ne publie les fichiers.

L'ajout ciblé a préparé les fichiers choisis dans l'index, puis `diff --cached --stat` a montré l'étendue de cet ajout avant commit. Ce contrôle porte sur l'index, pas sur une future compilation. La sélection historique reflète les fichiers de l'époque ; ce n'est pas une recette à recopier pour une publication actuelle.

### 3.5. Créer le commit puis publier sans forcer

La note historique rapporte le commit initial **`067e6a4`**, message `chore: initialize ConfluxLab repository`, portant **36 fichiers**. Il comprenait documentation, skill, notes, marqueurs et ébauche Experience, sans modification applicative pour cette tâche.

Le premier push HTTPS de `main` a réussi. `-u` a associé la branche locale à **`origin/main`**, son upstream : la branche distante suivie par défaut. `origin/main` est une référence locale représentant le dernier état distant connu, pas une interrogation permanente de GitHub. Ici « upstream » désigne cette relation de suivi ; aucun second remote nommé `upstream` n'est impliqué.

Le deuxième commit historique, **`7cfee84`**, porte le message `docs: record INT-37 Git initialization` : la note de la tâche et ses liens ont été enregistrés après le premier push, puis publiés. Ces identifiants et résultats avaient été observés lors de l'initialisation ; ils n'ont pas été réinterrogés pendant la réécriture et ne décrivent pas les modifications non commitées actuelles.

Un push ordinaire protège contre la mise à jour non fast-forward d'une branche : si le distant a divergé, Git peut refuser plutôt que remplacer son historique. Cela ne garantit pas que les fichiers soient corrects ou exempts de secrets. Aucun `--force`, rebase, déploiement ou workflow CI n'a été réalisé dans INT-37.

### 3.6. Distinguer `gh`, SSH et Git HTTPS

Les diagnostics historiques ne portaient pas tous sur le même canal :

| Canal | Observation historique | Ce qu'elle établit |
|---|---|---|
| GitHub CLI `gh` | `gh repo view` échouait avec `HTTP 401: Bad credentials`, y compris lors du diagnostic sans variables de tokens GitHub | L'authentification de cette commande CLI était défaillante |
| Git par SSH | Essai non interactif refusé : `Permission denied (publickey)` | L'authentification SSH essayée n'aboutissait pas |
| Git par HTTPS | Push réussi avec la configuration Git existante | Cette voie avait effectivement permis la publication |

`gh` utilise une authentification pour ses opérations GitHub ; SSH s'appuie sur une clé reconnue et sa configuration ; Git HTTPS utilise ses propres mécanismes d'identifiants, éventuellement un credential helper. Ils peuvent être reliés par configuration, mais ce n'est pas automatique. Le succès HTTPS ne permet pas d'identifier précisément le mécanisme de stockage utilisé. **La panne de `gh` ou de SSH n'était donc pas bloquante pour ce push HTTPS.** Aucun secret n'a été demandé, affiché ou écrit dans le projet.

## 4. Commandes expliquées

**Racine de référence :** dossier `conflux-lab/`. Les blocs ci-dessous documentent la séquence historique ; **aucune de ces commandes d'initialisation ou publication n'est relancée pour cette réécriture**. L'inventaire actuel a utilisé uniquement `git --no-optional-locks status --short` et `git --no-pager diff --stat`, en lecture locale. Les variantes historiques de lecture avec `--no-pager` explicitent l'absence de pager ; elles ne constituent pas une nouvelle interrogation du distant.

### 4.1. Préparer un dépôt initialement absent

```sh
git --no-pager ls-remote https://github.com/libileh91/conflux-lab.git
git init -b main
git remote add origin https://github.com/libileh91/conflux-lab.git
git --no-pager remote -v
```

La première commande est une **lecture réseau**, avec accès distant requis ; elle avait retourné zéro référence dans le contrôle historique. `init -b main` crée les métadonnées locales et choisit la branche initiale. `remote add` exige que le nom ne soit pas déjà utilisé ; `remote -v` affiche les URL configurées sans tester leur accessibilité. Ces préconditions appartiennent à l'initialisation : réexécuter ce bloc dans le dépôt actuel n'est pas demandé.

### 4.2. Préparer puis enregistrer un instantané local

```sh
git add -- .gitignore .agents docs experience-api notes capability-api/.gitkeep contracts/.gitkeep
git --no-pager diff --cached --stat
GIT_EDITOR=true GIT_TERMINAL_PROMPT=0 git commit -m "chore: initialize ConfluxLab repository"
```

`add` prépare aussi les modifications et suppressions sous les chemins sélectionnés ; `--` sépare les options des chemins. `diff --cached` compare l'index au commit courant, ou au contenu vide avant le premier commit ; `--stat` résume les fichiers/lignes sans montrer tout le contenu. Ce résumé ne remplace pas une revue du diff complet. `commit -m` crée ensuite le commit **local** à partir de l'index ; il exige une identité auteur configurée et ne lance ni Maven ni un push par nature.

`GIT_EDITOR=true` neutralise l'ouverture d'un éditeur ; `-m` fournit déjà le message. `GIT_TERMINAL_PROMPT=0` désactive les demandes interactives d'identifiants Git, sans fournir d'identifiants. Le résultat rapporté pour cette création est `067e6a4`, 36 fichiers ; aucun nouveau commit n'a été créé ici.

### 4.3. Publier et configurer le suivi

```sh
GIT_TERMINAL_PROMPT=0 GCM_INTERACTIVE=never git push -u origin main
```

Cette commande suppose le commit local, l'adresse `origin`, le réseau et les droits d'écriture disponibles. Elle publie `main` sans option de force ; `-u` configure son upstream après succès. `GCM_INTERACTIVE=never` demande au Git Credential Manager, s'il est utilisé, de ne pas ouvrir d'interaction ; cela ne prouve pas qu'il était le mécanisme d'authentification effectif. Sortie historique conservée, **non reproduite aujourd'hui** :

```text
To https://github.com/libileh91/conflux-lab.git
 * [new branch]      main -> main
branch 'main' set up to track 'origin/main'.
```

## 5. Vérifications et limites

La note initiale rapporte un inventaire avant publication et une recherche ciblée de motifs de clés privées, tokens GitHub et clés AWS : aucun motif détecté **par ce contrôle historique**, sans audit exhaustif. Les fichiers d'exclusion et marqueurs ont été relus pour ce cours ; aucun contrôle réseau ou scan de secrets nouveau n'a été exécuté.

Les preuves historiques sont le commit initial `067e6a4`, son push confirmé et le repère suivant `7cfee84`. Un état « propre » observé à une clôture historique signifie seulement qu'à cet instant les zones de travail/index ne présentaient pas de modifications signalées par Git ; il ne prouve ni absence de fichiers ignorés ni compilation. **Ce n'est pas l'état présent : des modifications non commitées existent pendant cette reprise documentaire.** L'inventaire local de début de réécriture a bien constaté ces modifications avec `status`/`diff`, sans réinterroger les commits ou la branche distante.

Ni commit ni push ne prouvent que l'application compile ou fonctionne. Aucun build, test applicatif ou serveur n'a été lancé pour INT-37 ; les blocages du socle restent décrits dans la [note INT-02](INT-02-rendre-le-build-reproductible.md). Aucun helper, réglage Git global, droit du dépôt ou visibilité GitHub n'a été modifié pour cette tâche. Cette réécriture ne réauthentifie aucun outil et ne publie rien.

## 6. Suivi centralisé

- [TD-B008 — Réauthentification gh/SSH optionnelle, non bloquante pour Git HTTPS](../todos.md#td-b008)
- [TD-R006 — Git : commit, push et authentifications distinctes](../todos.md#td-r006)
