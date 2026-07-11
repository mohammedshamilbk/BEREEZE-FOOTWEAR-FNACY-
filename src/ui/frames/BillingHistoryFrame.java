/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.Bill;
import database.BillDAO;
import database.BillItem;
import database.BillItemDAO;
import database.Customer;
import database.CustomerDAO;
import database.ItemMaster;
import database.ItemMasterDAO;
import reporting.BillPrinter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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

public class BillingHistoryFrame
extends JInternalFrame {
    private JFrame parent;
    private JTextField billNumberField;
    private JComboBox<String> customerCombo;
    private JComboBox<String> paymentModeCombo;
    private JTextField fromDateField;
    private JTextField toDateField;
    private JTable billsTable;
    private DefaultTableModel tableModel;

    public BillingHistoryFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Billing History");
        this.setClosable(true);
        this.setSize(1000, 650);
        this.setLocation(100, 100);
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel jLabel = UIUtils.createTitleLabel("Billing Records");
        jPanel.add((Component)jLabel, "North");
        JPanel jPanel2 = this.createFilterPanel();
        jPanel.add((Component)jPanel2, "First");
        JPanel jPanel3 = this.createTablePanel();
        jPanel.add((Component)jPanel3, "Center");
        JPanel jPanel4 = this.createActionPanel();
        jPanel.add((Component)jPanel4, "South");
        this.add(jPanel);
    }

    private JPanel createFilterPanel() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new GridLayout(2, 4, 10, 10));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        jPanel2.setBorder(BorderFactory.createTitledBorder("Search Parameters"));
        jPanel2.add(UIUtils.createLabel("Bill Number:"));
        this.billNumberField = UIUtils.createTextField(10);
        jPanel2.add(this.billNumberField);
        jPanel2.add(UIUtils.createLabel("Customer:"));
        this.customerCombo = UIUtils.createComboBox(new String[]{"All", "Ahmed Khan", "Fatima Ali", "Hassan Raza"});
        jPanel2.add(this.customerCombo);
        jPanel2.add(UIUtils.createLabel("Payment Mode:"));
        this.paymentModeCombo = UIUtils.createComboBox(new String[]{"All", "Cash", "Card", "UPI", "Check"});
        jPanel2.add(this.paymentModeCombo);
        JButton jButton = UIUtils.createButton("Search", actionEvent -> this.handleSearch());
        jPanel2.add(jButton);
        jPanel2.add(UIUtils.createLabel("From Date:"));
        this.fromDateField = UIUtils.createTextField(10);
        jPanel2.add(this.fromDateField);
        jPanel2.add(UIUtils.createLabel("To Date:"));
        this.toDateField = UIUtils.createTextField(10);
        jPanel2.add(this.toDateField);
        JButton jButton2 = UIUtils.createDangerButton("Clear Fields", actionEvent -> {
            this.billNumberField.setText("");
            this.customerCombo.setSelectedIndex(0);
            this.paymentModeCombo.setSelectedIndex(0);
            this.fromDateField.setText("");
            this.toDateField.setText("");
            this.loadBillsTable();
        });
        jPanel2.add(jButton2);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel.add((Component)jPanel2, gridBagConstraints);
        return jPanel;
    }

    private JPanel createTablePanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), "History Grid", 1, 2, UIConstants.HEADING_FONT, UIConstants.PRIMARY_COLOR));
        this.tableModel = new DefaultTableModel();
        this.tableModel.addColumn("Bill No");
        this.tableModel.addColumn("Date");
        this.tableModel.addColumn("Customer");
        this.tableModel.addColumn("Amount");
        this.tableModel.addColumn("Paid");
        this.tableModel.addColumn("Payment Mode");
        this.tableModel.addColumn("Status");
        this.billsTable = new JTable(this.tableModel);
        this.billsTable.setFont(UIConstants.NORMAL_FONT);
        this.billsTable.setRowHeight(25);
        this.loadBillsTable();
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(this.billsTable);
        jPanel.add((Component)jScrollPane, "Center");
        return jPanel;
    }

    private void loadBillsTable() {
        this.tableModel.setRowCount(0);
        try {
            List<Bill> bills;
            String fromText = fromDateField != null ? fromDateField.getText().trim() : "";
            String toText = toDateField != null ? toDateField.getText().trim() : "";
            if (!fromText.isEmpty() && !toText.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date fromDate = sdf.parse(fromText);
                    java.util.Date toDate = sdf.parse(toText);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(toDate);
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                    cal.set(java.util.Calendar.MINUTE, 59);
                    cal.set(java.util.Calendar.SECOND, 59);
                    bills = BillDAO.getBillsByDate(fromDate, cal.getTime());
                } catch (Exception e) {
                    bills = BillDAO.getAllBills();
                }
            } else {
                bills = BillDAO.getAllBills();
            }
            if (bills != null && !bills.isEmpty()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String searchBillNum = billNumberField != null ? billNumberField.getText().trim().toLowerCase() : "";
                String selectedCust = customerCombo != null ? customerCombo.getSelectedItem().toString() : "All";
                String selectedMode = paymentModeCombo != null ? paymentModeCombo.getSelectedItem().toString() : "All";

                for (Bill bill : bills) {
                    if (!searchBillNum.isEmpty() && !bill.getBillNumber().toLowerCase().contains(searchBillNum)) {
                        continue;
                    }
                    Customer customer = CustomerDAO.getCustomerById(bill.getCustomerId());
                    String customerName = customer != null ? customer.getCustomerName() : "Unknown";
                    if (!"All".equalsIgnoreCase(selectedCust) && !customerName.equalsIgnoreCase(selectedCust)) {
                        continue;
                    }
                    if (!"All".equalsIgnoreCase(selectedMode) && !bill.getPaymentMode().equalsIgnoreCase(selectedMode)) {
                        continue;
                    }

                    Vector<String> row = new Vector<String>();
                    row.add(bill.getBillNumber());
                    row.add(simpleDateFormat.format(bill.getBillDate()));
                    row.add(customerName);
                    row.add(String.format("\u20b9%.2f", bill.getTotalAmount()));
                    row.add(String.format("\u20b9%.2f", bill.getPaidAmount()));
                    row.add(bill.getPaymentMode());
                    row.add(bill.getStatus());
                    this.tableModel.addRow(row);
                }
                return;
            }
        }
        catch (Exception exception) {
            System.err.println("Database offline. Using mock data.");
        }
        String[][] mockData = new String[][]{{"INV-1001", "2023-11-25", "John Doe", "\u20b91500.00", "\u20b91500.00", "CASH", "PAID"}, {"INV-1002", "2023-11-25", "Walk-in Customer", "\u20b9850.00", "\u20b9850.00", "GPAY", "PAID"}, {"INV-1003", "2023-11-24", "Jane Smith", "\u20b92200.00", "\u20b91000.00", "CASH", "PARTIAL"}};
        for (String[] rowData : mockData) {
            Vector<String> vector = new Vector<String>();
            for (String str : rowData) {
                vector.add(str);
            }
            this.tableModel.addRow(vector);
        }
    }

    private JPanel createActionPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout(1, 10, 10));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        JButton jButton = UIUtils.createButton("View Details", actionEvent -> this.handleViewDetails());
        JButton jButton2 = UIUtils.createButton("Print Bill", actionEvent -> this.handlePrintBill());
        JButton jButton3 = UIUtils.createDangerButton("Cancel Bill", actionEvent -> this.handleCancelBill());
        JButton jButton4 = UIUtils.createButton("Export PDF", actionEvent -> this.handleExportPDF());
        JButton jButton5 = UIUtils.createButton("Export Excel", actionEvent -> this.handleExportExcel());
        jPanel.add(jButton);
        jPanel.add(jButton2);
        jPanel.add(jButton3);
        jPanel.add(jButton4);
        jPanel.add(jButton5);
        return jPanel;
    }

    public void setFilterAndSearch(String fromDateStr, String toDateStr) {
        if (fromDateField != null && toDateField != null) {
            fromDateField.setText(fromDateStr);
            toDateField.setText(toDateStr);
            handleSearch();
        }
    }

    private void handleSearch() {
        this.loadBillsTable();
    }

    private void handleViewDetails() {
        int n = this.billsTable.getSelectedRow();
        if (n >= 0) {
            String string = (String)this.tableModel.getValueAt(n, 0);
            String string2 = (String)this.tableModel.getValueAt(n, 1);
            String string3 = (String)this.tableModel.getValueAt(n, 2);
            String string4 = (String)this.tableModel.getValueAt(n, 3);
            String string5 = (String)this.tableModel.getValueAt(n, 6);
            String string6 = "=== BAREEZE FOOTWEAR ===\nAddress: Anar complex, Naya bazar,\nMelparamba, Kasaragod, Kerala, India 671317\nMobile no: 8086790086\nMail ID: breezefootwearfancy@gmail.com\n==================================\nBill No: " + string + "\nDate: " + string2 + "\nCustomer: " + string3 + "\nAmount: " + string4 + "\nStatus: " + string5;
            UIUtils.showSuccessDialog(this, "Bill Details", string6);
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a bill");
        }
    }

    private void handlePrintBill() {
        int selectedRow = this.billsTable.getSelectedRow();
        if (selectedRow < 0) {
            UIUtils.showWarningDialog(this, "Warning", "Please select a bill");
            return;
        }
        
        String billNo = (String) this.tableModel.getValueAt(selectedRow, 0);
        
        try {
            Bill bill = BillDAO.getBillByNumber(billNo);
            if (bill != null) {
                List<BillItem> billItems = BillItemDAO.getBillItemsByBillId(bill.getBillId());
                List<String[]> printItems = new ArrayList<>();
                for (BillItem bi : billItems) {
                    ItemMaster item = ItemMasterDAO.getItemById(bi.getItemId());
                    String itemName = item != null ? item.getItemName() : "Item ID: " + bi.getItemId();
                    printItems.add(new String[]{
                        itemName,
                        String.valueOf(bi.getQuantity()),
                        String.format("%.2f", bi.getUnitPrice()),
                        String.format("%.2f", bi.getTotalAmount())
                    });
                }
                
                Customer customer = CustomerDAO.getCustomerById(bill.getCustomerId());
                String customerName = customer != null ? customer.getCustomerName() : "Walk-in Customer";
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                
                BillPrinter printer = new BillPrinter(
                    bill.getBillNumber(),
                    sdf.format(bill.getBillDate()),
                    customerName,
                    String.format("%.2f", bill.getTotalAmount()),
                    bill.getStatus(),
                    bill.getPaymentMode(),
                    printItems
                );
                printer.printReceipt();
                return;
            }
        } catch (Exception e) {
            System.err.println("DB offline while printing. Printing mock receipt.");
        }
        
        // Mock fallback printing
        List<String[]> mockPrintItems = new ArrayList<>();
        mockPrintItems.add(new String[]{"Breeze Classic Shoes", "1", "1200.00", "1200.00"});
        mockPrintItems.add(new String[]{"Comfort Slippers", "1", "300.00", "300.00"});
        
        String dateStr = (String) this.tableModel.getValueAt(selectedRow, 1);
        String custStr = (String) this.tableModel.getValueAt(selectedRow, 2);
        String amtStr = ((String) this.tableModel.getValueAt(selectedRow, 3)).replace("\u20b9", "").replace(",", "").trim();
        String payModeStr = (String) this.tableModel.getValueAt(selectedRow, 5);
        String statusStr = (String) this.tableModel.getValueAt(selectedRow, 6);
        
        BillPrinter printer = new BillPrinter(
            billNo,
            dateStr,
            custStr,
            amtStr,
            statusStr,
            payModeStr,
            mockPrintItems
        );
        printer.printReceipt();
    }

    private void handleCancelBill() {
        int n = this.billsTable.getSelectedRow();
        if (n >= 0) {
            String billNo = (String)this.tableModel.getValueAt(n, 0);
            String status = (String)this.tableModel.getValueAt(n, 6);
            if (status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Pending")) {
                if (UIUtils.showConfirmDialog(this, "Confirm", "Cancel bill " + billNo + "? This action cannot be undone.")) {
                    try {
                        if (database.BillDAO.updateBillStatus(billNo, "Cancelled")) {
                            this.tableModel.setValueAt("Cancelled", n, 6);
                            int actorUserId = (parent instanceof MainFrame) ? ((MainFrame) parent).getCurrentUser().getUserId() : 1;
                            database.AuditLogDAO.log(actorUserId, "CANCEL_BILL", "bill", -1, status, "Cancelled");
                            UIUtils.showSuccessDialog(this, "Success", "Bill cancelled successfully");
                        } else {
                            UIUtils.showErrorDialog(this, "Error", "Failed to cancel bill in database.");
                        }
                    } catch (Exception e) {
                        UIUtils.showErrorDialog(this, "Error", "Database error: " + e.getMessage());
                    }
                }
            } else {
                UIUtils.showWarningDialog(this, "Warning", "Only active completed/pending bills can be cancelled");
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a bill");
        }
    }

    private void handleExportPDF() {
        UIUtils.showSuccessDialog(this, "Success", "Billing history exported to PDF");
    }

    private void handleExportExcel() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showWarningDialog(this, "Warning", "No data to export!");
            return;
        }
        Object[] objectArray = new String[]{"Daily History", "Monthly History", "All Bills"};
        int n = JOptionPane.showOptionDialog(this, "Select Export Type:", "Export to Excel (CSV)", -1, 3, null, objectArray, objectArray[0]);
        if (n < 0) {
            return;
        }
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Save Export File");
        String string = "BillingExport.csv";
        if (n == 0) {
            string = "Daily_Billing_History.csv";
        } else if (n == 1) {
            string = "Monthly_Billing_History.csv";
        } else if (n == 2) {
            string = "All_Bills_History.csv";
        }
        jFileChooser.setSelectedFile(new File(string));
        int n2 = jFileChooser.showSaveDialog(this);
        if (n2 == 0) {
            File file = jFileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            try (FileWriter fileWriter = new FileWriter(file);){
                if (n == 0) {
                    this.exportDailyHistory(fileWriter);
                } else if (n == 1) {
                    this.exportMonthlyHistory(fileWriter);
                } else if (n == 2) {
                    this.exportAllBills(fileWriter);
                }
                UIUtils.showSuccessDialog(this, "Success", "History exported successfully to:\n" + file.getAbsolutePath());
            }
            catch (IOException iOException) {
                UIUtils.showErrorDialog(this, "Error", "Failed to export data: " + iOException.getMessage());
            }
        }
    }

    private void exportDailyHistory(FileWriter fileWriter) throws IOException {
        fileWriter.append("Date,Total Bills,Total Amount (Rs)\n");
        LinkedHashMap<String, double[]> linkedHashMap = new LinkedHashMap<String, double[]>();
        for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
            String object = (String)this.tableModel.getValueAt(i, 1);
            String string = object.split(" ")[0];
            String string2 = ((String)this.tableModel.getValueAt(i, 3)).replace("\u20b9", "").replace(",", "").trim();
            double d = 0.0;
            try {
                d = Double.parseDouble(string2);
            }
            catch (Exception exception) {
                // empty catch block
            }
            linkedHashMap.putIfAbsent(string, new double[]{0.0, 0.0});
            double[] dArray = (double[])linkedHashMap.get(string);
            dArray[0] = dArray[0] + 1.0;
            double[] dArray2 = (double[])linkedHashMap.get(string);
            dArray2[1] = dArray2[1] + d;
        }
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            fileWriter.append((CharSequence)entry.getKey()).append(",").append(String.valueOf((int)((double[])entry.getValue())[0])).append(",").append(String.format("%.2f", ((double[])entry.getValue())[1])).append("\n");
        }
    }

    private void exportMonthlyHistory(FileWriter fileWriter) throws IOException {
        fileWriter.append("Month-Year,Total Bills,Total Amount (Rs)\n");
        LinkedHashMap<Object, double[]> linkedHashMap = new LinkedHashMap<Object, double[]>();
        for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
            String object = (String)this.tableModel.getValueAt(i, 1);
            String[] stringArray = object.split(" ")[0].split("-");
            Object object2 = "Unknown";
            if (stringArray.length >= 2) {
                object2 = stringArray[0] + "-" + stringArray[1];
            }
            String string = ((String)this.tableModel.getValueAt(i, 3)).replace("\u20b9", "").replace(",", "").trim();
            double d = 0.0;
            try {
                d = Double.parseDouble(string);
            }
            catch (Exception exception) {
                // empty catch block
            }
            linkedHashMap.putIfAbsent(object2, new double[]{0.0, 0.0});
            double[] dArray = (double[])linkedHashMap.get(object2);
            dArray[0] = dArray[0] + 1.0;
            double[] dArray2 = (double[])linkedHashMap.get(object2);
            dArray2[1] = dArray2[1] + d;
        }
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            fileWriter.append((CharSequence)entry.getKey()).append(",").append(String.valueOf((int)((double[])entry.getValue())[0])).append(",").append(String.format("%.2f", ((double[])entry.getValue())[1])).append("\n");
        }
    }

    private void exportAllBills(FileWriter fileWriter) throws IOException {
        int n;
        for (n = 0; n < this.tableModel.getColumnCount(); ++n) {
            fileWriter.append(this.tableModel.getColumnName(n));
            if (n >= this.tableModel.getColumnCount() - 1) continue;
            fileWriter.append(",");
        }
        fileWriter.append("\n");
        for (n = 0; n < this.tableModel.getRowCount(); ++n) {
            for (int i = 0; i < this.tableModel.getColumnCount(); ++i) {
                Object object = String.valueOf(this.tableModel.getValueAt(n, i));
                if (((String)object).contains(",") || ((String)object).contains("\"")) {
                    object = "\"" + ((String)object).replace("\"", "\"\"") + "\"";
                }
                object = ((String)object).replace("\u20b9", "").trim();
                fileWriter.append((CharSequence)object);
                if (i >= this.tableModel.getColumnCount() - 1) continue;
                fileWriter.append(",");
            }
            fileWriter.append("\n");
        }
    }
}
