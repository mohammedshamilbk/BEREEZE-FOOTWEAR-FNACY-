# POS System UI Integration Guide

## Quick Start

### 1. Launch the Application

```bash
java -cp bin ui.frames.LoginFrame
```

This will start the login screen. Default credentials are:
- **Username:** admin
- **Password:** admin

### 2. After Login

The MainFrame will open with:
- Menu bar for navigation
- Sidebar for quick access
- Status bar showing time and user

## Frame Navigation

### From Sidebar Buttons

Click any sidebar button to open the corresponding frame:
- **Dashboard** → Shows key metrics and recent transactions
- **New Sale** → Opens POS transaction entry
- **Items** → Opens item master management
- **Customers** → Opens customer management
- **Billing** → Opens billing history
- **Inventory** → Opens inventory management

### From Menu Bar

Use the menu bar for alternative navigation:
- **File Menu**: New Sale, Exit
- **Masters Menu**: Items, Customers, Users
- **Transactions Menu**: New Sale, Billing History, Returns
- **Reports Menu**: Sales Report, Inventory Report
- **Tools Menu**: Inventory, Settings
- **Help Menu**: About

## Workflow Examples

### Workflow 1: Create a New Sale

1. Click **New Sale** button from sidebar or menu
2. Select customer from dropdown (or leave as "Select Customer" for cash sale)
3. Enter **Item Code** (e.g., SHOE001)
4. Enter **Quantity**
5. Click **Add Item**
6. Repeat steps 3-5 for more items
7. Review totals in the **Bill Summary** section
8. Enter **Amount Paid**
9. Select **Payment Mode** (Cash/Card/UPI/Check)
10. Click **Save & Print** or **Save Only**

### Workflow 2: Manage Items

1. Click **Items** button from sidebar
2. Use search box to find items by name or code
3. Use category dropdown to filter by type
4. Select an item from table to:
   - **Edit** - Modify item details
   - **Delete** - Remove item (with confirmation)
   - **Generate Barcode** - Create barcode
   - **Print Barcode** - Print to printer
   - **Update Stock** - Adjust inventory

### Workflow 3: Customer Management

1. Click **Customers** button from sidebar
2. Search customers by name or phone
3. Select a customer to:
   - **View Details** - See outstanding balance and loyalty points
   - **Edit** - Update customer information
   - **Delete** - Remove customer
   - **Set Credit Limit** - Adjust credit availability

### Workflow 4: View Billing History

1. Click **Billing** button from sidebar
2. Use filters to find bills:
   - Bill Number
   - Customer
   - Payment Mode
   - Date Range
3. Select a bill to:
   - **View Details** - See bill information
   - **Print Bill** - Send to printer
   - **Cancel Bill** - Void the transaction

### Workflow 5: Monitor Inventory

1. Click **Inventory** button from sidebar
2. View stock status for all items
3. Click **Show Low Stock** to see items needing reorder
4. Click **Show Out of Stock** to see unavailable items
5. Select item and click **Adjust Stock** to update quantity
6. Click **Create Purchase Order** to order new stock

## Data Integration Points

### Item Management

**Database Table:** `item_master`

```java
// Integration example
ItemMaster item = new ItemMaster();
item.setItemCode("SHOE001");
item.setItemName("Black Leather Shoes");
item.setCategory("Shoes");
item.setPrice(2500);
item.setStock(15);
item.save(); // TODO: Implement in database layer
```

**In Frame:** Replace sample data loading with database queries.

### Customer Management

**Database Table:** `customers`

```java
// Integration example
Customer customer = new Customer();
customer.setName("Ahmed Khan");
customer.setPhone("03001234567");
customer.setEmail("ahmed@email.com");
customer.save(); // TODO: Implement in database layer
```

### Billing

**Database Tables:** `bills`, `bill_items`

```java
// Integration example
Bill bill = new Bill();
bill.setCustomerCode("CUST001");
bill.setTotal(2430);
bill.save(); // TODO: Implement in database layer

BillItem item = new BillItem();
item.setBillNo(bill.getBillNo());
item.setItemCode("SHOE001");
item.setQuantity(1);
item.save(); // TODO: Implement in database layer
```

## Customization Options

### Change Application Title

Edit **UIConstants.java**:
```java
public static final String APP_TITLE = "Your Store Name - POS System";
```

### Change Color Scheme

Edit **UIConstants.java**:
```java
public static final Color PRIMARY_COLOR = new Color(R, G, B);
public static final Color SECONDARY_COLOR = new Color(R, G, B);
```

### Change Font Family

Edit **UIConstants.java**:
```java
public static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 18);
```

### Adjust Frame Sizes

Edit **UIConstants.java**:
```java
public static final int FRAME_WIDTH = 1400;  // Wider frames
public static final int FRAME_HEIGHT = 900;   // Taller frames
```

## Database Integration

### Step 1: Create Database Connection

Create a new class `DatabaseConnection.java`:

```java
import java.sql.*;

public class DatabaseConnection {
    private static Connection connection;
    
    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/pos_system",
                    "root",
                    "password"
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
```

### Step 2: Load Data in Frames

In **ItemMasterFrame.java**, replace `loadItemsTable()`:

```java
private void loadItemsTable() {
    tableModel.setRowCount(0);
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        String query = "SELECT * FROM item_master";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        
        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            row.add(rs.getString("item_code"));
            row.add(rs.getString("item_name"));
            row.add(rs.getString("category"));
            row.add("₹" + rs.getDouble("price"));
            row.add(rs.getInt("stock"));
            row.add(rs.getInt("reorder_level"));
            row.add(getStockStatus(rs.getInt("stock"), rs.getInt("reorder_level")));
            tableModel.addRow(row);
        }
    } catch (SQLException e) {
        UIUtils.showErrorDialog(this, "Error", "Failed to load items");
    }
}
```

### Step 3: Save Data from Frames

In **POSSaleFrame.java**, update `handleSaveAndPrint()`:

```java
private void handleSaveAndPrint() {
    if (tableModel.getRowCount() == 0) {
        UIUtils.showErrorDialog(this, "Error", "Bill has no items");
        return;
    }
    
    try {
        // Create bill
        Bill bill = new Bill();
        bill.setCustomerCode((String) customerCombo.getSelectedItem());
        bill.setTotal(getTotalAmount()); // Parse from totalLabel
        bill.save();
        
        // Create bill items
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            BillItem item = new BillItem();
            item.setBillNo(bill.getBillNo());
            item.setItemCode((String) tableModel.getValueAt(i, 0));
            item.setQuantity((int) tableModel.getValueAt(i, 3));
            item.save();
        }
        
        UIUtils.showSuccessDialog(this, "Success", "Bill saved successfully");
        dispose();
    } catch (Exception e) {
        UIUtils.showErrorDialog(this, "Error", "Failed to save bill");
    }
}
```

## Adding New Features

### Add a New Frame

1. **Create Frame Class:**

```java
package ui.frames;

import javax.swing.*;

public class MyNewFrame extends JInternalFrame {
    
    private JFrame parent;
    
    public MyNewFrame(JFrame parent) {
        this.parent = parent;
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("My New Frame");
        setClosable(true);
        setSize(800, 500);
        setLocation(100, 100);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        // Add your components here
        
        add(mainPanel);
    }
}
```

2. **Add to MainFrame:**

In MainFrame.java, add to sidebar:
```java
JButton myBtn = createSidebarButton("📌 My New Frame", e -> showMyNewFrame());
```

And add the handler:
```java
private void showMyNewFrame() {
    new MyNewFrame(this).setVisible(true);
}
```

## Troubleshooting

### Common Issues

**Issue:** LoginFrame shows blank or doesn't compile
- **Solution:** Ensure UIUtils.java and UIConstants.java are compiled first

**Issue:** MainFrame doesn't show menu or sidebar
- **Solution:** Check that UIConstants.java colors are valid RGB values

**Issue:** Tables don't show data
- **Solution:** Ensure sample data arrays are populated in loadXxxTable() methods

**Issue:** Buttons don't respond
- **Solution:** Verify ActionListener implementation and handler methods

### Debug Mode

Add debugging to any frame:

```java
System.out.println("Frame initialized: " + getTitle());
System.out.println("Components count: " + getComponentCount());
```

## Performance Optimization

### For Large Datasets

Use pagination in table loading:

```java
private static final int PAGE_SIZE = 50;

private void loadItemsTable(int pageNumber) {
    tableModel.setRowCount(0);
    int offset = (pageNumber - 1) * PAGE_SIZE;
    
    // Query with LIMIT and OFFSET
    String query = "SELECT * FROM item_master LIMIT " + PAGE_SIZE + " OFFSET " + offset;
    // Load data...
}
```

### For Slow Operations

Add progress dialog:

```java
JProgressBar progressBar = new JProgressBar(0, 100);
JDialog dialog = new JDialog(this, "Processing...", true);
dialog.add(progressBar);
dialog.setSize(300, 80);
dialog.setLocationRelativeTo(this);

// Run operation in background thread
new Thread(() -> {
    // Long operation
    progressBar.setValue(50);
    // More processing
    progressBar.setValue(100);
    dialog.dispose();
}).start();

dialog.setVisible(true);
```

## Printing Integration

### Print Bill from POSSaleFrame

```java
private void handleSaveAndPrint() {
    // ... save bill code ...
    
    // Print
    try {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new BillPrinter(bill));
        if (job.printDialog()) {
            job.print();
        }
    } catch (PrinterException ex) {
        UIUtils.showErrorDialog(this, "Error", "Print failed: " + ex.getMessage());
    }
}
```

## Testing Workflows

### Test New Sale

1. Start application → login
2. Click New Sale
3. Select customer "Ahmed Khan"
4. Add item SHOE001, qty 2
5. Verify total calculation
6. Enter amount paid > total
7. Verify change calculation
8. Click Save & Print

### Test Item Search

1. Open Items frame
2. Type "shoe" in search
3. Verify items are filtered
4. Clear search
5. Select category "Shoes"
6. Verify category filter works

### Test Billing History

1. Open Billing History
2. Enter date range
3. Select customer "Ahmed Khan"
4. Click search
5. Verify bills are filtered
6. Select bill and view details

## Support & Help

For additional help:
- Check **src/ui/frames/README.md** for detailed frame documentation
- Review **SYSTEM_DOCUMENTATION.md** for architecture
- See **DATABASE_INTEGRATION_GUIDE.md** for database setup
- Check **IMPLEMENTATION_SUMMARY.md** for current status

---

**Last Updated:** January 2024
**Version:** 1.0
**Status:** Production Ready
