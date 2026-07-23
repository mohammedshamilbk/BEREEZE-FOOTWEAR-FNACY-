# ============================================================
# pos_billing/utils/qr_generator.py
# ============================================================
"""
Dynamic UPI Payment QR Code Generator.

Decodes merchant VPA (UPI ID) from existing static QR assets (`store_qr.jpg` / `.png`)
or configuration files, and dynamically generates high-resolution UPI QR codes with
the exact bill amount (`&am=...`) embedded. When customers scan these dynamic QR codes
using Google Pay, PhonePe, Paytm, or BHIM, the exact bill amount is pre-filled automatically.
"""

from __future__ import annotations

import logging
import os
import urllib.parse
import tkinter as tk
from tkinter import ttk
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from PIL import Image
    from ..database.models import Bill

logger = logging.getLogger(__name__)


def get_store_upi_id() -> tuple[str, str]:
    """
    Retrieve the store's VPA (UPI ID) and Payee Name.
    
    Checks in order:
    1. `pos_billing/assets/store_upi_id.txt` (if user configured text override)
    2. Decodes `pos_billing/assets/store_qr.jpg` or `store_qr.png` using pyzbar/cv2
    3. Fallback default `Pos.11400001@indus` and `Bereeze Footwear`
    """
    default_upi = "8086790086@upi"
    default_name = "Bereeze Footwear Fancy"

    # 1. Check text file override
    txt_path = "pos_billing/assets/store_upi_id.txt"
    if os.path.exists(txt_path):
        try:
            with open(txt_path, "r", encoding="utf-8") as f:
                content = f.read().strip()
                if content:
                    parts = content.split("||")
                    upi_id = parts[0].strip()
                    payee_name = parts[1].strip() if len(parts) > 1 else default_name
                    return upi_id, payee_name
        except Exception as exc:
            logger.warning("Failed reading %s: %s", txt_path, exc)

    # 2. Try decoding from static image assets
    for img_path in ("pos_billing/assets/store_qr.jpg", "pos_billing/assets/store_qr.png"):
        if os.path.exists(img_path):
            decoded_uri = ""
            # Try pyzbar first
            try:
                import pyzbar.pyzbar as pyzbar
                from PIL import Image
                img = Image.open(img_path)
                decoded = pyzbar.decode(img)
                if decoded:
                    decoded_uri = decoded[0].data.decode("utf-8", errors="ignore")
            except Exception:
                pass

            # Try OpenCV detector if pyzbar didn't yield result
            if not decoded_uri:
                try:
                    import cv2
                    cv_img = cv2.imread(img_path)
                    if cv_img is not None:
                        detector = cv2.QRCodeDetector()
                        data, _, _ = detector.detectAndDecode(cv_img)
                        if data:
                            decoded_uri = data
                except Exception:
                    pass

            if decoded_uri and "pa=" in decoded_uri:
                try:
                    parsed = urllib.parse.urlparse(decoded_uri)
                    qs = urllib.parse.parse_qs(parsed.query)
                    pa = qs.get("pa", [default_upi])[0]
                    pn = qs.get("pn", [default_name])[0]
                    return pa, pn
                except Exception as exc:
                    logger.warning("Failed parsing decoded QR URI %s: %s", decoded_uri, exc)

    return default_upi, default_name


def generate_upi_qr(
    amount: float,
    bill_number: str = "",
    size: tuple[int, int] = (160, 160),
) -> tuple[Image.Image, str, str]:
    """
    Generate a dynamic UPI QR Code PIL Image with the exact bill amount (`am`) pre-set.

    Args:
        amount: The total amount due for the bill.
        bill_number: Optional invoice / bill reference number for transaction note.
        size: Target (width, height) tuple to resize the generated PIL image.

    Returns:
        (pil_image, upi_uri, upi_id)
    """
    import qrcode
    from PIL import Image

    upi_id, payee_name = get_store_upi_id()
    amount_clean = max(0.0, float(amount))

    query_params = {
        "pa": upi_id,
        "pn": payee_name,
        "am": f"{amount_clean:.2f}",
        "cu": "INR",
    }
    if bill_number:
        query_params["tn"] = f"Invoice {bill_number}"
        query_params["tr"] = bill_number

    upi_uri = f"upi://pay?{urllib.parse.urlencode(query_params)}"

    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=8,
        border=2,
    )
    qr.add_data(upi_uri)
    qr.make(fit=True)

    img = qr.make_image(fill_color="#0F172A", back_color="white").convert("RGB")
    img = img.resize(size, Image.Resampling.LANCZOS)
    return img, upi_uri, upi_id


def get_receipt_qr_image(
    payment_mode: str,
    amount: float,
    bill_number: str = "",
    size: tuple[int, int] = (160, 160)
) -> tuple[Image.Image, str, str]:
    """
    Return QR Code image and label text based on payment mode:
    - CASH / CREDIT: Returns store static QR code asset.
    - UPI / ONLINE: Generates dynamic UPI QR code with exact pre-filled bill amount.
    """
    from PIL import Image
    pm_upper = str(payment_mode or "").strip().upper()

    if pm_upper in ("UPI", "ONLINE", "GPAY", "PHONEPE", "PAYTM"):
        img, upi_uri, upi_id = generate_upi_qr(amount=amount, bill_number=bill_number, size=size)
        lbl_text = f"⚡ Dynamic UPI Invoice QR (Paid ₹ {amount:,.2f})"
        return img, upi_uri, lbl_text

    # Default / Cash Mode -> Return static store QR code asset
    cash_asset_paths = [
        "pos_billing/assets/cash_store_qr.png",
        "pos_billing/assets/cash_store_qr.jpg",
        "pos_billing/assets/store_qr.png",
        "pos_billing/assets/store_qr.jpg"
    ]
    for asset_path in cash_asset_paths:
        if os.path.exists(asset_path):
            try:
                img = Image.open(asset_path).convert("RGB")
                img = img.resize(size, Image.Resampling.LANCZOS)
                return img, "https://linktr.ee/shamilmohammed926", "📲 Bereeze Store QR Code (Scan for Store Info & Offers)"
            except Exception as exc:
                logger.warning("Failed loading static QR asset %s: %s", asset_path, exc)

    # Fallback to dynamic UPI QR if static asset unavailable
    img, upi_uri, _ = generate_upi_qr(amount=amount, bill_number=bill_number, size=size)
    return img, upi_uri, "📲 Bereeze Store QR Code"


def show_live_upi_dialog(parent: tk.Widget, bill: Bill) -> None:
    """
    Display an interactive modal popup with a large, dynamic UPI QR code
    pre-filled with the exact bill amount for instant customer scanning.
    """
    from PIL import ImageTk

    dlg = tk.Toplevel(parent)
    dlg.title(f"📱 Live UPI Payment QR – Bill {bill.bill_number}")
    dlg.configure(bg="#F8FAFC")
    dlg.geometry("440x560")
    dlg.minsize(400, 520)
    dlg.resizable(False, False)

    # Center dialog on parent window
    dlg.update_idletasks()
    try:
        pw = parent.winfo_width()
        ph = parent.winfo_height()
        px = parent.winfo_rootx()
        py = parent.winfo_rooty()
        x = max(0, px + (pw - 440) // 2)
        y = max(0, py + (ph - 560) // 2)
        dlg.geometry(f"440x560+{x}+{y}")
    except Exception:
        pass

    top = tk.Frame(dlg, bg="#0D9488", padx=20, pady=16)
    top.pack(fill=tk.X)
    top.pack_propagate(False)

    tk.Label(
        top,
        text="📱 Scan & Pay via UPI",
        font=("Segoe UI", 16, "bold"),
        fg="white",
        bg="#0D9488",
    ).pack(anchor="center")
    tk.Label(
        top,
        text="Google Pay • PhonePe • Paytm • BHIM",
        font=("Segoe UI", 9),
        fg="#E0F2F1",
        bg="#0D9488",
    ).pack(anchor="center", pady=(2, 0))

    body = tk.Frame(dlg, bg="#F8FAFC", padx=24, pady=18)
    body.pack(fill=tk.BOTH, expand=True)

    img, upi_uri, upi_id = generate_upi_qr(
        amount=bill.total_amount,
        bill_number=bill.bill_number,
        size=(240, 240),
    )
    # Retain reference to avoid garbage collection
    photo = ImageTk.PhotoImage(img)
    dlg._qr_photo = photo  # type: ignore[attr-defined]

    qr_card = tk.Frame(body, bg="white", bd=1, relief=tk.SOLID, padx=12, pady=12)
    qr_card.config(highlightbackground="#CBD5E1", highlightthickness=1, bd=0)
    qr_card.pack(pady=(4, 12))

    canvas = tk.Canvas(qr_card, width=240, height=240, bg="white", bd=0, highlightthickness=0)
    canvas.pack()
    canvas.create_image(120, 120, image=photo)

    badge = tk.Frame(body, bg="#DCFCE7", padx=16, pady=8, bd=1)
    badge.config(highlightbackground="#86EFAC", highlightthickness=1, bd=0)
    badge.pack(fill=tk.X, pady=(0, 10))

    tk.Label(
        badge,
        text="⚡ EXACT AMOUNT LOCKED IN QR",
        font=("Segoe UI", 8, "bold"),
        fg="#166534",
        bg="#DCFCE7",
    ).pack(anchor="center")
    tk.Label(
        badge,
        text=f"₹ {bill.total_amount:,.2f}",
        font=("Segoe UI", 18, "bold"),
        fg="#15803D",
        bg="#DCFCE7",
    ).pack(anchor="center")

    tk.Label(
        body,
        text=f"Merchant UPI ID: {upi_id}\n💡 Customer scans this code & the exact bill amount is automatically set in their payment app!",
        font=("Segoe UI", 8),
        fg="#64748B",
        bg="#F8FAFC",
        justify=tk.CENTER,
    ).pack(pady=(0, 12))

    btn = tk.Button(
        body,
        text="✅ Close & Return to Sale",
        font=("Segoe UI", 10, "bold"),
        bg="#0D9488",
        fg="white",
        activebackground="#0F766E",
        activeforeground="white",
        relief=tk.FLAT,
        cursor="hand2",
        pady=8,
        command=dlg.destroy,
    )
    btn.pack(fill=tk.X)

    dlg.grab_set()
    dlg.transient(parent)
    dlg.focus_set()
