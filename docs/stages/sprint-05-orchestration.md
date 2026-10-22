# Sprint 05 — Orchestrer les six opérations

**Statut : INT-17 à INT-20 réalisées dans leur périmètre Java.** Un seul résultat est regroupé ici : un graphe de six opérations, vérifié avec des doublures, dont les invocations passent par un executor explicite. **Ce n'est pas un parcours de bout en bout : le graphe n'est pas raccordé au listener.**

**[Simulation ConfluxLab]** Les opérations et identifiants sont fictifs. INT-17/18/19 sont commitées localement dans `d6f0e86`, `516ea0e` et `eb0dc5a`, sans push ; le code INT-20 est commité localement dans `c989e29`. Cette condensation conserve les IDs et leurs preuves historiques, sans relancer le développement ni les vérifications.

## Story — Un résultat Java démontrable

En tant que développeur, je veux orchestrer les six opérations selon leurs données et dépendances, avec une exécution explicitement maîtrisée, afin de faire avancer les branches indépendantes sans annoncer de fin prématurée ou de faux succès.

**Prérequis réutilisés :** les types de demande existants et le pool borné `familyRequestExecutor` d'INT-15. Les contrats, la composition, les échecs et la soumission sont les étapes internes de ce résultat, pas quatre nouvelles stories à autoriser séparément.

## Résultat réalisé et critères résumés

- [x] Les six contrats internes utilisent des résultats typés : `PrincipalId`, `ContractId`, `DependentId` et `Void`, sans modifier le contrat HTTP public.
- [x] A précède B/C/D ; E dépend de B/C et F de B/D. Les rattachements n'attendent pas la branche indépendante et leur fin effective est suivie par aplatissement des futures.
- [x] La jonction finale attend B/C/D et les chemins E/F. Un échec représentatif de B empêche les appels E/F, conserve les branches déjà lancées et remonte sa cause après leur fin, sans faux succès ni annulation forcée.
- [x] Les six invocations passent par `submit`, avec `supplyAsync(..., executor)` et `thenCompose(Function.identity())`. Les rejets initial et en continuation deviennent des futures exceptionnelles ; aucun `commonPool` implicite ou `join/get` dans l'orchestrateur.
- [x] Un contrôle partage le même orchestrateur et le pool INT-15 entre deux UUID, avec **1 worker et 16 places** : la composition rend la main, l'échec de l'un ne termine pas l'autre. Cela ne prouve ni charge, ni câblage Spring de production.

Les cases décrivent les preuves Java déjà consignées, pas une nouvelle validation de cette session documentaire.

## IDs conservés — statut et preuve historique

| ID | Contribution au résultat | Preuve et statut |
|---|---|---|
| INT-17 | Contrats internes et doublure à futures pilotables | Réalisée ; `test-compile` réussi, **aucun test exécuté** ; `d6f0e86` |
| INT-18 | Composition nominale et indépendance des rattachements | Réalisée ; **2 cas ciblés réussis**, deux ordres C/D ; `516ea0e` |
| INT-19 | Attente finale des branches et propagation de l'échec | Réalisée pour le graphe Java ; **3 cas réussis**, dont les 2 précédents ; `eb0dc5a` |
| INT-20 | Soumission explicite, rejets et partage/isolation | Mécanisme Java réalisé, commit `c989e29` ; **6 cas réussis**, dont 3 réutilisés et 3 ajoutés : 2 rejets + 1 contrôle de partage |

**Ne pas additionner 2 + 3 + 6 :** les passages réutilisent les cas existants. La saturation réelle du pool reste une preuve historique d'INT-15, sans nouvel IT exécuté. Aucun test ou build n'a été relancé pour cette fiche.

## Cours commun

[INT-17 à INT-20 — Orchestrer les six opérations](../../notes/sprint-05/INT-17-orchestrer-les-six-operations.md) : DAG, contrats typés, fragments du code courant INT-20, composition/aplatissement, attente finale, executor, preuves et limites. Les commandes essentielles, preuves datées et limites des anciens incréments y sont réunies.

## Limites et suite

- **Pas de raccordement de production :** aucun bean d'orchestrateur ou implémentation/bean de production `FamilyOperations`, aucun appel HTTP partenaire ni état final MongoDB. Le listener s'arrête à la tentative atomique `PENDING → PROCESSING` ; une future en échec ne constitue pas un `FAILED` persisté.
- **Le type `Executor` ne garantit pas le bornage.** Le futur câblage devra fournir explicitement `familyRequestExecutor`, dont les défauts de production sont **2 workers / 16 places**, puis observer la terminaison sans `join/get` dans le listener ou son pool.
- Le pool borne ses workers et tâches en file, pas une activité externalisée dans un autre pool. Accepter A ne réserve pas la capacité du graphe entier ; les continuations peuvent être refusées. Annuler une future n'est pas un rollback des effets partenaires.
- La preuve reste Java, sans suivi HTTP/MongoDB final, benchmark ou reprise après crash. Le BFF reste mince ; Capability possède l'orchestration, dans une seule application et sans HTTP interne.

**Pas de clôture pédagogique déclarée :** la compréhension et la restitution orale ne sont pas évaluées par cette réorganisation ; aucun questionnaire ou journal de clôture supplémentaire n'est imposé.

Le prochain résultat, **INT-21 + INT-22 — Créer une famille de bout en bout**, reste à autoriser dans la [fiche partenaire](sprint-06-partenaire.md). Son raccordement est suivi par [TD-B018](../../notes/todos.md#td-b018) ; cette fiche n'ouvre pas ce développement.
