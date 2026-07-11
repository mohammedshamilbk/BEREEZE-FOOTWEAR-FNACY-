package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupplierDAO {
    private static final Logger LOGGER = Logger.getLogger(SupplierDAO.class.getName());
    
    public static final List<Supplier> supplierCache = new ArrayList<>();
    
    static {
        // Database is wiped clean - starting from zero
    }

    public static int addSupplier(Supplier supplier) {
        if (supplier.getSupplierId() <= 0) {
            supplier.setSupplierId(supplierCache.size() + 1);
        }
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().isEmpty()) {
            supplier.setSupplierCode("SUP" + String.format("%03d", supplier.getSupplierId()));
        }
        supplier.setStatus("ACTIVE");
        supplierCache.add(supplier);
        
        String sql = "INSERT INTO supplier (supplierCode, supplierName, phone, email, state, taxRegn, gstin, outstandingBalance, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, supplier.getSupplierCode());
            pstmt.setString(2, supplier.getSupplierName());
            pstmt.setString(3, supplier.getPhone());
            pstmt.setString(4, supplier.getEmail());
            pstmt.setString(5, supplier.getState());
            pstmt.setString(6, supplier.getTaxRegn());
            pstmt.setString(7, supplier.getGstin());
            pstmt.setDouble(8, supplier.getOutstandingBalance());
            pstmt.setString(9, supplier.getStatus());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        supplier.setSupplierId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding supplier to database: " + e.getMessage(), e);
        }
        return supplier.getSupplierId(); 
    }

    public static boolean updateSupplier(Supplier supplier) {
        // Update local cache
        for(int i=0; i<supplierCache.size(); i++) {
            if(supplierCache.get(i).getSupplierId() == supplier.getSupplierId()) {
                supplierCache.set(i, supplier);
                break;
            }
        }
        
        String sql = "UPDATE supplier SET supplierCode=?, supplierName=?, phone=?, email=?, state=?, taxRegn=?, gstin=?, outstandingBalance=?, status=? WHERE supplierId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, supplier.getSupplierCode());
            pstmt.setString(2, supplier.getSupplierName());
            pstmt.setString(3, supplier.getPhone());
            pstmt.setString(4, supplier.getEmail());
            pstmt.setString(5, supplier.getState());
            pstmt.setString(6, supplier.getTaxRegn());
            pstmt.setString(7, supplier.getGstin());
            pstmt.setDouble(8, supplier.getOutstandingBalance());
            pstmt.setString(9, supplier.getStatus());
            pstmt.setInt(10, supplier.getSupplierId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating supplier: " + e.getMessage(), e);
        }
        return true; // Return true for offline cache success
    }

    public static boolean updateOutstandingBalance(int supplierId, double amountChange) {
        // Update local cache
        for(Supplier s : supplierCache) {
            if(s.getSupplierId() == supplierId) {
                s.setOutstandingBalance(s.getOutstandingBalance() + amountChange);
                break;
            }
        }
        
        String sql = "UPDATE supplier SET outstandingBalance = outstandingBalance + ? WHERE supplierId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amountChange);
            pstmt.setInt(2, supplierId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating supplier balance: " + e.getMessage(), e);
        }
        return true; // Return true for offline cache success
    }

    public static List<Supplier> getAllSuppliers() {
        String sql = "SELECT * FROM supplier WHERE status='ACTIVE' ORDER BY supplierName ASC";
        List<Supplier> dbSuppliers = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                dbSuppliers.add(mapResultSetToSupplier(rs));
            }
            if (!dbSuppliers.isEmpty()) {
                supplierCache.clear();
                supplierCache.addAll(dbSuppliers);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving suppliers from DB, using cache fallback: " + e.getMessage());
        }
        return supplierCache;
    }

    public static Supplier getSupplierById(int supplierId) {
        String sql = "SELECT * FROM supplier WHERE supplierId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSupplier(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving supplier by ID: " + e.getMessage(), e);
        }
        // Fallback to cache
        for (Supplier s : supplierCache) {
            if (s.getSupplierId() == supplierId) {
                return s;
            }
        }
        return null;
    }

    public static List<Supplier> searchSuppliers(String keyword) {
        String sql = "SELECT * FROM supplier WHERE status='ACTIVE' AND (LOWER(supplierName) LIKE ? OR phone LIKE ?) ORDER BY supplierName ASC";
        List<Supplier> result = new ArrayList<>();
        String searchPattern = "%" + keyword.toLowerCase() + "%";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToSupplier(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching suppliers: " + e.getMessage(), e);
            // Fallback to cache
            for (Supplier s : supplierCache) {
                if ("ACTIVE".equals(s.getStatus()) && 
                    (s.getSupplierName().toLowerCase().contains(keyword.toLowerCase()) || 
                     s.getPhone().contains(keyword))) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    public static boolean deactivateSupplier(int supplierId) {
        // Update local cache
        for (int i = 0; i < supplierCache.size(); i++) {
            if (supplierCache.get(i).getSupplierId() == supplierId) {
                supplierCache.get(i).setStatus("INACTIVE");
                break;
            }
        }
        
        String sql = "UPDATE supplier SET status = 'INACTIVE' WHERE supplierId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deactivating supplier: " + e.getMessage(), e);
        }
        return true;
    }

    private static Supplier mapResultSetToSupplier(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setSupplierId(rs.getInt("supplierId"));
        s.setSupplierCode(rs.getString("supplierCode"));
        s.setSupplierName(rs.getString("supplierName"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setState(rs.getString("state"));
        s.setTaxRegn(rs.getString("taxRegn"));
        s.setGstin(rs.getString("gstin"));
        s.setOutstandingBalance(rs.getDouble("outstandingBalance"));
        s.setStatus(rs.getString("status"));
        s.setCreatedDate(rs.getTimestamp("createdDate"));
        return s;
    }
}
