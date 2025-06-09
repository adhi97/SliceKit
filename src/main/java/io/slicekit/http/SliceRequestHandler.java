package io.slicekit.http;

import io.slicekit.core.SliceMetadata;
import io.slicekit.discovery.SliceRegistry;
import io.slicekit.injection.SliceInjector;
import io.slicekit.validation.ValidationException;
import io.slicekit.validation.ValidationService;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates HTTP request processing through specialized components.
 * 
 * This class is responsible for:
 * - Matching incoming requests to registered slices
 * - Coordinating the request processing pipeline
 * - Delegating to specialized handlers for different concerns
 * 
 * Follows Single Responsibility Principle by delegating to:
 * - SliceInvoker for method invocation
 * - HttpErrorHandler for error responses  
 * - ResponseSerializer for success responses
 */
public final class SliceRequestHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceRequestHandler.class);
    
    private final SliceRegistry sliceRegistry;
    private final SliceInvoker sliceInvoker;
    private final HttpErrorHandler errorHandler;
    private final ResponseSerializer responseSerializer;
    
    /**
     * Creates a new slice request handler with specialized components.
     * 
     * @param sliceRegistry slice registry for route resolution, must not be null
     * @param sliceInjector dependency injector for slices, must not be null
     * @throws IllegalArgumentException if parameters are null
     */
    public SliceRequestHandler(final SliceRegistry sliceRegistry, final SliceInjector sliceInjector) {
        this.sliceRegistry = Objects.requireNonNull(sliceRegistry, "Slice registry cannot be null");
        
        // Create specialized components following SRP
        final JsonService jsonService = JsonService.getDefault();
        final ValidationService validationService = new ValidationService();
        
        this.sliceInvoker = new SliceInvoker(sliceInjector, validationService);
        this.errorHandler = new HttpErrorHandler(jsonService, sliceRegistry, validationService);
        this.responseSerializer = new ResponseSerializer(jsonService);
    }
    
    /**
     * Coordinates HTTP request processing through specialized components.
     * 
     * @param exchange HTTP server exchange containing request/response, must not be null
     * @throws Exception if request handling fails
     */
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
        
        final String method = exchange.getRequestMethod().toString();
        final String path = exchange.getRequestPath();
        
        logger.debug("Handling request: {} {}", method, path);
        
        try {
            // Find matching slice
            final Optional<SliceMetadata> sliceMetadata = sliceRegistry.findSlice(method, path);
            
            if (sliceMetadata.isPresent()) {
                handleSliceRequest(exchange, sliceMetadata.get());
            } else {
                errorHandler.handleNotFound(exchange, method, path);
            }
            
        } catch (ValidationException e) {
            errorHandler.handleValidationError(exchange, e);
        } catch (Exception e) {
            errorHandler.handleInternalError(exchange, e);
        }
    }
    
    /**
     * Handles a request that matches a registered slice by delegating to specialized components.
     */
    private void handleSliceRequest(final HttpServerExchange exchange, final SliceMetadata sliceMetadata) 
            throws Exception {
        
        logger.debug("Routing request to slice: {}", sliceMetadata.getName());
        
        try {
            // Create slice request wrapper
            final SliceRequest request = new SliceRequest(exchange, JsonService.getDefault());
            
            // Delegate to SliceInvoker for method invocation
            final Object result = sliceInvoker.invokeSlice(sliceMetadata, request);
            
            // Delegate to ResponseSerializer for response handling
            responseSerializer.handleSliceResponse(exchange, result);
            
        } catch (Exception e) {
            logger.error("Error executing slice {}: {}", sliceMetadata.getName(), e.getMessage(), e);
            throw e;
        }
    }
}