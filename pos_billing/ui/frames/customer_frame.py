# ============================================================
# pos_billing/ui/frames/customer_frame.py  (CustomerFrame.java → Python)
# ============================================================
"""Customer management panel."""

from __future__ import annotations

import tkinter as tk
from tkinter import messagebox, ttk

from ...database import dao
from ...database.models import Customer, User
from ..constants import APP_BACKGROUND, DARK_COLOR, HEADING_FONT, NORMAL_FONT
from ..widgets import (
    create_button, create_danger_button, create_entry, create_label,
    create_success_button, create_table,
)


class CustomerFrame(tk.Frame):
    def __init__(self, parent: tk.Widget, user: User) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.user = user
        self._customers: list[Customer] = []
        self._build()
        self._load()

    def _build(self) -> None:
        top = tk.Frame(self, bg=APP_BACKGROUND)
        top.pack(fill=tk.X, padx=16, pady=10)

        tk.Label(top, text="👥  Customers",
                 font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR).pack(side=tk.LEFT)
        create_success_button(top, "➕ Add Customer", command=self._add_dialog).pack(
            side=tk.RIGHT, padx=4
        )
        create_button(top, "🔄 Refresh", command=self._load).pack(side=tk.RIGHT, padx=4)

        # Search
        srch = tk.Frame(self, bg=APP_BACKGROUND)
        srch.pack(fill=tk.X, padx=16, pady=(0, 6))
        create_label(srch, "Search:").pack(side=tk.LEFT)
        self._q = tk.StringVar()
        self._q.trace_add("write", lambda *_: self._filter())
        e = create_entry(srch, width=30)
        e.config(textvariable=self._q)
        e.pack(side=tk.LEFT, padx=6)

        # Table
        wrap = tk.Frame(self, bg=APP_BACKGROUND)
        wrap.pack(fill=tk.BOTH, expand=True, padx=16)

        cols = [
            ("code",    100, "Code"),
            ("name",    160, "Name"),
            ("phone",   110, "Phone"),
            ("email",   160, "Email"),
            ("city",     90, "City"),
            ("type",     90, "Type"),
            ("loyalty",  80, "Loyalty Pts"),
            ("status",   80, "Status"),
        ]
        self._tree = create_table(wrap, cols, height=20)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        vsb = ttk.Scrollbar(wrap, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        btn_row = tk.Frame(self, bg=APP_BACKGROUND)
        btn_row.pack(pady=6)
        create_button(btn_row, "✏️ Edit Selected", command=self._edit_dialog).pack(
            side=tk.LEFT, padx=6
        )

    def _load(self) -> None:
        self._customers = dao.get_all_customers()
        self._populate(self._customers)

    def _populate(self, custs: list[Customer]) -> None:
        for row in self._tree.get_children():
            self._tree.delete(row)
        for c in custs:
            self._tree.insert("", tk.END, iid=str(c.customer_id), values=(
                c.customer_code, c.customer_name, c.phone, c.email,
                c.city, c.customer_type, f"{c.loyalty_points:.1f}", c.status,
            ))

    def _filter(self) -> None:
        q = self._q.get().lower()
        self._populate([
            c for c in self._customers
            if q in c.customer_name.lower() or q in c.phone.lower()
            or q in c.customer_code.lower()
        ])

    def _add_dialog(self) -> None:
        CustomerDialog(self, title="Add Customer", on_save=self._save_new)

    def _save_new(self, data: dict) -> None:
        c = Customer(
            customer_code=data["customer_code"],
            customer_name=data["customer_name"],
            phone=data["phone"],
            email=data["email"],
        )
        c.address = data["address"]
        c.city = data["city"]
        c.state = data["state"]
        c.pincode = data["pincode"]
        c.credit_limit = float(data["credit_limit"] or 0)
        c.customer_type = data["customer_type"] or "REGULAR"
        if dao.save_customer(c):
            messagebox.showinfo("Saved", f"Customer '{c.customer_name}' saved.", parent=self)
            self._load()
        else:
            messagebox.showerror("Error", "Could not save customer.", parent=self)

    def _edit_dialog(self) -> None:
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select a customer to edit.", parent=self)
            return
        cid = int(sel[0])
        cust = next((c for c in self._customers if c.customer_id == cid), None)
        if cust:
            CustomerDialog(self, title="Edit Customer", customer=cust,
                           on_save=self._update_customer)

    def _update_customer(self, data: dict) -> None:
        cid = int(data.get("customer_id", 0))
        cust = next((c for c in self._customers if c.customer_id == cid), None)
        if not cust:
            return
        cust.customer_code = data["customer_code"]
        cust.customer_name = data["customer_name"]
        cust.phone = data["phone"]
        cust.email = data["email"]
        cust.address = data["address"]
        cust.city = data["city"]
        cust.state = data["state"]
        cust.pincode = data["pincode"]
        cust.credit_limit = float(data["credit_limit"] or 0)
        cust.customer_type = data["customer_type"] or "REGULAR"
        if dao.save_customer(cust):
            messagebox.showinfo("Updated", "Customer updated.", parent=self)
            self._load()


class CustomerDialog(tk.Toplevel):
    _FIELDS = [
        ("customer_code", "Customer Code*"),
        ("customer_name", "Customer Name*"),
        ("phone",         "Phone"),
        ("email",         "Email"),
        ("address",       "Address"),
        ("city",          "City"),
        ("state",         "State"),
        ("pincode",       "Pincode"),
        ("credit_limit",  "Credit Limit ₹"),
        ("customer_type", "Type (REGULAR/WHOLESALE/RETAIL)"),
    ]

    def __init__(self, parent, title: str, customer: Customer | None = None,
                 on_save=None) -> None:
        super().__init__(parent)
        self.title(title)
        self.geometry("400x450")
        self.resizable(False, False)
        self.configure(bg=APP_BACKGROUND)
        self._customer = customer
        self._on_save = on_save
        self._entries: dict[str, tk.Entry] = {}
        self._build()
        if customer:
            self._populate(customer)
        self.grab_set()
        self.transient(parent)

    def _build(self) -> None:
        form = tk.Frame(self, bg=APP_BACKGROUND)
        form.pack(fill=tk.BOTH, expand=True, padx=20, pady=10)
        for i, (key, label) in enumerate(self._FIELDS):
            create_label(form, label).grid(row=i, column=0, sticky=tk.W, pady=2)
            e = create_entry(form, width=24)
            e.grid(row=i, column=1, padx=8, pady=2)
            self._entries[key] = e

        btn_row = tk.Frame(self, bg=APP_BACKGROUND)
        btn_row.pack(pady=10)
        create_success_button(btn_row, "💾 Save", command=self._save).pack(side=tk.LEFT, padx=8)
        tk.Button(btn_row, text="Cancel", command=self.destroy,
                  font=NORMAL_FONT, relief=tk.FLAT).pack(side=tk.LEFT, padx=8)

    def _populate(self, c: Customer) -> None:
        mapping = {
            "customer_code": c.customer_code, "customer_name": c.customer_name,
            "phone": c.phone, "email": c.email, "address": c.address,
            "city": c.city, "state": c.state, "pincode": c.pincode,
            "credit_limit": str(c.credit_limit), "customer_type": c.customer_type,
        }
        for k, v in mapping.items():
            if k in self._entries:
                self._entries[k].delete(0, tk.END)
                self._entries[k].insert(0, v)

    def _save(self) -> None:
        data = {k: e.get().strip() for k, e in self._entries.items()}
        if not data["customer_code"] or not data["customer_name"]:
            messagebox.showerror("Validation", "Code and Name are required.", parent=self)
            return
        if self._customer:
            data["customer_id"] = self._customer.customer_id
        if self._on_save:
            self._on_save(data)
        self.destroy()
