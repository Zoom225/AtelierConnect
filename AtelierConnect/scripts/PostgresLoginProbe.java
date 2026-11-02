import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Read-only JDBC probe; secrets come exclusively from process environment. */
class PostgresLoginProbe {
    public static void main(String[] args) throws Exception {
        var properties = new Properties();
        properties.setProperty("user", System.getenv("POSTGRES_USER"));
        properties.setProperty("password", System.getenv("POSTGRES_PASSWORD"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "5");
        try (var connection = DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"), properties)) {
            connection.setReadOnly(true);
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT current_database(), extract(epoch from pg_postmaster_start_time())::text")) {
                rows.next();
                if (!"atelierconnect_db".equals(rows.getString(1))) {
                    System.err.println("JDBC_DATABASE_MISMATCH");
                    System.exit(2);
                }
                System.out.println("JDBC_OK");
                System.out.println("SERVER_STARTED_AT=" + rows.getString(2));
            }
        } catch (SQLException failure) {
            System.err.println("JDBC_FAILED SQLSTATE=" + failure.getSQLState());
            String reason = switch (String.valueOf(failure.getSQLState())) {
                case "28P01" -> "PASSWORD_AUTHENTICATION_FAILED";
                case "28000" -> "ROLE_OR_ACCESS_RULE_REJECTED";
                case "3D000" -> "DATABASE_NOT_FOUND";
                case "08001", "08006" -> "NETWORK_PORT_OR_SERVER_UNAVAILABLE";
                default -> "SQL_ERROR";
            };
            // Never print exception messages: a server may include sensitive input.
            System.err.println("JDBC_REASON=" + reason);
            System.exit(1);
        }
    }
}
