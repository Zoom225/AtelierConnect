# Noms des centres : villes belges — 5 novembre 2026

Le propriétaire a demandé deux noms de villes belges distincts. Le choix est :

| ID conservé | Ancien nom | Nouveau nom | Salles rattachées |
| --- | --- | --- | --- |
| 1 | Centre Lyon | Centre Bruxelles | Salle A, Salle B, Salle C |
| 2 | Centre Paris | Centre Namur | Salle 1, Salle 2 |

Seuls les champs `nom` de ces deux centres sont concernés. Les adresses,
horaires, tarifs, durées, salles, identifiants, comptes et réservations restent
conservés. Les e-mails historiques des administrateurs ne sont pas des noms de
centres ; ils restent valides pour se connecter.

`DataInitializer` utilise ces deux mêmes noms pour les bases neuves, avec les
adresses de démonstration belges déjà prévues dans l'initialiseur. Il ne renomme
pas automatiquement les centres d'une base existante.

## Migration explicite

Scripts : `scripts/migrations/20261105_rename_centres_belgian_cities.up.sql`
et `20261105_rename_centres_belgian_cities.down.sql`.

Ils vérifient les identifiants, anciens noms et éventuelles collisions. La
transaction compare les empreintes des lignes de toutes les tables en excluant
uniquement les deux champs de nom autorisés, ainsi que l'état des séquences.
Toute incohérence annule l'opération. La réexécution est acceptée. Le retour
arrière restitue les noms Centre Lyon / Centre Paris, sans toucher aux données.
Les mappings SQL et `spring.jpa.hibernate.ddl-auto=update` restent inchangés.

Pour une autre copie de cette même base historique : arrêter son backend,
réaliser une sauvegarde `pg_dump -Fc`, la restaurer sur une base séparée et
y vérifier la migration et son retour arrière avant application.
Depuis `AtelierConnect/`, après ces contrôles :

```powershell
.\scripts\set-local-environment.ps1
Get-Content .\scripts\migrations\20261105_rename_centres_belgian_cities.up.sql -Raw -Encoding UTF8 |
    docker exec -i atelierconnect-db psql -X -v ON_ERROR_STOP=1 -U $env:POSTGRES_USER -d atelierconnect_db
if ($LASTEXITCODE -ne 0) { throw 'Renommage refusé : consulter la sortie SQL.' }
```

Le retour arrière emploie le fichier `.down.sql` par la même procédure.
Redémarrer avec `AtelierConnect — Local` et `APP_DATA_INITIALIZE=false`.
Actualiser le navigateur ; une reconnexion recharge également le nom de centre
enregistré dans une ancienne session.

## Validation locale

- Sauvegarde protégée par les droits Windows :
  `.local/centre-cities/20261105_181558_649/before-cities.dump`, ignorée par Git.
- Copie restaurée dédiée : `atelierconnect_cities_20261105_181558_649`,
  sur le conteneur PostgreSQL de validation (5549).
- Migration, réexécution, retour arrière et refus d'un nom personnalisé
  vérifiés sur cette copie avant l'application locale.
- Seuls les deux champs `centres.nom` changent : les empreintes des autres
  données des neuf tables, des séquences et des contraintes sont préservées.
- 192 tests backend réussis sur la base isolée
  `atelierconnect_test_20261105_181512_594`, aucun échec, erreur ou test ignoré.
- Empreintes et journaux conservés dans le même répertoire local, dont
  `summary.json`, `source-before.json` et `source-after.json`.
- Après redémarrage, l'API expose `Centre Bruxelles` et `Centre Namur` ; Swagger,
  API publique et proxys Angular 4200/56700 répondent HTTP 200.
- Trois pages vérifiées en Chromium : centres, salles et création participant.
  Les deux noms sont affichés, les salles restent rattachées aux mêmes centres,
  et la sélection de Centre Namur / Salle 1 fonctionne sans erreur JavaScript.
- Les empreintes des neuf tables, séquences et contraintes sont encore identiques
  à l'état migré après redémarrage et vérification navigateur. Les captures et
  `browser-verification.json` restent dans le répertoire de preuve local.
