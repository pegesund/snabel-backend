-- Add status column to projects table
ALTER TABLE projects
ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Add comment to explain the column
COMMENT ON COLUMN projects.status IS 'Project status: ACTIVE, COMPLETED, ON_HOLD, CANCELLED';
