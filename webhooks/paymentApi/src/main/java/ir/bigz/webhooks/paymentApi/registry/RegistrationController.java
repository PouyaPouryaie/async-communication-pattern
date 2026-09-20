package ir.bigz.webhooks.paymentApi.registry;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a merchant (an online store) register the webhook URL and secret this
 * application will use to deliver signed payment events back to it.
 */
@RestController
@RequestMapping("/api/registry")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterMerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request) {
        return ResponseEntity.ok(registrationService.register(request));
    }
}
