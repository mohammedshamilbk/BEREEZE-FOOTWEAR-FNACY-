package payment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.Random;

public class PaymentGateway {
    private static final Logger logger = Logger.getLogger(PaymentGateway.class.getName());
    
    private String lastTransactionId;
    private Map<String, String> transactionLog;
    private static final double DECLINE_PROBABILITY = 0.1; // 10% chance of decline
    private static final int MAX_RETRIES = 3;
    
    public PaymentGateway() {
        this.transactionLog = new HashMap<>();
    }
    
    public boolean authorizeCardPayment(String cardNumber, double amount, String cvv) {
        logger.info("Authorizing card payment: Amount=" + amount);
        
        // Simulate authorization with retry logic
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Simulate network delay
                Thread.sleep(500);
                
                // Mock authorization logic
                if (shouldDeclineTransaction()) {
                    logger.warning("Authorization declined on attempt " + attempt);
                    if (attempt < MAX_RETRIES) {
                        logger.info("Retrying authorization...");
                        continue;
                    }
                    return false;
                }
                
                // Simulate successful authorization
                this.lastTransactionId = generateMockTransactionId();
                transactionLog.put(lastTransactionId, "AUTHORIZED");
                logger.info("Card payment authorized successfully. TransactionID: " + lastTransactionId);
                return true;
                
            } catch (InterruptedException e) {
                logger.warning("Authorization interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    public boolean initiateUPITransaction(String upiId, double amount) {
        logger.info("Initiating UPI transaction: UPI=" + maskUPI(upiId) + ", Amount=" + amount);
        
        try {
            // Simulate transaction processing
            Thread.sleep(800);
            
            // Mock gateway response
            if (shouldDeclineTransaction()) {
                logger.warning("UPI transaction declined");
                return false;
            }
            
            this.lastTransactionId = generateMockTransactionId();
            transactionLog.put(lastTransactionId, "PROCESSED");
            logger.info("UPI transaction processed successfully. TransactionID: " + lastTransactionId);
            return true;
            
        } catch (InterruptedException e) {
            logger.warning("UPI transaction interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public boolean refundTransaction(String transactionId, double amount) {
        logger.info("Processing refund for transactionID: " + transactionId + ", Amount: " + amount);
        
        if (!transactionLog.containsKey(transactionId)) {
            logger.warning("Transaction not found: " + transactionId);
            return false;
        }
        
        try {
            Thread.sleep(600);
            
            transactionLog.put(transactionId, "REFUNDED");
            String refundId = "REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            logger.info("Refund processed successfully. RefundID: " + refundId);
            return true;
            
        } catch (InterruptedException e) {
            logger.warning("Refund processing interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public String getTransactionStatus(String transactionId) {
        return transactionLog.getOrDefault(transactionId, "NOT_FOUND");
    }
    
    public String getLastTransactionId() {
        return lastTransactionId;
    }
    
    private boolean shouldDeclineTransaction() {
        Random random = new Random();
        return random.nextDouble() < DECLINE_PROBABILITY;
    }
    
    private String generateMockTransactionId() {
        return "MOCK-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String maskUPI(String upiId) {
        if (upiId == null || upiId.length() < 8) {
            return "****";
        }
        int atIndex = upiId.indexOf('@');
        if (atIndex > 2) {
            return upiId.substring(0, 2) + "****" + upiId.substring(atIndex);
        }
        return "****" + upiId.substring(atIndex);
    }
    
    public Map<String, String> getTransactionLog() {
        return new HashMap<>(transactionLog);
    }
    
    @Override
    public String toString() {
        return "PaymentGateway{" +
                "lastTransactionId='" + lastTransactionId + '\'' +
                ", transactionLogSize=" + transactionLog.size() +
                '}';
    }
}
