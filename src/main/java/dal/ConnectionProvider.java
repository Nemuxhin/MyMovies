package dal;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

/**
 * Provides database connections using configuration loaded from a properties file.
 * This class centralizes all database connection settings for the application.
 */
public class ConnectionProvider {

    // Name and location of the configuration file inside resources
    private static final String CONFIG_FILE_NAME = "/db.settings";

    // SQL Server data source used to create connections
    private final SQLServerDataSource ds;

    /**
     * Initializes the data source and loads database configuration.
     */
    public ConnectionProvider() {
        ds = new SQLServerDataSource();
        try {
            loadProperties();
        } catch (IOException e) {
            // If the configuration cannot be loaded, the application cannot connect to the database
            System.err.println("Could not load '" + CONFIG_FILE_NAME + "'. Database connection will fail.");
            e.printStackTrace();
        }
    }

    /**
     * Loads database connection properties from the db.settings file.
     */
    private void loadProperties() throws IOException {

        Properties props = new Properties();

        // Load properties file from classpath
        try (InputStream is = getClass().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (is == null) {
                throw new IOException(
                        "Property file '" + CONFIG_FILE_NAME + "' not found in classpath."
                );
            }
            props.load(is);
        }

        // Basic connection settings
        ds.setServerName(props.getProperty("server"));
        ds.setDatabaseName(props.getProperty("database"));
        ds.setUser(props.getProperty("user"));
        ds.setPassword(props.getProperty("password"));

        // Optional connection settings
        ds.setPortNumber(Integer.parseInt(props.getProperty("port", "1433")));
        ds.setEncrypt(Boolean.parseBoolean(props.getProperty("encrypt", "true")));
        ds.setTrustServerCertificate(
                Boolean.parseBoolean(props.getProperty("trustServerCertificate", "true"))
        );
        ds.setLoginTimeout(Integer.parseInt(props.getProperty("loginTimeout", "5")));
    }

    /**
     * Returns a new database connection.
     *
     * @return a valid SQL Server connection
     * @throws SQLServerException if the connection cannot be established
     */
    public Connection getConnection() throws SQLServerException {
        return ds.getConnection();
    }
}
