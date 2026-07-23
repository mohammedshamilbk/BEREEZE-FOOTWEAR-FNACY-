# ============================================================
# pos_billing/database/models.py
# (Customer.java, ItemMaster.java, Bill.java, BillItem.java,
#  User.java → Python dataclasses)
# ============================================================
"""
Data model classes mirroring the Java POJOs.

All models are plain Python dataclasses – no ORM dependency required.
"""

from __future__ import annotations

import hashlib
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional


# ─────────────────────────────────────────────────────────────
# User  (User.java)
# ─────────────────────────────────────────────────────────────
@dataclass
class User:
    username: str
    password: str
    full_name: str
    role: str                          # ADMIN | CASHIER | MANAGER | OWNER
    user_id: int = 0
    email: str = ""
    phone: str = ""
    status: str = "ACTIVE"            # ACTIVE | INACTIVE
    daily_sales_target: float = 0.0
    total_sales_achieved: float = 0.0
    created_date: datetime = field(default_factory=datetime.now)

    # ------------------------------------------------------------------
    def authenticate(self, input_password: str) -> bool:
        """Support plain-text, unsalted SHA-256, and salted SHA-256."""
        if not input_password or not self.password:
            return False
        if self.status != "ACTIVE":
            return False
        # 1 – plain text
        if self.password == input_password:
            return True
        # 2 – unsalted SHA-256
        h = hashlib.sha256(input_password.encode()).hexdigest()
        if self.password == h:
            return True
        # 3 – salted SHA-256  (password::username)
        hs = hashlib.sha256(f"{input_password}::{self.username}".encode()).hexdigest()
        return self.password == hs

    def has_permission(self, action: str) -> bool:
        if self.role == "ADMIN":
            return True
        if self.role == "MANAGER":
            return action != "DELETE_USER"
        if self.role == "CASHIER":
            return action in ("SALES", "RETURN")
        return False

    def __str__(self) -> str:
        return f"{self.username} - {self.full_name} ({self.role})"


# ─────────────────────────────────────────────────────────────
# Customer  (Customer.java)
# ─────────────────────────────────────────────────────────────
@dataclass
class Customer:
    customer_code: str
    customer_name: str
    phone: str
    email: str
    customer_id: int = 0
    address: str = ""
    city: str = ""
    state: str = ""
    pincode: str = ""
    credit_limit: float = 0.0
    outstanding_amount: float = 0.0
    customer_type: str = "REGULAR"    # REGULAR | WHOLESALE | RETAIL
    loyalty_points: float = 0.0
    status: str = "ACTIVE"           # ACTIVE | INACTIVE
    registration_date: datetime = field(default_factory=datetime.now)
    last_purchase_date: Optional[datetime] = None

    def add_loyalty_points(self, amount: float) -> None:
        """10% of purchase amount is credited as loyalty points."""
        self.loyalty_points += amount * 0.1

    def get_effective_credit_limit(self) -> float:
        return self.credit_limit - self.outstanding_amount

    def __str__(self) -> str:
        return f"{self.customer_code} - {self.customer_name} ({self.phone})"


# ─────────────────────────────────────────────────────────────
# ItemMaster  (ItemMaster.java)
# ─────────────────────────────────────────────────────────────
@dataclass
class ItemMaster:
    item_code: str
    item_name: str
    category: str
    manufacturer: str
    purchase_price: float
    selling_price: float
    barcode: str
    size: str
    color: str
    material: str
    item_id: int = 0
    stock_quantity: int = 0
    reorder_level: int = 10
    status: str = "ACTIVE"           # ACTIVE | INACTIVE
    created_date: datetime = field(default_factory=datetime.now)
    modified_date: Optional[datetime] = None

    def calculate_profit(self) -> float:
        return self.selling_price - self.purchase_price

    def __str__(self) -> str:
        return f"{self.item_code} - {self.item_name} ({self.size}, {self.color})"


# ─────────────────────────────────────────────────────────────
# BillItem  (BillItem.java)
# ─────────────────────────────────────────────────────────────
@dataclass
class BillItem:
    item_id: int
    item_code: str
    item_name: str
    quantity: int
    unit_price: float
    discount: float = 0.0
    bill_item_id: int = 0
    bill_id: int = 0
    purchase_price: float = 0.0
    taxable_amount: float = field(init=False, default=0.0)
    total_amount: float = field(init=False, default=0.0)

    def __post_init__(self) -> None:
        self.calculate_amount()

    def calculate_amount(self) -> None:
        self.taxable_amount = (self.unit_price - self.discount) * self.quantity
        self.total_amount = self.taxable_amount

    def __str__(self) -> str:
        return f"{self.item_name} x {self.quantity} @ {self.unit_price:.2f} = {self.total_amount:.2f}"


# ─────────────────────────────────────────────────────────────
# Bill  (Bill.java)
# ─────────────────────────────────────────────────────────────
@dataclass
class Bill:
    bill_number: str
    bill_type: str                    # SALES | SALES_RETURN | PURCHASE | PURCHASE_RETURN
    customer_id: int
    customer_name: str
    bill_id: int = 0
    supplier_id: int = 0
    customer_phone: str = ""
    user_id: int = 0
    subtotal: float = 0.0
    total_discount: float = 0.0
    total_amount: float = 0.0
    paid_amount: float = 0.0
    balance_amount: float = 0.0
    payment_mode: str = ""            # CASH | CARD | CHEQUE | ONLINE
    remarks: str = ""
    status: str = "PENDING"          # PENDING | COMPLETED | CANCELLED
    bill_date: datetime = field(default_factory=datetime.now)
    created_date: datetime = field(default_factory=datetime.now)
    bill_items: List[BillItem] = field(default_factory=list)

    # ------------------------------------------------------------------
    def add_bill_item(self, item: BillItem) -> None:
        item.bill_id = self.bill_id
        self.bill_items.append(item)

    def remove_bill_item(self, index: int) -> None:
        if 0 <= index < len(self.bill_items):
            self.bill_items.pop(index)

    def calculate_totals(self) -> None:
        self.subtotal = 0.0
        self.total_discount = 0.0
        for item in self.bill_items:
            item.calculate_amount()
            self.subtotal += item.taxable_amount
            self.total_discount += item.discount * item.quantity
        self.total_amount = self.subtotal
        self.balance_amount = self.total_amount - self.paid_amount

    def apply_discount(self, discount_amount: float) -> None:
        if discount_amount <= self.total_amount:
            self.total_discount = discount_amount
            self.total_amount -= discount_amount
            self.balance_amount = self.total_amount - self.paid_amount

    def complete_bill(self, payment: float, payment_mode: str) -> bool:
        if payment >= self.total_amount:
            self.paid_amount = payment
            self.payment_mode = payment_mode
            self.balance_amount = self.total_amount - payment
            self.status = "COMPLETED"
            return True
        return False

    def print_bill(self) -> None:
        """Print a formatted receipt to stdout (mirrors Bill.printBill)."""
        sep = "=" * 48
        thin = "-" * 48
        print(f"\n{sep}")
        print("          BAREEZE FOOTWEAR             ")
        print("Address: Anar complex, Naya bazar,")
        print("Melparamba, Kasaragod, Kerala, India 671317")
        print("Mobile no: 8086790086")
        print("Mail ID: breezefootwearfancy@gmail.com")
        print(sep)
        print(f"Bill No: {self.bill_number}   Date: {self.bill_date:%Y-%m-%d %H:%M}")
        print(f"Type: {self.bill_type}")
        print(f"Customer: {self.customer_name} ({self.customer_phone})")
        print(thin)
        print(f"{'Item Name':<25} {'Qty':>3}  {'Price':>7}  {'Total':>8}")
        print(thin)
        for item in self.bill_items:
            print(f"{item.item_name:<25} {item.quantity:>3}  {item.unit_price:>7.2f}  {item.total_amount:>8.2f}")
        print(thin)
        print(f"{'Subtotal:':<38} {self.subtotal:>8.2f}")
        print(f"{'Total Discount:':<38} {self.total_discount:>8.2f}")
        print(thin)
        print(f"{'Total Amount:':<38} {self.total_amount:>8.2f}")
        print(f"{'Paid Amount:':<38} {self.paid_amount:>8.2f}")
        print(f"{'Balance:':<38} {self.balance_amount:>8.2f}")
        print(f"Payment Mode: {self.payment_mode}")
        print(f"{sep}\n")


# ─────────────────────────────────────────────────────────────
# Supplier  (Supplier.java)
# ─────────────────────────────────────────────────────────────
@dataclass
class Supplier:
    supplier_code: str
    supplier_name: str
    supplier_id: int = 0
    phone: str = ""
    email: str = ""
    state: str = ""
    tax_regn: str = ""
    gstin: str = ""
    outstanding_balance: float = 0.0
    status: str = "ACTIVE"
    created_date: datetime = field(default_factory=datetime.now)

    def __str__(self) -> str:
        return f"{self.supplier_code} - {self.supplier_name}"


# ─────────────────────────────────────────────────────────────
# PurchaseBill (Supplier Bill / Invoice)
# ─────────────────────────────────────────────────────────────
@dataclass
class PurchaseBill:
    bill_number: str
    supplier_id: int
    total_amount: float
    paid_amount: float = 0.0
    balance_due: float = 0.0
    purchase_bill_id: int = 0
    purchase_date: str = ""
    status: str = "PENDING"
    created_by: int = 0


# ─────────────────────────────────────────────────────────────
# Expense (Shop Expenses & Outflow)
# ─────────────────────────────────────────────────────────────
@dataclass
class Expense:
    category: str
    description: str
    amount: float
    payment_mode: str = "CASH"          # CASH | UPI | ONLINE | CHEQUE
    expense_id: int = 0
    expense_date: str = field(default_factory=lambda: datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    user_id: int = 0
    created_date: str = field(default_factory=lambda: datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

    def __str__(self) -> str:
        return f"{self.category}: ₹ {self.amount:,.2f} ({self.payment_mode})"


