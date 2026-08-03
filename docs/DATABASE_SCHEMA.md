# Database Schema Documentation — Bereeze Footwear Fancy POS

## Entity Relationship Overview

The Bereeze Footwear Fancy Cloud Database is built on a normalized PostgreSQL schema (with backward-compatible SQLite DDL support). It handles Users, Customers, Product Master Inventory, Game Stations, Customer Sessions, POS Billing, Suppliers, Expenses, Audit Logs, and System Backups.

---

## 1. Table Definitions

### `user`
Stores system staff, admins, managers, and super admins.
- `userId` (INTEGER, PK, AUTOINCREMENT / SERIAL)
- `username` (VARCHAR(50), UNIQUE, NOT NULL)
- `password` (VARCHAR(255), NOT NULL) — SHA-256 / bcrypt hash
- `fullName` (VARCHAR(100), NOT NULL)
- `role` (VARCHAR(20), NOT NULL) — `SUPER_ADMIN`, `ADMIN`, `MANAGER`, `STAFF`
- `email` (VARCHAR(100))
- `phone` (VARCHAR(20))
- `status` (VARCHAR(20), DEFAULT 'ACTIVE')
- `dailySalesTarget` (DOUBLE PRECISION, DEFAULT 0)
- `createdDate` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### `customer`
Stores customer details, credit limits, outstanding balances, and loyalty points.
- `customerId` (INTEGER, PK, AUTOINCREMENT)
- `customerCode` (VARCHAR(50), UNIQUE, NOT NULL)
- `customerName` (VARCHAR(100), NOT NULL)
- `phone` (VARCHAR(20))
- `email` (VARCHAR(100))
- `address` (TEXT)
- `city` (VARCHAR(50))
- `state` (VARCHAR(50))
- `pincode` (VARCHAR(10))
- `creditLimit` (DOUBLE PRECISION, DEFAULT 0)
- `outstandingAmount` (DOUBLE PRECISION, DEFAULT 0)
- `customerType` (VARCHAR(20), DEFAULT 'REGULAR')
- `loyaltyPoints` (DOUBLE PRECISION, DEFAULT 0)
- `status` (VARCHAR(20), DEFAULT 'ACTIVE')
- `registrationDate` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
- `lastPurchaseDate` (TIMESTAMP)

### `item_master`
Product master catalogue storing shoes, sizes, colors, and stock levels.
- `itemId` (INTEGER, PK, AUTOINCREMENT)
- `itemCode` (VARCHAR(50), UNIQUE, NOT NULL)
- `itemName` (VARCHAR(150), NOT NULL)
- `category` (VARCHAR(50), NOT NULL)
- `manufacturer` (VARCHAR(100))
- `purchasePrice` (DOUBLE PRECISION, NOT NULL)
- `sellingPrice` (DOUBLE PRECISION, NOT NULL)
- `barcode` (VARCHAR(100), UNIQUE)
- `stockQuantity` (INTEGER, DEFAULT 0)
- `reorderLevel` (INTEGER, DEFAULT 10)
- `size` (VARCHAR(20))
- `color` (VARCHAR(30))
- `material` (VARCHAR(50))
- `status` (VARCHAR(20), DEFAULT 'ACTIVE')
- `createdDate` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### `game_station`
Manages game stations, VR consoles, and rental counters.
- `station_id` (INTEGER, PK, AUTOINCREMENT)
- `station_name` (VARCHAR(100), UNIQUE, NOT NULL)
- `station_type` (VARCHAR(50), DEFAULT 'CONSOLE')
- `hourly_rate` (DOUBLE PRECISION, DEFAULT 100.0)
- `status` (VARCHAR(20), DEFAULT 'AVAILABLE') — `AVAILABLE`, `OCCUPIED`, `MAINTENANCE`
- `current_session_id` (INTEGER, NULLABLE)

### `game_session`
Tracks real-time customer check-in and check-out session timers.
- `session_id` (INTEGER, PK, AUTOINCREMENT)
- `station_id` (INTEGER, FK -> `game_station.station_id`)
- `customer_id` (INTEGER, FK -> `customer.customerId`)
- `customer_name` (VARCHAR(100), NOT NULL)
- `start_time` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
- `end_time` (TIMESTAMP, NULLABLE)
- `duration_minutes` (DOUBLE PRECISION, DEFAULT 0)
- `rate_per_hour` (DOUBLE PRECISION, NOT NULL)
- `total_amount` (DOUBLE PRECISION, DEFAULT 0)
- `paid_amount` (DOUBLE PRECISION, DEFAULT 0)
- `payment_mode` (VARCHAR(20))
- `status` (VARCHAR(20), DEFAULT 'ACTIVE') — `ACTIVE`, `COMPLETED`, `CANCELLED`
- `user_id` (INTEGER, FK -> `user.userId`)

### `bill`
Header records for sales and return transactions.
- `billId` (INTEGER, PK, AUTOINCREMENT)
- `billNumber` (VARCHAR(50), UNIQUE, NOT NULL)
- `billType` (VARCHAR(30), NOT NULL) — `SALES`, `SALES_RETURN`
- `billDate` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
- `customerId` (INTEGER, FK -> `customer.customerId`)
- `userId` (INTEGER, FK -> `user.userId`)
- `subtotal` (DOUBLE PRECISION, DEFAULT 0)
- `totalDiscount` (DOUBLE PRECISION, DEFAULT 0)
- `totalAmount` (DOUBLE PRECISION, DEFAULT 0)
- `paidAmount` (DOUBLE PRECISION, DEFAULT 0)
- `paymentMode` (VARCHAR(30)) — `CASH`, `CARD`, `UPI`, `CHEQUE`, `CREDIT`
- `status` (VARCHAR(20), DEFAULT 'COMPLETED')

### `bill_item`
Line-item detail for each bill.
- `billItemId` (INTEGER, PK, AUTOINCREMENT)
- `billId` (INTEGER, FK -> `bill.billId`)
- `itemId` (INTEGER, FK -> `item_master.itemId`)
- `quantity` (INTEGER, NOT NULL)
- `unitPrice` (DOUBLE PRECISION, NOT NULL)
- `discount` (DOUBLE PRECISION, DEFAULT 0)
- `totalAmount` (DOUBLE PRECISION, NOT NULL)

### `expense`
Shop and rental station operating expense entries.
- `expense_id` (INTEGER, PK, AUTOINCREMENT)
- `category` (VARCHAR(50), NOT NULL)
- `description` (TEXT)
- `amount` (DOUBLE PRECISION, NOT NULL)
- `payment_mode` (VARCHAR(20), DEFAULT 'CASH')
- `expense_date` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
- `user_id` (INTEGER, FK -> `user.userId`)

### `audit_log`
Security and activity audit trail.
- `logId` (INTEGER, PK, AUTOINCREMENT)
- `userId` (INTEGER, FK -> `user.userId`)
- `action` (VARCHAR(100), NOT NULL)
- `tableName` (VARCHAR(50), NOT NULL)
- `recordId` (INTEGER, NOT NULL)
- `oldValue` (TEXT)
- `newValue` (TEXT)
- `actionDate` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
