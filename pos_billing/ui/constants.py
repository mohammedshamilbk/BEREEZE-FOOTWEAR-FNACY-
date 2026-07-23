# ============================================================
# pos_billing/ui/constants.py  (UIConstants.java → Python)
# ============================================================
"""
UI colour palette and font constants.

Mirrors Java's UIConstants exactly so the Python UI uses the
same design language as the original Swing application.
"""

# ─── Colour palette (RGB hex) ──────────────────────────────────────────────
PRIMARY_COLOR    = "#009688"   # teal
SECONDARY_COLOR  = "#36454F"   # dark slate
ACCENT_COLOR     = "#00796B"   # dark teal
DANGER_COLOR     = "#E0115F"   # crimson
WARNING_COLOR    = "#F57F17"   # amber
SUCCESS_COLOR    = "#26A69A"   # light teal
DARK_COLOR       = "#212121"   # near-black
LIGHT_COLOR      = "#F5F5F5"   # near-white
LIGHT_BG         = LIGHT_COLOR # alias for frames expecting LIGHT_BG
BORDER_COLOR     = "#BDBDBD"   # medium grey
APP_BACKGROUND   = "#FAFAFA"   # off-white

# Text on coloured backgrounds
TEXT_ON_PRIMARY   = "#FFFFFF"
TEXT_ON_SECONDARY = "#FFFFFF"
TEXT_ON_DANGER    = "#FFFFFF"
TEXT_ON_WARNING   = "#000000"
TEXT_ON_SUCCESS   = "#FFFFFF"
TEXT_ON_APP_BG    = "#212121"

# Status badge colour pairs (bg, fg)
STATUS_SUCCESS  = ("#E8F5E9", "#2E7D32")
STATUS_WARNING  = ("#FFF3E0", "#E65100")
STATUS_DANGER   = ("#FFEBEE", "#C62828")
STATUS_NEUTRAL  = ("#F5F5F5", "#616161")

# ─── Fonts ─────────────────────────────────────────────────────────────────
# Tkinter font tuples: (family, size, style)
TITLE_FONT   = ("Segoe UI", 18, "bold")
HEADING_FONT = ("Segoe UI", 14, "bold")
NORMAL_FONT  = ("Segoe UI", 11)
SMALL_FONT   = ("Segoe UI", 9)
MONO_FONT    = ("Consolas", 11)

# ─── Layout constants ──────────────────────────────────────────────────────
FRAME_WIDTH      = 1200
FRAME_HEIGHT     = 800
DIALOG_WIDTH     = 640
DIALOG_HEIGHT    = 420
PADDING          = 10
COMPONENT_HEIGHT = 30
SIDEBAR_WIDTH    = 180

APP_TITLE = "Bereezefootwearfancy"
