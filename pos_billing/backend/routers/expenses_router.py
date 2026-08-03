# pos_billing/backend/routers/expenses_router.py
"""
Expenses Management Router.
Broadcasts EXPENSE_ADDED event over WebSocket when expenses are recorded.
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_staff
from pos_billing.backend.schemas import ExpenseCreate, ExpenseOut
from pos_billing.backend.websocket_manager import ws_manager
from pos_billing.database.dao import ExpenseDAO
from pos_billing.database.models import Expense, User

router = APIRouter(prefix="/api/v1/expenses", tags=["Expenses Management"])


@router.get("", response_model=List[ExpenseOut])
def get_all_expenses(current_user: User = Depends(require_staff)):
    dao = ExpenseDAO()
    expenses = dao.get_all_expenses()
    return [
        ExpenseOut(
            expense_id=e.expense_id,
            category=e.category,
            description=e.description,
            amount=e.amount,
            payment_mode=e.payment_mode,
            expense_date=str(e.expense_date),
            user_id=e.user_id,
        )
        for e in expenses
    ]


@router.post("", response_model=ExpenseOut, status_code=status.HTTP_201_CREATED)
async def create_expense(payload: ExpenseCreate, current_user: User = Depends(require_staff)):
    dao = ExpenseDAO()
    expense = Expense(
        category=payload.category,
        description=payload.description,
        amount=payload.amount,
        payment_mode=payload.payment_mode,
        user_id=current_user.user_id,
    )
    exp_id = dao.insert_expense(expense)
    expense.expense_id = exp_id

    # Broadcast WebSocket notification
    await ws_manager.broadcast(
        event_type="EXPENSE_ADDED",
        data={
            "expense_id": exp_id,
            "category": expense.category,
            "description": expense.description,
            "amount": expense.amount,
            "payment_mode": expense.payment_mode,
        },
        sender_id=current_user.user_id,
    )

    return ExpenseOut(
        expense_id=expense.expense_id,
        category=expense.category,
        description=expense.description,
        amount=expense.amount,
        payment_mode=expense.payment_mode,
        expense_date=str(expense.expense_date),
        user_id=expense.user_id,
    )
