# pos_billing/backend/routers/inventory_router.py
"""
Inventory & Product Master Management Router.
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_staff, require_manager
from pos_billing.backend.schemas import ItemCreate, ItemOut, ItemUpdate
from pos_billing.database.dao import ItemMasterDAO
from pos_billing.database.models import ItemMaster, User

router = APIRouter(prefix="/api/v1/inventory", tags=["Inventory Management"])


@router.get("", response_model=List[ItemOut])
def get_all_items(current_user: User = Depends(require_staff)):
    dao = ItemMasterDAO()
    items = dao.get_all_items()
    return [
        ItemOut(
            item_id=i.item_id,
            item_code=i.item_code,
            item_name=i.item_name,
            category=i.category,
            manufacturer=i.manufacturer,
            purchase_price=i.purchase_price,
            selling_price=i.selling_price,
            barcode=i.barcode,
            stock_quantity=i.stock_quantity,
            reorder_level=i.reorder_level,
            size=i.size,
            color=i.color,
            material=i.material,
            status=i.status,
            created_date=str(i.created_date),
        )
        for i in items
    ]


@router.get("/low-stock", response_model=List[ItemOut])
def get_low_stock_items(current_user: User = Depends(require_staff)):
    dao = ItemMasterDAO()
    items = dao.get_low_stock_items()
    return [
        ItemOut(
            item_id=i.item_id,
            item_code=i.item_code,
            item_name=i.item_name,
            category=i.category,
            manufacturer=i.manufacturer,
            purchase_price=i.purchase_price,
            selling_price=i.selling_price,
            barcode=i.barcode,
            stock_quantity=i.stock_quantity,
            reorder_level=i.reorder_level,
            size=i.size,
            color=i.color,
            material=i.material,
            status=i.status,
            created_date=str(i.created_date),
        )
        for i in items
    ]


@router.post("", response_model=ItemOut, status_code=status.HTTP_201_CREATED)
def create_item(payload: ItemCreate, current_user: User = Depends(require_manager)):
    dao = ItemMasterDAO()
    existing = dao.find_by_code(payload.item_code)
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Item code '{payload.item_code}' already exists.",
        )

    new_item = ItemMaster(
        item_code=payload.item_code,
        item_name=payload.item_name,
        category=payload.category,
        manufacturer=payload.manufacturer or "",
        purchase_price=payload.purchase_price,
        selling_price=payload.selling_price,
        barcode=payload.barcode or payload.item_code,
        stock_quantity=payload.stock_quantity,
        reorder_level=payload.reorder_level,
        size=payload.size or "",
        color=payload.color or "",
        material=payload.material or "",
    )
    item_id = dao.insert_item(new_item)
    created = dao.find_by_id(item_id) or new_item
    return ItemOut(
        item_id=created.item_id,
        item_code=created.item_code,
        item_name=created.item_name,
        category=created.category,
        manufacturer=created.manufacturer,
        purchase_price=created.purchase_price,
        selling_price=created.selling_price,
        barcode=created.barcode,
        stock_quantity=created.stock_quantity,
        reorder_level=created.reorder_level,
        size=created.size,
        color=created.color,
        material=created.material,
        status=created.status,
        created_date=str(created.created_date),
    )


@router.put("/{item_id}", response_model=ItemOut)
def update_item(item_id: int, payload: ItemUpdate, current_user: User = Depends(require_manager)):
    dao = ItemMasterDAO()
    item = dao.find_by_id(item_id)
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Item ID {item_id} not found.",
        )

    if payload.item_name is not None:
        item.item_name = payload.item_name
    if payload.category is not None:
        item.category = payload.category
    if payload.manufacturer is not None:
        item.manufacturer = payload.manufacturer
    if payload.purchase_price is not None:
        item.purchase_price = payload.purchase_price
    if payload.selling_price is not None:
        item.selling_price = payload.selling_price
    if payload.barcode is not None:
        item.barcode = payload.barcode
    if payload.stock_quantity is not None:
        item.stock_quantity = payload.stock_quantity
    if payload.reorder_level is not None:
        item.reorder_level = payload.reorder_level
    if payload.size is not None:
        item.size = payload.size
    if payload.color is not None:
        item.color = payload.color
    if payload.material is not None:
        item.material = payload.material
    if payload.status is not None:
        item.status = payload.status

    dao.update_item(item)
    updated = dao.find_by_id(item_id)
    return ItemOut(
        item_id=updated.item_id,
        item_code=updated.item_code,
        item_name=updated.item_name,
        category=updated.category,
        manufacturer=updated.manufacturer,
        purchase_price=updated.purchase_price,
        selling_price=updated.selling_price,
        barcode=updated.barcode,
        stock_quantity=updated.stock_quantity,
        reorder_level=updated.reorder_level,
        size=updated.size,
        color=updated.color,
        material=updated.material,
        status=updated.status,
        created_date=str(updated.created_date),
    )
