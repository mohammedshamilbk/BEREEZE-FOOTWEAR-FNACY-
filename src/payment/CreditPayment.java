package payment;

import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Logger;

public class CreditPayment extends PaymentMethod {
    private static final Logger logger = Logger.getLogger(CreditPayment.class.getName());
    
    private String customerId;
    private double creditLimit;
    private double outstandingAmount;
    private LocalDate dueDate;
    private boolean termsAccepted;
    private String creditApprovalStatus;
    private double billedAmount;
    
    public CreditPayment(String customerId, double creditLimit, double billedAmount) {
        super();
        this.customerId = customerId;
        this.creditLimit = creditLimit;
        this.billedAmount = billedAmount;
        this.outstandingAmount = 0;
        this.termsAccepted = false;
        this.creditApprovalStatus = "PENDING";
    }
    
    @Override
    public boolean processPayment(double amount) {
        if (!validatePayment()) {
            status = "FAILED";
            logger.warning("Credit payment validation failed");
            return false;
        }
        
        if (!termsAccepted) {
            logger.warning("Terms and conditions not accepted");
            status = "FAILED";
            return false;
        }
        
        if (!isCustomerApproved()) {
            logger.warning("Customer credit approval not confirmed");
            status = "FAILED";
            return false;
        }
        
        double availableCredit = creditLimit - outstandingAmount;
        if (amount > availableCredit) {
            logger.warning("Amount exceeds available credit limit. Available: " + availableCredit + ", Requested: " + amount);
            status = "FAILED";
            return false;
        }
        
        this.outstandingAmount += amount;
        this.status = "SUCCESS";
        this.transactionReference = generateTransactionId();
        setDueDate();
        
        logger.info("Credit payment processed for customer: " + customerId + ", Amount: " + amount + 
                   ", Outstanding: " + outstandingAmount);
        return true;
    }
    
    @Override
    public boolean validatePayment() {
        if (customerId == null || customerId.trim().isEmpty()) {
            logger.warning("Customer ID is required");
            return false;
        }
        
        if (creditLimit <= 0) {
            logger.warning("Invalid credit limit");
            return false;
        }
        
        if (billedAmount <= 0) {
            logger.warning("Invalid billed amount");
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getTransactionId() {
        return transactionReference;
    }
    
    @Override
    public String getPaymentMethodName() {
        return "CREDIT";
    }
    
    public void acceptTermsAndConditions() {
        this.termsAccepted = true;
        logger.info("Terms and conditions accepted by customer: " + customerId);
    }
    
    public void rejectTermsAndConditions() {
        this.termsAccepted = false;
        logger.info("Terms and conditions rejected by customer: " + customerId);
    }
    
    public boolean isTermsAccepted() {
        return termsAccepted;
    }
    
    public void approveCreditLimit() {
        this.creditApprovalStatus = "APPROVED";
        logger.info("Credit limit approved for customer: " + customerId);
    }
    
    public void rejectCreditLimit() {
        this.creditApprovalStatus = "REJECTED";
        logger.warning("Credit limit rejected for customer: " + customerId);
    }
    
    public boolean isCustomerApproved() {
        return creditApprovalStatus.equals("APPROVED");
    }
    
    private void setDueDate() {
        this.dueDate = LocalDate.now().plusDays(30);
    }
    
    public double getAvailableCredit() {
        return creditLimit - outstandingAmount;
    }
    
    public double getOutstandingAmount() {
        return outstandingAmount;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public void recordPayment(double paymentAmount) {
        if (paymentAmount > outstandingAmount) {
            logger.warning("Payment amount exceeds outstanding amount");
            return;
        }
        
        this.outstandingAmount -= paymentAmount;
        logger.info("Payment recorded for customer: " + customerId + ", Amount: " + paymentAmount + 
                   ", Remaining Outstanding: " + outstandingAmount);
    }
    
    public boolean isOverdue() {
        return dueDate != null && LocalDate.now().isAfter(dueDate) && outstandingAmount > 0;
    }
    
    public String getCreditApprovalStatus() {
        return creditApprovalStatus;
    }
    
    private String generateTransactionId() {
        return "TXN-CREDIT-" + customerId + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public double getCreditLimit() {
        return creditLimit;
    }
    
    @Override
    public String toString() {
        return "CreditPayment{" +
                "customerId='" + customerId + '\'' +
                ", creditLimit=" + creditLimit +
                ", outstandingAmount=" + outstandingAmount +
                ", dueDate=" + dueDate +
                ", termsAccepted=" + termsAccepted +
                ", creditApprovalStatus='" + creditApprovalStatus + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
