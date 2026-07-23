# ============================================================
# pos_billing/main.py  (POSBillingSystem.java → Python)
# ============================================================
"""
Application entry point.

Mirrors the Java main() in POSBillingSystem.java:
  1. Initialise the database (create tables + seed data).
  2. Open the Login window.
"""

import logging
import sys
from pathlib import Path

# Ensure project root directory is in sys.path so 'pos_billing' package imports resolve correctly
PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

# ── Logging setup ──────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s – %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("pos_billing.log", encoding="utf-8"),
    ],
)
logger = logging.getLogger(__name__)


def main() -> None:
    """Initialise the database and launch the POS system UI."""
    logger.info("Starting Bereeze Footwear POS Billing System…")

    # 1. Init database
    try:
        from pos_billing.database.db_init import initialize_database
        initialize_database()
        logger.info("Database ready.")
    except Exception as exc:
        logger.error("Database initialisation failed: %s", exc)
        # Continue anyway – the app may still work in offline / in-memory mode

    # 2. Launch UI
    try:
        from pos_billing.ui.login_frame import LoginFrame
        app = LoginFrame()
        app.mainloop()
    except Exception as exc:
        logger.exception("Fatal error launching UI: %s", exc)
        sys.exit(1)


if __name__ == "__main__":
    main()
