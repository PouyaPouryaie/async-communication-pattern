package ir.bigz.webhooks.paymentApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
public class PaymentApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApiApplication.class, args);
	}

}
