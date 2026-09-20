package ir.bigz.webhooks.orderApi.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("No product found with id " + productId);
    }
}
