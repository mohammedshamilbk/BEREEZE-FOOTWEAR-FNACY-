import database.Bill;
import database.BillItem;
import java.util.Date;

public class TestInvoicePrint {
    public static void main(String[] args) {
        System.out.println("\n--- GENERATING SAMPLE INVOICE ---\n");
        Bill bill = new Bill("INV-20240115-1001", "SALES", 1, "Raj Kumar");
        bill.setCustomerPhone("9876543210");
        bill.setBillDate(new Date());
        
        bill.addBillItem(new BillItem(1, "SHOE001", "Running Shoes", 1, 2500.0));
        bill.addBillItem(new BillItem(2, "SHOE002", "Casual Loafers", 2, 1500.0));
        
        bill.calculateTotals();
        bill.completeBill(6000.0, "CASH");
        
        bill.printBill();
    }
}
