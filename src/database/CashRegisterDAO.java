package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for Cash Register operations
 */
public class CashRegisterDAO {
    private static final Logger LOGGER = Logger.getLogger(CashRegisterDAO.class.getName());

    /**
     * Opens today's register if it doesn't exist.
     * Uses the last closed register's closing balance as today's opening balance.
     */
    public static void openTodayRegister(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            // Check if today's register already exists
            String checkSql = "SELECT register_id FROM cash_register WHERE register_date = CURDATE()";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    LOGGER.log(Level.INFO, "Register for today is already open/created.");
                    return; // Already exists
                }
            }

            // Get last closing balance
            double openingBalance = getLastClosingBalance();

            // Create today's register
            String insertSql = "INSERT INTO cash_register (register_date, opening_balance, status, opened_by) VALUES (CURDATE(), ?, 'OPEN', ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setDouble(1, openingBalance);
                insertStmt.setInt(2, userId);
                insertStmt.executeUpdate();
                LOGGER.log(Level.INFO, "Opened new register for today with opening balance: " + openingBalance);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error opening today's register: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the closing balance of the most recent closed register.
     */
    public static double getLastClosingBalance() {
        String sql = "SELECT closing_balance FROM cash_register WHERE status = 'CLOSED' ORDER BY register_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("closing_balance");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting last closing balance: " + e.getMessage(), e);
        }
        return 0.0;
    }

    /**
     * Gets today's opening balance.
     */
    public static double getTodayOpeningBalance() {
        String sql = "SELECT opening_balance FROM cash_register WHERE register_date = CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("opening_balance");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting today's opening balance: " + e.getMessage(), e);
        }
        return 0.0;
    }

    /**
     * Closes today's register, calculates the closing balance, and marks it CLOSED.
     */
    public static void closeTodayRegister(int userId, double cashSales, double cashIn, double cashOut) {
        try (Connection conn = DBConnection.getConnection()) {
            double openingBalance = getTodayOpeningBalance();
            double closingBalance = openingBalance + cashSales + cashIn - cashOut;

            String sql = "UPDATE cash_register SET closing_balance = ?, cash_sales = ?, cash_in = ?, cash_out = ?, status = 'CLOSED', closed_by = ?, closed_at = CURRENT_TIMESTAMP WHERE register_date = CURDATE()";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, closingBalance);
                stmt.setDouble(2, cashSales);
                stmt.setDouble(3, cashIn);
                stmt.setDouble(4, cashOut);
                stmt.setInt(5, userId);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    LOGGER.log(Level.INFO, "Register closed successfully. Closing balance: " + closingBalance);
                } else {
                    LOGGER.log(Level.WARNING, "Failed to close register. It may not exist for today.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing today's register: " + e.getMessage(), e);
        }
    }

    /**
     * Returns total CASH-only sales for today from the bill table.
     * <ul>
     *   <li>Filters {@code paymentMode = 'CASH'} (case-insensitive)</li>
     *   <li>Excludes CANCELLED bills</li>
     *   <li>COALESCE ensures 0.0 is returned when there are no rows</li>
     * </ul>
     *
     * @return today's cash sales total, or 0.0 if none
     */
    public static double getTodayCashSales() {
        String sql = "SELECT COALESCE(SUM(totalAmount), 0) AS total FROM bill " +
                     "WHERE DATE(billDate) = CURDATE() " +
                     "  AND LOWER(paymentMode) = 'cash' " +
                     "  AND billType = 'SALES' " +
                     "  AND status != 'CANCELLED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating today's cash sales: " + e.getMessage(), e);
        }
        return 0.0;
    }

    /**
     * Calculates the live closing balance preview without writing to the database.
     */
    public static double getLiveClosingBalancePreview() {
        double openingBalance = 0;
        double cashIn = 0;
        double cashOut = 0;
        String sql = "SELECT opening_balance, cash_in, cash_out FROM cash_register WHERE register_date = CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                openingBalance = rs.getDouble("opening_balance");
                cashIn = rs.getDouble("cash_in");
                cashOut = rs.getDouble("cash_out");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting register details for preview: " + e.getMessage(), e);
        }
        
        double cashSales = getTodayCashSales();
        return openingBalance + cashSales + cashIn - cashOut;
    }

    /**
     * Returns today's closing balance.
     * <ul>
     *   <li>If the register is {@code CLOSED}: returns the locked-in {@code closing_balance} from the DB.</li>
     *   <li>If the register is {@code OPEN}: returns a live preview
     *       ({@code opening + cash_sales_from_bills + cash_in - cash_out}).</li>
     *   <li>If no register exists for today: returns 0.0.</li>
     * </ul>
     */
    public static double getTodayClosingBalance() {
        String sql = "SELECT closing_balance, status FROM cash_register WHERE register_date = CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                String status = rs.getString("status");
                if ("CLOSED".equalsIgnoreCase(status)) {
                    return rs.getDouble("closing_balance"); // ← locked-in final value
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting today's closing balance: " + e.getMessage(), e);
        }
        // OPEN or no register yet → return live running preview
        return getLiveClosingBalancePreview();
    }

    /**
     * Returns today's register status: "OPEN", "CLOSED", or "NOT_OPENED"
     * if no register row exists yet for today.
     */
    public static String getTodayRegisterStatus() {
        String sql = "SELECT status FROM cash_register WHERE register_date = CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("status");   // "OPEN" or "CLOSED"
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting today's register status: " + e.getMessage(), e);
        }
        return "NOT_OPENED";
    }

    /**
     * Returns the {@code cash_in} value currently stored for today's register.
     * Useful for pre-populating the Close Register dialog if the cashier had
     * already saved a mid-day cash adjustment.
     *
     * @return stored cash_in, or 0.0 if no register for today
     */
    public static double getTodayCashIn() {
        String sql = "SELECT cash_in FROM cash_register WHERE register_date = CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getDouble("cash_in");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting today's cash_in: " + e.getMessage(), e);
        }
        return 0.0;
    }

    /**
     * Returns the {@code cash_out} value currently stored for today's register.
     *
     * @return stored cash_out, or 0.0 if no register for today
     */
    public static double getTodayCashOut() {
        String sql = "SELECT cash_out FROM cash_register WHERE register_date = CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getDouble("cash_out");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting today's cash_out: " + e.getMessage(), e);
        }
        return 0.0;
    }

    /**
     * Persists a mid-day cash adjustment (cash_in / cash_out) to the open register
     * WITHOUT closing it.  This lets the live preview on the Dashboard and the
     * Close Register dialog always reflect any owner-entered adjustments.
     *
     * <p>Only updates if today's register exists and is still {@code OPEN}.</p>
     *
     * @param cashIn  cumulative cash added to the drawer today (e.g. owner float)
     * @param cashOut cumulative cash removed from the drawer today (e.g. expenses)
     * @return true if the update succeeded, false otherwise
     */
    public static boolean updateCashAdjustments(double cashIn, double cashOut) {
        String sql = "UPDATE cash_register " +
                     "SET cash_in = ?, cash_out = ? " +
                     "WHERE register_date = CURDATE() AND status = 'OPEN'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, cashIn);
            stmt.setDouble(2, cashOut);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                LOGGER.log(Level.INFO, "Cash adjustments saved: in=" + cashIn + ", out=" + cashOut);
                return true;
            } else {
                LOGGER.log(Level.WARNING, "updateCashAdjustments: no open register for today.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating cash adjustments: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Gets the register history between two dates.
     */
    public static List<CashRegister> getRegisterHistory(Date fromDate, Date toDate) {
        List<CashRegister> history = new ArrayList<>();
        String sql = "SELECT cr.*, u1.username as openedByName, u2.username as closedByName " +
                     "FROM cash_register cr " +
                     "LEFT JOIN user u1 ON cr.opened_by = u1.userId " +
                     "LEFT JOIN user u2 ON cr.closed_by = u2.userId " +
                     "WHERE cr.register_date >= ? AND cr.register_date <= ? " +
                     "ORDER BY cr.register_date DESC";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Format dates to YYYY-MM-DD for comparison, or use java.sql.Date
            stmt.setDate(1, new java.sql.Date(fromDate.getTime()));
            stmt.setDate(2, new java.sql.Date(toDate.getTime()));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                CashRegister register = new CashRegister();
                register.setRegisterId(rs.getInt("register_id"));
                register.setRegisterDate(rs.getDate("register_date"));
                register.setOpeningBalance(rs.getDouble("opening_balance"));
                register.setClosingBalance(rs.getDouble("closing_balance"));
                register.setCashSales(rs.getDouble("cash_sales"));
                register.setCashIn(rs.getDouble("cash_in"));
                register.setCashOut(rs.getDouble("cash_out"));
                
                String status = rs.getString("status");
                register.setStatus(status);
                
                register.setOpenedBy(rs.getInt("opened_by"));
                register.setClosedBy(rs.getInt("closed_by"));
                register.setOpenedAt(rs.getTimestamp("opened_at"));
                register.setClosedAt(rs.getTimestamp("closed_at"));
                
                String openedBy = rs.getString("openedByName");
                register.setOpenedByName(openedBy != null ? openedBy : "Unknown");
                
                String closedBy = rs.getString("closedByName");
                register.setClosedByName(closedBy != null ? closedBy : "Unknown");
                
                // If OPEN, populate the live preview
                if ("OPEN".equalsIgnoreCase(status)) {
                    // Only compute live preview if this is today's register to be safe
                    // But actually, we can just use the getLiveClosingBalancePreview() method
                    // However, getLiveClosingBalancePreview() assumes CURDATE()
                    // If we have an OPEN register from a past date, calculating live preview
                    // might be inaccurate if it relies on CURDATE().
                    // Since it's usually just today, we will check if it's today
                    java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                    if (register.getRegisterDate().toString().equals(today.toString())) {
                        register.setLivePreview(true);
                        register.setDisplayClosingBalance(getLiveClosingBalancePreview());
                    } else {
                        register.setLivePreview(false);
                        register.setDisplayClosingBalance(register.getClosingBalance());
                    }
                } else {
                    register.setLivePreview(false);
                    register.setDisplayClosingBalance(register.getClosingBalance());
                }
                
                history.add(register);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching register history: " + e.getMessage(), e);
        }
        return history;
    }
}
