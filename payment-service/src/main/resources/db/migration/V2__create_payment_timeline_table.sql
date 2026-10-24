CREATE TABLE IF NOT EXISTS payment_timeline (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    failure_reason VARCHAR(255),
    CONSTRAINT fk_timeline_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_timeline_payment_id ON payment_timeline (payment_id);
