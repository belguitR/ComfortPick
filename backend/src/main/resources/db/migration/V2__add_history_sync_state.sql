ALTER TABLE riot_accounts
    ADD COLUMN auto_sync_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'IDLE',
    ADD COLUMN sync_target_match_count INT NOT NULL DEFAULT 500,
    ADD COLUMN sync_backfill_cursor INT NOT NULL DEFAULT 0,
    ADD COLUMN sync_next_run_at TIMESTAMP NULL,
    ADD COLUMN sync_last_sync_at TIMESTAMP NULL,
    ADD COLUMN sync_last_error_code TEXT NULL,
    ADD COLUMN sync_last_error_message TEXT NULL;

CREATE INDEX ix_riot_accounts_sync_next_run_at
    ON riot_accounts (sync_next_run_at);
