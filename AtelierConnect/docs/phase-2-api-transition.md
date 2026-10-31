# API transitoire — phase 2

Les nouveaux chemins utilisent les mêmes contrôleurs et les mêmes services que les anciens chemins. Les anciens chemins restent disponibles pour Angular 21 jusqu'à la phase 3 ; il n'existe pas de copie de la logique métier.

| Nouveau chemin | Alias actuel conservé |
| --- | --- |
| `/api/participants` | `/api/membres` |
| `/api/ateliers` | `/api/matches` |
| `/api/salles` | `/api/terrains` |
| `/api/centres` | `/api/sites` |
| `/api/reservations` | Identique |
| `/api/paiements` | Identique |

Les sous-chemins et les champs JSON portent encore les noms historiques (`matchId`, `membreId`, `terrainId`, `siteId`, etc.). La phase 3 devra remplacer ces champs côté Angular après publication de DTO métier dédiés ; aucune suppression de clé ou de route historique ne doit précéder cette adaptation.

`PATCH /api/ateliers/{id}/consent-open-public` (également sous l'alias `/api/matches`) enregistre l'accord daté de l'organisateur authentifié. La conversion d'un atelier privé incomplet n'a lieu qu'à partir de J-2 après cet accord. Les paiements et réservations existants conservent leur statut ; la conversion ne crée ni paiement ni pénalité. Une annulation suit les règles de remboursement existantes.

L'indicateur `ouvertVisiteurs` d'un centre vaut `false` lorsque la valeur est absente. Les centres déjà enregistrés ont une valeur SQL `NULL` après l'ajout de la colonne : un administrateur doit les ouvrir explicitement aux visiteurs. Aucune ligne existante n'est supprimée.
