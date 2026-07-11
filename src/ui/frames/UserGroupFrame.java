/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Objects;
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

public class UserGroupFrame
extends JInternalFrame {
    private DefaultTableModel tableModel;
    private JTable groupTable;

    public UserGroupFrame(JFrame jFrame) {
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("User Groups Management");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(500, 400);
        this.setLocation(150, 150);
        JPanel jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JLabel jLabel = UIUtils.createLabel("Manage System User Groups & Roles");
        jLabel.setFont(UIConstants.TITLE_FONT);
        jPanel.add((Component)jLabel, "North");
        this.tableModel = new DefaultTableModel(new String[]{"Group ID", "Role Name", "Description", "Status"}, 0){
            @Override
            public boolean isCellEditable(int n, int n2) {
                return false;
            }
        };
        this.groupTable = new JTable(this.tableModel);
        this.groupTable.setRowHeight(25);
        this.groupTable.setFont(UIConstants.NORMAL_FONT);
        this.loadMockData();
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(this.groupTable);
        jPanel.add((Component)jScrollPane, "Center");
        JPanel jPanel2 = new JPanel(new FlowLayout(2, 10, 10));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton = UIUtils.createSuccessButton("Add New Group", actionEvent -> this.handleAddGroup());
        JButton jButton2 = UIUtils.createButton("Edit Permissions", actionEvent -> this.handleEditGroup());
        jPanel2.add(jButton);
        jPanel2.add(jButton2);
        jPanel.add((Component)jPanel2, "South");
        this.add(jPanel);
    }

    private void loadMockData() {
        this.tableModel.addRow(new Object[]{"1", "ADMIN", "Full system access including masters and reports", "ACTIVE"});
        this.tableModel.addRow(new Object[]{"2", "CASHIER", "Limited to POS Billing and basic functions", "ACTIVE"});
        this.tableModel.addRow(new Object[]{"3", "MANAGER", "Access to sales, inventory and reporting", "ACTIVE"});
    }

    private void handleAddGroup() {
        JPanel jPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        jPanel.add(new JLabel("Role Name:"));
        JTextField jTextField = new JTextField();
        jPanel.add(jTextField);
        jPanel.add(new JLabel("Description:"));
        JTextField jTextField2 = new JTextField();
        jPanel.add(jTextField2);
        int n = JOptionPane.showConfirmDialog(this, jPanel, "Add New User Group", 2);
        if (n == 0) {
            String string = jTextField.getText().trim().toUpperCase();
            String string2 = jTextField2.getText().trim();
            if (!string.isEmpty()) {
                this.tableModel.addRow(new Object[]{String.valueOf(this.tableModel.getRowCount() + 1), string, string2, "ACTIVE"});
                UIUtils.showSuccessDialog(this, "Success", "User Group added successfully!");
            } else {
                UIUtils.showErrorDialog(this, "Error", "Role Name cannot be empty.");
            }
        }
    }

    private void handleEditGroup() {
        int n = this.groupTable.getSelectedRow();
        if (n == -1) {
            UIUtils.showWarningDialog(this, "No Selection", "Please select a User Group to edit.");
            return;
        }
        String string = (String)this.tableModel.getValueAt(n, 1);
        UIUtils.showSuccessDialog(this, "Edit Permissions", "Permissions management for " + string + " will open here.");
    }
}
