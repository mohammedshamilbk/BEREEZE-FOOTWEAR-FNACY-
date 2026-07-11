-- Bereeze Footwear POS System - MySQL Database Schema
-- Version 1.0
-- Created: 2026-06-05

-- Drop existing database if it exists (for fresh setup)
-- DROP DATABASE IF EXISTS bereeze_pos;
-- CREATE DATABASE bereeze_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE bereeze_pos;

-- =====================================================
-- TABLE: item_master
-- Description: Inventory management for products
-- =====================================================
CREATE TABLE IF NOT EXISTS item_master (
    itemId INT PRIMARY KEY AUTO_INCREMENT,
    itemCode VARCHAR(50) NOT NULL UNIQUE,
    itemName VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    purchasePrice DECIMAL(10, 2) NOT NULL,
    sellingPrice DECIMAL(10, 2) NOT NULL,
    barcode VARCHAR(50) UNIQUE,
    stockQuantity INT NOT NULL DEFAULT 0,
    reorderLevel INT NOT NULL DEFAULT 10,
    size VARCHAR(20),
    color VARCHAR(30),
    material VARCHAR(50),
    createdDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifiedDate DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    INDEX idx_itemCode (itemCode),
    INDEX idx_barcode (barcode),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: customer
-- Description: Customer master data with credit management
-- =====================================================
CREATE TABLE IF NOT EXISTS customer (
    customerId INT PRIMARY KEY AUTO_INCREMENT,
    customerCode VARCHAR(50) NOT NULL UNIQUE,
    customerName VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(200),
    city VARCHAR(50),
    state VARCHAR(50),
    pincode VARCHAR(10),
    creditLimit DECIMAL(12, 2) NOT NULL DEFAULT 0,
    outstandingAmount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    customerType VARCHAR(30) DEFAULT 'REGULAR',
    loyaltyPoints DECIMAL(10, 2) NOT NULL DEFAULT 0,
    registrationDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastPurchaseDate DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    INDEX idx_customerCode (customerCode),
    INDEX idx_phone (phone),
    INDEX idx_customerType (customerType),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: user
-- Description: System users with authentication
-- =====================================================
CREATE TABLE IF NOT EXISTS user (
    userId INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    fullName VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    createdDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    dailySalesTarget DECIMAL(12, 2) DEFAULT 0,
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: bill
-- Description: Main billing/invoice records
-- =====================================================
CREATE TABLE IF NOT EXISTS bill (
    billId INT PRIMARY KEY AUTO_INCREMENT,
    billNumber VARCHAR(50) NOT NULL UNIQUE,
    billType VARCHAR(50) NOT NULL,
    billDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customerId INT,
    supplierId INT,
    userId INT NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    totalDiscount DECIMAL(12, 2) DEFAULT 0,
    totalAmount DECIMAL(12, 2) NOT NULL,
    paidAmount DECIMAL(12, 2) DEFAULT 0,
    paymentMode VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    remarks TEXT,
    createdDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customerId) REFERENCES customer(customerId) ON DELETE SET NULL,
    FOREIGN KEY (userId) REFERENCES user(userId) ON DELETE RESTRICT,
    INDEX idx_billNumber (billNumber),
    INDEX idx_billType (billType),
    INDEX idx_billDate (billDate),
    INDEX idx_customerId (customerId),
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_paymentMode (paymentMode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: bill_item
-- Description: Line items in each bill
-- =====================================================
CREATE TABLE IF NOT EXISTS bill_item (
    billItemId INT PRIMARY KEY AUTO_INCREMENT,
    billId INT NOT NULL,
    itemId INT NOT NULL,
    quantity INT NOT NULL,
    unitPrice DECIMAL(10, 2) NOT NULL,
    discount DECIMAL(10, 2) DEFAULT 0,
    totalAmount DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (billId) REFERENCES bill(billId) ON DELETE CASCADE,
    FOREIGN KEY (itemId) REFERENCES item_master(itemId) ON DELETE RESTRICT,
    INDEX idx_billId (billId),
    INDEX idx_itemId (itemId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: transaction
-- Description: Transaction logging for payments
-- =====================================================
CREATE TABLE IF NOT EXISTS transaction (
    transactionId INT PRIMARY KEY AUTO_INCREMENT,
    billId INT NOT NULL,
    transactionDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(12, 2) NOT NULL,
    paymentMode VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    FOREIGN KEY (billId) REFERENCES bill(billId) ON DELETE CASCADE,
    INDEX idx_billId (billId),
    INDEX idx_transactionDate (transactionDate),
    INDEX idx_paymentMode (paymentMode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: supplier
-- Description: Supplier master data
-- =====================================================
CREATE TABLE IF NOT EXISTS supplier (
    supplierId INT PRIMARY KEY AUTO_INCREMENT,
    supplierCode VARCHAR(50) NOT NULL UNIQUE,
    supplierName VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    state VARCHAR(50),
    taxRegn VARCHAR(50),
    gstin VARCHAR(50),
    outstandingBalance DECIMAL(12, 2) NOT NULL DEFAULT 0,
    createdDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    INDEX idx_supplierCode (supplierCode),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: supplier_transaction
-- Description: Payments made to suppliers
-- =====================================================
CREATE TABLE IF NOT EXISTS supplier_transaction (
    transactionId INT PRIMARY KEY AUTO_INCREMENT,
    supplierId INT NOT NULL,
    billId INT,
    transactionDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(12, 2) NOT NULL,
    paymentMode VARCHAR(30),
    type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT',
    remarks TEXT,
    FOREIGN KEY (supplierId) REFERENCES supplier(supplierId) ON DELETE CASCADE,
    FOREIGN KEY (billId) REFERENCES bill(billId) ON DELETE SET NULL,
    INDEX idx_supplierId (supplierId),
    INDEX idx_transactionDate (transactionDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: audit_log
-- Description: Track all changes to critical tables
-- =====================================================
CREATE TABLE IF NOT EXISTS audit_log (
    logId INT PRIMARY KEY AUTO_INCREMENT,
    userId INT,
    action VARCHAR(50) NOT NULL,
    tableName VARCHAR(50) NOT NULL,
    recordId INT NOT NULL,
    oldValue TEXT,
    newValue TEXT,
    actionDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userId) REFERENCES user(userId) ON DELETE SET NULL,
    INDEX idx_userId (userId),
    INDEX idx_tableName (tableName),
    INDEX idx_actionDate (actionDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Initial Data Inserts (Optional)
-- =====================================================

-- Insert default admin user (password: admin123)
INSERT IGNORE INTO user (userId, username, password, fullName, role, email, status) 
VALUES (1, 'admin', 'admin123', 'Administrator', 'ADMIN', 'admin@bereeze.com', 'ACTIVE');

-- Insert sample cashier user (password: cashier123)
INSERT IGNORE INTO user (userId, username, password, fullName, role, email, status) 
VALUES (2, 'cashier', 'cashier123', 'Cashier User', 'CASHIER', 'cashier@bereeze.com', 'ACTIVE');

-- =====================================================
-- Sample Data (Uncomment to use)
-- =====================================================

-- Sample Item Master Data
/*
-- INSERT INTO item_master (itemCode, itemName, category, manufacturer, purchasePrice, sellingPrice, barcode, stockQuantity, size, color, material, status)
-- VALUES 
-- ('SHOE001', 'Premium Running Shoe', 'Sports', 'Nike', 2000.00, 3999.00, '9876543210128', 50, '10', 'Black', 'Mesh', 'ACTIVE'),
-- ('SHOE002', 'Casual Leather Shoe', 'Casual', 'Bata', 1500.00, 2499.00, '9876543210129', 30, '9', 'Brown', 'Leather', 'ACTIVE'),
-- ('SAND001', 'Comfort Sandal', 'Sandals', 'Relaxo', 500.00, 899.00, '9876543210130', 100, '8', 'White', 'Rubber', 'ACTIVE');
*/

-- Sample Customer Data
/*
INSERT INTO customer (customerCode, customerName, phone, email, city, state, creditLimit, status)
VALUES 
('CUST001', 'Rajesh Kumar', '9876543210', 'rajesh@email.com', 'Mumbai', 'Maharashtra', 50000, 'ACTIVE'),
('CUST002', 'Priya Singh', '9876543211', 'priya@email.com', 'Delhi', 'Delhi', 75000, 'ACTIVE');
*/

-- =====================================================
-- TABLE: cash_register
-- Description: Daily cash register opening and closing balances
-- =====================================================
CREATE TABLE IF NOT EXISTS cash_register (
    register_id INT PRIMARY KEY AUTO_INCREMENT,
    register_date DATE NOT NULL UNIQUE,
    opening_balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
    closing_balance DECIMAL(12, 2) NOT NULL DEFAULT 0,
    cash_sales DECIMAL(12, 2) NOT NULL DEFAULT 0,
    cash_in DECIMAL(12, 2) NOT NULL DEFAULT 0,
    cash_out DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_by INT,
    closed_by INT,
    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at DATETIME,
    FOREIGN KEY (opened_by) REFERENCES user(userId) ON DELETE SET NULL,
    FOREIGN KEY (closed_by) REFERENCES user(userId) ON DELETE SET NULL,
    INDEX idx_register_date (register_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table structure for table `purchase_bill`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchase_bill (
    purchase_bill_id INT AUTO_INCREMENT PRIMARY KEY,
    bill_number VARCHAR(50) NOT NULL UNIQUE,
    supplier_id INT NOT NULL,
    purchase_date DATETIME NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    balance_due DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES supplier(supplierId),
    FOREIGN KEY (created_by) REFERENCES user(userId),
    INDEX idx_pur_bill_number (bill_number),
    INDEX idx_pur_supplier (supplier_id),
    INDEX idx_pur_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table structure for table `purchase_bill_item`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchase_bill_item (
    purchase_bill_item_id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_bill_id INT NOT NULL,
    item_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    purchase_price DECIMAL(10, 2) NOT NULL,
    gst DECIMAL(5, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (purchase_bill_id) REFERENCES purchase_bill(purchase_bill_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES item_master(itemId),
    INDEX idx_pur_item_bill (purchase_bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table structure for table `purchase_payment`
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchase_payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_bill_id INT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference_note TEXT,
    paid_by INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (purchase_bill_id) REFERENCES purchase_bill(purchase_bill_id) ON DELETE CASCADE,
    FOREIGN KEY (paid_by) REFERENCES user(userId),
    INDEX idx_pur_payment_bill (purchase_bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

COMMIT;
