package ui.frames;

import database.ItemMaster;
import database.ItemMasterDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

/**
 * Inventory Management Frame showing real stock status from the database.
 * 
 * Support showing:
 *  - All Items
 *  - Low Stock Items (stockQuantity > 0 and <= reorderLevel)
 *  - Out of Stock Items (stockQuantity == 0)
 */
public class InventoryFrame extends JInternalFrame {
    private JFrame parent;
    private JComboBox<String> categoryCombo;
    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private boolean showOnlyLowStockOnStartup = false;
    
    // Store current state of filter
    private String currentStockFilter = "ALL"; // "ALL", "LOW_STOCK", "OUT_OF_STOCK"

    public InventoryFrame(JFrame jFrame) {
        this(jFrame, false);
    }

    public InventoryFrame(JFrame jFrame, boolean showOnlyLowStockOnStartup) {
        this.parent = jFrame;
        this.showOnlyLowStockOnStartup = showOnlyLowStockOnStartup;
        this.initializeUI();
        
        if (this.showOnlyLowStockOnStartup) {
            this.handleShowLowStock();
        } else {
            this.loadInventoryTable();
        }
    }

    private void initializeUI() {
        this.setTitle("Inventory Management");
        this.setClosable(true);
        this.setSize(1000, 650);
        this.setLocation(100, 100);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = UIUtils.createTitleLabel("Inventory Stock Management");
        mainPanel.add((Component)titleLabel, "North");
        
        JPanel filterPanel = this.createFilterPanel();
        mainPanel.add((Component)filterPanel, "First");
        
        JPanel tablePanel = this.createTablePanel();
        mainPanel.add((Component)tablePanel, "Center");
        
        JPanel actionPanel = this.createActionPanel();
        mainPanel.add((Component)actionPanel, "South");
        
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
        
        JPanel filterGroup = new JPanel();
        filterGroup.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterGroup.setBackground(UIConstants.APP_BACKGROUND);
        filterGroup.setBorder(BorderFactory.createTitledBorder("Inventory Filters"));
        
        filterGroup.add(UIUtils.createLabel("Category:"));
        this.categoryCombo = UIUtils.createComboBox(new String[]{"All", "Shoes", "Sandals", "Slippers"});
        this.categoryCombo.addActionListener(actionEvent -> this.handleCategoryFilter());
        filterGroup.add(this.categoryCombo);
        
        JButton showLowBtn = UIUtils.createButton("Show Low Stock", actionEvent -> this.handleShowLowStock());
        showLowBtn.setBackground(UIConstants.WARNING_COLOR);
        showLowBtn.setForeground(UIConstants.TEXT_ON_WARNING);
        filterGroup.add(showLowBtn);
        
        JButton showOutBtn = UIUtils.createButton("Show Out of Stock", actionEvent -> this.handleShowOutOfStock());
        showOutBtn.setBackground(UIConstants.DANGER_COLOR);
        showOutBtn.setForeground(UIConstants.TEXT_ON_DANGER);
        filterGroup.add(showOutBtn);
        
        JButton refreshBtn = UIUtils.createButton("Show All / Refresh", actionEvent -> {
            this.currentStockFilter = "ALL";
            this.loadInventoryTable();
        });
        filterGroup.add(refreshBtn);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add((Component)filterGroup, gbc);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), 
                "Stock Details Grid", 
                javax.swing.border.TitledBorder.LEFT, 
                javax.swing.border.TitledBorder.TOP, 
                UIConstants.HEADING_FONT, 
                UIConstants.PRIMARY_COLOR));
        
        this.tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tableModel.addColumn("Item ID"); // Hidden/helper or first col
        this.tableModel.addColumn("Item Code");
        this.tableModel.addColumn("Item Name");
        this.tableModel.addColumn("Category");
        this.tableModel.addColumn("Current Stock");
        this.tableModel.addColumn("Reorder Level");
        this.tableModel.addColumn("Shortage");
        this.tableModel.addColumn("Stock Status");
        this.tableModel.addColumn("Last Updated");
        
        this.inventoryTable = new JTable(this.tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component component = super.prepareRenderer(renderer, row, col);
                String status = (String) this.getValueAt(row, 7);
                
                if (this.isRowSelected(row)) {
                    component.setBackground(this.getSelectionBackground());
                    component.setForeground(this.getSelectionForeground());
                } else {
                    if ("Low Stock".equals(status)) {
                        component.setBackground(UIConstants.STATUS_WARNING_BG);
                        component.setForeground(UIConstants.STATUS_WARNING_FG);
                    } else if ("Out of Stock".equals(status)) {
                        component.setBackground(UIConstants.STATUS_DANGER_BG);
                        component.setForeground(UIConstants.STATUS_DANGER_FG);
                    } else {
                        component.setBackground(UIConstants.APP_BACKGROUND);
                        component.setForeground(UIConstants.TEXT_ON_APP_BG);
                    }
                }
                return component;
            }
        };
        
        this.inventoryTable.setFont(UIConstants.NORMAL_FONT);
        this.inventoryTable.setRowHeight(25);
        
        // Hide Item ID column but keep it in model
        this.inventoryTable.getColumnModel().getColumn(0).setMinWidth(0);
        this.inventoryTable.getColumnModel().getColumn(0).setMaxWidth(0);
        this.inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        
        JScrollPane scrollPane = UIUtils.createTableScrollPane(this.inventoryTable);
        panel.add((Component)scrollPane, "Center");
        return panel;
    }

    private void loadInventoryTable() {
        this.tableModel.setRowCount(0);
        List<ItemMaster> items = new ArrayList<>();
        
        // Fetch matching the filter
        if ("LOW_STOCK".equals(this.currentStockFilter)) {
            items = ItemMasterDAO.getLowStockItems();
        } else if ("OUT_OF_STOCK".equals(this.currentStockFilter)) {
            items = ItemMasterDAO.getOutOfStockItems();
        } else {
            items = ItemMasterDAO.getAllItems();
        }
        
        String selectedCategory = this.categoryCombo != null ? (String) this.categoryCombo.getSelectedItem() : "All";
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm a");

        for (ItemMaster item : items) {
            // Apply category filter in-memory for ease and consistency
            if (!"All".equalsIgnoreCase(selectedCategory) && !item.getCategory().equalsIgnoreCase(selectedCategory)) {
                continue;
            }
            
            String status = "Good";
            int shortage = 0;
            if (item.getStockQuantity() == 0) {
                status = "Out of Stock";
                shortage = item.getReorderLevel();
            } else if (item.getStockQuantity() <= item.getReorderLevel()) {
                status = "Low Stock";
                shortage = item.getReorderLevel() - item.getStockQuantity();
            }
            
            this.tableModel.addRow(new Object[]{
                item.getItemId(),
                item.getItemCode(),
                item.getItemName(),
                item.getCategory(),
                item.getStockQuantity(),
                item.getReorderLevel(),
                shortage,
                status,
                item.getModifiedDate() != null ? df.format(item.getModifiedDate()) : (item.getCreatedDate() != null ? df.format(item.getCreatedDate()) : "N/A")
            });
        }
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));
        
        JButton adjustBtn = UIUtils.createButton("Adjust Stock", actionEvent -> this.handleAdjustStock());
        JButton transferBtn = UIUtils.createButton("Stock Transfer", actionEvent -> this.handleStockTransfer());
        JButton createPoBtn = UIUtils.createButton("Create Purchase Order", actionEvent -> this.handleCreatePurchaseOrder());
        JButton breakdownBtn = UIUtils.createButton("Category Breakdown", actionEvent -> this.handleCategoryBreakdown());
        JButton printBtn = UIUtils.createButton("Print Report", actionEvent -> this.handlePrintReport());
        
        panel.add(adjustBtn);
        panel.add(transferBtn);
        panel.add(createPoBtn);
        panel.add(breakdownBtn);
        panel.add(printBtn);
        return panel;
    }

    private void handleCategoryFilter() {
        this.loadInventoryTable();
    }

    private void handleShowLowStock() {
        this.currentStockFilter = "LOW_STOCK";
        this.loadInventoryTable();
    }

    private void handleShowOutOfStock() {
        this.currentStockFilter = "OUT_OF_STOCK";
        this.loadInventoryTable();
    }

    private void handleAdjustStock() {
        int selectedRow = this.inventoryTable.getSelectedRow();
        if (selectedRow >= 0) {
            int itemId = (Integer) this.tableModel.getValueAt(selectedRow, 0);
            String itemCode = (String) this.tableModel.getValueAt(selectedRow, 1);
            int currentStock = (Integer) this.tableModel.getValueAt(selectedRow, 4);
            
            String newQtyStr = JOptionPane.showInputDialog(this, 
                "Item: " + itemCode + "\nCurrent Stock: " + currentStock + "\n\nEnter new stock quantity:");
            
            if (newQtyStr != null && !newQtyStr.trim().isEmpty()) {
                try {
                    int newQty = Integer.parseInt(newQtyStr.trim());
                    if (newQty < 0) {
                        UIUtils.showErrorDialog(this, "Error", "Stock quantity cannot be negative.");
                        return;
                    }
                    
                    boolean success = ItemMasterDAO.updateStockQuantityAbsolute(itemId, newQty);
                    if (success) {
                        UIUtils.showSuccessDialog(this, "Success", "Stock adjusted successfully to " + newQty);
                        this.loadInventoryTable();
                        
                        // Notify siblings (Dashboard) to update counts
                        if (parent instanceof MainFrame) {
                            for (javax.swing.JInternalFrame frame : ((MainFrame) parent).getDesktopPane().getAllFrames()) {
                                if (frame instanceof DashboardFrame) {
                                    ((DashboardFrame) frame).refreshStats();
                                    break;
                                }
                            }
                        }
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

    private void handleStockTransfer() {
        UIUtils.showSuccessDialog(this, "Info", "Stock transfer dialog will open here");
    }

    private void handleCreatePurchaseOrder() {
        UIUtils.showSuccessDialog(this, "Info", "Purchase order creation dialog will open here");
    }

    private void handleCategoryBreakdown() {
        List<ItemMaster> items = ItemMasterDAO.getAllItems();
        int shoesStock = 0;
        int sandalsStock = 0;
        int slippersStock = 0;
        int otherStock = 0;
        
        for (ItemMaster item : items) {
            String cat = item.getCategory().toLowerCase();
            if (cat.contains("shoe")) {
                shoesStock += item.getStockQuantity();
            } else if (cat.contains("sandal")) {
                sandalsStock += item.getStockQuantity();
            } else if (cat.contains("slipper")) {
                slippersStock += item.getStockQuantity();
            } else {
                otherStock += item.getStockQuantity();
            }
        }
        
        int total = shoesStock + sandalsStock + slippersStock + otherStock;
        
        String breakDownStr = "Stock Breakdown by Category:\n\n" +
                              "Shoes: " + shoesStock + " units\n" +
                              "Sandals: " + sandalsStock + " units\n" +
                              "Slippers: " + slippersStock + " units\n" +
                              "Other: " + otherStock + " units\n\n" +
                              "Total Stock: " + total + " units";
                              
        UIUtils.showSuccessDialog(this, "Stock Breakdown", breakDownStr);
    }

    private void handlePrintReport() {
        UIUtils.showSuccessDialog(this, "Success", "Inventory report sent to printer");
    }
}
