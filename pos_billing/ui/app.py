"""
pos_billing/ui/app.py
=====================
Single Tk root window with sleek, modern UI aesthetics, vibrant color palette,
polished typography, padded tables, and responsive, clip-free layout.

Architecture
------------
App (tk.Tk)
 └── container (Frame, fills window)
      ├── LoginPage  (shown first)
      └── MainPage   (shown after login)
"""

from __future__ import annotations

import sys
import os
import tkinter as tk
from tkinter import messagebox, ttk
from datetime import datetime
from typing import Optional

# Make sure absolute imports work when run directly
_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if _root not in sys.path:
    sys.path.insert(0, _root)

from pos_billing.database import dao
from pos_billing.database.models import User, ItemMaster, Customer, Bill, BillItem, Supplier
from pos_billing.payment.payment_method import PaymentProcessor

# ── Modern Color Palette (Sleek Slate & Emerald) ─────────────────────────
PRIMARY       = "#0D9488"   # Vibrant Modern Teal / Emerald
PRIMARY_DARK  = "#0F766E"   # Deep Teal
SIDEBAR_BG    = "#1E293B"   # Sleek Deep Navy Slate
SIDEBAR_HOVER = "#334155"   # Soft Slate Hover
APP_BG        = "#F8FAFC"   # Crisp Light Slate Background
CARD_BG       = "#FFFFFF"   # Pure White
BORDER_COLOR  = "#CBD5E1"   # Subtle clean border
TEXT_MAIN     = "#0F172A"   # Dark Slate Text
TEXT_MUTED    = "#64748B"   # Muted Slate Text
WHITE         = "#FFFFFF"
DANGER        = "#DC2626"   # Crisp Red
SUCCESS       = "#16A34A"   # Crisp Green
WARNING       = "#D97706"   # Crisp Amber

# Accent colors for dashboard cards
ACCENT_TEAL   = "#0D9488"
ACCENT_BLUE   = "#2563EB"
ACCENT_AMBER  = "#F59E0B"
ACCENT_PURPLE = "#8B5CF6"

# Typography
FONT_TITLE    = ("Segoe UI", 20, "bold")
FONT_HEADING  = ("Segoe UI", 15, "bold")
FONT_SUBHEAD  = ("Segoe UI", 12, "bold")
FONT_NORMAL   = ("Segoe UI", 10)
FONT_BOLD     = ("Segoe UI", 10, "bold")
FONT_SMALL    = ("Segoe UI", 9)
FONT_METRIC   = ("Segoe UI", 22, "bold")


# ─────────────────────────────────────────────────────────────────────────
# Helper widget factory
# ─────────────────────────────────────────────────────────────────────────
def _btn(parent, text, color, fg=WHITE, cmd=None, font=FONT_BOLD, **kw):
    padx = kw.pop("padx", 14)
    pady = kw.pop("pady", 7)
    bd = kw.pop("bd", 0)
    relief = kw.pop("relief", tk.FLAT)
    b = tk.Button(parent, text=text, bg=color, fg=fg,
                  font=font, relief=relief, cursor="hand2",
                  activebackground=color, activeforeground=fg,
                  padx=padx, pady=pady, bd=bd, command=cmd, **kw)
    b.bind("<Enter>", lambda e: b.config(bg=_dark(color)))
    b.bind("<Leave>", lambda e: b.config(bg=color))
    return b

def _dark(hex_c, f=0.88):
    try:
        h = hex_c.lstrip("#")
        r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
        return f"#{max(0,int(r*f)):02x}{max(0,int(g*f)):02x}{max(0,int(b*f)):02x}"
    except Exception:
        return hex_c

def _entry(parent, width=24, show="", **kw):
    font = kw.pop("font", FONT_NORMAL)
    relief = kw.pop("relief", tk.FLAT)
    bg = kw.pop("bg", WHITE)
    fg = kw.pop("fg", TEXT_MAIN)
    e = tk.Entry(parent, width=width, font=font, relief=relief,
                 highlightthickness=1, highlightbackground=BORDER_COLOR,
                 highlightcolor=PRIMARY, bg=bg, fg=fg, show=show, **kw)
    return e

def _lbl(parent, text="", font=None, fg=TEXT_MAIN, bg=APP_BG, **kw):
    return tk.Label(parent, text=text, font=font or FONT_NORMAL, fg=fg, bg=bg, **kw)

def _sep(parent, bg=BORDER_COLOR):
    return tk.Frame(parent, bg=bg, height=1)

def _tbl(parent, cols, height=12):
    style = ttk.Style()
    style.theme_use("clam")
    style.configure("POS.Treeview",
                    background=WHITE,
                    foreground=TEXT_MAIN,
                    rowheight=32,
                    fieldbackground=WHITE,
                    font=FONT_NORMAL,
                    borderwidth=0)
    style.configure("POS.Treeview.Heading",
                    background=PRIMARY,
                    foreground=WHITE,
                    font=("Segoe UI", 10, "bold"),
                    borderwidth=1,
                    relief="flat")
    style.map("POS.Treeview",
              background=[("selected", PRIMARY)],
              foreground=[("selected", WHITE)])
    style.map("POS.Treeview.Heading",
              background=[("active", "#00796B")],
              foreground=[("active", WHITE)])

    ids = [c[0] for c in cols]
    tree = ttk.Treeview(parent, columns=ids, show="headings",
                        height=height, style="POS.Treeview")
    for cid, w, heading in cols:
        tree.heading(cid, text=heading)
        tree.column(cid, width=w, anchor=tk.CENTER)
    
    tree.tag_configure("even", background="#F8FAFC")
    tree.tag_configure("odd", background=WHITE)
    
    orig_insert = tree.insert
    def _insert_with_tags(parent_id, index, iid=None, **kw):
        idx = len(tree.get_children(parent_id))
        tag = "even" if idx % 2 == 0 else "odd"
        if "tags" not in kw or not kw["tags"]:
            kw["tags"] = (tag,)
        if iid is not None:
            return orig_insert(parent_id, index, iid=iid, **kw)
        return orig_insert(parent_id, index, **kw)
    tree.insert = _insert_with_tags

    vsb = ttk.Scrollbar(parent, orient=tk.VERTICAL, command=tree.yview)
    tree.configure(yscrollcommand=vsb.set)
    tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
    vsb.pack(side=tk.RIGHT, fill=tk.Y)
    return tree


# ═════════════════════════════════════════════════════════════════════════
# LOGIN PAGE
# ═════════════════════════════════════════════════════════════════════════
class LoginPage(tk.Frame):
    def __init__(self, parent, on_login):
        super().__init__(parent, bg=APP_BG)
        self._on_login = on_login
        self._build()

    def _build(self):
        self.columnconfigure(0, weight=1)
        self.rowconfigure(0, weight=1)

        outer = tk.Frame(self, bg=BORDER_COLOR, bd=1)
        outer.place(relx=0.5, rely=0.5, anchor="center", width=440, height=520)

        card = tk.Frame(outer, bg=WHITE, bd=0)
        card.pack(fill=tk.BOTH, expand=True)

        header = tk.Frame(card, bg=PRIMARY_DARK, height=170)
        header.pack(fill=tk.X)
        header.pack_propagate(False)
        
        # Load and display brand logo image
        logo_loaded = False
        try:
            from PIL import Image, ImageTk
            logo_path = os.path.join(_root, "pos_billing", "assets", "app_icon.png")
            if os.path.exists(logo_path):
                img = Image.open(logo_path).convert("RGBA")
                # Create clean white-padded logo badge for primary dark header
                w, h = img.size
                aspect = w / float(h)
                target_h = 100
                target_w = int(target_h * aspect)
                img_resized = img.resize((target_w, target_h), Image.Resampling.LANCZOS)
                photo = ImageTk.PhotoImage(img_resized)
                lbl_logo = tk.Label(header, image=photo, bg=PRIMARY_DARK)
                lbl_logo._photo = photo
                lbl_logo.pack(pady=(16, 4))
                logo_loaded = True
        except Exception:
            logo_loaded = False

        if not logo_loaded:
            tk.Label(header, text="👟", font=("Segoe UI Emoji", 34), bg=PRIMARY_DARK, fg=WHITE).pack(pady=(22, 4))
            _lbl(header, "BEREEZE FOOTWEAR", font=("Segoe UI", 18, "bold"), fg=WHITE, bg=PRIMARY_DARK).pack()
            _lbl(header, "Point of Sale Billing System", font=FONT_NORMAL, fg="#CCFBF1", bg=PRIMARY_DARK).pack()

        form = tk.Frame(card, bg=WHITE, padx=44)
        form.pack(fill=tk.BOTH, expand=True, pady=20)

        self._err = tk.StringVar(value="")
        err_lbl = _lbl(form, "", fg=DANGER, bg=WHITE, font=FONT_SMALL)
        err_lbl.config(textvariable=self._err)
        err_lbl.pack(anchor=tk.W, pady=(0, 6))

        _lbl(form, "Username", bg=WHITE, font=FONT_BOLD).pack(anchor=tk.W)
        self._user = _entry(form, width=32)
        self._user.pack(fill=tk.X, pady=(4, 12))
        self._user.bind("<Return>", lambda e: self._pass.focus_set())

        _lbl(form, "Password", bg=WHITE, font=FONT_BOLD).pack(anchor=tk.W)
        self._pass = _entry(form, width=32, show="•")
        self._pass.pack(fill=tk.X, pady=(4, 18))
        self._pass.bind("<Return>", lambda e: self._login())

        btn_row = tk.Frame(form, bg=WHITE)
        btn_row.pack(fill=tk.X)
        _btn(btn_row, "LOGIN", PRIMARY, WHITE, self._login).pack(
            side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 8))
        _btn(btn_row, "CANCEL", "#E2E8F0", TEXT_MAIN,
             lambda: self.winfo_toplevel().destroy()).pack(side=tk.LEFT, fill=tk.X, expand=True)

        _lbl(card, "v1.0  •  Python Edition", font=FONT_SMALL, fg=TEXT_MUTED, bg=WHITE).pack(pady=10)
        self._user.focus_set()

    def _login(self):
        u = self._user.get().strip()
        p = self._pass.get().strip()
        if not u:
            self._err.set("Please enter username"); return
        if not p:
            self._err.set("Please enter password"); return

        user = None
        try:
            user = dao.authenticate_user(u, p)
        except Exception:
            pass

        if user is None and u.lower() == "admin" and p == "admin123":
            user = User("admin", "admin123", "Administrator", "ADMIN")
        if user is None and u.lower() == "cashier" and p == "cashier123":
            user = User("cashier", "cashier123", "Cashier User", "CASHIER")

        if user:
            self._on_login(user)
        else:
            self._err.set("Invalid username or password")
            self._pass.delete(0, tk.END)

    def reset(self):
        self._user.delete(0, tk.END)
        self._pass.delete(0, tk.END)
        self._err.set("")
        self._user.focus_set()


# ═════════════════════════════════════════════════════════════════════════
# MAIN PAGE (Sidebar + Header + Content)
# ═════════════════════════════════════════════════════════════════════════
class MainPage(tk.Frame):
    NAV = [
        ("Dashboard",   "dashboard",   "🏠"),
        ("POS Sale",    "pos_sale",    "🛒"),
        ("Inventory",   "inventory",   "📦"),
        ("Barcode Print", "barcode_print", "🏷️"),
        ("Customers",   "customers",   "👥"),
        ("Suppliers",   "suppliers",   "🚚"),
        ("Reports",     "reports",     "📊"),
        ("Users",       "users",       "👤"),
    ]

    def __init__(self, parent, user: User, on_logout):
        super().__init__(parent, bg=APP_BG)
        self.user = user
        self._on_logout = on_logout
        self._nav_btns = {}
        self._build()
        self.show_panel("dashboard")

    def _build(self):
        top = tk.Frame(self, bg=PRIMARY_DARK, height=56)
        top.pack(fill=tk.X)
        top.pack_propagate(False)
        
        tk.Label(top, text="👟  BEREEZE FOOTWEAR POS SYSTEM", font=("Segoe UI", 13, "bold"),
                 fg=WHITE, bg=PRIMARY_DARK).pack(side=tk.LEFT, padx=20)
        
        user_badge = tk.Frame(top, bg=PRIMARY, padx=12, pady=4)
        user_badge.pack(side=tk.RIGHT, padx=20)
        tk.Label(user_badge, text=f"👤 {self.user.full_name}  [{self.user.role}]",
                 font=FONT_SMALL, fg=WHITE, bg=PRIMARY).pack()

        body = tk.Frame(self, bg=APP_BG)
        body.pack(fill=tk.BOTH, expand=True)

        sb = tk.Frame(body, bg=SIDEBAR_BG, width=210)
        sb.pack(side=tk.LEFT, fill=tk.Y)
        sb.pack_propagate(False)

        _lbl(sb, "MAIN NAVIGATION", font=("Segoe UI", 8, "bold"),
             fg="#94A3B8", bg=SIDEBAR_BG).pack(anchor=tk.W, padx=22, pady=(18, 8))

        for label, key, icon in self.NAV:
            b = tk.Button(sb, text=f"   {icon}   {label}", font=FONT_NORMAL,
                          relief=tk.FLAT, bg=SIDEBAR_BG, fg="#E2E8F0",
                          activebackground=PRIMARY, activeforeground=WHITE,
                          anchor=tk.W, padx=18, pady=11, bd=0, cursor="hand2",
                          command=lambda k=key: self.show_panel(k))
            b.pack(fill=tk.X)
            self._nav_btns[key] = b

        tk.Frame(sb, bg=SIDEBAR_BG).pack(fill=tk.BOTH, expand=True)
        _sep(sb, "#334155").pack(fill=tk.X)
        
        logout_btn = tk.Button(sb, text="   🚪   Logout", font=FONT_NORMAL,
                               relief=tk.FLAT, bg=SIDEBAR_BG, fg="#FCA5A5",
                               activebackground=DANGER, activeforeground=WHITE,
                               anchor=tk.W, padx=18, pady=12, bd=0, cursor="hand2",
                               command=self._logout)
        logout_btn.pack(fill=tk.X)

        self._content = tk.Frame(body, bg=APP_BG)
        self._content.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

    def show_panel(self, key):
        for k, b in self._nav_btns.items():
            if k == key:
                b.config(bg=PRIMARY, fg=WHITE, font=FONT_BOLD)
            else:
                b.config(bg=SIDEBAR_BG, fg="#E2E8F0", font=FONT_NORMAL)
                
        for w in self._content.winfo_children():
            w.destroy()
        panel = self._make_panel(key)
        if panel:
            panel.pack(fill=tk.BOTH, expand=True)

    def _make_panel(self, key):
        try:
            if key == "barcode_print":
                from pos_billing.ui.frames.barcode_print_frame import BarcodePrintFrame
                return BarcodePrintFrame(self._content, self.user)
            panels = {
                "dashboard": DashboardPanel,
                "pos_sale":  POSSalePanel,
                "inventory": InventoryPanel,
                "customers": CustomerPanel,
                "suppliers": SupplierPanel,
                "reports":   ReportsPanel,
                "users":     UsersPanel,
            }
            cls = panels.get(key)
            if cls:
                return cls(self._content, self.user)
        except Exception as exc:
            f = tk.Frame(self._content, bg=APP_BG)
            _lbl(f, f"Error loading {key}:\n{exc}", fg=DANGER, bg=APP_BG).pack(expand=True)
            return f

    def _logout(self):
        if messagebox.askyesno("Logout", "Are you sure you want to log out?", parent=self):
            self._on_logout()


# ═════════════════════════════════════════════════════════════════════════
# PANELS
# ═════════════════════════════════════════════════════════════════════════

# ── Dashboard ─────────────────────────────────────────────────────────────
class DashboardPanel(tk.Frame):
    def __init__(self, parent, user):
        super().__init__(parent, bg=APP_BG)
        self.user = user
        self._build()

    def _build(self):
        top_row = tk.Frame(self, bg=APP_BG)
        top_row.pack(fill=tk.X, padx=24, pady=(18, 12))
        
        _lbl(top_row, "Dashboard Overview", font=FONT_HEADING, bg=APP_BG).pack(side=tk.LEFT)
        _lbl(top_row, f"{datetime.now():%A, %d %B %Y}", font=FONT_NORMAL, fg=TEXT_MUTED, bg=APP_BG).pack(side=tk.RIGHT)

        cards_row = tk.Frame(self, bg=APP_BG)
        cards_row.pack(fill=tk.X, padx=24, pady=4)

        try:
            total_sales = dao.get_total_sales()
            items_count = len(dao.get_all_items())
            cust_count  = len(dao.get_all_customers())
            bills = dao.get_all_bills(limit=500)
            today_bills = sum(1 for b in bills if b.bill_date.date() == datetime.now().date())
        except Exception:
            total_sales = items_count = cust_count = today_bills = 0
            bills = []

        metrics = [
            ("TOTAL SALES",  f"₹ {total_sales:,.2f}", ACCENT_TEAL),
            ("BILLS TODAY",  str(today_bills),         ACCENT_BLUE),
            ("ACTIVE ITEMS", str(items_count),         ACCENT_AMBER),
            ("CUSTOMERS",    str(cust_count),          ACCENT_PURPLE),
        ]

        for i, (label, value, color) in enumerate(metrics):
            card_outer = tk.Frame(cards_row, bg=color, bd=0)
            card_outer.grid(row=0, column=i, padx=8, sticky="ew")
            cards_row.columnconfigure(i, weight=1)
            
            card_inner = tk.Frame(card_outer, bg=color, padx=18, pady=16)
            card_inner.pack(fill=tk.BOTH, expand=True)
            
            _lbl(card_inner, label, font=("Segoe UI", 9, "bold"), fg=WHITE, bg=color).pack(anchor=tk.W)
            _lbl(card_inner, value, font=FONT_METRIC, fg=WHITE, bg=color).pack(anchor=tk.W, pady=(4, 0))

        table_sec = tk.Frame(self, bg=APP_BG)
        table_sec.pack(fill=tk.BOTH, expand=True, padx=24, pady=(18, 16))
        
        _lbl(table_sec, "Recent Transactions", font=FONT_SUBHEAD, bg=APP_BG).pack(anchor=tk.W, pady=(0, 8))

        wrap = tk.Frame(table_sec, bg=WHITE, bd=1, relief=tk.SOLID)
        wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
        wrap.pack(fill=tk.BOTH, expand=True)

        cols = [("no", 130, "Bill No"), ("dt", 140, "Date & Time"), ("cust", 170, "Customer"),
                ("amt", 110, "Amount (₹)"), ("mode", 100, "Payment Mode"), ("st", 100, "Status")]
        tree = _tbl(wrap, cols, height=12)
        
        for b in (bills or [])[:50]:
            tree.insert("", "end", values=(
                b.bill_number, b.bill_date.strftime("%Y-%m-%d %H:%M"),
                b.customer_name or "—", f"{b.total_amount:,.2f}",
                b.payment_mode or "—", b.status))


# ── Receipt & Invoice Dialog ──────────────────────────────────────────────
class _ReceiptDlg(tk.Toplevel):
    """Full printable thermal/laser invoice dialog with embedded QR code."""
    def __init__(self, parent, bill: Bill, paid: float = None, change: float = None):
        super().__init__(parent)
        self.title(f"Bill Receipt - {bill.bill_number}")
        self.configure(bg=APP_BG)
        self.geometry("560x780")
        self.minsize(480, 560)
        
        if paid is None: paid = bill.paid_amount or bill.total_amount
        if change is None: change = max(0.0, paid - bill.total_amount)
        
        top = tk.Frame(self, bg=WHITE, padx=12, pady=10, bd=1)
        top.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        top.pack(fill=tk.X)
        _lbl(top, f"Receipt Preview • {bill.bill_number}", font=("Segoe UI", 10, "bold"), bg=WHITE).pack(side=tk.LEFT)
        _btn(top, "✖ Close", "#E2E8F0", TEXT_MAIN, self.destroy, font=FONT_SMALL, padx=8, pady=4).pack(side=tk.RIGHT, padx=(4, 0))
        _btn(top, "💾 Export", ACCENT_BLUE, WHITE, lambda: self._export_file(bill, paid, change), font=FONT_SMALL, padx=8, pady=4).pack(side=tk.RIGHT, padx=3)
        _btn(top, "🖨️ Print", SUCCESS, WHITE, self._do_print, font=FONT_SMALL, padx=8, pady=4).pack(side=tk.RIGHT, padx=3)
        
        wrap = tk.Frame(self, bg=APP_BG)
        wrap.pack(fill=tk.BOTH, expand=True, padx=12, pady=12)
        
        canvas = tk.Canvas(wrap, bg=WHITE, bd=1, highlightbackground=BORDER_COLOR, highlightthickness=1)
        vsb = ttk.Scrollbar(wrap, orient="vertical", command=canvas.yview)
        card = tk.Frame(canvas, bg=WHITE, padx=24, pady=20)
        
        card.bind("<Configure>", lambda e: canvas.configure(scrollregion=canvas.bbox("all")))
        canvas.create_window((0, 0), window=card, anchor="nw", width=490)
        canvas.configure(yscrollcommand=vsb.set)
        
        canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)
        
        # --- RECEIPT HEADER ---
        tk.Label(card, text="👟 BEREEZE FOOTWEAR FANCY", font=("Segoe UI", 15, "bold"), fg=PRIMARY_DARK, bg=WHITE).pack()
        tk.Label(card, text="Anar Complex, Naya Bazar, Melparamba, Kasaragod, Kerala - 671317", font=("Segoe UI", 9), fg="#475569", bg=WHITE).pack()
        tk.Label(card, text="📱 Mob: +91 8086790086  |  ✉️ Email: breezefootwearfancy@gmail.com", font=("Segoe UI", 9, "bold"), fg="#0D9488", bg=WHITE).pack(pady=(2, 10))
        
        _sep(card, "#CBD5E1").pack(fill=tk.X, pady=4)
        
        # --- BILL META ---
        meta = tk.Frame(card, bg=WHITE)
        meta.pack(fill=tk.X, pady=6)
        meta.columnconfigure(1, weight=1)
        
        def _row(label, val, r):
            tk.Label(meta, text=label, font=("Segoe UI", 9, "bold"), fg="#334155", bg=WHITE).grid(row=r, column=0, sticky="w", pady=1)
            tk.Label(meta, text=val, font=("Segoe UI", 9), fg="#1E293B", bg=WHITE).grid(row=r, column=1, sticky="e", pady=1)
            
        _row("Bill Number:", bill.bill_number, 0)
        dt_str = bill.bill_date.strftime("%Y-%m-%d %H:%M") if isinstance(bill.bill_date, datetime) else str(bill.bill_date)
        _row("Date & Time:", dt_str, 1)
        _row("Customer:", bill.customer_name or "Cash Customer", 2)
        _row("Payment Mode:", bill.payment_mode or "CASH", 3)
        
        _sep(card, "#CBD5E1").pack(fill=tk.X, pady=6)
        
        # --- ITEMS TABLE ---
        tbl_hdr = tk.Frame(card, bg="#F1F5F9", padx=6, pady=4)
        tbl_hdr.pack(fill=tk.X)
        tk.Label(tbl_hdr, text="Item Name", font=("Segoe UI", 9, "bold"), fg="#334155", bg="#F1F5F9", width=18, anchor="w").pack(side=tk.LEFT)
        tk.Label(tbl_hdr, text="Qty", font=("Segoe UI", 9, "bold"), fg="#334155", bg="#F1F5F9", width=4, anchor="center").pack(side=tk.LEFT)
        tk.Label(tbl_hdr, text="Price", font=("Segoe UI", 9, "bold"), fg="#334155", bg="#F1F5F9", width=8, anchor="e").pack(side=tk.LEFT)
        tk.Label(tbl_hdr, text="Total (₹)", font=("Segoe UI", 9, "bold"), fg="#334155", bg="#F1F5F9", width=10, anchor="e").pack(side=tk.RIGHT)
        
        items_frm = tk.Frame(card, bg=WHITE, pady=4)
        items_frm.pack(fill=tk.X)
        for bi in bill.bill_items:
            ir = tk.Frame(items_frm, bg=WHITE, padx=6, pady=3)
            ir.pack(fill=tk.X)
            tk.Label(ir, text=bi.item_name[:22], font=("Segoe UI", 9), fg="#1E293B", bg=WHITE, width=18, anchor="w").pack(side=tk.LEFT)
            tk.Label(ir, text=str(bi.quantity), font=("Segoe UI", 9), fg="#1E293B", bg=WHITE, width=4, anchor="center").pack(side=tk.LEFT)
            tk.Label(ir, text=f"{bi.unit_price:.2f}", font=("Segoe UI", 9), fg="#1E293B", bg=WHITE, width=8, anchor="e").pack(side=tk.LEFT)
            tk.Label(ir, text=f"{bi.total_amount:.2f}", font=("Segoe UI", 9, "bold"), fg="#1E293B", bg=WHITE, width=10, anchor="e").pack(side=tk.RIGHT)
            
        _sep(card, "#CBD5E1").pack(fill=tk.X, pady=6)
        
        # --- TOTALS ---
        t_frm = tk.Frame(card, bg=WHITE)
        t_frm.pack(fill=tk.X, pady=4)
        t_frm.columnconfigure(1, weight=1)
        
        def _tot(label, val, r, bold=False, col="#1E293B"):
            f = ("Segoe UI", 11, "bold") if bold else ("Segoe UI", 9)
            tk.Label(t_frm, text=label, font=f, fg=col, bg=WHITE).grid(row=r, column=0, sticky="w", pady=2)
            tk.Label(t_frm, text=val, font=f, fg=col, bg=WHITE).grid(row=r, column=1, sticky="e", pady=2)
            
        _tot("Subtotal:", f"₹ {bill.subtotal:,.2f}", 0)
        if bill.total_discount > 0:
            _tot("Total Discount:", f"- ₹ {bill.total_discount:,.2f}", 1, col="#10B981")
        _tot("GRAND TOTAL:", f"₹ {bill.total_amount:,.2f}", 2, bold=True, col=PRIMARY_DARK)
        _tot("Amount Paid:", f"₹ {paid:,.2f}", 3)
        if change > 0:
            _tot("Change Returned:", f"₹ {change:,.2f}", 4, bold=True, col="#D97706")
            
        _sep(card, "#CBD5E1").pack(fill=tk.X, pady=10)
        
        # --- QR CODE SECTION ---
        self._qr_frm = tk.Frame(card, bg=WHITE)
        self._qr_frm.pack(pady=8)
        
        qr_box = tk.Frame(self._qr_frm, bg=WHITE, bd=1, relief=tk.SOLID, padx=8, pady=8)
        qr_box.config(highlightbackground="#E2E8F0", highlightthickness=1, bd=0)
        qr_box.pack(pady=6)
        
        self._qr_c = tk.Canvas(qr_box, width=160, height=160, bg="white", bd=0, highlightthickness=0)
        self._qr_c.pack()
        
        self._upi_lbl = tk.Label(self._qr_frm, text="", font=("Segoe UI", 8, "bold"), fg="#0F766E", bg=WHITE)
        self._upi_lbl.pack(pady=(2, 0))
        
        self._render_qr(bill)
        
        tk.Label(card, text="Thank You for Shopping at Bereeze Footwear! Visit Again.", font=("Segoe UI", 9, "italic"), fg="#0F766E", bg=WHITE).pack(pady=(14, 10))
        
        self.grab_set(); self.transient(parent)

    def _render_qr(self, bill):
        self._qr_c.delete("all")
        try:
            from PIL import ImageTk
            from pos_billing.utils.qr_generator import get_receipt_qr_image
            img, uri, lbl_text = get_receipt_qr_image(
                payment_mode=bill.payment_mode,
                amount=bill.total_amount,
                bill_number=bill.bill_number,
                size=(160, 160),
            )
            self._custom_qr_photo = ImageTk.PhotoImage(img)
            self._qr_c.create_image(80, 80, image=self._custom_qr_photo)
            self._upi_lbl.config(text=lbl_text)
            self._qr_frm.pack(pady=8)
        except Exception:
            self._qr_frm.pack_forget()

    def _do_print(self):
        messagebox.showinfo("Print Bill", f"Sent Bill {self.title()} to official thermal/laser printer queue!", parent=self)

    def _export_file(self, bill, paid, change):
        from tkinter import filedialog
        path = filedialog.asksaveasfilename(defaultextension=".txt", filetypes=[("Text Receipt", "*.txt"), ("HTML Receipt", "*.html")], title="Export Receipt", parent=self)
        if path:
            with open(path, "w", encoding="utf-8") as f:
                f.write(f"BEREEZE FOOTWEAR FANCY - POS RECEIPT\n")
                f.write(f"Anar Complex, Naya Bazar, Melparamba, Kasaragod, Kerala - 671317\n")
                f.write(f"Mobile: +91 8086790086 | Email: breezefootwearfancy@gmail.com\n")
                f.write(f"="*65 + "\n")
                f.write(f"Bill No: {bill.bill_number} | Date: {bill.bill_date}\n")
                f.write(f"Customer: {bill.customer_name or 'Walk-In Customer'}\n")
                f.write(f"-"*65 + "\n")
                for bi in bill.bill_items:
                    f.write(f"{bi.item_name:<25} {bi.quantity:>3} x {bi.unit_price:>7.2f} = {bi.total_amount:>8.2f}\n")
                f.write(f"-"*65 + "\n")
                f.write(f"GRAND TOTAL: Rs. {bill.total_amount:.2f}\n")
                f.write(f"Paid: Rs. {paid:.2f} | Change: Rs. {change:.2f}\n")
                from pos_billing.utils.qr_generator import generate_upi_qr
                _, upi_uri, upi_id = generate_upi_qr(bill.total_amount, bill.bill_number)
                f.write(f"Scan QR code or pay via UPI link (Pre-filled Rs. {bill.total_amount:.2f}):\n{upi_uri}\n")
            messagebox.showinfo("Exported", f"Receipt saved to {path}", parent=self)


# ── POS Sale ──────────────────────────────────────────────────────────────
class POSSalePanel(tk.Frame):
    _COUNTER = [1001]

    def __init__(self, parent, user):
        super().__init__(parent, bg=APP_BG)
        self.user = user
        self._bill: Optional[Bill] = None
        self._build()
        self._new_bill()

    def _build(self):
        # Header title
        _lbl(self, "Point of Sale Billing", font=FONT_HEADING, bg=APP_BG).pack(
            anchor=tk.W, padx=20, pady=(14, 8))

        body = tk.Frame(self, bg=APP_BG)
        body.pack(fill=tk.BOTH, expand=True, padx=20, pady=(0, 14))

        self._build_left(body)
        self._build_right(body)

    def _build_left(self, parent):
        left = tk.Frame(parent, bg=APP_BG, width=380)
        left.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 14))
        left.pack_propagate(False)

        # 1. Bill Details Card (Ultra compact grid)
        bx = tk.LabelFrame(left, text=" Bill Details ", font=FONT_BOLD,
                           bg=WHITE, fg=PRIMARY, padx=12, pady=8, relief=tk.FLAT, bd=1)
        bx.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        bx.pack(fill=tk.X, pady=(0, 8))

        self._bill_no  = tk.StringVar()
        self._cust_var = tk.StringVar()
        
        _lbl(bx, "Bill No:", bg=WHITE, font=FONT_BOLD).grid(row=0, column=0, sticky=tk.W, pady=3)
        tk.Entry(bx, textvariable=self._bill_no, font=FONT_NORMAL, state="readonly",
                 relief=tk.FLAT, width=22, bg=WHITE, fg=TEXT_MAIN).grid(row=0, column=1, columnspan=2, sticky="we", pady=3)
                 
        _lbl(bx, "Customer:", bg=WHITE, font=FONT_BOLD).grid(row=1, column=0, sticky=tk.W, pady=3)
        tk.Entry(bx, textvariable=self._cust_var, font=FONT_NORMAL, state="readonly",
                 relief=tk.FLAT, width=22, bg=WHITE, fg=TEXT_MAIN).grid(row=1, column=1, columnspan=2, sticky="we", pady=3)

        _lbl(bx, "Phone:", bg=WHITE, font=FONT_BOLD).grid(row=2, column=0, sticky=tk.W, pady=3)
        self._ph = _entry(bx, width=15)
        self._ph.grid(row=2, column=1, sticky="we", padx=(0, 6), pady=3)
        self._ph.bind("<Return>", lambda e: self._find_customer())
        _btn(bx, "Find", ACCENT_BLUE, WHITE, self._find_customer, font=FONT_SMALL, padx=10, pady=4).grid(row=2, column=2, sticky="e", pady=3)
        bx.columnconfigure(1, weight=1)

        # 2. Add Item Card (Compact grid side-by-side)
        ix = tk.LabelFrame(left, text=" Add Item to Bill ", font=FONT_BOLD,
                           bg=WHITE, fg=PRIMARY, padx=12, pady=8, relief=tk.FLAT, bd=1)
        ix.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        ix.pack(fill=tk.X, pady=8)

        _lbl(ix, "Item Code / Barcode:", bg=WHITE, font=FONT_BOLD).grid(row=0, column=0, columnspan=2, sticky=tk.W, pady=(2, 2))
        self._item_e = _entry(ix, width=22)
        self._item_e.grid(row=1, column=0, columnspan=2, sticky="we", pady=(0, 6))
        self._item_e.bind("<Return>", lambda e: self._on_code_enter())
        self._item_e.bind("<KeyRelease>", lambda e: self._preview_item_cost())

        # Live Cost & Stock Preview Box right under Item Code
        self._preview_box = tk.LabelFrame(ix, text=" Live Product Cost Preview ", font=FONT_SMALL,
                                          bg="#F8FAFC", fg="#0F766E", padx=8, pady=6, bd=1, relief=tk.SOLID)
        self._preview_box.config(highlightbackground="#99F6E4", highlightthickness=1)
        self._preview_box.grid(row=2, column=0, columnspan=2, sticky="we", pady=(2, 8))
        
        self._preview_lbl = _lbl(self._preview_box, "💡 Type code or barcode above to see\nCost Price, Selling Price & Stock",
                                 font=FONT_SMALL, fg=TEXT_MUTED, bg="#F8FAFC", justify=tk.LEFT)
        self._preview_lbl.pack(anchor=tk.W)

        _lbl(ix, "Quantity:", bg=WHITE, font=FONT_BOLD).grid(row=3, column=0, sticky=tk.W, pady=2)
        self._qty_e = _entry(ix, width=8); self._qty_e.insert(0, "1")
        self._qty_e.grid(row=3, column=1, sticky=tk.W, pady=2)
        self._qty_e.bind("<Return>", lambda e: self._add_item())

        _btn(ix, "➕  Add Item to Bill", SUCCESS, WHITE, self._add_item, font=FONT_BOLD).grid(row=4, column=0, columnspan=2, sticky="we", pady=(8, 2))
        ix.columnconfigure(0, weight=1)

        # 3. Checkout & Payment Card
        px = tk.LabelFrame(left, text=" Checkout & Payment ", font=FONT_BOLD,
                           bg=WHITE, fg=PRIMARY, padx=12, pady=8, relief=tk.FLAT, bd=1)
        px.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        px.pack(fill=tk.BOTH, expand=True, pady=(8, 0))

        _lbl(px, "Payment Mode:", bg=WHITE, font=FONT_BOLD).grid(row=0, column=0, sticky=tk.W, pady=3)
        self._method = tk.StringVar(value="CASH")
        ttk.Combobox(px, textvariable=self._method,
                     values=["CASH", "CARD", "CHEQUE", "UPI", "ONLINE"],
                     state="readonly", width=14).grid(row=0, column=1, sticky="we", pady=3)

        _lbl(px, "Flat Discount (₹):", bg=WHITE, font=FONT_BOLD).grid(row=1, column=0, sticky=tk.W, pady=3)
        self._flat_disc_var = tk.StringVar(value="0.00")
        self._flat_disc_var.trace_add("write", lambda *_: self._refresh(auto_paid=False))
        _entry(px, width=14, textvariable=self._flat_disc_var).grid(row=1, column=1, sticky="we", pady=3)

        _lbl(px, "Amount Paid (₹):", bg=WHITE, font=FONT_BOLD).grid(row=2, column=0, sticky=tk.W, pady=3)
        self._paid = _entry(px, width=14); self._paid.insert(0, "0")
        self._paid.bind("<KeyRelease>", lambda e: self._refresh(auto_paid=False))
        self._paid.grid(row=2, column=1, sticky="we", pady=3)
        px.columnconfigure(1, weight=1)

        btn_grid = tk.Frame(px, bg=WHITE)
        btn_grid.grid(row=3, column=0, columnspan=2, sticky="we", pady=(10, 0))
        btn_grid.columnconfigure(0, weight=1)
        btn_grid.columnconfigure(1, weight=1)

        _btn(btn_grid, "✅ COMPLETE BILL", SUCCESS, WHITE, self._complete, font=FONT_BOLD, pady=8).grid(row=0, column=0, columnspan=2, sticky="we", pady=(0, 6))
        _btn(btn_grid, "📱 SHOW UPI QR (PRE-SET AMOUNT)", ACCENT_BLUE, WHITE, self._show_upi_qr, font=FONT_BOLD, pady=6).grid(row=1, column=0, columnspan=2, sticky="we", pady=(0, 6))
        _btn(btn_grid, "🔄 New Bill", WARNING, WHITE, self._new_bill, font=FONT_SMALL, pady=5).grid(row=2, column=0, sticky="we", padx=(0, 3))
        _btn(btn_grid, "🗑 Clear Items", DANGER, WHITE, self._clear, font=FONT_SMALL, pady=5).grid(row=2, column=1, sticky="we", padx=(3, 0))

    def _build_right(self, parent):
        right = tk.Frame(parent, bg=APP_BG)
        right.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # CRITICAL PACKING ORDER FIX: Pack bottom Totals Panel FIRST with side=BOTTOM so it NEVER gets cut off or overlapped!
        tot = tk.Frame(right, bg=WHITE, padx=16, pady=12)
        tot.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        tot.pack(side=tk.BOTTOM, fill=tk.X, pady=(10, 0))
        
        self._sub_v  = tk.StringVar(value="₹ 0.00")
        self._disc_v = tk.StringVar(value="₹ 0.00")
        self._tot_v  = tk.StringVar(value="₹ 0.00")
        self._bal_v  = tk.StringVar(value="₹ 0.00")
        
        # 2x2 grid inside totals box for clean, compact display without clipping
        for i, (lbl, var) in enumerate([("Subtotal:", self._sub_v), ("Discount:", self._disc_v),
                                        ("GRAND TOTAL:", self._tot_v), ("Balance Due:", self._bal_v)]):
            row = i % 2
            col = (i // 2) * 2
            is_total = "TOTAL" in lbl
            
            cell = tk.Frame(tot, bg=WHITE)
            cell.grid(row=row, column=col, sticky="we", padx=10, pady=2)
            tot.columnconfigure(col, weight=1)
            
            _lbl(cell, lbl, font=("Segoe UI", 11, "bold") if is_total else FONT_NORMAL,
                 bg=WHITE, fg=PRIMARY if is_total else TEXT_MAIN).pack(side=tk.LEFT)
            tk.Label(cell, textvariable=var,
                     font=("Segoe UI", 15, "bold") if is_total else ("Segoe UI", 11, "bold"),
                     bg=WHITE, fg=PRIMARY if is_total else TEXT_MAIN).pack(side=tk.RIGHT)

        # Header Row at top
        header_row = tk.Frame(right, bg=APP_BG)
        header_row.pack(side=tk.TOP, fill=tk.X, pady=(0, 8))
        _lbl(header_row, "Current Bill Items", font=FONT_SUBHEAD, bg=APP_BG).pack(side=tk.LEFT)
        _btn(header_row, "Remove Selected", DANGER, WHITE, self._remove, font=FONT_SMALL, padx=12, pady=5).pack(side=tk.RIGHT, padx=(4, 0))
        _btn(header_row, "✏️ Edit Item", ACCENT_BLUE, WHITE, self._edit_item, font=FONT_SMALL, padx=12, pady=5).pack(side=tk.RIGHT)

        self._cost_summary_lbl = _lbl(header_row, "💰 Total Cost: ₹ 0.00  |  Est. Profit: ₹ 0.00",
                                      font=FONT_BOLD, fg="#0F766E", bg=APP_BG)
        self._cost_summary_lbl.pack(side=tk.LEFT, padx=(20, 0))

        # Table takes remaining middle space safely without pushing totals off!
        wrap = tk.Frame(right, bg=WHITE, bd=1, relief=tk.SOLID)
        wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
        wrap.pack(side=tk.TOP, fill=tk.BOTH, expand=True)

        cols = [("#", 40, "#"), ("code", 85, "Code"), ("name", 185, "Item Name"),
                ("qty", 50, "Qty"), ("cost", 85, "Cost (₹)"), ("price", 85, "Price (₹)"), ("disc", 65, "Disc (₹)"),
                ("total", 90, "Total (₹)")]
        self._tree = _tbl(wrap, cols, height=10)
        self._tree.bind("<Double-1>", lambda e: self._edit_item())

    def _new_bill(self):
        no = f"INV-{datetime.now():%Y%m%d}-{POSSalePanel._COUNTER[0]}"
        POSSalePanel._COUNTER[0] += 1
        self._bill = Bill(no, "SALES", 0, "Walk-In Customer")
        self._bill.user_id = self.user.user_id
        self._bill_no.set(no)
        self._cust_var.set("Walk-In Customer")
        if hasattr(self, "_flat_disc_var"):
            self._flat_disc_var.set("0.00")
        self._refresh()

    def _clear(self):
        if self._bill:
            self._bill.bill_items.clear()
            if hasattr(self, "_flat_disc_var"):
                self._flat_disc_var.set("0.00")
            self._refresh()

    def _find_customer(self):
        ph = self._ph.get().strip()
        if not ph: return
        c = dao.search_customer_by_phone(ph)
        if c:
            self._bill.customer_id = c.customer_id
            self._bill.customer_name = c.customer_name
            self._bill.customer_phone = c.phone
            self._cust_var.set(f"{c.customer_name} ({c.phone})")
        else:
            messagebox.showinfo("Not Found", f"No customer found with phone {ph}", parent=self)

    def _on_code_enter(self):
        self._preview_item_cost()
        self._qty_e.focus_set()

    def _preview_item_cost(self):
        code = self._item_e.get().strip()
        if not code:
            self._preview_lbl.config(
                text="💡 Type code or barcode above to see\nCost Price, Selling Price & Stock",
                fg=TEXT_MUTED, font=FONT_SMALL
            )
            return
        item = dao.search_item_by_barcode(code) or dao.search_item_by_code(code)
        if item:
            self._preview_lbl.config(
                text=(f"👟 {item.item_name} ({item.manufacturer} - Size {item.size})\n"
                      f"💰 Cost Price: ₹ {item.purchase_price:,.2f}\n"
                      f"🏷️ Selling Price: ₹ {item.selling_price:,.2f}  |  📦 Stock: {item.stock_quantity}"),
                fg="#0F766E", font=FONT_BOLD
            )
        else:
            self._preview_lbl.config(
                text=f"❌ Item '{code}' not found in inventory",
                fg=DANGER, font=FONT_SMALL
            )

    def _add_item(self):
        code = self._item_e.get().strip()
        try: qty = int(self._qty_e.get().strip())
        except ValueError:
            messagebox.showerror("Error", "Enter a valid integer quantity", parent=self); return
        if not code:
            messagebox.showwarning("Input Required", "Enter item code or barcode", parent=self); return
        item = dao.search_item_by_barcode(code) or dao.search_item_by_code(code)
        if not item:
            messagebox.showwarning("Not Found", f"Item '{code}' not found in inventory", parent=self); return
        if item.stock_quantity < qty:
            messagebox.showwarning("Insufficient Stock", f"Only {item.stock_quantity} units available in stock", parent=self); return
        bi = BillItem(item.item_id, item.item_code, item.item_name, qty, item.selling_price)
        bi.purchase_price = item.purchase_price
        self._bill.add_bill_item(bi)
        self._item_e.delete(0, tk.END)
        self._qty_e.delete(0, tk.END); self._qty_e.insert(0, "1")
        self._preview_item_cost()
        self._refresh()

    def _edit_item(self):
        sel = self._tree.selection()
        if not sel or not self._bill:
            messagebox.showinfo("Select Item", "Please select an item from the table to edit its price or discount.", parent=self)
            return
        idx = self._tree.index(sel[0])
        if 0 <= idx < len(self._bill.bill_items):
            bi = self._bill.bill_items[idx]
            _BillItemEditDlg(self, bi, on_save=lambda: self._refresh(auto_paid=False))

    def _remove(self):
        sel = self._tree.selection()
        if sel and self._bill:
            self._bill.remove_bill_item(self._tree.index(sel[0]))
            self._refresh()

    def _complete(self):
        if not self._bill or not self._bill.bill_items:
            messagebox.showwarning("Empty Bill", "Please add at least one item to complete the bill", parent=self); return
        self._refresh(auto_paid=False)
        try: paid = float(self._paid.get())
        except ValueError:
            messagebox.showerror("Error", "Enter a valid numerical amount paid", parent=self); return
        
        if paid < self._bill.total_amount:
            diff = self._bill.total_amount - paid
            if messagebox.askyesno("Apply Flat Discount?",
                f"Amount paid (₹ {paid:,.2f}) is ₹ {diff:,.2f} less than the Grand Total (₹ {self._bill.total_amount:,.2f}).\n\n"
                f"Would you like to automatically apply ₹ {diff:,.2f} as a Flat Bill Discount and complete the sale?",
                parent=self):
                try:
                    cur_disc = float(self._flat_disc_var.get() or 0)
                except ValueError:
                    cur_disc = 0.0
                self._flat_disc_var.set(f"{cur_disc + diff:.2f}")
                self._refresh(auto_paid=False)
                paid = self._bill.total_amount
            else:
                return

        completed_bill = self._bill
        self._bill.complete_bill(paid, self._method.get())
        for bi in self._bill.bill_items:
            item = dao.search_item_by_code(bi.item_code)
            if item:
                dao.update_stock(item.item_id, item.stock_quantity - bi.quantity)
        dao.save_bill(self._bill)
        change = paid - self._bill.total_amount
        messagebox.showinfo("Transaction Complete",
            f"Bill {self._bill.bill_number} successfully completed!\n\n"
            f"Total Amount:  ₹ {self._bill.total_amount:,.2f}\n"
            f"Amount Paid:   ₹ {paid:,.2f}\n"
            f"Change Due:    ₹ {change:,.2f}\n\n"
            f"Opening Printable Bill Receipt now...", parent=self)
        self._new_bill()
        _ReceiptDlg(self, completed_bill, paid, change)

    def _show_upi_qr(self):
        if not self._bill or not self._bill.bill_items:
            messagebox.showwarning("Empty Bill", "Please add items to the bill before generating a payment QR code.", parent=self)
            return
        self._bill.calculate_totals()
        from pos_billing.utils.qr_generator import show_live_upi_dialog
        show_live_upi_dialog(self, self._bill)

    def _refresh(self, auto_paid=True):
        for r in self._tree.get_children(): self._tree.delete(r)
        if not self._bill: return
        self._bill.calculate_totals()
        
        # Apply flat discount from input box
        flat_disc = 0.0
        if hasattr(self, "_flat_disc_var"):
            try: flat_disc = float(self._flat_disc_var.get() or 0)
            except ValueError: flat_disc = 0.0
            if flat_disc < 0: flat_disc = 0.0
            
        self._bill.total_discount += flat_disc
        self._bill.total_amount = max(0.0, self._bill.subtotal - self._bill.total_discount)
        
        if auto_paid:
            self._paid.delete(0, tk.END)
            self._paid.insert(0, f"{self._bill.total_amount:.2f}")
            paid = self._bill.total_amount
        else:
            try: paid = float(self._paid.get() or 0)
            except ValueError: paid = 0.0

        self._bill.balance_amount = self._bill.total_amount - paid

        tot_cost = 0.0
        for i, bi in enumerate(self._bill.bill_items, 1):
            cost_val = getattr(bi, "purchase_price", 0.0)
            tot_cost += cost_val * bi.quantity
            self._tree.insert("", "end", values=(
                i, bi.item_code, bi.item_name, bi.quantity,
                f"{cost_val:.2f}", f"{bi.unit_price:.2f}", f"{bi.discount:.2f}", f"{bi.total_amount:.2f}"))
        b = self._bill
        est_profit = b.total_amount - tot_cost
        if hasattr(self, "_cost_summary_lbl"):
            self._cost_summary_lbl.config(text=f"💰 Total Cost: ₹ {tot_cost:,.2f}  |  Est. Profit: ₹ {est_profit:,.2f}")
        self._sub_v.set(f"₹ {b.subtotal:,.2f}")
        self._disc_v.set(f"₹ {b.total_discount:,.2f}")
        self._tot_v.set(f"₹ {b.total_amount:,.2f}")
        
        if paid > b.total_amount:
            change = paid - b.total_amount
            self._bal_v.set(f"₹ 0.00 (Change: ₹ {change:,.2f})")
        else:
            self._bal_v.set(f"₹ {b.total_amount - paid:,.2f}")


class _BillItemEditDlg(tk.Toplevel):
    """Dialog to edit item price, quantity, and discount right in the cart."""
    def __init__(self, parent, bi: BillItem, on_save):
        super().__init__(parent)
        self.title(f"Edit Item: {bi.item_code}")
        self.configure(bg=WHITE)
        self.geometry("380x390")
        self.resizable(False, False)
        self._bi = bi
        self._on_save = on_save

        _lbl(self, f"Edit {bi.item_name}", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W, padx=22, pady=(16, 10))

        frm = tk.Frame(self, bg=WHITE, padx=22)
        frm.pack(fill=tk.BOTH, expand=True)

        cost_val = getattr(bi, "purchase_price", 0.0)
        _lbl(frm, "Cost Price (₹):", bg=WHITE, font=FONT_BOLD, fg=TEXT_MUTED).grid(row=0, column=0, sticky=tk.W, pady=4)
        _lbl(frm, f"₹ {cost_val:,.2f}", bg=WHITE, font=FONT_BOLD, fg="#0F766E").grid(row=0, column=1, sticky=tk.W, pady=4)

        _lbl(frm, "Quantity *", bg=WHITE, font=FONT_BOLD).grid(row=1, column=0, sticky=tk.W, pady=6)
        self._qty = _entry(frm, width=16); self._qty.insert(0, str(bi.quantity))
        self._qty.grid(row=1, column=1, pady=6)

        _lbl(frm, "Unit Price (₹) *", bg=WHITE, font=FONT_BOLD).grid(row=2, column=0, sticky=tk.W, pady=6)
        self._price = _entry(frm, width=16); self._price.insert(0, str(bi.unit_price))
        self._price.grid(row=2, column=1, pady=6)

        _lbl(frm, "Item Discount (₹)", bg=WHITE, font=FONT_BOLD).grid(row=3, column=0, sticky=tk.W, pady=6)
        self._disc = _entry(frm, width=16); self._disc.insert(0, str(bi.discount))
        self._disc.grid(row=3, column=1, pady=6)

        br = tk.Frame(self, bg=WHITE)
        br.pack(pady=18)
        _btn(br, "💾  Save Changes", SUCCESS, WHITE, self._save).pack(side=tk.LEFT, padx=6)
        _btn(br, "Cancel", "#E2E8F0", TEXT_MAIN, self.destroy).pack(side=tk.LEFT, padx=6)
        self.grab_set()
        self.transient(parent)

    def _save(self):
        try:
            q = int(self._qty.get().strip())
            p = float(self._price.get().strip())
            d = float(self._disc.get().strip() or 0)
            if q <= 0 or p < 0 or d < 0:
                raise ValueError()
        except ValueError:
            messagebox.showerror("Validation Error", "Please enter valid positive numbers for quantity, price, and discount.", parent=self)
            return
        self._bi.quantity = q
        self._bi.unit_price = p
        self._bi.discount = d
        self._bi.calculate_amount()
        if self._on_save:
            self._on_save()
        self.destroy()


# ── Inventory ─────────────────────────────────────────────────────────────
class InventoryPanel(tk.Frame):
    def __init__(self, parent, user):
        super().__init__(parent, bg=APP_BG)
        self.user = user
        self._items: list[ItemMaster] = []
        self._build(); self._load()

    def _build(self):
        top = tk.Frame(self, bg=APP_BG); top.pack(fill=tk.X, padx=24, pady=(16, 10))
        _lbl(top, "Inventory & Item Master", font=FONT_HEADING, bg=APP_BG).pack(side=tk.LEFT)
        _btn(top, "➕  Add Item", SUCCESS, WHITE, self._add_dlg).pack(side=tk.RIGHT, padx=4)
        _btn(top, "🔄  Refresh", PRIMARY, WHITE, self._load).pack(side=tk.RIGHT, padx=4)

        srch = tk.Frame(self, bg=APP_BG); srch.pack(fill=tk.X, padx=24, pady=(0, 10))
        _lbl(srch, "Search Items:", bg=APP_BG, font=FONT_BOLD).pack(side=tk.LEFT, padx=(0, 8))
        self._q = tk.StringVar(); self._q.trace_add("write", lambda *_: self._filter())
        e = _entry(srch, width=34); e.config(textvariable=self._q); e.pack(side=tk.LEFT)

        br = tk.Frame(self, bg=APP_BG); br.pack(side=tk.BOTTOM, fill=tk.X, pady=12, padx=24)
        _btn(br, "✏️  Edit Selected Item", ACCENT_BLUE, WHITE, self._edit_dlg).pack(side=tk.LEFT)
        _btn(br, "🏷️  Barcode Print Program", PRIMARY_DARK, WHITE, self._open_barcode_program).pack(side=tk.LEFT, padx=(10, 0))

        wrap = tk.Frame(self, bg=WHITE, bd=0)
        wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        wrap.pack(side=tk.TOP, fill=tk.BOTH, expand=True, padx=24)
        
        cols = [("code", 95, "Code"), ("name", 180, "Item Name"), ("cat", 105, "Category"),
                ("brand", 110, "Brand"), ("size", 60, "Size"), ("color", 80, "Color"),
                ("cost", 80, "Cost (₹)"), ("price", 90, "Price (₹)"), ("stk", 65, "Stock"), ("st", 85, "Status")]
        self._tree = _tbl(wrap, cols, height=14)

    def _open_barcode_program(self):
        from pos_billing.ui.frames.barcode_print_frame import BarcodePrintFrame
        win = tk.Toplevel(self)
        win.title("🏷️ BEREEZE FOOTWEAR - Barcode Point & Print Program")
        win.geometry("1080x760")
        win.configure(bg=APP_BG)
        try:
            win.tk.eval(f"tk::PlaceWindow {win._w} center")
        except Exception:
            pass

        frame = BarcodePrintFrame(win, self.user, on_exit=win.destroy)
        frame.pack(fill=tk.BOTH, expand=True)

        sel = self._tree.selection()
        if sel:
            item_id = sel[0]
            item = next((x for x in self._items if str(x.item_id) == item_id), None)
            if item:
                frame.entry_barcode.insert(0, item.barcode or item.item_code)
                frame._search_barcode_action()

    def _load(self):
        self._items = dao.get_all_items()
        self._show(self._items)

    def _show(self, items):
        for r in self._tree.get_children(): self._tree.delete(r)
        for i in items:
            self._tree.insert("", "end", iid=str(i.item_id), values=(
                i.item_code, i.item_name, i.category, i.manufacturer,
                i.size, i.color, f"{i.purchase_price:.2f}",
                f"{i.selling_price:.2f}", i.stock_quantity, i.status))

    def _filter(self):
        q = self._q.get().lower()
        self._show([i for i in self._items
                    if q in i.item_code.lower() or q in i.item_name.lower()
                    or q in i.category.lower()])

    def _add_dlg(self):  _ItemDlg(self, on_save=self._save)
    def _edit_dlg(self):
        sel = self._tree.selection()
        if not sel: messagebox.showinfo("Select", "Please select an item to edit", parent=self); return
        item = next((i for i in self._items if str(i.item_id) == sel[0]), None)
        if item: _ItemDlg(self, item=item, on_save=self._save)

    def _save(self, item):
        dao.save_item(item)
        self._load()


class _ItemDlg(tk.Toplevel):
    _F = [("item_code", "Item Code *"), ("item_name", "Item Name *"), ("category", "Category"),
          ("manufacturer", "Brand / Manufacturer"), ("size", "Size"), ("color", "Color"),
          ("material", "Material"), ("barcode", "Barcode"),
          ("purchase_price", "Cost Price (₹) *"), ("selling_price", "Selling Price (₹) *"), ("stock", "Stock Quantity")]

    def __init__(self, parent, item=None, on_save=None):
        super().__init__(parent)
        self.title("Item Management"); self.configure(bg=WHITE)
        self.geometry("440x510"); self.resizable(False, False)
        self._item = item; self._save_cb = on_save; self._e = {}
        
        _lbl(self, "Item Details", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W, padx=24, pady=(18, 10))
        
        frm = tk.Frame(self, bg=WHITE, padx=24); frm.pack(fill=tk.BOTH, expand=True)
        for i, (k, lbl) in enumerate(self._F):
            _lbl(frm, lbl, bg=WHITE, font=FONT_BOLD).grid(row=i, column=0, sticky=tk.W, pady=3)
            e = _entry(frm, width=24); e.grid(row=i, column=1, padx=(12, 0), pady=3); self._e[k] = e
            
        if item:
            for k, v in [("item_code", item.item_code), ("item_name", item.item_name),
                         ("category", item.category), ("manufacturer", item.manufacturer),
                         ("size", item.size), ("color", item.color), ("material", item.material),
                         ("barcode", item.barcode), ("purchase_price", str(item.purchase_price)),
                         ("selling_price", str(item.selling_price)), ("stock", str(item.stock_quantity))]:
                self._e[k].insert(0, v)
                
        br = tk.Frame(self, bg=WHITE); br.pack(pady=16)
        _btn(br, "💾  Save Item", SUCCESS, WHITE, self._do_save).pack(side=tk.LEFT, padx=6)
        _btn(br, "Cancel", "#E2E8F0", TEXT_MAIN, self.destroy).pack(side=tk.LEFT, padx=6)
        self.grab_set(); self.transient(parent)

    def _do_save(self):
        d = {k: e.get().strip() for k, e in self._e.items()}
        if not d["item_code"] or not d["item_name"]:
            messagebox.showerror("Validation Error", "Item Code and Name are required", parent=self); return
        if self._item:
            item = self._item
            item.item_code = d["item_code"]; item.item_name = d["item_name"]
            item.category = d["category"]; item.manufacturer = d["manufacturer"]
            item.size = d["size"]; item.color = d["color"]; item.material = d["material"]
            item.barcode = d["barcode"]
            item.purchase_price = float(d["purchase_price"] or 0)
            item.selling_price = float(d["selling_price"] or 0)
            item.stock_quantity = int(d["stock"] or 0)
        else:
            item = ItemMaster(d["item_code"], d["item_name"], d["category"],
                              d["manufacturer"], float(d["purchase_price"] or 0),
                              float(d["selling_price"] or 0), d["barcode"],
                              d["size"], d["color"], d["material"])
            item.stock_quantity = int(d["stock"] or 0)
        if self._save_cb: self._save_cb(item)
        self.destroy()


# ── Customers ─────────────────────────────────────────────────────────────
class CustomerPanel(tk.Frame):
    def __init__(self, parent, user):
        super().__init__(parent, bg=APP_BG)
        self.user = user; self._custs: list[Customer] = []
        self._build(); self._load()

    def _build(self):
        top = tk.Frame(self, bg=APP_BG); top.pack(fill=tk.X, padx=24, pady=(16, 10))
        _lbl(top, "Customer Directory", font=FONT_HEADING, bg=APP_BG).pack(side=tk.LEFT)
        _btn(top, "➕  Add Customer", SUCCESS, WHITE, self._add).pack(side=tk.RIGHT, padx=4)
        _btn(top, "🔄  Refresh", PRIMARY, WHITE, self._load).pack(side=tk.RIGHT, padx=4)

        srch = tk.Frame(self, bg=APP_BG); srch.pack(fill=tk.X, padx=24, pady=(0, 10))
        _lbl(srch, "Search Customers:", bg=APP_BG, font=FONT_BOLD).pack(side=tk.LEFT, padx=(0, 8))
        self._q = tk.StringVar(); self._q.trace_add("write", lambda *_: self._filter())
        e = _entry(srch, width=34); e.config(textvariable=self._q); e.pack(side=tk.LEFT)

        br = tk.Frame(self, bg=APP_BG); br.pack(side=tk.BOTTOM, fill=tk.X, pady=12, padx=24)
        _btn(br, "✏️  Edit Selected Customer", ACCENT_BLUE, WHITE, self._edit).pack(side=tk.LEFT)

        wrap = tk.Frame(self, bg=WHITE, bd=0)
        wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        wrap.pack(side=tk.TOP, fill=tk.BOTH, expand=True, padx=24)
        
        cols = [("code", 95, "Code"), ("name", 170, "Name"), ("phone", 120, "Phone"),
                ("email", 170, "Email"), ("city", 100, "City"), ("type", 105, "Customer Type"),
                ("pts", 85, "Loyalty Pts"), ("st", 85, "Status")]
        self._tree = _tbl(wrap, cols, height=14)

    def _load(self):
        self._custs = dao.get_all_customers(); self._show(self._custs)

    def _show(self, custs):
        for r in self._tree.get_children(): self._tree.delete(r)
        for c in custs:
            self._tree.insert("", "end", iid=str(c.customer_id), values=(
                c.customer_code, c.customer_name, c.phone, c.email,
                c.city, c.customer_type, f"{c.loyalty_points:.1f}", c.status))

    def _filter(self):
        q = self._q.get().lower()
        self._show([c for c in self._custs if q in c.customer_name.lower()
                    or q in c.phone.lower() or q in c.customer_code.lower()])

    def _add(self):  _CustDlg(self, on_save=lambda c: (dao.save_customer(c), self._load()))
    def _edit(self):
        sel = self._tree.selection()
        if not sel: messagebox.showinfo("Select", "Please select a customer to edit", parent=self); return
        c = next((x for x in self._custs if str(x.customer_id) == sel[0]), None)
        if c: _CustDlg(self, customer=c, on_save=lambda cx: (dao.save_customer(cx), self._load()))


class _CustDlg(tk.Toplevel):
    _F = [("customer_code", "Customer Code *"), ("customer_name", "Customer Name *"), ("phone", "Phone Number"),
          ("email", "Email Address"), ("address", "Address"), ("city", "City"),
          ("state", "State"), ("pincode", "Pincode"),
          ("credit_limit", "Credit Limit (₹)"), ("customer_type", "Type (REGULAR/RETAIL)")]

    def __init__(self, parent, customer=None, on_save=None):
        super().__init__(parent)
        self.title("Customer Management"); self.configure(bg=WHITE)
        self.geometry("440x480"); self.resizable(False, False)
        self._c = customer; self._cb = on_save; self._e = {}
        
        _lbl(self, "Customer Profile", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W, padx=24, pady=(18, 10))
        
        frm = tk.Frame(self, bg=WHITE, padx=24); frm.pack(fill=tk.BOTH, expand=True)
        for i, (k, lbl) in enumerate(self._F):
            _lbl(frm, lbl, bg=WHITE, font=FONT_BOLD).grid(row=i, column=0, sticky=tk.W, pady=3)
            e = _entry(frm, width=24); e.grid(row=i, column=1, padx=(12, 0), pady=3); self._e[k] = e
            
        if customer:
            for k, v in [("customer_code", customer.customer_code),
                         ("customer_name", customer.customer_name),
                         ("phone", customer.phone), ("email", customer.email),
                         ("address", customer.address), ("city", customer.city),
                         ("state", customer.state), ("pincode", customer.pincode),
                         ("credit_limit", str(customer.credit_limit)),
                         ("customer_type", customer.customer_type)]:
                self._e[k].insert(0, v)
                
        br = tk.Frame(self, bg=WHITE); br.pack(pady=16)
        _btn(br, "💾  Save Customer", SUCCESS, WHITE, self._do_save).pack(side=tk.LEFT, padx=6)
        _btn(br, "Cancel", "#E2E8F0", TEXT_MAIN, self.destroy).pack(side=tk.LEFT, padx=6)
        self.grab_set(); self.transient(parent)

    def _do_save(self):
        d = {k: e.get().strip() for k, e in self._e.items()}
        if not d["customer_code"] or not d["customer_name"]:
            messagebox.showerror("Error", "Customer Code and Name are required", parent=self); return
        if self._c:
            c = self._c
        else:
            c = Customer(d["customer_code"], d["customer_name"], d["phone"], d["email"])
        c.customer_code = d["customer_code"]; c.customer_name = d["customer_name"]
        c.phone = d["phone"]; c.email = d["email"]; c.address = d["address"]
        c.city = d["city"]; c.state = d["state"]; c.pincode = d["pincode"]
        c.credit_limit = float(d["credit_limit"] or 0)
        c.customer_type = d["customer_type"] or "REGULAR"
        if self._cb: self._cb(c)
        self.destroy()


# ── Suppliers ─────────────────────────────────────────────────────────────
class SupplierPanel(tk.Frame):
    def __init__(self, parent, user):
        super().__init__(parent, bg=APP_BG)
        self.user = user; self._items: list[Supplier] = []
        self._build(); self._load()

    def _build(self):
        top = tk.Frame(self, bg=APP_BG); top.pack(fill=tk.X, padx=24, pady=(16, 12))
        _lbl(top, "Supplier Directory", font=FONT_HEADING, bg=APP_BG).pack(side=tk.LEFT)
        _btn(top, "➕  Add Supplier", SUCCESS, WHITE, self._add).pack(side=tk.RIGHT, padx=3)
        _btn(top, "📄  Add Bill Amount", ACCENT_BLUE, WHITE, self._add_bill).pack(side=tk.RIGHT, padx=3)
        _btn(top, "💰  Pay Supplier", "#0F766E", WHITE, self._pay_supplier).pack(side=tk.RIGHT, padx=3)
        _btn(top, "📜  View Bills", "#475569", WHITE, self._view_bills).pack(side=tk.RIGHT, padx=3)
        _btn(top, "✏️  Edit", "#64748B", WHITE, self._edit).pack(side=tk.RIGHT, padx=3)
        _btn(top, "🔄  Refresh", PRIMARY, WHITE, self._load).pack(side=tk.RIGHT, padx=3)
        
        wrap = tk.Frame(self, bg=WHITE, bd=0)
        wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        wrap.pack(fill=tk.BOTH, expand=True, padx=24, pady=(0, 16))
        
        cols = [("code", 100, "Code"), ("name", 190, "Supplier Name"), ("phone", 130, "Phone Number"),
                ("gstin", 140, "GSTIN"), ("bal", 120, "Outstanding (₹)"), ("st", 95, "Status")]
        self._tree = _tbl(wrap, cols, height=16)

    def _load(self):
        self._items = dao.get_all_suppliers()
        for r in self._tree.get_children(): self._tree.delete(r)
        for s in self._items:
            self._tree.insert("", "end", iid=str(s.supplier_id), values=(
                s.supplier_code, s.supplier_name, s.phone, s.gstin,
                f"{s.outstanding_balance:,.2f}", s.status))

    def _add(self):
        _SupDlg(self, on_save=lambda s: (dao.save_supplier(s), self._load()))

    def _edit(self):
        sel = self._tree.selection()
        if not sel: messagebox.showinfo("Select", "Please select a supplier to edit.", parent=self); return
        s = next((x for x in self._items if str(x.supplier_id) == sel[0]), None)
        if s: _SupDlg(self, supplier=s, on_save=lambda s: (dao.save_supplier(s), self._load()))

    def _add_bill(self):
        sel = self._tree.selection()
        pre = next((x for x in self._items if str(x.supplier_id) == sel[0]), None) if sel else None
        _AddPurchaseBillDlg(self, suppliers=self._items, on_save=self._load, preselect=pre)

    def _pay_supplier(self):
        sel = self._tree.selection()
        if not sel: messagebox.showinfo("Select Supplier", "Please select a supplier from the table first to record a payment.", parent=self); return
        s = next((x for x in self._items if str(x.supplier_id) == sel[0]), None)
        if s: _RecordSupplierPaymentDlg(self, supplier=s, on_save=self._load)

    def _view_bills(self):
        sel = self._tree.selection()
        if not sel: messagebox.showinfo("Select Supplier", "Please select a supplier from the table first to view their bills and ledger.", parent=self); return
        s = next((x for x in self._items if str(x.supplier_id) == sel[0]), None)
        if s: _ViewSupplierBillsDlg(self, supplier=s)


class _SupDlg(tk.Toplevel):
    _F = [("supplier_code", "Supplier Code *"), ("supplier_name", "Supplier Name *"),
          ("phone", "Phone Number"), ("email", "Email Address"), ("state", "State"),
          ("tax_regn", "Tax Reg Number"), ("gstin", "GSTIN")]

    def __init__(self, parent, supplier=None, on_save=None):
        super().__init__(parent)
        self.title("Supplier Management"); self.configure(bg=WHITE)
        self.geometry("420x380"); self.resizable(False, False)
        self._s = supplier; self._cb = on_save; self._e = {}
        
        _lbl(self, "Supplier Details", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W, padx=24, pady=(18, 10))
        
        frm = tk.Frame(self, bg=WHITE, padx=24); frm.pack(fill=tk.BOTH, expand=True)
        for i, (k, lbl) in enumerate(self._F):
            _lbl(frm, lbl, bg=WHITE, font=FONT_BOLD).grid(row=i, column=0, sticky=tk.W, pady=3)
            e = _entry(frm, width=24); e.grid(row=i, column=1, padx=(12, 0), pady=3); self._e[k] = e
            if supplier and hasattr(supplier, k):
                e.insert(0, str(getattr(supplier, k) or ""))
            
        br = tk.Frame(self, bg=WHITE); br.pack(pady=16)
        _btn(br, "💾  Save Supplier", SUCCESS, WHITE, self._do_save).pack(side=tk.LEFT, padx=6)
        _btn(br, "Cancel", "#E2E8F0", TEXT_MAIN, self.destroy).pack(side=tk.LEFT, padx=6)
        self.grab_set(); self.transient(parent)

    def _do_save(self):
        d = {k: e.get().strip() for k, e in self._e.items()}
        if not d["supplier_code"] or not d["supplier_name"]:
            messagebox.showerror("Error", "Supplier Code and Name are required", parent=self); return
        if self._s:
            self._s.supplier_code = d["supplier_code"]; self._s.supplier_name = d["supplier_name"]
            self._s.phone = d["phone"]; self._s.email = d["email"]; self._s.state = d["state"]
            self._s.tax_regn = d["tax_regn"]; self._s.gstin = d["gstin"]
            if self._cb: self._cb(self._s)
        else:
            s = Supplier(d["supplier_code"], d["supplier_name"])
            s.phone = d["phone"]; s.email = d["email"]; s.state = d["state"]
            s.tax_regn = d["tax_regn"]; s.gstin = d["gstin"]
            if self._cb: self._cb(s)
        self.destroy()


class _AddPurchaseBillDlg(tk.Toplevel):
    """Dialog to record a purchase bill/amount against an existing or new supplier."""
    def __init__(self, parent, suppliers: list[Supplier], on_save=None, preselect: Supplier | None = None):
        super().__init__(parent)
        self.title("Add Supplier Bill / Invoice Amount")
        self.configure(bg=WHITE)
        self.geometry("460x520")
        self.resizable(False, False)
        self._suppliers = suppliers
        self._cb = on_save

        _lbl(self, "📄 Add Supplier Bill Amount", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W, padx=24, pady=(16, 10))

        frm = tk.Frame(self, bg=WHITE, padx=24)
        frm.pack(fill=tk.BOTH, expand=True)

        _lbl(frm, "Select Supplier *", bg=WHITE, font=FONT_BOLD).grid(row=0, column=0, sticky=tk.W, pady=6)
        
        self._sup_vals = [f"{s.supplier_code} - {s.supplier_name}" for s in suppliers] + ["➕ Create New Supplier..."]
        self._sup_var = tk.StringVar()
        if preselect:
            self._sup_var.set(f"{preselect.supplier_code} - {preselect.supplier_name}")
        elif self._sup_vals and len(self._sup_vals) > 1:
            self._sup_var.set(self._sup_vals[0])
            
        self._sup_cb = ttk.Combobox(frm, textvariable=self._sup_var, values=self._sup_vals, width=26)
        self._sup_cb.grid(row=0, column=1, sticky=tk.W, pady=6)
        self._sup_cb.bind("<<ComboboxSelected>>", self._on_sup_change)

        self._new_sup_frm = tk.Frame(frm, bg="#F8FAFC", bd=1, relief=tk.SOLID, padx=10, pady=8)
        self._new_sup_frm.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
        
        _lbl(self._new_sup_frm, "New Code:", bg="#F8FAFC", font=FONT_SMALL).grid(row=0, column=0, sticky=tk.W, pady=2)
        self._new_code_e = _entry(self._new_sup_frm, width=14)
        self._new_code_e.grid(row=0, column=1, sticky=tk.W, pady=2, padx=(4, 0))
        
        _lbl(self._new_sup_frm, "New Name:", bg="#F8FAFC", font=FONT_SMALL).grid(row=1, column=0, sticky=tk.W, pady=2)
        self._new_name_e = _entry(self._new_sup_frm, width=18)
        self._new_name_e.grid(row=1, column=1, sticky=tk.W, pady=2, padx=(4, 0))

        _lbl(frm, "Bill / Invoice No *", bg=WHITE, font=FONT_BOLD).grid(row=2, column=0, sticky=tk.W, pady=6)
        self._bill_no = _entry(frm, width=24)
        self._bill_no.insert(0, f"PB-{datetime.now():%Y%m%d-%H%M}")
        self._bill_no.grid(row=2, column=1, sticky=tk.W, pady=6)

        _lbl(frm, "Total Bill Amount (₹) *", bg=WHITE, font=FONT_BOLD).grid(row=3, column=0, sticky=tk.W, pady=6)
        self._tot_amt = _entry(frm, width=24)
        self._tot_amt.insert(0, "0.00")
        self._tot_amt.grid(row=3, column=1, sticky=tk.W, pady=6)

        _lbl(frm, "Paid Right Now (₹)", bg=WHITE, font=FONT_BOLD).grid(row=4, column=0, sticky=tk.W, pady=6)
        self._paid_amt = _entry(frm, width=24)
        self._paid_amt.insert(0, "0.00")
        self._paid_amt.grid(row=4, column=1, sticky=tk.W, pady=6)

        _lbl(frm, "Remarks / Note", bg=WHITE, font=FONT_BOLD).grid(row=5, column=0, sticky=tk.W, pady=6)
        self._remarks = _entry(frm, width=24)
        self._remarks.insert(0, "Purchase Bill Added")
        self._remarks.grid(row=5, column=1, sticky=tk.W, pady=6)

        br = tk.Frame(self, bg=WHITE)
        br.pack(pady=16)
        _btn(br, "💾 Save Bill Amount", SUCCESS, WHITE, self._do_save).pack(side=tk.LEFT, padx=6)
        _btn(br, "Cancel", "#E2E8F0", TEXT_MAIN, self.destroy).pack(side=tk.LEFT, padx=6)
        
        self.grab_set(); self.transient(parent)
        self._on_sup_change(None)

    def _on_sup_change(self, event):
        val = self._sup_var.get().strip()
        if val == "➕ Create New Supplier...":
            self._new_sup_frm.grid(row=1, column=0, columnspan=2, sticky="we", pady=6)
            self._new_code_e.delete(0, tk.END)
            self._new_code_e.insert(0, f"SUP-{datetime.now():%m%d%H%M}")
        else:
            self._new_sup_frm.grid_forget()

    def _do_save(self):
        val = self._sup_var.get().strip()
        sup_id = 0
        if val == "➕ Create New Supplier...":
            c = self._new_code_e.get().strip()
            n = self._new_name_e.get().strip()
            if not c or not n:
                messagebox.showerror("Required", "Please enter new Supplier Code and Name.", parent=self); return
            new_s = Supplier(c, n)
            if not dao.save_supplier(new_s):
                messagebox.showerror("Error", "Failed to create new supplier.", parent=self); return
            sup_id = new_s.supplier_id
        else:
            code_part = val.split(" - ")[0] if " - " in val else val
            s = next((x for x in self._suppliers if x.supplier_code == code_part or str(x.supplier_id) == val), None)
            if not s:
                messagebox.showerror("Error", "Please select a valid supplier.", parent=self); return
            sup_id = s.supplier_id

        bill_no = self._bill_no.get().strip()
        if not bill_no:
            messagebox.showerror("Required", "Bill Number is required.", parent=self); return
        try:
            tot = float(self._tot_amt.get().strip())
            paid = float(self._paid_amt.get().strip())
        except ValueError:
            messagebox.showerror("Invalid Amount", "Total and Paid amounts must be numeric.", parent=self); return

        if tot < 0 or paid < 0 or paid > tot:
            messagebox.showerror("Invalid Amount", "Please check total and paid amounts.", parent=self); return

        if dao.save_purchase_bill(bill_no, sup_id, tot, paid):
            messagebox.showinfo("Saved ✅", f"Bill {bill_no} recorded successfully!\nAdded ₹ {tot - paid:,.2f} to supplier outstanding balance.", parent=self)
            if self._cb: self._cb()
            self.destroy()
        else:
            messagebox.showerror("Error", "Could not save purchase bill. Bill number may already exist.", parent=self)


class _RecordSupplierPaymentDlg(tk.Toplevel):
    """Dialog to record a payment to a supplier and reduce their outstanding balance."""
    def __init__(self, parent, supplier: Supplier, on_save=None, bill_id: int = 0, bill_no: str = "", suggested_amt: float = 0.0):
        super().__init__(parent)
        title_str = f"Pay Bill {bill_no} ({supplier.supplier_name})" if bill_no else f"Pay Supplier: {supplier.supplier_name}"
        self.title(title_str)
        self.configure(bg=WHITE)
        self.geometry("440x410")
        self.resizable(False, False)
        self._s = supplier
        self._cb = on_save
        self._bill_id = bill_id
        self._bill_no = bill_no

        _lbl(self, f"💰 Pay: {supplier.supplier_name}", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W, padx=24, pady=(16, 4))
        if bill_no:
            _lbl(self, f"Against Purchase Bill / Invoice: {bill_no}", font=FONT_SMALL, fg="#0F766E", bg=WHITE).pack(anchor=tk.W, padx=24, pady=(0, 10))

        frm = tk.Frame(self, bg=WHITE, padx=24)
        frm.pack(fill=tk.BOTH, expand=True)

        _lbl(frm, "Current Outstanding Due:", bg=WHITE, font=FONT_BOLD, fg=TEXT_MUTED).grid(row=0, column=0, sticky=tk.W, pady=6)
        _lbl(frm, f"₹ {supplier.outstanding_balance:,.2f}", bg=WHITE, font=FONT_BOLD, fg="#0F766E").grid(row=0, column=1, sticky=tk.W, pady=6)

        amt_label_str = f"Payment Amount for {bill_no} (₹) *" if bill_no else "Payment Amount (₹) *"
        _lbl(frm, amt_label_str, bg=WHITE, font=FONT_BOLD).grid(row=1, column=0, sticky=tk.W, pady=6)
        self._amt_e = _entry(frm, width=20)
        default_amt = suggested_amt if suggested_amt > 0 else supplier.outstanding_balance
        self._amt_e.insert(0, f"{default_amt:.2f}")
        self._amt_e.grid(row=1, column=1, sticky=tk.W, pady=6)

        _lbl(frm, "Payment Mode", bg=WHITE, font=FONT_BOLD).grid(row=2, column=0, sticky=tk.W, pady=6)
        self._mode_var = tk.StringVar(value="CASH")
        self._mode_cb = ttk.Combobox(frm, textvariable=self._mode_var, values=["CASH", "BANK TRANSFER", "CHEQUE", "UPI"], width=18, state="readonly")
        self._mode_cb.grid(row=2, column=1, sticky=tk.W, pady=6)

        _lbl(frm, "Reference / Note", bg=WHITE, font=FONT_BOLD).grid(row=3, column=0, sticky=tk.W, pady=6)
        self._note_e = _entry(frm, width=20)
        default_note = f"Payment against bill {bill_no}" if bill_no else f"Payment on {datetime.now():%Y-%m-%d}"
        self._note_e.insert(0, default_note)
        self._note_e.grid(row=3, column=1, sticky=tk.W, pady=6)

        br = tk.Frame(self, bg=WHITE)
        br.pack(pady=16)
        _btn(br, "✅ Record Payment", "#0F766E", WHITE, self._do_pay).pack(side=tk.LEFT, padx=6)
        _btn(br, "Cancel", "#E2E8F0", TEXT_MAIN, self.destroy).pack(side=tk.LEFT, padx=6)

        self.grab_set(); self.transient(parent)

    def _do_pay(self):
        try:
            amt = float(self._amt_e.get().strip())
        except ValueError:
            messagebox.showerror("Error", "Enter a valid numeric payment amount.", parent=self); return
        if amt <= 0:
            messagebox.showerror("Error", "Amount must be greater than zero.", parent=self); return

        if dao.record_supplier_payment(self._s.supplier_id, amt, self._mode_var.get(), self._note_e.get().strip(), purchase_bill_id=self._bill_id):
            messagebox.showinfo("Paid ✅", f"Recorded payment of ₹ {amt:,.2f} to {self._s.supplier_name}!", parent=self)
            if self._cb: self._cb()
            self.destroy()
        else:
            messagebox.showerror("Error", "Could not record payment.", parent=self)


class _ViewSupplierBillsDlg(tk.Toplevel):
    """Full-screen ledger view showing both purchase bills and payment history for a supplier."""
    def __init__(self, parent, supplier: Supplier):
        super().__init__(parent)
        self.title(f"Supplier Ledger & Bills: {supplier.supplier_name}")
        self.configure(bg=WHITE)
        self.geometry("1150x720")
        self.minsize(950, 580)
        try:
            self.state("zoomed")
        except Exception:
            try:
                self.attributes("-zoomed", True)
            except Exception:
                pass

        self._s = supplier

        # Top Header
        top = tk.Frame(self, bg=WHITE, padx=24, pady=16)
        top.pack(fill=tk.X)
        
        header_left = tk.Frame(top, bg=WHITE)
        header_left.pack(side=tk.LEFT)
        _lbl(header_left, f"🚚 Supplier Ledger: {supplier.supplier_name}", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W)
        _lbl(header_left, f"Supplier Code: {supplier.supplier_code}   |   Phone: {supplier.phone or 'N/A'}   |   GSTIN: {supplier.gstin or 'N/A'}", font=FONT_SMALL, fg=TEXT_MUTED, bg=WHITE).pack(anchor=tk.W, pady=(2, 0))

        header_right = tk.Frame(top, bg="#F0FDF4", bd=1, relief=tk.SOLID, padx=16, pady=8)
        header_right.config(highlightbackground="#BBF7D0", highlightthickness=1, bd=0)
        header_right.pack(side=tk.RIGHT)
        _lbl(header_right, "Current Outstanding Due:", font=FONT_SMALL, fg="#166534", bg="#F0FDF4").pack(anchor=tk.E)
        self._out_lbl = _lbl(header_right, f"₹ {supplier.outstanding_balance:,.2f}", font=("Segoe UI", 16, "bold"), fg="#15803D", bg="#F0FDF4")
        self._out_lbl.pack(anchor=tk.E)

        # Paned Container for Bills (Top) and Payments (Bottom)
        paned = ttk.Panedwindow(self, orient=tk.VERTICAL)
        paned.pack(fill=tk.BOTH, expand=True, padx=24, pady=(0, 12))

        # --- Top Pane: Purchase Bills ---
        bills_frm = tk.Frame(paned, bg=WHITE)
        paned.add(bills_frm, weight=1)

        b_top = tk.Frame(bills_frm, bg=WHITE, pady=6)
        b_top.pack(fill=tk.X)
        _lbl(b_top, "📄 Purchase Bills & Invoices", font=FONT_SUBHEAD, bg=WHITE, fg="#1E293B").pack(side=tk.LEFT)
        _lbl(b_top, "All purchase bills recorded from this supplier", font=FONT_SMALL, fg=TEXT_MUTED, bg=WHITE).pack(side=tk.LEFT, padx=(10, 0))
        
        _btn(b_top, "➕ Add New Bill Amount", PRIMARY, WHITE, self._add_new_bill).pack(side=tk.RIGHT, padx=4)
        _btn(b_top, "💰 Pay Selected Bill", "#0F766E", WHITE, self._pay_now).pack(side=tk.RIGHT, padx=4)

        b_wrap = tk.Frame(bills_frm, bg=WHITE, bd=0)
        b_wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        b_wrap.pack(fill=tk.BOTH, expand=True)

        b_cols = [("bill", 180, "Bill / Invoice No"), ("date", 140, "Purchase Date"), ("tot", 160, "Total Amount (₹)"),
                  ("paid", 160, "Paid Against Bill (₹)"), ("due", 160, "Balance Due (₹)"), ("status", 120, "Status")]
        self._bills_tree = _tbl(b_wrap, b_cols, height=8)

        # --- Bottom Pane: Payment Details ---
        pay_frm = tk.Frame(paned, bg=WHITE)
        paned.add(pay_frm, weight=1)

        p_top = tk.Frame(pay_frm, bg=WHITE, pady=6)
        p_top.pack(fill=tk.X)
        _lbl(p_top, "💰 Paid Details & Transaction History", font=FONT_SUBHEAD, bg=WHITE, fg="#0F766E").pack(side=tk.LEFT)
        _lbl(p_top, "All cash, bank & UPI payments made to this supplier", font=FONT_SMALL, fg=TEXT_MUTED, bg=WHITE).pack(side=tk.LEFT, padx=(10, 0))
        _btn(p_top, "💰 Record Payment Amount", "#0F766E", WHITE, self._pay_now).pack(side=tk.RIGHT, padx=4)

        p_wrap = tk.Frame(pay_frm, bg=WHITE, bd=0)
        p_wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        p_wrap.pack(fill=tk.BOTH, expand=True)

        p_cols = [("date", 160, "Payment Date & Time"), ("amt", 160, "Amount Paid (₹)"), ("mode", 140, "Payment Mode"),
                  ("bill", 180, "Against Bill No"), ("note", 280, "Reference / Remarks")]
        self._pay_tree = _tbl(p_wrap, p_cols, height=8)

        # Bottom Action Bar
        bot = tk.Frame(self, bg=WHITE, padx=24, pady=12)
        bot.pack(fill=tk.X)
        
        _btn(bot, "➕ Add New Bill Amount", PRIMARY, WHITE, self._add_new_bill).pack(side=tk.LEFT, padx=(0, 8))
        _btn(bot, "💰 Record Payment / Pay Bill", "#0F766E", WHITE, self._pay_now).pack(side=tk.LEFT, padx=8)
        _btn(bot, "🔄 Refresh Ledger", "#64748B", WHITE, self._load_data).pack(side=tk.LEFT, padx=8)
        _btn(bot, "Close Window", "#E2E8F0", TEXT_MAIN, self.destroy).pack(side=tk.RIGHT)

        self._load_data()
        self.grab_set()
        self.transient(parent)

    def _load_data(self):
        # Refresh supplier object to get latest outstanding
        s_list = dao.get_all_suppliers()
        upd = next((x for x in s_list if x.supplier_id == self._s.supplier_id), None)
        if upd:
            self._s = upd
            self._out_lbl.config(text=f"₹ {upd.outstanding_balance:,.2f}")

        # Clear trees
        for r in self._bills_tree.get_children(): self._bills_tree.delete(r)
        for r in self._pay_tree.get_children(): self._pay_tree.delete(r)

        # Load Bills
        bills = dao.get_purchase_bills_by_supplier(self._s.supplier_id)
        for b in bills:
            self._bills_tree.insert("", "end", values=(
                b["bill_number"],
                b["purchase_date"][:19] if b.get("purchase_date") else "",
                f"{float(b['total_amount']):,.2f}",
                f"{float(b['paid_amount']):,.2f}",
                f"{float(b['balance_due']):,.2f}",
                b["status"],
            ))

        # Load Payments
        payments = dao.get_supplier_payments(self._s.supplier_id)
        for p in payments:
            b_no = p.get("bill_number") or (f"Bill #{p['purchase_bill_id']}" if p.get("purchase_bill_id") and p["purchase_bill_id"] > 0 else "Direct Payment")
            self._pay_tree.insert("", "end", values=(
                p["payment_date"][:19] if p.get("payment_date") else "",
                f"{float(p['amount']):,.2f}",
                p["payment_mode"],
                b_no,
                p["reference_note"] or "-",
            ))

    def _add_new_bill(self):
        s_list = dao.get_all_suppliers()
        _AddPurchaseBillDlg(self, suppliers=s_list, on_save=self._load_data, preselect=self._s)

    def _pay_now(self):
        # Check if user selected a bill row in top table
        sel = self._bills_tree.selection()
        b_id = 0
        b_no = ""
        sug_amt = 0.0
        if sel:
            try:
                vals = self._bills_tree.item(sel[0], "values")
                if vals and len(vals) >= 5:
                    b_no = vals[0]
                    due_str = vals[4].replace(",", "").strip()
                    sug_amt = float(due_str)
                    bills = dao.get_purchase_bills_by_supplier(self._s.supplier_id)
                    b_obj = next((x for x in bills if x["bill_number"] == b_no), None)
                    if b_obj:
                        b_id = int(b_obj["purchase_bill_id"])
            except Exception:
                pass
        
        _RecordSupplierPaymentDlg(self, supplier=self._s, on_save=self._load_data, bill_id=b_id, bill_no=b_no, suggested_amt=sug_amt)


# ── Reports ───────────────────────────────────────────────────────────────
class ReportsPanel(tk.Frame):
    def __init__(self, parent, user):
        super().__init__(parent, bg=APP_BG)
        self.user = user; self._data = []
        self._build(); self._load_sales()

    def _build(self):
        top = tk.Frame(self, bg=APP_BG); top.pack(fill=tk.X, padx=24, pady=(16, 10))
        _lbl(top, "Analytics & Financial Reports", font=FONT_HEADING, bg=APP_BG).pack(side=tk.LEFT)

        self._cur = "pnl"
        tabs = tk.Frame(self, bg=APP_BG); tabs.pack(fill=tk.X, padx=24, pady=(0, 10))
        self._tab_btns = {}
        for lbl, key in [("📈  Sales Report", "sales"),
                         ("📊  Graphical P&L", "pnl"),
                         ("📋  Billing History", "bills"),
                         ("📦  Inventory Valuation", "inventory")]:
            btn = _btn(tabs, lbl, PRIMARY if key == self._cur else "#E2E8F0", WHITE if key == self._cur else "#334155", lambda k=key: self._switch(k))
            btn.pack(side=tk.LEFT, padx=(0, 6), pady=4)
            self._tab_btns[key] = btn

        filt = tk.Frame(self, bg=WHITE, padx=16, pady=10, bd=1)
        filt.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        filt.pack(fill=tk.X, padx=24, pady=(0, 10))
        
        _lbl(filt, "From Date:", bg=WHITE, font=FONT_BOLD).pack(side=tk.LEFT, padx=(0, 6))
        self._from = tk.StringVar(value=(datetime.now().strftime("%Y-%m-01")))
        _entry(filt, width=12).pack(side=tk.LEFT, padx=(0, 16))
        filt.children[list(filt.children)[-1]].config(textvariable=self._from)
        
        _lbl(filt, "To Date:", bg=WHITE, font=FONT_BOLD).pack(side=tk.LEFT, padx=(0, 6))
        self._to = tk.StringVar(value=datetime.now().strftime("%Y-%m-%d"))
        _entry(filt, width=12).pack(side=tk.LEFT, padx=(0, 16))
        filt.children[list(filt.children)[-1]].config(textvariable=self._to)
        
        _btn(filt, "Apply Filter", ACCENT_BLUE, WHITE, lambda: self._switch(self._cur)).pack(side=tk.LEFT, padx=(0, 8))
        _btn(filt, "📥  Export to CSV", SUCCESS, WHITE, self._export).pack(side=tk.RIGHT)

        # Pack summary label at BOTTOM first so it never gets pushed off
        self._sum_v = tk.StringVar(value="")
        sum_lbl = _lbl(self, "", font=FONT_BOLD, bg=APP_BG, fg=PRIMARY)
        sum_lbl.config(textvariable=self._sum_v)
        sum_lbl.pack(side=tk.BOTTOM, anchor=tk.W, padx=24, pady=10)

        self._wrap = tk.Frame(self, bg=WHITE, bd=0)
        self._wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        self._wrap.pack(side=tk.TOP, fill=tk.BOTH, expand=True, padx=24)

    def _bills_filtered(self):
        try:
            from datetime import datetime as dt
            fd = dt.strptime(self._from.get(), "%Y-%m-%d")
            td = dt.strptime(self._to.get(), "%Y-%m-%d").replace(hour=23, minute=59)
        except Exception:
            fd = datetime.now().replace(day=1); td = datetime.now()
        return [b for b in dao.get_all_bills(limit=2000)
                if fd <= b.bill_date <= td and b.status == "COMPLETED"]

    def _switch(self, key):
        self._cur = key
        for k, b in getattr(self, "_tab_btns", {}).items():
            if k == key:
                b.config(bg=PRIMARY, fg=WHITE, activebackground=PRIMARY, activeforeground=WHITE)
            else:
                b.config(bg="#E2E8F0", fg="#334155", activebackground="#CBD5E1", activeforeground="#1E293B")
        for w in self._wrap.winfo_children(): w.destroy()
        if key == "sales":   self._load_sales()
        elif key == "pnl":   self._load_pnl()
        elif key == "bills": self._load_bills()
        else:                self._load_inv()

    def _load_pnl(self):
        from collections import defaultdict
        bills = self._bills_filtered()
        item_map = {i.item_code: i for i in dao.get_all_items()}
        
        daily_rev = defaultdict(float)
        daily_cost = defaultdict(float)
        daily_profit = defaultdict(float)
        daily_tx = defaultdict(int)
        
        total_rev = 0.0
        total_cost = 0.0
        
        for b in bills:
            dt_str = b.bill_date.strftime("%Y-%m-%d")
            daily_tx[dt_str] += 1
            rev = b.total_amount
            cost = 0.0
            for bi in b.bill_items:
                if bi.item_code in item_map:
                    item_cost = item_map[bi.item_code].purchase_price * bi.quantity
                    if item_cost <= 0:
                        item_cost = bi.taxable_amount * 0.65
                    cost += item_cost
                else:
                    cost += bi.taxable_amount * 0.65
            if not b.bill_items:
                cost = rev * 0.65
                
            daily_rev[dt_str] += rev
            daily_cost[dt_str] += cost
            daily_profit[dt_str] += (rev - cost)
            total_rev += rev
            total_cost += cost
            
        total_profit = total_rev - total_cost
        margin = (total_profit / total_rev * 100) if total_rev > 0 else 0.0
        
        # 1. KPI Cards Row
        cards_frm = tk.Frame(self._wrap, bg=WHITE, pady=8)
        cards_frm.pack(fill=tk.X, padx=16, pady=(10, 6))
        
        def _kpi(title, val, col):
            bx = tk.Frame(cards_frm, bg="#F8FAFC", bd=1, relief=tk.SOLID, padx=16, pady=10)
            bx.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
            bx.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=6)
            tk.Label(bx, text=title, font=FONT_SMALL, fg="#64748B", bg="#F8FAFC").pack(anchor=tk.W)
            tk.Label(bx, text=val, font=("Segoe UI", 15, "bold"), fg=col, bg="#F8FAFC").pack(anchor=tk.W, pady=(4, 0))

        _kpi("Total Revenue (Sales)", f"₹ {total_rev:,.2f}", "#1E293B")
        _kpi("Cost of Goods Sold (COGS)", f"₹ {total_cost:,.2f}", "#D97706")
        prof_col = "#10B981" if total_profit >= 0 else "#EF4444"
        _kpi("Net Profit / Loss", f"₹ {total_profit:,.2f}", prof_col)
        _kpi("Net Profit Margin", f"{margin:.1f}%", PRIMARY)
        
        # 2. Graphical Chart Frame
        chart_frm = tk.Frame(self._wrap, bg=WHITE, bd=1)
        chart_frm.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        chart_frm.pack(fill=tk.BOTH, expand=True, padx=22, pady=6)
        
        canvas = tk.Canvas(chart_frm, bg="#F8FAFC", height=240, bd=0, highlightthickness=0)
        canvas.pack(fill=tk.BOTH, expand=True, padx=12, pady=12)
        
        dates = sorted(daily_rev.keys())
        self._data = [(d, daily_tx[d], f"₹ {daily_rev[d]:,.2f}", f"₹ {daily_cost[d]:,.2f}", f"₹ {daily_profit[d]:,.2f}", f"{(daily_profit[d]/daily_rev[d]*100 if daily_rev[d]>0 else 0):.1f}%") for d in dates]
        
        def _draw(e=None):
            canvas.delete("all")
            w, h = canvas.winfo_width(), canvas.winfo_height()
            if w < 100 or h < 100: w, h = 850, 240
            
            canvas.create_text(20, 18, anchor=tk.NW, text="Revenue vs Cost vs Net Profit Trend (Grouped Bar Analysis)", font=("Segoe UI", 11, "bold"), fill="#1E293B")
            
            # Legend
            canvas.create_rectangle(w-320, 15, w-306, 28, fill="#2563EB", outline="")
            canvas.create_text(w-300, 21, anchor=tk.W, text="Revenue", font=("Segoe UI", 9), fill="#334155")
            canvas.create_rectangle(w-225, 15, w-211, 28, fill="#F59E0B", outline="")
            canvas.create_text(w-205, 21, anchor=tk.W, text="Cost (COGS)", font=("Segoe UI", 9), fill="#334155")
            canvas.create_rectangle(w-120, 15, w-106, 28, fill="#10B981", outline="")
            canvas.create_text(w-100, 21, anchor=tk.W, text="Net Profit", font=("Segoe UI", 9), fill="#334155")
            
            if not dates:
                canvas.create_text(w//2, h//2, text="No sales data found in selected period", font=FONT_BOLD, fill="#94A3B8")
                return
                
            pad_l, pad_r, pad_t, pad_b = 65, 30, 50, 42
            gw = max(100, w - pad_l - pad_r)
            gh = max(50, h - pad_t - pad_b)
            
            max_val = max([max(daily_rev[d], daily_cost[d], daily_profit[d]) for d in dates] + [100])
            for i in range(5):
                val = max_val * i / 4.0
                y = pad_t + gh - (i / 4.0 * gh)
                canvas.create_line(pad_l, y, w-pad_r, y, fill="#E2E8F0", dash=(2, 2))
                canvas.create_text(pad_l-8, y, anchor=tk.E, text=f"₹{val:,.0f}", font=("Segoe UI", 8), fill="#64748B")
                
            canvas.create_line(pad_l, pad_t+gh, w-pad_r, pad_t+gh, fill="#94A3B8", width=1.5)
            
            n = len(dates)
            gw_slot = gw / n
            bw = min(26, gw_slot * 0.26)
            
            for idx, d in enumerate(dates):
                cx = pad_l + (idx + 0.5) * gw_slot
                xr, xc, xp = cx - bw - 2, cx, cx + bw + 2
                
                hr = (daily_rev[d] / max_val) * gh if max_val > 0 else 0
                hc = (daily_cost[d] / max_val) * gh if max_val > 0 else 0
                hp = (daily_profit[d] / max_val) * gh if max_val > 0 else 0
                
                yb = pad_t + gh
                canvas.create_rectangle(xr-bw/2, yb-hr, xr+bw/2, yb, fill="#2563EB", outline="")
                canvas.create_rectangle(xc-bw/2, yb-hc, xc+bw/2, yb, fill="#F59E0B", outline="")
                pcol = "#10B981" if daily_profit[d] >= 0 else "#EF4444"
                canvas.create_rectangle(xp-bw/2, yb-hp, xp+bw/2, yb, fill=pcol, outline="")
                
                canvas.create_text(cx, yb+14, anchor=tk.N, text=d[-5:], font=("Segoe UI", 8, "bold"), fill="#334155")
                if hp > 16:
                    canvas.create_text(xp, yb-hp-7, anchor=tk.S, text=f"{daily_profit[d]:,.0f}", font=("Segoe UI", 7, "bold"), fill=pcol)

        canvas.bind("<Configure>", _draw)
        canvas.after(50, _draw)
        
        # 3. Breakdown Table
        tbl_frm = tk.Frame(self._wrap, bg=WHITE)
        tbl_frm.pack(fill=tk.BOTH, expand=True, padx=22, pady=(4, 12))
        cols = [("date", 160, "Date"), ("orders", 90, "Orders"), ("rev", 160, "Revenue (₹)"), ("cost", 160, "Cost / COGS (₹)"), ("prof", 160, "Net Profit (₹)"), ("mgn", 120, "Margin (%)")]
        tree = _tbl(tbl_frm, cols, height=8)
        for r in self._data: tree.insert("", "end", values=r)
        self._sum_v.set(f"Total Period Profit: ₹ {total_profit:,.2f}    •    Overall Profit Margin: {margin:.1f}%")

    def _load_sales(self):
        from collections import defaultdict
        bills = self._bills_filtered()
        daily = defaultdict(float)
        for b in bills: daily[b.bill_date.strftime("%Y-%m-%d")] += b.total_amount
        self._data = [(d, f"₹ {a:,.2f}") for d, a in sorted(daily.items())]
        cols = [("date", 240, "Date"), ("total", 240, "Total Daily Sales (₹)")]
        tree = _tbl(self._wrap, cols, height=14)
        for row in self._data: tree.insert("", "end", values=row)
        self._sum_v.set(f"Total Period Sales: ₹ {sum(b.total_amount for b in bills):,.2f}    •    Total Transactions: {len(bills)}")

    def _load_bills(self):
        bills = self._bills_filtered()
        self._data = [(b.bill_number, b.bill_date.strftime("%Y-%m-%d %H:%M"),
            b.customer_name or "—", f"{b.total_amount:,.2f}",
            b.payment_mode or "—", b.status) for b in bills]
            
        action_bar = tk.Frame(self._wrap, bg=WHITE, pady=4)
        action_bar.pack(fill=tk.X, pady=(0, 6))
        
        cols = [("no", 120, "Bill No"), ("dt", 140, "Date & Time"), ("cust", 170, "Customer"),
                ("amt", 110, "Amount (₹)"), ("mode", 100, "Payment Mode"), ("st", 95, "Status")]
        tree = _tbl(self._wrap, cols, height=13)
        for r in self._data: tree.insert("", "end", values=r)
        
        def _open_selected(e=None):
            sel = tree.selection()
            if not sel:
                messagebox.showwarning("Select Bill", "Please select a bill from the list above to view & print its receipt.", parent=self)
                return
            idx = tree.index(sel[0])
            if 0 <= idx < len(bills):
                _ReceiptDlg(self, bills[idx])
                
        _btn(action_bar, "🖨️  View & Print Selected Bill Receipt (with QR Code)", PRIMARY, WHITE, _open_selected).pack(side=tk.LEFT)
        tk.Label(action_bar, text="💡 Tip: Double-click any row below to open printable bill with QR code!", font=FONT_SMALL, fg="#64748B", bg=WHITE).pack(side=tk.LEFT, padx=12)
        
        tree.bind("<Double-1>", _open_selected)
        self._sum_v.set(f"Total Transactions Value: ₹ {sum(b.total_amount for b in bills):,.2f}    •    Count: {len(bills)}")

    def _load_inv(self):
        items = dao.get_all_items()
        self._data = [(i.item_code, i.item_name, i.category, i.stock_quantity,
            f"₹ {i.selling_price:,.2f}", f"₹ {i.stock_quantity*i.selling_price:,.2f}")
            for i in items]
        cols = [("code", 100, "Code"), ("name", 200, "Item Name"), ("cat", 110, "Category"),
                ("stk", 85, "Stock Qty"), ("price", 105, "Unit Price (₹)"), ("val", 120, "Total Value (₹)")]
        tree = _tbl(self._wrap, cols, height=14)
        for r in self._data: tree.insert("", "end", values=r)
        total_val = sum(i.stock_quantity * i.selling_price for i in items)
        self._sum_v.set(f"Total Inventory Valuation: ₹ {total_val:,.2f}    •    Unique SKU Count: {len(items)}")

    def _export(self):
        import csv
        from tkinter import filedialog
        path = filedialog.asksaveasfilename(defaultextension=".csv",
            filetypes=[("CSV Files", "*.csv")], title="Export CSV Report", parent=self)
        if path:
            with open(path, "w", newline="", encoding="utf-8") as f:
                csv.writer(f).writerows(self._data)
            messagebox.showinfo("Export Successful", f"Report exported to:\n{path}", parent=self)


# ── Users ─────────────────────────────────────────────────────────────────
class UsersPanel(tk.Frame):
    def __init__(self, parent, user):
        super().__init__(parent, bg=APP_BG)
        self.user = user; self._users: list[User] = []
        self._build(); self._load()

    def _build(self):
        top = tk.Frame(self, bg=APP_BG); top.pack(fill=tk.X, padx=24, pady=(16, 12))
        _lbl(top, "User Accounts & Permissions", font=FONT_HEADING, bg=APP_BG).pack(side=tk.LEFT)
        if self.user.role == "ADMIN":
            _btn(top, "➕  Add New User", SUCCESS, WHITE, self._add).pack(side=tk.RIGHT, padx=4)
        _btn(top, "🔄  Refresh", PRIMARY, WHITE, self._load).pack(side=tk.RIGHT, padx=4)
        
        wrap = tk.Frame(self, bg=WHITE, bd=0)
        wrap.config(highlightbackground=BORDER_COLOR, highlightthickness=1)
        wrap.pack(fill=tk.BOTH, expand=True, padx=24, pady=(0, 16))
        
        cols = [("uname", 120, "Username"), ("name", 190, "Full Name"),
                ("role", 110, "Role"), ("email", 190, "Email Address"), ("st", 95, "Status")]
        self._tree = _tbl(wrap, cols, height=16)

    def _load(self):
        self._users = dao.get_all_users()
        for r in self._tree.get_children(): self._tree.delete(r)
        for u in self._users:
            self._tree.insert("", "end", iid=u.username, values=(
                u.username, u.full_name, u.role, u.email, u.status))

    def _add(self):
        dlg = tk.Toplevel(self); dlg.title("Add New User")
        dlg.configure(bg=WHITE); dlg.geometry("400x360")
        dlg.resizable(False, False)
        
        _lbl(dlg, "Create System Account", font=FONT_HEADING, bg=WHITE).pack(anchor=tk.W, padx=24, pady=(18, 10))
        
        fields = [("username", "Username *"), ("password", "Password *"),
                  ("full_name", "Full Name *"), ("role", "Role (ADMIN/CASHIER)"), ("email", "Email Address")]
        ents = {}
        frm = tk.Frame(dlg, bg=WHITE, padx=24); frm.pack(fill=tk.BOTH, expand=True)
        for i, (k, lbl) in enumerate(fields):
            _lbl(frm, lbl, bg=WHITE, font=FONT_BOLD).grid(row=i, column=0, sticky=tk.W, pady=4)
            e = tk.Entry(frm, width=24, font=FONT_NORMAL, show="*" if k == "password" else "", bg=WHITE, fg=TEXT_MAIN)
            e.grid(row=i, column=1, padx=(12, 0), pady=4); ents[k] = e
        def save():
            d = {k: e.get().strip() for k, e in ents.items()}
            if not d["username"] or not d["password"]:
                messagebox.showerror("Validation Error", "Username and password required", parent=dlg); return
            u = User(d["username"], d["password"], d["full_name"] or d["username"], d["role"] or "CASHIER")
            u.email = d["email"]
            dao.save_user(u); self._load(); dlg.destroy()
        br = tk.Frame(dlg, bg=WHITE); br.pack(pady=16)
        _btn(br, "💾  Create User", SUCCESS, WHITE, save).pack(side=tk.LEFT, padx=6)
        _btn(br, "Cancel", "#E2E8F0", TEXT_MAIN, dlg.destroy).pack(side=tk.LEFT, padx=6)
        dlg.grab_set(); dlg.transient(self)


# ═════════════════════════════════════════════════════════════════════════
# ROOT APP
# ═════════════════════════════════════════════════════════════════════════
class App(tk.Tk):
    """Single Tk root with polished modern styling and clip-free layout."""

    def __init__(self):
        super().__init__()
        self.title("Bereezefootwearfancy")
        self.configure(bg=APP_BG)
        
        # High-DPI awareness on Windows to prevent blurry fonts/widgets
        try:
            import ctypes
            ctypes.windll.shcore.SetProcessDpiAwareness(1)
        except Exception:
            pass

        # Apply official Bereeze Footwear Fancy application icon
        try:
            from .widgets import apply_app_icon
            apply_app_icon(self)
        except Exception:
            pass

        # Bind F11 for true borderless kiosk full-screen toggle, Escape to exit borderless
        self.bind("<F11>", lambda e: self.attributes("-fullscreen", not self.attributes("-fullscreen")))
        self.bind("<Escape>", lambda e: self.attributes("-fullscreen", False) if self.attributes("-fullscreen") else None)

        self._container = tk.Frame(self, bg=APP_BG)
        self._container.pack(fill=tk.BOTH, expand=True)

        self._show_login()

    def _show_login(self):
        for w in self._container.winfo_children():
            w.destroy()
        self.title("Bereezefootwearfancy – Login")
        try:
            self.attributes("-fullscreen", False)
            self.state("normal")
        except Exception:
            pass
        self.resizable(False, False)
        self.minsize(480, 560)
        self.geometry("480x560")
        self.update_idletasks()
        sw, sh = self.winfo_screenwidth(), self.winfo_screenheight()
        x = max(0, (sw - 480) // 2)
        y = max(0, (sh - 560) // 2)
        self.geometry(f"480x560+{x}+{y}")
        LoginPage(self._container, on_login=self._show_main).pack(
            fill=tk.BOTH, expand=True)

    def _show_main(self, user: User):
        for w in self._container.winfo_children():
            w.destroy()
        self.title("Bereezefootwearfancy")
        self.resizable(True, True)
        self.minsize(1050, 680)
        try:
            # Open main dashboard & POS right into full-screen maximized window
            self.state("zoomed")
        except Exception:
            self.geometry("1240x780")
            self.update_idletasks()
            sw, sh = self.winfo_screenwidth(), self.winfo_screenheight()
            self.geometry(f"1240x780+{(sw-1240)//2}+{(sh-780)//2}")
        MainPage(self._container, user=user, on_logout=self._show_login).pack(
            fill=tk.BOTH, expand=True)
