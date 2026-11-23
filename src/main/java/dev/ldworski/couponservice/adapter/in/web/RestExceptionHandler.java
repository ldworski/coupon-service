package dev.ldworski.couponservice.adapter.in.web;

import dev.ldworski.couponservice.domain.exception.CountryIdentificationUnavailableException;
import dev.ldworski.couponservice.domain.exception.CouponAlreadyRedeemedException;
import dev.ldworski.couponservice.domain.exception.CouponCodeAlreadyExistsException;
import dev.ldworski.couponservice.domain.exception.CouponNotAvailableInLocationException;
import dev.ldworski.couponservice.domain.exception.CouponNotFoundException;
import dev.ldworski.couponservice.domain.exception.CouponUsageLimitReachedException;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
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

    private final Tracer tracer;

    RestExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }

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
        return problemDetail(HttpStatus.BAD_REQUEST, "Malformed request", "Failed to read request");
    }

    @ExceptionHandler(CouponCodeAlreadyExistsException.class)
    ProblemDetail handleCouponCodeAlreadyExists(CouponCodeAlreadyExistsException exception) {
        return problemDetail(HttpStatus.CONFLICT, "Coupon code already exists", exception.getMessage());
    }

    @ExceptionHandler(CouponNotFoundException.class)
    ProblemDetail handleCouponNotFound(CouponNotFoundException exception) {
        return problemDetail(HttpStatus.NOT_FOUND, "Coupon not found", exception.getMessage());
    }

    @ExceptionHandler(CouponNotAvailableInLocationException.class)
    ProblemDetail handleCouponNotAvailableInLocation(CouponNotAvailableInLocationException exception) {
        return problemDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, "Coupon not available in location", exception.getMessage());
    }

    @ExceptionHandler(CouponAlreadyRedeemedException.class)
    ProblemDetail handleCouponAlreadyRedeemed(CouponAlreadyRedeemedException exception) {
        return problemDetail(
                HttpStatus.CONFLICT, "Coupon already redeemed", "Coupon has already been redeemed by this user");
    }

    @ExceptionHandler(CouponUsageLimitReachedException.class)
    ProblemDetail handleCouponUsageLimitReached(CouponUsageLimitReachedException exception) {
        return problemDetail(HttpStatus.CONFLICT, "Coupon usage limit reached", exception.getMessage());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handlePessimisticLockingFailure(PessimisticLockingFailureException exception) {
        log.info("Lock not acquired in time: {}", exception.getMessage());
        var problemDetail = problemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Resource temporarily unavailable",
                "Resource is temporarily locked, try again later");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(problemDetail);
    }

    @ExceptionHandler(CountryIdentificationUnavailableException.class)
    ResponseEntity<ProblemDetail> handleCountryIdentificationUnavailable(
            CountryIdentificationUnavailableException exception) {
        log.warn("Country identification failed", exception);
        var problemDetail = problemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Location service unavailable",
                "Unable to identify user's location, try again later");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .headers(errorResponse.getHeaders())
                    .body(withMetadata(errorResponse.getBody()));
        }
        log.warn("Unexpected error while processing request", exception);
        var problemDetail =
                problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Unexpected error occurred");
        return ResponseEntity.internalServerError().body(problemDetail);
    }

    private ProblemDetail problemDetail(HttpStatus status, String title, String detail) {
        var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        // immutability?
        return withMetadata(problemDetail);
    }

    private ProblemDetail withMetadata(ProblemDetail problemDetail) {
        problemDetail.setProperty("timestamp", Instant.now());
        var currentSpan = tracer.currentSpan();
        if (currentSpan != null && !currentSpan.context().traceId().isEmpty()) {
            problemDetail.setProperty("correlationId", currentSpan.context().traceId());
        }
        return problemDetail;
    }

    private ProblemDetail validationProblemDetail(List<FieldValidationError> errors) {
        var problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Validation error", "Request validation failed");
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
