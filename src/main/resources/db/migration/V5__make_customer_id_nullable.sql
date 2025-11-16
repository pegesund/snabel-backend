-- Make customer_id nullable for development (temporarily)
-- This allows creating departments and projects without authentication

ALTER TABLE departments ALTER COLUMN customer_id DROP NOT NULL;
ALTER TABLE projects ALTER COLUMN customer_id DROP NOT NULL;
