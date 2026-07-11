package reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CSV export functionality for reports.
 * Simple, universal format compatible with all spreadsheet applications.
 * Thread-safe for concurrent exports.
 */
public class CSVExporter {
    
    private static final DateTimeFormatter FILENAME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String DELIMITER = ",";
    private static final String QUOTE = "\"";
    
    private String defaultOutputDirectory;
    
    public CSVExporter() {
        this.defaultOutputDirectory = System.getProperty("user.home") + File.separator + "Downloads";
    }
    
    public CSVExporter(String outputDirectory) {
        this.defaultOutputDirectory = outputDirectory;
    }
    
    /**
     * Export SalesReport to CSV
     */
    public synchronized File exportSalesReport(SalesReport report) throws IOException {
        return exportSalesReport(report, generateFilename("SalesReport"));
    }
    
    /**
     * Export SalesReport to CSV with custom filename
     */
    public synchronized File exportSalesReport(SalesReport report, String filename) throws IOException {
        File outputFile = createOutputFile(filename, ".csv");
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writeReportHeader(writer, report);
            
            writer.append("\n--- SALES SUMMARY ---\n");
            writer.append("Metric,Value\n");
            writer.append(quote("Total Sales")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getTotalSales()))).append("\n");
            writer.append(quote("Transaction Count")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getTransactionCount()))).append("\n");
            
            Map<String, Object> data = report.getReportData();
            
            writer.append("\n--- CATEGORY WISE SALES ---\n");
            writer.append("Category,Amount\n");
            Map<String, Double> categoryWiseSales = (Map<String, Double>) data.get("categoryWiseSales");
            categoryWiseSales.forEach((cat, amt) -> {
                try {
                    writer.append(quote(cat)).append(DELIMITER)
                          .append(quote(String.valueOf(amt))).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            
            writer.append("\n--- SALESPERSON PERFORMANCE ---\n");
            writer.append("Salesperson,Sales Amount\n");
            Map<String, Double> salesPerf = (Map<String, Double>) data.get("salespersonPerformance");
            salesPerf.forEach((person, sales) -> {
                try {
                    writer.append(quote(person)).append(DELIMITER)
                          .append(quote(String.valueOf(sales))).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            
            writer.append("\n--- PAYMENT MODE BREAKDOWN ---\n");
            writer.append("Mode,Count\n");
            Map<String, Integer> paymentModes = (Map<String, Integer>) data.get("paymentModeBreakdown");
            paymentModes.forEach((mode, count) -> {
                try {
                    writer.append(quote(mode)).append(DELIMITER)
                          .append(quote(String.valueOf(count))).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        return outputFile;
    }
    
    /**
     * Export InventoryReport to CSV
     */
    public synchronized File exportInventoryReport(InventoryReport report) throws IOException {
        return exportInventoryReport(report, generateFilename("InventoryReport"));
    }
    
    /**
     * Export InventoryReport to CSV with custom filename
     */
    public synchronized File exportInventoryReport(InventoryReport report, String filename) throws IOException {
        File outputFile = createOutputFile(filename, ".csv");
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writeReportHeader(writer, report);
            
            writer.append("\n--- INVENTORY SUMMARY ---\n");
            writer.append("Metric,Value\n");
            writer.append(quote("Current Stock Value")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getCurrentStockValue()))).append("\n");
            writer.append(quote("Low Stock Items")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getLowStockItemCount()))).append("\n");
            
            Map<String, Object> data = report.getReportData();
            
            writer.append("\n--- LOW STOCK ITEMS ---\n");
            writer.append("Item Code,Item Name,Current Stock,Minimum Stock,Category\n");
            List<InventoryReport.LowStockItem> lowStockItems = 
                (List<InventoryReport.LowStockItem>) data.get("lowStockItems");
            lowStockItems.forEach(item -> {
                try {
                    writer.append(quote(item.itemCode)).append(DELIMITER)
                          .append(quote(item.itemName)).append(DELIMITER)
                          .append(quote(String.valueOf(item.currentStock))).append(DELIMITER)
                          .append(quote(String.valueOf(item.minimumStock))).append(DELIMITER)
                          .append(quote(item.category)).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        return outputFile;
    }
    
    /**
     * Export CustomerReport to CSV
     */
    public synchronized File exportCustomerReport(CustomerReport report) throws IOException {
        return exportCustomerReport(report, generateFilename("CustomerReport"));
    }
    
    /**
     * Export CustomerReport to CSV with custom filename
     */
    public synchronized File exportCustomerReport(CustomerReport report, String filename) throws IOException {
        File outputFile = createOutputFile(filename, ".csv");
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writeReportHeader(writer, report);
            
            writer.append("\n--- CUSTOMER OVERVIEW ---\n");
            writer.append("Metric,Value\n");
            writer.append(quote("Total Customers")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getTotalCustomers()))).append("\n");
            writer.append(quote("New Customers")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getNewCustomersInPeriod()))).append("\n");
            writer.append(quote("Retention Rate")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getCustomerRetentionRate()))).append("\n");
            writer.append(quote("Outstanding Balance")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getTotalOutstandingBalance()))).append("\n");
            
            Map<String, Object> data = report.getReportData();
            
            writer.append("\n--- TOP CUSTOMERS ---\n");
            writer.append("Customer ID,Customer Name,Total Spent,Transactions,Last Purchase\n");
            List<CustomerReport.TopCustomer> topCustomers = 
                (List<CustomerReport.TopCustomer>) data.get("topCustomers");
            topCustomers.forEach(customer -> {
                try {
                    writer.append(quote(customer.customerId)).append(DELIMITER)
                          .append(quote(customer.customerName)).append(DELIMITER)
                          .append(quote(String.valueOf(customer.totalSpent))).append(DELIMITER)
                          .append(quote(String.valueOf(customer.transactionCount))).append(DELIMITER)
                          .append(quote(String.valueOf(customer.lastPurchaseDate))).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        return outputFile;
    }
    
    /**
     * Export FinancialReport to CSV
     */
    public synchronized File exportFinancialReport(FinancialReport report) throws IOException {
        return exportFinancialReport(report, generateFilename("FinancialReport"));
    }
    
    /**
     * Export FinancialReport to CSV with custom filename
     */
    public synchronized File exportFinancialReport(FinancialReport report, String filename) throws IOException {
        File outputFile = createOutputFile(filename, ".csv");
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writeReportHeader(writer, report);
            
            writer.append("\n--- INCOME STATEMENT ---\n");
            writer.append("Item,Amount\n");
            writer.append(quote("Total Revenue")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getTotalRevenue()))).append("\n");
            writer.append(quote("Net Profit")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getNetProfit()))).append("\n");
            writer.append(quote("Gross Margin %")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getGrossMarginPercent()))).append("\n");
            writer.append(quote("Net Margin %")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getNetMarginPercent()))).append("\n");
            
            Map<String, Object> data = report.getReportData();
            
            writer.append("\n--- OPERATING EXPENSES ---\n");
            writer.append("Category,Amount\n");
            Map<String, Double> expenses = (Map<String, Double>) data.get("operatingExpenses");
            expenses.forEach((cat, amt) -> {
                try {
                    writer.append(quote(cat)).append(DELIMITER)
                          .append(quote(String.valueOf(amt))).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        return outputFile;
    }
    
    /**
     * Export PaymentReport to CSV
     */
    public synchronized File exportPaymentReport(PaymentReport report) throws IOException {
        return exportPaymentReport(report, generateFilename("PaymentReport"));
    }
    
    /**
     * Export PaymentReport to CSV with custom filename
     */
    public synchronized File exportPaymentReport(PaymentReport report, String filename) throws IOException {
        File outputFile = createOutputFile(filename, ".csv");
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writeReportHeader(writer, report);
            
            writer.append("\n--- COLLECTION SUMMARY ---\n");
            writer.append("Item,Amount\n");
            writer.append(quote("Total Collected")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getTotalCollected()))).append("\n");
            writer.append(quote("Total Pending")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getTotalPendingPayments()))).append("\n");
            writer.append(quote("Pending Count")).append(DELIMITER)
                   .append(quote(String.valueOf(report.getPendingPaymentCount()))).append("\n");
            
            Map<String, Object> data = report.getReportData();
            
            writer.append("\n--- PENDING PAYMENTS ---\n");
            writer.append("Customer ID,Customer Name,Amount,Due Date,Days Overdue,Invoice ID\n");
            List<PaymentReport.PendingPayment> pending = 
                (List<PaymentReport.PendingPayment>) data.get("pendingPayments");
            pending.forEach(p -> {
                try {
                    writer.append(quote(p.customerId)).append(DELIMITER)
                          .append(quote(p.customerName)).append(DELIMITER)
                          .append(quote(String.valueOf(p.amount))).append(DELIMITER)
                          .append(quote(String.valueOf(p.dueDate))).append(DELIMITER)
                          .append(quote(String.valueOf(p.daysOverdue))).append(DELIMITER)
                          .append(quote(p.invoiceId)).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        return outputFile;
    }
    

    /**
     * Set default output directory for exports
     */
    public void setDefaultOutputDirectory(String directory) {
        this.defaultOutputDirectory = directory;
    }
    
    /**
     * Generate unique filename with timestamp
     */
    private String generateFilename(String prefix) {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        return prefix + "_" + timestamp;
    }
    
    /**
     * Create output file with directory validation
     */
    private File createOutputFile(String filename, String extension) throws IOException {
        File directory = new File(defaultOutputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File outputFile = new File(directory, filename + extension);
        if (!outputFile.createNewFile()) {
            outputFile.delete();
            outputFile.createNewFile();
        }
        
        return outputFile;
    }
    
    /**
     * Quote CSV value to handle special characters
     */
    private String quote(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(DELIMITER) || value.contains(QUOTE) || value.contains("\n")) {
            return QUOTE + value.replace(QUOTE, QUOTE + QUOTE) + QUOTE;
        }
        return value;
    }
    
    /**
     * Write common report header to CSV
     */
    private void writeReportHeader(FileWriter writer, ReportGenerator report) throws IOException {
        writer.append(quote(report.getReportTitle())).append("\n");
        writer.append("Generated on,").append(quote(report.getReportDate().toString())).append("\n");
        writer.append("Generated by,").append(quote(report.getGeneratedBy())).append("\n");
    }
    
    /**
     * Get the default output directory
     */
    public String getDefaultOutputDirectory() {
        return defaultOutputDirectory;
    }
}
