# Quick Start Guide - Bereeze Footwear POS Database Layer

## ⚡ 5-Minute Setup

### 1. Add Maven Dependency
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

### 2. Create Database
```bash
mysql -u root -p
mysql> source src/database/schema.sql;
```

### 3. Copy Files
Copy all files from `src/database/` to your project

### 4. Run Initializer
```java
DatabaseInitializer.initializeDatabase();
```

### 5. Start Using!
```java
User user = UserDAO.authenticateUser("admin", "admin123");
List<ItemMaster> items = ItemMasterDAO.getAllItems();
```

## 📁 What You Get

| Component | Purpose | File |
|-----------|---------|------|
| Database Schema | 7 tables with indexes | `schema.sql` |
| Connection Pool | HikariCP management | `DBConnection.java` |
| Item DAO | Product operations | `ItemMasterDAO.java` |
| Customer DAO | Customer management | `CustomerDAO.java` |
| Bill DAO | Billing operations | `BillDAO.java` |
| Bill Item DAO | Line items | `BillItemDAO.java` |
| User DAO | Authentication | `UserDAO.java` |
| Transaction DAO | Payment logging | `TransactionDAO.java` |
| Initializer | Setup utility | `DatabaseInitializer.java` |

## 🔑 Key Features

✅ **70+ Methods** for all CRUD operations
✅ **SQL Injection Prevention** - Parameterized statements
✅ **Connection Pooling** - HikariCP for performance
✅ **Proper Logging** - Track all operations
✅ **Error Handling** - Graceful failure management
✅ **Data Validation** - Credit limits, stock checks
✅ **Soft Deletes** - Data preservation
✅ **Date Range Queries** - Reporting support

## 💡 Common Operations

```java
// Add Item
ItemMaster item = new ItemMaster(...);
int itemId = ItemMasterDAO.addItem(item);

// Search
List<ItemMaster> results = ItemMasterDAO.searchItems("Nike");

// Customer
Customer customer = CustomerDAO.getCustomerByPhone("9876543210");

// Bill
Bill bill = new Bill(...);
int billId = BillDAO.saveBill(bill);

// Report
double sales = BillDAO.getTotalSales(from, to);
```

## 🗄️ Database Tables

```
item_master (17 cols)
customer (17 cols)
user (10 cols)
bill (14 cols)
bill_item (8 cols)
transaction (6 cols)
audit_log (8 cols)
```

## 📊 Statistics

- **Files**: 10 (Java + SQL + Docs)
- **Size**: ~110 KB
- **Methods**: 70+
- **LOC**: 3,500+
- **Setup Time**: 5 minutes

## 🚀 Next Steps

1. Read `README.md` for complete documentation
2. Check `DATABASE_INTEGRATION_GUIDE.md` for examples
3. Review `IMPLEMENTATION_SUMMARY.md` for overview
4. Refer to `SYSTEM_DOCUMENTATION.md` for architecture

## ❓ Troubleshooting

| Issue | Solution |
|-------|----------|
| Connection Error | Check DB_URL in DBConnection.java |
| Table Not Found | Run schema.sql |
| Driver Not Found | Verify MySQL JAR in classpath |
| Authentication Failed | Verify DB credentials |

## 📞 Support

- **schema.sql** - Database structure reference
- **README.md** - Complete API documentation
- **DATABASE_INTEGRATION_GUIDE.md** - Usage examples
- **JavaDoc Comments** - Method documentation

---

**All files are production-ready! Start building! 🎉**
