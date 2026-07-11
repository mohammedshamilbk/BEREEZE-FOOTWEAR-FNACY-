import database.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class ResetDB {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Disabling foreign key checks...");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            System.out.println("Truncating tables...");
            stmt.execute("TRUNCATE TABLE audit_log");
            stmt.execute("TRUNCATE TABLE supplier_transaction");
            stmt.execute("TRUNCATE TABLE transaction");
            stmt.execute("TRUNCATE TABLE bill_item");
            stmt.execute("TRUNCATE TABLE bill");
            stmt.execute("TRUNCATE TABLE supplier");
            stmt.execute("TRUNCATE TABLE user");
            stmt.execute("TRUNCATE TABLE customer");
            stmt.execute("TRUNCATE TABLE item_master");
            
            System.out.println("Enabling foreign key checks...");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            System.out.println("Inserting default admin and cashier users...");
            stmt.execute("INSERT IGNORE INTO user (userId, username, password, fullName, role, email, status) VALUES (1, 'admin', 'admin123', 'Administrator', 'ADMIN', 'admin@bereeze.com', 'ACTIVE')");
            stmt.execute("INSERT IGNORE INTO user (userId, username, password, fullName, role, email, status) VALUES (2, 'cashier', 'cashier123', 'Cashier User', 'CASHIER', 'cashier@bereeze.com', 'ACTIVE')");
            
            System.out.println("Database reset successful!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
