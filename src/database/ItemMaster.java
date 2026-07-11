package database;

import java.io.Serializable;
import java.util.Date;

public class ItemMaster implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int itemId;
    private String itemCode;
    private String itemName;
    private String category;
    private String manufacturer;
    private double purchasePrice;
    private double sellingPrice;
    private String barcode;
    private int stockQuantity;
    private int reorderLevel;
    private String size;
    private String color;
    private String material;
    private Date createdDate;
    private Date modifiedDate;
    private String status; // ACTIVE, INACTIVE
    
    public ItemMaster() {
    }
    
    public ItemMaster(String itemCode, String itemName, String category, String manufacturer,
                      double purchasePrice, double sellingPrice, String barcode,
                      String size, String color, String material) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.category = category;
        this.manufacturer = manufacturer;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.barcode = barcode;
        this.size = size;
        this.color = color;
        this.material = material;
        this.status = "ACTIVE";
        this.createdDate = new Date();
    }
    
    // Getters and Setters
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    
    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    
    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double calculateProfit() {
        return sellingPrice - purchasePrice;
    }
    
    @Override
    public String toString() {
        return itemCode + " - " + itemName + " (" + size + ", " + color + ")";
    }
}
