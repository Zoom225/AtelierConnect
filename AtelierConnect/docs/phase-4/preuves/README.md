# Captures publiques de recette

Images produites par Chromium, depuis les tests automatisés
`frontend/e2e/recette-accessibility.spec.ts`, lors de l'exécution locale
`20261101_123446_496`. Elles ont été inspectées visuellement par l'agent.
Aucun mot de passe, JWT ou compte connecté n'est visible.

- [Accueil français, desktop](accueil-fr-desktop.png) : marque et vocabulaire AtelierConnect.
- [Accueil anglais, mobile](accueil-en-mobile.png) : viewport de 390 px, contenu complet sans débordement horizontal.
- [Connexion au clavier](connexion-clavier.png) : formulaire vide, focus atteint par Tab sur le champ mot de passe.

Les quatre parcours transactionnels sont prouvés par les assertions Playwright,
les réponses de la vraie API et les états PostgreSQL ; aucune trace réseau ni
capture contenant des secrets n'est ajoutée à Git. La CI conserve ces PNG publics
dans l'artefact `recette-public-screenshots` lorsque le job navigateur réussit.

Une capture automatique et une inspection par agent ne remplacent pas une recette
humaine complète, ni une évaluation avec lecteur d'écran ou sur téléphone physique.
