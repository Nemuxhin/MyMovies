package dal;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionProvider {

    private final SQLServerDataSource ds;

    public ConnectionProvider() {
        ds = new SQLServerDataSource();
        // Set the data from your screenshot
        ds.setServerName("10.176.111.34");
        ds.setDatabaseName("MyMovie");
        ds.setPortNumber(1433);
        ds.setUser("CS2025b_e_9");
        ds.setPassword("YOUR_PASSWORD_HERE"); // <--- WRITE YOUR REAL PASSWORD HERE

        // Settings for school servers
        ds.setEncrypt("true");
        ds.setTrustServerCertificate(true);
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}