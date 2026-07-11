/*
 * Decompiled with CFR 0.152.
 */
package ui.frames;

import java.awt.Color;
import java.awt.Font;

public class UIConstants {
    public static final Color PRIMARY_COLOR = new Color(0, 150, 136);
    public static final Color SECONDARY_COLOR = new Color(54, 69, 79);
    public static final Color ACCENT_COLOR = new Color(0, 121, 107);
    public static final Color DANGER_COLOR = new Color(224, 17, 95);
    public static final Color WARNING_COLOR = new Color(245, 127, 23);
    public static final Color SUCCESS_COLOR = new Color(38, 166, 154);
    public static final Color DARK_COLOR = new Color(33, 33, 33);
    public static final Color LIGHT_COLOR = new Color(245, 245, 245);
    public static final Color BORDER_COLOR = new Color(189, 189, 189);
    
    // Explicit Contrast Colors
    // READABLE PAIRING GUIDELINE CONTRACTS:
    // 1. PRIMARY_COLOR/ACCENT_COLOR/SECONDARY_COLOR backgrounds MUST always pair with TEXT_ON_PRIMARY/TEXT_ON_SECONDARY (White text).
    // 2. DANGER_COLOR backgrounds MUST pair with TEXT_ON_DANGER (White text).
    // 3. SUCCESS_COLOR backgrounds MUST pair with TEXT_ON_SUCCESS (White text).
    // 4. WARNING_COLOR backgrounds MUST pair with TEXT_ON_WARNING (Black text).
    // 5. LIGHT_COLOR/APP_BACKGROUND backgrounds MUST pair with DARK_COLOR/TEXT_ON_APP_BG (Dark text).
    public static final Color APP_BACKGROUND = new Color(250, 250, 250);
    public static final Color TEXT_ON_PRIMARY = Color.WHITE;
    public static final Color TEXT_ON_SECONDARY = Color.WHITE;
    public static final Color TEXT_ON_DANGER = Color.WHITE;
    public static final Color TEXT_ON_WARNING = Color.BLACK;
    public static final Color TEXT_ON_SUCCESS = Color.WHITE;
    public static final Color TEXT_ON_APP_BG = new Color(33, 33, 33);

    // Centrally Managed High-Contrast Status Color Pairs
    // Used for status badges, table indicators, and labels to ensure dark text on light backgrounds
    public static final Color STATUS_SUCCESS_BG = new Color(232, 245, 233);
    public static final Color STATUS_SUCCESS_FG = new Color(46, 125, 50);
    public static final Color STATUS_WARNING_BG = new Color(255, 243, 224);
    public static final Color STATUS_WARNING_FG = new Color(230, 81, 0);
    public static final Color STATUS_DANGER_BG = new Color(255, 235, 235);
    public static final Color STATUS_DANGER_FG = new Color(198, 40, 40);
    public static final Color STATUS_NEUTRAL_BG = new Color(245, 245, 245);
    public static final Color STATUS_NEUTRAL_FG = new Color(97, 97, 97);
    public static final Font TITLE_FONT = new Font("Segoe UI", 1, 18);
    public static final Font HEADING_FONT = new Font("Segoe UI", 1, 14);
    public static final Font NORMAL_FONT = new Font("Segoe UI", 0, 12);
    public static final Font SMALL_FONT = new Font("Segoe UI", 0, 10);
    public static final int FRAME_WIDTH = 1200;
    public static final int FRAME_HEIGHT = 800;
    public static final int DIALOG_WIDTH = 600;
    public static final int DIALOG_HEIGHT = 400;
    public static final int PADDING = 10;
    public static final int COMPONENT_HEIGHT = 30;
    public static final String APP_TITLE = "Bareeze Footwear - POS System";
    public static final String ICON_HOME = "\u00f0\u0178\u008f\u00a0";
    public static final String ICON_SALES = "\u00f0\u0178\u2019\u00b3";
    public static final String ICON_ITEMS = "\u00f0\u0178\u201c\u00a6";
    public static final String ICON_CUSTOMERS = "\u00f0\u0178\u2018\u00a5";
    public static final String ICON_REPORTS = "\u00f0\u0178\u201c\u0160";
    public static final String ICON_SETTINGS = "\u00e2\u0161\u2122\u00ef\u00b8\u008f";
    public static final String ICON_LOGOUT = "\u00f0\u0178\u0161\u00aa";
}
