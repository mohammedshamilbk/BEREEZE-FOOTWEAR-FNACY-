package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for ItemMaster operations
 * Handles all database operations for item_master table
 */
public class ItemMasterDAO {
    private static final Logger LOGGER = Logger.getLogger(ItemMasterDAO.class.getName());
    
    /**
     * Add a new item to the database
     * 
     * @param item ItemMaster object to add
     * @return Generated itemId, or -1 if failed
     */
    public static int addItem(ItemMaster item) {
        String sql = "INSERT INTO item_master (itemCode, itemName, category, manufacturer, " +
                    "purchasePrice, sellingPrice, barcode, stockQuantity, reorderLevel, " +
                    "size, color, material, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, item.getItemCode());
            pstmt.setString(2, item.getItemName());
            pstmt.setString(3, item.getCategory());
            pstmt.setString(4, item.getManufacturer());
            pstmt.setDouble(5, item.getPurchasePrice());
            pstmt.setDouble(6, item.getSellingPrice());
            pstmt.setString(7, item.getBarcode());
            pstmt.setInt(8, item.getStockQuantity());
            pstmt.setInt(9, item.getReorderLevel());
            pstmt.setString(10, item.getSize());
            pstmt.setString(11, item.getColor());
            pstmt.setString(12, item.getMaterial());
            pstmt.setString(13, item.getStatus());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int itemId = rs.getInt(1);
                        item.setItemId(itemId);
                        LOGGER.log(Level.INFO, "Item added successfully with ID: " + itemId);
                        return itemId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding item: " + e.getMessage(), e);
        }
        return -1;
    }
    
    /**
     * Update an existing item
     * 
     * @param item ItemMaster object with updated values
     * @return true if successful, false otherwise
     */
    public static boolean updateItem(ItemMaster item) {
        String sql = "UPDATE item_master SET itemCode=?, itemName=?, category=?, manufacturer=?, " +
                    "purchasePrice=?, sellingPrice=?, barcode=?, stockQuantity=?, reorderLevel=?, " +
                    "size=?, color=?, material=?, status=?, modifiedDate=CURRENT_TIMESTAMP " +
                    "WHERE itemId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, item.getItemCode());
            pstmt.setString(2, item.getItemName());
            pstmt.setString(3, item.getCategory());
            pstmt.setString(4, item.getManufacturer());
            pstmt.setDouble(5, item.getPurchasePrice());
            pstmt.setDouble(6, item.getSellingPrice());
            pstmt.setString(7, item.getBarcode());
            pstmt.setInt(8, item.getStockQuantity());
            pstmt.setInt(9, item.getReorderLevel());
            pstmt.setString(10, item.getSize());
            pstmt.setString(11, item.getColor());
            pstmt.setString(12, item.getMaterial());
            pstmt.setString(13, item.getStatus());
            pstmt.setInt(14, item.getItemId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Item updated successfully with ID: " + item.getItemId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating item: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Delete an item
     * 
     * @param itemId Item ID to delete
     * @return true if successful, false otherwise
     */
    public static boolean deleteItem(int itemId) {
        String sql = "UPDATE item_master SET status='INACTIVE' WHERE itemId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                AuditLogDAO.log(-1, "DELETE_ITEM", "item_master", itemId, "ACTIVE", "INACTIVE");
                LOGGER.log(Level.INFO, "Item soft-deleted successfully with ID: " + itemId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting item: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Get item by ID
     * 
     * @param itemId Item ID to retrieve
     * @return ItemMaster object or null if not found
     */
    public static ItemMaster getItemById(int itemId) {
        String sql = "SELECT * FROM item_master WHERE itemId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving item by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get item by barcode
     * 
     * @param barcode Item barcode to retrieve
     * @return ItemMaster object or null if not found
     */
    public static ItemMaster getItemByBarcode(String barcode) {
        String sql = "SELECT * FROM item_master WHERE barcode=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, barcode);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving item by barcode: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get all active items
     * 
     * @return List of ItemMaster objects
     */
    public static List<ItemMaster> getAllItems() {
        String sql = "SELECT * FROM item_master WHERE status='ACTIVE' ORDER BY itemName ASC";
        List<ItemMaster> items = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
            LOGGER.log(Level.INFO, "Retrieved " + items.size() + " items from database");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all items: " + e.getMessage(), e);
        }
        return items;
    }
    
    /**
     * Search items by keyword (name or code)
     * 
     * @param keyword Search keyword
     * @return List of matching ItemMaster objects
     */
    public static List<ItemMaster> searchItems(String keyword) {
        String sql = "SELECT * FROM item_master WHERE status='ACTIVE' " +
                    "AND (itemCode LIKE ? OR itemName LIKE ? OR category LIKE ?) " +
                    "ORDER BY itemName ASC";
        List<ItemMaster> items = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
            LOGGER.log(Level.INFO, "Found " + items.size() + " items matching keyword: " + keyword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching items: " + e.getMessage(), e);
        }
        return items;
    }
    
    /**
     * Get items that are below their reorder level (but still have stock > 0)
     * 
     * @return List of low stock items
     */
    public static List<ItemMaster> getLowStockItems() {
        String sql = "SELECT * FROM item_master WHERE status='ACTIVE' AND stockQuantity > 0 AND stockQuantity <= reorderLevel " +
                    "ORDER BY (stockQuantity - reorderLevel) ASC";
        List<ItemMaster> items = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
            LOGGER.log(Level.INFO, "Found " + items.size() + " items with low stock");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving low stock items: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * Get the count of items that are below their reorder level (stock > 0 and stock <= reorderLevel)
     * 
     * @return Count of low stock items
     */
    public static int getLowStockItemsCount() {
        String sql = "SELECT COUNT(*) FROM item_master WHERE status='ACTIVE' AND stockQuantity > 0 AND stockQuantity <= reorderLevel";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving low stock count: " + e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * Update stock quantity for an item
     * 
     * @param itemId Item ID to update
     * @param quantityChange Quantity change (positive or negative)
     * @return true if successful, false otherwise
     */
    public static boolean updateStockQuantity(int itemId, int quantityChange) {
        String sql = "UPDATE item_master SET stockQuantity = stockQuantity + ? " +
                    "WHERE itemId=? AND (stockQuantity + ?) >= 0";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, quantityChange);
            pstmt.setInt(2, itemId);
            pstmt.setInt(3, quantityChange);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Stock updated for item " + itemId + " by " + quantityChange);
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Insufficient stock for item " + itemId);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating stock: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Get items that are completely out of stock (stockQuantity == 0)
     * 
     * @return List of out of stock items
     */
    public static List<ItemMaster> getOutOfStockItems() {
        String sql = "SELECT * FROM item_master WHERE status='ACTIVE' AND stockQuantity = 0 ORDER BY itemName ASC";
        List<ItemMaster> items = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
            LOGGER.log(Level.INFO, "Found " + items.size() + " out of stock items");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving out of stock items: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * Set absolute stock quantity for an item (used in stock adjustments)
     * 
     * @param itemId Item ID to update
     * @param newQty New absolute stock quantity
     * @return true if successful, false otherwise
     */
    public static boolean updateStockQuantityAbsolute(int itemId, int newQty) {
        String sql = "UPDATE item_master SET stockQuantity = ? WHERE itemId = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newQty);
            pstmt.setInt(2, itemId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Stock set to " + newQty + " for item ID " + itemId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error setting absolute stock: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Map ResultSet row to ItemMaster object
     * 
     * @param rs ResultSet to map
     * @return ItemMaster object
     * @throws SQLException
     */
    private static ItemMaster mapResultSetToItem(ResultSet rs) throws SQLException {
        ItemMaster item = new ItemMaster();
        item.setItemId(rs.getInt("itemId"));
        item.setItemCode(rs.getString("itemCode"));
        item.setItemName(rs.getString("itemName"));
        item.setCategory(rs.getString("category"));
        item.setManufacturer(rs.getString("manufacturer"));
        item.setPurchasePrice(rs.getDouble("purchasePrice"));
        item.setSellingPrice(rs.getDouble("sellingPrice"));
        item.setBarcode(rs.getString("barcode"));
        item.setStockQuantity(rs.getInt("stockQuantity"));
        
        try {
            item.setReorderLevel(rs.getInt("reorderLevel"));
        } catch (SQLException e) {
            // column might not exist if schema hasn't been updated
            item.setReorderLevel(10);
        }
        
        item.setSize(rs.getString("size"));
        item.setColor(rs.getString("color"));
        item.setMaterial(rs.getString("material"));
        item.setCreatedDate(rs.getTimestamp("createdDate"));
        item.setModifiedDate(rs.getTimestamp("modifiedDate"));
        item.setStatus(rs.getString("status"));
        return item;
    }
}
