import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import database.*;
import database.User;

public class POSBillingSystem {

    private List<ItemMaster> itemMasters;
    private List<Bill> bills;
    private List<Customer> customers;
    private Map<String, User> users;
    private int billCounter;
    private int itemCounter;
    private int customerCounter;
    private User currentUser;

    public POSBillingSystem() {
        this.itemMasters = new ArrayList<>();
        this.bills = new ArrayList<>();
        this.customers = new ArrayList<>();
        this.users = new HashMap<>();
        this.billCounter = 1000;
        this.itemCounter = 1;
        this.customerCounter = 1;
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Add sample users
        users.put("admin", new User("admin", "admin123", "Admin User", "ADMIN"));
        users.put("cashier", new User("cashier", "cashier123", "Cashier User", "CASHIER"));

        // Add sample customers
        Customer cust1 = new Customer("CUST001", "Raj Kumar", "9876543210", "raj@email.com");
        cust1.setAddress("123 Main St");
        cust1.setCity("Delhi");
        cust1.setPincode("110001");
        cust1.setCreditLimit(50000);
        customers.add(cust1);

        Customer cust2 = new Customer("CUST002", "Priya Singh", "9876543211", "priya@email.com");
        cust2.setAddress("456 Oak Ave");
        cust2.setCity("Mumbai");
        cust2.setPincode("400001");
        customers.add(cust2);

        // Add sample items
        addItemMaster(new ItemMaster("SHOE001", "Running Shoes", "Sports", "Nike",
                2500, 5999, "8901234567890", "10", "Black", "Mesh"));

        addItemMaster(new ItemMaster("SHOE002", "Casual Loafers", "Casual", "Bata",
                1500, 3499, "8901234567891", "9", "Brown", "Leather"));

        addItemMaster(new ItemMaster("SHOE003", "Formal Shoes", "Formal", "Lee Cooper",
                3000, 7499, "8901234567892", "10", "Black", "Synthetic"));

        addItemMaster(new ItemMaster("SHOE004", "Sandals", "Casual", "Adidas",
                800, 1999, "8901234567893", "8", "Blue", "Rubber"));

        addItemMaster(new ItemMaster("SHOE005", "Sports Boots", "Sports", "Puma",
                2000, 4999, "8901234567894", "11", "Red", "Nylon"));
    }

    public boolean loginUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.authenticate(password)) {
            currentUser = user;
            System.out.println("Login successful! Welcome " + user.getFullName());
            return true;
        }
        System.out.println("Invalid username or password!");
        return false;
    }

    public void logoutUser() {
        if (currentUser != null) {
            System.out.println("Logging out user: " + currentUser.getFullName());
            currentUser = null;
        }
    }

    public void addItemMaster(ItemMaster item) {
        item.setItemId(itemCounter++);
        itemMasters.add(item);
        System.out.println("Item added: " + item.getItemName());
    }

    public ItemMaster searchItemByBarcode(String barcode) {
        for (ItemMaster item : itemMasters) {
            if (item.getBarcode().equals(barcode)) {
                return item;
            }
        }
        return null;
    }

    public ItemMaster searchItemByCode(String itemCode) {
        for (ItemMaster item : itemMasters) {
            if (item.getItemCode().equals(itemCode)) {
                return item;
            }
        }
        return null;
    }

    public Customer searchCustomerByPhone(String phone) {
        for (Customer customer : customers) {
            if (customer.getPhone().equals(phone)) {
                return customer;
            }
        }
        return null;
    }

    public void addCustomer(Customer customer) {
        customer.setCustomerId(customerCounter++);
        customers.add(customer);
        System.out.println("Customer added: " + customer.getCustomerName());
    }

    public Bill createNewBill(String billType, int customerId) {
        Customer customer = customers.stream()
                .filter(c -> c.getCustomerId() == customerId)
                .findFirst()
                .orElse(null);

        if (customer == null) {
            System.out.println("Customer not found!");
            return null;
        }

        String billNumber = "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + (++billCounter);

        Bill bill = new Bill(billNumber, billType, customerId, customer.getCustomerName());
        bill.setCustomerPhone(customer.getPhone());
        bill.setUserId(currentUser.getUserId());
        bill.setCreatedDate(new Date());

        return bill;
    }

    public void addItemToBill(Bill bill, String itemCode, int quantity) {
        ItemMaster item = searchItemByCode(itemCode);
        if (item == null) {
            System.out.println("Item not found!");
            return;
        }

        if (item.getStockQuantity() < quantity) {
            System.out.println("Insufficient stock! Available: " + item.getStockQuantity());
            return;
        }

        BillItem billItem = new BillItem(item.getItemId(), item.getItemCode(),
                item.getItemName(), quantity, item.getSellingPrice());

        bill.addBillItem(billItem);
        item.setStockQuantity(item.getStockQuantity() - quantity);
        System.out.println("Added: " + item.getItemName() + " x " + quantity);
    }

    public void completeBillAndPay(Bill bill, double payment, String paymentMode) {
        bill.calculateTotals();

        if (bill.completeBill(payment, paymentMode)) {
            bills.add(bill);
            bill.setStatus("COMPLETED");

            Customer customer = customers.stream()
                    .filter(c -> c.getCustomerId() == bill.getCustomerId())
                    .findFirst()
                    .orElse(null);

            if (customer != null) {
                customer.addLoyaltyPoints(bill.getTotalAmount());
                customer.setLastPurchaseDate(new Date());
            }

            bill.printBill();
            System.out.println("Bill completed successfully!");
        } else {
            System.out.println("Payment amount is less than total amount!");
        }
    }

    public void displayAllItems() {
        System.out.println("\n========== ITEM MASTER ==========");
        System.out.printf("%-10s %-30s %-10s %-10s %-10s%n", "Code", "Name", "Price", "Stock", "Barcode");
        System.out.println("=====================================");

        for (ItemMaster item : itemMasters) {
            System.out.printf("%-10s %-30s %-10.2f %-10d %-10s%n",
                    item.getItemCode(), item.getItemName(), item.getSellingPrice(),
                    item.getStockQuantity(), item.getBarcode());
        }
    }

    public void displayAllCustomers() {
        System.out.println("\n========== CUSTOMERS ==========");
        System.out.printf("%-10s %-20s %-15s %-20s%n", "Code", "Name", "Phone", "Type");
        System.out.println("======================================");

        for (Customer customer : customers) {
            System.out.printf("%-10s %-20s %-15s %-20s%n",
                    customer.getCustomerCode(), customer.getCustomerName(),
                    customer.getPhone(), customer.getCustomerType());
        }
    }

    public void displayAllBills() {
        System.out.println("\n========== BILLING HISTORY ==========");
        System.out.printf("%-15s %-20s %-15s %-12s %-15s%n", "Bill No", "Customer", "Amount", "Status", "Date");
        System.out.println("===========================================");

        for (Bill bill : bills) {
            System.out.printf("%-15s %-20s %-15.2f %-12s %-15s%n",
                    bill.getBillNumber(), bill.getCustomerName(),
                    bill.getTotalAmount(), bill.getStatus(),
                    new Date(bill.getCreatedDate().getTime()));
        }
    }

    public double getTotalSales() {
        return bills.stream().mapToDouble(Bill::getTotalAmount).sum();
    }

    public static void main(String[] args) {
        // Enforce look and feel and open LoginFrame (the secure entry point)
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        javax.swing.SwingUtilities.invokeLater(() -> new ui.frames.LoginFrame().setVisible(true));
    }
}
