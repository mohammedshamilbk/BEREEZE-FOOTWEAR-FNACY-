package payment;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Payment System Quick Start and Examples
 * Demonstrates usage of all payment classes
 */
public class PaymentSystemExamples {
    
    public static void main(String[] args) {
        System.out.println("========== PAYMENT SYSTEM EXAMPLES ==========\n");
        
        // Initialize processor
        PaymentProcessor processor = new PaymentProcessor();
        
        // Example 1: Cash Payment
        System.out.println("1. CASH PAYMENT EXAMPLE");
        System.out.println("-".repeat(40));
        cashPaymentExample(processor);
        
        // Example 2: Card Payment
        System.out.println("\n2. CARD PAYMENT EXAMPLE");
        System.out.println("-".repeat(40));
        cardPaymentExample(processor);
        
        // Example 3: UPI/Digital Payment
        System.out.println("\n3. DIGITAL PAYMENT (UPI) EXAMPLE");
        System.out.println("-".repeat(40));
        digitalPaymentExample(processor);
        
        // Example 4: Cheque Payment
        System.out.println("\n4. CHEQUE PAYMENT EXAMPLE");
        System.out.println("-".repeat(40));
        chequePaymentExample(processor);
        
        // Example 5: Credit Payment
        System.out.println("\n5. CREDIT PAYMENT EXAMPLE");
        System.out.println("-".repeat(40));
        creditPaymentExample(processor);
        
        // Example 6: Split Payment
        System.out.println("\n6. SPLIT PAYMENT EXAMPLE");
        System.out.println("-".repeat(40));
        splitPaymentExample(processor);
        
        // Example 7: Refund Processing
        System.out.println("\n7. REFUND PROCESSING EXAMPLE");
        System.out.println("-".repeat(40));
        refundExample(processor);
        
        // Example 8: Payment Statistics
        System.out.println("\n8. PAYMENT STATISTICS");
        System.out.println("-".repeat(40));
        paymentStatisticsExample(processor);
    }
    
    // ============ EXAMPLE 1: CASH PAYMENT ============
    private static void cashPaymentExample(PaymentProcessor processor) {
        double billAmount = 2500.00;
        CashPayment cash = new CashPayment(billAmount);
        
        // Scenario: Customer gives exact amount
        double receivedAmount = 2500.00;
        cash.setReceivedAmount(receivedAmount);
        
        if (cash.processPayment(receivedAmount)) {
            System.out.println("✓ Payment successful");
            System.out.println("  Received: ₹" + receivedAmount);
            System.out.println("  Bill: ₹" + billAmount);
            System.out.println("  Change: ₹" + cash.getChangeAmount());
            System.out.println("  Receipt:\n" + cash.generateReceipt());
            
            // Record in processor
            processor.processPayment(null, "BILL001", "CUST001", cash, billAmount);
        }
    }
    
    // ============ EXAMPLE 2: CARD PAYMENT ============
    private static void cardPaymentExample(PaymentProcessor processor) {
        String cardNumber = "4532123456789012"; // Valid VISA
        String cardHolder = "JOHN DOE";
        String cvv = "123";
        int month = 12;
        int year = 2025;
        double amount = 3500.00;
        
        CardPayment card = new CardPayment(cardNumber, cardHolder, cvv, month, year);
        
        System.out.println("Processing card payment...");
        System.out.println("  Card: " + card.maskCardNumber());
        System.out.println("  Holder: " + card.getCardHolderName());
        System.out.println("  Type: " + card.getCardType());
        
        if (card.validatePayment()) {
            if (card.processPayment(amount)) {
                System.out.println("✓ Payment authorized");
                System.out.println("  Amount: ₹" + amount);
                System.out.println("  Auth Code: " + card.getAuthorizationCode());
                System.out.println("  Transaction ID: " + card.getTransactionId());
                
                // Record in processor
                processor.processPayment(null, "BILL002", "CUST002", card, amount);
            } else {
                System.out.println("✗ Authorization declined");
            }
        } else {
            System.out.println("✗ Card validation failed");
        }
    }
    
    // ============ EXAMPLE 3: UPI/DIGITAL PAYMENT ============
    private static void digitalPaymentExample(PaymentProcessor processor) {
        String upiId = "customer@okhdfcbank";
        String mobileNumber = "9876543210";
        double amount = 1500.00;
        
        DigitalPayment upi = new DigitalPayment(upiId, mobileNumber);
        
        System.out.println("Processing UPI payment...");
        System.out.println("  UPI: " + upi.maskUPI());
        System.out.println("  Mobile: " + mobileNumber);
        System.out.println("  Amount: ₹" + amount);
        
        if (upi.validatePayment()) {
            System.out.println("✓ UPI validated");
            System.out.println("  OTP sent to registered mobile");
            
            // Simulate OTP verification (in real scenario, user enters OTP)
            upi.simulateOTPVerification();
            System.out.println("✓ OTP verified");
            
            if (upi.processPayment(amount)) {
                System.out.println("✓ Payment successful");
                System.out.println("  Transaction ID: " + upi.getTransactionId());
                System.out.println("  Gateway Txn: " + upi.getGatewayTransactionId());
                
                // Record in processor
                processor.processPayment(null, "BILL003", "CUST003", upi, amount);
            }
        }
    }
    
    // ============ EXAMPLE 4: CHEQUE PAYMENT ============
    private static void chequePaymentExample(PaymentProcessor processor) {
        String chequeNumber = "500123";
        String bankName = "HDFC Bank";
        String ifscCode = "HDFC0000001";
        String accountNumber = "123456789012";
        LocalDate chequeDate = LocalDate.now().plusDays(5);
        double amount = 5000.00;
        
        ChequePayment cheque = new ChequePayment(
            chequeNumber, bankName, ifscCode, accountNumber, chequeDate, amount
        );
        
        System.out.println("Processing cheque payment...");
        System.out.println("  Cheque: " + chequeNumber);
        System.out.println("  Bank: " + bankName);
        System.out.println("  Amount: ₹" + amount);
        System.out.println("  Date: " + chequeDate);
        System.out.println("  Post-dated: " + (cheque.isPostDated() ? "Yes" : "No"));
        
        if (cheque.validatePayment()) {
            if (cheque.processPayment(amount)) {
                System.out.println("✓ Cheque recorded");
                System.out.println("  Status: " + cheque.getChequeStatus());
                System.out.println("  Transaction ID: " + cheque.getTransactionId());
                
                // Simulate cheque clearing after 2-3 business days
                System.out.println("\n  [After clearance...]");
                cheque.clearCheque();
                System.out.println("✓ Cheque cleared");
                System.out.println("  Status: " + cheque.getChequeStatus());
                
                // Record in processor
                processor.processPayment(null, "BILL004", "CUST004", cheque, amount);
            }
        }
    }
    
    // ============ EXAMPLE 5: CREDIT PAYMENT ============
    private static void creditPaymentExample(PaymentProcessor processor) {
        String customerId = "CUST005";
        double creditLimit = 100000.00;
        double billAmount = 15000.00;
        
        CreditPayment credit = new CreditPayment(customerId, creditLimit, billAmount);
        
        System.out.println("Processing credit payment...");
        System.out.println("  Customer: " + customerId);
        System.out.println("  Credit Limit: ₹" + creditLimit);
        System.out.println("  Bill Amount: ₹" + billAmount);
        
        // Step 1: Approve credit limit
        credit.approveCreditLimit();
        System.out.println("✓ Credit limit approved");
        
        // Step 2: Accept terms
        credit.acceptTermsAndConditions();
        System.out.println("✓ Terms & conditions accepted");
        
        // Step 3: Process payment
        if (credit.processPayment(billAmount)) {
            System.out.println("✓ Payment recorded on credit");
            System.out.println("  Outstanding: ₹" + credit.getOutstandingAmount());
            System.out.println("  Available Credit: ₹" + credit.getAvailableCredit());
            System.out.println("  Due Date: " + credit.getDueDate());
            
            // Record in processor
            processor.processPayment(null, "BILL005", customerId, credit, billAmount);
        }
    }
    
    // ============ EXAMPLE 6: SPLIT PAYMENT ============
    private static void splitPaymentExample(PaymentProcessor processor) {
        double totalAmount = 5000.00;
        double cashAmount = 2500.00;
        double cardAmount = 2500.00;
        
        System.out.println("Processing split payment...");
        System.out.println("  Total: ₹" + totalAmount);
        System.out.println("  Cash: ₹" + cashAmount);
        System.out.println("  Card: ₹" + cardAmount);
        
        // Cash payment
        CashPayment cash = new CashPayment(cashAmount);
        cash.setReceivedAmount(cashAmount);
        
        // Card payment
        CardPayment card = new CardPayment(
            "5412345678901234", "JANE SMITH", "456", 6, 2026
        );
        
        // Create list of payment methods and amounts
        List<PaymentMethod> methods = Arrays.asList(cash, card);
        List<Double> amounts = Arrays.asList(cashAmount, cardAmount);
        
        if (processor.splitPayment(null, "BILL006", "CUST006", methods, amounts)) {
            System.out.println("✓ Split payment successful");
            System.out.println("  Cash: " + cash.getPaymentStatus());
            System.out.println("  Card: " + card.getPaymentStatus());
        }
    }
    
    // ============ EXAMPLE 7: REFUND PROCESSING ============
    private static void refundExample(PaymentProcessor processor) {
        System.out.println("Processing refund...");
        System.out.println("  Original Bill: BILL002");
        System.out.println("  Refund Amount: ₹700.00");
        
        // Get original transaction
        List<Transaction> allTxns = processor.getAllTransactions();
        if (allTxns.size() > 0) {
            Transaction originalTxn = allTxns.get(0);
            
            PaymentMethod refundMethod = originalTxn.getPaymentMethod();
            if (processor.refundProcessing("BILL002", "CUST002", refundMethod, 700.00)) {
                System.out.println("✓ Refund processed successfully");
                System.out.println("  Original Transaction: " + originalTxn.getTransactionId());
                System.out.println("  Refund Status: " + originalTxn.getStatus());
            }
        }
    }
    
    // ============ EXAMPLE 8: PAYMENT STATISTICS ============
    private static void paymentStatisticsExample(PaymentProcessor processor) {
        String customerId = "CUST001";
        
        // Get statistics
        Map<String, Object> stats = processor.getPaymentStatistics(customerId);
        
        System.out.println("Payment Statistics for: " + customerId);
        System.out.println("-".repeat(40));
        
        stats.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
        
        // Get payment history
        List<Transaction> history = processor.getPaymentHistory(customerId);
        System.out.println("\n  Total Transactions: " + history.size());
        
        // Get successful transactions
        List<Transaction> successful = processor.getPaymentHistoryByStatus(
            customerId, 
            Transaction.TransactionStatus.SUCCESS.getValue()
        );
        System.out.println("  Successful: " + successful.size());
        
        // Get failed transactions
        List<Transaction> failed = processor.getPaymentHistoryByStatus(
            customerId, 
            Transaction.TransactionStatus.FAILED.getValue()
        );
        System.out.println("  Failed: " + failed.size());
    }
}
