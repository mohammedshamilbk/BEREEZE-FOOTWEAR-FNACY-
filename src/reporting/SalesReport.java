package reporting;

import java.time.LocalDate;
import java.util.*;

/**
 * Daily/Periodic sales report with comprehensive analysis.
 * Includes total sales, transactions, category breakdown, payment modes, and peak hours.
 */
public class SalesReport extends ReportGenerator {
    
    private double totalSales;
    private int transactionCount;
    private Map<String, Double> categoryWiseSales;
    private Map<String, Double> salespersonPerformance;
    private Map<String, Integer> paymentModeBreakdown;
    private Map<Integer, Double> peakHoursAnalysis;
    private double averageTransactionValue;
    private double discountGiven;
    
    public SalesReport(String generatedBy) {
        super("Daily Sales Report", generatedBy);
        this.categoryWiseSales = new LinkedHashMap<>();
        this.salespersonPerformance = new LinkedHashMap<>();
        this.paymentModeBreakdown = new LinkedHashMap<>();
        this.peakHoursAnalysis = new TreeMap<>();
    }
    
    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }
    
    public void setTransactionCount(int count) {
        this.transactionCount = count;
    }
    
    public void addCategoryWiseSales(String category, double amount) {
        this.categoryWiseSales.put(category, amount);
    }
    
    public void addSalespersonPerformance(String salesperson, double amount) {
        this.salespersonPerformance.put(salesperson, 
            this.salespersonPerformance.getOrDefault(salesperson, 0.0) + amount);
    }
    
    public void addPaymentMode(String mode, int count) {
        this.paymentModeBreakdown.put(mode, 
            this.paymentModeBreakdown.getOrDefault(mode, 0) + count);
    }
    
    public void addPeakHour(int hour, double sales) {
        this.peakHoursAnalysis.put(hour, sales);
    }
    
    public void setDiscountGiven(double discount) {
        this.discountGiven = discount;
    }
    
    @Override
    public Object generateReport() {
        calculateAverageTransactionValue();
        
        StringBuilder report = new StringBuilder();
        report.append(getReportHeader());
        
        report.append("\n--- SALES SUMMARY ---\n");
        report.append(String.format("Total Sales: %s%n", formatCurrency(totalSales)));
        report.append(String.format("Total Transactions: %d%n", transactionCount));
        report.append(String.format("Average Transaction Value: %s%n", formatCurrency(averageTransactionValue)));
        report.append(String.format("Total Discount Given: %s%n", formatCurrency(discountGiven)));
        
        report.append("\n--- CATEGORY WISE BREAKDOWN ---\n");
        double categoryTotal = categoryWiseSales.values().stream().mapToDouble(Double::doubleValue).sum();
        categoryWiseSales.forEach((category, amount) -> {
            double percentage = calculatePercentage(amount, categoryTotal);
            report.append(String.format("%-20s %s (%s)%n", 
                category, formatCurrency(amount), formatPercentage(percentage)));
        });
        
        report.append("\n--- SALESPERSON PERFORMANCE ---\n");
        double salesTotal = salespersonPerformance.values().stream().mapToDouble(Double::doubleValue).sum();
        salespersonPerformance.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .forEach(entry -> {
                double percentage = calculatePercentage(entry.getValue(), salesTotal);
                report.append(String.format("%-20s %s (%s)%n", 
                    entry.getKey(), formatCurrency(entry.getValue()), formatPercentage(percentage)));
            });
        
        report.append("\n--- PAYMENT MODE BREAKDOWN ---\n");
        int totalTransactions = paymentModeBreakdown.values().stream().mapToInt(Integer::intValue).sum();
        paymentModeBreakdown.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
            .forEach(entry -> {
                int percentage = (int) calculatePercentage(entry.getValue(), totalTransactions);
                report.append(String.format("%-20s %d transactions (%d%%)%n", 
                    entry.getKey(), entry.getValue(), percentage));
            });
        
        report.append("\n--- PEAK HOURS ANALYSIS ---\n");
        peakHoursAnalysis.forEach((hour, sales) -> {
            report.append(String.format("%02d:00 - %02d:59 %s%n", 
                hour, hour, formatCurrency(sales)));
        });
        
        report.append("\n").append(getReportFooter());
        return report.toString();
    }
    
    @Override
    public Map<String, Object> getReportData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reportTitle", reportTitle);
        data.put("reportDate", reportDate);
        data.put("generatedBy", generatedBy);
        data.put("totalSales", totalSales);
        data.put("transactionCount", transactionCount);
        data.put("averageTransactionValue", averageTransactionValue);
        data.put("discountGiven", discountGiven);
        data.put("categoryWiseSales", new LinkedHashMap<>(categoryWiseSales));
        data.put("salespersonPerformance", new LinkedHashMap<>(salespersonPerformance));
        data.put("paymentModeBreakdown", new LinkedHashMap<>(paymentModeBreakdown));
        data.put("peakHoursAnalysis", new LinkedHashMap<>(peakHoursAnalysis));
        return data;
    }
    
    private void calculateAverageTransactionValue() {
        this.averageTransactionValue = transactionCount > 0 ? totalSales / transactionCount : 0;
    }
    
    public double getTotalSales() {
        return totalSales;
    }
    
    public int getTransactionCount() {
        return transactionCount;
    }
    
    public Map<String, Double> getCategoryWiseSales() {
        return new LinkedHashMap<>(categoryWiseSales);
    }
}
