# Email Configuration System

A comprehensive, database-driven email management system for the Kabengo Safaris Spring Boot application.

## Overview

This module provides a secure, flexible way to manage multiple email accounts and send emails from your application. Features include:

- **Multi-provider support** - Gmail, Outlook, custom SMTP servers
- **Encrypted credentials** - AES-256 password encryption
- **Permission-based access** - User and role-level access control
- **Connection testing** - Validate SMTP configurations
- **Email statistics** - Track sent/failed emails per configuration
- **Retry logic** - Automatic retry with configurable delays
- **Rate limiting** - Control email sending speed
- **Audit logging** - Track all configuration changes

## Files Included

### Core Entities
- **[EmailConfiguration.java](EmailConfiguration.java)** - SMTP configuration entity
- **[EmailConfigurationPermission.java](EmailConfigurationPermission.java)** - Access control entity

### Data Access
- **[EmailConfigurationRepository.java](EmailConfigurationRepository.java)** - Configuration CRUD
- **[EmailConfigurationPermissionRepository.java](EmailConfigurationPermissionRepository.java)** - Permission CRUD

### Business Logic
- **[EmailConfigurationService.java](EmailConfigurationService.java)** - Main service with CRUD and email logic
- **[EmailSender.java](EmailSender.java)** - Email sending utility component
- **[EncryptionUtil.java](EncryptionUtil.java)** - Password encryption utility

### REST API
- **[EmailConfigurationController.java](EmailConfigurationController.java)** - REST endpoints for management

### Initialization
- **[EmailConfigurationInitializer.java](EmailConfigurationInitializer.java)** - Creates default configurations on startup

### Documentation
- **[EMAIL_CONFIGURATION_GUIDE.md](EMAIL_CONFIGURATION_GUIDE.md)** - Complete guide with examples
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Quick reference for common tasks
- **[README.md](README.md)** - This file

## Quick Start

### 1. Database Setup
The JPA entities will automatically create tables on first run. No manual SQL needed.

### 2. Default Configurations
On startup, three template configurations are created:
- `gmail` - Gmail SMTP template
- `outlook` - Outlook/Office365 template
- `default` - Generic SMTP template (set as default)

### 3. Configure Your Email Account
Update one of the templates with real credentials:

```bash
curl -X PUT http://localhost:4450/api/email-config/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Production Notifications",
    "fromEmail": "notifications@yourcompany.com",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "smtpUsername": "your-email@gmail.com",
    "smtpPassword": "your-app-password",
    "enabled": true,
    "isDefault": true,
    "verifyOnSave": true
  }'
```

### 4. Send Your First Email
```java
@Autowired
private EmailSender emailSender;

public void sendWelcome(String email) {
    emailSender.sendSimpleEmail(
        email,
        "Welcome to Kabengo Safaris",
        "Thank you for signing up!"
    );
}
```

## Architecture

### Entity Relationships
```
EmailConfiguration (1) ----< (many) EmailConfigurationPermission
                                              |
                                      +-------+-------+
                                      |               |
                                  User (many)    Role (many)
```

### Data Flow
```
REST API Controller
    ↓
EmailConfigurationService
    ├── CRUD operations
    ├── Permission management
    └── JavaMailSender creation
        ↓
    EmailSender (utility)
        ├── sendSimpleEmail()
        ├── sendHtmlEmail()
        ├── sendWithRetry()
        └── sendBatchEmail()
            ↓
        JavaMailSender
            ↓
        SMTP Server (Gmail, Outlook, etc.)
```

## Key Features

### 1. Multiple Email Providers
- Gmail SMTP
- Outlook/Office365
- Custom SMTP servers
- SendGrid SMTP
- Any SMTP-compatible service

### 2. Encrypted Password Storage
All SMTP passwords are encrypted using AES-256 before storing in the database.

### 3. Permission-Based Access Control
- Grant specific users access to email configs
- Grant entire roles access
- Set expiration dates on permissions
- Track daily usage per user

### 4. SMTP Connection Testing
Test connectivity before using a configuration in production.

### 5. Email Statistics
Track sent and failed emails per configuration for monitoring.

### 6. Retry Logic
Failed emails are automatically retried with configurable delays.

### 7. Rate Limiting
Control email sending speed to prevent overwhelming SMTP servers.

## REST API Endpoints

All endpoints require authentication and appropriate permissions.

### Configuration Management
```
POST   /api/email-config                    Create configuration
GET    /api/email-config                    List all configurations
GET    /api/email-config/{id}               Get configuration by ID
GET    /api/email-config/name/{name}        Get configuration by name
GET    /api/email-config/default            Get default configuration
PUT    /api/email-config/{id}               Update configuration
DELETE /api/email-config/{id}               Delete configuration
POST   /api/email-config/{id}/test          Test SMTP connection
```

### Permission Management
```
POST   /api/email-config/{id}/grant-user/{userId}    Grant user access
POST   /api/email-config/{id}/grant-role/{roleId}    Grant role access
DELETE /api/email-config/{id}/revoke-user/{userId}   Revoke user access
```

## Usage Examples

### Send Simple Text Email
```java
emailSender.sendSimpleEmail(
    "customer@example.com",
    "Order Confirmation",
    "Your order has been confirmed!"
);
```

### Send HTML Email
```java
String htmlBody = "<h1>Welcome</h1><p>Thank you for registering!</p>";
emailSender.sendHtmlEmail(
    "customer@example.com",
    "Welcome",
    htmlBody
);
```

### Send with Retry Logic
```java
emailSender.sendEmailWithRetry(
    "customer@example.com",
    "Important Update",
    "This is important",
    3  // Retry 3 times on failure
);
```

### Send from Specific Configuration
```java
emailSender.sendSimpleEmail(
    "customer@example.com",
    "Support Response",
    "Your support ticket has been resolved",
    supportConfigId  // Use specific config
);
```

### Grant Access to Users
```java
emailConfigService.grantUserAccess(
    configId,           // Which config
    userId,             // Which user
    "Allow to send notifications",
    "admin@company.com" // Who granted this
);
```

### Grant Access to Roles
```java
emailConfigService.grantRoleAccess(
    configId,
    bookingManagerRoleId,
    "Booking managers need to send confirmations",
    "admin@company.com"
);
```

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
  "smtpUsername": "your-email@gmail.com",
  "smtpPassword": "your-app-password",
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
  "name": "company",
  "displayName": "Company Mail Server",
  "smtpHost": "mail.company.com",
  "smtpPort": 25,
  "useTls": false,
  "useSsl": false
}
```

## Database Schema

### email_configurations
Stores SMTP server configurations

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT | Primary Key |
| name | VARCHAR(255) | Unique identifier |
| display_name | VARCHAR(255) | Human-readable name |
| from_email | VARCHAR(255) | Sender email address |
| smtp_host | VARCHAR(255) | SMTP server hostname |
| smtp_port | INT | SMTP server port |
| smtp_username | VARCHAR(255) | Authentication username |
| smtp_password | VARCHAR(1000) | Encrypted password |
| use_tls | BOOLEAN | Enable TLS |
| use_ssl | BOOLEAN | Enable SSL |
| enabled | BOOLEAN | Enable/disable config |
| is_default | BOOLEAN | Mark as default |
| emails_sent_count | BIGINT | Statistics |
| emails_failed_count | BIGINT | Statistics |
| created_at | DATETIME | Audit timestamp |
| updated_at | DATETIME | Audit timestamp |

### email_configuration_permissions
Controls access to email configurations

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT | Primary Key |
| email_configuration_id | BIGINT | FK to configuration |
| user_id | BIGINT | FK to user (nullable) |
| role_id | BIGINT | FK to role (nullable) |
| enabled | BOOLEAN | Enable/disable permission |
| expires_at | DATETIME | Optional expiration date |
| created_at | DATETIME | Audit timestamp |

## Security Considerations

1. **Password Encryption** - All SMTP passwords are AES-256 encrypted using `EncryptionUtil`
2. **Encryption Key Management** - See [Encryption Key Setup](#encryption-key-setup) below
3. **Access Control** - Integrated with role/permission system
4. **Permission Checking** - @PreAuthorize annotations on all endpoints
5. **Audit Logging** - All configuration changes are logged
6. **Expiring Permissions** - Permissions can expire automatically
7. **Rate Limiting** - Prevent spam by limiting emails per minute/day

## Encryption Key Setup

The email configuration system uses AES-256 encryption to protect sensitive SMTP passwords. The encryption key must be properly configured:

### Development Setup

For development, the system uses a default key. No additional setup is required, but you'll see a warning in the logs:

```
WARN ... No encryption key found in environment or properties. Using default key for development only.
```

### Production Setup

For production, you **must** set the encryption key via environment variable:

```bash
# Option 1: Set environment variable (RECOMMENDED)
export EMAIL_ENCRYPTION_KEY="your-base64-encoded-32-byte-key"

# Option 2: Set in application.properties (NOT RECOMMENDED for production)
email.encryption.key=your-base64-encoded-32-byte-key
```

### Generate a Secure Encryption Key

The encryption key must be a **Base64-encoded 256-bit (32-byte) key**. Generate one using:

**Option A: Using OpenSSL**
```bash
openssl rand 32 | base64
# Example output: 8JY8VxzKfXyP3mL9N5qR2tWaBcDeFgHiJkLmNoPqRsT=
```

**Option B: Using Java**
```java
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Encryption Key: " + encoded);
    }
}
```

**Option C: Using an online Base64 encoder**
- Generate 32 random bytes
- Encode as Base64

### Key Resolution Order

The system loads the encryption key in this order:

1. Environment variable: `EMAIL_ENCRYPTION_KEY`
2. Application property: `email.encryption.key` (from application.properties)
3. Legacy environment variable: `ENCRYPTION_KEY`
4. Default key (development only - will show warning)

### Docker/Kubernetes Setup

When running in containers, set the environment variable:

```dockerfile
ENV EMAIL_ENCRYPTION_KEY="your-key-here"
```

Or in Kubernetes:

```yaml
env:
  - name: EMAIL_ENCRYPTION_KEY
    valueFrom:
      secretKeyRef:
        name: email-secrets
        key: encryption-key
```

### Important Security Notes

- Never commit encryption keys to version control
- Use different keys for different environments (dev, staging, prod)
- Store keys in a secrets vault (HashiCorp Vault, AWS Secrets Manager, etc.)
- Rotate keys periodically
- If a key is compromised, re-encrypt all stored passwords with a new key

## Troubleshooting

### Connection Test Fails
1. Verify SMTP host and port are correct
2. Verify username and password are correct
3. Check firewall allows outbound SMTP connection
4. Try connecting from command line: `telnet smtp.gmail.com 587`

### Email Not Sending
1. Verify configuration is enabled
2. Verify user/role has permission to use configuration
3. Check application logs for error messages
4. Verify recipient email address is valid
5. Check SMTP server isn't rejecting the email

### Permission Denied
1. Verify user has appropriate permission or ADMIN role
2. Verify role has appropriate permission or ADMIN role
3. Check permission expiration date hasn't passed

### Database Errors
1. Verify JPA entities are being scanned
2. Ensure database tables exist (check ddl-auto setting)
3. Verify database user has CREATE/ALTER table permissions

## Performance Tips

1. **Mail Sender Caching** - JavaMailSender instances are cached by configId
2. **Batch Emails** - Use sendBatchEmail() for sending to multiple recipients
3. **Async Sending** - Consider marking email operations as @Async
4. **Test Before Prod** - Always test SMTP connection before enabling

## Dependencies

This module requires:
- Spring Boot 3.5.7+
- Spring Mail starter
- Spring Security (for access control)
- Spring Data JPA (for database access)
- Lombok (for entity annotations)

All dependencies are already included in your project's pom.xml.

## Next Steps

1. Read [EMAIL_CONFIGURATION_GUIDE.md](EMAIL_CONFIGURATION_GUIDE.md) for complete guide
2. Check [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for API reference
3. Create your first email configuration
4. Test SMTP connection
5. Grant access to users/roles
6. Integrate with your service classes
7. Send your first email!

## Support

For issues or questions:
1. Check the comprehensive guide: EMAIL_CONFIGURATION_GUIDE.md
2. Review quick reference: QUICK_REFERENCE.md
3. Check application logs for error messages
4. Review the troubleshooting section in the guides

---

**Version:** 1.0.0
**Created:** 2025-11-19
**Last Updated:** 2025-11-19
**Status:** Ready for Production

The email configuration system is complete and ready to use. All entities, services, repositories, controllers, and documentation have been created following Spring Boot and your application's architectural patterns.
