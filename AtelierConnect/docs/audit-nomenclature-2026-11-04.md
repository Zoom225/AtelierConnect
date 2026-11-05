# Audit de la nomenclature AtelierConnect — 4 novembre 2026

**Évolution du 5 novembre :** les noms des centres sont désormais
[Centre Bruxelles et Centre Namur](belgian-centre-cities.md), à la demande du
propriétaire. Les données et identifiants de connexion restent conservés.

La recherche couvre le code Java et Angular, les tests, les scripts, les
configurations et la documentation. Les dépendances, builds, secrets, journaux
et sauvegardes ne sont pas des sources à renommer. Les centres et salles de la
base applicative ont été consultés dans une transaction en lecture seule.

## Corrections

- Swagger : `Padel Backend API` devient `AtelierConnect API`, et le contact
  `Padel Backend` devient `AtelierConnect`. La description concerne la gestion
  et la réservation d'ateliers collaboratifs. L'adresse historique du compte
  administrateur est retirée du contact public ; aucun compte n'est modifié.
- Les exemples des tests d'autorisation et de contrôleurs utilisent des e-mails
  en `atelierconnect.example` ; les fixtures Angular utilisent des centres et
  salles d'ateliers, avec les mêmes règles et assertions métier.
- Un test vérifie l'identité Swagger et le maintien de la sécurité Bearer/JWT.

## Occurrences conservées et leur raison

| Emplacement | Raison et traitement |
| --- | --- |
| Base : anciens `Padel Club Lyon/Paris`, centres 1 et 2 | Renommage approuvé et appliqué vers `Centre Lyon/Paris`, sans changement d'identifiants. |
| Base : anciens `Court A/B/C/1/2`, salles 1 à 5 | Renommage approuvé et appliqué vers `Salle A/B/C/1/2`, avec les mêmes réservations et rattachements. |
| E-mails administrateurs en `padel.com` et README | Identifiants locaux fonctionnels, conservés pour ne pas casser les connexions. |
| `DataInitializer` et son test de compatibilité | Reprise des anciens comptes sans réinitialisation de leurs mots de passe. |
| Clés `padel_auth_session`, `padel_admin_session`, `padel_member_session`, `padelplay_language` | Sessions et langue déjà enregistrées dans les navigateurs. Clés internes, sans libellé visible dans l'interface. |
| Routes `matches`, `matches/new`, `terrains` | Compatibilité des anciennes URL ; les routes canoniques existent déjà. |
| Test navigateur recherchant les anciens termes | Assertion interdisant leur retour sur l'accueil, conservée pour détecter une régression. |
| Rapports historiques et plan de migration | Preuves datées, conservées avec leur contexte historique. |
| `.run/Unnamed.run.xml` | Ancien lancement vers `padelmultiple` et `com.padell.padell.PadellApplication`. Configuration conservée ; utiliser `AtelierConnect — Local`. |

Les packages applicatifs actuels sont sous `com.atelierconnect`. Les tables
canoniques sont `ateliers`, `participants`, `centres`, `salles`. Cet audit ne
change ni leurs mappings, ni leurs identifiants, ni les données métier.
Les nouveaux comptes et centres de démonstration de l'initialiseur utilisent
déjà l'identité AtelierConnect. Les anciennes valeurs ne sont pas masquées
par des substitutions dans le frontend.

Le [rapport de renommage](resource-label-renaming.md) décrit la sauvegarde,
la répétition sur copie, le retour arrière et les contrôles après application.

## Vérifications du correctif

```powershell
# Depuis la racine Git : PostgreSQL de test dédié sur 5549
.\AtelierConnect\scripts\test-backend-isolated.ps1

# Depuis AtelierConnect/frontend
npm test -- --maxWorkers=2
npm run build
```

- 189 tests backend réussis, aucun échec, erreur ou test ignoré. Base dédiée :
  `atelierconnect_test_20261104_164420_687`, conteneur `atelierconnect-validation-db`.
- 83 tests Angular réussis ; compilation Angular de production réussie.
- Le nouveau test `SwaggerConfigTest` vérifie la marque et la sécurité JWT.
- Les preuves de tests restent locales sous `.local/diagnostics/`.
- Après redémarrage via `AtelierConnect — Local`, `/v3/api-docs` affiche
  `AtelierConnect API` et le contact `AtelierConnect`. Swagger UI, l'API publique
  et ses accès via les proxys Angular sur 4200 et 56700 répondent HTTP 200.

La recette Chromium complète précédente (8 parcours) reste datée du 4 novembre
dans le rapport de refonte ; elle n'a pas été réexécutée pour ce correctif de
nomenclature.
