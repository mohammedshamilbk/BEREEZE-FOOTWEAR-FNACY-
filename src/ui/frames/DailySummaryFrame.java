package ui.frames;

import database.TransactionDAO;
import database.TransactionDAO.DailySummary;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class DailySummaryFrame extends JInternalFrame {
    private JFrame parent;
    private JTextField fromDateField;
    private JTextField toDateField;
    private JTable summaryTable;
    private DefaultTableModel tableModel;
    private List<DailySummary> summaryList = new ArrayList<>();

    public DailySummaryFrame(JFrame jFrame) {
        this.parent = jFrame;
        this.initializeUI();
        this.loadSummaryTable();
    }

    private void initializeUI() {
        this.setTitle("Daily Register History");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(1100, 650);
        this.setLocation(50, 50);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBackground(UIConstants.APP_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = UIUtils.createTitleLabel("Daily Sales & Register Summary");
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

        filterGroup.add(UIUtils.createLabel("From Date:"));
        this.fromDateField = UIUtils.createTextField(10);
        this.fromDateField.setText(LocalDate.now().minusDays(7).toString());
        filterGroup.add(this.fromDateField);

        filterGroup.add(UIUtils.createLabel("To Date:"));
        this.toDateField = UIUtils.createTextField(10);
        this.toDateField.setText(LocalDate.now().toString());
        filterGroup.add(this.toDateField);

        JButton searchBtn = UIUtils.createButton("Search", actionEvent -> this.loadSummaryTable());
        filterGroup.add(searchBtn);

        JButton clearBtn = UIUtils.createDangerButton("Reset Range", actionEvent -> {
            this.fromDateField.setText(LocalDate.now().minusDays(7).toString());
            this.toDateField.setText(LocalDate.now().toString());
            this.loadSummaryTable();
        });
        filterGroup.add(clearBtn);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add((Component)filterGroup, gbc);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(UIConstants.APP_BACKGROUND);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 1), 
                "Daily Register Records", 
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
        this.tableModel.addColumn("GPay / UPI");
        this.tableModel.addColumn("Card");
        this.tableModel.addColumn("Bank");
        this.tableModel.addColumn("Credit");
        this.tableModel.addColumn("Total Sales");
        this.tableModel.addColumn("Closing Balance");
        this.tableModel.addColumn("Bills Count");

        this.summaryTable = new JTable(this.tableModel);
        this.summaryTable.setFont(UIConstants.NORMAL_FONT);
        this.summaryTable.setRowHeight(25);
        this.summaryTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        this.summaryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    handleViewBills();
                }
            }
        });

        JScrollPane scrollPane = UIUtils.createTableScrollPane(this.summaryTable);
        panel.add((Component)scrollPane, "Center");
        return panel;
    }

    private void loadSummaryTable() {
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

            summaryList = TransactionDAO.getDailyPaymentModeSummary(fromDate, cal.getTime());

            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");

            for (DailySummary sum : summaryList) {
                Vector<Object> row = new Vector<>();
                row.add(df.format(sum.getDate()));
                row.add("\u20b9" + nf.format(sum.getOpeningBalance()));
                row.add("\u20b9" + nf.format(sum.getCashTotal()));
                row.add("\u20b9" + nf.format(sum.getGpayTotal()));
                row.add("\u20b9" + nf.format(sum.getCardTotal()));
                row.add("\u20b9" + nf.format(sum.getBankTotal()));
                row.add("\u20b9" + nf.format(sum.getCreditTotal()));
                row.add("\u20b9" + nf.format(sum.getGrandTotal()));
                row.add("\u20b9" + nf.format(sum.getClosingBalance()));
                row.add(sum.getBillCount());
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

        JButton viewBillsBtn = UIUtils.createButton("View Day's Bills", actionEvent -> this.handleViewBills());
        viewBillsBtn.setBackground(UIConstants.PRIMARY_COLOR);
        viewBillsBtn.setForeground(Color.WHITE);

        JButton exportBtn = UIUtils.createButton("Export to Excel", actionEvent -> this.handleExportExcel());
        exportBtn.setBackground(UIConstants.SUCCESS_COLOR);
        exportBtn.setForeground(Color.WHITE);

        JButton refreshBtn = UIUtils.createButton("Refresh", actionEvent -> this.loadSummaryTable());
        JButton closeBtn = UIUtils.createDangerButton("Close", actionEvent -> this.dispose());

        panel.add(viewBillsBtn);
        panel.add(exportBtn);
        panel.add(refreshBtn);
        panel.add(closeBtn);
        return panel;
    }

    private void handleViewBills() {
        int selectedRow = this.summaryTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                // Get the raw date from summaryList using index
                DailySummary selectedSummary = summaryList.get(selectedRow);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String dateStr = sdf.format(selectedSummary.getDate());

                if (parent instanceof MainFrame) {
                    BillingHistoryFrame historyFrame = new BillingHistoryFrame(parent);
                    ((MainFrame) parent).showFrame(historyFrame);
                    historyFrame.setFilterAndSearch(dateStr, dateStr);
                }
            } catch (Exception e) {
                UIUtils.showErrorDialog(this, "Error", "Could not drill down: " + e.getMessage());
            }
        } else {
            UIUtils.showWarningDialog(this, "Warning", "Please select a row first.");
        }
    }

    private void handleExportExcel() {
        if (this.tableModel.getRowCount() == 0) {
            UIUtils.showWarningDialog(this, "Warning", "No data to export!");
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Export File");
        fileChooser.setSelectedFile(new File("Daily_Sales_Register_Summary.csv"));
        int selection = fileChooser.showSaveDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            try (FileWriter fw = new FileWriter(file)) {
                // Headers
                for (int col = 0; col < this.tableModel.getColumnCount(); col++) {
                    fw.append(this.tableModel.getColumnName(col));
                    if (col < this.tableModel.getColumnCount() - 1) {
                        fw.append(",");
                    }
                }
                fw.append("\n");

                // Rows
                for (int row = 0; row < this.tableModel.getRowCount(); row++) {
                    for (int col = 0; col < this.tableModel.getColumnCount(); col++) {
                        String val = String.valueOf(this.tableModel.getValueAt(row, col));
                        // Clean currency symbols for standard spreadsheet compatibility
                        val = val.replace("\u20b9", "").replace(",", "").trim();
                        if (val.contains(",") || val.contains("\"")) {
                            val = "\"" + val.replace("\"", "\"\"") + "\"";
                        }
                        fw.append(val);
                        if (col < this.tableModel.getColumnCount() - 1) {
                            fw.append(",");
                        }
                    }
                    fw.append("\n");
                }
                UIUtils.showSuccessDialog(this, "Success", "Register history exported successfully to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                UIUtils.showErrorDialog(this, "Error", "Failed to export data: " + e.getMessage());
            }
        }
    }

    public void setFilterAndSearch(String fromDateStr, String toDateStr) {
        if (fromDateField != null && toDateField != null) {
            fromDateField.setText(fromDateStr);
            toDateField.setText(toDateStr);
            loadSummaryTable();
        }
    }
}
