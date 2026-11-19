package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.model.CountryCode;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "coupon")
public class CouponEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(nullable = false, updatable = false)
    private String code;

    @Column(nullable = false)
    private int currentUsage;

    @Column(nullable = false, updatable = false)
    private int maxAllowedUsage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CountryCode countryCode;

    @Column(nullable = false, updatable = false)
    private Instant created;

    protected CouponEntity() {}

    protected CouponEntity(
            UUID id, String code, int maxAllowedUsage, int currentUsage, CountryCode countryCode, Instant created) {
        this.id = id;
        this.code = code;
        this.maxAllowedUsage = maxAllowedUsage;
        this.currentUsage = currentUsage;
        this.countryCode = countryCode;
        this.created = created;
    }

    @Override
    public UUID getId() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCurrentUsage() {
        return currentUsage;
    }

    public void setCurrentUsage(int currentUsage) {
        this.currentUsage = currentUsage;
    }

    public int getMaxAllowedUsage() {
        return maxAllowedUsage;
    }

    public void setMaxAllowedUsage(int maxAllowedUsage) {
        this.maxAllowedUsage = maxAllowedUsage;
    }

    public Instant getCreated() {
        return created;
    }

    public CountryCode getCountryCode() {
        return countryCode;
    }
}
