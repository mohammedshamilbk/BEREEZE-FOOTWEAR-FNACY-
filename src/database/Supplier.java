package database;

import java.io.Serializable;
import java.util.Date;

public class Supplier implements Serializable {
    private static final long serialVersionUID = 1L;

    private int supplierId;
    private String supplierCode;
    private String supplierName;
    private String phone;
    private String email;
    private String state;
    private String taxRegn;
    private String gstin;
    private double outstandingBalance; // Positive means we owe them money, negative means we paid in advance
    private String status;
    private Date createdDate;

    public Supplier() {
        this.status = "ACTIVE";
        this.createdDate = new Date();
    }

    public Supplier(String supplierCode, String supplierName, String phone) {
        this();
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.phone = phone;
    }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getTaxRegn() { return taxRegn; }
    public void setTaxRegn(String taxRegn) { this.taxRegn = taxRegn; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(double outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    @Override
    public String toString() {
        return supplierName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Supplier supplier = (Supplier) obj;
        return supplierId == supplier.supplierId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(supplierId);
    }
}
