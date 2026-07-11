package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Bill operations
 * Handles all database operations for bill table
 */
public class BillDAO {
    private static final Logger LOGGER = Logger.getLogger(BillDAO.class.getName());
    
    /**
     * Save a new bill
     * 
     * @param bill Bill object to save
     * @return Generated billId, or -1 if failed
     */
    public static int saveBill(Bill bill) {
        String sql = "INSERT INTO bill (billNumber, billType, billDate, customerId, supplierId, userId, " +
                    "subtotal, totalDiscount, totalAmount, paidAmount, " +
                    "paymentMode, status, remarks) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, bill.getBillNumber());
            pstmt.setString(2, bill.getBillType());
            pstmt.setTimestamp(3, new Timestamp(bill.getBillDate().getTime()));
            
            if (bill.getCustomerId() > 0) pstmt.setInt(4, bill.getCustomerId());
            else pstmt.setNull(4, java.sql.Types.INTEGER);
            
            if (bill.getSupplierId() > 0) pstmt.setInt(5, bill.getSupplierId());
            else pstmt.setNull(5, java.sql.Types.INTEGER);
            
            pstmt.setInt(6, bill.getUserId());
            pstmt.setDouble(7, bill.getSubtotal());
            pstmt.setDouble(8, bill.getTotalDiscount());
            pstmt.setDouble(9, bill.getTotalAmount());
            pstmt.setDouble(10, bill.getPaidAmount());
            pstmt.setString(11, bill.getPaymentMode());
            pstmt.setString(12, bill.getStatus());
            pstmt.setString(13, bill.getRemarks());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int billId = rs.getInt(1);
                        bill.setBillId(billId);
                        LOGGER.log(Level.INFO, "Bill saved successfully with ID: " + billId);
                        return billId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving bill: " + e.getMessage(), e);
        }
        return -1;
    }
    
    /**
     * Update an existing bill
     * 
     * @param bill Bill object with updated values
     * @return true if successful, false otherwise
     */
    public static boolean updateBill(Bill bill) {
        String sql = "UPDATE bill SET billNumber=?, billType=?, billDate=?, customerId=?, supplierId=?, " +
                    "subtotal=?, totalDiscount=?, totalAmount=?, paidAmount=?, " +
                    "paymentMode=?, status=?, remarks=? WHERE billId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, bill.getBillNumber());
            pstmt.setString(2, bill.getBillType());
            pstmt.setTimestamp(3, new Timestamp(bill.getBillDate().getTime()));
            
            if (bill.getCustomerId() > 0) pstmt.setInt(4, bill.getCustomerId());
            else pstmt.setNull(4, java.sql.Types.INTEGER);
            
            if (bill.getSupplierId() > 0) pstmt.setInt(5, bill.getSupplierId());
            else pstmt.setNull(5, java.sql.Types.INTEGER);
            
            pstmt.setDouble(6, bill.getSubtotal());
            pstmt.setDouble(7, bill.getTotalDiscount());
            pstmt.setDouble(8, bill.getTotalAmount());
            pstmt.setDouble(9, bill.getPaidAmount());
            pstmt.setString(10, bill.getPaymentMode());
            pstmt.setString(11, bill.getStatus());
            pstmt.setString(12, bill.getRemarks());
            pstmt.setInt(13, bill.getBillId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Bill updated successfully with ID: " + bill.getBillId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating bill: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Get bill by ID
     * 
     * @param billId Bill ID to retrieve
     * @return Bill object or null if not found
     */
    public static Bill getBillById(int billId) {
        String sql = "SELECT * FROM bill WHERE billId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBill(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bill by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get bill by bill number
     * 
     * @param billNumber Bill number to retrieve
     * @return Bill object or null if not found
     */
    public static Bill getBillByNumber(String billNumber) {
        String sql = "SELECT * FROM bill WHERE billNumber=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, billNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBill(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bill by number: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get all bills
     * 
     * @return List of Bill objects
     */
    public static List<Bill> getAllBills() {
        String sql = "SELECT * FROM bill ORDER BY billDate DESC";
        List<Bill> bills = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }
            LOGGER.log(Level.INFO, "Retrieved " + bills.size() + " bills from database");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all bills: " + e.getMessage(), e);
        }
        return bills;
    }
    
    /**
     * Search bills by number
     * 
     * @param keyword Search keyword
     * @return List of matching Bill objects
     */
    public static List<Bill> searchBills(String keyword) {
        String sql = "SELECT * FROM bill WHERE billNumber LIKE ? ORDER BY billDate DESC";
        List<Bill> bills = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapResultSetToBill(rs));
                }
            }
            LOGGER.log(Level.INFO, "Found " + bills.size() + " bills matching keyword: " + keyword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching bills: " + e.getMessage(), e);
        }
        return bills;
    }
    
    /**
     * Update bill status
     * 
     * @param billNumber Bill number
     * @param status New status
     * @return true if successful
     */
    public static boolean updateBillStatus(String billNumber, String status) {
        String sql = "UPDATE bill SET status=? WHERE billNumber=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, billNumber);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating bill status: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Get bills within date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return List of Bill objects
     */
    public static List<Bill> getBillsByDate(Date fromDate, Date toDate) {
        String sql = "SELECT * FROM bill WHERE billDate >= ? AND billDate <= ? ORDER BY billDate DESC";
        List<Bill> bills = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapResultSetToBill(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + bills.size() + " bills for date range");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bills by date: " + e.getMessage(), e);
        }
        return bills;
    }
    
    /**
     * Get bills for a specific customer
     * 
     * @param customerId Customer ID
     * @return List of Bill objects
     */
    public static List<Bill> getBillsByCustomer(int customerId) {
        String sql = "SELECT * FROM bill WHERE customerId=? ORDER BY billDate DESC";
        List<Bill> bills = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapResultSetToBill(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + bills.size() + " bills for customer " + customerId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bills by customer: " + e.getMessage(), e);
        }
        return bills;
    }
    
    /**
     * Get bills for a specific supplier
     * 
     * @param supplierId Supplier ID
     * @return List of Bill objects
     */
    public static List<Bill> getBillsBySupplier(int supplierId) {
        String sql = "SELECT * FROM bill WHERE supplierId=? ORDER BY billDate DESC";
        List<Bill> bills = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, supplierId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapResultSetToBill(rs));
                }
            }
        } catch (SQLException e) {
            // Log error
        }
        return bills;
    }
    
    /**
     * Get bills by payment mode
     * 
     * @param paymentMode Payment mode (CASH, CARD, CHEQUE, ONLINE, CREDIT)
     * @return List of Bill objects
     */
    public static List<Bill> getBillsByPaymentMode(String paymentMode) {
        String sql = "SELECT * FROM bill WHERE paymentMode=? ORDER BY billDate DESC";
        List<Bill> bills = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, paymentMode);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapResultSetToBill(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + bills.size() + " bills for payment mode: " + paymentMode);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bills by payment mode: " + e.getMessage(), e);
        }
        return bills;
    }
    
    /**
     * Get total sales for a date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return Total sales amount
     */
    public static double getTotalSales(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) return 0.0;
        String sql = "SELECT COALESCE(SUM(totalAmount), 0) AS totalSales FROM bill " +
                    "WHERE billDate >= ? AND billDate <= ? AND billType = 'SALES' AND status != 'CANCELLED'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("totalSales");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total sales: " + e.getMessage(), e);
        }
        return 0.0;
    }
    
    /**
     * Get total sales for the current day
     * 
     * @return Total sales amount for today
     */
    public static double getTodaySales() {
        LocalDate today = LocalDate.now();
        Date fromDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
        return getTotalSales(fromDate, toDate);
    }

    /**
     * Backward-compatible alias — existing callers of getTodaySalesTotal() still compile.
     * @return today's total sales
     */
    public static double getTodaySalesTotal() {
        return getTodaySales();
    }
    
    /**
     * Get a daily breakdown of sales for a date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return Map of LocalDate to total sales amount, sorted by date
     */
    public static Map<LocalDate, Double> getDailySalesBreakdown(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) return new java.util.TreeMap<>();
        String sql = "SELECT DATE(billDate) as saleDate, COALESCE(SUM(totalAmount), 0) as dailyTotal FROM bill " +
                    "WHERE billDate >= ? AND billDate <= ? AND billType = 'SALES' AND status != 'CANCELLED' " +
                    "GROUP BY DATE(billDate) ORDER BY DATE(billDate) ASC";
                    
        Map<LocalDate, Double> breakdown = new java.util.TreeMap<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("saleDate");
                    double dailyTotal = rs.getDouble("dailyTotal");
                    if (sqlDate != null) {
                        breakdown.put(sqlDate.toLocalDate(), dailyTotal);
                    }
                }
            }
            LOGGER.log(Level.INFO, "Retrieved daily sales breakdown for " + breakdown.size() + " days");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving daily sales breakdown: " + e.getMessage(), e);
        }
        return breakdown;
    }
    

    
    /**
     * Count bills for date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return Count of bills
     */
    public static int getBillCount(Date fromDate, Date toDate) {
        String sql = "SELECT COUNT(*) as billCount FROM bill " +
                    "WHERE billDate >= ? AND billDate <= ? AND status='COMPLETED'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("billCount");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting bills: " + e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * Count pending purchase bills
     * 
     * @return Count of pending purchase bills
     */
    public static int getPendingPurchaseBillCount() {
        String sql = "SELECT COUNT(*) as billCount FROM bill WHERE billType='PURCHASE' AND status='PENDING'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("billCount");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting pending purchase bills: " + e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * Map ResultSet row to Bill object
     * 
     * @param rs ResultSet to map
     * @return Bill object
     * @throws SQLException
     */
    private static Bill mapResultSetToBill(ResultSet rs) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId(rs.getInt("billId"));
        bill.setBillNumber(rs.getString("billNumber"));
        bill.setBillType(rs.getString("billType"));
        bill.setBillDate(new Date(rs.getTimestamp("billDate").getTime()));
        bill.setCustomerId(rs.getInt("customerId"));
        bill.setSupplierId(rs.getInt("supplierId"));
        bill.setUserId(rs.getInt("userId"));
        bill.setSubtotal(rs.getDouble("subtotal"));
        bill.setTotalDiscount(rs.getDouble("totalDiscount"));
        bill.setTotalAmount(rs.getDouble("totalAmount"));
        bill.setPaidAmount(rs.getDouble("paidAmount"));
        bill.setPaymentMode(rs.getString("paymentMode"));
        bill.setStatus(rs.getString("status"));
        bill.setRemarks(rs.getString("remarks"));
        bill.setCreatedDate(new Date(rs.getTimestamp("createdDate").getTime()));
        return bill;
    }
}
