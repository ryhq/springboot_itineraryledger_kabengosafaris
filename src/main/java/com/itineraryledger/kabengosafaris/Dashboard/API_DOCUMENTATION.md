# Dashboard API Documentation

## Overview

The Dashboard API provides comprehensive statistics and metrics for the Kabenga Safaris management system. It aggregates data across all key business entities including Quotes, Invoices, Customers, Safaris, Users, Activities, Accommodations, and Parks.

**Base URL:** `/api/dashboard`

**Version:** 1.0

**Authentication:** Required (JWT Bearer Token)

---

## Table of Contents

1. [API Endpoints](#api-endpoints)
   - [Get Comprehensive Dashboard Statistics](#1-get-comprehensive-dashboard-statistics)
   - [Get Quote Statistics](#2-get-quote-statistics)
   - [Get Invoice Statistics](#3-get-invoice-statistics)
   - [Get Customer Statistics](#4-get-customer-statistics)
   - [Get Safari Statistics](#5-get-safari-statistics)
   - [Health Check](#6-health-check)
2. [Data Models](#data-models)
3. [Permissions](#permissions)
4. [Error Handling](#error-handling)
5. [Usage Examples](#usage-examples)

---

## API Endpoints

### 1. Get Comprehensive Dashboard Statistics

Retrieves all dashboard statistics in a single response, including metrics for quotes, invoices, customers, safaris, users, activities, accommodations, and parks.

**Endpoint:** `GET /api/dashboard/stats`

**Permission Required:** `PERM_VIEW_DASHBOARD`

**Request Headers:**
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Dashboard statistics retrieved successfully",
  "data": {
    // Quote Metrics
    "totalQuotes": 150,
    "draftQuotes": 25,
    "sentQuotes": 30,
    "acceptedQuotes": 20,
    "convertedQuotes": 15,
    "expiredQuotes": 10,
    "quoteConversionRate": 66.67,
    "quotesByStatus": {
      "DRAFT": 25,
      "READY": 15,
      "SENT": 30,
      "ACCEPTED": 20,
      "REJECTED": 10,
      "EXPIRED": 10,
      "CANCELLED": 5,
      "CONVERTED": 15
    },

    // Invoice Metrics
    "totalInvoices": 120,
    "draftInvoices": 15,
    "sentInvoices": 20,
    "paidInvoices": 50,
    "overdueInvoices": 10,
    "partiallyPaidInvoices": 5,
    "invoicesByStatus": {
      "DRAFT": 15,
      "SENT": 20,
      "VIEWED": 10,
      "PARTIALLY_PAID": 5,
      "PAID": 50,
      "OVERDUE": 10,
      "CANCELLED": 5,
      "REFUNDED": 5
    },
    "totalRevenue": [
      {
        "currency": "USD",
        "amount": 150000.00
      },
      {
        "currency": "KES",
        "amount": 2500000.00
      }
    ],
    "pendingRevenue": [
      {
        "currency": "USD",
        "amount": 25000.00
      }
    ],
    "overdueRevenue": [
      {
        "currency": "USD",
        "amount": 5000.00
      }
    ],

    // Customer Metrics
    "totalCustomers": 200,
    "activeCustomers": 180,
    "vipCustomers": 15,
    "newCustomersThisMonth": 12,
    "newCustomersThisWeek": 3,

    // Safari Metrics
    "totalSafaris": 100,
    "activeSafaris": 80,
    "upcomingSafaris": 25,
    "ongoingSafaris": 5,
    "completedSafaris": 60,
    "cancelledSafaris": 8,
    "safarisByState": {
      "DRAFT": 10,
      "PENDING_APPROVAL": 5,
      "APPROVED": 8,
      "CONFIRMED": 20,
      "PENDING_PAYMENT": 12,
      "FULLY_PAID": 15,
      "IN_PROGRESS": 5,
      "COMPLETED": 60,
      "CLOSED": 50,
      "ON_HOLD": 2,
      "CANCELLED": 8,
      "REFUND_PENDING": 1,
      "REFUND_COMPLETE": 3,
      "DISPUTED": 1
    },

    // Activity Metrics
    "totalActivities": 50,
    "totalAccommodations": 35,
    "totalParks": 15,

    // User Metrics
    "totalUsers": 25,
    "activeUsers": 20,

    // Recent Activity (5 most recent items)
    "recentQuotes": [
      {
        "id": "encoded_id_123",
        "code": "QT-1001-0126-1",
        "title": "5-Day Masai Mara Safari",
        "type": "QUOTE",
        "status": "Sent",
        "createdAt": "2026-02-08 14:30:00",
        "createdBy": "john.doe"
      }
    ],
    "recentInvoices": [
      {
        "id": "encoded_id_456",
        "code": "INV-000101",
        "title": "Safari Package Payment",
        "type": "INVOICE",
        "status": "Paid",
        "createdAt": "2026-02-07 10:15:00",
        "createdBy": "jane.smith"
      }
    ],
    "recentSafaris": [
      {
        "id": "encoded_id_789",
        "code": "SAF-5D4N-01001",
        "title": "Amboseli Adventure",
        "type": "SAFARI",
        "status": "CONFIRMED",
        "createdAt": "2026-02-06 09:00:00",
        "createdBy": "admin"
      }
    ],
    "recentCustomers": [
      {
        "id": "encoded_id_321",
        "code": "CUS-000101",
        "title": "John Smith",
        "type": "CUSTOMER",
        "status": "Active",
        "createdAt": "2026-02-05 16:45:00",
        "createdBy": "System"
      }
    ]
  }
}
```

**Error Response (500 Internal Server Error):**
```json
{
  "statusCode": 500,
  "message": "Failed to fetch dashboard statistics: <error details>",
  "errorCode": "DASHBOARD_ERROR"
}
```

---

### 2. Get Quote Statistics

Retrieves statistics specifically for quotes, including conversion rates and status breakdown.

**Endpoint:** `GET /api/dashboard/stats/quotes`

**Permission Required:** `PERM_VIEW_DASHBOARD` OR `PERM_READ_QUOTE`

**Request Headers:**
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Quote statistics retrieved successfully",
  "data": {
    "totalQuotes": 150,
    "draftQuotes": 25,
    "sentQuotes": 30,
    "acceptedQuotes": 20,
    "convertedQuotes": 15,
    "expiredQuotes": 10,
    "conversionRate": 66.67,
    "quotesByStatus": {
      "DRAFT": 25,
      "READY": 15,
      "SENT": 30,
      "ACCEPTED": 20,
      "REJECTED": 10,
      "EXPIRED": 10,
      "CANCELLED": 5,
      "CONVERTED": 15
    },
    "recentQuotes": [
      {
        "id": "encoded_id_123",
        "code": "QT-1001-0126-1",
        "title": "5-Day Masai Mara Safari",
        "type": "QUOTE",
        "status": "Sent",
        "createdAt": "2026-02-08 14:30:00",
        "createdBy": "john.doe"
      }
    ]
  }
}
```

**Conversion Rate Calculation:**
```
Conversion Rate = ((Accepted Quotes + Converted Quotes) / Total Sent Quotes) × 100
```

**Error Response (500 Internal Server Error):**
```json
{
  "statusCode": 500,
  "message": "Failed to fetch quote statistics: <error details>",
  "errorCode": "QUOTE_STATS_ERROR"
}
```

---

### 3. Get Invoice Statistics

Retrieves invoice statistics including revenue metrics broken down by currency.

**Endpoint:** `GET /api/dashboard/stats/invoices`

**Permission Required:** `PERM_VIEW_DASHBOARD` OR `PERM_READ_INVOICE`

**Request Headers:**
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Invoice statistics retrieved successfully",
  "data": {
    "totalInvoices": 120,
    "draftInvoices": 15,
    "sentInvoices": 20,
    "paidInvoices": 50,
    "overdueInvoices": 10,
    "partiallyPaidInvoices": 5,
    "invoicesByStatus": {
      "DRAFT": 15,
      "SENT": 20,
      "VIEWED": 10,
      "PARTIALLY_PAID": 5,
      "PAID": 50,
      "OVERDUE": 10,
      "CANCELLED": 5,
      "REFUNDED": 5
    },
    "totalRevenue": [
      {
        "currency": "USD",
        "amount": 150000.00
      },
      {
        "currency": "KES",
        "amount": 2500000.00
      },
      {
        "currency": "EUR",
        "amount": 125000.00
      }
    ],
    "pendingRevenue": [
      {
        "currency": "USD",
        "amount": 25000.00
      },
      {
        "currency": "KES",
        "amount": 500000.00
      }
    ],
    "overdueRevenue": [
      {
        "currency": "USD",
        "amount": 5000.00
      }
    ],
    "recentInvoices": [
      {
        "id": "encoded_id_456",
        "code": "INV-000101",
        "title": "Safari Package Payment",
        "type": "INVOICE",
        "status": "Paid",
        "createdAt": "2026-02-07 10:15:00",
        "createdBy": "jane.smith"
      }
    ]
  }
}
```

**Revenue Metrics Definitions:**
- **Total Revenue**: Sum of grand totals from all PAID invoices
- **Pending Revenue**: Sum of balances from SENT, VIEWED, and PARTIALLY_PAID invoices
- **Overdue Revenue**: Sum of balances from OVERDUE invoices

**Error Response (500 Internal Server Error):**
```json
{
  "statusCode": 500,
  "message": "Failed to fetch invoice statistics: <error details>",
  "errorCode": "INVOICE_STATS_ERROR"
}
```

---

### 4. Get Customer Statistics

Retrieves customer statistics including growth trends and VIP customer counts.

**Endpoint:** `GET /api/dashboard/stats/customers`

**Permission Required:** `PERM_VIEW_DASHBOARD` OR `PERM_READ_CUSTOMER`

**Request Headers:**
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Customer statistics retrieved successfully",
  "data": {
    "totalCustomers": 200,
    "activeCustomers": 180,
    "vipCustomers": 15,
    "newCustomersThisMonth": 12,
    "newCustomersThisWeek": 3,
    "recentCustomers": [
      {
        "id": "encoded_id_321",
        "code": "CUS-000101",
        "title": "John Smith",
        "type": "CUSTOMER",
        "status": "Active",
        "createdAt": "2026-02-05 16:45:00",
        "createdBy": "System"
      }
    ]
  }
}
```

**Metric Definitions:**
- **Active Customers**: Customers with `isActive = true`
- **VIP Customers**: Customers flagged as VIP
- **New Customers This Month**: Customers created in the last 30 days
- **New Customers This Week**: Customers created in the last 7 days

**Error Response (500 Internal Server Error):**
```json
{
  "statusCode": 500,
  "message": "Failed to fetch customer statistics: <error details>",
  "errorCode": "CUSTOMER_STATS_ERROR"
}
```

---

### 5. Get Safari Statistics

Retrieves safari statistics including upcoming and ongoing safaris.

**Endpoint:** `GET /api/dashboard/stats/safaris`

**Permission Required:** `PERM_VIEW_DASHBOARD` OR `PERM_READ_SAFARI`

**Request Headers:**
```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Safari statistics retrieved successfully",
  "data": {
    "totalSafaris": 100,
    "activeSafaris": 80,
    "upcomingSafaris": 25,
    "ongoingSafaris": 5,
    "completedSafaris": 60,
    "cancelledSafaris": 8,
    "safarisByState": {
      "DRAFT": 10,
      "PENDING_APPROVAL": 5,
      "APPROVED": 8,
      "CONFIRMED": 20,
      "PENDING_PAYMENT": 12,
      "FULLY_PAID": 15,
      "IN_PROGRESS": 5,
      "COMPLETED": 60,
      "CLOSED": 50,
      "ON_HOLD": 2,
      "CANCELLED": 8,
      "REFUND_PENDING": 1,
      "REFUND_COMPLETE": 3,
      "DISPUTED": 1
    },
    "recentSafaris": [
      {
        "id": "encoded_id_789",
        "code": "SAF-5D4N-01001",
        "title": "Amboseli Adventure",
        "type": "SAFARI",
        "status": "CONFIRMED",
        "createdAt": "2026-02-06 09:00:00",
        "createdBy": "admin"
      }
    ]
  }
}
```

**Metric Definitions:**
- **Active Safaris**: Safaris with `isActive = true`
- **Upcoming Safaris**: Safaris with start date within the next 30 days
- **Ongoing Safaris**: Safaris where current date is between start date and end date (inclusive)
- **Completed Safaris**: Safaris in COMPLETED state
- **Cancelled Safaris**: Safaris in CANCELLED state

**Error Response (500 Internal Server Error):**
```json
{
  "statusCode": 500,
  "message": "Failed to fetch safari statistics: <error details>",
  "errorCode": "SAFARI_STATS_ERROR"
}
```

---

### 6. Health Check

Simple health check endpoint to verify the dashboard API is operational.

**Endpoint:** `GET /api/dashboard/health`

**Permission Required:** None (Public)

**Request Headers:**
```http
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Dashboard API is healthy",
  "data": null
}
```

**Use Case:**
- Kubernetes/Docker health checks
- API monitoring systems
- Load balancer health probes

---

## Data Models

### DashboardStatsDTO

Complete dashboard statistics data transfer object.

```java
{
  // Quote Metrics
  "totalQuotes": Long,
  "draftQuotes": Long,
  "sentQuotes": Long,
  "acceptedQuotes": Long,
  "convertedQuotes": Long,
  "expiredQuotes": Long,
  "quoteConversionRate": Double,
  "quotesByStatus": Map<String, Long>,

  // Invoice Metrics
  "totalInvoices": Long,
  "draftInvoices": Long,
  "sentInvoices": Long,
  "paidInvoices": Long,
  "overdueInvoices": Long,
  "partiallyPaidInvoices": Long,
  "invoicesByStatus": Map<String, Long>,
  "totalRevenue": List<RevenueByCurrency>,
  "pendingRevenue": List<RevenueByCurrency>,
  "overdueRevenue": List<RevenueByCurrency>,

  // Customer Metrics
  "totalCustomers": Long,
  "activeCustomers": Long,
  "vipCustomers": Long,
  "newCustomersThisMonth": Long,
  "newCustomersThisWeek": Long,

  // Safari Metrics
  "totalSafaris": Long,
  "activeSafaris": Long,
  "upcomingSafaris": Long,
  "ongoingSafaris": Long,
  "completedSafaris": Long,
  "cancelledSafaris": Long,
  "safarisByState": Map<String, Long>,

  // Activity Metrics
  "totalActivities": Long,
  "totalAccommodations": Long,
  "totalParks": Long,

  // User Metrics
  "totalUsers": Long,
  "activeUsers": Long,

  // Recent Activity
  "recentQuotes": List<RecentActivityDTO>,
  "recentInvoices": List<RecentActivityDTO>,
  "recentSafaris": List<RecentActivityDTO>,
  "recentCustomers": List<RecentActivityDTO>
}
```

### RevenueByCurrency

Revenue aggregated by currency code.

```java
{
  "currency": String,  // e.g., "USD", "KES", "EUR"
  "amount": BigDecimal
}
```

### RecentActivityDTO

Recent activity item for timeline display.

```java
{
  "id": String,          // Obfuscated entity ID
  "code": String,        // Entity code (e.g., "QT-1001-0126-1")
  "title": String,       // Entity title/name
  "type": String,        // "QUOTE", "INVOICE", "SAFARI", "CUSTOMER"
  "status": String,      // Current status
  "createdAt": String,   // Format: "yyyy-MM-dd HH:mm:ss"
  "createdBy": String    // Username of creator
}
```

---

## Permissions

The Dashboard API uses role-based access control with the following permissions:

| Permission | Description | Endpoints |
|-----------|-------------|-----------|
| `PERM_VIEW_DASHBOARD` | Full access to all dashboard statistics | All `/api/dashboard/stats/*` endpoints |
| `PERM_READ_QUOTE` | Access to quote statistics | `/api/dashboard/stats/quotes` |
| `PERM_READ_INVOICE` | Access to invoice statistics | `/api/dashboard/stats/invoices` |
| `PERM_READ_CUSTOMER` | Access to customer statistics | `/api/dashboard/stats/customers` |
| `PERM_READ_SAFARI` | Access to safari statistics | `/api/dashboard/stats/safaris` |

**Permission Logic:**
- The comprehensive stats endpoint (`/stats`) requires `PERM_VIEW_DASHBOARD`
- Individual stats endpoints accept either `PERM_VIEW_DASHBOARD` OR the specific read permission
- Health check endpoint is public (no authentication required)

---

## Error Handling

All endpoints follow a consistent error response format:

### Common HTTP Status Codes

| Status Code | Description | When It Occurs |
|------------|-------------|----------------|
| 200 | Success | Request completed successfully |
| 401 | Unauthorized | Missing or invalid authentication token |
| 403 | Forbidden | User lacks required permission |
| 500 | Internal Server Error | Server-side error during processing |

### Error Response Format

```json
{
  "statusCode": 500,
  "message": "Human-readable error description",
  "errorCode": "MACHINE_READABLE_ERROR_CODE"
}
```

### Error Codes

| Error Code | HTTP Status | Description |
|-----------|-------------|-------------|
| `DASHBOARD_ERROR` | 500 | General dashboard statistics retrieval error |
| `QUOTE_STATS_ERROR` | 500 | Quote statistics retrieval error |
| `INVOICE_STATS_ERROR` | 500 | Invoice statistics retrieval error |
| `CUSTOMER_STATS_ERROR` | 500 | Customer statistics retrieval error |
| `SAFARI_STATS_ERROR` | 500 | Safari statistics retrieval error |

---

## Usage Examples

### Example 1: Fetch Complete Dashboard Statistics (cURL)

```bash
curl -X GET "http://localhost:8080/api/dashboard/stats" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json"
```

### Example 2: Fetch Quote Statistics (JavaScript/Axios)

```javascript
import axios from 'axios';

const fetchQuoteStats = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/dashboard/stats/quotes', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    console.log('Quote Stats:', response.data.data);
    console.log('Conversion Rate:', response.data.data.conversionRate + '%');
  } catch (error) {
    console.error('Error fetching quote stats:', error.response.data);
  }
};
```

### Example 3: Fetch Invoice Revenue (Python)

```python
import requests

def fetch_invoice_stats(token):
    url = "http://localhost:8080/api/dashboard/stats/invoices"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    response = requests.get(url, headers=headers)

    if response.status_code == 200:
        data = response.json()["data"]

        print("Total Revenue:")
        for revenue in data["totalRevenue"]:
            print(f"  {revenue['currency']}: {revenue['amount']}")

        print("\nPending Revenue:")
        for revenue in data["pendingRevenue"]:
            print(f"  {revenue['currency']}: {revenue['amount']}")
    else:
        print(f"Error: {response.status_code}")
        print(response.json())
```

### Example 4: React Hook for Dashboard

```typescript
import { useState, useEffect } from 'react';
import axios from 'axios';

interface DashboardStats {
  totalQuotes: number;
  totalInvoices: number;
  totalCustomers: number;
  totalSafaris: number;
  // ... other fields
}

export const useDashboardStats = (token: string) => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const response = await axios.get(
          'http://localhost:8080/api/dashboard/stats',
          {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          }
        );

        setStats(response.data.data);
        setError(null);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch dashboard stats');
      } finally {
        setLoading(false);
      }
    };

    if (token) {
      fetchStats();
    }
  }, [token]);

  return { stats, loading, error };
};
```

### Example 5: Health Check Monitoring

```bash
#!/bin/bash
# Simple health check script for monitoring

DASHBOARD_URL="http://localhost:8080/api/dashboard/health"

response=$(curl -s -o /dev/null -w "%{http_code}" "$DASHBOARD_URL")

if [ "$response" -eq 200 ]; then
    echo "✅ Dashboard API is healthy"
    exit 0
else
    echo "❌ Dashboard API is down (HTTP $response)"
    exit 1
fi
```

---

## Performance Considerations

### Caching Recommendations

The dashboard statistics can be expensive to calculate for large datasets. Consider implementing caching:

1. **Redis Cache**: Cache complete stats for 5 minutes
2. **Individual Caches**: Cache each stats endpoint separately with different TTLs
3. **Invalidation**: Clear cache on relevant entity updates

### Optimization Tips

1. **Use Specific Endpoints**: Fetch only the data you need
   - Use `/stats/quotes` instead of `/stats` if you only need quote data

2. **Pagination for Recent Items**: Recent activity is limited to 5 items per type

3. **Repository Counts**: All count queries use efficient database-level counting

4. **Avoid Polling**: Consider WebSockets for real-time updates instead of polling

---

## Rate Limiting

**Recommendation:** Implement rate limiting on dashboard endpoints:
- `/api/dashboard/stats`: 60 requests per minute per user
- Individual stats endpoints: 120 requests per minute per user
- Health check: No rate limiting

---

## Changelog

### Version 1.0 (2026-02-09)
- Initial release
- 6 endpoints implemented
- Multi-currency revenue support
- Recent activity tracking
- Comprehensive metrics across all entities

---

## Support

For issues, questions, or feature requests regarding the Dashboard API:

- **GitHub Issues**: [kabengosafaris/issues](https://github.com/kabengosafaris/issues)
- **Email**: dev@kabengosafaris.com
- **Documentation**: See [README.md](./README.md) for additional information

---

## Related Documentation

- [Dashboard README](./README.md) - Architecture and implementation details
- [Quote API Documentation](../Quote/README.md)
- [Invoice API Documentation](../Invoice/README.md)
- [Safari API Documentation](../Safari/README.md)
- [Customer API Documentation](../Customer/README.md)

---

**Last Updated:** 2026-02-09
**API Version:** 1.0
**Author:** Kabenga Safaris Development Team
