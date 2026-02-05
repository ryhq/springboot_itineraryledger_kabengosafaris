# Full Invoice API Documentation

## Overview

This API endpoint retrieves a complete invoice with all its nested data in a single request. It provides a comprehensive view of the entire invoice structure including customer information, safari summary, line items with multi-currency pricing, financial totals (subtotals, taxes, discounts, grand totals, amounts paid, balances), and active bank accounts for payment instructions.

## Endpoint

```
GET /api/invoices/{idObfuscated}/full
```

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `idObfuscated` | String | Yes | The obfuscated invoice ID |

### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | Bearer {token} | Yes |
| `Content-Type` | application/json | Yes |

### Required Permission

```
PERM_READ_INVOICE
```

## Response Structure

### Success Response (200 OK)

```json
{
  "status": 200,
  "message": "Full invoice retrieved successfully",
  "data": {
    // Invoice base fields
    "id": "inv789xyz",
    "invoiceCode": "INV-2024-001",
    "title": "7-Day Serengeti Safari - Smith Family",
    "description": "Complete safari package invoice including accommodation, activities, and park fees",
    "status": "SENT",
    "statusDisplayName": "Sent",
    "paymentStatus": "PARTIALLY_PAID",
    "paymentStatusDisplayName": "Partially Paid",

    // Pricing details
    "taxPercentage": 18.00,
    "discountPercentage": 5.00,
    "discountReason": "Repeat customer discount",

    // Dates
    "issueDate": "2024-02-01",
    "dueDate": "2024-03-01",
    "sentDate": "2024-02-01",
    "paidDate": null,
    "isOverdue": false,

    // Notes
    "customerNotes": "Payment due within 30 days. Late payment fees may apply.",
    "internalNotes": "VIP client - priority follow-up",
    "paymentTerms": "30 days net. 50% deposit required 60 days before safari start date. Balance due 14 days before departure.",

    // Audit
    "isActive": true,
    "createdByName": "alice.agent",
    "updatedByName": "bob.finance",
    "createdAt": "2024-02-01T09:00:00",
    "updatedAt": "2024-02-15T14:30:00",

    // Summary statistics
    "totalLineItemsCount": 12,
    "totalCurrenciesCount": 2,
    "currencies": ["USD", "EUR"],

    // Customer information
    "customer": {
      "id": "cust456",
      "customerCode": "CUST-2023-042",
      "customerName": "Mr. John Smith",
      "email": "john.smith@example.com",
      "phone": "+1-555-0123",
      "nationality": "United States",
      "address": "123 Main Street",
      "city": "New York",
      "country": "United States"
    },

    // Safari summary
    "safari": {
      "id": "sfr789",
      "name": "Serengeti & Ngorongoro Discovery",
      "code": "SAF-2024-067",
      "state": "CONFIRMED",
      "stateDisplayName": "Confirmed",
      "totalDays": 7,
      "totalNights": 6,
      "startDate": "2024-06-15",
      "endDate": "2024-06-21",
      "description": "Luxury safari experience through Tanzania's most iconic parks",
      "startLocation": "Arusha",
      "endLocation": "Arusha"
    },

    // Invoice line items with multi-currency prices
    "lineItems": [
      {
        "id": "line001",
        "itemType": "ACCOMMODATION",
        "itemTypeDisplayName": "Accommodation",
        "itemName": "Four Seasons Safari Lodge - Deluxe Suite (3 nights)",
        "description": "Full board accommodation including all meals and house beverages",
        "displayOrder": 1,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 2,
            "unitPrice": 500.00,
            "totalPrice": 3000.00,
            "breakdown": "2 rooms x 3 nights x $500/night",
            "formattedUnitPrice": "$500.00",
            "formattedTotalPrice": "$3,000.00"
          },
          {
            "currency": "EUR",
            "quantity": 2,
            "unitPrice": 465.00,
            "totalPrice": 2790.00,
            "breakdown": "2 rooms x 3 nights x €465/night",
            "formattedUnitPrice": "€465.00",
            "formattedTotalPrice": "€2,790.00"
          }
        ]
      },
      {
        "id": "line002",
        "itemType": "PARK_FEE",
        "itemTypeDisplayName": "Park Fee",
        "itemName": "Serengeti National Park Entry Fees",
        "description": "Park conservation fees for 2 adults and 2 children (3 days)",
        "displayOrder": 2,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 12,
            "unitPrice": 70.00,
            "totalPrice": 840.00,
            "breakdown": "4 people x 3 days x $70/person/day",
            "formattedUnitPrice": "$70.00",
            "formattedTotalPrice": "$840.00"
          }
        ]
      },
      {
        "id": "line003",
        "itemType": "ACTIVITY",
        "itemTypeDisplayName": "Activity",
        "itemName": "Hot Air Balloon Safari with Champagne Breakfast",
        "description": "Sunrise balloon flight over Serengeti plains",
        "displayOrder": 3,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 4,
            "unitPrice": 599.00,
            "totalPrice": 2396.00,
            "breakdown": "4 people x $599/person",
            "formattedUnitPrice": "$599.00",
            "formattedTotalPrice": "$2,396.00"
          }
        ]
      },
      {
        "id": "line004",
        "itemType": "TRANSPORT",
        "itemTypeDisplayName": "Transport",
        "itemName": "Private 4x4 Safari Vehicle with Pop-up Roof",
        "description": "Land Cruiser with professional driver-guide for entire safari duration",
        "displayOrder": 4,
        "isActive": true,
        "prices": [
          {
            "currency": "USD",
            "quantity": 7,
            "unitPrice": 250.00,
            "totalPrice": 1750.00,
            "breakdown": "7 days x $250/day",
            "formattedUnitPrice": "$250.00",
            "formattedTotalPrice": "$1,750.00"
          }
        ]
      }
    ],

    // Financial totals by currency
    "subtotals": [
      {
        "currency": "USD",
        "totalPrice": 9986.00,
        "formattedTotalPrice": "$9,986.00"
      },
      {
        "currency": "EUR",
        "totalPrice": 2790.00,
        "formattedTotalPrice": "€2,790.00"
      }
    ],

    "taxes": [
      {
        "currency": "USD",
        "totalPrice": 1797.48,
        "formattedTotalPrice": "$1,797.48"
      },
      {
        "currency": "EUR",
        "totalPrice": 502.20,
        "formattedTotalPrice": "€502.20"
      }
    ],

    "discounts": [
      {
        "currency": "USD",
        "totalPrice": 499.30,
        "formattedTotalPrice": "$499.30"
      },
      {
        "currency": "EUR",
        "totalPrice": 139.50,
        "formattedTotalPrice": "€139.50"
      }
    ],

    "grandTotals": [
      {
        "currency": "USD",
        "totalPrice": 11284.18,
        "formattedTotalPrice": "$11,284.18"
      },
      {
        "currency": "EUR",
        "totalPrice": 3152.70,
        "formattedTotalPrice": "€3,152.70"
      }
    ],

    "amountsPaid": [
      {
        "currency": "USD",
        "totalPrice": 5000.00,
        "formattedTotalPrice": "$5,000.00"
      },
      {
        "currency": "EUR",
        "totalPrice": 0.00,
        "formattedTotalPrice": "€0.00"
      }
    ],

    "balances": [
      {
        "currency": "USD",
        "totalPrice": 6284.18,
        "formattedTotalPrice": "$6,284.18"
      },
      {
        "currency": "EUR",
        "totalPrice": 3152.70,
        "formattedTotalPrice": "€3,152.70"
      }
    ],

    // Bank accounts for payment (active accounts matching invoice currencies)
    "bankAccounts": [
      {
        "accountName": "Kabengo Safaris Ltd - USD Operations",
        "accountHolderName": "Kabengo Safaris Limited",
        "bankName": "Standard Chartered Bank Tanzania",
        "bankBranch": "Dar es Salaam Branch",
        "branchAddress": "Ohio Street, Plot 12/13",
        "branchCity": "Dar es Salaam",
        "branchCountry": "Tanzania",
        "accountNumber": "0123456789",
        "currency": "USD",
        "swiftBicCode": "SCBLTZTZ",
        "iban": null,
        "routingNumber": null,
        "sortCode": null,
        "intermediaryBankName": "Standard Chartered Bank New York",
        "intermediarySwiftCode": "SCBLUS33",
        "invoiceDisplayNotes": "For international USD wire transfers, please use SWIFT transfer with intermediary bank details."
      },
      {
        "accountName": "Kabengo Safaris Ltd - EUR Operations",
        "accountHolderName": "Kabengo Safaris Limited",
        "bankName": "CRDB Bank PLC",
        "bankBranch": "Corporate Branch",
        "branchAddress": "Azikiwe Street",
        "branchCity": "Dar es Salaam",
        "branchCountry": "Tanzania",
        "accountNumber": "9876543210",
        "currency": "EUR",
        "swiftBicCode": "CORUTZTZ",
        "iban": "TZ12345678901234567890",
        "routingNumber": null,
        "sortCode": null,
        "intermediaryBankName": null,
        "intermediarySwiftCode": null,
        "invoiceDisplayNotes": "EUR payments via SEPA or SWIFT accepted."
      }
    ]
  },
  "timestamp": "2024-02-20T10:00:00.000Z"
}
```

### Error Responses

#### Invalid ID (400 Bad Request)

```json
{
  "status": 400,
  "message": "Invalid invoice ID",
  "errorCode": "INVALID_INVOICE_ID",
  "timestamp": "2024-02-20T10:00:00.000Z"
}
```

#### Not Found (404 Not Found)

```json
{
  "status": 404,
  "message": "Invoice not found",
  "errorCode": "INVOICE_NOT_FOUND",
  "timestamp": "2024-02-20T10:00:00.000Z"
}
```

#### Server Error (500 Internal Server Error)

```json
{
  "status": 500,
  "message": "Failed to fetch full invoice",
  "errorCode": "FULL_INVOICE_FETCH_FAILED",
  "timestamp": "2024-02-20T10:00:00.000Z"
}
```

## Data Structure Hierarchy

```
FullInvoiceDTO
├── Invoice fields (id, invoiceCode, title, status, paymentStatus, dates, pricing)
├── Summary statistics
│   ├── totalLineItemsCount
│   ├── totalCurrenciesCount
│   └── currencies (array of currency codes)
├── customer: CustomerDTO
│   └── Customer information (name, code, email, phone, address, nationality)
├── safari: SafariDTO
│   └── Safari summary (name, code, state, dates, locations)
├── lineItems: List<LineItemDTO>
│   └── LineItemDTO
│       ├── Item details (type, name, description, displayOrder)
│       └── prices: List<PriceDTO>  [Multi-currency prices]
│           └── PriceDTO (currency, quantity, unitPrice, totalPrice, breakdown)
├── subtotals: List<PriceDTO>  [By currency]
├── taxes: List<PriceDTO>  [By currency]
├── discounts: List<PriceDTO>  [By currency]
├── grandTotals: List<PriceDTO>  [By currency]
├── amountsPaid: List<PriceDTO>  [By currency]
├── balances: List<PriceDTO>  [By currency]
└── bankAccounts: List<BankAccountDTO>  [Active accounts for invoice currencies]
    └── BankAccountDTO (account details, bank info, international codes)
```

## Field Descriptions

### Invoice Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated unique identifier |
| `invoiceCode` | String | Auto-generated invoice code (e.g., INV-2024-001) |
| `title` | String | Invoice title/name |
| `description` | String | Optional description or summary |
| `status` | Enum | DRAFT, SENT, VIEWED, OVERDUE, PAID, PARTIALLY_PAID, CANCELLED, VOID |
| `statusDisplayName` | String | Human-readable status |
| `paymentStatus` | Enum | UNPAID, PARTIALLY_PAID, PAID, OVERDUE, REFUNDED, CANCELLED |
| `paymentStatusDisplayName` | String | Human-readable payment status |
| `taxPercentage` | Decimal | Tax percentage (e.g., 18.00 for 18% VAT) |
| `discountPercentage` | Decimal | Discount percentage applied |
| `discountReason` | String | Reason for discount |
| `issueDate` | Date | Invoice issue date |
| `dueDate` | Date | Payment due date |
| `sentDate` | Date | Date invoice was sent to customer |
| `paidDate` | Date | Date invoice was fully paid |
| `isOverdue` | Boolean | Whether invoice is overdue |
| `customerNotes` | String | Notes visible to customer |
| `internalNotes` | String | Internal notes (staff only) |
| `paymentTerms` | String | Payment terms and conditions |

### CustomerDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated customer ID |
| `customerCode` | String | Customer code (e.g., CUST-2023-042) |
| `customerName` | String | Customer display name (full name or company) |
| `email` | String | Primary email address |
| `phone` | String | Primary phone number |
| `nationality` | String | Customer nationality |
| `address` | String | Street address |
| `city` | String | City |
| `country` | String | Country |

### SafariDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated safari ID |
| `name` | String | Safari name |
| `code` | String | Safari code |
| `state` | String | Safari state (PLANNING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED) |
| `stateDisplayName` | String | Human-readable state |
| `totalDays` | Integer | Total number of days |
| `totalNights` | Integer | Total number of nights |
| `startDate` | Date | Safari start date |
| `endDate` | Date | Safari end date |
| `description` | String | Safari description |
| `startLocation` | String | Starting point |
| `endLocation` | String | Ending point |

### LineItemDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Obfuscated line item ID |
| `itemType` | Enum | ACCOMMODATION, PARK_FEE, ACTIVITY, TRANSPORT, GUIDE, MEAL, EQUIPMENT, INSURANCE, OTHER |
| `itemTypeDisplayName` | String | Human-readable item type |
| `itemName` | String | Display name of the item |
| `description` | String | Detailed description |
| `displayOrder` | Integer | Sort order for display |
| `isActive` | Boolean | Whether line item is active |
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

### BankAccountDTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `accountName` | String | Display name for the account |
| `accountHolderName` | String | Legal name of account holder |
| `bankName` | String | Bank name |
| `bankBranch` | String | Bank branch name |
| `branchAddress` | String | Branch physical address |
| `branchCity` | String | Branch city |
| `branchCountry` | String | Branch country |
| `accountNumber` | String | Bank account number |
| `currency` | String | Account currency (matches invoice currencies) |
| `swiftBicCode` | String | SWIFT/BIC code for international transfers |
| `iban` | String | IBAN for European/international transfers |
| `routingNumber` | String | US ACH routing number |
| `sortCode` | String | UK sort code |
| `intermediaryBankName` | String | Intermediary/correspondent bank name |
| `intermediarySwiftCode` | String | Intermediary bank SWIFT code |
| `invoiceDisplayNotes` | String | Customer-facing payment instructions |

## Usage Examples

### cURL

```bash
curl -X GET \
  'https://api.kabengosafaris.com/api/invoices/inv789xyz/full' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json'
```

### JavaScript (Fetch)

```javascript
const response = await fetch('/api/invoices/inv789xyz/full', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
const invoice = data.data;

console.log(invoice.invoiceCode); // "INV-2024-001"
console.log(invoice.customer.customerName); // "Mr. John Smith"
console.log(invoice.lineItems.length); // 12
console.log(invoice.paymentStatus); // "PARTIALLY_PAID"

// Get balance due in USD
const usdBalance = invoice.balances.find(b => b.currency === 'USD');
console.log(usdBalance.formattedTotalPrice); // "$6,284.18"

// Get bank account for USD payments
const usdBankAccount = invoice.bankAccounts.find(ba => ba.currency === 'USD');
console.log(usdBankAccount.bankName); // "Standard Chartered Bank Tanzania"
console.log(usdBankAccount.swiftBicCode); // "SCBLTZTZ"
```

### Python (Requests)

```python
import requests

response = requests.get(
    'https://api.kabengosafaris.com/api/invoices/inv789xyz/full',
    headers={
        'Authorization': f'Bearer {access_token}',
        'Content-Type': 'application/json'
    }
)

data = response.json()
invoice = data['data']

print(f"Invoice: {invoice['invoiceCode']}")
print(f"Customer: {invoice['customer']['customerName']}")
print(f"Status: {invoice['statusDisplayName']}")
print(f"Payment Status: {invoice['paymentStatusDisplayName']}")
print(f"Line Items: {invoice['totalLineItemsCount']}")
print(f"Currencies: {invoice['totalCurrenciesCount']}")

# Print all balances
for balance in invoice['balances']:
    print(f"Balance in {balance['currency']}: {balance['formattedTotalPrice']}")

# Print bank account details for payment
print("\nPayment Bank Accounts:")
for bank_account in invoice['bankAccounts']:
    print(f"  {bank_account['currency']}: {bank_account['bankName']}")
    print(f"  Account: {bank_account['accountNumber']}")
    print(f"  SWIFT: {bank_account['swiftBicCode']}")
    if bank_account['invoiceDisplayNotes']:
        print(f"  Notes: {bank_account['invoiceDisplayNotes']}")
    print()
```

## Multi-Currency Pricing and Payments

The invoice system supports multi-currency pricing and payment tracking:

- Each line item can have multiple prices in different currencies
- Financial totals (subtotals, taxes, discounts, grand totals, amounts paid, balances) are calculated separately for each currency
- Common currencies: USD, EUR, TZS (Tanzanian Shilling), KES (Kenyan Shilling), GBP, ZAR
- Currency symbols are automatically applied in formatted prices
- Balances track outstanding amounts per currency

### Financial Calculation

```
For each currency:
  Subtotal = Sum of all line item total prices in that currency
  Tax = Subtotal × (taxPercentage / 100)
  Discount = Subtotal × (discountPercentage / 100)
  Grand Total = Subtotal + Tax - Discount
  Balance = Grand Total - Amount Paid
```

## Bank Account Integration

The `bankAccounts` field contains active bank accounts that match the currencies present in the invoice:

- **Dynamic Selection**: Automatically includes only bank accounts with currencies that match the invoice grand totals
- **Active Only**: Only active (`isActive = true`) bank accounts are included
- **Payment Instructions**: Each bank account includes customer-facing notes in `invoiceDisplayNotes` field
- **International Support**: Includes SWIFT/BIC codes, IBAN, routing numbers, and intermediary bank details
- **Multi-Currency**: If invoice has USD and EUR line items, both USD and EUR bank accounts will be included

### Use Cases for Bank Accounts
- PDF invoice generation (display payment instructions)
- Customer payment portal (show relevant payment methods)
- Accounting reconciliation (match incoming payments to correct accounts)
- Multi-currency payment processing

## Invoice Status Workflow

| Status | Description |
|--------|-------------|
| `DRAFT` | Invoice is being prepared |
| `SENT` | Invoice sent to customer |
| `VIEWED` | Customer has viewed the invoice |
| `OVERDUE` | Payment due date has passed |
| `PAID` | Invoice fully paid |
| `PARTIALLY_PAID` | Partial payment received |
| `CANCELLED` | Invoice cancelled |
| `VOID` | Invoice voided (accounting adjustment) |

## Payment Status

| Payment Status | Description |
|----------------|-------------|
| `UNPAID` | No payment received |
| `PARTIALLY_PAID` | Partial payment received (balance > 0) |
| `PAID` | Fully paid (balance = 0) |
| `OVERDUE` | Payment overdue |
| `REFUNDED` | Payment refunded to customer |
| `CANCELLED` | Payment cancelled |

## Notes

- All null fields are excluded from the response (using `@JsonInclude(JsonInclude.Include.NON_NULL)`)
- Line items are ordered by `displayOrder` (ascending)
- Prices within line items can be in multiple currencies
- Financial totals are grouped and calculated separately for each currency
- Bank accounts are dynamically included based on invoice currencies
- The `isOverdue` field is computed based on current date vs. due date
- This endpoint returns all data in a single request - ideal for:
  - Generating PDF invoices with payment instructions
  - Displaying complete invoice details to customers
  - Exporting invoice data
  - Payment processing systems
- For listing invoices without nested data, use `GET /api/invoices` instead
- Customer and Safari references are optional (can be null)

## Performance Considerations

- This endpoint performs multiple database queries to fetch all nested data
- Response size can be large for invoices with many line items or currencies
- Consider caching the response for frequently accessed invoices
- Bank account queries are filtered by currency to minimize data transfer
- Use pagination on the list endpoint (`GET /api/invoices`) for browsing invoices

## Related Endpoints

- `GET /api/invoices` - List all invoices (paginated, without nested data)
- `GET /api/invoices/{id}` - Get single invoice (basic data only)
- `POST /api/invoices` - Create new invoice
- `PUT /api/invoices/{id}` - Update invoice
- `POST /api/invoices/{id}/recalculate-totals` - Recalculate invoice totals
- `DELETE /api/invoices` - Delete invoices (bulk)
