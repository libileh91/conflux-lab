# Sprint 09 — Atelier CD et restitution d'entretien

**Statut : planifié — théorie uniquement.** Prérequis : comprendre le parcours et la CI ; accord pour cet atelier. Un accès GitHub bloqué n'empêche pas l'atelier, mais la CI ne doit pas être présentée comme validée.

## Objectif

Expliquer comment une version pourrait être livrée et présenter le parcours en entretien, sans créer de pipeline de livraison ni déployer quoi que ce soit.
Ce sprint est un objectif pédagogique : chaque séance attend son feu vert individuel ; le plan ne lance rien. Les vérifications sont documentaires et orales, sans test exécutable requis.

## Repères

- Un **artefact immuable** est une version construite une fois, conservée sans modification entre les environnements.
- La **CD** désigne la livraison ou le déploiement continus, deux pratiques à distinguer et étudiées ici seulement en théorie.
- Un **rollback** est un retour à une version précédente ; il n'annule pas automatiquement les données modifiées ni les actions d'un partenaire.

## Tâches — un feu vert par séance

### INT-33 — Dessiner la livraison conceptuelle
**Statut : à faire — feu vert requis.**

En tant que développeur débutant, je veux visualiser les étapes d'une livraison, afin de distinguer une version vérifiée d'une application réellement déployée.

**À faire :**
- Dessiner la promotion du même artefact entre environnements, avec configuration externe, contrôles et approbations.
- Expliquer la livraison continue (version prête à livrer) et le déploiement continu (mise en service automatique après validation).

**Terminé quand :**
- [ ] Le schéma montre le même artefact, les environnements et les contrôles, sans workflow exécutable.
- [ ] La restitution distingue CI, livraison continue et déploiement continu, sans affirmer qu'une CI verte prouve un déploiement.

### INT-34 — Analyser un incident
**Statut : à faire — feu vert requis.**

En tant que développeur, je veux raisonner sur un incident après livraison, afin d'expliquer les contrôles utiles et les limites d'un retour en arrière.

**À faire :**
- Relier les contrôles rapides, l'aptitude à recevoir du trafic, la mise en service et la surveillance à un incident fictif.
- Expliquer quand revenir à une version précédente et ce que ce retour ne répare pas.

**Terminé quand :**
- [ ] Le scénario explique comment détecter le problème et décider de poursuivre ou d'arrêter la mise en service.
- [ ] L'explication précise les limites du rollback après un changement de données ou une action partenaire, sans promettre de rollback distribué.

### INT-35 — Restituer le parcours
**Statut : à faire — feu vert requis.**

En tant que développeur débutant, je veux présenter le parcours sans lire le code, afin de montrer que je comprends ses responsabilités et ses limites.

**À faire :**
- Dessiner de mémoire les modules, l'acceptation HTTP, le suivi et le graphe A–F.
- Expliquer une panne et le test qui la révèle, en distinguant tests prévus et réellement exécutés.

**Terminé quand :**
- [ ] Le dessin respecte le monolithe modulaire : BFF mince et Capability métier dans la même JVM, appels Java internes et Feign/Resilience4j uniquement vers WireMock ; l'ordre A–F est expliqué.
- [ ] La restitution distingue acceptation, traitement et concurrence, puis explique les événements locaux non durables, l'UUID de corrélation sans garantie d'idempotence et les échecs partiels sans exactly-once.

### INT-36 — Simuler l'entretien
**Statut : à faire — feu vert requis.**

En tant que développeur préparant un entretien, je veux m'entraîner à répondre clairement et honnêtement, afin de défendre ce que je comprends sans m'attribuer des faits non confirmés.

**À faire :**
- Poser une question, attendre la réponse, puis proposer une correction et une formulation orale en 30–60 secondes avant la question suivante.
- Distinguer **[AXA rapporté]**, **[Simulation ConfluxLab]**, **[Proposition]** et **[Non confirmé]**.

**Terminé quand :**
- [ ] La séance avance une question à la fois et sépare la contribution AXA rapportée, la simulation et les recommandations actuelles ; les inconnues restent signalées.
- [ ] Une courte restitution et les points à retravailler sont consignés après la séance, sans attribuer à AXA les mesures du laboratoire.

## Interdits pratiques

Aucun workflow `deploy`, aucun secret, aucune publication d'image, aucun SSH, VPS, cloud, Kubernetes ni déploiement effectif. Une option peut être expliquée sans être installée.

## Démo et clôture

- [ ] Présenter le schéma conceptuel, puis répondre à la question de départ : « La CI est verte : pourquoi cela ne prouve-t-il ni que l'application est déployée, ni qu'elle fonctionnera correctement en production ? »

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les documents produits et les réponses réellement discutées ; aucun test ou déploiement n'est requis pour cet atelier théorique.
