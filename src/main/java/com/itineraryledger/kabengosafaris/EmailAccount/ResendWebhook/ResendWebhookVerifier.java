package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifies Resend webhook signatures using the svix headers.
 *
 * Resend uses Svix for webhook delivery. The signature is computed as:
 * HMAC-SHA256(webhook_secret, "{svix-id}.{svix-timestamp}.{body}")
 *
 * The svix-signature header contains one or more signatures prefixed with "v1,".
 */
@Component
@Slf4j
public class ResendWebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Verify the webhook signature.
     *
     * @param payload     The raw request body
     * @param svixId      The svix-id header value
     * @param svixTimestamp The svix-timestamp header value
     * @param svixSignature The svix-signature header value (may contain multiple comma-separated signatures)
     * @param webhookSecret The webhook signing secret (from Resend dashboard, stored per account)
     * @return true if signature is valid
     */
    public boolean verify(String payload, String svixId, String svixTimestamp, String svixSignature, String webhookSecret) {
        try {
            if (payload == null || svixId == null || svixTimestamp == null || svixSignature == null || webhookSecret == null) {
                log.warn("Missing required parameters for webhook verification");
                return false;
            }

            // The secret from Resend may be prefixed with "whsec_"
            String secret = webhookSecret;
            if (secret.startsWith("whsec_")) {
                secret = secret.substring(6);
            }

            // Decode the base64 secret
            byte[] secretBytes = Base64.getDecoder().decode(secret);

            // Construct the signed content: "{svix-id}.{svix-timestamp}.{body}"
            String signedContent = svixId + "." + svixTimestamp + "." + payload;

            // Compute HMAC-SHA256
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            byte[] hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "v1," + Base64.getEncoder().encodeToString(hash);

            // The svix-signature header may contain multiple signatures separated by spaces
            String[] signatures = svixSignature.split(" ");
            for (String sig : signatures) {
                if (sig.trim().equals(expectedSignature)) {
                    return true;
                }
            }

            log.warn("Webhook signature verification failed");
            return false;

        } catch (Exception e) {
            log.error("Error verifying webhook signature: {}", e.getMessage());
            return false;
        }
    }
}
