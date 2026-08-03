# pos_billing/backend/routers/suppliers_router.py
"""
Supplier Ledger & Purchase Invoices Router.
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_staff, require_manager
from pos_billing.backend.schemas import SupplierCreate, SupplierOut
from pos_billing.database.dao import SupplierDAO
from pos_billing.database.models import Supplier, User

router = APIRouter(prefix="/api/v1/suppliers", tags=["Supplier Ledger"])


@router.get("", response_model=List[SupplierOut])
def get_all_suppliers(current_user: User = Depends(require_staff)):
    dao = SupplierDAO()
    suppliers = dao.get_all_suppliers()
    return [
        SupplierOut(
            supplier_id=s.supplier_id,
            supplier_code=s.supplier_code,
            supplier_name=s.supplier_name,
            phone=s.phone,
            email=s.email,
            state=s.state,
            tax_regn=s.tax_regn,
            gstin=s.gstin,
            outstanding_balance=s.outstanding_balance,
            status=s.status,
        )
        for s in suppliers
    ]


@router.post("", response_model=SupplierOut, status_code=status.HTTP_201_CREATED)
def create_supplier(payload: SupplierCreate, current_user: User = Depends(require_manager)):
    dao = SupplierDAO()
    new_sup = Supplier(
        supplier_code=payload.supplier_code,
        supplier_name=payload.supplier_name,
        phone=payload.phone or "",
        email=payload.email or "",
        state=payload.state or "",
        tax_regn=payload.tax_regn or "",
        gstin=payload.gstin or "",
    )
    sup_id = dao.insert_supplier(new_sup)
    created = dao.find_by_id(sup_id) or new_sup
    return SupplierOut(
        supplier_id=created.supplier_id,
        supplier_code=created.supplier_code,
        supplier_name=created.supplier_name,
        phone=created.phone,
        email=created.email,
        state=created.state,
        tax_regn=created.tax_regn,
        gstin=created.gstin,
        outstanding_balance=created.outstanding_balance,
        status=created.status,
    )
