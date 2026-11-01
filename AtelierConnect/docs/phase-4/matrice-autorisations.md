# Matrice des autorisations et preuves JWT

`AuthorizationJwtIntegrationTest` utilise des JWT signés par la vraie configuration,
des administrateurs/participants persistés et la chaîne Spring Security réelle.
Les requêtes passent dans MockMvc, sans authentification simulée par guard Angular
ni `@WithMockUser`. Les parcours Chromium ajoutent des connexions HTTP réelles.
Les dumps automatiques de requêtes sont désactivés dans ce test pour ne pas
journaliser les Bearer tokens ; les assertions et erreurs de tests restent actives.

| Ressource / opération | Non connecté | Participant | ADMIN GLOBAL | ADMIN SITE |
| --- | --- | --- | --- | --- |
| Catalogue centres/salles, ateliers publics | Lecture publique | Lecture publique | Lecture publique | Lecture publique, y compris hors centre car catalogue public |
| Liste administrative des ateliers | Refus | Refus | Tous | Son centre, par identifiants et non par nom |
| Atelier privé / réservations d'atelier | Refus | Propriétaire ou autorisé selon contrôleur | Lecture administrative autorisée | Lecture administrative dans son centre |
| Centres : créer/modifier/supprimer | Refus | Refus | Autorisé | Refus, même sur son centre |
| Salles : créer/modifier/supprimer | Refus | Refus | Tous centres | Son centre ; aucune modification d'un centre étranger |
| Participants : liste/gestion | Refus | Lecture individuelle propre selon route | Tous | Son centre ; liste filtrée et accès direct étranger refusé |
| Fermetures : lecture | Refus | Refus | Toutes | Globales et celles de son centre |
| Fermetures : écriture/suppression | Refus | Refus | Globales et tous centres | Son centre ; ni fermeture globale ni centre étranger |
| Réservation individuelle | Refus | Propriétaire uniquement | La route individuelle n'est pas une permission générale admin | La route individuelle n'est pas une permission générale admin |
| Paiements | Refus | Propriétaire uniquement | Pas de paiement au nom d'un participant via ce rôle | Pas de paiement au nom d'un participant via ce rôle |
| Consentement privé | Refus | Organisateur uniquement | Ne donne pas l'accord à sa place | Ne donne pas l'accord à sa place |

## Tests et régressions

| Méthode de `AuthorizationJwtIntegrationTest` | Ce qui est démontré |
| --- | --- |
| `siteListUsesIdentifiersEvenWhenCentresHaveIdenticalNames` | Deux centres homonymes restent isolés ; un SITE ne reçoit pas les ateliers/participants de l'autre |
| `siteCannotModifyForeignRoomParticipantOrCentre` | Accès direct étranger refusé, aucune modification persistée ; création/suppression de centre refusée |
| `globalCanManageRoomsAcrossBothCentresAndReadParticipantsAndAteliers` | GLOBAL peut modifier les salles des deux centres et lire leurs ressources administratives |
| `closurePermissionsAreEnforcedThroughRealJwt` | SITE écrit/supprime dans son centre, voit les globales, ne voit pas les étrangères ; GLOBAL gère les autres centres et les globales |
| `memberCannotReadPayOrCancelSomeoneElsesReservation` | Réservation propre lisible ; lecture/paiement/annulation d'autrui refusés ; états réservation et paiement inchangés |

Les refus peuvent être 400 (règle métier), 403 (Spring Security / autorisation),
ou 404 si la ressource est absente, conformément au contrat existant. Un 500 n'est
pas considéré comme un refus de sécurité acceptable et a fait échouer la recette.
Les tests existants JWT invalide, controllers et autorisations complètent ce tableau.

## Limites de cette preuve

Un catalogue public n'est pas rendu privé parce qu'un administrateur SITE le consulte.
Les catégories PARTENAIRE/ABONNE/VISITEUR ne sont pas les types d'administrateur
GLOBAL/SITE. Aucun mécanisme de révocation, refresh token ou paiement bancaire réel
n'est ajouté par ces tests. Une revue humaine de l'ensemble des écrans par rôle
reste à effectuer ; la matrice ne constitue pas une certification de sécurité.
