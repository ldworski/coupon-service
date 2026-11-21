package dev.ldworski.couponservice.adapter.in.web.validation;

import dev.ldworski.couponservice.domain.model.IpAddress;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IpAddressValidator implements ConstraintValidator<ValidIpAddress, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            //TODO nie podoba mi się to
            new IpAddress(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
