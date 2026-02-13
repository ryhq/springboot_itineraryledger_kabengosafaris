# Dashboard API

Comprehensive dashboard statistics API for the Kabenga Safaris management system.

## Overview

The Dashboard API provides aggregated statistics and metrics across all key business entities including Quotes, Invoices, Customers, Safaris, and more. It's designed to power dashboard views with real-time data.

## API Endpoints

### 1. Get Comprehensive Dashboard Statistics

**Endpoint:** `GET /api/dashboard/stats`

**Permission:** `PERM_VIEW_DASHBOARD`

**Description:** Returns all dashboard statistics in a single response including quotes, invoices, customers, safaris, activities, and recent activity.

**Response Structure:**
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
      "SENT": 30,
      "ACCEPTED": 20,
      ...
    },

    // Invoice Metrics
    "totalInvoices": 120,
    "draftInvoices": 15,
    "sentInvoices": 20,
    "paidInvoices": 50,
    "overdueInvoices": 10,
    "partiallyPaidInvoices": 5,
    "invoicesByStatus": { ... },
    "totalRevenue": [
      { "currency": "USD", "amount": 150000.00 },
      { "currency": "KES", "amount": 2500000.00 }
    ],
    "pendingRevenue": [ ... ],
    "overdueRevenue": [ ... ],

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
    "safarisByState": { ... },

    // Activity Metrics
    "totalActivities": 50,
    "totalAccommodations": 35,
    "totalParks": 15,

    // User Metrics
    "totalUsers": 25,
    "activeUsers": 20,

    // Recent Activity
    "recentQuotes": [ ... ],
    "recentInvoices": [ ... ],
    "recentSafaris": [ ... ],
    "recentCustomers": [ ... ]
  }
}
```

### 2. Get Quote Statistics Only

**Endpoint:** `GET /api/dashboard/stats/quotes`

**Permission:** `PERM_VIEW_DASHBOARD` or `PERM_READ_QUOTE`

**Description:** Returns only quote-related statistics including conversion rates and recent quotes.

**Example Response:**
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
        "id": "encoded_id",
        "code": "QT-1001-0126-1",
        "title": "5-Day Masai Mara Safari",
        "type": "QUOTE",
        "status": "Sent",
        "createdAt": "2026-01-15 10:30:00",
        "createdBy": "john.doe"
      },
      ...
    ]
  }
}
```

### 3. Get Invoice Statistics Only

**Endpoint:** `GET /api/dashboard/stats/invoices`

**Permission:** `PERM_VIEW_DASHBOARD` or `PERM_READ_INVOICE`

**Description:** Returns invoice statistics including revenue metrics by currency.

**Key Metrics:**
- Total invoices by status
- Revenue by currency (total, pending, overdue)
- Recent invoices

### 4. Get Customer Statistics Only

**Endpoint:** `GET /api/dashboard/stats/customers`

**Permission:** `PERM_VIEW_DASHBOARD` or `PERM_READ_CUSTOMER`

**Description:** Returns customer statistics including new customer trends.

**Key Metrics:**
- Total customers
- Active/VIP customers
- New customers (this week, this month)
- Recent customers

### 5. Get Safari Statistics Only

**Endpoint:** `GET /api/dashboard/stats/safaris`

**Permission:** `PERM_VIEW_DASHBOARD` or `PERM_READ_SAFARI`

**Description:** Returns safari statistics including upcoming and ongoing safaris.

**Key Metrics:**
- Total safaris by state
- Upcoming safaris (starting within 30 days)
- Ongoing safaris (currently in progress)
- Completed/cancelled safaris
- Recent safaris

### 6. Health Check

**Endpoint:** `GET /api/dashboard/health`

**Permission:** None (public)

**Description:** Health check endpoint to verify dashboard API is running.

## Usage Examples

### Fetch All Dashboard Stats

```bash
curl -X GET "http://localhost:8080/api/dashboard/stats" \
  -H "Authorization: Bearer <your-token>"
```

### Fetch Quote Stats Only

```bash
curl -X GET "http://localhost:8080/api/dashboard/stats/quotes" \
  -H "Authorization: Bearer <your-token>"
```

### Fetch Invoice Stats with Revenue

```bash
curl -X GET "http://localhost:8080/api/dashboard/stats/invoices" \
  -H "Authorization: Bearer <your-token>"
```

## Permission Requirements

The dashboard API uses the following permissions:

- `PERM_VIEW_DASHBOARD` - Full access to all dashboard statistics
- `PERM_READ_QUOTE` - Access to quote statistics
- `PERM_READ_INVOICE` - Access to invoice statistics
- `PERM_READ_CUSTOMER` - Access to customer statistics
- `PERM_READ_SAFARI` - Access to safari statistics

## Data Aggregation Logic

### Quote Conversion Rate
```
Conversion Rate = ((Accepted Quotes + Converted Quotes) / Total Sent Quotes) × 100
```

### Revenue Metrics
- **Total Revenue**: Sum of grand totals from all PAID invoices, grouped by currency
- **Pending Revenue**: Sum of balances from SENT, VIEWED, and PARTIALLY_PAID invoices
- **Overdue Revenue**: Sum of balances from OVERDUE invoices

### Upcoming Safaris
Safaris with start date within the next 30 days

### Ongoing Safaris
Safaris where current date is between start date and end date (inclusive)

### Recent Activity
Returns the 5 most recently created items for each entity type, ordered by creation date (descending)

## Architecture

### Components

1. **DashboardController** - REST API endpoints
2. **DashboardService** - Business logic and data aggregation
3. **DashboardStatsDTO** - Response data structure

### Dependencies

The dashboard service aggregates data from:
- QuoteRepository
- InvoiceRepository
- CustomerRepository
- SafariRepository
- UserRepository
- ActivityRepository
- AccommodationRepository
- ParkRepository

## Performance Considerations

- All count queries use repository-level counting (efficient)
- Recent activity queries use pagination (limit 5 items)
- Revenue calculations aggregate in-memory after fetching relevant invoices
- Consider caching dashboard stats for high-traffic scenarios

## Future Enhancements

Potential improvements:
1. Add date range filters for statistics
2. Implement caching with Redis
3. Add trend analysis (week-over-week, month-over-month)
4. Real-time updates via WebSockets
5. Export dashboard data as PDF/Excel
6. Customizable dashboard widgets

## Error Handling

All endpoints return standardized error responses:

```json
{
  "statusCode": 500,
  "message": "Failed to fetch dashboard statistics: <error details>",
  "errorCode": "DASHBOARD_ERROR"
}
```

## Testing

To test the dashboard API:

```bash
# Health check
curl http://localhost:8080/api/dashboard/health

# Get all stats (requires authentication)
curl -X GET http://localhost:8080/api/dashboard/stats \
  -H "Authorization: Bearer <token>"
```

## Support

For issues or questions about the Dashboard API, contact the development team.
