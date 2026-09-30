package ir.bigz.webhooks.paymentApi.exception;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(String merchantId) {
        super("No merchant registered with id " + merchantId);
    }
}
