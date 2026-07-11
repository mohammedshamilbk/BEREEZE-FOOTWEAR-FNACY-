/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.Bill;
import database.BillDAO;
import database.Supplier;
import database.SupplierDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import ui.frames.MainFrame;
import ui.frames.PurchaseFrame;
import ui.frames.SupplierDialog;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class SupplierLedgerFrame
extends JInternalFrame {
    private Supplier supplier;
    private DefaultTableModel tableModel;
    private JLabel balanceLabel;
    private JLabel totalBilledLabel;
    private JLabel totalPaidLabel;
    private JLabel nameLabel;
    private JLabel phoneLabel;

    public SupplierLedgerFrame(Supplier supplier) {
        this.supplier = supplier;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Ledger: " + this.supplier.getSupplierName());
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(800, 500);
        this.setLocation(100, 100);
        JPanel jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel2 = new JPanel(new GridLayout(3, 2, 5, 5));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        this.nameLabel = UIUtils.createLabel("Supplier: " + this.supplier.getSupplierName());
        this.phoneLabel = UIUtils.createLabel("Phone: " + this.supplier.getPhone());
        jPanel2.add(this.nameLabel);
        jPanel2.add(this.phoneLabel);
        this.totalBilledLabel = UIUtils.createLabel("Total Billed: \u20b90.00");
        this.totalPaidLabel = UIUtils.createLabel("Total Paid: \u20b90.00");
        jPanel2.add(this.totalBilledLabel);
        jPanel2.add(this.totalPaidLabel);
        this.balanceLabel = UIUtils.createLabel(String.format("Current Outstanding Balance: \u20b9%.2f", this.supplier.getOutstandingBalance()));
        this.balanceLabel.setForeground(this.supplier.getOutstandingBalance() > 0.0 ? UIConstants.DANGER_COLOR : UIConstants.SUCCESS_COLOR);
        this.balanceLabel.setFont(UIConstants.HEADING_FONT);
        jPanel2.add(this.balanceLabel);
        jPanel2.add(new JLabel(""));
        jPanel.add((Component)jPanel2, "North");
        this.tableModel = new DefaultTableModel(new String[]{"Date", "Type", "Ref No", "Bill Amount", "Payment Amount", "Balance"}, 0);
        JTable jTable = new JTable(this.tableModel);
        jTable.setRowHeight(25);
        jTable.setFont(UIConstants.NORMAL_FONT);
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(jTable);
        jPanel.add((Component)jScrollPane, "Center");
        this.loadLedgerData();
        JPanel jPanel3 = new JPanel(new FlowLayout(2, 10, 10));
        jPanel3.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton = UIUtils.createButton("Add New Bill", actionEvent -> this.handleAddBill());
        JButton jButton2 = UIUtils.createButton("Quick Bill (Direct)", actionEvent -> this.handleQuickBill());
        JButton jButton3 = UIUtils.createSuccessButton("Make Payment", actionEvent -> this.handleMakePayment());
        JButton jButton4 = UIUtils.createButton("Edit Supplier Data", actionEvent -> {
            JFrame jFrame = (JFrame)SwingUtilities.getWindowAncestor(this);
            SupplierDialog supplierDialog = new SupplierDialog((Frame)jFrame, this.supplier);
            supplierDialog.setVisible(true);
            if (supplierDialog.isSaved()) {
                this.supplier = supplierDialog.getSupplier();
                this.setTitle("Ledger: " + this.supplier.getSupplierName());
                this.nameLabel.setText("Supplier: " + this.supplier.getSupplierName());
                this.phoneLabel.setText("Phone: " + this.supplier.getPhone());
            }
        });
        JButton jButton5 = UIUtils.createSuccessButton("Save", actionEvent -> {
            UIUtils.showSuccessDialog(this, "Saved Successfully", "All data and transactions have been securely saved to the database.");
            this.loadLedgerData();
        });
        jPanel3.add(jButton5);
        jPanel3.add(jButton4);
        jPanel3.add(jButton);
        jPanel3.add(jButton2);
        jPanel3.add(jButton3);
        jPanel.add((Component)jPanel3, "South");
        this.add(jPanel);
    }

    private void handleAddBill() {
        JFrame jFrame = (JFrame)SwingUtilities.getWindowAncestor(this);
        PurchaseFrame purchaseFrame = new PurchaseFrame(jFrame);
        if (jFrame instanceof MainFrame) {
            ((MainFrame)jFrame).showFrame(purchaseFrame);
        } else {
            if (this.getParent() instanceof JDesktopPane) {
                this.getParent().add(purchaseFrame);
            } else if (this.getDesktopPane() != null) {
                this.getDesktopPane().add(purchaseFrame);
            } else if (jFrame != null && jFrame.getContentPane() instanceof JDesktopPane) {
                jFrame.getContentPane().add(purchaseFrame);
            } else if (this.getParent() != null) {
                this.getParent().add(purchaseFrame);
            }
            purchaseFrame.setVisible(true);
            try {
                purchaseFrame.setSelected(true);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void handleQuickBill() {
        JPanel jPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        jPanel.add(new JLabel("Ref No (Bill No):"));
        JTextField jTextField = new JTextField("INV-" + System.currentTimeMillis() % 10000L);
        jPanel.add(jTextField);
        jPanel.add(new JLabel("Total Bill Amount:"));
        JTextField jTextField2 = new JTextField("0.00");
        jPanel.add(jTextField2);
        jPanel.add(new JLabel("Payment Given Now:"));
        JTextField jTextField3 = new JTextField("0.00");
        jPanel.add(jTextField3);
        Object[] objectArray = new Object[]{"Save", "Cancel"};
        int n = JOptionPane.showOptionDialog(this, jPanel, "Quick Direct Bill Entry", 2, -1, null, objectArray, objectArray[0]);
        if (n == 0) {
            Object object = jTextField.getText().trim();
            if (((String)object).isEmpty()) {
                object = "INV-" + System.currentTimeMillis() % 10000L;
            }
            try {
                double d = Double.parseDouble(jTextField2.getText().trim());
                double d2 = Double.parseDouble(jTextField3.getText().trim());
                if (d <= 0.0) {
                    UIUtils.showErrorDialog(this, "Invalid Input", "Bill amount must be greater than zero.");
                    return;
                }
                if (d2 < 0.0) {
                    UIUtils.showErrorDialog(this, "Invalid Input", "Payment amount cannot be negative.");
                    return;
                }
                double d3 = d - d2;
                SupplierDAO.updateOutstandingBalance(this.supplier.getSupplierId(), d3);
                Bill bill = new Bill();
                bill.setBillNumber((String)object);
                bill.setBillType("PURCHASE");
                bill.setBillDate(new Date());
                bill.setSupplierId(this.supplier.getSupplierId());
                bill.setUserId(1);
                bill.setSubtotal(d);
                bill.setTotalAmount(d);
                bill.setPaidAmount(d2);
                bill.setStatus("PENDING");
                BillDAO.saveBill(bill);
                this.addBillEntry((String)object, d, d2);
                UIUtils.showSuccessDialog(this, "Quick Bill Added", "Bill " + (String)object + " for \u20b9" + d + " added successfully.");
            }
            catch (NumberFormatException numberFormatException) {
                UIUtils.showErrorDialog(this, "Invalid Input", "Please enter valid numeric amounts.");
            }
        }
    }

    private void handleMakePayment() {
        JPanel jPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        jPanel.add(new JLabel("Amount:"));
        JTextField jTextField = new JTextField("0.00");
        jPanel.add(jTextField);
        jPanel.add(new JLabel("Payment Type:"));
        JComboBox<String> jComboBox = new JComboBox<String>(new String[]{"Cash", "Card", "UPI", "Check", "Bank Transfer"});
        jPanel.add(jComboBox);
        Object[] objectArray = new Object[]{"Save", "Cancel"};
        int n = JOptionPane.showOptionDialog(this, jPanel, "Enter Payment Details", 2, -1, null, objectArray, objectArray[0]);
        if (n == 0) {
            String string = jTextField.getText().trim();
            String string2 = (String)jComboBox.getSelectedItem();
            if (!string.isEmpty()) {
                try {
                    double d = Double.parseDouble(string);
                    if (d <= 0.0) {
                        UIUtils.showErrorDialog(this, "Invalid Input", "Payment amount must be greater than zero.");
                        return;
                    }
                    SupplierDAO.updateOutstandingBalance(this.supplier.getSupplierId(), -d);
                    this.tableModel.addRow(new Object[]{LocalDate.now().toString(), "PAYMENT (" + string2.toUpperCase() + ")", "PAY-" + System.currentTimeMillis() % 10000L, "0.00", String.format("%.2f", d), String.format("%.2f", this.supplier.getOutstandingBalance())});
                    this.updateSummary();
                    UIUtils.showSuccessDialog(this, "Payment Recorded", "Payment of \u20b9" + d + " via " + string2 + " successfully recorded.");
                }
                catch (NumberFormatException numberFormatException) {
                    UIUtils.showErrorDialog(this, "Invalid Input", "Please enter a valid numeric amount.");
                }
            }
        }
    }

    private void loadLedgerData() {
        double d;
        this.tableModel.setRowCount(0);
        List<Bill> list = BillDAO.getBillsBySupplier(this.supplier.getSupplierId());
        double d2 = this.supplier.getOutstandingBalance();
        double d3 = 0.0;
        if (list != null) {
            for (Bill bill : list) {
                d3 += bill.getTotalAmount() - bill.getPaidAmount();
            }
        }
        if (Math.abs(d = d2 - d3) > 0.01) {
            this.tableModel.addRow(new Object[]{LocalDate.now().toString(), "OPENING/EXISTING", "SYS-BAL", d > 0.0 ? String.format("%.2f", d) : "0.00", d < 0.0 ? String.format("%.2f", -d) : "0.00", String.format("%.2f", d)});
        }
        double d4 = d;
        if (list != null && !list.isEmpty()) {
            for (int i = list.size() - 1; i >= 0; --i) {
                Bill bill = list.get(i);
                double d5 = bill.getTotalAmount();
                double d6 = bill.getPaidAmount();
                this.tableModel.addRow(new Object[]{new SimpleDateFormat("yyyy-MM-dd").format(bill.getBillDate()), bill.getBillType(), bill.getBillNumber(), String.format("%.2f", d5), String.format("%.2f", d6), String.format("%.2f", d4 += d5 - d6)});
            }
        }
        this.updateSummary();
    }

    public Supplier getSupplier() {
        return this.supplier;
    }

    public void addBillEntry(String string, double d, double d2) {
        this.tableModel.addRow(new Object[]{LocalDate.now().toString(), "PURCHASE", string, String.format("%.2f", d), "0.00", String.format("%.2f", this.supplier.getOutstandingBalance() + d2)});
        if (d2 > 0.0) {
            this.tableModel.addRow(new Object[]{LocalDate.now().toString(), "PAYMENT (ADVANCE)", string + "-PAY", "0.00", String.format("%.2f", d2), String.format("%.2f", this.supplier.getOutstandingBalance())});
        }
        this.updateSummary();
    }

    public void updateSummary() {
        double d = 0.0;
        double d2 = 0.0;
        for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
            d += Double.parseDouble(this.tableModel.getValueAt(i, 3).toString());
            d2 += Double.parseDouble(this.tableModel.getValueAt(i, 4).toString());
        }
        if (this.totalBilledLabel != null) {
            this.totalBilledLabel.setText(String.format("Total Billed: \u20b9%.2f", d));
        }
        if (this.totalPaidLabel != null) {
            this.totalPaidLabel.setText(String.format("Total Paid: \u20b9%.2f", d2));
        }
        this.balanceLabel.setText(String.format("Current Outstanding Balance: \u20b9%.2f", this.supplier.getOutstandingBalance()));
        this.balanceLabel.setForeground(this.supplier.getOutstandingBalance() > 0.0 ? UIConstants.DANGER_COLOR : UIConstants.SUCCESS_COLOR);
    }
}
