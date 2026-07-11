package payment;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

public class CashPayment extends PaymentMethod {
    private static final Logger logger = Logger.getLogger(CashPayment.class.getName());
    
    private double receivedAmount;
    private double billAmount;
    private double changeAmount;
    private String receiptNumber;
    
    public CashPayment(double billAmount) {
        super();
        this.billAmount = billAmount;
        this.changeAmount = 0;
        this.receiptNumber = generateReceiptNumber();
    }
    
    @Override
    public boolean processPayment(double amount) {
        if (receivedAmount < billAmount) {
            logger.warning("Insufficient cash received. Bill: " + billAmount + ", Received: " + amount);
            status = "FAILED";
            return false;
        }
        
        this.receivedAmount = amount;
        calculateChange();
        this.status = "SUCCESS";
        this.transactionReference = generateTransactionId();
        
        logger.info("Cash payment processed successfully. Amount: " + amount + ", Change: " + changeAmount);
        return true;
    }
    
    @Override
    public boolean validatePayment() {
        if (receivedAmount <= 0) {
            logger.warning("Invalid received amount: " + receivedAmount);
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
        return "CASH";
    }
    
    public void setReceivedAmount(double amount) {
        this.receivedAmount = amount;
    }
    
    public double getReceivedAmount() {
        return receivedAmount;
    }
    
    public double getChangeAmount() {
        return changeAmount;
    }
    
    private void calculateChange() {
        this.changeAmount = receivedAmount - billAmount;
    }
    
    public String generateReceipt() {
        StringBuilder receipt = new StringBuilder();
        receipt.append("========== CASH RECEIPT ==========\n");
        receipt.append("Receipt Number: ").append(receiptNumber).append("\n");
        receipt.append("Date & Time: ").append(paymentDate).append("\n");
        receipt.append("Bill Amount: ₹").append(String.format("%.2f", billAmount)).append("\n");
        receipt.append("Amount Received: ₹").append(String.format("%.2f", receivedAmount)).append("\n");
        receipt.append("Change: ₹").append(String.format("%.2f", changeAmount)).append("\n");
        receipt.append("Transaction ID: ").append(transactionReference).append("\n");
        receipt.append("Status: ").append(status).append("\n");
        receipt.append("==================================\n");
        
        logger.info("Receipt generated: " + receiptNumber);
        return receipt.toString();
    }
    
    public String getReceiptNumber() {
        return receiptNumber;
    }
    
    private String generateReceiptNumber() {
        return "CASH-" + System.currentTimeMillis();
    }
    
    private String generateTransactionId() {
        return "TXN-CASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    @Override
    public String toString() {
        return "CashPayment{" +
                "receivedAmount=" + receivedAmount +
                ", billAmount=" + billAmount +
                ", changeAmount=" + changeAmount +
                ", receiptNumber='" + receiptNumber + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
