package ui.frames;

import database.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PurchaseBillFrame extends JInternalFrame {
    private JFrame parent;
    private JTable billsTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilterCombo;
    private JTextField searchField;
    private JButton makePaymentBtn;
    private JButton viewHistoryBtn;
    private List<PurchaseBill> allBillsList = new ArrayList<>();

    public PurchaseBillFrame(JFrame parent) {
        this.parent = parent;
        this.initializeUI();
        this.loadPurchaseBills();
    }

    private void initializeUI() {
        this.setTitle("Purchase Bills Management");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1000, 600);
        this.setLocation(50, 50);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header Title
        JLabel titleLabel = UIUtils.createTitleLabel("Purchase Bills Tracking");
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Control / Filter Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        controlPanel.setBackground(UIConstants.APP_BACKGROUND);

        controlPanel.add(UIUtils.createLabel("Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"All", "Pending", "Partial", "Paid"});
        statusFilterCombo.setFont(UIConstants.NORMAL_FONT);
        statusFilterCombo.setPreferredSize(new Dimension(120, 30));
        statusFilterCombo.addActionListener(e -> filterBills());
        controlPanel.add(statusFilterCombo);

        controlPanel.add(UIUtils.createLabel("Search:"));
        searchField = UIUtils.createTextField(15);
        searchField.setPreferredSize(new Dimension(180, 30));
        searchField.setToolTipText("Search by supplier name or bill number");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterBills(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterBills(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterBills(); }
        });
        controlPanel.add(searchField);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // Bills Table
        String[] columns = {"ID", "Bill Number", "Supplier", "Purchase Date", "Total Amount", "Paid Amount", "Balance Due", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        billsTable = new JTable(tableModel);
        billsTable.setFont(UIConstants.NORMAL_FONT);
        billsTable.setRowHeight(25);
        billsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom renderer for Status Column color coding
        billsTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String status = value.toString();
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                        c.setForeground(table.getSelectionForeground());
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        if ("PENDING".equalsIgnoreCase(status)) {
                            c.setBackground(UIConstants.STATUS_DANGER_BG);
                            c.setForeground(UIConstants.STATUS_DANGER_FG);
                            setFont(new Font("Segoe UI", Font.BOLD, 12));
                        } else if ("PARTIAL".equalsIgnoreCase(status)) {
                            c.setBackground(UIConstants.STATUS_WARNING_BG);
                            c.setForeground(UIConstants.STATUS_WARNING_FG);
                            setFont(new Font("Segoe UI", Font.BOLD, 12));
                        } else if ("PAID".equalsIgnoreCase(status)) {
                            c.setBackground(UIConstants.STATUS_SUCCESS_BG);
                            c.setForeground(UIConstants.STATUS_SUCCESS_FG);
                            setFont(new Font("Segoe UI", Font.BOLD, 12));
                        } else {
                            c.setBackground(table.getBackground());
                            c.setForeground(UIConstants.DARK_COLOR);
                            setFont(UIConstants.NORMAL_FONT);
                        }
                    }
                }
                return c;
            }
        });

        billsTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = billsTable.getSelectedRow();
            if (selectedRow >= 0) {
                String status = tableModel.getValueAt(selectedRow, 7).toString();
                makePaymentBtn.setEnabled(!"PAID".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(status));
                viewHistoryBtn.setEnabled(true);
            } else {
                makePaymentBtn.setEnabled(false);
                viewHistoryBtn.setEnabled(false);
            }
        });

        billsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    viewPaymentHistory();
                }
            }
        });

        JScrollPane scrollPane = UIUtils.createTableScrollPane(billsTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Action Buttons Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        actionPanel.setBackground(UIConstants.APP_BACKGROUND);

        JButton addBillBtn = UIUtils.createSuccessButton("Add Purchase Bill", e -> openAddPurchaseBillDialog());
        addBillBtn.setPreferredSize(new Dimension(160, 35));
        actionPanel.add(addBillBtn);

        JButton manageSuppliersBtn = UIUtils.createButton("Manage Suppliers", e -> {
            if (parent != null) {
                ((MainFrame) parent).showFrame(new SupplierFrame(parent));
            }
        });
        manageSuppliersBtn.setBackground(UIConstants.SECONDARY_COLOR);
        manageSuppliersBtn.setForeground(Color.WHITE);
        manageSuppliersBtn.setPreferredSize(new Dimension(150, 35));
        actionPanel.add(manageSuppliersBtn);

        makePaymentBtn = UIUtils.createButton("Make Payment", e -> openMakePaymentDialog());
        makePaymentBtn.setBackground(UIConstants.ACCENT_COLOR);
        makePaymentBtn.setForeground(Color.WHITE);
        makePaymentBtn.setPreferredSize(new Dimension(130, 35));
        makePaymentBtn.setEnabled(false);
        actionPanel.add(makePaymentBtn);

        viewHistoryBtn = UIUtils.createButton("Payment History", e -> viewPaymentHistory());
        viewHistoryBtn.setBackground(UIConstants.SECONDARY_COLOR);
        viewHistoryBtn.setForeground(Color.WHITE);
        viewHistoryBtn.setPreferredSize(new Dimension(140, 35));
        viewHistoryBtn.setEnabled(false);
        actionPanel.add(viewHistoryBtn);

        JButton refreshBtn = UIUtils.createButton("Refresh", e -> loadPurchaseBills());
        refreshBtn.setPreferredSize(new Dimension(90, 35));
        actionPanel.add(refreshBtn);

        JButton closeBtn = UIUtils.createDangerButton("Close", e -> this.dispose());
        closeBtn.setPreferredSize(new Dimension(90, 35));
        actionPanel.add(closeBtn);

        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        // Assemble Top Header + Title
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(UIConstants.APP_BACKGROUND);
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(controlPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        this.add(mainPanel);
    }

    private void loadPurchaseBills() {
        tableModel.setRowCount(0);
        allBillsList = PurchaseBillDAO.getAllPurchaseBills();
        filterBills();
    }

    private void filterBills() {
        tableModel.setRowCount(0);
        String selectedStatus = statusFilterCombo.getSelectedItem().toString();
        String query = searchField.getText().trim().toLowerCase();

        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        for (PurchaseBill bill : allBillsList) {
            boolean matchesStatus = "All".equalsIgnoreCase(selectedStatus) || bill.getStatus().equalsIgnoreCase(selectedStatus);
            boolean matchesSearch = query.isEmpty() || 
                                    bill.getBillNumber().toLowerCase().contains(query) || 
                                    (bill.getSupplierName() != null && bill.getSupplierName().toLowerCase().contains(query));

            if (matchesStatus && matchesSearch) {
                tableModel.addRow(new Object[]{
                    bill.getPurchaseBillId(),
                    bill.getBillNumber(),
                    bill.getSupplierName() != null ? bill.getSupplierName() : "Supplier ID: " + bill.getSupplierId(),
                    bill.getPurchaseDate() != null ? df.format(bill.getPurchaseDate()) : "N/A",
                    "\u20b9" + nf.format(bill.getTotalAmount()),
                    "\u20b9" + nf.format(bill.getPaidAmount()),
                    "\u20b9" + nf.format(bill.getBalanceDue()),
                    bill.getStatus()
                });
            }
        }
    }

    private void openAddPurchaseBillDialog() {
        JDialog dialog = new JDialog(parent, "Record New Stock Purchase", true);
        dialog.setSize(860, 660);
        dialog.setMinimumSize(new Dimension(840, 620));
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIConstants.APP_BACKGROUND);
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Supplier selection
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(UIUtils.createLabel("Select Supplier:"), gbc);
        
        gbc.gridx = 1;
        JPanel supplierSelectPanel = new JPanel(new BorderLayout(5, 0));
        supplierSelectPanel.setBackground(UIConstants.APP_BACKGROUND);
        
        JComboBox<Supplier> supplierCombo = new JComboBox<>();
        List<Supplier> suppliersList = SupplierDAO.getAllSuppliers();
        if (suppliersList.isEmpty()) {
            System.err.println("[PurchaseBillFrame] No suppliers loaded — DB may be empty or offline.");
        }
        for (Supplier s : suppliersList) {
            supplierCombo.addItem(s);
        }
        supplierSelectPanel.add(supplierCombo, BorderLayout.CENTER);
        
        JButton quickAddSupplierBtn = UIUtils.createSuccessButton("+ Add", e -> {
            AddSupplierDialog addDialog = new AddSupplierDialog(parent, null, () -> {
                supplierCombo.removeAllItems();
                List<Supplier> updatedList = SupplierDAO.getAllSuppliers();
                Supplier newlyAdded = null;
                for (Supplier s : updatedList) {
                    supplierCombo.addItem(s);
                    if (newlyAdded == null || s.getSupplierId() > newlyAdded.getSupplierId()) {
                        newlyAdded = s;
                    }
                }
                if (newlyAdded != null) {
                    supplierCombo.setSelectedItem(newlyAdded);
                }
            });
            addDialog.setVisible(true);
        });
        quickAddSupplierBtn.setPreferredSize(new Dimension(80, 28));
        supplierSelectPanel.add(quickAddSupplierBtn, BorderLayout.EAST);
        
        formPanel.add(supplierSelectPanel, gbc);

        // Item entry panel — search by code/name/barcode
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(UIUtils.createLabel("Item Code:"), gbc);

        gbc.gridx = 1;
        JPanel itemSearchPanel = new JPanel(new BorderLayout(5, 0));
        itemSearchPanel.setBackground(UIConstants.APP_BACKGROUND);

        JTextField itemSearchField = UIUtils.createTextField(15);
        itemSearchField.setToolTipText("Enter item code, name, or barcode and press Enter or Search");
        itemSearchPanel.add(itemSearchField, BorderLayout.CENTER);

        JButton searchItemBtn = UIUtils.createButton("\uD83D\uDD0D Search", null);
        searchItemBtn.setPreferredSize(new Dimension(100, 30));
        itemSearchPanel.add(searchItemBtn, BorderLayout.EAST);

        formPanel.add(itemSearchPanel, gbc);

        // Item details label (shows selected item info)
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel itemDetailsLabel = new JLabel(" ");
        itemDetailsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemDetailsLabel.setForeground(UIConstants.ACCENT_COLOR);
        itemDetailsLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        formPanel.add(itemDetailsLabel, gbc);
        gbc.gridwidth = 1;

        // Declare costField early so the search lambda can reference it
        JTextField costField = UIUtils.createTextField(10);

        // Selected item holder (single-element array to allow mutation from lambda)
        final ItemMaster[] selectedItem = {null};

        // Search action
        Runnable searchAction = () -> {
            String query = itemSearchField.getText().trim();
            if (query.isEmpty()) {
                UIUtils.showWarningDialog(dialog, "Search", "Please enter an item code, name, or barcode.");
                return;
            }

            // First try exact barcode match
            ItemMaster found = ItemMasterDAO.getItemByBarcode(query);
            if (found != null) {
                selectedItem[0] = found;
                itemDetailsLabel.setText("\u2705 " + found.getItemCode() + " — " + found.getItemName()
                    + "  |  Size: " + found.getSize() + "  |  Color: " + found.getColor()
                    + "  |  Price: \u20b9" + String.format("%.2f", found.getPurchasePrice()));
                costField.setText(String.format("%.2f", found.getPurchasePrice()));
                return;
            }

            // Search by code/name/category
            List<ItemMaster> results = ItemMasterDAO.searchItems(query);
            if (results.isEmpty()) {
                selectedItem[0] = null;
                itemDetailsLabel.setText("\u274C No items found for: \"" + query + "\"");
                costField.setText("");
                UIUtils.showWarningDialog(dialog, "Not Found", "No items match \"" + query + "\".\nTry a different code, name, or barcode.");
            } else if (results.size() == 1) {
                // Single result — auto-select
                selectedItem[0] = results.get(0);
                itemDetailsLabel.setText("\u2705 " + selectedItem[0].getItemCode() + " — " + selectedItem[0].getItemName()
                    + "  |  Size: " + selectedItem[0].getSize() + "  |  Color: " + selectedItem[0].getColor()
                    + "  |  Price: \u20b9" + String.format("%.2f", selectedItem[0].getPurchasePrice()));
                costField.setText(String.format("%.2f", selectedItem[0].getPurchasePrice()));
            } else {
                // Multiple results — show chooser
                String[] options = new String[results.size()];
                for (int i = 0; i < results.size(); i++) {
                    ItemMaster im = results.get(i);
                    options[i] = im.getItemCode() + " — " + im.getItemName()
                        + " (" + im.getSize() + ", " + im.getColor() + ") \u20b9" + String.format("%.2f", im.getPurchasePrice());
                }
                String chosen = (String) JOptionPane.showInputDialog(dialog,
                    "Multiple items found (" + results.size() + "). Select one:",
                    "Select Item", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                if (chosen != null) {
                    int idx = java.util.Arrays.asList(options).indexOf(chosen);
                    selectedItem[0] = results.get(idx);
                    itemDetailsLabel.setText("\u2705 " + selectedItem[0].getItemCode() + " — " + selectedItem[0].getItemName()
                        + "  |  Size: " + selectedItem[0].getSize() + "  |  Color: " + selectedItem[0].getColor()
                        + "  |  Price: \u20b9" + String.format("%.2f", selectedItem[0].getPurchasePrice()));
                    costField.setText(String.format("%.2f", selectedItem[0].getPurchasePrice()));
                }
            }
        };

        searchItemBtn.addActionListener(e -> searchAction.run());
        itemSearchField.addActionListener(e -> searchAction.run()); // Enter key

        // Cost and Quantity
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(UIUtils.createLabel("Cost Price (\u20b9):"), gbc);

        gbc.gridx = 1;
        formPanel.add(costField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(UIUtils.createLabel("Quantity:"), gbc);

        gbc.gridx = 1;
        JTextField qtyField = UIUtils.createTextField(10);
        qtyField.setText("1");
        formPanel.add(qtyField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(UIUtils.createLabel("GST (%):"), gbc);

        gbc.gridx = 1;
        JTextField gstField = UIUtils.createTextField(10);
        gstField.setText("18.00");
        formPanel.add(gstField, gbc);

        // Item details table in dialog
        String[] tblCols = {"Item ID", "Item Code", "Item Name", "Qty", "Cost Price", "GST %", "Line Total"};
        DefaultTableModel dlgTableModel = new DefaultTableModel(tblCols, 0) {
            @Override
            public boolean isCellEditable(int r, int cl) { return false; }
        };
        JTable dlgTable = new JTable(dlgTableModel);
        dlgTable.setRowHeight(24);
        dlgTable.setFont(UIConstants.NORMAL_FONT);
        // Use explicit widths so columns are readable — disable auto-stretch mode
        dlgTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Hide Item ID column (internal key only)
        dlgTable.getColumnModel().getColumn(0).setMinWidth(0);
        dlgTable.getColumnModel().getColumn(0).setMaxWidth(0);
        dlgTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        // Readable widths for the visible columns
        dlgTable.getColumnModel().getColumn(1).setPreferredWidth(90);  // Item Code
        dlgTable.getColumnModel().getColumn(2).setPreferredWidth(220); // Item Name
        dlgTable.getColumnModel().getColumn(3).setPreferredWidth(55);  // Qty
        dlgTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Cost Price
        dlgTable.getColumnModel().getColumn(5).setPreferredWidth(70);  // GST %
        dlgTable.getColumnModel().getColumn(6).setPreferredWidth(110); // Line Total
        JScrollPane dlgTableScroll = new JScrollPane(dlgTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dlgTableScroll.setPreferredSize(new Dimension(760, 160));

        // Add item button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JButton addItemBtn = UIUtils.createSuccessButton("Add Line Item", e -> {
            ItemMaster selItem = selectedItem[0];
            if (selItem == null) {
                UIUtils.showErrorDialog(dialog, "Selection Error", "Please search and select an item first.");
                return;
            }
            try {
                int qty = Integer.parseInt(qtyField.getText().trim());
                double cost = Double.parseDouble(costField.getText().trim());
                double gstPct = Double.parseDouble(gstField.getText().trim());
                if (qty <= 0 || cost < 0 || gstPct < 0) {
                    UIUtils.showErrorDialog(dialog, "Validation Error", "Quantity and cost must be positive.");
                    return;
                }
                double tax = cost * qty * (gstPct / 100.0);
                double lineTotal = (cost * qty) + tax;

                dlgTableModel.addRow(new Object[]{
                    selItem.getItemId(),
                    selItem.getItemCode(),
                    selItem.getItemName(),
                    qty,
                    String.format("%.2f", cost),
                    String.format("%.2f", gstPct),
                    String.format("%.2f", lineTotal)
                });

                // Clear for next item entry
                selectedItem[0] = null;
                itemSearchField.setText("");
                itemDetailsLabel.setText(" ");
                costField.setText("");
                qtyField.setText("1");
                itemSearchField.requestFocusInWindow();
            } catch (NumberFormatException ex) {
                UIUtils.showErrorDialog(dialog, "Input Error", "Please enter valid numbers for cost, quantity, and GST.");
            }
        });
        formPanel.add(addItemBtn, gbc);

        gbc.gridy = 7; gbc.gridwidth = 2;
        formPanel.add(dlgTableScroll, gbc);

        // Bottom dialog buttons & Total
        JPanel bottomDlgPanel = new JPanel(new BorderLayout(10, 10));
        bottomDlgPanel.setBackground(UIConstants.APP_BACKGROUND);
        bottomDlgPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));

        JLabel grandTotalLabel = new JLabel("Grand Total: \u20b90.00");
        grandTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        grandTotalLabel.setForeground(UIConstants.ACCENT_COLOR);
        bottomDlgPanel.add(grandTotalLabel, BorderLayout.WEST);

        dlgTableModel.addTableModelListener(e -> {
            double total = 0.0;
            for (int i = 0; i < dlgTableModel.getRowCount(); i++) {
                total += Double.parseDouble(dlgTableModel.getValueAt(i, 6).toString());
            }
            grandTotalLabel.setText(String.format("Grand Total: \u20b9%,.2f", total));
        });

        JPanel dlgBtnContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        dlgBtnContainer.setBackground(UIConstants.APP_BACKGROUND);

        JButton saveBillBtn = UIUtils.createSuccessButton("Save Bill", e -> {
            Supplier selSup = (Supplier) supplierCombo.getSelectedItem();
            if (selSup == null) {
                UIUtils.showErrorDialog(dialog, "Save Error", "Please select a Supplier.");
                return;
            }
            if (dlgTableModel.getRowCount() == 0) {
                UIUtils.showErrorDialog(dialog, "Save Error", "Cannot save an empty purchase bill.");
                return;
            }

            double total = 0.0;
            List<PurchaseBillItem> billItems = new ArrayList<>();
            for (int i = 0; i < dlgTableModel.getRowCount(); i++) {
                int itemId = (Integer) dlgTableModel.getValueAt(i, 0);
                int qty = (Integer) dlgTableModel.getValueAt(i, 3);
                double price = Double.parseDouble(dlgTableModel.getValueAt(i, 4).toString());
                double gst = Double.parseDouble(dlgTableModel.getValueAt(i, 5).toString());
                double lineTotal = Double.parseDouble(dlgTableModel.getValueAt(i, 6).toString());
                total += lineTotal;

                PurchaseBillItem bi = new PurchaseBillItem();
                bi.setItemId(itemId);
                bi.setQuantity(qty);
                bi.setPurchasePrice(price);
                bi.setGst(gst);
                bi.setLineTotal(lineTotal);
                billItems.add(bi);
            }

            PurchaseBill bill = new PurchaseBill();
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String billNumber = "PUR-" + dateStr + "-" + String.format("%05d", (int)(Math.random() * 100000));
            bill.setBillNumber(billNumber);
            bill.setSupplierId(selSup.getSupplierId());
            bill.setPurchaseDate(new Date());
            bill.setTotalAmount(total);
            bill.setPaidAmount(0.0);
            bill.setBalanceDue(total);
            bill.setStatus("PENDING");
            bill.setCreatedBy(1); // Default Admin User ID

            int result = PurchaseBillDAO.addPurchaseBill(bill, billItems);
            if (result > 0) {
                UIUtils.showSuccessDialog(dialog, "Success", "Purchase Bill " + billNumber + " saved successfully!");
                dialog.dispose();
                loadPurchaseBills();
            } else {
                UIUtils.showErrorDialog(dialog, "Database Error", "Failed to save purchase bill to database.");
            }
        });
        dlgBtnContainer.add(saveBillBtn);

        JButton cancelBillBtn = UIUtils.createDangerButton("Cancel", e -> dialog.dispose());
        dlgBtnContainer.add(cancelBillBtn);

        bottomDlgPanel.add(dlgBtnContainer, BorderLayout.EAST);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(bottomDlgPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void openMakePaymentDialog() {
        int selectedRow = billsTable.getSelectedRow();
        if (selectedRow < 0) {
            UIUtils.showWarningDialog(this, "Select Bill", "Please select a purchase bill first.");
            return;
        }

        int billId = (Integer) tableModel.getValueAt(selectedRow, 0);
        PurchaseBill bill = PurchaseBillDAO.getPurchaseBillById(billId);
        if (bill == null) return;

        JDialog dialog = new JDialog(parent, "Record Supplier Payment", true);
        dialog.setSize(400, 330);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(UIConstants.APP_BACKGROUND);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(UIUtils.createLabel("Bill Number:"), gbc);
        gbc.gridx = 1;
        JLabel lblBillNum = new JLabel(bill.getBillNumber());
        lblBillNum.setFont(UIConstants.HEADING_FONT);
        dialog.add(lblBillNum, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(UIUtils.createLabel("Balance Due:"), gbc);
        gbc.gridx = 1;
        JLabel lblBalDue = new JLabel(String.format("\u20b9%,.2f", bill.getBalanceDue()));
        lblBalDue.setFont(UIConstants.HEADING_FONT);
        lblBalDue.setForeground(UIConstants.DANGER_COLOR);
        dialog.add(lblBalDue, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(UIUtils.createLabel("Payment Amount (\u20b9):"), gbc);
        gbc.gridx = 1;
        JTextField amountField = UIUtils.createTextField(10);
        amountField.setText(String.format("%.2f", bill.getBalanceDue()));
        dialog.add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        dialog.add(UIUtils.createLabel("Payment Mode:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> modeCombo = new JComboBox<>(new String[]{"CASH", "GPAY", "BANK TO BANK"});
        modeCombo.setFont(UIConstants.NORMAL_FONT);
        dialog.add(modeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        dialog.add(UIUtils.createLabel("Ref Note / UTR:"), gbc);
        gbc.gridx = 1;
        JTextField refField = UIUtils.createTextField(10);
        dialog.add(refField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(UIConstants.APP_BACKGROUND);

        JButton confirmBtn = UIUtils.createSuccessButton("Confirm Payment", e -> {
            try {
                double amt = Double.parseDouble(amountField.getText().trim());
                if (amt <= 0) {
                    UIUtils.showErrorDialog(dialog, "Validation Error", "Amount must be greater than zero.");
                    return;
                }
                if (amt > bill.getBalanceDue()) {
                    UIUtils.showErrorDialog(dialog, "Validation Error", "Amount cannot exceed Balance Due.");
                    return;
                }
                String mode = modeCombo.getSelectedItem().toString();
                String ref = refField.getText().trim();

                boolean success = PurchaseBillDAO.recordPayment(bill.getPurchaseBillId(), amt, mode, ref, 1);
                if (success) {
                    UIUtils.showSuccessDialog(dialog, "Success", "Payment of \u20b9" + amt + " recorded successfully!");
                    dialog.dispose();
                    loadPurchaseBills();
                } else {
                    UIUtils.showErrorDialog(dialog, "Payment Error", "Failed to register payment.");
                }
            } catch (NumberFormatException ex) {
                UIUtils.showErrorDialog(dialog, "Input Error", "Please enter a valid amount.");
            }
        });
        btnPanel.add(confirmBtn);

        JButton cancelBtn = UIUtils.createDangerButton("Cancel", e -> dialog.dispose());
        btnPanel.add(cancelBtn);

        dialog.add(btnPanel, gbc);
        dialog.setVisible(true);
    }

    private void viewPaymentHistory() {
        int selectedRow = billsTable.getSelectedRow();
        if (selectedRow < 0) {
            UIUtils.showWarningDialog(this, "Select Bill", "Please select a purchase bill to view payment history.");
            return;
        }

        int billId = (Integer) tableModel.getValueAt(selectedRow, 0);
        PurchaseBill bill = PurchaseBillDAO.getPurchaseBillById(billId);
        if (bill == null) return;

        List<PurchasePayment> payments = PurchaseBillDAO.getPaymentsForBill(billId);

        JDialog dialog = new JDialog(parent, "Payment History - Bill: " + bill.getBillNumber(), true);
        dialog.setSize(550, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        contentPanel.setBackground(UIConstants.APP_BACKGROUND);

        JLabel infoLabel = new JLabel("<html><b>Bill Total:</b> \u20b9" + String.format("%,.2f", bill.getTotalAmount()) +
                                      "  |  <b>Paid:</b> \u20b9" + String.format("%,.2f", bill.getPaidAmount()) +
                                      "  |  <b>Due:</b> \u20b9" + String.format("%,.2f", bill.getBalanceDue()) + "</html>");
        infoLabel.setFont(UIConstants.NORMAL_FONT);
        contentPanel.add(infoLabel, BorderLayout.NORTH);

        String[] cols = {"Payment ID", "Date", "Amount", "Mode", "Reference / Note"};
        DefaultTableModel pmModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm a");
        for (PurchasePayment p : payments) {
            pmModel.addRow(new Object[]{
                p.getPaymentId(),
                p.getPaymentDate() != null ? sdf.format(p.getPaymentDate()) : "N/A",
                "\u20b9" + String.format("%,.2f", p.getAmount()),
                p.getPaymentMode(),
                p.getReferenceNote() != null ? p.getReferenceNote() : ""
            });
        }

        JTable pmTable = new JTable(pmModel);
        pmTable.setRowHeight(22);
        pmTable.setFont(UIConstants.NORMAL_FONT);
        JScrollPane pmScroll = UIUtils.createTableScrollPane(pmTable);
        contentPanel.add(pmScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(UIConstants.APP_BACKGROUND);
        JButton closeBtn = UIUtils.createButton("OK", e -> dialog.dispose());
        closeBtn.setPreferredSize(new Dimension(80, 30));
        btnPanel.add(closeBtn);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
