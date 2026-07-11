package reporting;

import java.util.*;

/**
 * Profit & Loss statement and financial analysis report.
 * Includes revenue summary, COGS, operating expenses, P&L calculation,
 * margin analysis, and trend analysis.
 */
public class FinancialReport extends ReportGenerator {
    
    private double totalRevenue;
    private double costOfGoodsSold;
    private double grossProfit;
    private Map<String, Double> operatingExpenses;
    private double operatingProfit;
    private double interestExpense;
    private double taxAmount;
    private double netProfit;
    private double grossMarginPercent;
    private double netMarginPercent;
    private Map<String, Double> marginByCategory;
    private Map<String, Double> trendAnalysis;
    private Map<String, Double> expenseBreakdown;
    
    public FinancialReport(String generatedBy) {
        super("Financial Report - P&L Statement", generatedBy);
        this.operatingExpenses = new LinkedHashMap<>();
        this.marginByCategory = new LinkedHashMap<>();
        this.trendAnalysis = new LinkedHashMap<>();
        this.expenseBreakdown = new LinkedHashMap<>();
    }
    
    public void setTotalRevenue(double revenue) {
        this.totalRevenue = revenue;
    }
    
    public void setCostOfGoodsSold(double cogs) {
        this.costOfGoodsSold = cogs;
    }
    
    public void addOperatingExpense(String category, double amount) {
        this.operatingExpenses.put(category, amount);
    }
    
    public void setInterestExpense(double amount) {
        this.interestExpense = amount;
    }
    
    public void setTaxAmount(double amount) {
        this.taxAmount = amount;
    }
    
    public void addMarginByCategory(String category, double margin) {
        this.marginByCategory.put(category, margin);
    }
    
    public void addTrendData(String period, double value) {
        this.trendAnalysis.put(period, value);
    }
    
    public void addExpenseBreakdown(String expense, double amount) {
        this.expenseBreakdown.put(expense, amount);
    }
    
    public void calculateFinancials() {
        this.grossProfit = totalRevenue - costOfGoodsSold;
        this.grossMarginPercent = calculatePercentage(grossProfit, totalRevenue);
        
        double totalExpenses = operatingExpenses.values().stream()
            .mapToDouble(Double::doubleValue).sum();
        
        this.operatingProfit = grossProfit - totalExpenses;
        this.netProfit = operatingProfit - interestExpense - taxAmount;
        this.netMarginPercent = calculatePercentage(netProfit, totalRevenue);
    }
    
    @Override
    public Object generateReport() {
        calculateFinancials();
        
        StringBuilder report = new StringBuilder();
        report.append(getReportHeader());
        
        report.append("\n--- INCOME STATEMENT ---\n");
        report.append(String.format("%-40s %s%n", "Total Revenue:", formatCurrency(totalRevenue)));
        report.append(String.format("%-40s %s%n", "Cost of Goods Sold (COGS):", formatCurrency(costOfGoodsSold)));
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-40s %s (%.2f%%)%n", 
            "Gross Profit:", formatCurrency(grossProfit), grossMarginPercent));
        
        report.append("\n--- OPERATING EXPENSES ---\n");
        double totalOpEx = 0;
        for (Map.Entry<String, Double> entry : operatingExpenses.entrySet()) {
            report.append(String.format("%-40s %s%n", entry.getKey() + ":", formatCurrency(entry.getValue())));
            totalOpEx += entry.getValue();
        }
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-40s %s%n", "Total Operating Expenses:", formatCurrency(totalOpEx)));
        
        report.append("\n--- OPERATING PROFIT ---\n");
        report.append(String.format("%-40s %s%n", "Operating Profit:", formatCurrency(operatingProfit)));
        
        report.append("\n--- OTHER EXPENSES ---\n");
        report.append(String.format("%-40s %s%n", "Interest Expense:", formatCurrency(interestExpense)));
        report.append(String.format("%-40s %s%n", "Tax Amount:", formatCurrency(taxAmount)));
        
        report.append("\n=== NET PROFIT/LOSS ===\n");
        report.append(String.format("%-40s %s%n", "NET PROFIT:", formatCurrency(netProfit)));
        
        report.append("\n--- MARGIN ANALYSIS ---\n");
        report.append(String.format("%-40s %.2f%%%n", "Gross Margin %:", grossMarginPercent));
        report.append(String.format("%-40s %.2f%%%n", "Operating Margin %:", 
            calculatePercentage(operatingProfit, totalRevenue)));
        report.append(String.format("%-40s %.2f%%%n", "Net Profit Margin %:", netMarginPercent));
        
        report.append("\n--- MARGIN BY CATEGORY ---\n");
        marginByCategory.forEach((category, margin) -> 
            report.append(String.format("%-30s %.2f%%%n", category + ":", margin))
        );
        
        report.append("\n--- EXPENSE BREAKDOWN ---\n");
        expenseBreakdown.forEach((expense, amount) -> 
            report.append(String.format("%-30s %s%n", expense + ":", formatCurrency(amount)))
        );
        
        report.append("\n--- TREND ANALYSIS ---\n");
        trendAnalysis.forEach((period, value) -> 
            report.append(String.format("%-30s %s%n", period + ":", formatCurrency(value)))
        );
        
        report.append("\n").append(getReportFooter());
        return report.toString();
    }
    
    @Override
    public Map<String, Object> getReportData() {
        calculateFinancials();
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reportTitle", reportTitle);
        data.put("reportDate", reportDate);
        data.put("generatedBy", generatedBy);
        data.put("totalRevenue", totalRevenue);
        data.put("costOfGoodsSold", costOfGoodsSold);
        data.put("grossProfit", grossProfit);
        data.put("grossMarginPercent", grossMarginPercent);
        data.put("operatingExpenses", new LinkedHashMap<>(operatingExpenses));
        data.put("operatingProfit", operatingProfit);
        data.put("interestExpense", interestExpense);
        data.put("taxAmount", taxAmount);
        data.put("netProfit", netProfit);
        data.put("netMarginPercent", netMarginPercent);
        data.put("marginByCategory", new LinkedHashMap<>(marginByCategory));
        data.put("trendAnalysis", new LinkedHashMap<>(trendAnalysis));
        data.put("expenseBreakdown", new LinkedHashMap<>(expenseBreakdown));
        return data;
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
    }
    
    public double getNetProfit() {
        return netProfit;
    }
    
    public double getNetMarginPercent() {
        return netMarginPercent;
    }
    
    public double getGrossMarginPercent() {
        return grossMarginPercent;
    }
}
