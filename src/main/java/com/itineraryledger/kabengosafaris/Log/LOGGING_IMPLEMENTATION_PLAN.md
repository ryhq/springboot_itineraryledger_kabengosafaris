# Enhanced Logging System Implementation Plan

## Overview

This document outlines the implementation of an advanced logging system for Kabengo Safaris, building upon the ItineraryLedger implementation with significant enhancements.

## Key Enhancements Over Base Implementation

### 1. Security Analysis Features
- **SQL Injection Detection**: Pattern matching for SQL injection attempts
- **XSS Attack Detection**: Detect cross-site scripting patterns
- **Brute Force Detection**: Track failed login attempts from same IP
- **Suspicious Patterns**: Detect path traversal, command injection attempts
- **IP Blacklisting**: Auto-blacklist IPs with repeated malicious patterns

### 2. Performance Monitoring
- **Slow Request Tracking**: Flag requests taking longer than threshold
- **Memory Usage Tracking**: Monitor per-request memory consumption
- **Database Query Tracking**: Track number of queries per request
- **Response Size Alerts**: Flag unusually large responses

### 3. Bot Detection & Classification
- **User Agent Analysis**: Identify known bot signatures
- **Behavioral Analysis**: Detect bot-like request patterns
- **Bot Classification**: Categorize bots (good/bad/crawler/scraper)
- **Rate Pattern Analysis**: Detect automated request patterns

### 4. Geographic IP Analysis
- **IP Geolocation**: Extract country, city, region from IP
- **ISP Information**: Identify internet service provider
- **Timezone Detection**: Determine user timezone
- **VPN/Proxy Detection**: Flag requests from known VPN/proxy services

### 5. Advanced Analytics
- **Endpoint Usage Statistics**: Most/least used endpoints
- **Error Rate Tracking**: Track error rates by endpoint, time, user
- **User Session Analytics**: Session duration, actions per session
- **API Response Time Analysis**: Average, median, 95th percentile
- **Traffic Pattern Analysis**: Peak hours, daily/weekly patterns
- **User Agent Distribution**: Browser/OS/device statistics

### 6. Alert System
- **Configurable Alerts**: Email/webhook notifications
- **Alert Types**: Error spikes, slow responses, security threats
- **Alert Thresholds**: Customizable thresholds for each alert type
- **Alert Aggregation**: Group similar alerts to reduce noise
- **Alert History**: Track all fired alerts

### 7. Request/Response Body Logging
- **Selective Body Logging**: Log bodies for specific endpoints
- **Sensitive Data Redaction**: Auto-redact passwords, tokens, credit cards
- **Size Limits**: Truncate large bodies
- **Structured Storage**: Store as JSON for easy querying

### 8. Advanced Export Capabilities
- **Multiple Formats**: TXT, CSV, JSON, Excel, PDF
- **Filtered Exports**: Export with applied filters
- **Scheduled Exports**: Auto-export daily/weekly reports
- **Export Templates**: Predefined export formats for common use cases

### 9. Real-Time Dashboard
- **Live Metrics**: Real-time request rate, error rate, response time
- **Active Users**: Current active sessions
- **Geographic Map**: Visualize request origins
- **Top Endpoints**: Most accessed endpoints in real-time
- **Error Stream**: Live stream of errors as they occur

### 10. Log Retention & Archival
- **Automatic Archival**: Archive logs older than X days
- **Compression**: Compress archived logs to save space
- **Retention Policies**: Different retention periods for different log types
- **Cleanup Jobs**: Scheduled jobs to delete very old logs

## File Structure

```
/Log
├── Controller
│   ├── LogController.java              # Main REST controller
│   ├── LogSSEController.java           # Real-time log streaming via SSE
│   ├── LogAnalyticsController.java     # Analytics and metrics endpoints
│   └── LogSecurityController.java      # Security analysis endpoints
├── DTOs
│   ├── AccessLogDTO.java               # Enhanced access log DTO
│   ├── SecurityAnalysisDTO.java        # Security threat analysis
│   ├── PerformanceMetricsDTO.java      # Performance metrics
│   ├── BotDetectionDTO.java            # Bot detection results
│   ├── GeoLocationDTO.java             # Geographic information
│   ├── EndpointAnalyticsDTO.java       # Endpoint usage statistics
│   ├── HourlyMetricsDTO.java           # Hourly aggregated metrics
│   ├── AlertConfigDTO.java             # Alert configuration
│   ├── ExportRequestDTO.java           # Export request parameters
│   └── LogFilterDTO.java               # Filter criteria
├── Services
│   ├── AccessLogParserService.java     # Parse Tomcat access logs
│   ├── AccessLogService.java           # Core log retrieval and filtering
│   ├── SecurityAnalysisService.java    # Security threat detection
│   ├── PerformanceMonitorService.java  # Performance tracking
│   ├── BotDetectionService.java        # Bot identification
│   ├── GeoLocationService.java         # IP geolocation
│   ├── LogAnalyticsService.java        # Analytics and statistics
│   ├── LogExportService.java           # Export functionality
│   ├── AlertService.java               # Alert system
│   ├── LogRetentionService.java        # Archive and cleanup
│   └── LogSSEService.java              # SSE broadcast service
├── Enums
│   ├── ThreatType.java                 # Security threat types
│   ├── BotType.java                    # Bot classifications
│   ├── AlertSeverity.java              # Alert severity levels
│   └── ExportFormat.java               # Export format types
└── Utils
    ├── IPUtils.java                    # IP manipulation utilities
    ├── PatternDetector.java            # Pattern matching for threats
    ├── GeoIPDatabase.java              # GeoIP database wrapper
    └── UserAgentParser.java            # User agent parsing
```

## Database Tables (Optional - for persistent storage)

### `access_logs` table
```sql
CREATE TABLE access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    remote_address VARCHAR(45),
    local_address VARCHAR(45),
    local_port INT,
    remote_host VARCHAR(255),
    timestamp DATETIME,
    request_method VARCHAR(10),
    request_uri TEXT,
    request_protocol VARCHAR(20),
    status INT,
    response_size_bytes BIGINT,
    time_taken_micros BIGINT,
    user_agent TEXT,
    referer TEXT,
    x_forwarded_for VARCHAR(255),
    cookie TEXT,
    host VARCHAR(255),
    ssl_session_id VARCHAR(255),
    request_thread_name VARCHAR(255),

    -- Security Analysis
    is_suspicious BOOLEAN DEFAULT FALSE,
    threat_type VARCHAR(50),
    threat_score INT,

    -- Performance
    is_slow_request BOOLEAN DEFAULT FALSE,

    -- Bot Detection
    is_bot BOOLEAN DEFAULT FALSE,
    bot_type VARCHAR(50),

    -- Geographic
    country_code VARCHAR(2),
    country_name VARCHAR(100),
    city VARCHAR(100),
    region VARCHAR(100),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_timestamp (timestamp),
    INDEX idx_remote_address (remote_address),
    INDEX idx_status (status),
    INDEX idx_suspicious (is_suspicious),
    INDEX idx_slow (is_slow_request),
    INDEX idx_bot (is_bot)
);
```

### `log_alerts` table
```sql
CREATE TABLE log_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(50),
    severity VARCHAR(20),
    title VARCHAR(255),
    description TEXT,
    triggered_at DATETIME,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by_id BIGINT,
    acknowledged_at DATETIME,
    metadata JSON,

    INDEX idx_triggered_at (triggered_at),
    INDEX idx_acknowledged (acknowledged)
);
```

### `endpoint_statistics` table
```sql
CREATE TABLE endpoint_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint VARCHAR(255),
    method VARCHAR(10),
    date DATE,
    hour TINYINT,
    request_count INT DEFAULT 0,
    error_count INT DEFAULT 0,
    total_response_time_micros BIGINT DEFAULT 0,
    avg_response_time_micros BIGINT DEFAULT 0,
    max_response_time_micros BIGINT DEFAULT 0,
    min_response_time_micros BIGINT DEFAULT 0,

    UNIQUE INDEX idx_endpoint_date_hour (endpoint, method, date, hour)
);
```

## Configuration (application.properties)

```properties
# -----------------------------------------
# Tomcat Access Log Configuration
# -----------------------------------------
# Enable Tomcat access logging
server.tomcat.accesslog.enabled=true

# Access log pattern (comprehensive format)
server.tomcat.accesslog.pattern=%a %A %p %h %l %u %t "%r" %s %b %D "%{User-Agent}i" "%{Referer}i" "%{X-Forwarded-For}i" "%{Cookie}i" "%{Host}i" %S %I

# Access log directory
server.tomcat.accesslog.directory=logs/access

# Access log file prefix
server.tomcat.accesslog.prefix=access_log

# Access log file suffix
server.tomcat.accesslog.suffix=.log

# Access log buffering
server.tomcat.accesslog.buffered=true

# Rotate logs daily
server.tomcat.accesslog.rotate=true

# Date format in filename
server.tomcat.accesslog.file-date-format=.yyyy-MM-dd

# -----------------------------------------
# Application Logging Configuration
# -----------------------------------------
# Application log file
logging.file.name=logs/application.log

# Log file pattern
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Console log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# -----------------------------------------
# Enhanced Logging Features Configuration
# -----------------------------------------
# Enable security analysis
log.security.analysis.enabled=true

# Security threat score threshold (0-100)
log.security.threat.threshold=50

# Enable automatic IP blacklisting
log.security.auto.blacklist=false

# Blacklist threshold (number of threats before blacklist)
log.security.blacklist.threshold=10

# Enable bot detection
log.bot.detection.enabled=true

# Enable performance monitoring
log.performance.monitoring.enabled=true

# Slow request threshold (in milliseconds)
log.performance.slow.threshold=5000

# Enable geolocation
log.geo.location.enabled=true

# GeoIP database path (MaxMind GeoLite2)
log.geo.database.path=/opt/geoip/GeoLite2-City.mmdb

# Enable request/response body logging
log.body.logging.enabled=false

# Endpoints to log bodies for (comma-separated)
log.body.endpoints=/api/auth/login,/api/auth/register

# Maximum body size to log (in bytes)
log.body.max.size=10240

# Enable analytics
log.analytics.enabled=true

# Analytics aggregation interval (in minutes)
log.analytics.interval=60

# Enable alerts
log.alerts.enabled=true

# Alert email recipients (comma-separated)
log.alerts.email.recipients=admin@kabengosafaris.com

# Alert webhook URL
log.alerts.webhook.url=

# Log retention (in days)
log.retention.days=90

# Enable automatic archival
log.retention.archive.enabled=true

# Archive path
log.retention.archive.path=logs/archive

# Enable automatic cleanup
log.retention.cleanup.enabled=true

# Cleanup schedule (cron expression)
log.retention.cleanup.cron=0 0 2 * * ?
```

## API Endpoints

### Log Management
- `GET /api/logs` - Get paginated logs with filtering
- `GET /api/logs/structured` - Get structured log data
- `GET /api/logs/summary` - Get summary statistics
- `GET /api/logs/{id}` - Get single log entry
- `GET /api/logs/download` - Download raw log file
- `GET /api/logs/export` - Export logs (TXT/CSV/JSON/Excel/PDF)
- `GET /api/logs/sse/stream` - Real-time log stream (SSE)

### Security Analysis
- `GET /api/logs/security/threats` - Get detected threats
- `GET /api/logs/security/ips` - Get suspicious IPs
- `GET /api/logs/security/blacklist` - Get blacklisted IPs
- `POST /api/logs/security/blacklist` - Add IP to blacklist
- `DELETE /api/logs/security/blacklist/{ip}` - Remove IP from blacklist

### Performance Monitoring
- `GET /api/logs/performance/slow-requests` - Get slow requests
- `GET /api/logs/performance/metrics` - Get performance metrics
- `GET /api/logs/performance/endpoints` - Get endpoint performance stats

### Bot Detection
- `GET /api/logs/bots` - Get bot traffic
- `GET /api/logs/bots/classification` - Get bot classification statistics

### Analytics
- `GET /api/logs/analytics/overview` - Dashboard overview metrics
- `GET /api/logs/analytics/endpoints` - Endpoint usage statistics
- `GET /api/logs/analytics/hourly` - Hourly traffic patterns
- `GET /api/logs/analytics/geographic` - Geographic distribution
- `GET /api/logs/analytics/user-agents` - User agent statistics
- `GET /api/logs/analytics/errors` - Error rate analysis

### Alerts
- `GET /api/logs/alerts` - Get all alerts
- `GET /api/logs/alerts/{id}` - Get single alert
- `POST /api/logs/alerts/{id}/acknowledge` - Acknowledge alert
- `GET /api/logs/alerts/config` - Get alert configuration
- `PUT /api/logs/alerts/config` - Update alert configuration

## Implementation Phases

### Phase 1: Core Infrastructure (Week 1)
1. Create directory structure
2. Implement enhanced DTOs
3. Implement AccessLogParserService
4. Implement AccessLogService with basic filtering
5. Create LogController with basic endpoints
6. Configure Tomcat access logging

### Phase 2: Security Features (Week 2)
1. Implement PatternDetector utility
2. Implement SecurityAnalysisService
3. Create LogSecurityController
4. Add security analysis endpoints
5. Implement IP blacklisting

### Phase 3: Performance & Bot Detection (Week 3)
1. Implement PerformanceMonitorService
2. Implement UserAgentParser utility
3. Implement BotDetectionService
4. Add performance and bot endpoints

### Phase 4: Geographic & Analytics (Week 4)
1. Integrate GeoIP database
2. Implement GeoLocationService
3. Implement LogAnalyticsService
4. Create LogAnalyticsController
5. Add analytics endpoints

### Phase 5: Real-Time & Exports (Week 5)
1. Implement LogSSEService
2. Implement LogSSEController
3. Implement LogExportService (TXT, CSV, JSON)
4. Add Excel export
5. Add PDF export

### Phase 6: Alerts & Retention (Week 6)
1. Implement AlertService
2. Add email alert integration
3. Add webhook alert integration
4. Implement LogRetentionService
5. Add scheduled cleanup jobs

### Phase 7: Testing & Documentation (Week 7)
1. Unit tests for all services
2. Integration tests for controllers
3. Performance testing
4. API documentation
5. User guide

## Dependencies

Add to `pom.xml`:

```xml
<!-- GeoIP -->
<dependency>
    <groupId>com.maxmind.geoip2</groupId>
    <artifactId>geoip2</artifactId>
    <version>4.1.0</version>
</dependency>

<!-- User Agent Parser -->
<dependency>
    <groupId>eu.bitwalker</groupId>
    <artifactId>UserAgentUtils</artifactId>
    <version>1.21</version>
</dependency>

<!-- Apache POI (Excel export) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- iText (PDF export) -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>8.0.2</version>
    <type>pom</type>
</dependency>

<!-- SSE Support (included in Spring Boot) -->
<!-- No additional dependency needed -->

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## Security Considerations

1. **Access Control**: All log endpoints require ADMIN or SUPERUSER role
2. **Data Sanitization**: Sanitize user-provided filter values to prevent injection
3. **Rate Limiting**: Apply rate limiting to log endpoints
4. **Data Privacy**: Implement GDPR-compliant data retention and deletion
5. **Sensitive Data**: Redact passwords, tokens, API keys from logs
6. **IP Privacy**: Option to anonymize IP addresses (last octet)

## Performance Optimizations

1. **File Streaming**: Stream large log files instead of loading into memory
2. **Caching**: Cache frequently accessed statistics
3. **Async Processing**: Process analytics and security analysis asynchronously
4. **Batch Operations**: Batch database inserts for persistent storage
5. **Index Optimization**: Proper indexes on timestamp, IP, status columns
6. **Compression**: Compress old logs automatically

## Monitoring & Alerting Examples

### Alert: High Error Rate
- **Trigger**: Error rate > 10% in last 5 minutes
- **Action**: Send email + webhook notification
- **Severity**: HIGH

### Alert: Slow API Response
- **Trigger**: Average response time > 3 seconds in last 10 minutes
- **Action**: Send email notification
- **Severity**: MEDIUM

### Alert: Security Threat Detected
- **Trigger**: SQL injection pattern detected
- **Action**: Send immediate email + webhook, auto-blacklist IP
- **Severity**: CRITICAL

### Alert: Unusual Traffic Spike
- **Trigger**: Request rate increases by 300% in 5 minutes
- **Action**: Send email notification
- **Severity**: MEDIUM

## Dashboard Metrics

### Real-Time Metrics
- Requests per second
- Average response time
- Error rate (%)
- Active sessions
- Top 5 endpoints
- Recent errors

### Historical Metrics
- Daily/weekly traffic trends
- Response time trends
- Error rate trends
- Geographic distribution map
- Browser/OS distribution
- Bot vs human traffic ratio

## Next Steps

1. Review and approve this implementation plan
2. Set up development environment
3. Create feature branch: `feature/enhanced-logging`
4. Begin Phase 1 implementation
5. Regular progress reviews after each phase

---

**Document Version**: 1.0
**Last Updated**: 2026-02-09
**Author**: Claude Code
**Status**: Pending Approval
