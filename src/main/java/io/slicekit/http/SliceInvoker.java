package io.slicekit.http;

import io.slicekit.core.SliceMetadata;
import io.slicekit.injection.SliceInjector;
import io.slicekit.validation.ValidationException;
import io.slicekit.validation.ValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Handles slice method invocation and argument preparation.
 * 
 * Responsible for:
 * - Preparing method arguments from HTTP requests
 * - Validating request parameters with @Valid annotation
 * - Invoking slice handler methods
 * - Creating slice instances via dependency injection
 */
public final class SliceInvoker {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceInvoker.class);
    
    private final SliceInjector sliceInjector;
    private final ValidationService validationService;
    
    /**
     * Creates a new slice invoker.
     * 
     * @param sliceInjector dependency injector for creating slice instances, must not be null
     * @param validationService validation service for @Valid parameters, must not be null
     * @throws IllegalArgumentException if parameters are null
     */
    public SliceInvoker(final SliceInjector sliceInjector, final ValidationService validationService) {
        this.sliceInjector = Objects.requireNonNull(sliceInjector, "Slice injector cannot be null");
        this.validationService = Objects.requireNonNull(validationService, "Validation service cannot be null");
    }
    
    /**
     * Invokes a slice handler method with the given request.
     * 
     * @param sliceMetadata metadata of the slice to invoke, must not be null
     * @param request HTTP request wrapper, must not be null
     * @return result from the slice handler method
     * @throws Exception if slice invocation fails
     * @throws ValidationException if request validation fails
     */
    public Object invokeSlice(final SliceMetadata sliceMetadata, final SliceRequest request) throws Exception {
        Objects.requireNonNull(sliceMetadata, "Slice metadata cannot be null");
        Objects.requireNonNull(request, "Slice request cannot be null");
        
        logger.debug("Invoking slice: {}", sliceMetadata.getName());
        
        try {
            // Create slice instance using dependency injection
            final var sliceInstance = sliceInjector.createSliceInstance(sliceMetadata);
            
            // Prepare method arguments
            final var args = prepareMethodArguments(sliceMetadata.getHandlerMethod(), request);
            
            // Invoke handler method
            final var result = sliceMetadata.getHandlerMethod().invoke(sliceInstance, args);
            
            logger.debug("Successfully invoked slice: {}", sliceMetadata.getName());
            return result;
            
        } catch (Exception e) {
            logger.error("Error invoking slice {}: {}", sliceMetadata.getName(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Prepares method arguments for slice handler invocation.
     * 
     * Supports zero arguments or single parameter (request body deserialization).
     * Validates parameters annotated with @Valid.
     * 
     * @param handlerMethod method to prepare arguments for, must not be null
     * @param request HTTP request wrapper, must not be null
     * @return array of prepared arguments
     * @throws ValidationException if validation fails for @Valid parameters
     * @throws IllegalArgumentException if method signature is unsupported
     */
    private Object[] prepareMethodArguments(final Method handlerMethod, final SliceRequest request) throws ValidationException {
        Objects.requireNonNull(handlerMethod, "Handler method cannot be null");
        Objects.requireNonNull(request, "Slice request cannot be null");
        
        final var parameters = handlerMethod.getParameters();
        
        if (parameters.length == 0) {
            // No parameters - return empty array
            return new Object[0];
        }
        
        if (parameters.length == 1) {
            // Single parameter - attempt to deserialize request body
            final var parameter = parameters[0];
            final var parameterType = parameter.getType();
            
            // Special handling for SliceRequest type
            if (parameterType.equals(SliceRequest.class)) {
                return new Object[]{request};
            }
            
            // For other types, try to deserialize from request body
            if (request.hasBody() && request.isJsonContent()) {
                return handleJsonParameter(parameter, parameterType, request);
            } else {
                // No body or not JSON - pass null for MVP
                logger.warn("Handler method expects parameter of type {} but request has no JSON body", 
                    parameterType.getSimpleName());
                return new Object[]{null};
            }
        }
        
        // Multiple parameters not supported in MVP
        throw new IllegalArgumentException(
            "Handler methods with multiple parameters are not supported in MVP. Found %d parameters in method %s"
                .formatted(parameters.length, handlerMethod.getName()));
    }
    
    /**
     * Handles JSON parameter deserialization and validation.
     */
    private Object[] handleJsonParameter(final Parameter parameter, final Class<?> parameterType, final SliceRequest request) 
            throws ValidationException {
        try {
            final Object arg = request.getBodyAs(parameterType);
            
            // Check if parameter has @Valid annotation for validation
            if (parameter.isAnnotationPresent(Valid.class) && arg != null) {
                logger.debug("Validating request parameter of type: {}", parameterType.getSimpleName());
                validationService.validateAndThrow(arg);
            }
            
            return new Object[]{arg};
        } catch (ValidationException e) {
            // Re-throw validation exceptions to be handled by caller
            throw e;
        } catch (Exception e) {
            logger.error("Failed to deserialize request body to {}: {}", 
                parameterType.getSimpleName(), e.getMessage());
            throw new IllegalArgumentException(
                "Failed to deserialize request body to " + parameterType.getSimpleName(), e);
        }
    }
}