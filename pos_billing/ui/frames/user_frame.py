# ============================================================
# pos_billing/ui/frames/user_frame.py  (UserGroupFrame.java → Python)
# ============================================================
"""User management panel – ADMIN only."""

from __future__ import annotations

import tkinter as tk
from tkinter import messagebox, ttk

from ...database import dao
from ...database.models import User
from ..constants import APP_BACKGROUND, DARK_COLOR, HEADING_FONT, NORMAL_FONT
from ..widgets import (
    create_entry, create_label, create_success_button, create_table, create_button,
)


class UserFrame(tk.Frame):
    def __init__(self, parent: tk.Widget, user: User) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.current_user = user
        self._users: list[User] = []
        self._build()
        self._load()

    def _build(self) -> None:
        top = tk.Frame(self, bg=APP_BACKGROUND)
        top.pack(fill=tk.X, padx=16, pady=10)
        tk.Label(top, text="👤  User Management",
                 font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR).pack(side=tk.LEFT)

        if self.current_user.role == "ADMIN":
            create_success_button(top, "➕ Add User", command=self._add_dialog).pack(
                side=tk.RIGHT, padx=4
            )

        create_button(top, "🔄 Refresh", command=self._load).pack(side=tk.RIGHT, padx=4)

        wrap = tk.Frame(self, bg=APP_BACKGROUND)
        wrap.pack(fill=tk.BOTH, expand=True, padx=16)

        cols = [
            ("username",  120, "Username"),
            ("name",      160, "Full Name"),
            ("role",       90, "Role"),
            ("email",     160, "Email"),
            ("status",     80, "Status"),
        ]
        self._tree = create_table(wrap, cols, height=22)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        vsb = ttk.Scrollbar(wrap, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

    def _load(self) -> None:
        self._users = dao.get_all_users()
        for row in self._tree.get_children():
            self._tree.delete(row)
        for u in self._users:
            self._tree.insert("", tk.END, iid=u.username, values=(
                u.username, u.full_name, u.role, u.email, u.status,
            ))

    def _add_dialog(self) -> None:
        UserDialog(self, on_save=self._save_user)

    def _save_user(self, data: dict) -> None:
        u = User(
            username=data["username"],
            password=data["password"],
            full_name=data["full_name"],
            role=data["role"] or "CASHIER",
        )
        u.email = data["email"]
        u.phone = data["phone"]
        if dao.save_user(u):
            messagebox.showinfo("Saved", f"User '{u.username}' created.", parent=self)
            self._load()
        else:
            messagebox.showerror("Error", "Could not save user.", parent=self)


class UserDialog(tk.Toplevel):
    _FIELDS = [
        ("username",  "Username*"),
        ("password",  "Password*"),
        ("full_name", "Full Name*"),
        ("role",      "Role (ADMIN/MANAGER/CASHIER)"),
        ("email",     "Email"),
        ("phone",     "Phone"),
    ]

    def __init__(self, parent, on_save=None) -> None:
        super().__init__(parent)
        self.title("Add User")
        self.geometry("360x340")
        self.resizable(False, False)
        self.configure(bg=APP_BACKGROUND)
        self._on_save = on_save
        self._entries: dict[str, tk.Entry] = {}
        self._build()
        self.grab_set()
        self.transient(parent)

    def _build(self) -> None:
        form = tk.Frame(self, bg=APP_BACKGROUND)
        form.pack(fill=tk.BOTH, expand=True, padx=20, pady=10)
        for i, (key, label) in enumerate(self._FIELDS):
            create_label(form, label).grid(row=i, column=0, sticky=tk.W, pady=3)
            show = "•" if key == "password" else ""
            e = tk.Entry(form, width=22, font=NORMAL_FONT, show=show)
            e.grid(row=i, column=1, padx=8, pady=3)
            self._entries[key] = e

        btn_row = tk.Frame(self, bg=APP_BACKGROUND)
        btn_row.pack(pady=10)
        create_success_button(btn_row, "💾 Create", command=self._save).pack(side=tk.LEFT, padx=8)
        tk.Button(btn_row, text="Cancel", command=self.destroy,
                  font=NORMAL_FONT, relief=tk.FLAT).pack(side=tk.LEFT, padx=8)

    def _save(self) -> None:
        data = {k: e.get().strip() for k, e in self._entries.items()}
        if not data["username"] or not data["password"] or not data["full_name"]:
            messagebox.showerror("Validation",
                                 "Username, password, and full name are required.", parent=self)
            return
        if self._on_save:
            self._on_save(data)
        self.destroy()
