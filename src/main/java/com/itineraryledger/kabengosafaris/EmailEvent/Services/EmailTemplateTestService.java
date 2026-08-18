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

import java.time.LocalDate;
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

    /** The same property the live availability letter greets with, so a test reads identically. */
    @Value("${app.company.name:Kabengo Safaris}")
    private String companyName;

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final UserRepository userRepository;
    private final EmailTemplateService emailTemplateService;
    private final EmailSendingService emailSendingService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecuritySettingsGetterServices securitySettingsGetterServices;
    private final IdObfuscator idObfuscator;

    @Value("${app.management.base.url}")
    private String appManagementBaseUrl;

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

            case "PASSWORD_RESET":
                return generatePasswordResetTestData(user);

            case "BACKUP_SUCCESS":
                return generateBackupSuccessTestData(user);

            case "BACKUP_FAILURE":
                return generateBackupFailureTestData(user);

            case "NEWSLETTER_SUBSCRIPTION":
                return generateNewsletterSubscriptionTestData(user);

            case "BOOKING_INQUIRY":
                return generateBookingInquiryTestData(user);

            case "CONTACT_US":
                return generateContactUsTestData(user);

            case "SEND_QUOTE":
                return generateQuoteSentTestData(user);

            case "SAFARI_PAYMENT_GAP":
                return generateSafariPaymentGapTestData(user);

            case "SAFARI_READINESS_ALERT":
                return generateSafariReadinessAlertTestData(user);

            case "SAFARI_STARTED":
                return generateSafariStartedTestData(user);

            case "SAFARI_COMPLETED":
                return generateSafariCompletedTestData(user);

            case "SAFARI_POST_TRIP_REMINDER":
                return generateSafariPostTripReminderTestData(user);

            case "AVAILABILITY_REQUEST":
                return generateAvailabilityRequestTestData(user);

            case "SEND_SAFARI_DETAILS":
                return generateSafariDetailsTestData(user);

            case "SEND_SAFARI_MESSAGE":
                return generateSafariCustomerMessageTestData(user);

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
        String activationLink = appManagementBaseUrl + "/account-activation?token=" + activationToken;

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

    /**
     * Generate test data for PASSWORD_RESET email event
     *
     * @param user The user to send the test email to
     * @return TestEmailData with password reset-specific variables and subject
     */
    private TestEmailData generatePasswordResetTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        // Generate test password reset token
        String resetToken = jwtTokenProvider.generatePasswordResetTokenFromUsername(user.getUsername());
        String resetLink = appManagementBaseUrl + "/reset-password?token=" + resetToken;

        // Calculate expiration time
        Long expirationMinutes = securitySettingsGetterServices.getPasswordResetJwtExpirationMinutes();
        LocalDateTime expirationDateTime = LocalDateTime.now().plusMinutes(expirationMinutes);
        LocalDateTime requestedAt = LocalDateTime.now();

        // Populate variables
        variables.put("username", user.getUsername());
        variables.put("email", user.getEmail());
        variables.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        variables.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        variables.put("resetToken", resetToken);
        variables.put("resetLink", resetLink);
        variables.put("expirationMinutes", String.valueOf(expirationMinutes));
        variables.put("expirationDateTime", expirationDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("requestedAt", requestedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String subject = "[TEST] Password Reset Request - Kabengosafaris";

        return new TestEmailData(variables, subject);
    }

    /**
     * Generate test data for BACKUP_SUCCESS email event
     *
     * @param user The user to send the test email to
     * @return TestEmailData with backup success-specific variables and subject
     */
    private TestEmailData generateBackupSuccessTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        // Generate realistic test backup data
        LocalDateTime backupTime = LocalDateTime.now();
        LocalDateTime nextBackupTime = LocalDateTime.now().plusDays(1);
        String testBackupFilename = "kabengosafaris_backup_" +
            backupTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip";

        // Populate variables
        variables.put("backupTime", backupTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("backupType", "FULL");
        variables.put("backupSize", "2.8 MB");
        variables.put("databaseIncluded", "Yes");
        variables.put("filesIncluded", "Yes");
        variables.put("compressionFormat", "zip");
        variables.put("compressionLevel", "6");
        variables.put("backupPath", "/opt/lampp/htdocs/kabengosafaris/backups/" + testBackupFilename);
        variables.put("backupDownloadLink", appManagementBaseUrl + "/api/backups/download/" + testBackupFilename);
        variables.put("retentionDays", "30");
        variables.put("nextBackupTime", nextBackupTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String subject = "[TEST] Backup Completed Successfully - Kabengosafaris";

        return new TestEmailData(variables, subject);
    }

    /**
     * Generate test data for BACKUP_FAILURE email event
     *
     * @param user The user to send the test email to
     * @return TestEmailData with backup failure-specific variables and subject
     */
    private TestEmailData generateBackupFailureTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        // Generate realistic test failure data
        LocalDateTime failureTime = LocalDateTime.now();
        LocalDateTime lastSuccessfulBackup = LocalDateTime.now().minusDays(1);
        LocalDateTime nextBackupTime = LocalDateTime.now().plusHours(6);

        // Populate variables
        variables.put("failureTime", failureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("backupType", "FULL");
        variables.put("lastSuccessfulBackup", lastSuccessfulBackup.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("attemptNumber", "1");
        variables.put("errorMessage", "Database backup failed: Connection timeout after 30 seconds. Please check database server connectivity.");
        variables.put("nextBackupTime", nextBackupTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String subject = "[TEST] ⚠️ Backup Failed - Kabengosafaris";

        return new TestEmailData(variables, subject);
    }

    private TestEmailData generateNewsletterSubscriptionTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        variables.put("subscriberEmail", user.getEmail());
        variables.put("subscriberName", user.getFirstName() + " " + user.getLastName());
        variables.put("subscriptionDate", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));
        variables.put("preferredLocale", "en");
        variables.put("source", "WEBSITE");
        variables.put("isResubscription", "false");
        variables.put("linkedCustomerName", user.getFirstName() + " " + user.getLastName());
        variables.put("totalActiveSubscribers", "142");

        String subject = "[TEST] New Newsletter Subscription: " + user.getEmail();

        return new TestEmailData(variables, subject);
    }

    private TestEmailData generateBookingInquiryTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("inquiryCode", "INQ-0001-" + String.format("%02d-%02d", now.getMonthValue(), now.getYear() % 100));
        variables.put("firstName", user.getFirstName() != null ? user.getFirstName() : "John");
        variables.put("lastName", user.getLastName() != null ? user.getLastName() : "Doe");
        variables.put("email", user.getEmail());
        variables.put("phone", "+255 700 123 456");
        variables.put("country", "United States");
        variables.put("adults", "2");
        variables.put("children", "1");
        variables.put("totalTravelers", "3");
        variables.put("preferredStartDate", now.plusMonths(2).format(dateFormatter));
        variables.put("preferredEndDate", now.plusMonths(2).plusDays(7).format(dateFormatter));
        variables.put("budgetCategory", "MID_RANGE");
        variables.put("tripType", "WILDLIFE_SAFARI");
        variables.put("specialRequests", "We would love to see the Great Migration if possible. Vegetarian meals preferred.");
        variables.put("message", "Hi, we're planning a family safari trip to Tanzania and would love to get more details about your packages.");
        variables.put("source", "WEBSITE");
        variables.put("preferredLocale", "en");
        variables.put("inquiryDate", now.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));
        variables.put("itineraryName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("itineraryCode", "SAF-001");
        variables.put("itineraryTotalDays", "7");
        variables.put("itineraryTotalNights", "6");
        variables.put("itineraryStartLocation", "Arusha");
        variables.put("itineraryEndLocation", "Arusha");
        variables.put("itineraryDescription", "Experience the best of Northern Tanzania with visits to Serengeti, Ngorongoro Crater, and Lake Manyara.");

        String subject = "[TEST] New Booking Inquiry: " + variables.get("inquiryCode") + " - " + variables.get("firstName") + " " + variables.get("lastName");

        return new TestEmailData(variables, subject);
    }

    private TestEmailData generateContactUsTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();

        variables.put("contactCode", "MSG-0001-" + String.format("%02d-%02d", now.getMonthValue(), now.getYear() % 100));
        variables.put("name", user.getFirstName() + " " + user.getLastName());
        variables.put("email", user.getEmail());
        variables.put("phone", "+255 700 987 654");
        variables.put("subject", "Safari Package Inquiry");
        variables.put("message", "Hello, I would like to know more about your 5-day Serengeti safari package. Could you please send me the detailed itinerary and pricing? We are a group of 4 adults planning to visit in August.");
        variables.put("source", "WEBSITE");
        variables.put("preferredLocale", "en");
        variables.put("contactDate", now.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

        String subject = "[TEST] New Contact Message: " + variables.get("contactCode") + " - " + variables.get("subject");

        return new TestEmailData(variables, subject);
    }

    private TestEmailData generateQuoteSentTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("quoteCode", "QT-1042-" + String.format("%02d%02d", now.getMonthValue(), now.getYear() % 100) + "-1");
        variables.put("quoteTitle", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("sentDate", now.format(dateFormatter));
        variables.put("safariStartDate", now.plusMonths(2).format(dateFormatter));
        variables.put("itineraryName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("itineraryCode", "ITI-7D6N-1007");
        variables.put("tripType", "Private Safari");
        variables.put("totalDays", "7");
        variables.put("totalNights", "6");
        variables.put("startLocation", "Arusha");
        variables.put("endLocation", "Arusha");
        variables.put("grandTotal", "USD 3,500.00");
        variables.put("itemsSummary",
            "<div class=\"details-box\">" +
            "<h3>Cost Breakdown</h3>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Accommodation (6 nights)</span><span class=\"detail-value\">USD 1,800.00</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Park Entry Fees</span><span class=\"detail-value\">USD 1,200.00</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Activities (Game Drives)</span><span class=\"detail-value\">USD 500.00</span></div>" +
            "</div>");
        variables.put("validFrom", now.format(dateFormatter));
        variables.put("validTo", now.plusDays(30).format(dateFormatter));
        variables.put("depositPercentage", "30%");
        variables.put("depositDueDate", now.plusDays(7).format(dateFormatter));
        variables.put("fullPaymentDueDate", now.plusDays(45).format(dateFormatter));
        variables.put("customerNotes", "Package includes all park entry fees, accommodation, meals, and ground transport. International flights not included.");
        variables.put("companyEmail", "info@kabengosafaris.com");

        String subject = "[TEST] Your Safari Quote: " + variables.get("quoteCode") + " - " + variables.get("quoteTitle");

        return new TestEmailData(variables, subject);
    }

    /**
     * Replace {{variableName}} placeholders with actual values
     */
    private TestEmailData generateSafariPaymentGapTestData(User user) {
        Map<String, String> variables = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("safariCode", "SAF-7D6N-1007");
        variables.put("safariName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("startDate", now.minusDays(2).format(dateFormatter));
        variables.put("endDate", now.plusDays(5).format(dateFormatter));
        variables.put("currentState", "Pending Payment");
        variables.put("daysOverdue", "2");
        variables.put("totalDays", "7");
        variables.put("alertDate", now.format(dateFormatter));

        return new TestEmailData(variables, "[TEST] CRITICAL: Payment Gap — SAF-7D6N-1007 — 7-Day Serengeti & Ngorongoro Safari");
    }

    private TestEmailData generateSafariReadinessAlertTestData(User user) {
        Map<String, String> variables = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("safariCode", "SAF-7D6N-1007");
        variables.put("safariName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("startDate", now.plusDays(5).format(dateFormatter));
        variables.put("daysUntilStart", "5");
        variables.put("phase", "Starting Soon");
        variables.put("issueCount", "3");
        variables.put("issuesList",
            "<li style=\"margin: 4px 0;\">No vehicles assigned</li>" +
            "<li style=\"margin: 4px 0;\">2 overnight day(s) missing accommodation</li>" +
            "<li style=\"margin: 4px 0;\">No passenger categories defined</li>");
        variables.put("alertDate", now.format(dateFormatter));

        return new TestEmailData(variables, "[TEST] Safari Readiness Issues — SAF-7D6N-1007 starts in 5 day(s)");
    }

    private TestEmailData generateSafariStartedTestData(User user) {
        Map<String, String> variables = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("safariCode", "SAF-7D6N-1007");
        variables.put("safariName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("startDate", now.format(dateFormatter));
        variables.put("endDate", now.plusDays(6).format(dateFormatter));
        variables.put("totalDays", "7");
        variables.put("totalNights", "6");
        variables.put("startLocation", "Arusha");
        variables.put("alertDate", now.format(dateFormatter));

        return new TestEmailData(variables, "[TEST] Safari Started: SAF-7D6N-1007 — 7-Day Serengeti & Ngorongoro Safari");
    }

    private TestEmailData generateSafariCompletedTestData(User user) {
        Map<String, String> variables = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("safariCode", "SAF-7D6N-1007");
        variables.put("safariName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("startDate", now.minusDays(7).format(dateFormatter));
        variables.put("endDate", now.minusDays(1).format(dateFormatter));
        variables.put("totalDays", "7");
        variables.put("alertDate", now.format(dateFormatter));

        return new TestEmailData(variables, "[TEST] Safari Completed: SAF-7D6N-1007 — 7-Day Serengeti & Ngorongoro Safari");
    }

    private TestEmailData generateSafariPostTripReminderTestData(User user) {
        Map<String, String> variables = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("safariCode", "SAF-7D6N-1007");
        variables.put("safariName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("endDate", now.minusDays(3).format(dateFormatter));
        variables.put("daysSinceEnd", "3");
        variables.put("pendingTasks",
            "<li style=\"margin: 4px 0;\">Collect guest feedback and reviews</li>" +
            "<li style=\"margin: 4px 0;\">Reconcile expenses and receipts</li>" +
            "<li style=\"margin: 4px 0;\">Update driver/guide logs</li>" +
            "<li style=\"margin: 4px 0;\">Close out any open invoices</li>" +
            "<li style=\"margin: 4px 0;\">File park entry receipts</li>");
        variables.put("alertDate", now.format(dateFormatter));

        return new TestEmailData(variables, "[TEST] Post-Trip Tasks Pending — SAF-7D6N-1007 — ended 3 day(s) ago");
    }

    /**
     * Sample values for the availability request — a real-shaped ask, not lorem.
     *
     * The point of a test send is to see what a lodge will see, so the sample has the two things
     * that make this letter awkward: several room types on one booking, and a meal plan that
     * CHANGES mid-stay. A test with one room and one board would look perfect while hiding both.
     *
     * roomConfiguration and mealPlan arrive as HTML list items, exactly as the live path supplies
     * them, so the test exercises the same markup rather than a simplified version of it.
     */
    private TestEmailData generateAvailabilityRequestTestData(User user) {
        Map<String, String> variables = new HashMap<>();

        LocalDate checkIn = LocalDate.now().plusMonths(5).withDayOfMonth(29);
        LocalDate checkOut = checkIn.plusDays(3);
        DateTimeFormatter slash = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("d MMM yyyy");

        String accent = "#1c7a58";

        variables.put("greetingName", "Reservations Team");
        variables.put("brandName", companyName != null && !companyName.isBlank()
            ? companyName : "Kabengo Safaris");
        variables.put("accommodationName", "Tukaone Weavers Camp");
        variables.put("checkIn", checkIn.format(slash));
        variables.put("checkOut", checkOut.format(slash));
        variables.put("nights", "3");
        variables.put("guestCount", "5 Guests");
        variables.put("paxBreakdown", "4 non-resident adults, 1 non-resident child");
        variables.put("roomConfiguration",
            "<li style=\"margin: 2px 0\"><strong style=\"color: #111827\">2 &times;</strong> Double Room"
            + " <span style=\"color: #6b7280\">&middot; Standard Tent</span></li>"
            + "<li style=\"margin: 2px 0\"><strong style=\"color: #111827\">1 &times;</strong> Triple Room"
            + " <span style=\"color: #6b7280\">&middot; Standard Tent</span></li>");
        variables.put("mealPlan",
            night(checkIn, slash, "Half Board")
            + night(checkIn.plusDays(1), slash, "Full Board")
            + night(checkIn.plusDays(2), slash, "Full Board"));
        /* one visit in the sample, as in the live letter: a second visit is a second request */
        variables.put("stayBlocks", "");
        variables.put("reference", "SAF-14D13N-01003 · Ultimate Northern Tanzania, Culture & Zanzibar Beach");
        variables.put("accentColor", accent);

        String subject = "[TEST] Availability Request · " + variables.get("accommodationName")
            + " · " + checkIn.getDayOfMonth() + "–" + checkOut.format(shortDate);

        return new TestEmailData(variables, subject);
    }

    /** One night of the sample meal plan, in the markup the live letter uses. */
    private String night(LocalDate date, DateTimeFormatter slash, String board) {
        return "<li style=\"margin: 2px 0\"><span style=\"color: #6b7280\">"
            + date.format(slash) + "</span> &ndash; <strong style=\"color: #111827\">"
            + board + "</strong></li>";
    }

    private TestEmailData generateSafariDetailsTestData(User user) {
        Map<String, String> variables = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("safariCode", "SAF-7D6N-1007");
        variables.put("safariName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("startDate", now.plusMonths(2).format(dateFormatter));
        variables.put("endDate", now.plusMonths(2).plusDays(6).format(dateFormatter));
        variables.put("totalDays", "7");
        variables.put("totalNights", "6");
        variables.put("startLocation", "Arusha");
        variables.put("endLocation", "Arusha");
        variables.put("itineraryName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("state", "Confirmed");
        variables.put("daySummary",
            "<div class=\"detail-row\"><span class=\"detail-label\">Day 1 (Jun 15)</span><span class=\"detail-value\">Arrival in Arusha</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Day 2 (Jun 16)</span><span class=\"detail-value\">Tarangire National Park</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Day 3 (Jun 17)</span><span class=\"detail-value\">Lake Manyara National Park</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Day 4 (Jun 18)</span><span class=\"detail-value\">Ngorongoro Crater</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Day 5 (Jun 19)</span><span class=\"detail-value\">Serengeti National Park</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Day 6 (Jun 20)</span><span class=\"detail-value\">Serengeti Game Drives</span></div>" +
            "<div class=\"detail-row\"><span class=\"detail-label\">Day 7 (Jun 21)</span><span class=\"detail-value\">Departure from Arusha</span></div>");
        variables.put("specialRequests", "Vegetarian meals preferred. Would love to see the Great Migration.");
        variables.put("emergencyContact", "+255 700 123 456 (Kabengo Safaris 24/7)");
        variables.put("sentDate", now.format(dateFormatter));
        variables.put("companyEmail", "info@kabengosafaris.com");

        return new TestEmailData(variables, "[TEST] Your Safari Details: SAF-7D6N-1007 — 7-Day Serengeti & Ngorongoro Safari");
    }

    private TestEmailData generateSafariCustomerMessageTestData(User user) {
        Map<String, String> variables = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        variables.put("customerName", user.getFirstName() + " " + user.getLastName());
        variables.put("safariCode", "SAF-7D6N-1007");
        variables.put("safariName", "7-Day Serengeti & Ngorongoro Safari");
        variables.put("startDate", now.plusMonths(2).format(dateFormatter));
        variables.put("endDate", now.plusMonths(2).plusDays(6).format(dateFormatter));
        variables.put("messageSubject", "Important Update: Vehicle Upgrade");
        variables.put("messageBody",
            "<p>We are pleased to inform you that we have upgraded your safari vehicle from a standard Land Cruiser to a <strong>pop-top Land Cruiser</strong> at no additional cost!</p>" +
            "<p>This vehicle features a hydraulic roof for 360-degree wildlife viewing and is equipped with:</p>" +
            "<ul><li>Charging ports for cameras and phones</li><li>Cool box for refreshments</li><li>Binoculars for each passenger</li></ul>" +
            "<p>Your driver-guide, <strong>Joseph Mollel</strong>, has over 15 years of experience in the Serengeti ecosystem and speaks English, French, and Swahili.</p>");
        variables.put("sentDate", now.format(dateFormatter));
        variables.put("senderName", "Ricksy Faby, Operations Manager");
        variables.put("companyEmail", "info@kabengosafaris.com");

        return new TestEmailData(variables, "[TEST] SAF-7D6N-1007 — Important Update: Vehicle Upgrade");
    }

    /**
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
