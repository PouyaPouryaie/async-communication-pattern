package ir.bigz.webhooks.paymentApi.repository;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
