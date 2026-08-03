# ============================================================
# pos_billing/utils/auto_save_manager.py
# ============================================================
"""
Auto-Save & Auto-Backup Manager Module.
Ensures ALL data (database transactions, daily sales Excel exports, and billing draft carts)
are saved automatically without requiring manual user intervention.
"""

from __future__ import annotations

import atexit
import json
import logging
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Optional

from .path_manager import BACKUPS_DIR, EXPORTS_DIR, RECORDS_DIR
from .backup_manager import create_backup, list_backups
from .excel_exporter import export_all_days_sales_to_excel

logger = logging.getLogger(__name__)

# Throttling state for DB backups
_last_backup_time: float = 0.0
_backup_lock = threading.Lock()
_timer_started: bool = False
DRAFT_CART_FILE = RECORDS_DIR / "draft_cart.json"


def auto_save_database(prefix: str = "autosave") -> Optional[Path]:
    """
    Perform automatic database backup and clean up older backup files beyond limit.
    """
    global _last_backup_time
    with _backup_lock:
        try:
            backup_path = create_backup(custom_prefix=prefix)
            _last_backup_time = time.time()

            # Prune old backups (keep latest 30 files)
            all_backups = list_backups()
            if len(all_backups) > 30:
                for old_file in all_backups[30:]:
                    try:
                        old_file.unlink()
                        logger.info("Pruned old auto-backup: %s", old_file)
                    except Exception as exc:
                        logger.debug("Failed to prune old backup %s: %s", old_file, exc)
            return backup_path
        except Exception as exc:
            logger.error("auto_save_database failed: %s", exc)
            return None


def auto_save_daily_excel() -> Optional[str]:
    """
    Automatically export current all-days sales data to Excel.
    Saves to exports/daily_sales_autosave.xlsx and exports/daily_sales_autosave_YYYYMMDD.xlsx.
    """
    try:
        main_path = EXPORTS_DIR / "daily_sales_autosave.xlsx"
        out = export_all_days_sales_to_excel(filepath=str(main_path))

        today_stamp = datetime.now().strftime("%Y%m%d")
        dated_path = EXPORTS_DIR / f"daily_sales_autosave_{today_stamp}.xlsx"
        export_all_days_sales_to_excel(filepath=str(dated_path))

        logger.info("Auto-saved daily sales Excel report to %s", out)
        return out
    except Exception as exc:
        logger.error("auto_save_daily_excel failed: %s", exc)
        return None


def save_draft_cart(cart_data: dict) -> bool:
    """Auto-save pending billing cart items to draft_cart.json."""
    try:
        RECORDS_DIR.mkdir(parents=True, exist_ok=True)
        with open(DRAFT_CART_FILE, "w", encoding="utf-8") as f:
            json.dump(cart_data, f, indent=2, default=str)
        return True
    except Exception as exc:
        logger.error("save_draft_cart failed: %s", exc)
        return False


def load_draft_cart() -> Optional[dict]:
    """Load pending billing cart items from draft_cart.json if present."""
    if not DRAFT_CART_FILE.exists():
        return None
    try:
        with open(DRAFT_CART_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as exc:
        logger.error("load_draft_cart failed: %s", exc)
        return None


def clear_draft_cart() -> bool:
    """Clear draft_cart.json when a bill is successfully saved or cleared."""
    if DRAFT_CART_FILE.exists():
        try:
            DRAFT_CART_FILE.unlink()
            return True
        except Exception as exc:
            logger.error("clear_draft_cart failed: %s", exc)
            return False
    return True


def trigger_auto_save(event_name: str = "data_change", force_db_backup: bool = False) -> None:
    """
    Non-blocking auto-save handler.
    Automatically saves daily Excel export and triggers throttled DB backup in background thread.
    """
    def _background_worker():
        global _last_backup_time
        try:
            # 1. Always update daily sales Excel file
            auto_save_daily_excel()

            # 2. Check if DB backup is needed (if forced or > 120 seconds since last backup)
            now = time.time()
            if force_db_backup or (now - _last_backup_time > 120):
                auto_save_database(prefix=f"auto_{event_name}")
        except Exception as exc:
            logger.error("Background auto-save error: %s", exc)

    t = threading.Thread(target=_background_worker, daemon=True, name="AutoSaveThread")
    t.start()


def start_auto_save_background_timer(interval_seconds: int = 300) -> None:
    """
    Start a background daemon thread that periodically auto-saves DB and Excel files every interval_seconds.
    """
    global _timer_started
    if _timer_started:
        return
    _timer_started = True

    def _timer_loop():
        logger.info("Auto-save background timer started (interval: %d seconds)", interval_seconds)
        while True:
            time.sleep(interval_seconds)
            try:
                auto_save_database(prefix="periodic")
                auto_save_daily_excel()
            except Exception as exc:
                logger.error("Periodic auto-save error: %s", exc)

    timer_thread = threading.Thread(target=_timer_loop, daemon=True, name="AutoSaveTimerThread")
    timer_thread.start()


def _on_shutdown():
    """Emergency shutdown hook to perform final auto-save of database and Excel files."""
    try:
        logger.info("Application shutting down: executing final auto-save...")
        auto_save_database(prefix="shutdown")
        auto_save_daily_excel()
    except Exception as exc:
        logger.error("Shutdown auto-save error: %s", exc)


# Register automatic shutdown hook
atexit.register(_on_shutdown)

# Start periodic auto-save timer on module load (5 min interval)
start_auto_save_background_timer(interval_seconds=300)
