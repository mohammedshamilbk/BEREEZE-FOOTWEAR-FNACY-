# pos_billing/backend/schemas.py
"""
Pydantic Schemas for API Request Validation and Response Serialisation.
"""

from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field, EmailStr


# --- Auth Schemas ---
class LoginRequest(BaseModel):
    username: str
    password: str


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class UserOut(BaseModel):
    user_id: int
    username: str
    full_name: str
    role: str
    email: Optional[str] = ""
    phone: Optional[str] = ""
    status: str
    daily_sales_target: float = 0.0
    total_sales_achieved: float = 0.0
    created_date: Optional[str] = None


class UserCreate(BaseModel):
    username: str
    password: str
    full_name: str
    role: str = "STAFF"
    email: Optional[str] = ""
    phone: Optional[str] = ""
    daily_sales_target: float = 0.0


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    role: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    status: Optional[str] = None
    password: Optional[str] = None
    daily_sales_target: Optional[float] = None


# --- Customer Schemas ---
class CustomerOut(BaseModel):
    customer_id: int
    customer_code: str
    customer_name: str
    phone: str
    email: Optional[str] = ""
    address: Optional[str] = ""
    city: Optional[str] = ""
    state: Optional[str] = ""
    pincode: Optional[str] = ""
    credit_limit: float = 0.0
    outstanding_amount: float = 0.0
    customer_type: str = "REGULAR"
    loyalty_points: float = 0.0
    status: str = "ACTIVE"
    registration_date: Optional[str] = None
    last_purchase_date: Optional[str] = None


class CustomerCreate(BaseModel):
    customer_name: str
    phone: str
    email: Optional[str] = ""
    address: Optional[str] = ""
    city: Optional[str] = ""
    state: Optional[str] = ""
    pincode: Optional[str] = ""
    credit_limit: float = 0.0
    customer_type: str = "REGULAR"


class CustomerUpdate(BaseModel):
    customer_name: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None
    city: Optional[str] = None
    state: Optional[str] = None
    pincode: Optional[str] = None
    credit_limit: Optional[float] = None
    customer_type: Optional[str] = None
    status: Optional[str] = None


# --- Item Master / Inventory Schemas ---
class ItemOut(BaseModel):
    item_id: int
    item_code: str
    item_name: str
    category: str
    manufacturer: Optional[str] = ""
    purchase_price: float
    selling_price: float
    barcode: Optional[str] = ""
    stock_quantity: int
    reorder_level: int = 10
    size: Optional[str] = ""
    color: Optional[str] = ""
    material: Optional[str] = ""
    status: str = "ACTIVE"
    created_date: Optional[str] = None


class ItemCreate(BaseModel):
    item_code: str
    item_name: str
    category: str
    manufacturer: Optional[str] = ""
    purchase_price: float
    selling_price: float
    barcode: Optional[str] = None
    stock_quantity: int = 0
    reorder_level: int = 10
    size: Optional[str] = ""
    color: Optional[str] = ""
    material: Optional[str] = ""


class ItemUpdate(BaseModel):
    item_name: Optional[str] = None
    category: Optional[str] = None
    manufacturer: Optional[str] = None
    purchase_price: Optional[float] = None
    selling_price: Optional[float] = None
    barcode: Optional[str] = None
    stock_quantity: Optional[int] = None
    reorder_level: Optional[int] = None
    size: Optional[str] = None
    color: Optional[str] = None
    material: Optional[str] = None
    status: Optional[str] = None


# --- Game Station / Session Schemas ---
class GameStationOut(BaseModel):
    station_id: int
    station_name: str
    station_type: str
    hourly_rate: float
    status: str  # AVAILABLE | OCCUPIED | MAINTENANCE
    current_session_id: Optional[int] = None


class GameSessionCreate(BaseModel):
    station_id: int
    customer_id: int
    rate_per_hour: Optional[float] = None
    notes: Optional[str] = ""


class GameSessionCheckout(BaseModel):
    session_id: int
    payment_mode: str = "CASH"
    paid_amount: Optional[float] = None
    discount: float = 0.0


class GameSessionOut(BaseModel):
    session_id: int
    station_id: int
    station_name: str
    customer_id: int
    customer_name: str
    start_time: str
    end_time: Optional[str] = None
    duration_minutes: float = 0.0
    rate_per_hour: float
    total_amount: float = 0.0
    paid_amount: float = 0.0
    payment_mode: str = ""
    status: str  # ACTIVE | COMPLETED | CANCELLED
    user_id: int


# --- POS Billing Schemas ---
class BillItemCreate(BaseModel):
    item_id: int
    quantity: int
    unit_price: float
    discount: float = 0.0


class BillItemOut(BaseModel):
    bill_item_id: int
    bill_id: int
    item_id: int
    item_code: str
    item_name: str
    quantity: int
    unit_price: float
    discount: float
    total_amount: float


class CheckoutRequest(BaseModel):
    customer_id: int
    bill_type: str = "SALES"  # SALES | SALES_RETURN
    bill_items: List[BillItemCreate]
    subtotal: float
    total_discount: float = 0.0
    total_amount: float
    paid_amount: float
    payment_mode: str  # CASH | CARD | UPI | CHEQUE | CREDIT
    remarks: Optional[str] = ""


class BillOut(BaseModel):
    bill_id: int
    bill_number: str
    bill_type: str
    bill_date: str
    customer_id: int
    customer_name: str
    user_id: int
    subtotal: float
    total_discount: float
    total_amount: float
    paid_amount: float
    balance_amount: float
    payment_mode: str
    status: str
    remarks: str
    bill_items: List[BillItemOut] = []


# --- Expense Schemas ---
class ExpenseCreate(BaseModel):
    category: str
    description: str
    amount: float
    payment_mode: str = "CASH"


class ExpenseOut(BaseModel):
    expense_id: int
    category: str
    description: str
    amount: float
    payment_mode: str
    expense_date: str
    user_id: int


# --- Supplier & Purchase Schemas ---
class SupplierOut(BaseModel):
    supplier_id: int
    supplier_code: str
    supplier_name: str
    phone: Optional[str] = ""
    email: Optional[str] = ""
    state: Optional[str] = ""
    tax_regn: Optional[str] = ""
    gstin: Optional[str] = ""
    outstanding_balance: float = 0.0
    status: str = "ACTIVE"


class SupplierCreate(BaseModel):
    supplier_code: str
    supplier_name: str
    phone: Optional[str] = ""
    email: Optional[str] = ""
    state: Optional[str] = ""
    tax_regn: Optional[str] = ""
    gstin: Optional[str] = ""


# --- Dashboard Stats Schema ---
class DashboardStatsOut(BaseModel):
    active_customers: int
    occupied_stations: int
    available_stations: int
    today_revenue: float
    today_bills: int
    pending_payments: float
    today_expenses: float
    today_profit: float
    yesterday_revenue: float
    yesterday_expenses: float
    yesterday_bills: int


# --- Audit & Sync Schemas ---
class AuditLogOut(BaseModel):
    log_id: int
    user_id: Optional[int]
    action: str
    table_name: str
    record_id: int
    old_value: Optional[str] = ""
    new_value: Optional[str] = ""
    action_date: str


class SyncBatchRequest(BaseModel):
    device_id: str
    pending_transactions: List[dict]
