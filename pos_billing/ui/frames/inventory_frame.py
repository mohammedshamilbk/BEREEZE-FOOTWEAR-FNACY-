# ============================================================
# pos_billing/ui/frames/inventory_frame.py  (InventoryFrame.java → Python)
# ============================================================
"""Inventory / Item Master management panel."""

from __future__ import annotations

import tkinter as tk
from tkinter import messagebox, ttk

from ...database import dao
from ...database.models import ItemMaster, User
from ..constants import APP_BACKGROUND, DARK_COLOR, HEADING_FONT, NORMAL_FONT, PRIMARY_COLOR
from ..widgets import (
    create_button, create_danger_button, create_entry, create_label,
    create_success_button, create_table,
)


class InventoryFrame(tk.Frame):
    def __init__(self, parent: tk.Widget, user: User) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.user = user
        self._items: list[ItemMaster] = []
        self._build()
        self._load_items()

    def _build(self) -> None:
        # ── Title + toolbar ────────────────────────────────────────────────
        top = tk.Frame(self, bg=APP_BACKGROUND)
        top.pack(fill=tk.X, padx=16, pady=10)

        tk.Label(top, text="📦  Inventory / Item Master",
                 font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR).pack(side=tk.LEFT)

        create_success_button(top, "➕ Add Item", command=self._add_item_dialog).pack(
            side=tk.RIGHT, padx=4
        )
        create_button(top, "🔄 Refresh", command=self._load_items).pack(side=tk.RIGHT, padx=4)

        # ── Search bar ────────────────────────────────────────────────────
        srch = tk.Frame(self, bg=APP_BACKGROUND)
        srch.pack(fill=tk.X, padx=16, pady=(0, 6))
        create_label(srch, "Search:").pack(side=tk.LEFT)
        self._search_var = tk.StringVar()
        self._search_var.trace_add("write", lambda *_: self._filter())
        create_entry(srch, width=30).pack(side=tk.LEFT, padx=6)
        srch.children[list(srch.children)[-1]].config(textvariable=self._search_var)

        # ── Table ─────────────────────────────────────────────────────────
        tbl_wrap = tk.Frame(self, bg=APP_BACKGROUND)
        tbl_wrap.pack(fill=tk.BOTH, expand=True, padx=16)

        cols = [
            ("code",     100, "Code"),
            ("name",     180, "Name"),
            ("cat",       90, "Category"),
            ("brand",    100, "Brand"),
            ("size",      60, "Size"),
            ("color",     80, "Color"),
            ("cost",      80, "Cost ₹"),
            ("price",     80, "Price ₹"),
            ("stock",     60, "Stock"),
            ("status",    80, "Status"),
        ]
        self._tree = create_table(tbl_wrap, cols, height=20)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        vsb = ttk.Scrollbar(tbl_wrap, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        # Edit / Delete buttons
        btn_row = tk.Frame(self, bg=APP_BACKGROUND)
        btn_row.pack(pady=6)
        create_button(btn_row, "✏️ Edit Selected", command=self._edit_item).pack(
            side=tk.LEFT, padx=6
        )
        create_button(btn_row, "🏷️ Barcode Print Program", command=self._open_barcode_program).pack(
            side=tk.LEFT, padx=6
        )
        if self.user.role == "ADMIN":
            create_danger_button(btn_row, "🗑 Delete Selected",
                                 command=self._delete_item).pack(side=tk.LEFT, padx=6)

    def _open_barcode_program(self) -> None:
        from .barcode_print_frame import BarcodePrintFrame
        win = tk.Toplevel(self)
        win.title("🏷️ BEREEZE FOOTWEAR - Barcode Point & Print Program")
        win.geometry("1080x760")
        win.configure(bg=APP_BACKGROUND)
        try:
            win.tk.eval(f"tk::PlaceWindow {win._w} center")
        except Exception:
            pass

        frame = BarcodePrintFrame(win, self.user, on_exit=win.destroy)
        frame.pack(fill=tk.BOTH, expand=True)

        sel = self._tree.selection()
        if sel:
            idx = self._tree.index(sel[0])
            if hasattr(self, "_filtered_items") and 0 <= idx < len(self._filtered_items):
                item = self._filtered_items[idx]
                frame.entry_barcode.insert(0, item.barcode or item.item_code)
                frame._search_barcode_action()

    # ── Data helpers ───────────────────────────────────────────────────────
    def _load_items(self) -> None:
        self._items = dao.get_all_items()
        self._populate(self._items)

    def _populate(self, items: list[ItemMaster]) -> None:
        for row in self._tree.get_children():
            self._tree.delete(row)
        for item in items:
            self._tree.insert("", tk.END, iid=str(item.item_id), values=(
                item.item_code, item.item_name, item.category, item.manufacturer,
                item.size, item.color,
                f"{item.purchase_price:.2f}", f"{item.selling_price:.2f}",
                item.stock_quantity, item.status,
            ))

    def _filter(self) -> None:
        q = self._search_var.get().lower()
        filtered = [
            i for i in self._items
            if q in i.item_code.lower() or q in i.item_name.lower()
            or q in i.category.lower() or q in i.manufacturer.lower()
        ]
        self._populate(filtered)

    # ── Dialogs ────────────────────────────────────────────────────────────
    def _add_item_dialog(self) -> None:
        ItemDialog(self, title="Add New Item", on_save=self._save_new_item)

    def _save_new_item(self, data: dict) -> None:
        item = ItemMaster(
            item_code=data["item_code"],
            item_name=data["item_name"],
            category=data["category"],
            manufacturer=data["manufacturer"],
            purchase_price=float(data["purchase_price"] or 0),
            selling_price=float(data["selling_price"] or 0),
            barcode=data["barcode"],
            size=data["size"],
            color=data["color"],
            material=data["material"],
        )
        item.stock_quantity = int(data["stock"] or 0)
        if dao.save_item(item):
            messagebox.showinfo("Saved", f"Item '{item.item_name}' saved.", parent=self)
            self._load_items()
        else:
            messagebox.showerror("Error", "Could not save item.", parent=self)

    def _edit_item(self) -> None:
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select an item to edit.", parent=self)
            return
        item_id = int(sel[0])
        item = next((i for i in self._items if i.item_id == item_id), None)
        if item:
            ItemDialog(self, title="Edit Item", item=item, on_save=self._update_item)

    def _update_item(self, data: dict) -> None:
        item_id = int(data.get("item_id", 0))
        item = next((i for i in self._items if i.item_id == item_id), None)
        if not item:
            return
        item.item_code = data["item_code"]
        item.item_name = data["item_name"]
        item.category = data["category"]
        item.manufacturer = data["manufacturer"]
        item.purchase_price = float(data["purchase_price"] or 0)
        item.selling_price = float(data["selling_price"] or 0)
        item.barcode = data["barcode"]
        item.size = data["size"]
        item.color = data["color"]
        item.material = data["material"]
        item.stock_quantity = int(data["stock"] or 0)
        if dao.save_item(item):
            messagebox.showinfo("Updated", "Item updated.", parent=self)
            self._load_items()

    def _delete_item(self) -> None:
        sel = self._tree.selection()
        if not sel:
            return
        if messagebox.askyesno("Delete", "Delete selected item?", parent=self):
            item_id = int(sel[0])
            item = next((i for i in self._items if i.item_id == item_id), None)
            if item:
                item.status = "INACTIVE"
                dao.save_item(item)
                self._load_items()


# ─── Add / Edit dialog ────────────────────────────────────────────────────
class ItemDialog(tk.Toplevel):
    _FIELDS = [
        ("item_code",      "Item Code*"),
        ("item_name",      "Item Name*"),
        ("category",       "Category"),
        ("manufacturer",   "Manufacturer / Brand"),
        ("size",           "Size"),
        ("color",          "Color"),
        ("material",       "Material"),
        ("barcode",        "Barcode"),
        ("purchase_price", "Purchase Price ₹*"),
        ("selling_price",  "Selling Price ₹*"),
        ("stock",          "Stock Quantity"),
    ]

    def __init__(self, parent, title: str, item: ItemMaster | None = None,
                 on_save=None) -> None:
        super().__init__(parent)
        self.title(title)
        self.geometry("420x480")
        self.resizable(False, False)
        self.configure(bg=APP_BACKGROUND)
        self._item = item
        self._on_save = on_save
        self._entries: dict[str, tk.Entry] = {}
        self._build()
        if item:
            self._populate(item)
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

    def _populate(self, item: ItemMaster) -> None:
        mapping = {
            "item_code": item.item_code, "item_name": item.item_name,
            "category": item.category, "manufacturer": item.manufacturer,
            "size": item.size, "color": item.color, "material": item.material,
            "barcode": item.barcode,
            "purchase_price": str(item.purchase_price),
            "selling_price": str(item.selling_price),
            "stock": str(item.stock_quantity),
        }
        for key, val in mapping.items():
            if key in self._entries:
                self._entries[key].delete(0, tk.END)
                self._entries[key].insert(0, val)

    def _save(self) -> None:
        data = {k: e.get().strip() for k, e in self._entries.items()}
        if not data["item_code"] or not data["item_name"]:
            messagebox.showerror("Validation", "Item Code and Name are required.", parent=self)
            return
        if self._item:
            data["item_id"] = self._item.item_id
        if self._on_save:
            self._on_save(data)
        self.destroy()
