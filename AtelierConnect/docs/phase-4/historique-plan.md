# Données historiques : inventaire et plan soumis à décision

Audit en lecture seule du 1er novembre 2026, 12:11 (Europe/Paris).
**Aucune migration, modification de prix, annulation ou archivage n'a été exécuté.**

## Sauvegarde et copie

Base source : `atelierconnect_db`, conteneur applicatif sain sur 5441.
Sauvegarde PostgreSQL au format custom : `.local/recette/recette_20261101_121059.dump`.
Copie restaurée : `atelierconnect_recette_snapshot_20261101_121059`, sur le serveur
de test indépendant, port 5549. Les identifiants et mots de passe restent privés.
Le JSON d'inventaire local est dans `.local/recette/historical-inventory.json`.

Les propositions ci-dessous sont des décisions à faire valider, pas des opérations
SQL à lancer. Refaire une sauvegarde et l'inventaire juste avant toute intervention.

## Quatre ateliers hérités, tous à venir au moment de l'audit

Tous durent 90 minutes, ont un prix total de 60 EUR et un prix individuel de 15 EUR.
Les compteurs stockés correspondent aux réservations confirmées ; il n'y a pas de
dépassement de six places ni de compteur à corriger artificiellement.

| ID | Date / horaire actuel | Type / état | Salle / organisateur (IDs) | Confirmés / attente / annulés | Paiements validés |
| --- | --- | --- | --- | --- | --- |
| 1 | 02/11/2026, 10:00–11:30 | PUBLIC / PLANIFIE | 1 / 1 | 0 / 1 / 0 | 0 EUR |
| 2 | 03/11/2026, 14:00–15:30 | PRIVE / PLANIFIE | 2 / 3 | 1 / 0 / 0 | 15 EUR |
| 3 | 04/11/2026, 18:00–19:30 | PUBLIC / PLANIFIE | 4 / 4 | 2 / 0 / 0 | 30 EUR |
| 4 | 05/11/2026, 16:00–17:30 | PUBLIC / COMPLET | 3 / 1 | 4 / 0 / 0 | 60 EUR |

Il y a respectivement 1, 1, 2 et 4 participants distincts. Aucun des quatre ateliers
n'a d'accord d'ouverture publique ni de date de consentement. Ne pas créer un accord
implicite pour le privé #2. Les sept paiements validés totalisent 105 EUR et doivent
rester traçables. Le paiement en attente de #1 vaut 15 EUR.

## Proposition individuelle

| ID | Recommandation proposée | Conditions et impacts |
| --- | --- | --- |
| 1 | Migration métier possible avant séance, après accord de l'organisateur | Fin proposée 12:00 ; total 90, individuel 15 ; conserver réservation et paiement en attente. Contrôler horaires, pause et créneaux jusqu'à 12:20. Arbitrer l'impayé J−1, sans créer de dette rétroactive automatiquement. |
| 2 | Migration avec accord explicite de l'organisateur et du participant déjà confirmé | Fin proposée 16:00 ; total 90, individuel 15 ; garder PRIVE, la place confirmée et le paiement de 15. L'accord de changement de durée n'est pas un consentement à l'ouverture publique. |
| 3 | Migration après validation par l'organisateur et information des deux inscrits | Fin proposée 20:00 ; total 90, individuel 15 ; préserver deux réservations confirmées et 30 EUR validés. Vérifier la salle et les horaires jusqu'à 20:20. |
| 4 | Décision métier obligatoire entre conservation comme séance héritée fermée ou migration avec réouverture | Fin proposée en cas de migration : 18:00, total 90, individuel 15. Garder les quatre places et les 60 EUR payés. Passer de COMPLET à PLANIFIE ouvrirait deux places : ne jamais le faire sans accord de l'organisateur. |

Le prix individuel de 15 EUR n'augmente pas : aucune surcharge ni restitution des
paiements existants n'est proposée automatiquement. L'évolution du total reflète
le modèle de six places ; elle ne doit pas être confondue avec un débit bancaire.
La plateforme ne réalise de toute façon aucune transaction bancaire réelle.

**Conservation temporaire** signifie garder les lignes intactes en attendant une
décision, pas considérer leur durée conforme aux nouveaux ateliers.
**Archivage** n'est pas recommandé automatiquement pour des séances futures déjà
réservées ; après leur tenue, une politique d'archivage pourra conserver les faits
historiques. Aucun champ ni mécanisme d'archivage nouveau n'a été installé.

Une suspension des nouvelles inscriptions sur ces séances serait elle-même une
décision d'exploitation à approuver ; elle n'a pas été ajoutée silencieusement.

## Référentiels portant encore les anciens noms

| Objet / ID | Valeur source | Renommage éventuellement proposé, à approuver |
| --- | --- | --- |
| Centre 1 | Padel Club Lyon | Centre Lyon |
| Centre 2 | Padel Club Paris | Centre Paris |
| Salles 1 / 2 / 3 | Court A / Court B / Court C | Salle A / Salle B / Salle C |
| Salles 4 / 5 | Court 1 / Court 2 | Salle 1 / Salle 2 |

Ces chaînes sont des données, pas des libellés codés de l'interface. Elles sont
conservées pour respecter l'interdiction de modifier l'historique sans validation.
Les identifiants et relations doivent rester identiques lors d'un renommage approuvé.
Ne pas transformer Lyon/Paris en Bruxelles/Namur : ce serait une autre modification
fonctionnelle, non justifiée par un simple changement de vocabulaire.

Les anciennes durées de centre et les prix de salle restent également stockés.
Le formulaire de création affiche maintenant la règle du nouvel atelier (90 EUR)
et ne réutilise plus le tarif historique d'une salle.

## Garde-fous du futur plan d'exécution

1. Obtenir une décision explicite par atelier et pour les sept noms de référentiel.
2. Sauvegarder, restaurer une copie récente et comparer les neuf tables avant essai.
3. Vérifier les conflits sur les durées cibles et pauses de 20 minutes, les fermetures,
   les horaires et les inscriptions intervenues depuis cet inventaire.
4. Préparer des mises à jour ciblées par ID et ancienne valeur attendue, dans une
   transaction ; faire approuver le résultat sur copie et le plan de restauration.
5. Conserver les identifiants de réservations/paiements, montants validés, catégories,
   mots de passe et consentements. Aucun DROP, TRUNCATE, volume supprimé ou Flyway.
6. Faire autoriser séparément l'exécution sur la base source et informer les acteurs
   concernés par les changements de durée ou de disponibilité.

Le principal risque d'une correction immédiate serait de changer une séance déjà
payée sans accord. Le principal risque de ne rien arbitrer est de continuer à
présenter des séances futures de 90 minutes dans une application annonçant 120 minutes.
