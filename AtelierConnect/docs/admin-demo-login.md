# Connexion administrateur locale

La connexion utilise **l'e-mail** comme identifiant et **le matricule** comme
mot de passe. Le serveur vérifie le hash BCrypt enregistré et émet un JWT.

| Compte | E-mail de connexion | Mot de passe initial |
| --- | --- | --- |
| Administrateur global | `admin@atelierconnect.example` | `ADMIN001` |
| Centre Bruxelles | `admin.bruxelles@atelierconnect.example` | `ADMIN002` |
| Centre Namur | `admin.namur@atelierconnect.example` | `ADMIN003` |

Ces adresses `.example` sont réservées à la démonstration et ne reçoivent pas
de courrier. L'initialiseur retrouve chaque compte par son matricule, remplace
uniquement son ancienne adresse connue ou vide et conserve les adresses
personnalisées. Il refuse un e-mail déjà utilisé par un autre administrateur.

Sur une base dont l'initialisation est désactivée, le script
`scripts/migrations/20261108_align_demo_admin_emails.up.sql` effectue uniquement
la migration des anciennes adresses, après sauvegarde. Il est rejouable et ne
change ni les mots de passe, ni les rôles, ni les centres. Se reconnecter ensuite
avec le nouvel e-mail pour renouveler le JWT.

Les nouveaux comptes de démonstration créés par `DataInitializer` suivent
cette règle. Les mots de passe déjà présents ne sont pas réinitialisés à
chaque démarrage. Pour appliquer explicitement la règle aux administrateurs
existants de la base locale :

```powershell
cd C:\Users\user\IdeaProjects\AtelierConnect\AtelierConnect
.\scripts\set-demo-admin-passwords.ps1
```

Cette commande modifie uniquement les hashes des administrateurs, vérifie le
résultat avant validation de la transaction et conserve les e-mails, matricules,
participants, identifiants PostgreSQL et clé JWT. Aucune migration SQL
supplémentaire n'est nécessaire pour ce changement de mots de passe.
`APP_DEMO_ADMIN_PASSWORD` n'est plus utilisé par l'initialiseur.
