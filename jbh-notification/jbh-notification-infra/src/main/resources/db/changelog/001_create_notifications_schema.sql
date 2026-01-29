-- Create notifications schema if not exists
CREATE SCHEMA IF NOT EXISTS notifications;

-- Notification table (singular to avoid notifications.notifications confusion)
CREATE TABLE notifications.notification
(
    id                UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),

    -- Recipient information
    recipient_id      UUID                     NOT NULL,
    recipient_email   VARCHAR(255)             NOT NULL,

    -- Sender information (optional - for system notifications, sender may be null)
    sender_user_id    UUID,
    sender_email      VARCHAR(255),

    -- Notification content
    subject           VARCHAR(500)             NOT NULL,
    message           TEXT,

    -- Classification and status
    notification_type VARCHAR(50)              NOT NULL,
    template          VARCHAR(50),
    status            VARCHAR(50)              NOT NULL DEFAULT 'PENDING',

    -- Flexible metadata storage
    metadata          JSONB,

    -- Timestamps
    sent_at           TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_notification_type_valid CHECK (
        notification_type IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')
        ),

    CONSTRAINT chk_status_valid CHECK (
        status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED', 'READ')
        ),
    CONSTRAINT chk_subject_not_empty CHECK (length(trim(subject)) > 0),
    CONSTRAINT chk_recipient_email_format CHECK (
        recipient_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
        ),
    CONSTRAINT chk_sender_email_format CHECK (
        sender_email IS NULL OR sender_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
        )
);

-- Indexes for common query patterns
CREATE INDEX idx_notification_recipient_id ON notifications.notification (recipient_id);
CREATE INDEX idx_notification_recipient_id_status ON notifications.notification (recipient_id, status);
CREATE INDEX idx_notification_recipient_id_read ON notifications.notification (recipient_id, read);
CREATE INDEX idx_notification_created_at ON notifications.notification (created_at DESC);
CREATE INDEX idx_notification_type_status ON notifications.notification (notification_type, status);
