package dev.ldworski.couponservice.adapter.in.web.validation;

import dev.ldworski.couponservice.domain.model.IpAddress;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IpAddressValidator implements ConstraintValidator<ValidIpAddress, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || IpAddress.isValid(value);
    }
}
