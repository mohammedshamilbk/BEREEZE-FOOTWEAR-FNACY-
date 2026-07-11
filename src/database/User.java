package database;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String role; // ADMIN, CASHIER, MANAGER, OWNER
    private String email;
    private String phone;
    private Date createdDate;
    private String status; // ACTIVE, INACTIVE
    private double dailySalesTarget;
    private double totalSalesAchieved;
    
    public User() {
    }
    
    public User(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.status = "ACTIVE";
        this.createdDate = new Date();
        this.totalSalesAchieved = 0;
    }
    public boolean authenticate(String password) {
        if (password == null || this.password == null) return false;
        
        // Handle legacy plain-text passwords
        if (this.password.equals(password)) return "ACTIVE".equals(status);
        
        // Handle legacy unsalted SHA-256 passwords
        String hashedInput = SecurityUtils.hashPassword(password);
        if (this.password.equals(hashedInput) && "ACTIVE".equals(status)) return true;

        // Handle modern salted SHA-256 passwords
        String saltedInput = SecurityUtils.hashPassword(password, username);
        return this.password.equals(saltedInput) && "ACTIVE".equals(status);
    }
    
    public boolean hasPermission(String action) {
        switch (role) {
            case "ADMIN":
                return true;
            case "MANAGER":
                return !action.equals("DELETE_USER");
            case "CASHIER":
                return action.equals("SALES") || action.equals("RETURN");
            default:
                return false;
        }
    }
    
    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double getDailySalesTarget() { return dailySalesTarget; }
    public void setDailySalesTarget(double dailySalesTarget) { this.dailySalesTarget = dailySalesTarget; }
    
    public double getTotalSalesAchieved() { return totalSalesAchieved; }
    public void setTotalSalesAchieved(double totalSalesAchieved) { this.totalSalesAchieved = totalSalesAchieved; }
    
    @Override
    public String toString() {
        return username + " - " + fullName + " (" + role + ")";
    }
}
