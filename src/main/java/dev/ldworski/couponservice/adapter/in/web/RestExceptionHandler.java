package dev.ldworski.couponservice.adapter.in.web;

import dev.ldworski.couponservice.domain.exception.CountryIdentificationUnavailableException;
import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException;
import dev.ldworski.couponservice.domain.exception.CouponCodeAlreadyExistsException;
import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException;
import dev.ldworski.couponservice.domain.exception.CouponNotFoundException;
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

@RestControllerAdvice
// TODO przemyśleć walidacje i ich obsługę
class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final String RETRY_AFTER_SECONDS = "1";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(MethodArgumentNotValidException exception) {
        return validationProblemDetail(exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .toList());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        return validationProblemDetail(exception.getParameterValidationResults().stream()
                .flatMap(result -> result instanceof ParameterErrors parameterErrors
                        ? parameterErrors.getFieldErrors().stream()
                                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                        : result.getResolvableErrors().stream()
                                .map(error -> new FieldValidationError(
                                        result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof MismatchedInputException mismatchedInput) {
            return validationProblemDetail(
                    List.of(new FieldValidationError(fieldPath(mismatchedInput), mismatchMessage(mismatchedInput))));
        }
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Failed to read request");
        problemDetail.setTitle("Malformed request");
        return problemDetail;
    }

    @ExceptionHandler(CouponCodeAlreadyExistsException.class)
    ProblemDetail handleCouponCodeAlreadyExists(CouponCodeAlreadyExistsException exception) {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Coupon code already exists");
        problemDetail.setProperty("code", exception.getCode().value());
        return problemDetail;
    }

    @ExceptionHandler(CouponNotFoundException.class)
    ProblemDetail handleCouponNotFound(CouponNotFoundException exception) {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Coupon not found");
        problemDetail.setProperty("code", exception.getCode().value());
        return problemDetail;
    }

    @ExceptionHandler(CouponNotAvailableInLocationException.class)
    ProblemDetail handleCouponNotAvailableInLocation(CouponNotAvailableInLocationException exception) {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        problemDetail.setTitle("Coupon not available in location");
        problemDetail.setProperty("code", exception.getCode().value());
        return problemDetail;
    }

    @ExceptionHandler(CouponAlreadyRedeemedException.class)
    ProblemDetail handleCouponAlreadyRedeemed(CouponAlreadyRedeemedException exception) {
        var problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Coupon has already been redeemed by this user");
        problemDetail.setTitle("Coupon already redeemed");
        return problemDetail;
    }

    @ExceptionHandler(CouponUsageLimitReachedException.class)
    ProblemDetail handleCouponUsageLimitReached(CouponUsageLimitReachedException exception) {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Coupon usage limit reached");
        problemDetail.setProperty("code", exception.getCode().value());
        return problemDetail;
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handlePessimisticLockingFailure(PessimisticLockingFailureException exception) {
        log.info("Lock not acquired in time: {}", exception.getMessage());
        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Resource is temporarily locked, try again later");
        problemDetail.setTitle("Resource temporarily unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(problemDetail);
    }

    @ExceptionHandler(CountryIdentificationUnavailableException.class)
    ResponseEntity<ProblemDetail> handleCountryIdentificationUnavailable(
            CountryIdentificationUnavailableException exception) {
        log.warn("Country identification failed", exception);
        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Unable to identify user's location, try again later");
        problemDetail.setTitle("Location service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .headers(errorResponse.getHeaders())
                    .body(errorResponse.getBody());
        }
        log.warn("Unexpected error while processing request", exception);
        var problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred");
        problemDetail.setTitle("Internal server error");
        return ResponseEntity.internalServerError().body(problemDetail);
    }

    private static ProblemDetail validationProblemDetail(List<FieldValidationError> errors) {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problemDetail.setTitle("Validation error");
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    private static String fieldPath(MismatchedInputException exception) {
        return exception.getPath().stream()
                .map(reference -> reference.getPropertyName() != null
                        ? reference.getPropertyName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining("."))
                .replace(".[", "[");
    }

    private static String mismatchMessage(MismatchedInputException exception) {
        if (exception instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum()) {
            return "unrecognized value";
        }
        return "is missing or has invalid value";
    }

    public record FieldValidationError(String field, String message) {}
}
