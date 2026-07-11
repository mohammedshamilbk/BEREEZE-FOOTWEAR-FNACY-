package database;

import java.util.Date;

public class CashRegister {
    private int registerId;
    private Date registerDate;
    private double openingBalance;
    private double closingBalance;
    private double cashSales;
    private double cashIn;
    private double cashOut;
    private String status;
    private int openedBy;
    private int closedBy;
    private Date openedAt;
    private Date closedAt;
    
    // For joined display purposes
    private String openedByName;
    private String closedByName;
    
    // For live preview
    private boolean isLivePreview;
    private double displayClosingBalance;

    public CashRegister() {}

    public int getRegisterId() { return registerId; }
    public void setRegisterId(int registerId) { this.registerId = registerId; }

    public Date getRegisterDate() { return registerDate; }
    public void setRegisterDate(Date registerDate) { this.registerDate = registerDate; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public double getClosingBalance() { return closingBalance; }
    public void setClosingBalance(double closingBalance) { this.closingBalance = closingBalance; }

    public double getCashSales() { return cashSales; }
    public void setCashSales(double cashSales) { this.cashSales = cashSales; }

    public double getCashIn() { return cashIn; }
    public void setCashIn(double cashIn) { this.cashIn = cashIn; }

    public double getCashOut() { return cashOut; }
    public void setCashOut(double cashOut) { this.cashOut = cashOut; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getOpenedBy() { return openedBy; }
    public void setOpenedBy(int openedBy) { this.openedBy = openedBy; }

    public int getClosedBy() { return closedBy; }
    public void setClosedBy(int closedBy) { this.closedBy = closedBy; }

    public Date getOpenedAt() { return openedAt; }
    public void setOpenedAt(Date openedAt) { this.openedAt = openedAt; }

    public Date getClosedAt() { return closedAt; }
    public void setClosedAt(Date closedAt) { this.closedAt = closedAt; }

    public String getOpenedByName() { return openedByName; }
    public void setOpenedByName(String openedByName) { this.openedByName = openedByName; }

    public String getClosedByName() { return closedByName; }
    public void setClosedByName(String closedByName) { this.closedByName = closedByName; }
    
    public boolean isLivePreview() { return isLivePreview; }
    public void setLivePreview(boolean isLivePreview) { this.isLivePreview = isLivePreview; }
    
    public double getDisplayClosingBalance() { return displayClosingBalance; }
    public void setDisplayClosingBalance(double displayClosingBalance) { this.displayClosingBalance = displayClosingBalance; }
}
