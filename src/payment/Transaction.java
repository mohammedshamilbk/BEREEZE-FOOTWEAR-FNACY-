package payment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Transaction {
    private static final Logger logger = Logger.getLogger(Transaction.class.getName());
    
    private String transactionId;
    private String billId;
    private String customerId;
    private PaymentMethod paymentMethod;
    private double amount;
    private LocalDateTime transactionDate;
    private String status;
    private String referenceNumber;
    private String receiptData;
    private Map<String, String> metadata;
    
    public enum TransactionStatus {
        PENDING("PENDING"),
        SUCCESS("SUCCESS"),
        FAILED("FAILED"),
        REFUNDED("REFUNDED"),
        DECLINED("DECLINED");
        
        private final String value;
        
        TransactionStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public Transaction(String billId, String customerId, PaymentMethod paymentMethod, double amount) {
        this.billId = billId;
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now();
        this.status = TransactionStatus.PENDING.getValue();
        this.metadata = new HashMap<>();
        logger.info("Transaction created. Bill: " + billId + ", Amount: " + amount);
    }
    
    public void recordTransaction(String transactionId, String referenceNumber, String status) {
        this.transactionId = transactionId;
        this.referenceNumber = referenceNumber;
        this.status = status;
        logger.info("Transaction recorded. ID: " + transactionId + ", Status: " + status);
    }
    
    public void setReceiptData(String receiptData) {
        this.receiptData = receiptData;
    }
    
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
    
    public String getMetadata(String key) {
        return metadata.getOrDefault(key, null);
    }
    
    public Map<String, String> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getBillId() {
        return billId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public String getPaymentMethodName() {
        return paymentMethod != null ? paymentMethod.getPaymentMethodName() : "UNKNOWN";
    }
    
    public double getAmount() {
        return amount;
    }
    
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
        logger.info("Transaction status updated to: " + status);
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public String getReceiptData() {
        return receiptData;
    }
    
    public boolean isSuccessful() {
        return status.equals(TransactionStatus.SUCCESS.getValue());
    }
    
    public boolean isFailed() {
        return status.equals(TransactionStatus.FAILED.getValue()) || 
               status.equals(TransactionStatus.DECLINED.getValue());
    }
    
    public boolean isRefunded() {
        return status.equals(TransactionStatus.REFUNDED.getValue());
    }
    
    public String generateReceiptSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("========== TRANSACTION RECEIPT ==========\n");
        summary.append("Transaction ID: ").append(transactionId).append("\n");
        summary.append("Bill ID: ").append(billId).append("\n");
        summary.append("Customer ID: ").append(customerId).append("\n");
        summary.append("Payment Method: ").append(getPaymentMethodName()).append("\n");
        summary.append("Amount: ₹").append(String.format("%.2f", amount)).append("\n");
        summary.append("Date & Time: ").append(transactionDate).append("\n");
        summary.append("Status: ").append(status).append("\n");
        summary.append("Reference: ").append(referenceNumber).append("\n");
        summary.append("==========================================\n");
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", billId='" + billId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", paymentMethod='" + getPaymentMethodName() + '\'' +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                ", status='" + status + '\'' +
                ", referenceNumber='" + referenceNumber + '\'' +
                '}';
    }
}
