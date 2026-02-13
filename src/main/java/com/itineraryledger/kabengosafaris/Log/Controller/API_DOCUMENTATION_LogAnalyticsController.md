# Log Analytics Controller API Documentation

## Overview

The Log Analytics Controller provides comprehensive analytics and insights from access logs, including dashboard metrics, endpoint statistics, traffic patterns, geographic distribution, user agent analysis, and error tracking.

## Base URL

```
/api/logs/analytics
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

### ApiResponse Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | `true` for successful requests, `false` for errors |
| `statusCode` | Integer | HTTP status code (200, 400, 404, 500, etc.) |
| `message` | String | Human-readable success or error message |
| `data` | Object | Response payload (only in success responses) |
| `errorCode` | String | Machine-readable error code (only in error responses) |
| `timestamp` | String | ISO 8601 timestamp when response was generated |

**Note:** In the examples below, response data is shown within the `data` field of the ApiResponse wrapper.

---

## Endpoints

### 1. Get Dashboard Overview

Retrieve high-level summary metrics for monitoring and dashboard display.

**Endpoint:** `GET /api/logs/analytics/overview`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to analyze (format: yyyy-MM-dd) |

#### Response

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Dashboard overview retrieved successfully",
  "data": {
    "totalRequests": 10523,
    "uniqueIPs": 1245,
    "errorRate": 3.5,
    "successRate": 96.5,
    "avgResponseTime": 187,
    "topEndpoint": "/api/parks",
    "topEndpointCount": 3421,
    "suspiciousRequests": 15,
    "botRequests": 523,
    "botPercentage": 4.97,
    "slowRequests": 42,
    "slowRequestPercentage": 0.4,
    "bandwidthUsed": 2147483648,
    "bandwidthFormatted": "2.00 GB"
  },
  "timestamp": "2026-02-10T14:23:45.123"
}
```

#### Response Fields (within `data` object)

| Field | Type | Description |
|-------|------|-------------|
| `totalRequests` | Integer | Total number of requests |
| `uniqueIPs` | Integer | Number of unique IP addresses |
| `errorRate` | Double | Percentage of requests with 4xx/5xx errors |
| `successRate` | Double | Percentage of requests with 2xx/3xx status |
| `avgResponseTime` | Long | Average response time in milliseconds |
| `topEndpoint` | String | Most frequently accessed endpoint |
| `topEndpointCount` | Integer | Number of requests to top endpoint |
| `suspiciousRequests` | Integer | Number of security threats detected |
| `botRequests` | Integer | Number of bot requests |
| `botPercentage` | Double | Percentage of requests from bots |
| `slowRequests` | Integer | Number of slow requests |
| `slowRequestPercentage` | Double | Percentage of slow requests |
| `bandwidthUsed` | Long | Total bandwidth in bytes |
| `bandwidthFormatted` | String | Human-readable bandwidth (KB/MB/GB) |

#### Examples

**Get today's overview:**
```bash
GET /api/logs/analytics/overview
```

**Get overview for specific date:**
```bash
GET /api/logs/analytics/overview?date=2026-02-09
```

---

### 2. Get Endpoint Statistics

Retrieve detailed statistics for top N endpoints.

**Endpoint:** `GET /api/logs/analytics/endpoints`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to analyze (format: yyyy-MM-dd) |
| `limit` | Integer | No | 20 | Number of endpoints to return (1-100) |

#### Response

**Note:** For brevity, subsequent examples show only the `data` field content. All responses are wrapped in the ApiResponse format shown above.

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Endpoint statistics retrieved successfully",
  "data": {
    "endpoints": [
      {
        "endpoint": "/api/parks",
      "requestCount": 3421,
      "avgResponseTime": 145,
      "minResponseTime": 23,
      "maxResponseTime": 2345,
      "errorRate": 2.1,
      "successRate": 97.9,
      "bandwidthUsed": 52428800,
      "bandwidthFormatted": "50.00 MB",
      "uniqueIPs": 234,
      "slowRequestCount": 5,
      "suspiciousRequestCount": 2,
      "botRequestCount": 78
    },
    {
      "endpoint": "/api/accommodations",
      "requestCount": 2156,
      "avgResponseTime": 234,
      "minResponseTime": 45,
      "maxResponseTime": 3456,
      "errorRate": 1.8,
      "successRate": 98.2,
      "bandwidthUsed": 31457280,
      "bandwidthFormatted": "30.00 MB",
      "uniqueIPs": 189,
      "slowRequestCount": 12,
      "suspiciousRequestCount": 0,
      "botRequestCount": 45
    }
  ],
  "count": 2,
  "date": "2026-02-09"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `endpoint` | String | API endpoint URI |
| `requestCount` | Integer | Total number of requests |
| `avgResponseTime` | Long | Average response time in milliseconds |
| `minResponseTime` | Long | Fastest response time |
| `maxResponseTime` | Long | Slowest response time |
| `errorRate` | Double | Percentage of error responses |
| `successRate` | Double | Percentage of successful responses |
| `bandwidthUsed` | Long | Total bandwidth consumed (bytes) |
| `bandwidthFormatted` | String | Human-readable bandwidth |
| `uniqueIPs` | Integer | Number of unique IPs accessing endpoint |
| `slowRequestCount` | Integer | Number of slow requests |
| `suspiciousRequestCount` | Integer | Number of security threats |
| `botRequestCount` | Integer | Number of bot requests |

Endpoints are sorted by request count (most accessed first).

#### Examples

**Get top 20 endpoints:**
```bash
GET /api/logs/analytics/endpoints
```

**Get top 10 endpoints:**
```bash
GET /api/logs/analytics/endpoints?limit=10
```

**Analyze specific date:**
```bash
GET /api/logs/analytics/endpoints?date=2026-02-09&limit=50
```

---

### 3. Get Hourly Metrics

Retrieve traffic patterns broken down by hour (24-hour analysis).

**Endpoint:** `GET /api/logs/analytics/hourly`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to analyze (format: yyyy-MM-dd) |

#### Response

```json
{
  "hourlyMetrics": [
    {
      "hour": 0,
      "requestCount": 234,
      "avgResponseTime": 156,
      "errorRate": 2.5,
      "uniqueIPs": 45,
      "suspiciousCount": 1,
      "botCount": 12,
      "slowRequestCount": 3
    },
    {
      "hour": 1,
      "requestCount": 189,
      "avgResponseTime": 143,
      "errorRate": 1.8,
      "uniqueIPs": 38,
      "suspiciousCount": 0,
      "botCount": 8,
      "slowRequestCount": 1
    }
    // ... 22 more hours
  ],
  "date": "2026-02-09"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `hour` | Integer | Hour of day (0-23) |
| `requestCount` | Integer | Number of requests in this hour |
| `avgResponseTime` | Long | Average response time (ms) |
| `errorRate` | Double | Percentage of errors |
| `uniqueIPs` | Integer | Unique IPs active in this hour |
| `suspiciousCount` | Integer | Security threats detected |
| `botCount` | Integer | Bot requests |
| `slowRequestCount` | Integer | Slow requests |

Returns 24 entries (one for each hour), even if some hours have zero traffic.

#### Examples

**Get today's hourly breakdown:**
```bash
GET /api/logs/analytics/hourly
```

**Analyze specific date:**
```bash
GET /api/logs/analytics/hourly?date=2026-02-09
```

#### Use Cases

- Identify peak traffic hours
- Plan maintenance windows
- Detect unusual traffic patterns
- Optimize resource allocation
- Monitor performance throughout the day

---

### 4. Get Geographic Distribution

Retrieve country-based traffic distribution.

**Endpoint:** `GET /api/logs/analytics/geographic`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to analyze (format: yyyy-MM-dd) |

#### Response

```json
{
  "distribution": [
    {
      "countryCode": "TZ",
      "countryName": "Tanzania",
      "requestCount": 5234,
      "percentage": 49.7,
      "uniqueIPs": 623,
      "avgResponseTime": 145,
      "suspiciousCount": 5,
      "botCount": 89
    },
    {
      "countryCode": "US",
      "countryName": "United States",
      "requestCount": 2156,
      "percentage": 20.5,
      "uniqueIPs": 412,
      "avgResponseTime": 234,
      "suspiciousCount": 12,
      "botCount": 156
    },
    {
      "countryCode": "Unknown",
      "countryName": "Unknown",
      "requestCount": 1523,
      "percentage": 14.5,
      "uniqueIPs": 289,
      "avgResponseTime": 187,
      "suspiciousCount": 8,
      "botCount": 45
    }
  ],
  "count": 3,
  "date": "2026-02-09",
  "note": "Geographic data requires GeoIP database. Returns 'Unknown' if not enabled."
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `countryCode` | String | ISO 3166-1 alpha-2 country code (or "Unknown") |
| `countryName` | String | Full country name |
| `requestCount` | Integer | Number of requests from this country |
| `percentage` | Double | Percentage of total requests |
| `uniqueIPs` | Integer | Unique IPs from this country |
| `avgResponseTime` | Long | Average response time (ms) |
| `suspiciousCount` | Integer | Security threats from this country |
| `botCount` | Integer | Bot requests from this country |

Countries are sorted by request count (highest first).

#### Examples

**Get today's geographic distribution:**
```bash
GET /api/logs/analytics/geographic
```

**Analyze specific date:**
```bash
GET /api/logs/analytics/geographic?date=2026-02-09
```

#### Notes

- Requires GeoIP database configuration (MaxMind GeoLite2)
- Returns "Unknown" for all requests if GeoIP is not configured
- Accuracy depends on IP database freshness
- VPN/Proxy traffic may show incorrect locations

---

### 5. Get User Agent Statistics

Retrieve browser and operating system distribution.

**Endpoint:** `GET /api/logs/analytics/user-agents`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to analyze (format: yyyy-MM-dd) |

#### Response

```json
{
  "browsers": [
    {
      "type": "BROWSER",
      "name": "Chrome",
      "requestCount": 5234,
      "percentage": 49.7,
      "uniqueIPs": 623
    },
    {
      "type": "BROWSER",
      "name": "Firefox",
      "requestCount": 2156,
      "percentage": 20.5,
      "uniqueIPs": 412
    },
    {
      "type": "BROWSER",
      "name": "Safari",
      "requestCount": 1523,
      "percentage": 14.5,
      "uniqueIPs": 289
    }
  ],
  "operatingSystems": [
    {
      "type": "OS",
      "name": "Windows",
      "requestCount": 4523,
      "percentage": 43.0,
      "uniqueIPs": 567
    },
    {
      "type": "OS",
      "name": "Android",
      "requestCount": 2834,
      "percentage": 26.9,
      "uniqueIPs": 423
    },
    {
      "type": "OS",
      "name": "iOS",
      "requestCount": 1956,
      "percentage": 18.6,
      "uniqueIPs": 312
    }
  ],
  "date": "2026-02-09"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `type` | String | "BROWSER" or "OS" |
| `name` | String | Browser/OS name |
| `requestCount` | Integer | Number of requests |
| `percentage` | Double | Percentage of total requests |
| `uniqueIPs` | Integer | Unique IPs using this browser/OS |

Each list is sorted by request count (most popular first).

#### Examples

**Get today's user agent stats:**
```bash
GET /api/logs/analytics/user-agents
```

**Analyze specific date:**
```bash
GET /api/logs/analytics/user-agents?date=2026-02-09
```

#### Use Cases

- Browser compatibility planning
- Mobile vs desktop traffic analysis
- Feature support decisions
- Progressive enhancement strategies

---

### 6. Get Error Analysis

Retrieve detailed error statistics broken down by status code and endpoint.

**Endpoint:** `GET /api/logs/analytics/errors`

**Permission Required:** `PERM_READ_LOG`

#### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `date` | Date (ISO) | No | Today | Date to analyze (format: yyyy-MM-dd) |

#### Response

```json
{
  "clientErrors": [
    {
      "status": 404,
      "statusCategory": "4xx",
      "endpoint": "/api/nonexistent",
      "errorCount": 523,
      "percentage": 45.2,
      "uniqueIPs": 123
    },
    {
      "status": 400,
      "statusCategory": "4xx",
      "endpoint": "/api/parks",
      "errorCount": 234,
      "percentage": 20.2,
      "uniqueIPs": 67
    }
  ],
  "serverErrors": [
    {
      "status": 500,
      "statusCategory": "5xx",
      "endpoint": "/api/bookings",
      "errorCount": 45,
      "percentage": 3.9,
      "uniqueIPs": 23
    },
    {
      "status": 503,
      "statusCategory": "5xx",
      "endpoint": "/api/payments",
      "errorCount": 12,
      "percentage": 1.0,
      "uniqueIPs": 8
    }
  ],
  "totalErrors": 814,
  "date": "2026-02-09"
}
```

**No Errors Response:**
```json
{
  "errors": [],
  "count": 0,
  "date": "2026-02-09",
  "message": "No errors found for the specified date"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `status` | Integer | HTTP status code |
| `statusCategory` | String | "4xx" or "5xx" |
| `endpoint` | String | API endpoint where error occurred |
| `errorCount` | Integer | Number of occurrences |
| `percentage` | Double | Percentage of total errors |
| `uniqueIPs` | Integer | Unique IPs experiencing this error |

Errors are sorted by count (most frequent first) within each category.

#### Error Categories

**Client Errors (4xx):**
- User errors, invalid requests, missing resources
- Examples: 400 Bad Request, 404 Not Found, 401 Unauthorized

**Server Errors (5xx):**
- Application errors, service unavailability
- Examples: 500 Internal Server Error, 503 Service Unavailable

#### Examples

**Get today's errors:**
```bash
GET /api/logs/analytics/errors
```

**Analyze specific date:**
```bash
GET /api/logs/analytics/errors?date=2026-02-09
```

#### Use Cases

- Identify broken endpoints
- Track error trends
- Prioritize bug fixes
- Monitor service health
- Detect breaking changes

---

## Error Responses

### 400 Bad Request

Invalid parameters provided.

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Future dates are not allowed",
  "errorCode": "INVALID_DATE",
  "timestamp": "2026-02-10T14:23:45.123"
}
```

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Limit must be between 1 and 100",
  "errorCode": "INVALID_LIMIT",
  "timestamp": "2026-02-10T14:23:45.123"
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

No log data found for specified date.

```json
{
  "success": false,
  "statusCode": 404,
  "message": "No logs found for the specified date: 2026-02-09",
  "errorCode": "NO_LOGS_FOUND",
  "timestamp": "2026-02-10T14:23:45.123"
}
```

```json
{
  "success": false,
  "statusCode": 404,
  "message": "No endpoint data found for the specified date: 2026-02-09",
  "errorCode": "NO_DATA_FOUND",
  "timestamp": "2026-02-10T14:23:45.123"
}
```

### 500 Internal Server Error

Server error while processing request.

```json
{
  "success": false,
  "statusCode": 500,
  "message": "Failed to generate dashboard overview: Connection timeout",
  "errorCode": "INTERNAL_ERROR",
  "timestamp": "2026-02-10T14:23:45.123"
}
```

---

## Best Practices

### 1. Dashboard Monitoring

**Create a daily overview dashboard:**
```bash
GET /api/logs/analytics/overview
```

**Key metrics to watch:**
- Error rate > 5% → investigate
- Avg response time > 500ms → performance issue
- Suspicious requests > 0 → security concern

### 2. Performance Analysis

**Identify slow endpoints:**
```bash
GET /api/logs/analytics/endpoints?limit=20
```

**Look for:**
- Endpoints with avgResponseTime > 1000ms
- High maxResponseTime values
- Many slowRequestCount

### 3. Traffic Pattern Analysis

**Analyze peak hours:**
```bash
GET /api/logs/analytics/hourly
```

**Use to:**
- Schedule maintenance during low-traffic hours
- Scale resources for peak times
- Detect unusual patterns (potential attacks)

### 4. Error Tracking

**Monitor errors daily:**
```bash
GET /api/logs/analytics/errors
```

**Focus on:**
- 5xx errors (server-side issues)
- Recurring 4xx errors (potential UX issues)
- Endpoints with high error rates

### 5. Geographic Insights

**Understand user distribution:**
```bash
GET /api/logs/analytics/geographic
```

**Use for:**
- CDN configuration
- Regional performance optimization
- Targeted marketing

---

## Integration Examples

### Daily Health Check Script

```bash
#!/bin/bash
# Check API health and send alert if issues found

TOKEN="your-jwt-token"
BASE_URL="https://api.kabengosafaris.com"

# Get overview
OVERVIEW=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/logs/analytics/overview")

ERROR_RATE=$(echo $OVERVIEW | jq '.errorRate')
AVG_RESPONSE=$(echo $OVERVIEW | jq '.avgResponseTime')
SUSPICIOUS=$(echo $OVERVIEW | jq '.suspiciousRequests')

# Check thresholds
if (( $(echo "$ERROR_RATE > 5.0" | bc -l) )); then
  echo "ALERT: High error rate: $ERROR_RATE%"
fi

if (( $(echo "$AVG_RESPONSE > 500" | bc -l) )); then
  echo "ALERT: Slow response time: ${AVG_RESPONSE}ms"
fi

if [ $SUSPICIOUS -gt 0 ]; then
  echo "WARNING: $SUSPICIOUS suspicious requests detected"
fi
```

### Weekly Performance Report

```bash
#!/bin/bash
# Generate weekly performance report

TOKEN="your-jwt-token"
BASE_URL="https://api.kabengosafaris.com"

echo "=== Weekly Performance Report ==="
echo ""

# Loop through last 7 days
for i in {0..6}; do
  DATE=$(date -d "$i days ago" +%Y-%m-%d)
  OVERVIEW=$(curl -s -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/api/logs/analytics/overview?date=$DATE")

  REQUESTS=$(echo $OVERVIEW | jq '.totalRequests')
  AVG_TIME=$(echo $OVERVIEW | jq '.avgResponseTime')
  ERROR_RATE=$(echo $OVERVIEW | jq '.errorRate')

  echo "$DATE: $REQUESTS requests, ${AVG_TIME}ms avg, ${ERROR_RATE}% errors"
done
```

---

## Notes

- All analytics are calculated in real-time from log files
- No persistent storage; data regenerated on each request
- Performance optimized for single-day analysis
- Multi-day analysis requires multiple API calls
- Cache results client-side for better performance
- Best used with specific date parameter for historical analysis

---

**Last Updated:** 2026-02-09
**API Version:** 1.0
**Controller:** LogAnalyticsController.java
