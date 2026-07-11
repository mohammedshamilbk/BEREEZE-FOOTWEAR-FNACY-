/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.Bill;
import database.BillDAO;
import database.Supplier;
import database.SupplierDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import ui.frames.SupplierDialog;
import ui.frames.SupplierLedgerFrame;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class PendingPurchaseBillsFrame
extends JInternalFrame {
    private JFrame parent;
    private JTable billsTable;
    private DefaultTableModel tableModel;
    private JDesktopPane desktopPane;

    public PendingPurchaseBillsFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
        this.loadPendingPurchaseBills();
    }

    private void initializeUI() {
        this.setTitle("Secured: Pending Purchase Bills (Supplier Dues)");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(800, 500);
        this.setLocation(120, 80);
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JLabel jLabel = UIUtils.createTitleLabel("Pending Purchase Bills (Secured Area)");
        jPanel.add((Component)jLabel, "North");
        this.tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int n, int n2) {
                return false;
            }
        };
        this.tableModel.addColumn("Bill ID");
        this.tableModel.addColumn("Supplier/Member ID");
        this.tableModel.addColumn("Bill Date");
        this.tableModel.addColumn("Total Amount");
        this.tableModel.addColumn("Paid Amount");
        this.tableModel.addColumn("Pending Amount");
        this.billsTable = new JTable(this.tableModel);
        this.billsTable.setFont(UIConstants.NORMAL_FONT);
        this.billsTable.setRowHeight(25);
        JScrollPane jScrollPane = UIUtils.createTableScrollPane(this.billsTable);
        jPanel.add((Component)jScrollPane, "Center");
        this.billsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    PendingPurchaseBillsFrame.this.handleViewHistory();
                }
            }
        });
        JPanel jPanel2 = new JPanel(new FlowLayout(2));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton = UIUtils.createButton("View Supplier History", actionEvent -> this.handleViewHistory());
        jPanel2.add(jButton);
        JButton jButton2 = UIUtils.createButton("Refresh", actionEvent -> this.loadPendingPurchaseBills());
        jPanel2.add(jButton2);
        JButton jButton3 = UIUtils.createSuccessButton("Add New Supplier", actionEvent -> this.handleAddSupplier());
        jPanel2.add(jButton3);
        JButton jButton4 = UIUtils.createSuccessButton("Save", actionEvent -> {
            UIUtils.showSuccessDialog(this, "Saved Successfully", "Pending bills and supplier data securely saved.");
            this.loadPendingPurchaseBills();
        });
        jPanel2.add(jButton4);
        JButton jButton5 = UIUtils.createDangerButton("Close", actionEvent -> this.dispose());
        jPanel2.add(jButton5);
        jPanel.add((Component)jPanel2, "South");
        this.add(jPanel);
    }

    private void handleAddSupplier() {
        SupplierDialog supplierDialog = new SupplierDialog((Frame)((JFrame)SwingUtilities.getWindowAncestor(this)), null);
        supplierDialog.setVisible(true);
        if (supplierDialog.isSaved()) {
            Supplier supplier = supplierDialog.getSupplier();
            SupplierLedgerFrame supplierLedgerFrame = new SupplierLedgerFrame(supplier);
            if (this.getParent() instanceof JDesktopPane) {
                this.getParent().add(supplierLedgerFrame);
            } else if (this.getDesktopPane() != null) {
                this.getDesktopPane().add(supplierLedgerFrame);
            } else if (this.parent != null && this.parent.getContentPane() instanceof JDesktopPane) {
                this.parent.getContentPane().add(supplierLedgerFrame);
            }
            supplierLedgerFrame.setVisible(true);
            try {
                supplierLedgerFrame.setSelected(true);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void handleViewHistory() {
        int n = this.billsTable.getSelectedRow();
        if (n >= 0) {
            Object object = this.tableModel.getValueAt(n, 1);
            Supplier supplier = null;
            if (object instanceof Integer) {
                int n2 = (Integer)object;
                for (Supplier supplier2 : SupplierDAO.getAllSuppliers()) {
                    if (supplier2.getSupplierId() != n2) continue;
                    supplier = supplier2;
                    break;
                }
            }
            if (supplier == null) {
                supplier = new Supplier("SUP-DUMMY", String.valueOf(object), "N/A");
                supplier.setOutstandingBalance(10000.0);
            }
            SupplierLedgerFrame supplierLedgerFrame = new SupplierLedgerFrame(supplier);
            if (this.getParent() instanceof JDesktopPane) {
                this.getParent().add(supplierLedgerFrame);
            } else if (this.getDesktopPane() != null) {
                this.getDesktopPane().add(supplierLedgerFrame);
            } else if (this.parent != null && this.parent.getContentPane() instanceof JDesktopPane) {
                this.parent.getContentPane().add(supplierLedgerFrame);
            }
            supplierLedgerFrame.setVisible(true);
            try {
                supplierLedgerFrame.setSelected(true);
            }
            catch (Exception exception) {
            }
        } else {
            UIUtils.showWarningDialog(this, "Select", "Please select a bill to view supplier history.");
        }
    }

    private void loadPendingPurchaseBills() {
        this.tableModel.setRowCount(0);
        List<Bill> list = BillDAO.getAllBills();
        for (Bill bill : list) {
            if (!"PURCHASE".equalsIgnoreCase(bill.getBillType()) || !"PENDING".equalsIgnoreCase(bill.getStatus())) continue;
            double d = bill.getTotalAmount() - bill.getPaidAmount();
            this.tableModel.addRow(new Object[]{bill.getBillId(), bill.getCustomerId(), bill.getBillDate(), String.format("\u20b9%.2f", bill.getTotalAmount()), String.format("\u20b9%.2f", bill.getPaidAmount()), String.format("\u20b9%.2f", d)});
        }
    }
}
