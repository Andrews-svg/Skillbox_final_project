package searchengine.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

public class MySQLConnectionTest {

    private static final String URL = "jdbc:mysql://localhost:3306/search_engine";
    private static final String USER =
            System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "defaultUser";
    private static final String PASSWORD =
            System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "defaultPass";

    @Test
    public void testDatabaseConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            assertNotNull(connection, "Соединение с базой данных не установлено!");
            System.out.println("Соединение с базой данных успешно установлено!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Ошибка соединения с базой данных: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("Соединение закрыто.");
                } catch (SQLException e) {
                    System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
                }
            }
        }
    }
}