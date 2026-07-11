package database;

import java.io.Serializable;

public class BillItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int billItemId;
    private int billId;
    private int itemId;
    private String itemCode;
    private String itemName;
    private int quantity;
    private double unitPrice;
    private double discount;
    private double taxableAmount;
    private double totalAmount;
    
    public BillItem() {
    }
    
    public BillItem(int itemId, String itemCode, String itemName, int quantity,
                    double unitPrice) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = 0;
        calculateAmount();
    }
    
    public void calculateAmount() {
        taxableAmount = (unitPrice - discount) * quantity;
        totalAmount = taxableAmount;
    }
    
    // Getters and Setters
    public int getBillItemId() { return billItemId; }
    public void setBillItemId(int billItemId) { this.billItemId = billItemId; }
    
    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }
    
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; calculateAmount(); }
    
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; calculateAmount(); }
    
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; calculateAmount(); }
    
    public double getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(double taxableAmount) { this.taxableAmount = taxableAmount; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    @Override
    public String toString() {
        return itemName + " x " + quantity + " @ " + unitPrice + " = " + totalAmount;
    }
}
