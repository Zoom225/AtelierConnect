# Connexion des participants en local

Le mot de passe de démonstration est fourni par `APP_DEMO_MEMBER_PASSWORD` dans la configuration locale privée.
Utiliser le matricule du participant comme identifiant.

Pour appliquer ce mot de passe aux participants existants de la base locale :

```powershell
cd C:\Users\user\IdeaProjects\AtelierConnect\AtelierConnect
.\scripts\set-demo-participant-passwords.ps1
```

Cette commande explicite remplace uniquement les hashes des mots de passe des
participants dans `participants`. Elle vérifie les hashes avant de valider la
transaction. Elle ne modifie aucun compte administrateur, rôle PostgreSQL ou
secret JWT et n'est pas exécutée automatiquement au démarrage.

La commande met également à jour `APP_DEMO_MEMBER_PASSWORD` dans la
configuration IntelliJ privée existante. Redémarrer le backend avec
`AtelierConnect — Local` pour les nouveaux comptes de démonstration et les
participants créés sans mot de passe explicite. Un mot de passe fourni
explicitement à la création d'un participant reste respecté.
