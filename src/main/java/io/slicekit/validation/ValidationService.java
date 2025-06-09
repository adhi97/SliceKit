package io.slicekit.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for validating request objects using Bean Validation (JSR-303/380).
 * 
 * This service provides slice-aware validation capabilities while leveraging
 * the industry-standard Bean Validation framework.
 */
public final class ValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    
    private final Validator validator;
    
    /**
     * Creates a validation service with the default validator factory.
     */
    public ValidationService() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        logger.debug("ValidationService initialized with Hibernate Validator");
    }
    
    /**
     * Validates an object and returns validation result.
     * 
     * @param object the object to validate
     * @return validation result containing any constraint violations
     */
    public ValidationResult validate(final Object object) {
        if (object == null) {
            logger.debug("Validation skipped for null object");
            return ValidationResult.valid();
        }
        
        logger.debug("Validating object of type: {}", object.getClass().getSimpleName());
        
        final Set<ConstraintViolation<Object>> violations = validator.validate(object);
        
        if (violations.isEmpty()) {
            logger.debug("Validation passed for object: {}", object.getClass().getSimpleName());
            return ValidationResult.valid();
        }
        
        logger.debug("Validation failed with {} violations for object: {}", 
            violations.size(), object.getClass().getSimpleName());
        
        return ValidationResult.invalid(violations);
    }
    
    /**
     * Validates an object and throws ValidationException if invalid.
     * 
     * @param object the object to validate
     * @throws ValidationException if validation fails
     */
    public void validateAndThrow(final Object object) throws ValidationException {
        final ValidationResult result = validate(object);
        if (!result.isValid()) {
            throw new ValidationException(result.getViolations());
        }
    }
    
    /**
     * Creates a formatted error message from constraint violations.
     * 
     * @param violations the constraint violations
     * @return formatted error message
     */
    public String formatViolations(final Set<ConstraintViolation<Object>> violations) {
        return violations.stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));
    }
}