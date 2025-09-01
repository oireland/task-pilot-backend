ALTER TABLE plans
    ADD max_file_size INTEGER;

ALTER TABLE plans
    ALTER COLUMN max_file_size SET NOT NULL;