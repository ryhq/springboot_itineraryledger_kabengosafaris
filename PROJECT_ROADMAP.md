# Kabengo Safaris - Project Roadmap

## Project Overview

**Kabengo Safaris** is a comprehensive Safari Tourism Management System designed to handle the complete lifecycle of safari bookings - from initial customer inquiry through trip completion and financial settlement.

---

## Current State (Implemented)

### Core Modules

| Module | Status | Description |
|--------|--------|-------------|
| **Itinerary** | ✅ Complete | Template-based safari itinerary management with days, parks, activities, accommodations, and passenger configurations |
| **Safari** | ✅ Complete | Safari booking instance with 25+ state lifecycle, phase tracking, and day-by-day execution |
| **Accommodation** | ✅ Complete | Lodging management with room types, board types, seasonal rates, and multi-branch support |
| **Park** | ✅ Complete | National parks and reserves with activities, tariffs, and visitor information |
| **Activity** | ✅ Complete | Standalone activities catalog with pricing and requirements |
| **Tariff** | ✅ Complete | Dynamic pricing system for parks, activities, and accommodations |
| **Pax Categories** | ✅ Complete | Passenger categorization by age and nationality for differential pricing |
| **Cost Estimation** | ✅ Complete | Quick budget calculation API for itineraries before formal quotation |

### Supporting Infrastructure

| Module | Status | Description |
|--------|--------|-------------|
| **User Management** | ✅ Complete | Authentication, registration, MFA support |
| **Permissions & Roles** | ✅ Complete | Role-based access control with dynamic permissions |
| **Audit Logging** | ✅ Complete | Comprehensive audit trail with configurable policies |
| **Email Events** | ✅ Complete | Email template management and event tracking |
| **PDF Generation** | ✅ Complete | Template-based PDF generation for documents |
| **Seasons** | ✅ Complete | Global and accommodation-specific seasonal configurations |

### Cost Estimation API (Recently Implemented)

**Purpose:** Quick budget calculation for itineraries before creating formal quotations.

**Endpoint:** `GET /api/itineraries/{id}/estimate-cost`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| startDate | LocalDate | Today | Start date for season determination |
| useStoRate | Boolean | true | Use STO (Special Tour Operator) rates vs. rack rates |
| currency | String | USD | Preferred output currency |

**Response Structure:**
```json
{
  "itineraryId": "abc123",
  "itineraryCode": "ITI-00010126",
  "itineraryName": "7-Day Serengeti Safari",
  "totalDays": 7,
  "totalNights": 6,
  "startDate": "2026-02-15",
  "endDate": "2026-02-21",
  "seasonName": "High Season",
  "totalPax": 4,
  "paxBreakdown": [
    {"nationCategoryName": "Non-Resident", "ageCategoryName": "Adult", "count": 2},
    {"nationCategoryName": "Non-Resident", "ageCategoryName": "Child", "count": 2}
  ],
  "parkFeeCosts": {
    "total": 2400.00,
    "currency": "USD",
    "itemCount": 5,
    "items": [...]
  },
  "accommodationCosts": {
    "total": 3600.00,
    "currency": "USD",
    "itemCount": 6,
    "items": [...]
  },
  "activityCosts": {...},
  "subtotal": 6000.00,
  "perPersonCost": 1500.00,
  "currency": "USD",
  "rateType": "STO",
  "hasIncompleteRates": false,
  "warnings": null,
  "estimatedAt": "2026-01-20T15:30:00"
}
```

**Cost Calculation Logic:**
1. **Park Fees:** Based on tariff charging basis (PER_PERSON, PER_VEHICLE, PER_GROUP)
   - PER_PERSON: Rate × pax count for each nationality/age category
   - PER_VEHICLE/GROUP: Uses highest priority nationality to determine rate
2. **Accommodation:** Rate × room count per night
3. **Season Detection:** Automatically determines applicable season from start date

**Files:**
- [ItineraryCostEstimationDTO.java](src/main/java/com/itineraryledger/kabengosafaris/Itinerary/DTOs/ItineraryCostEstimationDTO.java)
- [ItineraryCostEstimationService.java](src/main/java/com/itineraryledger/kabengosafaris/Itinerary/Services/ItineraryCostEstimationService.java)
- [ItineraryController.java](src/main/java/com/itineraryledger/kabengosafaris/Itinerary/Controller/ItineraryController.java) (endpoint)

---

## Modules To Be Implemented

### Phase 1: Customer & Quotation (Priority: HIGH)

#### 1.1 Customer Module

**Purpose:** Centralized customer/client management - every safari booking needs a customer reference.

**Entity: Customer**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| code | String | Auto-generated (e.g., CUS-00010126) |
| customerType | Enum | INDIVIDUAL, CORPORATE, TRAVEL_AGENT |
| title | String | Mr, Mrs, Ms, Dr, etc. |
| firstName | String | Required for individuals |
| lastName | String | Required for individuals |
| companyName | String | Required for corporate/agents |
| email | String | Primary email (unique) |
| phone | String | Primary phone |
| alternateEmail | String | Secondary email |
| alternatePhone | String | Secondary phone |
| nationality | String | Country of citizenship |
| residency | String | Country of residence |
| passportNumber | String | Optional, for visa processing |
| passportExpiry | LocalDate | Passport expiration date |
| dateOfBirth | LocalDate | For age-based pricing |
| address | String | Street address |
| city | String | City |
| state | String | State/Province |
| country | String | Country |
| postalCode | String | Postal/ZIP code |
| preferredLanguage | String | Communication language |
| preferredCurrency | String | Billing currency (USD, EUR, etc.) |
| source | Enum | HOW_FOUND (WEBSITE, REFERRAL, AGENT, SOCIAL_MEDIA, TRADE_SHOW, REPEAT) |
| referredBy | String | Referral source name |
| dietaryRequirements | TEXT | Special dietary needs |
| medicalConditions | TEXT | Relevant medical info |
| specialRequests | TEXT | General preferences |
| interests | TEXT | Safari interests (JSON array: wildlife, photography, birding, etc.) |
| internalNotes | TEXT | Staff-only notes |
| isVip | Boolean | VIP customer flag |
| isBlacklisted | Boolean | Problem customer flag |
| blacklistReason | TEXT | Reason if blacklisted |
| totalBookings | Integer | Computed: number of safaris |
| totalSpent | BigDecimal | Computed: lifetime value |
| lastBookingDate | LocalDateTime | Most recent booking |
| createdAt | LocalDateTime | Record creation |
| updatedAt | LocalDateTime | Last update |
| createdBy | Long | User who created |

**Related Entities:**

- **CustomerContact** - Additional contacts (spouse, assistant, etc.)
- **CustomerDocument** - Passport scans, visa copies, travel insurance
- **CustomerNote** - Communication history and notes

**Enums:**
- `CustomerType`: INDIVIDUAL, CORPORATE, TRAVEL_AGENT
- `CustomerSource`: WEBSITE, REFERRAL, AGENT, SOCIAL_MEDIA, TRADE_SHOW, REPEAT, OTHER

**API Endpoints:**
```
POST   /api/customers                    - Create customer
GET    /api/customers                    - List with filters
GET    /api/customers/{id}               - Get by ID
GET    /api/customers/code/{code}        - Get by code
PUT    /api/customers/{id}               - Update customer
DELETE /api/customers                    - Bulk delete
GET    /api/customers/{id}/bookings      - Customer's booking history
GET    /api/customers/{id}/documents     - Customer documents
POST   /api/customers/{id}/documents     - Upload document
POST   /api/customers/{id}/notes         - Add note
GET    /api/customers/search             - Search by name/email/phone
```

**Permissions:**
- PERM_CREATE_CUSTOMER
- PERM_READ_CUSTOMER
- PERM_UPDATE_CUSTOMER
- PERM_DELETE_CUSTOMER
- PERM_READ_CUSTOMER_DOCUMENTS
- PERM_MANAGE_CUSTOMER_DOCUMENTS

---

#### 1.2 Quotation Module

**Purpose:** Generate cost estimates from itinerary templates before confirming bookings.

**Entity: Quotation**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| code | String | Auto-generated (e.g., QUO-00010126) |
| customer | Customer | ManyToOne, required |
| itinerary | Itinerary | ManyToOne, template reference |
| name | String | Quote name/title |
| status | Enum | DRAFT, SENT, VIEWED, ACCEPTED, REJECTED, EXPIRED, REVISED |
| version | Integer | Quote revision number (starts at 1) |
| parentQuotation | Quotation | Self-reference for revisions |
| startDate | LocalDate | Proposed safari start |
| endDate | LocalDate | Proposed safari end |
| totalDays | Integer | Number of days |
| totalNights | Integer | Number of nights |
| totalPax | Integer | Total passengers |
| currency | String | Quote currency (USD, EUR, etc.) |
| exchangeRate | BigDecimal | Rate to base currency |
| subtotal | BigDecimal | Sum of line items |
| discountType | Enum | NONE, PERCENTAGE, FIXED |
| discountValue | BigDecimal | Discount amount/percentage |
| discountReason | String | Why discount applied |
| taxRate | BigDecimal | Tax percentage |
| taxAmount | BigDecimal | Calculated tax |
| totalAmount | BigDecimal | Final total |
| depositRequired | BigDecimal | Required deposit amount |
| depositPercentage | BigDecimal | Deposit as percentage |
| validUntil | LocalDate | Quote expiration date |
| sentAt | LocalDateTime | When sent to customer |
| viewedAt | LocalDateTime | When customer viewed |
| respondedAt | LocalDateTime | When customer responded |
| acceptedAt | LocalDateTime | When accepted |
| rejectedAt | LocalDateTime | When rejected |
| rejectionReason | TEXT | Why rejected |
| termsAndConditions | TEXT | Quote-specific T&C |
| inclusions | TEXT | What's included (JSON array) |
| exclusions | TEXT | What's not included (JSON array) |
| internalNotes | TEXT | Staff notes |
| customerNotes | TEXT | Notes visible to customer |
| createdAt | LocalDateTime | Record creation |
| updatedAt | LocalDateTime | Last update |
| createdBy | Long | User who created |
| assignedTo | Long | Sales person assigned |

**Related Entities:**

**QuotationPax** - Passenger configuration
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| quotation | Quotation | ManyToOne |
| nationCategory | PaxNationCategory | ManyToOne |
| ageCategory | PaxAgeCategory | ManyToOne |
| count | Integer | Number of pax |
| unitPrice | BigDecimal | Price per person |
| totalPrice | BigDecimal | count × unitPrice |

**QuotationLineItem** - Itemized pricing
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| quotation | Quotation | ManyToOne |
| dayNumber | Integer | Which day (null for overall) |
| itemType | Enum | ACCOMMODATION, PARK_FEE, ACTIVITY, TRANSPORT, GUIDE, MEAL, OTHER |
| itemName | String | Description |
| referenceId | Long | FK to actual entity |
| referenceType | String | Entity type name |
| quantity | Integer | Number of units |
| unitPrice | BigDecimal | Price per unit |
| totalPrice | BigDecimal | quantity × unitPrice |
| notes | String | Line item notes |
| sortOrder | Integer | Display order |

**QuotationDay** - Day-by-day breakdown (mirrors ItineraryDay structure)

**Enums:**
- `QuotationStatus`: DRAFT, SENT, VIEWED, ACCEPTED, REJECTED, EXPIRED, REVISED
- `DiscountType`: NONE, PERCENTAGE, FIXED
- `LineItemType`: ACCOMMODATION, PARK_FEE, ACTIVITY, TRANSPORT, GUIDE, MEAL, OTHER

**API Endpoints:**
```
POST   /api/quotations                           - Create quotation
POST   /api/quotations/from-itinerary/{id}       - Create from itinerary template
GET    /api/quotations                           - List with filters
GET    /api/quotations/{id}                      - Get by ID
GET    /api/quotations/code/{code}               - Get by code
PUT    /api/quotations/{id}                      - Update quotation
DELETE /api/quotations                           - Bulk delete
POST   /api/quotations/{id}/send                 - Send to customer
POST   /api/quotations/{id}/revise               - Create revision
POST   /api/quotations/{id}/accept               - Mark as accepted
POST   /api/quotations/{id}/reject               - Mark as rejected
POST   /api/quotations/{id}/convert-to-safari    - Convert to Safari booking
GET    /api/quotations/{id}/pdf                  - Generate PDF quote
GET    /api/quotations/customer/{customerId}     - Customer's quotes
POST   /api/quotations/{id}/calculate            - Recalculate totals
```

**Permissions:**
- PERM_CREATE_QUOTATION
- PERM_READ_QUOTATION
- PERM_UPDATE_QUOTATION
- PERM_DELETE_QUOTATION
- PERM_SEND_QUOTATION
- PERM_CONVERT_QUOTATION

---

### Phase 2: Invoice & Payment (Priority: HIGH)

#### 2.1 Invoice Module

**Purpose:** Generate billing documents and track payment obligations.

**Entity: Invoice**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| code | String | Auto-generated (e.g., INV-00010126) |
| invoiceNumber | String | Display number (e.g., INV-2026-0001) |
| customer | Customer | ManyToOne, required |
| safari | Safari | ManyToOne, optional |
| quotation | Quotation | ManyToOne, optional |
| invoiceType | Enum | DEPOSIT, BALANCE, FULL, PROFORMA, CREDIT_NOTE |
| status | Enum | DRAFT, SENT, VIEWED, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, DISPUTED |
| issueDate | LocalDate | Invoice date |
| dueDate | LocalDate | Payment due date |
| currency | String | Invoice currency |
| exchangeRate | BigDecimal | Rate to base currency |
| subtotal | BigDecimal | Sum of line items |
| discountAmount | BigDecimal | Applied discount |
| taxRate | BigDecimal | Tax percentage |
| taxAmount | BigDecimal | Calculated tax |
| totalAmount | BigDecimal | Final total |
| paidAmount | BigDecimal | Amount received |
| balanceDue | BigDecimal | totalAmount - paidAmount |
| paymentTerms | String | e.g., "Net 30", "50% deposit" |
| bankDetails | TEXT | Payment instructions |
| notes | TEXT | Invoice notes |
| internalNotes | TEXT | Staff-only notes |
| sentAt | LocalDateTime | When sent |
| viewedAt | LocalDateTime | When viewed |
| paidAt | LocalDateTime | When fully paid |
| cancelledAt | LocalDateTime | When cancelled |
| cancellationReason | TEXT | Why cancelled |
| remindersSent | Integer | Number of reminders |
| lastReminderAt | LocalDateTime | Last reminder date |
| createdAt | LocalDateTime | Record creation |
| updatedAt | LocalDateTime | Last update |
| createdBy | Long | User who created |

**Related Entities:**

**InvoiceLineItem** - Itemized billing
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| invoice | Invoice | ManyToOne |
| description | String | Item description |
| quantity | BigDecimal | Number of units |
| unitPrice | BigDecimal | Price per unit |
| totalPrice | BigDecimal | quantity × unitPrice |
| taxable | Boolean | Subject to tax |
| sortOrder | Integer | Display order |

**Enums:**
- `InvoiceType`: DEPOSIT, BALANCE, FULL, PROFORMA, CREDIT_NOTE
- `InvoiceStatus`: DRAFT, SENT, VIEWED, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, DISPUTED

**API Endpoints:**
```
POST   /api/invoices                         - Create invoice
POST   /api/invoices/from-quotation/{id}     - Create from quotation
POST   /api/invoices/from-safari/{id}        - Create from safari
GET    /api/invoices                         - List with filters
GET    /api/invoices/{id}                    - Get by ID
GET    /api/invoices/number/{number}         - Get by invoice number
PUT    /api/invoices/{id}                    - Update invoice
DELETE /api/invoices                         - Bulk delete
POST   /api/invoices/{id}/send               - Send to customer
POST   /api/invoices/{id}/remind             - Send reminder
POST   /api/invoices/{id}/cancel             - Cancel invoice
POST   /api/invoices/{id}/mark-paid          - Mark as paid
GET    /api/invoices/{id}/pdf                - Generate PDF invoice
GET    /api/invoices/customer/{customerId}   - Customer's invoices
GET    /api/invoices/safari/{safariId}       - Safari's invoices
GET    /api/invoices/overdue                 - List overdue invoices
```

**Permissions:**
- PERM_CREATE_INVOICE
- PERM_READ_INVOICE
- PERM_UPDATE_INVOICE
- PERM_DELETE_INVOICE
- PERM_SEND_INVOICE
- PERM_CANCEL_INVOICE

---

#### 2.2 Payment Module

**Purpose:** Track actual money received, deposits, refunds, and reconciliation.

**Entity: Payment**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| code | String | Auto-generated (e.g., PAY-00010126) |
| invoice | Invoice | ManyToOne, required |
| customer | Customer | ManyToOne (denormalized) |
| paymentType | Enum | DEPOSIT, PARTIAL, FULL, REFUND |
| paymentMethod | Enum | BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, CASH, CHEQUE, MOBILE_MONEY, PAYPAL, OTHER |
| status | Enum | PENDING, PROCESSING, CONFIRMED, FAILED, REFUNDED, DISPUTED |
| amount | BigDecimal | Payment amount |
| currency | String | Payment currency |
| exchangeRate | BigDecimal | Rate to base currency |
| amountInBaseCurrency | BigDecimal | Converted amount |
| transactionReference | String | Bank/gateway reference |
| paymentDate | LocalDate | When payment made |
| receivedDate | LocalDate | When funds received |
| bankName | String | Payer's bank |
| accountNumber | String | Last 4 digits (masked) |
| cardType | String | Visa, Mastercard, etc. |
| payerName | String | Name on payment |
| payerEmail | String | Payer email |
| notes | TEXT | Payment notes |
| internalNotes | TEXT | Staff-only notes |
| receiptSent | Boolean | Receipt emailed |
| receiptSentAt | LocalDateTime | When receipt sent |
| verifiedBy | Long | User who verified |
| verifiedAt | LocalDateTime | When verified |
| refundReason | TEXT | If refund, why |
| originalPayment | Payment | Self-reference for refunds |
| createdAt | LocalDateTime | Record creation |
| updatedAt | LocalDateTime | Last update |
| createdBy | Long | User who created |

**Enums:**
- `PaymentType`: DEPOSIT, PARTIAL, FULL, REFUND
- `PaymentMethod`: BANK_TRANSFER, CREDIT_CARD, DEBIT_CARD, CASH, CHEQUE, MOBILE_MONEY, PAYPAL, OTHER
- `PaymentStatus`: PENDING, PROCESSING, CONFIRMED, FAILED, REFUNDED, DISPUTED

**API Endpoints:**
```
POST   /api/payments                         - Record payment
GET    /api/payments                         - List with filters
GET    /api/payments/{id}                    - Get by ID
GET    /api/payments/code/{code}             - Get by code
PUT    /api/payments/{id}                    - Update payment
DELETE /api/payments/{id}                    - Delete payment
POST   /api/payments/{id}/verify             - Verify/confirm payment
POST   /api/payments/{id}/refund             - Process refund
POST   /api/payments/{id}/send-receipt       - Send receipt
GET    /api/payments/invoice/{invoiceId}     - Invoice's payments
GET    /api/payments/customer/{customerId}   - Customer's payments
GET    /api/payments/pending                 - List pending verifications
GET    /api/payments/report                  - Payment summary report
```

**Permissions:**
- PERM_CREATE_PAYMENT
- PERM_READ_PAYMENT
- PERM_UPDATE_PAYMENT
- PERM_DELETE_PAYMENT
- PERM_VERIFY_PAYMENT
- PERM_PROCESS_REFUND

---

### Phase 3: Operations (Priority: MEDIUM)

#### 3.1 Vehicle Module

**Purpose:** Fleet management for safari vehicles.

**Entity: Vehicle**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| code | String | Auto-generated (e.g., VEH-001) |
| registrationNumber | String | License plate (unique) |
| vehicleType | Enum | LAND_CRUISER, LAND_ROVER, MINIBUS, VAN, SEDAN, OTHER |
| make | String | Toyota, Land Rover, etc. |
| model | String | Model name |
| year | Integer | Manufacturing year |
| color | String | Vehicle color |
| capacity | Integer | Max passengers |
| features | TEXT | JSON array (pop-up roof, fridge, charging ports, etc.) |
| status | Enum | AVAILABLE, ON_SAFARI, MAINTENANCE, OUT_OF_SERVICE |
| ownershipType | Enum | OWNED, LEASED, CONTRACTED |
| fuelType | Enum | DIESEL, PETROL, HYBRID |
| insuranceExpiry | LocalDate | Insurance expiration |
| inspectionExpiry | LocalDate | Inspection due date |
| lastServiceDate | LocalDate | Last maintenance |
| nextServiceDue | LocalDate | Next maintenance due |
| currentMileage | Integer | Odometer reading |
| primaryImage | String | Vehicle photo |
| internalNotes | TEXT | Staff notes |
| isActive | Boolean | Active in fleet |
| createdAt | LocalDateTime | Record creation |
| updatedAt | LocalDateTime | Last update |

**Related Entities:**
- **VehicleMaintenanceLog** - Service history
- **VehicleAssignment** - Safari assignments

**Enums:**
- `VehicleType`: LAND_CRUISER, LAND_ROVER, MINIBUS, VAN, SEDAN, OTHER
- `VehicleStatus`: AVAILABLE, ON_SAFARI, MAINTENANCE, OUT_OF_SERVICE
- `OwnershipType`: OWNED, LEASED, CONTRACTED
- `FuelType`: DIESEL, PETROL, HYBRID

**API Endpoints:**
```
POST   /api/vehicles                         - Create vehicle
GET    /api/vehicles                         - List with filters
GET    /api/vehicles/{id}                    - Get by ID
PUT    /api/vehicles/{id}                    - Update vehicle
DELETE /api/vehicles                         - Bulk delete
GET    /api/vehicles/available               - List available vehicles
POST   /api/vehicles/{id}/assign             - Assign to safari
POST   /api/vehicles/{id}/maintenance        - Log maintenance
GET    /api/vehicles/{id}/history            - Assignment history
GET    /api/vehicles/maintenance-due         - Due for service
```

**Permissions:**
- PERM_CREATE_VEHICLE
- PERM_READ_VEHICLE
- PERM_UPDATE_VEHICLE
- PERM_DELETE_VEHICLE
- PERM_ASSIGN_VEHICLE

---

#### 3.2 Staff Module

**Purpose:** Manage guides, drivers, and operational staff.

**Entity: Staff**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| code | String | Auto-generated (e.g., STF-001) |
| user | User | ManyToOne, optional (if system user) |
| staffType | Enum | GUIDE, DRIVER, GUIDE_DRIVER, COORDINATOR, COOK, OTHER |
| firstName | String | Required |
| lastName | String | Required |
| email | String | Contact email |
| phone | String | Contact phone |
| alternatePhone | String | Emergency contact |
| dateOfBirth | LocalDate | DOB |
| nationality | String | Citizenship |
| idNumber | String | National ID |
| licenseNumber | String | Driver's license |
| licenseExpiry | LocalDate | License expiration |
| languages | TEXT | JSON array (English, Swahili, French, etc.) |
| certifications | TEXT | JSON array (KPSGA Bronze/Silver/Gold, First Aid, etc.) |
| specializations | TEXT | JSON array (birding, photography, walking safaris, etc.) |
| yearsExperience | Integer | Years in industry |
| status | Enum | ACTIVE, ON_LEAVE, UNAVAILABLE, TERMINATED |
| employmentType | Enum | FULL_TIME, PART_TIME, FREELANCE |
| hireDate | LocalDate | Start date |
| terminationDate | LocalDate | End date if terminated |
| dailyRate | BigDecimal | Standard daily rate |
| currency | String | Rate currency |
| bankName | String | For payments |
| bankAccountNumber | String | Account number |
| emergencyContactName | String | Emergency contact |
| emergencyContactPhone | String | Emergency phone |
| primaryImage | String | Profile photo |
| internalNotes | TEXT | Staff notes |
| rating | BigDecimal | Average rating (1-5) |
| totalSafaris | Integer | Computed: safari count |
| createdAt | LocalDateTime | Record creation |
| updatedAt | LocalDateTime | Last update |

**Related Entities:**
- **StaffAvailability** - Calendar availability
- **StaffAssignment** - Safari assignments
- **StaffDocument** - Certificates, license copies

**Enums:**
- `StaffType`: GUIDE, DRIVER, GUIDE_DRIVER, COORDINATOR, COOK, OTHER
- `StaffStatus`: ACTIVE, ON_LEAVE, UNAVAILABLE, TERMINATED
- `EmploymentType`: FULL_TIME, PART_TIME, FREELANCE

**API Endpoints:**
```
POST   /api/staff                            - Create staff
GET    /api/staff                            - List with filters
GET    /api/staff/{id}                       - Get by ID
PUT    /api/staff/{id}                       - Update staff
DELETE /api/staff                            - Bulk delete
GET    /api/staff/available                  - List available staff
POST   /api/staff/{id}/assign                - Assign to safari
GET    /api/staff/{id}/calendar              - Availability calendar
POST   /api/staff/{id}/availability          - Set availability
GET    /api/staff/{id}/history               - Assignment history
GET    /api/staff/guides                     - List guides only
GET    /api/staff/drivers                    - List drivers only
```

**Permissions:**
- PERM_CREATE_STAFF
- PERM_READ_STAFF
- PERM_UPDATE_STAFF
- PERM_DELETE_STAFF
- PERM_ASSIGN_STAFF
- PERM_VIEW_STAFF_AVAILABILITY

---

## Implementation Order

```
Phase 1 (Priority: HIGH)
├── 1.1 Customer Module
│   ├── Entity + Repository
│   ├── DTOs
│   ├── Services (CRUD)
│   ├── Controller
│   ├── Specifications
│   └── Permissions
│
└── 1.2 Quotation Module
    ├── Entities (Quotation, QuotationPax, QuotationLineItem, QuotationDay)
    ├── Repositories
    ├── DTOs
    ├── Services (CRUD + Convert to Safari)
    ├── Controller
    ├── Specifications
    ├── PDF Template
    └── Permissions

Phase 2 (Priority: HIGH)
├── 2.1 Invoice Module
│   ├── Entities (Invoice, InvoiceLineItem)
│   ├── Repositories
│   ├── DTOs
│   ├── Services (CRUD + Send + Reminders)
│   ├── Controller
│   ├── Specifications
│   ├── PDF Template
│   └── Permissions
│
└── 2.2 Payment Module
    ├── Entity + Repository
    ├── DTOs
    ├── Services (CRUD + Verify + Refund)
    ├── Controller
    ├── Specifications
    └── Permissions

Phase 3 (Priority: MEDIUM)
├── 3.1 Vehicle Module
│   ├── Entities (Vehicle, VehicleMaintenanceLog, VehicleAssignment)
│   ├── Repositories
│   ├── DTOs
│   ├── Services
│   ├── Controller
│   └── Permissions
│
└── 3.2 Staff Module
    ├── Entities (Staff, StaffAvailability, StaffAssignment, StaffDocument)
    ├── Repositories
    ├── DTOs
    ├── Services
    ├── Controller
    └── Permissions
```

---

## Updated Complete Workflow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SALES PIPELINE                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Customer Inquiry                                                   │
│        ↓                                                             │
│   CUSTOMER ◄─── Create/lookup customer record                        │
│        ↓                                                             │
│   ITINERARY ◄─── Select template or create custom                    │
│        ↓                                                             │
│   QUOTATION ◄─── Generate pricing, send to customer                  │
│        ↓                                                             │
│   [Customer Reviews] → Accept / Reject / Request Changes             │
│        ↓                                                             │
│   QUOTATION (Revised) ◄─── If changes requested                      │
│        ↓                                                             │
│   [Accepted]                                                         │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                        BOOKING PHASE                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   SAFARI ◄─── Created from accepted quotation                        │
│        ↓                                                             │
│   INVOICE (Deposit) ◄─── Generate deposit invoice                    │
│        ↓                                                             │
│   PAYMENT ◄─── Record deposit received                               │
│        ↓                                                             │
│   [Safari Confirmed]                                                 │
│        ↓                                                             │
│   INVOICE (Balance) ◄─── Generate balance invoice                    │
│        ↓                                                             │
│   PAYMENT ◄─── Record balance received                               │
│        ↓                                                             │
│   [Fully Paid]                                                       │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                        OPERATIONS PHASE                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   VEHICLE ◄─── Assign vehicle(s) to safari                           │
│        ↓                                                             │
│   STAFF ◄─── Assign guide/driver to safari                           │
│        ↓                                                             │
│   [Safari Execution]                                                 │
│        ↓                                                             │
│   Safari Day 1 → Day 2 → ... → Last Day                              │
│        ↓                                                             │
│   [Safari Completed]                                                 │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                        POST-TRIP                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Feedback Request → Customer Review                                 │
│        ↓                                                             │
│   Staff/Vehicle Release                                              │
│        ↓                                                             │
│   Financial Reconciliation                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Safari State Integration

The existing Safari module has comprehensive state management. Here's how new modules integrate:

| Safari State | Triggered By | Related Actions |
|--------------|--------------|-----------------|
| DRAFT | Quotation accepted | Create Safari from Quotation |
| PENDING_DEPOSIT | Invoice generated | Generate deposit Invoice |
| DEPOSIT_RECEIVED | Payment recorded | Record deposit Payment |
| PENDING_BALANCE | Balance invoice sent | Generate balance Invoice |
| FULLY_PAID | Full payment received | Record balance Payment |
| CONFIRMED | Operations ready | Assign Vehicle + Staff |
| IN_PROGRESS | Safari starts | Day-by-day execution |
| COMPLETED | Safari ends | Release resources, request feedback |

---

## PDF Documents to Create

| Document | Module | Template Variables |
|----------|--------|-------------------|
| Quotation PDF | Quotation | customer, quotation, lineItems, totals, terms |
| Invoice PDF | Invoice | customer, invoice, lineItems, totals, bankDetails |
| Payment Receipt | Payment | customer, payment, invoice reference |
| Safari Confirmation | Safari | customer, safari, itinerary, assignments |
| Safari Voucher | Safari | safari, accommodations, activities, staff |

---

## Database Schema Notes

### Foreign Key Relationships

```
Customer (1) ←→ (N) Quotation
Customer (1) ←→ (N) Invoice
Customer (1) ←→ (N) Safari (add customer_id to Safari)
Customer (1) ←→ (N) Payment

Quotation (1) ←→ (1) Safari (quotation_id on Safari)
Quotation (1) ←→ (N) Invoice

Safari (1) ←→ (N) Invoice
Safari (1) ←→ (N) VehicleAssignment
Safari (1) ←→ (N) StaffAssignment

Invoice (1) ←→ (N) Payment

Vehicle (1) ←→ (N) VehicleAssignment
Staff (1) ←→ (N) StaffAssignment
```

### Safari Entity Updates Required

Add to existing Safari entity:
```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;

@ManyToOne
@JoinColumn(name = "quotation_id")
private Quotation quotation;
```

---

## Permission Updates

Add to PermissionInitializer:

```java
// Customer permissions
"CUSTOMER",

// Quotation permissions
"QUOTATION",

// Invoice permissions
"INVOICE",

// Payment permissions
"PAYMENT",

// Vehicle permissions
"VEHICLE",

// Staff permissions
"STAFF",

// Custom permissions
{"SEND_QUOTATION", "UPDATE", "QUOTATION", "Send quotation to customer"},
{"CONVERT_QUOTATION", "UPDATE", "QUOTATION", "Convert quotation to safari booking"},
{"SEND_INVOICE", "UPDATE", "INVOICE", "Send invoice to customer"},
{"CANCEL_INVOICE", "UPDATE", "INVOICE", "Cancel an invoice"},
{"VERIFY_PAYMENT", "UPDATE", "PAYMENT", "Verify/confirm a payment"},
{"PROCESS_REFUND", "UPDATE", "PAYMENT", "Process payment refund"},
{"ASSIGN_VEHICLE", "UPDATE", "VEHICLE", "Assign vehicle to safari"},
{"ASSIGN_STAFF", "UPDATE", "STAFF", "Assign staff to safari"},
{"VIEW_STAFF_AVAILABILITY", "READ", "STAFF", "View staff availability calendar"},
```

---

## Estimated Effort

| Module | Entities | Services | Complexity |
|--------|----------|----------|------------|
| Customer | 4 | 8 | Medium |
| Quotation | 4 | 12 | High |
| Invoice | 2 | 10 | Medium |
| Payment | 1 | 8 | Medium |
| Vehicle | 3 | 8 | Medium |
| Staff | 4 | 10 | Medium |

---

## Notes

1. **Code Generation Pattern**: Follow existing pattern (e.g., ITI-00010126 for itineraries)
2. **ID Obfuscation**: Use existing IdObfuscator for all public-facing IDs
3. **Audit Logging**: Apply @AuditLogAnnotation to all mutating operations
4. **API Response**: Use existing ApiResponse wrapper for consistency
5. **Validation**: Use Jakarta validation annotations on DTOs
6. **Specifications**: Create JPA Specifications for complex filtering

---

*Last Updated: January 2026*
