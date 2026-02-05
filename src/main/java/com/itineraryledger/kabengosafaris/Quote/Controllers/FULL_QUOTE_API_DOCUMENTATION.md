# Full Quote API Documentation

## Overview

This API endpoint retrieves a complete quote with all its nested data in a single request. It provides a comprehensive view of the entire quote structure including customer information, itinerary summary, line items with multi-currency pricing, and financial totals (subtotals, taxes, discounts, and grand totals).

## Endpoint

```
GET /api/quotes/{id}/full
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | String | Yes | The obfuscated quote ID |

### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | Bearer {token} | Yes |
| `Content-Type` | application/json | Yes |

### Required Permission

```
PERM_READ_QUOTE
```

## Response Structure

### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Full quote retrieved successfully",
  "data": {
    // Quote base fields
    "id": "xyz789abc",
    "quoteCode": "QTE-2024-001",
    "title": "7-Day Serengeti Safari - Smith Family",
    "description": "Comprehensive safari package including accommodation, park fees, and activities",
    "status": "APPROVED",
    "statusDisplayName": "Approved",
    "version": 2,
    "versionNotes": "Updated pricing for high season",

    // Pricing details
    "isStoRate": false,
    "taxPercentage": 18.00,
    "discountPercentage": 5.00,
    "discountReason": "Repeat customer discount",

    // Validity and dates
    "sentDate": "2024-01-15",
    "validFrom": "2024-01-15",
    "validTo": "2024-03-15",
    "isValid": true,
    "validityStatusMessage": "Valid until 2024-03-15",

    // Payment terms
    "depositPercentage": 30.00,
    "depositDueDate": "2024-02-01",
    "fullPaymentDueDate": "2024-06-01",

    // Notes
    "customerNotes": "All meals included as per itinerary. Drinks at own expense.",
    "internalNotes": "VIP client - ensure best vehicle assignment",

    // Approval
    "approverName": "john.manager",
    "approvedByName": "jane.director",
    "approvedAt": "2024-01-16T14:30:00",
    "approvalNotes": "Pricing approved for repeat customer",

    // Audit
    "isActive": true,
    "createdByName": "alice.agent",
    "updatedByName": "alice.agent",
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-16T15:00:00",

    // Summary statistics
    "totalItemsCount": 15,
    "totalCurrenciesCount": 2,

    // Customer information
    "customer": {
      "id": "cust123",
      "customerName": "Mr. John Smith",
      "email": "john.smith@example.com",
      "phone": "+1-555-0123",
      "nationality": "United States",
      "address": "123 Main Street",
      "city": "New York",
      "country": "United States"
    },

    // Itinerary summary
    "itinerary": {
      "id": "iti456",
      "name": "7-Day Serengeti & Ngorongoro Safari",
      "code": "ITI-7D6N-001",
      "status": "PUBLISHED",
      "statusDisplayName": "Published",
      "tripType": "PRIVATE",
      "tripTypeDisplayName": "Private Safari",
      "budgetCategory": "LUXURY",
      "budgetCategoryDisplayName": "Luxury",
      "totalDays": 7,
      "totalNights": 6,
      "description": "An unforgettable safari experience...",
      "startLocation": "Arusha",
      "endLocation": "Arusha"
    },

    // Quote line items with multi-currency prices
    "items": [
      {
        "id": "item001",
        "itemType": "ACCOMMODATION",
        "itemTypeDisplayName": "Accommodation",
        "itemName": "Four Seasons Safari Lodge - Savannah Room (3 nights)",
        "description": "Luxury accommodation with full board, including all meals and selected beverages",
        "displayOrder": 1,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 2,
            "unitPrice": 450.00,
            "totalPrice": 2700.00,
            "breakdown": "2 rooms x 3 nights x $450/night",
            "formattedUnitPrice": "$ 450.00",
            "formattedTotalPrice": "$ 2,700.00"
          },
          {
            "currency": "EUR",
            "quantity": 2,
            "unitPrice": 420.00,
            "totalPrice": 2520.00,
            "breakdown": "2 rooms x 3 nights x €420/night",
            "formattedUnitPrice": "€ 420.00",
            "formattedTotalPrice": "€ 2,520.00"
          }
        ]
      },
      {
        "id": "item002",
        "itemType": "PARK_FEE",
        "itemTypeDisplayName": "Park Fee",
        "itemName": "Serengeti National Park - Entry Fees",
        "description": "Park entry fees for 2 adults and 2 children (3 days)",
        "displayOrder": 2,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 12,
            "unitPrice": 60.00,
            "totalPrice": 720.00,
            "breakdown": "4 people x 3 days x $60/person/day",
            "formattedUnitPrice": "$ 60.00",
            "formattedTotalPrice": "$ 720.00"
          }
        ]
      },
      {
        "id": "item003",
        "itemType": "ACTIVITY",
        "itemTypeDisplayName": "Activity",
        "itemName": "Hot Air Balloon Safari",
        "description": "Sunrise balloon flight over Serengeti with champagne breakfast",
        "displayOrder": 3,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 4,
            "unitPrice": 550.00,
            "totalPrice": 2200.00,
            "breakdown": "4 people x $550/person",
            "formattedUnitPrice": "$ 550.00",
            "formattedTotalPrice": "$ 2,200.00"
          }
        ]
      },
      {
        "id": "item004",
        "itemType": "TRANSPORT",
        "itemTypeDisplayName": "Transport",
        "itemName": "4x4 Safari Vehicle with Pop-up Roof",
        "description": "Private 4x4 Land Cruiser with professional driver-guide for entire safari",
        "displayOrder": 4,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 7,
            "unitPrice": 200.00,
            "totalPrice": 1400.00,
            "breakdown": "7 days x $200/day",
            "formattedUnitPrice": "$ 200.00",
            "formattedTotalPrice": "$ 1,400.00"
          }
        ]
      }
    ],

    // Financial totals by currency
    "subtotals": [
      {
        "currency": "USD",
        "totalPrice": 8520.00,
        "formattedTotalPrice": "$ 8,520.00"
      },
      {
        "currency": "EUR",
        "totalPrice": 2520.00,
        "formattedTotalPrice": "€ 2,520.00"
      }
    ],

    "taxes": [
      {
        "currency": "USD",
        "totalPrice": 1533.60,
        "formattedTotalPrice": "$ 1,533.60"
      },
      {
        "currency": "EUR",
        "totalPrice": 453.60,
        "formattedTotalPrice": "€ 453.60"
      }
    ],

    "discounts": [
      {
        "currency": "USD",
        "totalPrice": 426.00,
        "formattedTotalPrice": "$ 426.00"
      },
      {
        "currency": "EUR",
        "totalPrice": 126.00,
        "formattedTotalPrice": "€ 126.00"
      }
    ],

    "grandTotals": [
      {
        "currency": "USD",
        "totalPrice": 9627.60,
        "formattedTotalPrice": "$ 9,627.60"
      },
      {
        "currency": "EUR",
        "totalPrice": 2847.60,
        "formattedTotalPrice": "€ 2,847.60"
      }
    ]
  },
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

### Error Responses

#### Invalid ID (400 Bad Request)

```json
{
  "status": 400,
  "message": "Invalid quote ID",
  "errorCode": "INVALID_QUOTE_ID",
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

#### Not Found (404 Not Found)

```json
{
  "status": 404,
  "message": "Quote not found",
  "errorCode": "QUOTE_NOT_FOUND",
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

#### Server Error (500 Internal Server Error)

```json
{
  "status": 500,
  "message": "Failed to fetch full quote",
  "errorCode": "FULL_QUOTE_FETCH_FAILED",
  "timestamp": "2024-01-20T15:00:00.000Z"
}
```

## Data Structure Hierarchy

```
FullQuoteDTO
├── Quote fields (id, quoteCode, title, status, pricing, dates, etc.)
├── Summary statistics
│   ├── totalItemsCount
│   └── totalCurrenciesCount
├── customer: CustomerDTO
│   └── Customer information (name, email, phone, address)
├── itinerary: ItineraryDTO
│   └── Itinerary summary (name, code, trip details)
├── items: List<QuoteItemDTO>
│   └── QuoteItemDTO
│       ├── Item details (type, name, description)
│       └── prices: List<PriceDTO>  [Multi-currency prices]
│           └── PriceDTO (currency, quantity, unitPrice, totalPrice)
├── subtotals: List<PriceDTO>  [By currency]
├── taxes: List<PriceDTO>  [By currency]
├── discounts: List<PriceDTO>  [By currency]
└── grandTotals: List<PriceDTO>  [By currency]
```

## Field Descriptions

### Quote Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated unique identifier |
| `quoteCode` | String | Auto-generated quote code (e.g., QTE-2024-001) |
| `title` | String | Quote title/name |
| `description` | String | Optional description or summary |
| `status` | Enum | DRAFT, PENDING_REVIEW, APPROVED, SENT, ACCEPTED, REJECTED, EXPIRED, CANCELLED |
| `statusDisplayName` | String | Human-readable status |
| `version` | Integer | Version number for tracking revisions |
| `versionNotes` | String | Notes about this version |
| `isStoRate` | Boolean | Single Tourism Operator rate flag |
| `taxPercentage` | Decimal | Tax percentage (e.g., 18.00 for 18% VAT) |
| `discountPercentage` | Decimal | Discount percentage applied |
| `discountReason` | String | Reason for discount |
| `isValid` | Boolean | Whether quote is currently valid |
| `validityStatusMessage` | String | Human-readable validity status |

### CustomerDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated customer ID |
| `customerName` | String | Customer display name (full name or company) |
| `email` | String | Primary email address |
| `phone` | String | Primary phone number |
| `nationality` | String | Customer nationality |
| `address` | String | Street address |
| `city` | String | City |
| `country` | String | Country |

### ItineraryDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated itinerary ID |
| `name` | String | Itinerary name |
| `code` | String | Itinerary code |
| `status` | String | Itinerary status |
| `statusDisplayName` | String | Human-readable status |
| `tripType` | String | Trip type (PRIVATE, GROUP, etc.) |
| `tripTypeDisplayName` | String | Human-readable trip type |
| `budgetCategory` | String | Budget category (LUXURY, MID_RANGE, etc.) |
| `budgetCategoryDisplayName` | String | Human-readable budget category |
| `totalDays` | Integer | Total number of days |
| `totalNights` | Integer | Total number of nights |
| `description` | String | Itinerary description |
| `startLocation` | String | Starting point |
| `endLocation` | String | Ending point |

### QuoteItemDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated item ID |
| `itemType` | Enum | ACCOMMODATION, PARK_FEE, ACTIVITY, TRANSPORT, GUIDE, MEAL, EQUIPMENT, INSURANCE, OTHER |
| `itemTypeDisplayName` | String | Human-readable item type |
| `itemName` | String | Display name of the item |
| `description` | String | Detailed description |
| `displayOrder` | Integer | Sort order for display |
| `isActive` | Boolean | Whether item is active |
| `prices` | List | List of prices in different currencies |

### PriceDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `currency` | String | Currency code (USD, EUR, TZS, etc.) |
| `quantity` | Integer | Quantity/count for calculation |
| `unitPrice` | Decimal | Price per unit |
| `totalPrice` | Decimal | Total price (quantity × unitPrice) |
| `breakdown` | String | Human-readable breakdown explanation |
| `formattedUnitPrice` | String | Formatted unit price with currency symbol |
| `formattedTotalPrice` | String | Formatted total price with currency symbol |

## Usage Examples

### cURL

```bash
curl -X GET \
  'https://api.kabengosafaris.com/api/quotes/xyz789abc/full' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json'
```

### JavaScript (Fetch)

```javascript
const response = await fetch('/api/quotes/xyz789abc/full', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log(data.data.quoteCode); // "QTE-2024-001"
console.log(data.data.customer.customerName); // "Mr. John Smith"
console.log(data.data.items.length); // 15
console.log(data.data.grandTotals); // Array of totals by currency

// Calculate total in USD
const usdTotal = data.data.grandTotals.find(t => t.currency === 'USD');
console.log(usdTotal.formattedTotalPrice); // "$ 9,627.60"
```

### Python (Requests)

```python
import requests

response = requests.get(
    'https://api.kabengosafaris.com/api/quotes/xyz789abc/full',
    headers={
        'Authorization': f'Bearer {access_token}',
        'Content-Type': 'application/json'
    }
)

data = response.json()
quote = data['data']

print(f"Quote: {quote['quoteCode']}")
print(f"Customer: {quote['customer']['customerName']}")
print(f"Items: {quote['totalItemsCount']}")
print(f"Currencies: {quote['totalCurrenciesCount']}")

# Print all grand totals
for total in quote['grandTotals']:
    print(f"Total in {total['currency']}: {total['formattedTotalPrice']}")
```

## Multi-Currency Pricing

The quote system supports multi-currency pricing to accommodate passengers with different nationality categories:

- Each quote item can have multiple prices in different currencies
- Financial totals (subtotals, taxes, discounts, grand totals) are calculated separately for each currency
- Common currencies: USD, EUR, TZS (Tanzanian Shilling), KES (Kenyan Shilling), GBP, ZAR
- Currency symbols are automatically applied in formatted prices

### Tax and Discount Calculation

```
For each currency:
  Subtotal = Sum of all item total prices in that currency
  Tax = Subtotal × (taxPercentage / 100)
  Discount = Subtotal × (discountPercentage / 100)
  Grand Total = Subtotal + Tax - Discount
```

## Quote Status Workflow

| Status | Description |
|--------|-------------|
| `DRAFT` | Quote is being prepared |
| `PENDING_REVIEW` | Awaiting internal review/approval |
| `APPROVED` | Internally approved, ready to send |
| `SENT` | Sent to customer |
| `ACCEPTED` | Customer accepted the quote |
| `REJECTED` | Customer rejected the quote |
| `EXPIRED` | Quote validity period has passed |
| `CANCELLED` | Quote was cancelled |

## Notes

- All null fields are excluded from the response (using `@JsonInclude(JsonInclude.Include.NON_NULL)`)
- Items are ordered by `displayOrder` (ascending)
- Prices within items can be in multiple currencies
- Financial totals are grouped and calculated separately for each currency
- This endpoint returns all data in a single request - ideal for:
  - Generating PDF quotes
  - Displaying complete quote details
  - Exporting quote data
- For listing quotes without nested data, use `GET /api/quotes` instead
- The `isValid` field indicates if the quote is within its validity period
- Version tracking allows maintaining multiple versions of the same quote

## Performance Considerations

- This endpoint performs multiple database queries to fetch all nested data
- Response size can be large for quotes with many items or currencies
- Consider caching the response for frequently accessed quotes
- Use pagination on the list endpoint (`GET /api/quotes`) for browsing quotes
