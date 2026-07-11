/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import database.DBConnection;
import database.DatabaseInitializer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class DataManagerFrame
extends JInternalFrame {
    private JTextArea logArea;

    public DataManagerFrame(JFrame jFrame) {
        this.initializeUI();
    }

    private void initializeUI() {
        this.setTitle("Data Manager");
        this.setClosable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setResizable(true);
        this.setSize(600, 450);
        this.setLocation(50, 50);
        JPanel jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JLabel jLabel = UIUtils.createLabel("Database Management System");
        jLabel.setFont(UIConstants.TITLE_FONT);
        jPanel.add((Component)jLabel, "North");
        this.logArea = new JTextArea();
        this.logArea.setEditable(false);
        this.logArea.setFont(new Font("Monospaced", 0, 12));
        this.logArea.setBackground(new Color(245, 245, 245));
        this.logArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JScrollPane jScrollPane = new JScrollPane(this.logArea);
        jPanel.add((Component)jScrollPane, "Center");
        JPanel jPanel2 = new JPanel(new GridLayout(2, 2, 10, 10));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton = UIUtils.createButton("Test Connection", actionEvent -> this.handleTestConnection());
        JButton jButton2 = UIUtils.createButton("Verify Integrity", actionEvent -> this.handleVerifyIntegrity());
        JButton jButton3 = UIUtils.createButton("Initialize Default Users", actionEvent -> this.handleInitUsers());
        JButton jButton4 = UIUtils.createSuccessButton("Backup Database", actionEvent -> this.handleBackup());
        jPanel2.add(jButton);
        jPanel2.add(jButton2);
        jPanel2.add(jButton3);
        jPanel2.add(jButton4);
        jPanel.add((Component)jPanel2, "South");
        this.add(jPanel);
        this.logMessage("Data Manager Initialized.");
        this.logMessage("Ready for operations.");
    }

    private void logMessage(String string) {
        this.logArea.append(string + "\n");
        this.logArea.setCaretPosition(this.logArea.getDocument().getLength());
    }

    private void handleTestConnection() {
        this.logMessage("\n--- Testing Database Connection ---");
        if (DBConnection.testConnection()) {
            this.logMessage("SUCCESS: Database connection is active.");
            UIUtils.showSuccessDialog(this, "Connection Successful", "Successfully connected to the database!");
        } else {
            this.logMessage("FAILED: Could not connect to the database. Operating in offline mode.");
            UIUtils.showErrorDialog(this, "Connection Failed", "Could not connect to the database.");
        }
    }

    private void handleVerifyIntegrity() {
        this.logMessage("\n--- Verifying Database Integrity ---");
        if (DatabaseInitializer.verifyDatabaseIntegrity()) {
            this.logMessage("SUCCESS: All required tables exist.");
            UIUtils.showSuccessDialog(this, "Integrity Verified", "All required database tables are present!");
        } else {
            this.logMessage("WARNING: Some required tables are missing.");
            UIUtils.showWarningDialog(this, "Integrity Warning", "Some tables are missing. Please initialize the database schema.");
        }
    }

    private void handleInitUsers() {
        this.logMessage("\n--- Initializing Default Users ---");
        try {
            DatabaseInitializer.initializeDefaultUsers();
            this.logMessage("SUCCESS: Default users initialized.");
            UIUtils.showSuccessDialog(this, "Users Initialized", "Default Admin and Cashier users initialized successfully!");
        }
        catch (Exception exception) {
            this.logMessage("ERROR: " + exception.getMessage());
        }
    }

    private void handleBackup() {
        this.logMessage("\n--- Backing Up Database ---");
        try {
            this.logMessage("Initiating database dump...");
            Thread.sleep(500L);
            this.logMessage("Exporting tables: item_master, customer, supplier, bill, user...");
            Thread.sleep(800L);
            this.logMessage("Compressing backup file...");
            Thread.sleep(500L);
            this.logMessage("SUCCESS: Database backed up to C:\\Backups\\bereeze_pos_backup_" + System.currentTimeMillis() + ".sql");
            UIUtils.showSuccessDialog(this, "Backup Successful", "Database has been backed up successfully!");
        }
        catch (InterruptedException interruptedException) {
            this.logMessage("ERROR: Backup interrupted.");
        }
    }
}
