/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import ui.frames.UIConstants;
import ui.frames.UIUtils;

public class UpdateDialog
extends JDialog {
    private JProgressBar progressBar;
    private JButton updateBtn;
    private JLabel statusLabel;
    private JTextArea releaseNotesArea;

    public UpdateDialog(JFrame jFrame) {
        super(jFrame, "Software Update Manager", true);
        this.initializeUI();
    }

    private void initializeUI() {
        this.setSize(500, 400);
        this.setLocationRelativeTo(this.getParent());
        this.setResizable(false);
        this.setLayout(new BorderLayout());
        JPanel jPanel = new JPanel(new BorderLayout(15, 15));
        jPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel2 = new JPanel(new GridLayout(2, 1));
        jPanel2.setBackground(UIConstants.APP_BACKGROUND);
        JLabel jLabel = UIUtils.createLabel("Bareeze Footwear POS Updater");
        jLabel.setFont(UIConstants.TITLE_FONT);
        JLabel jLabel2 = UIUtils.createLabel("Current Version: 1.0.0  |  Latest Version: 1.1.0");
        jLabel2.setFont(UIConstants.HEADING_FONT);
        jLabel2.setForeground(UIConstants.PRIMARY_COLOR);
        jPanel2.add(jLabel);
        jPanel2.add(jLabel2);
        jPanel.add((Component)jPanel2, "North");
        JPanel jPanel3 = new JPanel(new BorderLayout(5, 5));
        jPanel3.setBackground(UIConstants.APP_BACKGROUND);
        jPanel3.add((Component)UIUtils.createLabel("What's New in Version 1.1.0:"), "North");
        this.releaseNotesArea = new JTextArea();
        this.releaseNotesArea.setEditable(false);
        this.releaseNotesArea.setFont(UIConstants.NORMAL_FONT);
        this.releaseNotesArea.setText("\u00e2\u20ac\u00a2 Added Data Manager module for database backups and integrity checks.\n\u00e2\u20ac\u00a2 Improved Supplier Ledger with Quick Direct Bill Entry.\n\u00e2\u20ac\u00a2 Added direct 'Save Supplier Data' in Purchase Entry.\n\u00e2\u20ac\u00a2 Added 'Edit Supplier Data' button in Supplier Ledgers.\n\u00e2\u20ac\u00a2 Added Keyboard 'Enter' key shortcuts on Login Screen.\n\u00e2\u20ac\u00a2 Enhanced dropdown behavior for newly added suppliers.\n\u00e2\u20ac\u00a2 Bug fixes and performance improvements.");
        this.releaseNotesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.releaseNotesArea.setBackground(new Color(245, 245, 250));
        JScrollPane jScrollPane = new JScrollPane(this.releaseNotesArea);
        jScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        jPanel3.add((Component)jScrollPane, "Center");
        jPanel.add((Component)jPanel3, "Center");
        JPanel jPanel4 = new JPanel(new BorderLayout(10, 10));
        jPanel4.setBackground(UIConstants.APP_BACKGROUND);
        JPanel jPanel5 = new JPanel(new BorderLayout(5, 5));
        jPanel5.setBackground(UIConstants.APP_BACKGROUND);
        this.statusLabel = UIUtils.createLabel("Ready to download update...");
        this.statusLabel.setFont(UIConstants.SMALL_FONT);
        this.progressBar = new JProgressBar(0, 100);
        this.progressBar.setStringPainted(true);
        this.progressBar.setVisible(false);
        jPanel5.add((Component)this.statusLabel, "North");
        jPanel5.add((Component)this.progressBar, "Center");
        JPanel jPanel6 = new JPanel(new FlowLayout(2));
        jPanel6.setBackground(UIConstants.APP_BACKGROUND);
        JButton jButton = UIUtils.createButton("Close", actionEvent -> this.dispose());
        this.updateBtn = UIUtils.createSuccessButton("Download & Install Update", actionEvent -> this.startUpdateProcess());
        jPanel6.add(jButton);
        jPanel6.add(this.updateBtn);
        jPanel4.add((Component)jPanel5, "Center");
        jPanel4.add((Component)jPanel6, "South");
        jPanel.add((Component)jPanel4, "South");
        this.add(jPanel);
    }

    private void startUpdateProcess() {
        this.updateBtn.setEnabled(false);
        this.progressBar.setVisible(true);
        this.progressBar.setValue(0);
        this.statusLabel.setText("Connecting to update server...");
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int progress;
            @Override
            public void run() {
                this.progress += 5;
                if (this.progress <= 30) {
                    SwingUtilities.invokeLater(() -> UpdateDialog.this.statusLabel.setText("Downloading update packages... (" + this.progress + "%)"));
                } else if (this.progress <= 70) {
                    SwingUtilities.invokeLater(() -> UpdateDialog.this.statusLabel.setText("Extracting files... (" + this.progress + "%)"));
                } else if (this.progress < 100) {
                    SwingUtilities.invokeLater(() -> UpdateDialog.this.statusLabel.setText("Installing new components... (" + this.progress + "%)"));
                } else {
                    SwingUtilities.invokeLater(() -> {
                        UpdateDialog.this.statusLabel.setText("Update complete!");
                        UpdateDialog.this.progressBar.setValue(100);
                        UpdateDialog.this.statusLabel.setForeground(UIConstants.SUCCESS_COLOR);
                        UIUtils.showSuccessDialog(UpdateDialog.this, "Update Complete", "Software has been successfully updated to version 1.1.0!\n\nPlease restart the application to apply the changes.");
                        UpdateDialog.this.dispose();
                    });
                    timer.cancel();
                    return;
                }
                SwingUtilities.invokeLater(() -> UpdateDialog.this.progressBar.setValue(this.progress));
            }
        }, 500L, 200L);
    }
}
