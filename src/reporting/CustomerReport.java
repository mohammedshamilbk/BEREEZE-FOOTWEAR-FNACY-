package reporting;

import java.time.LocalDate;
import java.util.*;

/**
 * Customer analytics and insights report.
 * Includes acquisition trends, top customers, loyalty points,
 * outstanding balances, retention rate, and customer type distribution.
 */
public class CustomerReport extends ReportGenerator {
    
    private int totalCustomers;
    private int newCustomersInPeriod;
    private Map<LocalDate, Integer> customerAcquisitionTrend;
    private List<TopCustomer> topCustomers;
    private Map<String, Integer> loyaltyPointsDistribution;
    private double totalOutstandingBalance;
    private Map<String, Double> outstandingByCustomer;
    private double customerRetentionRate;
    private Map<String, Integer> customerTypeDistribution;
    
    private double totalCustomerLifetimeValue;
    private double averageCustomerSpend;
    private int activeCustomers;
    private int inactiveCustomers;
    
    public static class TopCustomer {
        public String customerId;
        public String customerName;
        public double totalSpent;
        public int transactionCount;
        public LocalDate lastPurchaseDate;
        
        public TopCustomer(String id, String name, double spent, int count, LocalDate lastDate) {
            this.customerId = id;
            this.customerName = name;
            this.totalSpent = spent;
            this.transactionCount = count;
            this.lastPurchaseDate = lastDate;
        }
    }
    
    public CustomerReport(String generatedBy) {
        super("Customer Analytics Report", generatedBy);
        this.customerAcquisitionTrend = new TreeMap<>();
        this.topCustomers = new ArrayList<>();
        this.loyaltyPointsDistribution = new LinkedHashMap<>();
        this.outstandingByCustomer = new LinkedHashMap<>();
        this.customerTypeDistribution = new LinkedHashMap<>();
    }
    
    public void setTotalCustomers(int total) {
        this.totalCustomers = total;
    }
    
    public void setNewCustomersInPeriod(int count) {
        this.newCustomersInPeriod = count;
    }
    
    public void addAcquisitionTrendData(LocalDate date, int count) {
        this.customerAcquisitionTrend.put(date, count);
    }
    
    public void addTopCustomer(TopCustomer customer) {
        this.topCustomers.add(customer);
    }
    
    public void addLoyaltyPointsDistribution(String range, int count) {
        this.loyaltyPointsDistribution.put(range, count);
    }
    
    public void setOutstandingBalance(double amount) {
        this.totalOutstandingBalance = amount;
    }
    
    public void addOutstandingBalance(String customerName, double amount) {
        this.outstandingByCustomer.put(customerName, amount);
    }
    
    public void setCustomerRetentionRate(double rate) {
        this.customerRetentionRate = rate;
    }
    
    public void addCustomerTypeDistribution(String type, int count) {
        this.customerTypeDistribution.put(type, count);
    }
    
    public void setTotalCustomerLifetimeValue(double value) {
        this.totalCustomerLifetimeValue = value;
    }
    
    public void setActiveAndInactiveCustomers(int active, int inactive) {
        this.activeCustomers = active;
        this.inactiveCustomers = inactive;
    }
    
    @Override
    public Object generateReport() {
        calculateAverageCustomerSpend();
        
        StringBuilder report = new StringBuilder();
        report.append(getReportHeader());
        
        report.append("\n--- CUSTOMER OVERVIEW ---\n");
        report.append(String.format("Total Customers: %d%n", totalCustomers));
        report.append(String.format("New Customers (This Period): %d%n", newCustomersInPeriod));
        report.append(String.format("Active Customers: %d%n", activeCustomers));
        report.append(String.format("Inactive Customers: %d%n", inactiveCustomers));
        report.append(String.format("Customer Retention Rate: %s%n", formatPercentage(customerRetentionRate)));
        report.append(String.format("Total Lifetime Value: %s%n", formatCurrency(totalCustomerLifetimeValue)));
        report.append(String.format("Average Customer Spend: %s%n", formatCurrency(averageCustomerSpend)));
        
        report.append("\n--- CUSTOMER ACQUISITION TREND ---\n");
        customerAcquisitionTrend.forEach((date, count) -> 
            report.append(String.format("%s: %d new customers%n", formatDate(date), count))
        );
        
        report.append("\n--- TOP CUSTOMERS BY SALES ---\n");
        if (topCustomers.isEmpty()) {
            report.append("No customer data available.\n");
        } else {
            report.append(String.format("%-15s %-25s %-15s %-15s %-15s%n",
                "Customer ID", "Customer Name", "Total Spent", "Transactions", "Last Purchase"));
            report.append("-".repeat(85)).append("\n");
            topCustomers.forEach(customer -> 
                report.append(String.format("%-15s %-25s %s %-15d %-15s%n",
                    customer.customerId, customer.customerName, 
                    formatCurrency(customer.totalSpent), customer.transactionCount,
                    formatDate(customer.lastPurchaseDate)))
            );
        }
        
        report.append("\n--- LOYALTY POINTS DISTRIBUTION ---\n");
        loyaltyPointsDistribution.forEach((range, count) -> 
            report.append(String.format("%-20s %d customers%n", range, count))
        );
        
        report.append("\n--- OUTSTANDING BALANCES ---\n");
        report.append(String.format("Total Outstanding Amount: %s%n", formatCurrency(totalOutstandingBalance)));
        
        if (!outstandingByCustomer.isEmpty()) {
            report.append("\nTop Outstanding Customers:\n");
            outstandingByCustomer.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .forEach(entry -> 
                    report.append(String.format("%-30s %s%n", 
                        entry.getKey(), formatCurrency(entry.getValue())))
                );
        }
        
        report.append("\n--- CUSTOMER TYPE DISTRIBUTION ---\n");
        int totalTypes = customerTypeDistribution.values().stream().mapToInt(Integer::intValue).sum();
        customerTypeDistribution.forEach((type, count) -> {
            double percentage = calculatePercentage(count, totalTypes);
            report.append(String.format("%-20s %d customers (%s)%n", 
                type, count, formatPercentage(percentage)));
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
        data.put("totalCustomers", totalCustomers);
        data.put("newCustomersInPeriod", newCustomersInPeriod);
        data.put("activeCustomers", activeCustomers);
        data.put("inactiveCustomers", inactiveCustomers);
        data.put("customerRetentionRate", customerRetentionRate);
        data.put("totalCustomerLifetimeValue", totalCustomerLifetimeValue);
        data.put("averageCustomerSpend", averageCustomerSpend);
        data.put("customerAcquisitionTrend", new TreeMap<>(customerAcquisitionTrend));
        data.put("topCustomers", new ArrayList<>(topCustomers));
        data.put("loyaltyPointsDistribution", new LinkedHashMap<>(loyaltyPointsDistribution));
        data.put("totalOutstandingBalance", totalOutstandingBalance);
        data.put("outstandingByCustomer", new LinkedHashMap<>(outstandingByCustomer));
        data.put("customerTypeDistribution", new LinkedHashMap<>(customerTypeDistribution));
        return data;
    }
    
    private void calculateAverageCustomerSpend() {
        this.averageCustomerSpend = totalCustomers > 0 ? totalCustomerLifetimeValue / totalCustomers : 0;
    }
    
    public int getTotalCustomers() {
        return totalCustomers;
    }
    
    public int getNewCustomersInPeriod() {
        return newCustomersInPeriod;
    }
    
    public double getCustomerRetentionRate() {
        return customerRetentionRate;
    }
    
    public double getTotalOutstandingBalance() {
        return totalOutstandingBalance;
    }
}
