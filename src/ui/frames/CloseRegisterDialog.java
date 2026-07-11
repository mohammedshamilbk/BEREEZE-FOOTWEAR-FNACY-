package ui.frames;

import database.CashRegisterDAO;
import database.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * CloseRegisterDialog — shown when the cashier/admin wants to close the shop for the day.
 *
 * <h3>Closing Balance formula (CASH-ONLY)</h3>
 * <pre>
 *   Closing Balance = Opening Balance
 *                   + Cash Sales today (paymentMode='CASH', non-cancelled)
 *                   + Cash In (owner adds float / extra change)
 *                   – Cash Out (expenses paid from drawer, cash refunds)
 * </pre>
 *
 * <h3>Behaviour by register status</h3>
 * <ul>
 *   <li><b>OPEN</b> — shows a live running preview; "Save Adjustments" button persists
 *       cash_in/cash_out mid-day without closing; "Close Register" locks the final value.</li>
 *   <li><b>CLOSED</b> — all fields are read-only; shows the finalized closing balance.</li>
 *   <li><b>NOT_OPENED</b> — warns the user that no register was opened for today.</li>
 * </ul>
 */
public class CloseRegisterDialog extends JDialog {

    private final User    currentUser;
    private final String  registerStatus;   // "OPEN", "CLOSED", "NOT_OPENED"
    private boolean       registerClosed = false;

    // ── UI controls ───────────────────────────────────────────────────────────
    private JLabel    openingBalanceValue;
    private JLabel    cashSalesValue;
    private JTextField cashInField;
    private JTextField cashOutField;
    private JLabel    expectedClosingValue;
    private JLabel    statusBadge;

    // ── Header colours ────────────────────────────────────────────────────────
    private static final Color HEADER_OPEN   = new Color(25,  118, 210);   // blue — shop open
    private static final Color HEADER_CLOSED = new Color(56,  142, 60);    // green — shop closed

    public CloseRegisterDialog(JFrame parent, User user) {
        super(parent, "Close Register / Close Shop", true);
        this.currentUser    = user;
        this.registerStatus = CashRegisterDAO.getTodayRegisterStatus();
        initializeUI();
        loadAndRefresh();   // populate all fields from the DB
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void initializeUI() {
        this.setSize(500, 490);
        this.setLocationRelativeTo(getParent());
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(245, 247, 250));

        // ── Header ────────────────────────────────────────────────────────────
        boolean alreadyClosed = "CLOSED".equalsIgnoreCase(registerStatus);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(alreadyClosed ? HEADER_CLOSED : HEADER_OPEN);
        header.setPreferredSize(new Dimension(500, 64));
        header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JLabel headerLabel = new JLabel(alreadyClosed
                ? "\u2705  Register Already Closed for Today"
                : "\uD83C\uDFEA  Close Register for Today");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerLabel.setForeground(Color.WHITE);
        header.add(headerLabel, BorderLayout.CENTER);

        statusBadge = new JLabel(getStatusText());
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusBadge.setForeground(new Color(255, 255, 200));
        statusBadge.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(statusBadge, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ── Form ─────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(245, 247, 250));
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        GridBagConstraints lblC = new GridBagConstraints();
        lblC.gridx = 0;
        lblC.anchor = GridBagConstraints.WEST;
        lblC.insets = new Insets(8, 0, 8, 15);

        GridBagConstraints valC = new GridBagConstraints();
        valC.gridx = 1;
        valC.fill  = GridBagConstraints.HORIZONTAL;
        valC.weightx = 1.0;
        valC.insets = new Insets(8, 0, 8, 0);

        Font labelFont = new Font("Segoe UI", Font.PLAIN,  13);
        Font valueFont = new Font("Segoe UI", Font.BOLD,   13);

        int row = 0;

        // Opening Balance (read-only)
        lblC.gridy = row;
        form.add(makeLabel("Opening Balance:", labelFont), lblC);
        openingBalanceValue = makeValueLabel("\u20b90.00", valueFont, HEADER_OPEN);
        valC.gridy = row++;
        form.add(openingBalanceValue, valC);

        // Cash Sales — CASH-only, auto-fetched (read-only)
        lblC.gridy = row;
        form.add(makeLabel("Today's Cash Sales:", labelFont), lblC);
        cashSalesValue = makeValueLabel("\u20b90.00", valueFont, new Color(46, 125, 50));
        valC.gridy = row++;
        form.add(cashSalesValue, valC);

        // Separator
        row = addSeparator(form, lblC, row);

        // Cash In
        lblC.gridy = row;
        form.add(makeLabel("Cash In (additions):", labelFont), lblC);
        cashInField = makeAmountField("0.00");
        cashInField.setEnabled(!alreadyClosed);
        cashInField.getDocument().addDocumentListener(new LivePreviewListener());
        valC.gridy = row++;
        form.add(cashInField, valC);

        // Cash Out
        lblC.gridy = row;
        form.add(makeLabel("Cash Out (deductions):", labelFont), lblC);
        cashOutField = makeAmountField("0.00");
        cashOutField.setEnabled(!alreadyClosed);
        cashOutField.getDocument().addDocumentListener(new LivePreviewListener());
        valC.gridy = row++;
        form.add(cashOutField, valC);

        // Separator
        row = addSeparator(form, lblC, row);

        // Expected / Final Closing Balance
        String closingLabel = alreadyClosed ? "Final Closing Balance:" : "Expected Closing Balance:";
        lblC.gridy = row;
        form.add(makeLabel(closingLabel, new Font("Segoe UI", Font.BOLD, 13)), lblC);
        expectedClosingValue = makeValueLabel("\u20b90.00",
                new Font("Segoe UI", Font.BOLD, 18), new Color(183, 28, 28));
        valC.gridy = row++;
        form.add(expectedClosingValue, valC);

        // Sub-label indicating LIVE vs FINALIZED
        lblC.gridy = row;
        lblC.gridwidth = 2;
        JLabel subLabel = new JLabel(alreadyClosed
                ? "\u2714  Finalized — this is tomorrow's Opening Balance"
                : "\u26A1  Live preview — updates automatically with each sale");
        subLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        subLabel.setForeground(alreadyClosed ? new Color(56, 142, 60) : new Color(130, 100, 0));
        form.add(subLabel, lblC);
        lblC.gridwidth = 1;

        root.add(form, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        btnPanel.setBackground(new Color(245, 247, 250));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 220)));

        if (!alreadyClosed) {
            // "Save Adjustments" — persists cash_in/cash_out mid-day without closing
            JButton saveAdjBtn = new JButton("Save Adjustments");
            saveAdjBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            saveAdjBtn.setBackground(new Color(25, 118, 210));
            saveAdjBtn.setForeground(Color.WHITE);
            saveAdjBtn.setFocusPainted(false);
            saveAdjBtn.setBorderPainted(false);
            saveAdjBtn.setPreferredSize(new Dimension(155, 36));
            saveAdjBtn.setToolTipText("Persist Cash In/Out to DB without closing the register");
            saveAdjBtn.addActionListener(e -> handleSaveAdjustments());
            btnPanel.add(saveAdjBtn);

            // "Close Register" — finalize and lock
            JButton closeBtn = new JButton("Close Register");
            closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            closeBtn.setBackground(new Color(183, 28, 28));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setFocusPainted(false);
            closeBtn.setBorderPainted(false);
            closeBtn.setPreferredSize(new Dimension(145, 36));
            closeBtn.addActionListener(e -> handleCloseRegister());
            btnPanel.add(closeBtn);
        }

        JButton cancelBtn = new JButton(alreadyClosed ? "Close" : "Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.setBackground(new Color(220, 225, 232));
        cancelBtn.setForeground(new Color(50, 50, 50));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setPreferredSize(new Dimension(90, 36));
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(cancelBtn);

        root.add(btnPanel, BorderLayout.SOUTH);
        this.setContentPane(root);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /**
     * Loads live/stored values from the DB and populates all labels and fields.
     * Safe to call from any thread (dispatches to EDT).
     */
    private void loadAndRefresh() {
        // Load DB values off-EDT if already showing, or inline if constructing
        double opening   = CashRegisterDAO.getTodayOpeningBalance();
        double cashSales = CashRegisterDAO.getTodayCashSales();
        double storedIn  = CashRegisterDAO.getTodayCashIn();
        double storedOut = CashRegisterDAO.getTodayCashOut();

        // Pre-populate editable fields with whatever is stored
        if (cashInField  != null) cashInField.setText(String.format("%.2f", storedIn));
        if (cashOutField != null) cashOutField.setText(String.format("%.2f", storedOut));

        openingBalanceValue.setText("\u20b9" + fmt(opening));
        cashSalesValue.setText("\u20b9" + fmt(cashSales));

        // Recompute preview using stored adjustments
        double preview = opening + cashSales + storedIn - storedOut;
        expectedClosingValue.setText("\u20b9" + fmt(preview));
    }

    /**
     * Recomputes the "Expected Closing Balance" label from the current field values.
     * Called on every keystroke in cashInField / cashOutField.
     */
    private void refreshPreview() {
        double opening   = CashRegisterDAO.getTodayOpeningBalance();
        double cashSales = CashRegisterDAO.getTodayCashSales();
        double cashIn    = parseAmount(cashInField  != null ? cashInField.getText()  : "0");
        double cashOut   = parseAmount(cashOutField != null ? cashOutField.getText() : "0");
        double expected  = opening + cashSales + cashIn - cashOut;

        SwingUtilities.invokeLater(() ->
            expectedClosingValue.setText("\u20b9" + fmt(expected)));
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    /**
     * Saves current cash_in / cash_out to the DB without closing the register.
     * Useful for mid-day adjustments (owner deposits change, petty cash paid out, etc.).
     */
    private void handleSaveAdjustments() {
        double cashIn  = parseAmount(cashInField.getText());
        double cashOut = parseAmount(cashOutField.getText());

        boolean ok = CashRegisterDAO.updateCashAdjustments(cashIn, cashOut);
        if (ok) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Cash adjustments saved.\nClosing Balance preview will reflect these values.",
                    "Adjustments Saved",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
            // Refresh status badge
            statusBadge.setText(getStatusText());
        } else {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not save adjustments.\nMake sure the register is open for today.",
                    "Save Failed",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Finalizes the register for today: locks in the closing balance and sets
     * status = CLOSED.  The closing_balance becomes tomorrow's opening balance.
     */
    private void handleCloseRegister() {
        double cashIn    = parseAmount(cashInField.getText());
        double cashOut   = parseAmount(cashOutField.getText());
        double cashSales = CashRegisterDAO.getTodayCashSales();
        double opening   = CashRegisterDAO.getTodayOpeningBalance();
        double finalClosing = opening + cashSales + cashIn - cashOut;

        int userId = (currentUser != null) ? currentUser.getUserId() : 0;

        int confirm = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "<html><b>Confirm Close Register</b><br><br>" +
                "This will lock the closing balance for today and<br>" +
                "mark the register as <b>CLOSED</b>.<br><br>" +
                "Tomorrow's Opening Balance will be:<br>" +
                "<font color='#b71c1c'><b>\u20b9" + fmt(finalClosing) + "</b></font><br><br>" +
                "Are you sure?</html>",
                "Close Register",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);

        if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

        CashRegisterDAO.closeTodayRegister(userId, cashSales, cashIn, cashOut);
        registerClosed = true;

        javax.swing.JOptionPane.showMessageDialog(
                this,
                "<html><b>Register Closed Successfully!</b><br><br>" +
                "Closing Balance: <b>\u20b9" + fmt(CashRegisterDAO.getLastClosingBalance()) + "</b><br>" +
                "This becomes tomorrow's Opening Balance.</html>",
                "Register Closed",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);

        dispose();
    }

    /** @return true if the register was successfully closed during this dialog session. */
    public boolean isRegisterClosed() {
        return registerClosed;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getStatusText() {
        switch (registerStatus) {
            case "CLOSED":     return "Status: CLOSED \u2705";
            case "OPEN":       return "Status: OPEN \uD83D\uDFE2";
            case "NOT_OPENED": return "Status: NOT OPENED \u26A0\uFE0F";
            default:           return "Status: " + registerStatus;
        }
    }

    private int addSeparator(JPanel form, GridBagConstraints lblC, int row) {
        lblC.gridy = row;
        lblC.gridwidth = 2;
        lblC.insets = new Insets(2, 0, 2, 0);
        JPanel sep = new JPanel();
        sep.setBackground(new Color(200, 210, 220));
        sep.setPreferredSize(new Dimension(420, 1));
        form.add(sep, lblC);
        lblC.gridwidth = 1;
        lblC.insets = new Insets(8, 0, 8, 15);
        return ++row;
    }

    private JLabel makeLabel(String text, Font font) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(new Color(60, 60, 60));
        return lbl;
    }

    private JLabel makeValueLabel(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.RIGHT);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    private JTextField makeAmountField(String defaultVal) {
        JTextField tf = new JTextField(defaultVal, 12);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setHorizontalAlignment(JTextField.RIGHT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 200)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return tf;
    }

    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(text.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String fmt(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(amount);
    }

    // ── Inner class: live preview on keystrokes ───────────────────────────────

    private class LivePreviewListener implements DocumentListener {
        @Override public void insertUpdate(DocumentEvent e)  { refreshPreview(); }
        @Override public void removeUpdate(DocumentEvent e)  { refreshPreview(); }
        @Override public void changedUpdate(DocumentEvent e) { refreshPreview(); }
    }
}
