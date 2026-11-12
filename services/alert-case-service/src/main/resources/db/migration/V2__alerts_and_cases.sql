CREATE TABLE alert (
                       id           UUID PRIMARY KEY,
                       alert_key    VARCHAR(64)  NOT NULL UNIQUE,
                       rule         VARCHAR(32)  NOT NULL,
                       trader_id    VARCHAR(32)  NOT NULL,
                       symbol       VARCHAR(16),
                       severity     INT          NOT NULL,
                       status       VARCHAR(16)  NOT NULL DEFAULT 'NEW',
                       window_start TIMESTAMPTZ,
                       window_end   TIMESTAMPTZ,
                       created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_alert_filters ON alert (status, rule, created_at DESC);
CREATE INDEX idx_alert_trader ON alert (trader_id);

CREATE TABLE surveillance_case (
                                   id         UUID PRIMARY KEY,
                                   title      VARCHAR(200) NOT NULL,
                                   status     VARCHAR(16)  NOT NULL,
                                   assignee   VARCHAR(64),
                                   priority   INT          NOT NULL DEFAULT 3,
                                   opened_by  VARCHAR(64)  NOT NULL,
                                   opened_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                   closed_at  TIMESTAMPTZ,
                                   version    BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE case_alert (
                            case_id  UUID NOT NULL REFERENCES surveillance_case (id),
                            alert_id UUID NOT NULL REFERENCES alert (id),
                            PRIMARY KEY (case_id, alert_id)
);

CREATE TABLE case_event (
                            id      BIGSERIAL PRIMARY KEY,
                            case_id UUID        NOT NULL REFERENCES surveillance_case (id),
                            actor   VARCHAR(64) NOT NULL,
                            action  VARCHAR(32) NOT NULL,
                            detail  JSONB,
                            at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_case_event_case ON case_event (case_id, at);