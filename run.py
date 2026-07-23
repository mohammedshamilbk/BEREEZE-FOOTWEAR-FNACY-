"""
run.py  -  Bereeze Footwear POS Billing System
==============================================
Double-click this file OR run:  python run.py
"""
import sys
import os

# Add project root to path so 'pos_billing' package is always found
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

# ── Ensure Required Directories ─────────────────────────────────────────
try:
    from pos_billing.utils.path_manager import ensure_directories_exist
    ensure_directories_exist()
except Exception as e:
    print(f"[WARN] Directory check skipped: {e}")

# ── DB init ──────────────────────────────────────────────────────────────
try:
    from pos_billing.database.db_init import initialize_database
    initialize_database()
    print("[OK] Database ready.")
except Exception as e:
    print(f"[WARN] DB init skipped: {e}")

# ── Launch UI ─────────────────────────────────────────────────────────────
from pos_billing.ui.app import App

if __name__ == "__main__":
    app = App()
    app.mainloop()
