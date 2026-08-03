# pos_billing/backend/config.py
"""
Configuration settings for FastAPI Cloud Backend.
Loads environment variables with sensible defaults.
Uses standard Python dataclass / object for zero-dependency portability.
"""

import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent


class Settings:
    APP_NAME: str = "Bereeze Footwear Fancy POS Cloud API"
    ENV: str = os.getenv("ENV", "development")
    DEBUG: bool = os.getenv("DEBUG", "True").lower() in ("true", "1")
    
    # Secret Key for JWT & Security
    SECRET_KEY: str = os.getenv("SECRET_KEY", "bereeze_super_secret_jwt_key_2026_change_in_prod")
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24  # 24 hours
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7
    
    # Database Settings (PostgreSQL default with SQLite fallback)
    DATABASE_URL: str = os.getenv("DATABASE_URL", f"sqlite:///{BASE_DIR / 'pos_data.sqlite'}")
    
    # CORS Settings
    ALLOWED_ORIGINS: list[str] = ["*"]
    
    # Store / VPA Settings for Dynamic UPI QR
    STORE_NAME: str = "Bereeze Footwear Fancy"
    STORE_VPA: str = os.getenv("STORE_VPA", "bereezefootwear@upi")
    STORE_PHONE: str = "8086790086"
    STORE_ADDRESS: str = "Anar complex, Naya bazar, Melparamba, Kasaragod, Kerala, India 671317"


settings = Settings()
