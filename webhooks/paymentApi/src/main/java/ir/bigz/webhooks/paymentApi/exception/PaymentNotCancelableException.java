package ir.bigz.webhooks.paymentApi.exception;

import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;

public class PaymentNotCancelableException extends RuntimeException {

    public PaymentNotCancelableException(PaymentStatus status) {
        super("Payment cannot be canceled from status " + status);
    }
}