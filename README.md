# 👟 Bereeze Footwear Fancy — POS Billing System

A full-featured, enterprise-grade **Point of Sale (POS) & Inventory Management System** built with **Java Swing** and **MySQL**, designed specifically for footwear retail businesses. It handles everything from sales billing and purchase management to supplier tracking, inventory monitoring, and financial reporting.

![Status](https://img.shields.io/badge/status-active-brightgreen)
![Java](https://img.shields.io/badge/Java-8%2B-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-lightgrey)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Database Schema](#-database-schema)
- [Getting Started](#-getting-started)
- [Default Login Credentials](#-default-login-credentials)
- [Screenshots](#-screenshots)
- [Roadmap](#-roadmap)
- [Security](#-security)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🧾 Overview

**Bereeze Footwear Fancy** is a desktop POS application tailored for retail shoe stores. It provides a complete billing workflow — from scanning/adding items to a cart, applying GST, accepting multiple payment modes, and printing receipts — alongside back-office tools for inventory, suppliers, purchases, and daily cash reconciliation.

The system is built using the **DAO (Data Access Object)** pattern with **HikariCP** connection pooling for efficient MySQL access, and a custom **Swing UI framework** with centralized styling constants and reusable components.

---

## ✨ Features

### 🛒 Point of Sale (Billing)
- Fast item lookup by code/barcode with quantity entry
- Real-time subtotal, discount, GST (18%), and total calculation
- Multiple payment modes: **Cash, Card, UPI/GPay, Cheque, Bank Transfer, Credit**
- Change calculation and receipt printing with logo & QR code
- Hold Bill / Recall Bill support

### 📊 Dashboard
- **Opening & Closing Balance** cards (auto-carried forward daily via `cash_register`)
- **Today's Sales** (cash-only vs. all payment modes)
- **Low Stock Items** alert card with configurable reorder levels
- Clickable balance cards with full historical drill-down

### 📦 Inventory & Item Management
- Full item CRUD with category, manufacturer, size, color, and material attributes
- EAN-13 barcode generation and printing
- Low stock / out-of-stock highlighting
- Stock adjustment and transfer tools

### 👥 Customer Management
- Customer registration with credit limits and loyalty points
- Outstanding balance tracking
- Search by name/phone, customer type segmentation (Retail/Wholesale)

### 🚚 Supplier & Purchase Management
- Supplier master with inline "Add New Supplier"
- **Pending Purchase Bills** module with line items and payment tracking
- Purchase payments across CASH / GPAY / BANK TO BANK modes
- Full purchase bill history and outstanding tracking

### 💳 Payment Processing Engine
- Modular payment system supporting 5 methods: Cash, Card, UPI/Digital, Cheque, Credit
- Card validation via **Luhn algorithm**, CVV & expiry checks
- UPI payments with OTP verification (timeout & attempt-limited)
- Cheque tracking (Pending / Cleared / Bounced)
- Credit payments with approval workflow and due-date tracking
- Split payments and refund processing
- Card/UPI masking for PCI-conscious data handling

### 📈 Reports & Summaries
- Daily Sales Summary by payment mode
- GST / tax reporting
- Billing history with filters (date range, customer, payment mode)
- Export to PDF/Excel (planned)

### 🔐 Security & Stability
- Parameterized `PreparedStatement`s across all DAOs (SQL injection safe)
- Centralized error surfacing (real stack traces before user-facing messages)
- Role-based access (ADMIN / MANAGER / CASHIER / OWNER)
- Audit logging table for compliance
- Externalized DB credentials (recommended for production)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java (JDK 8+, tested up to JDK 25) |
| UI Framework | Java Swing |
| Database | MySQL 8.x |
| Connection Pooling | HikariCP 5.0.1 |
| JDBC Driver | MySQL Connector/J 8.0.33 |
| Build | `javac` via `run.bat` (Maven/Gradle compatible) |
| IDE / Agent Workflow | Antigravity (AI-assisted development) |

---

## 📁 Project Structure

```
Bereezefootwearfancy/
├── src/
│   ├── database/
│   │   ├── schema.sql                 # Full DB schema
│   │   ├── DBConnection.java          # HikariCP pool manager
│   │   ├── ItemMasterDAO.java
│   │   ├── CustomerDAO.java
│   │   ├── BillDAO.java
│   │   ├── BillItemDAO.java
│   │   ├── UserDAO.java
│   │   ├── TransactionDAO.java
│   │   ├── PurchaseBillDAO.java
│   │   ├── CashRegisterDAO.java
│   │   └── DatabaseInitializer.java
│   ├── payment/
│   │   ├── PaymentMethod.java         # Abstract base class
│   │   ├── CashPayment.java
│   │   ├── CardPayment.java
│   │   ├── DigitalPayment.java
│   │   ├── ChequePayment.java
│   │   ├── CreditPayment.java
│   │   ├── PaymentGateway.java
│   │   ├── Transaction.java
│   │   └── PaymentProcessor.java
│   ├── ui/frames/
│   │   ├── UIConstants.java           # Colors, fonts, dimensions
│   │   ├── UIUtils.java               # Component factory
│   │   ├── LoginFrame.java
│   │   ├── MainFrame.java
│   │   ├── DashboardFrame.java
│   │   ├── POSSaleFrame.java
│   │   ├── ItemMasterFrame.java
│   │   ├── CustomerFrame.java
│   │   ├── BillingHistoryFrame.java
│   │   ├── InventoryFrame.java
│   │   ├── PurchaseBillFrame.java
│   │   └── SupplierFrame.java
│   └── reporting/
├── bin/                                # Compiled classes
├── run.bat                             # Compile & launch script
└── docs/                               # Project documentation
```

---

## 🗄 Database Schema

Core tables (see `src/database/schema.sql` for full DDL):

| Table | Purpose |
|---|---|
| `item_master` | Product inventory (barcode, stock, pricing) |
| `customer` | Customer master (credit limit, loyalty points) |
| `bill` / `bill_item` | Sales billing & GST-inclusive line items |
| `user` | Role-based user accounts |
| `transaction` | Payment transaction log |
| `supplier` | Supplier master |
| `purchase_bill` / `purchase_bill_item` | Purchase records |
| `purchase_payment` | Purchase payment tracking |
| `cash_register` | Daily opening/closing balance |
| `audit_log` | Change tracking for compliance |

---

## 🚀 Getting Started

### Prerequisites
- JDK 8+ installed and on `PATH`
- MySQL Server 8.x running locally or remotely
- MySQL Connector/J and HikariCP JARs on the classpath

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/bereeze-footwear-fancy.git
cd bereeze-footwear-fancy
```

### 2. Set up the database
```bash
mysql -u root -p
mysql> CREATE DATABASE bereeze_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
mysql> source src/database/schema.sql;
```

### 3. Configure database credentials
Edit `src/database/DBConnection.java`:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/bereeze_pos";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "your_password";
```
> ⚠️ For production, externalize these credentials (e.g. via a `.env` or properties file) instead of hardcoding them.

### 4. Compile and run
```bash
run.bat
```
This compiles all sources (`src/*.java`, `src/database/*.java`, `src/payment/*.java`, `src/reporting/*.java`, `src/ui/frames/*.java`) into `bin/` and launches the application.

Or manually:
```bash
javac -encoding UTF-8 -d bin src/*.java src/database/*.java src/payment/*.java src/reporting/*.java src/ui/frames/*.java
java -cp bin POSBillingSystem
```

---

## 🔑 Default Login Credentials

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Cashier | `cashier` | `cashier123` |

> Change these immediately in any production deployment.

---

## 🖼 Screenshots

> _Add screenshots of the LoginFrame, Dashboard, POS Sale screen, and Inventory view here once available._

```
docs/screenshots/
├── login.png
├── dashboard.png
├── pos-sale.png
└── inventory.png
```

---

## 🗺 Roadmap

- [ ] Database-backed reports (PDF/Excel export)
- [ ] Real payment gateway integration
- [ ] Multi-store / multi-branch support
- [ ] Barcode scanner hardware integration
- [ ] Cloud backup & sync
- [ ] Web/mobile companion app

---

## 🔐 Security

This project follows these hardening practices:
- All SQL queries use `PreparedStatement` to prevent SQL injection
- Passwords should be hashed (bcrypt recommended) before storage
- Sensitive data (card numbers, UPI IDs) is masked in logs and receipts
- Audit logging captures sensitive operations
- Role-based access control restricts feature access by user role

If you discover a security issue, please open an issue or contact the maintainer privately before public disclosure.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

All Rights Reserved — © Bereeze Footwear Development Team.

*(Update this section with your preferred open-source license — e.g. MIT, Apache 2.0 — if you plan to make the repository public and open for contributions.)*


