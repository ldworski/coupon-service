package dev.ldworski.couponservice.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record CouponRedemptionId(
        @Column(nullable = false, updatable = false) UUID couponId,
        @Column(nullable = false, updatable = false) UUID userId)
{}
