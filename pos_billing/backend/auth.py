# pos_billing/backend/auth.py
"""
Authentication module handling JWT tokens, password hashing & verification,
and FastAPI Security Dependencies with Role-Based Access Control (RBAC).
Includes zero-dependency JWT implementation fallback.
"""

import base64
import hashlib
import hmac
import json
import time
from datetime import datetime, timedelta, timezone
from typing import Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from pydantic import BaseModel

from pos_billing.backend.config import settings
from pos_billing.database.dao import UserDAO
from pos_billing.database.models import User

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")


class Token(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int
    user: dict


class TokenData(BaseModel):
    username: Optional[str] = None
    role: Optional[str] = None
    user_id: Optional[int] = None


# --- Pure Python Standard Library JWT Fallback ---
def _b64_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("utf-8").rstrip("=")


def _b64_decode(data_str: str) -> bytes:
    padding = "=" * (4 - (len(data_str) % 4))
    return base64.urlsafe_b64decode(data_str + padding)


def jwt_encode(payload: dict, secret: str) -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    header_b64 = _b64_encode(json.dumps(header).encode("utf-8"))
    payload_b64 = _b64_encode(json.dumps(payload, default=str).encode("utf-8"))
    signature_input = f"{header_b64}.{payload_b64}".encode("utf-8")
    sig = hmac.new(secret.encode("utf-8"), signature_input, hashlib.sha256).digest()
    sig_b64 = _b64_encode(sig)
    return f"{header_b64}.{payload_b64}.{sig_b64}"


def jwt_decode(token: str, secret: str) -> dict:
    parts = token.split(".")
    if len(parts) != 3:
        raise ValueError("Invalid token structure")
    header_b64, payload_b64, sig_b64 = parts
    signature_input = f"{header_b64}.{payload_b64}".encode("utf-8")
    expected_sig = _b64_encode(hmac.new(secret.encode("utf-8"), signature_input, hashlib.sha256).digest())
    if not hmac.compare_digest(sig_b64, expected_sig):
        raise ValueError("Invalid signature")
    payload = json.loads(_b64_decode(payload_b64).decode("utf-8"))
    if "exp" in payload and time.time() > payload["exp"]:
        raise ValueError("Token expired")
    return payload


def hash_password(password: str) -> str:
    """Returns SHA-256 hashed password matching system standard."""
    return hashlib.sha256(password.encode("utf-8")).hexdigest()


def verify_password(plain_password: str, hashed_password: str, username: str = "") -> bool:
    """Supports Plain-text, unsalted SHA-256, and salted SHA-256."""
    if not plain_password or not hashed_password:
        return False
    if plain_password == hashed_password:
        return True
    if hashlib.sha256(plain_password.encode("utf-8")).hexdigest() == hashed_password:
        return True
    salted = hashlib.sha256(f"{plain_password}::{username}".encode("utf-8")).hexdigest()
    if salted == hashed_password:
        return True
    return False


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    to_encode = data.copy()
    expire_ts = time.time() + (expires_delta.total_seconds() if expires_delta else settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60)
    to_encode.update({"exp": expire_ts, "type": "access"})
    return jwt_encode(to_encode, settings.SECRET_KEY)


def create_refresh_token(data: dict) -> str:
    to_encode = data.copy()
    expire_ts = time.time() + (settings.REFRESH_TOKEN_EXPIRE_DAYS * 86400)
    to_encode.update({"exp": expire_ts, "type": "refresh"})
    return jwt_encode(to_encode, settings.SECRET_KEY)


def get_current_user(token: str = Depends(oauth2_scheme)) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt_decode(token, settings.SECRET_KEY)
        username: str = payload.get("sub")
        if username is None or payload.get("type") != "access":
            raise credentials_exception
        token_data = TokenData(
            username=username,
            role=payload.get("role"),
            user_id=payload.get("user_id"),
        )
    except Exception:
        raise credentials_exception

    dao = UserDAO()
    user = dao.find_by_username(token_data.username)
    if user is None or user.status != "ACTIVE":
        raise credentials_exception
    return user


class RoleChecker:
    def __init__(self, allowed_roles: list[str]):
        self.allowed_roles = [r.upper() for r in allowed_roles]

    def __call__(self, current_user: User = Depends(get_current_user)) -> User:
        user_role = (current_user.role or "").upper()
        if user_role in ("SUPER_ADMIN", "SUPERADMIN", "ADMIN"):
            return current_user
        if user_role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"User with role '{current_user.role}' lacks sufficient permissions.",
            )
        return current_user


require_super_admin = RoleChecker(["SUPER_ADMIN", "SUPERADMIN"])
require_admin = RoleChecker(["ADMIN", "SUPER_ADMIN", "SUPERADMIN"])
require_manager = RoleChecker(["ADMIN", "MANAGER", "SUPER_ADMIN", "SUPERADMIN"])
require_staff = RoleChecker(["ADMIN", "MANAGER", "STAFF", "CASHIER", "SUPER_ADMIN", "SUPERADMIN"])
