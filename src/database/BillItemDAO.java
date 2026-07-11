package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for BillItem operations
 * Handles all database operations for bill_item table
 */
public class BillItemDAO {
    private static final Logger LOGGER = Logger.getLogger(BillItemDAO.class.getName());
    
    /**
     * Add a new bill item
     * 
     * @param billItem BillItem object to add
     * @return Generated billItemId, or -1 if failed
     */
    public static int addBillItem(BillItem billItem) {
        String sql = "INSERT INTO bill_item (billId, itemId, quantity, unitPrice, discount, totalAmount) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, billItem.getBillId());
            pstmt.setInt(2, billItem.getItemId());
            pstmt.setInt(3, billItem.getQuantity());
            pstmt.setDouble(4, billItem.getUnitPrice());
            pstmt.setDouble(5, billItem.getDiscount());
            pstmt.setDouble(6, billItem.getTotalAmount());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int billItemId = rs.getInt(1);
                        billItem.setBillItemId(billItemId);
                        LOGGER.log(Level.INFO, "Bill item added successfully with ID: " + billItemId);
                        return billItemId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding bill item: " + e.getMessage(), e);
        }
        return -1;
    }
    
    /**
     * Update a bill item
     * 
     * @param billItem BillItem object with updated values
     * @return true if successful, false otherwise
     */
    public static boolean updateBillItem(BillItem billItem) {
        String sql = "UPDATE bill_item SET billId=?, itemId=?, quantity=?, unitPrice=?, " +
                    "discount=?, totalAmount=? WHERE billItemId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billItem.getBillId());
            pstmt.setInt(2, billItem.getItemId());
            pstmt.setInt(3, billItem.getQuantity());
            pstmt.setDouble(4, billItem.getUnitPrice());
            pstmt.setDouble(5, billItem.getDiscount());
            pstmt.setDouble(6, billItem.getTotalAmount());
            pstmt.setInt(7, billItem.getBillItemId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Bill item updated successfully with ID: " + billItem.getBillItemId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating bill item: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Delete a bill item
     * 
     * @param billItemId Bill item ID to delete
     * @return true if successful, false otherwise
     */
    public static boolean deleteBillItem(int billItemId) {
        String sql = "DELETE FROM bill_item WHERE billItemId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billItemId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Bill item deleted successfully with ID: " + billItemId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting bill item: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Get bill item by ID
     * 
     * @param billItemId Bill item ID to retrieve
     * @return BillItem object or null if not found
     */
    public static BillItem getBillItemById(int billItemId) {
        String sql = "SELECT * FROM bill_item WHERE billItemId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billItemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBillItem(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bill item by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get all bill items for a bill
     * 
     * @param billId Bill ID
     * @return List of BillItem objects
     */
    public static List<BillItem> getBillItemsByBillId(int billId) {
        String sql = "SELECT * FROM bill_item WHERE billId=? ORDER BY billItemId ASC";
        List<BillItem> billItems = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    billItems.add(mapResultSetToBillItem(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + billItems.size() + " bill items for bill " + billId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bill items: " + e.getMessage(), e);
        }
        return billItems;
    }
    
    /**
     * Get bill items for an item
     * 
     * @param itemId Item ID
     * @return List of BillItem objects
     */
    public static List<BillItem> getBillItemsByItemId(int itemId) {
        String sql = "SELECT * FROM bill_item WHERE itemId=? ORDER BY billItemId DESC";
        List<BillItem> billItems = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    billItems.add(mapResultSetToBillItem(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + billItems.size() + " bill items for item " + itemId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving bill items by item: " + e.getMessage(), e);
        }
        return billItems;
    }
    

    
    /**
     * Get total amount for a bill (sum of all bill items)
     * 
     * @param billId Bill ID
     * @return Total amount
     */
    public static double getTotalAmountByBill(int billId) {
        String sql = "SELECT SUM(totalAmount) as totalAmount FROM bill_item WHERE billId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("totalAmount");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving total amount: " + e.getMessage(), e);
        }
        return 0.0;
    }
    
    /**
     * Get quantity sum for an item in a bill
     * 
     * @param billId Bill ID
     * @param itemId Item ID
     * @return Total quantity
     */
    public static int getItemQuantityInBill(int billId, int itemId) {
        String sql = "SELECT SUM(quantity) as totalQuantity FROM bill_item " +
                    "WHERE billId=? AND itemId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billId);
            pstmt.setInt(2, itemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("totalQuantity");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving item quantity: " + e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * Delete all bill items for a bill
     * 
     * @param billId Bill ID
     * @return true if successful, false otherwise
     */
    public static boolean deleteAllBillItems(int billId) {
        String sql = "DELETE FROM bill_item WHERE billId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected >= 0) {
                LOGGER.log(Level.INFO, "All bill items deleted for bill " + billId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting all bill items: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Map ResultSet row to BillItem object
     * 
     * @param rs ResultSet to map
     * @return BillItem object
     * @throws SQLException
     */
    private static BillItem mapResultSetToBillItem(ResultSet rs) throws SQLException {
        BillItem billItem = new BillItem();
        billItem.setBillItemId(rs.getInt("billItemId"));
        billItem.setBillId(rs.getInt("billId"));
        billItem.setItemId(rs.getInt("itemId"));
        billItem.setQuantity(rs.getInt("quantity"));
        billItem.setUnitPrice(rs.getDouble("unitPrice"));
        billItem.setDiscount(rs.getDouble("discount"));
        billItem.setTotalAmount(rs.getDouble("totalAmount"));
        return billItem;
    }
}
