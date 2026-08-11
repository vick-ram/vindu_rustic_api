ALTER TABLE users
    RENAME COLUMN phone TO phone_number;

ALTER TABLE users
    RENAME COLUMN avatar TO avatar_url;

ALTER TABLE users
    RENAME COLUMN enable_2fa TO two_factor_enabled;


ALTER TABLE users
    ADD COLUMN two_factor_method VARCHAR(20),
    ADD COLUMN notification_preferences JSONB NOT NULL DEFAULT '{}'::JSONB;


ALTER TABLE users
    ADD CONSTRAINT chk_two_factor_method
        CHECK (
            two_factor_method IS NULL
                OR two_factor_method IN ('app', 'sms', 'email')
            );