package ir.bigz.webhooks.paymentApi.payment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulates the entry point of a real payment provider (e.g. Stripe/PayPal):
 * a merchant calls this to start a payment and gets an immediate "processing"
 * response, with the final outcome delivered later via webhook.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiatedResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        PaymentInitiatedResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/cancel")
    public CancelPaymentResponse cancel(@Valid @RequestBody CancelPaymentRequest request) {
        return paymentService.cancelPayment(request);
    }
}
