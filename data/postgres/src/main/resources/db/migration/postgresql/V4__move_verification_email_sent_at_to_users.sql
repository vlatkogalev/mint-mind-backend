ALTER TABLE users
    ADD COLUMN IF NOT EXISTS verification_email_sent_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE profiles
    DROP COLUMN IF EXISTS verification_email_sent_at;
