# pos_billing/backend/routers/auth_router.py
"""
Authentication Router handling User Login, Refresh Tokens, Profile Fetching, and Logout.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from pos_billing.backend.auth import (
    create_access_token,
    create_refresh_token,
    get_current_user,
    verify_password,
    Token,
)
from pos_billing.backend.schemas import LoginRequest, RefreshTokenRequest, UserOut
from pos_billing.database.dao import UserDAO
from pos_billing.database.models import User

router = APIRouter(prefix="/api/v1/auth", tags=["Authentication"])


@router.post("/login", response_model=Token)
def login(request: LoginRequest):
    dao = UserDAO()
    user = dao.find_by_username(request.username)
    if not user or not verify_password(request.password, user.password, user.username):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if user.status != "ACTIVE":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive. Please contact your administrator.",
        )

    access_token = create_access_token(
        data={"sub": user.username, "role": user.role, "user_id": user.user_id}
    )
    refresh_token = create_refresh_token(
        data={"sub": user.username, "role": user.role, "user_id": user.user_id}
    )

    return Token(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=3600 * 24,
        user={
            "user_id": user.user_id,
            "username": user.username,
            "full_name": user.full_name,
            "role": user.role,
            "email": user.email,
        },
    )


@router.post("/login/form", response_model=Token)
def login_form(form_data: OAuth2PasswordRequestForm = Depends()):
    return login(LoginRequest(username=form_data.username, password=form_data.password))


@router.get("/me", response_model=UserOut)
def get_profile(current_user: User = Depends(get_current_user)):
    return UserOut(
        user_id=current_user.user_id,
        username=current_user.username,
        full_name=current_user.full_name,
        role=current_user.role,
        email=current_user.email,
        phone=current_user.phone,
        status=current_user.status,
        daily_sales_target=current_user.daily_sales_target,
        total_sales_achieved=current_user.total_sales_achieved,
        created_date=str(current_user.created_date),
    )


@router.post("/logout")
def logout(current_user: User = Depends(get_current_user)):
    return {"message": "Successfully logged out"}
