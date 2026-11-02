import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit, one-off reset for the local demonstration database only. */
class SetLocalParticipantPasswords {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || !url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):5441/atelierconnect_db")) {
            throw new IllegalArgumentException("Only the local application database is allowed.");
        }
        String password = System.getenv("APP_DEMO_MEMBER_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("APP_DEMO_MEMBER_PASSWORD is required.");
        }
        Properties properties = new Properties();
        properties.setProperty("user", System.getenv("POSTGRES_USER"));
        properties.setProperty("password", System.getenv("POSTGRES_PASSWORD"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "30");
        var encoder = new BCryptPasswordEncoder();
        try (var connection = DriverManager.getConnection(url, properties)) {
            connection.setAutoCommit(false);
            try {
                int total = 0;
                int changed = 0;
                try (var select = connection.prepareStatement("SELECT id, password_hash FROM participants ORDER BY id FOR UPDATE");
                     var update = connection.prepareStatement("UPDATE participants SET password_hash = ? WHERE id = ?");
                     var rows = select.executeQuery()) {
                    while (rows.next()) {
                        total++;
                        String oldHash = rows.getString("password_hash");
                        if (oldHash != null && encoder.matches(password, oldHash)) continue;
                        update.setString(1, encoder.encode(password));
                        update.setLong(2, rows.getLong("id"));
                        if (update.executeUpdate() != 1) throw new IllegalStateException("Participant update failed.");
                        changed++;
                    }
                }
                int verified = 0;
                try (var select = connection.prepareStatement("SELECT password_hash FROM participants");
                     var rows = select.executeQuery()) {
                    while (rows.next()) {
                        if (!encoder.matches(password, rows.getString(1))) {
                            throw new IllegalStateException("Password verification failed; rolling back.");
                        }
                        verified++;
                    }
                }
                if (verified != total) throw new IllegalStateException("Participant count changed; rolling back.");
                connection.commit();
                System.out.println("PARTICIPANTS=" + total + "; UPDATED=" + changed + "; VERIFIED=" + verified);
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException failure) {
            // Do not include server messages or connection properties in logs.
            System.err.println("PARTICIPANT_RESET_FAILED SQLSTATE=" + failure.getSQLState());
            System.exit(1);
        }
    }
}
