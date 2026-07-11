package database;

import java.util.Date;
import java.util.List;

public class PurchaseBill {
    private int purchaseBillId;
    private String billNumber;
    private int supplierId;
    private String supplierName; // Optional display property
    private Date purchaseDate;
    private double totalAmount;
    private double paidAmount;
    private double balanceDue;
    private String status;
    private int createdBy;
    private Date createdAt;
    
    private List<PurchaseBillItem> items;
    private List<PurchasePayment> payments;

    public int getPurchaseBillId() { return purchaseBillId; }
    public void setPurchaseBillId(int purchaseBillId) { this.purchaseBillId = purchaseBillId; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getBalanceDue() { return balanceDue; }
    public void setBalanceDue(double balanceDue) { this.balanceDue = balanceDue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public List<PurchaseBillItem> getItems() { return items; }
    public void setItems(List<PurchaseBillItem> items) { this.items = items; }

    public List<PurchasePayment> getPayments() { return payments; }
    public void setPayments(List<PurchasePayment> payments) { this.payments = payments; }
}
