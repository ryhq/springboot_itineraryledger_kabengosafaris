# Email Configuration System Guide

## Overview

The Email Configuration System provides a database-driven, secure way to manage multiple email accounts and send emails from your Spring Boot application. It allows you to:

- **Configure multiple email providers** (Gmail, Outlook, custom SMTP servers)
- **Control access via permissions** (user-level and role-level access control)
- **Send emails dynamically** from configured accounts
- **Track email statistics** (sent count, failed count)
- **Test SMTP connections** to validate configurations
- **Encrypt passwords** securely before storing in database
- **Implement retry logic** for failed email sends
- **Rate limit emails** per minute/hour
- **Audit all email activities** with user who sent and from which account

---

## Architecture

### Core Components

1. **EmailConfiguration** - Entity storing SMTP configuration details
2. **EmailConfigurationPermission** - Control who can use which email config
3. **EmailConfigurationService** - Business logic for CRUD and email sending
4. **EmailConfigurationController** - REST API endpoints
5. **EmailSender** - Utility component for sending emails
6. **EncryptionUtil** - Encrypts sensitive data (passwords)
7. **EmailConfigurationRepository** - Database access
8. **EmailConfigurationPermissionRepository** - Permission database access
9. **EmailConfigurationInitializer** - Initializes default configurations on startup

### Database Tables

#### email_configurations
Stores SMTP server configurations and credentials

```sql
CREATE TABLE email_configurations (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(255) NOT NULL,
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INT NOT NULL,
    smtp_username VARCHAR(255) NOT NULL,
    smtp_password VARCHAR(1000) NOT NULL,  -- ENCRYPTED
    use_tls BOOLEAN NOT NULL,
    use_ssl BOOLEAN NOT NULL,
    enabled BOOLEAN NOT NULL,
    is_default BOOLEAN NOT NULL,
    provider_type VARCHAR(100) NOT NULL,
    rate_limit_per_minute INT,
    max_retry_attempts INT,
    retry_delay_seconds INT,
    verify_on_save BOOLEAN,
    last_tested_at DATETIME,
    last_error_message VARCHAR(1000),
    emails_sent_count BIGINT,
    emails_failed_count BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
```

#### email_configuration_permissions
Controls which users/roles can use which email configs

```sql
CREATE TABLE email_configuration_permissions (
    id BIGINT PRIMARY KEY,
    email_configuration_id BIGINT NOT NULL,
    user_id BIGINT,
    role_id BIGINT,
    enabled BOOLEAN NOT NULL,
    max_emails_per_day INT,
    grant_reason LONGTEXT,
    granted_by VARCHAR(255),
    created_at DATETIME NOT NULL,
    expires_at DATETIME,
    FOREIGN KEY (email_configuration_id) REFERENCES email_configurations(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

---

## Quick Start

### 1. Initial Setup

On application startup, three template configurations are automatically created:

- **gmail** - Gmail SMTP template (disabled by default)
- **outlook** - Outlook/Office365 template (disabled by default)
- **default** - Generic SMTP template (set as default, disabled)

### 2. Configure an Email Account

Update a template configuration with real credentials:

```bash
curl -X PUT http://localhost:4450/api/email-config/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Production Notifications",
    "fromEmail": "notifications@yourcompany.com",
    "fromName": "Your Company",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "smtpUsername": "your-email@gmail.com",
    "smtpPassword": "your-app-password",
    "enabled": true,
    "isDefault": true,
    "verifyOnSave": true
  }'
```

### 3. Test the Configuration

```bash
curl -X POST http://localhost:4450/api/email-config/1/test \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. Grant Access to Users/Roles

```bash
# Grant user access
curl -X POST http://localhost:4450/api/email-config/1/grant-user/5 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Allow user to send notifications",
    "grantedBy": "admin@company.com"
  }'

# Grant role access
curl -X POST http://localhost:4450/api/email-config/1/grant-role/2 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Allow booking managers to send confirmations",
    "grantedBy": "admin@company.com"
  }'
```

### 5. Send an Email

From a service or controller:

```java
@Autowired
private EmailSender emailSender;

// Send simple text email using default config
emailSender.sendSimpleEmail("customer@example.com", "Welcome", "Welcome to Kabengo Safaris!");

// Send HTML email using specific config
String htmlBody = "<h1>Welcome</h1><p>Thank you for booking with us!</p>";
emailSender.sendHtmlEmail("customer@example.com", "Welcome", htmlBody, configId);

// Send with retry logic
emailSender.sendEmailWithRetry("customer@example.com", "Booking Confirmation",
    "Your booking is confirmed", 3);
```

---

## REST API Endpoints

### Configuration Management

#### Create Configuration
```
POST /api/email-config
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "name": "marketing",
  "displayName": "Marketing Email",
  "fromEmail": "marketing@company.com",
  "fromName": "Marketing Team",
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "marketing@gmail.com",
  "smtpPassword": "app-password",
  "useTls": true,
  "useSsl": false,
  "enabled": true,
  "providerType": "GMAIL"
}
```

#### Get All Configurations
```
GET /api/email-config
Authorization: Bearer TOKEN
```

#### Get Configuration by ID
```
GET /api/email-config/{id}
Authorization: Bearer TOKEN
```

#### Get Configuration by Name
```
GET /api/email-config/name/{name}
Authorization: Bearer TOKEN
```

#### Get Default Configuration
```
GET /api/email-config/default
Authorization: Bearer TOKEN
```

#### Update Configuration
```
PUT /api/email-config/{id}
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "displayName": "Updated Name",
  "fromEmail": "new-email@company.com",
  "enabled": true
}
```

#### Delete Configuration
```
DELETE /api/email-config/{id}
Authorization: Bearer TOKEN
```

#### Test Connection
```
POST /api/email-config/{id}/test
Authorization: Bearer TOKEN
```

### Permission Management

#### Grant User Access
```
POST /api/email-config/{id}/grant-user/{userId}?reason=string&grantedBy=string
Authorization: Bearer TOKEN
```

#### Grant Role Access
```
POST /api/email-config/{id}/grant-role/{roleId}?reason=string&grantedBy=string
Authorization: Bearer TOKEN
```

#### Revoke User Access
```
DELETE /api/email-config/{id}/revoke-user/{userId}
Authorization: Bearer TOKEN
```

---

## Security Features

### 1. Password Encryption
All SMTP passwords are encrypted using AES-256 before storing in the database.

```java
// Automatic encryption on save
config.setSmtpPassword(EncryptionUtil.encrypt(plainPassword));

// Automatic decryption when creating mail sender
String decrypted = EncryptionUtil.decrypt(encryptedPassword);
```

### 2. Permission-Based Access Control

Only users with appropriate permissions can:
- Create/update/delete configurations
- Test connections
- Grant/revoke access
- Send emails

```java
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_create')")
```

### 3. User/Role-Based Email Usage

Control which users and roles can use which email accounts:

- Grant specific users access to certain configs
- Grant entire roles access to email configs
- Set expiration dates on permissions
- Track who created which permissions

### 4. Audit Logging

All email configuration changes are logged with:
- User who made the change
- Timestamp
- Configuration changed
- Change details

---

## Common Configuration Examples

### Gmail Configuration

```json
{
  "name": "gmail",
  "displayName": "Gmail SMTP",
  "fromEmail": "your-email@gmail.com",
  "fromName": "Your Name",
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "your-email@gmail.com",
  "smtpPassword": "your-app-password",
  "useTls": true,
  "useSsl": false,
  "providerType": "GMAIL"
}
```

**Note:** For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833), not your actual Gmail password.

### Outlook/Office365

```json
{
  "name": "outlook",
  "displayName": "Outlook SMTP",
  "fromEmail": "your-email@outlook.com",
  "fromName": "Your Name",
  "smtpHost": "smtp.office365.com",
  "smtpPort": 587,
  "smtpUsername": "your-email@outlook.com",
  "smtpPassword": "your-password",
  "useTls": true,
  "useSsl": false,
  "providerType": "OUTLOOK"
}
```

### Custom SMTP Server

```json
{
  "name": "custom",
  "displayName": "Custom SMTP",
  "fromEmail": "noreply@company.com",
  "fromName": "Company Name",
  "smtpHost": "mail.company.com",
  "smtpPort": 25,
  "smtpUsername": "username",
  "smtpPassword": "password",
  "useTls": true,
  "useSsl": false,
  "providerType": "CUSTOM"
}
```

### SendGrid SMTP

```json
{
  "name": "sendgrid",
  "displayName": "SendGrid SMTP",
  "fromEmail": "noreply@company.com",
  "fromName": "Company Name",
  "smtpHost": "smtp.sendgrid.net",
  "smtpPort": 587,
  "smtpUsername": "apikey",
  "smtpPassword": "SG.xxxxxxxxxxxxx",
  "useTls": true,
  "useSsl": false,
  "providerType": "SENDGRID"
}
```

---

## Usage Examples

### 1. Send Welcome Email to New Users

```java
@Service
@RequiredArgsConstructor
public class UserRegistrationService {
    private final EmailSender emailSender;

    public void sendWelcomeEmail(User user) {
        String htmlBody = String.format(
            "<h1>Welcome %s!</h1>" +
            "<p>Thank you for registering with Kabengo Safaris.</p>" +
            "<p>Your username: %s</p>",
            user.getFirstName(),
            user.getUsername()
        );

        emailSender.sendHtmlEmail(
            user.getEmail(),
            "Welcome to Kabengo Safaris",
            htmlBody
        );
    }
}
```

### 2. Send Booking Confirmation

```java
@Service
@RequiredArgsConstructor
public class BookingService {
    private final EmailSender emailSender;
    private final EmailConfigurationService emailConfigService;

    public void sendBookingConfirmation(Booking booking) {
        // Get the "confirmations" email config
        EmailConfiguration config = emailConfigService.getConfigurationByName("confirmations");

        String htmlBody = buildBookingConfirmationHTML(booking);

        emailSender.sendHtmlEmailWithRetry(
            booking.getCustomerEmail(),
            "Booking Confirmation #" + booking.getId(),
            htmlBody,
            3,  // Retry 3 times
            config.getId()
        );
    }
}
```

### 3. Bulk Email to Group (with Rate Limiting)

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final EmailSender emailSender;

    public void notifyAllCustomers(String subject, String message, int delayMs) {
        List<String> customerEmails = getCustomerEmails();
        List<String> validEmails = EmailSender.filterValidEmails(customerEmails);

        for (String email : validEmails) {
            emailSender.sendSimpleEmail(email, subject, message);

            try {
                Thread.sleep(delayMs);  // Rate limiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 4. Permission-Based Email Access

```java
@Service
@RequiredArgsConstructor
public class SecureEmailService {
    private final EmailSender emailSender;
    private final EmailConfigurationService configService;
    private final SecurityContext securityContext;

    public void sendEmailAsCurrentUser(String to, String subject, String body) {
        User currentUser = securityContext.getCurrentUser();

        // Get email configs user has access to
        List<EmailConfiguration> accessibleConfigs =
            configService.getAccessibleConfigurationsForUser(currentUser.getId());

        if (accessibleConfigs.isEmpty()) {
            throw new IllegalAccessException("User has no email configurations");
        }

        // Use first accessible config
        EmailConfiguration config = accessibleConfigs.get(0);
        emailSender.sendSimpleEmail(to, subject, body, config.getId());
    }
}
```

---

## Security Best Practices

### 1. Encryption Key Management

The encryption key is automatically loaded from environment variables or properties at runtime. **For development**, a default key is used (you'll see a warning).

**For production, you MUST set the encryption key:**

```bash
# Set environment variable (RECOMMENDED)
export EMAIL_ENCRYPTION_KEY="your-base64-encoded-32-byte-key"
```

**Generate a secure key using OpenSSL:**
```bash
openssl rand 32 | base64
# Output example: 8JY8VxzKfXyP3mL9N5qR2tWaBcDeFgHiJkLmNoPqRsT=
```

**Or set in application.properties (NOT RECOMMENDED for production):**
```properties
email.encryption.key=your-base64-encoded-32-byte-key
```

**Key resolution order:**
1. `EMAIL_ENCRYPTION_KEY` environment variable
2. `email.encryption.key` application property
3. `ENCRYPTION_KEY` environment variable (legacy)
4. Default key (development only)

**Docker example:**
```dockerfile
ENV EMAIL_ENCRYPTION_KEY="your-key-here"
```

**Kubernetes example:**
```yaml
env:
  - name: EMAIL_ENCRYPTION_KEY
    valueFrom:
      secretKeyRef:
        name: email-secrets
        key: encryption-key
```

**Important:**
- The key must be a Base64-encoded 256-bit (32-byte) key
- Never commit keys to version control
- Use different keys per environment (dev, staging, prod)
- Store in a secrets vault (HashiCorp Vault, AWS Secrets Manager, etc.)
- Rotate keys periodically

### 2. Use App Passwords for Gmail/Outlook

Never use your actual account password. Generate an app password:

- **Gmail:** [Create App Password](https://support.google.com/accounts/answer/185833)
- **Outlook:** [Create App Password](https://support.microsoft.com/en-us/account-billing/using-app-passwords-with-your-microsoft-account)

### 3. Restrict Email Configuration Access

Add to `application.properties`:

```properties
# Only ADMIN role can manage email configurations
email.config.admin-only=true

# Required permission for sending emails
email.send.required-permission=PERM_email_send
```

### 4. Audit Logging Integration

Enable audit logging for email configuration changes:

```java
@AuditLogAnnotation(action = "CREATE_EMAIL_CONFIG")
public EmailConfiguration createConfiguration(EmailConfiguration config) { ... }
```

### 5. Rate Limiting

Configure rate limiting per configuration:

```java
config.setRateLimitPerMinute(100);  // Max 100 emails/minute
config.setMaxEmailsPerDay(10000);   // Max 10k emails/day per user
```

---

## Troubleshooting

### Configuration Won't Connect

1. **Check credentials** - Username and password are correct
2. **Verify SMTP settings** - Host and port are correct for provider
3. **Test connection** - Use the `/test` endpoint
4. **Check firewall** - Ensure outbound SMTP port is open
5. **Enable TLS/SSL** - Most providers require secure connection

### Email Not Sending

1. **Check permissions** - User/role has access to configuration
2. **Verify enabled** - Configuration must be enabled
3. **Review logs** - Check application logs for error messages
4. **Test locally** - Try sending to yourself first
5. **Check retry logic** - Emails are retried automatically

### Database Errors

1. **Run migrations** - Ensure tables exist
2. **Check DB connection** - Verify database is accessible
3. **Check user permissions** - DB user has CREATE/ALTER rights
4. **Review SQL logs** - Enable SQL debug logging

### Encryption Issues

1. **Key mismatch** - Ensure encryption key is consistent
2. **Corrupted password** - Password may have been corrupted; update it
3. **Charset issues** - Use UTF-8 encoding

---

## Permissions Required

To use email configuration endpoints, users need one of:

- `ROLE_ADMIN` - Full access
- `PERM_email_config_create` - Create configurations
- `PERM_email_config_read` - View configurations
- `PERM_email_config_update` - Update configurations
- `PERM_email_config_delete` - Delete configurations
- `PERM_email_config_test` - Test connections
- `PERM_email_config_grant` - Grant/revoke access
- `PERM_email_send` - Send emails

---

## Future Enhancements

Potential improvements to the system:

1. **Email Templates** - Store reusable email templates in database
2. **Scheduled Emails** - Send emails at specific times
3. **Email Queue** - Queue emails for asynchronous sending
4. **Attachments** - Full support for file attachments
5. **Analytics** - Track open rates, click rates
6. **Webhook Integration** - Receive delivery status updates
7. **Email History** - Store all sent emails in database
8. **Multi-language Support** - Localized email templates
9. **A/B Testing** - Test different email variants
10. **Integration with ESPs** - SendGrid, Mailgun, AWS SES APIs

---

## Support

For issues or questions:

1. Check the logs in your application
2. Verify database tables exist
3. Test SMTP connection manually
4. Review this guide's troubleshooting section
5. Contact the development team

Last Updated: 2025-11-19
Version: 1.0.0
