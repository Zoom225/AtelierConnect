package com.atelierconnect.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class IsolatedPostgresTest {
    @ParameterizedTest
    @CsvSource({
            "jdbc:postgresql://localhost:5549/atelierconnect_test_20261108_120000_123,false,atelier_validation,true",
            "jdbc:postgresql://127.0.0.1:5550/atelierconnect_test_20261108_120000_123,false,atelier_validation,true",
            "jdbc:postgresql://localhost:5441/atelierconnect_ci,true,atelier_ci,true",
            "jdbc:postgresql://127.0.0.1:5441/atelierconnect_ci,true,atelier_ci,true",
            "jdbc:postgresql://localhost:5441/atelierconnect_ci,false,atelier_ci,false",
            "jdbc:postgresql://localhost:5441/atelierconnect_ci,true,atelierconnect,false",
            "jdbc:postgresql://localhost:5441/atelierconnect_db,true,atelier_ci,false",
            "jdbc:postgresql://localhost:5550/atelierconnect_db,false,atelier_validation,false",
            "jdbc:postgresql://remote:5550/atelierconnect_test_20261108_120000_123,false,atelier_validation,false",
            ",false,atelier_validation,false"
    })
    void onlyDedicatedLocalOrGithubTestDatabasesAreAllowed(String url, boolean github, String user, boolean allowed) {
        assertThat(IsolatedPostgres.isAllowed(url, github, user)).isEqualTo(allowed);
    }
}
