# pos_billing/backend/routers/pos_router.py
"""
POS Billing & Cart Checkout Router.
Handles sales billing, inventory auto-deduction, customer credit balance updates,
loyalty points, dynamic UPI QR pre-fill payload, and broadcasts BILL_CREATED event over WebSocket.
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_staff
from pos_billing.backend.config import settings
from pos_billing.backend.schemas import BillItemOut, BillOut, CheckoutRequest
from pos_billing.backend.websocket_manager import ws_manager
from pos_billing.database.dao import BillDAO, CustomerDAO, ItemMasterDAO
from pos_billing.database.models import Bill, BillItem, User
from pos_billing.utils.qr_generator import generate_upi_qr_data_url

router = APIRouter(prefix="/api/v1/pos", tags=["POS Billing"])


@router.post("/checkout", response_model=BillOut)
async def checkout_bill(payload: CheckoutRequest, current_user: User = Depends(require_staff)):
    bill_dao = BillDAO()
    cust_dao = CustomerDAO()
    item_dao = ItemMasterDAO()

    customer = cust_dao.find_by_id(payload.customer_id)
    cust_name = customer.customer_name if customer else "WALK-IN CUSTOMER"
    cust_phone = customer.phone if customer else ""

    bill_number = f"BILL-{bill_dao.get_next_bill_number():05d}"
    bill = Bill(
        bill_number=bill_number,
        bill_type=payload.bill_type,
        customer_id=payload.customer_id,
        customer_name=cust_name,
        customer_phone=cust_phone,
        user_id=current_user.user_id,
        subtotal=payload.subtotal,
        total_discount=payload.total_discount,
        total_amount=payload.total_amount,
        paid_amount=payload.paid_amount,
        balance_amount=payload.total_amount - payload.paid_amount,
        payment_mode=payload.payment_mode,
        remarks=payload.remarks or "",
        status="COMPLETED" if payload.paid_amount >= payload.total_amount else "PENDING",
    )

    # Process Bill Items & deduct inventory stock
    for item_req in payload.bill_items:
        master_item = item_dao.find_by_id(item_req.item_id)
        if not master_item:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Item ID {item_req.item_id} not found in inventory.",
            )
        bill_item = BillItem(
            item_id=master_item.item_id,
            item_code=master_item.item_code,
            item_name=master_item.item_name,
            quantity=item_req.quantity,
            unit_price=item_req.unit_price,
            discount=item_req.discount,
            purchase_price=master_item.purchase_price,
        )
        bill.add_bill_item(bill_item)

    # Save to Database using parameterized transaction
    bill_id = bill_dao.insert_bill(bill)
    if not bill_id:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to save bill transaction.",
        )

    # Credit loyalty points & update outstanding balance if customer selected
    if customer:
        customer.add_loyalty_points(payload.total_amount)
        if bill.balance_amount > 0:
            customer.outstanding_amount += bill.balance_amount
        cust_dao.update_customer(customer)

    bill.bill_id = bill_id

    # Broadcast WebSocket notification
    await ws_manager.broadcast(
        event_type="BILL_CREATED",
        data={
            "bill_id": bill_id,
            "bill_number": bill.bill_number,
            "customer_name": cust_name,
            "total_amount": bill.total_amount,
            "payment_mode": bill.payment_mode,
        },
        sender_id=current_user.user_id,
    )

    return BillOut(
        bill_id=bill.bill_id,
        bill_number=bill.bill_number,
        bill_type=bill.bill_type,
        bill_date=str(bill.bill_date),
        customer_id=bill.customer_id,
        customer_name=bill.customer_name,
        user_id=bill.user_id,
        subtotal=bill.subtotal,
        total_discount=bill.total_discount,
        total_amount=bill.total_amount,
        paid_amount=bill.paid_amount,
        balance_amount=bill.balance_amount,
        payment_mode=bill.payment_mode,
        status=bill.status,
        remarks=bill.remarks,
        bill_items=[
            BillItemOut(
                bill_item_id=bi.bill_item_id,
                bill_id=bill.bill_id,
                item_id=bi.item_id,
                item_code=bi.item_code,
                item_name=bi.item_name,
                quantity=bi.quantity,
                unit_price=bi.unit_price,
                discount=bi.discount,
                total_amount=bi.total_amount,
            )
            for bi in bill.bill_items
        ],
    )


@router.get("/upi-qr")
def get_upi_qr(amount: float, payee_name: Optional[str] = None):
    """Generates dynamic UPI QR Code Data URL pre-filled with bill amount."""
    vpa = settings.STORE_VPA
    name = payee_name or settings.STORE_NAME
    qr_data_url = generate_upi_qr_data_url(vpa, name, amount)
    return {
        "vpa": vpa,
        "payee_name": name,
        "amount": amount,
        "qr_code": qr_data_url,
    }


@router.get("/bills", response_model=List[BillOut])
def get_recent_bills(limit: int = 50, current_user: User = Depends(require_staff)):
    dao = BillDAO()
    bills = dao.get_recent_bills(limit=limit)
    return [
        BillOut(
            bill_id=b.bill_id,
            bill_number=b.bill_number,
            bill_type=b.bill_type,
            bill_date=str(b.bill_date),
            customer_id=b.customer_id,
            customer_name=b.customer_name,
            user_id=b.user_id,
            subtotal=b.subtotal,
            total_discount=b.total_discount,
            total_amount=b.total_amount,
            paid_amount=b.paid_amount,
            balance_amount=b.balance_amount,
            payment_mode=b.payment_mode,
            status=b.status,
            remarks=b.remarks,
            bill_items=[],
        )
        for b in bills
    ]
