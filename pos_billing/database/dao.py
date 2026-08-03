# ============================================================
# pos_billing/database/dao.py
# (UserDAO, CustomerDAO, ItemMasterDAO, BillDAO, BillItemDAO → Python)
# ============================================================
"""
Data Access Objects (DAO) for all entities.

Each function follows the pattern:
    - Opens a connection via get_connection()
    - Executes the query
    - Returns a Python model object (or list of objects)
    - Closes the connection in a finally block
"""

from __future__ import annotations

import logging
from datetime import datetime
from typing import List, Optional

from .connection import get_connection, close_connection
from .models import User, Customer, ItemMaster, BillItem, Bill, Supplier, Expense

logger = logging.getLogger(__name__)


# ─────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────
def _dt(value) -> Optional[datetime]:
    """Parse a datetime string from SQLite (or return None)."""
    if value is None:
        return None
    if isinstance(value, datetime):
        return value
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(str(value), fmt)
        except ValueError:
            continue
    return None


def _auto_save(event: str = "change", force_db: bool = False) -> None:
    """Trigger non-blocking auto-save and auto-backup."""
    try:
        from ..utils.auto_save_manager import trigger_auto_save
        trigger_auto_save(event, force_db_backup=force_db)
    except Exception as exc:
        logger.debug("Auto-save trigger error: %s", exc)


# ═══════════════════════════════════════════════════════════════
# USER DAO  (UserDAO.java → Python)
# ═══════════════════════════════════════════════════════════════
def _row_to_user(row) -> User:
    return User(
        user_id=row["userId"],
        username=row["username"],
        password=row["password"],
        full_name=row["fullName"],
        role=row["role"],
        email=row["email"] or "",
        phone=row["phone"] or "",
        status=row["status"],
        daily_sales_target=float(row["dailySalesTarget"] or 0),
        created_date=_dt(row["createdDate"]) or datetime.now(),
    )


def authenticate_user(username: str, password: str) -> Optional[User]:
    """Authenticate a user and return the User object (or None)."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM user WHERE username=? AND status='ACTIVE'", (username,))
        row = cur.fetchone()
        if row:
            user = _row_to_user(row)
            if user.authenticate(password):
                return user
        return None
    except Exception as exc:
        logger.error("authenticate_user error: %s", exc)
        return None
    finally:
        close_connection(conn)


def get_all_users() -> List[User]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM user ORDER BY fullName")
        return [_row_to_user(r) for r in cur.fetchall()]
    except Exception as exc:
        logger.error("get_all_users error: %s", exc)
        return []
    finally:
        close_connection(conn)


def save_user(user: User) -> bool:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """INSERT INTO user (username, password, fullName, role, email, phone, status, dailySalesTarget)
               VALUES (?,?,?,?,?,?,?,?)""",
            (user.username, user.password, user.full_name, user.role,
             user.email, user.phone, user.status, user.daily_sales_target),
        )
        conn.commit()
        return True
    except Exception as exc:
        logger.error("save_user error: %s", exc)
        return False
    finally:
        close_connection(conn)


# ═══════════════════════════════════════════════════════════════
# CUSTOMER DAO  (CustomerDAO.java → Python)
# ═══════════════════════════════════════════════════════════════
def _row_to_customer(row) -> Customer:
    return Customer(
        customer_id=row["customerId"],
        customer_code=row["customerCode"],
        customer_name=row["customerName"],
        phone=row["phone"] or "",
        email=row["email"] or "",
        address=row["address"] or "",
        city=row["city"] or "",
        state=row["state"] or "",
        pincode=row["pincode"] or "",
        credit_limit=float(row["creditLimit"] or 0),
        outstanding_amount=float(row["outstandingAmount"] or 0),
        customer_type=row["customerType"] or "REGULAR",
        loyalty_points=float(row["loyaltyPoints"] or 0),
        status=row["status"],
        registration_date=_dt(row["registrationDate"]) or datetime.now(),
        last_purchase_date=_dt(row["lastPurchaseDate"]),
    )


def get_all_customers() -> List[Customer]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM customer ORDER BY customerName")
        return [_row_to_customer(r) for r in cur.fetchall()]
    except Exception as exc:
        logger.error("get_all_customers error: %s", exc)
        return []
    finally:
        close_connection(conn)


def search_customer_by_phone(phone: str) -> Optional[Customer]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM customer WHERE phone=?", (phone,))
        row = cur.fetchone()
        return _row_to_customer(row) if row else None
    except Exception as exc:
        logger.error("search_customer_by_phone error: %s", exc)
        return None
    finally:
        close_connection(conn)


def search_customer_by_code(code: str) -> Optional[Customer]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM customer WHERE customerCode=?", (code,))
        row = cur.fetchone()
        return _row_to_customer(row) if row else None
    except Exception as exc:
        logger.error("search_customer_by_code error: %s", exc)
        return None
    finally:
        close_connection(conn)


def save_customer(customer: Customer) -> bool:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """INSERT OR REPLACE INTO customer
               (customerId, customerCode, customerName, phone, email,
                address, city, state, pincode, creditLimit, outstandingAmount,
                customerType, loyaltyPoints, status)
               VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (customer.customer_id or None, customer.customer_code, customer.customer_name,
             customer.phone, customer.email, customer.address, customer.city,
             customer.state, customer.pincode, customer.credit_limit,
             customer.outstanding_amount, customer.customer_type,
             customer.loyalty_points, customer.status),
        )
        conn.commit()
        if customer.customer_id == 0:
            customer.customer_id = cur.lastrowid
        return True
    except Exception as exc:
        logger.error("save_customer error: %s", exc)
        return False
    finally:
        close_connection(conn)


def update_customer_loyalty(customer_id: int, loyalty_points: float,
                             last_purchase_date: str) -> bool:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            "UPDATE customer SET loyaltyPoints=?, lastPurchaseDate=? WHERE customerId=?",
            (loyalty_points, last_purchase_date, customer_id),
        )
        conn.commit()
        return True
    except Exception as exc:
        logger.error("update_customer_loyalty error: %s", exc)
        return False
    finally:
        close_connection(conn)


# ═══════════════════════════════════════════════════════════════
# ITEM MASTER DAO  (ItemMasterDAO.java → Python)
# ═══════════════════════════════════════════════════════════════
def _row_to_item(row) -> ItemMaster:
    return ItemMaster(
        item_id=row["itemId"],
        item_code=row["itemCode"],
        item_name=row["itemName"],
        category=row["category"],
        manufacturer=row["manufacturer"] or "",
        purchase_price=float(row["purchasePrice"]),
        selling_price=float(row["sellingPrice"]),
        barcode=row["barcode"] or "",
        stock_quantity=int(row["stockQuantity"]),
        reorder_level=int(row["reorderLevel"]),
        size=row["size"] or "",
        color=row["color"] or "",
        material=row["material"] or "",
        status=row["status"],
        created_date=_dt(row["createdDate"]) or datetime.now(),
        modified_date=_dt(row["modifiedDate"]),
    )


def get_all_items() -> List[ItemMaster]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM item_master WHERE status='ACTIVE' ORDER BY itemName")
        return [_row_to_item(r) for r in cur.fetchall()]
    except Exception as exc:
        logger.error("get_all_items error: %s", exc)
        return []
    finally:
        close_connection(conn)


def search_item_by_barcode(barcode: str) -> Optional[ItemMaster]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM item_master WHERE barcode=?", (barcode,))
        row = cur.fetchone()
        return _row_to_item(row) if row else None
    except Exception as exc:
        logger.error("search_item_by_barcode error: %s", exc)
        return None
    finally:
        close_connection(conn)


def search_item_by_code(item_code: str) -> Optional[ItemMaster]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM item_master WHERE itemCode=?", (item_code,))
        row = cur.fetchone()
        return _row_to_item(row) if row else None
    except Exception as exc:
        logger.error("search_item_by_code error: %s", exc)
        return None
    finally:
        close_connection(conn)


def save_item(item: ItemMaster) -> bool:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """INSERT OR REPLACE INTO item_master
               (itemId, itemCode, itemName, category, manufacturer,
                purchasePrice, sellingPrice, barcode, stockQuantity,
                reorderLevel, size, color, material, status)
               VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (item.item_id or None, item.item_code, item.item_name,
             item.category, item.manufacturer, item.purchase_price,
             item.selling_price, item.barcode, item.stock_quantity,
             item.reorder_level, item.size, item.color, item.material, item.status),
        )
        conn.commit()
        if item.item_id == 0:
            item.item_id = cur.lastrowid
        return True
    except Exception as exc:
        logger.error("save_item error: %s", exc)
        return False
    finally:
        close_connection(conn)


def update_stock(item_id: int, new_quantity: int) -> bool:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            "UPDATE item_master SET stockQuantity=?, modifiedDate=datetime('now') WHERE itemId=?",
            (new_quantity, item_id),
        )
        conn.commit()
        return True
    except Exception as exc:
        logger.error("update_stock error: %s", exc)
        return False
    finally:
        close_connection(conn)


# ═══════════════════════════════════════════════════════════════
# BILL DAO  (BillDAO.java + BillItemDAO.java → Python)
# ═══════════════════════════════════════════════════════════════
def _row_to_bill(row) -> Bill:
    return Bill(
        bill_id=row["billId"],
        bill_number=row["billNumber"],
        bill_type=row["billType"],
        customer_id=row["customerId"] or 0,
        customer_name=row["customerName"] if "customerName" in row.keys() else "",
        supplier_id=row["supplierId"] or 0,
        user_id=row["userId"],
        subtotal=float(row["subtotal"] or 0),
        total_discount=float(row["totalDiscount"] or 0),
        total_amount=float(row["totalAmount"] or 0),
        paid_amount=float(row["paidAmount"] or 0),
        payment_mode=row["paymentMode"] or "",
        remarks=row["remarks"] or "",
        status=row["status"],
        bill_date=_dt(row["billDate"]) or datetime.now(),
        created_date=_dt(row["createdDate"]) or datetime.now(),
    )


def save_bill(bill: Bill) -> bool:
    """Save (insert) a Bill and all its BillItems in one transaction."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """INSERT INTO bill
               (billNumber, billType, billDate, customerId, supplierId, userId,
                subtotal, totalDiscount, totalAmount, paidAmount, paymentMode,
                status, remarks)
               VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (bill.bill_number, bill.bill_type,
             bill.bill_date.strftime("%Y-%m-%d %H:%M:%S"),
             bill.customer_id or None, bill.supplier_id or None, bill.user_id,
             bill.subtotal, bill.total_discount, bill.total_amount,
             bill.paid_amount, bill.payment_mode, bill.status, bill.remarks),
        )
        bill.bill_id = cur.lastrowid

        for item in bill.bill_items:
            cur.execute(
                """INSERT INTO bill_item
                   (billId, itemId, itemCode, itemName, quantity, unitPrice, discount, totalAmount)
                   VALUES (?,?,?,?,?,?,?,?)""",
                (bill.bill_id, item.item_id, item.item_code, item.item_name,
                 item.quantity, item.unit_price, item.discount, item.total_amount),
            )
        conn.commit()
        _auto_save("save_bill", force_db=True)
        return True
    except Exception as exc:
        logger.error("save_bill error: %s", exc)
        conn.rollback()
        return False
    finally:
        close_connection(conn)


def get_all_bills(limit: int = 200) -> List[Bill]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """SELECT b.*, c.customerName
               FROM bill b
               LEFT JOIN customer c ON b.customerId = c.customerId
               ORDER BY b.billDate DESC LIMIT ?""",
            (limit,),
        )
        bills = []
        for row in cur.fetchall():
            b = _row_to_bill(row)
            # Load items
            cur2 = conn.cursor()
            cur2.execute(
                """SELECT bi.*, im.purchasePrice
                   FROM bill_item bi
                   LEFT JOIN item_master im ON bi.itemId = im.itemId
                   WHERE bi.billId=?""",
                (b.bill_id,),
            )
            for irow in cur2.fetchall():
                bi = BillItem(
                    bill_item_id=irow["billItemId"],
                    bill_id=irow["billId"],
                    item_id=irow["itemId"],
                    item_code=irow["itemCode"],
                    item_name=irow["itemName"],
                    quantity=irow["quantity"],
                    unit_price=float(irow["unitPrice"]),
                    discount=float(irow["discount"] or 0),
                    purchase_price=float(irow["purchasePrice"] or 0.0),
                )
                bi.total_amount = float(irow["totalAmount"])
                b.bill_items.append(bi)
            bills.append(b)
        return bills
    except Exception as exc:
        logger.error("get_all_bills error: %s", exc)
        return []
    finally:
        close_connection(conn)


def get_bill_by_number(bill_number: str) -> Optional[Bill]:
    """Retrieve complete bill with items by bill number."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """SELECT b.*, c.customerName
               FROM bill b
               LEFT JOIN customer c ON b.customerId = c.customerId
               WHERE b.billNumber = ?""",
            (bill_number,),
        )
        row = cur.fetchone()
        if not row:
            return None
        b = _row_to_bill(row)
        cur2 = conn.cursor()
        cur2.execute(
            """SELECT bi.*, im.purchasePrice
               FROM bill_item bi
               LEFT JOIN item_master im ON bi.itemId = im.itemId
               WHERE bi.billId = ?""",
            (b.bill_id,),
        )
        for irow in cur2.fetchall():
            bi = BillItem(
                bill_item_id=irow["billItemId"],
                bill_id=irow["billId"],
                item_id=irow["itemId"],
                item_code=irow["itemCode"],
                item_name=irow["itemName"],
                quantity=irow["quantity"],
                unit_price=float(irow["unitPrice"]),
                discount=float(irow["discount"] or 0),
                purchase_price=float(irow["purchasePrice"] or 0.0),
            )
            bi.total_amount = float(irow["totalAmount"])
            b.bill_items.append(bi)
        return b
    except Exception as exc:
        logger.error("get_bill_by_number error: %s", exc)
        return None
    finally:
        close_connection(conn)


def update_bill_status(bill_number: str, status: str) -> bool:
    """Update status of a bill by bill number (e.g. CANCELLED or COMPLETED)."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("UPDATE bill SET status=? WHERE billNumber=?", (status, bill_number))
        conn.commit()
        _auto_save("update_bill_status", force_db=True)
        return True
    except Exception as exc:
        logger.error("update_bill_status error: %s", exc)
        return False
    finally:
        close_connection(conn)



def get_total_sales() -> float:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT SUM(totalAmount) FROM bill WHERE status='COMPLETED'")
        result = cur.fetchone()[0]
        return float(result or 0)
    except Exception as exc:
        logger.error("get_total_sales error: %s", exc)
        return 0.0
    finally:
        close_connection(conn)


# ═══════════════════════════════════════════════════════════════
# SUPPLIER DAO  (SupplierDAO.java → Python)
# ═══════════════════════════════════════════════════════════════
def _row_to_supplier(row) -> Supplier:
    return Supplier(
        supplier_id=row["supplierId"],
        supplier_code=row["supplierCode"],
        supplier_name=row["supplierName"],
        phone=row["phone"] or "",
        email=row["email"] or "",
        state=row["state"] or "",
        tax_regn=row["taxRegn"] or "",
        gstin=row["gstin"] or "",
        outstanding_balance=float(row["outstandingBalance"] or 0),
        status=row["status"],
        created_date=_dt(row["createdDate"]) or datetime.now(),
    )


def get_all_suppliers() -> List[Supplier]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT * FROM supplier ORDER BY supplierName")
        return [_row_to_supplier(r) for r in cur.fetchall()]
    except Exception as exc:
        logger.error("get_all_suppliers error: %s", exc)
        return []
    finally:
        close_connection(conn)


def save_supplier(supplier: Supplier) -> bool:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """INSERT OR REPLACE INTO supplier
               (supplierId, supplierCode, supplierName, phone, email,
                state, taxRegn, gstin, outstandingBalance, status)
               VALUES (?,?,?,?,?,?,?,?,?,?)""",
            (supplier.supplier_id or None, supplier.supplier_code,
             supplier.supplier_name, supplier.phone, supplier.email,
             supplier.state, supplier.tax_regn, supplier.gstin,
             supplier.outstanding_balance, supplier.status),
        )
        conn.commit()
        if supplier.supplier_id == 0:
            supplier.supplier_id = cur.lastrowid
        return True
    except Exception as exc:
        logger.error("save_supplier error: %s", exc)
        return False
    finally:
        close_connection(conn)


def save_purchase_bill(bill_number: str, supplier_id: int, total_amount: float,
                       paid_amount: float = 0.0, status: str = "PENDING", created_by: int = 0) -> bool:
    """Save a new supplier purchase bill and update the supplier's outstanding balance."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        balance_due = max(0.0, total_amount - paid_amount)
        if balance_due == 0 and total_amount > 0:
            status = "PAID"
        elif paid_amount > 0 and balance_due > 0:
            status = "PARTIAL"

        cur.execute(
            """INSERT INTO purchase_bill
               (bill_number, supplier_id, total_amount, paid_amount, balance_due, status, created_by)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (bill_number, supplier_id, total_amount, paid_amount, balance_due, status, created_by),
        )
        bill_id = cur.lastrowid

        # Update supplier's outstanding balance with whatever amount wasn't paid immediately
        if balance_due > 0:
            cur.execute(
                "UPDATE supplier SET outstandingBalance = outstandingBalance + ? WHERE supplierId = ?",
                (balance_due, supplier_id),
            )

        # If any amount was paid right now, record it in purchase_payment table
        if paid_amount > 0:
            try:
                cur.execute("ALTER TABLE purchase_payment ADD COLUMN supplier_id INTEGER DEFAULT 0")
            except Exception:
                pass
            cur.execute(
                """INSERT INTO purchase_payment (purchase_bill_id, supplier_id, amount, payment_mode, reference_note)
                   VALUES (?, ?, ?, ?, ?)""",
                (bill_id, supplier_id, paid_amount, "CASH", f"Initial payment for bill {bill_number}"),
            )

        conn.commit()
        return True
    except Exception as exc:
        logger.error("save_purchase_bill error: %s", exc)
        return False
    finally:
        close_connection(conn)


def record_supplier_payment(supplier_id: int, amount: float, payment_mode: str,
                            reference_note: str = "", purchase_bill_id: int = 0) -> bool:
    """Record a payment made to a supplier and reduce their outstanding balance."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        try:
            cur.execute("ALTER TABLE purchase_payment ADD COLUMN supplier_id INTEGER DEFAULT 0")
        except Exception:
            pass
        cur.execute(
            """INSERT INTO purchase_payment (purchase_bill_id, supplier_id, amount, payment_mode, reference_note)
               VALUES (?, ?, ?, ?, ?)""",
            (purchase_bill_id, supplier_id, amount, payment_mode, reference_note),
        )

        cur.execute(
            """UPDATE supplier
               SET outstandingBalance = CASE
                   WHEN outstandingBalance - ? < 0 THEN 0
                   ELSE outstandingBalance - ?
               END
               WHERE supplierId = ?""",
            (amount, amount, supplier_id),
        )

        if purchase_bill_id > 0:
            cur.execute(
                """UPDATE purchase_bill
                   SET paid_amount = paid_amount + ?,
                       balance_due = CASE WHEN balance_due - ? < 0 THEN 0 ELSE balance_due - ? END,
                       status = CASE WHEN balance_due - ? <= 0 THEN 'PAID' ELSE 'PARTIAL' END
                   WHERE purchase_bill_id = ?""",
                (amount, amount, amount, amount, purchase_bill_id),
            )

        conn.commit()
        return True
    except Exception as exc:
        logger.error("record_supplier_payment error: %s", exc)
        return False
    finally:
        close_connection(conn)


def get_purchase_bills_by_supplier(supplier_id: int) -> List[dict]:
    """Retrieve all purchase bills for a specific supplier."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """SELECT purchase_bill_id, bill_number, purchase_date, total_amount,
                      paid_amount, balance_due, status
               FROM purchase_bill WHERE supplier_id = ? ORDER BY purchase_bill_id DESC""",
            (supplier_id,),
        )
        return [dict(r) for r in cur.fetchall()]
    except Exception as exc:
        logger.error("get_purchase_bills_by_supplier error: %s", exc)
        return []
    finally:
        close_connection(conn)


def get_supplier_payments(supplier_id: int) -> List[dict]:
    """Retrieve all payments recorded for a specific supplier."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        try:
            cur.execute("ALTER TABLE purchase_payment ADD COLUMN supplier_id INTEGER DEFAULT 0")
            conn.commit()
        except Exception:
            pass
            
        cur.execute(
            """SELECT pp.payment_id, pp.purchase_bill_id, pp.amount, pp.payment_mode,
                      pp.payment_date, pp.reference_note, pb.bill_number
               FROM purchase_payment pp
               LEFT JOIN purchase_bill pb ON pp.purchase_bill_id = pb.purchase_bill_id
               WHERE pp.supplier_id = ? OR pb.supplier_id = ?
               ORDER BY pp.payment_id DESC""",
            (supplier_id, supplier_id),
        )
        return [dict(r) for r in cur.fetchall()]
    except Exception as exc:
        logger.error("get_supplier_payments error: %s", exc)
        return []
    finally:
        close_connection(conn)


# ═══════════════════════════════════════════════════════════════
# BARCODE / VOUCHER SEARCH DAOs
# ═══════════════════════════════════════════════════════════════
def get_bill_by_number(bill_number: str, voucher_type: Optional[str] = None) -> Optional[Bill]:
    """Retrieve a sales/return bill by exact or partial bill number, with items populated."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        query = "SELECT b.*, c.customerName FROM bill b LEFT JOIN customer c ON b.customerId = c.customerId WHERE b.billNumber LIKE ?"
        params: list = [f"%{bill_number.strip()}%"]
        if voucher_type and voucher_type != "ALL":
            query += " AND b.billType = ?"
            params.append(voucher_type)
        cur.execute(query, tuple(params))
        row = cur.fetchone()
        if not row:
            return None
        b = _row_to_bill(row)
        
        cur2 = conn.cursor()
        cur2.execute(
            """SELECT bi.*, im.purchasePrice, im.barcode, im.size, im.color, im.category
               FROM bill_item bi
               LEFT JOIN item_master im ON bi.itemId = im.itemId
               WHERE bi.billId=?""",
            (b.bill_id,),
        )
        for irow in cur2.fetchall():
            bi = BillItem(
                bill_item_id=irow["billItemId"],
                bill_id=irow["billId"],
                item_id=irow["itemId"],
                item_code=irow["itemCode"],
                item_name=irow["itemName"],
                quantity=irow["quantity"],
                unit_price=float(irow["unitPrice"]),
                discount=float(irow["discount"] or 0),
                purchase_price=float(irow["purchasePrice"] or 0.0),
            )
            bi.total_amount = float(irow["totalAmount"])
            # attach extra attributes dynamically for barcode printing
            setattr(bi, "barcode", irow["barcode"] or irow["itemCode"])
            setattr(bi, "size", irow["size"] or "")
            setattr(bi, "color", irow["color"] or "")
            setattr(bi, "category", irow["category"] or "")
            b.bill_items.append(bi)
        return b
    except Exception as exc:
        logger.error("get_bill_by_number error: %s", exc)
        return None
    finally:
        close_connection(conn)


def get_purchase_bill_with_items(bill_number: str) -> Optional[dict]:
    """Retrieve a purchase invoice and its items by exact or partial bill number."""
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """SELECT pb.*, s.supplierName, s.supplierCode
               FROM purchase_bill pb
               LEFT JOIN supplier s ON pb.supplier_id = s.supplierId
               WHERE pb.bill_number LIKE ?""",
            (f"%{bill_number.strip()}%",),
        )
        row = cur.fetchone()
        if not row:
            return None
        
        header = dict(row)
        cur2 = conn.cursor()
        cur2.execute(
            """SELECT pbi.*, im.itemCode, im.itemName, im.barcode, im.sellingPrice, im.size, im.color, im.category
               FROM purchase_bill_item pbi
               LEFT JOIN item_master im ON pbi.item_id = im.itemId
               WHERE pbi.purchase_bill_id = ?""",
            (header["purchase_bill_id"],),
        )
        header["items"] = [dict(r) for r in cur2.fetchall()]
        return header
    except Exception as exc:
        logger.error("get_purchase_bill_with_items error: %s", exc)
        return None
    finally:
        close_connection(conn)


def search_voucher_items(voucher_type: str, invoice_number: str) -> List[dict]:
    """
    Search for items across Sales Bills or Purchase Invoices matching invoice_number and voucher_type.
    Returns standardized dicts ready for Barcode Table.
    """
    results: List[dict] = []
    if not invoice_number or not invoice_number.strip():
        return results
    
    clean_no = invoice_number.strip()
    v_type_upper = voucher_type.upper().strip() if voucher_type else "ALL"
    
    # 1. Search Sales / Sales Return Bills
    if v_type_upper in ("SALES", "SALES_RETURN", "ALL") or "SALES" in v_type_upper:
        target_type = None if v_type_upper == "ALL" else ("SALES_RETURN" if "RETURN" in v_type_upper else "SALES")
        bill = get_bill_by_number(clean_no, target_type)
        if bill:
            for item in bill.bill_items:
                results.append({
                    "item_id": item.item_id,
                    "item_code": item.item_code,
                    "item_name": item.item_name,
                    "barcode": getattr(item, "barcode", item.item_code) or item.item_code,
                    "quantity": item.quantity,
                    "selling_price": item.unit_price,
                    "size": getattr(item, "size", ""),
                    "color": getattr(item, "color", ""),
                    "category": getattr(item, "category", ""),
                    "voucher_number": bill.bill_number,
                    "voucher_type": bill.bill_type,
                    "source": f"Sales Bill ({bill.customer_name or 'Walk-in'})"
                })
    
    # 2. Search Purchase Invoices if no results or if ALL/PURCHASE requested
    if (not results and v_type_upper == "ALL") or "PURCHASE" in v_type_upper:
        pb = get_purchase_bill_with_items(clean_no)
        if pb and "items" in pb:
            for item in pb["items"]:
                results.append({
                    "item_id": item.get("item_id", 0),
                    "item_code": item.get("itemCode", f"ITEM-{item.get('item_id',0)}"),
                    "item_name": item.get("itemName", "Received Item"),
                    "barcode": item.get("barcode") or item.get("itemCode") or f"ITEM-{item.get('item_id',0)}",
                    "quantity": item.get("quantity", 1),
                    "selling_price": float(item.get("sellingPrice") or item.get("purchase_price") or 0.0),
                    "size": item.get("size", ""),
                    "color": item.get("color", ""),
                    "category": item.get("category", ""),
                    "voucher_number": pb.get("bill_number", clean_no),
                    "voucher_type": "PURCHASE",
                    "source": f"Purchase Invoice ({pb.get('supplierName', 'Supplier')})"
                })
                
    return results


# ═══════════════════════════════════════════════════════════════
# EXPENSE DAO
# ═══════════════════════════════════════════════════════════════
def _row_to_expense(row) -> Expense:
    return Expense(
        expense_id=row["expense_id"],
        expense_date=row["expense_date"],
        category=row["category"],
        description=row["description"] or "",
        amount=float(row["amount"]),
        payment_mode=row["payment_mode"] or "CASH",
        user_id=row["user_id"],
        created_date=row["created_date"] or "",
    )


def save_expense(expense: Expense) -> None:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS expense (
                expense_id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_date TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT,
                amount REAL NOT NULL,
                payment_mode TEXT DEFAULT 'CASH',
                user_id INTEGER DEFAULT 0,
                created_date TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        if expense.expense_id > 0:
            cur.execute("""
                UPDATE expense
                SET expense_date=?, category=?, description=?, amount=?, payment_mode=?, user_id=?
                WHERE expense_id=?
            """, (expense.expense_date, expense.category, expense.description,
                  expense.amount, expense.payment_mode, expense.user_id, expense.expense_id))
        else:
            cur.execute("""
                INSERT INTO expense (expense_date, category, description, amount, payment_mode, user_id, created_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (expense.expense_date, expense.category, expense.description,
                  expense.amount, expense.payment_mode, expense.user_id,
                  expense.created_date or datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
            if hasattr(cur, "lastrowid"):
                expense.expense_id = cur.lastrowid
        conn.commit()
        _auto_save("save_expense")
    finally:
        close_connection(conn)


def delete_expense(expense_id: int) -> None:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS expense (
                expense_id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_date TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT,
                amount REAL NOT NULL,
                payment_mode TEXT DEFAULT 'CASH',
                user_id INTEGER DEFAULT 0,
                created_date TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        cur.execute("DELETE FROM expense WHERE expense_id = ?", (expense_id,))
        conn.commit()
        _auto_save("delete_expense")
    finally:
        close_connection(conn)


def get_all_expenses() -> List[Expense]:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS expense (
                expense_id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_date TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT,
                amount REAL NOT NULL,
                payment_mode TEXT DEFAULT 'CASH',
                user_id INTEGER DEFAULT 0,
                created_date TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        cur.execute("SELECT * FROM expense ORDER BY expense_date DESC, expense_id DESC")
        rows = cur.fetchall()
        return [_row_to_expense(r) for r in rows]
    finally:
        close_connection(conn)


def get_total_expenses(start_date: str = None, end_date: str = None, payment_mode: str = None) -> float:
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS expense (
                expense_id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_date TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT,
                amount REAL NOT NULL,
                payment_mode TEXT DEFAULT 'CASH',
                user_id INTEGER DEFAULT 0,
                created_date TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        sql = "SELECT COALESCE(SUM(amount), 0.0) FROM expense WHERE 1=1"
        params = []
        if start_date:
            sql += " AND expense_date >= ?"
            params.append(start_date)
        if end_date:
            sql += " AND expense_date <= ?"
            params.append(end_date)
        if payment_mode:
            sql += " AND UPPER(payment_mode) = ?"
            params.append(payment_mode.upper())
        cur.execute(sql, params)
        res = cur.fetchone()
        return float(res[0]) if res and res[0] is not None else 0.0
    finally:
        close_connection(conn)


def get_daily_sales_by_payment_mode(date_str: str = None) -> dict:
    """Return dict with cash_sales, upi_sales, cash_expenses, upi_expenses, net_cash for a date (YYYY-MM-DD)."""
    if not date_str:
        date_str = datetime.now().strftime("%Y-%m-%d")

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS expense (
                expense_id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_date TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT,
                amount REAL NOT NULL,
                payment_mode TEXT DEFAULT 'CASH',
                user_id INTEGER DEFAULT 0,
                created_date TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        start = f"{date_str} 00:00:00"
        end = f"{date_str} 23:59:59"
        cur.execute("""
            SELECT paymentMode, COALESCE(SUM(totalAmount), 0.0)
            FROM bill
            WHERE billDate >= ? AND billDate <= ? AND status = 'COMPLETED'
            GROUP BY paymentMode
        """, (start, end))
        rows = cur.fetchall()
        totals = {"CASH": 0.0, "UPI": 0.0, "CARD": 0.0, "CHEQUE": 0.0, "ONLINE": 0.0}
        for row in rows:
            mode = (row[0] or "CASH").upper()
            totals[mode] = float(row[1])

        # Expenses today
        cur.execute("""
            SELECT payment_mode, COALESCE(SUM(amount), 0.0)
            FROM expense
            WHERE expense_date >= ? AND expense_date <= ?
            GROUP BY payment_mode
        """, (start, end))
        exp_rows = cur.fetchall()
        exp_totals = {"CASH": 0.0, "UPI": 0.0, "CARD": 0.0, "ONLINE": 0.0}
        for row in exp_rows:
            mode = (row[0] or "CASH").upper()
            exp_totals[mode] = float(row[1])

        cash_sales = totals["CASH"]
        upi_sales = totals["UPI"] + totals["ONLINE"]
        cash_exp = exp_totals["CASH"]
        upi_exp = exp_totals["UPI"] + exp_totals["ONLINE"]

        return {
            "date": date_str,
            "cash_sales": cash_sales,
            "upi_sales": upi_sales,
            "other_sales": totals["CARD"] + totals["CHEQUE"],
            "total_sales": sum(totals.values()),
            "cash_expenses": cash_exp,
            "upi_expenses": upi_exp,
            "total_expenses": sum(exp_totals.values()),
            "net_cash_in_counter": cash_sales - cash_exp,
        }
    finally:
        close_connection(conn)


# ═══════════════════════════════════════════════════════════════
# GAME STATIONS & GAME SESSIONS DAO
# ═══════════════════════════════════════════════════════════════
class GameSessionDAO:
    def get_all_stations(self) -> List[dict]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT station_id, station_name, station_type, hourly_rate, status, current_session_id FROM game_station ORDER BY station_id")
            rows = cur.fetchall()
            return [
                {
                    "station_id": r["station_id"],
                    "station_name": r["station_name"],
                    "station_type": r["station_type"],
                    "hourly_rate": float(r["hourly_rate"]),
                    "status": r["status"],
                    "current_session_id": r["current_session_id"],
                }
                for r in rows
            ]
        finally:
            close_connection(conn)

    def get_active_sessions(self) -> List[dict]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("""
                SELECT s.*, st.station_name
                FROM game_session s
                JOIN game_station st ON s.station_id = st.station_id
                WHERE s.status = 'ACTIVE'
                ORDER BY s.session_id DESC
            """)
            rows = cur.fetchall()
            return [
                {
                    "session_id": r["session_id"],
                    "station_id": r["station_id"],
                    "station_name": r["station_name"],
                    "customer_id": r["customer_id"],
                    "customer_name": r["customer_name"],
                    "start_time": str(r["start_time"]),
                    "end_time": str(r["end_time"]) if r["end_time"] else None,
                    "duration_minutes": float(r["duration_minutes"] or 0),
                    "rate_per_hour": float(r["rate_per_hour"]),
                    "total_amount": float(r["total_amount"] or 0),
                    "paid_amount": float(r["paid_amount"] or 0),
                    "payment_mode": r["payment_mode"] or "",
                    "status": r["status"],
                    "user_id": r["user_id"],
                }
                for r in rows
            ]
        finally:
            close_connection(conn)

    def start_session(self, station_id: int, customer_id: int, customer_name: str, rate_per_hour: float = None, user_id: int = 1) -> dict:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT station_name, hourly_rate, status FROM game_station WHERE station_id = ?", (station_id,))
            st = cur.fetchone()
            if not st or st["status"] == "OCCUPIED":
                return None

            rate = rate_per_hour if rate_per_hour is not None else float(st["hourly_rate"])
            start_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            cur.execute("""
                INSERT INTO game_session (station_id, customer_id, customer_name, start_time, rate_per_hour, status, user_id)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?)
            """, (station_id, customer_id, customer_name, start_str, rate, user_id))
            session_id = cur.lastrowid

            cur.execute("""
                UPDATE game_station
                SET status = 'OCCUPIED', current_session_id = ?
                WHERE station_id = ?
            """, (session_id, station_id))

            conn.commit()
            return {
                "session_id": session_id,
                "station_id": station_id,
                "station_name": st["station_name"],
                "customer_id": customer_id,
                "customer_name": customer_name,
                "start_time": start_str,
                "rate_per_hour": rate,
            }
        finally:
            close_connection(conn)

    def end_session(self, session_id: int, payment_mode: str = "CASH", discount: float = 0.0) -> dict:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("""
                SELECT s.*, st.station_name
                FROM game_session s
                JOIN game_station st ON s.station_id = st.station_id
                WHERE s.session_id = ? AND s.status = 'ACTIVE'
            """, (session_id,))
            row = cur.fetchone()
            if not row:
                return None

            start_dt = datetime.strptime(str(row["start_time"])[:19], "%Y-%m-%d %H:%M:%S")
            end_dt = datetime.now()
            end_str = end_dt.strftime("%Y-%m-%d %H:%M:%S")

            duration_mins = max(1.0, (end_dt - start_dt).total_seconds() / 60.0)
            rate = float(row["rate_per_hour"])
            total_amt = max(0.0, (duration_mins / 60.0 * rate) - discount)

            cur.execute("""
                UPDATE game_session
                SET end_time = ?, duration_minutes = ?, total_amount = ?, paid_amount = ?, payment_mode = ?, status = 'COMPLETED'
                WHERE session_id = ?
            """, (end_str, duration_mins, total_amt, total_amt, payment_mode, session_id))

            cur.execute("""
                UPDATE game_station
                SET status = 'AVAILABLE', current_session_id = NULL
                WHERE station_id = ?
            """, (row["station_id"],))

            conn.commit()
            return {
                "session_id": session_id,
                "station_id": row["station_id"],
                "station_name": row["station_name"],
                "customer_id": row["customer_id"],
                "customer_name": row["customer_name"],
                "start_time": str(row["start_time"]),
                "end_time": end_str,
                "duration_minutes": duration_mins,
                "rate_per_hour": rate,
                "total_amount": total_amt,
                "paid_amount": total_amt,
                "payment_mode": payment_mode,
                "user_id": row["user_id"],
            }
        finally:
            close_connection(conn)


class AuditLogDAO:
    def get_recent_logs(self, limit: int = 100) -> List[dict]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("""
                SELECT logId as log_id, userId as user_id, action, tableName as table_name, recordId as record_id, oldValue as old_value, newValue as new_value, actionDate as action_date
                FROM audit_log
                ORDER BY logId DESC
                LIMIT ?
            """, (limit,))
            rows = cur.fetchall()
            return [dict(r) for r in rows]
        finally:
            close_connection(conn)


class UserDAO:
    def find_by_username(self, username: str) -> Optional[User]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM user WHERE username=?", (username,))
            row = cur.fetchone()
            return _row_to_user(row) if row else None
        finally:
            close_connection(conn)

    def find_by_id(self, user_id: int) -> Optional[User]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM user WHERE userId=?", (user_id,))
            row = cur.fetchone()
            return _row_to_user(row) if row else None
        finally:
            close_connection(conn)

    def get_all_users(self) -> List[User]:
        return get_all_users()

    def insert_user(self, user: User) -> bool:
        return save_user(user)

    def update_user(self, user: User) -> bool:
        return save_user(user)

    def delete_user(self, user_id: int) -> bool:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("DELETE FROM user WHERE userId=?", (user_id,))
            conn.commit()
            return True
        finally:
            close_connection(conn)


class CustomerDAO:
    def get_all_customers(self) -> List[Customer]:
        return get_all_customers()

    def find_by_id(self, customer_id: int) -> Optional[Customer]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM customer WHERE customerId=?", (customer_id,))
            row = cur.fetchone()
            return _row_to_customer(row) if row else None
        finally:
            close_connection(conn)

    def insert_customer(self, customer: Customer) -> int:
        save_customer(customer)
        return customer.customer_id or 1

    def update_customer(self, customer: Customer) -> bool:
        return save_customer(customer)

    def get_next_customer_id(self) -> int:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT COALESCE(MAX(customerId), 0) + 1 FROM customer")
            return cur.fetchone()[0]
        finally:
            close_connection(conn)


class ItemMasterDAO:
    def get_all_items(self) -> List[ItemMaster]:
        return get_all_items()

    def get_low_stock_items(self) -> List[ItemMaster]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM item_master WHERE stockQuantity <= reorderLevel AND status='ACTIVE'")
            return [_row_to_item(r) for r in cur.fetchall()]
        finally:
            close_connection(conn)

    def find_by_id(self, item_id: int) -> Optional[ItemMaster]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM item_master WHERE itemId=?", (item_id,))
            row = cur.fetchone()
            return _row_to_item(row) if row else None
        finally:
            close_connection(conn)

    def find_by_code(self, item_code: str) -> Optional[ItemMaster]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM item_master WHERE itemCode=?", (item_code,))
            row = cur.fetchone()
            return _row_to_item(row) if row else None
        finally:
            close_connection(conn)

    def insert_item(self, item: ItemMaster) -> int:
        save_item(item)
        return item.item_id or 1

    def update_item(self, item: ItemMaster) -> bool:
        return save_item(item)


class BillDAO:
    def get_all_bills(self) -> List[Bill]:
        return get_all_bills()

    def get_recent_bills(self, limit: int = 50) -> List[Bill]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM bill ORDER BY billId DESC LIMIT ?", (limit,))
            return [_row_to_bill(r) for r in cur.fetchall()]
        finally:
            close_connection(conn)

    def get_bills_by_date(self, date_str: str) -> List[Bill]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            start = f"{date_str} 00:00:00"
            end = f"{date_str} 23:59:59"
            cur.execute("SELECT * FROM bill WHERE billDate >= ? AND billDate <= ?", (start, end))
            return [_row_to_bill(r) for r in cur.fetchall()]
        finally:
            close_connection(conn)

    def get_next_bill_number(self) -> int:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT COALESCE(MAX(billId), 0) + 1 FROM bill")
            return cur.fetchone()[0]
        finally:
            close_connection(conn)

    def insert_bill(self, bill: Bill) -> int:
        res = save_bill(bill)
        return bill.bill_id if res else 0


class ExpenseDAO:
    def get_all_expenses(self) -> List[Expense]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM expense ORDER BY expense_id DESC")
            rows = cur.fetchall()
            return [
                Expense(
                    expense_id=r["expense_id"],
                    category=r["category"],
                    description=r["description"] or "",
                    amount=float(r["amount"]),
                    payment_mode=r["payment_mode"] or "CASH",
                    expense_date=str(r["expense_date"]),
                    user_id=r["user_id"] if "user_id" in r.keys() else 0,
                )
                for r in rows
            ]
        finally:
            close_connection(conn)

    def get_expenses_by_date(self, date_str: str) -> List[Expense]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            start = f"{date_str} 00:00:00"
            end = f"{date_str} 23:59:59"
            cur.execute("SELECT * FROM expense WHERE expense_date >= ? AND expense_date <= ?", (start, end))
            rows = cur.fetchall()
            return [
                Expense(
                    expense_id=r["expense_id"],
                    category=r["category"],
                    description=r["description"] or "",
                    amount=float(r["amount"]),
                    payment_mode=r["payment_mode"] or "CASH",
                    expense_date=str(r["expense_date"]),
                    user_id=r["user_id"] if "user_id" in r.keys() else 0,
                )
                for r in rows
            ]
        finally:
            close_connection(conn)

    def insert_expense(self, expense: Expense) -> int:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("""
                INSERT INTO expense (expense_date, category, description, amount, payment_mode, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
            """, (expense.expense_date, expense.category, expense.description, expense.amount, expense.payment_mode, expense.user_id))
            conn.commit()
            return cur.lastrowid
        finally:
            close_connection(conn)


class SupplierDAO:
    def get_all_suppliers(self) -> List[Supplier]:
        return get_all_suppliers()

    def find_by_id(self, supplier_id: int) -> Optional[Supplier]:
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute("SELECT * FROM supplier WHERE supplierId=?", (supplier_id,))
            row = cur.fetchone()
            return _row_to_supplier(row) if row else None
        finally:
            close_connection(conn)

    def insert_supplier(self, supplier: Supplier) -> int:
        save_supplier(supplier)
        return supplier.supplier_id or 1





