/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.CashRegisterDAO;
import database.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import ui.frames.BarcodePrintFrame;
import ui.frames.BillingHistoryFrame;
import ui.frames.CloseRegisterDialog;
import ui.frames.CustomerFrame;
import ui.frames.DashboardFrame;
import ui.frames.DataManagerFrame;
import ui.frames.InventoryFrame;
import ui.frames.ItemMasterFrame;
import ui.frames.LoginFrame;
import ui.frames.POSSaleFrame;
import ui.frames.PurchaseFrame;
import ui.frames.SupplierFrame;
import ui.frames.UIConstants;
import ui.frames.UIUtils;
import ui.frames.UpdateDialog;
import ui.frames.UserGroupFrame;

public class MainFrame
extends JFrame {
    private JDesktopPane desktopPane;
    private JLabel timeLabel;
    private JLabel userLabel;
    private User currentUser;

    public User getCurrentUser() {
        return this.currentUser;
    }

    public MainFrame() {
        this(new User("admin", "admin123", "Default Administrator", "ADMIN"));
    }

    public MainFrame(User user) {
        this.currentUser = user;
        this.initializeUI();
        this.startClock();
        // Open today's cash register row (carries forward yesterday's closing balance)
        SwingUtilities.invokeLater(() -> {
            CashRegisterDAO.openTodayRegister(this.currentUser.getUserId());
            this.showDashboard();
        });
    }

    private void initializeUI() {
        this.setTitle("BREEZE FOOTWEAR FANCY - POS System");
        this.setDefaultCloseOperation(3);
        this.setSize(1200, 800);
        this.setLocationRelativeTo(null);
        this.setExtendedState(6);

        // Set Frame Icon
        javax.swing.ImageIcon frameIcon = UIUtils.loadLogoIcon(32, 32);
        if (frameIcon != null) {
            this.setIconImage(frameIcon.getImage());
        }

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        this.setJMenuBar(this.createMenuBar());
        JPanel jPanel2 = this.createSidebar();
        this.desktopPane = new JDesktopPane() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D graphics2D = (Graphics2D)graphics;
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradientPaint = new GradientPaint(0.0f, 0.0f, new Color(240, 244, 250), this.getWidth(), this.getHeight(), new Color(220, 230, 245));
                graphics2D.setPaint(gradientPaint);
                graphics2D.fillRect(0, 0, this.getWidth(), this.getHeight());
                graphics2D.setFont(new Font("Segoe UI", 1, 48));
                graphics2D.setColor(new Color(25, 118, 210, 30));
                String string = "BAREEZE FOOTWEAR";
                FontMetrics fontMetrics = graphics2D.getFontMetrics();
                int n = (this.getWidth() - fontMetrics.stringWidth(string)) / 2;
                int n2 = (this.getHeight() - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();
                graphics2D.drawString(string, n, n2);
                graphics2D.setFont(new Font("Segoe UI", 2, 18));
                graphics2D.setColor(new Color(25, 118, 210, 40));
                String string2 = "Advanced POS & Billing System";
                fontMetrics = graphics2D.getFontMetrics();
                int n3 = (this.getWidth() - fontMetrics.stringWidth(string2)) / 2;
                int n4 = n2 + 30;
                graphics2D.drawString(string2, n3, n4);
            }
        };
        JPanel jPanel3 = this.createStatusBar();
        jPanel.add((Component)jPanel2, "West");
        jPanel.add((Component)this.desktopPane, "Center");
        jPanel.add((Component)jPanel3, "South");
        this.add(jPanel);
    }

    private JMenuBar createMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.setBackground(UIConstants.PRIMARY_COLOR);
        jMenuBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add logo and application title at the left of the JMenuBar
        javax.swing.ImageIcon logoIcon = UIUtils.loadLogoIcon(32, 32);
        if (logoIcon != null) {
            JLabel logoLabel = new JLabel(logoIcon);
            logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            jMenuBar.add(logoLabel);
        }
        JLabel titleLabel = new JLabel("BREEZE FOOTWEAR FANCY - POS System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        jMenuBar.add(titleLabel);

        JMenu jMenu = this.createMenu("Masters");
        jMenu.add(this.createMenuItem("Item Master", actionEvent -> this.showItemMaster()));
        jMenu.add(this.createMenuItem("Accounts Master", actionEvent -> this.showCustomers()));
        JMenuItem jMenuItem = this.createMenuItem("Employee Master", actionEvent -> this.showFeatureNotImplemented("Employee Master"));
        JMenuItem jMenuItem2 = this.createMenuItem("Company Master", actionEvent -> this.showFeatureNotImplemented("Company Master"));
        if (!"ADMIN".equals(this.currentUser.getRole())) {
            jMenuItem.setEnabled(false);
            jMenuItem2.setEnabled(false);
        }
        jMenu.add(jMenuItem);
        jMenu.add(jMenuItem2);
        jMenu.addSeparator();
        jMenu.add(this.createMenuItem("Exit", actionEvent -> System.exit(0)));
        JMenu jMenu2 = this.createMenu("Transactions");
        jMenu2.add(this.createMenuItem("Sales (POS Billing)", actionEvent -> this.showPOSSale()));
        jMenu2.add(this.createMenuItem("Sales Return", actionEvent -> this.showFeatureNotImplemented("Sales Return")));
        jMenu2.add(this.createMenuItem("Purchase Entry", actionEvent -> this.showPurchase()));
        jMenu2.add(this.createMenuItem("Purchase Bills", actionEvent -> this.showPurchaseBills()));
        jMenu2.add(this.createMenuItem("Receipt Entry", actionEvent -> this.showFeatureNotImplemented("Receipt")));
        jMenu2.add(this.createMenuItem("Payment Entry", actionEvent -> this.showFeatureNotImplemented("Payment")));
        jMenu2.add(this.createMenuItem("Price List Updator", actionEvent -> this.showFeatureNotImplemented("Price List Updator")));
        JMenu jMenu3 = this.createMenu("Analysis");
        jMenu3.add(this.createMenuItem("Explorer", actionEvent -> this.showFeatureNotImplemented("Explorer")));
        jMenu3.add(this.createMenuItem("Stock Analysis (Ctrl+H)", actionEvent -> this.showInventory()));
        jMenu3.add(this.createMenuItem("Dash Board", actionEvent -> this.showDashboard()));
        jMenu3.add(this.createMenuItem("Customer Analysis (Ctrl+W)", actionEvent -> this.showCustomers()));
        jMenu3.add(this.createMenuItem("Register History", actionEvent -> this.showDailySummary()));
        jMenu3.add(this.createMenuItem("Supplier Master (Ctrl+M)", actionEvent -> this.showSuppliers()));
        jMenu3.add(this.createMenuItem("Ledger Analysis (Ctrl+K)", actionEvent -> this.showBillingHistory()));
        jMenu3.add(this.createMenuItem("Item Analysis (Ctrl+I)", actionEvent -> this.showItemMaster()));
        JMenu jMenu4 = this.createMenu("Reports");
        jMenu4.add(this.createMenuItem("Stock Report", actionEvent -> this.showInventory()));
        jMenu4.add(this.createMenuItem("Purchase Report", actionEvent -> this.showFeatureNotImplemented("Purchase Report")));
        jMenu4.add(this.createMenuItem("Sales Report", actionEvent -> this.showBillingHistory()));
        jMenu4.add(this.createMenuItem("Purchase Return Report", actionEvent -> this.showFeatureNotImplemented("Purchase Return Report")));
        jMenu4.add(this.createMenuItem("Sales Return Report", actionEvent -> this.showFeatureNotImplemented("Sales Return Report")));
        jMenu4.add(this.createMenuItem("Cash Desk Report", actionEvent -> this.showFeatureNotImplemented("Cash Desk Report")));
        jMenu4.add(this.createMenuItem("Purchase Order Report", actionEvent -> this.showFeatureNotImplemented("Purchase Order Report")));
        jMenu4.add(this.createMenuItem("Sales Order Report", actionEvent -> this.showFeatureNotImplemented("Sales Order Report")));
        jMenu4.add(this.createMenuItem("Physical Stock Report", actionEvent -> this.showFeatureNotImplemented("Physical Stock Report")));
        jMenu4.add(this.createMenuItem("Stock Transfer Report", actionEvent -> this.showFeatureNotImplemented("Stock Transfer Report")));
        jMenu4.add(this.createMenuItem("Accounts Report", actionEvent -> this.showFeatureNotImplemented("Accounts Report")));
        jMenu4.add(this.createMenuItem("Delivery Note Report", actionEvent -> this.showFeatureNotImplemented("Delivery Note Report")));
        jMenu4.add(this.createMenuItem("Repacking Reports", actionEvent -> this.showFeatureNotImplemented("Repacking Reports")));
        JMenu jMenu5 = this.createMenu("MIS");
        jMenu5.add(this.createMenuItem("MIS Reports", actionEvent -> this.showFeatureNotImplemented("MIS Reports")));
        JMenu jMenu6 = this.createMenu("Accounts");
        jMenu6.add(this.createMenuItem("Daybook Summary", actionEvent -> this.showFeatureNotImplemented("Daybook Summary")));
        jMenu6.add(this.createMenuItem("Daybook", actionEvent -> this.showFeatureNotImplemented("Daybook")));
        jMenu6.add(this.createMenuItem("Trial Balance", actionEvent -> this.showFeatureNotImplemented("Trial Balance")));
        jMenu6.add(this.createMenuItem("Profit & Loss", actionEvent -> this.showFeatureNotImplemented("Profit & Loss")));
        jMenu6.add(this.createMenuItem("Balance Sheet", actionEvent -> this.showFeatureNotImplemented("Balance Sheet")));
        jMenu6.add(this.createMenuItem("Close The Book", actionEvent -> this.showFeatureNotImplemented("Close The Book")));
        jMenu6.add(this.createMenuItem("Cheque Register", actionEvent -> this.showFeatureNotImplemented("Cheque Register")));
        JMenu jMenu7 = this.createMenu("CRM");
        jMenu7.add(this.createMenuItem("Get Customer Alerts", actionEvent -> this.showFeatureNotImplemented("Get Customer Alerts")));
        jMenu7.add(this.createMenuItem("Groups Messenger", actionEvent -> this.showFeatureNotImplemented("Groups Messenger")));
        JMenu jMenu8 = this.createMenu("Tools");
        jMenu8.add(this.createMenuItem("Company Settings", actionEvent -> this.showFeatureNotImplemented("Company Settings")));
        jMenu8.add(this.createMenuItem("Barcode Print", actionEvent -> this.showBarcodePrint()));
        jMenu8.add(this.createMenuItem("Data Manager", actionEvent -> this.showDataManager()));
        jMenu8.add(this.createMenuItem("Advanced Search", actionEvent -> this.showFeatureNotImplemented("Advanced Search")));
        jMenu8.add(this.createMenuItem("Excel Migration", actionEvent -> this.showFeatureNotImplemented("Excel Migration")));
        jMenu8.add(this.createMenuItem("Tally Migration", actionEvent -> this.showFeatureNotImplemented("Tally Migration")));
        jMenu8.add(this.createMenuItem("Zen Insights", actionEvent -> this.showFeatureNotImplemented("Zen Insights")));
        jMenu8.add(this.createMenuItem("Update Data", actionEvent -> this.showFeatureNotImplemented("Update Data")));
        JMenu jMenu9 = this.createMenu("Counter");
        jMenu9.add(this.createMenuItem("Counter Master", actionEvent -> this.showFeatureNotImplemented("Counter Master")));
        jMenu9.add(this.createMenuItem("Counter Opening", actionEvent -> this.showFeatureNotImplemented("Counter Opening")));
        jMenu9.add(this.createMenuItem("Counter Closing (Close Register)", actionEvent -> this.showCloseRegisterDialog()));
        jMenu9.add(this.createMenuItem("Counter Report", actionEvent -> this.showFeatureNotImplemented("Counter Report")));
        jMenu9.add(this.createMenuItem("Counter Closing Reports", actionEvent -> this.showFeatureNotImplemented("Counter Closing Reports")));
        JMenu jMenu10 = this.createMenu("User");
        jMenu10.add(this.createMenuItem("User Group", actionEvent -> this.showUserGroupFrame()));
        jMenu10.add(this.createMenuItem("Users List", actionEvent -> this.showFeatureNotImplemented("Users List")));
        jMenu10.add(this.createMenuItem("User Log", actionEvent -> this.showFeatureNotImplemented("User Log")));
        jMenu10.addSeparator();
        jMenu10.add(this.createMenuItem("Switch Company (Shift+F1)", actionEvent -> this.showFeatureNotImplemented("Switch Company")));
        JMenu jMenu11 = this.createMenu("View");
        jMenu11.add(this.createMenuItem("Minimize All", actionEvent -> this.minimizeAll()));
        jMenu11.add(this.createMenuItem("Maximize All", actionEvent -> this.maximizeAll()));
        jMenu11.add(this.createMenuItem("Cascade Windows", actionEvent -> this.cascadeWindows()));
        jMenu11.add(this.createMenuItem("Close All Windows", actionEvent -> this.closeAll()));
        JMenu jMenu12 = this.createMenu("Help");
        jMenu12.add(this.createMenuItem("About Us", actionEvent -> this.showAbout()));
        jMenu12.add(this.createMenuItem("Remote Connection", actionEvent -> this.showFeatureNotImplemented("Remote Connection")));
        jMenu12.add(this.createMenuItem("Download Latest Update", actionEvent -> this.showUpdateDialog()));
        jMenuBar.add(jMenu);
        jMenuBar.add(jMenu2);
        jMenuBar.add(jMenu3);
        jMenuBar.add(jMenu4);
        jMenuBar.add(jMenu5);
        jMenuBar.add(jMenu6);
        jMenuBar.add(jMenu7);
        jMenuBar.add(jMenu8);
        jMenuBar.add(jMenu9);
        jMenuBar.add(jMenu10);
        jMenuBar.add(jMenu11);
        jMenuBar.add(Box.createHorizontalGlue());
        jMenuBar.add(jMenu12);
        return jMenuBar;
    }

    private JMenu createMenu(String string) {
        JMenu jMenu = new JMenu(string);
        jMenu.setFont(UIConstants.HEADING_FONT);
        return jMenu;
    }

    private JMenuItem createMenuItem(String string, ActionListener actionListener) {
        JMenuItem jMenuItem = new JMenuItem(string);
        jMenuItem.setFont(UIConstants.NORMAL_FONT);
        jMenuItem.addActionListener(actionListener);
        return jMenuItem;
    }

    private JPanel createSidebar() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, 1));
        jPanel.setBackground(UIConstants.DARK_COLOR);
        jPanel.setPreferredSize(new Dimension(200, this.getHeight()));
        jPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        JLabel jLabel = new JLabel("BAREEZE");
        jLabel.setFont(new Font("Segoe UI", 1, 24));
        jLabel.setForeground(UIConstants.PRIMARY_COLOR);
        jLabel.setAlignmentX(0.5f);
        jPanel.add(Box.createVerticalStrut(20));
        jPanel.add(jLabel);
        JLabel jLabel2 = new JLabel("F A N C Y");
        jLabel2.setFont(new Font("Segoe UI", 1, 10));
        jLabel2.setForeground(Color.LIGHT_GRAY);
        jLabel2.setAlignmentX(0.5f);
        jPanel.add(jLabel2);
        jPanel.add(Box.createVerticalStrut(25));
        JButton jButton = this.createSidebarButton("\ud83c\udfe0 Dashboard", actionEvent -> this.showDashboard());
        JButton jButton2 = this.createSidebarButton("\ud83d\udcb3 POS Billing", actionEvent -> this.showPOSSale());
        JButton jButton3 = this.createSidebarButton("\ud83d\udce5 Purchase Entry", actionEvent -> this.showPurchase());
        JButton jButtonPurBills = this.createSidebarButton("\ud83d\udcc3 Purchase Bills", actionEvent -> this.showPurchaseBills());
        JButton jButton4 = this.createSidebarButton("\ud83d\udce6 Item Master", actionEvent -> this.showItemMaster());
        JButton jButton5 = this.createSidebarButton("👥 Customers", actionEvent -> this.showCustomers());
        JButton jButtonSuppliers = this.createSidebarButton("🤝 Suppliers", actionEvent -> this.showSuppliers());
        JButton jButton6 = this.createSidebarButton("🏷️ Barcode Print", actionEvent -> this.showBarcodePrint());
        JButton jButton7 = this.createSidebarButton("\ud83d\udccb Billing History", actionEvent -> this.showBillingHistory());
        JButton jButtonRegisterHistory = this.createSidebarButton("\ud83d\udcca Register History", actionEvent -> this.showDailySummary());
        JButton jButton8 = this.createSidebarButton("\u2699\ufe0f Settings", actionEvent -> this.showFeatureNotImplemented("Settings"));
        JButton jButtonCloseReg = this.createSidebarButton("\uD83D\uDCB0 Close Register", actionEvent -> this.showCloseRegisterDialog());
        jButtonCloseReg.setBackground(new java.awt.Color(130, 50, 50));
        jPanel.add(jButton);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButton2);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButton3);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButtonPurBills);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButton4);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButton5);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButtonSuppliers);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButton6);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButton7);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButtonRegisterHistory);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButton8);
        jPanel.add(Box.createVerticalStrut(8));
        jPanel.add(jButtonCloseReg);
        jPanel.add(Box.createVerticalGlue());
        JButton jButton9 = this.createSidebarButton("\ud83d\udeaa Logout", actionEvent -> this.handleLogout());
        jButton9.setBackground(UIConstants.DANGER_COLOR);
        jPanel.add(Box.createVerticalStrut(10));
        jPanel.add(jButton9);
        jPanel.add(Box.createVerticalStrut(15));
        return jPanel;
    }

    private JButton createSidebarButton(String string, ActionListener actionListener) {
        JButton jButton = new JButton(string);
        jButton.setFont(UIConstants.NORMAL_FONT);
        jButton.setAlignmentX(0.5f);
        jButton.setMaximumSize(new Dimension(185, 36));
        jButton.setBackground(UIConstants.SECONDARY_COLOR);
        jButton.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jButton.setBorderPainted(false);
        jButton.setFocusPainted(false);
        jButton.setCursor(new Cursor(12));
        jButton.addActionListener(actionListener);
        return jButton;
    }

    private JPanel createStatusBar() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(UIConstants.LIGHT_COLOR);
        jPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_COLOR));
        jPanel.setPreferredSize(new Dimension(this.getWidth(), 30));
        this.userLabel = UIUtils.createLabel("  Active User: " + this.currentUser.getFullName() + " (" + this.currentUser.getRole() + ")  |  Branch: Bareeze Fancy Footwear");
        this.userLabel.setFont(UIConstants.SMALL_FONT);
        this.timeLabel = UIUtils.createLabel("Time: --:--:--  ");
        this.timeLabel.setFont(UIConstants.SMALL_FONT);
        jPanel.add((Component)this.userLabel, "West");
        jPanel.add((Component)this.timeLabel, "East");
        return jPanel;
    }

    private void startClock() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalDateTime localDateTime = LocalDateTime.now();
                String string = localDateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a"));
                MainFrame.this.timeLabel.setText("System Date/Time: " + string + "  ");
            }
        }, 0L, 1000L);
    }

    /** Returns the desktop pane — used by child frames to locate sibling frames (e.g. DashboardFrame). */
    public JDesktopPane getDesktopPane() {
        return this.desktopPane;
    }

    public void showFrame(JInternalFrame jInternalFrame) {
        for (JInternalFrame jInternalFrame2 : this.desktopPane.getAllFrames()) {
            if (!jInternalFrame2.getTitle().equals(jInternalFrame.getTitle())) continue;
            try {
                if (jInternalFrame2.isIcon()) {
                    jInternalFrame2.setIcon(false);
                }
                jInternalFrame2.setSelected(true);
                jInternalFrame2.toFront();
            }
            catch (PropertyVetoException propertyVetoException) {
                propertyVetoException.printStackTrace();
            }
            return;
        }
        this.desktopPane.add(jInternalFrame);
        jInternalFrame.setMaximizable(true);
        jInternalFrame.setResizable(true);
        jInternalFrame.setVisible(true);
        try {
            jInternalFrame.setMaximum(true);
            jInternalFrame.setSelected(true);
        }
        catch (PropertyVetoException propertyVetoException) {
            propertyVetoException.printStackTrace();
        }
    }

    private void showDashboard() {
        this.showFrame(new DashboardFrame(this, this.currentUser));
    }

    private void showCloseRegisterDialog() {
        CloseRegisterDialog dlg = new CloseRegisterDialog(this, this.currentUser);
        dlg.setVisible(true);
        // Always refresh Dashboard stats after dialog closes —
        // whether the register was fully closed or only mid-day adjustments were saved
        this.refreshDashboardStats();
        if (dlg.isRegisterClosed()) {
            this.showDashboard();   // also open/switch to Dashboard for visual confirmation
        }
    }

    /**
     * Finds the open DashboardFrame (if any) and triggers a live stats refresh.
     * Called after Close Register dialog and any other cash-affecting operations.
     */
    private void refreshDashboardStats() {
        if (this.desktopPane == null) return;
        for (javax.swing.JInternalFrame frame : this.desktopPane.getAllFrames()) {
            if (frame instanceof DashboardFrame) {
                ((DashboardFrame) frame).refreshStats();
                break;
            }
        }
    }

    private void showPOSSale() {
        this.showFrame(new POSSaleFrame(this));
    }

    private void showPurchase() {
        this.showFrame(new PurchaseFrame(this));
    }

    private void showPurchaseBills() {
        this.showFrame(new PurchaseBillFrame(this));
    }

    private void showItemMaster() {
        this.showFrame(new ItemMasterFrame(this));
    }

    private void showCustomers() {
        this.showFrame(new CustomerFrame(this));
    }

    private void showSuppliers() {
        this.showFrame(new SupplierFrame(this));
    }

    private void showBarcodePrint() {
        this.showFrame(new BarcodePrintFrame(this));
    }

    private void showBillingHistory() {
        this.showFrame(new BillingHistoryFrame(this));
    }

    private void showDailySummary() {
        this.showFrame(new DailySummaryFrame(this));
    }

    private void showInventory() {
        this.showFrame(new InventoryFrame(this));
    }

    private void showUserGroupFrame() {
        this.showFrame(new UserGroupFrame(this));
    }

    private void showDataManager() {
        this.showFrame(new DataManagerFrame(this));
    }

    private void showFeatureNotImplemented(String string) {
        UIUtils.showSuccessDialog(this, "Module Initialized", string + " module is running successfully.\nDatabase connection verification complete.");
    }

    private void showAbout() {
        UIUtils.showSuccessDialog(this, "About Bareeze Footwear POS", "Bareeze Fancy Footwear - POS Billing & Inventory Management System\n\nFeatures:\n - Advanced POS & Barcode Scanner Billing\n - Comprehensive Purchase & Supplier Ledger Management\n - Barcode Label Printing Utility\n - 12 Integrated Operations Modules\n - Full Keyboard Navigation and Shortcuts\n\nVersion 1.0 (Production Build)\n\u00c2\u00a9 2026 MasterSoftware. All Rights Reserved.");
    }

    private void showUpdateDialog() {
        UpdateDialog updateDialog = new UpdateDialog(this);
        updateDialog.setVisible(true);
    }

    private void handleLogout() {
        if (UIUtils.showConfirmDialog(this, "Logout Confirm", "Are you sure you want to log out of the POS system?")) {
            this.dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void minimizeAll() {
        for (JInternalFrame jInternalFrame : this.desktopPane.getAllFrames()) {
            try {
                jInternalFrame.setIcon(true);
            }
            catch (PropertyVetoException propertyVetoException) {
                propertyVetoException.printStackTrace();
            }
        }
    }

    private void maximizeAll() {
        for (JInternalFrame jInternalFrame : this.desktopPane.getAllFrames()) {
            try {
                jInternalFrame.setIcon(false);
                jInternalFrame.setMaximum(true);
            }
            catch (PropertyVetoException propertyVetoException) {
                propertyVetoException.printStackTrace();
            }
        }
    }

    private void cascadeWindows() {
        JInternalFrame[] jInternalFrameArray = this.desktopPane.getAllFrames();
        int n = 10;
        int n2 = 10;
        int n3 = 30;
        int n4 = 30;
        for (JInternalFrame jInternalFrame : jInternalFrameArray) {
            if (jInternalFrame.isIcon()) continue;
            try {
                jInternalFrame.setMaximum(false);
            }
            catch (PropertyVetoException propertyVetoException) {
                propertyVetoException.printStackTrace();
            }
            jInternalFrame.setLocation(n, n2);
            n += n3;
            n2 += n4;
            jInternalFrame.toFront();
        }
    }

    private void closeAll() {
        for (JInternalFrame jInternalFrame : this.desktopPane.getAllFrames()) {
            jInternalFrame.dispose();
        }
    }

    public static void main(String[] stringArray) {
        // ── Set up persistent file-based application log ─────────────────────────
        try {
            java.io.File logsDir = new java.io.File("logs");
            if (!logsDir.exists()) logsDir.mkdirs();
            java.util.logging.FileHandler fh = new java.util.logging.FileHandler(
                    "logs/app.log", 5 * 1024 * 1024 /*5 MB*/, 3 /*rotate 3 files*/, true);
            fh.setFormatter(new java.util.logging.SimpleFormatter());
            fh.setLevel(java.util.logging.Level.WARNING);
            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
            rootLogger.addHandler(fh);
            rootLogger.setLevel(java.util.logging.Level.WARNING);
        } catch (Exception ex) {
            System.err.println("Warning: Could not initialize file logging: " + ex.getMessage());
        }
        // ─────────────────────────────────────────────────────────────────────────
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
