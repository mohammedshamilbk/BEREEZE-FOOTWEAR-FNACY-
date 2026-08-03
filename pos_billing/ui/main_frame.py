# ============================================================
# pos_billing/ui/main_frame.py  (MainFrame.java → Python)
# ============================================================
"""
Main application window with sidebar navigation.

Mirrors Java's MainFrame: sidebar buttons navigate between panels
(Dashboard, POS Sale, Items, Customers, Suppliers, Reports, Users).
"""

from __future__ import annotations

import tkinter as tk
from tkinter import messagebox
from typing import Callable, Optional

from ..database.models import User
from .constants import (
    APP_BACKGROUND, APP_TITLE, BORDER_COLOR, DARK_COLOR,
    FRAME_HEIGHT, FRAME_WIDTH, HEADING_FONT, NORMAL_FONT,
    PRIMARY_COLOR, SECONDARY_COLOR, SIDEBAR_WIDTH,
    SMALL_FONT, TEXT_ON_APP_BG, TEXT_ON_PRIMARY, TITLE_FONT,
)
from .widgets import create_label


class MainFrame(tk.Toplevel):
    """
    Main application window.

    Args:
        user:       The currently logged-in User.
        on_logout:  Callback invoked when the user clicks Logout.
    """

    _NAV_ITEMS = [
        ("🏠  Dashboard",     "dashboard"),
        ("🛒  POS Sale",      "pos_sale"),
        ("📦  Inventory",     "inventory"),
        ("🏷️  Barcode Print", "barcode_print"),
        ("💸  Expenses",      "expenses"),
        ("👥  Customers",     "customers"),
        ("🚚  Suppliers",     "suppliers"),
        ("📊  Reports",       "reports"),
        ("👤  Users",         "users"),
    ]

    def __init__(self, user: User, on_logout: Optional[Callable] = None) -> None:
        super().__init__()
        self.current_user = user
        self._on_logout_cb = on_logout
        self._active_frame: Optional[tk.Widget] = None
        self._active_nav: Optional[str] = None

        self.title(APP_TITLE)
        self.geometry(f"{FRAME_WIDTH}x{FRAME_HEIGHT}")
        self.configure(bg=APP_BACKGROUND)
        self.protocol("WM_DELETE_WINDOW", self._confirm_exit)

        # Apply official Bereeze Footwear Fancy app icon
        try:
            from .widgets import apply_app_icon
            apply_app_icon(self)
        except Exception:
            pass

        # Launch main app in Full Screen / Maximized state
        try:
            self.state("zoomed")
        except Exception:
            sw, sh = self.winfo_screenwidth(), self.winfo_screenheight()
            self.geometry(f"{sw}x{sh}+0+0")

        self._build_ui()
        self._navigate("dashboard")  # default page

    # ── UI construction ────────────────────────────────────────────────────
    def _build_ui(self) -> None:
        # Top bar
        topbar = tk.Frame(self, bg=PRIMARY_COLOR, height=52)
        topbar.pack(fill=tk.X)
        topbar.pack_propagate(False)

        tk.Label(
            topbar, text=f"  👟  {APP_TITLE}",
            font=HEADING_FONT, bg=PRIMARY_COLOR, fg=TEXT_ON_PRIMARY,
        ).pack(side=tk.LEFT, padx=10)

        tk.Label(
            topbar,
            text=f"👤 {self.current_user.full_name}  [{self.current_user.role}]",
            font=SMALL_FONT, bg=PRIMARY_COLOR, fg="#E0F2F1",
        ).pack(side=tk.RIGHT, padx=12)

        # Body row (sidebar + content)
        body = tk.Frame(self, bg=APP_BACKGROUND)
        body.pack(fill=tk.BOTH, expand=True)

        # Sidebar
        self._sidebar = tk.Frame(body, bg=SECONDARY_COLOR, width=SIDEBAR_WIDTH)
        self._sidebar.pack(side=tk.LEFT, fill=tk.Y)
        self._sidebar.pack_propagate(False)
        self._build_sidebar()

        # Separator
        tk.Frame(body, bg=BORDER_COLOR, width=1).pack(side=tk.LEFT, fill=tk.Y)

        # Content area
        self._content = tk.Frame(body, bg=APP_BACKGROUND)
        self._content.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

    def _build_sidebar(self) -> None:
        # App logo area
        logo_loaded = False
        try:
            import os
            from PIL import Image, ImageTk
            from .widgets import _ICON_PNG
            if os.path.exists(_ICON_PNG):
                img = Image.open(_ICON_PNG).convert("RGBA")
                img_resized = img.resize((64, 64), Image.Resampling.LANCZOS)
                photo = ImageTk.PhotoImage(img_resized)
                lbl_logo = tk.Label(self._sidebar, image=photo, bg=SECONDARY_COLOR)
                lbl_logo._photo = photo
                lbl_logo.pack(pady=(12, 4))
                logo_loaded = True
        except Exception:
            logo_loaded = False

        tk.Label(
            self._sidebar, text="BEREEZE\nFOOTWEAR FANCY",
            font=("Segoe UI", 10, "bold"),
            bg=SECONDARY_COLOR, fg=TEXT_ON_PRIMARY,
            pady=4 if logo_loaded else 16,
        ).pack(fill=tk.X)

        tk.Frame(self._sidebar, bg=BORDER_COLOR, height=1).pack(fill=tk.X)

        # Nav buttons with smooth animated hover effects
        self._nav_buttons: dict[str, tk.Button] = {}
        for label, key in self._NAV_ITEMS:
            btn = tk.Button(
                self._sidebar, text=f"  {label}",
                font=NORMAL_FONT, relief=tk.FLAT, bd=0,
                bg=SECONDARY_COLOR, fg=TEXT_ON_PRIMARY,
                activebackground=PRIMARY_COLOR, activeforeground=TEXT_ON_PRIMARY,
                anchor=tk.W, padx=12, pady=10, cursor="hand2",
                command=lambda k=key: self._navigate(k),
            )
            btn.pack(fill=tk.X)
            self._nav_buttons[key] = btn

            # Animated Hover Bindings
            def _on_enter(e, b=btn, k=key):
                if getattr(self, "_active_nav", None) != k:
                    b.config(bg="#00796B")

            def _on_leave(e, b=btn, k=key):
                if getattr(self, "_active_nav", None) == k:
                    b.config(bg=PRIMARY_COLOR)
                else:
                    b.config(bg=SECONDARY_COLOR)

            btn.bind("<Enter>", _on_enter)
            btn.bind("<Leave>", _on_leave)

        # Spacer + Logout at bottom
        tk.Frame(self._sidebar, bg=SECONDARY_COLOR).pack(fill=tk.BOTH, expand=True)
        tk.Frame(self._sidebar, bg=BORDER_COLOR, height=1).pack(fill=tk.X)
        logout_btn = tk.Button(
            self._sidebar, text="  🚪  Logout",
            font=NORMAL_FONT, relief=tk.FLAT, bd=0,
            bg=SECONDARY_COLOR, fg="#FF8A65",
            activebackground="#B71C1C", activeforeground=TEXT_ON_PRIMARY,
            anchor=tk.W, padx=12, pady=10, cursor="hand2",
            command=self._logout,
        )
        logout_btn.pack(fill=tk.X)
        logout_btn.bind("<Enter>", lambda e: logout_btn.config(bg="#B71C1C", fg="white"))
        logout_btn.bind("<Leave>", lambda e: logout_btn.config(bg=SECONDARY_COLOR, fg="#FF8A65"))

    # ── Navigation ─────────────────────────────────────────────────────────
    def _navigate(self, key: str) -> None:
        self._active_nav = key

        # Update sidebar highlight
        for k, btn in self._nav_buttons.items():
            if k == key:
                btn.config(bg=PRIMARY_COLOR, font=("Segoe UI", 10, "bold"))
            else:
                btn.config(bg=SECONDARY_COLOR, font=NORMAL_FONT)

        # Destroy current content
        for child in self._content.winfo_children():
            child.destroy()

        # Load the requested panel
        panel = self._load_panel(key)
        if panel:
            panel.pack(fill=tk.BOTH, expand=True)

    def _load_panel(self, key: str) -> Optional[tk.Frame]:
        """Instantiate and return the panel widget for the given nav key."""
        try:
            if key == "dashboard":
                from .frames.dashboard_frame import DashboardFrame
                return DashboardFrame(self._content, self.current_user, on_navigate=self._navigate)
            if key == "pos_sale":
                from .frames.pos_sale_frame import POSSaleFrame
                return POSSaleFrame(self._content, self.current_user)
            if key == "inventory":
                from .frames.inventory_frame import InventoryFrame
                return InventoryFrame(self._content, self.current_user)
            if key == "customers":
                from .frames.customer_frame import CustomerFrame
                return CustomerFrame(self._content, self.current_user)
            if key == "suppliers":
                from .frames.supplier_frame import SupplierFrame
                return SupplierFrame(self._content, self.current_user)
            if key == "reports":
                from .frames.reports_frame import ReportsFrame
                return ReportsFrame(self._content, self.current_user)
            if key == "users":
                from .frames.user_frame import UserFrame
                return UserFrame(self._content, self.current_user)
            if key == "barcode_print":
                from .frames.barcode_print_frame import BarcodePrintFrame
                return BarcodePrintFrame(self._content, self.current_user)
            if key == "expenses":
                from .frames.expenses_frame import ExpensesFrame
                return ExpensesFrame(self._content, self.current_user)
        except Exception as exc:
            # Show a friendly placeholder if the panel isn't ready yet
            f = tk.Frame(self._content, bg=APP_BACKGROUND)
            tk.Label(
                f, text=f"⚠️  Module not ready: {key}\n\n{exc}",
                font=NORMAL_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
            ).pack(expand=True)
            return f
        return None

    # ── Actions ────────────────────────────────────────────────────────────
    def _logout(self) -> None:
        if messagebox.askyesno("Logout", "Are you sure you want to logout?", parent=self):
            self.destroy()
            if self._on_logout_cb:
                self._on_logout_cb()

    def _confirm_exit(self) -> None:
        if messagebox.askyesno("Exit", "Exit the POS system?", parent=self):
            try:
                from ..utils.auto_save_manager import auto_save_database, auto_save_daily_excel
                auto_save_database(prefix="exit")
                auto_save_daily_excel()
            except Exception:
                pass
            self.destroy()
            if self._on_logout_cb:
                self._on_logout_cb()
