# ConfluxLab — kit de cadrage Zed

Ce kit installe **Farsameh** comme référent technique du laboratoire Java/Spring et actualise le DAT pour y inclure la CI (pratique future) et la CD (théorie seulement).

## Structure

```text
confluxlab/
├── AGENTS.md                         # Règles permanentes dans Zed
├── .agents/
│   └── skills/
│       └── farsameh/
│           └── SKILL.md              # Skill Zed /farsameh
└── docs/
    └── DAT.md                        # DAT ConfluxLab V0.2
```

## Installation

1. Copier **le contenu** de ce kit à la **racine du dépôt Git ConfluxLab** (préserver le dossier caché `.agents`).
2. Ouvrir ce dépôt dans Zed ; s'assurer que le worktree est déclaré fiable (*trusted*), si Zed le demande.
3. Dans l'Agent Panel, invoquer `/farsameh` ou mentionner `@farsameh` pour une session dédiée. Le skill peut également être sélectionné automatiquement lorsque la demande correspond à sa description.
4. `AGENTS.md` contient les règles permanentes, **à condition qu'aucun autre fichier d'instructions prioritaire ne le remplace** (par ex. `.rules`).
5. Commencer en mode **ARCHI** : « Farsameh, lis le DAT et challenge le parcours avec un diagramme de séquence et les scénarios d'échec. Ne code pas encore. »

**Important** : un *skill* n'est pas un processus autonome ni un agent séparé tournant en arrière-plan. C'est un jeu d'instructions chargé par le Zed Agent. La combinaison `AGENTS.md` + `SKILL.md` forme ici l'équivalent pratique d'une persona projet.

## Scope CI/CD

- **CI** : GitHub Actions sur PR/push (après implémentation), Maven `verify`, tests unitaires, tests MongoDB via Testcontainers et HTTP via WireMock.
- **CD** : documentation de l'approche, des risques et des réponses d'entretien ; **aucun déploiement à configurer**.

## Fichier existant `docs/DAT.md`

Si le dépôt dispose déjà d'un DAT différent, **comparer les versions avant de le remplacer**, conserver les parties validées et intégrer seulement les nouveautés CI/CD. Ne pas écraser des modifications locales sans revue.
