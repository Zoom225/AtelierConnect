package com.atelierconnect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/** Reject the old schema before Hibernate update can create parallel empty tables. */
@Component
@RequiredArgsConstructor
public class SchemaNamingGuard implements HibernatePropertiesCustomizer {
    private static final Set<String> LEGACY_TABLES = Set.of("matches", "membres", "sites", "terrains");
    private final DataSource dataSource;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        try (var connection = dataSource.getConnection();
             var tables = connection.getMetaData().getTables(
                     connection.getCatalog(), connection.getSchema(), null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (LEGACY_TABLES.contains(tables.getString("TABLE_NAME"))) {
                    throw new IllegalStateException("Ancien schema SQL detecte. Arreter le backend, verifier une sauvegarde "
                            + "et appliquer scripts/migrations/20261102_align_entity_names.up.sql avant de redemarrer. "
                            + "Aucune table parallele ne sera creee automatiquement.");
                }
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("Impossible de verifier les noms du schema SQL avant Hibernate.", failure);
        }
    }
}
