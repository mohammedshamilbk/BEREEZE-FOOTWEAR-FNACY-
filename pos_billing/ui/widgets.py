# ============================================================
# pos_billing/ui/widgets.py  (UIUtils.java → Python)
# ============================================================
"""
Reusable Tkinter widget factory functions.

Mirrors Java's UIUtils static methods so all frames can call
create_button(), create_label(), etc. consistently.
"""

from __future__ import annotations

import tkinter as tk
from tkinter import ttk
from typing import Callable, Optional

import os
from pathlib import Path

from .constants import (
    ACCENT_COLOR, APP_BACKGROUND, BORDER_COLOR, DANGER_COLOR, DARK_COLOR,
    HEADING_FONT, NORMAL_FONT, PRIMARY_COLOR, SMALL_FONT,
    SUCCESS_COLOR, TEXT_ON_APP_BG, TEXT_ON_DANGER, TEXT_ON_PRIMARY,
    WARNING_COLOR,
)

_BASE_DIR = Path(__file__).resolve().parent.parent.parent
_ICON_ICO = _BASE_DIR / "pos_billing" / "assets" / "app_icon.ico"
_ICON_PNG = _BASE_DIR / "pos_billing" / "assets" / "app_icon.png"
_cached_icon_photo = None


def apply_app_icon(win: tk.Misc) -> None:
    """Apply the official Bereeze Footwear Fancy app icon to any Tk root or Toplevel window."""
    global _cached_icon_photo

    # Windows AppUserModelID to force taskbar icon
    try:
        import ctypes
        myappid = "bereeze.footwear.pos.1.0"
        ctypes.windll.shell32.SetCurrentProcessExplicitAppUserModelID(myappid)
    except Exception:
        pass

    try:
        if os.path.exists(_ICON_PNG):
            from PIL import Image, ImageTk
            if _cached_icon_photo is None:
                img = Image.open(_ICON_PNG)
                _cached_icon_photo = ImageTk.PhotoImage(img)
            win.iconphoto(True, _cached_icon_photo)

        if os.path.exists(_ICON_ICO):
            try:
                win.iconbitmap(str(_ICON_ICO))
            except Exception:
                pass
    except Exception:
        pass


# ─── Labels ────────────────────────────────────────────────────────────────
def create_label(parent: tk.Widget, text: str,
                 font=NORMAL_FONT, fg: str = TEXT_ON_APP_BG,
                 bg: str = APP_BACKGROUND, **kwargs) -> tk.Label:
    return tk.Label(parent, text=text, font=font, fg=fg, bg=bg, **kwargs)


def create_heading(parent: tk.Widget, text: str, **kwargs) -> tk.Label:
    return create_label(parent, text, font=HEADING_FONT, **kwargs)


# ─── Entry / Password Fields ───────────────────────────────────────────────
def create_entry(parent: tk.Widget, width: int = 30, **kwargs) -> tk.Entry:
    e = tk.Entry(parent, width=width, font=NORMAL_FONT,
                 relief=tk.FLAT, highlightthickness=1,
                 highlightbackground=BORDER_COLOR,
                 highlightcolor=PRIMARY_COLOR, **kwargs)
    return e


def create_password_entry(parent: tk.Widget, width: int = 30, **kwargs) -> tk.Entry:
    return create_entry(parent, width=width, show="•", **kwargs)


# ─── Buttons ───────────────────────────────────────────────────────────────
def _make_button(parent: tk.Widget, text: str,
                 bg: str, fg: str, command: Optional[Callable] = None,
                 **kwargs) -> tk.Button:
    padx = kwargs.pop("padx", 14)
    pady = kwargs.pop("pady", 6)
    font = kwargs.pop("font", NORMAL_FONT)
    relief = kwargs.pop("relief", tk.FLAT)
    cursor = kwargs.pop("cursor", "hand2")
    bd = kwargs.pop("bd", 0)
    btn = tk.Button(
        parent, text=text, bg=bg, fg=fg,
        font=font, relief=relief, cursor=cursor,
        activebackground=_darken(bg, 0.8), activeforeground=fg,
        padx=padx, pady=pady, command=command, bd=bd, **kwargs,
    )
    hover_bg = _lighten(bg, 1.15)
    press_bg = _darken(bg, 0.80)

    # Smooth Animated Hover & Click feedback
    btn.bind("<Enter>", lambda e: btn.config(bg=hover_bg))
    btn.bind("<Leave>", lambda e: btn.config(bg=bg))
    btn.bind("<Button-1>", lambda e: btn.config(bg=press_bg))
    btn.bind("<ButtonRelease-1>", lambda e: btn.config(bg=hover_bg))
    return btn


def create_button(parent: tk.Widget, text: str,
                  command: Optional[Callable] = None, **kwargs) -> tk.Button:
    return _make_button(parent, text, PRIMARY_COLOR, TEXT_ON_PRIMARY, command, **kwargs)


def create_success_button(parent: tk.Widget, text: str,
                           command: Optional[Callable] = None, **kwargs) -> tk.Button:
    return _make_button(parent, text, SUCCESS_COLOR, TEXT_ON_PRIMARY, command, **kwargs)


def create_danger_button(parent: tk.Widget, text: str,
                          command: Optional[Callable] = None, **kwargs) -> tk.Button:
    return _make_button(parent, text, DANGER_COLOR, TEXT_ON_DANGER, command, **kwargs)


def create_warning_button(parent: tk.Widget, text: str,
                           command: Optional[Callable] = None, **kwargs) -> tk.Button:
    return _make_button(parent, text, WARNING_COLOR, DARK_COLOR, command, **kwargs)


def create_secondary_button(parent: tk.Widget, text: str,
                             command: Optional[Callable] = None, **kwargs) -> tk.Button:
    return _make_button(parent, text, BORDER_COLOR, DARK_COLOR, command, **kwargs)


# ─── Checkbox ──────────────────────────────────────────────────────────────
def create_checkbox(parent: tk.Widget, text: str,
                    variable: Optional[tk.BooleanVar] = None, **kwargs) -> tk.Checkbutton:
    var = variable or tk.BooleanVar()
    cb = tk.Checkbutton(parent, text=text, variable=var,
                        font=NORMAL_FONT, bg=APP_BACKGROUND,
                        fg=TEXT_ON_APP_BG, activebackground=APP_BACKGROUND, **kwargs)
    cb._var = var  # type: ignore[attr-defined]
    return cb


# ─── Combobox ──────────────────────────────────────────────────────────────
def create_combobox(parent: tk.Widget, values: list,
                    width: int = 20, **kwargs) -> ttk.Combobox:
    style = ttk.Style()
    style.configure("Custom.TCombobox", fieldbackground="white",
                    foreground=DARK_COLOR, font=NORMAL_FONT)
    cb = ttk.Combobox(parent, values=values, width=width,
                      style="Custom.TCombobox", **kwargs)
    return cb


# ─── Scrollable Treeview / Table ───────────────────────────────────────────
def create_table(parent: tk.Widget, columns: list[tuple[str, int, str]],
                 height: int = 15) -> ttk.Treeview:
    """
    Create a styled Treeview table with high-contrast headers.

    Args:
        columns: List of (column_id, width, heading) tuples.
        height:  Number of visible rows.

    Returns:
        Configured Treeview widget.
    """
    style = ttk.Style()
    try:
        style.theme_use("clam")
    except Exception:
        pass

    # Configure default Treeview and Custom.Treeview styles
    style.configure("Treeview",
                    background="white", foreground=DARK_COLOR,
                    rowheight=28, fieldbackground="white",
                    font=NORMAL_FONT, borderwidth=1)
    style.configure("Treeview.Heading",
                    background=PRIMARY_COLOR, foreground=TEXT_ON_PRIMARY,
                    font=("Segoe UI", 10, "bold"), borderwidth=1, relief="flat")
    style.map("Treeview",
              background=[("selected", PRIMARY_COLOR)],
              foreground=[("selected", TEXT_ON_PRIMARY)])
    style.map("Treeview.Heading",
              background=[("active", ACCENT_COLOR)],
              foreground=[("active", TEXT_ON_PRIMARY)])

    style.configure("Custom.Treeview",
                    background="white", foreground=DARK_COLOR,
                    rowheight=28, fieldbackground="white",
                    font=NORMAL_FONT, borderwidth=1)
    style.configure("Custom.Treeview.Heading",
                    background=PRIMARY_COLOR, foreground=TEXT_ON_PRIMARY,
                    font=("Segoe UI", 10, "bold"), borderwidth=1, relief="flat")
    style.map("Custom.Treeview",
              background=[("selected", PRIMARY_COLOR)],
              foreground=[("selected", TEXT_ON_PRIMARY)])
    style.map("Custom.Treeview.Heading",
              background=[("active", ACCENT_COLOR)],
              foreground=[("active", TEXT_ON_PRIMARY)])

    col_ids = [c[0] for c in columns]
    tree = ttk.Treeview(parent, columns=col_ids, show="headings",
                        height=height, style="Custom.Treeview")
    for col_id, width, heading in columns:
        tree.heading(col_id, text=heading)
        tree.column(col_id, width=width, anchor=tk.CENTER)
    return tree


def add_scrollbar(parent: tk.Widget, tree: ttk.Treeview) -> None:
    """Attach a vertical scrollbar to a Treeview."""
    vsb = ttk.Scrollbar(parent, orient=tk.VERTICAL, command=tree.yview)
    tree.configure(yscrollcommand=vsb.set)
    vsb.pack(side=tk.RIGHT, fill=tk.Y)


# ─── Section Frame ─────────────────────────────────────────────────────────
def create_section_frame(parent: tk.Widget, title: str,
                          bg: str = APP_BACKGROUND) -> tk.LabelFrame:
    return tk.LabelFrame(parent, text=f"  {title}  ",
                         font=NORMAL_FONT, bg=bg,
                         fg=PRIMARY_COLOR, relief=tk.GROOVE, bd=1)


# ─── Colour utilities ──────────────────────────────────────────────────────
def center_window(win: tk.Misc, width: Optional[int] = None, height: Optional[int] = None) -> None:
    """Center a Tk root or Toplevel window on the screen reliably across display configurations."""
    try:
        win.update_idletasks()
        w = width or win.winfo_width()
        h = height or win.winfo_height()
        if w <= 1:
            w = 900
        if h <= 1:
            h = 700
        sw = win.winfo_screenwidth()
        sh = win.winfo_screenheight()
        x = max(0, (sw - w) // 2)
        y = max(0, (sh - h) // 2)
        win.geometry(f"{w}x{h}+{x}+{y}")
    except Exception:
        pass


def _darken(hex_color: str, factor: float = 0.85) -> str:
    """Return a slightly darker shade of a hex colour."""
    try:
        h = hex_color.lstrip("#")
        r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
        r = max(0, int(r * factor))
        g = max(0, int(g * factor))
        b = max(0, int(b * factor))
        return f"#{r:02x}{g:02x}{b:02x}"
    except Exception:
        return hex_color


def _lighten(hex_color: str, factor: float = 1.15) -> str:
    """Return a slightly lighter shade of a hex colour for smooth hover animations."""
    try:
        h = hex_color.lstrip("#")
        r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
        r = min(255, int(r * factor))
        g = min(255, int(g * factor))
        b = min(255, int(b * factor))
        return f"#{r:02x}{g:02x}{b:02x}"
    except Exception:
        return hex_color
