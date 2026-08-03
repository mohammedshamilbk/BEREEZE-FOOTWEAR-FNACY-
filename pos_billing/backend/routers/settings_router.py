# pos_billing/backend/routers/settings_router.py
"""
Store Settings & Game Pricing Management Router.
"""

from typing import Dict
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_admin
from pos_billing.backend.config import settings
from pos_billing.database.models import User

router = APIRouter(prefix="/api/v1/settings", tags=["System Settings"])


@router.get("")
def get_settings(current_user: User = Depends(require_admin)):
    return {
        "store_name": settings.STORE_NAME,
        "store_vpa": settings.STORE_VPA,
        "store_phone": settings.STORE_PHONE,
        "store_address": settings.STORE_ADDRESS,
        "environment": settings.ENV,
        "debug_mode": settings.DEBUG,
    }


@router.put("")
def update_settings(payload: Dict[str, str], current_user: User = Depends(require_admin)):
    if "store_vpa" in payload:
        settings.STORE_VPA = payload["store_vpa"]
    if "store_name" in payload:
        settings.STORE_NAME = payload["store_name"]
    return {"message": "Settings updated successfully", "settings": get_settings(current_user)}
