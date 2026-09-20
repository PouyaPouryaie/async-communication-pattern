package ir.bigz.webhooks.paymentApi.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes the HMAC-SHA256 signature used to secure webhook payloads.
 * The same algorithm is duplicated on the online store side so it can
 * independently recompute the signature and compare it to the one
 * this application sends.
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
