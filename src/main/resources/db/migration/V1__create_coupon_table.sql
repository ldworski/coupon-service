CREATE TABLE IF NOT EXISTS coupon
(
    id                uuid         NOT NULL PRIMARY KEY,
    code              varchar(255) NOT NULL
        CONSTRAINT uq_coupon_code UNIQUE
        CONSTRAINT ck_coupon_code_format CHECK (code ~ '^[A-Z0-9]+$'),
    current_usage     integer      NOT NULL DEFAULT 0 CHECK (current_usage >= 0),
    max_allowed_usage integer      NOT NULL CHECK (max_allowed_usage > 0),
    country_code      varchar(2)   NOT NULL,
    created           timestamptz  NOT NULL,
    CONSTRAINT ck_coupon_current_usage_within_limit CHECK (current_usage <= max_allowed_usage)
);

-- TODO ujednolicić tworzenie CONSTRAINT
