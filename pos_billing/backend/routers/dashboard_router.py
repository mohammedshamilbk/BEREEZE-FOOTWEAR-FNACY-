# pos_billing/backend/routers/dashboard_router.py
"""
Real-Time Dashboard & Yesterday vs Today Comparative Statistics Router.
"""

from datetime import datetime, timedelta
from fastapi import APIRouter, Depends
from pos_billing.backend.auth import require_staff
from pos_billing.backend.schemas import DashboardStatsOut
from pos_billing.database.dao import (
    BillDAO,
    CustomerDAO,
    ExpenseDAO,
    GameSessionDAO,
)
from pos_billing.database.models import User

router = APIRouter(prefix="/api/v1/dashboard", tags=["Dashboard Statistics"])


@router.get("/stats", response_model=DashboardStatsOut)
def get_dashboard_stats(current_user: User = Depends(require_staff)):
    today_str = datetime.now().strftime("%Y-%m-%d")
    yesterday_str = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")

    session_dao = GameSessionDAO()
    cust_dao = CustomerDAO()
    bill_dao = BillDAO()
    exp_dao = ExpenseDAO()

    # Active customers & game stations
    active_sessions = session_dao.get_active_sessions()
    stations = session_dao.get_all_stations()

    occupied = len(active_sessions)
    total_stations = len(stations)
    available = max(0, total_stations - occupied)

    # Today's & Yesterday's sales / bills
    today_bills = bill_dao.get_bills_by_date(today_str)
    yesterday_bills = bill_dao.get_bills_by_date(yesterday_str)

    today_rev = sum(b.total_amount for b in today_bills if b.status == "COMPLETED")
    yesterday_rev = sum(b.total_amount for b in yesterday_bills if b.status == "COMPLETED")

    pending_payments = sum(b.balance_amount for b in today_bills if b.status == "PENDING")

    # Today's & Yesterday's expenses
    today_expenses_list = exp_dao.get_expenses_by_date(today_str)
    yesterday_expenses_list = exp_dao.get_expenses_by_date(yesterday_str)

    today_exp = sum(e.amount for e in today_expenses_list)
    yesterday_exp = sum(e.amount for e in yesterday_expenses_list)

    today_profit = today_rev - today_exp

    all_customers = cust_dao.get_all_customers()

    return DashboardStatsOut(
        active_customers=len(all_customers),
        occupied_stations=occupied,
        available_stations=available,
        today_revenue=today_rev,
        today_bills=len(today_bills),
        pending_payments=pending_payments,
        today_expenses=today_exp,
        today_profit=today_profit,
        yesterday_revenue=yesterday_rev,
        yesterday_expenses=yesterday_exp,
        yesterday_bills=len(yesterday_bills),
    )
