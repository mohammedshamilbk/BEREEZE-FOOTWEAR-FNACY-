# BEREEZE FOOTWEAR - POS BILLING SYSTEM
## Advanced Billing & Inventory Management System

---

## SYSTEM ARCHITECTURE

### 1. CORE MODULES

#### A. ITEM MASTER MODULE
**Purpose**: Manage product inventory and details

**Features**:
- Add/Edit/Delete Items
- Barcode Management (EAN-13)
- Stock Management
- Multi-attribute items (Size, Color, Material)
- Price Management
- Profit Calculation

**Data Fields**:
- Item ID, Code, Name
- Category (Sports, Casual, Formal, Sandals, Boots)
- Manufacturer
- Purchase & Selling Price
- GST Rate
- Barcode (EAN-13 with check digit)
- Stock Quantity
- Size, Color, Material
- Status (ACTIVE/INACTIVE)

---

#### B. CUSTOMER MODULE
**Purpose**: Manage customer relationships and credit

**Features**:
- Customer Registration
- Credit Limit Management
- Loyalty Points Program
- Outstanding Amount Tracking
- Purchase History
- Contact Management

**Customer Types**:
- REGULAR (Individual)
- WHOLESALE (Bulk buyers)
- RETAIL (Store owners)

**Data Fields**:
- Customer ID, Code, Name
- Phone, Email
- Address, City, State, Pincode
- Credit Limit & Outstanding Amount
- Loyalty Points
- Registration & Last Purchase Date
- Status (ACTIVE/INACTIVE)

---

#### C. BILLING MODULE (Advanced POS)
**Purpose**: Core transaction processing

**Billing Types**:
1. **POS BILLING** - Direct counter sales
2. **BARCODE BILLING** - Scan & sell
3. **CREDIT BILLING** - Wholesale/Credit customers
4. **RETURN BILLING** - Sales returns
5. **PURCHASE BILLING** - Supplier purchases
6. **RECEIPT BILLING** - Payments received

**Bill Components**:
- Bill Number (Auto-generated: INV-YYYYMMDD-XXXXX)
- Customer Details
- Line Items (Item, Qty, Price, Discount, GST)
- Subtotal, Discount, GST, Total Amount
- Payment Mode (CASH, CARD, CHEQUE, ONLINE, CREDIT)
- Payment Status (PENDING, COMPLETED, PARTIAL)

**Advanced Features**:
- Item-level Discounts
- Bill-level Discounts
- GST Calculation (18%, 12%, 5%, etc.)
- Partial Payments
- Change Calculation
- Print Receipts

---

#### D. BARCODE SYSTEM
**Purpose**: Efficient product identification and sales

**Features**:
- EAN-13 Barcode Generation
- QR Code Generation
- Barcode Validation
- Barcode Print Module
- Batch Barcode Generation
- Barcode Settings (Format, Scheme)

**Barcode Types**:
1. **EAN-13** - Standard product code
2. **QR Code** - Contains bill info
3. **CUSTOM CODE** - Manufacturer specific

---

#### E. USER & AUTHENTICATION MODULE
**Purpose**: Access control and user management

**User Roles**:
1. **ADMIN** - Full system access
2. **MANAGER** - Reports, Master data, Users (except delete)
3. **CASHIER** - Sales, Returns, Basic reports
4. **OWNER** - Analytics, Reports, Settings

**Features**:
- User Login/Logout
- Permission-based Access
- Sales Target Tracking
- User Activity Logging
- Password Management

---

### 2. TRANSACTION TYPES

| Type | From | To | GST | Purpose |
|------|------|-----|-----|---------|
| SALES | Inventory | Customer | Yes | Normal shop sales |
| SALES_RETURN | Customer | Inventory | Yes | Refund process |
| PURCHASE | Supplier | Inventory | Yes | Stock purchase |
| PURCHASE_RETURN | Inventory | Supplier | Yes | Return to supplier |
| RECEIPT | Customer | Bank | No | Payment received |
| PAYMENT | Bank | Supplier | No | Payment made |

---

### 3. REPORTS AVAILABLE

#### A. SALES REPORTS
- Daily Sales Summary
- Sales by Category
- Sales by Salesperson
- Sales Trends
- Item-wise Sales

#### B. INVENTORY REPORTS
- Stock Status
- Low Stock Alert
- Stock Analysis
- Stock Movement
- Stock Transfer Reports

#### C. FINANCIAL REPORTS
- Daily Cash Report
- GST Report (GSTR-1, GSTR-2)
- Accounts Payable
- Accounts Receivable
- Daybook
- Trial Balance
- Profit & Loss

#### D. CUSTOMER REPORTS
- Customer Analysis
- Outstanding Dues
- Loyalty Points Report
- Customer Wise Sales
- Payment Pending Customers

#### E. SUPPLIER REPORTS
- Supplier Analysis
- Outstanding Payables
- Supplier Performance
- Purchase Analysis

#### F. ANALYSIS REPORTS
- Dash Board (KPIs)
- Flow Analysis
- Customer Analysis
- Supplier Analysis
- Ledger Analysis
- Stock Analysis

---

### 4. SPECIAL FEATURES

#### A. BARCODE PRINT MODULE
**Capabilities**:
- Single/Batch Barcode Print
- Custom Print Schemes
- Print Settings Configuration
- EAN-13 Format
- QR Code Option
- Print to Thermal Printer

**Print Options**:
- Save to PDF
- Print Direct
- Print Preview
- Print Draft

#### B. DATA MANAGEMENT
- Excel Import/Export
- Tally Integration
- CSV Import
- Backup & Restore
- Data Cleanup

#### C. CRM FEATURES
- Lost Customer Recovery
- Group Messaging
- Customer Communications
- Follow-up Management

#### D. ADVANCED FEATURES
- Multi-store Support
- User-wise Sales Tracking
- Discount Authorization
- Credit Limit Enforcement
- Automatic Low Stock Alert
- Loyalty Points Auto-Deduction

---

### 5. PAYMENT MODES

1. **CASH** - Direct payment
2. **CARD** - Credit/Debit Card
3. **CHEQUE** - Cheque payment
4. **ONLINE** - Digital payment
5. **CREDIT** - On credit (for approved customers)
6. **MIXED** - Combination of above

---

### 6. GST CALCULATIONS

**Standard Rates**:
- Premium Shoes: 18%
- Regular Shoes: 18%
- Sandals/Casual: 18%
- Custom rates by category

**GST Components**:
- Base Price (Item Selling Price)
- GST Amount = Base * (GST% / 100)
- Final Price = Base + GST Amount

---

### 7. DATABASE SCHEMA OVERVIEW

```
Tables:
- ItemMaster (item_id, code, name, category, manufacturer, prices, barcode, stock)
- Customer (customer_id, code, name, contact, credit_limit, outstanding)
- Bill (bill_id, bill_number, type, date, customer_id, total_amount, payment_mode)
- BillItem (bill_item_id, bill_id, item_id, qty, price, discount, gst, total)
- User (user_id, username, role, email, phone, sales_target)
- Barcode (barcode_id, barcode_code, item_id, format, generated_date)
- Transaction (transaction_id, type, date, amount, user_id, status)
- Report (report_id, report_type, generated_date, data)
```

---

### 8. KEY FEATURES TO IMPLEMENT

**Phase 1 - Core (Currently Implemented)**:
- [x] Item Master CRUD
- [x] Customer Master CRUD
- [x] POS Billing
- [x] Bill Item Management
- [x] Barcode Generation (EAN-13)
- [x] User Authentication
- [x] Basic Reports

**Phase 2 - Advanced**:
- [ ] Database Integration (MySQL/PostgreSQL)
- [ ] GUI (Swing/JavaFX)
- [ ] Thermal Printer Support
- [ ] Advanced Reporting
- [ ] Excel Export
- [ ] Multi-store Support
- [ ] Inventory Sync

**Phase 3 - Enterprise**:
- [ ] Web Interface
- [ ] Mobile App
- [ ] Cloud Backup
- [ ] CRM Integration
- [ ] Email/SMS Notifications
- [ ] Analytics Dashboard
- [ ] API Development

---

## USAGE GUIDE

### Basic POS Sale Workflow:
1. Login as Cashier
2. Select "New POS Sale"
3. Enter/Select Customer
4. Scan/Enter Items (or use item code)
5. Enter Quantity
6. Review Bill Items
7. Apply Discount (if any)
8. Enter Payment Amount
9. Select Payment Mode
10. Print Bill
11. Logout

### Barcode Billing:
1. Barcode scanner inputs to Item Code field
2. Automatically adds item (quantity = 1)
3. Increase quantity if same item scanned again
4. Complete bill as normal

### Sales Return:
1. Select "Sales Return"
2. Enter Customer
3. Select Item to Return
4. Enter Return Quantity
5. Complete refund
6. Print Return Slip

---

## SYSTEM REQUIREMENTS

**Java**: JDK 8 or higher
**RAM**: 2GB minimum
**Storage**: 500MB for application + database
**Printer**: Thermal printer for barcode/bill printing (optional)
**Scanner**: USB Barcode Scanner (optional)

---

## TESTING CREDENTIALS

**Admin**:
- Username: admin
- Password: admin123

**Cashier**:
- Username: cashier
- Password: cashier123

---

## FUTURE ENHANCEMENTS

1. Multi-language Support
2. Biometric Authentication
3. Cloud-based POS
4. Mobile Ordering
5. Inventory Auto-ordering
6. Customer Self-Service Portal
7. Advanced Analytics
8. AI-based Demand Forecasting
9. Integration with E-commerce
10. Loyalty Program Integration

---

**Version**: 1.0
**Last Updated**: 2026-06-05
**Company**: Bereeze Footwear
