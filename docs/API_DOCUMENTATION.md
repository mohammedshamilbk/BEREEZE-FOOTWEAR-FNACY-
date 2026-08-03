# API Documentation — Bereeze Footwear Fancy Cloud REST & WebSocket API

The Bereeze Footwear Fancy Cloud Backend provides a RESTful API and real-time WebSockets for multi-device synchronization.

## Base URL
- **HTTP**: `http://localhost:8000/api/v1`
- **WebSocket**: `ws://localhost:8000/ws`
- **Interactive Swagger Docs**: `http://localhost:8000/docs`

---

## 🔑 Authentication Endpoints

### `POST /auth/login`
Authenticates user and returns JWT bearer tokens.
- **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1Ni...",
    "refresh_token": "eyJhbGciOiJIUzI1Ni...",
    "token_type": "bearer",
    "expires_in": 86400,
    "user": {
      "user_id": 1,
      "username": "admin",
      "role": "ADMIN"
    }
  }
  ```

---

## 🎮 Game Station & Session Endpoints

### `GET /sessions/stations`
Lists all game stations and current availability.

### `POST /sessions/checkin`
Checks in a customer to a station and starts real-time clock.
- **Request Body**:
  ```json
  {
    "station_id": 1,
    "customer_id": 1
  }
  ```

### `POST /sessions/checkout`
Ends a session, computes exact elapsed duration and bill amount.
- **Request Body**:
  ```json
  {
    "session_id": 1,
    "payment_mode": "CASH"
  }
  ```

---

## 🛒 POS Billing Endpoints

### `POST /pos/checkout`
Submits cart items, decrements stock levels, generates bill receipt.

### `GET /pos/upi-qr?amount=4999.0`
Generates a dynamic base64 PNG data URL of a UPI QR Code pre-filled with the exact bill total.

---

## ⚡ WebSockets Protocol (`/ws`)

Connect to `/ws?user_id={id}` to receive instant real-time events.

### Broadcast Event Types
- `CUSTOMER_CHECKIN`: Triggered when customer checks in to a station.
- `CUSTOMER_CHECKOUT`: Triggered when session ends.
- `BILL_CREATED`: Triggered on completed POS sale.
- `EXPENSE_ADDED`: Triggered when store expense is recorded.
