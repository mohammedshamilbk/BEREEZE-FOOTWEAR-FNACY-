/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.Font;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import ui.frames.UIConstants;

public class UIUtils {
    public static JButton createButton(String string, ActionListener actionListener) {
        JButton jButton = new JButton(string);
        jButton.setFont(UIConstants.NORMAL_FONT);
        jButton.setForeground(UIConstants.TEXT_ON_PRIMARY);
        jButton.setBackground(UIConstants.PRIMARY_COLOR);
        jButton.setBorderPainted(false);
        jButton.setFocusPainted(false);
        jButton.setCursor(new Cursor(12));
        if (actionListener != null) {
            jButton.addActionListener(actionListener);
        }
        return jButton;
    }

    public static JButton createPrimaryButton(String string, ActionListener actionListener) {
        return UIUtils.createButton(string, actionListener);
    }

    public static JButton createSuccessButton(String string, ActionListener actionListener) {
        JButton jButton = UIUtils.createButton(string, actionListener);
        jButton.setBackground(UIConstants.SUCCESS_COLOR);
        jButton.setForeground(UIConstants.TEXT_ON_SUCCESS);
        return jButton;
    }

    public static JButton createDangerButton(String string, ActionListener actionListener) {
        JButton jButton = UIUtils.createButton(string, actionListener);
        jButton.setBackground(UIConstants.DANGER_COLOR);
        jButton.setForeground(UIConstants.TEXT_ON_DANGER);
        return jButton;
    }

    public static JLabel createLabel(String string) {
        JLabel jLabel = new JLabel(string);
        jLabel.setFont(UIConstants.NORMAL_FONT);
        jLabel.setForeground(UIConstants.DARK_COLOR);
        return jLabel;
    }

    public static JLabel createHeadingLabel(String string) {
        JLabel jLabel = new JLabel(string);
        jLabel.setFont(UIConstants.HEADING_FONT);
        jLabel.setForeground(UIConstants.PRIMARY_COLOR);
        return jLabel;
    }

    public static JLabel createTitleLabel(String string) {
        JLabel jLabel = new JLabel(string);
        jLabel.setFont(UIConstants.TITLE_FONT);
        jLabel.setForeground(UIConstants.DARK_COLOR);
        return jLabel;
    }

    public static JTextField createTextField(int n) {
        JTextField jTextField = new JTextField(n);
        jTextField.setFont(UIConstants.NORMAL_FONT);
        jTextField.setPreferredSize(new Dimension(200, 30));
        jTextField.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        return jTextField;
    }

    public static JPasswordField createPasswordField(int n) {
        JPasswordField jPasswordField = new JPasswordField(n);
        jPasswordField.setFont(UIConstants.NORMAL_FONT);
        jPasswordField.setPreferredSize(new Dimension(200, 30));
        jPasswordField.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        return jPasswordField;
    }

    public static JComboBox<String> createComboBox(String[] stringArray) {
        JComboBox<String> jComboBox = new JComboBox<String>(stringArray);
        jComboBox.setFont(UIConstants.NORMAL_FONT);
        jComboBox.setPreferredSize(new Dimension(200, 30));
        return jComboBox;
    }

    public static JCheckBox createCheckBox(String string) {
        JCheckBox jCheckBox = new JCheckBox(string);
        jCheckBox.setFont(UIConstants.NORMAL_FONT);
        jCheckBox.setForeground(UIConstants.DARK_COLOR);
        return jCheckBox;
    }

    public static JTextArea createTextArea(int n, int n2) {
        JTextArea jTextArea = new JTextArea(n, n2);
        jTextArea.setFont(UIConstants.NORMAL_FONT);
        jTextArea.setLineWrap(true);
        jTextArea.setWrapStyleWord(true);
        jTextArea.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        return jTextArea;
    }

    public static JPanel createInputPanel(String string, JComponent jComponent) {
        JPanel jPanel = new JPanel(new FlowLayout(0, 10, 5));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        jPanel.add(UIUtils.createLabel(string));
        jPanel.add(jComponent);
        return jPanel;
    }

    public static JPanel createButtonPanel(JButton ... jButtonArray) {
        JPanel jPanel = new JPanel(new FlowLayout(2, 10, 5));
        jPanel.setBackground(UIConstants.APP_BACKGROUND);
        for (JButton jButton : jButtonArray) {
            jPanel.add(jButton);
        }
        return jPanel;
    }

    public static void showErrorDialog(Component component, String string, String string2) {
        JOptionPane.showMessageDialog(component, string2, string, 0);
    }

    public static void showSuccessDialog(Component component, String string, String string2) {
        JOptionPane.showMessageDialog(component, string2, string, 1);
    }

    public static void showWarningDialog(Component component, String string, String string2) {
        JOptionPane.showMessageDialog(component, string2, string, 2);
    }

    public static boolean showConfirmDialog(Component component, String string, String string2) {
        int n = JOptionPane.showConfirmDialog(component, string2, string, 0);
        return n == 0;
    }

    public static JScrollPane createTableScrollPane(JTable jTable) {
        JScrollPane jScrollPane = new JScrollPane(jTable);
        jScrollPane.setBackground(UIConstants.APP_BACKGROUND);
        jTable.setBackground(UIConstants.APP_BACKGROUND);
        jTable.setFont(UIConstants.NORMAL_FONT);
        jTable.setRowHeight(25);
        
        jTable.getTableHeader().setDefaultRenderer(new javax.swing.table.TableCellRenderer() {
            private final javax.swing.JLabel label = new javax.swing.JLabel() {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    g.setColor(getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                    super.paintComponent(g);
                }
            };

            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                label.setText(value != null ? value.toString() : "");
                label.setBackground(UIConstants.PRIMARY_COLOR);
                label.setForeground(UIConstants.TEXT_ON_PRIMARY);
                label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
                label.setOpaque(true);
                label.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 1, java.awt.Color.LIGHT_GRAY));
                label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                return label;
            }
        });

        jTable.getTableHeader().setBackground(UIConstants.PRIMARY_COLOR); // Fallback
        jTable.getTableHeader().setForeground(UIConstants.TEXT_ON_PRIMARY);
        jTable.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        
        return jScrollPane;
    }

    private static final java.util.Map<String, javax.swing.ImageIcon> iconCache = new java.util.HashMap<>();

    public static javax.swing.ImageIcon loadImageIcon(String path, int width, int height) {
        String key = path + "_" + width + "x" + height;
        if (iconCache.containsKey(key)) {
            return iconCache.get(key);
        }
        try {
            java.net.URL imgURL = UIUtils.class.getResource(path);
            if (imgURL != null) {
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(imgURL);
                java.awt.Image img = icon.getImage();
                java.awt.Image newImg = img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
                javax.swing.ImageIcon scaledIcon = new javax.swing.ImageIcon(newImg);
                iconCache.put(key, scaledIcon);
                return scaledIcon;
            } else {
                System.err.println("Resource image not found at classpath path: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static javax.swing.ImageIcon loadLogoIcon(int width, int height) {
        return loadImageIcon("/resources/images/logo.png", width, height);
    }
}
