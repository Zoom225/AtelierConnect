package com.atelierconnect.config;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SchemaNamingGuardTest {
    @Test
    void legacyTablesPreventHibernateFromCreatingParallelTables() throws Exception {
        SchemaNamingGuard guard = guardWithTables("matches");
        assertThatThrownBy(() -> guard.customize(new HashMap<>()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("20261102_align_entity_names.up.sql");
    }

    @Test
    void migratedSchemaCanStart() throws Exception {
        SchemaNamingGuard guard = guardWithTables("ateliers", "participants", "centres", "salles");
        assertThatCode(() -> guard.customize(new HashMap<>())).doesNotThrowAnyException();
    }

    @Test
    void emptyIsolatedDatabaseCanBeInitializedByHibernate() throws Exception {
        SchemaNamingGuard guard = guardWithTables();
        assertThatCode(() -> guard.customize(new HashMap<>())).doesNotThrowAnyException();
    }

    private SchemaNamingGuard guardWithTables(String... names) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.getSchema()).thenReturn("public");
        when(metadata.getTables(isNull(), eq("public"), isNull(), any(String[].class))).thenReturn(tables);
        int[] position = {-1};
        when(tables.next()).thenAnswer(invocation -> ++position[0] < names.length);
        when(tables.getString("TABLE_NAME")).thenAnswer(invocation -> names[position[0]]);
        return new SchemaNamingGuard(dataSource);
    }
}
