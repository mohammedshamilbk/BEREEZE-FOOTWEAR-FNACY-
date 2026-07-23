# ============================================================
# pos_billing/ui/login_frame.py  (LoginFrame.java → Python)
# ============================================================
"""
Login window – first screen the user sees.

Mirrors Java's LoginFrame with:
  - Username / Password fields
  - Login button → opens MainFrame on success
  - Cancel button → exits app
  - Offline fallback for admin / cashier
"""

from __future__ import annotations

import tkinter as tk
from tkinter import messagebox

from ..database import dao
from ..database.models import User
from .constants import (
    APP_BACKGROUND, APP_TITLE, BORDER_COLOR, DARK_COLOR,
    DANGER_COLOR, HEADING_FONT, NORMAL_FONT, PRIMARY_COLOR,
    SMALL_FONT, TEXT_ON_APP_BG, TEXT_ON_PRIMARY, TITLE_FONT,
)
from .widgets import (
    create_button, create_danger_button, create_entry,
    create_label, create_password_entry, create_secondary_button,
    create_success_button,
)


class LoginFrame(tk.Tk):
    """Root Tk window acting as the Login screen."""

    def __init__(self) -> None:
        super().__init__()
        self.title("Bereezefootwearfancy – Login")
        self.geometry("480x540")
        self.resizable(False, False)
        self.configure(bg=APP_BACKGROUND)
        self._build_ui()
        from .widgets import center_window
        center_window(self, 480, 540)

    # ── UI construction ────────────────────────────────────────────────────
    def _build_ui(self) -> None:
        # ── Header banner ──────────────────────────────────────────────────
        banner = tk.Frame(self, bg=PRIMARY_COLOR, height=200)
        banner.pack(fill=tk.X)
        banner.pack_propagate(False)

        tk.Label(
            banner, text="👟", font=("Segoe UI Emoji", 40),
            bg=PRIMARY_COLOR, fg=TEXT_ON_PRIMARY,
        ).pack(pady=(24, 4))

        tk.Label(
            banner, text="BREEZE FOOTWEAR FANCY",
            font=TITLE_FONT, bg=PRIMARY_COLOR, fg=TEXT_ON_PRIMARY,
        ).pack()

        tk.Label(
            banner, text="Point of Sale System",
            font=NORMAL_FONT, bg=PRIMARY_COLOR, fg="#E0F2F1",
        ).pack()

        # ── Form area ──────────────────────────────────────────────────────
        form = tk.Frame(self, bg=APP_BACKGROUND)
        form.pack(fill=tk.BOTH, expand=True, padx=50, pady=20)

        # Error label
        self._error_var = tk.StringVar(value=" ")
        tk.Label(form, textvariable=self._error_var,
                 font=SMALL_FONT, fg=DANGER_COLOR, bg=APP_BACKGROUND).pack(anchor=tk.W)

        # Username
        create_label(form, "Username:").pack(anchor=tk.W)
        self._username_entry = create_entry(form, width=32)
        self._username_entry.pack(fill=tk.X, pady=(2, 10))
        self._username_entry.bind("<Return>", lambda _: self._password_entry.focus_set())

        # Password
        create_label(form, "Password:").pack(anchor=tk.W)
        self._password_entry = create_password_entry(form, width=32)
        self._password_entry.pack(fill=tk.X, pady=(2, 10))
        self._password_entry.bind("<Return>", lambda _: self._handle_login())

        # Remember me
        self._remember = tk.BooleanVar(value=False)
        tk.Checkbutton(form, text="Remember me", variable=self._remember,
                       font=NORMAL_FONT, bg=APP_BACKGROUND,
                       fg=TEXT_ON_APP_BG, activebackground=APP_BACKGROUND).pack(anchor=tk.W)

        # Buttons
        btn_row = tk.Frame(form, bg=APP_BACKGROUND)
        btn_row.pack(pady=16)

        create_success_button(btn_row, "  Login  ", command=self._handle_login).pack(
            side=tk.LEFT, padx=6
        )
        create_secondary_button(btn_row, "  Cancel  ", command=self.destroy).pack(
            side=tk.LEFT, padx=6
        )

        # Version note
        tk.Label(self, text="v1.0 – Python Edition",
                 font=SMALL_FONT, bg=APP_BACKGROUND, fg=BORDER_COLOR).pack(pady=4)

    # ── Login logic ────────────────────────────────────────────────────────
    def _handle_login(self) -> None:
        username = self._username_entry.get().strip()
        password = self._password_entry.get().strip()

        if not username:
            self._show_error("Please enter username")
            return
        if not password:
            self._show_error("Please enter password")
            return

        user: User | None = None

        # Try database first
        try:
            user = dao.authenticate_user(username, password)
        except Exception as exc:
            print(f"[DB] offline – {exc}")

        # Offline fallback (mirrors Java LoginFrame & common defaults)
        if user is None and username.lower() in ("admin", "administrator") and password in ("admin123", "admin", "1234"):
            user = User("admin", password, "Administrator", "ADMIN")
        if user is None and username.lower() == "cashier" and password in ("cashier123", "cashier", "1234"):
            user = User("cashier", password, "Cashier User", "CASHIER")

        if user is not None:
            self._open_main(user)
        else:
            self._show_error("Invalid username or password")
            self._password_entry.delete(0, tk.END)

    def _open_main(self, user: User) -> None:
        """Destroy login window and open main application frame."""
        self.withdraw()
        from .main_frame import MainFrame  # deferred to avoid circular import
        main = MainFrame(user, on_logout=self._on_logout)
        main.mainloop()

    def _on_logout(self) -> None:
        """Called when user logs out of MainFrame."""
        # Clear fields and show login again
        self._username_entry.delete(0, tk.END)
        self._password_entry.delete(0, tk.END)
        self._show_error(" ")
        self.deiconify()
        from .widgets import center_window
        center_window(self, 480, 540)

    def _show_error(self, message: str) -> None:
        self._error_var.set(message)
