# ============================================================
# pos_billing/ui/frames/dashboard_frame.py  (DashboardFrame.java → Python)
# ============================================================
"""Dashboard / home page – shows key metrics, quick action buttons, yesterday vs today comparison, shop closing, and recent bills."""

from __future__ import annotations

import csv
import logging
import os
import tkinter as tk
from datetime import datetime, timedelta
from tkinter import filedialog, messagebox, ttk
from typing import Callable, Optional

from ...database import dao
from ...database.models import User
from ...utils.path_manager import EXPORTS_DIR
from ..constants import (
    APP_BACKGROUND, BORDER_COLOR, DARK_COLOR, DANGER_COLOR, HEADING_FONT,
    NORMAL_FONT, PRIMARY_COLOR, SMALL_FONT, SUCCESS_COLOR,
    TEXT_ON_PRIMARY, WARNING_COLOR,
)
from ..widgets import (
    _make_button, create_button, create_danger_button,
    create_secondary_button, create_success_button, create_table
)

logger = logging.getLogger(__name__)


class DashboardFrame(tk.Frame):
    def __init__(self, parent: tk.Widget, user: User, on_navigate: Optional[Callable[[str], None]] = None) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.user = user
        self.on_navigate = on_navigate
        self._tree: Optional[ttk.Treeview] = None
        self._build()

    def _nav(self, key: str) -> None:
        """Navigate to another module frame."""
        if self.on_navigate:
            self.on_navigate(key)
        else:
            top = self.winfo_toplevel()
            if hasattr(top, "_navigate"):
                top._navigate(key)

    def _open_shop_closing_dialog(self) -> None:
        """Open the Shop Closing / Day-End Cash Reconciliation Dialog."""
        ShopClosingDialog(self, user=self.user, on_complete=self._build)

    def _build(self) -> None:
        """Build the dashboard layout with Quick Actions, Yesterday vs Today KPIs, and Recent Bills."""
        for child in self.winfo_children():
            child.destroy()

        # ── 1. Header & Top Quick Action Buttons Bar ─────────────────────
        header = tk.Frame(self, bg=APP_BACKGROUND)
        header.pack(fill=tk.X, padx=20, pady=(14, 6))

        # Header Title Left
        title_box = tk.Frame(header, bg=APP_BACKGROUND)
        title_box.pack(side=tk.LEFT)

        tk.Label(
            title_box,
            text="🏠  Dashboard",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(side=tk.LEFT)

        tk.Label(
            title_box,
            text=f"  –  {datetime.now():%A, %d %B %Y}",
            font=("Segoe UI", 11), bg=APP_BACKGROUND, fg="#64748B",
        ).pack(side=tk.LEFT)

        # Quick Action Buttons Bar Right
        action_bar = tk.Frame(header, bg=APP_BACKGROUND)
        action_bar.pack(side=tk.RIGHT)

        _make_button(action_bar, "🛒 New Sale", "#059669", "white", lambda: self._nav("pos_sale"), pady=5, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(action_bar, "💸 Add Expense", "#DC2626", "white", lambda: self._nav("expenses"), pady=5, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(action_bar, "🔒 Close Shop", "#4F46E5", "white", self._open_shop_closing_dialog, pady=5, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(action_bar, "📦 Inventory", "#0D9488", "white", lambda: self._nav("inventory"), pady=5, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(action_bar, "👥 Customers", "#7B1FA2", "white", lambda: self._nav("customers"), pady=5, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(action_bar, "📊 Reports", "#0284C7", "white", lambda: self._nav("reports"), pady=5, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(action_bar, "🔄 Refresh", "#475569", "white", self._build, pady=5, padx=8).pack(side=tk.LEFT, padx=3)

        # Dates & Calculations
        today_date = datetime.now()
        yesterday_date = today_date - timedelta(days=1)

        today_str = today_date.strftime("%Y-%m-%d")
        yesterday_str = yesterday_date.strftime("%Y-%m-%d")
        month_str = today_date.strftime("%Y-%m")

        today_data = dao.get_daily_sales_by_payment_mode(today_str)
        yesterday_data = dao.get_daily_sales_by_payment_mode(yesterday_str)

        total_sales = dao.get_total_sales()
        items_count = len(dao.get_all_items())
        cust_count  = len(dao.get_all_customers())

        all_bills = dao.get_all_bills(limit=1000)
        bills_today = sum(1 for b in all_bills if b.bill_date.strftime("%Y-%m-%d") == today_str)
        bills_yesterday = sum(1 for b in all_bills if b.bill_date.strftime("%Y-%m-%d") == yesterday_str)
        monthly_sales = sum(b.total_amount for b in all_bills if b.bill_date.strftime("%Y-%m").startswith(month_str) and b.status == "COMPLETED")

        # ── 2. YESTERDAY'S PERFORMANCE SUMMARY ────────────────────────────────
        y_hdr = tk.Frame(self, bg=APP_BACKGROUND)
        y_hdr.pack(fill=tk.X, padx=22, pady=(2, 2))
        tk.Label(
            y_hdr,
            text=f"🗓️  Yesterday's Calculations ({yesterday_date:%d %b %Y})",
            font=("Segoe UI", 10, "bold"), bg=APP_BACKGROUND, fg="#475569",
        ).pack(side=tk.LEFT)

        card_row_yesterday = tk.Frame(self, bg=APP_BACKGROUND)
        card_row_yesterday.pack(fill=tk.X, padx=20, pady=(2, 6))

        yesterday_metrics = [
            ("💵 Cash in Counter (Yesterday)", f"₹ {yesterday_data['net_cash_in_counter']:,.2f}", "#B45309", "reports"),
            ("📱 UPI Received (Yesterday)",   f"₹ {yesterday_data['upi_sales']:,.2f}",          "#0369A1", "reports"),
            ("💸 Expenses (Yesterday)",      f"₹ {yesterday_data['total_expenses']:,.2f}",      "#B91C1C", "expenses"),
            ("📊 Total Sales (Yesterday)",    f"₹ {yesterday_data['total_sales']:,.2f} ({bills_yesterday} Bills)", "#0F766E", "reports"),
        ]

        for i, (label, value, color, nav_target) in enumerate(yesterday_metrics):
            card = tk.Frame(card_row_yesterday, bg=color, bd=0, relief=tk.FLAT, padx=14, pady=8, cursor="hand2")
            card.grid(row=0, column=i, padx=5, sticky="ew")
            card_row_yesterday.columnconfigure(i, weight=1)

            lbl1 = tk.Label(card, text=label, font=("Segoe UI", 8, "bold"), bg=color, fg="#E2E8F0", cursor="hand2")
            lbl1.pack(anchor=tk.W)
            lbl2 = tk.Label(card, text=value, font=("Segoe UI", 14, "bold"), bg=color, fg="white", cursor="hand2")
            lbl2.pack(anchor=tk.W, pady=(1, 0))

            for widget in (card, lbl1, lbl2):
                widget.bind("<Button-1>", lambda e, target=nav_target: self._nav(target))

        # ── 3. TODAY'S PERFORMANCE SUMMARY ──────────────────────────────────
        t_hdr = tk.Frame(self, bg=APP_BACKGROUND)
        t_hdr.pack(fill=tk.X, padx=22, pady=(4, 2))
        tk.Label(
            t_hdr,
            text=f"☀️  Today's Calculations ({today_date:%d %b %Y})",
            font=("Segoe UI", 10, "bold"), bg=APP_BACKGROUND, fg="#0F172A",
        ).pack(side=tk.LEFT)

        card_row_today = tk.Frame(self, bg=APP_BACKGROUND)
        card_row_today.pack(fill=tk.X, padx=20, pady=(2, 4))

        card_row_general = tk.Frame(self, bg=APP_BACKGROUND)
        card_row_general.pack(fill=tk.X, padx=20, pady=(4, 6))

        # Today KPIs
        today_metrics = [
            ("💵 Cash in Counter (Today)", f"₹ {today_data['net_cash_in_counter']:,.2f}", "#D97706", "expenses"),
            ("📱 UPI Received (Today)",   f"₹ {today_data['upi_sales']:,.2f}",          "#0284C7", "reports"),
            ("💸 Expenses (Today)",      f"₹ {today_data['total_expenses']:,.2f}",      "#DC2626", "expenses"),
            ("📊 Sales (This Month)",     f"₹ {monthly_sales:,.2f}",                     "#0D9488", "reports"),
        ]

        for i, (label, value, color, nav_target) in enumerate(today_metrics):
            card = tk.Frame(card_row_today, bg=color, bd=0, relief=tk.FLAT, padx=14, pady=10, cursor="hand2")
            card.grid(row=0, column=i, padx=5, sticky="ew")
            card_row_today.columnconfigure(i, weight=1)

            lbl1 = tk.Label(card, text=label, font=SMALL_FONT, bg=color, fg="white", cursor="hand2")
            lbl1.pack(anchor=tk.W)
            lbl2 = tk.Label(card, text=value, font=("Segoe UI", 15, "bold"), bg=color, fg="white", cursor="hand2")
            lbl2.pack(anchor=tk.W, pady=(2, 0))

            for widget in (card, lbl1, lbl2):
                widget.bind("<Button-1>", lambda e, target=nav_target: self._nav(target))

        # General KPIs
        general_metrics = [
            ("💰 Total Revenue (All Time)", f"₹ {total_sales:,.2f}", PRIMARY_COLOR, "reports"),
            ("📋 Bills Today",             str(bills_today),         SUCCESS_COLOR, "pos_sale"),
            ("📦 Active Items",            str(items_count),         WARNING_COLOR, "inventory"),
            ("👥 Customers",               str(cust_count),          "#7B1FA2",     "customers"),
        ]

        for i, (label, value, color, nav_target) in enumerate(general_metrics):
            card = tk.Frame(card_row_general, bg=color, bd=0, relief=tk.FLAT, padx=14, pady=10, cursor="hand2")
            card.grid(row=0, column=i, padx=5, sticky="ew")
            card_row_general.columnconfigure(i, weight=1)

            lbl1 = tk.Label(card, text=label, font=SMALL_FONT, bg=color, fg="white", cursor="hand2")
            lbl1.pack(anchor=tk.W)
            lbl2 = tk.Label(card, text=value, font=("Segoe UI", 15, "bold"), bg=color, fg="white", cursor="hand2")
            lbl2.pack(anchor=tk.W, pady=(2, 0))

            for widget in (card, lbl1, lbl2):
                widget.bind("<Button-1>", lambda e, target=nav_target: self._nav(target))

        # ── 4. Recent Bills Header & Action Buttons ─────────────────────────
        hdr_box = tk.Frame(self, bg=APP_BACKGROUND)
        hdr_box.pack(fill=tk.X, padx=22, pady=(8, 4))

        tk.Label(
            hdr_box, text="Recent Bills",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(side=tk.LEFT)

        tk.Label(
            hdr_box, text="💡 Tip: Select a row or double-click to perform actions",
            font=SMALL_FONT, bg=APP_BACKGROUND, fg="#64748B",
        ).pack(side=tk.LEFT, padx=(12, 0))

        # Recent Bills Action Buttons Bar Right
        tbl_actions = tk.Frame(hdr_box, bg=APP_BACKGROUND)
        tbl_actions.pack(side=tk.RIGHT)

        _make_button(tbl_actions, "👁️ View & Print Bill", "#0284C7", "white", self._open_selected_bill, pady=4, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(tbl_actions, "🚫 Cancel Bill", "#DC2626", "white", self._cancel_selected_bill, pady=4, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(tbl_actions, "📥 Export CSV", "#059669", "white", self._export_recent_bills_csv, pady=4, padx=10).pack(side=tk.LEFT, padx=3)
        _make_button(tbl_actions, "🔄 Refresh List", "#475569", "white", self._reload_bills_table, pady=4, padx=10).pack(side=tk.LEFT, padx=3)

        # ── 5. Recent Bills Treeview Table ──────────────────────────────────
        tbl_frame = tk.Frame(self, bg=APP_BACKGROUND)
        tbl_frame.pack(fill=tk.BOTH, expand=True, padx=20, pady=(0, 10))

        cols = [
            ("bill_no",   130, "Bill No"),
            ("date",      120, "Date"),
            ("customer",  160, "Customer"),
            ("amount",    100, "Amount (₹)"),
            ("mode",       90, "Payment"),
            ("status",     90, "Status"),
        ]
        self._tree = create_table(tbl_frame, cols, height=10)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        vsb = ttk.Scrollbar(tbl_frame, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        self._reload_bills_table()

        self._tree.bind("<Double-1>", lambda e: self._open_selected_bill())
        self._tree.bind("<Return>", lambda e: self._open_selected_bill())

    def _reload_bills_table(self) -> None:
        """Reload recent bills data into treeview."""
        if not self._tree:
            return
        for item in self._tree.get_children():
            self._tree.delete(item)

        for bill in dao.get_all_bills(limit=50):
            tag = "cancelled" if bill.status == "CANCELLED" else "normal"
            self._tree.insert("", tk.END, values=(
                bill.bill_number,
                bill.bill_date.strftime("%Y-%m-%d %H:%M"),
                bill.customer_name or "—",
                f"{bill.total_amount:,.2f}",
                bill.payment_mode or "—",
                bill.status,
            ), tags=(tag,))

        self._tree.tag_configure("cancelled", foreground="#94A3B8")

    def _get_selected_bill_number(self) -> Optional[str]:
        """Return the bill number of currently selected row in table."""
        if not self._tree:
            return None
        sel = self._tree.selection()
        if not sel:
            return None
        vals = self._tree.item(sel[0], "values")
        if vals and len(vals) > 0:
            return str(vals[0])
        return None

    def _open_selected_bill(self) -> None:
        """Action handler to view & print full receipt of selected bill."""
        bill_no = self._get_selected_bill_number()
        if not bill_no:
            messagebox.showwarning("Select Bill", "Please select a bill from the table first.", parent=self)
            return
        bill = dao.get_bill_by_number(bill_no)
        if bill:
            from ..app import _ReceiptDlg
            _ReceiptDlg(self, bill)
        else:
            messagebox.showerror("Error", f"Bill '{bill_no}' not found.", parent=self)

    def _cancel_selected_bill(self) -> None:
        """Action handler to cancel/void selected bill."""
        bill_no = self._get_selected_bill_number()
        if not bill_no:
            messagebox.showwarning("Select Bill", "Please select a bill to cancel.", parent=self)
            return

        bill = dao.get_bill_by_number(bill_no)
        if not bill:
            messagebox.showerror("Error", f"Bill '{bill_no}' not found.", parent=self)
            return

        if bill.status == "CANCELLED":
            messagebox.showinfo("Already Cancelled", f"Bill '{bill_no}' is already cancelled.", parent=self)
            return

        if messagebox.askyesno("Confirm Cancellation", f"Are you sure you want to cancel bill '{bill_no}'?\nThis will update the bill status to CANCELLED.", parent=self):
            success = dao.update_bill_status(bill_no, "CANCELLED")
            if success:
                messagebox.showinfo("Success", f"Bill '{bill_no}' has been cancelled.", parent=self)
                self._build()  # Refresh metrics & bills list
            else:
                messagebox.showerror("Error", f"Failed to cancel bill '{bill_no}'.", parent=self)

    def _export_recent_bills_csv(self) -> None:
        """Action handler to export recent bills list to a CSV file."""
        if not self._tree:
            return
        path = filedialog.asksaveasfilename(
            defaultextension=".csv",
            filetypes=[("CSV files", "*.csv")],
            title="Export Recent Bills to CSV",
            initialfile="recent_bills.csv"
        )
        if not path:
            return
        try:
            with open(path, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow(["Bill No", "Date", "Customer", "Amount (₹)", "Payment Mode", "Status"])
                for item_id in self._tree.get_children():
                    writer.writerow(self._tree.item(item_id, "values"))
            messagebox.showinfo("Exported", f"Recent bills exported successfully to:\n{path}", parent=self)
        except Exception as exc:
            messagebox.showerror("Export Error", str(exc), parent=self)


class ShopClosingDialog(tk.Toplevel):
    """Interactive modal window for Shop Closing & Day-End Cash Handover Reconciliation."""

    def __init__(self, parent: tk.Widget, user: User, on_complete: Optional[Callable[[], None]] = None) -> None:
        super().__init__(parent)
        self.title("🔒 Shop Closing & Day-End Cash Reconciliation")
        self.geometry("560x650")
        self.resizable(False, False)
        self.configure(bg="#F8FAFC")
        self.user = user
        self.on_complete = on_complete

        # Center on parent
        self.update_idletasks()
        try:
            pw, ph = parent.winfo_width(), parent.winfo_height()
            px, py = parent.winfo_rootx(), parent.winfo_rooty()
            x = max(0, px + (pw - 560) // 2)
            y = max(0, py + (ph - 650) // 2)
            self.geometry(f"560x650+{x}+{y}")
        except Exception:
            pass

        self._today_str = datetime.now().strftime("%Y-%m-%d")
        self._yesterday_str = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")

        self._today_data = dao.get_daily_sales_by_payment_mode(self._today_str)
        self._yesterday_data = dao.get_daily_sales_by_payment_mode(self._yesterday_str)

        self._build()
        self.grab_set()
        self.transient(parent)

    def _build(self) -> None:
        # Header banner
        hdr = tk.Frame(self, bg="#4F46E5", padx=20, pady=16)
        hdr.pack(fill=tk.X)
        tk.Label(
            hdr, text="🔒  Shop Closing & Day-End Handover",
            font=("Segoe UI", 16, "bold"), fg="white", bg="#4F46E5",
        ).pack(anchor="w")
        tk.Label(
            hdr, text=f"Date: {datetime.now():%A, %d %B %Y} | User: {self.user.full_name}",
            font=("Segoe UI", 9), fg="#E0E7FF", bg="#4F46E5",
        ).pack(anchor="w", pady=(2, 0))

        body = tk.Frame(self, bg="#F8FAFC", padx=20, pady=14)
        body.pack(fill=tk.BOTH, expand=True)

        # Comparison Section
        comp_box = tk.LabelFrame(body, text=" 📊 Calculations Summary (Yesterday vs Today) ", font=("Segoe UI", 10, "bold"), bg="#F8FAFC", fg="#0F172A", padx=12, pady=10)
        comp_box.pack(fill=tk.X, pady=(0, 12))

        # Grid comparison
        tk.Label(comp_box, text="Metric", font=("Segoe UI", 9, "bold"), bg="#F8FAFC", fg="#64748B").grid(row=0, column=0, sticky="w", pady=3)
        tk.Label(comp_box, text=f"Yesterday ({self._yesterday_str})", font=("Segoe UI", 9, "bold"), bg="#F8FAFC", fg="#B45309").grid(row=0, column=1, sticky="e", padx=12, pady=3)
        tk.Label(comp_box, text=f"Today ({self._today_str})", font=("Segoe UI", 9, "bold"), bg="#F8FAFC", fg="#0D9488").grid(row=0, column=2, sticky="e", pady=3)

        rows = [
            ("Gross Cash Sales", f"₹ {self._yesterday_data['cash_sales']:,.2f}", f"₹ {self._today_data['cash_sales']:,.2f}"),
            ("UPI / Online Received", f"₹ {self._yesterday_data['upi_sales']:,.2f}", f"₹ {self._today_data['upi_sales']:,.2f}"),
            ("Total Shop Expenses", f"₹ {self._yesterday_data['cash_expenses'] + self._yesterday_data['upi_expenses']:,.2f}", f"₹ {self._today_data['cash_expenses'] + self._today_data['upi_expenses']:,.2f}"),
            ("Net Cash in Counter", f"₹ {self._yesterday_data['net_cash_in_counter']:,.2f}", f"₹ {self._today_data['net_cash_in_counter']:,.2f}"),
        ]

        for idx, (label, y_val, t_val) in enumerate(rows, start=1):
            tk.Label(comp_box, text=label, font=("Segoe UI", 9), bg="#F8FAFC", fg="#334155").grid(row=idx, column=0, sticky="w", pady=2)
            tk.Label(comp_box, text=y_val, font=("Segoe UI", 9, "bold"), bg="#F8FAFC", fg="#475569").grid(row=idx, column=1, sticky="e", padx=12, pady=2)
            tk.Label(comp_box, text=t_val, font=("Segoe UI", 9, "bold"), bg="#F8FAFC", fg="#0F172A").grid(row=idx, column=2, sticky="e", pady=2)

        # Cash Reconciliation Section
        rec_box = tk.LabelFrame(body, text=" 💵 Cash Counter Tally & Verification ", font=("Segoe UI", 10, "bold"), bg="#F8FAFC", fg="#0F172A", padx=12, pady=10)
        rec_box.pack(fill=tk.X, pady=(0, 12))

        expected_cash = self._today_data['net_cash_in_counter']

        tk.Label(rec_box, text="System Expected Cash in Counter:", font=("Segoe UI", 9), bg="#F8FAFC").grid(row=0, column=0, sticky="w", pady=4)
        tk.Label(rec_box, text=f"₹ {expected_cash:,.2f}", font=("Segoe UI", 11, "bold"), bg="#F8FAFC", fg="#D97706").grid(row=0, column=1, sticky="e", pady=4)

        tk.Label(rec_box, text="Physical Cash Count in Drawer (₹):", font=("Segoe UI", 9, "bold"), bg="#F8FAFC").grid(row=1, column=0, sticky="w", pady=4)
        self._cash_entry = tk.Entry(rec_box, font=("Segoe UI", 10, "bold"), width=15, justify="right")
        self._cash_entry.insert(0, f"{expected_cash:.2f}")
        self._cash_entry.grid(row=1, column=1, sticky="e", pady=4)

        tk.Label(rec_box, text="Cash Variance (Difference):", font=("Segoe UI", 9), bg="#F8FAFC").grid(row=2, column=0, sticky="w", pady=4)
        self._var_lbl = tk.Label(rec_box, text="₹ 0.00 (Matched)", font=("Segoe UI", 10, "bold"), bg="#F8FAFC", fg="#16A34A")
        self._var_lbl.grid(row=2, column=1, sticky="e", pady=4)

        self._cash_entry.bind("<KeyRelease>", self._update_variance)

        # Closing Notes
        tk.Label(body, text="Shop Closing Notes / Handover Remarks:", font=("Segoe UI", 9, "bold"), bg="#F8FAFC", fg="#334155").pack(anchor="w", pady=(4, 2))
        self._notes_entry = tk.Entry(body, font=("Segoe UI", 10), width=50)
        self._notes_entry.insert(0, "All counter cash verified and shop closed safely.")
        self._notes_entry.pack(fill=tk.X, pady=(0, 14))

        # Bottom Buttons
        btn_box = tk.Frame(body, bg="#F8FAFC")
        btn_box.pack(fill=tk.X)

        save_btn = tk.Button(
            btn_box, text="💾 Save & Export Closing Report", font=("Segoe UI", 10, "bold"),
            bg="#4F46E5", fg="white", activebackground="#4338CA", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", pady=8, command=self._save_report
        )
        save_btn.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 6))

        cancel_btn = tk.Button(
            btn_box, text="Cancel", font=("Segoe UI", 10),
            bg="#E2E8F0", fg="#0F172A", activebackground="#CBD5E1",
            relief=tk.FLAT, cursor="hand2", pady=8, command=self.destroy
        )
        cancel_btn.pack(side=tk.RIGHT, padx=(6, 0))

    def _update_variance(self, event=None) -> None:
        try:
            val = float(self._cash_entry.get().strip() or 0)
            diff = val - self._today_data['net_cash_in_counter']
            if abs(diff) < 0.01:
                self._var_lbl.config(text="₹ 0.00 (Matched)", fg="#16A34A")
            elif diff > 0:
                self._var_lbl.config(text=f"+ ₹ {diff:,.2f} (Excess Cash)", fg="#0284C7")
            else:
                self._var_lbl.config(text=f"- ₹ {abs(diff):,.2f} (Shortage)", fg="#DC2626")
        except ValueError:
            self._var_lbl.config(text="Invalid Cash Value", fg="#DC2626")

    def _save_report(self) -> None:
        filename = f"day_closing_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"
        filepath = EXPORTS_DIR / filename
        try:
            with open(filepath, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow(["BEREEZE FOOTWEAR - SHOP CLOSING & DAY END REPORT"])
                writer.writerow(["Date", datetime.now().strftime("%Y-%m-%d %H:%M:%S")])
                writer.writerow(["Closed By", self.user.full_name])
                writer.writerow([])
                writer.writerow(["METRIC", f"YESTERDAY ({self._yesterday_str})", f"TODAY ({self._today_str})"])
                writer.writerow(["Gross Cash Sales", f"{self._yesterday_data['cash_sales']:.2f}", f"{self._today_data['cash_sales']:.2f}"])
                writer.writerow(["UPI / Online Sales", f"{self._yesterday_data['upi_sales']:.2f}", f"{self._today_data['upi_sales']:.2f}"])
                writer.writerow(["Total Expenses", f"{self._yesterday_data['total_expenses']:.2f}", f"{self._today_data['total_expenses']:.2f}"])
                writer.writerow(["Net Cash in Counter", f"{self._yesterday_data['net_cash_in_counter']:.2f}", f"{self._today_data['net_cash_in_counter']:.2f}"])
                writer.writerow([])
                writer.writerow(["CASH TALLY VERIFICATION"])
                writer.writerow(["Physical Cash Counted", self._cash_entry.get()])
                writer.writerow(["Variance", self._var_lbl.cget("text")])
                writer.writerow(["Closing Remarks", self._notes_entry.get()])

            messagebox.showinfo(
                "Day Closing Complete",
                f"Shop Closing Report successfully saved and exported to:\n\n{filepath}",
                parent=self
            )
            if self.on_complete:
                self.on_complete()
            self.destroy()
        except Exception as exc:
            messagebox.showerror("Export Error", f"Could not save closing report: {exc}", parent=self)
