"""
Backup and Recovery Manager Module
Handles automatic and manual SQLite database backup, compression, listing, and restoration.
"""

import logging
import os
import shutil
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import List, Optional

from .path_manager import BACKUPS_DIR, BASE_DIR

logger = logging.getLogger(__name__)

DB_FILE = BASE_DIR / "pos_data.sqlite"


def create_backup(custom_prefix: str = "auto") -> Optional[Path]:
    """
    Create a backup of pos_data.sqlite in the BACKUPS_DIR folder.
    Returns the Path to the backup file if successful, or None on failure.
    """
    if not DB_FILE.exists():
        logger.warning("Database file %s does not exist. Cannot backup.", DB_FILE)
        return None

    try:
        BACKUPS_DIR.mkdir(parents=True, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_name = f"backup_{custom_prefix}_{timestamp}.sqlite"
        target_path = BACKUPS_DIR / backup_name

        # Safe SQLite online backup to ensure consistency even if DB is in use
        src_conn = sqlite3.connect(str(DB_FILE))
        dst_conn = sqlite3.connect(str(target_path))
        with dst_conn:
            src_conn.backup(dst_conn)
        dst_conn.close()
        src_conn.close()

        logger.info("Database backup created successfully: %s", target_path)
        return target_path
    except Exception as exc:
        logger.error("Failed to create database backup: %s", exc)
        # Fallback to direct file copy if online backup fails
        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup_name = f"backup_{custom_prefix}_{timestamp}.sqlite"
            target_path = BACKUPS_DIR / backup_name
            shutil.copy2(DB_FILE, target_path)
            logger.info("Database copy backup created successfully: %s", target_path)
            return target_path
        except Exception as copy_exc:
            logger.error("Fallback file copy backup also failed: %s", copy_exc)
            return None


def list_backups() -> List[Path]:
    """List all database backups available in BACKUPS_DIR ordered by modification time (newest first)."""
    if not BACKUPS_DIR.exists():
        return []
    backups = list(BACKUPS_DIR.glob("*.sqlite")) + list(BACKUPS_DIR.glob("*.db"))
    backups.sort(key=lambda p: p.stat().st_mtime, reverse=True)
    return backups


def restore_backup(backup_path: Path) -> bool:
    """
    Restore pos_data.sqlite from a specified backup file.
    Creates a pre-restore backup first for recovery safety.
    """
    if not backup_path.exists():
        logger.error("Backup file %s does not exist.", backup_path)
        return False

    try:
        # Step 1: Create a safety backup of current state
        if DB_FILE.exists():
            create_backup(custom_prefix="pre_restore")

        # Step 2: Perform online restore or file copy
        src_conn = sqlite3.connect(str(backup_path))
        dst_conn = sqlite3.connect(str(DB_FILE))
        with dst_conn:
            src_conn.backup(dst_conn)
        dst_conn.close()
        src_conn.close()

        logger.info("Database successfully restored from %s", backup_path)
        return True
    except Exception as exc:
        logger.error("Failed to restore database from %s: %s", backup_path, exc)
        try:
            shutil.copy2(backup_path, DB_FILE)
            logger.info("Database successfully restored via file copy from %s", backup_path)
            return True
        except Exception as copy_exc:
            logger.error("Fallback file copy restore also failed: %s", copy_exc)
            return False
