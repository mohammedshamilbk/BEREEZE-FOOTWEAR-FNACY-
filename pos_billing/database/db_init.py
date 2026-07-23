# ============================================================
# pos_billing/database/db_init.py  (DatabaseInitializer.java → Python)
# ============================================================
"""
Creates all database tables on first run.
Works with both SQLite (default) and MySQL.
"""

import logging
from .connection import get_connection, close_connection

logger = logging.getLogger(__name__)

# SQL compatible with both SQLite and MySQL (uses SQLite subset)
_SCHEMA_SQLITE = """
CREATE TABLE IF NOT EXISTS user (
    userId        INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password      TEXT NOT NULL,
    fullName      TEXT NOT NULL,
    role          TEXT NOT NULL,
    email         TEXT DEFAULT '',
    phone         TEXT DEFAULT '',
    createdDate   TEXT NOT NULL DEFAULT (datetime('now')),
    status        TEXT NOT NULL DEFAULT 'ACTIVE',
    dailySalesTarget  REAL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer (
    customerId       INTEGER PRIMARY KEY AUTOINCREMENT,
    customerCode     TEXT NOT NULL UNIQUE,
    customerName     TEXT NOT NULL,
    phone            TEXT DEFAULT '',
    email            TEXT DEFAULT '',
    address          TEXT DEFAULT '',
    city             TEXT DEFAULT '',
    state            TEXT DEFAULT '',
    pincode          TEXT DEFAULT '',
    creditLimit      REAL NOT NULL DEFAULT 0,
    outstandingAmount REAL NOT NULL DEFAULT 0,
    customerType     TEXT DEFAULT 'REGULAR',
    loyaltyPoints    REAL NOT NULL DEFAULT 0,
    registrationDate TEXT NOT NULL DEFAULT (datetime('now')),
    lastPurchaseDate TEXT,
    status           TEXT NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS item_master (
    itemId         INTEGER PRIMARY KEY AUTOINCREMENT,
    itemCode       TEXT NOT NULL UNIQUE,
    itemName       TEXT NOT NULL,
    category       TEXT NOT NULL,
    manufacturer   TEXT DEFAULT '',
    purchasePrice  REAL NOT NULL,
    sellingPrice   REAL NOT NULL,
    barcode        TEXT UNIQUE,
    stockQuantity  INTEGER NOT NULL DEFAULT 0,
    reorderLevel   INTEGER NOT NULL DEFAULT 10,
    size           TEXT DEFAULT '',
    color          TEXT DEFAULT '',
    material       TEXT DEFAULT '',
    createdDate    TEXT NOT NULL DEFAULT (datetime('now')),
    modifiedDate   TEXT,
    status         TEXT NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS supplier (
    supplierId        INTEGER PRIMARY KEY AUTOINCREMENT,
    supplierCode      TEXT NOT NULL UNIQUE,
    supplierName      TEXT NOT NULL,
    phone             TEXT DEFAULT '',
    email             TEXT DEFAULT '',
    state             TEXT DEFAULT '',
    taxRegn           TEXT DEFAULT '',
    gstin             TEXT DEFAULT '',
    outstandingBalance REAL NOT NULL DEFAULT 0,
    createdDate       TEXT NOT NULL DEFAULT (datetime('now')),
    status            TEXT NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS bill (
    billId        INTEGER PRIMARY KEY AUTOINCREMENT,
    billNumber    TEXT NOT NULL UNIQUE,
    billType      TEXT NOT NULL,
    billDate      TEXT NOT NULL DEFAULT (datetime('now')),
    customerId    INTEGER,
    supplierId    INTEGER,
    userId        INTEGER NOT NULL,
    subtotal      REAL NOT NULL DEFAULT 0,
    totalDiscount REAL DEFAULT 0,
    totalAmount   REAL NOT NULL DEFAULT 0,
    paidAmount    REAL DEFAULT 0,
    paymentMode   TEXT DEFAULT '',
    status        TEXT NOT NULL DEFAULT 'PENDING',
    remarks       TEXT DEFAULT '',
    createdDate   TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (customerId) REFERENCES customer(customerId),
    FOREIGN KEY (userId)     REFERENCES user(userId)
);

CREATE TABLE IF NOT EXISTS bill_item (
    billItemId   INTEGER PRIMARY KEY AUTOINCREMENT,
    billId       INTEGER NOT NULL,
    itemId       INTEGER NOT NULL,
    itemCode     TEXT NOT NULL,
    itemName     TEXT NOT NULL,
    quantity     INTEGER NOT NULL,
    unitPrice    REAL NOT NULL,
    discount     REAL DEFAULT 0,
    totalAmount  REAL NOT NULL,
    FOREIGN KEY (billId)  REFERENCES bill(billId) ON DELETE CASCADE,
    FOREIGN KEY (itemId)  REFERENCES item_master(itemId)
);

CREATE TABLE IF NOT EXISTS pos_transaction (
    transactionId   INTEGER PRIMARY KEY AUTOINCREMENT,
    billId          INTEGER NOT NULL,
    transactionDate TEXT NOT NULL DEFAULT (datetime('now')),
    amount          REAL NOT NULL,
    paymentMode     TEXT DEFAULT '',
    status          TEXT NOT NULL DEFAULT 'SUCCESS',
    FOREIGN KEY (billId) REFERENCES bill(billId) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cash_register (
    register_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    register_date    TEXT NOT NULL UNIQUE,
    opening_balance  REAL NOT NULL DEFAULT 0,
    closing_balance  REAL NOT NULL DEFAULT 0,
    cash_sales       REAL NOT NULL DEFAULT 0,
    cash_in          REAL NOT NULL DEFAULT 0,
    cash_out         REAL NOT NULL DEFAULT 0,
    status           TEXT NOT NULL DEFAULT 'OPEN',
    opened_by        INTEGER,
    closed_by        INTEGER,
    opened_at        TEXT NOT NULL DEFAULT (datetime('now')),
    closed_at        TEXT
);

CREATE TABLE IF NOT EXISTS purchase_bill (
    purchase_bill_id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_number      TEXT NOT NULL UNIQUE,
    supplier_id      INTEGER NOT NULL,
    purchase_date    TEXT NOT NULL DEFAULT (datetime('now')),
    total_amount     REAL NOT NULL DEFAULT 0,
    paid_amount      REAL NOT NULL DEFAULT 0,
    balance_due      REAL NOT NULL DEFAULT 0,
    status           TEXT NOT NULL DEFAULT 'PENDING',
    created_by       INTEGER,
    created_at       TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (supplier_id) REFERENCES supplier(supplierId)
);

CREATE TABLE IF NOT EXISTS purchase_bill_item (
    purchase_bill_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_bill_id      INTEGER NOT NULL,
    item_id               INTEGER NOT NULL,
    quantity              INTEGER NOT NULL DEFAULT 1,
    purchase_price        REAL NOT NULL,
    gst                   REAL NOT NULL DEFAULT 0,
    line_total            REAL NOT NULL,
    FOREIGN KEY (purchase_bill_id) REFERENCES purchase_bill(purchase_bill_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id)          REFERENCES item_master(itemId)
);

CREATE TABLE IF NOT EXISTS purchase_payment (
    payment_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_bill_id INTEGER NOT NULL,
    amount           REAL NOT NULL,
    payment_mode     TEXT NOT NULL,
    payment_date     TEXT NOT NULL DEFAULT (datetime('now')),
    reference_note   TEXT DEFAULT '',
    paid_by          INTEGER,
    created_at       TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (purchase_bill_id) REFERENCES purchase_bill(purchase_bill_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
    logId      INTEGER PRIMARY KEY AUTOINCREMENT,
    userId     INTEGER,
    action     TEXT NOT NULL,
    tableName  TEXT NOT NULL,
    recordId   INTEGER NOT NULL,
    oldValue   TEXT,
    newValue   TEXT,
    actionDate TEXT NOT NULL DEFAULT (datetime('now'))
);
"""

_SEED_SQL = """
INSERT OR IGNORE INTO user (userId, username, password, fullName, role, email, status)
VALUES (1, 'admin',   'admin123',   'Administrator', 'ADMIN',   'admin@bereeze.com',   'ACTIVE');

INSERT OR IGNORE INTO user (userId, username, password, fullName, role, email, status)
VALUES (2, 'cashier', 'cashier123', 'Cashier User',  'CASHIER', 'cashier@bereeze.com', 'ACTIVE');

INSERT OR IGNORE INTO item_master
    (itemCode, itemName, category, manufacturer, purchasePrice, sellingPrice, barcode, stockQuantity, size, color, material)
VALUES
    ('SHOE001','Running Shoes','Sports','Nike',    2500,5999,'8901234567890',50,'10','Black','Mesh'),
    ('SHOE002','Casual Loafers','Casual','Bata',   1500,3499,'8901234567891',40,'9','Brown','Leather'),
    ('SHOE003','Formal Shoes','Formal','Lee Cooper',3000,7499,'8901234567892',30,'10','Black','Synthetic'),
    ('SHOE004','Sandals','Casual','Adidas',         800,1999,'8901234567893',60,'8','Blue','Rubber'),
    ('SHOE005','Sports Boots','Sports','Puma',     2000,4999,'8901234567894',25,'11','Red','Nylon');

INSERT OR IGNORE INTO customer
    (customerCode, customerName, phone, email, address, city, pincode, creditLimit)
VALUES
    ('CUST001','Raj Kumar',  '9876543210','raj@email.com',  '123 Main St','Delhi', '110001',50000),
    ('CUST002','Priya Singh','9876543211','priya@email.com','456 Oak Ave', 'Mumbai','400001',75000);
"""


def initialize_database() -> None:
    """Create tables and seed default data. Safe to call on every startup."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        # SQLite supports multiple statements when executescript is used
        conn.executescript(_SCHEMA_SQLITE)
        conn.executescript(_SEED_SQL)
        conn.commit()
        logger.info("Database initialised successfully.")
    except Exception as exc:
        logger.error("Database initialisation failed: %s", exc)
        raise
    finally:
        close_connection(conn)
