# pos_billing/backend/routers/backups_router.py
"""
Automatic and Manual Database Backup & Disaster Recovery Router.
"""

import os
from pathlib import Path
from typing import List
from fastapi import APIRouter, Depends, HTTPException, Response, status
from pos_billing.backend.auth import require_admin
from pos_billing.database.models import User
from pos_billing.utils.backup_manager import (
    create_backup,
    list_backups,
    restore_backup,
)

router = APIRouter(prefix="/api/v1/backups", tags=["Database Backups"])


@router.get("")
def get_backup_list(current_user: User = Depends(require_admin)):
    backups = list_backups()
    return [{"filename": b.name, "size_bytes": b.stat().st_size, "modified_time": b.stat().st_mtime} for b in backups]


@router.post("/create")
def trigger_backup(current_user: User = Depends(require_admin)):
    path = create_backup()
    if not path:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Database backup creation failed.",
        )
    return {
        "message": "Backup created successfully",
        "filename": path.name,
        "size_bytes": path.stat().st_size,
    }


@router.post("/restore/{filename}")
def restore_database_backup(filename: str, current_user: User = Depends(require_admin)):
    backups = list_backups()
    target = next((b for b in backups if b.name == filename), None)
    if not target:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Backup file '{filename}' not found.",
        )

    success = restore_backup(target)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to restore database from backup.",
        )
    return {"message": f"Database successfully restored from {filename}"}
