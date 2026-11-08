# Connexion administrateur locale

La connexion utilise **l'e-mail** comme identifiant et **le matricule** comme
mot de passe. Le serveur vérifie le hash BCrypt enregistré et émet un JWT.

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
participants, identifiants PostgreSQL et clé JWT. Aucune migration SQL n'est
nécessaire. `APP_DEMO_ADMIN_PASSWORD` n'est plus utilisé par l'initialiseur.
