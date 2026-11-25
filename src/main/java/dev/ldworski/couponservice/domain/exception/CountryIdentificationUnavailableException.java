package dev.ldworski.couponservice.domain.exception;

public class CountryIdentificationUnavailableException extends RuntimeException {

    public CountryIdentificationUnavailableException(Throwable cause) {
        super("Country identification is temporarily unavailable", cause);
    }
}
