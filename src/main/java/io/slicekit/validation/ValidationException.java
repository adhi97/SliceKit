package io.slicekit.validation;

import jakarta.validation.ConstraintViolation;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception thrown when request validation fails.
 * 
 * Contains the constraint violations that caused the validation to fail.
 */
public final class ValidationException extends Exception {
    
    private final Set<ConstraintViolation<Object>> violations;
    
    /**
     * Creates a validation exception with constraint violations.
     * 
     * @param violations the constraint violations
     */
    public ValidationException(final Set<ConstraintViolation<Object>> violations) {
        super(createMessage(violations));
        this.violations = violations;
    }
    
    /**
     * Gets the constraint violations.
     * 
     * @return set of constraint violations
     */
    public Set<ConstraintViolation<Object>> getViolations() {
        return violations;
    }
    
    /**
     * Gets the violation count.
     * 
     * @return number of violations
     */
    public int getViolationCount() {
        return violations.size();
    }
    
    /**
     * Creates a formatted error message from violations.
     */
    private static String createMessage(final Set<ConstraintViolation<Object>> violations) {
        if (violations.isEmpty()) {
            return "Validation failed";
        }
        
        final String violationMessages = violations.stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));
        
        return "Validation failed: " + violationMessages;
    }
}