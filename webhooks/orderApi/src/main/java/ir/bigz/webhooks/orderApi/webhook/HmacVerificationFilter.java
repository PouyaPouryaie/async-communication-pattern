package ir.bigz.webhooks.orderApi.webhook;

import ir.bigz.webhooks.orderApi.common.HmacSignatureUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifies the {@code X-Webhook-Signature} header on inbound payment webhooks
 * before the request reaches {@link PaymentWebhookController}. Registered
 * only for the payment webhook URL by {@link WebhookSecurityConfig} — it must
 * not run for the public order API.
 */
public class HmacVerificationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HmacVerificationFilter.class);
    static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private final String webhookSecret;

    public HmacVerificationFilter(@Value("${app.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String receivedSignature = request.getHeader(SIGNATURE_HEADER);
        String body = new String(cachedRequest.getCachedBody(), StandardCharsets.UTF_8);
        String expectedSignature = HmacSignatureUtil.sign(body, webhookSecret);

        if (receivedSignature == null || !constantTimeEquals(receivedSignature, expectedSignature)) {
            log.warn("Rejected webhook request with invalid or missing {} header", SIGNATURE_HEADER);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"invalid webhook signature\"}");
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
