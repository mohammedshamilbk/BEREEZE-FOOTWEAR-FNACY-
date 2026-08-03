# pos_billing/backend/routers/users_router.py
"""
Users & Roles Management Router.
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from pos_billing.backend.auth import hash_password, require_admin, get_current_user
from pos_billing.backend.schemas import UserCreate, UserOut, UserUpdate
from pos_billing.database.dao import UserDAO
from pos_billing.database.models import User

router = APIRouter(prefix="/api/v1/users", tags=["Users Management"])


@router.get("", response_model=List[UserOut])
def get_all_users(current_user: User = Depends(require_admin)):
    dao = UserDAO()
    users = dao.get_all_users()
    return [
        UserOut(
            user_id=u.user_id,
            username=u.username,
            full_name=u.full_name,
            role=u.role,
            email=u.email,
            phone=u.phone,
            status=u.status,
            daily_sales_target=u.daily_sales_target,
            total_sales_achieved=u.total_sales_achieved,
            created_date=str(u.created_date),
        )
        for u in users
    ]


@router.post("", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def create_user(payload: UserCreate, current_user: User = Depends(require_admin)):
    dao = UserDAO()
    existing = dao.find_by_username(payload.username)
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Username '{payload.username}' already exists.",
        )

    new_user = User(
        username=payload.username,
        password=hash_password(payload.password),
        full_name=payload.full_name,
        role=payload.role,
        email=payload.email or "",
        phone=payload.phone or "",
        daily_sales_target=payload.daily_sales_target,
    )
    success = dao.insert_user(new_user)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create user record.",
        )

    created = dao.find_by_username(payload.username)
    return UserOut(
        user_id=created.user_id,
        username=created.username,
        full_name=created.full_name,
        role=created.role,
        email=created.email,
        phone=created.phone,
        status=created.status,
        daily_sales_target=created.daily_sales_target,
        total_sales_achieved=created.total_sales_achieved,
        created_date=str(created.created_date),
    )


@router.put("/{user_id}", response_model=UserOut)
def update_user(user_id: int, payload: UserUpdate, current_user: User = Depends(require_admin)):
    dao = UserDAO()
    user = dao.find_by_id(user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User with ID {user_id} not found.",
        )

    if payload.full_name is not None:
        user.full_name = payload.full_name
    if payload.role is not None:
        user.role = payload.role
    if payload.email is not None:
        user.email = payload.email
    if payload.phone is not None:
        user.phone = payload.phone
    if payload.status is not None:
        user.status = payload.status
    if payload.password:
        user.password = hash_password(payload.password)
    if payload.daily_sales_target is not None:
        user.daily_sales_target = payload.daily_sales_target

    dao.update_user(user)
    updated = dao.find_by_id(user_id)
    return UserOut(
        user_id=updated.user_id,
        username=updated.username,
        full_name=updated.full_name,
        role=updated.role,
        email=updated.email,
        phone=updated.phone,
        status=updated.status,
        daily_sales_target=updated.daily_sales_target,
        total_sales_achieved=updated.total_sales_achieved,
        created_date=str(updated.created_date),
    )


@router.delete("/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_user(user_id: int, current_user: User = Depends(require_admin)):
    dao = UserDAO()
    user = dao.find_by_id(user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User with ID {user_id} not found.",
        )
    dao.delete_user(user_id)
    return None
