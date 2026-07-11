package payment;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class ChequePayment extends PaymentMethod {
    private static final Logger logger = Logger.getLogger(ChequePayment.class.getName());
    
    private String chequeNumber;
    private String bankName;
    private String ifscCode;
    private String accountNumber;
    private LocalDate chequeDate;
    private String chequeStatus;
    private double chequeAmount;
    private static final int MAX_POSTDATED_DAYS = 180; // 6 months
    
    // Cheque status tracking
    private static final Map<String, String> chequeStatusMap = new HashMap<>();
    
    public ChequePayment(String chequeNumber, String bankName, String ifscCode, 
                         String accountNumber, LocalDate chequeDate, double chequeAmount) {
        super();
        this.chequeNumber = chequeNumber;
        this.bankName = bankName;
        this.ifscCode = ifscCode;
        this.accountNumber = accountNumber;
        this.chequeDate = chequeDate;
        this.chequeAmount = chequeAmount;
        this.chequeStatus = "PENDING";
    }
    
    @Override
    public boolean processPayment(double amount) {
        if (!validatePayment()) {
            status = "FAILED";
            logger.warning("Cheque payment validation failed");
            return false;
        }
        
        if (Math.abs(amount - chequeAmount) > 0.01) {
            logger.warning("Cheque amount does not match bill amount. Cheque: " + chequeAmount + ", Bill: " + amount);
            status = "FAILED";
            return false;
        }
        
        this.status = "PENDING";
        this.chequeStatus = "PENDING";
        this.transactionReference = generateTransactionId();
        chequeStatusMap.put(chequeNumber, chequeStatus);
        
        logger.info("Cheque payment recorded. Cheque: " + chequeNumber + ", Amount: " + amount);
        return true;
    }
    
    @Override
    public boolean validatePayment() {
        if (!isChequeNumberValid()) {
            logger.warning("Invalid cheque number");
            return false;
        }
        
        if (!isChequeeDateValid()) {
            logger.warning("Invalid cheque date or post-dated beyond 6 months");
            return false;
        }
        
        if (bankName == null || bankName.trim().isEmpty()) {
            logger.warning("Bank name is required");
            return false;
        }
        
        if (!isIFSCValid()) {
            logger.warning("Invalid IFSC code");
            return false;
        }
        
        if (!isAccountNumberValid()) {
            logger.warning("Invalid account number");
            return false;
        }
        
        if (chequeAmount <= 0) {
            logger.warning("Invalid cheque amount");
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
        return "CHEQUE";
    }
    
    private boolean isChequeNumberValid() {
        String clean = chequeNumber.replaceAll("\\D", "");
        return clean.length() >= 6 && clean.length() <= 10;
    }
    
    private boolean isChequeeDateValid() {
        LocalDate today = LocalDate.now();
        
        // Cheque should not be dated before today
        if (chequeDate.isBefore(today)) {
            return false;
        }
        
        // Check if post-dated within allowed period (6 months)
        long daysPostdated = ChronoUnit.DAYS.between(today, chequeDate);
        return daysPostdated <= MAX_POSTDATED_DAYS;
    }
    
    private boolean isIFSCValid() {
        if (ifscCode == null) {
            return false;
        }
        // IFSC code format: 4 letters + 0 + 6 digits
        return ifscCode.matches("^[A-Z]{4}0[A-Z0-9]{6}$");
    }
    
    private boolean isAccountNumberValid() {
        if (accountNumber == null) {
            return false;
        }
        // Account number should be 9-18 digits
        String clean = accountNumber.replaceAll("\\D", "");
        return clean.length() >= 9 && clean.length() <= 18;
    }
    
    public void clearCheque() {
        this.chequeStatus = "CLEARED";
        this.status = "SUCCESS";
        chequeStatusMap.put(chequeNumber, chequeStatus);
        logger.info("Cheque cleared: " + chequeNumber);
    }
    
    public void bounceCheque(String reason) {
        this.chequeStatus = "BOUNCED";
        this.status = "FAILED";
        chequeStatusMap.put(chequeNumber, chequeStatus);
        logger.warning("Cheque bounced: " + chequeNumber + ", Reason: " + reason);
    }
    
    private String generateTransactionId() {
        return "CHQ-" + chequeNumber + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    public String getChequeNumber() {
        return chequeNumber;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public String getIfscCode() {
        return ifscCode;
    }
    
    public LocalDate getChequeDate() {
        return chequeDate;
    }
    
    public String getChequeStatus() {
        return chequeStatus;
    }
    
    public double getChequeAmount() {
        return chequeAmount;
    }
    
    public static String getChequeStatusFromMap(String chequeNumber) {
        return chequeStatusMap.getOrDefault(chequeNumber, "NOT_FOUND");
    }
    
    public boolean isPostDated() {
        return chequeDate.isAfter(LocalDate.now());
    }
    
    public long getDaysUntilChequeDate() {
        return ChronoUnit.DAYS.between(LocalDate.now(), chequeDate);
    }
    
    @Override
    public String toString() {
        return "ChequePayment{" +
                "chequeNumber='" + chequeNumber + '\'' +
                ", bankName='" + bankName + '\'' +
                ", chequeDate=" + chequeDate +
                ", chequeAmount=" + chequeAmount +
                ", chequeStatus='" + chequeStatus + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
