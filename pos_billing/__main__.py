# ============================================================
# pos_billing/__main__.py
# ============================================================
"""Allows running with:  python -m pos_billing"""

from pathlib import Path
import sys

# Ensure project root directory is in sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from pos_billing.main import main

if __name__ == "__main__":
    main()
