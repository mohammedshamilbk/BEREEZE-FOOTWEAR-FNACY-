/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.Customer;
import database.CustomerDAO;
import database.AuditLogDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class CustomerFrame
extends JInternalFrame {
    private JFrame parent;
    private JTextField searchField;
    private JTable customersTable;
    private DefaultTableModel tableModel;

    public CustomerFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Customers");
        this.setClosable(true);
        this.setSize(1000, 600);
        this.setLocation(100, 100);
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel jLabel = UIUtils.createTitleLabel("Customer Management");
        jPanel.add((Component)jLabel, "North");
        JPanel jPanel2 = this.createSearchPanel();
        jPanel.add((Component)jPanel2, "First");
        JPanel jPanel3 = this.createTablePanel();
        jPanel.add((Component)jPanel3, "Center");
        JPanel jPanel4 = this.createActionPanel();
        jPanel.add((Component)jPanel4, "South");
        this.add(jPanel);
    }

    private JPanel createSearchPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new FlowLayout(0, 15, 10));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Customer Search Parameters"));
        jPanel2.add(UIUtils.createLabel("Search by Name/Phone:"));
        this.searchField = UIUtils.createTextField(20);
        this.searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                CustomerFrame.this.handleSearch();
            }
        });
        jPanel2.add(this.searchField);
        JButton jButton = UIUtils.createDangerButton("Clear Fields", actionEvent -> {
            this.searchField.setText("");
            this.loadCustomersTable();
        });
        jPanel2.add(jButton);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel.add((Component)jPanel2, gridBagConstraints);
        return jPanel;
    }

    private JPanel createTablePanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), "Customer Details Grid", 1, 2, UIConstants.HEADING_FONT, UIConstants.PRIMARY_COLOR));
        this.tableModel = new DefaultTableModel();
        this.tableModel.addColumn("Customer Code");
        this.tableModel.addColumn("Name");
        this.tableModel.addColumn("Phone");
        this.tableModel.addColumn("Email");
        this.tableModel.addColumn("Type");
        this.tableModel.addColumn("Outstanding");
        this.tableModel.addColumn("Credit Limit");
        this.tableModel.addColumn("Loyalty Points");
        this.customersTable = new JTable(this.tableModel);
        this.customersTable.setFont(UIConstants.NORMAL_FONT);
        this.customersTable.setRowHeight(25);
        this.loadCustomersTable();
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(this.customersTable);
        jPanel.add((Component)jScrollPane, "Center");
        return jPanel;
    }

    private void loadCustomersTable() {
        this.tableModel.setRowCount(0);
        try {
            List<Customer> list = CustomerDAO.getAllCustomers();
            for (Customer customer : list) {
                Vector<Object> vector = new Vector<Object>();
                vector.add(customer.getCustomerCode());
                vector.add(customer.getCustomerName());
                vector.add(customer.getPhone());
                vector.add(customer.getEmail());
                vector.add(customer.getCustomerType());
                vector.add("\u20b9" + String.format("%.2f", customer.getOutstandingAmount()));
                vector.add("\u20b9" + String.format("%.2f", customer.getCreditLimit()));
                vector.add(String.format("%.0f", customer.getLoyaltyPoints()));
                this.tableModel.addRow(vector);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private JPanel createActionPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout(1, 10, 10));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        JButton jButton = UIUtils.createSuccessButton("Add Customer", actionEvent -> this.handleAddCustomer());
        JButton jButton2 = UIUtils.createButton("Edit Customer", actionEvent -> this.handleEditCustomer());
        JButton jButton3 = UIUtils.createDangerButton("Delete Customer", actionEvent -> this.handleDeleteCustomer());
        JButton jButton4 = UIUtils.createButton("View Details", actionEvent -> this.handleViewDetails());
        JButton jButton5 = UIUtils.createButton("Set Credit Limit", actionEvent -> this.handleSetCreditLimit());
        jPanel.add(jButton);
        jPanel.add(jButton2);
        jPanel.add(jButton3);
        jPanel.add(jButton4);
        jPanel.add(jButton5);
        return jPanel;
    }

    private void handleSearch() {
        String string = this.searchField.getText().trim().toLowerCase();
        if (string.isEmpty()) {
            this.loadCustomersTable();
            return;
        }
        this.tableModel.setRowCount(0);
        try {
            for (Customer customer : CustomerDAO.getAllCustomers()) {
                if (!customer.getCustomerName().toLowerCase().contains(string) && !customer.getPhone().contains(string) && (customer.getCustomerCode() == null || !customer.getCustomerCode().toLowerCase().contains(string))) continue;
                Vector<Object> vector = new Vector<Object>();
                vector.add(customer.getCustomerCode());
                vector.add(customer.getCustomerName());
                vector.add(customer.getPhone());
                vector.add(customer.getEmail());
                vector.add(customer.getCustomerType());
                vector.add("\u20b9" + String.format("%.2f", customer.getOutstandingAmount()));
                vector.add("\u20b9" + String.format("%.2f", customer.getCreditLimit()));
                vector.add(String.format("%.0f", customer.getLoyaltyPoints()));
                this.tableModel.addRow(vector);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void handleAddCustomer() {
        String string = JOptionPane.showInputDialog(this, (Object)"Enter Customer Name:");
        if (string == null || string.trim().isEmpty()) {
            return;
        }
        String string2 = JOptionPane.showInputDialog(this, (Object)"Enter Customer Phone/Mobile:");
        if (string2 == null || string2.trim().isEmpty()) {
            return;
        }
        String string3 = JOptionPane.showInputDialog(this, (Object)"Enter Customer Email (Optional):");
        Customer customer = new Customer();
        customer.setCustomerName(string.trim());
        customer.setPhone(string2.trim());
        customer.setEmail(string3 == null ? "" : string3.trim());
        customer.setCreditLimit(10000.0);
        customer.setOutstandingAmount(0.0);
        customer.setLoyaltyPoints(0.0);
        customer.setCustomerType("Retail");
        try {
            int n = CustomerDAO.addCustomer(customer);
            if (n > 0) {
                UIUtils.showSuccessDialog(this, "Success", "Customer added successfully!");
                this.loadCustomersTable();
            }
        }
        catch (Exception exception) {
            UIUtils.showErrorDialog(this, "Error", "Failed to add customer: " + exception.getMessage());
        }
    }

    private void handleEditCustomer() {
        int n = this.customersTable.getSelectedRow();
        if (n >= 0) {
            String string = (String)this.tableModel.getValueAt(n, 0);
            Customer customer = null;
            for (Customer customer2 : CustomerDAO.getAllCustomers()) {
                if (customer2.getCustomerCode().equals(string)) {
                    customer = customer2;
                    break;
                }
            }
            String newName;
            if (customer != null && (newName = JOptionPane.showInputDialog(this, "Edit Customer Name:", customer.getCustomerName())) != null && !newName.trim().isEmpty()) {
                customer.setCustomerName(newName.trim());
                CustomerDAO.updateCustomer(customer);
                UIUtils.showSuccessDialog(this, "Success", "Customer updated successfully!");
                this.loadCustomersTable();
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a customer to edit");
        }
    }

    private void handleDeleteCustomer() {
        int n = this.customersTable.getSelectedRow();
        if (n >= 0) {
            String string = (String)this.tableModel.getValueAt(n, 0);
            Customer customer = null;
            for (Customer customer2 : CustomerDAO.getAllCustomers()) {
                if (!customer2.getCustomerCode().equals(string)) continue;
                customer = customer2;
                break;
            }
            if (customer != null && UIUtils.showConfirmDialog(this, "Confirm", "Delete customer " + customer.getCustomerName() + "?")) {
                CustomerDAO.deleteCustomer(customer.getCustomerId());
                UIUtils.showSuccessDialog(this, "Success", "Customer deleted successfully");
                this.loadCustomersTable();
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a customer to delete");
        }
    }

    private void handleViewDetails() {
        int n = this.customersTable.getSelectedRow();
        if (n >= 0) {
            String string = (String)this.tableModel.getValueAt(n, 0);
            Customer customer = null;
            for (Customer customer2 : CustomerDAO.getAllCustomers()) {
                if (!customer2.getCustomerCode().equals(string)) continue;
                customer = customer2;
                break;
            }
            if (customer != null) {
                String string2 = "Customer Code: " + customer.getCustomerCode() + "\nName: " + customer.getCustomerName() + "\nPhone: " + customer.getPhone() + "\nEmail: " + customer.getEmail() + "\nOutstanding: \u20b9" + String.format("%.2f", customer.getOutstandingAmount()) + "\nCredit Limit: \u20b9" + String.format("%.2f", customer.getCreditLimit()) + "\nLoyalty Points: " + String.format("%.0f", customer.getLoyaltyPoints());
                UIUtils.showSuccessDialog(this, "Customer Details", string2);
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a customer");
        }
    }

    private void handleSetCreditLimit() {
        int n = this.customersTable.getSelectedRow();
        if (n >= 0) {
            String string = (String)this.tableModel.getValueAt(n, 0);
            Customer customer = null;
            for (Customer customer2 : CustomerDAO.getAllCustomers()) {
                if (customer2.getCustomerCode().equals(string)) {
                    customer = customer2;
                    break;
                }
            }
            String newLimitStr;
            if (customer != null && (newLimitStr = JOptionPane.showInputDialog(this, "Enter New Credit Limit:", String.format("%.2f", customer.getCreditLimit()))) != null) {
                try {
                    double d = Double.parseDouble(newLimitStr);
                    if (d < 0) {
                        UIUtils.showErrorDialog(this, "Error", "Credit limit cannot be negative.");
                        return;
                    }
                    double oldLimit = customer.getCreditLimit();
                    customer.setCreditLimit(d);
                    if (CustomerDAO.updateCustomer(customer)) {
                        int actorUserId = (parent instanceof MainFrame) ? ((MainFrame) parent).getCurrentUser().getUserId() : 1;
                        AuditLogDAO.log(actorUserId, "CREDIT_LIMIT_CHANGE", "customer", customer.getCustomerId(), "Old: " + oldLimit, "New: " + d);
                        UIUtils.showSuccessDialog(this, "Success", "Credit limit updated!");
                        this.loadCustomersTable();
                    } else {
                        UIUtils.showErrorDialog(this, "Error", "Failed to update credit limit in database.");
                    }
                }
                catch (NumberFormatException numberFormatException) {
                    UIUtils.showErrorDialog(this, "Error", "Please enter a valid numeric amount.");
                }
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a customer");
        }
    }
}
