/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.Supplier;
import database.SupplierDAO;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import ui.frames.UIUtils;

public class SupplierDialog
extends JDialog {
    private Supplier supplier;
    private boolean isSaved = false;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField stateField;
    private JTextField taxRegnField;
    private JTextField gstinField;

    public SupplierDialog(Frame frame, Supplier supplier) {
        super(frame, supplier == null ? "Add New Supplier" : "Edit Supplier", true);
        this.supplier = supplier;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setSize(400, 350);
        this.setLocationRelativeTo(this.getOwner());
        JPanel jPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        jPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.add(UIUtils.createLabel("Supplier Name*:"));
        this.nameField = UIUtils.createTextField(15);
        jPanel.add(this.nameField);
        jPanel.add(UIUtils.createLabel("Phone*:"));
        this.phoneField = UIUtils.createTextField(15);
        jPanel.add(this.phoneField);
        jPanel.add(UIUtils.createLabel("Email:"));
        this.emailField = UIUtils.createTextField(15);
        jPanel.add(this.emailField);
        jPanel.add(UIUtils.createLabel("State:"));
        this.stateField = UIUtils.createTextField(15);
        jPanel.add(this.stateField);
        jPanel.add(UIUtils.createLabel("Tax Regn:"));
        this.taxRegnField = UIUtils.createTextField(15);
        jPanel.add(this.taxRegnField);
        jPanel.add(UIUtils.createLabel("GSTIN:"));
        this.gstinField = UIUtils.createTextField(15);
        jPanel.add(this.gstinField);
        if (this.supplier != null) {
            this.nameField.setText(this.supplier.getSupplierName());
            this.phoneField.setText(this.supplier.getPhone());
            this.emailField.setText(this.supplier.getEmail() == null ? "" : this.supplier.getEmail());
            this.stateField.setText(this.supplier.getState() == null ? "" : this.supplier.getState());
            this.taxRegnField.setText(this.supplier.getTaxRegn() == null ? "" : this.supplier.getTaxRegn());
            this.gstinField.setText(this.supplier.getGstin() == null ? "" : this.supplier.getGstin());
        }
        JButton jButton = UIUtils.createSuccessButton("Save", actionEvent -> this.handleSave());
        JButton jButton2 = UIUtils.createButton("Cancel", actionEvent -> this.dispose());
        jPanel.add(jButton);
        jPanel.add(jButton2);
        this.add(jPanel);
    }

    private void handleSave() {
        String string = this.nameField.getText().trim();
        String string2 = this.phoneField.getText().trim();
        if (string.isEmpty() || string2.isEmpty()) {
            UIUtils.showErrorDialog(this, "Validation Error", "Supplier Name and Phone are required.");
            return;
        }
        if (this.supplier == null) {
            this.supplier = new Supplier();
            this.supplier.setOutstandingBalance(0.0);
        }
        this.supplier.setSupplierName(string);
        this.supplier.setPhone(string2);
        this.supplier.setEmail(this.emailField.getText().trim());
        this.supplier.setState(this.stateField.getText().trim());
        this.supplier.setTaxRegn(this.taxRegnField.getText().trim());
        this.supplier.setGstin(this.gstinField.getText().trim());
        if (this.supplier.getSupplierId() <= 0) {
            SupplierDAO.addSupplier(this.supplier);
        } else {
            SupplierDAO.updateSupplier(this.supplier);
        }
        this.isSaved = true;
        this.dispose();
    }

    public boolean isSaved() {
        return this.isSaved;
    }

    public Supplier getSupplier() {
        return this.supplier;
    }
}
