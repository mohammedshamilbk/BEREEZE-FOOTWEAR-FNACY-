/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class BarcodePrintFrame
extends JInternalFrame {
    private JFrame parent;
    private JTextField barcodeField;
    private JComboBox<String> voucherTypeCombo;
    private JTextField invoiceNoField;
    private JComboBox<String> printSchemeCombo;
    private JComboBox<String> printerCombo;
    private JTable detailsTable;
    private DefaultTableModel tableModel;

    public BarcodePrintFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Barcode Print Utility");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1000, 680);
        this.setLocation(100, 50);
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel jPanel2 = this.createHeaderPanel();
        jPanel.add((Component)jPanel2, "North");
        JPanel jPanel3 = new JPanel(new BorderLayout());
        jPanel3.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel4 = this.createParamsPanel();
        JPanel jPanel5 = this.createGridPanel();
        jPanel3.add((Component)jPanel4, "North");
        jPanel3.add((Component)jPanel5, "Center");
        jPanel.add((Component)jPanel3, "Center");
        this.add(jPanel);
        this.setupKeyboardShortcuts();
    }

    private JPanel createHeaderPanel() {
        JPanel jPanel = new JPanel(new FlowLayout(0, 15, 10));
        jPanel.setBackground(UIConstants.PRIMARY_COLOR);
        JLabel jLabel = new JLabel("Tools -> Barcode Point ");
        jLabel.setFont(UIConstants.TITLE_FONT);
        jLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jPanel.add(jLabel);
        JButton jButton = UIUtils.createSuccessButton("Save (F5)", actionEvent -> this.handleSave());
        JButton jButton2 = UIUtils.createButton("Print (Ctrl+P)", actionEvent -> this.handlePrint());
        jButton2.setBackground(UIConstants.ACCENT_COLOR);
        jButton2.setForeground(UIConstants.DARK_COLOR);
        JButton jButton3 = UIUtils.createButton("View Label", actionEvent -> this.handleViewLabel());
        jButton3.setBackground(Color.DARK_GRAY);
        jPanel.add(jButton);
        jPanel.add(jButton2);
        jPanel.add(jButton3);
        return jPanel;
    }

    private JPanel createParamsPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel2 = new JPanel(new FlowLayout(0, 10, 5));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Scan Parameters"));
        jPanel2.add(UIUtils.createLabel("Barcode:"));
        this.barcodeField = UIUtils.createTextField(10);
        this.barcodeField.addActionListener(actionEvent -> this.handleBarcodeScan());
        jPanel2.add(this.barcodeField);
        JButton jButton = UIUtils.createDangerButton("Clear All", actionEvent -> this.handleClearAll());
        jButton.setPreferredSize(new Dimension(90, 30));
        jPanel2.add(jButton);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel.add((Component)jPanel2, gridBagConstraints);
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 10, 5));
        jPanel3.setBackground(UIConstants.APP_BACKGROUND);
        jPanel3.setBorder(BorderFactory.createTitledBorder("Load from Bill"));
        jPanel3.add(UIUtils.createLabel("Voucher Type:"));
        this.voucherTypeCombo = UIUtils.createComboBox(new String[]{"Purchase", "Sales", "Manual"});
        this.voucherTypeCombo.setPreferredSize(new Dimension(100, 30));
        jPanel3.add(this.voucherTypeCombo);
        jPanel3.add(UIUtils.createLabel("Inv No:"));
        this.invoiceNoField = UIUtils.createTextField(8);
        jPanel3.add(this.invoiceNoField);
        JButton jButton2 = UIUtils.createButton("Load", actionEvent -> this.handleLoadVoucher());
        jButton2.setPreferredSize(new Dimension(75, 30));
        jPanel3.add(jButton2);
        gridBagConstraints.gridx = 1;
        jPanel.add((Component)jPanel3, gridBagConstraints);
        JPanel jPanel4 = new JPanel(new FlowLayout(0, 10, 5));
        jPanel4.setBackground(UIConstants.APP_BACKGROUND);
        jPanel4.setBorder(BorderFactory.createTitledBorder("Printer Settings"));
        jPanel4.add(UIUtils.createLabel("Scheme:"));
        this.printSchemeCombo = UIUtils.createComboBox(new String[]{"barcode2", "barcode3", "Standard A4", "Thermal 2-Column"});
        this.printSchemeCombo.setPreferredSize(new Dimension(100, 30));
        jPanel4.add(this.printSchemeCombo);
        jPanel4.add(UIUtils.createLabel("Printer:"));
        this.printerCombo = UIUtils.createComboBox(new String[]{"Select the Printer", "TVS LP 46 Neo", "TSC TE244", "Microsoft Print to PDF"});
        this.printerCombo.setPreferredSize(new Dimension(140, 30));
        jPanel4.add(this.printerCombo);
        gridBagConstraints.gridx = 2;
        jPanel.add((Component)jPanel4, gridBagConstraints);
        return jPanel;
    }

    private JPanel createGridPanel() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), "Barcode Details Grid", 1, 2, UIConstants.HEADING_FONT, UIConstants.PRIMARY_COLOR));
        this.tableModel = new DefaultTableModel();
        this.tableModel.addColumn("Sl.No");
        this.tableModel.addColumn("Barcode");
        this.tableModel.addColumn("Item Code");
        this.tableModel.addColumn("Item Name");
        this.tableModel.addColumn("Size");
        this.tableModel.addColumn("Qty to Print");
        this.tableModel.addColumn("Cost Price");
        this.tableModel.addColumn("MRP");
        this.detailsTable = new JTable(this.tableModel);
        this.detailsTable.setFont(UIConstants.NORMAL_FONT);
        this.detailsTable.setRowHeight(25);
        this.tableModel.addRow(new Object[]{"1", "8901234567890", "SHOE001", "Running Shoes Black", "10", "15", "2000.00", "5999.00"});
        this.tableModel.addRow(new Object[]{"2", "8901234567891", "SHOE002", "Casual Loafers Brown", "9", "5", "1200.00", "3499.00"});
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(this.detailsTable);
        jPanel.add((Component)jScrollPane, "Center");
        return jPanel;
    }

    private void handleBarcodeScan() {
        String string = this.barcodeField.getText().trim();
        if (!string.isEmpty()) {
            int n = this.tableModel.getRowCount() + 1;
            this.tableModel.addRow(new Object[]{String.valueOf(n), string, "CODE-" + (100 + n), "Scanned Footwear Item #" + n, "8", "1", "500.00", "1299.00"});
            this.barcodeField.setText("");
        }
    }

    private void handleClearAll() {
        if (UIUtils.showConfirmDialog(this, "Clear All", "Are you sure you want to clear the entire barcode printing grid?")) {
            this.tableModel.setRowCount(0);
        }
    }

    private void handleLoadVoucher() {
        String string = this.voucherTypeCombo.getSelectedItem().toString();
        String string2 = this.invoiceNoField.getText().trim();
        if (string2.isEmpty()) {
            UIUtils.showWarningDialog(this, "Load Bill", "Please specify an Invoice Number to load.");
            return;
        }
        this.tableModel.setRowCount(0);
        this.tableModel.addRow(new Object[]{"1", "8901002003001", "MOCK001", "Fancy Chappal Soft", "6", "12", "150.00", "499.00"});
        this.tableModel.addRow(new Object[]{"2", "8901002003002", "MOCK002", "Premium Heels Gold", "7", "6", "400.00", "1299.00"});
        UIUtils.showSuccessDialog(this, "Voucher Loaded", "Loaded 2 items from " + string + " Bill: " + string2);
    }

    private void handleViewLabel() {
        int n = this.detailsTable.getSelectedRow();
        if (n >= 0) {
            String string = this.tableModel.getValueAt(n, 3).toString();
            String string2 = this.tableModel.getValueAt(n, 1).toString();
            String string3 = this.tableModel.getValueAt(n, 7).toString();
            String string4 = this.tableModel.getValueAt(n, 4).toString();
            String string5 = String.format("=== BARCODE LABEL PREVIEW ===\n\n  Store: Bareeze Fancy Footwear\n  Item:  %s\n  Size:  %s\n  MRP:   \u00e2\u201a\u00b9 %s\n  Barcode:  ||||||||||||||| %s\n\n  Scheme: %s", string, string4, string3, string2, this.printSchemeCombo.getSelectedItem());
            JOptionPane.showMessageDialog(this, string5, "Barcode Label Preview", 1);
        } else {
            UIUtils.showWarningDialog(this, "Select Item", "Please select an item from the details table to preview the label.");
        }
    }

    private void handleSave() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showErrorDialog(this, "Save Error", "Grid is empty. Nothing to save.");
            return;
        }
        UIUtils.showSuccessDialog(this, "Print Queue Saved", "Barcode print details queued successfully.");
        this.dispose();
    }

    private void handlePrint() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showErrorDialog(this, "Print Error", "Grid is empty. Nothing to print.");
            return;
        }
        String string = this.printerCombo.getSelectedItem().toString();
        if (string.equals("Select the Printer")) {
            UIUtils.showWarningDialog(this, "Select Printer", "Please select a barcode printer from the settings panel.");
            return;
        }
        UIUtils.showSuccessDialog(this, "Printing", "Sent " + this.tableModel.getRowCount() + " label batches to printer: " + string);
        this.dispose();
    }

    private void setupKeyboardShortcuts() {
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(116, 0), "saveAction");
        this.getRootPane().getActionMap().put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                BarcodePrintFrame.this.handleSave();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(80, 128), "printAction");
        this.getRootPane().getActionMap().put("printAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                BarcodePrintFrame.this.handlePrint();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(27, 0), "escapeAction");
        this.getRootPane().getActionMap().put("escapeAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                BarcodePrintFrame.this.dispose();
            }
        });
    }
}
