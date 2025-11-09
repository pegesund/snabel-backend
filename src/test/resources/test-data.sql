-- Test data for Snabel Accounting System

-- Insert test customer
INSERT INTO customers (id, organization_number, company_name, contact_person, email, phone, city, country, created_at, updated_at, active)
VALUES (1, '123456789', 'Test Company AS', 'John Doe', 'test@example.com', '+4712345678', 'Oslo', 'Norge', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Insert test user (password is 'snabeltann' bcrypt hashed)
INSERT INTO users (id, customer_id, username, password_hash, email, full_name, role, created_at, updated_at, active)
VALUES (1, 1, 'snabel', '$2a$10$vPPqE3qM2OqP3YhHPNqNJ.iJY7YfqFqO0mJ2xC1vPr9wZqTqY1s9G', 'snabel@example.com', 'Snabel User', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Insert standard accounts (Norwegian NS 4102)
INSERT INTO standard_accounts (id, account_number, account_name, account_type, account_class, vat_code, description, is_system, active) VALUES
(1, '1500', 'Varelager', 'ASSET', '1', NULL, 'Varebeholdning', true, true),
(2, '1600', 'Kundefordringer', 'ASSET', '1', NULL, 'Fordringer på kunder', true, true),
(3, '1900', 'Bank', 'ASSET', '1', NULL, 'Bankinnskudd', true, true),
(4, '2400', 'Leverandørgjeld', 'LIABILITY', '2', NULL, 'Gjeld til leverandører', true, true),
(5, '2700', 'Skyldig MVA', 'LIABILITY', '2', NULL, 'Merverdiavgift', true, true),
(6, '3000', 'Salgsinntekt', 'REVENUE', '3', '3', 'Salgsinntekt 25% MVA', true, true),
(7, '4000', 'Varekjøp', 'EXPENSE', '4', '3', 'Innkjøp av varer', true, true),
(8, '6300', 'Strøm', 'EXPENSE', '6', '3', 'Elektrisitet og oppvarming', true, true);

-- Insert customer accounts
INSERT INTO accounts (id, customer_id, standard_account_id, account_number, account_name, account_type, vat_code, balance, currency, created_at, updated_at, active) VALUES
(1, 1, 3, '1900', 'Bankkonto Hovedkonto', 'ASSET', NULL, 100000.00, 'NOK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
(2, 1, 2, '1600', 'Kundefordringer', 'ASSET', NULL, 0.00, 'NOK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
(3, 1, 6, '3000', 'Salgsinntekt', 'REVENUE', '3', 0.00, 'NOK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
(4, 1, 5, '2700', 'Skyldig MVA', 'LIABILITY', NULL, 0.00, 'NOK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Reset sequences
ALTER SEQUENCE customers_id_seq RESTART WITH 2;
ALTER SEQUENCE users_id_seq RESTART WITH 2;
ALTER SEQUENCE standard_accounts_id_seq RESTART WITH 9;
ALTER SEQUENCE accounts_id_seq RESTART WITH 5;
