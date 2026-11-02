# Recette complète du 2 novembre 2026

Commande exécutée depuis le dossier backend, après arrêt des serveurs locaux
sur les ports 8082 et 4200 :

```powershell
.\scripts\validate-fullstack.ps1 -SkipInstall
```

Le conteneur PostgreSQL 15 dédié à la validation porte l'étiquette
`atelierconnect.validation=true` et utilise le port 5549. La procédure crée
une base backend et une base navigateur distinctes pour chaque exécution.
Elle refuse le conteneur applicatif et le port PostgreSQL applicatif 5441.

| Vérification | Résultat |
| --- | --- |
| Maven clean verify, compilation et JAR | Réussi |
| Tests backend | 188 réussis, 0 échec, 0 erreur, 0 ignoré |
| Tests Angular | 83 réussis, 0 échec |
| Compilation Angular | Réussie |
| Parcours Chromium sur les vrais serveurs | 6 réussis, 0 échec, 0 ignoré |
| Persistance après redémarrage PostgreSQL de test | Vérifiée |

Les tests navigateur couvrent notamment la réservation privée, les invitations,
le paiement, les autorisations, le consentement et la gestion administrative.
L'authentification de l'administrateur de démonstration utilise son matricule,
récupéré uniquement dans l'environnement de test au moment de la recette.

Cette exécution porte l'identifiant `20261102_215914_089`. Les journaux et le
rapport JSON détaillé restent sous `.local/validation/20261102_215914_089/`,
ignorés par Git. Aucune configuration privée ni sauvegarde n'est publiée.
Il s'agit de résultats locaux ; ils ne préjugent pas du résultat d'une future
exécution GitHub Actions.

Les essais de migration et leurs contrôles de conservation sont décrits dans
[schema-naming-migration.md](schema-naming-migration.md). Les parcours visiteur
autorisé et suspendu sont décrits dans
[demo-visitor-access.md](demo-visitor-access.md).
