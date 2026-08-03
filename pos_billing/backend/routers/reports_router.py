# pos_billing/backend/routers/reports_router.py
"""
Financial Summaries, P&L Analysis, and Reports Router.
Supports downloading day closing statement CSVs and multi-tab Excel workbooks.
"""

import io
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Response, status
from fastapi.responses import StreamingResponse
from pos_billing.backend.auth import require_manager, require_staff
from pos_billing.database.dao import BillDAO, ExpenseDAO
from pos_billing.database.models import User
from pos_billing.utils.excel_exporter import generate_excel_report

router = APIRouter(prefix="/api/v1/reports", tags=["Reports & Analytics"])


@router.get("/pnl")
def get_pnl_summary(
    start_date: str = "",
    end_date: str = "",
    current_user: User = Depends(require_manager),
):
    bill_dao = BillDAO()
    exp_dao = ExpenseDAO()

    all_bills = bill_dao.get_all_bills()
    all_expenses = exp_dao.get_all_expenses()

    filtered_bills = [
        b for b in all_bills if b.status == "COMPLETED"
    ]
    if start_date:
        filtered_bills = [b for b in filtered_bills if str(b.bill_date)[:10] >= start_date]
    if end_date:
        filtered_bills = [b for b in filtered_bills if str(b.bill_date)[:10] <= end_date]

    filtered_expenses = all_expenses
    if start_date:
        filtered_expenses = [e for e in filtered_expenses if str(e.expense_date)[:10] >= start_date]
    if end_date:
        filtered_expenses = [e for e in filtered_expenses if str(e.expense_date)[:10] <= end_date]

    gross_revenue = sum(b.total_amount for b in filtered_bills)
    total_expenses = sum(e.amount for e in filtered_expenses)
    net_profit = gross_revenue - total_expenses

    return {
        "start_date": start_date or "ALL",
        "end_date": end_date or "ALL",
        "total_bills": len(filtered_bills),
        "gross_revenue": gross_revenue,
        "total_expenses": total_expenses,
        "net_profit": net_profit,
        "profit_margin_percent": round((net_profit / gross_revenue * 100), 2) if gross_revenue > 0 else 0.0,
    }


@router.get("/export/excel")
def export_excel_report(current_user: User = Depends(require_manager)):
    file_path = generate_excel_report()
    if not file_path or not file_path.exists():
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate Excel report.",
        )
    with open(file_path, "rb") as f:
        content = f.read()

    filename = file_path.name
    return Response(
        content=content,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f"attachment; filename={filename}"},
    )
