CREATE TABLE IF NOT EXISTS coupon
(
    id                uuid         NOT NULL,
    code              varchar(255) NOT NULL,
    current_usage     integer      NOT NULL DEFAULT 0,
    max_allowed_usage integer      NOT NULL,
    country_code      varchar(2)   NOT NULL,
    created           timestamptz  NOT NULL,
    CONSTRAINT pk_coupon PRIMARY KEY (id),
    CONSTRAINT uq_coupon_code UNIQUE (code),
    CONSTRAINT ck_coupon_code_format CHECK (code ~ '^[A-Z0-9]+$'),
    CONSTRAINT ck_coupon_current_usage_not_negative CHECK (current_usage >= 0),
    CONSTRAINT ck_coupon_max_allowed_usage_positive CHECK (max_allowed_usage > 0),
    CONSTRAINT ck_coupon_current_usage_within_limit CHECK (current_usage <= max_allowed_usage)
);
