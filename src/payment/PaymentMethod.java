package payment;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public abstract class PaymentMethod {
    private static final Logger logger = Logger.getLogger(PaymentMethod.class.getName());
    
    protected LocalDateTime paymentDate;
    protected String status;
    protected String transactionReference;
    
    public PaymentMethod() {
        this.paymentDate = LocalDateTime.now();
        this.status = "PENDING";
        this.transactionReference = "";
    }
    
    public abstract boolean processPayment(double amount);
    
    public abstract boolean validatePayment();
    
    public abstract String getTransactionId();
    
    public abstract String getPaymentMethodName();
    
    public String getPaymentStatus() {
        return status;
    }
    
    public void setPaymentStatus(String status) {
        this.status = status;
        logger.info("Payment status updated to: " + status);
    }
    
    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String reference) {
        this.transactionReference = reference;
    }
    
    @Override
    public String toString() {
        return "PaymentMethod{" +
                "paymentDate=" + paymentDate +
                ", status='" + status + '\'' +
                ", transactionReference='" + transactionReference + '\'' +
                ", method='" + getPaymentMethodName() + '\'' +
                '}';
    }
}
