package ir.bigz.webhooks.orderApi.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HmacSignatureUtilTest {

    @Test
    void signMatchesKnownHmacSha256TestVector() {
        // RFC 4231 test case 1: key = 20 bytes of 0x0b, data = "Hi There"
        String key = "\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b\u000b";
        String data = "Hi There";

        String signature = HmacSignatureUtil.sign(data, key);

        assertEquals(
                "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
                signature);
    }

    @Test
    void signIsDeterministicForSameInputAndSecret() {
        String first = HmacSignatureUtil.sign("payload", "secret");
        String second = HmacSignatureUtil.sign("payload", "secret");

        assertEquals(first, second);
    }

    @Test
    void signDiffersWhenSecretDiffers() {
        String signedWithSecretA = HmacSignatureUtil.sign("payload", "secret-a");
        String signedWithSecretB = HmacSignatureUtil.sign("payload", "secret-b");

        assertNotEquals(signedWithSecretA, signedWithSecretB);
    }
}
