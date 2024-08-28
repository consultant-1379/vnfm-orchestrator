ALTER TABLE task ADD COLUMN last_update_time TIMESTAMP NOT NULL;

UPDATE task SET last_update_time = NOW();