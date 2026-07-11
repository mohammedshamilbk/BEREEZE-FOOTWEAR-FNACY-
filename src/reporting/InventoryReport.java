package reporting;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Stock analysis and inventory management report.
 * Includes current stock value, low stock items, stock movement,
 * category-wise inventory, aging analysis, and transfer history.
 */
public class InventoryReport extends ReportGenerator {
    
    private double currentStockValue;
    private int totalItemsInStock;
    private List<LowStockItem> lowStockItems;
    private Map<String, StockMovement> categoryWiseInventory;
    private Map<String, Integer> stockAgingAnalysis;
    private List<StockTransfer> transferHistory;
    
    private double deadStockValue;
    private int deadStockCount;
    private double fastMovingValue;
    private int fastMovingCount;
    
    public static class LowStockItem {
        public String itemCode;
        public String itemName;
        public int currentStock;
        public int minimumStock;
        public String category;
        
        public LowStockItem(String code, String name, int current, int minimum, String category) {
            this.itemCode = code;
            this.itemName = name;
            this.currentStock = current;
            this.minimumStock = minimum;
            this.category = category;
        }
    }
    
    public static class StockMovement {
        public String category;
        public int totalQuantity;
        public double totalValue;
        public int inwardQuantity;
        public int outwardQuantity;
        
        public StockMovement(String category) {
            this.category = category;
        }
    }
    
    public static class StockTransfer {
        public LocalDateTime transferDate;
        public String fromLocation;
        public String toLocation;
        public String itemCode;
        public int quantity;
        public String reason;
        
        public StockTransfer(LocalDateTime date, String from, String to, 
                           String code, int qty, String reason) {
            this.transferDate = date;
            this.fromLocation = from;
            this.toLocation = to;
            this.itemCode = code;
            this.quantity = qty;
            this.reason = reason;
        }
    }
    
    public InventoryReport(String generatedBy) {
        super("Inventory Stock Analysis Report", generatedBy);
        this.lowStockItems = new ArrayList<>();
        this.categoryWiseInventory = new LinkedHashMap<>();
        this.stockAgingAnalysis = new LinkedHashMap<>();
        this.transferHistory = new ArrayList<>();
    }
    
    public void setCurrentStockValue(double value) {
        this.currentStockValue = value;
    }
    
    public void setTotalItemsInStock(int count) {
        this.totalItemsInStock = count;
    }
    
    public void addLowStockItem(LowStockItem item) {
        this.lowStockItems.add(item);
    }
    
    public void addCategoryWiseInventory(String category, StockMovement movement) {
        this.categoryWiseInventory.put(category, movement);
    }
    
    public void addStockAgingAnalysis(String ageGroup, int count) {
        this.stockAgingAnalysis.put(ageGroup, count);
    }
    
    public void addStockTransfer(StockTransfer transfer) {
        this.transferHistory.add(transfer);
    }
    
    public void setDeadStock(int count, double value) {
        this.deadStockCount = count;
        this.deadStockValue = value;
    }
    
    public void setFastMovingStock(int count, double value) {
        this.fastMovingCount = count;
        this.fastMovingValue = value;
    }
    
    @Override
    public Object generateReport() {
        StringBuilder report = new StringBuilder();
        report.append(getReportHeader());
        
        report.append("\n--- INVENTORY SUMMARY ---\n");
        report.append(String.format("Total Stock Value: %s%n", formatCurrency(currentStockValue)));
        report.append(String.format("Total Items in Stock: %d%n", totalItemsInStock));
        report.append(String.format("Dead Stock Count: %d items (%s)%n", deadStockCount, formatCurrency(deadStockValue)));
        report.append(String.format("Fast Moving Stock Count: %d items (%s)%n", fastMovingCount, formatCurrency(fastMovingValue)));
        report.append(String.format("Inventory Turnover: %.2f%%\n", 
            (fastMovingValue / currentStockValue) * 100));
        
        report.append("\n--- LOW STOCK ITEMS (Need Replenishment) ---\n");
        if (lowStockItems.isEmpty()) {
            report.append("No items currently below minimum stock level.\n");
        } else {
            report.append(String.format("%-15s %-30s %-15s %-15s %-15s%n", 
                "Item Code", "Item Name", "Current Stock", "Minimum Stock", "Category"));
            report.append("-".repeat(90)).append("\n");
            lowStockItems.forEach(item -> 
                report.append(String.format("%-15s %-30s %-15d %-15d %-15s%n", 
                    item.itemCode, item.itemName, item.currentStock, item.minimumStock, item.category))
            );
        }
        
        report.append("\n--- CATEGORY WISE INVENTORY ---\n");
        report.append(String.format("%-20s %-15s %-15s %-10s %-10s%n", 
            "Category", "Quantity", "Value", "Inward", "Outward"));
        report.append("-".repeat(70)).append("\n");
        categoryWiseInventory.forEach((category, movement) -> 
            report.append(String.format("%-20s %-15d %s %-10d %-10d%n", 
                category, movement.totalQuantity, formatCurrency(movement.totalValue),
                movement.inwardQuantity, movement.outwardQuantity))
        );
        
        report.append("\n--- STOCK AGING ANALYSIS ---\n");
        stockAgingAnalysis.forEach((ageGroup, count) -> 
            report.append(String.format("%-30s %d items%n", ageGroup, count))
        );
        
        report.append("\n--- RECENT STOCK TRANSFERS ---\n");
        if (transferHistory.isEmpty()) {
            report.append("No stock transfers recorded.\n");
        } else {
            report.append(String.format("%-20s %-15s %-15s %-15s %-10s %-20s%n", 
                "Date", "From", "To", "Item Code", "Qty", "Reason"));
            report.append("-".repeat(95)).append("\n");
            transferHistory.stream().limit(20).forEach(transfer -> 
                report.append(String.format("%-20s %-15s %-15s %-15s %-10d %-20s%n",
                    formatDateTime(transfer.transferDate), transfer.fromLocation,
                    transfer.toLocation, transfer.itemCode, transfer.quantity, transfer.reason))
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
        data.put("currentStockValue", currentStockValue);
        data.put("totalItemsInStock", totalItemsInStock);
        data.put("deadStockCount", deadStockCount);
        data.put("deadStockValue", deadStockValue);
        data.put("fastMovingCount", fastMovingCount);
        data.put("fastMovingValue", fastMovingValue);
        data.put("lowStockItems", new ArrayList<>(lowStockItems));
        data.put("categoryWiseInventory", new LinkedHashMap<>(categoryWiseInventory));
        data.put("stockAgingAnalysis", new LinkedHashMap<>(stockAgingAnalysis));
        data.put("transferHistory", new ArrayList<>(transferHistory));
        return data;
    }
    
    public double getCurrentStockValue() {
        return currentStockValue;
    }
    
    public int getLowStockItemCount() {
        return lowStockItems.size();
    }
    
    public List<LowStockItem> getLowStockItems() {
        return new ArrayList<>(lowStockItems);
    }
}
