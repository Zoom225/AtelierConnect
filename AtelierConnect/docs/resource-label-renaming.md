# Renommage des centres et salles — 4 novembre 2026

**Évolution du 5 novembre :** les centres 1 et 2 portent maintenant les noms de
[Centre Bruxelles et Centre Namur](belgian-centre-cities.md). Les tableaux de ce
rapport décrivent la première étape du 4 novembre ; les noms des salles restent identiques.

Renommage explicitement demandé et appliqué à la base locale `atelierconnect_db`.
Seuls les sept champs `nom` ci-dessous ont changé. Les villes, adresses, horaires,
prix, durées, identifiants, relations, comptes et données métier sont conservés.

| Objet | ID | Ancien nom | Nom actuel |
| --- | --- | --- | --- |
| Centre | 1 | Padel Club Lyon | Centre Lyon |
| Centre | 2 | Padel Club Paris | Centre Paris |
| Salle, centre 1 | 1 | Court A | Salle A |
| Salle, centre 1 | 2 | Court B | Salle B |
| Salle, centre 1 | 3 | Court C | Salle C |
| Salle, centre 2 | 4 | Court 1 | Salle 1 |
| Salle, centre 2 | 5 | Court 2 | Salle 2 |

## Code et migration

`DataInitializer` reconnaît les noms actuels en priorité, ainsi que les noms
des bases neuves (par exemple `Salle Création A`) et les anciens noms. Il ne
renomme pas les ressources automatiquement au démarrage. Les trois versions
de nomenclature sont couvertes par un test paramétré du parcours de reprise.
Les fixtures de tests utilisent les noms de salles actuels.

Les scripts sous `scripts/migrations/` sont :

- `20261104_rename_demo_resource_labels.up.sql` : renommage vers les noms actuels.
- `20261104_rename_demo_resource_labels.down.sql` : retour aux anciens libellés.

Ils ciblent cette base historique Lyon/Paris par identifiant et nom attendu,
pas les centres de Bruxelles/Namur d'une base neuve. Un nom personnalisé, une
ressource absente, un rattachement inattendu ou une collision provoque un refus.
Chaque script est transactionnel et réexécutable. Les écritures sur les tables
sont verrouillées pendant la vérification. Les empreintes de toutes les lignes,
en excluant uniquement les sept champs autorisés, et l'état des séquences sont
comparés avant validation. Les mappings SQL et `ddl-auto=update` restent inchangés.

## Procédure opérateur

Le renommage est déjà appliqué à la base locale. Pour une autre copie de la
même base historique : arrêter son backend, réaliser une sauvegarde `pg_dump -Fc`,
la restaurer sur une base séparée et y vérifier la migration et son retour arrière.
Ne jamais restaurer par-dessus les données applicatives pour cette opération.

Depuis le dossier `AtelierConnect/`, après ces vérifications :

```powershell
.\scripts\set-local-environment.ps1
Get-Content .\scripts\migrations\20261104_rename_demo_resource_labels.up.sql -Raw -Encoding UTF8 |
    docker exec -i atelierconnect-db psql -X -v ON_ERROR_STOP=1 -U $env:POSTGRES_USER -d atelierconnect_db
if ($LASTEXITCODE -ne 0) { throw 'Renommage refusé : consulter la sortie SQL.' }
```

Le retour arrière utilise le fichier `.down.sql` par la même procédure, backend
arrêté. Il conserve également les données et refuse les libellés personnalisés.
Ne pas exécuter le retour arrière pour simplement actualiser l'affichage.

Redémarrer avec `AtelierConnect — Local` et `APP_DATA_INITIALIZE=false`, puis
actualiser le navigateur. Une reconnexion recharge les noms de centre stockés
dans une ancienne session. Les e-mails et mots de passe restent identiques.

## Preuves locales

- Sauvegarde protégée par les droits Windows, ignorée par Git :
  `.local/resource-labels/20261104_170654_263/before-labels.dump`.
- Copie restaurée : `atelierconnect_labels_20261104_170654_263` sur le conteneur
  de validation PostgreSQL 15 (5549), séparée de la base applicative.
- Toutes les lignes des neuf tables et les séquences correspondent à la source.
  Les contraintes restaurées sont équivalentes ; PostgreSQL reformule certains
  casts d'énumération lors de la restauration. Sur la source, les définitions
  brutes des contraintes sont strictement identiques avant et après renommage.
- Migration, répétition, retour arrière et refus d'un nom personnalisé vérifiés
  sur la copie avant l'application locale.
- Après application, résultat comparé à la copie répétée : seuls les sept noms
  changent ; les neuf tables, comptes, réservations et paiements sont préservés.
- Éléments de preuve, empreintes et journaux : même répertoire local, `summary.json`.
- 192 tests backend réussis sur la base isolée
  `atelierconnect_test_20261104_170558_606`, sans échec, erreur ou test ignoré.
- Après redémarrage, l'API renvoie les deux centres et cinq salles avec leurs
  nouveaux noms ; Swagger, l'API publique et les proxys 4200/56700 répondent HTTP 200.
- Contrôle Chromium en lecture seule des pages d'administration des centres et
  salles et du formulaire participant : nouveaux libellés affichés, sélection
  de Centre Paris / Salle 1 réussie, zéro ancien libellé visible et zéro erreur
  JavaScript. Captures locales `centres.png`, `salles.png`, `member-create.png`
  et rapport `browser-verification.json` dans le répertoire de preuve.
- Après redémarrage et contrôle navigateur, toutes les empreintes des neuf
  tables, séquences et contraintes correspondent toujours au résultat migré.

Les anciens mots conservés dans les scripts inverses, le test de compatibilité
et les rapports historiques documentent la transition ; ils ne sont plus les
libellés renvoyés pour ces centres et salles par la base locale.
