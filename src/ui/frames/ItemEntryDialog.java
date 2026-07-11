package ui.frames;

import database.ItemMaster;
import database.ItemMasterDAO;
import database.AuditLogDAO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Dialog for adding or editing an item in Item Master.
 */
public class ItemEntryDialog extends JDialog {
    private JTextField itemNameField;
    private JTextField itemCodeField;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> manufacturerCombo;
    private JComboBox<String> activeStatusCombo;
    private JTextField hsnCodeField;
    private JTextField pRateField;
    private JTextField mrpField;
    private JTextField sRate1Field;
    private JComboBox<String> sRateTypeCombo;
    private JComboBox<String> barcodeModeCombo;
    private JTextField barcodeField;
    private JComboBox<String> productClassCombo;
    
    private boolean saved = false;
    private ItemMaster itemToEdit;

    public ItemEntryDialog(Frame frame, String title, boolean modal) {
        this(frame, title, modal, null);
    }

    public ItemEntryDialog(Frame frame, String title, boolean modal, ItemMaster itemToEdit) {
        super(frame, title, modal);
        this.itemToEdit = itemToEdit;
        this.initializeUI();
        if (itemToEdit != null) {
            populateFields();
        }
    }

    private void initializeUI() {
        this.setSize(850, 680);
        this.setLocationRelativeTo(this.getParent());
        this.getContentPane().setBackground(UIConstants.LIGHT_COLOR);
        this.setLayout(new BorderLayout());
        
        JPanel headerPanel = this.createActionHeaderPanel();
        this.add(headerPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(UIConstants.APP_BACKGROUND);
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15), 
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(UIConstants.APP_BACKGROUND);
        
        JPanel generalInfoPanel = new JPanel(new GridBagLayout());
        generalInfoPanel.setBackground(UIConstants.APP_BACKGROUND);
        generalInfoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), 
            "General Item Information", 
            1, 2, 
            UIConstants.HEADING_FONT, 
            UIConstants.PRIMARY_COLOR
        ));
        
        GridBagConstraints gbcGen = new GridBagConstraints();
        gbcGen.insets = new Insets(4, 4, 4, 4);
        gbcGen.fill = GridBagConstraints.HORIZONTAL;
        gbcGen.weightx = 1.0;
        
        gbcGen.gridx = 0;
        gbcGen.gridy = 0;
        generalInfoPanel.add(UIUtils.createLabel("Item Name: *"), gbcGen);
        
        gbcGen.gridx = 1;
        this.itemNameField = UIUtils.createTextField(15);
        generalInfoPanel.add(this.itemNameField, gbcGen);
        
        gbcGen.gridx = 0;
        gbcGen.gridy = 1;
        generalInfoPanel.add(UIUtils.createLabel("Item Code: *"), gbcGen);
        
        gbcGen.gridx = 1;
        this.itemCodeField = UIUtils.createTextField(15);
        generalInfoPanel.add(this.itemCodeField, gbcGen);
        
        gbcGen.gridx = 0;
        gbcGen.gridy = 2;
        generalInfoPanel.add(UIUtils.createLabel("Category:"), gbcGen);
        
        gbcGen.gridx = 1;
        this.categoryCombo = UIUtils.createComboBox(new String[]{"SHOES", "CHAPPAL", "SANDALS", "SLIPPERS", "DEFAULT"});
        generalInfoPanel.add(this.categoryCombo, gbcGen);
        
        gbcGen.gridx = 0;
        gbcGen.gridy = 3;
        generalInfoPanel.add(UIUtils.createLabel("Manufacturer:"), gbcGen);
        
        gbcGen.gridx = 1;
        this.manufacturerCombo = UIUtils.createComboBox(new String[]{"Breeze", "VKC", "Paragon", "Bata", "Sparx", "Other"});
        generalInfoPanel.add(this.manufacturerCombo, gbcGen);
        
        gbcGen.gridx = 0;
        gbcGen.gridy = 4;
        generalInfoPanel.add(UIUtils.createLabel("Status:"), gbcGen);
        
        gbcGen.gridx = 1;
        this.activeStatusCombo = UIUtils.createComboBox(new String[]{"ACTIVE", "INACTIVE"});
        generalInfoPanel.add(this.activeStatusCombo, gbcGen);
        
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.insets = new Insets(6, 6, 6, 6);
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.weightx = 1.0;
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 0;
        leftPanel.add(generalInfoPanel, gbcLeft);
        
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(UIConstants.APP_BACKGROUND);
        rightPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), 
            "Pricing & Barcode", 
            1, 2, 
            UIConstants.HEADING_FONT, 
            UIConstants.PRIMARY_COLOR
        ));
        
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.insets = new Insets(4, 4, 4, 4);
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.weightx = 1.0;
        
        gbcRight.gridx = 0;
        gbcRight.gridy = 0;
        rightPanel.add(UIUtils.createLabel("Purchase Rate: *"), gbcRight);
        
        gbcRight.gridx = 1;
        this.pRateField = UIUtils.createTextField(15);
        rightPanel.add(this.pRateField, gbcRight);
        
        gbcRight.gridx = 0;
        gbcRight.gridy = 1;
        rightPanel.add(UIUtils.createLabel("Selling Rate (S Rate 1): *"), gbcRight);
        
        gbcRight.gridx = 1;
        this.sRate1Field = UIUtils.createTextField(15);
        rightPanel.add(this.sRate1Field, gbcRight);
        
        gbcRight.gridx = 0;
        gbcRight.gridy = 2;
        rightPanel.add(UIUtils.createLabel("MRP:"), gbcRight);
        
        gbcRight.gridx = 1;
        this.mrpField = UIUtils.createTextField(15);
        rightPanel.add(this.mrpField, gbcRight);
        
        gbcRight.gridx = 0;
        gbcRight.gridy = 3;
        rightPanel.add(UIUtils.createLabel("Barcode:"), gbcRight);
        
        gbcRight.gridx = 1;
        this.barcodeField = UIUtils.createTextField(15);
        rightPanel.add(this.barcodeField, gbcRight);
        
        gbcRight.gridx = 0;
        gbcRight.gridy = 4;
        rightPanel.add(UIUtils.createLabel("Product Class:"), gbcRight);
        
        gbcRight.gridx = 1;
        this.productClassCombo = UIUtils.createComboBox(new String[]{"Stock Item", "Service Item"});
        rightPanel.add(this.productClassCombo, gbcRight);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(leftPanel, gbc);
        
        gbc.gridx = 1;
        centerPanel.add(rightPanel, gbc);
        
        this.add(centerPanel, BorderLayout.CENTER);
        
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(UIConstants.DARK_COLOR);
        footerPanel.setPreferredSize(new Dimension(this.getWidth(), 35));
        JLabel shortcutLabel = new JLabel("Keyboard Shortcuts: F3 Create | F4 Edit | F5 Save | F6 Print | F7 Delete | Esc Close | F8 Batch View");
        shortcutLabel.setFont(UIConstants.NORMAL_FONT);
        shortcutLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        footerPanel.add(shortcutLabel);
        this.add(footerPanel, BorderLayout.SOUTH);
        
        this.setupKeyboardShortcuts();
    }

    private void populateFields() {
        this.itemNameField.setText(itemToEdit.getItemName());
        this.itemCodeField.setText(itemToEdit.getItemCode());
        this.itemCodeField.setEditable(false);
        this.categoryCombo.setSelectedItem(itemToEdit.getCategory().toUpperCase());
        this.manufacturerCombo.setSelectedItem(itemToEdit.getManufacturer());
        this.activeStatusCombo.setSelectedItem(itemToEdit.getStatus());
        this.pRateField.setText(String.format("%.2f", itemToEdit.getPurchasePrice()));
        this.sRate1Field.setText(String.format("%.2f", itemToEdit.getSellingPrice()));
        this.mrpField.setText(String.format("%.2f", itemToEdit.getSellingPrice()));
        this.barcodeField.setText(itemToEdit.getBarcode() != null ? itemToEdit.getBarcode() : itemToEdit.getItemCode());
    }

    private JPanel createActionHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(UIConstants.PRIMARY_COLOR);
        panel.setPreferredSize(new Dimension(this.getWidth(), 60));
        
        JLabel titleLabel = new JLabel("Electronic Reservation Slip (ERS) - Item Master ");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setForeground(UIConstants.TEXT_ON_PRIMARY);
        panel.add(titleLabel);
        
        JButton saveBtn = UIUtils.createSuccessButton("Save (F5)", actionEvent -> this.handleSave());
        JButton deleteBtn = UIUtils.createDangerButton("Delete (F7)", actionEvent -> this.handleDelete());
        JButton findBtn = UIUtils.createButton("Find (Ctrl+F)", actionEvent -> this.handleFind());
        findBtn.setBackground(UIConstants.ACCENT_COLOR);
        findBtn.setForeground(UIConstants.DARK_COLOR);
        
        panel.add(saveBtn);
        panel.add(deleteBtn);
        panel.add(findBtn);
        return panel;
    }

    private void handleSave() {
        String name = this.itemNameField.getText().trim();
        String code = this.itemCodeField.getText().trim();
        String pRateStr = this.pRateField.getText().trim();
        String sRateStr = this.sRate1Field.getText().trim();
        String barcode = this.barcodeField.getText().trim();
        
        if (name.isEmpty() || code.isEmpty() || pRateStr.isEmpty() || sRateStr.isEmpty()) {
            UIUtils.showErrorDialog(this, "Validation Error", "Please fill in all mandatory fields (*)\n- Item Name\n- Item Code\n- Purchase Rate\n- Selling Rate");
            return;
        }
        
        double pRate = 0;
        double sRate = 0;
        try {
            pRate = Double.parseDouble(pRateStr);
            sRate = Double.parseDouble(sRateStr);
        } catch (NumberFormatException e) {
            UIUtils.showErrorDialog(this, "Validation Error", "Purchase and Selling rates must be valid numbers.");
            return;
        }
        
        if (pRate < 0 || sRate < 0) {
            UIUtils.showErrorDialog(this, "Validation Error", "Rates/Prices cannot be negative.");
            return;
        }
        
        if (sRate < pRate) {
            if (!UIUtils.showConfirmDialog(this, "Warning", "Selling rate is less than purchase rate. Do you wish to continue?")) {
                return;
            }
        }
        
        boolean success;
        if (itemToEdit == null) {
            // Check if itemCode already exists
            ItemMaster existing = ItemMasterDAO.getAllItems().stream()
                .filter(i -> i.getItemCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
            if (existing != null) {
                UIUtils.showErrorDialog(this, "Duplicate Code", "Item Code already exists: " + code);
                return;
            }
            
            ItemMaster newItem = new ItemMaster();
            newItem.setItemCode(code);
            newItem.setItemName(name);
            newItem.setCategory(this.categoryCombo.getSelectedItem() != null ? this.categoryCombo.getSelectedItem().toString() : "DEFAULT");
            newItem.setManufacturer(this.manufacturerCombo.getSelectedItem() != null ? this.manufacturerCombo.getSelectedItem().toString() : "");
            newItem.setPurchasePrice(pRate);
            newItem.setSellingPrice(sRate);
            newItem.setBarcode(barcode.isEmpty() ? code : barcode);
            newItem.setStockQuantity(0);
            newItem.setReorderLevel(10);
            newItem.setStatus("ACTIVE");
            newItem.setCreatedDate(new Date());
            
            int newId = ItemMasterDAO.addItem(newItem);
            success = newId > 0;
            if (success) {
                AuditLogDAO.log(-1, "CREATE_ITEM", "item_master", newId, null, "Code: " + code + ", Name: " + name);
            }
        } else {
            itemToEdit.setItemName(name);
            itemToEdit.setCategory(this.categoryCombo.getSelectedItem() != null ? this.categoryCombo.getSelectedItem().toString() : "DEFAULT");
            itemToEdit.setManufacturer(this.manufacturerCombo.getSelectedItem() != null ? this.manufacturerCombo.getSelectedItem().toString() : "");
            itemToEdit.setPurchasePrice(pRate);
            itemToEdit.setSellingPrice(sRate);
            itemToEdit.setBarcode(barcode.isEmpty() ? code : barcode);
            itemToEdit.setStatus(this.activeStatusCombo.getSelectedItem() != null ? this.activeStatusCombo.getSelectedItem().toString() : "ACTIVE");
            
            success = ItemMasterDAO.updateItem(itemToEdit);
            if (success) {
                AuditLogDAO.log(-1, "UPDATE_ITEM", "item_master", itemToEdit.getItemId(), null, "Code: " + code);
            }
        }
        
        if (success) {
            this.saved = true;
            UIUtils.showSuccessDialog(this, "Success", "Item saved successfully.");
            this.dispose();
        } else {
            UIUtils.showErrorDialog(this, "Save Error", "Failed to save the item to database.");
        }
    }

    private void handleDelete() {
        if (itemToEdit != null) {
            if (UIUtils.showConfirmDialog(this, "Confirm Delete", "Are you sure you want to deactivate this Item record?")) {
                if (ItemMasterDAO.deleteItem(itemToEdit.getItemId())) {
                    AuditLogDAO.log(-1, "DELETE_ITEM", "item_master", itemToEdit.getItemId(), "ACTIVE", "INACTIVE");
                    UIUtils.showSuccessDialog(this, "Success", "Item record deactivated.");
                    this.saved = true;
                    this.dispose();
                } else {
                    UIUtils.showErrorDialog(this, "Error", "Failed to deactivate item.");
                }
            }
        } else {
            this.dispose();
        }
    }

    private void handleFind() {
        String search = JOptionPane.showInputDialog(this, "Enter Item Code / Barcode to find:", "Find Item", JOptionPane.QUESTION_MESSAGE);
        if (search != null && !search.trim().isEmpty()) {
            ItemMaster item = ItemMasterDAO.getItemByBarcode(search.trim());
            if (item == null) {
                item = ItemMasterDAO.getAllItems().stream()
                    .filter(i -> i.getItemCode().equalsIgnoreCase(search.trim()))
                    .findFirst().orElse(null);
            }
            
            if (item != null) {
                this.itemToEdit = item;
                populateFields();
                UIUtils.showSuccessDialog(this, "Item Loaded", "Item record found and loaded.");
            } else {
                UIUtils.showErrorDialog(this, "Not Found", "No item found with barcode/code: " + search);
            }
        }
    }

    private void setupKeyboardShortcuts() {
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(116, 0), "saveAction");
        this.getRootPane().getActionMap().put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ItemEntryDialog.this.handleSave();
            }
        });
        
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(118, 0), "deleteAction");
        this.getRootPane().getActionMap().put("deleteAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ItemEntryDialog.this.handleDelete();
            }
        });
        
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke(27, 0), "escapeAction");
        this.getRootPane().getActionMap().put("escapeAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ItemEntryDialog.this.dispose();
            }
        });
    }

    public boolean isSaved() {
        return this.saved;
    }
}
