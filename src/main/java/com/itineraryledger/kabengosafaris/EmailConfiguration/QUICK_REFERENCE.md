# Email Configuration System - Quick Reference

## File Structure

```
EmailConfiguration/
├── EmailConfiguration.java                    # Entity (SMTP configuration)
├── EmailConfigurationPermission.java          # Entity (access control)
├── EmailConfigurationRepository.java          # Data access
├── EmailConfigurationPermissionRepository.java # Data access
├── EmailConfigurationService.java             # Business logic
├── EmailConfigurationController.java          # REST API
├── EmailSender.java                           # Email sending utility
├── EncryptionUtil.java                        # Password encryption
├── EmailConfigurationInitializer.java         # Startup initialization
├── EMAIL_CONFIGURATION_GUIDE.md               # Full documentation
└── QUICK_REFERENCE.md                         # This file
```

---

## Core Classes

### EmailConfiguration Entity
Stores SMTP server configuration and credentials

**Key Fields:**
- `name` - Unique identifier
- `fromEmail` - Sender email address
- `smtpHost` - SMTP server hostname
- `smtpPort` - SMTP server port (25, 465, 587, 2525)
- `smtpUsername` - Authentication username
- `smtpPassword` - ENCRYPTED password
- `useTls` - Enable TLS (port 587)
- `useSsl` - Enable SSL (port 465)
- `enabled` - Enable/disable configuration
- `isDefault` - Mark as default config
- `rateLimitPerMinute` - Rate limiting
- `maxRetryAttempts` - Retry count

### EmailConfigurationPermission Entity
Controls who can use which email configurations

**Key Fields:**
- `emailConfiguration` - Which config this permission grants
- `user` - Specific user (null = role-based)
- `role` - Specific role (null = user-specific)
- `enabled` - Enable/disable permission
- `maxEmailsPerDay` - Usage limit
- `expiresAt` - Expiration date (null = never)

### EmailConfigurationService
Main business logic service

**Key Methods:**
```java
// CRUD Operations
EmailConfiguration createConfiguration(EmailConfiguration config)
EmailConfiguration updateConfiguration(Long id, EmailConfiguration updates)
EmailConfiguration getConfiguration(Long id)
EmailConfiguration getConfigurationByName(String name)
EmailConfiguration getDefaultConfiguration()
List<EmailConfiguration> getAllConfigurations()
void deleteConfiguration(Long id)

// Connection Testing
boolean testEmailConnection(EmailConfiguration config)

// Permission Management
EmailConfigurationPermission grantUserAccess(Long configId, Long userId, ...)
EmailConfigurationPermission grantRoleAccess(Long configId, Long roleId, ...)
boolean userHasAccessToConfig(Long configId, Long userId)
boolean roleHasAccessToConfig(Long configId, Long roleId)
void revokeUserAccess(Long configId, Long userId)

// Email Sending (via JavaMailSender)
JavaMailSender getMailSender(Long configId)

// Statistics
void recordSuccessfulSend(Long configId)
void recordFailedSend(Long configId)
```

### EmailSender Component
Utility for sending emails

**Key Methods:**
```java
// Simple text email
void sendSimpleEmail(String to, String subject, String body)
void sendSimpleEmail(String to, String subject, String body, Long configId)

// HTML email
void sendHtmlEmail(String to, String subject, String htmlBody)
void sendHtmlEmail(String to, String subject, String htmlBody, Long configId)

// With retry logic
void sendEmailWithRetry(String to, String subject, String body, int maxRetries)
void sendHtmlEmailWithRetry(String to, String subject, String htmlBody, int maxRetries)

// Batch sending
void sendBatchEmail(List<String> recipients, String subject, String body)

// Email validation
static boolean isValidEmailAddress(String email)
static List<String> filterValidEmails(List<String> emails)
```

---

## REST API Quick Reference

### Create Configuration
```bash
POST /api/email-config
Headers: Authorization: Bearer TOKEN
Body: { name, displayName, fromEmail, smtpHost, ... }
Response: 201 Created with config details
```

### List All Configurations
```bash
GET /api/email-config
Headers: Authorization: Bearer TOKEN
Response: 200 OK with config list
```

### Get Configuration by ID
```bash
GET /api/email-config/{id}
Headers: Authorization: Bearer TOKEN
Response: 200 OK with config details
```

### Get by Name
```bash
GET /api/email-config/name/{name}
Headers: Authorization: Bearer TOKEN
Response: 200 OK or 404 Not Found
```

### Get Default Configuration
```bash
GET /api/email-config/default
Headers: Authorization: Bearer TOKEN
Response: 200 OK with default config
```

### Update Configuration
```bash
PUT /api/email-config/{id}
Headers: Authorization: Bearer TOKEN
Body: { fields to update }
Response: 200 OK with updated config
```

### Delete Configuration
```bash
DELETE /api/email-config/{id}
Headers: Authorization: Bearer TOKEN
Response: 200 OK or 400 Bad Request (if default)
```

### Test Connection
```bash
POST /api/email-config/{id}/test
Headers: Authorization: Bearer TOKEN
Response: 200 OK { success: true/false, testedAt, error }
```

### Grant User Access
```bash
POST /api/email-config/{id}/grant-user/{userId}?reason=TEXT&grantedBy=TEXT
Headers: Authorization: Bearer TOKEN
Response: 201 Created with permission details
```

### Grant Role Access
```bash
POST /api/email-config/{id}/grant-role/{roleId}?reason=TEXT&grantedBy=TEXT
Headers: Authorization: Bearer TOKEN
Response: 201 Created with permission details
```

### Revoke User Access
```bash
DELETE /api/email-config/{id}/revoke-user/{userId}
Headers: Authorization: Bearer TOKEN
Response: 200 OK
```

---

## Common Use Cases

### Setup Gmail Account
1. Create App Password: https://support.google.com/accounts/answer/185833
2. Create configuration:
   ```json
   {
     "name": "gmail",
     "fromEmail": "your-email@gmail.com",
     "smtpHost": "smtp.gmail.com",
     "smtpPort": 587,
     "smtpUsername": "your-email@gmail.com",
     "smtpPassword": "app-password-generated",
     "useTls": true
   }
   ```
3. Test connection
4. Set as default
5. Enable it

### Send Email in Code
```java
@Autowired
private EmailSender emailSender;

// Option 1: Simple email with default config
emailSender.sendSimpleEmail("user@example.com", "Subject", "Body");

// Option 2: HTML email with specific config
emailSender.sendHtmlEmail(email, subject, htmlBody, configId);

// Option 3: With retry logic
emailSender.sendEmailWithRetry(email, subject, body, 3);
```

### Grant Access to Role
```java
@Autowired
private EmailConfigurationService emailConfigService;

// All users with "booking_manager" role can now send from this config
emailConfigService.grantRoleAccess(
    configId,
    roleId,
    "Allow booking managers to send confirmations",
    "admin@company.com"
);
```

### Check User Access
```java
// Check if user can use this config
if (emailConfigService.userHasAccessToConfig(configId, userId)) {
    // Send email using this config
}
```

---

## Required Permissions

| Action | Permission |
|--------|-----------|
| Create config | `PERM_email_config_create` or `ROLE_ADMIN` |
| Read configs | `PERM_email_config_read` or `ROLE_ADMIN` |
| Update config | `PERM_email_config_update` or `ROLE_ADMIN` |
| Delete config | `PERM_email_config_delete` or `ROLE_ADMIN` |
| Test connection | `PERM_email_config_test` or `ROLE_ADMIN` |
| Grant access | `PERM_email_config_grant` or `ROLE_ADMIN` |
| Send email | Any user with access to config (via permissions) |

---

## Database Configuration

Add to `application.properties`:

```properties
# Email Configuration (optional, uses defaults if not set)
email.config.enable-test-send=false
email.config.default-retry-attempts=3
email.config.default-retry-delay-seconds=5
```

---

## Configuration Examples

### Gmail
```json
{
  "name": "gmail",
  "displayName": "Gmail SMTP",
  "fromEmail": "noreply@company.com",
  "fromName": "Company Name",
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "company@gmail.com",
  "smtpPassword": "app-password",
  "useTls": true,
  "useSsl": false
}
```

### Outlook
```json
{
  "name": "outlook",
  "displayName": "Outlook SMTP",
  "smtpHost": "smtp.office365.com",
  "smtpPort": 587,
  "useTls": true,
  "useSsl": false
}
```

### Custom SMTP
```json
{
  "name": "custom",
  "displayName": "Custom SMTP",
  "smtpHost": "mail.company.com",
  "smtpPort": 25,
  "useTls": false,
  "useSsl": false
}
```

### SendGrid
```json
{
  "name": "sendgrid",
  "displayName": "SendGrid SMTP",
  "smtpHost": "smtp.sendgrid.net",
  "smtpPort": 587,
  "smtpUsername": "apikey",
  "smtpPassword": "SG.xxxxxxxxxx",
  "useTls": true,
  "useSsl": false
}
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Connection fails | Check host/port, verify credentials, test from command line |
| Permission denied | Grant user/role access via `grant-user` or `grant-role` endpoint |
| Email not sending | Check if config is enabled, verify permissions, check logs |
| Password encryption error | Ensure encryption key is set correctly |
| Rate limiting | Configure `rateLimitPerMinute` on configuration |
| Database error | Run migrations, check DB permissions |

---

## Key Concepts

### Configuration
A stored SMTP server configuration with credentials (encrypted). Can be enabled/disabled and tested independently.

### Permission
Controls which users/roles can use which configurations. Can have expiration dates and usage limits.

### Encryption
All passwords are automatically encrypted using AES-256 before storing in database.

### Rate Limiting
Configurable per configuration to prevent spam (e.g., max 100 emails/minute).

### Retry Logic
Failed emails can be automatically retried with configurable delays.

### Statistics
Track sent/failed email counts per configuration for monitoring.

---

## Integration Points

### With Audit Logging
Add `@AuditLogAnnotation` to service methods to track all configuration changes.

### With Permissions System
Uses existing Role/Permission system for access control.

### With User System
Integrates with User entity for user-specific permissions.

### With Role System
Integrates with Role entity for role-based permissions.

---

## Performance Tips

1. **Cache mail senders** - EmailSender caches JavaMailSender instances by configId
2. **Batch emails** - Use `sendBatchEmail()` with delays for bulk sending
3. **Async sending** - Consider marking email operations as `@Async`
4. **Test before production** - Always test SMTP connection before enabling
5. **Monitor statistics** - Check `emailsSentCount` and `emailsFailedCount`

---

## Security Tips

1. **Encryption Key Setup** - See [Encryption Key Configuration](#encryption-key-configuration) below
2. **Use App Passwords** - Gmail/Outlook require app-specific passwords
3. **Rotate Credentials** - Change passwords periodically
4. **Limit Access** - Only grant permissions to users who need them
5. **Audit Access** - Review who has access to which configs
6. **HTTPS Only** - Always use HTTPS for API calls
7. **Expire Permissions** - Set expiration dates on temporary access

---

## Encryption Key Configuration

### What It Is

All SMTP passwords in the email configuration system are encrypted using AES-256. The encryption key must be properly configured, especially for production.

### Development Setup

For development, the system uses a default key automatically. You'll see a warning:

```
WARN ... No encryption key found in environment or properties. Using default key for development only.
```

This is normal for development and requires no additional setup.

### Production Setup (REQUIRED)

For production, you **must** set the encryption key:

```bash
# Recommended: Set as environment variable
export EMAIL_ENCRYPTION_KEY="your-base64-encoded-32-byte-key"
```

### Generate a Secure Key

Generate a Base64-encoded 256-bit (32-byte) key:

```bash
# Using OpenSSL
openssl rand 32 | base64
# Example: 8JY8VxzKfXyP3mL9N5qR2tWaBcDeFgHiJkLmNoPqRsT=
```

### Key Resolution Order

The system loads the encryption key in this order:

1. Environment variable: `EMAIL_ENCRYPTION_KEY`
2. Application property: `email.encryption.key` (application.properties)
3. Legacy environment variable: `ENCRYPTION_KEY`
4. Default key (development only)

### Docker/Kubernetes

```dockerfile
ENV EMAIL_ENCRYPTION_KEY="your-key-here"
```

```yaml
env:
  - name: EMAIL_ENCRYPTION_KEY
    valueFrom:
      secretKeyRef:
        name: email-secrets
        key: encryption-key
```

### Important Rules

- Never commit encryption keys to version control
- Use different keys for dev, staging, and production
- Store keys in a secrets vault (HashiCorp Vault, AWS Secrets Manager, etc.)
- Rotate keys periodically
- If compromised, re-encrypt all passwords with a new key

---

## Next Steps

1. Create first email configuration
2. Test SMTP connection
3. Grant access to users/roles
4. Integrate with your service classes
5. Send test emails
6. Monitor statistics
7. Set up alerts for failures

---

Last Updated: 2025-11-19
Version: 1.0.0
