# ============================================================
# pos_billing/ui/frames/barcode_print_frame.py
# ============================================================
"""
Barcode Point & Print Program GUI Frame.

Implements the complete workflow:
  Launch -> Main Window -> Menu/Toolbar/Settings (Optical System Printer Selection)
  Wait For User Action:
    - Enter Barcode -> Validate -> Search Product -> Fill Table or Show Error
    - Load Invoice -> Select Voucher Type + Invoice No -> Fetch -> Display -> Fill Table
    - Change Settings -> Select Printer + Scheme -> Save
  Ready For Print:
    - Print Barcode -> Send to optical Windows Printer -> Complete -> Wait Next
    - Preview -> Open visual layout rendering
    - Save Layout -> Save configuration
    - Clear All -> Reset all fields -> Exit Program
"""

from __future__ import annotations

import logging
import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
from typing import TYPE_CHECKING, List, Dict, Any, Optional

from ...database import dao
from ...utils.barcode_printer import (
    get_available_printers,
    get_default_printer,
    load_barcode_settings,
    save_barcode_settings,
    render_label_image,
    render_print_sheet,
    print_to_system_printer,
    generate_barcode_image,
)
from ..widgets import center_window
from ..constants import (
    APP_BACKGROUND,
    PRIMARY_COLOR,
    SECONDARY_COLOR,
    DARK_COLOR,
    LIGHT_BG,
    TEXT_ON_PRIMARY,
    TEXT_ON_APP_BG,
    BORDER_COLOR,
    HEADING_FONT,
    TITLE_FONT,
    NORMAL_FONT,
    SMALL_FONT,
)

if TYPE_CHECKING:
    from ...database.models import User
    from PIL import ImageTk, Image

logger = logging.getLogger(__name__)


# ─────────────────────────────────────────────────────────────
# Preview Dialog (Visual Mockup before printing)
# ─────────────────────────────────────────────────────────────
class BarcodePreviewDialog(tk.Toplevel):
    """Interactive visual preview of rendered label sheets."""

    def __init__(self, parent: tk.Widget, pages: List[Any], printer_name: str, scheme: str) -> None:
        super().__init__(parent)
        self.pages = pages
        self.printer_name = printer_name
        self.scheme = scheme
        self.current_page = 0
        self._photo_ref: Optional[Any] = None

        self.title(f"👁️ Barcode Print Preview – {len(pages)} Page(s) ({scheme})")
        self.geometry("950x750")
        self.configure(bg="#F8FAFC")
        center_window(self, 950, 750)

        self._build_ui()
        self._show_page(0)

    def _build_ui(self) -> None:
        top = tk.Frame(self, bg="#0D9488", padx=20, pady=14)
        top.pack(fill=tk.X)
        top.pack_propagate(False)

        tk.Label(
            top,
            text="👁️ LIVE BARCODE LABEL PREVIEW",
            font=("Segoe UI", 15, "bold"),
            fg="white",
            bg="#0D9488"
        ).pack(side=tk.LEFT)

        tk.Label(
            top,
            text=f"🖨️ Target Printer: {self.printer_name}",
            font=("Segoe UI", 10, "bold"),
            fg="#CCFBF1",
            bg="#0D9488"
        ).pack(side=tk.RIGHT)

        # Main Canvas area with scrollbars
        canvas_frame = tk.Frame(self, bg="#E2E8F0", bd=2, relief=tk.SUNKEN)
        canvas_frame.pack(fill=tk.BOTH, expand=True, padx=16, pady=16)

        self.canvas = tk.Canvas(canvas_frame, bg="#94A3B8", highlightthickness=0)
        v_scroll = ttk.Scrollbar(canvas_frame, orient=tk.VERTICAL, command=self.canvas.yview)
        h_scroll = ttk.Scrollbar(canvas_frame, orient=tk.HORIZONTAL, command=self.canvas.xview)
        self.canvas.configure(yscrollcommand=v_scroll.set, xscrollcommand=h_scroll.set)

        v_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        h_scroll.pack(side=tk.BOTTOM, fill=tk.X)
        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Bind configure event so label preview image stays dynamically centered
        self.canvas.bind("<Configure>", lambda e: self._render_canvas_image())

        # Footer controls
        bot = tk.Frame(self, bg="#F8FAFC", pady=12, padx=20)
        bot.pack(fill=tk.X)

        self.lbl_page_info = tk.Label(bot, text="Page 1 of 1", font=("Segoe UI", 11, "bold"), bg="#F8FAFC", fg="#1E293B")
        self.lbl_page_info.pack(side=tk.LEFT, padx=10)

        nav_frame = tk.Frame(bot, bg="#F8FAFC")
        nav_frame.pack(side=tk.LEFT, padx=20)

        self.btn_prev = tk.Button(
            nav_frame, text="◀ Previous Page", font=NORMAL_FONT, bg="#E2E8F0", fg="#1E293B",
            relief=tk.FLAT, cursor="hand2", padx=12, pady=6, command=lambda: self._show_page(self.current_page - 1)
        )
        self.btn_prev.pack(side=tk.LEFT, padx=4)

        self.btn_next = tk.Button(
            nav_frame, text="Next Page ▶", font=NORMAL_FONT, bg="#E2E8F0", fg="#1E293B",
            relief=tk.FLAT, cursor="hand2", padx=12, pady=6, command=lambda: self._show_page(self.current_page + 1)
        )
        self.btn_next.pack(side=tk.LEFT, padx=4)

        tk.Button(
            bot, text="🖨️ Print Right Now", font=("Segoe UI", 11, "bold"),
            bg="#0D9488", fg="white", activebackground="#0F766E", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", padx=18, pady=6,
            command=self._do_print
        ).pack(side=tk.RIGHT, padx=6)

        tk.Button(
            bot, text="Close", font=NORMAL_FONT,
            bg="#64748B", fg="white", activebackground="#475569", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", padx=16, pady=6,
            command=self.destroy
        ).pack(side=tk.RIGHT, padx=6)

    def _show_page(self, idx: int) -> None:
        if not self.pages:
            return
        self.current_page = max(0, min(idx, len(self.pages) - 1))
        page_img = self.pages[self.current_page]

        try:
            from PIL import ImageTk
            self._photo_ref = ImageTk.PhotoImage(page_img)
        except Exception as exc:
            logger.error("Failed rendering preview photo: %s", exc)
            return

        self._render_canvas_image()

        self.lbl_page_info.config(text=f"Page {self.current_page + 1} of {len(self.pages)}")
        self.btn_prev.config(state=tk.NORMAL if self.current_page > 0 else tk.DISABLED)
        self.btn_next.config(state=tk.NORMAL if self.current_page < len(self.pages) - 1 else tk.DISABLED)

    def _render_canvas_image(self) -> None:
        if self._photo_ref is None:
            return
        self.canvas.delete("all")

        iw = self._photo_ref.width()
        ih = self._photo_ref.height()

        self.update_idletasks()
        cw = self.canvas.winfo_width()
        ch = self.canvas.winfo_height()

        # Calculate exact center coordinates (horizontal and vertical)
        cx = max(iw // 2 + 20, cw // 2) if cw > 1 else (iw // 2 + 20)
        cy = max(ih // 2 + 20, ch // 2) if ch > 1 else (ih // 2 + 20)

        self.canvas.create_image(cx, cy, anchor=tk.CENTER, image=self._photo_ref)

        scroll_w = max(iw + 80, cw)
        scroll_h = max(ih + 80, ch)
        self.canvas.configure(scrollregion=(0, 0, scroll_w, scroll_h))

    def _do_print(self) -> None:
        success, msg = print_to_system_printer(self.pages, self.printer_name, copies=1)
        if success:
            messagebox.showinfo("Printing Complete", msg, parent=self)
            self.destroy()
        else:
            messagebox.showerror("Print Error", msg, parent=self)


# ─────────────────────────────────────────────────────────────
# Main Barcode Print Program Frame
# ─────────────────────────────────────────────────────────────
class BarcodePrintFrame(tk.Frame):
    """
    Main Barcode Point & Print Program UI.
    Can be embedded inside MainFrame (sidebar) or run as top-level window.
    """

    def __init__(self, parent: tk.Widget, user: Optional[User] = None, on_exit: Optional[Any] = None) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.current_user = user
        self.on_exit_cb = on_exit

        self.queue_items: List[Dict[str, Any]] = []
        self.settings = load_barcode_settings()

        self._build_header_and_toolbar()
        self._build_settings_strip()
        self._build_action_inputs()
        self._build_barcode_table()
        self._build_action_footer()

        self._update_status("Wait For User Action – Enter Barcode, Load Invoice, or Change Settings")

    # ── 1. Header & Toolbar (Tools Menu, Save, Print, View) ────────────────
    def _build_header_and_toolbar(self) -> None:
        top = tk.Frame(self, bg=PRIMARY_COLOR, height=56)
        top.pack(fill=tk.X)
        top.pack_propagate(False)

        # Title
        title_box = tk.Frame(top, bg=PRIMARY_COLOR)
        title_box.pack(side=tk.LEFT, padx=16)

        tk.Label(
            title_box, text="🏷️  BARCODE POINT & PRINT PROGRAM",
            font=("Segoe UI", 14, "bold"), bg=PRIMARY_COLOR, fg=TEXT_ON_PRIMARY
        ).pack(side=tk.LEFT)

        tk.Label(
            title_box, text=" | High-Speed Label Generator",
            font=("Segoe UI", 10), bg=PRIMARY_COLOR, fg="#B2DFDB"
        ).pack(side=tk.LEFT, padx=(6, 0))

        # Tools Menu (Menu Button)
        self.menu_tools = tk.Menu(self, tearoff=0, font=NORMAL_FONT)
        self.menu_tools.add_command(label="🔄 Refresh Optical Printers", command=self._refresh_printers)
        self.menu_tools.add_command(label="⚙️ Barcode Scheme Settings", command=self._open_scheme_settings)
        self.menu_tools.add_separator()
        self.menu_tools.add_command(label="🧪 Print Test Sample Label", command=self._print_test_sample)
        self.menu_tools.add_separator()
        self.menu_tools.add_command(label="🧹 Reset Configuration to Default", command=self._reset_config)

        # Toolbar Buttons (Right side)
        toolbar = tk.Frame(top, bg=PRIMARY_COLOR)
        toolbar.pack(side=tk.RIGHT, padx=12)

        def make_tb_btn(label: str, cmd: Any, bg_c: str = SECONDARY_COLOR) -> tk.Button:
            return tk.Button(
                toolbar, text=label, font=("Segoe UI", 9, "bold"),
                bg=bg_c, fg=TEXT_ON_PRIMARY, relief=tk.FLAT, cursor="hand2",
                padx=12, pady=5, activebackground="#004D40", activeforeground=TEXT_ON_PRIMARY,
                command=cmd
            )

        make_tb_btn("Tools ▾", lambda: self.menu_tools.post(self.winfo_pointerx(), self.winfo_pointery())).pack(side=tk.LEFT, padx=4)
        make_tb_btn("💾 Save", self._save_settings_action, "#0284C7").pack(side=tk.LEFT, padx=4)
        make_tb_btn("👁️ View", self._preview_action, "#0EA5E9").pack(side=tk.LEFT, padx=4)
        make_tb_btn("🖨️ Print", self._print_action, "#16A34A").pack(side=tk.LEFT, padx=4)

    # ── 2. Settings Strip (Optical Printer Selection & Barcode Scheme) ─────
    def _build_settings_strip(self) -> None:
        strip = tk.Frame(self, bg=LIGHT_BG, bd=1, relief=tk.SOLID, padx=16, pady=10)
        strip.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
        strip.pack(fill=tk.X, padx=16, pady=(12, 6))

        # Optical Printer Dropdown
        tk.Label(strip, text="🖨️ Optical System Printer:", font=("Segoe UI", 10, "bold"), bg=LIGHT_BG, fg=DARK_COLOR).pack(side=tk.LEFT)

        self.combo_printers = ttk.Combobox(strip, font=("Segoe UI", 10), state="readonly", width=32)
        self.combo_printers.pack(side=tk.LEFT, padx=(8, 4))
        self._refresh_printers()

        btn_ref = tk.Button(
            strip, text="🔄", font=SMALL_FONT, bg="#E2E8F0", fg=DARK_COLOR,
            relief=tk.FLAT, cursor="hand2", padx=6, pady=2, command=self._refresh_printers
        )
        btn_ref.pack(side=tk.LEFT, padx=(0, 16))

        # Barcode Scheme Dropdown
        tk.Label(strip, text="📐 Scheme:", font=("Segoe UI", 10, "bold"), bg=LIGHT_BG, fg=DARK_COLOR).pack(side=tk.LEFT)
        self.combo_scheme = ttk.Combobox(
            strip, font=("Segoe UI", 10), state="readonly", width=22,
            values=[
                "Code 128 (Universal Standard)",
                "Code 39 (Alphanumeric)",
                "EAN-13 (Retail Standard)",
                "QR Code (2D High Density)"
            ]
        )
        self.combo_scheme.set(self.settings.get("barcode_scheme", "Code 128 (Universal Standard)"))
        self.combo_scheme.pack(side=tk.LEFT, padx=(6, 16))

        # Layout / Paper Dimensions
        tk.Label(strip, text="📄 Paper Layout:", font=("Segoe UI", 10, "bold"), bg=LIGHT_BG, fg=DARK_COLOR).pack(side=tk.LEFT)
        self.combo_layout = ttk.Combobox(
            strip, font=("Segoe UI", 10), state="readonly", width=26,
            values=[
                "Thermal Roll - 50x25mm (2 Columns)",
                "Thermal Roll - 50x25mm (1 Column)",
                "Sheet A4 (24 Labels - 3x8)",
                "Sheet A4 (40 Labels - 4x10)"
            ]
        )
        self.combo_layout.set(self.settings.get("layout_type", "Thermal Roll - 50x25mm (2 Columns)"))
        self.combo_layout.pack(side=tk.LEFT, padx=(6, 0))

    def _refresh_printers(self) -> None:
        """Dynamically fetch connected optical/system printers asynchronously without UI lag."""
        import threading

        # Set fast cached printers immediately
        printers = get_available_printers()
        self.combo_printers["values"] = printers
        saved = self.settings.get("selected_printer")
        if saved and saved in printers:
            self.combo_printers.set(saved)
        elif printers:
            self.combo_printers.set(printers[0])

        # Run deep refresh in background daemon thread
        def _bg_fetch():
            fresh_printers = get_available_printers(force_refresh=True)
            def _update_ui():
                if self.winfo_exists():
                    self.combo_printers["values"] = fresh_printers
                    if saved and saved in fresh_printers:
                        self.combo_printers.set(saved)
            self.after(0, _update_ui)

        threading.Thread(target=_bg_fetch, daemon=True).start()

    # ── 3. User Action Inputs (Enter Barcode / Load Invoice) ───────────────
    def _build_action_inputs(self) -> None:
        action_frame = tk.Frame(self, bg=APP_BACKGROUND)
        action_frame.pack(fill=tk.X, padx=16, pady=6)

        # Left Box: Enter Barcode / Search Product
        box_barcode = tk.LabelFrame(
            action_frame, text=" ⚡ Mode 1: Enter Barcode & Validate Product ",
            font=("Segoe UI", 10, "bold"), bg=LIGHT_BG, fg=PRIMARY_COLOR, padx=14, pady=12
        )
        box_barcode.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(0, 8))

        row1 = tk.Frame(box_barcode, bg=LIGHT_BG)
        row1.pack(fill=tk.X, pady=4)

        tk.Label(row1, text="Barcode / Code / Name:", font=NORMAL_FONT, bg=LIGHT_BG, fg=DARK_COLOR).pack(side=tk.LEFT)
        self.entry_barcode = tk.Entry(row1, font=("Segoe UI", 11), width=24, bd=1, relief=tk.SOLID)
        self.entry_barcode.pack(side=tk.LEFT, padx=8, fill=tk.X, expand=True)
        self.entry_barcode.bind("<Return>", lambda e: self._search_barcode_action())
        self.entry_barcode.bind("<KeyRelease>", self._validate_barcode_live)

        self.lbl_validate_icon = tk.Label(row1, text="❓", font=("Segoe UI", 12), bg=LIGHT_BG, fg="#64748B")
        self.lbl_validate_icon.pack(side=tk.LEFT, padx=(0, 8))

        btn_search = tk.Button(
            row1, text="🔍 Search Product", font=("Segoe UI", 9, "bold"),
            bg=PRIMARY_COLOR, fg=TEXT_ON_PRIMARY, relief=tk.FLAT, cursor="hand2",
            padx=12, pady=5, command=self._search_barcode_action
        )
        btn_search.pack(side=tk.RIGHT)

        # Right Box: Load Invoice / Bill Voucher
        box_invoice = tk.LabelFrame(
            action_frame, text=" 🧾 Mode 2: Load Invoice / Bill Voucher ",
            font=("Segoe UI", 10, "bold"), bg=LIGHT_BG, fg=PRIMARY_COLOR, padx=14, pady=12
        )
        box_invoice.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(8, 0))

        row2 = tk.Frame(box_invoice, bg=LIGHT_BG)
        row2.pack(fill=tk.X, pady=4)

        tk.Label(row2, text="Voucher Type:", font=NORMAL_FONT, bg=LIGHT_BG, fg=DARK_COLOR).pack(side=tk.LEFT)
        self.combo_voucher_type = ttk.Combobox(
            row2, font=("Segoe UI", 9), state="readonly", width=16,
            values=["SALES (Customer Bill)", "SALES_RETURN", "PURCHASE (Supplier Invoice)", "PURCHASE_RETURN", "ALL VOUCHERS"]
        )
        self.combo_voucher_type.set("PURCHASE (Supplier Invoice)")
        self.combo_voucher_type.pack(side=tk.LEFT, padx=(6, 12))

        tk.Label(row2, text="Invoice No:", font=NORMAL_FONT, bg=LIGHT_BG, fg=DARK_COLOR).pack(side=tk.LEFT)
        self.entry_invoice_no = tk.Entry(row2, font=("Segoe UI", 11), width=16, bd=1, relief=tk.SOLID)
        self.entry_invoice_no.pack(side=tk.LEFT, padx=6, fill=tk.X, expand=True)
        self.entry_invoice_no.bind("<Return>", lambda e: self._load_invoice_action())

        btn_load_inv = tk.Button(
            row2, text="📥 Click Load Button", font=("Segoe UI", 9, "bold"),
            bg="#0D9488", fg=TEXT_ON_PRIMARY, relief=tk.FLAT, cursor="hand2",
            padx=12, pady=5, command=self._load_invoice_action
        )
        btn_load_inv.pack(side=tk.RIGHT)

    def _validate_barcode_live(self, event: Optional[Any] = None) -> None:
        """Live feedback on barcode structure."""
        val = self.entry_barcode.get().strip()
        if not val:
            self.lbl_validate_icon.config(text="❓", fg="#64748B")
        elif len(val) >= 3:
            self.lbl_validate_icon.config(text="✅", fg="#16A34A")
        else:
            self.lbl_validate_icon.config(text="⏳", fg="#EAB308")

    # ── 4. Barcode Table Panel (Fill Barcode Table) ────────────────────────
    def _build_barcode_table(self) -> None:
        table_frame = tk.Frame(self, bg=APP_BACKGROUND)
        table_frame.pack(fill=tk.BOTH, expand=True, padx=16, pady=6)

        # Header bar
        tb_top = tk.Frame(table_frame, bg=APP_BACKGROUND)
        tb_top.pack(fill=tk.X, pady=(0, 4))
        tk.Label(
            tb_top, text="📋 Barcode Queue Table (Ready For Print)",
            font=("Segoe UI", 11, "bold"), bg=APP_BACKGROUND, fg=DARK_COLOR
        ).pack(side=tk.LEFT)

        # Quick controls
        btn_box = tk.Frame(tb_top, bg=APP_BACKGROUND)
        btn_box.pack(side=tk.RIGHT)

        tk.Button(btn_box, text="+1 Qty", font=SMALL_FONT, bg="#E2E8F0", fg=DARK_COLOR, relief=tk.FLAT, cursor="hand2", padx=8, command=lambda: self._adjust_selected_qty(1)).pack(side=tk.LEFT, padx=2)
        tk.Button(btn_box, text="-1 Qty", font=SMALL_FONT, bg="#E2E8F0", fg=DARK_COLOR, relief=tk.FLAT, cursor="hand2", padx=8, command=lambda: self._adjust_selected_qty(-1)).pack(side=tk.LEFT, padx=2)
        tk.Button(btn_box, text="🗑️ Remove Row", font=SMALL_FONT, bg="#FEE2E2", fg="#991B1B", relief=tk.FLAT, cursor="hand2", padx=8, command=self._remove_selected_row).pack(side=tk.LEFT, padx=2)
        tk.Button(btn_box, text="🧹 Clear All", font=SMALL_FONT, bg="#64748B", fg=TEXT_ON_PRIMARY, relief=tk.FLAT, cursor="hand2", padx=8, command=self._clear_all_action).pack(side=tk.LEFT, padx=2)

        # Treeview
        cols = ("idx", "item_code", "barcode", "item_name", "category", "size_color", "price", "label_qty")
        self.tree = ttk.Treeview(table_frame, columns=cols, show="headings", height=10)

        self.tree.heading("idx", text="#")
        self.tree.heading("item_code", text="Item Code")
        self.tree.heading("barcode", text="Barcode Number")
        self.tree.heading("item_name", text="Product Name")
        self.tree.heading("category", text="Category")
        self.tree.heading("size_color", text="Size / Color")
        self.tree.heading("price", text="Selling Price (₹)")
        self.tree.heading("label_qty", text="Label Qty")

        self.tree.column("idx", width=40, anchor="center")
        self.tree.column("item_code", width=110, anchor="w")
        self.tree.column("barcode", width=140, anchor="center")
        self.tree.column("item_name", width=220, anchor="w")
        self.tree.column("category", width=110, anchor="w")
        self.tree.column("size_color", width=120, anchor="center")
        self.tree.column("price", width=110, anchor="e")
        self.tree.column("label_qty", width=90, anchor="center")

        v_scroll = ttk.Scrollbar(table_frame, orient=tk.VERTICAL, command=self.tree.yview)
        self.tree.configure(yscrollcommand=v_scroll.set)
        v_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.tree.pack(fill=tk.BOTH, expand=True)

        self.tree.bind("<Double-1>", self._on_tree_double_click)

    # ── 5. Action Footer & Status Bar ──────────────────────────────────────
    def _build_action_footer(self) -> None:
        footer = tk.Frame(self, bg=LIGHT_BG, bd=1, relief=tk.SOLID, padx=16, pady=12)
        footer.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
        footer.pack(fill=tk.X, padx=16, pady=(6, 12))

        # Big Action Buttons (packed FIRST on RIGHT so they stay fully visible)
        btn_box = tk.Frame(footer, bg=LIGHT_BG)
        btn_box.pack(side=tk.RIGHT)

        tk.Button(
            btn_box, text="💾 Save Layout", font=("Segoe UI", 10, "bold"),
            bg="#0284C7", fg="white", activebackground="#0369A1", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", padx=10, pady=6,
            command=self._save_settings_action
        ).pack(side=tk.LEFT, padx=3)

        tk.Button(
            btn_box, text="👁️ Preview Labels", font=("Segoe UI", 10, "bold"),
            bg="#0EA5E9", fg="white", activebackground="#0284C7", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", padx=10, pady=6,
            command=self._preview_action
        ).pack(side=tk.LEFT, padx=3)

        tk.Button(
            btn_box, text="🖨️ Print Barcode (Send to Printer)", font=("Segoe UI", 10, "bold"),
            bg="#16A34A", fg="white", activebackground="#15803D", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", padx=12, pady=6,
            command=self._print_action
        ).pack(side=tk.LEFT, padx=3)

        tk.Button(
            btn_box, text="🧹 Clear All", font=("Segoe UI", 10),
            bg="#64748B", fg="white", activebackground="#475569", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", padx=10, pady=6,
            command=self._clear_all_action
        ).pack(side=tk.LEFT, padx=3)

        tk.Button(
            btn_box, text="🚪 Exit Program", font=("Segoe UI", 10, "bold"),
            bg="#DC2626", fg="white", activebackground="#B91C1C", activeforeground="white",
            relief=tk.FLAT, cursor="hand2", padx=10, pady=6,
            command=self._exit_action
        ).pack(side=tk.LEFT, padx=(3, 0))

        # Status Bar Text (packed SECOND on LEFT to fill available remaining space)
        self.lbl_status = tk.Label(footer, text="Status: Ready", font=("Segoe UI", 10, "bold"), bg=LIGHT_BG, fg=DARK_COLOR, anchor="w")
        self.lbl_status.pack(side=tk.LEFT, fill=tk.X, expand=True)

    def _update_status(self, text: str) -> None:
        total_items = len(self.queue_items)
        total_labels = sum(int(it.get("label_qty", 1)) for it in self.queue_items)
        printer = self.combo_printers.get() or "None"
        self.lbl_status.config(text=f"📌 {text} | Items: {total_items} | Total Labels: {total_labels} | Target: {printer[:22]}")

    # ── Actions & Workflow Methods ─────────────────────────────────────────
    def _search_barcode_action(self) -> None:
        """Validate Barcode -> Search Product -> Barcode Found? -> Fill Table or Show Error."""
        query = self.entry_barcode.get().strip()
        if not query:
            messagebox.showwarning("Validation Error", "Please enter a Barcode, Item Code, or Product Name to search.", parent=self)
            self._update_status("Wait Again – Input cannot be empty.")
            return

        # 1. Validate / Search in database
        item = dao.search_item_by_barcode(query)
        if not item:
            item = dao.search_item_by_code(query)
        
        # Also try partial name search if exact code/barcode not found
        if not item:
            all_items = dao.get_all_items()
            matches = [i for i in all_items if query.lower() in i.item_name.lower() or query.lower() in i.barcode.lower()]
            if len(matches) == 1:
                item = matches[0]
            elif len(matches) > 1:
                # Let user pick if multiple names match
                names = "\n".join(f"- {m.item_code}: {m.item_name} (Barcode: {m.barcode})" for m in matches[:6])
                messagebox.showinfo("Multiple Matches Found", f"Found multiple products matching '{query}':\n\n{names}\n\nPlease enter the exact Barcode or Item Code.", parent=self)
                return

        if not item:
            # Barcode Not Found -> Show Error -> Wait Again
            messagebox.showerror(
                "❌ Barcode / Product Not Found",
                f"No product matching barcode or code '{query}' was found in the inventory.\n\n"
                "Please verify the code or add the item in Inventory Management.",
                parent=self
            )
            self._update_status(f"Wait Again – Barcode '{query}' not found.")
            return

        # Barcode Found -> Fill Barcode Table
        # Check if already in queue
        for existing in self.queue_items:
            if existing["item_id"] == item.item_id or existing.get("barcode") == item.barcode:
                existing["label_qty"] += 1
                self._refresh_table()
                self.entry_barcode.delete(0, tk.END)
                self._update_status(f"Ready For Print – Increased quantity for '{item.item_name}' to {existing['label_qty']}.")
                return

        new_row = {
            "item_id": item.item_id,
            "item_code": item.item_code,
            "item_name": item.item_name,
            "barcode": item.barcode or item.item_code,
            "quantity": item.stock_quantity,
            "selling_price": item.selling_price,
            "size": item.size or "",
            "color": item.color or "",
            "category": item.category or "",
            "label_qty": self.settings.get("default_label_qty", 1),
            "source": "Direct Search"
        }
        self.queue_items.append(new_row)
        self._refresh_table()
        self.entry_barcode.delete(0, tk.END)
        self._update_status(f"Ready For Print – Added '{item.item_name}' (Barcode: {new_row['barcode']}).")

    def _load_invoice_action(self) -> None:
        """Select Voucher Type + Enter Invoice Number -> Click Load Button -> Fetch -> Get Product List -> Display -> Fill Table."""
        voucher_type_full = self.combo_voucher_type.get()
        voucher_type = voucher_type_full.split()[0]  # e.g., "SALES" or "PURCHASE" or "ALL"
        invoice_no = self.entry_invoice_no.get().strip()

        if not invoice_no:
            messagebox.showwarning("Input Required", "Please enter an Invoice / Bill Number to load.", parent=self)
            return

        items = dao.search_voucher_items(voucher_type, invoice_no)
        if not items:
            messagebox.showerror(
                "❌ Invoice Not Found",
                f"No items found for {voucher_type_full} matching invoice number '{invoice_no}'.\n\n"
                "Please check the voucher type and invoice number.",
                parent=self
            )
            self._update_status(f"Wait Again – Invoice '{invoice_no}' not found.")
            return

        # Display Products / Summary dialog before adding
        summary = "\n".join(
            f"• {it['item_name']} (Code: {it['item_code']} | Qty: {it['quantity']})"
            for it in items[:10]
        )
        if len(items) > 10:
            summary += f"\n...and {len(items) - 10} more item(s)."

        confirm = messagebox.askyesno(
            "📥 Display & Load Invoice Products",
            f"Fetched {len(items)} product(s) from invoice '{invoice_no}':\n\n{summary}\n\n"
            "Add all these products to the Barcode Table with their quantities pre-filled for printing?",
            parent=self
        )
        if not confirm:
            self._update_status("Wait Again – Invoice loading cancelled by user.")
            return

        # Fill Barcode Table
        added_count = 0
        for it in items:
            # Set label_qty = invoiced quantity
            it["label_qty"] = max(1, int(it.get("quantity", 1)))
            
            # Check if existing row
            found = False
            for existing in self.queue_items:
                if existing["item_code"] == it["item_code"] and existing.get("barcode") == it.get("barcode"):
                    existing["label_qty"] += it["label_qty"]
                    found = True
                    break
            if not found:
                self.queue_items.append(it)
                added_count += 1

        self._refresh_table()
        self.entry_invoice_no.delete(0, tk.END)
        self._update_status(f"Ready For Print – Loaded {len(items)} item(s) from invoice '{invoice_no}'.")

    def _refresh_table(self) -> None:
        for row in self.tree.get_children():
            self.tree.delete(row)

        for idx, item in enumerate(self.queue_items, start=1):
            sc = f"{item.get('size','')}{' / ' if item.get('size') and item.get('color') else ''}{item.get('color','')}"
            self.tree.insert(
                "", tk.END,
                values=(
                    idx,
                    item.get("item_code", ""),
                    item.get("barcode", ""),
                    item.get("item_name", ""),
                    item.get("category", ""),
                    sc,
                    f"{float(item.get('selling_price',0)):,.2f}",
                    item.get("label_qty", 1)
                )
            )
        self._update_status("Ready For Print" if self.queue_items else "Wait For User Action")

    def _adjust_selected_qty(self, delta: int) -> None:
        sel = self.tree.selection()
        if not sel:
            messagebox.showinfo("Select Item", "Please select an item in the table to adjust its label quantity.", parent=self)
            return
        
        idx_str = self.tree.item(sel[0], "values")[0]
        idx = int(idx_str) - 1
        if 0 <= idx < len(self.queue_items):
            curr = int(self.queue_items[idx].get("label_qty", 1))
            self.queue_items[idx]["label_qty"] = max(1, curr + delta)
            self._refresh_table()

    def _remove_selected_row(self) -> None:
        sel = self.tree.selection()
        if not sel:
            return
        idx_str = self.tree.item(sel[0], "values")[0]
        idx = int(idx_str) - 1
        if 0 <= idx < len(self.queue_items):
            removed = self.queue_items.pop(idx)
            self._refresh_table()
            self._update_status(f"Removed '{removed.get('item_name')}' from queue.")

    def _on_tree_double_click(self, event: Any) -> None:
        sel = self.tree.selection()
        if not sel:
            return
        idx_str = self.tree.item(sel[0], "values")[0]
        idx = int(idx_str) - 1
        if 0 <= idx < len(self.queue_items):
            curr = self.queue_items[idx].get("label_qty", 1)
            new_qty = simpledialog.askinteger("Set Label Quantity", f"Enter exact number of labels to print for '{self.queue_items[idx]['item_name']}':", initialvalue=curr, minvalue=1, maxvalue=500, parent=self)
            if new_qty is not None:
                self.queue_items[idx]["label_qty"] = new_qty
                self._refresh_table()

    def _clear_all_action(self) -> None:
        """Clear All -> Reset All Fields -> Wait For Next Action."""
        if not self.queue_items and not self.entry_barcode.get() and not self.entry_invoice_no.get():
            return
        if self.queue_items and not messagebox.askyesno("Clear All", "Reset all fields and clear the Barcode Queue table?", parent=self):
            return

        self.queue_items.clear()
        self.entry_barcode.delete(0, tk.END)
        self.entry_invoice_no.delete(0, tk.END)
        self.lbl_validate_icon.config(text="❓", fg="#64748B")
        self._refresh_table()
        self._update_status("Reset All Fields – Ready for next action.")

    def _save_settings_action(self) -> None:
        """Save Layout -> Save Configuration."""
        self.settings["selected_printer"] = self.combo_printers.get()
        self.settings["barcode_scheme"] = self.combo_scheme.get()
        self.settings["layout_type"] = self.combo_layout.get()
        
        if save_barcode_settings(self.settings):
            messagebox.showinfo("Configuration Saved", f"Successfully saved Barcode & Printer settings:\n\n• Printer: {self.settings['selected_printer']}\n• Scheme: {self.settings['barcode_scheme']}\n• Layout: {self.settings['layout_type']}", parent=self)
            self._update_status("Save Configuration – Settings updated successfully.")
        else:
            messagebox.showerror("Error", "Failed saving configuration file.", parent=self)

    def _preview_action(self) -> None:
        """Preview -> Open Preview asynchronously without freezing UI."""
        import threading
        if not self.queue_items:
            messagebox.showwarning("Empty Queue", "Please search for a product or load an invoice to preview barcode labels.", parent=self)
            return

        printer = self.combo_printers.get()
        scheme = self.combo_scheme.get()
        layout = self.combo_layout.get()

        self._update_status("Rendering label preview... Please wait.")
        self.config(cursor="watch")

        def _bg_render():
            pages = render_print_sheet(self.queue_items, scheme=scheme, layout_type=layout)
            def _finish():
                if self.winfo_exists():
                    self.config(cursor="")
                    if not pages:
                        messagebox.showwarning("Preview Error", "Could not render label pages.", parent=self)
                        self._update_status("Preview Error")
                    else:
                        self._update_status("Preview ready.")
                        BarcodePreviewDialog(self, pages, printer, scheme)
            self.after(0, _finish)

        threading.Thread(target=_bg_render, daemon=True).start()

    def _print_action(self) -> None:
        """Print Barcode -> Send To Printer asynchronously without freezing UI."""
        import threading
        if not self.queue_items:
            messagebox.showwarning("Empty Queue", "Please search for a product or load an invoice before printing.", parent=self)
            return

        printer = self.combo_printers.get()
        scheme = self.combo_scheme.get()
        layout = self.combo_layout.get()
        total_labels = sum(int(it.get("label_qty", 1)) for it in self.queue_items)

        confirm = messagebox.askyesno(
            "Confirm Print Job",
            f"Ready to print {total_labels} barcode label(s) across {len(self.queue_items)} product(s).\n\n"
            f"• Target Optical Printer: {printer}\n"
            f"• Barcode Scheme: {scheme}\n"
            f"• Layout: {layout}\n\n"
            "Send directly to printer now?",
            parent=self
        )
        if not confirm:
            self._update_status("Printing cancelled by user.")
            return

        self._update_status("Generating labels & sending to printer... Please wait.")
        self.config(cursor="watch")

        def _bg_print():
            pages = render_print_sheet(self.queue_items, scheme=scheme, layout_type=layout)
            success, msg = print_to_system_printer(pages, printer, copies=1)
            def _finish():
                if self.winfo_exists():
                    self.config(cursor="")
                    if success:
                        messagebox.showinfo("✅ Printing Complete", msg, parent=self)
                        self._update_status("Printing Complete! Waiting for next action.")
                    else:
                        messagebox.showerror("❌ Print Error", msg, parent=self)
                        self._update_status("Error sending job to printer.")
            self.after(0, _finish)

        threading.Thread(target=_bg_print, daemon=True).start()

    def _open_scheme_settings(self) -> None:
        """Modal dialog for customizing store name, dimensions, and options."""
        dlg = tk.Toplevel(self)
        dlg.title("⚙️ Barcode Scheme & Layout Configuration")
        dlg.geometry("450x380")
        dlg.configure(bg="#F8FAFC")
        try:
            dlg.tk.eval(f"tk::PlaceWindow {dlg._w} center")
        except Exception:
            pass

        top = tk.Frame(dlg, bg=PRIMARY_COLOR, padx=16, pady=12)
        top.pack(fill=tk.X)
        tk.Label(top, text="⚙️ Scheme Customization", font=("Segoe UI", 12, "bold"), fg="white", bg=PRIMARY_COLOR).pack(side=tk.LEFT)

        body = tk.Frame(dlg, bg="#F8FAFC", padx=20, pady=16)
        body.pack(fill=tk.BOTH, expand=True)

        tk.Label(body, text="Store Header Name on Labels:", font=NORMAL_FONT, bg="#F8FAFC", fg=DARK_COLOR).pack(anchor="w")
        ent_store = tk.Entry(body, font=("Segoe UI", 11), width=32)
        ent_store.insert(0, self.settings.get("store_name", "BEREEZE FOOTWEAR"))
        ent_store.pack(fill=tk.X, pady=(4, 12))

        tk.Label(body, text="Default Label Qty when Added:", font=NORMAL_FONT, bg="#F8FAFC", fg=DARK_COLOR).pack(anchor="w")
        ent_qty = tk.Entry(body, font=("Segoe UI", 11), width=10)
        ent_qty.insert(0, str(self.settings.get("default_label_qty", 1)))
        ent_qty.pack(anchor="w", pady=(4, 16))

        def save_and_close():
            self.settings["store_name"] = ent_store.get().strip() or "BEREEZE FOOTWEAR"
            try:
                self.settings["default_label_qty"] = max(1, int(ent_qty.get()))
            except ValueError:
                self.settings["default_label_qty"] = 1
            save_barcode_settings(self.settings)
            dlg.destroy()
            messagebox.showinfo("Saved", "Scheme settings updated successfully.", parent=self)

        tk.Button(body, text="💾 Save Customizations", font=("Segoe UI", 10, "bold"), bg=PRIMARY_COLOR, fg="white", relief=tk.FLAT, cursor="hand2", pady=6, command=save_and_close).pack(fill=tk.X)

    def _print_test_sample(self) -> None:
        """Generate and print a test sample label to verify optical printer alignment."""
        sample = [{
            "item_code": "SAMPLE-101",
            "barcode": "8901234567890",
            "item_name": "Test Sample Shoe - Black",
            "selling_price": 2999.00,
            "size": "9",
            "color": "Black",
            "label_qty": 1,
            "store_name": self.settings.get("store_name", "BEREEZE FOOTWEAR")
        }]
        printer = self.combo_printers.get()
        scheme = self.combo_scheme.get()
        layout = self.combo_layout.get()

        pages = render_print_sheet(sample, scheme=scheme, layout_type=layout)
        if pages:
            success, msg = print_to_system_printer(pages, printer, copies=1)
            if success:
                messagebox.showinfo("Test Print Sent", f"Sample label dispatched to '{printer}'.\n\n{msg}", parent=self)
            else:
                messagebox.showerror("Test Print Failed", msg, parent=self)

    def _reset_config(self) -> None:
        if messagebox.askyesno("Reset Configuration", "Reset printer choices and barcode schemes to system defaults?", parent=self):
            self.settings = {
                "selected_printer": get_default_printer(),
                "barcode_scheme": "Code 128 (Universal Standard)",
                "layout_type": "Thermal Roll - 50x25mm (2 Columns)",
                "store_name": "BEREEZE FOOTWEAR",
                "default_label_qty": 1
            }
            save_barcode_settings(self.settings)
            self._refresh_printers()
            self.combo_scheme.set(self.settings["barcode_scheme"])
            self.combo_layout.set(self.settings["layout_type"])
            self._update_status("Reset Configuration to Default.")

    def _exit_action(self) -> None:
        """Exit Program -> END."""
        if self.on_exit_cb:
            self.on_exit_cb()
        else:
            try:
                # If top level window, destroy; if embedded, try navigating back to dashboard
                top = self.winfo_toplevel()
                if isinstance(top, tk.Toplevel) and top.title() != "BEREEZE FOOTWEAR":
                    top.destroy()
                elif hasattr(top, "_navigate"):
                    top._navigate("dashboard")  # type: ignore[attr-defined]
            except Exception:
                pass
