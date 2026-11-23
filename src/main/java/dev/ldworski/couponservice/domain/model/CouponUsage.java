package dev.ldworski.couponservice.domain.model;

public record CouponUsage(int current, int maxAllowed) {

    public CouponUsage {
        if (maxAllowed <= 0) {
            throw new IllegalArgumentException("It should be possible to use a coupon at least once");
        }
        if (current < 0) {
            throw new IllegalArgumentException("current usages cannot be negative");
        }
        if (current > maxAllowed) {
            throw new IllegalArgumentException("current usages cannot exceed max allowed usage");
        }
    }

    public static CouponUsage unused(int maxAllowed) {
        return new CouponUsage(0, maxAllowed);
    }

    public CouponUsage incremented() {
        return new CouponUsage(current + 1, maxAllowed);
    }

    public boolean isLimitReached() {
        return current >= maxAllowed;
    }
}
