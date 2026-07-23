# 👟 Bereeze Footwear Fancy — Python POS Billing System

An enterprise-grade, high-performance **Point of Sale (POS), Inventory Management & Shop Closing System** built with **Python 3**, **Tkinter GUI**, and **SQLite / MySQL**, custom tailored for retail footwear businesses.

![Python](https://img.shields.io/badge/Python-3.8%2B-blue?logo=python&logoColor=white)
![GUI](https://img.shields.io/badge/GUI-Tkinter%20(High--DPI)-emerald)
![Database](https://img.shields.io/badge/Database-SQLite%20%2F%20MySQL-orange)
![Tests](https://img.shields.io/badge/Tests-Passing%20(14%2F14)-brightgreen)
![Security](https://img.shields.io/badge/Security-Hardened%20(SQLi%20Safe)-purple)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
  - [🛒 Point of Sale & Billing](#-point-of-sale--billing)
  - [🗓️ Yesterday vs. Today Comparative Dashboard](#️-yesterday-vs-today-comparative-dashboard)
  - [🔒 Shop Closing & Cash Reconciliation](#-shop-closing--cash-reconciliation)
  - [📱 Dynamic UPI QR Payment Generator](#-dynamic-upi-qr-payment-generator)
  - [🏷️ Barcode Printer & Label Engine](#️-barcode-printer--label-engine)
  - [📦 Inventory & Supplier Management](#-inventory--supplier-management)
  - [💾 Backup & Disaster Recovery](#-backup--disaster-recovery)
- [Project Architecture](#-project-architecture)
- [Getting Started](#-getting-started)
- [Running Automated Tests](#-running-automated-tests)
- [Database & Configuration](#-database--configuration)
- [Security & Quality Standards](#-security--quality-standards)
- [License](#-license)

---

## 🧾 Overview

**Bereeze Footwear Fancy POS** is a desktop application providing a complete retail workflow — from fast barcode scanning, item carts, and customer credit/loyalty tracking to dynamic UPI QR payments, custom thermal barcode label printing, expense monitoring, and day-end cash counter reconciliation.

It runs out-of-the-box with a local zero-config **SQLite** database (`pos_data.sqlite`) and seamlessly supports **MySQL** servers for multi-terminal retail setups.

---

## ✨ Key Features

### 🛒 Point of Sale & Billing
- **Instant Product Search**: Search products by name, code, or barcode.
- **Cart & Pricing Math**: Real-time subtotal, line-item discounts, flat bill discounts, and net totals.
- **Stock Auto-Deduction**: Inventory stock levels automatically decrement upon sale completion.
- **Multiple Payment Modes**: Cash (with change calculation), Card, UPI, Cheque, and Credit.
- **Receipt Generator**: Formatted printable bill receipts.

### 🗓️ Yesterday vs. Today Comparative Dashboard
- **Comparative Side-by-Side KPIs**: Displays **Yesterday's Summary** (Yesterday's Cash in Counter, Yesterday's UPI Received, Yesterday's Expenses, Yesterday's Sales & Bills) alongside **Today's Live Counter**.
- **Instant Financial Visibility**: Shop owners can instantly compare day-over-day sales, cash flow, and counter balances upon launch.

### 🔒 Shop Closing & Cash Reconciliation
- **Interactive Day-End Modal**: Accessible via the `🔒 Close Shop` button on the dashboard.
- **Physical Cash Tally**: Computes System Expected Cash in Counter (`Gross Cash Sales - Cash Expenses`) and compares against physical drawer cash entered by staff.
- **Real-Time Variance Calculation**: Highlights **Over / Short / Matched** cash balances instantly.
- **Report Exporting**: Exports timestamped CSV closing statements to `exports/day_closing_{YYYYMMDD}.csv`.

### 📱 Dynamic UPI QR Payment Generator
- **Exact Amount Locking**: Encodes the store's VPA (UPI ID) and locks the exact bill amount into the QR code.
- **One-Scan Checkout**: Customers scan the screen with Google Pay, PhonePe, Paytm, or BHIM, and the payment app automatically pre-fills the exact bill total.

### 🏷️ Barcode Printer & Label Engine
- **Multi-Scheme Support**: Generates Code 128, Code 39, EAN-13 1D barcodes and 2D QR codes.
- **Thermal Label Rendering**: Renders 2-column retail sticker labels (Store Name, Product Title, Size, Color, Price, Barcode).
- **Direct Windows Spooler**: Dispatches jobs directly to optical label/thermal printers (Zebra, Epson, POS-80) or virtual PDF printers.

### 📦 Inventory & Supplier Management
- **Full Item Master CRUD**: Sizes, colors, materials, categories, manufacturer details, purchase price, selling price, and reorder levels.
- **Supplier Ledger & Purchase Invoices**: Track supplier purchase bills, payment history (Cash, Bank Transfer, UPI), and outstanding supplier balances.
- **Customer Management**: Register customers, track outstanding balances, credit limits, and credit 10% loyalty points automatically.

### 💾 Backup & Disaster Recovery
- **SQLite Online Backup**: Creates non-blocking online database backups to `backups/` without locking active sales transactions.
- **Pre-Restore Safety Snapshots**: Takes an automatic safety snapshot before restoring any historical backup database.

---

## 🏗️ Project Architecture

```text
Bereezefootwearfancy/
├── pos_billing/
│   ├── assets/              # Store QR codes and barcode configuration files
│   ├── database/            # Database layer
│   │   ├── connection.py    # SQLite & MySQL connection provider
│   │   ├── dao.py           # Data Access Objects (100% Parameterized SQL)
│   │   ├── db_init.py       # Database schema initializer & tables builder
│   │   └── models.py        # Data models (User, Customer, ItemMaster, Bill, Expense)
│   ├── payment/             # Payment processing engine (Cash, Card, Cheque, Digital)
│   ├── ui/                  # Tkinter UI Architecture
│   │   ├── app.py           # Main application root & High-DPI controller
│   │   ├── constants.py     # Modern color palette & typography
│   │   ├── login_frame.py   # Authentication screen
│   │   ├── main_frame.py    # Navigation sidebar & frame switcher
│   │   ├── widgets.py      # Custom styled buttons, entry fields, & tables
│   │   └── frames/          # View modules
│   │       ├── dashboard_frame.py      # Yesterday vs Today & Shop Closing modal
│   │       ├── pos_sale_frame.py       # Live POS sale terminal
│   │       ├── inventory_frame.py      # Stock management
│   │       ├── customer_frame.py       # Customer management & credit
│   │       ├── supplier_frame.py       # Supplier ledger & purchase bills
│   │       ├── expenses_frame.py       # Store expenses tracker
│   │       ├── reports_frame.py        # Financial summaries & CSV/Excel exports
│   │       ├── user_frame.py           # User accounts & permissions
│   │       └── barcode_print_frame.py  # Thermal barcode label printer
│   └── utils/               # Utilities & Helpers
│       ├── backup_manager.py # Backup & disaster recovery system
│       ├── barcode_printer.py# Thermal label renderer & printer spooler
│       ├── path_manager.py   # Folder recovery & path traversal security
│       └── qr_generator.py   # Dynamic UPI QR generator
├── tests/                   # Automated unit test suite
│   ├── test_billing.py      # Billing calculation & payment processor tests
│   ├── test_dao.py          # Database CRUD & persistence tests
│   └── test_exports_backup.py # Folder safety & backup manager tests
├── run.py                   # Main application entry point
├── requirements.txt         # Optional dependencies configuration
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- **Python 3.8+** installed on Windows, macOS, or Linux.
- Tkinter (included with Python installation on Windows).

### Installation & Launch

1. **Clone the repository**:
   ```bash
   git clone https://github.com/mohammedshamilbk/Bereezefootwearfancy.git
   cd Bereezefootwearfancy
   ```

2. **Launch the POS Application**:
   ```bash
   python run.py
   ```
   *or*
   ```bash
   python -m pos_billing
   ```

3. **Default Login Credentials**:
   - **Username**: `admin`
   - **Password**: `admin123`

---

## 🧪 Running Automated Tests

Run the full unit test suite to verify billing calculations, database persistence, and backup routines:

```bash
python -m unittest discover -s tests -p "test_*.py"
```

**Expected Test Result Output**:
```text
Ran 14 tests in 0.091s

OK
```

---

## ⚙️ Database & Configuration

- **Default Storage**: The application automatically creates and uses `pos_data.sqlite` in the project root.
- **MySQL Integration**: To use a remote MySQL server, configure `config.properties` in the project root:
  ```properties
  db.url=jdbc:mysql://localhost:3306/bereeze_pos
  db.user=root
  db.password=your_password
  ```

---

## 🛡️ Security & Quality Standards

- **SQL Injection Defense**: 100% prepared parameterized SQL statements across all queries in `dao.py`.
- **Path Traversal Protection**: File export and backup functions sanitize inputs using `path_manager.py`.
- **Zero Unsafe Code**: Contains zero `eval()`, `exec()`, or `pickle` vulnerabilities.
- **High-DPI Awareness**: Includes Windows DPI awareness calls for sharp text rendering on modern high-resolution displays.

---

## 📄 License

All rights reserved © **Bereeze Footwear Fancy**.