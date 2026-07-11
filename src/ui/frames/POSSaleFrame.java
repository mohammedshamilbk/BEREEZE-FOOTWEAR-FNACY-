/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.BarcodeGenerator;
import database.Bill;
import database.BillDAO;
import database.BillItem;
import database.BillItemDAO;
import database.Customer;
import database.CustomerDAO;
import database.ItemMaster;
import database.ItemMasterDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import ui.frames.DashboardFrame;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class POSSaleFrame
extends JInternalFrame {
    private JFrame parent;
    private JComboBox<String> customerCombo;
    private JComboBox<String> saleOptionCombo;
    private JLabel custInfoLabel;
    private JTextField customerSearchField;
    private JTextField itemCodeField;
    private JTextField qtyField;
    private JTable billItemsTable;
    private DefaultTableModel tableModel;
    private boolean isRecalculatingLumpsum = false;
    private JLabel subtotalLabel;
    private JLabel discountLabel;
    private JTextField totalField;
    private JTextField itemNameField;
    private JTextField itemPriceField;
    private JTextField amountPaidField;
    private JTextField changeField;
    private JComboBox<String> paymentModeCombo;
    private double currentNetTotal = 0.0;
    private static Map<String, List<Vector<Object>>> heldBills = new HashMap<String, List<Vector<Object>>>();

    public POSSaleFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("POS Sale Billing");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1050, 720);
        this.setLocation(80, 40);
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel jLabel = UIUtils.createTitleLabel("POS Sale Billing System");
        jPanel.add((Component)jLabel, "North");
        JPanel jPanel2 = this.createTopPanel();
        jPanel.add((Component)jPanel2, "First");
        JPanel jPanel3 = this.createCenterPanel();
        jPanel.add((Component)jPanel3, "Center");
        JPanel jPanel4 = this.createBottomPanel();
        jPanel.add((Component)jPanel4, "South");
        this.add(jPanel);
        this.setupKeyboardShortcuts();
        SwingUtilities.invokeLater(() -> this.itemCodeField.requestFocusInWindow());
    }

    private JPanel createTopPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel2 = new JPanel(new BorderLayout(10, 5));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Customer & Invoice Info"));
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 15, 5));
        jPanel3.setBackground(UIConstants.APP_BACKGROUND);
        jPanel3.add(UIUtils.createLabel("Customer:"));
        this.customerCombo = new JComboBox();
        this.customerCombo.setFont(UIConstants.NORMAL_FONT);
        this.customerCombo.setPreferredSize(new Dimension(200, 30));
        this.refreshCustomerCombo();
        this.customerCombo.addActionListener(actionEvent -> this.handleCustomerSelection());
        jPanel3.add(this.customerCombo);
        JButton jButton = UIUtils.createSuccessButton("+", actionEvent -> this.handleQuickAddCustomer());
        jButton.setPreferredSize(new Dimension(40, 30));
        jButton.setToolTipText("Add New Customer");
        jPanel3.add(jButton);
        jPanel3.add(Box.createHorizontalStrut(10));
        jPanel3.add(UIUtils.createLabel("Search:"));
        this.customerSearchField = UIUtils.createTextField(12);
        this.customerSearchField.setToolTipText("Type customer name or phone and press Enter to load details");
        this.customerSearchField.addActionListener(actionEvent -> this.handleCustomerSearch());
        jPanel3.add(this.customerSearchField);
        jPanel3.add(UIUtils.createLabel("Sale Option:"));
        this.saleOptionCombo = UIUtils.createComboBox(new String[]{"Retail Sale", "Wholesale Sale", "Tax Free Sale", "Staff Sale"});
        jPanel3.add(this.saleOptionCombo);
        jPanel3.add(UIUtils.createLabel("  Invoice Date: " + String.valueOf(LocalDate.now())));
        this.custInfoLabel = UIUtils.createLabel("Walk-in Customer (No details)");
        this.custInfoLabel.setFont(new Font("Segoe UI", 1, 11));
        this.custInfoLabel.setForeground(Color.GRAY);
        JPanel jPanel4 = new JPanel(new FlowLayout(0, 20, 2));
        jPanel4.setBackground(UIConstants.APP_BACKGROUND);
        jPanel4.add(this.custInfoLabel);
        jPanel2.add((Component)jPanel3, "Center");
        jPanel2.add((Component)jPanel4, "South");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel.add((Component)jPanel2, gridBagConstraints);
        return jPanel;
    }

    private JPanel createCenterPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new FlowLayout(0, 12, 10));
        jPanel3.setBackground(UIConstants.APP_BACKGROUND);
        jPanel3.setBorder(BorderFactory.createTitledBorder("Scan / Add Footwear Items"));
        jPanel3.add(UIUtils.createLabel("Scan Barcode / Code:"));
        this.itemCodeField = UIUtils.createTextField(12);
        this.itemCodeField.addActionListener(actionEvent -> this.handleAddItem());
        jPanel3.add(this.itemCodeField);
        jPanel3.add(UIUtils.createLabel("Name:"));
        this.itemNameField = UIUtils.createTextField(15);
        jPanel3.add(this.itemNameField);
        jPanel3.add(UIUtils.createLabel("Price:"));
        this.itemPriceField = UIUtils.createTextField(6);
        this.itemPriceField.addActionListener(actionEvent -> this.handleAddItem());
        jPanel3.add(this.itemPriceField);
        jPanel3.add(UIUtils.createLabel("Qty:"));
        this.qtyField = UIUtils.createTextField(4);
        this.qtyField.setText("1");
        this.qtyField.addActionListener(actionEvent -> this.handleAddItem());
        jPanel3.add(this.qtyField);
        JButton jButton = UIUtils.createSuccessButton("Add (Enter)", actionEvent -> this.handleAddItem());
        jPanel3.add(jButton);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel2.add((Component)jPanel3, gridBagConstraints);
        jPanel.add((Component)jPanel2, "North");
        this.tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int n, int n2) {
                return n2 == 3 || n2 == 6 || n2 == 7;
            }
        };
        this.tableModel.addColumn("Item Code");
        this.tableModel.addColumn("Item Name");
        this.tableModel.addColumn("Category");
        this.tableModel.addColumn("Qty");
        this.tableModel.addColumn("Unit Price");
        this.tableModel.addColumn("Cost Price");
        this.tableModel.addColumn("Discount %");
        this.tableModel.addColumn("Line Total");
        this.billItemsTable = new JTable(this.tableModel);
        this.billItemsTable.setFont(UIConstants.NORMAL_FONT);
        this.billItemsTable.setRowHeight(25);
        this.tableModel.addTableModelListener(tableModelEvent -> {
            int n = tableModelEvent.getFirstRow();
            int n2 = tableModelEvent.getColumn();
            if (!(n < 0 || n2 != 3 && n2 != 6 && n2 != 7 || this.isRecalculatingLumpsum)) {
                if (n2 == 7) {
                    try {
                        double d = Double.parseDouble(this.tableModel.getValueAt(n, 7).toString());
                        int n3 = Integer.parseInt(this.tableModel.getValueAt(n, 3).toString());
                        double d2 = Double.parseDouble(this.tableModel.getValueAt(n, 4).toString());
                        double d3 = d2 * (double)n3;
                        if (d3 > 0.0) {
                            double d4 = (d3 - d) / d3 * 100.0;
                            this.isRecalculatingLumpsum = true;
                            this.tableModel.setValueAt(d4, n, 6);
                            this.isRecalculatingLumpsum = false;
                        }
                    }
                    catch (Exception exception) {
                        exception.printStackTrace();
                    }
                } else {
                    this.recalculateRow(n);
                }
                this.updateTotals();
            }
        });
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(this.billItemsTable);
        jPanel.add((Component)jScrollPane, "Center");
        JPanel jPanel4 = new JPanel(new FlowLayout(1, 15, 5));
        jPanel4.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton2 = UIUtils.createDangerButton("Remove Item (Del)", actionEvent -> this.handleRemoveItem());
        JButton jButton3 = UIUtils.createButton("Clear Bill (F2)", actionEvent -> this.handleClearBill());
        jButton3.setBackground(UIConstants.WARNING_COLOR);
        JButton jButton4 = UIUtils.createButton("Hold Bill", actionEvent -> this.handleHoldBill());
        jButton4.setBackground(UIConstants.SECONDARY_COLOR);
        JButton jButton5 = UIUtils.createButton("Recall Bill", actionEvent -> this.handleRecallBill());
        jPanel4.add(jButton2);
        jPanel4.add(jButton3);
        jPanel4.add(jButton4);
        jPanel4.add(jButton5);
        jPanel.add((Component)jPanel4, "South");
        return jPanel;
    }

    private JPanel createBottomPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new GridLayout(3, 2, 10, 6));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Bill Summary"));
        jPanel2.setPreferredSize(new Dimension(300, 110));
        jPanel2.add(UIUtils.createLabel("Subtotal:"));
        this.subtotalLabel = UIUtils.createLabel("\u20b90.00");
        jPanel2.add(this.subtotalLabel);
        jPanel2.add(UIUtils.createLabel("Discount:"));
        this.discountLabel = UIUtils.createLabel("\u20b90.00");
        jPanel2.add(this.discountLabel);
        jPanel2.add(UIUtils.createLabel("Net Total:"));
        this.totalField = UIUtils.createTextField(10);
        this.totalField.setFont(new Font("Segoe UI", 1, 16));
        this.totalField.setForeground(UIConstants.PRIMARY_COLOR);
        this.totalField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                POSSaleFrame.this.calculateDiscountFromTotal();
            }
        });
        jPanel2.add(this.totalField);
        jPanel.add((Component)jPanel2, "West");
        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new GridLayout(3, 2, 10, 10));
        jPanel3.setBackground(UIConstants.APP_BACKGROUND);
        jPanel3.setBorder(BorderFactory.createTitledBorder("Payment Settlement"));
        jPanel3.add(UIUtils.createLabel("Payment Mode:"));
        this.paymentModeCombo = UIUtils.createComboBox(new String[]{"Cash", "Card", "UPI", "Cheque"});
        jPanel3.add(this.paymentModeCombo);
        jPanel3.add(UIUtils.createLabel("Amount Paid (\u20b9):"));
        this.amountPaidField = UIUtils.createTextField(10);
        this.amountPaidField.addActionListener(actionEvent -> this.calculateChange());
        this.amountPaidField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                POSSaleFrame.this.calculateChange();
            }
        });
        jPanel3.add(this.amountPaidField);
        jPanel3.add(UIUtils.createLabel("Change Return:"));
        this.changeField = UIUtils.createTextField(10);
        this.changeField.setEditable(false);
        this.changeField.setFont(new Font("Segoe UI", 1, 14));
        this.changeField.setForeground(UIConstants.DANGER_COLOR);
        jPanel3.add(this.changeField);
        jPanel.add((Component)jPanel3, "Center");
        JPanel jPanel4 = new JPanel(new FlowLayout(2, 15, 10));
        jPanel4.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton = UIUtils.createSuccessButton("Save & Print (F9)", actionEvent -> this.handleSaveAndPrint());
        jButton.setPreferredSize(new Dimension(150, 36));
        JButton jButton2 = UIUtils.createButton("Save Only (F5)", actionEvent -> this.handleSave());
        jButton2.setBackground(UIConstants.PRIMARY_COLOR);
        jButton2.setPreferredSize(new Dimension(130, 36));
        JButton jButton3 = UIUtils.createDangerButton("Close (Esc)", actionEvent -> this.dispose());
        jButton3.setPreferredSize(new Dimension(110, 36));
        jPanel4.add(jButton);
        jPanel4.add(jButton2);
        jPanel4.add(jButton3);
        jPanel.add((Component)jPanel4, "South");
        return jPanel;
    }

    private void handleAddItem() {
        int n;
        String itemCode = this.itemCodeField.getText().trim().toUpperCase();
        String string = this.qtyField.getText().trim();
        if (itemCode.isEmpty() && this.itemNameField.getText().trim().isEmpty()) {
            return;
        }
        int n2 = 1;
        if (!string.isEmpty()) {
            try {
                n2 = Integer.parseInt(string);
            }
            catch (NumberFormatException numberFormatException) {
                UIUtils.showErrorDialog(this, "Quantity Error", "Quantity must be an integer.");
                this.qtyField.requestFocus();
                return;
            }
        }
        String string2 = "Custom Footwear Item";
        String string3 = "DEFAULT";
        double d = 999.0;
        String string4 = this.itemNameField.getText().trim();
        String string5 = this.itemPriceField.getText().trim();
        if (!string4.isEmpty() && !string5.isEmpty()) {
            string2 = string4;
            try {
                d = Double.parseDouble(string5);
            }
            catch (NumberFormatException numberFormatException) {
                UIUtils.showErrorDialog(this, "Price Error", "Invalid price entered.");
                return;
            }
            if (itemCode.isEmpty()) {
                itemCode = "MANUAL-" + System.currentTimeMillis();
            }
        } else {
            if (itemCode.isEmpty()) {
                return;
            }
            if (itemCode.equals("SHOE001") || itemCode.equals("8901234567890")) {
                string2 = "Running Shoes Black";
                string3 = "Shoes";
                d = 5999.0;
            } else if (itemCode.equals("SHOE002") || itemCode.equals("8901234567891")) {
                string2 = "Casual Loafers Brown";
                string3 = "Shoes";
                d = 3499.0;
            } else if (itemCode.equals("SHOE003") || itemCode.equals("8901234567892")) {
                string2 = "Formal Shoes Black";
                string3 = "Shoes";
                d = 7499.0;
            } else if (itemCode.equals("SHOE004") || itemCode.equals("8901234567893")) {
                string2 = "Casual Sandals Blue";
                string3 = "Sandals";
                d = 1999.0;
            } else if (itemCode.equals("SHOE005") || itemCode.equals("8901234567894")) {
                string2 = "Sports Boots Red";
                string3 = "Shoes";
                d = 4999.0;
            }
        }
        int n3 = -1;
        for (n = 0; n < this.tableModel.getRowCount(); ++n) {
            if (!this.tableModel.getValueAt(n, 0).equals(itemCode)) continue;
            n3 = n;
            break;
        }
        if (n3 >= 0) {
            n = (Integer)this.tableModel.getValueAt(n3, 3);
            this.tableModel.setValueAt(n + n2, n3, 3);
            this.recalculateRow(n3);
        } else {
            double d2;
            double d3 = 10.0;
            double d4 = d2 = (d - d * d3 / 100.0) * (double)n2;
            double d5 = d * 0.6;
            try {
                ItemMaster itemMaster2 = ItemMasterDAO.getItemByBarcode(itemCode);
                if (itemMaster2 == null) {
                    final String finalItemCode = itemCode;
                    itemMaster2 = ItemMasterDAO.getAllItems().stream().filter(itemMaster -> itemMaster.getItemCode().equalsIgnoreCase(finalItemCode)).findFirst().orElse(null);
                }
                if (itemMaster2 != null) {
                    d5 = itemMaster2.getPurchasePrice();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            Vector<Object> row = new Vector<Object>();
            row.add(itemCode);
            row.add(string2);
            row.add(string3);
            row.add(n2);
            row.add(d);
            row.add(d5);
            row.add(d3);
            row.add(d4);
            this.tableModel.addRow(row);
        }
        this.itemCodeField.setText("");
        this.itemNameField.setText("");
        this.itemPriceField.setText("");
        this.qtyField.setText("1");
        this.itemCodeField.requestFocusInWindow();
        this.updateTotals();
    }

    private void recalculateRow(int n) {
        try {
            double d;
            int n2 = Integer.parseInt(this.tableModel.getValueAt(n, 3).toString());
            double d2 = Double.parseDouble(this.tableModel.getValueAt(n, 4).toString());
            double d3 = Double.parseDouble(this.tableModel.getValueAt(n, 6).toString());
            double d4 = d = (d2 - d2 * d3 / 100.0) * (double)n2;
            this.tableModel.setValueAt(d4, n, 7);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void handleRemoveItem() {
        int n = this.billItemsTable.getSelectedRow();
        if (n >= 0) {
            this.tableModel.removeRow(n);
            this.updateTotals();
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select an item line in the table grid to remove.");
        }
    }

    private void handleClearBill() {
        if (UIUtils.showConfirmDialog(this, "Clear Bill", "Are you sure you want to clear all scanned line items?")) {
            this.tableModel.setRowCount(0);
            this.updateTotals();
        }
    }

    private void handleHoldBill() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showWarningDialog(this, "Empty Bill", "There are no items to hold.");
            return;
        }
        String string = "Bill-" + System.currentTimeMillis();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
            Vector<Object> vector = new Vector<Object>();
            for (int j = 0; j < this.tableModel.getColumnCount(); ++j) {
                vector.add(this.tableModel.getValueAt(i, j));
            }
            arrayList.add(vector);
        }
        heldBills.put(string, arrayList);
        this.tableModel.setRowCount(0);
        this.updateTotals();
        UIUtils.showSuccessDialog(this, "Bill Held", "Bill parked successfully. Hold ID: " + string);
    }

    private void handleRecallBill() {
        if (heldBills.isEmpty()) {
            UIUtils.showWarningDialog(this, "No Held Bills", "There are no parked bills to recall.");
            return;
        }
        if (this.tableModel.getRowCount() > 0 && !UIUtils.showConfirmDialog(this, "Current Bill Not Empty", "Loading a held bill will clear the current items. Proceed?")) {
            return;
        }
        Object[] objectArray = heldBills.keySet().toArray(new String[0]);
        String string = (String)JOptionPane.showInputDialog(this, "Select a held bill to resume:", "Recall Bill", 3, null, objectArray, objectArray[0]);
        if (string != null) {
            this.tableModel.setRowCount(0);
            List<Vector<Object>> list = heldBills.remove(string);
            for (Vector<Object> vector : list) {
                this.tableModel.addRow(vector);
            }
            this.updateTotals();
        }
    }

    private void updateTotals() {
        double d = 0.0;
        double d2 = 0.0;
        double d3 = 0.0;
        for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
            try {
                int n = Integer.parseInt(this.tableModel.getValueAt(i, 3).toString());
                double d4 = Double.parseDouble(this.tableModel.getValueAt(i, 4).toString());
                double d5 = Double.parseDouble(this.tableModel.getValueAt(i, 6).toString());
                double d6 = d4 * (double)n;
                double d7 = d6 * d5 / 100.0;
                double d8 = d6 - d7;
                d += d8;
                d2 += d7;
                d3 += d8;
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.currentNetTotal = d3;
        this.subtotalLabel.setText(String.format("\u20b9%.2f", d));
        this.discountLabel.setText(String.format("\u20b9%.2f", d2));
        if (!this.totalField.hasFocus()) {
            this.totalField.setText(String.format("%.2f", d3));
        }
        this.calculateChange();
    }

    private void calculateDiscountFromTotal() {
        if (this.isRecalculatingLumpsum) {
            return;
        }
        try {
            double d = Double.parseDouble(this.totalField.getText().trim());
            double d2 = 0.0;
            for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
                int n = Integer.parseInt(this.tableModel.getValueAt(i, 3).toString());
                double d3 = Double.parseDouble(this.tableModel.getValueAt(i, 4).toString());
                d2 += d3 * (double)n;
            }
            if (d2 > 0.0) {
                double d4 = (d2 - d) / d2 * 100.0;
                this.isRecalculatingLumpsum = true;
                for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
                    this.tableModel.setValueAt(d4, i, 6);
                    this.recalculateRow(i);
                }
                this.isRecalculatingLumpsum = false;
                this.updateTotals();
            }
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
    }

    private void calculateChange() {
        try {
            String string = this.amountPaidField.getText().trim();
            if (string.isEmpty()) {
                this.changeField.setText("");
                return;
            }
            double d = Double.parseDouble(string);
            double d2 = d - this.currentNetTotal;
            if (d2 >= 0.0) {
                this.changeField.setText(String.format("\u20b9%.2f", d2));
                this.changeField.setForeground(UIConstants.SECONDARY_COLOR);
            } else {
                this.changeField.setText(String.format("Shortage: \u20b9%.2f", Math.abs(d2)));
                this.changeField.setForeground(UIConstants.DANGER_COLOR);
            }
        }
        catch (NumberFormatException numberFormatException) {
            this.changeField.setText("");
        }
    }

    private void handleSaveAndPrint() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showErrorDialog(this, "Error", "Cannot print an empty POS Bill.");
            return;
        }
        if (!this.saveCurrentBill()) {
            String errMsg = (this.lastSaveError != null && !this.lastSaveError.isEmpty())
                    ? this.lastSaveError
                    : "Failed to save the bill. Please check logs/app.log for details.";
            UIUtils.showErrorDialog(this, "Save Error", errMsg);
            return;
        }
        this.notifyDashboardRefresh();              // ← refresh Dashboard Today's Sales
        String string = this.customerCombo.getSelectedItem() != null ? this.customerCombo.getSelectedItem().toString() : "";
        String string2 = this.saleOptionCombo.getSelectedItem() != null ? this.saleOptionCombo.getSelectedItem().toString() : "";
        double d = 0.0;
        double d2 = 0.0;
        try {
            d = Double.parseDouble(this.amountPaidField.getText().trim());
            d2 = d - this.currentNetTotal;
            if (d2 < 0.0) {
                d2 = 0.0;
            }
        }
        catch (NumberFormatException numberFormatException) {
            d = this.currentNetTotal;
        }
        String string3 = String.format("=== BAREEZE FOOTWEAR ===\nAddress: Anar complex, Naya bazar,\nMelparamba, Kasaragod, Kerala, India 671317\nMobile no: 8086790086\nMail ID: breezefootwearfancy@gmail.com\n==================================\nInvoice saved successfully.\nCustomer: %s\nSale Option: %s\nNet Amount: \u20b9%.2f\nPayment Mode: %s\nAmount Paid: \u20b9%.2f\nChange/Balance Return: \u20b9%.2f\n\nThermal Printing Queue active.", string, string2, this.currentNetTotal, this.paymentModeCombo.getSelectedItem(), d, d2);
        JPanel jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JTextArea jTextArea = new JTextArea(string3);
        jTextArea.setEditable(false);
        jTextArea.setFont(new Font("Monospaced", 0, 12));
        jTextArea.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.add((Component)jTextArea, "Center");
        try {
            File file = new File("images/qrcode.png");
            if (file.exists()) {
                BufferedImage bufferedImage = ImageIO.read(file);
                if (bufferedImage != null) {
                    Image image = bufferedImage.getScaledInstance(200, 200, 4);
                    JLabel jLabel = new JLabel(new ImageIcon(image));
                    JLabel jLabel2 = new JLabel("Scan to Pay \u20b9" + String.format("%.2f", this.currentNetTotal));
                    jLabel2.setFont(new Font("Segoe UI", 1, 14));
                    jLabel2.setHorizontalAlignment(0);
                    jLabel2.setForeground(UIConstants.PRIMARY_COLOR);
                    JPanel jPanel2 = new JPanel(new BorderLayout());
                    jPanel2.setBackground(UIConstants.APP_BACKGROUND);
                    jPanel2.add((Component)jLabel2, "North");
                    jPanel2.add((Component)jLabel, "Center");
                    jPanel.add((Component)jPanel2, "East");
                }
            } else {
                System.out.println("Warning: qrcode.png not found in images folder.");
            }
        }
        catch (Exception exception) {
            System.out.println("Could not load static QR image: " + exception.getMessage());
        }
        JOptionPane.showMessageDialog(this, jPanel, "POS Invoice Completed", -1);
        this.dispose();
    }

    private void handleSave() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showErrorDialog(this, "Error", "Cannot save an empty POS Bill.");
            return;
        }
        if (this.saveCurrentBill()) {
            String string = this.saleOptionCombo.getSelectedItem().toString();
            this.notifyDashboardRefresh();          // ← refresh Dashboard Today's Sales
            UIUtils.showSuccessDialog(this, "Success", "POS Bill (" + string + ") saved successfully.");
            this.dispose();
        } else {
            String errMsg = (this.lastSaveError != null && !this.lastSaveError.isEmpty())
                    ? this.lastSaveError
                    : "Failed to save the bill. Please check logs/app.log for details.";
            UIUtils.showErrorDialog(this, "Save Error", errMsg);
        }
    }

    private static final java.util.logging.Logger SAVE_LOGGER =
            java.util.logging.Logger.getLogger("ui.frames.POSSaleFrame");

    /**
     * Saves the current bill plus all its line items atomically inside a single
     * database transaction.  Stock is decremented per item inside the same txn.
     * Returns the generated billId on success, or -1 on any failure.
     */
    private int saveCurrentBillAtomic() {
        if (this.tableModel.getRowCount() == 0) return -1;

        // ── Collect bill header fields ────────────────────────────────────────
        String billNumber = BarcodeGenerator.generateInvoiceNumber(
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        String billType = (this.saleOptionCombo.getSelectedItem() != null)
                ? this.saleOptionCombo.getSelectedItem().toString() : "SALES";
        String paymentMode = (this.paymentModeCombo.getSelectedItem() != null)
                ? this.paymentModeCombo.getSelectedItem().toString() : "CASH";

        double paidAmount = this.currentNetTotal;
        try { paidAmount = Double.parseDouble(this.amountPaidField.getText().trim()); }
        catch (NumberFormatException ignored) {}

        int userId = 1;
        if (parent instanceof MainFrame) {
            try { userId = ((MainFrame) parent).getCurrentUser().getUserId(); }
            catch (Exception ex) {
                SAVE_LOGGER.log(java.util.logging.Level.WARNING,
                        "Could not get current user ID from MainFrame; defaulting to 1", ex);
            }
        }

        // ── Resolve customer ID from the combo selection ──────────────────────
        int customerId = 0;
        String custText = (this.customerCombo.getSelectedItem() != null)
                ? this.customerCombo.getSelectedItem().toString() : "";
        if (custText.contains("(") && custText.contains(")")) {
            String phone = custText.substring(custText.lastIndexOf('(') + 1, custText.lastIndexOf(')')).trim();
            try {
                database.Customer customer = database.CustomerDAO.getCustomerByPhone(phone);
                if (customer != null) customerId = customer.getCustomerId();
            } catch (Exception ex) {
                SAVE_LOGGER.log(java.util.logging.Level.WARNING,
                        "Could not resolve customer from phone '" + phone + "'", ex);
            }
        }

        // ── Resolve item IDs for all rows up-front (avoids partial failures) ──
        Object[][] rows = new Object[this.tableModel.getRowCount()][];
        for (int i = 0; i < this.tableModel.getRowCount(); i++) {
            String code = this.tableModel.getValueAt(i, 0).toString();
            int qty     = Integer.parseInt(this.tableModel.getValueAt(i, 3).toString());
            double price = Double.parseDouble(this.tableModel.getValueAt(i, 4).toString());
            double lineTotal = Double.parseDouble(this.tableModel.getValueAt(i, 7).toString());
            double discount  = price * qty - lineTotal;

            database.ItemMaster item = database.ItemMasterDAO.getItemByBarcode(code);
            if (item == null) {
                final String fc = code;
                item = database.ItemMasterDAO.getAllItems().stream()
                        .filter(m -> m.getItemCode().equalsIgnoreCase(fc))
                        .findFirst().orElse(null);
            }
            int itemId = (item != null) ? item.getItemId() : -1;
            rows[i] = new Object[]{code, qty, price, lineTotal, discount, itemId, item};
        }

        // ── Execute entire save inside ONE connection / ONE transaction ────────
        String insertBillSql =
            "INSERT INTO bill (billNumber, billType, billDate, customerId, supplierId, userId, " +
            "subtotal, totalDiscount, totalAmount, paidAmount, paymentMode, status, remarks) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql =
            "INSERT INTO bill_item (billId, itemId, quantity, unitPrice, discount, totalAmount) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        String updateStockSql =
            "UPDATE item_master SET stockQuantity = stockQuantity - ? WHERE itemId = ?";

        java.sql.Connection conn = null;
        try {
            conn = database.DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Insert bill header
            int billId;
            double subtotal = 0, totalDiscount = 0;
            for (Object[] r : rows) { subtotal += (int)r[1] * (double)r[2]; totalDiscount += (double)r[4]; }
            String status = (paidAmount >= this.currentNetTotal) ? "COMPLETED" : "PENDING";

            try (java.sql.PreparedStatement ps = conn.prepareStatement(insertBillSql,
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, billNumber);
                ps.setString(2, billType);
                ps.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
                if (customerId > 0) ps.setInt(4, customerId); else ps.setNull(4, java.sql.Types.INTEGER);
                ps.setNull(5, java.sql.Types.INTEGER); // supplierId not used in POS
                ps.setInt(6, userId);
                ps.setDouble(7, subtotal);
                ps.setDouble(8, totalDiscount);
                ps.setDouble(9, this.currentNetTotal);
                ps.setDouble(10, paidAmount);
                ps.setString(11, paymentMode);
                ps.setString(12, status);
                ps.setNull(13, java.sql.Types.VARCHAR); // remarks
                ps.executeUpdate();
                try (java.sql.ResultSet gk = ps.getGeneratedKeys()) {
                    if (!gk.next()) throw new java.sql.SQLException("Bill INSERT returned no generated key.");
                    billId = gk.getInt(1);
                }
            }

            // Insert bill items + decrement stock
            try (java.sql.PreparedStatement psItem = conn.prepareStatement(insertItemSql);
                 java.sql.PreparedStatement psStock = conn.prepareStatement(updateStockSql)) {
                for (Object[] r : rows) {
                    int qty   = (int)    r[1];
                    double price = (double) r[2];
                    double lineT = (double) r[3];
                    double disc  = (double) r[4];
                    int itemId   = (int)    r[5];

                    if (itemId <= 0) {
                        // Unknown item — use a placeholder itemId=1 (already checked at top)
                        SAVE_LOGGER.warning("Item code '" + r[0] + "' not found in DB; using itemId=1 for bill_item.");
                        itemId = 1;
                    }

                    psItem.setInt(1, billId);
                    psItem.setInt(2, itemId);
                    psItem.setInt(3, qty);
                    psItem.setDouble(4, price);
                    psItem.setDouble(5, disc);
                    psItem.setDouble(6, lineT);
                    psItem.addBatch();

                    psStock.setInt(1, qty);
                    psStock.setInt(2, itemId);
                    psStock.addBatch();
                }
                psItem.executeBatch();
                psStock.executeBatch();
            }

            conn.commit();
            SAVE_LOGGER.info("Bill " + billNumber + " (ID=" + billId + ") saved successfully.");
            return billId;

        } catch (java.sql.SQLException e) {
            // Log the full technical detail to file
            SAVE_LOGGER.log(java.util.logging.Level.SEVERE,
                    "saveCurrentBillAtomic() failed — SQLState=" + e.getSQLState()
                    + " ErrorCode=" + e.getErrorCode() + " — " + e.getMessage(), e);
            // Roll back partially-written data
            if (conn != null) try { conn.rollback(); } catch (java.sql.SQLException re) {
                SAVE_LOGGER.log(java.util.logging.Level.SEVERE, "Rollback failed: " + re.getMessage(), re);
            }
            // Surface a friendly but specific message to the caller
            this.lastSaveError = buildFriendlyError(e);
            return -1;
        } catch (Exception e) {
            SAVE_LOGGER.log(java.util.logging.Level.SEVERE, "Unexpected error saving bill: " + e.getMessage(), e);
            if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
            this.lastSaveError = "An unexpected error occurred while saving. Please try again or contact support.";
            return -1;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    /** Last human-friendly error from saveCurrentBillAtomic(), used by callers to show a specific dialog. */
    private String lastSaveError = "";

    /** Maps a SQLException to a user-friendly, non-technical description. */
    private String buildFriendlyError(java.sql.SQLException e) {
        String msg = (e.getMessage() != null) ? e.getMessage().toLowerCase() : "";
        String state = (e.getSQLState() != null) ? e.getSQLState() : "";
        if (state.startsWith("08") || msg.contains("connection")) {
            return "Cannot connect to the database. Please check your connection and try again.";
        }
        if (state.equals("23000") || msg.contains("foreign key") || msg.contains("constraint")) {
            return "A required reference (customer or item) does not exist in the database. Please verify and retry.";
        }
        if (state.equals("23000") && msg.contains("duplicate")) {
            return "A bill with this number already exists. Please try again (a new number will be generated).";
        }
        if (msg.contains("null") || msg.contains("not null") || msg.contains("cannot be null")) {
            return "A required field is missing (e.g. user or payment mode). Please fill all fields and retry.";
        }
        return "Failed to save the bill due to a database error. Full details have been written to logs/app.log.";
    }

    private boolean saveCurrentBill() {
        int billId = saveCurrentBillAtomic();
        return billId > 0;
    }

    /**
     * Finds any open {@link DashboardFrame} in the parent desktop pane and
     * triggers a live data refresh on it.  Called after every successful sale.
     */
    private void notifyDashboardRefresh() {
        if (!(this.parent instanceof MainFrame)) return;
        javax.swing.JDesktopPane desktop = ((MainFrame) this.parent).getDesktopPane();
        if (desktop == null) return;
        for (javax.swing.JInternalFrame frame : desktop.getAllFrames()) {
            if (frame instanceof DashboardFrame) {
                ((DashboardFrame) frame).refreshStats();
                break;
            }
        }
    }

    private void setupKeyboardShortcuts() {
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(113, 0), "clearAction");
        this.getRootPane().getActionMap().put("clearAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                POSSaleFrame.this.handleClearBill();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(116, 0), "saveAction");
        this.getRootPane().getActionMap().put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                POSSaleFrame.this.handleSave();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(120, 0), "printAction");
        this.getRootPane().getActionMap().put("printAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                POSSaleFrame.this.handleSaveAndPrint();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(27, 0), "escapeAction");
        this.getRootPane().getActionMap().put("escapeAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                POSSaleFrame.this.dispose();
            }
        });
    }

    private void refreshCustomerCombo() {
        if (this.customerCombo == null) {
            return;
        }
        this.customerCombo.removeAllItems();
        this.customerCombo.addItem("Walk-in Customer");
        try {
            List<Customer> list = CustomerDAO.getAllCustomers();
            for (Customer customer : list) {
                this.customerCombo.addItem(customer.getCustomerName() + " (" + customer.getPhone() + ")");
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void handleCustomerSelection() {
        if (this.customerCombo.getSelectedItem() == null) {
            return;
        }
        int n = this.customerCombo.getSelectedIndex();
        if (n <= 0) {
            this.custInfoLabel.setText("Walk-in Customer (No details)");
            this.custInfoLabel.setForeground(Color.GRAY);
            return;
        }
        String string = this.customerCombo.getSelectedItem().toString();
        String string2 = "";
        int n2 = string.lastIndexOf(40);
        int n3 = string.lastIndexOf(41);
        if (n2 >= 0 && n3 > n2) {
            string2 = string.substring(n2 + 1, n3).trim();
        }
        Customer customer = null;
        try {
            for (Customer customer2 : CustomerDAO.getAllCustomers()) {
                if (!customer2.getPhone().equals(string2)) continue;
                customer = customer2;
                break;
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        if (customer != null) {
            this.custInfoLabel.setText(String.format("Phone: %s  |  Credit Limit: \u20b9%.2f  |  Outstanding: \u20b9%.2f  |  Loyalty Points: %.0f", customer.getPhone(), customer.getCreditLimit(), customer.getOutstandingAmount(), customer.getLoyaltyPoints()));
            this.custInfoLabel.setForeground(UIConstants.SECONDARY_COLOR);
        } else {
            this.custInfoLabel.setText("Customer details not found");
            this.custInfoLabel.setForeground(UIConstants.DANGER_COLOR);
        }
    }

    private void handleQuickAddCustomer() {
        String string = JOptionPane.showInputDialog(this, "Enter Customer Name:", "Quick Add Customer", 3);
        if (string == null || string.trim().isEmpty()) {
            return;
        }
        String string2 = JOptionPane.showInputDialog(this, "Enter Customer Phone/Mobile:", "Quick Add Customer", 3);
        if (string2 == null || string2.trim().isEmpty()) {
            return;
        }
        Customer customer = new Customer();
        customer.setCustomerName(string.trim());
        customer.setPhone(string2.trim());
        customer.setCreditLimit(10000.0);
        customer.setOutstandingAmount(0.0);
        customer.setLoyaltyPoints(0.0);
        customer.setCustomerType("Retail");
        try {
            int n = CustomerDAO.addCustomer(customer);
            if (n > 0) {
                UIUtils.showSuccessDialog(this, "Customer Added", "Customer " + string + " added successfully!");
                this.refreshCustomerCombo();
                this.customerCombo.setSelectedItem(string.trim() + " (" + string2.trim() + ")");
            } else {
                UIUtils.showErrorDialog(this, "Error", "Failed to add customer.");
            }
        }
        catch (Exception exception) {
            UIUtils.showErrorDialog(this, "Error", "Database error: " + exception.getMessage());
        }
    }

    private void handleCustomerSearch() {
        String string = this.customerSearchField.getText().trim();
        if (string.isEmpty()) {
            return;
        }
        ArrayList<Customer> arrayList = new ArrayList<Customer>();
        try {
            for (Customer object : CustomerDAO.getAllCustomers()) {
                if (!object.getCustomerName().toLowerCase().contains(string.toLowerCase()) && (object.getPhone() == null || !object.getPhone().contains(string)) && (object.getCustomerCode() == null || !object.getCustomerCode().toLowerCase().contains(string.toLowerCase()))) continue;
                arrayList.add(object);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        if (arrayList.isEmpty()) {
            int n = JOptionPane.showConfirmDialog(this, "No customer found matching '" + string + "'.\nDo you want to add a new customer with this name?", "Customer Not Found", 0);
            if (n == 0) {
                this.handleQuickAddCustomerWithName(string);
            }
        } else if (arrayList.size() == 1) {
            Customer c = arrayList.get(0);
            String i = c.getCustomerName() + " (" + c.getPhone() + ")";
            boolean n = false;
            for (int customer = 0; customer < this.customerCombo.getItemCount(); ++customer) {
                if (!this.customerCombo.getItemAt(customer).equals(i)) continue;
                this.customerCombo.setSelectedIndex(customer);
                n = true;
                break;
            }
            if (!n) {
                this.refreshCustomerCombo();
                for (int customer = 0; customer < this.customerCombo.getItemCount(); ++customer) {
                    if (!this.customerCombo.getItemAt(customer).equals(i)) continue;
                    this.customerCombo.setSelectedIndex(customer);
                    break;
                }
            }
            this.customerSearchField.setText("");
            this.itemCodeField.requestFocusInWindow();
        } else {
            String[] options = new String[arrayList.size()];
            for (int i = 0; i < arrayList.size(); ++i) {
                Customer customer = arrayList.get(i);
                options[i] = customer.getCustomerName() + " (" + customer.getPhone() + ") - Bal: \u20b9" + customer.getOutstandingAmount();
            }
            String string3 = (String)JOptionPane.showInputDialog(this, "Multiple matches found. Select customer:", "Select Customer", -1, null, (Object[])options, options[0]);
            if (string3 != null) {
                int n = -1;
                for (int i = 0; i < options.length; ++i) {
                    if (!options[i].equals(string3)) continue;
                    n = i;
                    break;
                }
                if (n >= 0) {
                    Customer c = arrayList.get(n);
                    String iStr = c.getCustomerName() + " (" + c.getPhone() + ")";
                    boolean bl = false;
                    for (int j = 0; j < this.customerCombo.getItemCount(); ++j) {
                        if (!this.customerCombo.getItemAt(j).equals(iStr)) continue;
                        this.customerCombo.setSelectedIndex(j);
                        bl = true;
                        break;
                    }
                    if (!bl) {
                        this.refreshCustomerCombo();
                        for (int j = 0; j < this.customerCombo.getItemCount(); ++j) {
                            if (!this.customerCombo.getItemAt(j).equals(iStr)) continue;
                            this.customerCombo.setSelectedIndex(j);
                            break;
                        }
                    }
                    this.customerSearchField.setText("");
                    this.itemCodeField.requestFocusInWindow();
                }
            }
        }
    }

    private void handleQuickAddCustomerWithName(String string) {
        String string2 = JOptionPane.showInputDialog(this, "Enter Customer Phone/Mobile for " + string + ":", "Quick Add Customer", 3);
        if (string2 == null || string2.trim().isEmpty()) {
            return;
        }
        Customer customer = new Customer();
        customer.setCustomerName(string.trim());
        customer.setPhone(string2.trim());
        customer.setCreditLimit(10000.0);
        customer.setOutstandingAmount(0.0);
        customer.setLoyaltyPoints(0.0);
        customer.setCustomerType("Retail");
        try {
            int n = CustomerDAO.addCustomer(customer);
            if (n > 0) {
                UIUtils.showSuccessDialog(this, "Customer Added", "Customer " + string + " added successfully!");
                this.refreshCustomerCombo();
                this.customerCombo.setSelectedItem(string.trim() + " (" + string2.trim() + ")");
                this.customerSearchField.setText("");
                this.itemCodeField.requestFocusInWindow();
            } else {
                UIUtils.showErrorDialog(this, "Error", "Failed to add customer.");
            }
        }
        catch (Exception exception) {
            UIUtils.showErrorDialog(this, "Error", "Database error: " + exception.getMessage());
        }
    }
}
