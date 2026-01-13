package dal;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

public class ConnectionProvider {

    private static final String CONFIG_FILE_NAME = "/db.settings";
    private final SQLServerDataSource ds;

    public ConnectionProvider() {
        ds = new SQLServerDataSource();
        try {
            loadProperties();
        } catch (IOException e) {
            System.err.println("Could not load '" + CONFIG_FILE_NAME + "'. Using fallback/hardcoded values or failing.");
            e.printStackTrace();
        }
    }

    private void loadProperties() throws IOException {
        Properties props = new Properties();

        try (InputStream is = getClass().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (is == null) {
                throw new IOException("Property file '" + CONFIG_FILE_NAME + "' not found in classpath.");
            }
            props.load(is);
        }

        ds.setServerName(props.getProperty("server"));
        ds.setDatabaseName(props.getProperty("database"));
        ds.setUser(props.getProperty("user"));
        ds.setPassword(props.getProperty("password"));
        ds.setPortNumber(Integer.parseInt(props.getProperty("port")));
        ds.setEncrypt(props.getProperty("encrypt"));

        ds.setTrustServerCertificate(Boolean.parseBoolean(props.getProperty("trustServerCertificate")));
        ds.setLoginTimeout(Integer.parseInt(props.getProperty("loginTimeout")));
    }

    public Connection getConnection() throws SQLServerException {
        return ds.getConnection();
    }
}