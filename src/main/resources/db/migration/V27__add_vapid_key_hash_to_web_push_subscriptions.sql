ALTER TABLE web_push_subscriptions
    ADD COLUMN vapid_key_hash CHAR(64) NULL AFTER endpoint_hash,
    ADD KEY idx_web_push_user_vapid_key (user_id, enabled, vapid_key_hash);
