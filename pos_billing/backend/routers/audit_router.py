# pos_billing/backend/routers/audit_router.py
"""
Audit Logging & Security History Router.
"""

from typing import List
from fastapi import APIRouter, Depends
from pos_billing.backend.auth import require_admin
from pos_billing.backend.schemas import AuditLogOut
from pos_billing.database.dao import AuditLogDAO
from pos_billing.database.models import User

router = APIRouter(prefix="/api/v1/audit-logs", tags=["Audit & Security Logs"])


@router.get("", response_model=List[AuditLogOut])
def get_audit_logs(limit: int = 100, current_user: User = Depends(require_admin)):
    dao = AuditLogDAO()
    logs = dao.get_recent_logs(limit=limit)
    return [
        AuditLogOut(
            log_id=l["log_id"],
            user_id=l["user_id"],
            action=l["action"],
            table_name=l["table_name"],
            record_id=l["record_id"],
            old_value=l.get("old_value", ""),
            new_value=l.get("new_value", ""),
            action_date=str(l["action_date"]),
        )
        for l in logs
    ]
