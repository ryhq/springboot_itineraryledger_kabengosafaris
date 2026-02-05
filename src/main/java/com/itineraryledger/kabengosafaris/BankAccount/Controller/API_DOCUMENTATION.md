# Bank Account API Documentation

## Overview
The Bank Account API provides endpoints for managing company bank accounts used for receiving payments and displaying on invoices. Each bank account represents a unique account with its own currency, account details, and international payment codes (SWIFT/IBAN).

---

## Base URL
```
/api/bank-accounts
```

---

## Data Transfer Objects (DTOs)

### 1. CreateBankAccountDTO (Request)
Used when creating a new bank account.

```json
{
  "accountName": "string (required - display name)",
  "description": "string (optional - purpose/usage notes)",
  "bankName": "string (required - e.g., Standard Chartered Bank)",
  "bankBranch": "string (optional - e.g., Dar es Salaam Branch)",
  "branchAddress": "string (optional)",
  "branchCity": "string (optional)",
  "branchCountry": "string (optional)",
  "accountNumber": "string (required)",
  "accountHolderName": "string (required - legal entity name)",
  "currency": "string (required - 3-letter ISO code: USD, TZS, EUR, etc.)",
  "swiftBicCode": "string (optional - for international transfers)",
  "iban": "string (optional - International Bank Account Number)",
  "routingNumber": "string (optional - US ACH routing number)",
  "sortCode": "string (optional - UK sort code)",
  "intermediaryBankName": "string (optional - for correspondent banking)",
  "intermediarySwiftCode": "string (optional)",
  "isDefault": "boolean (optional - default account for this currency)",
  "isActive": "boolean (optional - default: true)",
  "internalNotes": "string (optional - staff-only notes)",
  "invoiceDisplayNotes": "string (optional - customer-facing notes for invoices)"
}
```

**Example - USD Account:**
```json
{
  "accountName": "Kabengo Safaris - USD Operations",
  "bankName": "Standard Chartered Bank Tanzania",
  "bankBranch": "Dar es Salaam Branch",
  "accountNumber": "0123456789",
  "accountHolderName": "Kabengo Safaris Limited",
  "currency": "USD",
  "swiftBicCode": "SCBLTZTZ",
  "isDefault": true,
  "isActive": true,
  "invoiceDisplayNotes": "For international USD transfers, please use SWIFT transfer."
}
```

**Example - TZS Account:**
```json
{
  "accountName": "Kabengo Safaris - TZS Operations",
  "bankName": "CRDB Bank",
  "bankBranch": "Arusha Branch",
  "accountNumber": "9876543210",
  "accountHolderName": "Kabengo Safaris Limited",
  "currency": "TZS",
  "isDefault": true,
  "isActive": true,
  "invoiceDisplayNotes": "For local Tanzanian Shilling payments."
}
```

---

### 2. UpdateBankAccountDTO (Request)
Used when updating an existing bank account. All fields are optional - only include fields you want to update.

```json
{
  "accountName": "string (optional)",
  "description": "string (optional)",
  "bankName": "string (optional)",
  "bankBranch": "string (optional)",
  "branchAddress": "string (optional)",
  "branchCity": "string (optional)",
  "branchCountry": "string (optional)",
  "accountNumber": "string (optional)",
  "accountHolderName": "string (optional)",
  "currency": "string (optional - 3-letter ISO code)",
  "swiftBicCode": "string (optional)",
  "iban": "string (optional)",
  "routingNumber": "string (optional)",
  "sortCode": "string (optional)",
  "intermediaryBankName": "string (optional)",
  "intermediarySwiftCode": "string (optional)",
  "isDefault": "boolean (optional)",
  "isActive": "boolean (optional)",
  "internalNotes": "string (optional)",
  "invoiceDisplayNotes": "string (optional)"
}
```

**Example - Update SWIFT Code:**
```json
{
  "swiftBicCode": "SCBLTZTZXXX"
}
```

**Example - Set as Default:**
```json
{
  "isDefault": true
}
```

**Example - Deactivate Account:**
```json
{
  "isActive": false
}
```

---

### 3. BankAccountDTO (Response)
Returned in API responses. Contains all bank account information.

```json
{
  "id": "string (obfuscated ID)",
  "accountCode": "string (auto-generated: BANK-XXXXXX)",
  "accountName": "string",
  "description": "string",
  "bankName": "string",
  "bankBranch": "string",
  "branchAddress": "string",
  "branchCity": "string",
  "branchCountry": "string",
  "accountNumber": "string",
  "accountHolderName": "string",
  "currency": "string (3-letter ISO code)",
  "swiftBicCode": "string",
  "iban": "string",
  "routingNumber": "string",
  "sortCode": "string",
  "intermediaryBankName": "string",
  "intermediarySwiftCode": "string",
  "isDefault": "boolean",
  "isActive": "boolean",
  "internalNotes": "string",
  "invoiceDisplayNotes": "string",
  "createdByName": "string",
  "updatedByName": "string",
  "createdAt": "datetime (ISO 8601)",
  "updatedAt": "datetime (ISO 8601)"
}
```

**Example Response:**
```json
{
  "id": "encoded_id_xyz",
  "accountCode": "BANK-000100",
  "accountName": "Kabengo Safaris - USD Operations",
  "bankName": "Standard Chartered Bank Tanzania",
  "bankBranch": "Dar es Salaam Branch",
  "accountNumber": "0123456789",
  "accountHolderName": "Kabengo Safaris Limited",
  "currency": "USD",
  "swiftBicCode": "SCBLTZTZ",
  "iban": null,
  "routingNumber": null,
  "sortCode": null,
  "intermediaryBankName": null,
  "intermediarySwiftCode": null,
  "isDefault": true,
  "isActive": true,
  "internalNotes": "Primary USD account for international transfers",
  "invoiceDisplayNotes": "For international USD transfers, please use SWIFT transfer.",
  "createdByName": "admin",
  "updatedByName": "admin",
  "createdAt": "2026-02-01T10:30:45",
  "updatedAt": "2026-02-01T10:30:45"
}
```

---

## Endpoints

### 1. Create Bank Account

**POST** `/api/bank-accounts`

Creates a new bank account. If `isDefault` is set to true, automatically unsets any existing default for that currency.

**Permission Required:** `PERM_CREATE_BANK_ACCOUNT`

**Request Body:** `CreateBankAccountDTO` (see above)

**Response:**
- **201 Created** - Bank account created successfully
- **400 Bad Request** - Invalid input (e.g., invalid currency format)
- **500 Internal Server Error** - Server error

**Success Response:**
```json
{
  "status": 201,
  "message": "Bank account created successfully",
  "data": { /* BankAccountDTO object */ }
}
```

**Error Response:**
```json
{
  "status": 400,
  "message": "Currency must be a 3-letter ISO code",
  "errorCode": "INVALID_CURRENCY_FORMAT"
}
```

---

### 2. Get Bank Account by ID

**GET** `/api/bank-accounts/{idObfuscated}`

Retrieves a single bank account by its obfuscated ID.

**Permission Required:** `PERM_READ_BANK_ACCOUNT`

**Path Parameters:**
- `idObfuscated` (string) - Obfuscated bank account ID

**Response:**
- **200 OK** - Bank account retrieved successfully
- **400 Bad Request** - Invalid bank account ID
- **404 Not Found** - Bank account not found
- **500 Internal Server Error** - Server error

**Success Response:**
```json
{
  "status": 200,
  "message": "Successfully retrieved bank account.",
  "data": { /* BankAccountDTO object */ }
}
```

---

### 3. Get All Bank Accounts

**GET** `/api/bank-accounts`

Retrieves all bank accounts with pagination, sorting, and filtering.

**Permission Required:** `PERM_READ_BANK_ACCOUNT`

**Query Parameters:**
- `currency` (string, optional) - Filter by currency code (exact match, e.g., "USD")
- `isActive` (boolean, optional) - Filter by active status
- `isDefault` (boolean, optional) - Filter by default status
- `search` (string, optional) - Search keyword (searches across account name, bank name, account number, account code)
- `page` (integer, optional, default: 0) - Page number (0-indexed)
- `size` (integer, optional, default: 20) - Page size
- `sortDirection` (string, optional, default: "desc") - Sort direction: "asc" or "desc" (sorts by createdAt)

**Response:**
- **200 OK** - Bank accounts retrieved successfully
- **400 Bad Request** - Invalid pagination parameters
- **500 Internal Server Error** - Server error

**Success Response:**
```json
{
  "status": 200,
  "message": "Successfully retrieved bank accounts.",
  "data": {
    "bankAccounts": [
      { /* BankAccountDTO object */ },
      { /* BankAccountDTO object */ }
    ],
    "currentPage": 0,
    "totalItems": 5,
    "totalPages": 1
  }
}
```

**Example Requests:**
```
GET /api/bank-accounts?page=0&size=20&sortDirection=desc
GET /api/bank-accounts?currency=USD
GET /api/bank-accounts?isActive=true&isDefault=true
GET /api/bank-accounts?search=Standard Chartered
```

---

### 4. Update Bank Account

**PUT** `/api/bank-accounts/{idObfuscated}`

Updates an existing bank account. Only provided fields will be updated.

**Permission Required:** `PERM_UPDATE_BANK_ACCOUNT`

**Path Parameters:**
- `idObfuscated` (string) - Obfuscated bank account ID

**Request Body:** `UpdateBankAccountDTO` (see above)

**Response:**
- **200 OK** - Bank account updated successfully
- **400 Bad Request** - Invalid input or bank account ID
- **404 Not Found** - Bank account not found
- **500 Internal Server Error** - Server error

**Success Response:**
```json
{
  "status": 200,
  "message": "Bank account updated successfully",
  "data": { /* Updated BankAccountDTO object */ }
}
```

---

### 5. Delete Bank Accounts

**DELETE** `/api/bank-accounts`

Deletes one or more bank accounts by their obfuscated IDs. This is a hard delete operation.

**Permission Required:** `PERM_DELETE_BANK_ACCOUNT`

**Request Body:** Array of obfuscated bank account IDs
```json
["encoded_id_1", "encoded_id_2", "encoded_id_3"]
```

**Response:**
- **200 OK** - Bank accounts deleted successfully
- **500 Internal Server Error** - Server error

**Success Response:**
```json
{
  "status": 200,
  "message": "3 bank account(s) deleted successfully",
  "data": null
}
```

**Notes:**
- Invalid IDs are silently skipped
- Non-existent bank accounts are logged and skipped
- The response indicates the count of successfully deleted accounts

---

## Business Rules

### Currency Code Validation
- Currency must be a 3-letter uppercase ISO 4217 code (e.g., USD, TZS, EUR, GBP, KES, UGX)
- The system automatically converts to uppercase
- Invalid formats are rejected with a validation error

### Default Account Logic
- Only one bank account per currency can be marked as default
- When setting a new account as default, the system automatically unsets the previous default for that currency
- This ensures consistent invoice generation

### Active/Inactive Status
- Inactive accounts can still be retrieved but should not be used for new invoices
- Setting `isActive` to false does not delete the account
- Inactive accounts are excluded from default account lookups

### Account Code Generation
- Account codes are auto-generated using the pattern: `BANK-{6-digit padded number}`
- Example: BANK-000100, BANK-000101, etc.
- Codes are generated based on the database ID + 100

---

## Common Use Cases

### Use Case 1: Setup Multi-Currency Bank Accounts
```
1. POST /api/bank-accounts (create USD account with isDefault=true)
2. POST /api/bank-accounts (create TZS account with isDefault=true)
3. POST /api/bank-accounts (create EUR account with isDefault=true)
4. GET /api/bank-accounts (verify all accounts created)
```

### Use Case 2: Update Bank Details
```
1. GET /api/bank-accounts/{id} (get current account details)
2. PUT /api/bank-accounts/{id} (update specific fields like SWIFT code)
3. GET /api/bank-accounts/{id} (verify changes)
```

### Use Case 3: Switch Default Account
```
1. GET /api/bank-accounts?currency=USD (find all USD accounts)
2. PUT /api/bank-accounts/{id} (set new account as default with isDefault=true)
   - System automatically unsets the previous default
3. GET /api/bank-accounts?currency=USD&isDefault=true (verify new default)
```

### Use Case 4: Deactivate Old Account
```
1. PUT /api/bank-accounts/{id} with { "isActive": false }
2. Account remains in database but won't be used for new invoices
```

### Use Case 5: Search and Filter Accounts
```
GET /api/bank-accounts?search=Standard
GET /api/bank-accounts?currency=USD&isActive=true
GET /api/bank-accounts?isDefault=true
```

---

## Security & Permissions

All endpoints require authentication and specific permissions:

- **CREATE_BANK_ACCOUNT** - Create new bank accounts
- **READ_BANK_ACCOUNT** - View bank accounts
- **UPDATE_BANK_ACCOUNT** - Modify bank accounts
- **DELETE_BANK_ACCOUNT** - Delete bank accounts

Recommended role assignments:
- **FINANCE_ADMIN**: All permissions
- **FINANCE_MANAGER**: READ, UPDATE
- **FINANCE_OFFICER**: READ only
- **ADMIN**: All permissions

---

## Notes

- All IDs in requests and responses are obfuscated for security
- Bank account numbers and sensitive details should be protected with appropriate access controls
- Internal notes are for staff use and should not be exposed to customers
- Invoice display notes will be shown to customers on invoices
- The system supports international payment methods (SWIFT, IBAN) and regional codes (routing numbers, sort codes)
- For East African operations, focus on bankName, accountNumber, and branch details
- For international operations, prioritize swiftBicCode and iban fields
