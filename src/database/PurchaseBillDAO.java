package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PurchaseBillDAO {
    private static final Logger LOGGER = Logger.getLogger(PurchaseBillDAO.class.getName());

    public static int addPurchaseBill(PurchaseBill bill, List<PurchaseBillItem> items) {
        String insertBillSql = "INSERT INTO purchase_bill (bill_number, supplier_id, purchase_date, total_amount, paid_amount, balance_due, status, created_by) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO purchase_bill_item (purchase_bill_id, item_id, quantity, purchase_price, gst, line_total) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            int purchaseBillId = -1;
            
            // Insert Purchase Bill
            try (PreparedStatement pstmt = conn.prepareStatement(insertBillSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, bill.getBillNumber());
                pstmt.setInt(2, bill.getSupplierId());
                pstmt.setTimestamp(3, new Timestamp(bill.getPurchaseDate().getTime()));
                pstmt.setDouble(4, bill.getTotalAmount());
                pstmt.setDouble(5, bill.getPaidAmount());
                pstmt.setDouble(6, bill.getBalanceDue());
                pstmt.setString(7, bill.getStatus());
                pstmt.setInt(8, bill.getCreatedBy());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating purchase bill failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        purchaseBillId = generatedKeys.getInt(1);
                        bill.setPurchaseBillId(purchaseBillId);
                    } else {
                        throw new SQLException("Creating purchase bill failed, no ID obtained.");
                    }
                }
            }

            // Insert Items and update stock
            try (PreparedStatement pstmt = conn.prepareStatement(insertItemSql)) {
                for (PurchaseBillItem item : items) {
                    pstmt.setInt(1, purchaseBillId);
                    pstmt.setInt(2, item.getItemId());
                    pstmt.setInt(3, item.getQuantity());
                    pstmt.setDouble(4, item.getPurchasePrice());
                    pstmt.setDouble(5, item.getGst());
                    pstmt.setDouble(6, item.getLineTotal());
                    pstmt.addBatch();

                    // Increase stock quantity
                    boolean stockUpdated = ItemMasterDAO.updateStockQuantity(item.getItemId(), item.getQuantity());
                    if (!stockUpdated) {
                        throw new SQLException("Failed to update stock for item ID: " + item.getItemId());
                    }
                }
                pstmt.executeBatch();
            }

            // Update supplier outstanding balance
            SupplierDAO.updateOutstandingBalance(bill.getSupplierId(), bill.getBalanceDue());

            conn.commit();
            return purchaseBillId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction: " + ex.getMessage(), ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Error adding purchase bill: " + e.getMessage(), e);
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit or closing connection: " + ex.getMessage(), ex);
                }
            }
        }
    }

    public static List<PurchaseBill> getAllPurchaseBills() {
        return fetchPurchaseBills("SELECT pb.*, s.supplierName FROM purchase_bill pb JOIN supplier s ON pb.supplier_id = s.supplierId ORDER BY pb.purchase_date DESC");
    }

    public static List<PurchaseBill> getPendingPurchaseBills() {
        return fetchPurchaseBills("SELECT pb.*, s.supplierName FROM purchase_bill pb JOIN supplier s ON pb.supplier_id = s.supplierId WHERE pb.status IN ('PENDING', 'PARTIAL') ORDER BY pb.purchase_date DESC");
    }

    public static int getPendingPurchaseBillsCount() {
        String sql = "SELECT COUNT(*) AS billCount FROM purchase_bill WHERE status IN ('PENDING', 'PARTIAL')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("billCount");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting pending purchase bills count: " + e.getMessage(), e);
        }
        return 0;
    }

    public static PurchaseBill getPurchaseBillById(int id) {
        String sql = "SELECT pb.*, s.supplierName FROM purchase_bill pb JOIN supplier s ON pb.supplier_id = s.supplierId WHERE pb.purchase_bill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPurchaseBill(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving purchase bill by ID: " + e.getMessage(), e);
        }
        return null;
    }

    private static List<PurchaseBill> fetchPurchaseBills(String sql) {
        List<PurchaseBill> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPurchaseBill(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching purchase bills: " + e.getMessage(), e);
        }
        return list;
    }

    private static PurchaseBill mapResultSetToPurchaseBill(ResultSet rs) throws SQLException {
        PurchaseBill bill = new PurchaseBill();
        bill.setPurchaseBillId(rs.getInt("purchase_bill_id"));
        bill.setBillNumber(rs.getString("bill_number"));
        bill.setSupplierId(rs.getInt("supplier_id"));
        bill.setSupplierName(rs.getString("supplierName"));
        bill.setPurchaseDate(rs.getTimestamp("purchase_date"));
        bill.setTotalAmount(rs.getDouble("total_amount"));
        bill.setPaidAmount(rs.getDouble("paid_amount"));
        bill.setBalanceDue(rs.getDouble("balance_due"));
        bill.setStatus(rs.getString("status"));
        bill.setCreatedBy(rs.getInt("created_by"));
        bill.setCreatedAt(rs.getTimestamp("created_at"));
        return bill;
    }

    public static boolean recordPayment(int purchaseBillId, double amount, String paymentMode, String referenceNote, int userId) {
        PurchaseBill bill = getPurchaseBillById(purchaseBillId);
        if (bill == null) return false;

        if (amount > bill.getBalanceDue()) {
            amount = bill.getBalanceDue(); // prevent overpayment
        }

        double newPaid = bill.getPaidAmount() + amount;
        double newBalance = bill.getTotalAmount() - newPaid;
        String newStatus = newBalance <= 0 ? "PAID" : "PARTIAL";

        String insertPayment = "INSERT INTO purchase_payment (purchase_bill_id, amount, payment_mode, reference_note, paid_by) VALUES (?, ?, ?, ?, ?)";
        String updateBill = "UPDATE purchase_bill SET paid_amount = ?, balance_due = ?, status = ? WHERE purchase_bill_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt1 = conn.prepareStatement(insertPayment)) {
                pstmt1.setInt(1, purchaseBillId);
                pstmt1.setDouble(2, amount);
                pstmt1.setString(3, paymentMode);
                pstmt1.setString(4, referenceNote);
                pstmt1.setInt(5, userId);
                pstmt1.executeUpdate();
            }

            try (PreparedStatement pstmt2 = conn.prepareStatement(updateBill)) {
                pstmt2.setDouble(1, newPaid);
                pstmt2.setDouble(2, newBalance);
                pstmt2.setString(3, newStatus);
                pstmt2.setInt(4, purchaseBillId);
                pstmt2.executeUpdate();
            }

            // Update supplier outstanding balance (decrease it)
            SupplierDAO.updateOutstandingBalance(bill.getSupplierId(), -amount);

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back payment: " + ex.getMessage(), ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Error recording payment: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error restoring connection state: " + ex.getMessage(), ex);
                }
            }
        }
    }

    public static List<PurchasePayment> getPaymentsForBill(int purchaseBillId) {
        List<PurchasePayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM purchase_payment WHERE purchase_bill_id = ? ORDER BY payment_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, purchaseBillId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PurchasePayment p = new PurchasePayment();
                    p.setPaymentId(rs.getInt("payment_id"));
                    p.setPurchaseBillId(rs.getInt("purchase_bill_id"));
                    p.setAmount(rs.getDouble("amount"));
                    p.setPaymentMode(rs.getString("payment_mode"));
                    p.setPaymentDate(rs.getTimestamp("payment_date"));
                    p.setReferenceNote(rs.getString("reference_note"));
                    p.setPaidBy(rs.getInt("paid_by"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));
                    payments.add(p);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching payments: " + e.getMessage(), e);
        }
        return payments;
    }
}
