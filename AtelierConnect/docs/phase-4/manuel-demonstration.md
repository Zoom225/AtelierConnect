# Manuel utilisateur, administration et démonstration

## Participant

1. Ouvrir l'accueil sur le frontend, puis l'espace participant `/member`.
2. Se connecter avec le matricule et le mot de passe attribués. Le compte doit
   exister ; aucun parcours public d'inscription autonome n'est proposé.
3. Consulter son profil, sa catégorie, son centre éventuel et son solde.
4. Consulter `/member/ateliers`. Rejoindre un atelier public crée une réservation
   en attente ; cette action ne garantit pas encore la place.
5. Pour organiser, ouvrir `/member/ateliers/nouveau`, choisir centre, salle, date,
   heure et type. Le formulaire applique le délai de la catégorie ; le backend
   vérifie aussi les horaires, fermetures, accès et conflits de créneau.
6. Pour un atelier privé, saisir jusqu'à cinq matricules invités. L'organisateur
   peut également ajouter des invités depuis la gestion de ses réservations.
7. Dans `/member/reservations`, payer sa réservation. Il s'agit d'une **validation
   applicative simulée**, sans saisie ni débit de carte bancaire. La place devient
   confirmée si elle est encore disponible au moment du paiement.
8. Annuler une réservation depuis cette même page. Si elle était payée, le statut
   du paiement devient REMBOURSE simulé ; aucune restitution bancaire réelle.
9. L'organisateur peut modifier/annuler son atelier privé et confirmer un accord
   d'ouverture publique éventuelle. Ce consentement appartient uniquement à
   l'atelier sélectionné et reste enregistré après rechargement.

À J−2, le scheduler peut ouvrir un atelier privé planifié et incomplet si l'accord
explicite a été enregistré. Un atelier complet, annulé ou sans accord n'entre pas
dans ce traitement automatique. Le paiement de l'organisateur n'est pas dispensé.

Les erreurs de dette, suspension, doublon, centre interdit, date trop éloignée ou
créneau occupé doivent être traitées sans contourner les validations. Le nombre
de places confirmées reste limité à six, même lorsque des intentions de réservation
publiques sont simultanées.

Le bouton de déconnexion efface la session du navigateur. Il ne constitue pas une
révocation serveur immédiate d'un JWT déjà émis. La langue se choisit dans l'en-tête.

## Administrateur

Se connecter sur `/admin/login` avec l'email et le mot de passe administrateur.
Le rôle GLOBAL donne un périmètre global ; le rôle SITE limite les opérations au
centre attribué, selon les contrôles backend. Un rôle administratif SITE n'est
pas la catégorie ABONNE d'un participant.

| Écran | Utilisation réalisée |
| --- | --- |
| `/admin/centres` | Administrateur GLOBAL : créer, modifier et supprimer un centre ; définir l'ouverture aux visiteurs |
| `/admin/salles` | Créer une salle dans un centre autorisé, modifier son nom et supprimer une salle |
| `/admin/participants` | Lister/filtrer, créer et modifier les informations autorisées d'un participant ; mot de passe initial selon configuration ou saisie |
| `/admin/ateliers` | Consulter les ateliers autorisés et leurs réservations ; opérations administratives exposées selon le rôle |
| `/admin/fermetures` | Gérer les dates de fermeture, globales ou liées à un centre selon les permissions |

Le matricule et la catégorie ne sont pas librement modifiables après création dans
le formulaire participant. Le service de mise à jour d'une salle modifie son nom,
pas son centre : ne pas présenter le changement de rattachement comme une fonction
réalisée. La suppression d'un centre/salle peut cascader vers des données associées
selon le modèle JPA ; ne l'utiliser en démonstration que sur les éléments créés
pour cette démonstration, dans la base de test.

Les contrôles de formulaires et filtres sont ergonomiques. Les autorisations réelles
sont appliquées par Spring Security et les services d'autorisation.

## Démonstration indicative de 12 à 15 minutes

Préparer une base dédiée avec le script de validation, ou une instance de
démonstration isolée et des secrets définis dans le terminal/IDE. Ne jamais montrer
les mots de passe, les variables privées ni un JWT à l'écran. Les comptes de test
initialisés comprennent notamment les matricules `G1002`, `S10001`, `L10001` et
l'administrateur `admin@atelierconnect.example` ; leurs mots de passe proviennent
des variables privées de la nouvelle base, pas de la documentation.

| Étape | Action | Preuve à expliquer |
| --- | --- | --- |
| 1 | Présenter le besoin et les acteurs | Catégories participantes distinctes des rôles administrateurs |
| 2 | Afficher les diagrammes | Centre → salle → atelier ; réservation et paiement séparés |
| 3 | Accueil et connexion participant | Client Angular, appel HTTP, JWT et profil |
| 4 | Créer un atelier public par le formulaire | Date/centre autorisés ; création en attente |
| 5 | Payer puis annuler | Confirmation après paiement, remboursement simulé et états persistants |
| 6 | Atelier privé et invitation | Organisateur + cinq invités, responsabilité du propriétaire |
| 7 | Consentement puis rechargement | Accord explicite persistant ; conversion à J−2 expliquée par le test scheduler |
| 8 | Connexion admin et CRUD centre/salle temporaire | Contrôles backend et persistance réelle |
| 9 | Swagger et un refus HTTP | Contrat REST, validation et absence de contournement sans JWT |
| 10 | Tests concurrents et CI | Un seul gagnant pour la dernière place ; données conservées après redémarrage |
| 11 | Montrer les limites | Historique SQL conservé, simulation financière, arbitrages et recette restante |

Une attente jusqu'à minuit n'est pas nécessaire pour démontrer le code du scheduler :
montrer son test, puis réserver la validation de son déclenchement réel à la recette
d'exploitation. Ne pas présenter un appel manuel comme la preuve d'un déclenchement
horaire déjà observé.

## Questions à préparer pour la défense

- Pourquoi distinguer réservation et paiement ? Pour séparer l'intention de la
  place confirmée et gérer l'échec, l'annulation et la concurrence.
- Pourquoi un verrou PostgreSQL ? Deux processus peuvent lire cinq participants
  simultanément ; le verrou et le rafraîchissement sérialisent la décision.
- Pourquoi garder `matches` en SQL ? Pour préserver les données ; renommer le
  modèle Java n'est pas une migration physique validée.
- Pourquoi un test sans transaction englobante ? Une transaction de test pourrait
  masquer l'erreur de chargement paresseux observée après la modification d'une salle.
- Pourquoi les guards ne suffisent-ils pas ? Un client peut appeler directement
  l'API ; les services reconstruisent l'identité et contrôlent le périmètre.
- Qu'apporte le test navigateur ? Il vérifie formulaires, imports, proxy, JSON,
  API et données ensemble, au-delà d'une compilation ou d'un mock.
- Que manque-t-il avant une exploitation financière réelle ? Prestataire, preuves
  de paiement, idempotence externe, remboursements réels et exploitation adaptée.
