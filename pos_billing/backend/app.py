# pos_billing/backend/app.py
"""
Main FastAPI Enterprise Cloud Backend Application.
Assembles API Routers, CORS Middleware, WebSocket Endpoint, Health Checks,
and mounts static web assets.
"""

import logging
import sys
from pathlib import Path
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

# Ensure project root is in sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from pos_billing.backend.config import settings
from pos_billing.backend.routers import (
    audit_router,
    auth_router,
    backups_router,
    customers_router,
    dashboard_router,
    expenses_router,
    inventory_router,
    pos_router,
    reports_router,
    sessions_router,
    settings_router,
    suppliers_router,
    sync_router,
    users_router,
)
from pos_billing.backend.websocket_manager import ws_manager
from pos_billing.database.db_init import initialize_database

logger = logging.getLogger(__name__)

app = FastAPI(
    title=settings.APP_NAME,
    description="Enterprise Cloud Backend for Bereeze Footwear Fancy POS & Rental System",
    version="2.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def startup_event():
    logger.info("Initialising database tables...")
    try:
        initialize_database()
        logger.info("Database initialised successfully.")
    except Exception as exc:
        logger.warning(f"Database init warning: {exc}")


@app.get("/api/v1/health", tags=["Health Check"])
def health_check():
    return {
        "status": "HEALTHY",
        "app_name": settings.APP_NAME,
        "environment": settings.ENV,
        "version": "2.0.0",
    }


# Include Routers
app.include_router(auth_router.router)
app.include_router(users_router.router)
app.include_router(customers_router.router)
app.include_router(sessions_router.router)
app.include_router(pos_router.router)
app.include_router(inventory_router.router)
app.include_router(suppliers_router.router)
app.include_router(expenses_router.router)
app.include_router(dashboard_router.router)
app.include_router(reports_router.router)
app.include_router(settings_router.router)
app.include_router(backups_router.router)
app.include_router(audit_router.router)
app.include_router(sync_router.router)


# Real-time WebSocket Endpoint
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket, user_id: int = 0):
    await ws_manager.connect(websocket, user_id=user_id)
    try:
        while True:
            data = await websocket.receive_text()
            # Echo or ping message
            await ws_manager.send_personal_message({"type": "PONG", "payload": data}, websocket)
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket, user_id=user_id)


# Mount Web PWA static files if web directory exists
WEB_DIR = Path(__file__).resolve().parent.parent / "web"
if WEB_DIR.exists():
    app.mount("/", StaticFiles(directory=str(WEB_DIR), html=True), name="web")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("pos_billing.backend.app:app", host="0.0.0.0", port=8000, reload=True)
