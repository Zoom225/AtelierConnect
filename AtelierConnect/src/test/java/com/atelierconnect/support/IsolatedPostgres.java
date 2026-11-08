package com.atelierconnect.support;

import org.springframework.test.context.DynamicPropertyRegistry;

/** Refuse la base applicative avant toute initialisation du contexte de test. */
public final class IsolatedPostgres {
    private IsolatedPostgres() { }

    public static void configure(DynamicPropertyRegistry properties) {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        boolean githubActions = "true".equals(System.getenv("GITHUB_ACTIONS"));
        if (!isAllowed(url, githubActions, System.getenv("POSTGRES_USER"))) {
            throw new IllegalStateException("Use scripts/test-backend-isolated.ps1 or the dedicated GitHub Actions service: application databases are forbidden for these tests.");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("APP_PRIVATE_PENALTY_CRON", () -> "-");
    }

    static boolean isAllowed(String url, boolean githubActions, String user) {
        if (url == null) return false;
        boolean localTest = url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):(5549|5550)/atelierconnect_test_[0-9]{8}_[0-9]{6}_[0-9]{3}");
        boolean githubTest = githubActions && "atelier_ci".equals(user)
                && url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):5441/atelierconnect_ci");
        return localTest || githubTest;
    }
}
