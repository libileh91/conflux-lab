# Sprint 03 — Persistance MongoDB et cycle des états

**Statut : planifié.** Prérequis : sprint 02 clos et Docker disponible pour les tests. Chaque story attend son feu vert individuel.

## Objectif

Remplacer le stockage en mémoire par MongoDB pour retrouver une demande après un redémarrage et contrôler ses changements d'état.

## Repères

- La **persistance** conserve les données au-delà de l'arrêt de l'application.
- Un **repository** est le composant qui lit et écrit les données ; seul Capability y accède.
- **Testcontainers** lance un MongoDB temporaire dans Docker pour isoler les tests.

## Stories — un feu vert par tâche

### INT-09 — Définir les données et transitions
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux définir les informations conservées et les changements d'état permis, afin de représenter correctement la vie d'une demande.

**À faire :**
- Décrire `family_requests` : identifiant, données reçues, état, dates et éventuels résultat/erreur.
- Définir `PENDING → PROCESSING → SUCCEEDED/FAILED`, en expliquant chaque état.

**Terminé quand :**
- [ ] Le modèle décrit les champs et leurs dates de mise à jour, sans exposer le document MongoDB au BFF.
- [ ] Les tests acceptent les transitions prévues et refusent de revenir d'un état final vers un état en cours.

### INT-10 — Persister avant d'accepter
**Statut : à faire — feu vert requis.** Prérequis : INT-09 terminée.

En tant que client de l'API, je veux que ma demande soit enregistrée avant son acceptation, afin de pouvoir la retrouver si l'application redémarre.

**À faire :**
- Remplacer la mémoire par un repository MongoDB dans Capability.
- Attendre la confirmation d'écriture avant de renvoyer `202`.

**Terminé quand :**
- [ ] Un test enregistre et relit la demande avec le même UUID et les mêmes données ; l'unicité de l'identifiant est contrôlée en base.
- [ ] Un échec de stockage retourne une erreur HTTP explicite, jamais une acceptation fictive.

### INT-11 — Sécuriser les mises à jour
**Statut : à faire — feu vert requis.** Prérequis : INT-10 terminée.

En tant que développeur, je veux empêcher deux traitements de prendre la même demande en charge simultanément, afin d'éviter des exécutions concurrentes incohérentes.

**À faire :**
- Modifier un état seulement si sa valeur actuelle permet la transition demandée.
- Conserver des dates et erreurs cohérentes, sans écraser un résultat final.

**Terminé quand :**
- [ ] Un test de double prise en charge simultanée ne laisse qu'un passage de `PENDING` à `PROCESSING`.
- [ ] Les tests refusent les transitions invalides et les écrasements d'états finaux ; le suivi ne divulgue pas d'erreur interne.

### INT-12 — Vérifier la persistance
**Statut : à faire — feu vert requis.** Prérequis : INT-09/10/11 disponibles.

En tant que développeur, je veux tester la conservation des demandes avec un vrai MongoDB isolé, afin de vérifier autre chose qu'une simulation en mémoire.

**À faire :**
- Exécuter les tests MongoDB via Testcontainers avec des données isolées.
- Relancer l'application avec la même base pour démontrer la relecture ; raccorder les tests `*IT` à Maven Failsafe.

**Terminé quand :**
- [ ] La demande reste consultable après redémarrage de l'application avec la même base.
- [ ] `./mvnw -B -ntp verify` exécute réellement les tests d'intégration et produit leurs rapports ; Experience n'importe aucun repository ou document MongoDB.

## Limites et hors périmètre

Il n'y a pas encore de traitement en arrière-plan : les demandes restent normalement `PENDING`. Les autres transitions sont appelées dans les tests, pas simulées comme un parcours métier réussi.

Le UUID ne suffit pas à empêcher deux créations métier identiques. MongoDB et un partenaire HTTP ne partagent pas de transaction qui annulerait tout ensemble. Pas de reprise automatique, événement durable ou transaction distribuée.

## Démo et clôture

- [ ] Enregistrer une demande, redémarrer l'application avec la même base et retrouver son état.
- [ ] Montrer le test d'échec de stockage et expliquer pourquoi il interdit un faux `202`.

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les prérequis Docker, tests et preuve de relecture réellement observés.
