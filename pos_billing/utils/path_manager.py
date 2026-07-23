"""
Path Manager Module
Centralizes application directory locations and guarantees required directories exist.
"""

import os
from pathlib import Path

# Base project root directory
BASE_DIR = Path(__file__).resolve().parent.parent.parent

# Standard application directories
RECORDS_DIR = BASE_DIR / "records"
EXPORTS_DIR = BASE_DIR / "exports"
BACKUPS_DIR = BASE_DIR / "backups"
CONFIG_DIR = BASE_DIR / "config"
TEMP_DIR = BASE_DIR / "temp"
LOGS_DIR = BASE_DIR / "logs"

REQUIRED_DIRS = [
    RECORDS_DIR,
    EXPORTS_DIR,
    BACKUPS_DIR,
    CONFIG_DIR,
    TEMP_DIR,
    LOGS_DIR,
]


def ensure_directories_exist():
    """Ensure all required runtime directories exist."""
    for directory in REQUIRED_DIRS:
        directory.mkdir(parents=True, exist_ok=True)


def sanitize_filename(filename: str) -> str:
    """Sanitize filename to prevent directory traversal attacks."""
    clean = os.path.basename(filename)
    clean = "".join(c for c in clean if c.isalnum() or c in ("-", "_", "."))
    return clean or "unnamed_file"


def safe_path_join(base_dir: Path, filename: str) -> Path:
    """Safely resolve a path inside base_dir ensuring no traversal outside base_dir."""
    sanitized = sanitize_filename(filename)
    target = (base_dir / sanitized).resolve()
    if not str(target).startswith(str(base_dir.resolve())):
        raise ValueError("Invalid path: potential directory traversal attempt")
    return target


# Automatically initialize directories on import
ensure_directories_exist()
