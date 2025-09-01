-- Add the maxFileSize column to the plans table
ALTER TABLE plans ADD COLUMN max_file_size INT NOT NULL DEFAULT 1000000;

-- Update the existing plans with their respective max file sizes
UPDATE plans SET max_file_size = 1000000 WHERE name = 'Free';
UPDATE plans SET max_file_size = 3000000 WHERE name = 'Pro';