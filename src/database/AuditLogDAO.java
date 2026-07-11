package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for audit logging.
 * Logs sensitive business operations to the audit_log table.
 */
public class AuditLogDAO {
    private static final Logger LOGGER = Logger.getLogger(AuditLogDAO.class.getName());

    /**
     * Record an audit log entry in the database.
     * 
     * @param userId The ID of the user performing the action, or <= 0 if system/unauthenticated
     * @param action The name of the action performed (e.g. LOGIN_SUCCESS, CANCEL_BILL)
     * @param tableName The database table affected
     * @param recordId The primary key ID of the affected record, or -1 if not applicable
     * @param oldValue The previous state/value before the action (optional)
     * @param newValue The new state/value after the action (optional)
     */
    public static void log(int userId, String action, String tableName, int recordId, String oldValue, String newValue) {
        String sql = "INSERT INTO audit_log (userId, action, tableName, recordId, oldValue, newValue, actionDate) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (userId <= 0) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(1, userId);
            }
            pstmt.setString(2, action);
            pstmt.setString(3, tableName);
            pstmt.setInt(4, recordId);
            pstmt.setString(5, oldValue);
            pstmt.setString(6, newValue);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting audit log: " + e.getMessage(), e);
        }
    }
}
