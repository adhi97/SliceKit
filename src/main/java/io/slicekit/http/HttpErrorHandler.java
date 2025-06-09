package io.slicekit.http;

import io.slicekit.discovery.SliceRegistry;
import io.slicekit.validation.ValidationException;
import io.slicekit.validation.ValidationService;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Centralized HTTP error response handler.
 * 
 * Responsible for:
 * - Creating appropriate HTTP error responses
 * - Logging errors consistently
 * - JSON serialization of error responses
 */
public final class HttpErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpErrorHandler.class);
    
    private final JsonService jsonService;
    private final SliceRegistry sliceRegistry;
    private final ValidationService validationService;
    
    /**
     * Creates a new HTTP error handler.
     * 
     * @param jsonService JSON serialization service, must not be null
     * @param sliceRegistry slice registry for route information, must not be null
     * @param validationService validation service for formatting violations, must not be null
     * @throws IllegalArgumentException if parameters are null
     */
    public HttpErrorHandler(final JsonService jsonService, final SliceRegistry sliceRegistry, final ValidationService validationService) {
        this.jsonService = Objects.requireNonNull(jsonService, "JSON service cannot be null");
        this.sliceRegistry = Objects.requireNonNull(sliceRegistry, "Slice registry cannot be null");
        this.validationService = Objects.requireNonNull(validationService, "Validation service cannot be null");
    }
    
    /**
     * Handles validation errors with HTTP 400 response.
     * 
     * @param exchange HTTP exchange, must not be null
     * @param validationException validation exception containing violations, must not be null
     */
    public void handleValidationError(final HttpServerExchange exchange, final ValidationException validationException) {
        Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
        Objects.requireNonNull(validationException, "Validation exception cannot be null");
        
        final String method = exchange.getRequestMethod().toString();
        final String path = exchange.getRequestPath();
        
        logger.warn("Validation failed for request {} {}: {}", method, path, validationException.getMessage());
        
        exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        
        try {
            final var errorResponse = new ValidationErrorResponse(
                "Validation Failed",
                validationService.formatViolations(validationException.getViolations()),
                validationException.getViolationCount()
            );
            
            final var jsonResponse = jsonService.toJson(errorResponse);
            exchange.getResponseSender().send(jsonResponse);
        } catch (Exception e) {
            logger.error("Failed to serialize validation error response: {}", e.getMessage(), e);
            sendFallbackErrorResponse(exchange, "Validation Failed");
        }
    }
    
    /**
     * Handles not found errors with HTTP 404 response.
     * 
     * @param exchange HTTP exchange, must not be null
     * @param method HTTP method, must not be null
     * @param path HTTP path, must not be null
     */
    public void handleNotFound(final HttpServerExchange exchange, final String method, final String path) {
        Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
        Objects.requireNonNull(method, "HTTP method cannot be null");
        Objects.requireNonNull(path, "HTTP path cannot be null");
        
        logger.debug("No slice found for: {} {}", method, path);
        
        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        
        try {
            final var errorResponse = new NotFoundResponse(
                "Not Found",
                "No slice registered for %s %s".formatted(method, path),
                sliceRegistry.getAllRouteKeys()
            );
            
            final var jsonResponse = jsonService.toJson(errorResponse);
            exchange.getResponseSender().send(jsonResponse);
        } catch (Exception e) {
            logger.error("Failed to serialize not found error response: {}", e.getMessage(), e);
            sendFallbackErrorResponse(exchange, "Not Found");
        }
    }
    
    /**
     * Handles internal server errors with HTTP 500 response.
     * 
     * @param exchange HTTP exchange, must not be null
     * @param error exception that caused the internal error, must not be null
     */
    public void handleInternalError(final HttpServerExchange exchange, final Exception error) {
        Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
        Objects.requireNonNull(error, "Error exception cannot be null");
        
        final String method = exchange.getRequestMethod().toString();
        final String path = exchange.getRequestPath();
        
        logger.error("Error handling request {} {}: {}", method, path, error.getMessage(), error);
        
        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        
        try {
            final var errorResponse = new ErrorResponse(
                "Internal Server Error",
                error.getMessage()
            );
            
            final var jsonResponse = jsonService.toJson(errorResponse);
            exchange.getResponseSender().send(jsonResponse);
        } catch (Exception e) {
            logger.error("Failed to serialize internal error response: {}", e.getMessage(), e);
            sendFallbackErrorResponse(exchange, "Internal Server Error");
        }
    }
    
    /**
     * Sends a simple fallback error response when JSON serialization fails.
     */
    private void sendFallbackErrorResponse(final HttpServerExchange exchange, final String errorType) {
        exchange.getResponseSender().send("""
            {"error":"%s"}
            """.formatted(errorType));
    }
    
    /**
     * Response record for validation errors.
     */
    public record ValidationErrorResponse(
        String error,
        String message,
        int violationCount
    ) {}
    
    /**
     * Response record for generic errors.
     */
    public record ErrorResponse(
        String error,
        String message
    ) {}
    
    /**
     * Response record for not found errors.
     */
    public record NotFoundResponse(
        String error,
        String message,
        java.util.Set<String> availableRoutes
    ) {}
}