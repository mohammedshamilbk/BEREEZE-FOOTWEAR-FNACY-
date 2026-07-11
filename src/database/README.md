# Bereeze Footwear POS - Database Layer Documentation

## Overview
Complete MySQL database layer for the Bereeze Footwear POS Billing System using HikariCP connection pooling and DAO (Data Access Object) pattern for clean architecture.

## Files Included

### 1. **schema.sql**
Database schema with complete table definitions:
- `item_master` - Product inventory
- `customer` - Customer master data
- `user` - System users with authentication
- `bill` - Billing/invoice records
- `bill_item` - Line items in bills
- `transaction` - Payment transaction logging
- `audit_log` - Change tracking for critical tables

**Features:**
- Proper indexing for performance
- Foreign key constraints
- CASCADE/RESTRICT delete rules
- Timestamps with auto-update
- UTF8MB4 encoding for international support

### 2. **DBConnection.java**
HikariCP Connection Pool Manager

**Features:**
- Static connection pool initialization
- Automatic driver loading
- Connection timeout and lifecycle management
- Pool statistics monitoring
- Connection validation testing

**Configuration:**
```
Database: bereeze_pos
URL: jdbc:mysql://localhost:3306/bereeze_pos
User: root
Password: root
Pool Size: 10
Min Idle: 2
Connection Timeout: 20 seconds
```

**Usage:**
```java
// Get connection from pool
Connection conn = DBConnection.getConnection();

// Check pool statistics
System.out.println(DBConnection.getPoolStats());

// Test connection
if (DBConnection.testConnection()) {
    System.out.println("Database connection successful");
}

// Close pool (on application shutdown)
DBConnection.closeConnectionPool();
```

### 3. **ItemMasterDAO.java**
CRUD operations for product inventory

**Methods:**
- `addItem(ItemMaster)` - Add new item
- `updateItem(ItemMaster)` - Update existing item
- `deleteItem(int itemId)` - Delete item
- `getItemById(int)` - Retrieve by ID
- `getItemByBarcode(String)` - Retrieve by barcode
- `getAllItems()` - Get all active items
- `searchItems(String keyword)` - Search by name/code/category
- `getLowStockItems(int threshold)` - Get items below stock level
- `updateStockQuantity(int itemId, int quantity)` - Update stock

**Example:**
```java
// Add new item
ItemMaster item = new ItemMaster("SHOE001", "Running Shoe", "Sports", "Nike",
                                  2000, 3999, 18, "9876543210128", "10", "Black", "Mesh");
int itemId = ItemMasterDAO.addItem(item);

// Search items
List<ItemMaster> results = ItemMasterDAO.searchItems("Nike");

// Get low stock items
List<ItemMaster> lowStock = ItemMasterDAO.getLowStockItems(10);
```

### 4. **CustomerDAO.java**
CRUD operations for customer management

**Methods:**
- `addCustomer(Customer)` - Add new customer
- `updateCustomer(Customer)` - Update customer
- `deleteCustomer(int customerId)` - Delete/deactivate customer
- `getCustomerById(int)` - Retrieve by ID
- `getCustomerByPhone(String)` - Retrieve by phone
- `getAllCustomers()` - Get all active customers
- `searchCustomers(String keyword)` - Search by name/code/phone/email
- `getCustomersByType(String)` - Get by type (REGULAR/WHOLESALE/RETAIL)
- `updateOutstandingAmount(int customerId, double amount)` - Update outstanding
- `addLoyaltyPoints(int customerId, double points)` - Add loyalty points

**Example:**
```java
// Add customer
Customer customer = new Customer("CUST001", "Rajesh Kumar", "9876543210", "rajesh@email.com");
int customerId = CustomerDAO.addCustomer(customer);

// Get by phone
Customer cust = CustomerDAO.getCustomerByPhone("9876543210");

// Update outstanding amount (with credit limit check)
CustomerDAO.updateOutstandingAmount(customerId, 5000);

// Add loyalty points
CustomerDAO.addLoyaltyPoints(customerId, 500);
```

### 5. **BillDAO.java**
CRUD operations for billing/invoices

**Methods:**
- `saveBill(Bill)` - Save new bill
- `updateBill(Bill)` - Update existing bill
- `getBillById(int)` - Retrieve by ID
- `getBillByNumber(String)` - Retrieve by bill number
- `getAllBills()` - Get all bills
- `getBillsByDate(Date, Date)` - Get bills in date range
- `getBillsByCustomer(int)` - Get customer's bills
- `getBillsByPaymentMode(String)` - Get by payment mode
- `getTotalSales(Date, Date)` - Calculate total sales
- `getTotalGST(Date, Date)` - Calculate total GST
- `getBillCount(Date, Date)` - Count bills in range

**Example:**
```java
// Save bill
Bill bill = new Bill("INV-20260605-00001", "SALES", 1, "Rajesh Kumar");
bill.setUserId(1);
bill.setTotalAmount(5000);
int billId = BillDAO.saveBill(bill);

// Get sales report
Date fromDate = new Date(2026, 5, 1);
Date toDate = new Date(2026, 5, 31);
double totalSales = BillDAO.getTotalSales(fromDate, toDate);
double totalGST = BillDAO.getTotalGST(fromDate, toDate);
```

### 6. **BillItemDAO.java**
Operations for bill line items

**Methods:**
- `addBillItem(BillItem)` - Add item to bill
- `updateBillItem(BillItem)` - Update bill item
- `deleteBillItem(int billItemId)` - Delete bill item
- `getBillItemById(int)` - Retrieve by ID
- `getBillItemsByBillId(int)` - Get all items in bill
- `getBillItemsByItemId(int)` - Get bill history for item
- `getTotalGSTByBill(int)` - Calculate total GST for bill
- `getTotalAmountByBill(int)` - Get total bill amount
- `getItemQuantityInBill(int, int)` - Get item quantity in bill
- `deleteAllBillItems(int billId)` - Delete all items in bill

**Example:**
```java
// Add bill item
BillItem billItem = new BillItem(1, "SHOE001", "Running Shoe", 2, 3999, 18);
int billItemId = BillItemDAO.addBillItem(billItem);

// Get all items in bill
List<BillItem> items = BillItemDAO.getBillItemsByBillId(1);

// Get total for bill
double total = BillItemDAO.getTotalAmountByBill(1);
```

### 7. **UserDAO.java**
User management and authentication

**Methods:**
- `addUser(User)` - Add new user
- `updateUser(User)` - Update user
- `deleteUser(int userId)` - Delete/deactivate user
- `getUserById(int)` - Retrieve by ID
- `getUserByUsername(String)` - Retrieve by username
- `getAllUsers()` - Get all active users
- `getUsersByRole(String)` - Get users by role
- `authenticateUser(String username, String password)` - Login
- `updatePassword(int userId, String newPassword)` - Change password
- `usernameExists(String)` - Check username availability
- `updateSalesTarget(int userId, double target)` - Set sales target

**Example:**
```java
// User login
User user = UserDAO.authenticateUser("cashier", "cashier123");
if (user != null) {
    System.out.println("Login successful: " + user.getFullName());
}

// Add new user
User newUser = new User("manager1", "pass123", "Manager Name", "MANAGER");
int userId = UserDAO.addUser(newUser);

// Check username availability
if (!UserDAO.usernameExists("newcashier")) {
    // Username available
}
```

### 8. **TransactionDAO.java**
Transaction logging and reporting

**Methods:**
- `addTransaction(Transaction)` - Log transaction
- `getTransactionById(int)` - Retrieve by ID
- `getTransactionsByBill(int)` - Get bill transactions
- `getTransactionsByDate(Date, Date)` - Get by date range
- `getTransactionsByPaymentMode(String)` - Get by payment mode
- `getTotalTransactionAmount(Date, Date)` - Calculate total
- `getTransactionCount(Date, Date)` - Count transactions
- `getAverageTransactionAmount(Date, Date)` - Calculate average
- `getTotalByPaymentMode(String, Date, Date)` - Total by mode
- `getFailedTransactions()` - Get all failed transactions

**Example:**
```java
// Log transaction
Transaction txn = new Transaction(1, 5000, "CASH");
int txnId = TransactionDAO.addTransaction(txn);

// Get daily total
Date today = new Date();
double dailyTotal = TransactionDAO.getTotalTransactionAmount(today, today);

// Payment mode breakdown
double cashTotal = TransactionDAO.getTotalByPaymentMode("CASH", fromDate, toDate);
double cardTotal = TransactionDAO.getTotalByPaymentMode("CARD", fromDate, toDate);
```

## Setup Instructions

### 1. Database Setup
```sql
-- Create database
CREATE DATABASE bereeze_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Run schema.sql
source path/to/schema.sql;
```

### 2. Maven Dependencies
Add to `pom.xml`:
```xml
<!-- HikariCP Connection Pooling -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>

<!-- MySQL JDBC Driver -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

### 3. Configuration
Update database credentials in `DBConnection.java`:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/bereeze_pos";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "root";
```

### 4. Usage in Application
```java
// Initialize pool (automatic on first use)
Connection conn = DBConnection.getConnection();

// Use DAOs for database operations
ItemMaster item = ItemMasterDAO.getItemById(1);
Bill bill = BillDAO.getBillById(1);

// Close pool on shutdown
DBConnection.closeConnectionPool();
```

## Security Features

1. **SQL Injection Prevention**
   - Parameterized statements (PreparedStatement)
   - No string concatenation in queries

2. **Connection Pooling**
   - HikariCP for optimal performance
   - Configurable pool size and timeouts
   - Automatic connection validation

3. **Soft Deletes**
   - Customers and Users use status-based deletion
   - Preserves data integrity and audit trail

4. **Audit Logging**
   - Complete audit_log table for tracking changes
   - User, action, old/new values recorded

## Performance Optimization

1. **Indexing**
   - Indexes on frequently searched columns
   - Composite indexes for common query patterns

2. **Connection Pooling**
   - Reduces connection creation overhead
   - Reuses connections efficiently

3. **Query Optimization**
   - Proper JOINs and aggregations in database
   - Minimal data transfer

## Error Handling

All DAO methods:
- Use try-with-resources for automatic resource cleanup
- Catch and log all SQLException
- Return null or -1 for error conditions
- Never throw exceptions (graceful degradation)

## Logging

All DAOs use Java logging framework:
```
java.util.logging.Logger
```

Configure logging level in `logging.properties`:
```properties
.level= INFO
database.level= FINE
```

## Testing

Sample test code:
```java
public class DatabaseTest {
    public static void main(String[] args) {
        // Test connection
        if (DBConnection.testConnection()) {
            System.out.println("✓ Database connection OK");
        } else {
            System.out.println("✗ Database connection FAILED");
            return;
        }
        
        // Test DAO operations
        List<User> users = UserDAO.getAllUsers();
        System.out.println("✓ Retrieved " + users.size() + " users");
        
        List<Customer> customers = CustomerDAO.getAllCustomers();
        System.out.println("✓ Retrieved " + customers.size() + " customers");
        
        List<ItemMaster> items = ItemMasterDAO.getAllItems();
        System.out.println("✓ Retrieved " + items.size() + " items");
        
        // Close pool
        DBConnection.closeConnectionPool();
    }
}
```

## Troubleshooting

### Connection Failed
- Check MySQL server is running
- Verify credentials in DBConnection.java
- Check database exists: `SHOW DATABASES;`

### Table Not Found
- Run schema.sql to create tables
- Verify database name in DB_URL

### Performance Issues
- Check connection pool stats: `DBConnection.getPoolStats()`
- Verify indexes are created
- Monitor query logs

## Best Practices

1. Always call `DBConnection.getConnection()` instead of creating new connections
2. Use try-with-resources for automatic resource cleanup
3. Check return values for success/failure
4. Log operations for debugging
5. Handle null returns gracefully
6. Update audit_log for compliance

## Version History

- **v1.0** (2026-06-05) - Initial release
  - Complete schema with 7 tables
  - 8 DAO classes with full CRUD
  - HikariCP connection pooling
  - Logging and error handling

## Support

For issues or questions, refer to:
- SYSTEM_DOCUMENTATION.md for overall system architecture
- Individual DAO JavaDoc comments for specific methods
- Test cases in DatabaseTest.java
