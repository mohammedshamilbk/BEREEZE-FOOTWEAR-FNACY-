package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for User operations
 * Handles all database operations for user table
 */
public class UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());
    
    /**
     * Add a new user
     * 
     * @param user User object to add
     * @return Generated userId, or -1 if failed
     */
    public static int addUser(User user) {
        String sql = "INSERT INTO user (username, password, fullName, role, email, phone, " +
                    "status, dailySalesTarget) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, SecurityUtils.hashPassword(user.getPassword(), user.getUsername()));
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());
            pstmt.setString(7, user.getStatus());
            pstmt.setDouble(8, user.getDailySalesTarget());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        user.setUserId(userId);
                        AuditLogDAO.log(-1, "CREATE_USER", "user", userId, null, "Username: " + user.getUsername());
                        LOGGER.log(Level.INFO, "User added successfully with ID: " + userId);
                        return userId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding user: " + e.getMessage(), e);
        }
        return -1;
    }
    
    /**
     * Update an existing user
     * 
     * @param user User object with updated values
     * @return true if successful, false otherwise
     */
    public static boolean updateUser(User user) {
        String sql = "UPDATE user SET username=?, fullName=?, role=?, email=?, phone=?, " +
                    "status=?, dailySalesTarget=? WHERE userId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getStatus());
            pstmt.setDouble(7, user.getDailySalesTarget());
            pstmt.setInt(8, user.getUserId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "User updated successfully with ID: " + user.getUserId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating user: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Delete a user (soft delete by status)
     * 
     * @param userId User ID to delete
     * @return true if successful, false otherwise
     */
    public static boolean deleteUser(int userId) {
        String sql = "UPDATE user SET status='INACTIVE' WHERE userId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                AuditLogDAO.log(-1, "DELETE_USER", "user", userId, "ACTIVE", "INACTIVE");
                LOGGER.log(Level.INFO, "User deleted (deactivated) with ID: " + userId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Get user by ID
     * 
     * @param userId User ID to retrieve
     * @return User object or null if not found
     */
    public static User getUserById(int userId) {
        String sql = "SELECT * FROM user WHERE userId=? AND status='ACTIVE'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get user by username for authentication
     * 
     * @param username Username to retrieve
     * @return User object or null if not found
     */
    public static User getUserByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username=? AND status='ACTIVE'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
            LOGGER.log(Level.WARNING, "User not found or inactive: " + username);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user by username: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get all active users
     * 
     * @return List of User objects
     */
    public static List<User> getAllUsers() {
        String sql = "SELECT * FROM user WHERE status='ACTIVE' ORDER BY fullName ASC";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            LOGGER.log(Level.INFO, "Retrieved " + users.size() + " users from database");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all users: " + e.getMessage(), e);
        }
        return users;
    }
    
    /**
     * Get users by role
     * 
     * @param role Role to search (ADMIN, CASHIER, MANAGER, OWNER)
     * @return List of User objects
     */
    public static List<User> getUsersByRole(String role) {
        String sql = "SELECT * FROM user WHERE role=? AND status='ACTIVE' ORDER BY fullName ASC";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, role);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + users.size() + " users with role: " + role);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving users by role: " + e.getMessage(), e);
        }
        return users;
    }
    
    /**
     * Update user password
     * 
     * @param userId User ID to update
     * @param newPassword New password to set
     * @return true if successful, false otherwise
     */
    public static boolean updatePassword(int userId, String newPassword) {
        String username = null;
        String selectSql = "SELECT username FROM user WHERE userId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setInt(1, userId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting username for password update: " + e.getMessage(), e);
        }

        if (username == null) {
            LOGGER.log(Level.WARNING, "User not found for ID " + userId + ", cannot update password.");
            return false;
        }

        String sql = "UPDATE user SET password=? WHERE userId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, SecurityUtils.hashPassword(newPassword, username));
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                AuditLogDAO.log(userId, "UPDATE_PASSWORD", "user", userId, null, "Password updated successfully");
                LOGGER.log(Level.INFO, "Password updated for user " + userId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating password: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Authenticate user with username and password
     * 
     * @param username Username
     * @param password Password
     * @return User object if authentication successful, null otherwise
     */
    public static User authenticateUser(String username, String password) {
        User user = getUserByUsername(username);
        if (user != null) {
            if (user.authenticate(password)) {
                LOGGER.log(Level.INFO, "User authenticated successfully: " + username);
                AuditLogDAO.log(user.getUserId(), "LOGIN_SUCCESS", "user", user.getUserId(), null, "User logged in successfully");
                // Check if hash upgrade is needed
                String currentHash = user.getPassword();
                String expectedSaltedHash = SecurityUtils.hashPassword(password, username);
                if (!expectedSaltedHash.equals(currentHash)) {
                    LOGGER.log(Level.INFO, "Upgrading password to salted hash...");
                    int userId = user.getUserId();
                    new Thread(() -> updatePassword(userId, password)).start();
                }
                return user;
            } else {
                LOGGER.log(Level.WARNING, "Authentication failed (wrong password) for user: " + username);
                AuditLogDAO.log(-1, "LOGIN_FAILED", "user", user.getUserId(), null, "Wrong password for username: " + username);
            }
        } else {
            LOGGER.log(Level.WARNING, "Authentication failed (user not found) for user: " + username);
            AuditLogDAO.log(-1, "LOGIN_FAILED", "user", -1, null, "User not found for username: " + username);
        }
        return null;
    }
    
    /**
     * Check if username already exists
     * 
     * @param username Username to check
     * @return true if username exists, false otherwise
     */
    public static boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) as count FROM user WHERE username=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking username existence: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Update sales target for user
     * 
     * @param userId User ID to update
     * @param newTarget New sales target
     * @return true if successful, false otherwise
     */
    public static boolean updateSalesTarget(int userId, double newTarget) {
        String sql = "UPDATE user SET dailySalesTarget=? WHERE userId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newTarget);
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Sales target updated for user " + userId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating sales target: " + e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Map ResultSet row to User object
     * 
     * @param rs ResultSet to map
     * @return User object
     * @throws SQLException
     */
    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("userId"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("fullName"));
        user.setRole(rs.getString("role"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setCreatedDate(rs.getTimestamp("createdDate"));
        user.setStatus(rs.getString("status"));
        user.setDailySalesTarget(rs.getDouble("dailySalesTarget"));
        return user;
    }
}
