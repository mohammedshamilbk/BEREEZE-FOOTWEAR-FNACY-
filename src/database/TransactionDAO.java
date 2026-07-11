package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Transaction operations
 * Handles all database operations for transaction logging
 */
public class TransactionDAO {
    private static final Logger LOGGER = Logger.getLogger(TransactionDAO.class.getName());
    
    /**
     * Transaction class for mapping database records
     */
    public static class Transaction {
        private int transactionId;
        private int billId;
        private Date transactionDate;
        private double amount;
        private String paymentMode;
        private String status;
        
        public Transaction() {
            this.transactionDate = new Date();
            this.status = "SUCCESS";
        }
        
        public Transaction(int billId, double amount, String paymentMode) {
            this();
            this.billId = billId;
            this.amount = amount;
            this.paymentMode = paymentMode;
        }
        
        // Getters and Setters
        public int getTransactionId() { return transactionId; }
        public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
        
        public int getBillId() { return billId; }
        public void setBillId(int billId) { this.billId = billId; }
        
        public Date getTransactionDate() { return transactionDate; }
        public void setTransactionDate(Date transactionDate) { this.transactionDate = transactionDate; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public String getPaymentMode() { return paymentMode; }
        public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * DailySummary class for reporting daily sales grouped by payment mode
     */
    public static class DailySummary {
        private Date date;
        private double openingBalance;
        private double closingBalance;
        private double cashTotal;
        private double gpayTotal;
        private double cardTotal;
        private double bankTotal;
        private double creditTotal;
        private double grandTotal;
        private int billCount;
        
        // Getters and Setters
        public Date getDate() { return date; }
        public void setDate(Date date) { this.date = date; }
        
        public double getOpeningBalance() { return openingBalance; }
        public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }
        
        public double getClosingBalance() { return closingBalance; }
        public void setClosingBalance(double closingBalance) { this.closingBalance = closingBalance; }
        
        public double getCashTotal() { return cashTotal; }
        public void setCashTotal(double cashTotal) { this.cashTotal = cashTotal; }
        
        public double getGpayTotal() { return gpayTotal; }
        public void setGpayTotal(double gpayTotal) { this.gpayTotal = gpayTotal; }
        
        public double getCardTotal() { return cardTotal; }
        public void setCardTotal(double cardTotal) { this.cardTotal = cardTotal; }
        
        public double getBankTotal() { return bankTotal; }
        public void setBankTotal(double bankTotal) { this.bankTotal = bankTotal; }
        
        public double getCreditTotal() { return creditTotal; }
        public void setCreditTotal(double creditTotal) { this.creditTotal = creditTotal; }
        
        public double getGrandTotal() { return grandTotal; }
        public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }
        
        public int getBillCount() { return billCount; }
        public void setBillCount(int billCount) { this.billCount = billCount; }
    }
    
    /**
     * Add a new transaction record
     * 
     * @param transaction Transaction object to add
     * @return Generated transactionId, or -1 if failed
     */
    public static int addTransaction(Transaction transaction) {
        String sql = "INSERT INTO transaction (billId, transactionDate, amount, paymentMode, status) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, transaction.getBillId());
            pstmt.setTimestamp(2, new Timestamp(transaction.getTransactionDate().getTime()));
            pstmt.setDouble(3, transaction.getAmount());
            pstmt.setString(4, transaction.getPaymentMode());
            pstmt.setString(5, transaction.getStatus());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int transactionId = rs.getInt(1);
                        transaction.setTransactionId(transactionId);
                        LOGGER.log(Level.INFO, "Transaction added successfully with ID: " + transactionId);
                        return transactionId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding transaction: " + e.getMessage(), e);
        }
        return -1;
    }
    
    /**
     * Get transaction by ID
     * 
     * @param transactionId Transaction ID to retrieve
     * @return Transaction object or null if not found
     */
    public static Transaction getTransactionById(int transactionId) {
        String sql = "SELECT * FROM transaction WHERE transactionId=?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, transactionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transaction by ID: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get all transactions for a bill
     * 
     * @param billId Bill ID
     * @return List of Transaction objects
     */
    public static List<Transaction> getTransactionsByBill(int billId) {
        String sql = "SELECT * FROM transaction WHERE billId=? ORDER BY transactionDate DESC";
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, billId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + transactions.size() + " transactions for bill " + billId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions by bill: " + e.getMessage(), e);
        }
        return transactions;
    }
    
    /**
     * Get daily summary grouped by payment modes and including register balances.
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return List of DailySummary objects
     */
    public static List<DailySummary> getDailyPaymentModeSummary(Date fromDate, Date toDate) {
        String sql = "SELECT " +
                    "cr.register_date AS sale_date, " +
                    "cr.opening_balance, " +
                    "cr.closing_balance, " +
                    "COUNT(DISTINCT b.billId) AS bill_count, " +
                    "SUM(CASE WHEN UPPER(t.paymentMode) = 'CASH' THEN t.amount ELSE 0 END) AS cash_total, " +
                    "SUM(CASE WHEN UPPER(t.paymentMode) IN ('GPAY', 'UPI') THEN t.amount ELSE 0 END) AS gpay_total, " +
                    "SUM(CASE WHEN UPPER(t.paymentMode) = 'CARD' THEN t.amount ELSE 0 END) AS card_total, " +
                    "SUM(CASE WHEN UPPER(t.paymentMode) LIKE '%BANK%' OR UPPER(t.paymentMode) = 'CHEQUE' THEN t.amount ELSE 0 END) AS bank_total, " +
                    "SUM(CASE WHEN UPPER(t.paymentMode) = 'CREDIT' THEN t.amount ELSE 0 END) AS credit_total, " +
                    "SUM(t.amount) AS grand_total " +
                    "FROM cash_register cr " +
                    "LEFT JOIN transaction t ON DATE(t.transactionDate) = cr.register_date " +
                    "LEFT JOIN bill b ON t.billId = b.billId AND b.billType = 'SALES' AND b.status != 'CANCELLED' " +
                    "WHERE cr.register_date BETWEEN ? AND ? " +
                    "GROUP BY cr.register_date, cr.opening_balance, cr.closing_balance " +
                    "ORDER BY cr.register_date DESC";
                    
        List<DailySummary> summaries = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, new java.sql.Date(fromDate.getTime()));
            pstmt.setDate(2, new java.sql.Date(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DailySummary summary = new DailySummary();
                    summary.setDate(new java.util.Date(rs.getDate("sale_date").getTime()));
                    summary.setOpeningBalance(rs.getDouble("opening_balance"));
                    summary.setClosingBalance(rs.getDouble("closing_balance"));
                    summary.setBillCount(rs.getInt("bill_count"));
                    
                    // IF there are no bills on that day, SUM returns NULL which getDouble converts to 0.0
                    summary.setCashTotal(rs.getDouble("cash_total"));
                    summary.setGpayTotal(rs.getDouble("gpay_total"));
                    summary.setCardTotal(rs.getDouble("card_total"));
                    summary.setBankTotal(rs.getDouble("bank_total"));
                    summary.setCreditTotal(rs.getDouble("credit_total"));
                    summary.setGrandTotal(rs.getDouble("grand_total"));
                    
                    summaries.add(summary);
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + summaries.size() + " daily summaries.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving daily summaries: " + e.getMessage(), e);
        }
        return summaries;
    }
    
    /**
     * Get transactions within date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return List of Transaction objects
     */
    public static List<Transaction> getTransactionsByDate(Date fromDate, Date toDate) {
        String sql = "SELECT * FROM transaction WHERE transactionDate >= ? AND transactionDate <= ? " +
                    "ORDER BY transactionDate DESC";
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + transactions.size() + " transactions for date range");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions by date: " + e.getMessage(), e);
        }
        return transactions;
    }
    
    /**
     * Get transactions by payment mode
     * 
     * @param paymentMode Payment mode (CASH, CARD, CHEQUE, ONLINE, CREDIT)
     * @return List of Transaction objects
     */
    public static List<Transaction> getTransactionsByPaymentMode(String paymentMode) {
        String sql = "SELECT * FROM transaction WHERE paymentMode=? ORDER BY transactionDate DESC";
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, paymentMode);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
            LOGGER.log(Level.INFO, "Retrieved " + transactions.size() + " transactions for payment mode: " + paymentMode);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions by payment mode: " + e.getMessage(), e);
        }
        return transactions;
    }
    
    /**
     * Get total transaction amount for date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return Total transaction amount
     */
    public static double getTotalTransactionAmount(Date fromDate, Date toDate) {
        String sql = "SELECT SUM(amount) as totalAmount FROM transaction " +
                    "WHERE transactionDate >= ? AND transactionDate <= ? AND status='SUCCESS'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("totalAmount");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total transaction amount: " + e.getMessage(), e);
        }
        return 0.0;
    }
    
    /**
     * Get transaction count for date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return Count of transactions
     */
    public static int getTransactionCount(Date fromDate, Date toDate) {
        String sql = "SELECT COUNT(*) as transactionCount FROM transaction " +
                    "WHERE transactionDate >= ? AND transactionDate <= ? AND status='SUCCESS'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("transactionCount");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting transactions: " + e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * Get average transaction amount for date range
     * 
     * @param fromDate Start date
     * @param toDate End date
     * @return Average transaction amount
     */
    public static double getAverageTransactionAmount(Date fromDate, Date toDate) {
        String sql = "SELECT AVG(amount) as avgAmount FROM transaction " +
                    "WHERE transactionDate >= ? AND transactionDate <= ? AND status='SUCCESS'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avgAmount");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating average transaction amount: " + e.getMessage(), e);
        }
        return 0.0;
    }
    
    /**
     * Get total amount by payment mode for date range
     * 
     * @param paymentMode Payment mode
     * @param fromDate Start date
     * @param toDate End date
     * @return Total amount for payment mode
     */
    public static double getTotalByPaymentMode(String paymentMode, Date fromDate, Date toDate) {
        String sql = "SELECT SUM(amount) as totalAmount FROM transaction " +
                    "WHERE paymentMode=? AND transactionDate >= ? AND transactionDate <= ? " +
                    "AND status='SUCCESS'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, paymentMode);
            pstmt.setTimestamp(2, new Timestamp(fromDate.getTime()));
            pstmt.setTimestamp(3, new Timestamp(toDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("totalAmount");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total by payment mode: " + e.getMessage(), e);
        }
        return 0.0;
    }
    
    /**
     * Get all failed transactions
     * 
     * @return List of failed Transaction objects
     */
    public static List<Transaction> getFailedTransactions() {
        String sql = "SELECT * FROM transaction WHERE status='FAILED' ORDER BY transactionDate DESC";
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
            LOGGER.log(Level.INFO, "Retrieved " + transactions.size() + " failed transactions");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving failed transactions: " + e.getMessage(), e);
        }
        return transactions;
    }
    
    /**
     * Map ResultSet row to Transaction object
     * 
     * @param rs ResultSet to map
     * @return Transaction object
     * @throws SQLException
     */
    private static Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(rs.getInt("transactionId"));
        transaction.setBillId(rs.getInt("billId"));
        transaction.setTransactionDate(new Date(rs.getTimestamp("transactionDate").getTime()));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setPaymentMode(rs.getString("paymentMode"));
        transaction.setStatus(rs.getString("status"));
        return transaction;
    }
}
