package ir.bigz.webhooks.orderApi.payment;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the merchant id this store received when it registered itself with
 * the payment application. Populated once by {@link RegistrationRunner} on
 * startup and read by {@link PaymentClient} for every payment initiation call.
 */
@Component
public class MerchantRegistrationHolder {

    private final AtomicReference<String> merchantId = new AtomicReference<>();

    public void set(String id) {
        merchantId.set(id);
    }

    public String get() {
        String id = merchantId.get();
        if (id == null) {
            throw new IllegalStateException("This store has not registered with the payment application yet");
        }
        return id;
    }
}
