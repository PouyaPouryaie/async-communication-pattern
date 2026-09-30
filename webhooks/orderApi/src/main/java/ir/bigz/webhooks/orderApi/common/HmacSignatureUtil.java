package ir.bigz.webhooks.orderApi.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes the HMAC-SHA256 signature used to verify inbound payment webhooks.
 * This is intentionally duplicated from the payment application's identical
 * utility: the two services are independently deployed and only share the
 * wire contract (JSON shape + signing algorithm), not Java code.
 */
public final class HmacSignatureUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private HmacSignatureUtil() {
    }

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256 signature", e);
        }
    }
}
