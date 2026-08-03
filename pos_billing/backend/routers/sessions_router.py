# pos_billing/backend/routers/sessions_router.py
"""
Game Stations & Customer Check-In / Check-Out Sessions Router.
Provides real-time station management, active session timers, check-in, check-out billing,
and broadcasts live WebSocket updates.
"""

from datetime import datetime
from typing import Dict, List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import require_staff
from pos_billing.backend.schemas import (
    GameSessionCheckout,
    GameSessionCreate,
    GameSessionOut,
    GameStationOut,
)
from pos_billing.backend.websocket_manager import ws_manager
from pos_billing.database.dao import CustomerDAO, GameSessionDAO
from pos_billing.database.models import User

router = APIRouter(prefix="/api/v1/sessions", tags=["Game Stations & Customer Sessions"])


@router.get("/stations", response_model=List[GameStationOut])
def get_all_stations(current_user: User = Depends(require_staff)):
    dao = GameSessionDAO()
    stations = dao.get_all_stations()
    return [
        GameStationOut(
            station_id=s["station_id"],
            station_name=s["station_name"],
            station_type=s["station_type"],
            hourly_rate=float(s["hourly_rate"]),
            status=s["status"],
            current_session_id=s.get("current_session_id"),
        )
        for s in stations
    ]


@router.get("/active", response_model=List[GameSessionOut])
def get_active_sessions(current_user: User = Depends(require_staff)):
    dao = GameSessionDAO()
    sessions = dao.get_active_sessions()
    return [
        GameSessionOut(
            session_id=s["session_id"],
            station_id=s["station_id"],
            station_name=s["station_name"],
            customer_id=s["customer_id"],
            customer_name=s["customer_name"],
            start_time=s["start_time"],
            end_time=s.get("end_time"),
            duration_minutes=float(s.get("duration_minutes", 0.0)),
            rate_per_hour=float(s["rate_per_hour"]),
            total_amount=float(s.get("total_amount", 0.0)),
            paid_amount=float(s.get("paid_amount", 0.0)),
            payment_mode=s.get("payment_mode", ""),
            status=s["status"],
            user_id=s.get("user_id", 0),
        )
        for s in sessions
    ]


@router.post("/checkin", response_model=GameSessionOut)
async def checkin_customer(payload: GameSessionCreate, current_user: User = Depends(require_staff)):
    dao = GameSessionDAO()
    cust_dao = CustomerDAO()

    customer = cust_dao.find_by_id(payload.customer_id)
    if not customer:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Customer ID {payload.customer_id} not found.",
        )

    session_data = dao.start_session(
        station_id=payload.station_id,
        customer_id=payload.customer_id,
        customer_name=customer.customer_name,
        rate_per_hour=payload.rate_per_hour,
        user_id=current_user.user_id,
    )
    if not session_data:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Station is currently occupied or unavailable.",
        )

    response = GameSessionOut(
        session_id=session_data["session_id"],
        station_id=session_data["station_id"],
        station_name=session_data["station_name"],
        customer_id=session_data["customer_id"],
        customer_name=session_data["customer_name"],
        start_time=session_data["start_time"],
        end_time=None,
        duration_minutes=0.0,
        rate_per_hour=float(session_data["rate_per_hour"]),
        total_amount=0.0,
        paid_amount=0.0,
        payment_mode="",
        status="ACTIVE",
        user_id=current_user.user_id,
    )

    # Broadcast WebSocket notification
    await ws_manager.broadcast(
        event_type="CUSTOMER_CHECKIN",
        data={
            "session_id": response.session_id,
            "station_name": response.station_name,
            "customer_name": response.customer_name,
            "start_time": response.start_time,
        },
        sender_id=current_user.user_id,
    )

    return response


@router.post("/checkout", response_model=GameSessionOut)
async def checkout_customer(payload: GameSessionCheckout, current_user: User = Depends(require_staff)):
    dao = GameSessionDAO()
    completed_session = dao.end_session(
        session_id=payload.session_id,
        payment_mode=payload.payment_mode,
        discount=payload.discount,
    )
    if not completed_session:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Session ID {payload.session_id} is not active or not found.",
        )

    response = GameSessionOut(
        session_id=completed_session["session_id"],
        station_id=completed_session["station_id"],
        station_name=completed_session["station_name"],
        customer_id=completed_session["customer_id"],
        customer_name=completed_session["customer_name"],
        start_time=completed_session["start_time"],
        end_time=completed_session["end_time"],
        duration_minutes=float(completed_session["duration_minutes"]),
        rate_per_hour=float(completed_session["rate_per_hour"]),
        total_amount=float(completed_session["total_amount"]),
        paid_amount=float(completed_session["paid_amount"]),
        payment_mode=completed_session["payment_mode"],
        status="COMPLETED",
        user_id=completed_session.get("user_id", current_user.user_id),
    )

    # Broadcast WebSocket notification
    await ws_manager.broadcast(
        event_type="CUSTOMER_CHECKOUT",
        data={
            "session_id": response.session_id,
            "station_name": response.station_name,
            "customer_name": response.customer_name,
            "total_amount": response.total_amount,
            "duration_minutes": response.duration_minutes,
        },
        sender_id=current_user.user_id,
    )

    return response
