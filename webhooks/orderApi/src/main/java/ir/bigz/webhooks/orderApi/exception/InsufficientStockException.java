package ir.bigz.webhooks.orderApi.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Product " + productId + " has only " + available + " in stock, requested " + requested);
    }
}
