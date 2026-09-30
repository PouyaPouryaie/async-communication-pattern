package ir.bigz.webhooks.orderApi.webhook;

import ir.bigz.webhooks.orderApi.common.HmacSignatureUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HmacVerificationFilterTest {

    private static final String SECRET = "shared-secret";

    private final HmacVerificationFilter filter = new HmacVerificationFilter(SECRET);

    @Test
    void allowsRequestWithValidSignature() throws Exception {
        String body = "{\"paymentId\":\"p-1\"}";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/webhooks/payments");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader(HmacVerificationFilter.SIGNATURE_HEADER, HmacSignatureUtil.sign(body, SECRET));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsRequestWithMissingSignature() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/webhooks/payments");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsRequestWithWrongSignature() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/webhooks/payments");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        request.addHeader(HmacVerificationFilter.SIGNATURE_HEADER, "not-the-right-signature");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }
}
