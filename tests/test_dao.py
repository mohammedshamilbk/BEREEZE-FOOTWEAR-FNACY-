"""
Unit tests for DAO operations and SQLite persistence.
"""

import os
import unittest
from datetime import datetime

from pos_billing.database import dao, db_init
from pos_billing.database.models import Bill, BillItem, Customer, Expense, ItemMaster, User


class TestDAOOperations(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Initialize test DB tables
        db_init.initialize_database()

    def test_user_authentication_and_crud(self):
        uname = f"user_{int(datetime.now().timestamp())}"
        user = User(
            username=uname,
            password="testpassword",
            full_name="Test User",
            role="CASHIER",
            email="test@example.com",
            phone="9876543210",
        )
        saved = dao.save_user(user)
        self.assertTrue(saved)

        auth_user = dao.authenticate_user(uname, "testpassword")
        self.assertIsNotNone(auth_user)
        self.assertEqual(auth_user.full_name, "Test User")

    def test_customer_crud_and_loyalty(self):
        code = f"CUST-{int(datetime.now().timestamp())}"
        cust = Customer(
            customer_code=code,
            customer_name="Alice Green",
            phone="9988776655",
            email="alice@example.com",
            address="123 Main St",
            city="Kasaragod",
            credit_limit=5000.0,
        )
        saved = dao.save_customer(cust)
        self.assertTrue(saved)
        self.assertGreater(cust.customer_id, 0)

        fetched = dao.search_customer_by_code(code)
        self.assertIsNotNone(fetched)
        self.assertEqual(fetched.customer_name, "Alice Green")

    def test_item_master_crud_and_stock(self):
        item_code = f"ITM-{int(datetime.now().timestamp())}"
        item = ItemMaster(
            item_code=item_code,
            item_name="Leather Boots",
            category="Footwear",
            manufacturer="Bereeze",
            purchase_price=800.0,
            selling_price=1600.0,
            barcode=item_code,
            size="9",
            color="Brown",
            material="Leather",
            stock_quantity=50,
        )
        saved = dao.save_item(item)
        self.assertTrue(saved)
        self.assertGreater(item.item_id, 0)

        # Update stock
        updated = dao.update_stock(item.item_id, 45)
        self.assertTrue(updated)

        fetched = dao.search_item_by_code(item_code)
        self.assertEqual(fetched.stock_quantity, 45)

    def test_bill_save_and_retrieve(self):
        bill_no = f"INV-{int(datetime.now().timestamp())}"
        bill = Bill(
            bill_number=bill_no,
            bill_type="SALES",
            customer_id=0,
            customer_name="Walk-in Customer",
            user_id=1,
            subtotal=1000.0,
            total_amount=1000.0,
            paid_amount=1000.0,
            payment_mode="CASH",
            status="COMPLETED",
        )
        item = BillItem(
            item_id=1,
            item_code="TEST-001",
            item_name="Sample Product",
            quantity=1,
            unit_price=1000.0,
        )
        bill.add_bill_item(item)

        saved = dao.save_bill(bill)
        self.assertTrue(saved)

        retrieved = dao.get_bill_by_number(bill_no)
        self.assertIsNotNone(retrieved)
        self.assertEqual(retrieved.bill_number, bill_no)
        self.assertEqual(len(retrieved.bill_items), 1)

    def test_expense_crud(self):
        expense = Expense(
            category="Utilities",
            description="Electricity Bill",
            amount=2500.0,
            payment_mode="UPI",
        )
        dao.save_expense(expense)
        self.assertGreater(expense.expense_id, 0)

        expenses = dao.get_all_expenses()
        self.assertTrue(any(e.expense_id == expense.expense_id for e in expenses))


if __name__ == "__main__":
    unittest.main()
