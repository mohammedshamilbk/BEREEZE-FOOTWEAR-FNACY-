# ============================================================
# pos_billing/ui/frames/expenses_frame.py
# ============================================================
"""Shop Expenses & Outflow Management Panel."""

from __future__ import annotations

import csv
import tkinter as tk
from datetime import datetime
from tkinter import filedialog, messagebox, ttk

from ...database import dao
from ...database.models import Expense, User
from ...utils import excel_exporter
from ..constants import (
    APP_BACKGROUND, BORDER_COLOR, DANGER_COLOR, DARK_COLOR,
    HEADING_FONT, NORMAL_FONT, PRIMARY_COLOR, SMALL_FONT,
    SUCCESS_COLOR, TEXT_ON_PRIMARY, WARNING_COLOR,
)
from ..widgets import (
    create_button, create_danger_button, create_entry, create_label,
    create_success_button, create_table, create_warning_button,
)


class ExpensesFrame(tk.Frame):
    """Expenses panel – tracks cash and UPI outflows."""

    EXPENSE_CATEGORIES = [
        "Rent / Shop Lease",
        "Electricity / Utility",
        "Tea / Snacks / Food",
        "Freight / Transport / Courier",
        "Staff Salary / Wages",
        "Shop Maintenance / Repairs",
        "Packaging / Bags / Boxes",
        "Marketing / Advertising",
        "Miscellaneous / Other",
    ]

    def __init__(self, parent: tk.Widget, user: User) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.user = user
        self._expenses: list[Expense] = []
        self._build()
        self._load_expenses()

    def _build(self) -> None:
        # Title
        tk.Label(
            self, text="💸  Shop Expenses & Outflow Tracker",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(anchor=tk.W, padx=16, pady=(12, 4))

        # KPI Summary Cards Bar
        self._kpi_bar = tk.Frame(self, bg=APP_BACKGROUND)
        self._kpi_bar.pack(fill=tk.X, padx=16, pady=(0, 8))

        self._card_cash_var = tk.StringVar(value="₹ 0.00")
        self._card_upi_var  = tk.StringVar(value="₹ 0.00")
        self._card_today_var = tk.StringVar(value="₹ 0.00")
        self._card_month_var = tk.StringVar(value="₹ 0.00")

        kpis = [
            ("💵 Cash Expenses (Today)", self._card_cash_var, "#D97706"),
            ("📱 UPI Expenses (Today)",  self._card_upi_var,  "#2563EB"),
            ("💰 Total Expenses (Today)", self._card_today_var, "#DC2626"),
            ("📊 Total Monthly Expenses", self._card_month_var, "#8B5CF6"),
        ]

        for title, var, col in kpis:
            card = tk.Frame(self._kpi_bar, bg="#F8FAFC", bd=1, relief=tk.SOLID, padx=14, pady=8)
            card.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
            card.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=4)

            tk.Label(card, text=title, font=SMALL_FONT, bg="#F8FAFC", fg="#64748B").pack(anchor=tk.W)
            tk.Label(card, textvariable=var, font=("Segoe UI", 14, "bold"), bg="#F8FAFC", fg=col).pack(anchor=tk.W, pady=(2, 0))

        # Main Body: Form (Left) + Table (Right)
        body = tk.Frame(self, bg=APP_BACKGROUND)
        body.pack(fill=tk.BOTH, expand=True, padx=16, pady=(0, 10))

        # ── Left: Add Expense Form ──────────────────────────────────────────
        left = tk.LabelFrame(body, text=" Record New Expense ", font=SMALL_FONT,
                             bg=APP_BACKGROUND, fg=PRIMARY_COLOR, padx=12, pady=10)
        left.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 8))

        create_label(left, "Date & Time:").grid(row=0, column=0, sticky=tk.W, pady=4)
        self._date_entry = create_entry(left, width=22)
        self._date_entry.insert(0, datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        self._date_entry.grid(row=0, column=1, pady=4)

        create_label(left, "Category *:").grid(row=1, column=0, sticky=tk.W, pady=4)
        self._cat_cb = ttk.Combobox(left, values=self.EXPENSE_CATEGORIES, width=20, state="readonly")
        self._cat_cb.current(0)
        self._cat_cb.grid(row=1, column=1, pady=4)

        create_label(left, "Amount (₹) *:").grid(row=2, column=0, sticky=tk.W, pady=4)
        self._amount_entry = create_entry(left, width=22)
        self._amount_entry.grid(row=2, column=1, pady=4)

        create_label(left, "Payment Mode *:").grid(row=3, column=0, sticky=tk.W, pady=4)
        self._mode_cb = ttk.Combobox(left, values=["CASH", "UPI", "ONLINE", "CARD", "CHEQUE"], width=20, state="readonly")
        self._mode_cb.current(0)
        self._mode_cb.grid(row=3, column=1, pady=4)

        create_label(left, "Description / Notes:").grid(row=4, column=0, sticky=tk.W, pady=4)
        self._desc_entry = create_entry(left, width=22)
        self._desc_entry.grid(row=4, column=1, pady=4)

        create_success_button(left, "➕ Save Expense", command=self._add_expense).grid(
            row=5, column=0, columnspan=2, sticky="we", pady=(14, 4)
        )

        # ── Right: Expense Table ────────────────────────────────────────────
        right = tk.Frame(body, bg=APP_BACKGROUND)
        right.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Action bar above table
        act_bar = tk.Frame(right, bg=APP_BACKGROUND)
        act_bar.pack(fill=tk.X, pady=(0, 6))

        create_label(act_bar, "Filter:").pack(side=tk.LEFT, padx=(0, 4))
        self._filter_var = tk.StringVar(value="All")
        filter_cb = ttk.Combobox(act_bar, textvariable=self._filter_var, values=["All", "Today", "This Month"], width=12, state="readonly")
        filter_cb.pack(side=tk.LEFT, padx=4)
        filter_cb.bind("<<ComboboxSelected>>", lambda e: self._load_expenses())

        create_button(act_bar, "🔄 Refresh", command=self._load_expenses).pack(side=tk.RIGHT, padx=4)
        create_success_button(act_bar, "📊 Export Excel", command=self._export_excel).pack(side=tk.RIGHT, padx=4)
        create_button(act_bar, "💾 Export CSV", command=self._export_csv).pack(side=tk.RIGHT, padx=4)
        create_danger_button(act_bar, "🗑️ Delete Selected", command=self._delete_expense).pack(side=tk.RIGHT, padx=4)

        # Table container
        tbl_frame = tk.Frame(right, bg=APP_BACKGROUND)
        tbl_frame.pack(fill=tk.BOTH, expand=True)

        cols = [
            ("#",          35, "#"),
            ("date",      140, "Date & Time"),
            ("category",  180, "Category"),
            ("desc",      200, "Description"),
            ("amount",    100, "Amount (₹)"),
            ("mode",       80, "Mode"),
        ]
        self._tree = create_table(tbl_frame, cols, height=14)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        vsb = ttk.Scrollbar(tbl_frame, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

    def _add_expense(self) -> None:
        dt_str = self._date_entry.get().strip() or datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        cat = self._cat_cb.get().strip()
        amt_str = self._amount_entry.get().strip()
        mode = self._mode_cb.get().strip()
        desc = self._desc_entry.get().strip()

        if not amt_str:
            messagebox.showwarning("Input Required", "Please enter the expense amount.", parent=self)
            return

        try:
            amt = float(amt_str)
            if amt <= 0: raise ValueError
        except ValueError:
            messagebox.showerror("Invalid Amount", "Expense amount must be a positive number.", parent=self)
            return

        exp = Expense(
            expense_date=dt_str,
            category=cat,
            description=desc,
            amount=amt,
            payment_mode=mode,
            user_id=self.user.user_id,
        )
        dao.save_expense(exp)

        # Clear fields
        self._amount_entry.delete(0, tk.END)
        self._desc_entry.delete(0, tk.END)
        self._date_entry.delete(0, tk.END)
        self._date_entry.insert(0, datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

        self._load_expenses()
        messagebox.showinfo("Expense Saved", f"Saved expense of ₹ {amt:,.2f} under '{cat}' ({mode}).", parent=self)

    def _load_expenses(self) -> None:
        for r in self._tree.get_children():
            self._tree.delete(r)

        all_exp = dao.get_all_expenses()
        flt = self._filter_var.get()

        today_str = datetime.now().strftime("%Y-%m-%d")
        month_str = datetime.now().strftime("%Y-%m")

        filtered = []
        for e in all_exp:
            if flt == "Today" and not e.expense_date.startswith(today_str):
                continue
            if flt == "This Month" and not e.expense_date.startswith(month_str):
                continue
            filtered.append(e)

        self._expenses = filtered

        for i, e in enumerate(filtered, start=1):
            self._tree.insert("", tk.END, iid=str(e.expense_id), values=(
                i, e.expense_date[:19], e.category, e.description,
                f"{e.amount:,.2f}", e.payment_mode
            ))

        # Recalculate KPI summary values
        today_cash = sum(e.amount for e in all_exp if e.expense_date.startswith(today_str) and e.payment_mode.upper() == "CASH")
        today_upi  = sum(e.amount for e in all_exp if e.expense_date.startswith(today_str) and e.payment_mode.upper() in ("UPI", "ONLINE"))
        today_tot  = sum(e.amount for e in all_exp if e.expense_date.startswith(today_str))
        month_tot  = sum(e.amount for e in all_exp if e.expense_date.startswith(month_str))

        self._card_cash_var.set(f"₹ {today_cash:,.2f}")
        self._card_upi_var.set(f"₹ {today_upi:,.2f}")
        self._card_today_var.set(f"₹ {today_tot:,.2f}")
        self._card_month_var.set(f"₹ {month_tot:,.2f}")

    def _delete_expense(self) -> None:
        sel = self._tree.selection()
        if not sel:
            messagebox.showinfo("Select", "Please select an expense from the table to delete.", parent=self)
            return

        exp_id = int(sel[0])
        if messagebox.askyesno("Confirm Delete", "Are you sure you want to delete this expense record?", parent=self):
            dao.delete_expense(exp_id)
            self._load_expenses()

    def _export_excel(self) -> None:
        if not self._expenses:
            messagebox.showinfo("Export", "No expenses to export.", parent=self)
            return

        path = filedialog.asksaveasfilename(
            defaultextension=".xlsx",
            filetypes=[("Excel Files (*.xlsx)", "*.xlsx"), ("CSV Files (*.csv)", "*.csv")],
            title="Export Expense Register to Excel",
            initialfile=f"expenses_{datetime.now().strftime('%Y%m%d')}.xlsx",
            parent=self
        )
        if path:
            try:
                headers = ["Expense ID", "Date & Time", "Category", "Description", "Amount (₹)", "Payment Mode"]
                rows = [
                    [e.expense_id, e.expense_date, e.category, e.description, e.amount, e.payment_mode]
                    for e in self._expenses
                ]
                out_path = excel_exporter.export_table_to_excel(
                    filepath=path,
                    title="Bereeze Footwear - Shop Expenses Register",
                    headers=headers,
                    rows=rows,
                    sheet_name="Expenses"
                )
                messagebox.showinfo("Export Complete", f"Expenses exported successfully to:\n{out_path}", parent=self)
            except Exception as exc:
                messagebox.showerror("Export Error", f"Could not export expenses: {exc}", parent=self)

    def _export_csv(self) -> None:
        if not self._expenses:
            messagebox.showinfo("Export", "No expenses to export.", parent=self)
            return

        path = filedialog.asksaveasfilename(
            defaultextension=".csv",
            filetypes=[("CSV Files", "*.csv")],
            title="Export Expense Register",
            parent=self
        )
        if path:
            with open(path, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow(["Expense ID", "Date", "Category", "Description", "Amount (INR)", "Payment Mode"])
                for e in self._expenses:
                    writer.writerow([e.expense_id, e.expense_date, e.category, e.description, e.amount, e.payment_mode])
            messagebox.showinfo("Export Complete", f"Expenses exported to:\n{path}", parent=self)
