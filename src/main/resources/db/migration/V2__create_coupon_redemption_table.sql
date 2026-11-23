CREATE TABLE IF NOT EXISTS coupon_redemption
(
    coupon_id uuid        NOT NULL
        CONSTRAINT fk_coupon_redemption_coupon REFERENCES coupon (id),
    user_id   uuid        NOT NULL,
    created   timestamptz NOT NULL,
    CONSTRAINT pk_coupon_redemption PRIMARY KEY (coupon_id, user_id)
);
