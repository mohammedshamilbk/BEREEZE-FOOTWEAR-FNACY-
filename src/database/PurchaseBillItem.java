package database;

public class PurchaseBillItem {
    private int purchaseBillItemId;
    private int purchaseBillId;
    private int itemId;
    private String itemCode; // Display property
    private String itemName; // Display property
    private int quantity;
    private double purchasePrice;
    private double gst;
    private double lineTotal;

    public int getPurchaseBillItemId() { return purchaseBillItemId; }
    public void setPurchaseBillItemId(int purchaseBillItemId) { this.purchaseBillItemId = purchaseBillItemId; }

    public int getPurchaseBillId() { return purchaseBillId; }
    public void setPurchaseBillId(int purchaseBillId) { this.purchaseBillId = purchaseBillId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getGst() { return gst; }
    public void setGst(double gst) { this.gst = gst; }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}
