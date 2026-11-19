CREATE TABLE IF NOT EXISTS coupon
(
    id                uuid         NOT NULL PRIMARY KEY,
    code              varchar(255) NOT NULL CONSTRAINT uq_coupon_code UNIQUE,
    current_usage     integer      NOT NULL DEFAULT 0 CHECK (current_usage >= 0),
    max_allowed_usage integer      NOT NULL CHECK (max_allowed_usage > 0),
    country_code      varchar(2)   NOT NULL,
    created           timestamptz  NOT NULL
);
