# Log Controller API Documentation

## Overview

The Log Controller provides access to Tomcat access logs with advanced filtering, pagination, and export capabilities. It supports comprehensive security analysis, bot detection, performance monitoring, and multiple export formats.

## Base URL

```
/api/logs
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

### 1. Get Access Logs (Paginated)

Retrieve access logs with comprehensive filtering and pagination.

**Endpoint:** `GET /api/logs`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 0 | Page number (0-indexed) |
| `size` | Integer | No | 20 | Page size (number of records) |
| `date` | Date (ISO) | No | Today | Date to retrieve logs for (format: yyyy-MM-dd) |

#### Basic Filters

| Parameter | Type | Description |
|-----------|------|-------------|
| `remoteAddress` | String | Filter by client IP address |
| `localAddress` | String | Filter by server IP address |
| `localPort` | Integer | Filter by server port |
| `remoteHost` | String | Filter by remote hostname |
| `requestMethod` | String | HTTP method (GET, POST, PUT, DELETE, etc.) |
| `requestUri` | String | Request URI (partial match supported) |
| `status` | Integer | HTTP status code (200, 404, 500, etc.) |
| `statusCategory` | String | Status category (2xx, 3xx, 4xx, 5xx) |
| `userAgent` | String | User agent string (partial match) |
| `referer` | String | Referer URL (partial match) |
| `xForwardedFor` | String | X-Forwarded-For header value |
| `host` | String | Host header value |

#### Performance Filters

| Parameter | Type | Description |
|-----------|------|-------------|
| `responseSizeBytes` | Long | Response size in bytes |
| `responseSizeBytesArgument` | String | Comparison operator: `equality`, `inequality`, `greaterthan`, `lessthan`, `greaterthanorequalto`, `lessthanorequalto` |
| `timeTakenMillis` | Long | Time taken in milliseconds |
| `timeTakenMillisArgument` | String | Comparison operator (same as above) |
| `isSlowRequest` | Boolean | Filter slow requests (true/false) |
| `performanceGrade` | String | Performance grade (A, B, C, D, F) |

#### Security Filters

| Parameter | Type | Description |
|-----------|------|-------------|
| `isSuspicious` | Boolean | Filter suspicious/malicious requests |
| `threatType` | String | Threat type (SQL_INJECTION, XSS, PATH_TRAVERSAL, COMMAND_INJECTION, etc.) |
| `minThreatScore` | Integer | Minimum threat score (0-100) |

#### Bot Detection Filters

| Parameter | Type | Description |
|-----------|------|-------------|
| `isBot` | Boolean | Filter bot traffic (true/false) |
| `botType` | String | Bot type (SEARCH_ENGINE, SCRAPER, MONITORING, etc.) |

#### User Agent Filters

| Parameter | Type | Description |
|-----------|------|-------------|
| `browserName` | String | Browser name (Chrome, Firefox, Safari, etc.) |
| `operatingSystem` | String | Operating system (Windows, macOS, Linux, Android, iOS) |
| `deviceType` | String | Device type (DESKTOP, MOBILE, TABLET) |

#### Response

```json
{
  "logs": [
    {
      "logId": "encoded_id",
      "remoteAddress": "192.168.1.100",
      "localAddress": "10.0.0.1",
      "localPort": 8080,
      "remoteHost": "client.example.com",
      "timestamp": "09/Feb/2026:14:30:45 +0300",
      "timestampEpoch": 1707484245000,
      "requestLine": "GET /api/parks HTTP/1.1",
      "requestMethod": "GET",
      "requestUri": "/api/parks",
      "requestProtocol": "HTTP/1.1",
      "status": 200,
      "statusCategory": "2xx",
      "isSuccess": true,
      "isClientError": false,
      "isServerError": false,
      "responseSizeBytes": 15420,
      "responseSizeFormatted": "15.06 KB",
      "timeTakenMicros": 245000,
      "timeTakenMillis": 245,
      "timeTakenFormatted": "245ms",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0",
      "referer": "https://example.com",
      "xForwardedFor": null,
      "cookie": "session=abc123",
      "host": "api.kabengosafaris.com",
      "sslSessionId": "ssl_session_id",
      "requestThreadName": "http-nio-8080-exec-1",

      "isSuspicious": false,
      "threatType": null,
      "threatScore": null,

      "isBot": false,
      "botType": null,
      "botName": null,

      "isSlowRequest": false,
      "performanceGrade": "A",

      "browserName": "Chrome",
      "browserVersion": "120.0",
      "operatingSystem": "Windows",
      "operatingSystemVersion": "10",
      "deviceType": "DESKTOP",

      "countryCode": null,
      "countryName": null,
      "city": null,
      "region": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalPages": 5,
  "totalElements": 100,
  "summary": {
    "totalRequests": 100,
    "successRate": 95.5,
    "errorRate": 4.5,
    "avgResponseTime": 187,
    "suspiciousCount": 2,
    "botCount": 15
  }
}
```

#### Examples

**Basic retrieval (first page):**
```bash
GET /api/logs?page=0&size=20
```

**Filter by date:**
```bash
GET /api/logs?date=2026-02-09
```

**Filter by status code:**
```bash
GET /api/logs?status=404
```

**Filter by HTTP method and URI:**
```bash
GET /api/logs?requestMethod=POST&requestUri=/api/auth/login
```

**Find slow requests:**
```bash
GET /api/logs?isSlowRequest=true
```

**Find requests larger than 1MB:**
```bash
GET /api/logs?responseSizeBytes=1048576&responseSizeBytesArgument=greaterthan
```

**Find requests taking more than 1 second:**
```bash
GET /api/logs?timeTakenMillis=1000&timeTakenMillisArgument=greaterthan
```

**Find security threats:**
```bash
GET /api/logs?isSuspicious=true&minThreatScore=50
```

**Find SQL injection attempts:**
```bash
GET /api/logs?threatType=SQL_INJECTION
```

**Filter bot traffic:**
```bash
GET /api/logs?isBot=true&botType=SEARCH_ENGINE
```

**Find requests from specific browser:**
```bash
GET /api/logs?browserName=Chrome&deviceType=MOBILE
```

**Complex filtering:**
```bash
GET /api/logs?date=2026-02-09&status=500&requestMethod=POST&isSlowRequest=true
```

---

### 2. Export Access Logs

Export access logs in various formats with the same filtering options as the main endpoint.

**Endpoint:** `GET /api/logs/export`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

All the same filter parameters as the GET `/api/logs` endpoint, plus:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `format` | String | **Yes** | Export format: `TEXT`, `CSV`, `JSON`, `EXCEL` |

All other parameters are the same as the main GET endpoint.

#### Response

File download with appropriate Content-Type and filename:

| Format | Content-Type | Filename Pattern |
|--------|-------------|------------------|
| TEXT | text/plain | `access_logs_2026-02-09.txt` |
| CSV | text/csv | `access_logs_2026-02-09.csv` |
| JSON | application/json | `access_logs_2026-02-09.json` |
| EXCEL | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet | `access_logs_2026-02-09.xlsx` |

#### Format Details

**TEXT Format:**
- Raw log lines exactly as they appear in the access log file
- One line per request
- Best for importing into other log analysis tools

**CSV Format:**
- Comma-separated values with headers
- All fields included
- Excel-compatible
- Best for data analysis and spreadsheets

**JSON Format:**
- JSON array of log entry objects
- All enriched data included (security, performance, bot detection)
- Best for API integration and programmatic processing

**EXCEL Format:**
- Full Excel workbook (.xlsx)
- All fields in columns with headers
- Auto-sized columns
- Best for sharing with non-technical users

#### Examples

**Export all logs for today as CSV:**
```bash
GET /api/logs/export?format=CSV
```

**Export specific date as Excel:**
```bash
GET /api/logs/export?format=EXCEL&date=2026-02-09
```

**Export only errors as JSON:**
```bash
GET /api/logs/export?format=JSON&statusCategory=5xx
```

**Export security threats as Excel:**
```bash
GET /api/logs/export?format=EXCEL&isSuspicious=true&minThreatScore=70
```

**Export slow POST requests:**
```bash
GET /api/logs/export?format=CSV&requestMethod=POST&isSlowRequest=true
```

---

## Error Responses

### 400 Bad Request

Invalid parameters provided.

```json
{
  "error": "Invalid date format",
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

## Performance Considerations

1. **Pagination:** Always use pagination for large result sets to avoid memory issues
2. **Date Range:** Queries are limited to single-day logs
3. **Filtering:** More specific filters = faster response
4. **Export Limits:** Large exports may take time; consider filtering first
5. **File Size:** Log files can be large; exports may timeout for very large files

---

## Best Practices

1. **Always specify a date** for historical queries
2. **Use pagination** to avoid loading too many records
3. **Apply filters** to narrow down results before exporting
4. **Use CSV/Excel** for data analysis
5. **Use JSON** for programmatic integration
6. **Monitor slow requests** regularly using `isSlowRequest=true`
7. **Track security threats** using `isSuspicious=true`

---

## Comparison Operators

When filtering by numeric values (response size, time taken), use these operators:

| Operator | Description | Example |
|----------|-------------|---------|
| `equality` | Exact match | `timeTakenMillis=1000&timeTakenMillisArgument=equality` |
| `inequality` | Not equal | `status=200&statusArgument=inequality` |
| `greaterthan` | Greater than | `responseSizeBytes=1000000&responseSizeBytesArgument=greaterthan` |
| `lessthan` | Less than | `timeTakenMillis=100&timeTakenMillisArgument=lessthan` |
| `greaterthanorequalto` | Greater than or equal to | `timeTakenMillis=500&timeTakenMillisArgument=greaterthanorequalto` |
| `lessthanorequalto` | Less than or equal to | `responseSizeBytes=10000&responseSizeBytesArgument=lessthanorequalto` |

---

## Notes

- All timestamps are in the server's local timezone
- Log entries are returned in **reverse chronological order** (newest first)
- Security analysis is performed automatically on all logs
- Bot detection uses user agent analysis and behavioral patterns
- Performance grading: A (<100ms), B (100-500ms), C (500-1000ms), D (1000-3000ms), F (>3000ms)
- Geographic data requires GeoIP database configuration (returns null if not configured)

---

**Last Updated:** 2026-02-09
**API Version:** 1.0
**Controller:** LogController.java
