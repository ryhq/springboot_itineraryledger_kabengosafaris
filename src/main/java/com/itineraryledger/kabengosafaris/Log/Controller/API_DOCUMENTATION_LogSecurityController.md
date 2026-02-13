# Log Security Controller API Documentation

## Overview

The Log Security Controller provides specialized endpoints for security analysis of access logs, including threat detection, suspicious IP tracking, and security summary statistics.

## Base URL

```
/api/logs/security
```

## Authentication

All endpoints require authentication with a valid JWT token and the `PERM_READ_LOG` permission.

### Headers Required

```
Authorization: Bearer <jwt-token>
```

## Response Format

All endpoints return responses wrapped in a standardized `ApiResponse` format:

### Success Response Structure

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation description",
  "data": {
    // Actual response data here
  },
  "timestamp": "2026-02-10T14:23:45.123"
}
```

### Error Response Structure

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2026-02-10T14:23:45.123"
}
```

**Note:** Response examples below show the complete ApiResponse wrapper for the first example, then only the `data` field content for brevity.

---

## Endpoints

### 1. Get Security Threats

Retrieve detected security threats from access logs with filtering options.

**Endpoint:** `GET /api/logs/security/threats`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to retrieve threats for (format: yyyy-MM-dd) |
| `threatType` | String | No | All | Filter by threat type |
| `minThreatScore` | Integer | No | 0 | Minimum threat score (0-100) |

#### Threat Types

| Threat Type | Description |
|-------------|-------------|
| `SQL_INJECTION` | SQL injection attempt detected in query parameters or body |
| `XSS` | Cross-site scripting attempt detected |
| `PATH_TRAVERSAL` | Path traversal attempt (../, etc.) |
| `COMMAND_INJECTION` | Command injection attempt detected |
| `SUSPICIOUS_USER_AGENT` | Suspicious or malicious user agent string |
| `SCANNING` | Port scanning or vulnerability scanning detected |

#### Response

```json
{
  "threats": [
    {
      "logId": "encoded_id",
      "remoteAddress": "192.168.1.100",
      "timestamp": "09/Feb/2026:14:30:45 +0300",
      "requestMethod": "GET",
      "requestUri": "/api/users?id=1' OR '1'='1",
      "status": 400,
      "isSuspicious": true,
      "threatType": "SQL_INJECTION",
      "threatScore": 85,
      "userAgent": "sqlmap/1.0",
      "fullLog": "..."
    }
  ],
  "count": 1
}
```

**Empty Response (No Threats):**
```json
{
  "threats": [],
  "count": 0,
  "message": "No security threats detected"
}
```

#### Examples

**Get all threats for today:**
```bash
GET /api/logs/security/threats
```

**Get threats for specific date:**
```bash
GET /api/logs/security/threats?date=2026-02-09
```

**Filter by threat type:**
```bash
GET /api/logs/security/threats?threatType=SQL_INJECTION
```

**Get high-severity threats:**
```bash
GET /api/logs/security/threats?minThreatScore=70
```

**Combine filters:**
```bash
GET /api/logs/security/threats?date=2026-02-09&threatType=XSS&minThreatScore=50
```

---

### 2. Get Suspicious IP Addresses

Retrieve suspicious IP addresses grouped by threat count and severity.

**Endpoint:** `GET /api/logs/security/ips`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to analyze (format: yyyy-MM-dd) |
| `minThreatScore` | Integer | No | 50 | Minimum threat score to include |

#### Response

```json
{
  "suspiciousIPs": {
    "192.168.1.100": {
      "threatCount": 15,
      "maxThreatScore": 85,
      "avgThreatScore": 67.5,
      "threatTypes": ["SQL_INJECTION", "XSS"],
      "threats": [
        {
          "logId": "encoded_id",
          "timestamp": "09/Feb/2026:14:30:45 +0300",
          "requestUri": "/api/users?id=1' OR '1'='1",
          "threatType": "SQL_INJECTION",
          "threatScore": 85
        }
      ]
    },
    "10.0.0.50": {
      "threatCount": 8,
      "maxThreatScore": 72,
      "avgThreatScore": 58.3,
      "threatTypes": ["PATH_TRAVERSAL"],
      "threats": [...]
    }
  },
  "totalIPs": 2
}
```

**Empty Response (No Suspicious IPs):**
```json
{
  "suspiciousIPs": {},
  "totalIPs": 0,
  "message": "No suspicious IPs detected"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `threatCount` | Integer | Total number of threats from this IP |
| `maxThreatScore` | Integer | Highest threat score from this IP |
| `avgThreatScore` | Double | Average threat score across all threats |
| `threatTypes` | Array | Unique threat types detected from this IP |
| `threats` | Array | List of all threat events from this IP |

IPs are sorted by threat count (highest first).

#### Examples

**Get all suspicious IPs (default threshold 50):**
```bash
GET /api/logs/security/ips
```

**Get IPs with high-severity threats:**
```bash
GET /api/logs/security/ips?minThreatScore=80
```

**Analyze specific date:**
```bash
GET /api/logs/security/ips?date=2026-02-09&minThreatScore=60
```

---

### 3. Get Security Summary

Retrieve comprehensive security summary statistics for a given day.

**Endpoint:** `GET /api/logs/security/summary`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to summarize (format: yyyy-MM-dd) |

#### Response

```json
{
  "totalThreats": 45,
  "totalRequests": 10000,
  "threatPercentage": 0.45,
  "uniqueSuspiciousIPs": 8,

  "threatsByType": {
    "SQL_INJECTION": 18,
    "XSS": 12,
    "PATH_TRAVERSAL": 8,
    "COMMAND_INJECTION": 5,
    "SUSPICIOUS_USER_AGENT": 2
  },

  "averageThreatScore": 62.5,
  "maxThreatScore": 95,

  "severityDistribution": {
    "CRITICAL (80-100)": 12,
    "HIGH (50-79)": 23,
    "MEDIUM (30-49)": 8,
    "LOW (0-29)": 2
  },

  "topSuspiciousIPs": [
    {
      "ip": "192.168.1.100",
      "threatCount": 15
    },
    {
      "ip": "10.0.0.50",
      "threatCount": 12
    },
    {
      "ip": "172.16.0.25",
      "threatCount": 8
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `totalThreats` | Integer | Total number of detected threats |
| `totalRequests` | Integer | Total number of requests analyzed |
| `threatPercentage` | Double | Percentage of requests that were threats |
| `uniqueSuspiciousIPs` | Integer | Number of unique IPs generating threats |
| `threatsByType` | Object | Count of threats grouped by type |
| `averageThreatScore` | Double | Average threat score across all threats |
| `maxThreatScore` | Integer | Highest threat score detected |
| `severityDistribution` | Object | Count of threats by severity level |
| `topSuspiciousIPs` | Array | Top 10 most active malicious IPs |

#### Severity Levels

| Severity | Score Range | Description |
|----------|-------------|-------------|
| **CRITICAL** | 80-100 | Severe threats requiring immediate action |
| **HIGH** | 50-79 | Significant threats requiring attention |
| **MEDIUM** | 30-49 | Moderate threats to monitor |
| **LOW** | 0-29 | Minor suspicious activity |

#### Examples

**Get today's security summary:**
```bash
GET /api/logs/security/summary
```

**Get summary for specific date:**
```bash
GET /api/logs/security/summary?date=2026-02-09
```

---

## Threat Scoring Algorithm

Threat scores are calculated based on multiple factors:

### Score Components

| Factor | Weight | Description |
|--------|--------|-------------|
| Attack Pattern Severity | 40% | How dangerous the detected pattern is |
| Request Complexity | 20% | Number and severity of suspicious elements |
| Source Reputation | 15% | Historical behavior of the source IP |
| User Agent Suspiciousness | 15% | Known malicious tools/scanners |
| Frequency | 10% | Repeated attacks increase score |

### Score Interpretation

| Score | Level | Action Recommended |
|-------|-------|-------------------|
| **90-100** | Critical | Block IP immediately, investigate source |
| **70-89** | High | Monitor closely, consider rate limiting |
| **50-69** | Moderate | Log and monitor, apply basic protections |
| **30-49** | Low | Log for analysis, no immediate action |
| **0-29** | Minimal | False positive or benign anomaly |

---

## Error Responses

### 400 Bad Request

Invalid parameters provided.

```json
{
  "error": "Future dates are not allowed",
  "status": 400
}
```

```json
{
  "error": "Invalid threat score: must be 0-100",
  "status": 400
}
```

### 401 Unauthorized

Missing or invalid authentication token.

```json
{
  "error": "Unauthorized",
  "status": 401
}
```

### 403 Forbidden

User lacks required permissions.

```json
{
  "error": "Access denied - READ_LOG permission required",
  "status": 403
}
```

### 404 Not Found

Log file not found for specified date.

```json
{
  "error": "Logs for 2026-02-09 not found",
  "status": 404
}
```

### 500 Internal Server Error

Server error while processing request.

```json
{
  "error": "Error reading log file: <error message>",
  "status": 500
}
```

---

## Security Features

### 1. Threat Detection

The system automatically detects:

- **SQL Injection:** Common SQL injection patterns in URLs and request bodies
- **XSS Attacks:** Script injection attempts in parameters
- **Path Traversal:** Directory traversal attempts (../, /etc/passwd, etc.)
- **Command Injection:** Shell command injection patterns
- **Suspicious User Agents:** Known attack tools (sqlmap, nikto, etc.)
- **Scanning Activity:** Port scans, vulnerability scanners

### 2. Pattern Matching

Uses advanced regex patterns and heuristics:
- Common attack signatures database
- Encoded payload detection (base64, URL encoding)
- Multi-layer obfuscation detection
- Context-aware analysis

### 3. False Positive Reduction

- Whitelist known legitimate patterns
- Context-aware scoring (POST vs GET)
- User agent reputation scoring
- Historical IP behavior analysis

---

## Best Practices

1. **Monitor Critical Threats Daily**
   ```bash
   GET /api/logs/security/threats?minThreatScore=80
   ```

2. **Review Suspicious IPs Weekly**
   ```bash
   GET /api/logs/security/ips?minThreatScore=70
   ```

3. **Track Trends with Summary**
   ```bash
   GET /api/logs/security/summary?date=<yesterday>
   ```

4. **Investigate Specific Threat Types**
   ```bash
   GET /api/logs/security/threats?threatType=SQL_INJECTION
   ```

5. **Combine with Main Log Endpoint** for detailed analysis:
   ```bash
   GET /api/logs?isSuspicious=true&minThreatScore=70
   ```

---

## Integration Examples

### Alert on Critical Threats

```bash
#!/bin/bash
# Check for critical threats and send alert

RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "https://api.kabengosafaris.com/api/logs/security/threats?minThreatScore=80")

COUNT=$(echo $RESPONSE | jq '.count')

if [ $COUNT -gt 0 ]; then
  echo "ALERT: $COUNT critical threats detected!"
  # Send notification (email, Slack, etc.)
fi
```

### Daily Security Report

```bash
#!/bin/bash
# Generate daily security summary

DATE=$(date -d "yesterday" +%Y-%m-%d)
SUMMARY=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "https://api.kabengosafaris.com/api/logs/security/summary?date=$DATE")

echo "Security Summary for $DATE:"
echo $SUMMARY | jq '{totalThreats, uniqueSuspiciousIPs, threatsByType}'
```

---

## Notes

- Threat detection runs automatically on all access logs
- Scores are calculated in real-time during log parsing
- Threat data is not persisted; analysis is performed on-the-fly from log files
- For historical analysis, ensure log files are retained
- Geographic IP data enhances threat scoring if GeoIP is configured

---

**Last Updated:** 2026-02-09
**API Version:** 1.0
**Controller:** LogSecurityController.java
