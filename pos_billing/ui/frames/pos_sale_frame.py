# ============================================================
# pos_billing/ui/frames/pos_sale_frame.py  (POSSaleFrame.java → Python)
# ============================================================
"""POS Sale screen – main billing / invoice creation panel."""

from __future__ import annotations

import tkinter as tk
from datetime import datetime
from tkinter import messagebox, ttk

from ...database import dao
from ...database.models import Bill, BillItem, User
from ...payment.payment_method import PaymentProcessor
from ...utils import save_draft_cart, load_draft_cart, clear_draft_cart
from ..constants import (
    APP_BACKGROUND, BORDER_COLOR, DANGER_COLOR, DARK_COLOR,
    HEADING_FONT, NORMAL_FONT, PRIMARY_COLOR, SMALL_FONT,
    SUCCESS_COLOR, TEXT_ON_PRIMARY,
)
from ..widgets import (
    create_button, create_danger_button, create_entry, create_label,
    create_success_button, create_table, create_warning_button,
)


def _parse_discount_value(disc_str: str, base_amount: float) -> float:
    """Parse discount input string like '50' (₹50) or '10%' (10% of base_amount)."""
    s = disc_str.strip()
    if not s:
        return 0.0
    try:
        if s.endswith("%"):
            pct = float(s[:-1].strip())
            return max(0.0, (pct / 100.0) * base_amount)
        else:
            amt = float(s)
            return max(0.0, amt)
    except ValueError:
        return 0.0


class POSSaleFrame(tk.Frame):
    """POS Sale panel – mirrors Java POSSaleFrame."""

    def __init__(self, parent: tk.Widget, user: User) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.user = user
        self._current_bill: Bill | None = None
        self._bill_counter = 1001
        self._build()
        self._new_bill()

    # ── UI construction ────────────────────────────────────────────────────
    def _build(self) -> None:
        # Title
        tk.Label(
            self, text="🛒  POS Sale",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(anchor=tk.W, padx=16, pady=(12, 6))

        # Main body: left (form) + right (bill)
        body = tk.Frame(self, bg=APP_BACKGROUND)
        body.pack(fill=tk.BOTH, expand=True, padx=10)

        self._build_left(body)
        self._build_right(body)

    def _build_left(self, parent: tk.Widget) -> None:
        left = tk.Frame(parent, bg=APP_BACKGROUND)
        left.pack(side=tk.LEFT, fill=tk.BOTH, expand=False, padx=4)

        # ── Bill info ──────────────────────────────────────────────────────
        info = tk.LabelFrame(left, text=" Bill Info ", font=SMALL_FONT,
                             bg=APP_BACKGROUND, fg=PRIMARY_COLOR)
        info.pack(fill=tk.X, pady=4)

        self._bill_no_var = tk.StringVar()
        self._customer_var = tk.StringVar()

        r = 0
        for lbl, var in [("Bill No:", self._bill_no_var), ("Customer:", self._customer_var)]:
            create_label(info, lbl).grid(row=r, column=0, sticky=tk.W, padx=6, pady=3)
            tk.Entry(info, textvariable=var, font=NORMAL_FONT, width=22,
                     state="readonly").grid(row=r, column=1, padx=6, pady=3)
            r += 1

        # Customer search
        create_label(info, "Search Customer (Phone):").grid(
            row=r, column=0, sticky=tk.W, padx=6, pady=3
        )
        self._cust_search = create_entry(info, width=22)
        self._cust_search.grid(row=r, column=1, padx=6, pady=3)
        self._cust_search.bind("<Return>", lambda _: self._search_customer())
        r += 1
        create_button(info, "Find", command=self._search_customer).grid(
            row=r, column=1, sticky=tk.W, padx=6, pady=3
        )

        # ── Add item ──────────────────────────────────────────────────────
        item_box = tk.LabelFrame(left, text=" Add Item ", font=SMALL_FONT,
                                  bg=APP_BACKGROUND, fg=PRIMARY_COLOR)
        item_box.pack(fill=tk.X, pady=6)

        self._item_code_var = tk.StringVar()
        self._qty_var = tk.StringVar(value="1")

        create_label(item_box, "Item Code / Barcode:").grid(
            row=0, column=0, sticky=tk.W, padx=6, pady=3
        )
        self._item_entry = create_entry(item_box, width=22)
        self._item_entry.grid(row=0, column=1, padx=6, pady=3)
        self._item_entry.bind("<Return>", lambda _: self._add_item())
        self._item_entry.bind("<KeyRelease>", lambda e: self._preview_item_cost())

        self._preview_lbl = create_label(item_box, "📷 Ready for Barcode Scanner Machine or Manual Entry", font=SMALL_FONT)
        self._preview_lbl.grid(row=1, column=0, columnspan=2, sticky=tk.W, padx=6, pady=2)
        self.after(150, lambda: self._item_entry.focus_set())

        # Quantity & Item Discount in same row
        q_frame = tk.Frame(item_box, bg=APP_BACKGROUND)
        q_frame.grid(row=2, column=0, columnspan=2, sticky=tk.W, padx=6, pady=3)

        create_label(q_frame, "Qty:").pack(side=tk.LEFT, padx=(0, 2))
        self._qty_entry = create_entry(q_frame, width=6)
        self._qty_entry.insert(0, "1")
        self._qty_entry.pack(side=tk.LEFT, padx=(0, 10))
        self._qty_entry.bind("<Return>", lambda _: self._add_item())

        create_label(q_frame, "Item Disc (₹ / %):").pack(side=tk.LEFT, padx=(0, 2))
        self._item_disc_entry = create_entry(q_frame, width=8)
        self._item_disc_entry.insert(0, "0")
        self._item_disc_entry.pack(side=tk.LEFT)
        self._item_disc_entry.bind("<Return>", lambda _: self._add_item())

        btn_add_box = tk.Frame(item_box, bg=APP_BACKGROUND)
        btn_add_box.grid(row=3, column=0, columnspan=2, sticky=tk.W, padx=6, pady=4)

        create_success_button(btn_add_box, "Add Item ➕", command=self._add_item).pack(side=tk.LEFT, padx=(0, 4))
        create_warning_button(btn_add_box, "📦 Old Stock / Direct Item", command=self._open_add_custom_item).pack(side=tk.LEFT)

        # ── Payment & actions ─────────────────────────────────────────────
        pay_box = tk.LabelFrame(left, text=" Payment & Bill Discount ", font=SMALL_FONT,
                                 bg=APP_BACKGROUND, fg=PRIMARY_COLOR)
        pay_box.pack(fill=tk.X, pady=4)

        pay_methods = ["CASH", "CARD", "CHEQUE", "UPI", "ONLINE"]
        self._pay_method = tk.StringVar(value="CASH")
        create_label(pay_box, "Method:").grid(row=0, column=0, sticky=tk.W, padx=6, pady=2)
        ttk.Combobox(pay_box, textvariable=self._pay_method,
                     values=pay_methods, width=14,
                     state="readonly").grid(row=0, column=1, padx=6, pady=2)

        create_label(pay_box, "Bill Disc (₹ / %):").grid(row=1, column=0, sticky=tk.W, padx=6, pady=2)
        self._bill_disc_entry = create_entry(pay_box, width=14)
        self._bill_disc_entry.insert(0, "0")
        self._bill_disc_entry.grid(row=1, column=1, padx=6, pady=2)
        self._bill_disc_entry.bind("<KeyRelease>", lambda e: self._refresh_table())

        self._paid_var = tk.StringVar(value="0")
        create_label(pay_box, "Amount Paid (₹):").grid(row=2, column=0, sticky=tk.W, padx=6, pady=2)
        self._paid_entry = create_entry(pay_box, width=14, textvariable=self._paid_var)
        self._paid_entry.grid(row=2, column=1, padx=6, pady=2)
        self._paid_entry.bind("<KeyRelease>", lambda e: self._update_balance())

        btn_row = tk.Frame(pay_box, bg=APP_BACKGROUND)
        btn_row.grid(row=3, column=0, columnspan=2, pady=6)

        create_success_button(btn_row, "✅ Complete Bill",
                              command=self._complete_bill).pack(side=tk.LEFT, padx=3)
        create_button(btn_row, "📱 Show UPI QR",
                      command=self._show_upi_qr).pack(side=tk.LEFT, padx=3)
        create_warning_button(btn_row, "🔄 New Bill",
                              command=self._new_bill).pack(side=tk.LEFT, padx=3)
        create_danger_button(btn_row, "🗑 Clear",
                             command=self._clear_bill).pack(side=tk.LEFT, padx=3)

    def _build_right(self, parent: tk.Widget) -> None:
        right = tk.Frame(parent, bg=APP_BACKGROUND)
        right.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=6)

        # Header row: Title + Action Buttons
        hdr = tk.Frame(right, bg=APP_BACKGROUND)
        hdr.pack(fill=tk.X, pady=(4, 6))

        tk.Label(hdr, text="🧾 Bill Items Queue", font=("Segoe UI", 12, "bold"),
                 bg=APP_BACKGROUND, fg=DARK_COLOR).pack(side=tk.LEFT)

        create_danger_button(hdr, "🗑️ Remove Item",
                             command=self._remove_item).pack(side=tk.RIGHT, padx=(4, 0))
        create_button(hdr, "✏️ Edit Item",
                      command=self._edit_item).pack(side=tk.RIGHT, padx=4)
        self._held_btn = create_button(hdr, f"▶️ Held Bills ({len(POSSaleFrame._HELD_BILLS)})",
                                      command=self._show_held_bills)
        self._held_btn.pack(side=tk.RIGHT, padx=4)
        create_warning_button(hdr, "⏸️ Hold Bill",
                              command=self._hold_bill).pack(side=tk.RIGHT, padx=4)

        # Table container
        tbl_frame = tk.Frame(right, bg=APP_BACKGROUND)
        tbl_frame.pack(fill=tk.BOTH, expand=True)

        cols = [
            ("#",         35, "#"),
            ("code",      85, "Code"),
            ("name",     180, "Item Name"),
            ("qty",       50, "Qty"),
            ("cost",      75, "Cost (₹)"),
            ("price",     80, "Price"),
            ("disc",      65, "Disc"),
            ("total",     90, "Total"),
        ]
        self._bill_tree = create_table(tbl_frame, cols, height=10)
        self._bill_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self._bill_tree.bind("<Double-1>", lambda e: self._edit_item())

        vsb = ttk.Scrollbar(tbl_frame, orient=tk.VERTICAL,
                            command=self._bill_tree.yview)
        self._bill_tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        # Checkout Summary Card (Compact 2x2 Layout)
        tot_card = tk.Frame(right, bg="#F8FAFC", bd=1, relief=tk.SOLID, padx=16, pady=10)
        tot_card.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
        tot_card.pack(fill=tk.X, pady=(6, 4))

        self._subtotal_var   = tk.StringVar(value="₹ 0.00")
        self._discount_var   = tk.StringVar(value="₹ 0.00")
        self._total_var      = tk.StringVar(value="₹ 0.00")
        self._balance_var    = tk.StringVar(value="₹ 0.00")

        # Row 1: Subtotal & Grand Total
        r1 = tk.Frame(tot_card, bg="#F8FAFC")
        r1.pack(fill=tk.X, pady=2)

        f_sub = tk.Frame(r1, bg="#F8FAFC")
        f_sub.pack(side=tk.LEFT)
        create_label(f_sub, "Subtotal:", font=NORMAL_FONT, bg="#F8FAFC", fg="#475569").pack(side=tk.LEFT)
        tk.Label(f_sub, textvariable=self._subtotal_var, font=("Segoe UI", 11, "bold"),
                 bg="#F8FAFC", fg=DARK_COLOR).pack(side=tk.LEFT, padx=6)

        f_tot = tk.Frame(r1, bg="#F8FAFC")
        f_tot.pack(side=tk.RIGHT)
        create_label(f_tot, "GRAND TOTAL:", font=("Segoe UI", 11, "bold"), bg="#F8FAFC", fg="#1E293B").pack(side=tk.LEFT)
        tk.Label(f_tot, textvariable=self._total_var, font=("Segoe UI", 16, "bold"),
                 bg="#F8FAFC", fg="#10B981").pack(side=tk.LEFT, padx=6)

        # Row 2: Discount & Balance
        r2 = tk.Frame(tot_card, bg="#F8FAFC")
        r2.pack(fill=tk.X, pady=2)

        f_disc = tk.Frame(r2, bg="#F8FAFC")
        f_disc.pack(side=tk.LEFT)
        create_label(f_disc, "Discount:", font=NORMAL_FONT, bg="#F8FAFC", fg="#475569").pack(side=tk.LEFT)
        tk.Label(f_disc, textvariable=self._discount_var, font=("Segoe UI", 11, "bold"),
                 bg="#F8FAFC", fg=DARK_COLOR).pack(side=tk.LEFT, padx=6)

        f_bal = tk.Frame(r2, bg="#F8FAFC")
        f_bal.pack(side=tk.RIGHT)
        create_label(f_bal, "Balance / Change:", font=("Segoe UI", 10, "bold"), bg="#F8FAFC", fg="#475569").pack(side=tk.LEFT)
        tk.Label(f_bal, textvariable=self._balance_var, font=("Segoe UI", 13, "bold"),
                 bg="#F8FAFC", fg="#0284C7").pack(side=tk.LEFT, padx=6)

    # ── Business logic ─────────────────────────────────────────────────────
    def _new_bill(self) -> None:
        bill_no = f"INV-{datetime.now():%Y%m%d}-{self._bill_counter}"
        self._bill_counter += 1
        self._current_bill = Bill(
            bill_number=bill_no,
            bill_type="SALES",
            customer_id=0,
            customer_name="Walk-In Customer",
        )
        self._current_bill.user_id = self.user.user_id
        self._bill_no_var.set(bill_no)
        self._customer_var.set("Walk-In Customer")
        if hasattr(self, "_bill_disc_entry"):
            self._bill_disc_entry.delete(0, tk.END)
            self._bill_disc_entry.insert(0, "0")
        if hasattr(self, "_item_disc_entry"):
            self._item_disc_entry.delete(0, tk.END)
            self._item_disc_entry.insert(0, "0")

        # Auto-restore pending draft cart if available
        draft = load_draft_cart()
        if draft and isinstance(draft, dict) and draft.get("items"):
            for idict in draft.get("items", []):
                bi = BillItem(
                    bill_id=0,
                    item_id=idict.get("item_id", 0),
                    item_code=idict.get("item_code", "ITEM"),
                    item_name=idict.get("item_name", "Item"),
                    quantity=idict.get("quantity", 1),
                    unit_price=float(idict.get("unit_price", 0.0)),
                    discount=float(idict.get("discount", 0.0)),
                )
                bi.purchase_price = float(idict.get("purchase_price", 0.0))
                bi.calculate_amount()
                self._current_bill.bill_items.append(bi)
            if draft.get("customer_name"):
                self._customer_var.set(draft["customer_name"])

        self._refresh_table()

    def _clear_bill(self) -> None:
        clear_draft_cart()
        if self._current_bill:
            self._current_bill.bill_items.clear()
            if hasattr(self, "_bill_disc_entry"):
                self._bill_disc_entry.delete(0, tk.END)
                self._bill_disc_entry.insert(0, "0")
            if hasattr(self, "_item_disc_entry"):
                self._item_disc_entry.delete(0, tk.END)
                self._item_disc_entry.insert(0, "0")
            self._refresh_table()

    def _search_customer(self) -> None:
        phone = self._cust_search.get().strip()
        if not phone:
            messagebox.showwarning("Input", "Enter phone number to search.", parent=self)
            return
        cust = dao.search_customer_by_phone(phone)
        if cust:
            if self._current_bill:
                self._current_bill.customer_id = cust.customer_id
                self._current_bill.customer_name = cust.customer_name
                self._current_bill.customer_phone = cust.phone
            self._customer_var.set(str(cust))
        else:
            messagebox.showinfo("Not Found", f"No customer with phone {phone}.", parent=self)

    def _on_code_enter(self) -> None:
        self._preview_item_cost()
        self._qty_entry.focus_set()

    def _preview_item_cost(self) -> None:
        code = self._item_entry.get().strip()
        if not code:
            self._preview_lbl.config(
                text="💡 Type code/barcode to preview Cost & Stock",
                fg=DARK_COLOR
            )
            return
        item = dao.search_item_by_barcode(code) or dao.search_item_by_code(code)
        if item:
            self._preview_lbl.config(
                text=f"💰 Cost: ₹ {item.purchase_price:,.2f} | 🏷️ Price: ₹ {item.selling_price:,.2f} | Stock: {item.stock_quantity}",
                fg=PRIMARY_COLOR
            )
        else:
            self._preview_lbl.config(
                text=f"❌ Item '{code}' not found.",
                fg=DANGER_COLOR
            )

    def _add_item(self) -> None:
        code = self._item_entry.get().strip()
        qty_str = self._qty_entry.get().strip()
        disc_str = self._item_disc_entry.get().strip() if hasattr(self, "_item_disc_entry") else "0"

        if not code:
            messagebox.showwarning("Input Required", "Enter or scan item code / barcode.", parent=self)
            self._item_entry.focus_set()
            return

        try:
            qty = int(qty_str)
            if qty <= 0:
                raise ValueError
        except ValueError:
            messagebox.showerror("Invalid", "Quantity must be a positive integer.", parent=self)
            self._item_entry.focus_set()
            return

        # Search by barcode first, then by item code (case-insensitive)
        item = dao.search_item_by_barcode(code) or dao.search_item_by_code(code)
        if not item:
            if messagebox.askyesno(
                "Item Not Found 📦",
                f"Item / Barcode '{code}' was not found in inventory master.\n\n"
                f"Would you like to add it directly as an Old Stock / Custom Item with custom cost and sale rate?",
                parent=self
            ):
                self._open_add_custom_item(preset_code=code)
            else:
                self._item_entry.focus_set()
            return

        # Check if item already exists in bill to increment quantity seamlessly
        existing_bi = None
        if self._current_bill:
            for bi in self._current_bill.bill_items:
                if bi.item_code.lower() == item.item_code.lower() or bi.item_id == item.item_id:
                    existing_bi = bi
                    break

        new_total_qty = (existing_bi.quantity + qty) if existing_bi else qty

        if item.stock_quantity < new_total_qty:
            messagebox.showwarning(
                "Stock Limit", f"Only {item.stock_quantity} units available in stock for '{item.item_name}'.", parent=self
            )
            self._item_entry.focus_set()
            return

        # Calculate item-level discount
        base_item_total = item.selling_price * qty
        disc_val = _parse_discount_value(disc_str, base_item_total)
        item_disc_per_unit = disc_val / qty

        if existing_bi:
            existing_bi.quantity += qty
            if disc_val > 0:
                existing_bi.discount = item_disc_per_unit
            existing_bi.calculate_amount()
        else:
            bill_item = BillItem(
                item_id=item.item_id,
                item_code=item.item_code,
                item_name=item.item_name,
                quantity=qty,
                unit_price=item.selling_price,
                discount=item_disc_per_unit,
            )
            bill_item.purchase_price = item.purchase_price
            if self._current_bill:
                self._current_bill.add_bill_item(bill_item)

        # Clear inputs and re-focus for rapid barcode scanning / manual entry
        self._item_entry.delete(0, tk.END)
        self._qty_entry.delete(0, tk.END)
        self._qty_entry.insert(0, "1")
        if hasattr(self, "_item_disc_entry"):
            self._item_disc_entry.delete(0, tk.END)
            self._item_disc_entry.insert(0, "0")
        self._preview_item_cost()
        self._refresh_table()
        self._item_entry.focus_set()

    def _open_add_custom_item(self, preset_code: str = "") -> None:
        """Open dialog to add an old stock / direct custom item to bill queue."""
        _AddCustomItemDialog(self, preset_code=preset_code, on_add=self._add_custom_bill_item)

    def _add_custom_bill_item(self, bill_item: BillItem) -> None:
        """Add a custom / old stock bill item directly to current bill queue."""
        if self._current_bill:
            self._current_bill.add_bill_item(bill_item)
        self._item_entry.delete(0, tk.END)
        self._qty_entry.delete(0, tk.END)
        self._qty_entry.insert(0, "1")
        if hasattr(self, "_item_disc_entry"):
            self._item_disc_entry.delete(0, tk.END)
            self._item_disc_entry.insert(0, "0")
        self._preview_item_cost()
        self._refresh_table()
        self._item_entry.focus_set()

    def _remove_item(self) -> None:
        selected = self._bill_tree.selection()
        if not selected or not self._current_bill:
            return
        idx = self._bill_tree.index(selected[0])
        self._current_bill.remove_bill_item(idx)
        self._refresh_table()

    _HELD_BILLS: List[dict] = []

    def _hold_bill(self) -> None:
        """Park/Hold current customer's bill without deleting items."""
        if not self._current_bill or not self._current_bill.bill_items:
            messagebox.showinfo("Empty Bill", "No items in current bill to hold.", parent=self)
            return

        held_time = datetime.now().strftime("%I:%M %p")
        held_id = f"HOLD-{len(POSSaleFrame._HELD_BILLS) + 1}"
        cust_name = self._current_bill.customer_name or "Walk-In Customer"

        held_data = {
            "id": held_id,
            "bill": self._current_bill,
            "customer_var": self._customer_var.get() if hasattr(self, "_customer_var") else "Walk-In Customer",
            "cust_search": self._cust_search.get() if hasattr(self, "_cust_search") else "",
            "bill_disc": self._bill_disc_entry.get() if hasattr(self, "_bill_disc_entry") else "0",
            "held_time": held_time,
            "items_count": len(self._current_bill.bill_items),
            "total_amount": self._current_bill.total_amount,
        }

        POSSaleFrame._HELD_BILLS.append(held_data)

        old_no = self._current_bill.bill_number
        self._new_bill()
        self._update_held_btn_label()

        messagebox.showinfo(
            "Bill Parked ⏸️",
            f"Bill {old_no} ({cust_name}) held successfully at {held_time}.\n"
            f"You can now serve the next customer!",
            parent=self
        )

    def _show_held_bills(self) -> None:
        """Open modal dialog to view and resume held bills."""
        if not POSSaleFrame._HELD_BILLS:
            messagebox.showinfo("No Held Bills", "There are currently no held or parked bills.", parent=self)
            return

        _HeldBillsDialog(self, on_resume=self._resume_held_bill, on_update=self._update_held_btn_label)

    def _resume_held_bill(self, held_data: dict) -> None:
        """Restore a held bill into the active cart."""
        if self._current_bill and self._current_bill.bill_items:
            ans = messagebox.askyesnocancel(
                "Active Bill Exists",
                "Current bill has items!\n\n"
                "• Click YES to park/hold current bill first, then resume.\n"
                "• Click NO to overwrite current bill.\n"
                "• Click CANCEL to stop.",
                parent=self
            )
            if ans is None:
                return
            if ans is True:
                self._hold_bill()

        self._current_bill = held_data["bill"]
        if hasattr(self, "_customer_var") and "customer_var" in held_data:
            self._customer_var.set(held_data["customer_var"])
        if hasattr(self, "_cust_search") and "cust_search" in held_data:
            self._cust_search.delete(0, tk.END)
            self._cust_search.insert(0, held_data["cust_search"])
        if hasattr(self, "_bill_disc_entry") and "bill_disc" in held_data:
            self._bill_disc_entry.delete(0, tk.END)
            self._bill_disc_entry.insert(0, held_data["bill_disc"])

        if held_data in POSSaleFrame._HELD_BILLS:
            POSSaleFrame._HELD_BILLS.remove(held_data)

        self._bill_no_var.set(self._current_bill.bill_number)
        self._refresh_table()
        self._update_held_btn_label()

    def _update_held_btn_label(self) -> None:
        cnt = len(POSSaleFrame._HELD_BILLS)
        if hasattr(self, "_held_btn"):
            self._held_btn.config(text=f"▶️ Held Bills ({cnt})")

    def _edit_item(self) -> None:
        selected = self._bill_tree.selection()
        if not selected or not self._current_bill:
            messagebox.showinfo("Select Item", "Please select an item from the queue to edit.", parent=self)
            return
        idx = self._bill_tree.index(selected[0])
        if 0 <= idx < len(self._current_bill.bill_items):
            bi = self._current_bill.bill_items[idx]
            _EditCartItemDialog(self, bi, on_save=self._refresh_table)

    def _complete_bill(self) -> None:
        if not self._current_bill or not self._current_bill.bill_items:
            messagebox.showwarning("Empty Bill", "Add at least one item.", parent=self)
            return

        self._refresh_table()

        try:
            paid = float(self._paid_var.get())
        except ValueError:
            messagebox.showerror("Invalid", "Enter a valid payment amount.", parent=self)
            return

        method_name = self._pay_method.get()
        payment = PaymentProcessor.create_payment(
            method_name,
            amount_tendered=paid,
            amount_due=self._current_bill.total_amount,
        )
        if not PaymentProcessor.process(payment, self._current_bill.total_amount):
            messagebox.showerror("Payment", "Payment could not be processed.", parent=self)
            return

        if not self._current_bill.complete_bill(paid, method_name):
            messagebox.showerror("Payment", "Amount paid is less than total.", parent=self)
            return

        # Update stock quantities (only for registered inventory items)
        for bi in self._current_bill.bill_items:
            if bi.item_id and bi.item_id > 0:
                item = dao.search_item_by_code(bi.item_code)
                if item:
                    dao.update_stock(item.item_id, item.stock_quantity - bi.quantity)

        # Update customer loyalty
        if self._current_bill.customer_id:
            cust = dao.search_customer_by_code("")  # just ensure customer list is current
            dao.update_customer_loyalty(
                self._current_bill.customer_id,
                self._current_bill.total_amount * 0.1,
                datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            )

        # Save to DB
        dao.save_bill(self._current_bill)
        clear_draft_cart()

        # Print receipt to console (mirrors Bill.printBill)
        self._current_bill.print_bill()

        change = max(0.0, paid - self._current_bill.total_amount)
        messagebox.showinfo(
            "Bill Completed ✅",
            f"Bill {self._current_bill.bill_number} completed!\n"
            f"Total: ₹ {self._current_bill.total_amount:,.2f}\n"
            f"Paid:  ₹ {paid:,.2f}\n"
            f"Change: ₹ {change:,.2f}",
            parent=self,
        )

        completed_bill = self._current_bill
        self._new_bill()
        try:
            from pos_billing.ui.app import _ReceiptDlg
            _ReceiptDlg(self, completed_bill, paid, change)
        except Exception:
            pass

    def _show_upi_qr(self) -> None:
        if not self._current_bill or not self._current_bill.bill_items:
            messagebox.showwarning("Empty Bill", "Add at least one item first to generate a payment QR code.", parent=self)
            return
        self._refresh_table()
        from pos_billing.utils.qr_generator import show_live_upi_dialog
        show_live_upi_dialog(self, self._current_bill)

    def _update_balance(self) -> None:
        """Dynamically update change / balance when amount paid changes."""
        if not self._current_bill:
            return
        try:
            paid = float(self._paid_var.get())
        except ValueError:
            paid = 0.0
        tot = self._current_bill.total_amount
        self._current_bill.paid_amount = paid
        change = paid - tot
        if change >= 0:
            self._balance_var.set(f"Change: ₹ {change:,.2f}")
        else:
            self._balance_var.set(f"Due: ₹ {abs(change):,.2f}")

    def _refresh_table(self) -> None:
        """Refresh the bill items table and recalculate totals with discounts."""
        for row in self._bill_tree.get_children():
            self._bill_tree.delete(row)

        if not self._current_bill:
            return

        gross_subtotal = 0.0
        item_discounts = 0.0

        for i, bi in enumerate(self._current_bill.bill_items, start=1):
            bi.calculate_amount()
            item_gross = bi.unit_price * bi.quantity
            item_disc = bi.discount * bi.quantity
            gross_subtotal += item_gross
            item_discounts += item_disc

            cost_val = getattr(bi, "purchase_price", 0.0)
            self._bill_tree.insert("", tk.END, values=(
                i, bi.item_code, bi.item_name,
                bi.quantity,
                f"{cost_val:.2f}",
                f"{bi.unit_price:.2f}",
                f"{bi.discount:.2f}",
                f"{bi.total_amount:.2f}",
            ))

        # Check overall bill discount
        bill_disc_str = self._bill_disc_entry.get().strip() if hasattr(self, "_bill_disc_entry") else "0"
        net_after_item_disc = max(0.0, gross_subtotal - item_discounts)
        overall_disc = _parse_discount_value(bill_disc_str, net_after_item_disc)

        total_discount = item_discounts + overall_disc
        grand_total = max(0.0, gross_subtotal - total_discount)

        self._current_bill.subtotal = gross_subtotal
        self._current_bill.total_discount = total_discount
        self._current_bill.total_amount = grand_total

        paid = grand_total
        self._paid_var.set(f"{grand_total:.2f}")

        change = paid - grand_total
        self._current_bill.paid_amount = paid
        self._current_bill.balance_amount = change

        self._subtotal_var.set(f"₹ {gross_subtotal:,.2f}")
        self._discount_var.set(f"₹ {total_discount:,.2f}")
        self._total_var.set(f"₹ {grand_total:,.2f}")

        if change > 0:
            self._balance_var.set(f"Change: ₹ {change:,.2f}")
        else:
            self._balance_var.set(f"₹ 0.00")

        # Auto-save draft cart for recovery
        if self._current_bill and self._current_bill.bill_items:
            cart_data = {
                "bill_no": self._current_bill.bill_number,
                "customer_name": self._current_bill.customer_name or "Walk-In Customer",
                "items": [
                    {
                        "item_id": bi.item_id,
                        "item_code": bi.item_code,
                        "item_name": bi.item_name,
                        "quantity": bi.quantity,
                        "unit_price": bi.unit_price,
                        "discount": bi.discount,
                        "purchase_price": getattr(bi, "purchase_price", 0.0),
                        "total_amount": bi.total_amount
                    }
                    for bi in self._current_bill.bill_items
                ]
            }
            save_draft_cart(cart_data)
        else:
            clear_draft_cart()


class _EditCartItemDialog(tk.Toplevel):
    """Dialog to edit item quantity, cost price, selling price, and discount directly from table selection."""

    def __init__(self, parent: tk.Widget, bill_item: BillItem, on_save: Callable[[], None]) -> None:
        super().__init__(parent)
        self.title(f"Edit Cart Item - {bill_item.item_code}")
        self.configure(bg=APP_BACKGROUND)
        self.geometry("420x440")
        self.resizable(False, False)
        self.transient(parent)
        self.grab_set()

        self._bi = bill_item
        self._on_save = on_save

        tk.Label(
            self, text="✏️  Edit Cart Item",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(16, 4))

        tk.Label(
            self, text=f"{bill_item.item_name} ({bill_item.item_code})",
            font=NORMAL_FONT, bg=APP_BACKGROUND, fg=PRIMARY_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(0, 10))

        frm = tk.Frame(self, bg=APP_BACKGROUND, padx=20, pady=10)
        frm.pack(fill=tk.BOTH, expand=True)

        cost_val = getattr(bill_item, "purchase_price", 0.0)
        create_label(frm, "Cost Price (₹) *").grid(row=0, column=0, sticky=tk.W, pady=6)
        self._cost_entry = create_entry(frm, width=18)
        self._cost_entry.insert(0, f"{cost_val:.2f}")
        self._cost_entry.grid(row=0, column=1, pady=6)

        create_label(frm, "Selling Price / Rate (₹) *").grid(row=1, column=0, sticky=tk.W, pady=6)
        self._price_entry = create_entry(frm, width=18)
        self._price_entry.insert(0, f"{bill_item.unit_price:.2f}")
        self._price_entry.grid(row=1, column=1, pady=6)

        create_label(frm, "Quantity *").grid(row=2, column=0, sticky=tk.W, pady=6)
        self._qty_entry = create_entry(frm, width=18)
        self._qty_entry.insert(0, str(bill_item.quantity))
        self._qty_entry.grid(row=2, column=1, pady=6)

        create_label(frm, "Discount (₹ / %)").grid(row=3, column=0, sticky=tk.W, pady=6)
        self._disc_entry = create_entry(frm, width=18)
        self._disc_entry.insert(0, f"{bill_item.discount:.2f}")
        self._disc_entry.grid(row=3, column=1, pady=6)

        btn_box = tk.Frame(self, bg=APP_BACKGROUND, pady=12)
        btn_box.pack(fill=tk.X)

        create_success_button(btn_box, "💾 Save Changes", command=self._save).pack(side=tk.LEFT, padx=20)
        create_button(btn_box, "✖ Cancel", command=self.destroy).pack(side=tk.LEFT)

    def _save(self) -> None:
        try:
            cost = float(self._cost_entry.get().strip())
            if cost < 0: raise ValueError
        except ValueError:
            messagebox.showerror("Invalid", "Cost price must be a non-negative number.", parent=self)
            return

        try:
            price = float(self._price_entry.get().strip())
            if price < 0: raise ValueError
        except ValueError:
            messagebox.showerror("Invalid", "Selling price must be a non-negative number.", parent=self)
            return

        try:
            qty = int(self._qty_entry.get().strip())
            if qty <= 0: raise ValueError
        except ValueError:
            messagebox.showerror("Invalid", "Quantity must be a positive integer.", parent=self)
            return

        disc_str = self._disc_entry.get().strip()
        disc_val = _parse_discount_value(disc_str, price * qty)
        unit_disc = disc_val / qty if qty > 0 else 0.0

        self._bi.purchase_price = cost
        self._bi.unit_price = price
        self._bi.quantity = qty
        self._bi.discount = unit_disc
        self._bi.calculate_amount()

        if self._on_save:
            self._on_save()
        self.destroy()


class _AddCustomItemDialog(tk.Toplevel):
    """Dialog to add direct / old stock item with customizable cost and selling price."""

    def __init__(self, parent: tk.Widget, preset_code: str = "", on_add: Callable[[BillItem], None] = None) -> None:
        super().__init__(parent)
        self.title("Add Direct / Old Stock Item")
        self.configure(bg=APP_BACKGROUND)
        self.geometry("450x490")
        self.resizable(False, False)
        self.transient(parent)
        self.grab_set()

        self._on_add = on_add

        tk.Label(
            self, text="📦  Add Direct / Old Stock Item",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(16, 4))

        tk.Label(
            self, text="Add old stock or custom items with custom Cost Price & Sale Rate:",
            font=SMALL_FONT, bg=APP_BACKGROUND, fg=PRIMARY_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(0, 10))

        frm = tk.Frame(self, bg=APP_BACKGROUND, padx=20, pady=10)
        frm.pack(fill=tk.BOTH, expand=True)

        create_label(frm, "Item Code / Barcode *").grid(row=0, column=0, sticky=tk.W, pady=6)
        self._code_entry = create_entry(frm, width=20)
        self._code_entry.insert(0, preset_code if preset_code else f"OLD-{datetime.now():%S%f}"[:10])
        self._code_entry.grid(row=0, column=1, pady=6)

        create_label(frm, "Item Name / Description *").grid(row=1, column=0, sticky=tk.W, pady=6)
        self._name_entry = create_entry(frm, width=20)
        self._name_entry.insert(0, "Old Stock Item")
        self._name_entry.grid(row=1, column=1, pady=6)

        create_label(frm, "Selling Price / Rate (₹) *").grid(row=2, column=0, sticky=tk.W, pady=6)
        self._price_entry = create_entry(frm, width=20)
        self._price_entry.insert(0, "0.00")
        self._price_entry.grid(row=2, column=1, pady=6)

        create_label(frm, "Cost Price (₹) *").grid(row=3, column=0, sticky=tk.W, pady=6)
        self._cost_entry = create_entry(frm, width=20)
        self._cost_entry.insert(0, "0.00")
        self._cost_entry.grid(row=3, column=1, pady=6)

        create_label(frm, "Quantity *").grid(row=4, column=0, sticky=tk.W, pady=6)
        self._qty_entry = create_entry(frm, width=20)
        self._qty_entry.insert(0, "1")
        self._qty_entry.grid(row=4, column=1, pady=6)

        create_label(frm, "Discount (₹ / %)").grid(row=5, column=0, sticky=tk.W, pady=6)
        self._disc_entry = create_entry(frm, width=20)
        self._disc_entry.insert(0, "0")
        self._disc_entry.grid(row=5, column=1, pady=6)

        btn_box = tk.Frame(self, bg=APP_BACKGROUND, pady=12)
        btn_box.pack(fill=tk.X)

        create_success_button(btn_box, "➕ Add to Bill Queue", command=self._add).pack(side=tk.LEFT, padx=20)
        create_button(btn_box, "✖ Cancel", command=self.destroy).pack(side=tk.LEFT)

        self.after(100, lambda: self._name_entry.focus_set() if preset_code else self._code_entry.focus_set())

    def _add(self) -> None:
        code = self._code_entry.get().strip() or "OLD-STOCK"
        name = self._name_entry.get().strip() or "Old Stock Item"

        try:
            price = float(self._price_entry.get().strip())
            if price < 0: raise ValueError
        except ValueError:
            messagebox.showerror("Invalid", "Selling price must be a non-negative number.", parent=self)
            return

        try:
            cost = float(self._cost_entry.get().strip())
            if cost < 0: raise ValueError
        except ValueError:
            messagebox.showerror("Invalid", "Cost price must be a non-negative number.", parent=self)
            return

        try:
            qty = int(self._qty_entry.get().strip())
            if qty <= 0: raise ValueError
        except ValueError:
            messagebox.showerror("Invalid", "Quantity must be a positive integer.", parent=self)
            return

        disc_str = self._disc_entry.get().strip()
        disc_val = _parse_discount_value(disc_str, price * qty)
        unit_disc = disc_val / qty if qty > 0 else 0.0

        bi = BillItem(
            item_id=0,
            item_code=code,
            item_name=name,
            quantity=qty,
            unit_price=price,
            discount=unit_disc,
        )
        bi.purchase_price = cost
        bi.calculate_amount()

        if self._on_add:
            self._on_add(bi)
        self.destroy()


class _HeldBillsDialog(tk.Toplevel):
    """Dialog to view and resume parked/held customer bills."""

    def __init__(self, parent: POSSaleFrame, on_resume: Callable[[dict], None], on_update: Callable[[], None]) -> None:
        super().__init__(parent)
        self.title("Parked / Held Bills Manager")
        self.configure(bg=APP_BACKGROUND)
        self.geometry("600x420")
        self.resizable(False, False)
        self.transient(parent)
        self.grab_set()

        self._parent = parent
        self._on_resume = on_resume
        self._on_update = on_update

        tk.Label(
            self, text="⏸️  Parked / Held Bills Manager",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(16, 4))

        tk.Label(
            self, text="Select a held bill to resume serving that customer:",
            font=NORMAL_FONT, bg=APP_BACKGROUND, fg=PRIMARY_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(0, 10))

        tbl_frame = tk.Frame(self, bg=APP_BACKGROUND)
        tbl_frame.pack(fill=tk.BOTH, expand=True, padx=20)

        cols = [
            ("#",          35, "#"),
            ("bill_no",   140, "Bill No"),
            ("time",       90, "Held Time"),
            ("customer",  140, "Customer"),
            ("items",      60, "Items"),
            ("total",     100, "Total (₹)"),
        ]
        self._tree = create_table(tbl_frame, cols, height=8)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        vsb = ttk.Scrollbar(tbl_frame, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        self._load_table()

        btn_box = tk.Frame(self, bg=APP_BACKGROUND, pady=12, padx=20)
        btn_box.pack(fill=tk.X)

        create_success_button(btn_box, "▶️ Resume Selected Bill", command=self._resume).pack(side=tk.LEFT, padx=(0, 8))
        create_danger_button(btn_box, "🗑️ Discard Held Bill", command=self._discard).pack(side=tk.LEFT, padx=8)
        create_button(btn_box, "✖ Close", command=self.destroy).pack(side=tk.RIGHT)

        self._tree.bind("<Double-1>", lambda e: self._resume())

    def _load_table(self) -> None:
        for r in self._tree.get_children():
            self._tree.delete(r)

        for i, h in enumerate(POSSaleFrame._HELD_BILLS, start=1):
            b = h["bill"]
            self._tree.insert("", tk.END, iid=str(i-1), values=(
                i, b.bill_number, h["held_time"],
                b.customer_name or "Walk-In",
                h["items_count"],
                f"{h['total_amount']:,.2f}"
            ))

    def _resume(self) -> None:
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select a held bill from the list to resume.", parent=self)
            return
        idx = int(sel[0])
        if 0 <= idx < len(POSSaleFrame._HELD_BILLS):
            held_data = POSSaleFrame._HELD_BILLS[idx]
            self.destroy()
            self._on_resume(held_data)

    def _discard(self) -> None:
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select a held bill to discard.", parent=self)
            return
        idx = int(sel[0])
        if 0 <= idx < len(POSSaleFrame._HELD_BILLS):
            if messagebox.askyesno("Discard", "Are you sure you want to discard this held bill?", parent=self):
                POSSaleFrame._HELD_BILLS.pop(idx)
                self._load_table()
                if self._on_update:
                    self._on_update()
