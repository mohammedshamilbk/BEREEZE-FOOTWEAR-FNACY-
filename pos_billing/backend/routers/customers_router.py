# pos_billing/backend/routers/customers_router.py
"""
Customer Management Router.
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_staff
from pos_billing.backend.schemas import CustomerCreate, CustomerOut, CustomerUpdate
from pos_billing.database.dao import CustomerDAO
from pos_billing.database.models import Customer, User

router = APIRouter(prefix="/api/v1/customers", tags=["Customer Management"])


@router.get("", response_model=List[CustomerOut])
def get_all_customers(current_user: User = Depends(require_staff)):
    dao = CustomerDAO()
    customers = dao.get_all_customers()
    return [
        CustomerOut(
            customer_id=c.customer_id,
            customer_code=c.customer_code,
            customer_name=c.customer_name,
            phone=c.phone,
            email=c.email,
            address=c.address,
            city=c.city,
            state=c.state,
            pincode=c.pincode,
            credit_limit=c.credit_limit,
            outstanding_amount=c.outstanding_amount,
            customer_type=c.customer_type,
            loyalty_points=c.loyalty_points,
            status=c.status,
            registration_date=str(c.registration_date),
            last_purchase_date=str(c.last_purchase_date) if c.last_purchase_date else None,
        )
        for c in customers
    ]


@router.post("", response_model=CustomerOut, status_code=status.HTTP_201_CREATED)
def create_customer(payload: CustomerCreate, current_user: User = Depends(require_staff)):
    dao = CustomerDAO()
    code = f"CUST{dao.get_next_customer_id():04d}"
    new_customer = Customer(
        customer_code=code,
        customer_name=payload.customer_name,
        phone=payload.phone,
        email=payload.email or "",
        address=payload.address or "",
        city=payload.city or "",
        state=payload.state or "",
        pincode=payload.pincode or "",
        credit_limit=payload.credit_limit,
        customer_type=payload.customer_type,
    )
    cust_id = dao.insert_customer(new_customer)
    created = dao.find_by_id(cust_id) or new_customer
    return CustomerOut(
        customer_id=created.customer_id,
        customer_code=created.customer_code,
        customer_name=created.customer_name,
        phone=created.phone,
        email=created.email,
        address=created.address,
        city=created.city,
        state=created.state,
        pincode=created.pincode,
        credit_limit=created.credit_limit,
        outstanding_amount=created.outstanding_amount,
        customer_type=created.customer_type,
        loyalty_points=created.loyalty_points,
        status=created.status,
        registration_date=str(created.registration_date),
    )


@router.put("/{customer_id}", response_model=CustomerOut)
def update_customer(customer_id: int, payload: CustomerUpdate, current_user: User = Depends(require_staff)):
    dao = CustomerDAO()
    cust = dao.find_by_id(customer_id)
    if not cust:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Customer with ID {customer_id} not found.",
        )

    if payload.customer_name is not None:
        cust.customer_name = payload.customer_name
    if payload.phone is not None:
        cust.phone = payload.phone
    if payload.email is not None:
        cust.email = payload.email
    if payload.address is not None:
        cust.address = payload.address
    if payload.city is not None:
        cust.city = payload.city
    if payload.state is not None:
        cust.state = payload.state
    if payload.pincode is not None:
        cust.pincode = payload.pincode
    if payload.credit_limit is not None:
        cust.credit_limit = payload.credit_limit
    if payload.customer_type is not None:
        cust.customer_type = payload.customer_type
    if payload.status is not None:
        cust.status = payload.status

    dao.update_customer(cust)
    updated = dao.find_by_id(customer_id)
    return CustomerOut(
        customer_id=updated.customer_id,
        customer_code=updated.customer_code,
        customer_name=updated.customer_name,
        phone=updated.phone,
        email=updated.email,
        address=updated.address,
        city=updated.city,
        state=updated.state,
        pincode=updated.pincode,
        credit_limit=updated.credit_limit,
        outstanding_amount=updated.outstanding_amount,
        customer_type=updated.customer_type,
        loyalty_points=updated.loyalty_points,
        status=updated.status,
        registration_date=str(updated.registration_date),
        last_purchase_date=str(updated.last_purchase_date) if updated.last_purchase_date else None,
    )
