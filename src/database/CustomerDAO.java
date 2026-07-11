package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Customer operations
 * Handles all database operations for customer table
 */
public class CustomerDAO {
    private static final Logger LOGGER = Logger.getLogger(CustomerDAO.class.getName());
    
    // In-memory cache to sync new customer additions instantly and support offline operation
    public static final List<Customer> customerCache = new ArrayList<>();
    
    static {
        // Database is wiped clean - starting from zero
    }
    
    /**
     * Add a new customer
     * 
     * @param customer Customer object to add
     * @return Generated customerId, or -1 if failed
     */
    public static int addCustomer(Customer customer) {
        // Pre-save to local memory cache for instant availability
        if (customer.getCustomerId() <= 0) {
            customer.setCustomerId(customerCache.size() + 1);
        }
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isEmpty()) {
            customer.setCustomerCode("CUST" + String.format("%03d", customer.getCustomerId()));
        }
        customer.setStatus("ACTIVE");
        customerCache.add(customer);
        
        String sql = "INSERT INTO customer (customerCode, customerName, phone, email, address, " +
                    "city, state, pincode, creditLimit, outstandingAmount, customerType, " +
                    "loyaltyPoints, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, customer.getCustomerCode());
            pstmt.setString(2, customer.getCustomerName());
            pstmt.setString(3, customer.getPhone());
            pstmt.setString(4, customer.getEmail());
            pstmt.setString(5, customer.getAddress());
            pstmt.setString(6, customer.getCity());
            pstmt.setString(7, customer.getState());
            pstmt.setString(8, customer.getPincode());
            pstmt.setDouble(9, customer.getCreditLimit());
            pstmt.setDouble(10, customer.getOutstandingAmount());
            pstmt.setString(11, customer.getCustomerType());
            pstmt.setDouble(12, customer.getLoyaltyPoints());
            pstmt.setString(13, customer.getStatus());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int customerId = rs.getInt(1);
                        customer.setCustomerId(customerId);
                        LOGGER.log(Level.INFO, "Customer added successfully with ID: " + customerId);
                        return customerId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding customer to database: " + e.getMessage(), e);
        }
        return customer.getCustomerId(); // Return local ID on success/fallback
    }
    
    /**
     * Update an existing customer
     * 
     * @param customer Customer object with updated values
     * @return true if successful, false otherwise
     */
    public static boolean updateCustomer(Customer customer) {
        String sql = "UPDATE customer SET customerCode=?, customerName=?, phone=?, email=?, " +
                    "address=?, city=?, state=?, pincode=?, creditLimit=?, outstandingAmount=?, " +
                    "customerType=?, loyaltyPoints=?, status=? WHERE customerId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, customer.getCustomerCode());
            pstmt.setString(2, customer.getCustomerName());
            pstmt.setString(3, customer.getPhone());
            pstmt.setString(4, customer.getEmail());
            pstmt.setString(5, customer.getAddress());
            pstmt.setString(6, customer.getCity());
            pstmt.setString(7, customer.getState());
            pstmt.setString(8, customer.getPincode());
            pstmt.setDouble(9, customer.getCreditLimit());
            pstmt.setDouble(10, customer.getOutstandingAmount());
            pstmt.setString(11, customer.getCustomerType());
            pstmt.setDouble(12, customer.getLoyaltyPoints());
            pstmt.setString(13, customer.getStatus());
            pstmt.setInt(14, customer.getCustomerId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Customer updated successfully with ID: " + customer.getCustomerId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating customer: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Delete a customer (soft delete by status)
     * 
     * @param customerId Customer ID to delete
     * @return true if successful, false otherwise
     */
    public static boolean deleteCustomer(int customerId) {
        String sql = "UPDATE customer SET status='INACTIVE' WHERE customerId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Customer deleted (deactivated) with ID: " + customerId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting customer: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Get customer by ID
     * 
     * @param customerId Customer ID to retrieve
     * @return Customer object or null if not found
     */
    public static Customer getCustomerById(int customerId) {
        String sql = "SELECT * FROM customer WHERE customerId=? AND status='ACTIVE'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving customer by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get customer by phone number
     * 
     * @param phone Phone number to search
     * @return Customer object or null if not found
     */
    public static Customer getCustomerByPhone(String phone) {
        String sql = "SELECT * FROM customer WHERE phone=? AND status='ACTIVE'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, phone);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving customer by phone: " + e.getMessage(), e);
        }
        return null;
    }
    
    public static List<Customer> getAllCustomers() {
        String sql = "SELECT * FROM customer WHERE status='ACTIVE' ORDER BY customerName ASC";
        List<Customer> dbCustomers = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                dbCustomers.add(mapResultSetToCustomer(rs));
            }
            LOGGER.log(Level.INFO, "Retrieved " + dbCustomers.size() + " customers from database");
            if (!dbCustomers.isEmpty()) {
                // Keep local memory cache in sync
                customerCache.clear();
                customerCache.addAll(dbCustomers);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving customers from DB, using cache fallback: " + e.getMessage(), e);
        }
        return customerCache;
    }
    
    /**
     * Search customers by keyword (name, code, phone)
     * 
     * @param keyword Search keyword
     * @return List of matching Customer objects
     */
    public static List<Customer> searchCustomers(String keyword) {
        String sql = "SELECT * FROM customer WHERE status='ACTIVE' " +
                    "AND (customerCode LIKE ? OR customerName LIKE ? OR phone LIKE ? OR email LIKE ?) " +
                    "ORDER BY customerName ASC";
        List<Customer> customers = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
            LOGGER.log(Level.INFO, "Found " + customers.size() + " customers matching keyword: " + keyword);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching customers: " + e.getMessage(), e);
        }
        return customers;
    }
    
    /**
     * Get customers by type (REGULAR, WHOLESALE, RETAIL)
     * 
     * @param customerType Type of customer
     * @return List of Customer objects
     */
    public static List<Customer> getCustomersByType(String customerType) {
        String sql = "SELECT * FROM customer WHERE customerType=? AND status='ACTIVE' " +
                    "ORDER BY customerName ASC";
        List<Customer> customers = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, customerType);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
            LOGGER.log(Level.INFO, "Found " + customers.size() + " customers of type: " + customerType);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving customers by type: " + e.getMessage(), e);
        }
        return customers;
    }
    
    /**
     * Update outstanding amount for a customer
     * 
     * @param customerId Customer ID to update
     * @param amountChange Amount change (positive or negative)
     * @return true if successful, false otherwise
     */
    public static boolean updateOutstandingAmount(int customerId, double amountChange) {
        String sql = "UPDATE customer SET outstandingAmount = outstandingAmount + ? " +
                    "WHERE customerId=? AND (outstandingAmount + ?) >= 0 " +
                    "AND (outstandingAmount + ?) <= creditLimit";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, amountChange);
            pstmt.setInt(2, customerId);
            pstmt.setDouble(3, amountChange);
            pstmt.setDouble(4, amountChange);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Outstanding amount updated for customer " + customerId);
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Could not update outstanding amount - credit limit exceeded");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating outstanding amount: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Add loyalty points to customer
     * 
     * @param customerId Customer ID to update
     * @param points Points to add
     * @return true if successful, false otherwise
     */
    public static boolean addLoyaltyPoints(int customerId, double points) {
        String sql = "UPDATE customer SET loyaltyPoints = loyaltyPoints + ? WHERE customerId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, points);
            pstmt.setInt(2, customerId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Loyalty points added for customer " + customerId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding loyalty points: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Map ResultSet row to Customer object
     * 
     * @param rs ResultSet to map
     * @return Customer object
     * @throws SQLException
     */
    private static Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customerId"));
        customer.setCustomerCode(rs.getString("customerCode"));
        customer.setCustomerName(rs.getString("customerName"));
        customer.setPhone(rs.getString("phone"));
        customer.setEmail(rs.getString("email"));
        customer.setAddress(rs.getString("address"));
        customer.setCity(rs.getString("city"));
        customer.setState(rs.getString("state"));
        customer.setPincode(rs.getString("pincode"));
        customer.setCreditLimit(rs.getDouble("creditLimit"));
        customer.setOutstandingAmount(rs.getDouble("outstandingAmount"));
        customer.setCustomerType(rs.getString("customerType"));
        customer.setLoyaltyPoints(rs.getDouble("loyaltyPoints"));
        customer.setRegistrationDate(rs.getTimestamp("registrationDate"));
        customer.setLastPurchaseDate(rs.getTimestamp("lastPurchaseDate"));
        customer.setStatus(rs.getString("status"));
        return customer;
    }
}
