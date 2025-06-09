package io.slicekit.validation;

import jakarta.validation.ConstraintViolation;

import java.util.Collections;
import java.util.Set;

/**
 * Result of validation operation containing either success or constraint violations.
 */
public final class ValidationResult {
    
    private final boolean valid;
    private final Set<ConstraintViolation<Object>> violations;
    
    private ValidationResult(final boolean valid, final Set<ConstraintViolation<Object>> violations) {
        this.valid = valid;
        this.violations = violations != null ? violations : Collections.emptySet();
    }
    
    /**
     * Creates a valid result.
     * 
     * @return valid validation result
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptySet());
    }
    
    /**
     * Creates an invalid result with constraint violations.
     * 
     * @param violations the constraint violations
     * @return invalid validation result
     */
    public static ValidationResult invalid(final Set<ConstraintViolation<Object>> violations) {
        return new ValidationResult(false, violations);
    }
    
    /**
     * Checks if validation was successful.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets the constraint violations.
     * 
     * @return set of constraint violations (empty if valid)
     */
    public Set<ConstraintViolation<Object>> getViolations() {
        return violations;
    }
    
    /**
     * Gets the number of constraint violations.
     * 
     * @return violation count
     */
    public int getViolationCount() {
        return violations.size();
    }
}