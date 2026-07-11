package database;

import java.util.Date;

public class PurchasePayment {
    private int paymentId;
    private int purchaseBillId;
    private double amount;
    private String paymentMode;
    private Date paymentDate;
    private String referenceNote;
    private int paidBy;
    private Date createdAt;

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getPurchaseBillId() { return purchaseBillId; }
    public void setPurchaseBillId(int purchaseBillId) { this.purchaseBillId = purchaseBillId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceNote() { return referenceNote; }
    public void setReferenceNote(String referenceNote) { this.referenceNote = referenceNote; }

    public int getPaidBy() { return paidBy; }
    public void setPaidBy(int paidBy) { this.paidBy = paidBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
