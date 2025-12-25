package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for testing email templates by sending test emails to authenticated users
 *
 * This service handles:
 * - Validating the email event and template exist and are enabled
 * - Getting the authenticated user from the security context
 * - Generating appropriate test data based on the event type (each event has its own test implementation)
 * - Rendering the specified template with test variables
 * - Sending the test email to the user's email address
 *
 * IMPORTANT: Each email event type MUST have its own test implementation in the switch statement.
 * If a test is not implemented for an event, an error will be returned to the user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateTestService {

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final UserRepository userRepository;
    private final EmailTemplateService emailTemplateService;
    private final EmailSendingService emailSendingService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecuritySettingsGetterServices securitySettingsGetterServices;
    private final IdObfuscator idObfuscator;

    @Value("${app.base.url}")
    private String appBaseUrl;

    /**
     * Send a test email using a specific template to the authenticated user
     *
     * @param eventId The obfuscated email event ID
     * @param templateId The obfuscated template ID
     * @param authentication The authenticated user
     * @return ResponseEntity with test result
     */
    public ResponseEntity<ApiResponse<?>> sendTestEmail(String eventId, String templateId, Authentication authentication) {
        try {
            log.info("Sending test email for template ID: {} in event ID: {}", templateId, eventId);

            // 1. Decode IDs
            Long decodedEventId = idObfuscator.decodeId(eventId);
            Long decodedTemplateId = idObfuscator.decodeId(templateId);

            // 2. Get email event and validate
            EmailEvent event = emailEventRepository.findById(decodedEventId)
                .orElseThrow(() -> new IllegalArgumentException("Email event not found with ID: " + eventId));

            if (!event.getEnabled()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send test email - event is disabled: " + event.getName(), "EVENT_DISABLED")
                );
            }

            // 3. Get email template and validate
            EmailTemplate template = emailTemplateRepository.findById(decodedTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("Email template not found with ID: " + templateId));

            // Validate template belongs to this event
            if (!template.getEmailEvent().getId().equals(decodedEventId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Template does not belong to the specified event", "TEMPLATE_EVENT_MISMATCH")
                );
            }

            if (!template.getEnabled()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot send test email - template is disabled: " + template.getName(), "TEMPLATE_DISABLED")
                );
            }

            // 4. Get authenticated user
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));

            log.info("Sending test email for event '{}' using template '{}' to user: {} ({})",
                event.getName(), template.getName(), user.getUsername(), user.getEmail());

            // 5. Generate test variables and subject based on event type
            TestEmailData testData = generateTestEmailData(event.getName(), user);

            // 6. Render specific template with test variables
            String htmlContent = emailTemplateService.readTemplateFile(template.getFileName());
            htmlContent = replacePlaceholders(htmlContent, testData.variables);

            // 7. Send email
            emailSendingService.sendHtmlEmail(user.getEmail(), testData.subject, htmlContent);

            // 8. Build response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("eventName", event.getName());
            responseData.put("templateName", template.getName());
            responseData.put("recipientEmail", user.getEmail());
            responseData.put("subject", testData.subject);
            responseData.put("sentAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            log.info("Test email sent successfully for event '{}' using template '{}' to {}",
                event.getName(), template.getName(), user.getEmail());

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Test email sent successfully to " + user.getEmail(),
                    responseData
                )
            );

        } catch (UnsupportedOperationException e) {
            log.warn("Test not implemented for email event: {}", e.getMessage());
            return ResponseEntity.status(501).body(
                ApiResponse.error(501, e.getMessage(), "TEST_NOT_IMPLEMENTED")
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for test email: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, e.getMessage(), "INVALID_REQUEST")
            );
        } catch (IllegalStateException e) {
            log.error("Configuration error for test email: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Email configuration error: " + e.getMessage(), "CONFIGURATION_ERROR")
            );
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to send test email: " + e.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Generate test email data (variables and subject) for a given email event type
     *
     * IMPORTANT: Each email event type MUST have its own implementation here.
     * If a test is not implemented, an UnsupportedOperationException will be thrown.
     *
     * @param eventName The email event name
     * @param user The user to send the test email to
     * @return TestEmailData containing variables and subject
     * @throws UnsupportedOperationException if test is not implemented for this event type
     */
    private TestEmailData generateTestEmailData(String eventName, User user) {
        switch (eventName) {
            case "USER_REGISTRATION":
                return generateUserRegistrationTestData(user);

            // Add more cases as email events are implemented
            // case "PASSWORD_RESET":
            //     return generatePasswordResetTestData(user);
            //
            // case "EMAIL_VERIFICATION":
            //     return generateEmailVerificationTestData(user);

            default:
                throw new UnsupportedOperationException(
                    "Test email not implemented for event: " + eventName + ". " +
                    "Please implement a test case in EmailTemplateTestService.generateTestEmailData()"
                );
        }
    }

    /**
     * Generate test data for USER_REGISTRATION email event
     *
     * @param user The user to send the test email to
     * @return TestEmailData with registration-specific variables and subject
     */
    private TestEmailData generateUserRegistrationTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        // Generate test activation token
        String activationToken = jwtTokenProvider.generateRegistrationTokenFromUsername(user.getUsername());
        String activationLink = appBaseUrl + "/api/auth/activate?token=" + activationToken;

        // Calculate expiration time
        Long expirationMinutes = securitySettingsGetterServices.getRegistrationJwtExpirationMinutes();
        LocalDateTime expirationDateTime = LocalDateTime.now().plusMinutes(expirationMinutes);
        Long expirationHours = expirationMinutes / 60;

        // Populate variables
        variables.put("username", user.getUsername());
        variables.put("email", user.getEmail());
        variables.put("firstName", user.getFirstName());
        variables.put("lastName", user.getLastName());
        variables.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        variables.put("enabled", String.valueOf(user.getEnabled()));
        variables.put("accountLocked", String.valueOf(user.getAccountLocked()));
        variables.put("createdAt", user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("activationToken", activationToken);
        variables.put("activationLink", activationLink);
        variables.put("expirationHours", String.valueOf(expirationHours));
        variables.put("expirationDateTime", expirationDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String subject = "[TEST] Welcome to Kabengosafaris - Activate Your Account";

        return new TestEmailData(variables, subject);
    }

    // Add more test data generators for other email event types as needed
    // private TestEmailData generatePasswordResetTestData(User user) {
    //     Map<String, String> variables = new HashMap<>();
    //     variables.put("username", user.getUsername());
    //     variables.put("resetLink", appBaseUrl + "/reset-password?token=test_token");
    //     String subject = "[TEST] Password Reset Request";
    //     return new TestEmailData(variables, subject);
    // }

    /**
     * Replace {{variableName}} placeholders with actual values
     *
     * @param html The HTML template content
     * @param variables Map of variable names to values
     * @return HTML with placeholders replaced
     */
    private String replacePlaceholders(String html, Map<String, String> variables) {
        String result = html;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Inner class to hold test email data
     */
    private static class TestEmailData {
        final Map<String, String> variables;
        final String subject;

        TestEmailData(Map<String, String> variables, String subject) {
            this.variables = variables;
            this.subject = subject;
        }
    }
}
