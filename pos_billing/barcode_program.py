# ============================================================
# pos_billing/barcode_program.py
# ============================================================
"""
Standalone Launcher for the Barcode Point & Print Program.
Allows instant launch from terminal or desktop shortcuts.
"""

import logging
import sys
import tkinter as tk
from pathlib import Path
from tkinter import messagebox

# Ensure project root directory is in sys.path so 'pos_billing' package imports resolve correctly
PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s – %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("pos_billing_barcode.log", encoding="utf-8"),
    ],
)
logger = logging.getLogger(__name__)


def main() -> None:
    logger.info("Launching Standalone Barcode Point & Print Program...")

    # 1. Init Database
    try:
        from pos_billing.database.db_init import initialize_database
        initialize_database()
        logger.info("Database initialized.")
    except Exception as exc:
        logger.error("Database init failed: %s", exc)

    # 2. Get Default User or Admin
    from pos_billing.database.models import User
    from pos_billing.database import dao
    
    users = dao.get_all_users()
    admin_user = users[0] if users else User("admin", "admin", "Administrator", "ADMIN", 1)

    # 3. Launch Barcode GUI Window
    root = tk.Tk()
    root.title("🏷️ Bereezefootwearfancy - Barcode Program")
    root.geometry("1100x780")
    root.minsize(900, 650)
    try:
        root.state("zoomed")
    except Exception:
        sw, sh = root.winfo_screenwidth(), root.winfo_screenheight()
        root.geometry(f"{sw}x{sh}+0+0")

    try:
        from pos_billing.ui.frames.barcode_print_frame import BarcodePrintFrame
        frame = BarcodePrintFrame(root, user=admin_user, on_exit=root.destroy)
        frame.pack(fill=tk.BOTH, expand=True)
        root.mainloop()
    except Exception as exc:
        logger.exception("Fatal error launching Barcode Program UI: %s", exc)
        if tk._default_root:
            messagebox.showerror("Fatal Error", f"Could not launch Barcode Program:\n{exc}")
        sys.exit(1)


if __name__ == "__main__":
    main()
