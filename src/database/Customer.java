package database;

import java.io.Serializable;
import java.util.Date;

public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int customerId;
    private String customerCode;
    private String customerName;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private double creditLimit;
    private double outstandingAmount;
    private String customerType; // REGULAR, WHOLESALE, RETAIL
    private double loyaltyPoints;
    private Date registrationDate;
    private Date lastPurchaseDate;
    private String status; // ACTIVE, INACTIVE
    
    public Customer() {
    }
    
    public Customer(String customerCode, String customerName, String phone, String email) {
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.customerType = "REGULAR";
        this.status = "ACTIVE";
        this.registrationDate = new Date();
        this.loyaltyPoints = 0;
    }
    
    public void addLoyaltyPoints(double amount) {
        this.loyaltyPoints += (amount * 0.1); // 10% of purchase amount
    }
    
    public double getEffectiveCreditLimit() {
        return creditLimit - outstandingAmount;
    }
    
    // Getters and Setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    
    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }
    
    public double getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(double outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    
    public double getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(double loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    
    public Date getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }
    
    public Date getLastPurchaseDate() { return lastPurchaseDate; }
    public void setLastPurchaseDate(Date lastPurchaseDate) { this.lastPurchaseDate = lastPurchaseDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
    public String toString() {
        return customerCode + " - " + customerName + " (" + phone + ")";
    }
}
