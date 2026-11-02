# Analyse fonctionnelle et cahier des charges

## Besoin et périmètre

AtelierConnect organise des ateliers collaboratifs dans plusieurs centres. Un
participant consulte les salles et ateliers, organise ou rejoint un atelier,
valide un paiement applicatif et consulte ses réservations. L'administration
gère les centres, salles, participants et fermetures selon son périmètre.

La réservation en attente exprime une intention ; seule la validation applicative
du paiement confirme la place. Le stockage conserve des noms SQL de l'ancien
projet, mais les services et écrans utilisent le domaine AtelierConnect.

Sont inclus : application web Angular, API Spring Boot, PostgreSQL, JWT, opérations
administratives, paiements/remboursements simulés, scheduler et tests. Sont exclus
du périmètre réalisé : paiement bancaire réel, emails transactionnels, application
mobile native et exploitation de production avec supervision complète.

## Acteurs

| Acteur | Droits et contraintes |
| --- | --- |
| Public non connecté | Accueil, centres, salles et liste publique des ateliers ; aucune écriture protégée |
| PARTENAIRE | Réserver jusqu'à 30 jours, plusieurs centres |
| ABONNE | Jusqu'à 15 jours, uniquement son centre de rattachement |
| VISITEUR | Jusqu'à 7 jours, uniquement les centres ouverts aux visiteurs |
| Administrateur GLOBAL | Administration globale ; créer et gérer les centres |
| Administrateur SITE | Opérations autorisées dans son centre ; pas de création globale de centre |

Une catégorie de participant n'est pas un rôle administratif. Les codes historiques
des matricules `G/S/L` restent utilisés ; aucun nouveau format n'a été spécifié.

## Cas d'utilisation et critères d'acceptation

| Référence | Cas | Résultat observable |
| --- | --- | --- |
| UC01 | Se connecter participant/admin | Identité valide : JWT ; identité invalide : erreur sans révéler l'existence du compte |
| UC02 | Consulter centres, salles, ateliers publics | Données affichées via `/api`, sans session JPA ouverte dans la vue |
| UC03 | Créer un atelier | Date autorisée, centre accessible, salle disponible, horaires/fermetures respectés ; atelier et réservation organisateur créés |
| UC04 | Inviter en privé | Seul l'organisateur ; organisateur + cinq invités actifs maximum, même en concurrence |
| UC05 | Réserver en public | Inscription personnelle, absence de doublon, de dette et de suspension ; état EN_ATTENTE |
| UC06 | Valider le paiement | Propriétaire uniquement ; état PAYE et réservation CONFIRMEE ; compteur incrémenté une fois |
| UC07 | Annuler une réservation | État ANNULEE ; paiement déjà validé → REMBOURSE simulé ; place confirmée libérée une fois |
| UC08 | Consentir à l'ouverture publique | Organisateur uniquement, confirmation explicite ; booléen et date persistés ; autres ateliers inchangés |
| UC09 | Ouvrir automatiquement à J−2 | Atelier privé à venir, planifié, incomplet, consentement enregistré ; aucune ouverture sans accord |
| UC10 | Administrer centres et salles | Autorisation backend, formulaire valide, réponse JSON contenant le centre ; modifications persistantes |
| UC11 | Administrer participants/fermetures | Respect du périmètre administratif ; filtres Angular ne remplacent pas les contrôles backend |
| UC12 | Refuser une opération invalide | Erreur HTTP/JSON compréhensible ; aucun état de paiement ou compteur partiellement confirmé |

## Règles contractuelles conservées

- Capacité : six participants confirmés maximum ; privé : organisateur et cinq invités.
- Durée : 120 minutes ; pause entre ateliers : 20 minutes.
- Prix : 90 EUR par atelier, 15 EUR par participant.
- Types : PUBLIC ou PRIVE ; états : PLANIFIE, COMPLET, ANNULE.
- Réservations : EN_ATTENTE, CONFIRMEE, ANNULEE.
- Le premier paiement validé sous le verrou d'atelier obtient la place disponible.
- Les délais 30/15/7 sont calculés en jours calendaires à partir de la date serveur.
- Création/inscription refusées en cas de dette ou suspension active.
- L'ouverture publique automatique ne crée pas implicitement un consentement.

Ces constantes figurent dans `AtelierRules` et `shared/models/atelier-rules.ts`.
Les réservations publiques en attente ne garantissent pas une place : leur nombre
peut dépasser le nombre de places restantes ; le paiement arbitre l'attribution.

## Règles historiques conservées sans nouvel arbitrage

Le scheduler annule les impayés à J−1 et impute un montant à l'organisateur ; le
paiement suivant peut récupérer une dette. Ces règles existaient et n'ont pas été
redéfinies par la demande. La fin de suspension utilise une date strictement
postérieure à aujourd'hui ; préciser si le dernier jour devrait être inclus.

Les durées stockées sur les centres et le prix historique d'une salle ne changent
pas les constantes d'un nouvel atelier. Les ateliers anciens ne sont pas
automatiquement recalculés. Les horaires traversant minuit et le fuseau de référence
pour plusieurs pays nécessitent un cahier des charges complémentaire.

## Exigences techniques vérifiées

Java 21 / Spring Boot 3.5.0, Angular 21 et TypeScript 5.9, PostgreSQL 15, REST JSON,
JWT stateless, autorisations backend et transactions. Port backend 8082, frontend
4200, PostgreSQL applicatif 5441. URL frontend relative `/api` et proxy de
développement. `ddl-auto=update`, sans Flyway, avec mappings physiques conservés.

Les nouveaux développements sont validés sur des bases dédiées. Les critères
ergonomie, accessibilité, performances de charge et disponibilité de production
ne sont pas réputés validés par les tests fonctionnels actuels.
