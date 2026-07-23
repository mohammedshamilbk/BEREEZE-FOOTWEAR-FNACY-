# ============================================================
# pos_billing/ui/frames/supplier_frame.py  (SupplierFrame.java → Python)
# ============================================================
"""Supplier management panel."""

from __future__ import annotations

import tkinter as tk
from tkinter import messagebox, ttk

from ...database import dao
from ...database.models import Supplier, User
from ..constants import APP_BACKGROUND, DARK_COLOR, HEADING_FONT, NORMAL_FONT
from ..widgets import (
    create_button, create_entry, create_label,
    create_success_button, create_table,
)


class SupplierFrame(tk.Frame):
    def __init__(self, parent: tk.Widget, user: User) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.user = user
        self._suppliers: list[Supplier] = []
        self._build()
        self._load()

    def _build(self) -> None:
        top = tk.Frame(self, bg=APP_BACKGROUND)
        top.pack(fill=tk.X, padx=16, pady=10)
        tk.Label(top, text="🚚  Suppliers",
                 font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR).pack(side=tk.LEFT)
        create_success_button(top, "➕ Add Supplier", command=self._add_dialog).pack(
            side=tk.RIGHT, padx=4
        )
        create_button(top, "🔄 Refresh", command=self._load).pack(side=tk.RIGHT, padx=4)

        wrap = tk.Frame(self, bg=APP_BACKGROUND)
        wrap.pack(fill=tk.BOTH, expand=True, padx=16)

        cols = [
            ("code",    100, "Code"),
            ("name",    160, "Name"),
            ("phone",   110, "Phone"),
            ("email",   160, "Email"),
            ("gstin",   120, "GSTIN"),
            ("balance",  90, "Outstanding"),
            ("status",   80, "Status"),
        ]
        self._tree = create_table(wrap, cols, height=22)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        vsb = ttk.Scrollbar(wrap, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        btn_row = tk.Frame(self, bg=APP_BACKGROUND)
        btn_row.pack(pady=6)
        create_button(btn_row, "📄 Add Bill Amount", command=self._add_bill).pack(
            side=tk.LEFT, padx=6
        )
        create_button(btn_row, "💰 Pay Supplier", command=self._pay_supplier).pack(
            side=tk.LEFT, padx=6
        )
        create_button(btn_row, "📜 View Bills", command=self._view_bills).pack(
            side=tk.LEFT, padx=6
        )
        create_button(btn_row, "✏️ Edit Selected", command=self._edit_dialog).pack(
            side=tk.LEFT, padx=6
        )

    def _add_bill(self) -> None:
        from ..app import _AddPurchaseBillDlg
        sel = self._tree.selection()
        pre = None
        if sel:
            sid = int(sel[0])
            pre = next((x for x in self._suppliers if x.supplier_id == sid), None)
        _AddPurchaseBillDlg(self, suppliers=self._suppliers, on_save=self._load, preselect=pre)

    def _pay_supplier(self) -> None:
        from ..app import _RecordSupplierPaymentDlg
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select a supplier from the table first.", parent=self)
            return
        sid = int(sel[0])
        s = next((x for x in self._suppliers if x.supplier_id == sid), None)
        if s:
            _RecordSupplierPaymentDlg(self, supplier=s, on_save=self._load)

    def _view_bills(self) -> None:
        from ..app import _ViewSupplierBillsDlg
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select a supplier from the table first.", parent=self)
            return
        sid = int(sel[0])
        s = next((x for x in self._suppliers if x.supplier_id == sid), None)
        if s:
            _ViewSupplierBillsDlg(self, supplier=s)

    def _load(self) -> None:
        self._suppliers = dao.get_all_suppliers()
        for row in self._tree.get_children():
            self._tree.delete(row)
        for s in self._suppliers:
            self._tree.insert("", tk.END, iid=str(s.supplier_id), values=(
                s.supplier_code, s.supplier_name, s.phone, s.email,
                s.gstin, f"{s.outstanding_balance:.2f}", s.status,
            ))

    def _add_dialog(self) -> None:
        SupplierDialog(self, title="Add Supplier", on_save=self._save_new)

    def _save_new(self, data: dict) -> None:
        s = Supplier(
            supplier_code=data["supplier_code"],
            supplier_name=data["supplier_name"],
        )
        s.phone = data["phone"]
        s.email = data["email"]
        s.state = data["state"]
        s.tax_regn = data["tax_regn"]
        s.gstin = data["gstin"]
        if dao.save_supplier(s):
            messagebox.showinfo("Saved", "Supplier saved.", parent=self)
            self._load()
        else:
            messagebox.showerror("Error", "Could not save supplier.", parent=self)

    def _edit_dialog(self) -> None:
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select a supplier to edit.", parent=self)
            return
        sid = int(sel[0])
        s = next((x for x in self._suppliers if x.supplier_id == sid), None)
        if s:
            SupplierDialog(self, title="Edit Supplier", supplier=s,
                           on_save=self._update)

    def _update(self, data: dict) -> None:
        sid = int(data.get("supplier_id", 0))
        s = next((x for x in self._suppliers if x.supplier_id == sid), None)
        if not s:
            return
        s.supplier_code = data["supplier_code"]
        s.supplier_name = data["supplier_name"]
        s.phone = data["phone"]
        s.email = data["email"]
        s.state = data["state"]
        s.tax_regn = data["tax_regn"]
        s.gstin = data["gstin"]
        if dao.save_supplier(s):
            messagebox.showinfo("Updated", "Supplier updated.", parent=self)
            self._load()


class SupplierDialog(tk.Toplevel):
    _FIELDS = [
        ("supplier_code", "Supplier Code*"),
        ("supplier_name", "Supplier Name*"),
        ("phone",         "Phone"),
        ("email",         "Email"),
        ("state",         "State"),
        ("tax_regn",      "Tax Regn"),
        ("gstin",         "GSTIN"),
    ]

    def __init__(self, parent, title: str, supplier: Supplier | None = None,
                 on_save=None) -> None:
        super().__init__(parent)
        self.title(title)
        self.geometry("380x360")
        self.resizable(False, False)
        self.configure(bg=APP_BACKGROUND)
        self._supplier = supplier
        self._on_save = on_save
        self._entries: dict[str, tk.Entry] = {}
        self._build()
        if supplier:
            self._populate(supplier)
        self.grab_set()
        self.transient(parent)

    def _build(self) -> None:
        form = tk.Frame(self, bg=APP_BACKGROUND)
        form.pack(fill=tk.BOTH, expand=True, padx=20, pady=10)
        for i, (key, label) in enumerate(self._FIELDS):
            create_label(form, label).grid(row=i, column=0, sticky=tk.W, pady=3)
            e = create_entry(form, width=24)
            e.grid(row=i, column=1, padx=8, pady=3)
            self._entries[key] = e

        btn_row = tk.Frame(self, bg=APP_BACKGROUND)
        btn_row.pack(pady=10)
        create_success_button(btn_row, "💾 Save", command=self._save).pack(side=tk.LEFT, padx=8)
        tk.Button(btn_row, text="Cancel", command=self.destroy,
                  font=NORMAL_FONT, relief=tk.FLAT).pack(side=tk.LEFT, padx=8)

    def _populate(self, s: Supplier) -> None:
        mapping = {
            "supplier_code": s.supplier_code, "supplier_name": s.supplier_name,
            "phone": s.phone, "email": s.email, "state": s.state,
            "tax_regn": s.tax_regn, "gstin": s.gstin,
        }
        for k, v in mapping.items():
            if k in self._entries:
                self._entries[k].delete(0, tk.END)
                self._entries[k].insert(0, v)

    def _save(self) -> None:
        data = {k: e.get().strip() for k, e in self._entries.items()}
        if not data["supplier_code"] or not data["supplier_name"]:
            messagebox.showerror("Validation", "Code and Name are required.", parent=self)
            return
        if self._supplier:
            data["supplier_id"] = self._supplier.supplier_id
        if self._on_save:
            self._on_save(data)
        self.destroy()
