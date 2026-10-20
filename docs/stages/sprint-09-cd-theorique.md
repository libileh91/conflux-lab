# Sprint 09 — Atelier CD et restitution d'entretien

**Statut : planifié — théorie uniquement.** Prérequis : compréhension du parcours et de la CI ; accord pour cet atelier. Il peut être étudié même si un accès GitHub bloque la preuve de CI, à condition de ne pas prétendre celle-ci validée.

## Objectif

Savoir expliquer comment on livrerait l'application, sans créer de pipeline de livraison ni déployer quoi que ce soit.

## Séances, une par échange

- [ ] **INT-33 — Dessiner la livraison conceptuelle.** Même artefact immuable promu entre environnements, configuration externalisée, contrôles et approbations. Distinguer continuous delivery et continuous deployment.
- [ ] **INT-34 — Analyser un incident.** Expliquer smoke tests, readiness, observabilité, rollout et rollback, notamment les limites d'un rollback applicatif après changement de données ou effet partenaire.
- [ ] **INT-35 — Restituer le parcours.** Dessiner de mémoire les modules, la séquence d'acceptation et le graphe A–F ; expliquer une panne et le test qui la révèle.
- [ ] **INT-36 — Simuler l'entretien.** Une question à la fois, réponse de l'utilisateur puis correction et formulation orale en 30–60 secondes. Distinguer contribution AXA rapportée, simulation et recommandation actuelle.

## Critères de fin

- [ ] Un schéma conceptuel de livraison est documenté, sans workflow exécutable.
- [ ] L'utilisateur distingue acceptation, traitement, orchestration concurrente, CI et CD.
- [ ] Les limites d'événements locaux, d'idempotence et des échecs partiels sont expliquées sans promesse de production.
- [ ] Les faits AXA non confirmés restent explicitement inconnus ; les mesures du labo ne sont pas réattribuées à AXA.
- [ ] Une courte restitution et les points à retravailler sont consignés après l'atelier, pas à l'avance.

## Interdits pratiques

Aucun workflow `deploy`, secret de déploiement, publication d'image, SSH, VPS, cloud, Kubernetes ni déploiement effectif. On peut expliquer une option en théorie sans l'installer.

## Question de départ

« La CI est verte : pourquoi cela ne prouve-t-il ni que l'application est déployée, ni qu'elle fonctionnera correctement en production ? »

## Preuves de clôture

Compléter le [journal commun](README.md#journal-à-compléter-en-clôture-de-chaque-sprint) avec les documents produits et les réponses discutées. Aucun test ou déploiement n'est requis pour cet atelier théorique.
