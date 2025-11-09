# Data Structures - Snabel Accounting System

This document describes the domain model, entity relationships, and data structures used in the Snabel Accounting System.

## Overview

The system uses a **multi-tenant architecture** where all data is isolated by customer. The core domain model follows Norwegian accounting principles (NS 4102) with support for:
- Customer/company management
- User authentication and authorization
- Chart of accounts (Norwegian standard)
- Double-entry bookkeeping
- Invoice management
- VAT reporting

---

## Domain Model Diagram

```
┌─────────────────┐
│   Organization  │
│   (Customer)    │
└────────┬────────┘
         │ 1:N
         │
    ┌────┴──────────────────┬───────────────────┬──────────────────┐
    │                       │                   │                  │
┌───▼────┐            ┌─────▼──────┐      ┌────▼─────┐      ┌────▼──────┐
│  User  │            │  Account   │      │ Invoice  │      │  Journal  │
│        │            │            │      │          │      │   Entry   │
└────────┘            └─────┬──────┘      └────┬─────┘      └────┬──────┘
                            │ N:1              │ 1:N             │ 1:N
                      ┌─────▼──────┐      ┌────▼─────┐      ┌────▼──────┐
                      │  Standard  │      │ Invoice  │      │  Journal  │
                      │  Account   │      │   Line   │      │Entry Line │
                      └────────────┘      └──────────┘      └───────────┘
```

---

## Core Entities

### 1. Customer (Organization)

Represents a company using the accounting system. This is the **multi-tenancy root**.

```java
@Entity
@Table(name = "customers")
public class Customer {
    Long id;
    String organizationNumber;  // 9-digit Norwegian org number
    String companyName;
    String contactPerson;
    String email;
    String phone;
    String address;
    String postalCode;
    String city;
    String country;            // Default: "Norge"
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean active;           // Soft delete flag
}
```

**Key Points:**
- All other entities link to Customer for tenant isolation
- Organization number is unique (9 digits for Norway)
- Soft delete via `active` flag
- Created and updated timestamps for audit trail

**Relationships:**
- `1:N` → Users
- `1:N` → Accounts
- `1:N` → Invoices
- `1:N` → Journal Entries

---

### 2. User

User accounts with authentication and role-based authorization.

```java
@Entity
@Table(name = "users")
public class User {
    Long id;
    Customer customer;        // FK: Multi-tenant link
    String username;          // Unique login
    String passwordHash;      // BCrypt hash
    String email;
    String fullName;
    String role;              // USER, ACCOUNTANT, ADMIN
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime lastLogin;
    Boolean active;
}
```

**Roles:**
- `USER`: Read-only access (view reports, invoices)
- `ACCOUNTANT`: Create/edit transactions, invoices
- `ADMIN`: Full access including user management

**Security:**
- Passwords hashed with BCrypt (cost factor 10)
- JWT tokens for stateless authentication
- Token expiration: 24h (web), 30 days (mobile)

**Relationships:**
- `N:1` → Customer (each user belongs to one customer)
- Created journal entries and invoices reference user

---

### 3. StandardAccount

Norwegian standard chart of accounts (NS 4102). These are **system-wide templates**.

```java
@Entity
@Table(name = "standard_accounts")
public class StandardAccount {
    Long id;
    String accountNumber;     // e.g., "1900", "3000"
    String accountName;       // e.g., "Bankkonto", "Salgsinntekt"
    String accountType;       // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    String accountClass;      // NS 4102: Class 1-8
    String vatCode;           // Norwegian VAT: 0, 3, 33, 5
    String description;
    Boolean isSystem;         // true = seeded standard account
    Boolean active;
}
```

**Account Classes (NS 4102):**
- **Class 1**: Assets (Eiendeler) - e.g., 1900 = Bank account
- **Class 2**: Equity & Liabilities (Egenkapital og gjeld)
- **Class 3**: Operating Income (Driftsinntekter) - e.g., 3000 = Sales revenue
- **Class 4**: Cost of Goods Sold (Varekostnad)
- **Class 5**: Payroll Expenses (Lønnskostnader)
- **Class 6**: Other Operating Expenses (Annen driftskostnad)
- **Class 7**: Financial Income/Expenses (Finansinntekter/-kostnader)
- **Class 8**: Tax (Skattekostnad)

**VAT Codes:**
- `0`: No VAT
- `3`: 25% (standard Norwegian rate)
- `33`: 15% (reduced rate for food)
- `5`: VAT exempt

**Relationships:**
- `1:N` → Accounts (template for customer-specific accounts)

---

### 4. Account

Customer-specific chart of accounts. Each customer has their own account instances.

```java
@Entity
@Table(name = "accounts")
public class Account {
    Long id;
    Customer customer;           // FK: Tenant isolation
    StandardAccount standardAccount;  // FK: Optional template link
    String accountNumber;        // Customer's numbering
    String accountName;
    String accountType;          // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    String vatCode;
    BigDecimal balance;          // Current balance
    String currency;             // Default: "NOK"
    String description;
    Account parentAccount;       // FK: For account hierarchy
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean active;
}
```

**Key Points:**
- Derived from StandardAccount but customizable
- Supports account hierarchy via `parentAccount`
- Balance updated automatically by journal entries
- Unique constraint: `(customer_id, account_number)`

**Balance Rules:**
- **Assets/Expenses**: Debit increases, Credit decreases
- **Liabilities/Equity/Revenue**: Credit increases, Debit decreases

**Relationships:**
- `N:1` → Customer (tenant isolation)
- `N:1` → StandardAccount (optional template)
- `N:1` → Account (parent, for hierarchy)
- `1:N` → JournalEntryLines (transactions affecting this account)
- `1:N` → InvoiceLines (revenue accounts)

---

### 5. JournalEntry

Header for accounting transactions (Bilag/Posteringer).

```java
@Entity
@Table(name = "journal_entries")
public class JournalEntry {
    Long id;
    Customer customer;           // FK: Tenant
    String entryNumber;          // Entry reference
    LocalDate entryDate;         // Transaction date
    String description;
    String reference;            // External reference
    String entryType;            // MANUAL, INVOICE, PAYMENT, AUTOMATED
    User createdBy;              // FK: Audit trail
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean posted;              // Posted = finalized
    LocalDateTime postedAt;
    Boolean reversed;            // For corrections
    JournalEntry reversedBy;     // FK: Reversing entry

    List<JournalEntryLine> lines;  // Journal lines
}
```

**Entry Types:**
- `MANUAL`: Manual journal entry by accountant
- `INVOICE`: Auto-generated from invoice
- `PAYMENT`: Auto-generated from payment
- `AUTOMATED`: System-generated entries

**Posting:**
- Draft entries can be edited
- Posted entries are immutable
- Corrections via reversing entries

**Relationships:**
- `N:1` → Customer
- `N:1` → User (createdBy)
- `1:N` → JournalEntryLines
- `1:1` → JournalEntry (reversedBy, for corrections)

---

### 6. JournalEntryLine

Individual line items in a journal entry (double-entry bookkeeping).

```java
@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine {
    Long id;
    JournalEntry journalEntry;   // FK: Parent entry
    Account account;             // FK: Account affected
    String description;
    BigDecimal debitAmount;      // Debit side
    BigDecimal creditAmount;     // Credit side
    BigDecimal vatAmount;        // VAT portion
    String vatCode;
    String currency;             // Default: "NOK"
    BigDecimal exchangeRate;     // For foreign currency
    Integer lineNumber;          // Ordering
    LocalDateTime createdAt;
}
```

**Double-Entry Rules:**
- Each line has EITHER debit OR credit (not both)
- Total debits must equal total credits in entry
- Check constraint enforces this
- VAT tracked separately

**Example Entry** (Invoice):
```
Debit:  Account 1500 (Kundefordringer) = 12,500 NOK
Credit: Account 3000 (Salgsinntekt)    = 10,000 NOK
Credit: Account 2700 (Utgående MVA)    =  2,500 NOK
```

**Relationships:**
- `N:1` → JournalEntry
- `N:1` → Account

---

### 7. Invoice

Sales invoices with Norwegian VAT.

```java
@Entity
@Table(name = "invoices")
public class Invoice {
    Long id;
    Customer customer;               // FK: Tenant
    String invoiceNumber;            // Unique
    LocalDate invoiceDate;
    LocalDate dueDate;
    String clientName;               // Customer's client
    String clientOrganizationNumber; // 9-digit
    String clientAddress;
    String clientPostalCode;
    String clientCity;
    BigDecimal subtotal;             // Before VAT
    BigDecimal vatAmount;            // Total VAT
    BigDecimal totalAmount;          // Including VAT
    String currency;                 // Default: "NOK"
    String status;                   // DRAFT, SENT, PAID, OVERDUE, CANCELLED
    String paymentTerms;             // e.g., "14 dager"
    String notes;
    JournalEntry journalEntry;       // FK: Generated journal entry
    User createdBy;                  // FK: Audit
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime sentAt;
    LocalDateTime paidAt;

    List<InvoiceLine> lines;
}
```

**Status Flow:**
```
DRAFT → SENT → PAID
  ↓       ↓
CANCELLED
  ↓
OVERDUE (auto-calculated if past due date)
```

**Relationships:**
- `N:1` → Customer
- `N:1` → User (createdBy)
- `1:N` → InvoiceLines
- `1:1` → JournalEntry (auto-generated when posted)

---

### 8. InvoiceLine

Line items on invoices.

```java
@Entity
@Table(name = "invoice_lines")
public class InvoiceLine {
    Long id;
    Invoice invoice;             // FK: Parent invoice
    Integer lineNumber;
    String description;
    BigDecimal quantity;
    BigDecimal unitPrice;
    BigDecimal vatRate;          // 0, 15, or 25
    BigDecimal vatAmount;        // Calculated
    BigDecimal lineTotal;        // quantity * unitPrice + VAT
    Account account;             // FK: Revenue account
    LocalDateTime createdAt;
}
```

**Calculation:**
```
Line Subtotal = quantity × unitPrice
VAT Amount = Line Subtotal × (vatRate / 100)
Line Total = Line Subtotal + VAT Amount
```

**Relationships:**
- `N:1` → Invoice
- `N:1` → Account (revenue account, e.g., 3000 = Sales)

---

### 9. Payment

Payment records linking to invoices.

```java
@Entity
@Table(name = "payments")
public class Payment {
    Long id;
    Customer customer;           // FK: Tenant
    Invoice invoice;             // FK: Related invoice
    LocalDate paymentDate;
    BigDecimal amount;
    String currency;
    String paymentMethod;        // BANK_TRANSFER, VIPPS, CARD, CASH
    String reference;            // Payment reference
    String notes;
    JournalEntry journalEntry;   // FK: Generated entry
    User createdBy;              // FK: Audit
    LocalDateTime createdAt;
}
```

**Payment Methods:**
- `BANK_TRANSFER`: Traditional bank transfer
- `VIPPS`: Norwegian mobile payment
- `CARD`: Card payment
- `CASH`: Cash payment

**Relationships:**
- `N:1` → Customer
- `N:1` → Invoice
- `N:1` → User (createdBy)
- `1:1` → JournalEntry (auto-generated)

---

### 10. VatReport

VAT reporting for Norwegian tax authorities (MVA-oppgave).

```java
@Entity
@Table(name = "vat_reports")
public class VatReport {
    Long id;
    Customer customer;              // FK: Tenant
    LocalDate periodStart;
    LocalDate periodEnd;
    BigDecimal totalSalesVat;       // VAT collected
    BigDecimal totalPurchaseVat;    // VAT paid
    BigDecimal netVat;              // To pay/receive
    String status;                  // DRAFT, SUBMITTED, PAID
    LocalDateTime submittedAt;
    User createdBy;                 // FK: Audit
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

**Calculation:**
```
Total Sales VAT = Sum of all outgoing VAT (account 2700)
Total Purchase VAT = Sum of all incoming VAT (account 2740)
Net VAT = Total Sales VAT - Total Purchase VAT
```

**Relationships:**
- `N:1` → Customer
- `N:1` → User (createdBy)

---

### 11. AuditLog

Complete audit trail for all changes.

```java
@Entity
@Table(name = "audit_log")
public class AuditLog {
    Long id;
    Customer customer;           // FK: Tenant
    User user;                   // FK: Who made the change
    String entityType;           // Entity class name
    Long entityId;               // Entity ID
    String action;               // CREATE, UPDATE, DELETE
    JsonNode oldValues;          // JSONB: Previous state
    JsonNode newValues;          // JSONB: New state
    LocalDateTime createdAt;     // When
}
```

**Actions:**
- `CREATE`: New entity created
- `UPDATE`: Entity modified
- `DELETE`: Entity deleted (or soft-deleted)

**Relationships:**
- `N:1` → Customer
- `N:1` → User

---

## Entity Relationships Summary

### Customer (Multi-tenant Root)
```
Customer
  ├── Users (1:N)
  ├── Accounts (1:N)
  │     └── StandardAccount (N:1 template)
  ├── Invoices (1:N)
  │     └── InvoiceLines (1:N)
  │           └── Account (N:1 revenue account)
  ├── JournalEntries (1:N)
  │     └── JournalEntryLines (1:N)
  │           └── Account (N:1)
  ├── Payments (1:N)
  │     └── Invoice (N:1)
  ├── VatReports (1:N)
  └── AuditLog (1:N)
```

---

## Data Constraints & Rules

### 1. Multi-Tenancy
- **Every** entity (except StandardAccount) links to Customer
- All queries filter by customer_id automatically
- No cross-customer data access possible

### 2. Unique Constraints
- `customers.organization_number` - UNIQUE
- `users.username` - UNIQUE
- `accounts(customer_id, account_number)` - UNIQUE
- `invoices.invoice_number` - UNIQUE
- `journal_entries(customer_id, entry_number)` - UNIQUE

### 3. Check Constraints
- `journal_entry_lines`: Either debit OR credit > 0, not both
- `journal_entries`: SUM(debit) = SUM(credit) (enforced in application)

### 4. Soft Delete
- Users, Accounts, Customers use `active` flag
- No hard deletes to maintain audit trail

### 5. Cascading Rules
- Delete Customer → CASCADE all related data
- Delete JournalEntry → CASCADE all lines
- Delete Invoice → CASCADE all lines
- Users and Accounts → Soft delete only

---

## Data Flow Examples

### Creating an Invoice
```
1. User creates Invoice (status: DRAFT)
2. Add InvoiceLines with VAT calculation
3. Calculate totals (subtotal + VAT)
4. Mark as SENT
5. Auto-generate JournalEntry:
   - Debit: Account 1500 (Customer receivable)
   - Credit: Account 3000 (Sales revenue)
   - Credit: Account 2700 (VAT outgoing)
6. Update status to SENT
```

### Recording a Payment
```
1. Create Payment record
2. Link to Invoice
3. Auto-generate JournalEntry:
   - Debit: Account 1900 (Bank)
   - Credit: Account 1500 (Customer receivable)
4. Update Invoice status to PAID
5. Set paidAt timestamp
```

### Manual Journal Entry
```
1. Accountant creates JournalEntry (status: draft)
2. Add JournalEntryLines (debits and credits)
3. Validate: SUM(debits) = SUM(credits)
4. Mark as POSTED
5. Update Account balances
6. Entry becomes immutable
```

---

## Norwegian Accounting Specifics

### NS 4102 Account Structure
```
1xxx - Assets (Eiendeler)
  1900 - Bank account (Bankkonto)
  1500 - Customer receivables (Kundefordringer)

2xxx - Liabilities & Equity (Gjeld og egenkapital)
  2700 - VAT outgoing (Utgående MVA)
  2740 - VAT incoming (Inngående MVA)

3xxx - Revenue (Inntekter)
  3000 - Sales revenue (Salgsinntekt)

4xxx-6xxx - Expenses (Kostnader)
7xxx - Financial items (Finans)
8xxx - Tax (Skatt)
```

### VAT Rates in Norway
- **25%**: Standard rate (most goods and services)
- **15%**: Reduced rate (food, beverages)
- **0%**: Zero-rated (exports, newspapers)
- **Exempt**: VAT-exempt (healthcare, education)

---

## JSON Examples

### Login Request/Response
```json
// Request
{
  "username": "testuser",
  "password": "password",
  "deviceType": "web"
}

// Response
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGci...",
  "userId": 1,
  "username": "testuser",
  "customerId": 1,
  "role": "ADMIN",
  "expiresIn": 86400
}
```

### Account
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

### Invoice
```json
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
  "lines": [
    {
      "description": "Consulting services",
      "quantity": 10,
      "unitPrice": 1000.00,
      "vatRate": 25.00,
      "vatAmount": 2500.00,
      "lineTotal": 12500.00
    }
  ]
}
```

---

## Performance Considerations

### Indexes
All foreign keys are indexed:
- `customer_id` on all multi-tenant tables
- `account_id` on journal_entry_lines, invoice_lines
- `journal_entry_id` on journal_entry_lines
- `invoice_id` on invoice_lines, payments

### Query Optimization
- Always filter by `customer_id` first (index scan)
- Use prepared statements (Hibernate Panache)
- Pagination for large result sets
- Async queries with Mutiny (non-blocking)

### Balance Calculation
- Account balances cached in `accounts.balance`
- Updated via triggers or application logic
- Recalculated from journal_entry_lines if needed

---

## Security & Data Protection

### Sensitive Data
- **Passwords**: BCrypt hashed, never stored plain text
- **JWT Private Key**: Stored securely, never exposed via API
- **Organization Numbers**: Business ID (public in Norway)
- **Financial Data**: Protected by multi-tenancy + JWT

### Audit Trail
- All CUD operations logged in `audit_log`
- Old and new values stored as JSONB
- User and timestamp tracked
- 5-year retention (Norwegian bookkeeping law)

### Data Retention
- **Minimum**: 5 years (Bokføringsloven)
- Soft delete preserves audit trail
- Regular backups with point-in-time recovery

---

## Future Enhancements

### Planned Features
1. **Attachments**: Document management (receipts, contracts)
2. **Bank Integration**: Auto-import transactions
3. **Multi-Currency**: Full foreign exchange support
4. **Projects**: Cost tracking per project
5. **Budgets**: Budget vs. actual reporting
6. **Recurring Invoices**: Auto-generate monthly invoices
7. **Payment Reminders**: Auto-email for overdue invoices
8. **E-Invoice**: EHF format support (Norwegian standard)
9. **Altinn Integration**: Direct tax reporting

---

## Conclusion

The Snabel Accounting System data model is designed for:
- **Norwegian compliance**: NS 4102, VAT, organization numbers
- **Multi-tenancy**: Complete customer isolation
- **Auditability**: Full change tracking
- **Performance**: Indexed queries, async operations
- **Flexibility**: Customizable chart of accounts
- **Security**: BCrypt + JWT + row-level isolation
