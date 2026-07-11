# Database Integration Guide

## Directory Structure
```
Bereezefootwearfancy/
├── src/
│   └── database/
│       ├── schema.sql                 (Database creation script)
│       ├── DBConnection.java          (Connection pooling)
│       ├── ItemMasterDAO.java         (Item CRUD)
│       ├── CustomerDAO.java           (Customer CRUD)
│       ├── BillDAO.java               (Bill CRUD)
│       ├── BillItemDAO.java           (Bill Item operations)
│       ├── UserDAO.java               (User management)
│       ├── TransactionDAO.java        (Transaction logging)
│       ├── DatabaseInitializer.java   (Setup utility)
│       └── README.md                  (Complete documentation)
├── ItemMaster.java                    (Model class)
├── Customer.java                      (Model class)
├── Bill.java                          (Model class)
├── BillItem.java                      (Model class)
├── User.java                          (Model class)
├── SYSTEM_DOCUMENTATION.md            (System architecture)
└── IMPLEMENTATION_SUMMARY.md          (This file)
```

## Step-by-Step Integration

### Step 1: Add Dependencies

**Maven (pom.xml)**
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

**Gradle (build.gradle)**
```gradle
dependencies {
    implementation 'com.zaxxer:HikariCP:5.0.1'
    implementation 'mysql:mysql-connector-java:8.0.33'
}
```

### Step 2: Create Database

**Using MySQL Command Line:**
```bash
mysql -u root -p
mysql> source src/database/schema.sql;
```

**Or Manual Creation:**
```sql
CREATE DATABASE bereeze_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bereeze_pos;
source schema.sql;
```

### Step 3: Configure Database Connection

Edit `src/database/DBConnection.java`:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/bereeze_pos";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "root";  // Change this!
```

### Step 4: Initialize Database

Run the initializer to create default users:
```java
public static void main(String[] args) {
    DatabaseInitializer.initializeDatabase();
}
```

Or in your application startup:
```java
// In main application class
static {
    DatabaseInitializer.initializeDatabase();
}
```

### Step 5: Use DAOs in Your Application

**Example - POS Billing Module:**
```java
import java.util.List;

public class BillingService {
    
    public Bill createSale(int customerId, int userId) {
        // Create bill
        Bill bill = new Bill("INV-20260605-00001", "SALES", customerId, "Customer Name");
        bill.setUserId(userId);
        
        // Add items
        ItemMaster item = ItemMasterDAO.getItemById(1);
        BillItem billItem = new BillItem(item.getItemId(), item.getItemCode(), 
                                         item.getItemName(), 2, item.getSellingPrice(), 
                                         item.getGst());
        bill.addBillItem(billItem);
        
        // Calculate totals
        bill.calculateTotals();
        
        // Save bill
        int billId = BillDAO.saveBill(bill);
        
        // Save bill items
        for (BillItem item : bill.getBillItems()) {
            item.setBillId(billId);
            BillItemDAO.addBillItem(item);
        }
        
        // Log transaction
        TransactionDAO.Transaction txn = new TransactionDAO.Transaction(
            billId, bill.getTotalAmount(), "CASH"
        );
        TransactionDAO.addTransaction(txn);
        
        // Update customer loyalty points
        if (customerId > 0) {
            CustomerDAO.addLoyaltyPoints(customerId, bill.getTotalAmount() * 0.1);
            CustomerDAO.updateOutstandingAmount(customerId, bill.getTotalAmount());
        }
        
        return bill;
    }
    
    public void processPosTransaction(int userId, String username, String password) {
        // Authenticate user
        User user = UserDAO.authenticateUser(username, password);
        if (user == null) {
            System.out.println("Login failed!");
            return;
        }
        
        System.out.println("Welcome " + user.getFullName());
        
        // Create sale
        Bill bill = createSale(1, user.getUserId());
        System.out.println("Bill created: " + bill.getBillNumber());
    }
}
```

**Example - Inventory Management:**
```java
public class InventoryService {
    
    public void checkLowStock() {
        List<ItemMaster> lowStockItems = ItemMasterDAO.getLowStockItems(10);
        
        for (ItemMaster item : lowStockItems) {
            System.out.println("Low Stock Alert: " + item.getItemName() + 
                              " - Current: " + item.getStockQuantity());
        }
    }
    
    public void addNewItem(String itemCode, String itemName, double purchasePrice, 
                          double sellingPrice) {
        ItemMaster item = new ItemMaster(itemCode, itemName, "Casual", "Bereeze",
                                        purchasePrice, sellingPrice, 18, "", "8", 
                                        "Black", "Leather");
        int itemId = ItemMasterDAO.addItem(item);
        System.out.println("Item added with ID: " + itemId);
    }
    
    public void updateStock(int itemId, int quantity) {
        if (ItemMasterDAO.updateStockQuantity(itemId, quantity)) {
            System.out.println("Stock updated successfully");
        } else {
            System.out.println("Insufficient stock or invalid item");
        }
    }
}
```

**Example - Customer Management:**
```java
public class CustomerService {
    
    public void registerNewCustomer(String name, String phone, String email) {
        // Check if customer already exists
        Customer existing = CustomerDAO.getCustomerByPhone(phone);
        if (existing != null) {
            System.out.println("Customer already registered: " + existing.getCustomerName());
            return;
        }
        
        // Create new customer
        Customer customer = new Customer("CUST" + System.currentTimeMillis(), 
                                        name, phone, email);
        customer.setCreditLimit(50000);
        customer.setCity("Mumbai");
        customer.setState("Maharashtra");
        
        int customerId = CustomerDAO.addCustomer(customer);
        System.out.println("Customer registered with ID: " + customerId);
    }
    
    public void viewCustomerAccount(String phone) {
        Customer customer = CustomerDAO.getCustomerByPhone(phone);
        if (customer == null) {
            System.out.println("Customer not found");
            return;
        }
        
        System.out.println("Customer: " + customer.getCustomerName());
        System.out.println("Credit Limit: " + customer.getCreditLimit());
        System.out.println("Outstanding: " + customer.getOutstandingAmount());
        System.out.println("Available Credit: " + customer.getEffectiveCreditLimit());
        System.out.println("Loyalty Points: " + customer.getLoyaltyPoints());
    }
}
```

**Example - Reports:**
```java
public class ReportService {
    
    public void generateDailySalesReport(java.util.Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        java.util.Date startOfDay = cal.getTime();
        
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        java.util.Date endOfDay = cal.getTime();
        
        double totalSales = BillDAO.getTotalSales(startOfDay, endOfDay);
        double totalGST = BillDAO.getTotalGST(startOfDay, endOfDay);
        int billCount = BillDAO.getBillCount(startOfDay, endOfDay);
        
        System.out.println("========== DAILY SALES REPORT ==========");
        System.out.println("Date: " + date);
        System.out.println("Bills: " + billCount);
        System.out.println("Total Sales: " + totalSales);
        System.out.println("Total GST: " + totalGST);
        System.out.println("Net Sales: " + (totalSales - totalGST));
        System.out.println("=======================================");
    }
    
    public void generatePaymentModeReport(java.util.Date fromDate, java.util.Date toDate) {
        String[] modes = {"CASH", "CARD", "CHEQUE", "ONLINE"};
        
        System.out.println("========== PAYMENT MODE REPORT ==========");
        double grandTotal = 0;
        
        for (String mode : modes) {
            double modeTotal = BillDAO.getBillsByPaymentMode(mode).stream()
                .mapToDouble(Bill::getTotalAmount)
                .sum();
            System.out.printf("%s: ₹%.2f%n", mode, modeTotal);
            grandTotal += modeTotal;
        }
        
        System.out.printf("Total: ₹%.2f%n", grandTotal);
        System.out.println("========================================");
    }
}
```

### Step 6: Application Shutdown

Add shutdown hook to close connection pool:
```java
public class POSApplication {
    public static void main(String[] args) {
        // Initialize database
        DatabaseInitializer.initializeDatabase();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing database connections...");
            DBConnection.closeConnectionPool();
        }));
        
        // Start application
        startApplication();
    }
}
```

## Common Operations

### Add New Item
```java
ItemMaster item = new ItemMaster("SHOE001", "Running Shoe", "Sports", 
                                "Nike", 2000, 3999, 18, "9876543210128", 
                                "10", "Black", "Mesh");
int itemId = ItemMasterDAO.addItem(item);
```

### Search Items
```java
List<ItemMaster> results = ItemMasterDAO.searchItems("Nike");
```

### Get Customer
```java
Customer customer = CustomerDAO.getCustomerByPhone("9876543210");
```

### Add Bill
```java
Bill bill = new Bill("INV-001", "SALES", 1, "Customer Name");
bill.setUserId(1);
bill.setTotalAmount(5000);
int billId = BillDAO.saveBill(bill);
```

### Get Sales Report
```java
double totalSales = BillDAO.getTotalSales(fromDate, toDate);
int billCount = BillDAO.getBillCount(fromDate, toDate);
```

## Troubleshooting

### Connection Error: "No suitable driver found"
**Solution:** Ensure MySQL Connector JAR is in classpath
```bash
# Check Maven dependency
mvn dependency:tree | grep mysql

# Rebuild project
mvn clean install
```

### Table Not Found Error
**Solution:** Run schema.sql to create tables
```bash
mysql -u root -p bereeze_pos < schema.sql
```

### Authentication Failed
**Solution:** Check database credentials in DBConnection.java
```java
// Verify these values match your MySQL setup
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "root";
```

### Connection Timeout
**Solution:** Check MySQL server is running
```bash
# On Windows
net start MySQL80

# On Linux
sudo service mysql start

# Test connection
mysql -u root -p
```

## Performance Tips

1. **Use Connection Pool**
   - Always use `DBConnection.getConnection()`
   - Never create connections manually

2. **Batch Operations**
   - Use `deleteAllBillItems()` instead of loop delete

3. **Limit Result Sets**
   - Consider pagination for large datasets
   - Use date ranges for reports

4. **Index Usage**
   - Query on indexed columns (itemCode, phone, username)
   - Avoid large LIKE queries on unindexed columns

5. **Monitor Pool**
   ```java
   System.out.println(DBConnection.getPoolStats());
   ```

## Security Best Practices

1. **Use Parameterized Queries**
   - All DAOs use PreparedStatement (done ✓)

2. **Password Security**
   - Never store plain text passwords
   - Use bcrypt/hashing in application

3. **Input Validation**
   - Validate input before calling DAOs
   - Check user permissions in application

4. **Audit Logging**
   - Record sensitive operations
   - Use audit_log table

5. **Database Backup**
   ```bash
   mysqldump -u root -p bereeze_pos > backup.sql
   ```

## Support

- Refer to README.md for detailed API documentation
- Check SYSTEM_DOCUMENTATION.md for architecture
- Review DAO JavaDoc comments for method details

---

**Ready to integrate! All files are production-ready.** ✅
