# pos_billing/backend/websocket_manager.py
"""
WebSocket Connection Manager for real-time synchronization across connected clients.
Broadcasting events:
  - CUSTOMER_CHECKIN / CHECKOUT
  - SESSION_START / SESSION_END
  - BILL_CREATED / PAYMENT_COMPLETED
  - EXPENSE_ADDED
  - STATION_STATUS_CHANGED
  - DASHBOARD_REFRESH
"""

import json
import logging
from typing import Dict, List
from fastapi import WebSocket

logger = logging.getLogger(__name__)


class ConnectionManager:
    def __init__(self):
        # Active connections list
        self.active_connections: List[WebSocket] = []
        # Connections mapped by user_id
        self.user_connections: Dict[int, List[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, user_id: int = 0):
        await websocket.accept()
        self.active_connections.append(websocket)
        if user_id:
            if user_id not in self.user_connections:
                self.user_connections[user_id] = []
            self.user_connections[user_id].append(websocket)
        logger.info(f"WebSocket connected. Total active connections: {len(self.active_connections)}")

    def disconnect(self, websocket: WebSocket, user_id: int = 0):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
        if user_id and user_id in self.user_connections:
            if websocket in self.user_connections[user_id]:
                self.user_connections[user_id].remove(websocket)
            if not self.user_connections[user_id]:
                del self.user_connections[user_id]
        logger.info(f"WebSocket disconnected. Remaining connections: {len(self.active_connections)}")

    async def send_personal_message(self, message: dict, websocket: WebSocket):
        try:
            await websocket.send_text(json.dumps(message))
        except Exception as err:
            logger.error(f"Failed to send personal WebSocket message: {err}")

    async def broadcast(self, event_type: str, data: dict, sender_id: int = 0):
        """Broadcast event to all connected clients."""
        payload = {
            "event": event_type,
            "data": data,
            "sender_id": sender_id
        }
        message_str = json.dumps(payload)
        disconnected = []
        for connection in self.active_connections:
            try:
                await connection.send_text(message_str)
            except Exception as err:
                logger.warning(f"Error sending broadcast message, scheduling disconnect: {err}")
                disconnected.append(connection)

        for conn in disconnected:
            self.disconnect(conn)


ws_manager = ConnectionManager()
