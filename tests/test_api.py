# tests/test_api.py
"""
Automated Integration Tests for Cloud REST API & Auth.
"""

import unittest
from pos_billing.backend.auth import (
    create_access_token,
    hash_password,
    verify_password,
)
from pos_billing.backend.routers.auth_router import login, LoginRequest
from pos_billing.backend.routers.dashboard_router import get_dashboard_stats
from pos_billing.backend.routers.pos_router import get_upi_qr
from pos_billing.database.db_init import initialize_database
from pos_billing.database.models import User


class TestCloudAPI(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        initialize_database()
        cls.test_user = User(
            user_id=1,
            username="admin",
            password=hash_password("admin123"),
            full_name="Administrator",
            role="ADMIN",
            status="ACTIVE",
        )

    def test_password_hashing_and_verification(self):
        pwd = "admin123"
        hashed = hash_password(pwd)
        self.assertTrue(verify_password(pwd, hashed, "admin"))
        self.assertFalse(verify_password("wrong_password", hashed, "admin"))

    def test_auth_login_router(self):
        req = LoginRequest(username="admin", password="admin123")
        res = login(req)
        self.assertIsNotNone(res.access_token)
        self.assertEqual(res.token_type, "bearer")
        self.assertEqual(res.user["username"], "admin")

    def test_dashboard_stats_router(self):
        stats = get_dashboard_stats(current_user=self.test_user)
        self.assertIsNotNone(stats.today_revenue)
        self.assertIsNotNone(stats.occupied_stations)
        self.assertGreaterEqual(stats.available_stations, 0)

    def test_upi_qr_generation(self):
        res = get_upi_qr(amount=4999.0)
        self.assertIn("data:image/png;base64,", res["qr_code"])
        self.assertEqual(res["amount"], 4999.0)


if __name__ == "__main__":
    unittest.main()
