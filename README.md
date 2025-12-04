# Snabel Accounting System - Backend

A modern Norwegian accounting system built with Quarkus, fully async with reactive PostgreSQL, JWT authentication, and compliance with Norwegian accounting standard NS 4102.

## Features

- **Fully Async**: All database operations use Mutiny reactive programming with Hibernate Reactive
- **Norwegian Accounting Standard**: Pre-seeded with NS 4102 standard chart of accounts
- **JWT Authentication**: Token-based auth with different expiration times for web (24h) and mobile (30 days)
- **Multi-tenant**: Customer isolation with all API calls validated against JWT token
- **RESTful API**: Complete CRUD operations for accounts, invoices, journal entries
- **Database Migrations**: Flyway migrations for versioned schema management
- **Role-Based Access**: USER, ADMIN, and ACCOUNTANT roles with different permissions

## Quick Start

```bash
# 1. Set up database (using existing PostgreSQL with postgres/postgres credentials)
PGPASSWORD=postgres psql -h localhost -U postgres -d postgres <<EOF
CREATE USER snabel WITH PASSWORD 'snabel';
CREATE DATABASE snabel_accounting OWNER snabel;
GRANT ALL PRIVILEGES ON DATABASE snabel_accounting TO snabel;
\c snabel_accounting
GRANT ALL ON SCHEMA public TO snabel;
ALTER SCHEMA public OWNER TO snabel;
EOF

# 2. Run the application
./mvnw quarkus:dev
```

The application will start on http://localhost:8080

### Database Connection Details
- **Host**: localhost
- **Port**: 5432
- **Database**: snabel_accounting
- **Username**: snabel
- **Password**: snabel
- **PostgreSQL Admin**: postgres/postgres (for setup only)

## API Examples

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password",
    "deviceType": "web"
  }'
```

### List Accounts (with JWT token)
```bash
curl -X GET http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## Project Structure

```
accounting-backend/
├── src/main/java/no/snabel/
│   ├── model/          # Entities: Customer, User, Account, Invoice
│   ├── resource/       # REST endpoints
│   ├── service/        # Business logic
│   ├── security/       # JWT token service
│   └── dto/            # Data transfer objects
├── src/main/resources/
│   ├── db/migration/   # Flyway SQL migrations
│   └── application.properties
└── src/test/          # Comprehensive test suite
```

## Technology Stack

- Quarkus 3.29.2
- PostgreSQL 16 with async reactive driver
- Hibernate Reactive with Panache
- SmallRye JWT for authentication
- Flyway for migrations

## Norwegian Accounting (NS 4102)

Pre-seeded with standard accounts:
- Class 1: Assets
- Class 2: Equity & Liabilities
- Class 3: Operating Income (with Norwegian VAT: 0%, 15%, 25%)
- Class 4-8: Expenses, Payroll, Financial items, Tax

## Testing

```bash
./mvnw test
```

Tests include:
- Authentication flow
- Account CRUD operations
- Invoice lifecycle (create, send, mark paid)
- Role-based access control

## Creating EHF- and pdf-invoices

### Download pdf-invoice

Downloads the invoice and saves it to 'invoice-123.pdf' in the working directory.
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/invoices/123/pdf \
  -o invoice-123.pdf
```

### Download EHF-invoice

Downloads the invoice and saves it to 'invoice-123.xml' in the working directory.
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/invoices/123/efaktura \
  -o invoice-123.xml
```

## For More Information

See full documentation in the README or visit https://quarkus.io/
