package ui.frames;

import database.Supplier;
import database.SupplierDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class SupplierFrame extends JInternalFrame {
    private JFrame parent;
    private JTextField searchField;
    private JTable suppliersTable;
    private DefaultTableModel tableModel;
    private List<Supplier> supplierList = new ArrayList<>();

    public SupplierFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
        this.loadSuppliersTable();
    }

    private void initializeUI() {
        this.setTitle("Supplier Master Management");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1000, 600);
        this.setLocation(100, 100);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = UIUtils.createTitleLabel("Supplier Master Management");
        mainPanel.add((Component)titleLabel, "North");

        JPanel searchPanel = this.createSearchPanel();
        mainPanel.add((Component)searchPanel, "First");

        JPanel tablePanel = this.createTablePanel();
        mainPanel.add((Component)tablePanel, "Center");

        JPanel actionPanel = this.createActionPanel();
        mainPanel.add((Component)actionPanel, "South");

        this.add(mainPanel);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JPanel searchGroup = new JPanel();
        searchGroup.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        searchGroup.setBackground(UIConstants.APP_BACKGROUND);
        searchGroup.setBorder(BorderFactory.createTitledBorder("Supplier Search Parameters"));

        searchGroup.add(UIUtils.createLabel("Search by Name/Phone:"));
        this.searchField = UIUtils.createTextField(20);
        this.searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                SupplierFrame.this.handleSearch();
            }
        });
        searchGroup.add(this.searchField);

        JButton clearBtn = UIUtils.createDangerButton("Clear", actionEvent -> {
            this.searchField.setText("");
            this.loadSuppliersTable();
        });
        searchGroup.add(clearBtn);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add((Component)searchGroup, gbc);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), 
                "Supplier List Details", 
                javax.swing.border.TitledBorder.LEFT, 
                javax.swing.border.TitledBorder.TOP, 
                UIConstants.HEADING_FONT, 
                UIConstants.PRIMARY_COLOR));

        this.tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        
        this.tableModel.addColumn("Supplier Code");
        this.tableModel.addColumn("Name");
        this.tableModel.addColumn("Phone");
        this.tableModel.addColumn("Email");
        this.tableModel.addColumn("State/Address");
        this.tableModel.addColumn("GSTIN");
        this.tableModel.addColumn("Outstanding Balance");
        this.tableModel.addColumn("Status");

        this.suppliersTable = new JTable(this.tableModel);
        this.suppliersTable.setFont(UIConstants.NORMAL_FONT);
        this.suppliersTable.setRowHeight(25);
        this.suppliersTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = UIUtils.createTableScrollPane(this.suppliersTable);
        panel.add((Component)scrollPane, "Center");
        return panel;
    }

    private void loadSuppliersTable() {
        this.tableModel.setRowCount(0);
        try {
            supplierList = SupplierDAO.getAllSuppliers();
            for (Supplier supplier : supplierList) {
                Vector<Object> row = new Vector<>();
                row.add(supplier.getSupplierCode());
                row.add(supplier.getSupplierName());
                row.add(supplier.getPhone());
                row.add(supplier.getEmail() != null ? supplier.getEmail() : "");
                row.add(supplier.getState() != null ? supplier.getState() : "");
                row.add(supplier.getGstin() != null ? supplier.getGstin() : "");
                row.add("\u20b9" + String.format("%.2f", supplier.getOutstandingBalance()));
                row.add(supplier.getStatus());
                this.tableModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));

        JButton addBtn = UIUtils.createSuccessButton("Add Supplier", actionEvent -> this.handleAddSupplier());
        JButton editBtn = UIUtils.createButton("Edit Supplier", actionEvent -> this.handleEditSupplier());
        JButton deactivateBtn = UIUtils.createDangerButton("Deactivate Supplier", actionEvent -> this.handleDeactivateSupplier());
        JButton refreshBtn = UIUtils.createButton("Refresh", actionEvent -> this.loadSuppliersTable());

        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deactivateBtn);
        panel.add(refreshBtn);
        return panel;
    }

    private void handleSearch() {
        String keyword = this.searchField.getText().trim();
        if (keyword.isEmpty()) {
            this.loadSuppliersTable();
            return;
        }
        this.tableModel.setRowCount(0);
        try {
            supplierList = SupplierDAO.searchSuppliers(keyword);
            for (Supplier supplier : supplierList) {
                Vector<Object> row = new Vector<>();
                row.add(supplier.getSupplierCode());
                row.add(supplier.getSupplierName());
                row.add(supplier.getPhone());
                row.add(supplier.getEmail() != null ? supplier.getEmail() : "");
                row.add(supplier.getState() != null ? supplier.getState() : "");
                row.add(supplier.getGstin() != null ? supplier.getGstin() : "");
                row.add("\u20b9" + String.format("%.2f", supplier.getOutstandingBalance()));
                row.add(supplier.getStatus());
                this.tableModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddSupplier() {
        AddSupplierDialog dialog = new AddSupplierDialog(parent, null, () -> this.loadSuppliersTable());
        dialog.setVisible(true);
    }

    private void handleEditSupplier() {
        int selectedRow = this.suppliersTable.getSelectedRow();
        if (selectedRow >= 0) {
            Supplier selectedSupplier = supplierList.get(selectedRow);
            AddSupplierDialog dialog = new AddSupplierDialog(parent, selectedSupplier, () -> this.loadSuppliersTable());
            dialog.setVisible(true);
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a supplier to edit");
        }
    }

    private void handleDeactivateSupplier() {
        int selectedRow = this.suppliersTable.getSelectedRow();
        if (selectedRow >= 0) {
            Supplier selectedSupplier = supplierList.get(selectedRow);
            boolean confirm = UIUtils.showConfirmDialog(this, "Deactivate Supplier", 
                    "Are you sure you want to deactivate supplier '" + selectedSupplier.getSupplierName() + "'?");
            if (confirm) {
                boolean success = SupplierDAO.deactivateSupplier(selectedSupplier.getSupplierId());
                if (success) {
                    UIUtils.showSuccessDialog(this, "Success", "Supplier deactivated successfully.");
                    this.loadSuppliersTable();
                } else {
                    UIUtils.showErrorDialog(this, "Error", "Failed to deactivate supplier.");
                }
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a supplier to deactivate.");
        }
    }
}
