# Snabel Accounting System - API Documentation

This document describes all available REST API endpoints in the Snabel Accounting System.

## Base URL

```
http://localhost:8080
```

## Authentication

All endpoints except `/api/auth/login` require a valid JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

---

## Authentication Endpoints

### POST /api/auth/login

Authenticate a user and receive a JWT token.

**Request Body:**
```json
{
  "username": "string",
  "password": "string",
  "deviceType": "web" | "app"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "username": "testuser",
  "customerId": 1,
  "role": "ADMIN",
  "expiresIn": 86400
}
```

**Response (401 Unauthorized):**
```json
{
  "error": "Invalid username or password"
}
```

**Token Expiration:**
- Web tokens: 24 hours (86400 seconds)
- App tokens: 30 days (2592000 seconds)

**Example:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password",
    "deviceType": "web"
  }'
```

---

## Account Endpoints

All account endpoints require authentication.

### GET /api/accounts

List all active accounts for the authenticated customer.

**Permissions:** USER, ADMIN, ACCOUNTANT

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "accountNumber": "1900",
    "accountName": "Bankkonto Hovedkonto",
    "accountType": "ASSET",
    "vatCode": null,
    "balance": 100000.00,
    "currency": "NOK",
    "description": "Main bank account",
    "active": true
  }
]
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### GET /api/accounts/{id}

Get a specific account by ID.

**Permissions:** USER, ADMIN, ACCOUNTANT

**Path Parameters:**
- `id` (number): Account ID

**Response (200 OK):**
```json
{
  "id": 1,
  "accountNumber": "1900",
  "accountName": "Bankkonto Hovedkonto",
  "accountType": "ASSET",
  "vatCode": null,
  "balance": 100000.00,
  "currency": "NOK",
  "description": "Main bank account",
  "active": true
}
```

**Response (404 Not Found):**
Account not found or doesn't belong to customer.

### POST /api/accounts

Create a new account.

**Permissions:** ADMIN, ACCOUNTANT

**Request Body:**
```json
{
  "accountNumber": "2000",
  "accountName": "New Account",
  "accountType": "ASSET",
  "vatCode": "3",
  "description": "Account description"
}
```

**Account Types:**
- `ASSET` - Assets (Eiendeler)
- `LIABILITY` - Liabilities (Gjeld)
- `EQUITY` - Equity (Egenkapital)
- `REVENUE` - Revenue (Inntekter)
- `EXPENSE` - Expenses (Kostnader)

**VAT Codes:**
- `0` - No VAT
- `3` - 25% VAT (standard Norwegian rate)
- `33` - 15% VAT (reduced rate for food)
- `5` - VAT exempt

**Response (201 Created):**
```json
{
  "id": 5,
  "accountNumber": "2000",
  "accountName": "New Account",
  "accountType": "ASSET",
  "vatCode": "3",
  "balance": 0.00,
  "currency": "NOK",
  "description": "Account description",
  "active": true
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "2000",
    "accountName": "New Account",
    "accountType": "ASSET",
    "vatCode": "3"
  }'
```

### PUT /api/accounts/{id}

Update an existing account.

**Permissions:** ADMIN, ACCOUNTANT

**Path Parameters:**
- `id` (number): Account ID

**Request Body:**
```json
{
  "accountName": "Updated Account Name",
  "description": "Updated description",
  "vatCode": "5"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "accountNumber": "1900",
  "accountName": "Updated Account Name",
  "accountType": "ASSET",
  "vatCode": "5",
  "balance": 100000.00,
  "currency": "NOK",
  "description": "Updated description",
  "active": true
}
```

**Response (404 Not Found):**
Account not found.

### DELETE /api/accounts/{id}

Soft delete an account (sets active = false).

**Permissions:** ADMIN only

**Path Parameters:**
- `id` (number): Account ID

**Response (204 No Content):**
Account successfully deleted.

**Response (404 Not Found):**
Account not found.

---

## Invoice Endpoints

All invoice endpoints require authentication.

### GET /api/invoices

List invoices for the authenticated customer.

**Permissions:** USER, ADMIN, ACCOUNTANT

**Query Parameters:**
- `status` (optional): Filter by status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)
- `limit` (optional, default: 50): Maximum number of results

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "invoiceNumber": "INV-001",
    "invoiceDate": "2025-11-09",
    "dueDate": "2025-11-23",
    "clientName": "Test Client AS",
    "clientOrganizationNumber": "987654321",
    "subtotal": 10000.00,
    "vatAmount": 2500.00,
    "totalAmount": 12500.00,
    "currency": "NOK",
    "status": "DRAFT",
    "createdAt": "2025-11-09T10:30:00"
  }
]
```

**Example:**
```bash
# Get all invoices
curl -X GET http://localhost:8080/api/invoices \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get only DRAFT invoices
curl -X GET "http://localhost:8080/api/invoices?status=DRAFT&limit=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### GET /api/invoices/{id}

Get a specific invoice by ID.

**Permissions:** USER, ADMIN, ACCOUNTANT

**Path Parameters:**
- `id` (number): Invoice ID

**Response (200 OK):**
```json
{
  "id": 1,
  "invoiceNumber": "INV-001",
  "invoiceDate": "2025-11-09",
  "dueDate": "2025-11-23",
  "clientName": "Test Client AS",
  "clientOrganizationNumber": "987654321",
  "clientAddress": "Test Street 123",
  "clientPostalCode": "0123",
  "clientCity": "Oslo",
  "subtotal": 10000.00,
  "vatAmount": 2500.00,
  "totalAmount": 12500.00,
  "currency": "NOK",
  "status": "DRAFT",
  "paymentTerms": "14 dager",
  "notes": "Thank you for your business",
  "createdAt": "2025-11-09T10:30:00",
  "updatedAt": "2025-11-09T10:30:00"
}
```

**Response (404 Not Found):**
Invoice not found.

### POST /api/invoices

Create a new invoice.

**Permissions:** ADMIN, ACCOUNTANT

**Request Body:**
```json
{
  "invoiceNumber": "INV-002",
  "invoiceDate": "2025-11-09",
  "dueDate": "2025-11-23",
  "clientName": "Client Company AS",
  "clientOrganizationNumber": "987654321",
  "clientAddress": "Street Address 123",
  "clientPostalCode": "0123",
  "clientCity": "Oslo",
  "subtotal": 10000.00,
  "vatAmount": 2500.00,
  "totalAmount": 12500.00,
  "currency": "NOK",
  "paymentTerms": "14 dager",
  "buyerReference": "PO-12345",
  "orderReference": "ORDER-67890",
  "contractReference": "CONTRACT-2024-01",
  "paymentReference": "1234567890128",
  "bankAccount": "12345678901",
  "clientEndpointId": "987654321",
  "clientEndpointScheme": "0192",
  "notes": "Invoice notes"
}
```

**PEPPOL/EHF Fields (for PEPPOL BIS 3.0 Billing compliance):**
- `buyerReference` (string, recommended): Customer's reference (PEPPOL-EN16931-R003)
- `orderReference` (string, optional): Purchase order reference (alternative to buyerReference)
- `contractReference` (string, optional): Contract or agreement reference
- `paymentReference` (string, optional): KID number or payment reference
- `bankAccount` (string, optional): Override supplier's default bank account
- `clientEndpointId` (string, recommended): Buyer's electronic address for PEPPOL (PEPPOL-EN16931-R010)
- `clientEndpointScheme` (string, default: "0192"): Scheme identifier for buyer endpoint

**Notes:**
- Either `buyerReference` or `orderReference` should be provided for PEPPOL compliance
- If neither is provided, the invoice number will be used as fallback
- `clientEndpointId` defaults to `clientOrganizationNumber` if not provided
- Default endpoint scheme is "0192" (Norwegian organization number)

**Response (201 Created):**
```json
{
  "id": 2,
  "invoiceNumber": "INV-002",
  "status": "DRAFT",
  "createdAt": "2025-11-09T15:45:00",
  ...
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNumber": "INV-002",
    "invoiceDate": "2025-11-09",
    "dueDate": "2025-11-23",
    "clientName": "Test Client AS",
    "subtotal": 10000.00,
    "vatAmount": 2500.00,
    "totalAmount": 12500.00
  }'
```

### PUT /api/invoices/{id}

Update an existing invoice.

**Permissions:** ADMIN, ACCOUNTANT

**Path Parameters:**
- `id` (number): Invoice ID

**Request Body:**
```json
{
  "clientName": "Updated Client Name",
  "dueDate": "2025-12-01",
  "subtotal": 15000.00,
  "vatAmount": 3750.00,
  "totalAmount": 18750.00,
  "notes": "Updated notes"
}
```

**Response (200 OK):**
Updated invoice object.

**Response (404 Not Found):**
Invoice not found.

### PUT /api/invoices/{id}/send

Mark an invoice as sent.

**Permissions:** ADMIN, ACCOUNTANT

**Path Parameters:**
- `id` (number): Invoice ID

**Response (200 OK):**
```json
{
  "id": 1,
  "status": "SENT",
  "sentAt": "2025-11-09T16:00:00",
  ...
}
```

**Example:**
```bash
curl -X PUT http://localhost:8080/api/invoices/1/send \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### PUT /api/invoices/{id}/pay

Mark an invoice as paid.

**Permissions:** ADMIN, ACCOUNTANT

**Path Parameters:**
- `id` (number): Invoice ID

**Response (200 OK):**
```json
{
  "id": 1,
  "status": "PAID",
  "paidAt": "2025-11-09T16:30:00",
  ...
}
```

**Example:**
```bash
curl -X PUT http://localhost:8080/api/invoices/1/pay \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### GET /api/invoices/{id}/pdf

Download invoice as PDF file.

**Permissions:** USER, ADMIN, ACCOUNTANT, CLIENT

**Path Parameters:**
- `id` (number): Invoice ID

**Response (200 OK):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="faktura-{invoiceNumber}.pdf"`
- Binary PDF data

**Response (404 Not Found):**
Invoice not found.

**Example:**
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/invoices/123/pdf \
  -o invoice-123.pdf
```

### GET /api/invoices/{id}/efaktura

Download invoice as EHF 3.0 XML (eFaktura) for PEPPOL network.

**Permissions:** USER, ADMIN, ACCOUNTANT, CLIENT

**Path Parameters:**
- `id` (number): Invoice ID

**Response (200 OK):**
- Content-Type: `application/xml`
- Content-Disposition: `attachment; filename="efaktura-{invoiceNumber}.xml"`
- EHF 3.0 compliant XML (PEPPOL BIS Billing 3.0, Norwegian NS4102)

**Compliance:**
- PEPPOL BIS 3.0 Billing
- Norwegian NS4102 standard
- CEN EN16931 European e-invoicing standard

**Response (404 Not Found):**
Invoice not found.

**Example:**
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/invoices/123/efaktura \
  -o invoice-123.xml
```

**Validation:**
Generated XML can be validated at:
- [ELMA Validator](https://anskaffelser.dev/validator/)
- [PEPPOL Validation Service](https://peppol.helger.com/public/menuitem-validation-bis3)

---

## Error Responses

### 401 Unauthorized
```json
{
  "error": "Unauthorized"
}
```

Occurs when:
- No Authorization header provided
- Invalid or expired JWT token

### 403 Forbidden
```json
{
  "error": "Forbidden"
}
```

Occurs when:
- User doesn't have required role for the operation

### 404 Not Found
```json
{
  "error": "Not Found"
}
```

Occurs when:
- Resource doesn't exist
- Resource belongs to a different customer

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error"
}
```

Occurs when:
- Unexpected server error

---

## Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `204 No Content` - Resource deleted successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Rate Limiting

Currently, there is no rate limiting implemented. In production, consider implementing:
- Rate limiting per user/IP
- Request throttling for expensive operations
- Token refresh mechanism

---

## Notes

1. All date fields use ISO 8601 format (YYYY-MM-DD)
2. All datetime fields use ISO 8601 format with timezone
3. All currency amounts are in the smallest unit (e.g., NOK)
4. Customer ID is automatically extracted from JWT token
5. All responses are in JSON format
