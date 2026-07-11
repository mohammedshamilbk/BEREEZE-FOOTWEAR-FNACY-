package ui.frames;

import database.ItemMaster;
import database.ItemMasterDAO;
import database.AuditLogDAO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Vector;
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
import javax.swing.table.DefaultTableModel;

/**
 * JInternalFrame for managing Footwear Item Masters.
 */
public class ItemMasterFrame extends JInternalFrame {
    private JFrame parent;
    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JTable itemsTable;
    private DefaultTableModel tableModel;

    public ItemMasterFrame(JFrame parent) {
        this.parent = parent;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Item Master");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1000, 600);
        this.setLocation(100, 100);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = UIUtils.createTitleLabel("Item Management");
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel filterPanel = this.createFilterPanel();
        mainPanel.add(filterPanel, BorderLayout.BEFORE_FIRST_LINE);
        
        JPanel tablePanel = this.createTablePanel();
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        
        JPanel actionPanel = this.createActionPanel();
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        
        this.add(mainPanel);
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        rowPanel.setBackground(UIConstants.APP_BACKGROUND);
        rowPanel.setBorder(BorderFactory.createTitledBorder("Search & Filter Options"));
        
        rowPanel.add(UIUtils.createLabel("Search:"));
        this.searchField = UIUtils.createTextField(15);
        this.searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                ItemMasterFrame.this.handleSearch();
            }
        });
        rowPanel.add(this.searchField);
        
        rowPanel.add(UIUtils.createLabel("Category:"));
        this.categoryCombo = UIUtils.createComboBox(new String[]{"All", "Shoes", "Sandals", "Slippers", "Chappal"});
        this.categoryCombo.addActionListener(actionEvent -> this.handleCategoryFilter());
        rowPanel.add(this.categoryCombo);
        
        JButton clearBtn = UIUtils.createDangerButton("Clear Filters", actionEvent -> {
            this.searchField.setText("");
            this.categoryCombo.setSelectedIndex(0);
            this.loadItemsTable();
        });
        rowPanel.add(clearBtn);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(rowPanel, gbc);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), 
            "Item Details Grid", 
            1, 2, 
            UIConstants.HEADING_FONT, 
            UIConstants.PRIMARY_COLOR
        ));
        
        this.tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tableModel.addColumn("Item Code");
        this.tableModel.addColumn("Item Name");
        this.tableModel.addColumn("Category");
        this.tableModel.addColumn("Price");
        this.tableModel.addColumn("Stock");
        this.tableModel.addColumn("Reorder Level");
        this.tableModel.addColumn("Status");
        
        this.itemsTable = new JTable(this.tableModel);
        this.itemsTable.setFont(UIConstants.NORMAL_FONT);
        this.itemsTable.setRowHeight(25);
        
        this.loadItemsTable();
        JScrollPane scrollPane = UIUtils.createTableScrollPane(this.itemsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadItemsTable() {
        this.tableModel.setRowCount(0);
        try {
            String keyword = this.searchField.getText().trim();
            String category = this.categoryCombo.getSelectedItem() != null ? this.categoryCombo.getSelectedItem().toString() : "All";
            
            List<ItemMaster> items;
            if (!keyword.isEmpty()) {
                items = ItemMasterDAO.searchItems(keyword);
            } else {
                items = ItemMasterDAO.getAllItems();
            }
            
            for (ItemMaster item : items) {
                if (!category.equalsIgnoreCase("All") && !item.getCategory().equalsIgnoreCase(category)) {
                    continue;
                }
                
                Vector<Object> row = new Vector<>();
                row.add(item.getItemCode());
                row.add(item.getItemName());
                row.add(item.getCategory());
                row.add("\u20b9" + String.format("%.2f", item.getSellingPrice()));
                row.add(item.getStockQuantity());
                row.add(item.getReorderLevel());
                row.add(item.getStatus());
                this.tableModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));
        
        JButton addBtn = UIUtils.createSuccessButton("Add Item", actionEvent -> this.handleAddItem());
        JButton editBtn = UIUtils.createButton("Edit Item", actionEvent -> this.handleEditItem());
        JButton deleteBtn = UIUtils.createDangerButton("Delete Item", actionEvent -> this.handleDeleteItem());
        JButton generateBarcodeBtn = UIUtils.createButton("Generate Barcode", actionEvent -> this.handleGenerateBarcode());
        JButton printBarcodeBtn = UIUtils.createButton("Print Barcode", actionEvent -> this.handlePrintBarcode());
        JButton updateStockBtn = UIUtils.createButton("Update Stock", actionEvent -> this.handleStockUpdate());
        
        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);
        panel.add(generateBarcodeBtn);
        panel.add(printBarcodeBtn);
        panel.add(updateStockBtn);
        return panel;
    }

    private void handleSearch() {
        this.loadItemsTable();
    }

    private void handleCategoryFilter() {
        this.loadItemsTable();
    }

    private ItemMaster getSelectedItem() {
        int selectedRow = this.itemsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String itemCode = (String) this.tableModel.getValueAt(selectedRow, 0);
            return ItemMasterDAO.getAllItems().stream()
                .filter(i -> i.getItemCode().equalsIgnoreCase(itemCode))
                .findFirst().orElse(null);
        }
        return null;
    }

    private void handleAddItem() {
        ItemEntryDialog itemEntryDialog = new ItemEntryDialog(this.parent, "Add New Item - Electronic Reservation Slip", true, null);
        itemEntryDialog.setVisible(true);
        if (itemEntryDialog.isSaved()) {
            this.loadItemsTable();
            this.refreshDashboardStats();
        }
    }

    private void handleEditItem() {
        ItemMaster item = getSelectedItem();
        if (item != null) {
            ItemEntryDialog itemEntryDialog = new ItemEntryDialog(this.parent, "Edit Item Master Details", true, item);
            itemEntryDialog.setVisible(true);
            if (itemEntryDialog.isSaved()) {
                this.loadItemsTable();
                this.refreshDashboardStats();
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select an item to edit from the table.");
        }
    }

    private void handleDeleteItem() {
        ItemMaster item = getSelectedItem();
        if (item != null) {
            if (UIUtils.showConfirmDialog(this, "Confirm", "Deactivate item " + item.getItemName() + "?")) {
                if (ItemMasterDAO.deleteItem(item.getItemId())) {
                    UIUtils.showSuccessDialog(this, "Success", "Item deactivated successfully");
                    this.loadItemsTable();
                    this.refreshDashboardStats();
                } else {
                    UIUtils.showErrorDialog(this, "Error", "Failed to deactivate item.");
                }
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select an item to delete");
        }
    }

    private void handleGenerateBarcode() {
        ItemMaster item = getSelectedItem();
        if (item != null) {
            UIUtils.showSuccessDialog(this, "Success", "Barcode generated for " + item.getItemCode() + ": " + (item.getBarcode() != null ? item.getBarcode() : item.getItemCode()));
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select an item first.");
        }
    }

    private void handlePrintBarcode() {
        ItemMaster item = getSelectedItem();
        if (item != null) {
            UIUtils.showSuccessDialog(this, "Success", "Barcode printing initiated for " + item.getItemName());
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select an item first.");
        }
    }

    private void handleStockUpdate() {
        ItemMaster item = getSelectedItem();
        if (item != null) {
            String newQtyStr = JOptionPane.showInputDialog(this, 
                "Item: " + item.getItemCode() + "\nCurrent Stock: " + item.getStockQuantity() + "\n\nEnter new stock quantity:");
            
            if (newQtyStr != null && !newQtyStr.trim().isEmpty()) {
                try {
                    int newQty = Integer.parseInt(newQtyStr.trim());
                    if (newQty < 0) {
                        UIUtils.showErrorDialog(this, "Error", "Stock quantity cannot be negative.");
                        return;
                    }
                    
                    int actorUserId = (parent instanceof MainFrame) ? ((MainFrame) parent).getCurrentUser().getUserId() : 1;
                    int oldQty = item.getStockQuantity();
                    boolean success = ItemMasterDAO.updateStockQuantityAbsolute(item.getItemId(), newQty);
                    if (success) {
                        AuditLogDAO.log(actorUserId, "STOCK_ADJUSTMENT", "item_master", item.getItemId(), "Old: " + oldQty, "New: " + newQty);
                        UIUtils.showSuccessDialog(this, "Success", "Stock adjusted successfully to " + newQty);
                        this.loadItemsTable();
                        this.refreshDashboardStats();
                    } else {
                        UIUtils.showErrorDialog(this, "Error", "Failed to update stock in database.");
                    }
                } catch (NumberFormatException numberFormatException) {
                    UIUtils.showErrorDialog(this, "Error", "Please enter a valid integer number.");
                }
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select an item from the table first.");
        }
    }

    private void refreshDashboardStats() {
        if (parent instanceof MainFrame) {
            for (JInternalFrame frame : ((MainFrame) parent).getDesktopPane().getAllFrames()) {
                if (frame instanceof DashboardFrame) {
                    ((DashboardFrame) frame).refreshStats();
                    break;
                }
            }
        }
    }
}
