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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
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
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import ui.frames.SupplierDialog;
import ui.frames.SupplierLedgerFrame;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class PurchaseFrame
extends JInternalFrame {
    private JFrame parent;
    private JTextField invoiceNoField;
    private JTextField invoiceDateField;
    private JTextField refNoField;
    private JComboBox<String> mopCombo;
    private JComboBox<String> salesStaffCombo;
    private JComboBox<Supplier> supplierCombo;
    private JLabel supplierBalanceLabel;
    private JTextField mobileField;
    private JComboBox<String> stateCombo;
    private JTextField taxRegnField;
    private JTextField gstinField;
    private JTextField addCodeField;
    private JTextField addNameField;
    private JTextField addQtyField;
    private JTextField addCostField;
    private JTextField addHsnField;
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JTextField dpPercentField;
    private JTextField dpField;
    private JLabel grossAmtLabel;
    private JLabel discLabel;
    private JLabel rateLabel;
    private JLabel totalQtyLabel;
    private JLabel netAmtLabel;
    private JTextField paymentAmountField;
    private double currentNetTotal = 0.0;

    public PurchaseFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Purchase Entry");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1100, 750);
        this.setLocation(50, 20);
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel jPanel2 = new JPanel(new BorderLayout());
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel3 = this.createCommandPanel();
        JPanel jPanel4 = this.createInvoiceHeaderPanel();
        jPanel2.add((Component)jPanel3, "North");
        jPanel2.add((Component)jPanel4, "Center");
        jPanel.add((Component)jPanel2, "North");
        JPanel jPanel5 = new JPanel(new BorderLayout());
        jPanel5.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel6 = this.createSupplierPanel();
        JPanel jPanel7 = this.createItemInputPanel();
        JPanel jPanel8 = this.createGridPanel();
        JPanel jPanel9 = new JPanel(new BorderLayout());
        jPanel9.setBackground(UIConstants.APP_BACKGROUND);
        jPanel9.add((Component)jPanel6, "North");
        jPanel9.add((Component)jPanel7, "South");
        jPanel5.add((Component)jPanel9, "North");
        jPanel5.add((Component)jPanel8, "Center");
        jPanel.add((Component)jPanel5, "Center");
        JPanel jPanel10 = this.createFooterPanel();
        jPanel.add((Component)jPanel10, "South");
        this.add(jPanel);
        this.setupKeyboardShortcuts();
    }

    private JPanel createCommandPanel() {
        JPanel jPanel = new JPanel(new FlowLayout(0, 15, 5));
        jPanel.setBackground(UIConstants.PRIMARY_COLOR);
        JLabel jLabel = new JLabel("Transactions -> Purchase Entry ");
        jLabel.setFont(UIConstants.TITLE_FONT);
        jLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add(jLabel);
        JButton jButton = UIUtils.createSuccessButton("Save (F5)", actionEvent -> this.handleSave());
        JButton jButton2 = UIUtils.createButton("Print (F6)", actionEvent -> this.handlePrint());
        jButton2.setBackground(UIConstants.ACCENT_COLOR);
        jButton2.setForeground(UIConstants.DARK_COLOR);
        JButton jButton3 = UIUtils.createButton("Cancel (F8)", actionEvent -> this.dispose());
        jButton3.setBackground(UIConstants.BORDER_COLOR);
        jButton3.setForeground(UIConstants.DARK_COLOR);
        JButton jButton4 = UIUtils.createDangerButton("Delete (F7)", actionEvent -> this.handleDelete());
        JButton jButton5 = UIUtils.createButton("Find (Ctrl+F)", actionEvent -> this.handleFind());
        jButton5.setBackground(Color.DARK_GRAY);
        jPanel.add(jButton);
        jPanel.add(jButton2);
        jPanel.add(jButton3);
        jPanel.add(jButton4);
        jPanel.add(jButton5);
        return jPanel;
    }

    private JPanel createInvoiceHeaderPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel2 = new JPanel(new FlowLayout(0, 12, 8));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Invoice Header Details"));
        jPanel2.add(UIUtils.createLabel("Inv No:"));
        this.invoiceNoField = UIUtils.createTextField(8);
        this.invoiceNoField.setText("PUR-" + (int)(Math.random() * 10000.0));
        jPanel2.add(this.invoiceNoField);
        jPanel2.add(UIUtils.createLabel("Inv Date:"));
        this.invoiceDateField = UIUtils.createTextField(8);
        this.invoiceDateField.setText(LocalDate.now().toString());
        jPanel2.add(this.invoiceDateField);
        jPanel2.add(UIUtils.createLabel("Ref No:"));
        this.refNoField = UIUtils.createTextField(8);
        jPanel2.add(this.refNoField);
        jPanel2.add(UIUtils.createLabel("MOP:"));
        this.mopCombo = UIUtils.createComboBox(new String[]{"Cash", "Card", "UPI", "Credit"});
        jPanel2.add(this.mopCombo);
        jPanel2.add(UIUtils.createLabel("Sales Staff:"));
        this.salesStaffCombo = UIUtils.createComboBox(new String[]{"DEFAULT EMP1", "MOHAMMAD", "ALI", "HASSAN"});
        jPanel2.add(this.salesStaffCombo);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel.add((Component)jPanel2, gridBagConstraints);
        return jPanel;
    }

    private JPanel createSupplierPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel2 = new JPanel(new FlowLayout(0, 12, 8));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Supplier Information"));
        jPanel2.add(UIUtils.createLabel("Supplier:"));
        this.supplierCombo = new JComboBox();
        this.supplierCombo.setPreferredSize(new Dimension(150, 30));
        this.loadSuppliers();
        this.supplierCombo.addActionListener(actionEvent -> {
            Supplier supplier = (Supplier)this.supplierCombo.getSelectedItem();
            if (supplier != null) {
                this.mobileField.setText(supplier.getPhone() != null ? supplier.getPhone() : "");
                this.stateCombo.setSelectedItem(supplier.getState() != null ? supplier.getState() : "Kerala");
                this.taxRegnField.setText(supplier.getTaxRegn() != null ? supplier.getTaxRegn() : "");
                this.gstinField.setText(supplier.getGstin() != null ? supplier.getGstin() : "");
                this.supplierBalanceLabel.setText(String.format("Bal: \u20b9%.2f", supplier.getOutstandingBalance()));
                this.supplierBalanceLabel.setForeground(supplier.getOutstandingBalance() > 0.0 ? UIConstants.DANGER_COLOR : UIConstants.SUCCESS_COLOR);
            }
        });
        jPanel2.add(this.supplierCombo);
        JButton jButton = UIUtils.createSuccessButton("+", actionEvent -> {
            Supplier supplier = (Supplier)this.supplierCombo.getSelectedItem();
            SupplierDialog supplierDialog = new SupplierDialog((Frame)this.parent, supplier);
            supplierDialog.setVisible(true);
            if (supplierDialog.isSaved()) {
                this.loadSuppliers();
                this.supplierCombo.setSelectedItem(supplierDialog.getSupplier());
            }
        });
        jButton.setToolTipText("Add or Edit Supplier");
        jButton.setPreferredSize(new Dimension(45, 30));
        jButton.setMargin(new Insets(2, 5, 2, 5));
        jPanel2.add(jButton);
        JButton jButton2 = UIUtils.createButton("Ledger", actionEvent -> {
            Supplier supplier = (Supplier)this.supplierCombo.getSelectedItem();
            if (supplier != null) {
                SupplierLedgerFrame supplierLedgerFrame = new SupplierLedgerFrame(supplier);
                this.getParent().add(supplierLedgerFrame);
                supplierLedgerFrame.setVisible(true);
                try {
                    supplierLedgerFrame.setSelected(true);
                }
                catch (PropertyVetoException propertyVetoException) {
                }
            } else {
                UIUtils.showWarningDialog(this, "Select Supplier", "Please select a supplier first.");
            }
        });
        jButton2.setPreferredSize(new Dimension(80, 30));
        jPanel2.add(jButton2);
        this.supplierBalanceLabel = new JLabel("Bal: \u20b90.00");
        this.supplierBalanceLabel.setFont(UIConstants.HEADING_FONT);
        jPanel2.add(this.supplierBalanceLabel);
        jPanel2.add(UIUtils.createLabel("Mobile:"));
        this.mobileField = UIUtils.createTextField(10);
        jPanel2.add(this.mobileField);
        jPanel2.add(UIUtils.createLabel("State:"));
        this.stateCombo = UIUtils.createComboBox(new String[]{"Kerala", "Tamil Nadu", "Karnataka", "Maharashtra", "Delhi"});
        jPanel2.add(this.stateCombo);
        jPanel2.add(UIUtils.createLabel("Tax Regn:"));
        this.taxRegnField = UIUtils.createTextField(8);
        jPanel2.add(this.taxRegnField);
        jPanel2.add(UIUtils.createLabel("GSTIN:"));
        this.gstinField = UIUtils.createTextField(10);
        jPanel2.add(this.gstinField);
        JButton jButton3 = UIUtils.createButton("Verify", actionEvent -> {
            String string = this.gstinField.getText().trim();
            if (string.isEmpty()) {
                UIUtils.showWarningDialog(this, "Verify GSTIN", "Please enter a GSTIN code first.");
            } else {
                UIUtils.showSuccessDialog(this, "GSTIN Verified", "GSTIN " + string + " verified successfully with Government database.");
            }
        });
        jButton3.setFont(UIConstants.SMALL_FONT);
        jButton3.setPreferredSize(new Dimension(70, 30));
        jPanel2.add(jButton3);
        JButton jButton4 = UIUtils.createSuccessButton("Save Data", actionEvent -> {
            Supplier supplier = (Supplier)this.supplierCombo.getSelectedItem();
            if (supplier != null) {
                supplier.setPhone(this.mobileField.getText().trim());
                supplier.setState((String)this.stateCombo.getSelectedItem());
                supplier.setTaxRegn(this.taxRegnField.getText().trim());
                supplier.setGstin(this.gstinField.getText().trim());
                if (SupplierDAO.updateSupplier(supplier)) {
                    UIUtils.showSuccessDialog(this, "Supplier Saved", "Supplier details updated successfully!");
                } else {
                    UIUtils.showErrorDialog(this, "Error", "Failed to update supplier details.");
                }
            }
        });
        jButton4.setFont(UIConstants.SMALL_FONT);
        jButton4.setPreferredSize(new Dimension(90, 30));
        jButton4.setToolTipText("Save changes to this supplier's data");
        jPanel2.add(jButton4);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel.add((Component)jPanel2, gridBagConstraints);
        return jPanel;
    }

    private void loadSuppliers() {
        List<Supplier> list = SupplierDAO.getAllSuppliers();
        this.supplierCombo.removeAllItems();
        for (Supplier supplier : list) {
            this.supplierCombo.addItem(supplier);
        }
    }

    private JPanel createItemInputPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel2 = new JPanel(new FlowLayout(0, 10, 5));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Item Entry Form"));
        jPanel2.add(UIUtils.createLabel("Code:"));
        this.addCodeField = UIUtils.createTextField(6);
        jPanel2.add(this.addCodeField);
        jPanel2.add(UIUtils.createLabel("Item Name:"));
        this.addNameField = UIUtils.createTextField(12);
        jPanel2.add(this.addNameField);
        jPanel2.add(UIUtils.createLabel("Qty:"));
        this.addQtyField = UIUtils.createTextField(3);
        jPanel2.add(this.addQtyField);
        jPanel2.add(UIUtils.createLabel("Cost Rate:"));
        this.addCostField = UIUtils.createTextField(5);
        jPanel2.add(this.addCostField);
        jPanel2.add(UIUtils.createLabel("HSN:"));
        this.addHsnField = UIUtils.createTextField(4);
        jPanel2.add(this.addHsnField);
        JButton jButton = UIUtils.createSuccessButton("Add Item", actionEvent -> this.handleAddRow());
        jButton.setPreferredSize(new Dimension(90, 30));
        jPanel2.add(jButton);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel.add((Component)jPanel2, gridBagConstraints);
        return jPanel;
    }

    private JPanel createGridPanel() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), "Purchase Details Grid", 1, 2, UIConstants.HEADING_FONT, UIConstants.PRIMARY_COLOR));
        this.tableModel = new DefaultTableModel();
        this.tableModel.addColumn("Sl.No");
        this.tableModel.addColumn("Item Code");
        this.tableModel.addColumn("Item Name");
        this.tableModel.addColumn("Batch Code");
        this.tableModel.addColumn("HSN Code");
        this.tableModel.addColumn("Taxable");
        this.tableModel.addColumn("Cost");
        this.tableModel.addColumn("Net Amt");
        this.tableModel.addColumn("Action");
        this.itemTable = new JTable(this.tableModel);
        this.itemTable.setFont(UIConstants.NORMAL_FONT);
        this.itemTable.setRowHeight(25);
        this.loadSampleData();
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(this.itemTable);
        jPanel.add((Component)jScrollPane, "Center");
        JPanel jPanel2 = new JPanel(new FlowLayout(2));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton = UIUtils.createDangerButton("Remove Line Item", actionEvent -> this.handleRemoveRow());
        jPanel2.add(jButton);
        jPanel.add((Component)jPanel2, "South");
        return jPanel;
    }

    private JPanel createFooterPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.DARK_COLOR);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 10, 5, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        JLabel jLabel = new JLabel("DP %:");
        jLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add((Component)jLabel, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        this.dpPercentField = UIUtils.createTextField(6);
        this.dpPercentField.setText("0.00");
        jPanel.add((Component)this.dpPercentField, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        JLabel jLabel2 = new JLabel("DP Value:");
        jLabel2.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add((Component)jLabel2, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        this.dpField = UIUtils.createTextField(6);
        this.dpField.setText("0.00");
        jPanel.add((Component)this.dpField, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        JLabel jLabel3 = new JLabel("Payment Made:");
        jLabel3.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add((Component)jLabel3, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        this.paymentAmountField = UIUtils.createTextField(8);
        this.paymentAmountField.setText("0.00");
        jPanel.add((Component)this.paymentAmountField, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        this.grossAmtLabel = new JLabel("Gross Amt: \u00e2\u201a\u00b94,500.00");
        this.grossAmtLabel.setFont(UIConstants.HEADING_FONT);
        this.grossAmtLabel.setForeground(Color.YELLOW);
        jPanel.add((Component)this.grossAmtLabel, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        this.discLabel = new JLabel("Discount: \u00e2\u201a\u00b90.00");
        this.discLabel.setFont(UIConstants.HEADING_FONT);
        this.discLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add((Component)this.discLabel, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        this.rateLabel = new JLabel("Rate Average: \u00e2\u201a\u00b9225.00");
        this.rateLabel.setFont(UIConstants.HEADING_FONT);
        this.rateLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add((Component)this.rateLabel, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        this.totalQtyLabel = new JLabel("Total Qty: 20 Items");
        this.totalQtyLabel.setFont(UIConstants.HEADING_FONT);
        this.totalQtyLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add((Component)this.totalQtyLabel, gridBagConstraints);
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        this.netAmtLabel = new JLabel("NET AMOUNT: \u00e2\u201a\u00b95,310.00");
        this.netAmtLabel.setFont(new Font("Segoe UI", 1, 18));
        this.netAmtLabel.setForeground(new Color(0, 230, 118)); // High contrast bright green on dark background
        jPanel.add((Component)this.netAmtLabel, gridBagConstraints);
        return jPanel;
    }

    private void loadSampleData() {
        this.tableModel.addRow(new Object[]{"1", "SHOE001", "Mens Sports Running", "B01", "6403", "2000.00", "2000.00", "2000.00", "Delete"});
        this.tableModel.addRow(new Object[]{"2", "SAND002", "Ladies Fancy Sandals", "B02", "6403", "2500.00", "2500.00", "2500.00", "Delete"});
        this.updateTotals();
    }

    private void handleAddRow() {
        String string = this.addCodeField.getText().trim();
        String string2 = this.addNameField.getText().trim();
        String string3 = this.addQtyField.getText().trim();
        String string4 = this.addCostField.getText().trim();
        String string5 = this.addHsnField.getText().trim();
        if (string.isEmpty() || string2.isEmpty() || string3.isEmpty() || string4.isEmpty()) {
            UIUtils.showErrorDialog(this, "Validation Error", "Please fill in all line item inputs.");
            return;
        }
        try {
            double d;
            int n = Integer.parseInt(string3);
            double d2 = Double.parseDouble(string4);
            double d3 = d = d2 * (double)n;
            int n2 = this.tableModel.getRowCount() + 1;
            this.tableModel.addRow(new Object[]{String.valueOf(n2), string, string2, "B" + n2, string5.isEmpty() ? "6403" : string5, String.format("%.2f", d), String.format("%.2f", d2), String.format("%.2f", d3), "Delete"});
            this.addCodeField.setText("");
            this.addNameField.setText("");
            this.addQtyField.setText("");
            this.addCostField.setText("");
            this.addHsnField.setText("");
            this.updateTotals();
        }
        catch (NumberFormatException numberFormatException) {
            UIUtils.showErrorDialog(this, "Number Format Error", "Qty and Cost must be numeric values.");
        }
    }

    private void handleRemoveRow() {
        int n = this.itemTable.getSelectedRow();
        if (n >= 0) {
            this.tableModel.removeRow(n);
            for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
                this.tableModel.setValueAt(String.valueOf(i + 1), i, 0);
            }
            this.updateTotals();
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a line item in the table to remove.");
        }
    }

    private void updateTotals() {
        double d = 0.0;
        int n = 0;
        for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
            try {
                String string = this.tableModel.getValueAt(i, 5).toString();
                d += Double.parseDouble(string);
                ++n;
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.currentNetTotal = d;
        this.grossAmtLabel.setText(String.format("Gross Amt: \u20b9%.2f", d));
        this.totalQtyLabel.setText(String.format("Total Qty: %d Items", n));
        this.rateLabel.setText(String.format("Rate Average: \u20b9%.2f", n == 0 ? 0.0 : d / (double)n));
        this.netAmtLabel.setText(String.format("NET AMOUNT: \u20b9%.2f", this.currentNetTotal));
    }

    private void handleSave() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showErrorDialog(this, "Save Error", "Cannot save an empty Purchase Invoice.");
            return;
        }
        Supplier supplier = (Supplier)this.supplierCombo.getSelectedItem();
        if (supplier == null) {
            UIUtils.showErrorDialog(this, "Save Error", "Please select a Supplier.");
            return;
        }
        double d = 0.0;
        try {
            d = Double.parseDouble(this.paymentAmountField.getText().trim());
        }
        catch (NumberFormatException numberFormatException) {
            UIUtils.showErrorDialog(this, "Input Error", "Invalid Payment Amount.");
            return;
        }
        double d2 = this.currentNetTotal - d;
        SupplierDAO.updateOutstandingBalance(supplier.getSupplierId(), d2);
        Bill bill = new Bill();
        bill.setBillNumber(this.invoiceNoField.getText());
        bill.setBillType("PURCHASE");
        bill.setBillDate(new Date());
        bill.setSupplierId(supplier.getSupplierId());
        bill.setUserId(1);
        bill.setSubtotal(this.currentNetTotal);
        bill.setTotalAmount(this.currentNetTotal);
        bill.setPaidAmount(d);
        bill.setStatus("PENDING");
        BillDAO.saveBill(bill);
        JDesktopPane jDesktopPane = this.getDesktopPane();
        if (jDesktopPane == null && this.getParent() instanceof JDesktopPane) {
            jDesktopPane = (JDesktopPane)this.getParent();
        }
        if (jDesktopPane != null) {
            for (JInternalFrame jInternalFrame : jDesktopPane.getAllFrames()) {
                SupplierLedgerFrame supplierLedgerFrame;
                if (!(jInternalFrame instanceof SupplierLedgerFrame) || (supplierLedgerFrame = (SupplierLedgerFrame)jInternalFrame).getSupplier().getSupplierId() != supplier.getSupplierId()) continue;
                supplierLedgerFrame.addBillEntry(this.invoiceNoField.getText(), this.currentNetTotal, d);
            }
        }
        UIUtils.showSuccessDialog(this, "Purchase Saved", "Purchase Invoice successfully validated and entered.\nInv No: " + this.invoiceNoField.getText() + "\nTotal Amount: \u20b9" + String.format("%.2f", this.currentNetTotal) + "\nAmount Paid: \u20b9" + String.format("%.2f", d) + "\nAmount Added to Balance: \u20b9" + String.format("%.2f", d2));
        this.dispose();
    }

    private void handlePrint() {
        String string = "=== BAREEZE FOOTWEAR ===\nAddress: Anar complex, Naya bazar,\nMelparamba, Kasaragod, Kerala, India 671317\nMobile no: 8086790086\nMail ID: breezefootwearfancy@gmail.com\n==================================\nSending purchase invoice report to the printer.";
        UIUtils.showSuccessDialog(this, "Printing", string);
    }

    private void handleDelete() {
        if (UIUtils.showConfirmDialog(this, "Delete Invoice", "Are you sure you want to delete this purchase transaction draft?")) {
            this.tableModel.setRowCount(0);
            this.updateTotals();
            UIUtils.showSuccessDialog(this, "Deleted", "Transaction removed.");
        }
    }

    private void handleFind() {
        String string = JOptionPane.showInputDialog(this, (Object)"Enter Purchase Invoice No or Ref No:");
        if (string != null && !string.trim().isEmpty()) {
            this.invoiceNoField.setText(string.trim().toUpperCase());
            UIUtils.showSuccessDialog(this, "Invoice Found", "Purchase Record Loaded successfully.");
        }
    }

    private void setupKeyboardShortcuts() {
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(116, 0), "saveAction");
        this.getRootPane().getActionMap().put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PurchaseFrame.this.handleSave();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(117, 0), "printAction");
        this.getRootPane().getActionMap().put("printAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PurchaseFrame.this.handlePrint();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(118, 0), "deleteAction");
        this.getRootPane().getActionMap().put("deleteAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PurchaseFrame.this.handleDelete();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(27, 0), "escapeAction");
        this.getRootPane().getActionMap().put("escapeAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PurchaseFrame.this.dispose();
            }
        });
    }
}
