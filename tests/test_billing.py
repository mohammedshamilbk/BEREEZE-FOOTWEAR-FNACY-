"""
Unit tests for Billing calculation and Payment processing business logic.
"""

import unittest
from datetime import datetime

from pos_billing.database.models import Bill, BillItem, Customer, Expense, ItemMaster, User
from pos_billing.payment.payment_method import (
    CardPayment,
    CashPayment,
    ChequePayment,
    DigitalPayment,
    PaymentProcessor,
)


class TestBillingLogic(unittest.TestCase):

    def test_bill_item_totals(self):
        item = BillItem(
            item_id=1,
            item_code="SHO-001",
            item_name="Leather Shoes",
            quantity=2,
            unit_price=1500.0,
            discount=100.0,
        )
        # Taxable amount = (1500 - 100) * 2 = 2800.0
        self.assertEqual(item.taxable_amount, 2800.0)
        self.assertEqual(item.total_amount, 2800.0)

    def test_bill_totals_calculation(self):
        bill = Bill(
            bill_number="BILL-1001",
            bill_type="SALES",
            customer_id=1,
            customer_name="John Doe",
        )
        item1 = BillItem(
            item_id=1,
            item_code="SHO-001",
            item_name="Leather Shoes",
            quantity=1,
            unit_price=2000.0,
            discount=200.0,
        )
        item2 = BillItem(
            item_id=2,
            item_code="SAN-002",
            item_name="Fancy Sandals",
            quantity=2,
            unit_price=500.0,
            discount=50.0,
        )
        bill.add_bill_item(item1)
        bill.add_bill_item(item2)
        bill.calculate_totals()

        # Item 1: (2000 - 200) * 1 = 1800
        # Item 2: (500 - 50) * 2 = 900
        # Subtotal = 1800 + 900 = 2700
        self.assertEqual(bill.subtotal, 2700.0)
        self.assertEqual(bill.total_amount, 2700.0)
        self.assertEqual(bill.balance_amount, 2700.0)

    def test_full_payment(self):
        bill = Bill(
            bill_number="BILL-1002",
            bill_type="SALES",
            customer_id=1,
            customer_name="Jane Smith",
        )
        item = BillItem(
            item_id=1,
            item_code="SHO-001",
            item_name="Formal Shoes",
            quantity=1,
            unit_price=1000.0,
        )
        bill.add_bill_item(item)
        bill.calculate_totals()

        success = bill.complete_bill(1000.0, "CASH")
        self.assertTrue(success)
        self.assertEqual(bill.paid_amount, 1000.0)
        self.assertEqual(bill.balance_amount, 0.0)
        self.assertEqual(bill.status, "COMPLETED")

    def test_cash_payment_processor(self):
        payment = PaymentProcessor.create_payment("CASH", amount_tendered=1500.0, amount_due=1200.0)
        self.assertIsInstance(payment, CashPayment)
        processed = PaymentProcessor.process(payment, 1200.0)
        self.assertTrue(processed)
        self.assertEqual(payment.change_amount, 300.0)
        self.assertEqual(payment.get_payment_status(), "SUCCESS")

    def test_card_payment_processor(self):
        payment = PaymentProcessor.create_payment("CARD", card_number="1234", card_type="DEBIT")
        self.assertIsInstance(payment, CardPayment)
        processed = PaymentProcessor.process(payment, 500.0)
        self.assertTrue(processed)
        self.assertEqual(payment.get_payment_status(), "SUCCESS")

    def test_expense_creation(self):
        expense = Expense(
            category="Rent",
            description="Monthly Shop Rent",
            amount=15000.0,
            payment_mode="ONLINE",
        )
        self.assertEqual(expense.category, "Rent")
        self.assertEqual(expense.amount, 15000.0)
        self.assertEqual(expense.payment_mode, "ONLINE")


if __name__ == "__main__":
    unittest.main()
