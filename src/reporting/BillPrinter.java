package reporting;

import ui.frames.UIConstants;
import ui.frames.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.util.List;

public class BillPrinter implements Printable {

    private String billNo;
    private String date;
    private String customer;
    private String amount;
    private String status;
    private String paymentMode;
    private List<String[]> items; // e.g., ["Item Name", "Qty", "Price", "Total"]

    public BillPrinter(String billNo, String date, String customer, String amount, String status, String paymentMode, List<String[]> items) {
        this.billNo = billNo;
        this.date = date;
        this.customer = customer;
        this.amount = amount;
        this.status = status;
        this.paymentMode = paymentMode;
        this.items = items;
    }

    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
        if (page > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());

        int y = 20;

        // Draw Logo (Centered)
        ImageIcon logoIcon = UIUtils.loadLogoIcon(120, 70);
        if (logoIcon != null && logoIcon.getImage() != null) {
            int logoWidth = logoIcon.getIconWidth();
            int x = (int) (pf.getImageableWidth() / 2 - logoWidth / 2);
            g2d.drawImage(logoIcon.getImage(), x, y, null);
            y += logoIcon.getIconHeight() + 10;
        }

        // Shop Header
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        drawCenteredString(g2d, "BAREEZE FOOTWEAR FANCY", (int) pf.getImageableWidth(), y);
        y += 20;

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        drawCenteredString(g2d, "Anar complex, Naya bazar,", (int) pf.getImageableWidth(), y);
        y += 15;
        drawCenteredString(g2d, "Melparamba, Kasaragod, Kerala 671317", (int) pf.getImageableWidth(), y);
        y += 15;
        drawCenteredString(g2d, "Mobile no: 8086790086 | breezefootwearfancy@gmail.com", (int) pf.getImageableWidth(), y);
        y += 20;

        // Separator
        g2d.drawLine(10, y, (int) pf.getImageableWidth() - 10, y);
        y += 15;

        // Bill Details
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.drawString("Bill No: " + billNo, 10, y);
        g2d.drawString("Date: " + date, 150, y);
        y += 15;
        g2d.drawString("Customer: " + customer, 10, y);
        y += 15;
        if (paymentMode != null) {
            g2d.drawString("Payment Mode: " + paymentMode, 10, y);
            y += 15;
        }
        
        g2d.drawLine(10, y, (int) pf.getImageableWidth() - 10, y);
        y += 15;

        // Items Header
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2d.drawString("Item", 10, y);
        g2d.drawString("Qty", 120, y);
        g2d.drawString("Price", 160, y);
        g2d.drawString("Total", 220, y);
        y += 15;
        g2d.drawLine(10, y, (int) pf.getImageableWidth() - 10, y);
        y += 15;

        // Items List
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        if (items != null) {
            for (String[] item : items) {
                // Truncate item name if too long
                String itemName = item[0].length() > 15 ? item[0].substring(0, 15) + "..." : item[0];
                g2d.drawString(itemName, 10, y);
                g2d.drawString(item[1], 120, y);
                g2d.drawString(item[2], 160, y);
                g2d.drawString(item[3], 220, y);
                y += 15;
            }
        } else {
            g2d.drawString("Details unavailable", 10, y);
            y += 15;
        }

        g2d.drawLine(10, y, (int) pf.getImageableWidth() - 10, y);
        y += 15;

        // Totals
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString("Total Amount: " + amount, 100, y);
        y += 20;
        
        if (status != null && !status.isEmpty()) {
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2d.drawString("Status: " + status, 10, y);
            y += 20;
        }

        // QR Code for payment
        if (!"CASH".equalsIgnoreCase(paymentMode)) {
            g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
            drawCenteredString(g2d, "Scan to Pay via UPI", (int) pf.getImageableWidth(), y);
            y += 15;
            
            ImageIcon qrIcon = UIUtils.loadImageIcon("/resources/images/payment_qr.png", 120, 120);
            if (qrIcon != null && qrIcon.getImage() != null) {
                int qrWidth = qrIcon.getIconWidth();
                int qrx = (int) (pf.getImageableWidth() / 2 - qrWidth / 2);
                g2d.drawImage(qrIcon.getImage(), qrx, y, null);
                y += qrIcon.getIconHeight() + 15;
            }
        }

        // Footer
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 10));
        drawCenteredString(g2d, "Thank you for shopping with us!", (int) pf.getImageableWidth(), y);

        return PAGE_EXISTS;
    }

    private void drawCenteredString(Graphics g, String text, int width, int y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = (width - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
    
    public void printReceipt() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        boolean doPrint = job.printDialog();
        if (doPrint) {
            try {
                job.print();
            } catch (PrinterException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Printing failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
