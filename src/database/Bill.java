package database;

import java.io.Serializable;
import java.util.*;

public class Bill implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int billId;
    private String billNumber;
    private String billType; // SALES, SALES_RETURN, PURCHASE, PURCHASE_RETURN
    private Date billDate;
    private int customerId;
    private int supplierId;
    private String customerName;
    private String customerPhone;
    private List<BillItem> billItems;
    private double subtotal;
    private double totalDiscount;
    private double totalAmount;
    private double paidAmount;
    private double balanceAmount;
    private String paymentMode; // CASH, CARD, CHEQUE, ONLINE
    private String remarks;
    private String status; // PENDING, COMPLETED, CANCELLED
    private int userId;
    private Date createdDate;
    
    public Bill() {
        this.billItems = new ArrayList<>();
        this.billDate = new Date();
        this.status = "PENDING";
    }
    
    public Bill(String billNumber, String billType, int customerId, String customerName) {
        this();
        this.billNumber = billNumber;
        this.billType = billType;
        this.customerId = customerId;
        this.customerName = customerName;
    }
    
    public void addBillItem(BillItem item) {
        billItems.add(item);
        item.setBillId(this.billId);
    }
    
    public void removeBillItem(int index) {
        if (index >= 0 && index < billItems.size()) {
            billItems.remove(index);
        }
    }
    
    public void calculateTotals() {
        subtotal = 0;
        totalDiscount = 0;
        
        for (BillItem item : billItems) {
            item.calculateAmount();
            subtotal += item.getTaxableAmount();
            totalDiscount += (item.getDiscount() * item.getQuantity());
        }
        
        totalAmount = subtotal;
        balanceAmount = totalAmount - paidAmount;
    }
    
    public void applyDiscount(double discountAmount) {
        if (discountAmount <= totalAmount) {
            this.totalDiscount = discountAmount;
            this.totalAmount -= discountAmount;
            this.balanceAmount = totalAmount - paidAmount;
        }
    }
    
    public boolean completeBill(double payment, String paymentMode) {
        if (payment >= totalAmount) {
            this.paidAmount = payment;
            this.paymentMode = paymentMode;
            this.balanceAmount = totalAmount - payment;
            this.status = "COMPLETED";
            return true;
        }
        return false;
    }
    
    public void printBill() {
        System.out.println("\n========================================");
        System.out.println("          BAREEZE FOOTWEAR             ");
        System.out.println("Address: Anar complex, Naya bazar,");
        System.out.println("Melparamba, Kasaragod, Kerala, India 671317");
        System.out.println("Mobile no: 8086790086");
        System.out.println("Mail ID: breezefootwearfancy@gmail.com");
        System.out.println("========================================");
        System.out.println("Bill No: " + billNumber + "   Date: " + billDate);
        System.out.println("Type: " + billType);
        System.out.println("Customer: " + customerName + " (" + customerPhone + ")");
        System.out.println("----------------------------------------");
        System.out.println("Item Name                   Qty  Price  Total");
        System.out.println("----------------------------------------");
        
        for (BillItem item : billItems) {
            System.out.printf("%-25s %3d  %7.2f  %8.2f%n",
                    item.getItemName(), item.getQuantity(),
                    item.getUnitPrice(), item.getTotalAmount());
        }
        
        System.out.println("----------------------------------------");
        System.out.printf("Subtotal:              %15.2f%n", subtotal);
        System.out.printf("Total Discount:        %15.2f%n", totalDiscount);
        System.out.println("----------------------------------------");
        System.out.printf("Total Amount:          %15.2f%n", totalAmount);
        System.out.printf("Paid Amount:           %15.2f%n", paidAmount);
        System.out.printf("Balance:               %15.2f%n", balanceAmount);
        System.out.println("Payment Mode: " + paymentMode);
        System.out.println("========================================\n");
    }
    
    // Getters and Setters
    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }
    
    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }
    
    public String getBillType() { return billType; }
    public void setBillType(String billType) { this.billType = billType; }
    
    public Date getBillDate() { return billDate; }
    public void setBillDate(Date billDate) { this.billDate = billDate; }
    
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public List<BillItem> getBillItems() { return billItems; }
    public void setBillItems(List<BillItem> billItems) { this.billItems = billItems; }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    
    public double getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(double totalDiscount) { this.totalDiscount = totalDiscount; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
    
    public double getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(double balanceAmount) { this.balanceAmount = balanceAmount; }
    
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
}
