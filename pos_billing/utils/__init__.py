# ============================================================
# pos_billing/utils/security.py  (SecurityUtils.java → Python)
# ============================================================
"""Password hashing utilities – mirrors Java SecurityUtils."""

import hashlib
from .qr_generator import generate_upi_qr, get_store_upi_id, show_live_upi_dialog
from .excel_exporter import export_table_to_excel, export_all_days_sales_to_excel, export_day_closing_to_excel, is_excel_available
from .auto_save_manager import auto_save_database, auto_save_daily_excel, save_draft_cart, load_draft_cart, clear_draft_cart, trigger_auto_save

__all__ = [
    "hash_password", "authenticate", "generate_upi_qr", "get_store_upi_id", "show_live_upi_dialog",
    "export_table_to_excel", "export_all_days_sales_to_excel", "export_day_closing_to_excel", "is_excel_available",
    "auto_save_database", "auto_save_daily_excel", "save_draft_cart", "load_draft_cart", "clear_draft_cart", "trigger_auto_save"
]



def hash_password(password: str, salt: str = "") -> str:
    """
    Hash a password with SHA-256.

    Args:
        password: Plain-text password.
        salt:     Optional per-user salt (username used as salt in legacy mode).

    Returns:
        Hex-encoded SHA-256 digest.
    """
    if password is None:
        return ""
    raw = f"{password}::{salt}" if salt else password
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def authenticate(stored_password: str, input_password: str, username: str = "") -> bool:
    """
    Verify a password against a stored value.

    Supports three legacy modes (matching Java's User.authenticate):
    1. Plain-text comparison.
    2. Unsalted SHA-256 hash.
    3. Salted SHA-256 hash (password::username).

    Args:
        stored_password: The password value from the database / user record.
        input_password:  The password the user typed.
        username:        Used as the salt for salted-hash mode.

    Returns:
        True if the password matches, False otherwise.
    """
    if not stored_password or not input_password:
        return False

    # Mode 1 – plain text
    if stored_password == input_password:
        return True

    # Mode 2 – unsalted SHA-256
    if stored_password == hash_password(input_password):
        return True

    # Mode 3 – salted SHA-256
    if username and stored_password == hash_password(input_password, username):
        return True

    return False
