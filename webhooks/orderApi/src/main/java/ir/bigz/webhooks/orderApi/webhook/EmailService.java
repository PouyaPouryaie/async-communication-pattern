package ir.bigz.webhooks.orderApi.webhook;

import ir.bigz.webhooks.orderApi.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stands in for a real email provider. Logs what would be sent instead of
 * making an outbound call, since this demo does not wire up an SMTP server.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendOrderConfirmation(Order order) {
        log.info("Sending order confirmation email to {} for order {} (total {} {})",
                order.getCustomerEmail(), order.getId(), order.getTotalAmount(), order.getCurrency());
    }
}
