package ui.frames;

import database.CashRegister;
import database.CashRegisterDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class RegisterHistoryFrame extends JInternalFrame {
    private JFrame parent;
    private JTextField fromDateField;
    private JTextField toDateField;
    private JComboBox<String> presetCombo;
    private JTable historyTable;
    private DefaultTableModel tableModel;
    private List<CashRegister> registerList = new ArrayList<>();

    public RegisterHistoryFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
        this.loadHistoryTable();
    }

    private void initializeUI() {
        this.setTitle("Cash Register History Logs");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1100, 600);
        this.setLocation(50, 50);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = UIUtils.createTitleLabel("Register Opening & Closing Balance Logs");
        mainPanel.add((Component)titleLabel, "North");

        JPanel filterPanel = this.createFilterPanel();
        mainPanel.add((Component)filterPanel, "First");

        JPanel tablePanel = this.createTablePanel();
        mainPanel.add((Component)tablePanel, "Center");

        JPanel actionPanel = this.createActionPanel();
        mainPanel.add((Component)actionPanel, "South");

        this.add(mainPanel);
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JPanel filterGroup = new JPanel();
        filterGroup.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterGroup.setBackground(UIConstants.APP_BACKGROUND);
        filterGroup.setBorder(BorderFactory.createTitledBorder("Search Parameters"));

        filterGroup.add(UIUtils.createLabel("Preset:"));
        this.presetCombo = new JComboBox<>(new String[]{"Last 30 Days", "Last 7 Days", "This Month", "Custom Range"});
        this.presetCombo.setFont(UIConstants.NORMAL_FONT);
        this.presetCombo.setPreferredSize(new java.awt.Dimension(130, 30));
        this.presetCombo.addActionListener(e -> handlePresetSelection());
        filterGroup.add(this.presetCombo);

        filterGroup.add(UIUtils.createLabel("From Date:"));
        this.fromDateField = UIUtils.createTextField(10);
        this.fromDateField.setText(LocalDate.now().minusDays(30).toString());
        filterGroup.add(this.fromDateField);

        filterGroup.add(UIUtils.createLabel("To Date:"));
        this.toDateField = UIUtils.createTextField(10);
        this.toDateField.setText(LocalDate.now().toString());
        filterGroup.add(this.toDateField);

        JButton searchBtn = UIUtils.createButton("Search", actionEvent -> this.loadHistoryTable());
        filterGroup.add(searchBtn);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add((Component)filterGroup, gbc);
        return panel;
    }

    private void handlePresetSelection() {
        String sel = presetCombo.getSelectedItem().toString();
        if ("Last 7 Days".equals(sel)) {
            fromDateField.setText(LocalDate.now().minusDays(7).toString());
            toDateField.setText(LocalDate.now().toString());
            fromDateField.setEditable(false);
            toDateField.setEditable(false);
            loadHistoryTable();
        } else if ("Last 30 Days".equals(sel)) {
            fromDateField.setText(LocalDate.now().minusDays(30).toString());
            toDateField.setText(LocalDate.now().toString());
            fromDateField.setEditable(false);
            toDateField.setEditable(false);
            loadHistoryTable();
        } else if ("This Month".equals(sel)) {
            fromDateField.setText(LocalDate.now().withDayOfMonth(1).toString());
            toDateField.setText(LocalDate.now().toString());
            fromDateField.setEditable(false);
            toDateField.setEditable(false);
            loadHistoryTable();
        } else if ("Custom Range".equals(sel)) {
            fromDateField.setEditable(true);
            toDateField.setEditable(true);
        }
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), 
                "Register Log Details", 
                javax.swing.border.TitledBorder.LEFT, 
                javax.swing.border.TitledBorder.TOP, 
                UIConstants.HEADING_FONT, 
                UIConstants.PRIMARY_COLOR));

        this.tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        this.tableModel.addColumn("Date");
        this.tableModel.addColumn("Opening Balance");
        this.tableModel.addColumn("Cash Sales");
        this.tableModel.addColumn("Cash In");
        this.tableModel.addColumn("Cash Out");
        this.tableModel.addColumn("Closing Balance");
        this.tableModel.addColumn("Status");
        this.tableModel.addColumn("Opened By");
        this.tableModel.addColumn("Closed By");

        this.historyTable = new JTable(this.tableModel);
        this.historyTable.setFont(UIConstants.NORMAL_FONT);
        this.historyTable.setRowHeight(25);
        this.historyTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // Custom cell renderer for Status column
        this.historyTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (value != null) {
                    String status = value.toString();
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                        c.setForeground(table.getSelectionForeground());
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        if ("OPEN".equalsIgnoreCase(status)) {
                            c.setBackground(UIConstants.STATUS_SUCCESS_BG);
                            c.setForeground(UIConstants.STATUS_SUCCESS_FG);
                            setFont(new Font("Segoe UI", Font.BOLD, 12));
                        } else {
                            c.setBackground(UIConstants.STATUS_NEUTRAL_BG);
                            c.setForeground(UIConstants.STATUS_NEUTRAL_FG);
                            setFont(UIConstants.NORMAL_FONT);
                        }
                    }
                }
                return c;
            }
        });

        this.historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    handleViewDailySummary();
                }
            }
        });

        JScrollPane scrollPane = UIUtils.createTableScrollPane(this.historyTable);
        panel.add((Component)scrollPane, "Center");
        return panel;
    }

    private void loadHistoryTable() {
        this.tableModel.setRowCount(0);
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd");
            Date fromDate = sdfInput.parse(fromDateField.getText().trim());
            Date toDate = sdfInput.parse(toDateField.getText().trim());

            // Adjust toDate to end of day
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(toDate);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);

            registerList = CashRegisterDAO.getRegisterHistory(fromDate, cal.getTime());

            if (registerList.isEmpty()) {
                return;
            }

            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");

            for (CashRegister cr : registerList) {
                Vector<Object> row = new Vector<>();
                row.add(df.format(cr.getRegisterDate()));
                row.add("\u20b9" + nf.format(cr.getOpeningBalance()));
                row.add("\u20b9" + nf.format(cr.getCashSales()));
                row.add("\u20b9" + nf.format(cr.getCashIn()));
                row.add("\u20b9" + nf.format(cr.getCashOut()));
                
                if ("OPEN".equalsIgnoreCase(cr.getStatus()) && cr.isLivePreview()) {
                    row.add("\u20b9" + nf.format(cr.getDisplayClosingBalance()) + " (Live)");
                } else if ("OPEN".equalsIgnoreCase(cr.getStatus())) {
                    row.add("N/A (Live)");
                } else {
                    row.add("\u20b9" + nf.format(cr.getClosingBalance()));
                }
                
                row.add(cr.getStatus());
                row.add(cr.getOpenedByName() != null ? cr.getOpenedByName() : "User ID: " + cr.getOpenedBy());
                row.add(cr.getClosedByName() != null ? cr.getClosedByName() : ("CLOSED".equalsIgnoreCase(cr.getStatus()) ? "User ID: " + cr.getClosedBy() : "-"));
                this.tableModel.addRow(row);
            }
        } catch (Exception e) {
            UIUtils.showErrorDialog(this, "Error", "Failed to load register history. Make sure dates are in YYYY-MM-DD format.");
        }
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));

        JButton viewSummaryBtn = UIUtils.createButton("View Day's Summary", actionEvent -> this.handleViewDailySummary());
        viewSummaryBtn.setBackground(UIConstants.PRIMARY_COLOR);
        viewSummaryBtn.setForeground(Color.WHITE);

        JButton refreshBtn = UIUtils.createButton("Refresh", actionEvent -> this.loadHistoryTable());
        JButton closeBtn = UIUtils.createDangerButton("Close", actionEvent -> this.dispose());

        panel.add(viewSummaryBtn);
        panel.add(refreshBtn);
        panel.add(closeBtn);
        return panel;
    }

    private void handleViewDailySummary() {
        int selectedRow = this.historyTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                CashRegister selectedRegister = registerList.get(selectedRow);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String dateStr = sdf.format(selectedRegister.getRegisterDate());

                if (parent instanceof MainFrame) {
                    DailySummaryFrame summaryFrame = new DailySummaryFrame(parent);
                    ((MainFrame) parent).showFrame(summaryFrame);
                    summaryFrame.setFilterAndSearch(dateStr, dateStr);
                }
            } catch (Exception e) {
                UIUtils.showErrorDialog(this, "Error", "Could not view daily summary: " + e.getMessage());
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a row first.");
        }
    }
}
