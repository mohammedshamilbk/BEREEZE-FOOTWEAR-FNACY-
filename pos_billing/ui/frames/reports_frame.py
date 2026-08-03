# ============================================================
# pos_billing/ui/frames/reports_frame.py  (ReportGenerator.java → Python)
# ============================================================
"""Sales / Financial reports panel with normal tabular and graphical views."""

from __future__ import annotations

import csv
import logging
import tkinter as tk
from datetime import datetime, timedelta
from tkinter import filedialog, messagebox, ttk
from typing import Dict, List, Any

from ...database import dao
from ...database.models import User, Bill
from ...utils import excel_exporter
from ..constants import (
    APP_BACKGROUND, BORDER_COLOR, DARK_COLOR, HEADING_FONT,
    NORMAL_FONT, PRIMARY_COLOR, SECONDARY_COLOR, SMALL_FONT,
    SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR
)
from ..widgets import (
    create_button, create_label, create_table,
    create_success_button, create_danger_button, create_secondary_button
)

logger = logging.getLogger(__name__)


# ─────────────────────────────────────────────────────────────
# Reusable Tkinter Canvas Chart Renderer for Graphical Reports
# ─────────────────────────────────────────────────────────────
class ReportChartCanvas(tk.Frame):
    """Native Tkinter vector chart renderer supporting multiple chart models (Grouped Bar, Line & Area, Donut Breakdown)."""

    def __init__(self, parent: tk.Widget, title: str, series: List[Dict[str, Any]], chart_type: str = "pnl") -> None:
        super().__init__(parent, bg="#F8FAFC")
        self.chart_title = title
        self.series = series
        self.chart_type = chart_type
        self.chart_model = "grouped"  # "grouped", "area", "donut"

        # Top Header Container
        header_frame = tk.Frame(self, bg="#F8FAFC", padx=12, pady=6)
        header_frame.pack(fill=tk.X)

        tk.Label(
            header_frame, text=f"📊 {title}", font=("Segoe UI", 11, "bold"),
            bg="#F8FAFC", fg="#1E293B"
        ).pack(side=tk.LEFT)

        # Control Bar Right (Model Switcher & Legend)
        ctrl_box = tk.Frame(header_frame, bg="#F8FAFC")
        ctrl_box.pack(side=tk.RIGHT)

        # Model Selector Buttons
        m_box = tk.Frame(ctrl_box, bg="#E2E8F0", padx=2, pady=2)
        m_box.pack(side=tk.LEFT, padx=(0, 14))

        self.btn_grouped = tk.Button(m_box, text="📊 Bar", font=("Segoe UI", 8, "bold"),
                                     bg="#0284C7", fg="white", bd=0, padx=8, pady=3, cursor="hand2",
                                     command=lambda: self._switch_model("grouped"))
        self.btn_grouped.pack(side=tk.LEFT, padx=1)

        self.btn_area = tk.Button(m_box, text="📈 Line & Area", font=("Segoe UI", 8, "bold"),
                                  bg="#F1F5F9", fg="#475569", bd=0, padx=8, pady=3, cursor="hand2",
                                  command=lambda: self._switch_model("area"))
        self.btn_area.pack(side=tk.LEFT, padx=1)

        self.btn_donut = tk.Button(m_box, text="🍩 Donut Breakdown", font=("Segoe UI", 8, "bold"),
                                   bg="#F1F5F9", fg="#475569", bd=0, padx=8, pady=3, cursor="hand2",
                                   command=lambda: self._switch_model("donut"))
        self.btn_donut.pack(side=tk.LEFT, padx=1)

        # Legend Box
        self.legend_box = tk.Frame(ctrl_box, bg="#F8FAFC")
        self.legend_box.pack(side=tk.LEFT)
        self._update_legend()

        # Main Graphic Canvas
        self.canvas = tk.Canvas(self, bg="#FFFFFF", highlightthickness=1, highlightbackground="#E2E8F0")
        self.canvas.pack(fill=tk.BOTH, expand=True, padx=12, pady=(0, 12))
        self.canvas.bind("<Configure>", lambda e: self.after_idle(self.draw))
        self.after(50, self.draw)
        self.after(200, self.draw)

    def _switch_model(self, model: str) -> None:
        self.chart_model = model
        self.btn_grouped.config(bg="#0284C7" if model == "grouped" else "#F1F5F9", fg="white" if model == "grouped" else "#475569")
        self.btn_area.config(bg="#0284C7" if model == "area" else "#F1F5F9", fg="white" if model == "area" else "#475569")
        self.btn_donut.config(bg="#0284C7" if model == "donut" else "#F1F5F9", fg="white" if model == "donut" else "#475569")
        self._update_legend()
        self.draw()

    def _update_legend(self) -> None:
        for child in self.legend_box.winfo_children():
            child.destroy()
        if self.chart_type == "pnl":
            if self.chart_model in ("grouped", "area"):
                self._add_legend_item(self.legend_box, "Sales", "#10B981")
                self._add_legend_item(self.legend_box, "COGS", "#F59E0B")
                self._add_legend_item(self.legend_box, "Net Profit", "#3B82F6")
                self._add_legend_item(self.legend_box, "Margin % (Right)", "#8B5CF6", is_line=True)
            elif self.chart_model == "donut":
                self._add_legend_item(self.legend_box, "Net Profit", "#10B981")
                self._add_legend_item(self.legend_box, "COGS", "#F59E0B")
                self._add_legend_item(self.legend_box, "Expenses", "#EF4444")
        elif self.chart_type == "sales":
            self._add_legend_item(self.legend_box, "Daily Sales (₹)", "#10B981")
        elif self.chart_type == "inventory":
            self._add_legend_item(self.legend_box, "Stock Value (₹)", "#0EA5E9")

    def _add_legend_item(self, parent: tk.Widget, label: str, color: str, is_line: bool = False) -> None:
        f = tk.Frame(parent, bg="#F8FAFC")
        f.pack(side=tk.LEFT, padx=6)
        if is_line:
            cv = tk.Canvas(f, width=14, height=10, bg="#F8FAFC", highlightthickness=0)
            cv.create_line(0, 5, 14, 5, fill=color, width=3)
            cv.create_oval(4, 2, 10, 8, fill=color, outline="")
            cv.pack(side=tk.LEFT, padx=(0, 3))
        else:
            cv = tk.Canvas(f, width=10, height=10, bg="#F8FAFC", highlightthickness=0)
            cv.create_rectangle(0, 0, 10, 10, fill=color, outline="")
            cv.pack(side=tk.LEFT, padx=(0, 3))
        tk.Label(f, text=label, font=("Segoe UI", 8, "bold"), bg="#F8FAFC", fg="#475569").pack(side=tk.LEFT)

    def draw(self) -> None:
        """Render vector graphics onto the canvas based on selected model."""
        self.canvas.delete("all")
        self.update_idletasks()
        cw = self.canvas.winfo_width()
        ch = self.canvas.winfo_height()

        if cw <= 50 or ch <= 50:
            self.after(80, self.draw)
            return

        if not self.series:
            self.canvas.create_text(
                cw // 2, ch // 2,
                text="No report data available for the selected date filter.",
                font=("Segoe UI", 11, "bold"), fill="#64748B"
            )
            return

        if self.chart_model == "donut":
            self._draw_donut_chart(cw, ch)
        elif self.chart_model == "area":
            self._draw_area_chart(cw, ch)
        else:
            self._draw_grouped_bar_chart(cw, ch)

    # ── Model 1: Grouped Bar Chart ──────────────────────────────────────────
    def _draw_grouped_bar_chart(self, cw: int, ch: int) -> None:
        margin_l, margin_r = 75, 75
        margin_t, margin_b = 40, 50

        w_plot = cw - margin_l - margin_r
        h_plot = ch - margin_t - margin_b

        # Calculate left Y-axis scale limits (Amounts ₹)
        if self.chart_type == "pnl":
            max_val = max(
                max((s.get("sales", 0.0) for s in self.series), default=1.0),
                max((s.get("cost", 0.0) for s in self.series), default=1.0),
                max((s.get("profit", 0.0) for s in self.series), default=1.0),
                100.0
            )
        elif self.chart_type == "sales":
            max_val = max((s.get("total", 0.0) for s in self.series), default=100.0)
        else:
            max_val = max((s.get("val", 0.0) for s in self.series), default=100.0)

        max_val *= 1.18

        # Left Y-Axis Gridlines & Ticks (Amount ₹)
        num_ticks = 5
        for i in range(num_ticks + 1):
            val = (max_val / num_ticks) * i
            y = margin_t + h_plot - (i / num_ticks) * h_plot
            self.canvas.create_line(margin_l, y, cw - margin_r, y, fill="#E2E8F0", dash=(2, 4))
            self.canvas.create_text(margin_l - 8, y, text=f"₹{val:,.0f}", font=("Segoe UI", 8), fill="#64748B", anchor="e")

        # Right Y-Axis Ticks (Margin % for P&L)
        if self.chart_type == "pnl":
            for i in range(num_ticks + 1):
                pct = i * 20.0
                y = margin_t + h_plot - (i / num_ticks) * h_plot
                self.canvas.create_text(cw - margin_r + 8, y, text=f"{pct:.0f}%", font=("Segoe UI", 8, "bold"), fill="#8B5CF6", anchor="w")

        # Baseline X-Axis
        y_zero = margin_t + h_plot
        self.canvas.create_line(margin_l, y_zero, cw - margin_r, y_zero, fill="#94A3B8", width=1.5)

        # Columns
        n = len(self.series)
        group_width = w_plot / max(n, 1)

        step = max(1, int(35 / max(group_width, 1))) if group_width < 35 else 1
        margin_pts = []

        for idx, item in enumerate(self.series):
            gx = margin_l + idx * group_width
            lbl = str(item.get("date") or item.get("name") or "")

            if idx % step == 0 or idx == n - 1:
                lbl_disp = lbl[5:] if (len(lbl) == 10 and "-" in lbl) else lbl[:10]
                self.canvas.create_text(
                    gx + group_width / 2, y_zero + 16,
                    text=lbl_disp, font=("Segoe UI", 8, "bold" if n <= 15 else 7), fill="#334155"
                )

            if self.chart_type == "pnl":
                sales = float(item.get("sales", 0.0))
                cost = float(item.get("cost", 0.0))
                profit = float(item.get("profit", 0.0))
                margin_pct = float(item.get("margin", 0.0))

                sub_bars = 3
                bw = max(4.0, min(28.0, (group_width * 0.7) / sub_bars))
                start_x = gx + (group_width - (sub_bars * bw + (sub_bars - 1) * 3)) / 2

                # 1. Sales Bar (Green)
                x1 = start_x
                h_sales = (sales / max_val) * h_plot
                y1 = y_zero - h_sales
                if h_sales > 0:
                    self.canvas.create_rectangle(x1, y1, x1 + bw, y_zero, fill="#10B981", outline="")
                    if sales > 0 and bw > 10:
                        self.canvas.create_text(x1 + bw / 2, y1 - 8, text=f"₹{sales:,.0f}", font=("Segoe UI", 7), fill="#047857")

                # 2. Cost Bar (Amber)
                x2 = x1 + bw + 3
                h_cost = (cost / max_val) * h_plot
                y2 = y_zero - h_cost
                if h_cost > 0:
                    self.canvas.create_rectangle(x2, y2, x2 + bw, y_zero, fill="#F59E0B", outline="")
                    if cost > 0 and bw > 10:
                        self.canvas.create_text(x2 + bw / 2, y2 - 8, text=f"₹{cost:,.0f}", font=("Segoe UI", 7), fill="#B45309")

                # 3. Net Profit Bar (Blue / Red)
                x3 = x2 + bw + 3
                p_color = "#3B82F6" if profit >= 0 else "#EF4444"
                h_profit = (abs(profit) / max_val) * h_plot
                if h_profit > 0:
                    y3 = y_zero - h_profit if profit >= 0 else y_zero
                    y3_bot = y_zero if profit >= 0 else y_zero + h_profit
                    self.canvas.create_rectangle(x3, y3, x3 + bw, y3_bot, fill=p_color, outline="")
                    if profit != 0 and bw > 10:
                        self.canvas.create_text(x3 + bw / 2, y3 - 8 if profit >= 0 else y3_bot + 8,
                                                text=f"₹{profit:,.0f}", font=("Segoe UI", 7, "bold"), fill=p_color)

                # Store margin line point (plotted accurately against Right Y-Axis 0-100%)
                center_x = gx + group_width / 2
                margin_y = y_zero - (min(100.0, max(0.0, margin_pct)) / 100.0) * h_plot
                margin_pts.append((center_x, margin_y, margin_pct))

            elif self.chart_type in ("sales", "inventory"):
                val = float(item.get("total", 0.0) or item.get("val", 0.0))
                bw = max(6.0, min(42.0, group_width * 0.6))
                cx = gx + (group_width - bw) / 2
                h_bar = (val / max_val) * h_plot
                y_bar = y_zero - h_bar
                if h_bar > 0:
                    b_color = "#10B981" if self.chart_type == "sales" else "#0EA5E9"
                    self.canvas.create_rectangle(cx, y_bar, cx + bw, y_zero, fill=b_color, outline="")
                    if val > 0:
                        self.canvas.create_text(cx + bw / 2, y_bar - 10, text=f"₹{val:,.0f}", font=("Segoe UI", 8, "bold"), fill="#1E293B")

        # Draw Margin Line Overlay (purple) mapped cleanly to Right Y-Axis
        if self.chart_type == "pnl" and margin_pts:
            if len(margin_pts) > 1:
                for i in range(len(margin_pts) - 1):
                    pt1, pt2 = margin_pts[i], margin_pts[i + 1]
                    self.canvas.create_line(pt1[0], pt1[1], pt2[0], pt2[1], fill="#8B5CF6", width=3, smooth=True)

            for pt in margin_pts:
                px, py, mp = pt
                self.canvas.create_oval(px - 4, py - 4, px + 4, py + 4, fill="#8B5CF6", outline="white", width=1.5)
                # Text offset safely right above dot
                self.canvas.create_text(px, py - 12, text=f"{mp:.1f}%", font=("Segoe UI", 8, "bold"), fill="#6D28D9")

    # ── Model 2: Line & Area Trend Chart ──────────────────────────────────
    def _draw_area_chart(self, cw: int, ch: int) -> None:
        margin_l, margin_r = 75, 75
        margin_t, margin_b = 40, 50

        w_plot = cw - margin_l - margin_r
        h_plot = ch - margin_t - margin_b

        max_val = max(
            max((s.get("sales", 0.0) or s.get("total", 0.0) or s.get("val", 0.0) for s in self.series), default=1.0),
            max((s.get("cost", 0.0) for s in self.series), default=1.0),
            100.0
        ) * 1.18

        # Left Y-Axis Gridlines
        num_ticks = 5
        for i in range(num_ticks + 1):
            val = (max_val / num_ticks) * i
            y = margin_t + h_plot - (i / num_ticks) * h_plot
            self.canvas.create_line(margin_l, y, cw - margin_r, y, fill="#E2E8F0", dash=(2, 4))
            self.canvas.create_text(margin_l - 8, y, text=f"₹{val:,.0f}", font=("Segoe UI", 8), fill="#64748B", anchor="e")

        # Right Y-Axis Ticks (Margin %)
        if self.chart_type == "pnl":
            for i in range(num_ticks + 1):
                pct = i * 20.0
                y = margin_t + h_plot - (i / num_ticks) * h_plot
                self.canvas.create_text(cw - margin_r + 8, y, text=f"{pct:.0f}%", font=("Segoe UI", 8, "bold"), fill="#8B5CF6", anchor="w")

        y_zero = margin_t + h_plot
        self.canvas.create_line(margin_l, y_zero, cw - margin_r, y_zero, fill="#94A3B8", width=1.5)

        n = len(self.series)
        group_width = w_plot / max(n, 1)

        sales_pts, cost_pts, profit_pts, margin_pts = [], [], [], []

        for idx, item in enumerate(self.series):
            cx = margin_l + idx * group_width + group_width / 2
            lbl = str(item.get("date") or item.get("name") or "")
            lbl_disp = lbl[5:] if (len(lbl) == 10 and "-" in lbl) else lbl[:10]

            self.canvas.create_text(cx, y_zero + 16, text=lbl_disp, font=("Segoe UI", 8, "bold"), fill="#334155")

            sales = float(item.get("sales", 0.0) or item.get("total", 0.0) or item.get("val", 0.0))
            cost = float(item.get("cost", 0.0))
            profit = float(item.get("profit", 0.0))
            margin_pct = float(item.get("margin", 0.0))

            sales_pts.append((cx, y_zero - (sales / max_val) * h_plot, sales))
            cost_pts.append((cx, y_zero - (cost / max_val) * h_plot, cost))
            profit_pts.append((cx, y_zero - (profit / max_val) * h_plot, profit))
            margin_pts.append((cx, y_zero - (min(100.0, max(0.0, margin_pct)) / 100.0) * h_plot, margin_pct))

        # Helper to draw line series
        def _draw_series(pts, color, label_prefix="₹"):
            if len(pts) > 1:
                flat_coords = []
                for p in pts: flat_coords.extend([p[0], p[1]])
                self.canvas.create_line(*flat_coords, fill=color, width=3, smooth=True)

            for px, py, val in pts:
                if val > 0:
                    self.canvas.create_oval(px - 4, py - 4, px + 4, py + 4, fill=color, outline="white", width=1.5)
                    self.canvas.create_text(px, py - 10, text=f"{label_prefix}{val:,.0f}", font=("Segoe UI", 7, "bold"), fill=color)

        if self.chart_type == "pnl":
            _draw_series(sales_pts, "#10B981")
            _draw_series(cost_pts, "#F59E0B")
            _draw_series(profit_pts, "#3B82F6")

            # Margin line
            if len(margin_pts) > 1:
                flat_m = []
                for p in margin_pts: flat_m.extend([p[0], p[1]])
                self.canvas.create_line(*flat_m, fill="#8B5CF6", width=2.5, dash=(4, 2), smooth=True)

            for px, py, mp in margin_pts:
                self.canvas.create_oval(px - 3, py - 3, px + 3, py + 3, fill="#8B5CF6", outline="white")
                self.canvas.create_text(px, py + 12, text=f"{mp:.1f}%", font=("Segoe UI", 7, "bold"), fill="#6D28D9")
        else:
            b_color = "#10B981" if self.chart_type == "sales" else "#0EA5E9"
            _draw_series(sales_pts, b_color)

    # ── Model 3: Donut Breakdown Chart ──────────────────────────────────────
    def _draw_donut_chart(self, cw: int, ch: int) -> None:
        total_rev = sum(float(item.get("sales", 0.0) or item.get("total", 0.0) or item.get("val", 0.0)) for item in self.series)
        total_cost = sum(float(item.get("cost", 0.0)) for item in self.series)
        total_exp = sum(float(item.get("expenses", 0.0)) for item in self.series)
        net_profit = total_rev - total_cost - total_exp
        margin_pct = (net_profit / total_rev * 100.0) if total_rev > 0 else 0.0

        if total_rev <= 0:
            self.canvas.create_text(cw // 2, ch // 2, text="No financial data available for donut breakdown.", font=("Segoe UI", 11, "bold"), fill="#64748B")
            return

        # Layout: Left side Donut Ring, Right side Financial Scorecard
        cx = cw * 0.38
        cy = ch * 0.50
        r_outer = min(cw * 0.22, ch * 0.35)
        r_inner = r_outer * 0.60

        slices = [
            ("Net Profit", max(0.0, net_profit), "#10B981"),
            ("Cost of Goods", total_cost, "#F59E0B"),
            ("Operating Expenses", total_exp, "#EF4444"),
        ]

        start_angle = 90.0
        for name, val, color in slices:
            if val <= 0:
                continue
            extent = (val / total_rev) * 360.0
            self.canvas.create_arc(
                cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer,
                start=start_angle, extent=extent, fill=color, outline="white", width=2
            )
            start_angle += extent

        # Inner hole
        self.canvas.create_oval(cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner, fill="white", outline="#E2E8F0", width=1)

        # Center Text inside Donut
        self.canvas.create_text(cx, cy - 10, text="NET PROFIT", font=("Segoe UI", 9, "bold"), fill="#64748B")
        self.canvas.create_text(cx, cy + 10, text=f"₹{net_profit:,.2f}", font=("Segoe UI", 14, "bold"), fill="#10B981" if net_profit >= 0 else "#EF4444")
        self.canvas.create_text(cx, cy + 28, text=f"({margin_pct:.1f}% Margin)", font=("Segoe UI", 8, "bold"), fill="#8B5CF6")

        # Right Side Breakdown Scorecard Box
        box_x = cw * 0.64
        box_y = ch * 0.20
        box_w = cw * 0.32

        self.canvas.create_rectangle(box_x, box_y, box_x + box_w, box_y + 240, fill="#F8FAFC", outline="#E2E8F0", width=1)
        self.canvas.create_text(box_x + 16, box_y + 20, text="FINANCIAL BREAKDOWN", font=("Segoe UI", 10, "bold"), fill="#1E293B", anchor="w")

        rows = [
            ("💵 Total Sales Revenue", f"₹ {total_rev:,.2f}", "100.0 %", "#059669"),
            ("📦 Cost of Goods (COGS)", f"₹ {total_cost:,.2f}", f"{(total_cost/total_rev*100):.1f} %" if total_rev > 0 else "0%", "#D97706"),
            ("💸 Operating Expenses", f"₹ {total_exp:,.2f}", f"{(total_exp/total_rev*100):.1f} %" if total_rev > 0 else "0%", "#DC2626"),
            ("📈 Net Profit Margin", f"₹ {net_profit:,.2f}", f"{margin_pct:.1f} %", "#2563EB"),
        ]

        for i, (l_text, v_text, p_text, col) in enumerate(rows):
            ry = box_y + 55 + i * 42
            self.canvas.create_line(box_x + 12, ry - 10, box_x + box_w - 12, ry - 10, fill="#E2E8F0")
            self.canvas.create_text(box_x + 16, ry + 4, text=l_text, font=("Segoe UI", 9, "bold"), fill="#334155", anchor="w")
            self.canvas.create_text(box_x + box_w - 75, ry + 4, text=v_text, font=("Segoe UI", 9, "bold"), fill=col, anchor="e")
            self.canvas.create_text(box_x + box_w - 16, ry + 4, text=p_text, font=("Segoe UI", 8, "bold"), fill="#64748B", anchor="e")



# ─────────────────────────────────────────────────────────────
# Reports Main Frame
# ─────────────────────────────────────────────────────────────
class ReportsFrame(tk.Frame):
    def __init__(self, parent: tk.Widget, user: User) -> None:
        super().__init__(parent, bg=APP_BACKGROUND)
        self.user = user
        self._current_tab = "pnl"  # Default to Profit & Loss!
        self._view_mode = "table"   # "table" or "graph"
        self._current_data: list = []
        self._pnl_summary_data: list[dict] = []
        self._build()
        self._load_pnl_report()

    def _build(self) -> None:
        # Title Header
        top_hdr = tk.Frame(self, bg=APP_BACKGROUND)
        top_hdr.pack(fill=tk.X, padx=16, pady=(12, 4))

        tk.Label(top_hdr, text="📊  Financial & Sales Reports",
                 font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR).pack(side=tk.LEFT)

        # Tab Selection Buttons
        tab_bar = tk.Frame(self, bg=APP_BACKGROUND)
        tab_bar.pack(fill=tk.X, padx=16, pady=(4, 6))

        self._tabs = {}
        tabs_config = [
            ("💰 Profit & Loss", "pnl"),
            ("📈 Sales Report",  "sales"),
            ("🧾 Billing History", "bills"),
            ("📦 Inventory Value", "inventory"),
        ]
        for label, key in tabs_config:
            btn = tk.Button(
                tab_bar, text=label, font=("Segoe UI", 10, "bold"), relief=tk.FLAT,
                bg=PRIMARY_COLOR if key == self._current_tab else "#94A3B8",
                fg="white", padx=14, pady=6, cursor="hand2",
                command=lambda k=key: self._switch_tab(k),
            )
            btn.pack(side=tk.LEFT, padx=3)
            self._tabs[key] = btn

        # Filter & View Mode Controls
        filt = tk.Frame(self, bg=APP_BACKGROUND, bd=1, relief=tk.SOLID, padx=12, pady=8)
        filt.config(highlightbackground=BORDER_COLOR, highlightthickness=1, bd=0)
        filt.pack(fill=tk.X, padx=16, pady=4)

        create_label(filt, "From:").pack(side=tk.LEFT)
        self._from_var = tk.StringVar(
            value=(datetime.now() - timedelta(days=30)).strftime("%Y-%m-%d")
        )
        tk.Entry(filt, textvariable=self._from_var, width=12, font=NORMAL_FONT).pack(
            side=tk.LEFT, padx=4
        )

        create_label(filt, "To:").pack(side=tk.LEFT, padx=(10, 0))
        self._to_var = tk.StringVar(value=datetime.now().strftime("%Y-%m-%d"))
        tk.Entry(filt, textvariable=self._to_var, width=12, font=NORMAL_FONT).pack(
            side=tk.LEFT, padx=4
        )

        create_button(filt, "🔍 Apply Filter", command=self._apply_filter).pack(
            side=tk.LEFT, padx=4
        )
        create_success_button(filt, "📊 Export to Excel", command=self._export_excel).pack(
            side=tk.LEFT, padx=4
        )
        create_button(filt, "📅 Save All Days to Excel", command=self._export_all_days_excel).pack(
            side=tk.LEFT, padx=4
        )
        create_button(filt, "📥 Export CSV", command=self._export_csv).pack(
            side=tk.LEFT, padx=4
        )
        create_secondary_button(filt, "👁️ View Selected Date Bills", command=self._view_selected_date_bills).pack(
            side=tk.LEFT, padx=4
        )

        # View Mode Toggle (Normal Table vs Graphical Chart)
        view_frame = tk.Frame(filt, bg=APP_BACKGROUND)
        view_frame.pack(side=tk.RIGHT, padx=4)

        tk.Label(view_frame, text="View Mode:", font=("Segoe UI", 9, "bold"), bg=APP_BACKGROUND, fg=DARK_COLOR).pack(side=tk.LEFT, padx=(0, 6))

        self.btn_view_table = tk.Button(
            view_frame, text="📋 Normal Table", font=("Segoe UI", 9, "bold"),
            bg="#0284C7", fg="white", relief=tk.FLAT, cursor="hand2", padx=10, pady=4,
            command=lambda: self._set_view_mode("table")
        )
        self.btn_view_table.pack(side=tk.LEFT, padx=2)

        self.btn_view_graph = tk.Button(
            view_frame, text="📊 Graphical Chart", font=("Segoe UI", 9, "bold"),
            bg="#64748B", fg="white", relief=tk.FLAT, cursor="hand2", padx=10, pady=4,
            command=lambda: self._set_view_mode("graph")
        )
        self.btn_view_graph.pack(side=tk.LEFT, padx=2)

        # Content Main Container (holds KPI cards + Table/Chart)
        self._content_container = tk.Frame(self, bg=APP_BACKGROUND)
        self._content_container.pack(fill=tk.BOTH, expand=True, padx=16, pady=4)

        # Summary Bar Footer
        sum_bar = tk.Frame(self, bg=APP_BACKGROUND)
        sum_bar.pack(fill=tk.X, padx=16, pady=4)

        self._summary_var = tk.StringVar(value="")
        tk.Label(sum_bar, textvariable=self._summary_var, font=("Segoe UI", 10, "bold"),
                 bg=APP_BACKGROUND, fg=PRIMARY_COLOR).pack(side=tk.LEFT)

        tk.Label(sum_bar, text="💡 Tip: Double-click any Date row to view all bills for that day!",
                 font=SMALL_FONT, bg=APP_BACKGROUND, fg="#64748B").pack(side=tk.RIGHT)

    # ── View Mode & Tab Switching ──────────────────────────────────────────
    def _set_view_mode(self, mode: str) -> None:
        self._view_mode = mode
        if mode == "table":
            self.btn_view_table.config(bg="#0284C7", fg="white")
            self.btn_view_graph.config(bg="#64748B", fg="white")
        else:
            self.btn_view_table.config(bg="#64748B", fg="white")
            self.btn_view_graph.config(bg="#0EA5E9", fg="white")
        self._switch_tab(self._current_tab)

    def _switch_tab(self, key: str) -> None:
        self._current_tab = key
        for k, btn in self._tabs.items():
            btn.config(bg=PRIMARY_COLOR if k == key else "#94A3B8")

        if key == "pnl":
            self._load_pnl_report()
        elif key == "sales":
            self._load_sales_report()
        elif key == "bills":
            self._load_billing_history()
        elif key == "inventory":
            self._load_inventory_report()

    def _apply_filter(self) -> None:
        self._switch_tab(self._current_tab)

    # ── Profit & Loss Report Loader ────────────────────────────────────────
    def _load_pnl_report(self) -> None:
        """Calculate complete Profit & Loss details (Sales, COGS, Expenses, Net Profit, Margin %)."""
        bills = self._filtered_bills()

        from collections import defaultdict
        daily_revenue: dict[str, float] = defaultdict(float)
        daily_cogs: dict[str, float] = defaultdict(float)
        daily_cash: dict[str, float] = defaultdict(float)
        daily_upi: dict[str, float] = defaultdict(float)
        daily_bills_cnt: dict[str, int] = defaultdict(int)

        for b in bills:
            day = b.bill_date.strftime("%Y-%m-%d")
            daily_revenue[day] += b.total_amount
            daily_bills_cnt[day] += 1

            pmode = (b.payment_mode or "CASH").upper()
            if pmode == "CASH":
                daily_cash[day] += b.total_amount
            elif pmode in ("UPI", "ONLINE"):
                daily_upi[day] += b.total_amount

            b_cost = 0.0
            for item in b.bill_items:
                cost_price = item.purchase_price
                if cost_price <= 0:
                    cost_price = item.unit_price * 0.60
                b_cost += cost_price * item.quantity
            daily_cogs[day] += b_cost

        # Fetch expenses for the period
        all_expenses = dao.get_all_expenses()
        daily_expenses: dict[str, float] = defaultdict(float)
        for e in all_expenses:
            e_day = e.expense_date[:10]
            daily_expenses[e_day] += e.amount

        # Build chronological list
        all_dates = sorted(set(daily_revenue.keys()) | set(daily_cogs.keys()) | set(daily_expenses.keys()))
        self._pnl_summary_data = []
        table_rows = []

        total_rev = 0.0
        total_cogs = 0.0
        total_exp = 0.0
        total_cash_sales = sum(daily_cash.values())
        total_upi_sales = sum(daily_upi.values())

        for d in all_dates:
            rev = daily_revenue[d]
            cost = daily_cogs[d]
            exp = daily_expenses[d]
            profit = rev - cost - exp
            margin = (profit / rev * 100.0) if rev > 0 else 0.0
            cnt = daily_bills_cnt[d]

            total_rev += rev
            total_cogs += cost
            total_exp += exp

            self._pnl_summary_data.append({
                "date": d,
                "sales": rev,
                "cost": cost,
                "expenses": exp,
                "profit": profit,
                "margin": margin,
                "bills": cnt
            })

            table_rows.append((
                d,
                f"₹ {rev:,.2f}",
                f"₹ {cost:,.2f}",
                f"₹ {exp:,.2f}",
                f"₹ {profit:,.2f}",
                f"{margin:.1f} %",
                cnt
            ))

        total_profit = total_rev - total_cogs - total_exp
        total_margin = (total_profit / total_rev * 100.0) if total_rev > 0 else 0.0

        self._current_data = table_rows
        self._summary_var.set(
            f"Revenue: ₹ {total_rev:,.2f}  |  COGS: ₹ {total_cogs:,.2f}  |  Expenses: ₹ {total_exp:,.2f}  |  Net Profit: ₹ {total_profit:,.2f} ({total_margin:.1f}%)"
        )

        # Clear Container
        for w in self._content_container.winfo_children():
            w.destroy()

        if self._view_mode == "graph":
            # Render Graphical View
            chart = ReportChartCanvas(
                self._content_container,
                title="Profit & Loss Graphical Comparison (Revenue vs. Cost vs. Expenses vs. Net Profit)",
                series=self._pnl_summary_data,
                chart_type="pnl"
            )
            chart.pack(fill=tk.BOTH, expand=True)
        else:
            # Render Normal Table View + KPI Summary Header Cards
            cards_frame = tk.Frame(self._content_container, bg=APP_BACKGROUND)
            cards_frame.pack(fill=tk.X, pady=(0, 10))

            self._create_kpi_card(cards_frame, "💵 CASH SALES", f"₹ {total_cash_sales:,.2f}", "#D97706", "#FEF3C7").pack(side=tk.LEFT, fill=tk.X, expand=True, padx=3)
            self._create_kpi_card(cards_frame, "📱 UPI SALES", f"₹ {total_upi_sales:,.2f}", "#0284C7", "#E0F2FE").pack(side=tk.LEFT, fill=tk.X, expand=True, padx=3)
            self._create_kpi_card(cards_frame, "💸 EXPENSES", f"₹ {total_exp:,.2f}", "#DC2626", "#FEE2E2").pack(side=tk.LEFT, fill=tk.X, expand=True, padx=3)
            self._create_kpi_card(cards_frame, "📈 NET PROFIT", f"₹ {total_profit:,.2f}", "#10B981" if total_profit >= 0 else "#EF4444", "#ECFDF5" if total_profit >= 0 else "#FEF2F2").pack(side=tk.LEFT, fill=tk.X, expand=True, padx=3)

            cols = [
                ("date", 120, "Date"),
                ("rev", 130, "Sales Revenue (₹)"),
                ("cogs", 130, "Cost of Goods (₹)"),
                ("exp", 120, "Expenses (₹)"),
                ("profit", 140, "Net Profit / Loss (₹)"),
                ("margin", 100, "Margin (%)"),
                ("bills", 80, "Bills"),
            ]
            self._render_table(cols, table_rows)

    def _create_kpi_card(self, parent: tk.Widget, label: str, value: str, accent_color: str, bg_color: str) -> tk.Frame:
        card = tk.Frame(parent, bg=bg_color, bd=1, relief=tk.SOLID, padx=14, pady=10)
        card.config(highlightbackground=accent_color, highlightthickness=2, bd=0)
        tk.Label(card, text=label, font=("Segoe UI", 9, "bold"), bg=bg_color, fg=DARK_COLOR).pack(anchor=tk.W)
        tk.Label(card, text=value, font=("Segoe UI", 15, "bold"), bg=bg_color, fg=accent_color).pack(anchor=tk.W, pady=(4, 0))
        return card

    # ── Sales Report Loader ────────────────────────────────────────────────
    def _load_sales_report(self) -> None:
        """Summary: total sales per day in the date range."""
        bills = self._filtered_bills()

        from collections import defaultdict
        daily: dict[str, float] = defaultdict(float)
        for b in bills:
            day = b.bill_date.strftime("%Y-%m-%d")
            daily[day] += b.total_amount

        sorted_days = sorted(daily.items())
        self._current_data = [(day, f"₹ {amt:,.2f}") for day, amt in sorted_days]
        grand = sum(b.total_amount for b in bills)

        self._summary_var.set(f"Total Sales: ₹ {grand:,.2f}  |  Bills: {len(bills)}")

        for w in self._content_container.winfo_children():
            w.destroy()

        if self._view_mode == "graph":
            graph_data = [{"date": day, "total": amt} for day, amt in sorted_days]
            chart = ReportChartCanvas(
                self._content_container,
                title="Daily Sales Revenue Overview",
                series=graph_data,
                chart_type="sales"
            )
            chart.pack(fill=tk.BOTH, expand=True)
        else:
            cols = [("date", 200, "Date"), ("total", 200, "Total Sales (₹)")]
            self._render_table(cols, self._current_data)

    # ── Billing History Loader ─────────────────────────────────────────────
    def _load_billing_history(self) -> None:
        bills = self._filtered_bills()
        self._current_data = [
            (b.bill_number,
             b.bill_date.strftime("%Y-%m-%d %H:%M"),
             b.customer_name or "—",
             f"₹ {b.total_amount:,.2f}",
             b.payment_mode or "—",
             b.status)
            for b in bills
        ]
        total = sum(b.total_amount for b in bills)
        self._summary_var.set(f"Total Billing Amount: ₹ {total:,.2f}  |  Bills: {len(bills)}")

        for w in self._content_container.winfo_children():
            w.destroy()

        if self._view_mode == "graph":
            from collections import defaultdict
            daily_bills: dict[str, float] = defaultdict(float)
            for b in bills:
                daily_bills[b.bill_date.strftime("%Y-%m-%d")] += b.total_amount
            graph_data = [{"date": day, "total": amt} for day, amt in sorted(daily_bills.items())]
            chart = ReportChartCanvas(
                self._content_container,
                title="Billing History Distribution",
                series=graph_data,
                chart_type="sales"
            )
            chart.pack(fill=tk.BOTH, expand=True)
        else:
            cols = [
                ("bill_no",  130, "Bill No"),
                ("date",     140, "Date"),
                ("customer", 160, "Customer"),
                ("amount",   110, "Amount (₹)"),
                ("mode",      90, "Payment"),
                ("status",    90, "Status"),
            ]
            self._render_table(cols, self._current_data)

    # ── Inventory Report Loader ───────────────────────────────────────────
    def _load_inventory_report(self) -> None:
        items = dao.get_all_items()
        self._current_data = [
            (i.item_code, i.item_name, i.category,
             i.stock_quantity, f"₹ {i.selling_price:,.2f}",
             f"₹ {i.stock_quantity * i.selling_price:,.2f}")
            for i in items
        ]
        total_val = sum(i.stock_quantity * i.selling_price for i in items)
        self._summary_var.set(f"Inventory Total Value: ₹ {total_val:,.2f}  |  Items: {len(items)}")

        for w in self._content_container.winfo_children():
            w.destroy()

        if self._view_mode == "graph":
            top_items = sorted(items, key=lambda x: x.stock_quantity * x.selling_price, reverse=True)[:10]
            graph_data = [{"name": i.item_name[:12], "val": i.stock_quantity * i.selling_price} for i in top_items]
            chart = ReportChartCanvas(
                self._content_container,
                title="Top 10 Inventory Stock Values",
                series=graph_data,
                chart_type="inventory"
            )
            chart.pack(fill=tk.BOTH, expand=True)
        else:
            cols = [
                ("code",    100, "Code"),
                ("name",    180, "Name"),
                ("cat",      100, "Category"),
                ("stock",    80, "Stock"),
                ("price",    100, "Price (₹)"),
                ("value",   120, "Total Value (₹)"),
            ]
            self._render_table(cols, self._current_data)

    # ── Table Renderer Helper ──────────────────────────────────────────────
    def _render_table(self, cols: list, rows: list) -> None:
        self._current_headers = [c[2] for c in cols]
        tbl_frame = tk.Frame(self._content_container, bg=APP_BACKGROUND)
        tbl_frame.pack(fill=tk.BOTH, expand=True)

        tree = create_table(tbl_frame, cols, height=18)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        vsb = ttk.Scrollbar(tbl_frame, orient=tk.VERTICAL, command=tree.yview)
        tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        for row in rows:
            tree.insert("", tk.END, values=row)

        def _on_table_action(e=None):
            try:
                sel = tree.selection()
                if not sel: return
                vals = tree.item(sel[0], "values")
                if not vals or len(vals) == 0: return

                val0 = str(vals[0]).strip()
                # Case 1: Double clicking a Bill Number (INV-...)
                if val0.startswith("INV-"):
                    bill = dao.get_bill_by_number(val0)
                    if bill:
                        from ..app import _ReceiptDlg
                        _ReceiptDlg(self, bill)
                # Case 2: Double clicking a Date row (YYYY-MM-DD)
                elif len(val0) == 10 and val0.count("-") == 2:
                    _DailyBillsDialog(self, val0)
            except Exception as exc:
                logger.debug("Table action error: %s", exc)

        tree.bind("<Double-1>", _on_table_action)
        tree.bind("<Return>", _on_table_action)
        self._current_tree = tree

    # ── Date Filter & Excel / CSV Export Helpers ────────────────────────────────────
    def _filtered_bills(self) -> List[Bill]:
        try:
            from_dt = datetime.strptime(self._from_var.get().strip(), "%Y-%m-%d")
        except ValueError:
            from_dt = datetime.now() - timedelta(days=30)
        try:
            to_dt = datetime.strptime(self._to_var.get().strip(), "%Y-%m-%d").replace(
                hour=23, minute=59, second=59
            )
        except ValueError:
            to_dt = datetime.now()

        all_bills = dao.get_all_bills(limit=2000)
        filtered = []
        for b in all_bills:
            b_dt = b.bill_date
            if isinstance(b_dt, str):
                for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d"):
                    try:
                        b_dt = datetime.strptime(b_dt, fmt)
                        break
                    except ValueError:
                        continue
            if not isinstance(b_dt, datetime):
                b_dt = datetime.now()

            if from_dt <= b_dt <= to_dt and b.status == "COMPLETED":
                filtered.append(b)
        return filtered

    def _export_excel(self) -> None:
        """Export current active report tab data to Excel (.xlsx)."""
        path = filedialog.asksaveasfilename(
            defaultextension=".xlsx",
            filetypes=[("Excel Files (*.xlsx)", "*.xlsx"), ("CSV Files (*.csv)", "*.csv")],
            title=f"Export {self._current_tab.upper()} Report to Excel",
            initialfile=f"report_{self._current_tab}_{datetime.now().strftime('%Y%m%d')}.xlsx"
        )
        if not path:
            return
        try:
            tab_titles = {
                "pnl": "Profit & Loss Daily Report",
                "sales": "Daily Sales Revenue Summary",
                "bills": "Bills Master Log",
                "inventory": "Inventory Stock Report"
            }
            title = tab_titles.get(self._current_tab, "Report Data")
            headers = getattr(self, "_current_headers", ["Col1", "Col2", "Col3"])
            summary = self._summary_var.get()
            out_path = excel_exporter.export_table_to_excel(
                filepath=path,
                title=title,
                headers=headers,
                rows=self._current_data,
                sheet_name=self._current_tab.upper(),
                summary_text=summary
            )
            messagebox.showinfo("Export Successful", f"Report saved successfully to:\n{out_path}", parent=self)
        except Exception as exc:
            logger.error("Excel export error: %s", exc)
            messagebox.showerror("Export Error", f"Could not export report: {exc}", parent=self)

    def _export_all_days_excel(self) -> None:
        """Export EVERY DAY's sales report, bills log, expenses, and inventory to a multi-sheet Excel Workbook."""
        from_d = self._from_var.get().strip()
        to_d = self._to_var.get().strip()
        path = filedialog.asksaveasfilename(
            defaultextension=".xlsx",
            filetypes=[("Excel Files (*.xlsx)", "*.xlsx"), ("CSV Files (*.csv)", "*.csv")],
            title="Save Every Day's Sales to Excel Workbook",
            initialfile=f"all_days_sales_{datetime.now().strftime('%Y%m%d')}.xlsx"
        )
        if not path:
            return
        try:
            out_path = excel_exporter.export_all_days_sales_to_excel(
                filepath=path,
                from_date=from_d,
                to_date=to_d
            )
            messagebox.showinfo(
                "Export Complete",
                f"Every day's sales report, bills log, expenses, and inventory have been saved to Excel:\n\n{out_path}",
                parent=self
            )
        except Exception as exc:
            logger.error("All days export error: %s", exc)
            messagebox.showerror("Export Error", f"Could not export all days to Excel: {exc}", parent=self)

    def _export_csv(self) -> None:
        path = filedialog.asksaveasfilename(
            defaultextension=".csv",
            filetypes=[("CSV files", "*.csv")],
            title="Export Report to CSV",
        )
        if not path:
            return
        try:
            with open(path, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerows(self._current_data)
            messagebox.showinfo("Exported", f"Report data exported successfully to:\n{path}", parent=self)
        except Exception as exc:
            messagebox.showerror("Export Error", str(exc), parent=self)

    def _view_selected_date_bills(self) -> None:
        """View bills for the date currently selected in the report table."""
        if not hasattr(self, "_current_tree") or not self._current_tree:
            messagebox.showinfo("Select Date", "Please select a date row from the table first.", parent=self)
            return

        sel = self._current_tree.selection()
        if not sel:
            messagebox.showinfo("Select Date", "Please select a date row from the table first.", parent=self)
            return

        vals = self._current_tree.item(sel[0], "values")
        if not vals or len(vals) == 0:
            return

        val0 = str(vals[0]).strip()
        if val0.startswith("INV-"):
            bill = dao.get_bill_by_number(val0)
            if bill:
                from ..app import _ReceiptDlg
                _ReceiptDlg(self, bill)
        elif len(val0) == 10 and val0.count("-") == 2:
            _DailyBillsDialog(self, val0)
        else:
            messagebox.showinfo("Select Date", f"Selected row '{val0}' is not a valid date row.", parent=self)


class _DailyBillsDialog(tk.Toplevel):
    """Dialog to display all bills generated on a specific date when double-clicking a date row in reports."""

    def __init__(self, parent: tk.Widget, date_str: str) -> None:
        super().__init__(parent)
        self.title(f"Bills for Date {date_str}")
        self.configure(bg=APP_BACKGROUND)
        self.geometry("720x480")
        self.resizable(False, False)
        self.transient(parent)
        self.grab_set()

        self.date_str = date_str
        self._bills: List[Bill] = []
        self._tree: Optional[ttk.Treeview] = None

        # Fetch bills for date_str
        all_bills = dao.get_all_bills(limit=2000)
        for b in all_bills:
            b_day = b.bill_date.strftime("%Y-%m-%d") if hasattr(b.bill_date, "strftime") else str(b.bill_date)[:10]
            if b_day == date_str:
                self._bills.append(b)

        total_rev = sum(b.total_amount for b in self._bills)

        # Header Title
        tk.Label(
            self, text=f"📋  Bills for Date {date_str}",
            font=HEADING_FONT, bg=APP_BACKGROUND, fg=DARK_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(16, 4))

        tk.Label(
            self,
            text=f"Total Bills: {len(self._bills)}  |  Total Revenue: ₹ {total_rev:,.2f}  |  💡 Double-click any bill row to view full receipt",
            font=SMALL_FONT, bg=APP_BACKGROUND, fg=PRIMARY_COLOR,
        ).pack(anchor=tk.W, padx=20, pady=(0, 10))

        # Table container
        tbl_frame = tk.Frame(self, bg=APP_BACKGROUND)
        tbl_frame.pack(fill=tk.BOTH, expand=True, padx=20)

        cols = [
            ("bill_no",   140, "Bill No"),
            ("time",      110, "Date / Time"),
            ("customer",  170, "Customer"),
            ("amount",    110, "Amount (₹)"),
            ("mode",       90, "Payment"),
            ("status",     90, "Status"),
        ]
        self._tree = create_table(tbl_frame, cols, height=10)
        self._tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        vsb = ttk.Scrollbar(tbl_frame, orient=tk.VERTICAL, command=self._tree.yview)
        self._tree.configure(yscrollcommand=vsb.set)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)

        for b in self._bills:
            self._tree.insert("", tk.END, values=(
                b.bill_number,
                b.bill_date.strftime("%Y-%m-%d %H:%M") if hasattr(b.bill_date, "strftime") else str(b.bill_date)[:16],
                b.customer_name or "Walk-In Customer",
                f"{b.total_amount:,.2f}",
                b.payment_mode or "—",
                b.status,
            ))

        self._tree.bind("<Double-1>", lambda e: self._open_selected_bill())
        self._tree.bind("<Return>", lambda e: self._open_selected_bill())

        # Buttons Footer
        btn_box = tk.Frame(self, bg=APP_BACKGROUND, pady=12, padx=20)
        btn_box.pack(fill=tk.X)

        create_success_button(btn_box, "👁️ View & Print Selected Receipt", command=self._open_selected_bill).pack(side=tk.LEFT, padx=(0, 8))
        create_button(btn_box, "📊 Export to Excel", command=self._export_excel).pack(side=tk.LEFT, padx=4)
        create_button(btn_box, "📥 Export CSV", command=self._export_csv).pack(side=tk.LEFT, padx=4)
        create_secondary_button(btn_box, "✖ Close", command=self.destroy).pack(side=tk.RIGHT)

    def _get_selected_bill_no(self) -> Optional[str]:
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
        bill_no = self._get_selected_bill_no()
        if not bill_no:
            messagebox.showwarning("Select Bill", "Please select a bill row first.", parent=self)
            return
        bill = dao.get_bill_by_number(bill_no)
        if bill:
            from ..app import _ReceiptDlg
            _ReceiptDlg(self, bill)
        else:
            messagebox.showerror("Error", f"Bill '{bill_no}' not found.", parent=self)

    def _export_excel(self) -> None:
        if not self._tree:
            return
        path = filedialog.asksaveasfilename(
            defaultextension=".xlsx",
            filetypes=[("Excel Files (*.xlsx)", "*.xlsx"), ("CSV Files (*.csv)", "*.csv")],
            title=f"Export Bills for {self.date_str} to Excel",
            initialfile=f"bills_{self.date_str}.xlsx"
        )
        if not path:
            return
        try:
            headers = ["Bill No", "Date / Time", "Customer", "Amount (₹)", "Payment Mode", "Status"]
            rows = [self._tree.item(item_id, "values") for item_id in self._tree.get_children()]
            out_path = excel_exporter.export_table_to_excel(
                filepath=path,
                title=f"Bills for Date {self.date_str}",
                headers=headers,
                rows=rows,
                sheet_name="Daily Bills"
            )
            messagebox.showinfo("Exported", f"Bills for {self.date_str} exported successfully to:\n{out_path}", parent=self)
        except Exception as exc:
            messagebox.showerror("Export Error", str(exc), parent=self)

    def _export_csv(self) -> None:
        if not self._tree:
            return
        path = filedialog.asksaveasfilename(
            defaultextension=".csv",
            filetypes=[("CSV files", "*.csv")],
            title=f"Export Bills for {self.date_str} to CSV",
            initialfile=f"bills_{self.date_str}.csv"
        )
        if not path:
            return
        try:
            with open(path, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow(["Bill No", "Date / Time", "Customer", "Amount (₹)", "Payment Mode", "Status"])
                for item_id in self._tree.get_children():
                    writer.writerow(self._tree.item(item_id, "values"))
            messagebox.showinfo("Exported", f"Bills for {self.date_str} exported successfully to:\n{path}", parent=self)
        except Exception as exc:
            messagebox.showerror("Export Error", str(exc), parent=self)
