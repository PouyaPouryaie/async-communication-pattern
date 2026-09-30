package ir.bigz.webhooks.paymentApi.repository;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select payment from Payment payment where payment.id = :paymentId")
	Optional<Payment> findLockedById(@Param("paymentId") UUID paymentId);
}
