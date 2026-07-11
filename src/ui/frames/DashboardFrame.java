package ui.frames;

import database.BillDAO;
import database.CashRegisterDAO;
import database.ItemMasterDAO;
import database.PurchaseBillDAO;
import database.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import ui.frames.BillingHistoryFrame;
import ui.frames.InventoryFrame;
import ui.frames.MainFrame;
import ui.frames.PendingPurchaseBillsFrame;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

/**
 * Dashboard Frame — displays live statistics pulled from the database.
 *
 * <h3>Today's Sales logic</h3>
 * <ul>
 *   <li>Calls {@link BillDAO#getTodaySales()} which sums all non-CANCELLED
 *       SALES bills whose {@code billDate} falls between today 00:00:00 and
 *       23:59:59.  This is strictly per-day; tomorrow the card starts at ₹0.</li>
 *   <li>The stats are refreshed each time the frame is activated / brought to
 *       front (via {@link InternalFrameAdapter#internalFrameActivated}), so
 *       saving a POS bill and switching back immediately shows the new total.</li>
 *   <li>A public {@link #refreshStats()} method is available so
 *       {@link POSSaleFrame} (or any other frame) can trigger a refresh
 *       programmatically after a sale is saved.</li>
 * </ul>
 */
public class DashboardFrame extends JInternalFrame {

    private JFrame parent;
    private User   currentUser;

    // ── Live stat-card value labels (kept as fields so refreshStats() can update them) ──
    private JLabel openingBalanceLabel;
    private JLabel todaySalesLabel;
    private JLabel closingBalanceLabel;
    private JLabel closingStatusLabel;   // shows "(Live Preview)" or "(Finalized)"
    private JLabel pendingBillsLabel;
    private JLabel lowStockLabel;

    // ── Constructor used by MainFrame — receives the logged-in user ──────────
    public DashboardFrame(JFrame jFrame, User user) {
        this.parent      = jFrame;
        this.currentUser = user;
        this.initializeUI();
        this.attachRefreshListener();
    }

    /** Backward-compatible constructor (no user context). */
    public DashboardFrame(JFrame jFrame) {
        this(jFrame, null);
    }

    // ── Auto-refresh whenever the frame is activated / brought to front ───────
    private void attachRefreshListener() {
        this.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                new Thread(() -> {
                    double opening   = CashRegisterDAO.getTodayOpeningBalance();
                    double sales     = BillDAO.getTodaySales();
                    double closing   = CashRegisterDAO.getTodayClosingBalance();
                    String regStatus = CashRegisterDAO.getTodayRegisterStatus();
                    int    pending   = PurchaseBillDAO.getPendingPurchaseBillsCount();
                    int    lowStock  = ItemMasterDAO.getLowStockItemsCount();

                    SwingUtilities.invokeLater(() -> {
                        if (openingBalanceLabel != null)
                            openingBalanceLabel.setText("\u20b9" + fmt(opening));
                        if (todaySalesLabel != null)
                            todaySalesLabel.setText("\u20b9" + fmt(sales));
                        if (closingBalanceLabel != null)
                            closingBalanceLabel.setText("\u20b9" + fmt(closing));
                        if (closingStatusLabel != null)
                            closingStatusLabel.setText(toStatusText(regStatus));
                        if (pendingBillsLabel != null)
                            pendingBillsLabel.setText(pending + " Bills");
                        if (lowStockLabel != null)
                            lowStockLabel.setText(String.valueOf(lowStock));
                    });
                }, "DashboardRefresh").start();
            }
        });
    }

    /**
     * Public refresh method — call this after saving a sale so the dashboard
     * totals update without requiring the user to manually switch frames.
     *
     * <p>Safe to call from any thread.</p>
     */
    public void refreshStats() {
        new Thread(() -> {
            double opening   = CashRegisterDAO.getTodayOpeningBalance();
            double sales     = BillDAO.getTodaySales();
            double closing   = CashRegisterDAO.getTodayClosingBalance();
            String regStatus = CashRegisterDAO.getTodayRegisterStatus();
            int    pending   = PurchaseBillDAO.getPendingPurchaseBillsCount();
            int    lowStock  = ItemMasterDAO.getLowStockItemsCount();

            SwingUtilities.invokeLater(() -> {
                if (openingBalanceLabel != null)
                    openingBalanceLabel.setText("\u20b9" + fmt(opening));
                if (todaySalesLabel != null)
                    todaySalesLabel.setText("\u20b9" + fmt(sales));
                if (closingBalanceLabel != null)
                    closingBalanceLabel.setText("\u20b9" + fmt(closing));
                if (closingStatusLabel != null)
                    closingStatusLabel.setText(toStatusText(regStatus));
                if (pendingBillsLabel != null)
                    pendingBillsLabel.setText(pending + " Bills");
                if (lowStockLabel != null)
                    lowStockLabel.setText(String.valueOf(lowStock));
            });
        }, "DashboardRefresh").start();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void initializeUI() {
        this.setTitle("Dashboard");
        this.setClosable(true);
        this.setSize(1000, 600);
        this.setLocation(100, 100);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = UIUtils.createTitleLabel("Dashboard");
        mainPanel.add((Component) titleLabel, "North");

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UIConstants.APP_BACKGROUND);
        contentPanel.add(this.createStatsPanel());
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(this.createRecentTransactionsPanel());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.add((Component) scrollPane, "Center");

        this.add(mainPanel);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 15, 15));
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setPreferredSize(new Dimension(0, 250));

        // ── Initial load: query DB synchronously (we're already on EDT at init time) ──
        double opening  = CashRegisterDAO.getTodayOpeningBalance();
        double sales    = BillDAO.getTodaySales();          // ← live, per-day from DB
        double closing  = CashRegisterDAO.getTodayClosingBalance();
        int    pending  = PurchaseBillDAO.getPendingPurchaseBillsCount();
        int    lowStock = ItemMasterDAO.getLowStockItemsCount();

        // ── Row 1 ─────────────────────────────────────────────────────────────
        openingBalanceLabel = new JLabel("\u20b9" + fmt(opening));
        panel.add(buildCard("Opening Balance",  openingBalanceLabel, UIConstants.PRIMARY_COLOR,   () -> {
            if (parent instanceof MainFrame)
                ((MainFrame) parent).showFrame(new RegisterHistoryFrame((MainFrame) parent));
        }));

        todaySalesLabel = new JLabel("\u20b9" + fmt(sales));
        panel.add(buildCard("Today's Sales",    todaySalesLabel,     UIConstants.SUCCESS_COLOR,   () -> {
            if (parent instanceof MainFrame)
                ((MainFrame) parent).showFrame(new BillingHistoryFrame((MainFrame) parent));
        }));

        closingBalanceLabel = new JLabel("\u20b9" + fmt(closing));
        String regStatus = CashRegisterDAO.getTodayRegisterStatus();
        closingStatusLabel  = new JLabel(toStatusText(regStatus));
        closingStatusLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        closingStatusLabel.setForeground(new Color(220, 220, 220));
        panel.add(buildCardWithSub("Closing Balance", closingBalanceLabel, closingStatusLabel,
                UIConstants.SECONDARY_COLOR,   () -> {
                    if (parent instanceof MainFrame)
                        ((MainFrame) parent).showFrame(new RegisterHistoryFrame((MainFrame) parent));
                }));

        // ── Row 2 ─────────────────────────────────────────────────────────────
        pendingBillsLabel = new JLabel(pending + " Bills");
        panel.add(buildCard("Pending Purchase Bills", pendingBillsLabel, UIConstants.WARNING_COLOR, () -> {
            if (parent instanceof MainFrame)
                ((MainFrame) parent).showFrame(new PurchaseBillFrame((MainFrame) parent));
        }));

        lowStockLabel = new JLabel(String.valueOf(lowStock));
        panel.add(buildCard("Low Stock Items",  lowStockLabel,       UIConstants.DANGER_COLOR,    () -> {
            if (parent instanceof MainFrame)
                ((MainFrame) parent).showFrame(new InventoryFrame((MainFrame) parent, true));
        }));

        // Placeholder — 6th cell in 2×3 grid
        JPanel placeholder = new JPanel();
        placeholder.setBackground(UIConstants.APP_BACKGROUND);
        panel.add(placeholder);

        return panel;
    }

    /**
     * Builds a stat card panel. The {@code valueLabel} is kept external so it
     * can be updated later by {@link #refreshStats()} without rebuilding the panel.
     */
    private JPanel buildCard(String title, JLabel valueLabel, Color bgColor, Runnable onClick) {
        return buildCardWithSub(title, valueLabel, null, bgColor, onClick);
    }

    /**
     * Builds a stat card with an optional sub-label shown below the value
     * (used for the Closing Balance card to show "Live Preview" / "Finalized").
     */
    private JPanel buildCardWithSub(String title, JLabel valueLabel, JLabel subLabel,
                                    Color bgColor, Runnable onClick) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });

        Color fgColor = UIConstants.TEXT_ON_PRIMARY;
        if (bgColor == UIConstants.WARNING_COLOR) {
            fgColor = UIConstants.TEXT_ON_WARNING;
        } else if (bgColor == UIConstants.SUCCESS_COLOR) {
            fgColor = UIConstants.TEXT_ON_SUCCESS;
        } else if (bgColor == UIConstants.DANGER_COLOR) {
            fgColor = UIConstants.TEXT_ON_DANGER;
        } else if (bgColor == UIConstants.SECONDARY_COLOR) {
            fgColor = UIConstants.TEXT_ON_SECONDARY;
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.NORMAL_FONT);
        titleLabel.setForeground(fgColor);

        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(fgColor);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(valueLabel);
        if (subLabel != null) {
            card.add(Box.createVerticalStrut(4));
            subLabel.setForeground(fgColor == Color.BLACK ? new Color(60, 60, 60) : new Color(220, 220, 220));
            card.add(subLabel);
        }
        return card;
    }

    private JPanel createRecentTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder("Recent Transactions"));

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Bill No", "Customer", "Date", "Amount", "Status"}, 0);
        JTable table = new JTable(model);
        table.setFont(UIConstants.NORMAL_FONT);
        table.setRowHeight(25);

        panel.add((Component) new JScrollPane(table), "Center");
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Formats a double as Indian-locale currency (e.g. ₹1,25,000.00). */
    private String fmt(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(amount);
    }

    /** Returns a short human-readable status tag for the register status. */
    private String toStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "CLOSED":     return "\u2714 Finalized";
            case "OPEN":       return "\u26A1 Live Preview";
            case "NOT_OPENED": return "\u26A0 Not Opened";
            default:           return status;
        }
    }
}
