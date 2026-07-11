# Payment Processing System Documentation

## Overview
The Payment Processing System is a comprehensive Java-based framework designed for the Bereeze Footwear POS billing system. It supports multiple payment methods with robust validation, error handling, and transaction tracking.

## System Architecture

### Core Components

#### 1. **PaymentMethod.java** (Abstract Base Class)
The foundation of the payment system providing common interface for all payment types.

**Key Features:**
- Abstract methods: `processPayment()`, `validatePayment()`, `getTransactionId()`
- Common properties: `paymentDate`, `status`, `transactionReference`
- Methods: `getPaymentMethodName()`, `getPaymentStatus()`

**Usage Example:**
```java
PaymentMethod payment = new CashPayment(billAmount);
payment.processPayment(receivedAmount);
```

---

#### 2. **CashPayment.java**
Handles cash transactions with automatic change calculation and receipt generation.

**Key Features:**
- Received amount tracking
- Automatic change calculation
- Receipt number generation
- No validation requirements
- Receipt generation with detailed breakdown

**Methods:**
- `processPayment(double amount)` - Validates received amount against bill
- `validatePayment()` - Checks received amount validity
- `generateReceipt()` - Creates formatted cash receipt
- `getChangeAmount()` - Returns calculated change

**Example:**
```java
CashPayment cash = new CashPayment(billAmount);
cash.setReceivedAmount(receivedAmount);
if (cash.processPayment(receivedAmount)) {
    System.out.println(cash.generateReceipt());
}
```

---

#### 3. **CardPayment.java**
Secure credit/debit card payment processing with comprehensive validation.

**Key Features:**
- Card type detection (VISA, MASTERCARD, AMEX)
- Luhn algorithm validation
- CVV validation (3-4 digits based on card type)
- Expiry date validation
- Card number masking for security
- Mock payment gateway integration
- Authorization code generation

**Supported Card Types:**
- VISA: 13/16 digits starting with 4
- MASTERCARD: 16 digits, 51-55
- AMEX: 15 digits, 34/37

**Methods:**
- `validatePayment()` - Full validation including Luhn check
- `processPayment(double amount)` - Gateway authorization
- `maskCardNumber()` - Returns masked card (****-****-****-XXXX)
- `getAuthorizationCode()` - Returns authorization code

**Example:**
```java
CardPayment card = new CardPayment("4532123456789012", "John Doe", "123", 12, 2025);
if (card.processPayment(amount)) {
    System.out.println("Card: " + card.maskCardNumber());
    System.out.println("Auth Code: " + card.getAuthorizationCode());
}
```

---

#### 4. **DigitalPayment.java**
UPI and mobile wallet payment with OTP verification and timeout handling.

**Key Features:**
- UPI ID validation
- Mobile number validation
- OTP generation and verification
- OTP timeout handling (5 minutes)
- Maximum 3 OTP attempts
- UPI ID masking
- Mock gateway integration
- Transaction ID from gateway

**Methods:**
- `validatePayment()` - Validates UPI format and mobile number
- `processPayment(double amount)` - Requires OTP verification
- `generateOTP()` - Generates 6-digit OTP
- `verifyOTP(String enteredOTP)` - Validates OTP with timeout
- `simulateOTPVerification()` - For testing purposes
- `maskUPI()` - Returns masked UPI (XX****@bank)

**Example:**
```java
DigitalPayment upi = new DigitalPayment("user@upi", "9876543210");
if (upi.validatePayment()) {
    upi.simulateOTPVerification();
    if (upi.processPayment(amount)) {
        System.out.println("UPI: " + upi.maskUPI());
    }
}
```

---

#### 5. **ChequePayment.java**
Cheque payment processing with comprehensive validation and tracking.

**Key Features:**
- Cheque number validation (6-10 digits)
- IFSC code validation (standard format)
- Account number validation (9-18 digits)
- Cheque date validation
- Post-dated cheque support (max 6 months)
- Cheque status tracking (PENDING, CLEARED, BOUNCED)
- Cheque amount validation

**Statuses:**
- `PENDING` - Awaiting clearance
- `CLEARED` - Successfully cleared
- `BOUNCED` - Payment failed

**Methods:**
- `validatePayment()` - Complete cheque validation
- `processPayment(double amount)` - Records cheque transaction
- `clearCheque()` - Marks cheque as cleared
- `bounceCheque(String reason)` - Records bounced cheque
- `isPostDated()` - Checks if cheque is post-dated
- `getDaysUntilChequeDate()` - Returns days to cheque maturity

**Example:**
```java
ChequePayment cheque = new ChequePayment(
    "123456", "HDFC Bank", "HDFC0000001", "9876543210123",
    LocalDate.now().plusDays(10), 5000.00
);
if (cheque.processPayment(5000.00)) {
    cheque.clearCheque(); // When cheque clears
}
```

---

#### 6. **CreditPayment.java**
Customer credit payment with approval workflow and outstanding amount tracking.

**Key Features:**
- Customer credit approval workflow
- Credit limit validation
- Outstanding amount tracking
- Due date auto-setting (30 days)
- Terms and conditions agreement flag
- Overdue tracking
- Payment recording against outstanding amount

**Approval Workflow:**
1. Approval status must be set to "APPROVED"
2. Terms and conditions must be accepted
3. Amount must not exceed available credit

**Methods:**
- `validatePayment()` - Basic validation
- `processPayment(double amount)` - Full workflow validation
- `acceptTermsAndConditions()` / `rejectTermsAndConditions()`
- `approveCreditLimit()` / `rejectCreditLimit()`
- `recordPayment(double amount)` - Records payment against outstanding
- `getAvailableCredit()` - Returns remaining credit limit
- `isOverdue()` - Checks if payment is overdue

**Example:**
```java
CreditPayment credit = new CreditPayment("CUST001", 50000.0, 5000.0);
credit.approveCreditLimit();
credit.acceptTermsAndConditions();
if (credit.processPayment(5000.0)) {
    System.out.println("Outstanding: " + credit.getOutstandingAmount());
    System.out.println("Due Date: " + credit.getDueDate());
}
```

---

#### 7. **PaymentGateway.java**
Mock payment gateway simulating real-world authorization and transaction processing.

**Key Features:**
- Card payment authorization simulation
- UPI transaction processing simulation
- Refund processing
- Transaction status tracking
- Retry logic (3 attempts)
- Mock transaction ID generation
- 10% decline probability for testing

**Methods:**
- `authorizeCardPayment(String card, double amount, String cvv)` - With retry
- `initiateUPITransaction(String upi, double amount)` - Async processing
- `refundTransaction(String txnId, double amount)` - Refund handling
- `getTransactionStatus(String txnId)` - Status lookup
- `getLastTransactionId()` - Returns last processed transaction

**Example:**
```java
PaymentGateway gateway = new PaymentGateway();
if (gateway.authorizeCardPayment("4532123456789012", 5000.0, "123")) {
    String txnId = gateway.getLastTransactionId();
}
```

---

#### 8. **Transaction.java**
Complete transaction record with metadata and status tracking.

**Key Features:**
- Transaction ID, Bill ID, Customer ID tracking
- Payment method recording
- Amount and date tracking
- Transaction status enum
- Receipt data storage
- Metadata key-value pairs
- Status checking methods

**Transaction Statuses:**
- `PENDING` - Awaiting processing
- `SUCCESS` - Successfully processed
- `FAILED` - Processing failed
- `REFUNDED` - Refunded transaction
- `DECLINED` - Payment declined

**Methods:**
- `recordTransaction(String txnId, String ref, String status)` - Record details
- `setReceiptData(String data)` - Store receipt
- `addMetadata(String key, String value)` - Add custom data
- `isSuccessful()` / `isFailed()` / `isRefunded()` - Status checks
- `generateReceiptSummary()` - Receipt summary generation

**Example:**
```java
Transaction txn = new Transaction("BILL001", "CUST001", payment, 5000.0);
txn.recordTransaction("TXN123", "REF456", "SUCCESS");
txn.addMetadata("AUTH_CODE", "AUTH789");
System.out.println(txn.generateReceiptSummary());
```

---

#### 9. **PaymentProcessor.java**
Main payment processing orchestrator managing all payment operations.

**Key Features:**
- Single and split payment processing
- Refund processing
- Payment history tracking
- Transaction statistics
- Payment method-specific metadata
- Customer transaction grouping
- Receipt generation

**Methods:**

**Primary Operations:**
- `processPayment(Object bill, String billId, String customerId, PaymentMethod method, double amount)` 
  - Validates, processes, and records transaction
  
- `splitPayment(Object bill, String billId, String customerId, List<PaymentMethod> methods, List<Double> amounts)`
  - Process multiple payment methods for single bill
  
- `refundProcessing(String billId, String customerId, PaymentMethod method, double amount)`
  - Process refunds with transaction updates

**Query Methods:**
- `getPaymentHistory(String customerId)` - All transactions
- `getPaymentHistoryByStatus(String customerId, String status)` - Filtered
- `getTransactionById(String txnId)` - Find by transaction ID
- `findTransactionByBillId(String billId)` - Find by bill

**Statistics:**
- `getTotalPaymentsForCustomer(String customerId)` - Sum of payments
- `getSuccessfulTransactionCount(String customerId)` - Count
- `getFailedTransactionCount(String customerId)` - Count
- `getPaymentStatistics(String customerId)` - Complete stats map

**Example:**
```java
PaymentProcessor processor = new PaymentProcessor();

// Single payment
CashPayment cash = new CashPayment(5000.0);
cash.setReceivedAmount(5100.0);
processor.processPayment(bill, "BILL001", "CUST001", cash, 5000.0);

// Split payment
List<PaymentMethod> methods = Arrays.asList(
    new CashPayment(2500.0),
    new CardPayment("4532123456789012", "John", "123", 12, 2025)
);
List<Double> amounts = Arrays.asList(2500.0, 2500.0);
processor.splitPayment(bill, "BILL001", "CUST001", methods, amounts);

// Refund
processor.refundProcessing("BILL001", "CUST001", cash, 500.0);

// History
List<Transaction> history = processor.getPaymentHistory("CUST001");
Map<String, Object> stats = processor.getPaymentStatistics("CUST001");
```

---

## Error Handling & Validation

### Comprehensive Validation

**Card Payment:**
- Luhn algorithm check
- CVV format (3 digits for Visa/MC, 4 for AMEX)
- Expiry date validation (future date required)
- Card holder name validation

**Digital Payment (UPI):**
- UPI format: `username@bankname`
- Mobile number: 10 digits starting with 6-9
- OTP timeout: 5 minutes
- Maximum 3 OTP attempts

**Cheque Payment:**
- Cheque number: 6-10 digits
- IFSC format: 4 letters + 0 + 6 alphanumerics
- Account number: 9-18 digits
- Post-dated: Max 6 months from today
- Amount validation

**Credit Payment:**
- Customer ID validation
- Credit limit checking
- Outstanding amount tracking
- Terms agreement flag

### Exception Handling

All classes include comprehensive error handling:
- Try-catch blocks for network/gateway errors
- Validation before processing
- Transaction rollback on failure
- Detailed logging for debugging

### Logging

All classes use `java.util.logging.Logger`:
```
- INFO: Successful operations
- WARNING: Validation failures, insufficient funds
- SEVERE: System errors
```

---

## Integration Guide

### With Bill System
```java
Bill bill = new Bill(billId, customerId);
PaymentProcessor processor = new PaymentProcessor();

PaymentMethod payment = new CashPayment(bill.getTotalAmount());
processor.processPayment(bill, bill.getBillId(), bill.getCustomerId(), 
                        payment, bill.getTotalAmount());
```

### With Customer System
```java
Customer customer = customerDAO.getCustomer(customerId);
CreditPayment credit = new CreditPayment(customer.getId(), 
                                        customer.getCreditLimit(), 
                                        billAmount);
```

---

## Security Features

1. **Card Number Masking**
   - Only last 4 digits visible
   - Masked in logs and receipts

2. **UPI/Mobile Masking**
   - Partial masking of UPI ID
   - Mobile number not fully exposed

3. **Data Validation**
   - Input sanitization
   - Type checking
   - Range validation

4. **Secure OTP**
   - Time-limited (5 minutes)
   - Attempt limiting (3 tries)
   - Random generation

---

## Testing Scenarios

### Cash Payment
```java
CashPayment cash = new CashPayment(1000.0);
cash.setReceivedAmount(1000.0); // Exact amount
cash.setReceivedAmount(1500.0); // With change
cash.setReceivedAmount(500.0);  // Insufficient (fails)
```

### Card Payment
```java
// Valid VISA
CardPayment card1 = new CardPayment("4532123456789012", "John", "123", 12, 2025);

// Invalid VISA (will fail Luhn)
CardPayment card2 = new CardPayment("4532123456789010", "John", "123", 12, 2025);

// Expired card (will fail)
CardPayment card3 = new CardPayment("4532123456789012", "John", "123", 1, 2020);
```

### UPI Payment
```java
DigitalPayment upi = new DigitalPayment("user@okhdfcbank", "9876543210");
upi.simulateOTPVerification();
upi.verifyOTP("123456"); // Correct OTP
upi.verifyOTP("000000"); // Wrong OTP (retry)
```

### Cheque Payment
```java
ChequePayment cheque = new ChequePayment(
    "123456", "HDFC Bank", "HDFC0000001", "9876543210123",
    LocalDate.now().plusDays(10), 5000.0
);
cheque.clearCheque();
cheque.bounceCheque("Insufficient funds");
```

---

## Performance Considerations

1. **Transaction History**: Stored in memory; consider database for production
2. **Gateway Delays**: Simulated with Thread.sleep(); actual gateway integration needed
3. **Logging**: Asynchronous logging recommended for high-volume scenarios
4. **Split Payments**: Atomic operations recommended for consistency

---

## Future Enhancements

1. **Wallet Integration**: Multiple digital wallets
2. **EMI Support**: Installment payment plans
3. **Discount/Offers**: Payment-based promotions
4. **Analytics**: Payment trend analysis
5. **Reconciliation**: Bank statement matching
6. **Audit Trail**: Complete transaction audit
7. **Multi-currency**: Support for multiple currencies
8. **Tokenization**: Save payment methods

---

## File Locations

```
src/payment/
├── PaymentMethod.java
├── CashPayment.java
├── CardPayment.java
├── DigitalPayment.java
├── ChequePayment.java
├── CreditPayment.java
├── PaymentGateway.java
├── Transaction.java
└── PaymentProcessor.java
```

## Compilation

```bash
javac -d bin src/payment/*.java
```

## Class Diagram

```
                    PaymentMethod (Abstract)
                           ▲
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
    CashPayment      CardPayment      DigitalPayment
                           
    ChequePayment    CreditPayment

    PaymentGateway (Utility)
    Transaction (Record)
    PaymentProcessor (Orchestrator)
```

---

## Version History

- **v1.0** - Initial release
  - 9 payment classes
  - Mock gateway integration
  - Transaction tracking
  - Comprehensive validation

---

## Support & Maintenance

For issues or enhancements:
1. Check logs for detailed error messages
2. Verify input validation requirements
3. Test with sample data
4. Review transaction history for debugging

---

Generated: 2024
Payment System for Bereeze Footwear POS
