package database;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database Initializer - Utility for database setup and verification
 * Provides methods to initialize default data and verify database integrity
 */
public class DatabaseInitializer {
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    
    /**
     * Initialize default system users (admin, cashier)
     */
    public static void initializeDefaultUsers() {
        try {
            // Check if users already exist
            User admin = UserDAO.getUserByUsername("admin");
            User cashier = UserDAO.getUserByUsername("cashier");
            
            if (admin == null) {
                User adminUser = new User("admin", "admin123", "Administrator", "ADMIN");
                adminUser.setEmail("admin@bereeze.com");
                int adminId = UserDAO.addUser(adminUser);
                LOGGER.log(Level.INFO, "Default admin user created with ID: " + adminId);
            } else {
                LOGGER.log(Level.INFO, "Admin user already exists");
            }
            
            if (cashier == null) {
                User cashierUser = new User("cashier", "cashier123", "Cashier User", "CASHIER");
                cashierUser.setEmail("cashier@bereeze.com");
                int cashierId = UserDAO.addUser(cashierUser);
                LOGGER.log(Level.INFO, "Default cashier user created with ID: " + cashierId);
            } else {
                LOGGER.log(Level.INFO, "Cashier user already exists");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing default users: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify database connection and tables exist
     */
    public static boolean verifyDatabaseIntegrity() {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            
            String[] tables = {"item_master", "customer", "user", "bill", "bill_item", "transaction", "audit_log", "cash_register", "purchase_bill", "purchase_bill_item", "purchase_payment"};
            boolean allTablesExist = true;
            
            // Add column migration for item_master.reorderLevel if it doesn't exist
            try {
                ResultSet rsCol = metadata.getColumns(null, null, "item_master", "reorderLevel");
                if (!rsCol.next()) {
                    LOGGER.log(Level.INFO, "Adding reorderLevel column to item_master...");
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE item_master ADD COLUMN reorderLevel INT NOT NULL DEFAULT 10");
                        LOGGER.log(Level.INFO, "Successfully added reorderLevel column.");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error checking/adding reorderLevel column: " + e.getMessage());
            }
            
            for (String table : tables) {
                ResultSet rs = metadata.getTables(null, null, table, new String[]{"TABLE"});
                if (!rs.next()) {
                    LOGGER.log(Level.WARNING, "Table not found: " + table);
                    allTablesExist = false;
                } else {
                    LOGGER.log(Level.INFO, "âœ“ Table verified: " + table);
                }
                rs.close();
            }
            
            if (allTablesExist) {
                LOGGER.log(Level.INFO, "âœ“ All required tables exist");
                return true;
            } else {
                LOGGER.log(Level.SEVERE, "âœ— Some required tables are missing. Please run schema.sql");
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database integrity check failed: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get database statistics
     */
    public static void printDatabaseStats() {
        try {
            int userCount = UserDAO.getAllUsers().size();
            int customerCount = CustomerDAO.getAllCustomers().size();
            int itemCount = ItemMasterDAO.getAllItems().size();
            
            System.out.println("\n========== DATABASE STATISTICS ==========");
            System.out.println("Users: " + userCount);
            System.out.println("Customers: " + customerCount);
            System.out.println("Items: " + itemCount);
            System.out.println("Connection Pool: " + DBConnection.getPoolStats());
            System.out.println("==========================================\n");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error printing database stats: " + e.getMessage(), e);
        }
    }
    
    /**
     * Full database initialization
     */
    public static boolean initializeDatabase() {
        LOGGER.log(Level.INFO, "Starting database initialization...");
        
        // Test connection
        if (!DBConnection.testConnection()) {
            LOGGER.log(Level.SEVERE, "Cannot connect to database. Please check your connection settings.");
            return false;
        }
        LOGGER.log(Level.INFO, "âœ“ Database connection successful");
        
        // Verify tables
        if (!verifyDatabaseIntegrity()) {
            return false;
        }
        
        // Initialize default users
        initializeDefaultUsers();
        
        LOGGER.log(Level.INFO, "âœ“ Database initialization complete");
        printDatabaseStats();
        
        return true;
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        System.out.println("Bereeze Footwear - Database Initializer");
        System.out.println("========================================\n");
        
        if (initializeDatabase()) {
            System.out.println("âœ“ Database is ready to use!");
        } else {
            System.out.println("âœ— Database initialization failed. Please check the logs.");
        }
        
        // Close connection pool
        DBConnection.closeConnectionPool();
    }
}
