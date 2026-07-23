# ============================================================
# pos_billing/payment/payment_method.py
# (PaymentMethod.java + CashPayment.java + CardPayment.java
#  + ChequePayment.java + DigitalPayment.java → Python)
# ============================================================
"""
Abstract PaymentMethod base class and concrete implementations.

Mirrors Java's abstract PaymentMethod hierarchy.
"""

from __future__ import annotations

import logging
import uuid
from abc import ABC, abstractmethod
from datetime import datetime

logger = logging.getLogger(__name__)


# ─────────────────────────────────────────────────────────────
# Abstract Base  (PaymentMethod.java)
# ─────────────────────────────────────────────────────────────
class PaymentMethod(ABC):
    def __init__(self) -> None:
        self.payment_date: datetime = datetime.now()
        self.status: str = "PENDING"
        self.transaction_reference: str = ""

    @abstractmethod
    def process_payment(self, amount: float) -> bool: ...

    @abstractmethod
    def validate_payment(self) -> bool: ...

    @abstractmethod
    def get_transaction_id(self) -> str: ...

    @abstractmethod
    def get_payment_method_name(self) -> str: ...

    def get_payment_status(self) -> str:
        return self.status

    def set_payment_status(self, status: str) -> None:
        self.status = status
        logger.info("Payment status updated to: %s", status)

    def __str__(self) -> str:
        return (
            f"PaymentMethod{{paymentDate={self.payment_date}, "
            f"status='{self.status}', "
            f"transactionReference='{self.transaction_reference}', "
            f"method='{self.get_payment_method_name()}'}}"
        )


# ─────────────────────────────────────────────────────────────
# Cash Payment  (CashPayment.java)
# ─────────────────────────────────────────────────────────────
class CashPayment(PaymentMethod):
    def __init__(self, amount_tendered: float, amount_due: float) -> None:
        super().__init__()
        self.amount_tendered = amount_tendered
        self.amount_due = amount_due
        self.change_amount = max(0.0, amount_tendered - amount_due)
        self._transaction_id = f"CASH-{uuid.uuid4().hex[:8].upper()}"

    def process_payment(self, amount: float) -> bool:
        if self.amount_tendered >= amount:
            self.change_amount = self.amount_tendered - amount
            self.status = "SUCCESS"
            logger.info("Cash payment processed. Change: ₹%.2f", self.change_amount)
            return True
        self.status = "FAILED"
        logger.warning("Insufficient cash tendered.")
        return False

    def validate_payment(self) -> bool:
        return self.amount_tendered >= self.amount_due

    def get_transaction_id(self) -> str:
        return self._transaction_id

    def get_payment_method_name(self) -> str:
        return "CASH"


# ─────────────────────────────────────────────────────────────
# Card Payment  (CardPayment.java)
# ─────────────────────────────────────────────────────────────
class CardPayment(PaymentMethod):
    def __init__(self, card_number: str = "", card_type: str = "DEBIT") -> None:
        super().__init__()
        self.card_number = card_number  # last 4 digits only
        self.card_type = card_type      # CREDIT | DEBIT
        self._transaction_id = f"CARD-{uuid.uuid4().hex[:10].upper()}"

    def process_payment(self, amount: float) -> bool:
        # Simulate card processing
        self.status = "SUCCESS"
        self.transaction_reference = self._transaction_id
        logger.info("Card payment processed: ₹%.2f via %s", amount, self.card_type)
        return True

    def validate_payment(self) -> bool:
        return len(self.card_number) >= 4

    def get_transaction_id(self) -> str:
        return self._transaction_id

    def get_payment_method_name(self) -> str:
        return f"CARD ({self.card_type})"


# ─────────────────────────────────────────────────────────────
# Cheque Payment  (ChequePayment.java)
# ─────────────────────────────────────────────────────────────
class ChequePayment(PaymentMethod):
    def __init__(self, cheque_number: str = "", bank_name: str = "") -> None:
        super().__init__()
        self.cheque_number = cheque_number
        self.bank_name = bank_name
        self._transaction_id = f"CHQ-{cheque_number or uuid.uuid4().hex[:8].upper()}"

    def process_payment(self, amount: float) -> bool:
        if self.cheque_number:
            self.status = "PENDING"   # cheques are pending until cleared
            self.transaction_reference = self._transaction_id
            logger.info("Cheque payment recorded: %s / %s", self.cheque_number, self.bank_name)
            return True
        self.status = "FAILED"
        return False

    def validate_payment(self) -> bool:
        return bool(self.cheque_number and self.bank_name)

    def get_transaction_id(self) -> str:
        return self._transaction_id

    def get_payment_method_name(self) -> str:
        return "CHEQUE"


# ─────────────────────────────────────────────────────────────
# Digital / UPI Payment  (DigitalPayment.java)
# ─────────────────────────────────────────────────────────────
class DigitalPayment(PaymentMethod):
    def __init__(self, upi_id: str = "", gateway: str = "UPI") -> None:
        super().__init__()
        self.upi_id = upi_id
        self.gateway = gateway  # UPI | NETBANKING | WALLET
        self._transaction_id = f"UPI-{uuid.uuid4().hex[:12].upper()}"

    def process_payment(self, amount: float) -> bool:
        self.status = "SUCCESS"
        self.transaction_reference = self._transaction_id
        logger.info("Digital payment processed: ₹%.2f via %s", amount, self.gateway)
        return True

    def validate_payment(self) -> bool:
        return True  # Gateway handles validation

    def get_transaction_id(self) -> str:
        return self._transaction_id

    def get_payment_method_name(self) -> str:
        return f"DIGITAL ({self.gateway})"


# ─────────────────────────────────────────────────────────────
# Payment Processor  (PaymentProcessor.java → Python)
# ─────────────────────────────────────────────────────────────
class PaymentProcessor:
    """Factory + orchestrator mirroring Java's PaymentProcessor."""

    @staticmethod
    def create_payment(method_name: str, *args, **kwargs) -> PaymentMethod:
        """
        Factory method to build the right PaymentMethod.

        Args:
            method_name: 'CASH' | 'CARD' | 'CHEQUE' | 'DIGITAL' | 'UPI' | 'ONLINE'
            *args / **kwargs: Method-specific parameters forwarded to the constructor.
        """
        name = method_name.upper()
        amt_tendered = args[0] if len(args) > 0 else kwargs.get("amount_tendered", 0.0)
        amt_due = args[1] if len(args) > 1 else kwargs.get("amount_due", 0.0)

        if name == "CASH":
            return CashPayment(
                amount_tendered=amt_tendered,
                amount_due=amt_due,
            )
        if name in ("CARD", "DEBIT", "CREDIT"):
            return CardPayment(
                card_number=kwargs.get("card_number", ""),
                card_type=kwargs.get("card_type", "DEBIT"),
            )
        if name == "CHEQUE":
            return ChequePayment(
                cheque_number=kwargs.get("cheque_number", ""),
                bank_name=kwargs.get("bank_name", ""),
            )
        # Default → digital/UPI
        return DigitalPayment(
            upi_id=kwargs.get("upi_id", ""),
            gateway=kwargs.get("gateway", name),
        )

    @staticmethod
    def process(payment: PaymentMethod, amount: float) -> bool:
        """Validate and process a payment."""
        if not payment.validate_payment():
            logger.warning("Payment validation failed for %s", payment.get_payment_method_name())
            return False
        return payment.process_payment(amount)
