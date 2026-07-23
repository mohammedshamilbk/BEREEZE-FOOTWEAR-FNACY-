# ============================================================
# pos_billing/database/connection.py  (DBConnection.java → Python)
# ============================================================
"""
Database connection manager.

Reads credentials from config.properties (same file the Java app used)
and returns raw sqlite3 connections when MySQL is unavailable, making
the application work out-of-the-box without a MySQL server.
"""

import configparser
import logging
import os
import sqlite3
from pathlib import Path

logger = logging.getLogger(__name__)

# ─── Load config.properties ────────────────────────────────────────────────
_BASE_DIR = Path(__file__).resolve().parent.parent.parent  # project root
_CONFIG_FILE = _BASE_DIR / "config.properties"

_config = configparser.ConfigParser()
_DB_URL: str = ""
_DB_USER: str = ""
_DB_PASSWORD: str = ""
_SQLITE_PATH: Path = _BASE_DIR / "pos_data.sqlite"

def _load_properties() -> None:
    """
    Load config.properties.

    Java's .properties files have no [section] headers, but Python's
    configparser requires them.  We prepend a fake '[DEFAULT]' header
    in memory so we can parse the file without modifying it on disk.
    """
    global _DB_URL, _DB_USER, _DB_PASSWORD
    if _CONFIG_FILE.exists():
        try:
            raw = _CONFIG_FILE.read_text(encoding="utf-8")
            _config.read_string("[DEFAULT]\n" + raw)
            _DB_URL      = _config.get("DEFAULT", "db.url",      fallback="")
            _DB_USER     = _config.get("DEFAULT", "db.user",     fallback="")
            _DB_PASSWORD = _config.get("DEFAULT", "db.password", fallback="")
            logger.info("config.properties loaded.")
        except Exception as exc:
            logger.warning("Could not parse config.properties (%s) – using SQLite fallback.", exc)
    else:
        logger.warning("config.properties not found – using SQLite fallback.")

_load_properties()

# ─── Active connection counter & MySQL availability status ───────────────────
_active_connections: int = 0
_mysql_available: bool | None = None
_mysql_warning_logged: bool = False


# ─── Public API ─────────────────────────────────────────────────────────────

def get_connection() -> sqlite3.Connection:
    """
    Return a database connection.

    Tries MySQL (via mysql-connector-python) if db.url is configured;
    falls back to SQLite otherwise. Memoizes the fallback state so
    connection errors/warnings are only logged once rather than spammed on every query.
    """
    global _active_connections, _mysql_available, _mysql_warning_logged
    if _DB_URL and _mysql_available is not False:
        try:
            import mysql.connector  # type: ignore
            # Parse jdbc:mysql://host:port/dbname  →  host, port, database
            # Example: jdbc:mysql://localhost:3306/bereeze_pos
            url_part = _DB_URL.replace("jdbc:mysql://", "")
            host_port, db_name = url_part.split("/", 1)
            if ":" in host_port:
                host, port_str = host_port.split(":", 1)
                port = int(port_str)
            else:
                host, port = host_port, 3306
            conn = mysql.connector.connect(
                host=host,
                port=port,
                database=db_name,
                user=_DB_USER,
                password=_DB_PASSWORD,
                autocommit=False,
            )
            _active_connections += 1
            _mysql_available = True
            logger.debug("MySQL connection obtained.")
            return conn  # type: ignore[return-value]
        except Exception as exc:
            _mysql_available = False
            if not _mysql_warning_logged:
                logger.warning("MySQL connection failed (%s) – using SQLite fallback.", exc)
                _mysql_warning_logged = True

    # SQLite fallback
    conn = sqlite3.connect(str(_SQLITE_PATH), check_same_thread=False)
    conn.row_factory = sqlite3.Row
    _active_connections += 1
    logger.debug("SQLite connection obtained: %s", _SQLITE_PATH)
    return conn


def close_connection(conn) -> None:
    """Close the given connection and decrement the counter."""
    global _active_connections
    try:
        conn.close()
        _active_connections = max(0, _active_connections - 1)
    except Exception:
        pass


def get_pool_stats() -> str:
    mode = "MySQL" if _mysql_available else "SQLite"
    return f"{mode} Direct Connection Mode (Active: {_active_connections})"


def test_connection() -> bool:
    """Return True if a connection can be obtained."""
    global _mysql_available, _mysql_warning_logged
    _mysql_available = None
    _mysql_warning_logged = False
    try:
        conn = get_connection()
        close_connection(conn)
        return True
    except Exception as exc:
        logger.error("Connection test failed: %s", exc)
        return False
