package payment;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class CardPayment extends PaymentMethod {
    private static final Logger logger = Logger.getLogger(CardPayment.class.getName());
    
    private String cardNumber;
    private String cardHolderName;
    private String cvv;
    private YearMonth expiryDate;
    private String cardType;
    private String authorizationCode;
    
    // Card type patterns
    private static final Pattern VISA_PATTERN = Pattern.compile("^4[0-9]{12}(?:[0-9]{3})?$");
    private static final Pattern MASTERCARD_PATTERN = Pattern.compile("^5[1-5][0-9]{14}$");
    private static final Pattern AMEX_PATTERN = Pattern.compile("^3[47][0-9]{13}$");
    
    public CardPayment(String cardNumber, String cardHolderName, String cvv, int expiryMonth, int expiryYear) {
        super();
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.cvv = cvv;
        this.expiryDate = YearMonth.of(expiryYear, expiryMonth);
        this.cardType = detectCardType(cardNumber);
    }
    
    @Override
    public boolean processPayment(double amount) {
        if (!validatePayment()) {
            status = "FAILED";
            logger.warning("Payment validation failed");
            return false;
        }
        
        // Mock payment gateway integration
        PaymentGateway gateway = new PaymentGateway();
        if (gateway.authorizeCardPayment(cardNumber, amount, cvv)) {
            this.status = "SUCCESS";
            this.authorizationCode = generateAuthorizationCode();
            this.transactionReference = generateTransactionId();
            logger.info("Card payment authorized. Card: " + maskCardNumber() + ", Amount: " + amount);
            return true;
        } else {
            this.status = "DECLINED";
            logger.warning("Card payment declined");
            return false;
        }
    }
    
    @Override
    public boolean validatePayment() {
        if (!isCardNumberValid()) {
            logger.warning("Invalid card number");
            return false;
        }
        
        if (!isCVVValid()) {
            logger.warning("Invalid CVV");
            return false;
        }
        
        if (!isExpiryDateValid()) {
            logger.warning("Card expired or invalid expiry date");
            return false;
        }
        
        if (cardHolderName == null || cardHolderName.trim().isEmpty()) {
            logger.warning("Invalid card holder name");
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
        return "CARD (" + cardType + ")";
    }
    
    private boolean isCardNumberValid() {
        String cleanNumber = cardNumber.replaceAll("\\s", "");
        return luhnCheck(cleanNumber) && isValidCardType(cleanNumber);
    }
    
    private boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return sum % 10 == 0;
    }
    
    private boolean isValidCardType(String cleanNumber) {
        return VISA_PATTERN.matcher(cleanNumber).matches() ||
               MASTERCARD_PATTERN.matcher(cleanNumber).matches() ||
               AMEX_PATTERN.matcher(cleanNumber).matches();
    }
    
    private boolean isCVVValid() {
        String cleanCVV = cvv.replaceAll("\\D", "");
        if (cardType.equals("AMEX")) {
            return cleanCVV.length() == 4;
        }
        return cleanCVV.length() == 3;
    }
    
    private boolean isExpiryDateValid() {
        return expiryDate.isAfter(YearMonth.now());
    }
    
    private String detectCardType(String cardNumber) {
        String cleanNumber = cardNumber.replaceAll("\\s", "");
        
        if (VISA_PATTERN.matcher(cleanNumber).matches()) {
            return "VISA";
        } else if (MASTERCARD_PATTERN.matcher(cleanNumber).matches()) {
            return "MASTERCARD";
        } else if (AMEX_PATTERN.matcher(cleanNumber).matches()) {
            return "AMEX";
        }
        
        return "UNKNOWN";
    }
    
    public String maskCardNumber() {
        String clean = cardNumber.replaceAll("\\s", "");
        if (clean.length() < 4) {
            return "****";
        }
        return "**** **** **** " + clean.substring(clean.length() - 4);
    }
    
    private String generateAuthorizationCode() {
        return "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String generateTransactionId() {
        return "TXN-CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    public String getCardType() {
        return cardType;
    }
    
    public String getCardHolderName() {
        return cardHolderName;
    }
    
    public String getAuthorizationCode() {
        return authorizationCode;
    }
    
    public YearMonth getExpiryDate() {
        return expiryDate;
    }
    
    @Override
    public String toString() {
        return "CardPayment{" +
                "cardNumber='" + maskCardNumber() + '\'' +
                ", cardHolderName='" + cardHolderName + '\'' +
                ", cardType='" + cardType + '\'' +
                ", expiryDate=" + expiryDate +
                ", status='" + status + '\'' +
                ", authorizationCode='" + authorizationCode + '\'' +
                '}';
    }
}
