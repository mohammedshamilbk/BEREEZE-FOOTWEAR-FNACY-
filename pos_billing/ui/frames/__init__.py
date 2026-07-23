# pos_billing/ui/frames/__init__.py
"""Sub-frames package."""

from .dashboard_frame import DashboardFrame
from .pos_sale_frame import POSSaleFrame
from .inventory_frame import InventoryFrame
from .customer_frame import CustomerFrame
from .supplier_frame import SupplierFrame
from .reports_frame import ReportsFrame
from .user_frame import UserFrame
from .barcode_print_frame import BarcodePrintFrame

__all__ = [
    "DashboardFrame",
    "POSSaleFrame",
    "InventoryFrame",
    "CustomerFrame",
    "SupplierFrame",
    "ReportsFrame",
    "UserFrame",
    "BarcodePrintFrame",
]
