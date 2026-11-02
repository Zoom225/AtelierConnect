# Démonstration de l'accès visiteur

Les centres existants 1 et 2 autorisent désormais les visiteurs
(`ouvert_visiteurs=true`), conformément au choix demandé pour la démonstration.
Les nouveaux centres créés par `DataInitializer` sont déjà ouverts aux visiteurs.
Un administrateur peut modifier ce réglage dans la gestion des centres ; il
n'est pas réinitialisé automatiquement au démarrage.

## Parcours autorisé

Se connecter en participant avec le matricule `L10001` et son mot de passe
configuré, puis ouvrir la création d'atelier. Le formulaire propose uniquement
les centres autorisés aux visiteurs. Les dates doivent être futures, dans les
sept jours permis au profil VISITEUR, avec un créneau disponible et respectant
les horaires d'ouverture.

Le 2 novembre 2026, ce parcours a été vérifié depuis le navigateur sur la base
locale : atelier public **5**, salle **1**, le **3 novembre 2026 de 09:00 à
11:00**, réponse **HTTP 201**, puis accès à la page des réservations.
Cet atelier de démonstration est conservé en base avec la réservation et le
paiement en attente de l'organisateur.

## Parcours refusé

Le compte `L10002` conserve sa suspension active. Il peut se connecter mais ne
peut pas créer d'atelier. Le refus vérifié est **HTTP 400** avec le message
« Vous avez une suspension active et ne pouvez pas créer d'atelier. »
Ce refus métier est attendu ; les pénalités n'ont pas été supprimées.

## Configuration explicite

Le script `scripts/demo/open-centres-to-visitors.sql` configure uniquement
l'accès visiteur des centres 1 et 2, dans une transaction. Il ne modifie aucun
solde, aucune pénalité et aucune réservation. Les anciennes valeurs ont été
conservées dans un fichier local `visitor-access-before-*.json` sous
`.local/diagnostics` avant l'application. Le script n'est jamais exécuté
automatiquement par le backend.
