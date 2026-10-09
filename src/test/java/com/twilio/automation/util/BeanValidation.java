package com.twilio.automation.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thin wrapper around Hibernate Validator — the Java analogue to pydantic's declarative
 * constraints (@NotBlank, @Pattern, @DecimalMin/@Max on the model records) for expressing
 * semantic assertions with readable failure messages.
 */
public final class BeanValidation {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private BeanValidation() {}

    public static <T> void assertValid(T value) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new AssertionError("Bean validation failed: " + message);
        }
    }
}
