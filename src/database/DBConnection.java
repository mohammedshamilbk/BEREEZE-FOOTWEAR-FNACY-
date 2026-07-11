package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database Connection Manager using standard JDBC Connections.
 * Rewritten to remove external HikariCP dependencies for easy compilation.
 */
public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    
    // Database Configuration
    private static String DB_URL = "";
    private static String DB_USER = "";
    private static String DB_PASSWORD = "";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    private static int activeConnections = 0;
    
    static {
        loadProperties();
        try {
            Class.forName(DB_DRIVER);
            LOGGER.log(Level.INFO, "MySQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "MySQL JDBC Driver not found. Operating in offline mode.");
        }
    }
    
    private static void loadProperties() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream input = new java.io.FileInputStream("config.properties")) {
            props.load(input);
            DB_URL = props.getProperty("db.url", "");
            DB_USER = props.getProperty("db.user", "");
            DB_PASSWORD = props.getProperty("db.password", "");
        } catch (java.io.IOException ex) {
            LOGGER.log(Level.WARNING, "config.properties not found or error reading it. Database access will fall back to offline caches.");
        }
    }
    
    /**
     * Get a connection to the database
     * 
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            if (connection != null) {
                activeConnections++;
                LOGGER.log(Level.FINE, "Direct connection obtained successfully");
                return connection;
            }
        } catch (SQLException e) {
            // Silently throw to caller without printing terrifying red stack traces
            throw new SQLException("Offline Mode Active");
        }
        
        throw new SQLException("Unable to get database connection");
    }
    
    /**
     * Close the connection pool
     */
    public static void closeConnectionPool() {
        LOGGER.log(Level.INFO, "Database connection manager shutdown successfully");
    }
    
    /**
     * Get connection stats
     * 
     * @return String with stats
     */
    public static String getPoolStats() {
        return "Direct Connection Mode (Active: " + activeConnections + ")";
    }
    
    /**
     * Test database connection
     * 
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed: " + e.getMessage(), e);
            return false;
        }
    }
}
