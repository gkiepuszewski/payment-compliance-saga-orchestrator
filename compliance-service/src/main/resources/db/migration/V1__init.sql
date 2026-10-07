CREATE TABLE outbox_messages (
    id           UUID PRIMARY KEY,
    saga_id      UUID           NOT NULL,
    event_type   VARCHAR(40)    NOT NULL,
    payload_json TEXT           NOT NULL,
    target_url   VARCHAR(255)   NOT NULL,
    status       VARCHAR(10)    NOT NULL,
    attempts     INTEGER        NOT NULL,
    last_error   TEXT,
    created_at   TIMESTAMPTZ    NOT NULL,
    sent_at      TIMESTAMPTZ
);

-- Supports the OutboxRelay's "find PENDING rows, oldest first" polling query.
CREATE INDEX idx_outbox_messages_status_created_at ON outbox_messages (status, created_at);

CREATE TABLE inbox_messages (
    id          UUID PRIMARY KEY,
    type        VARCHAR(40)    NOT NULL,
    received_at TIMESTAMPTZ    NOT NULL,
    status      VARCHAR(10)    NOT NULL
);
