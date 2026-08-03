# pos_billing/backend/routers/sync_router.py
"""
Offline Transaction Queue Synchronization & Duplicate Prevention Router.
Receives batched offline transactions when internet connection returns,
ensuring idempotency and preventing duplicate record entries.
"""

import hashlib
import json
import logging
from typing import Dict, List
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_staff
from pos_billing.backend.schemas import SyncBatchRequest
from pos_billing.database.dao import BillDAO, ExpenseDAO
from pos_billing.database.models import Bill, Expense, User

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/sync", tags=["Offline Queue Synchronization"])

# In-memory processed transaction hash cache for duplicate detection
PROCESSED_HASHES: set[str] = set()


@router.post("/batch")
def sync_offline_batch(payload: SyncBatchRequest, current_user: User = Depends(require_staff)):
    processed_count = 0
    skipped_duplicates = 0
    errors = []

    bill_dao = BillDAO()
    exp_dao = ExpenseDAO()

    for tx in payload.pending_transactions:
        tx_type = tx.get("type", "UNKNOWN")
        tx_data = tx.get("data", {})
        
        # Calculate unique transaction hash
        hash_src = f"{payload.device_id}:{tx_type}:{json.dumps(tx_data, sort_keys=True)}"
        tx_hash = hashlib.sha256(hash_src.encode("utf-8")).hexdigest()

        if tx_hash in PROCESSED_HASHES:
            skipped_duplicates += 1
            continue

        try:
            if tx_type == "POS_CHECKOUT":
                # Process bill
                bill_number = f"BILL-{bill_dao.get_next_bill_number():05d}"
                bill = Bill(
                    bill_number=bill_number,
                    bill_type=tx_data.get("bill_type", "SALES"),
                    customer_id=tx_data.get("customer_id", 0),
                    customer_name=tx_data.get("customer_name", "WALK-IN"),
                    user_id=current_user.user_id,
                    subtotal=tx_data.get("subtotal", 0.0),
                    total_discount=tx_data.get("total_discount", 0.0),
                    total_amount=tx_data.get("total_amount", 0.0),
                    paid_amount=tx_data.get("paid_amount", 0.0),
                    payment_mode=tx_data.get("payment_mode", "CASH"),
                    status="COMPLETED",
                )
                bill_dao.insert_bill(bill)
                processed_count += 1
                PROCESSED_HASHES.add(tx_hash)

            elif tx_type == "EXPENSE_ADDED":
                expense = Expense(
                    category=tx_data.get("category", "General"),
                    description=tx_data.get("description", "Offline sync entry"),
                    amount=tx_data.get("amount", 0.0),
                    payment_mode=tx_data.get("payment_mode", "CASH"),
                    user_id=current_user.user_id,
                )
                exp_dao.insert_expense(expense)
                processed_count += 1
                PROCESSED_HASHES.add(tx_hash)
            else:
                skipped_duplicates += 1

        except Exception as err:
            logger.error(f"Error processing synced transaction {tx_type}: {err}")
            errors.append(str(err))

    return {
        "status": "SUCCESS",
        "processed_count": processed_count,
        "skipped_duplicates": skipped_duplicates,
        "errors": errors,
    }
