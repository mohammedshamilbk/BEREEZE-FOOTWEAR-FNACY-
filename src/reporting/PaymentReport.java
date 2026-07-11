package reporting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Payment analysis and collection report.
 * Tracks collection by payment mode, pending payments, cheque status,
 * digital payment summary, and payment failure analysis.
 */
public class PaymentReport extends ReportGenerator {
    
    private double totalCollected;
    private Map<String, Double> collectionByPaymentMode;
    private double totalPendingPayments;
    private List<PendingPayment> pendingPaymentDetails;
    private List<ChequeStatus> chequeStatusList;
    private Map<String, Integer> digitalPaymentSummary;
    private List<PaymentFailure> paymentFailures;
    
    private double cashCollected;
    private double cardCollected;
    private double upiCollected;
    private double chequeCollected;
    private int failureCount;
    
    public static class PendingPayment {
        public String customerId;
        public String customerName;
        public double amount;
        public LocalDate dueDate;
        public int daysOverdue;
        public String invoiceId;
        
        public PendingPayment(String customerId, String name, double amount,
                            LocalDate dueDate, int daysOverdue, String invoiceId) {
            this.customerId = customerId;
            this.customerName = name;
            this.amount = amount;
            this.dueDate = dueDate;
            this.daysOverdue = daysOverdue;
            this.invoiceId = invoiceId;
        }
    }
    
    public static class ChequeStatus {
        public String chequeNumber;
        public double amount;
        public LocalDate issueDate;
        public LocalDate maturityDate;
        public String status;
        public String bankName;
        
        public ChequeStatus(String number, double amount, LocalDate issued,
                           LocalDate maturity, String status, String bank) {
            this.chequeNumber = number;
            this.amount = amount;
            this.issueDate = issued;
            this.maturityDate = maturity;
            this.status = status;
            this.bankName = bank;
        }
    }
    
    public static class PaymentFailure {
        public LocalDateTime failureTime;
        public String paymentMode;
        public double amount;
        public String reason;
        public String transactionId;
        
        public PaymentFailure(LocalDateTime time, String mode, double amount,
                            String reason, String txnId) {
            this.failureTime = time;
            this.paymentMode = mode;
            this.amount = amount;
            this.reason = reason;
            this.transactionId = txnId;
        }
    }
    
    public PaymentReport(String generatedBy) {
        super("Payment Collection Report", generatedBy);
        this.collectionByPaymentMode = new LinkedHashMap<>();
        this.pendingPaymentDetails = new ArrayList<>();
        this.chequeStatusList = new ArrayList<>();
        this.digitalPaymentSummary = new LinkedHashMap<>();
        this.paymentFailures = new ArrayList<>();
    }
    
    public void setTotalCollected(double amount) {
        this.totalCollected = amount;
    }
    
    public void addCollectionByMode(String mode, double amount) {
        this.collectionByPaymentMode.put(mode, amount);
    }
    
    public void setTotalPendingPayments(double amount) {
        this.totalPendingPayments = amount;
    }
    
    public void addPendingPayment(PendingPayment payment) {
        this.pendingPaymentDetails.add(payment);
    }
    
    public void addChequeStatus(ChequeStatus cheque) {
        this.chequeStatusList.add(cheque);
    }
    
    public void addDigitalPaymentSummary(String type, int count) {
        this.digitalPaymentSummary.put(type, count);
    }
    
    public void addPaymentFailure(PaymentFailure failure) {
        this.paymentFailures.add(failure);
        this.failureCount++;
    }
    
    public void setCashCollected(double amount) {
        this.cashCollected = amount;
    }
    
    public void setCardCollected(double amount) {
        this.cardCollected = amount;
    }
    
    public void setUPICollected(double amount) {
        this.upiCollected = amount;
    }
    
    public void setChequeCollected(double amount) {
        this.chequeCollected = amount;
    }
    
    @Override
    public Object generateReport() {
        StringBuilder report = new StringBuilder();
        report.append(getReportHeader());
        
        report.append("\n--- COLLECTION SUMMARY ---\n");
        report.append(String.format("Total Collected: %s%n", formatCurrency(totalCollected)));
        report.append(String.format("Total Pending Payments: %s%n", formatCurrency(totalPendingPayments)));
        report.append(String.format("Collection Rate: %.2f%%%n", 
            (totalCollected / (totalCollected + totalPendingPayments)) * 100));
        
        report.append("\n--- COLLECTION BY PAYMENT MODE ---\n");
        report.append(String.format("Cash: %s%n", formatCurrency(cashCollected)));
        report.append(String.format("Card: %s%n", formatCurrency(cardCollected)));
        report.append(String.format("UPI/Digital Wallets: %s%n", formatCurrency(upiCollected)));
        report.append(String.format("Cheques: %s%n", formatCurrency(chequeCollected)));
        report.append(String.format("Other Modes: %s%n", 
            formatCurrency(totalCollected - (cashCollected + cardCollected + upiCollected + chequeCollected))));
        
        report.append("\n--- DETAILED PAYMENT MODE BREAKDOWN ---\n");
        double modeTotal = collectionByPaymentMode.values().stream().mapToDouble(Double::doubleValue).sum();
        collectionByPaymentMode.forEach((mode, amount) -> {
            double percentage = calculatePercentage(amount, modeTotal);
            report.append(String.format("%-20s %s (%.2f%%)%n", mode, formatCurrency(amount), percentage));
        });
        
        report.append("\n--- DIGITAL PAYMENT SUMMARY ---\n");
        digitalPaymentSummary.forEach((type, count) -> 
            report.append(String.format("%-30s %d transactions%n", type + ":", count))
        );
        
        report.append("\n--- PENDING PAYMENTS ---\n");
        if (pendingPaymentDetails.isEmpty()) {
            report.append("No pending payments.\n");
        } else {
            report.append(String.format("%-15s %-25s %-15s %-12s %-10s %-15s%n",
                "Customer ID", "Customer Name", "Amount", "Due Date", "Overdue", "Invoice ID"));
            report.append("-".repeat(92)).append("\n");
            pendingPaymentDetails.stream()
                .sorted((p1, p2) -> Integer.compare(p2.daysOverdue, p1.daysOverdue))
                .forEach(payment ->
                    report.append(String.format("%-15s %-25s %s %-12s %-10d %-15s%n",
                        payment.customerId, payment.customerName, formatCurrency(payment.amount),
                        formatDate(payment.dueDate), payment.daysOverdue, payment.invoiceId))
                );
        }
        
        report.append("\n--- CHEQUE STATUS ---\n");
        if (chequeStatusList.isEmpty()) {
            report.append("No cheques recorded.\n");
        } else {
            report.append(String.format("%-15s %-12s %-15s %-15s %-12s %-20s%n",
                "Cheque No.", "Amount", "Issue Date", "Maturity Date", "Status", "Bank Name"));
            report.append("-".repeat(89)).append("\n");
            chequeStatusList.forEach(cheque ->
                report.append(String.format("%-15s %s %-15s %-15s %-12s %-20s%n",
                    cheque.chequeNumber, formatCurrency(cheque.amount),
                    formatDate(cheque.issueDate), formatDate(cheque.maturityDate),
                    cheque.status, cheque.bankName))
            );
        }
        
        report.append("\n--- PAYMENT FAILURES ---\n");
        if (paymentFailures.isEmpty()) {
            report.append("No payment failures recorded.\n");
        } else {
            report.append(String.format("Total Failures: %d%n%n", failureCount));
            report.append(String.format("%-20s %-15s %-15s %-20s %-20s%n",
                "Failure Time", "Mode", "Amount", "Reason", "Transaction ID"));
            report.append("-".repeat(90)).append("\n");
            paymentFailures.stream().limit(20).forEach(failure ->
                report.append(String.format("%-20s %-15s %s %-20s %-20s%n",
                    formatDateTime(failure.failureTime), failure.paymentMode,
                    formatCurrency(failure.amount), failure.reason, failure.transactionId))
            );
        }
        
        report.append("\n").append(getReportFooter());
        return report.toString();
    }
    
    @Override
    public Map<String, Object> getReportData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reportTitle", reportTitle);
        data.put("reportDate", reportDate);
        data.put("generatedBy", generatedBy);
        data.put("totalCollected", totalCollected);
        data.put("totalPendingPayments", totalPendingPayments);
        data.put("collectionByPaymentMode", new LinkedHashMap<>(collectionByPaymentMode));
        data.put("cashCollected", cashCollected);
        data.put("cardCollected", cardCollected);
        data.put("upiCollected", upiCollected);
        data.put("chequeCollected", chequeCollected);
        data.put("pendingPayments", new ArrayList<>(pendingPaymentDetails));
        data.put("chequeStatus", new ArrayList<>(chequeStatusList));
        data.put("digitalPaymentSummary", new LinkedHashMap<>(digitalPaymentSummary));
        data.put("paymentFailures", new ArrayList<>(paymentFailures));
        data.put("failureCount", failureCount);
        return data;
    }
    
    public double getTotalCollected() {
        return totalCollected;
    }
    
    public double getTotalPendingPayments() {
        return totalPendingPayments;
    }
    
    public int getPendingPaymentCount() {
        return pendingPaymentDetails.size();
    }
    
    public int getFailureCount() {
        return failureCount;
    }
}
