package dev.ldworski.couponservice.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "coupon_redemption")
public class CouponRedemptionEntity implements Persistable<CouponRedemptionId> {

    @EmbeddedId
    private CouponRedemptionId id;

    @Transient
    private boolean isNew = true;

    @Column(nullable = false, updatable = false)
    private Instant created;

    protected CouponRedemptionEntity() {}

    protected CouponRedemptionEntity(CouponRedemptionId id, Instant created) {
        this.id = id;
        this.created = created;
    }

    @Override
    public CouponRedemptionId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public Instant getCreated() {
        return created;
    }
}
