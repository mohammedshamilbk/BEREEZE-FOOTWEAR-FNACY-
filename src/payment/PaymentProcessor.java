package payment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.UUID;

public class PaymentProcessor {
    private static final Logger logger = Logger.getLogger(PaymentProcessor.class.getName());
    
    private List<Transaction> transactionHistory;
    private Map<String, List<Transaction>> customerTransactions;
    private PaymentGateway paymentGateway;
    
    public PaymentProcessor() {
        this.transactionHistory = new ArrayList<>();
        this.customerTransactions = new HashMap<>();
        this.paymentGateway = new PaymentGateway();
    }
    
    public boolean processPayment(Object bill, String billId, String customerId, PaymentMethod paymentMethod, double amount) {
        logger.info("Processing payment for Bill: " + billId + ", Amount: " + amount + 
                   ", Method: " + paymentMethod.getPaymentMethodName());
        
        // Create transaction record
        Transaction transaction = new Transaction(billId, customerId, paymentMethod, amount);
        
        // Validate payment method
        if (!paymentMethod.validatePayment()) {
            logger.warning("Payment method validation failed");
            transaction.setStatus(Transaction.TransactionStatus.FAILED.getValue());
            recordTransaction(transaction, customerId);
            return false;
        }
        
        // Process payment
        if (!paymentMethod.processPayment(amount)) {
            logger.warning("Payment processing failed");
            transaction.setStatus(Transaction.TransactionStatus.FAILED.getValue());
            recordTransaction(transaction, customerId);
            return false;
        }
        
        // Record successful transaction
        String transactionId = paymentMethod.getTransactionId();
        String referenceNumber = paymentMethod.getTransactionReference();
        transaction.recordTransaction(transactionId, referenceNumber, 
                                     Transaction.TransactionStatus.SUCCESS.getValue());
        
        // Generate receipt for cash payment
        if (paymentMethod instanceof CashPayment) {
            CashPayment cashPayment = (CashPayment) paymentMethod;
            String receipt = cashPayment.generateReceipt();
            transaction.setReceiptData(receipt);
            transaction.addMetadata("RECEIPT_NUMBER", cashPayment.getReceiptNumber());
            transaction.addMetadata("CHANGE_AMOUNT", String.valueOf(cashPayment.getChangeAmount()));
        }
        
        // Handle Digital Payment OTP
        if (paymentMethod instanceof DigitalPayment) {
            DigitalPayment digitalPayment = (DigitalPayment) paymentMethod;
            transaction.addMetadata("UPI_ID", digitalPayment.maskUPI());
            transaction.addMetadata("GATEWAY_TXN_ID", digitalPayment.getGatewayTransactionId());
        }
        
        // Handle Card Payment
        if (paymentMethod instanceof CardPayment) {
            CardPayment cardPayment = (CardPayment) paymentMethod;
            transaction.addMetadata("CARD_LAST_4", cardPayment.maskCardNumber());
            transaction.addMetadata("AUTH_CODE", cardPayment.getAuthorizationCode());
        }
        
        // Handle Cheque Payment
        if (paymentMethod instanceof ChequePayment) {
            ChequePayment chequePayment = (ChequePayment) paymentMethod;
            transaction.addMetadata("CHEQUE_NUMBER", chequePayment.getChequeNumber());
            transaction.addMetadata("CHEQUE_STATUS", chequePayment.getChequeStatus());
        }
        
        // Handle Credit Payment
        if (paymentMethod instanceof CreditPayment) {
            CreditPayment creditPayment = (CreditPayment) paymentMethod;
            transaction.addMetadata("OUTSTANDING_AMOUNT", String.valueOf(creditPayment.getOutstandingAmount()));
            transaction.addMetadata("DUE_DATE", String.valueOf(creditPayment.getDueDate()));
        }
        
        recordTransaction(transaction, customerId);
        logger.info("Payment processed successfully. TransactionID: " + transactionId);
        return true;
    }
    
    public boolean splitPayment(Object bill, String billId, String customerId, 
                               List<PaymentMethod> paymentMethods, List<Double> amounts) {
        if (paymentMethods.size() != amounts.size()) {
            logger.warning("Payment methods count does not match amounts count");
            return false;
        }
        
        double totalAmount = 0;
        for (Double amount : amounts) {
            totalAmount += amount;
        }
        
        logger.info("Processing split payment. Bill: " + billId + ", Total: " + totalAmount + 
                   ", Methods: " + paymentMethods.size());
        
        boolean allSuccessful = true;
        
        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod method = paymentMethods.get(i);
            double amount = amounts.get(i);
            
            if (!processPayment(bill, billId, customerId, method, amount)) {
                logger.warning("Split payment failed for method: " + method.getPaymentMethodName());
                allSuccessful = false;
            }
        }
        
        if (allSuccessful) {
            logger.info("Split payment completed successfully");
        }
        
        return allSuccessful;
    }
    
    public boolean refundProcessing(String billId, String customerId, PaymentMethod paymentMethod, double refundAmount) {
        logger.info("Processing refund for Bill: " + billId + ", Refund Amount: " + refundAmount);
        
        // Find original transaction
        Transaction originalTransaction = findTransactionByBillId(billId);
        if (originalTransaction == null) {
            logger.warning("Original transaction not found for Bill: " + billId);
            return false;
        }
        
        // Create refund transaction
        Transaction refundTransaction = new Transaction(billId, customerId, paymentMethod, refundAmount);
        
        try {
            // Process refund through payment gateway
            if (paymentGateway.refundTransaction(originalTransaction.getTransactionId(), refundAmount)) {
                refundTransaction.recordTransaction(
                    "REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    "REF-" + originalTransaction.getReferenceNumber(),
                    Transaction.TransactionStatus.REFUNDED.getValue()
                );
                
                // Update original transaction status
                originalTransaction.setStatus(Transaction.TransactionStatus.REFUNDED.getValue());
                
                recordTransaction(refundTransaction, customerId);
                logger.info("Refund processed successfully. Amount: " + refundAmount);
                return true;
            }
        } catch (Exception e) {
            logger.warning("Refund processing exception: " + e.getMessage());
        }
        
        refundTransaction.setStatus(Transaction.TransactionStatus.FAILED.getValue());
        recordTransaction(refundTransaction, customerId);
        return false;
    }
    
    public String generatePaymentReceipt(Transaction transaction) {
        if (transaction == null) {
            logger.warning("Transaction is null, cannot generate receipt");
            return "";
        }
        
        return transaction.generateReceiptSummary();
    }
    
    public List<Transaction> getPaymentHistory(String customerId) {
        logger.info("Retrieving payment history for customer: " + customerId);
        return customerTransactions.getOrDefault(customerId, new ArrayList<>());
    }
    
    public List<Transaction> getPaymentHistoryByStatus(String customerId, String status) {
        List<Transaction> history = getPaymentHistory(customerId);
        List<Transaction> filtered = new ArrayList<>();
        
        for (Transaction txn : history) {
            if (txn.getStatus().equals(status)) {
                filtered.add(txn);
            }
        }
        
        return filtered;
    }
    
    public Transaction getTransactionById(String transactionId) {
        for (Transaction txn : transactionHistory) {
            if (txn.getTransactionId().equals(transactionId)) {
                return txn;
            }
        }
        return null;
    }
    
    public Transaction findTransactionByBillId(String billId) {
        for (Transaction txn : transactionHistory) {
            if (txn.getBillId().equals(billId)) {
                return txn;
            }
        }
        return null;
    }
    
    public double getTotalPaymentsForCustomer(String customerId) {
        List<Transaction> history = getPaymentHistory(customerId);
        double total = 0;
        
        for (Transaction txn : history) {
            if (txn.isSuccessful()) {
                total += txn.getAmount();
            }
        }
        
        return total;
    }
    
    public int getSuccessfulTransactionCount(String customerId) {
        List<Transaction> history = getPaymentHistoryByStatus(customerId, 
                                                              Transaction.TransactionStatus.SUCCESS.getValue());
        return history.size();
    }
    
    public int getFailedTransactionCount(String customerId) {
        List<Transaction> failedTxns = getPaymentHistoryByStatus(customerId, 
                                                                Transaction.TransactionStatus.FAILED.getValue());
        List<Transaction> declinedTxns = getPaymentHistoryByStatus(customerId, 
                                                                  Transaction.TransactionStatus.DECLINED.getValue());
        return failedTxns.size() + declinedTxns.size();
    }
    
    private void recordTransaction(Transaction transaction, String customerId) {
        transactionHistory.add(transaction);
        
        if (!customerTransactions.containsKey(customerId)) {
            customerTransactions.put(customerId, new ArrayList<>());
        }
        customerTransactions.get(customerId).add(transaction);
        
        logger.info("Transaction recorded for customer: " + customerId);
    }
    
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactionHistory);
    }
    
    public Map<String, Object> getPaymentStatistics(String customerId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Transaction> history = getPaymentHistory(customerId);
        
        double totalPayments = getTotalPaymentsForCustomer(customerId);
        int successfulCount = getSuccessfulTransactionCount(customerId);
        int failedCount = getFailedTransactionCount(customerId);
        
        stats.put("CUSTOMER_ID", customerId);
        stats.put("TOTAL_TRANSACTIONS", history.size());
        stats.put("SUCCESSFUL_TRANSACTIONS", successfulCount);
        stats.put("FAILED_TRANSACTIONS", failedCount);
        stats.put("TOTAL_AMOUNT_PAID", totalPayments);
        stats.put("LAST_TRANSACTION_DATE", history.isEmpty() ? "N/A" : 
                 history.get(history.size() - 1).getTransactionDate());
        
        return stats;
    }
    
    @Override
    public String toString() {
        return "PaymentProcessor{" +
                "totalTransactions=" + transactionHistory.size() +
                ", customerCount=" + customerTransactions.size() +
                '}';
    }
}
