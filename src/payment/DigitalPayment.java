package payment;

import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DigitalPayment extends PaymentMethod {
    private static final Logger logger = Logger.getLogger(DigitalPayment.class.getName());
    
    private String upiId;
    private String mobileNumber;
    private String gatewayTransactionId;
    private String otp;
    private boolean otpVerified;
    private int otpAttempts;
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final long OTP_TIMEOUT_SECONDS = 300;
    private long otpGeneratedTime;
    
    private static final Pattern UPI_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z]+$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    
    public DigitalPayment(String upiId, String mobileNumber) {
        super();
        this.upiId = upiId;
        this.mobileNumber = mobileNumber;
        this.otpVerified = false;
        this.otpAttempts = 0;
    }
    
    @Override
    public boolean processPayment(double amount) {
        if (!validatePayment()) {
            status = "FAILED";
            logger.warning("Digital payment validation failed");
            return false;
        }
        
        // Generate OTP
        generateOTP();
        logger.info("OTP generated for UPI: " + maskUPI());
        
        // In real scenario, OTP would be sent to mobile number
        // For now, simulating OTP verification
        if (!otpVerified) {
            logger.warning("OTP verification required for payment");
            return false;
        }
        
        // Mock gateway transaction
        PaymentGateway gateway = new PaymentGateway();
        if (gateway.initiateUPITransaction(upiId, amount)) {
            this.gatewayTransactionId = gateway.getLastTransactionId();
            this.status = "SUCCESS";
            this.transactionReference = generateTransactionId();
            logger.info("Digital payment successful. UPI: " + maskUPI() + ", Amount: " + amount);
            return true;
        } else {
            this.status = "FAILED";
            logger.warning("Gateway transaction failed");
            return false;
        }
    }
    
    @Override
    public boolean validatePayment() {
        if (!isUPIValid()) {
            logger.warning("Invalid UPI ID format");
            return false;
        }
        
        if (!isMobileNumberValid()) {
            logger.warning("Invalid mobile number format");
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
        return "DIGITAL (UPI)";
    }
    
    private boolean isUPIValid() {
        return upiId != null && UPI_PATTERN.matcher(upiId).matches();
    }
    
    private boolean isMobileNumberValid() {
        if (mobileNumber == null) {
            return false;
        }
        String clean = mobileNumber.replaceAll("\\D", "");
        return MOBILE_PATTERN.matcher(clean).matches();
    }
    
    private void generateOTP() {
        Random random = new Random();
        this.otp = String.format("%06d", random.nextInt(1000000));
        this.otpGeneratedTime = System.currentTimeMillis();
        this.otpAttempts = 0;
        logger.info("OTP generated: " + otp);
    }
    
    public boolean verifyOTP(String enteredOTP) {
        if (isOTPExpired()) {
            logger.warning("OTP has expired");
            this.otpVerified = false;
            return false;
        }
        
        if (otpAttempts >= MAX_OTP_ATTEMPTS) {
            logger.warning("Maximum OTP attempts exceeded");
            this.otpVerified = false;
            return false;
        }
        
        otpAttempts++;
        
        if (otp.equals(enteredOTP)) {
            this.otpVerified = true;
            logger.info("OTP verified successfully");
            return true;
        } else {
            logger.warning("Invalid OTP. Attempts remaining: " + (MAX_OTP_ATTEMPTS - otpAttempts));
            this.otpVerified = false;
            return false;
        }
    }
    
    private boolean isOTPExpired() {
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - otpGeneratedTime) / 1000;
        return elapsedSeconds > OTP_TIMEOUT_SECONDS;
    }
    
    public boolean isOTPVerified() {
        return otpVerified;
    }
    
    public void simulateOTPVerification() {
        this.otpVerified = true;
        logger.info("OTP verification simulated");
    }
    
    public String maskUPI() {
        if (upiId == null || upiId.length() < 8) {
            return "****";
        }
        int atIndex = upiId.indexOf('@');
        if (atIndex > 2) {
            return upiId.substring(0, 2) + "****" + upiId.substring(atIndex);
        }
        return "****" + upiId.substring(atIndex);
    }
    
    private String generateTransactionId() {
        return "TXN-UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    public String getUpiId() {
        return upiId;
    }
    
    public String getMobileNumber() {
        return mobileNumber;
    }
    
    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }
    
    public int getOtpAttemptsRemaining() {
        return Math.max(0, MAX_OTP_ATTEMPTS - otpAttempts);
    }
    
    @Override
    public String toString() {
        return "DigitalPayment{" +
                "upiId='" + maskUPI() + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", otpVerified=" + otpVerified +
                ", status='" + status + '\'' +
                ", gatewayTransactionId='" + gatewayTransactionId + '\'' +
                '}';
    }
}
